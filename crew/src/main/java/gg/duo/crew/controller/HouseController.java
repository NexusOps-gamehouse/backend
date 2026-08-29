package gg.duo.crew.controller;

import gg.duo.crew.domain.house.House;
import gg.duo.crew.domain.house.HouseRepository;
import gg.duo.crew.dto.HouseCurrencyDto;
import gg.duo.crew.dto.HouseDto;
import gg.duo.crew.service.HouseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/crew")
@RequiredArgsConstructor
public class HouseController {

    private final HouseService houseService;
    private final HouseRepository houseRepository; // 👈 리포지토리 의존성 추가

    private Long userId(Authentication auth) {
        return auth == null ? null : (Long) auth.getPrincipal();
    }

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

    @GetMapping("/my/houses")
    public List<HouseDto.Summary> myHouses(Authentication auth) {
        return houseService.myHouses(userId(auth));
    }

    @PostMapping("/houses/{houseId}/join")
    public HouseDto.Detail join(@PathVariable Long houseId, Authentication auth) {
        return houseService.apply(houseId, userId(auth));
    }

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

    // 🔒 하우스 재화(HC, XP) 조회 API
    @GetMapping("/houses/{houseId}/currency")
    public ResponseEntity<HouseCurrencyDto> getCurrency(
            @PathVariable Long houseId,
            Authentication auth) {
        Long userId = userId(auth);
        houseService.requireApprovedMember(houseId, userId);

        House house = houseRepository.findById(houseId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 하우스입니다."));

        HouseCurrencyDto response = HouseCurrencyDto.builder()
                .houseId(houseId)
                .currentHc(house.getHc())
                .currentXp(house.getXp())
                .build();

        return ResponseEntity.ok(response);
    }
}