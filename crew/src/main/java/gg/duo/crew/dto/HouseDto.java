package gg.duo.crew.dto;

import gg.duo.crew.domain.house.House;
import gg.duo.crew.domain.house.HouseActivityType;
import gg.duo.crew.domain.house.HouseMember;
import gg.duo.crew.domain.house.HouseType;
import gg.duo.crew.domain.house.JoinStatus;
import gg.duo.crew.domain.house.MemberRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/** House 관련 요청/응답 계약. */
public final class HouseDto {

    private HouseDto() {}

    /** 생성 · 수정 요청. */
    public record WriteRequest(
            @NotBlank(message = "House 이름은 필수입니다.") String name,
            String description,
            HouseType type,
            @jakarta.validation.constraints.NotNull(message = "House 활동 유형은 필수입니다.")
            HouseActivityType activityType,
            @NotBlank(message = "대표 게임은 필수입니다.")
            @jakarta.validation.constraints.Size(
                    max = 100,
                    message = "대표 게임은 100자 이하여야 합니다."
            )
            String representativeGame,
            @Positive(message = "정원은 1 이상이어야 합니다.") Integer maxMembers
    ) {}

    /** 목록용. */
    public record Summary(
            Long id,
            String name,
            String description,
            HouseType type,
            HouseActivityType activityType,
            String representativeGame,
            Long leaderId,
            String leaderName,
            int maxMembers,
            long memberCount,
            Instant createdAt,
            MemberRole myRole,
            JoinStatus myStatus
    ) {

        public static Summary of(
                House house,
                HouseMember me,
                String leaderName
        ) {
            return new Summary(
                    house.getId(),
                    house.getName(),
                    house.getDescription(),
                    house.getType(),
                    house.getActivityType(),
                    house.getRepresentativeGame(),
                    house.getLeaderId(),
                    leaderName,
                    house.getMaxMembers(),
                    house.approvedMemberCount(),
                    house.getCreatedAt(),
                    me == null ? null : me.getRole(),
                    me == null ? null : me.getStatus()
            );
        }
    }

    /** 상세용. 승인된 멤버만 내린다. */
    public record Detail(
            Long id,
            String name,
            String description,
            HouseType type,
            HouseActivityType activityType,
            String representativeGame,
            Long leaderId,
            String leaderName,
            int maxMembers,
            Instant createdAt,
            List<Member> members,
            MemberRole myRole,
            JoinStatus myStatus,
            long pendingCount
    ) {

        public static Detail of(
                House house,
                HouseMember me,
                Function<Long, String> nicknameResolver
        ) {
            List<Member> approved = house.getMembers().stream()
                    .filter(m -> m.getStatus() == JoinStatus.APPROVED)
                    .sorted(Comparator.comparing(HouseMember::getRole))
                    .map(m -> Member.of(
                            m,
                            nicknameResolver.apply(m.getUserId())
                    ))
                    .toList();

            long pending = house.getMembers().stream()
                    .filter(m -> m.getStatus() == JoinStatus.PENDING)
                    .count();

            return new Detail(
                    house.getId(),
                    house.getName(),
                    house.getDescription(),
                    house.getType(),
                    house.getActivityType(),
                    house.getRepresentativeGame(),
                    house.getLeaderId(),
                    nicknameResolver.apply(house.getLeaderId()),
                    house.getMaxMembers(),
                    house.getCreatedAt(),
                    approved,
                    me == null ? null : me.getRole(),
                    me == null ? null : me.getStatus(),
                    pending
            );
        }
    }

    public record Member(
            Long memberId,
            Long userId,
            String nickname,
            MemberRole role,
            JoinStatus status,
            Instant joinedAt,
            Instant requestedAt
    ) {

        public static Member of(HouseMember member, String nickname) {
            return new Member(
                    member.getId(),
                    member.getUserId(),
                    nickname,
                    member.getRole(),
                    member.getStatus(),
                    member.getJoinedAt(),
                    member.getRequestedAt()
            );
        }
    }

    /** 역할 변경 요청. */
    public record RoleRequest(MemberRole role) {}
}
