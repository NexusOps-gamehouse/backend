package gg.duo.service;

import gg.duo.dto.ApplicationDto;
import gg.duo.dto.UserDto;
import gg.duo.entity.*;
import gg.duo.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private static final Duration PENDING_TTL = Duration.ofHours(1);

    private final ApplicationRepository applicationRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final NotificationService notificationService;

    @Transactional
    public void apply(Long postId, Long meId) {
        Post post = postRepository.findById(postId).orElseThrow();
        if (post.getStatus() == Post.Status.CLOSED)
            throw new IllegalStateException("모집이 완료된 글입니다.");
        if (post.getAuthor().getId().equals(meId))
            throw new IllegalStateException("본인 글에는 참가 신청할 수 없습니다.");
        if (applicationRepository.existsByPostIdAndApplicantId(postId, meId))
            throw new IllegalStateException("이미 참가 신청한 글입니다.");

        User me = userRepository.findById(meId).orElseThrow();
        Application app = new Application();
        app.setPost(post);
        app.setApplicant(me);
        applicationRepository.save(app);

        notificationService.notify(post.getAuthor(),
                me.getNickname() + "님이 '" + post.getTitle() + "' 글에 참가 신청했습니다.",
                "/post/" + post.getId());
    }

    /** 신청자: 대기 중인 참가 신청 취소 */
    @Transactional
    public void cancel(Long postId, Long meId) {
        Application app = applicationRepository.findByPostIdAndApplicantId(postId, meId)
                .orElseThrow(() -> new IllegalArgumentException("취소할 참가 신청이 없습니다."));
        if (app.getStatus() != Application.Status.PENDING)
            throw new IllegalStateException("대기 중인 신청만 취소할 수 있습니다.");

        applicationRepository.delete(app);
    }

    /** 방장용: 신청자 목록 (만료된 대기 신청 제외) */
    @Transactional(readOnly = true)
    public List<ApplicationDto> listForPost(Long postId, Long meId) {
        Post post = postRepository.findById(postId).orElseThrow();
        if (!post.getAuthor().getId().equals(meId))
            throw new SecurityException("본인 글의 신청자만 볼 수 있습니다.");
        return applicationRepository.findByPostIdOrderByCreatedAtDesc(postId)
                .stream()
                .filter(a -> !isExpired(a))
                .map(this::toDto)
                .toList();
    }

    /** 신청자용: 내 신청 현황 (거절/만료 제외) */
    @Transactional(readOnly = true)
    public List<ApplicationDto> myApplications(Long meId) {
        return applicationRepository.findByApplicantIdOrderByCreatedAtDesc(meId)
                .stream()
                .filter(a -> a.getStatus() != Application.Status.REJECTED)
                .filter(a -> !isExpired(a))
                .map(this::toDto)
                .toList();
    }

    /** 승인 → 파티 채팅방에 멤버로 추가 (방 없으면 생성) */
    @Transactional
    public Map<String, Long> approve(Long applicationId, Long meId) {
        Application app = applicationRepository.findById(applicationId).orElseThrow();
        Post post = app.getPost();
        if (!post.getAuthor().getId().equals(meId))
            throw new SecurityException("본인 글의 신청만 처리할 수 있습니다.");
        if (app.getStatus() != Application.Status.PENDING)
            throw new IllegalStateException("이미 처리된 신청입니다.");

        app.setStatus(Application.Status.APPROVED);

        ChatRoom room = chatRoomRepository.findByPostId(post.getId()).orElseGet(() -> {
            ChatRoom r = new ChatRoom();
            r.setPost(post);
            r.setOwner(post.getAuthor());
            chatRoomRepository.save(r);
            ChatRoomMember ownerMember = new ChatRoomMember();
            ownerMember.setRoom(r);
            ownerMember.setUser(post.getAuthor());
            ownerMember.setConfirmed(true);
            chatRoomMemberRepository.save(ownerMember);
            return r;
        });

        if (!chatRoomMemberRepository.existsByRoomIdAndUserId(room.getId(), app.getApplicant().getId())) {
            ChatRoomMember member = new ChatRoomMember();
            member.setRoom(room);
            member.setUser(app.getApplicant());
            chatRoomMemberRepository.save(member);
        }

        notificationService.notify(app.getApplicant(),
                "'" + post.getTitle() + "' 참가 신청이 승인되었습니다. 파티 채팅방에 입장하세요!",
                "/chat/" + room.getId());
        return Map.of("chatRoomId", room.getId());
    }

    /** 방장: 파티원 모집 확정 */
    @Transactional
    public void confirm(Long applicationId, Long meId) {
        Application app = applicationRepository.findById(applicationId).orElseThrow();
        Post post = app.getPost();
        if (!post.getAuthor().getId().equals(meId))
            throw new SecurityException("본인 글의 신청만 처리할 수 있습니다.");
        if (app.getStatus() != Application.Status.APPROVED)
            throw new IllegalStateException("승인된(채팅 참여 중인) 신청만 확정할 수 있습니다.");

        app.setStatus(Application.Status.CONFIRMED);
        ChatRoom room = chatRoomRepository.findByPostId(post.getId()).orElseThrow();
        chatRoomMemberRepository.findByRoomIdAndUserId(room.getId(), app.getApplicant().getId())
                .ifPresent(m -> m.setConfirmed(true));

        notificationService.notify(app.getApplicant(),
                "'" + post.getTitle() + "' 파티에 확정되었습니다!",
                "/chat/" + room.getId());
    }

    @Transactional
    public void reject(Long applicationId, Long meId) {
        Application app = applicationRepository.findById(applicationId).orElseThrow();
        Post post = app.getPost();
        if (!post.getAuthor().getId().equals(meId))
            throw new SecurityException("본인 글의 신청만 처리할 수 있습니다.");
        if (app.getStatus() != Application.Status.PENDING)
            throw new IllegalStateException("이미 처리된 신청입니다.");

        app.setStatus(Application.Status.REJECTED);
        notificationService.notify(app.getApplicant(),
                "'" + post.getTitle() + "' 참가 신청이 거절되었습니다.", null);
    }

    private boolean isExpired(Application a) {
        return a.getStatus() == Application.Status.PENDING
                && a.getCreatedAt().isBefore(Instant.now().minus(PENDING_TTL));
    }

    private ApplicationDto toDto(Application a) {
        Long roomId = null;
        if (a.getStatus() == Application.Status.APPROVED
                || a.getStatus() == Application.Status.CONFIRMED) {
            roomId = chatRoomRepository.findByPostId(a.getPost().getId())
                    .map(ChatRoom::getId).orElse(null);
        }
        return new ApplicationDto(a.getId(), a.getStatus().name(), a.getCreatedAt(),
                a.getPost().getId(), a.getPost().getTitle(),
                UserDto.from(a.getApplicant()), roomId);
    }
}
