package gg.duo.crew.domain.house;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HouseMemberRepository extends JpaRepository<HouseMember, Long> {

    // houseId 는 house.id 로 해석된다(연관 프로퍼티 경로).
    Optional<HouseMember> findByHouseIdAndUserId(Long houseId, Long userId);

    List<HouseMember> findByHouseIdAndStatus(Long houseId, JoinStatus status);

    List<HouseMember> findByUserIdAndStatus(Long userId, JoinStatus status);

    @Query("SELECT m.house.id FROM HouseMember m WHERE m.userId = :userId AND m.status = :status")
    List<Long> findHouseIdsByUserIdAndStatus(@Param("userId") Long userId, @Param("status") JoinStatus status);

    boolean existsByHouseIdAndUserId(Long houseId, Long userId);
}
