package gg.duo.crew.dto;

import gg.duo.crew.domain.house.House;
import gg.duo.crew.domain.house.HouseMember;
import gg.duo.crew.domain.house.HouseType;
import gg.duo.crew.domain.house.JoinStatus;
import gg.duo.crew.domain.house.MemberRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

/** House 관련 요청/응답 계약. */
public final class HouseDto {

    private HouseDto() {}

    /** 생성 · 수정 요청. */
    public record WriteRequest(
            @NotBlank(message = "House 이름은 필수입니다.") String name,
            String description,
            HouseType type,
            @Positive(message = "정원은 1 이상이어야 합니다.") Integer maxMembers) {}

    /** 목록용. 멤버 배열까지 내리면 목록 한 번에 응답이 커진다 — 인원 수만 준다. */
    public record Summary(
            Long id,
            String name,
            String description,
            HouseType type,
            Long leaderId,
            int maxMembers,
            long memberCount,
            Instant createdAt,
            MemberRole myRole,
            JoinStatus myStatus) {

        public static Summary of(House house, HouseMember me) {
            return new Summary(
                    house.getId(), house.getName(), house.getDescription(), house.getType(),
                    house.getLeaderId(), house.getMaxMembers(), house.approvedMemberCount(),
                    house.getCreatedAt(),
                    me == null ? null : me.getRole(),
                    me == null ? null : me.getStatus());
        }
    }

    /** 상세용. 승인된 멤버만 내린다 — 대기자 목록은 관리 권한이 있어야 볼 수 있다. */
    public record Detail(
            Long id,
            String name,
            String description,
            HouseType type,
            Long leaderId,
            int maxMembers,
            Instant createdAt,
            List<Member> members,
            MemberRole myRole,
            JoinStatus myStatus,
            long pendingCount) {

        public static Detail of(House house, HouseMember me) {
            List<Member> approved = house.getMembers().stream()
                    .filter(m -> m.getStatus() == JoinStatus.APPROVED)
                    .sorted(Comparator.comparing(HouseMember::getRole))
                    .map(Member::of)
                    .toList();
            long pending = house.getMembers().stream()
                    .filter(m -> m.getStatus() == JoinStatus.PENDING)
                    .count();
            return new Detail(
                    house.getId(), house.getName(), house.getDescription(), house.getType(),
                    house.getLeaderId(), house.getMaxMembers(), house.getCreatedAt(),
                    approved,
                    me == null ? null : me.getRole(),
                    me == null ? null : me.getStatus(),
                    pending);
        }
    }

    public record Member(Long memberId, Long userId, MemberRole role,
                         JoinStatus status, Instant joinedAt, Instant requestedAt) {

        public static Member of(HouseMember m) {
            return new Member(m.getId(), m.getUserId(), m.getRole(), m.getStatus(),
                    m.getJoinedAt(), m.getRequestedAt());
        }
    }

    /** 역할 변경 요청. */
    public record RoleRequest(MemberRole role) {}
}
