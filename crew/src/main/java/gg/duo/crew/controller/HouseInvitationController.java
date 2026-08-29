package gg.duo.crew.controller;

import gg.duo.crew.dto.HouseInvitationDto;
import gg.duo.crew.service.HouseInvitationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/crew")
@RequiredArgsConstructor
public class HouseInvitationController {

    private final HouseInvitationService houseInvitationService;

    private Long userId(Authentication auth) {
        return auth == null ? null : (Long) auth.getPrincipal();
    }

    /**
     * House에 여러 친구를 초대한다.
     */
    @PostMapping("/houses/{houseId}/invitations")
    public HouseInvitationDto.InviteResponse invite(
            @PathVariable Long houseId,
            @Valid @RequestBody HouseInvitationDto.InviteRequest request,
            Authentication auth
    ) {
        return houseInvitationService.invite(
                houseId,
                userId(auth),
                request
        );
    }

    /**
     * 현재 사용자가 받은 대기 중 House 초대.
     */
    @GetMapping("/invitations/me")
    public List<HouseInvitationDto.Invitation> myInvitations(
            Authentication auth
    ) {
        return houseInvitationService.myInvitations(userId(auth));
    }

    /**
     * House 초대 수락.
     */
    @PostMapping("/invitations/{invitationId}/accept")
    public HouseInvitationDto.Invitation accept(
            @PathVariable Long invitationId,
            Authentication auth
    ) {
        return houseInvitationService.accept(
                invitationId,
                userId(auth)
        );
    }

    /**
     * House 초대 거절.
     */
    @PostMapping("/invitations/{invitationId}/reject")
    public HouseInvitationDto.Invitation reject(
            @PathVariable Long invitationId,
            Authentication auth
    ) {
        return houseInvitationService.reject(
                invitationId,
                userId(auth)
        );
    }
}
