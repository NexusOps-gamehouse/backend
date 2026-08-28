package gg.duo.crew.domain.house;

import gg.duo.common.exception.BusinessException;
import gg.duo.common.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "houses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class House {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private HouseType type;

    @Column(name = "leader_id", nullable = false)
    private Long leaderId;

    @Column(nullable = false)
    private Integer maxMembers;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    // XP 및 HC 필드 추가 (기본값 0L)
    @Column(nullable = false)
    private Long xp = 0L;

    @Column(nullable = false)
    private Long hc = 0L;

    @OneToMany(mappedBy = "house", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<HouseMember> members = new ArrayList<>();

    @Builder
    public House(String name, String description, HouseType type, Long leaderId, Integer maxMembers) {
        this.name = name;
        this.description = description;
        this.type = type != null ? type : HouseType.PUBLIC;
        this.leaderId = leaderId;
        this.maxMembers = (maxMembers == null || maxMembers <= 0) ? 20 : maxMembers;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    // 보상 적립 메서드 추가
    public void addReward(Long rewardXp, Long rewardHc) {
        if (rewardXp != null) {
            this.xp += rewardXp;
        }
        if (rewardHc != null) {
            this.hc += rewardHc;
        }
    }

    public void addMember(HouseMember member) {
        members.add(member);
        member.assignHouse(this);
    }

    public void removeMember(HouseMember member) {
        members.remove(member);
        member.assignHouse(null);
    }

    public void update(String name, String description, HouseType type, Integer maxMembers) {
        if (name != null && !name.isBlank()) this.name = name;
        if (description != null) this.description = description;
        if (type != null) this.type = type;
        if (maxMembers != null && maxMembers > 0) {
            if (maxMembers < approvedMemberCount()) {
                throw new BusinessException(ErrorCode.INVALID_INPUT,
                        "이미 가입한 인원보다 적은 정원으로는 바꿀 수 없습니다.");
            }
            this.maxMembers = maxMembers;
        }
    }

    public long approvedMemberCount() {
        return members.stream().filter(m -> m.getStatus() == JoinStatus.APPROVED).count();
    }

    public boolean isFull() {
        return approvedMemberCount() >= maxMembers;
    }

    public JoinStatus initialJoinStatus() {
        return type == HouseType.PUBLIC ? JoinStatus.APPROVED : JoinStatus.PENDING;
    }
}