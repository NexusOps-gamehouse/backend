package gg.duo.crew.domain.ranking;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "house_ranking_snapshots",
        uniqueConstraints = @UniqueConstraint(name = "uk_house_ranking_snapshot_week_house",
                columnNames = {"week_id", "house_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HouseRankingSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "week_id", nullable = false, length = 16)
    private String weekId;

    @Column(name = "house_id", nullable = false)
    private Long houseId;

    @Column(name = "baseline_rank", nullable = false)
    private Integer baselineRank;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "snapshot_complete", nullable = false)
    private boolean snapshotComplete;

    public HouseRankingSnapshot(String weekId, Long houseId, int baselineRank) {
        this.weekId = weekId;
        this.houseId = houseId;
        this.baselineRank = baselineRank;
        this.snapshotComplete = true;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
