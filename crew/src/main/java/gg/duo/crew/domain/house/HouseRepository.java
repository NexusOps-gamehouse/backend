package gg.duo.crew.domain.house;

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
     */
    @Query("SELECT DISTINCT h FROM House h LEFT JOIN FETCH h.members ORDER BY h.id DESC")
    List<House> findAllWithMembers();

    @Query("SELECT h FROM House h LEFT JOIN FETCH h.members WHERE h.id = :id")
    Optional<House> findByIdWithMembers(@Param("id") Long id);

    boolean existsByName(String name);

    // 👈 동시성 제어를 위한 비관적 락(Pessimistic Lock) 메서드 추가
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT h FROM House h WHERE h.id = :id")
    Optional<House> findByIdWithLock(@Param("id") Long id);
}