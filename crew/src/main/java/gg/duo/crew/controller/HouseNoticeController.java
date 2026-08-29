package gg.duo.crew.controller;

import gg.duo.crew.dto.NoticeDto;
import gg.duo.crew.service.NoticeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/crew/houses/{houseId}/notices")
@RequiredArgsConstructor
public class HouseNoticeController {

    private final NoticeService noticeService;

    private Long userId(Authentication auth) {
        return auth == null ? null : (Long) auth.getPrincipal();
    }

    @GetMapping
    public List<NoticeDto.Response> list(@PathVariable Long houseId, Authentication auth) {
        return noticeService.list(houseId, userId(auth));
    }

    @PostMapping
    public NoticeDto.Response create(@PathVariable Long houseId, Authentication auth,
                                     @Valid @RequestBody NoticeDto.WriteRequest req) {
        return noticeService.create(houseId, userId(auth), req);
    }

    @PutMapping("/{noticeId}")
    public NoticeDto.Response update(@PathVariable Long houseId, @PathVariable Long noticeId,
                                     Authentication auth,
                                     @Valid @RequestBody NoticeDto.UpdateRequest req) {
        return noticeService.update(houseId, noticeId, userId(auth), req);
    }

    @PutMapping("/{noticeId}/pin")
    public NoticeDto.Response pin(@PathVariable Long houseId, @PathVariable Long noticeId,
                                  @RequestBody Map<String, Boolean> body, Authentication auth) {
        return noticeService.pin(houseId, noticeId, userId(auth),
                Boolean.TRUE.equals(body.get("pinned")));
    }

    @DeleteMapping("/{noticeId}")
    public void delete(@PathVariable Long houseId, @PathVariable Long noticeId,
                       Authentication auth) {
        noticeService.delete(houseId, noticeId, userId(auth));
    }
}
