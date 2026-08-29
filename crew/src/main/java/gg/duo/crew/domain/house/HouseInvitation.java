package gg.duo.crew.domain.house;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(
        name = "house_invitations",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_house_invitation",
                columnNames = {"house_id", "invited_user_id"}
        ),
        indexes = {
                @Index(
                        name = "idx_house_invitation_user_status",
                        columnList = "invited_user_id,status"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HouseInvitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "house_id", nullable = false)
    private House house;

    @Column(name = "invited_user_id", nullable = false)
    private Long invitedUserId;

    @Column(name = "invited_by_user_id", nullable = false)
    private Long invitedByUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private InvitationStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "responded_at")
    private Instant respondedAt;

    @Builder
    public HouseInvitation(
            House house,
            Long invitedUserId,
            Long invitedByUserId,
            InvitationStatus status
    ) {
        this.house = house;
        this.invitedUserId = invitedUserId;
        this.invitedByUserId = invitedByUserId;
        this.status = status != null ? status : InvitationStatus.PENDING;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (status == null) {
            status = InvitationStatus.PENDING;
        }
    }

    /**
     * 같은 House에서 과거에 거절/수락된 초대를 다시 보낼 때
     * unique constraint 때문에 새 row를 만들지 않고 기존 row를 재사용한다.
     */
    public void reopen(Long inviterUserId) {
        this.invitedByUserId = inviterUserId;
        this.status = InvitationStatus.PENDING;
        this.createdAt = Instant.now();
        this.respondedAt = null;
    }

    public void accept() {
        this.status = InvitationStatus.ACCEPTED;
        this.respondedAt = Instant.now();
    }

    public void reject() {
        this.status = InvitationStatus.REJECTED;
        this.respondedAt = Instant.now();
    }
}
