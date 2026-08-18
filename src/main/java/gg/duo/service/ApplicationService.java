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
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

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
        return toDtos(applicationRepository.findByPostIdOrderByCreatedAtDesc(postId)
                .stream()
                .filter(a -> !isExpired(a))
                .toList());
    }

    /** 신청자용: 내 신청 현황 (거절/만료 제외) */
    @Transactional(readOnly = true)
    public List<ApplicationDto> myApplications(Long meId) {
        return toDtos(applicationRepository.findByApplicantIdOrderByCreatedAtDesc(meId)
                .stream()
                .filter(a -> a.getStatus() != Application.Status.REJECTED)
                .filter(a -> !isExpired(a))
                .toList());
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

        // 정원은 확정 단계에서만 강제한다. 채팅방 입장(approve)은 인원 제한이 없다.
        //
        // targetMembers 는 방장을 포함한 수이므로, 방장이 확정해 줄 수 있는 인원은
        // 그보다 하나 적다. (예: 4명짜리 방 → 방장 제외 3명까지 확정)
        int capacity = post.getTargetMembers() - 1;
        long confirmed = applicationRepository.countByPostIdAndStatus(
                post.getId(), Application.Status.CONFIRMED);
        if (confirmed >= capacity)
            throw new IllegalStateException(
                    "확정 인원이 모두 찼습니다. (최대 " + capacity + "명)");

        app.setStatus(Application.Status.CONFIRMED);
        ChatRoom room = chatRoomRepository.findByPostId(post.getId()).orElseThrow();
        chatRoomMemberRepository.findByRoomIdAndUserId(room.getId(), app.getApplicant().getId())
                .ifPresent(m -> m.setConfirmed(true));

        // 확정 인원이 다 차도 모집글을 자동으로 닫지 않는다.
        // 마감 여부는 방장이 "모집 완료"(PostService.close)로 직접 정한다.
        // 자리가 찬 것은 목록의 n/m 표시로 드러나면 충분하다.

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

    /**
     * 목록 변환.
     *
     * 신청마다 연관을 하나씩 끌어오지 않는다. 필요한 id 를 먼저 모아 IN 으로 한 번씩만
     * 조회하고 Map 에서 꺼내 쓴다. 글·신청자·채팅방 셋 다 같은 방식이다.
     *
     * 조회 쿼리에 조인(fetch join)을 걸어도 문장 수는 같지만, 바깥 행이 적으면 플래너가
     * nested loop 를 골라 항목마다 인덱스를 다시 훑는다. IN 한 방은 대상 테이블을 한 번만
     * 훑으므로 항목 수와 무관한 비용이 된다.
     *
     * a.getPost()·a.getApplicant() 는 프록시라 getId() 만 만지면 쿼리가 나가지 않는다.
     * 그래서 본문은 프록시가 아니라 Map 에서 읽는다.
     */
    private List<ApplicationDto> toDtos(List<Application> applications) {
        if (applications.isEmpty()) return List.of();

        Map<Long, String> titleByPostId = postRepository
                .findAllById(idsOf(applications, a -> a.getPost().getId()))
                .stream().collect(Collectors.toMap(Post::getId, Post::getTitle));

        Map<Long, UserDto> applicantById = userRepository
                .findAllById(idsOf(applications, a -> a.getApplicant().getId()))
                .stream().collect(Collectors.toMap(User::getId, UserDto::from));

        Set<Long> roomPostIds = idsOf(
                applications.stream().filter(ApplicationService::hasChatRoom).toList(),
                a -> a.getPost().getId());
        Map<Long, Long> roomIdByPostId = roomPostIds.isEmpty() ? Map.of()
                : chatRoomRepository.findByPostIdIn(roomPostIds).stream()
                        .collect(Collectors.toMap(r -> r.getPost().getId(), ChatRoom::getId));

        return applications.stream()
                .map(a -> new ApplicationDto(a.getId(), a.getStatus().name(), a.getCreatedAt(),
                        a.getPost().getId(), titleByPostId.get(a.getPost().getId()),
                        applicantById.get(a.getApplicant().getId()),
                        hasChatRoom(a) ? roomIdByPostId.get(a.getPost().getId()) : null))
                .toList();
    }

    private static Set<Long> idsOf(List<Application> applications, Function<Application, Long> id) {
        return applications.stream().map(id).collect(Collectors.toSet());
    }

    /** 승인된 신청만 채팅방을 갖는다 */
    private static boolean hasChatRoom(Application a) {
        return a.getStatus() == Application.Status.APPROVED
                || a.getStatus() == Application.Status.CONFIRMED;
    }
}
