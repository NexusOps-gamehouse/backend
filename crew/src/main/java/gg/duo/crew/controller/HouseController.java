package gg.duo.crew.controller;

import gg.duo.crew.dto.HouseDto;
import gg.duo.crew.service.HouseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * House(크루) API.
 *
 * 경로 접두어가 /api/crew 인 이유: 프론트의 vite 프록시와 k8s Ingress 가
 * 경로 앞부분만 보고 서비스를 고른다. /api/houses 로 두면 어느 서비스로
 * 보낼지 규칙을 하나 더 만들어야 하고, 서비스 이름과 경로가 어긋난다.
 *
 * ⚠️ 프론트에서 쓰려면 frontend/vite.config.js 프록시에 '/api/crew' → :8086 을
 *    추가해야 한다. (현재 프론트 House 화면은 아직 mocks/houseStorage.js 를 쓴다)
 */
@RestController
@RequestMapping("/api/crew")
@RequiredArgsConstructor
public class HouseController {

    private final HouseService houseService;

    private Long userId(Authentication auth) {
        return auth == null ? null : (Long) auth.getPrincipal();
    }

    /** House 목록. 비로그인도 볼 수 있다 — 로그인했다면 내 가입 상태가 함께 실린다. */
    @GetMapping("/houses")
    public List<HouseDto.Summary> list(Authentication auth) {
        return houseService.list(userId(auth));
    }

    @GetMapping("/houses/{houseId}")
    public HouseDto.Detail get(@PathVariable Long houseId, Authentication auth) {
        return houseService.get(houseId, userId(auth));
    }

    @PostMapping("/houses")
    public HouseDto.Detail create(Authentication auth,
                                  @Valid @RequestBody HouseDto.WriteRequest req) {
        return houseService.create(userId(auth), req);
    }

    @PutMapping("/houses/{houseId}")
    public HouseDto.Detail update(@PathVariable Long houseId, Authentication auth,
                                  @Valid @RequestBody HouseDto.WriteRequest req) {
        return houseService.update(houseId, userId(auth), req);
    }

    /** 내가 속한 House 들. */
    @GetMapping("/my/houses")
    public List<HouseDto.Summary> myHouses(Authentication auth) {
        return houseService.myHouses(userId(auth));
    }

    // ------------------------------------------------------------- 가입

    /** 가입 신청. PUBLIC 은 즉시 가입, PRIVATE 은 승인 대기 상태로 응답한다. */
    @PostMapping("/houses/{houseId}/join")
    public HouseDto.Detail join(@PathVariable Long houseId, Authentication auth) {
        return houseService.apply(houseId, userId(auth));
    }

    /** 신청 취소 · 탈퇴 — 둘 다 "내 멤버 행을 지운다"로 같다. */
    @DeleteMapping("/houses/{houseId}/join")
    public void leave(@PathVariable Long houseId, Authentication auth) {
        Long me = userId(auth);
        houseService.removeMember(houseId, me, me);
    }

    @GetMapping("/houses/{houseId}/join-requests")
    public List<HouseDto.Member> joinRequests(@PathVariable Long houseId, Authentication auth) {
        return houseService.pendingRequests(houseId, userId(auth));
    }

    @PostMapping("/houses/{houseId}/members/{targetUserId}/approve")
    public HouseDto.Member approve(@PathVariable Long houseId, @PathVariable Long targetUserId,
                                   Authentication auth) {
        return houseService.approve(houseId, userId(auth), targetUserId);
    }

    @PostMapping("/houses/{houseId}/members/{targetUserId}/reject")
    public HouseDto.Member reject(@PathVariable Long houseId, @PathVariable Long targetUserId,
                                  Authentication auth) {
        return houseService.reject(houseId, userId(auth), targetUserId);
    }

    // ------------------------------------------------------------- 멤버 관리

    @PutMapping("/houses/{houseId}/members/{targetUserId}/role")
    public HouseDto.Member changeRole(@PathVariable Long houseId, @PathVariable Long targetUserId,
                                      @RequestBody HouseDto.RoleRequest req, Authentication auth) {
        return houseService.changeRole(houseId, userId(auth), targetUserId, req.role());
    }

    @DeleteMapping("/houses/{houseId}/members/{targetUserId}")
    public void removeMember(@PathVariable Long houseId, @PathVariable Long targetUserId,
                             Authentication auth) {
        houseService.removeMember(houseId, userId(auth), targetUserId);
    }
}
