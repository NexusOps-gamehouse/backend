package gg.duo.crew.domain.house;

import gg.duo.crew.dto.HouseRankingCandidate;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface HouseRepository extends JpaRepository<House, Long> {

    /**
     * 목록 조회. members 를 함께 읽는다.
     *
     * fetch join 이 없으면 House 하나마다 members 를 다시 조회한다(N+1).
     * 목록 응답이 멤버 수를 보여주므로 어차피 전부 필요하다.
     * distinct 는 join 으로 늘어난 House 중복 행을 접는다.
     */
    @Query("SELECT DISTINCT h FROM House h LEFT JOIN FETCH h.members ORDER BY h.id DESC")
    List<House> findAllWithMembers();

    @Query("SELECT h FROM House h LEFT JOIN FETCH h.members WHERE h.id = :id")
    Optional<House> findByIdWithMembers(Long id);

    @Query("""
            SELECT new gg.duo.crew.dto.HouseRankingCandidate(
                h.id, h.name, h.createdAt, h.xp, h.representativeGame, h.maxMembers, COUNT(m.id))
            FROM House h
            LEFT JOIN h.members m ON m.status = :approvedStatus
            WHERE h.type = :houseType AND h.activityType = :activityType
            GROUP BY h.id, h.name, h.createdAt, h.xp, h.representativeGame, h.maxMembers
            ORDER BY h.xp DESC, h.createdAt ASC, h.name ASC, h.id ASC
            """)
    List<HouseRankingCandidate> findEligibleRankingCandidates(
            @Param("houseType") HouseType houseType,
            @Param("activityType") HouseActivityType activityType,
            @Param("approvedStatus") JoinStatus approvedStatus);

    boolean existsByName(String name);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT h FROM House h WHERE h.id = :id")
    Optional<House> findByIdWithLock(@Param("id") Long id);
}
