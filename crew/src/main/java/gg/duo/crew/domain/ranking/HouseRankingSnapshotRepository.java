package gg.duo.crew.domain.ranking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface HouseRankingSnapshotRepository extends JpaRepository<HouseRankingSnapshot, Long> {

    List<HouseRankingSnapshot> findByWeekId(String weekId);

    /** 같은 주차의 최초 snapshot 생성을 직렬화한다. PostgreSQL transaction advisory lock을 사용한다. */
    @Query(value = "SELECT pg_advisory_xact_lock(918273645)", nativeQuery = true)
    void lockSnapshotCreation();
}
