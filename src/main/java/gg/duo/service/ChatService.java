package gg.duo.service;

import gg.duo.dto.ChatDtos.MemberDto;
import gg.duo.dto.ChatDtos.MessageDto;
import gg.duo.dto.ChatDtos.MessagePage;
import gg.duo.dto.ChatDtos.RoomDetail;
import gg.duo.dto.ChatDtos.RoomDto;
import gg.duo.dto.UserDto;
import gg.duo.entity.*;
import gg.duo.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final ApplicationRepository applicationRepository;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public List<RoomDto> myRooms(Long meId) {
        return chatRoomMemberRepository.findByUserIdOrderByIdDesc(meId)
                .stream().map(m -> toRoomDto(m.getRoom())).toList();
    }

    /** 방 입장 시 한 번에 내려주는 메시지 수 */
    private static final int PAGE_SIZE = 50;

    /** 한 번에 요청할 수 있는 최대 메시지 수 */
    private static final int MAX_PAGE_SIZE = 100;

    @Transactional(readOnly = true)
    public RoomDetail roomDetail(Long roomId, Long meId) {
        ChatRoom room = chatRoomRepository.findById(roomId).orElseThrow();
        assertMember(room.getId(), meId);

        Slice page = loadMessages(roomId, null, PAGE_SIZE);
        return new RoomDetail(toRoomDto(room), page.messages(), page.hasMore());
    }

    /**
     * 이전 메시지 — beforeId 보다 앞선 것들.
     *
     * 방을 여는 시점 기준이 아니라 "이 메시지보다 앞"으로 요청하므로,
     * 그 사이에 새 메시지가 도착해도 경계가 밀리지 않는다.
     */
    @Transactional(readOnly = true)
    public MessagePage messagesBefore(Long roomId, Long beforeId, Integer size, Long meId) {
        assertMember(roomId, meId);
        int limit = (size == null) ? PAGE_SIZE : Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Slice page = loadMessages(roomId, beforeId, limit);
        return new MessagePage(page.messages(), page.hasMore());
    }

    private record Slice(List<MessageDto> messages, boolean hasMore) {}

    /**
     * 최신순으로 limit + 1 개를 읽어 hasMore 를 판단하고, 화면 순서(오래된 것이 앞)로
     * 뒤집어 돌려준다.
     *
     * 하나를 더 읽는 이유: 별도의 count 쿼리 없이 "더 있는지"를 알 수 있다.
     * limit + 1 개가 나왔다면 넘치는 하나를 버리고 hasMore = true 로 둔다.
     */
    private Slice loadMessages(Long roomId, Long beforeId, int limit) {
        Pageable probe = PageRequest.of(0, limit + 1);
        List<ChatMessage> found = (beforeId == null)
                ? chatMessageRepository.findByRoomIdOrderByIdDesc(roomId, probe)
                : chatMessageRepository.findByRoomIdAndIdLessThanOrderByIdDesc(roomId, beforeId, probe);

        boolean hasMore = found.size() > limit;
        List<ChatMessage> page = hasMore ? found.subList(0, limit) : found;

        // 조회는 최신순(desc), 화면은 오래된 것이 위 → 뒤집는다.
        List<MessageDto> messages = new ArrayList<>(page.size());
        for (int i = page.size() - 1; i >= 0; i--) {
            messages.add(toMessageDto(page.get(i)));
        }
        return new Slice(messages, hasMore);
    }

    @Transactional
    public MessageDto saveMessage(Long roomId, Long senderId, String content) {
        if (content == null || content.isBlank())
            throw new IllegalArgumentException("메시지를 입력해주세요.");
        ChatRoom room = chatRoomRepository.findById(roomId).orElseThrow();
        assertMember(room.getId(), senderId);
        User sender = userRepository.findById(senderId).orElseThrow();
        ChatMessage msg = new ChatMessage();
        msg.setRoom(room);
        msg.setSender(sender);
        msg.setContent(content.trim());
        chatMessageRepository.save(msg);
        return toMessageDto(msg);
    }

    /** 방장: 멤버 강퇴 (채팅방 제거 + 신청 거절 처리) */
    @Transactional
    public void kick(Long roomId, Long targetUserId, Long meId) {
        ChatRoom room = chatRoomRepository.findById(roomId).orElseThrow();
        if (!room.getOwner().getId().equals(meId))
            throw new SecurityException("방장만 내보낼 수 있습니다.");
        if (room.getOwner().getId().equals(targetUserId))
            throw new IllegalStateException("방장은 내보낼 수 없습니다.");

        ChatRoomMember member = chatRoomMemberRepository
                .findByRoomIdAndUserId(roomId, targetUserId).orElseThrow();
        User target = member.getUser();
        chatRoomMemberRepository.delete(member);

        applicationRepository.findByPostIdAndApplicantId(room.getPost().getId(), targetUserId)
                .ifPresent(a -> a.setStatus(Application.Status.REJECTED));

        notificationService.notify(target,
                "'" + room.getPost().getTitle() + "' 파티에서 내보내졌습니다.", null);
    }

    private void assertMember(Long roomId, Long userId) {
        if (!chatRoomMemberRepository.existsByRoomIdAndUserId(roomId, userId))
            throw new SecurityException("채팅방 참여자가 아닙니다.");
    }

    private RoomDto toRoomDto(ChatRoom room) {
        List<MemberDto> members = chatRoomMemberRepository
                .findByRoomIdOrderByJoinedAtAsc(room.getId())
                .stream().map(m -> {
                    boolean owner = room.getOwner().getId().equals(m.getUser().getId());
                    Long applicationId = owner ? null : applicationRepository
                            .findByPostIdAndApplicantId(room.getPost().getId(), m.getUser().getId())
                            .map(Application::getId).orElse(null);
                    return new MemberDto(UserDto.from(m.getUser()), m.isConfirmed(), owner, applicationId);
                }).toList();
        return new RoomDto(room.getId(), room.getPost().getId(), room.getPost().getTitle(),
                room.getOwner().getId(), room.getPost().getStatus().name(), members);
    }

    private MessageDto toMessageDto(ChatMessage m) {
        return new MessageDto(m.getId(), m.getRoom().getId(), m.getSender().getId(),
                m.getSender().getNickname(), m.getContent(), m.getCreatedAt());
    }
}
