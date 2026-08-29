package gg.duo.crew.service;

import gg.duo.common.exception.BusinessException;
import gg.duo.common.exception.ErrorCode;
import gg.duo.crew.domain.house.*;
import gg.duo.crew.dto.HouseInvitationDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class HouseInvitationService {

    private final HouseRepository houseRepository;
    private final HouseMemberRepository houseMemberRepository;
    private final HouseInvitationRepository houseInvitationRepository;

    /**
     * 비공개 House에 친구를 초대한다.
     *
     * 방장/부방장만 가능하고, 이미 가입했거나 가입 신청 중인 사용자는 건너뛴다.
     */
    @Transactional
    public HouseInvitationDto.InviteResponse invite(
            Long houseId,
            Long requesterId,
            HouseInvitationDto.InviteRequest request
    ) {
        requireAuthenticated(requesterId);

        House house = loadHouse(houseId);
        requireManager(house, requesterId);

        if (house.getType() != HouseType.PRIVATE) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT,
                    "친구 초대는 비공개 House에서만 사용할 수 있습니다."
            );
        }

        Set<Long> targets = new LinkedHashSet<>(request.userIds());

        List<HouseInvitationDto.Invitation> created = new ArrayList<>();
        int skipped = 0;

        for (Long targetUserId : targets) {
            if (targetUserId == null || targetUserId.equals(requesterId)) {
                skipped++;
                continue;
            }

            HouseMember membership = houseMemberRepository
                    .findByHouseIdAndUserId(houseId, targetUserId)
                    .orElse(null);

            /*
             * 이미 가입했거나 가입 신청 중이면 초대를 만들지 않는다.
             * REJECTED 멤버십만 과거 기록이므로 다시 초대 가능하다.
             */
            if (membership != null
                    && membership.getStatus() != JoinStatus.REJECTED) {
                skipped++;
                continue;
            }

            HouseInvitation invitation = houseInvitationRepository
                    .findByHouseIdAndInvitedUserId(houseId, targetUserId)
                    .orElse(null);

            if (invitation != null
                    && invitation.getStatus() == InvitationStatus.PENDING) {
                skipped++;
                continue;
            }

            if (invitation == null) {
                invitation = HouseInvitation.builder()
                        .house(house)
                        .invitedUserId(targetUserId)
                        .invitedByUserId(requesterId)
                        .status(InvitationStatus.PENDING)
                        .build();

                houseInvitationRepository.save(invitation);
            } else {
                invitation.reopen(requesterId);
            }

            created.add(HouseInvitationDto.Invitation.of(invitation));
        }

        if (created.isEmpty()) {
            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "초대할 수 있는 사용자가 없습니다."
            );
        }

        return new HouseInvitationDto.InviteResponse(
                created.size(),
                skipped,
                created
        );
    }

    /**
     * 현재 로그인한 사용자가 받은 대기 중 House 초대 목록.
     */
    @Transactional(readOnly = true)
    public List<HouseInvitationDto.Invitation> myInvitations(Long userId) {
        requireAuthenticated(userId);

        return houseInvitationRepository
                .findByInvitedUserIdAndStatusOrderByCreatedAtDesc(
                        userId,
                        InvitationStatus.PENDING
                )
                .stream()
                .map(HouseInvitationDto.Invitation::of)
                .toList();
    }

    /**
     * 초대 수락.
     *
     * 수락 즉시 HouseMember APPROVED 상태가 된다.
     */
    @Transactional
    public HouseInvitationDto.Invitation accept(
            Long invitationId,
            Long userId
    ) {
        requireAuthenticated(userId);

        HouseInvitation invitation = loadInvitation(invitationId);
        requireInvitee(invitation, userId);
        requirePending(invitation);

        House house = loadHouse(invitation.getHouse().getId());

        HouseMember existing = houseMemberRepository
                .findByHouseIdAndUserId(house.getId(), userId)
                .orElse(null);

        if (existing != null
                && existing.getStatus() == JoinStatus.APPROVED) {
            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "이미 가입한 House입니다."
            );
        }

        if (house.isFull()) {
            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "House 정원이 가득 찼습니다."
            );
        }

        if (existing != null) {
            existing.changeRole(MemberRole.MEMBER);
            existing.approve();
        } else {
            house.addMember(
                    HouseMember.builder()
                            .userId(userId)
                            .role(MemberRole.MEMBER)
                            .status(JoinStatus.APPROVED)
                            .build()
            );
        }

        invitation.accept();

        return HouseInvitationDto.Invitation.of(invitation);
    }

    /**
     * 초대 거절.
     */
    @Transactional
    public HouseInvitationDto.Invitation reject(
            Long invitationId,
            Long userId
    ) {
        requireAuthenticated(userId);

        HouseInvitation invitation = loadInvitation(invitationId);
        requireInvitee(invitation, userId);
        requirePending(invitation);

        invitation.reject();

        return HouseInvitationDto.Invitation.of(invitation);
    }

    private House loadHouse(Long houseId) {
        return houseRepository.findByIdWithMembers(houseId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.NOT_FOUND,
                        "House를 찾을 수 없습니다."
                ));
    }

    private HouseInvitation loadInvitation(Long invitationId) {
        return houseInvitationRepository.findByIdWithHouse(invitationId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.NOT_FOUND,
                        "House 초대를 찾을 수 없습니다."
                ));
    }

    private void requireAuthenticated(Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
    }

    /**
     * 초대 권한: 승인된 방장 또는 부방장.
     */
    private void requireManager(House house, Long requesterId) {
        HouseMember member = houseMemberRepository
                .findByHouseIdAndUserId(house.getId(), requesterId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.FORBIDDEN,
                        "House 관리 권한이 없습니다."
                ));

        if (member.getStatus() != JoinStatus.APPROVED
                || !member.getRole().canManage()) {
            throw new BusinessException(
                    ErrorCode.FORBIDDEN,
                    "House 관리 권한이 없습니다."
            );
        }
    }

    private void requireInvitee(
            HouseInvitation invitation,
            Long userId
    ) {
        if (!invitation.getInvitedUserId().equals(userId)) {
            throw new BusinessException(
                    ErrorCode.FORBIDDEN,
                    "이 초대를 처리할 권한이 없습니다."
            );
        }
    }

    private void requirePending(HouseInvitation invitation) {
        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "이미 처리된 House 초대입니다."
            );
        }
    }
}
