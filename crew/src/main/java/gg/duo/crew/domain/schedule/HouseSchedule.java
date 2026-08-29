package gg.duo.crew.domain.schedule;

import gg.duo.common.exception.BusinessException;
import gg.duo.common.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "house_schedules")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HouseSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "house_id", nullable = false)
    private Long houseId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private LocalDateTime scheduledAt;

    @Column(nullable = false)
    private Integer maxParticipants;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "house_schedule_participants",
            joinColumns = @JoinColumn(name = "schedule_id"))
    @Column(name = "user_id")
    private Set<Long> participantUserIds = new HashSet<>();

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Builder
    public HouseSchedule(Long houseId, String title, LocalDateTime scheduledAt, Integer maxParticipants) {
        this.houseId = houseId;
        this.title = title;
        this.scheduledAt = scheduledAt;
        // 정원을 안 주면 5인(팀 게임 한 판) 기준.
        // 예전에는 null 이 그대로 들어가 참가 시점의 size() >= maxParticipants 에서
        // NullPointerException 이 났다 — 일정은 만들어지는데 아무도 참가할 수 없었다.
        this.maxParticipants = (maxParticipants == null || maxParticipants <= 0) ? 5 : maxParticipants;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public void addParticipant(Long userId) {
        if (participantUserIds.contains(userId)) {
            throw new BusinessException(ErrorCode.CONFLICT, "이미 참가한 일정입니다.");
        }
        if (participantUserIds.size() >= maxParticipants) {
            throw new BusinessException(ErrorCode.CONFLICT, "정원이 초과되었습니다.");
        }
        participantUserIds.add(userId);
    }

    public void removeParticipant(Long userId) {
        participantUserIds.remove(userId);
    }

    public void update(String title, LocalDateTime scheduledAt, Integer maxParticipants) {
        int nextMaxParticipants = (maxParticipants == null || maxParticipants <= 0)
                ? this.maxParticipants : maxParticipants;
        if (nextMaxParticipants < participantUserIds.size()) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "현재 참가자 수보다 작은 정원으로 변경할 수 없습니다.");
        }
        this.title = title;
        this.scheduledAt = scheduledAt;
        this.maxParticipants = nextMaxParticipants;
    }
}
