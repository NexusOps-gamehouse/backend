package gg.duo.crew.domain.house;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(
        name = "house_members",
        // 같은 House 에 같은 사람이 두 번 들어가지 못하게 DB 가 막는다.
        // 신청 버튼 연타(더블 클릭)로 PENDING 행이 두 개 생기던 자리다.
        uniqueConstraints = @UniqueConstraint(
                name = "uk_house_member", columnNames = {"house_id", "user_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HouseMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "house_id")
    private House house;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private MemberRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private JoinStatus status;

    @Column(nullable = false, updatable = false)
    private Instant requestedAt;

    private Instant joinedAt;

    @Builder
    public HouseMember(Long userId, MemberRole role, JoinStatus status) {
        this.userId = userId;
        this.role = role != null ? role : MemberRole.MEMBER;
        this.status = status != null ? status : JoinStatus.PENDING;
    }

    @PrePersist
    void onCreate() {
        this.requestedAt = Instant.now();
        if (this.status == JoinStatus.APPROVED && this.joinedAt == null) {
            this.joinedAt = this.requestedAt;
        }
    }

    void assignHouse(House house) {
        this.house = house;
    }

    public void approve() {
        this.status = JoinStatus.APPROVED;
        this.joinedAt = Instant.now();
    }

    public void reject() {
        this.status = JoinStatus.REJECTED;
    }

    /**
     * 거절당한 사람이 다시 신청한다.
     *
     * 행을 새로 만들지 않는 이유: (house_id, user_id) 유니크 제약에 걸린다.
     * 기존 행을 PENDING 으로 되돌려 재사용한다.
     */
    public void reapply() {
        this.status = JoinStatus.PENDING;
        this.joinedAt = null;
    }

    public void changeRole(MemberRole role) {
        this.role = role;
    }
}
