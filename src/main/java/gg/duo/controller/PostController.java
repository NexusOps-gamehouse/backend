package gg.duo.controller;

import gg.duo.dto.PostDto;
import gg.duo.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    private Long userId(Authentication auth) {
        return auth == null ? null : (Long) auth.getPrincipal();
    }

    @GetMapping("/api/posts")
    public List<PostDto> list(Authentication auth,
                              @RequestParam(required = false) String searchType,
                              @RequestParam(required = false) String keyword,
                              @RequestParam(required = false) String game,
                              @RequestParam(required = false) String gameMode,
                              @RequestParam(required = false) String status) {
        return postService.list(userId(auth), searchType, keyword, game, gameMode, status);
    }

    /** 모집 완료 처리 */
    @PostMapping("/api/posts/{id}/close")
    public PostDto close(@PathVariable Long id, Authentication auth) {
        return postService.close(id, userId(auth));
    }

    @GetMapping("/api/posts/{id}")
    public PostDto get(@PathVariable Long id, Authentication auth) {
        return postService.get(id, userId(auth));
    }

    @PostMapping("/api/posts")
    public PostDto create(Authentication auth, @RequestBody PostDto.WriteRequest req) {
        return postService.create(userId(auth), req);
    }

    @PutMapping("/api/posts/{id}")
    public PostDto update(@PathVariable Long id, Authentication auth,
                          @RequestBody PostDto.WriteRequest req) {
        return postService.update(id, userId(auth), req);
    }

    @DeleteMapping("/api/posts/{id}")
    public void delete(@PathVariable Long id, Authentication auth) {
        postService.delete(id, userId(auth));
    }

    @GetMapping("/api/my/posts")
    public List<PostDto> myPosts(Authentication auth) {
        return postService.myPosts(userId(auth));
    }
}
