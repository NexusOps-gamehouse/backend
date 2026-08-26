package gg.duo.crew.domain.house;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HouseMemberRepository extends JpaRepository<HouseMember, Long> {

    // houseId 는 house.id 로 해석된다(연관 프로퍼티 경로).
    Optional<HouseMember> findByHouseIdAndUserId(Long houseId, Long userId);

    List<HouseMember> findByHouseIdAndStatus(Long houseId, JoinStatus status);

    List<HouseMember> findByUserIdAndStatus(Long userId, JoinStatus status);

    boolean existsByHouseIdAndUserId(Long houseId, Long userId);
}
