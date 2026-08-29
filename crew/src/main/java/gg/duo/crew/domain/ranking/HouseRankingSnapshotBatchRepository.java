package gg.duo.crew.domain.ranking;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HouseRankingSnapshotBatchRepository extends JpaRepository<HouseRankingSnapshotBatch, String> {

    Optional<HouseRankingSnapshotBatch> findByWeekId(String weekId);
}
