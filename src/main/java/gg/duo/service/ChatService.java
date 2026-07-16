package gg.duo.service;

import gg.duo.dto.ChatDtos.MemberDto;
import gg.duo.dto.ChatDtos.MessageDto;
import gg.duo.dto.ChatDtos.RoomDetail;
import gg.duo.dto.ChatDtos.RoomDto;
import gg.duo.dto.UserDto;
import gg.duo.entity.*;
import gg.duo.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional(readOnly = true)
    public RoomDetail roomDetail(Long roomId, Long meId) {
        ChatRoom room = chatRoomRepository.findById(roomId).orElseThrow();
        assertMember(room.getId(), meId);
        List<MessageDto> messages = chatMessageRepository.findByRoomIdOrderByCreatedAtAsc(roomId)
                .stream().map(this::toMessageDto).toList();
        return new RoomDetail(toRoomDto(room), messages);
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
