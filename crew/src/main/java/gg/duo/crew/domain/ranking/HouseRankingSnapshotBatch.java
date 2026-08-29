package gg.duo.crew.domain.ranking;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** House가 없었던 주차도 최초 조회 시점을 기억하는 snapshot marker. */
@Entity
@Table(name = "house_ranking_snapshot_batches")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HouseRankingSnapshotBatch {

    @Id
    @Column(name = "week_id", length = 16)
    private String weekId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "snapshot_complete", nullable = false)
    private boolean snapshotComplete;

    public HouseRankingSnapshotBatch(String weekId) {
        this.weekId = weekId;
        this.snapshotComplete = false;
    }

    public void complete() {
        this.snapshotComplete = true;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
