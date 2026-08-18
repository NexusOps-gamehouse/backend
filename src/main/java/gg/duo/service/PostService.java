package gg.duo.service;

import gg.duo.dto.PostDto;
import gg.duo.dto.UserDto;
import gg.duo.entity.Application;
import gg.duo.entity.ChatRoom;
import gg.duo.entity.Post;
import gg.duo.entity.User;
import gg.duo.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final ApplicationRepository applicationRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatMessageRepository chatMessageRepository;

    /** 목록 한 페이지의 최대 크기 — 클라이언트가 size 를 크게 보내도 여기서 막는다. */
    private static final int MAX_PAGE_SIZE = 100;

    /**
     * 목록 + 검색(제목/닉네임) + 필터(게임/모드/모집상태) + 페이징.
     *
     * 검색·필터 조건은 전부 쿼리에서 처리한다. 자바에서 거르면 DB 가 자른 뒤에
     * 거르는 순서가 되어 페이지마다 결과 개수가 들쭉날쭉해진다.
     */
    @Transactional(readOnly = true)
    public PostDto.ListResponse list(Long meId, String searchType, String keyword,
                                     String game, String gameMode, String status,
                                     int page, int size) {
        String kw = blankToNull(keyword);
        String g = blankToNull(game);
        String gm = blankToNull(gameMode);

        Post.Status st = null;
        if (blankToNull(status) != null) {
            try {
                st = Post.Status.valueOf(status.trim());
            } catch (IllegalArgumentException e) {
                // 알 수 없는 상태값 — 이전과 같이 결과 없음으로 처리한다.
                return new PostDto.ListResponse(List.of(), page, size, 0, false);
            }
        }

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE));

        String pattern = likePattern(kw);

        // 닉네임 검색은 작성자와 조인해야 하므로 쿼리를 나눠 두었다.
        // 검색어가 없으면 닉네임 조건 자체가 성립하지 않으니 제목 쪽으로 보낸다.
        Page<Post> found = ("nickname".equals(searchType) && kw != null)
                ? postRepository.searchByAuthorNickname(g, gm, st, pattern, pageable)
                : postRepository.searchByTitle(g, gm, st, pattern, pageable);

        List<PostDto.Summary> items = found.getContent().stream()
                .map(p -> toSummary(p, meId))
                .toList();

        return new PostDto.ListResponse(items, found.getNumber(), found.getSize(),
                found.getTotalElements(), found.hasNext());
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    /**
     * 검색어를 like 패턴으로 바꾼다.
     *
     * 검색어가 없어도 null 이 아니라 '%' 를 넘긴다. like 에 null 을 바인딩하면
     * 드라이버가 그 파라미터를 bytea 로 보내 Postgres 가 거부한다.
     *   ERROR: operator does not exist: character varying ~~ bytea
     *
     * 사용자가 친 %, _ 는 와일드카드가 아니라 글자 그대로 찾아야 하므로
     * 이스케이프 문자(!)를 앞에 붙인다. 쿼리 쪽에 escape '!' 가 선언돼 있다.
     * (이스케이프 문자 자신인 ! 를 가장 먼저 치환해야 이중 치환이 안 생긴다)
     */
    private static String likePattern(String keyword) {
        if (keyword == null) return "%";
        String escaped = keyword
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
        return "%" + escaped + "%";
    }

    @Transactional(readOnly = true)
    public PostDto get(Long postId, Long meId) {
        return toDto(postRepository.findById(postId).orElseThrow(), meId);
    }

    @Transactional(readOnly = true)
    public List<PostDto> myPosts(Long meId) {
        return postRepository.findByAuthorIdOrderByCreatedAtDesc(meId)
                .stream().map(p -> toDto(p, meId)).toList();
    }

    @Transactional
    public PostDto create(Long meId, PostDto.WriteRequest req) {
        validate(req);
        User author = userRepository.findById(meId).orElseThrow();
        Post post = new Post();
        post.setAuthor(author);
        applyFields(post, req);
        postRepository.save(post);
        return toDto(post, meId);
    }

    @Transactional
    public PostDto update(Long postId, Long meId, PostDto.WriteRequest req) {
        validate(req);
        Post post = postRepository.findById(postId).orElseThrow();
        if (!post.getAuthor().getId().equals(meId))
            throw new SecurityException("본인이 작성한 글만 수정할 수 있습니다.");
        applyFields(post, req);
        return toDto(post, meId);
    }

    /** 모집 완료 처리 — 이후 참가 신청 차단 */
    @Transactional
    public PostDto close(Long postId, Long meId) {
        Post post = postRepository.findById(postId).orElseThrow();
        if (!post.getAuthor().getId().equals(meId))
            throw new SecurityException("본인 글만 모집 완료할 수 있습니다.");
        post.setStatus(Post.Status.CLOSED);
        return toDto(post, meId);
    }

    @Transactional
    public void delete(Long postId, Long meId) {
        Post post = postRepository.findById(postId).orElseThrow();
        if (!post.getAuthor().getId().equals(meId))
            throw new SecurityException("본인이 작성한 글만 삭제할 수 있습니다.");
        // FK 참조 순서대로 연관 데이터 정리 (메시지 → 멤버 → 채팅방 → 신청 → 글)
        chatRoomRepository.findByPostId(postId).ifPresent(room -> {
            chatMessageRepository.deleteByRoomId(room.getId());
            chatRoomMemberRepository.deleteByRoomId(room.getId());
            chatRoomRepository.delete(room);
        });
        applicationRepository.deleteByPostId(postId);
        postRepository.delete(post);
    }

    private void applyFields(Post post, PostDto.WriteRequest req) {
        post.setTitle(req.title());
        post.setContent(req.content());
        post.setGame(req.game());
        post.setGameMode(req.gameMode());
        post.setPlayTime(req.playTime());
        post.setMicRequired(req.micRequired());
        post.setPositions(req.positions());
        int target = req.targetMembers() == null ? 2 : req.targetMembers();
        post.setTargetMembers(Math.max(2, Math.min(target, 10)));
    }

    private void validate(PostDto.WriteRequest req) {
        if (req.title() == null || req.title().isBlank())
            throw new IllegalArgumentException("제목을 입력해주세요.");
        if (req.content() == null || req.content().isBlank())
            throw new IllegalArgumentException("내용을 입력해주세요.");
    }

    /** 목록과 상세가 공통으로 쓰는 파생값 (글 자체에는 없고 조회해야 나오는 값들) */
    private record Extras(String myStatus, boolean mine, long pending,
                          long currentMembers, Long myRoomId) {}

    private Extras extras(Post p, Long meId) {
        boolean mine = meId != null && p.getAuthor().getId().equals(meId);

        String myStatus = null;
        if (meId != null && !mine) {
            myStatus = applicationRepository.findByPostIdAndApplicantId(p.getId(), meId)
                    .map(a -> a.getStatus().name()).orElse(null);
        }

        long pending = applicationRepository.countByPostIdAndStatus(p.getId(), Application.Status.PENDING);

        // 모집 현황(n/m)은 "확정된 인원"으로 센다.
        //
        // 예전에는 채팅방 인원(countByRoomId)을 셌는데, 채팅방에는 승인만 받고 아직
        // 확정되지 않은 사람도 들어와 있다. 그래서 자리가 남았는데도 정원이 찬 것처럼
        // 보이거나, 반대로 4/3 처럼 정원을 넘겨 표시되는 문제가 있었다.
        //
        // 채팅방 입장은 인원 제한이 없고(누구나 들러서 이야기할 수 있다),
        // 방장이 "확정"을 눌러야 비로소 한 자리가 채워진다.
        // 방장은 글을 만든 시점에 이미 한 자리를 차지하므로 +1 한다.
        long currentMembers = 1 + applicationRepository.countByPostIdAndStatus(
                p.getId(), Application.Status.CONFIRMED);

        ChatRoom room = chatRoomRepository.findByPostId(p.getId()).orElse(null);
        Long myRoomId = null;
        if (room != null && meId != null
                && chatRoomMemberRepository.existsByRoomIdAndUserId(room.getId(), meId)) {
            myRoomId = room.getId();
        }

        return new Extras(myStatus, mine, pending, currentMembers, myRoomId);
    }

    /** 목록용 — content 를 싣지 않는다. */
    private PostDto.Summary toSummary(Post p, Long meId) {
        Extras e = extras(p, meId);
        return new PostDto.Summary(p.getId(), p.getTitle(), p.getCreatedAt(),
                UserDto.from(p.getAuthor()), e.pending(), e.myStatus(), e.mine(),
                p.getGame(), p.getGameMode(), p.getPlayTime(), p.isMicRequired(),
                p.getPositions(), p.getTargetMembers(), e.currentMembers(),
                p.getStatus().name(), e.myRoomId());
    }

    private PostDto toDto(Post p, Long meId) {
        Extras e = extras(p, meId);
        return new PostDto(p.getId(), p.getTitle(), p.getContent(), p.getCreatedAt(),
                UserDto.from(p.getAuthor()), e.pending(), e.myStatus(), e.mine(),
                p.getGame(), p.getGameMode(), p.getPlayTime(), p.isMicRequired(),
                p.getPositions(), p.getTargetMembers(), e.currentMembers(),
                p.getStatus().name(), e.myRoomId());
    }
}
