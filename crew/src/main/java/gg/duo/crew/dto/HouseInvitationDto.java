package gg.duo.crew.dto;

import gg.duo.crew.domain.house.House;
import gg.duo.crew.domain.house.HouseActivityType;
import gg.duo.crew.domain.house.HouseInvitation;
import gg.duo.crew.domain.house.HouseType;
import gg.duo.crew.domain.house.InvitationStatus;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

public final class HouseInvitationDto {

    private HouseInvitationDto() {
    }

    /**
     * House 초대 요청.
     * 프론트에서 여러 친구를 동시에 선택할 수 있으므로 userIds 배열을 받는다.
     */
    public record InviteRequest(
            @NotEmpty(message = "초대할 사용자를 선택해주세요.")
            List<@NotNull Long> userIds
    ) {
    }

    public record HouseSummary(
            Long id,
            String name,
            HouseType type,
            HouseActivityType activityType,
            String representativeGame,
            int maxMembers,
            long currentMembers
    ) {
        public static HouseSummary of(House house) {
            return new HouseSummary(
                    house.getId(),
                    house.getName(),
                    house.getType(),
                    house.getActivityType(),
                    house.getRepresentativeGame(),
                    house.getMaxMembers(),
                    house.approvedMemberCount()
            );
        }
    }

    public record Invitation(
            Long id,
            HouseSummary house,
            Long invitedUserId,
            Long invitedByUserId,
            InvitationStatus status,
            Instant createdAt,
            Instant respondedAt
    ) {
        public static Invitation of(HouseInvitation invitation) {
            return new Invitation(
                    invitation.getId(),
                    HouseSummary.of(invitation.getHouse()),
                    invitation.getInvitedUserId(),
                    invitation.getInvitedByUserId(),
                    invitation.getStatus(),
                    invitation.getCreatedAt(),
                    invitation.getRespondedAt()
            );
        }
    }

    public record InviteResponse(
            int invitedCount,
            int skippedCount,
            List<Invitation> invitations
    ) {
    }
}
