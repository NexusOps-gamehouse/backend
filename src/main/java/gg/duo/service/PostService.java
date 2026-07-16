package gg.duo.service;

import gg.duo.dto.PostDto;
import gg.duo.dto.UserDto;
import gg.duo.entity.Application;
import gg.duo.entity.ChatRoom;
import gg.duo.entity.Post;
import gg.duo.entity.User;
import gg.duo.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final ApplicationRepository applicationRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatMessageRepository chatMessageRepository;

    /** 목록 + 검색(제목/닉네임) + 필터(게임/모드/모집상태) */
    @Transactional(readOnly = true)
    public List<PostDto> list(Long meId, String searchType, String keyword,
                              String game, String gameMode, String status) {
        Stream<Post> stream = postRepository.findAllByOrderByCreatedAtDesc().stream();
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            if ("nickname".equals(searchType)) {
                stream = stream.filter(p -> p.getAuthor().getNickname().contains(kw));
            } else {
                stream = stream.filter(p -> p.getTitle().contains(kw));
            }
        }
        if (game != null && !game.isBlank())
            stream = stream.filter(p -> game.equals(p.getGame()));
        if (gameMode != null && !gameMode.isBlank())
            stream = stream.filter(p -> gameMode.equals(p.getGameMode()));
        if (status != null && !status.isBlank())
            stream = stream.filter(p -> p.getStatus().name().equals(status));
        return stream.map(p -> toDto(p, meId)).toList();
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

    private PostDto toDto(Post p, Long meId) {
        String myStatus = null;
        boolean mine = meId != null && p.getAuthor().getId().equals(meId);
        if (meId != null && !mine) {
            myStatus = applicationRepository.findByPostIdAndApplicantId(p.getId(), meId)
                    .map(a -> a.getStatus().name()).orElse(null);
        }
        long pending = applicationRepository.countByPostIdAndStatus(p.getId(), Application.Status.PENDING);

        ChatRoom room = chatRoomRepository.findByPostId(p.getId()).orElse(null);
        long currentMembers = room == null ? 1 : chatRoomMemberRepository.countByRoomId(room.getId());
        Long myRoomId = null;
        if (room != null && meId != null
                && chatRoomMemberRepository.existsByRoomIdAndUserId(room.getId(), meId)) {
            myRoomId = room.getId();
        }

        return new PostDto(p.getId(), p.getTitle(), p.getContent(), p.getCreatedAt(),
                UserDto.from(p.getAuthor()), pending, myStatus, mine,
                p.getGame(), p.getGameMode(), p.getPlayTime(), p.isMicRequired(),
                p.getPositions(), p.getTargetMembers(), currentMembers,
                p.getStatus().name(), myRoomId);
    }
}
