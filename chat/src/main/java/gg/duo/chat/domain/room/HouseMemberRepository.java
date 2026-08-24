package gg.duo.chat.domain.room;

import gg.duo.crew.entity.HouseMember; // domain 제거
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface HouseMemberRepository extends JpaRepository<HouseMember, Long> {
    Optional<HouseMember> findByHouseIdAndUserId(Long houseId, Long userId);
}