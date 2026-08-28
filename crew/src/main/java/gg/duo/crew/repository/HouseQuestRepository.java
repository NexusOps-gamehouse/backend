package gg.duo.crew.repository;

import gg.duo.crew.domain.HouseQuest;
import gg.duo.crew.domain.QuestType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface HouseQuestRepository extends JpaRepository<HouseQuest, Long> {

    List<HouseQuest> findByHouseIdAndWeekStartDateBetween(
            Long houseId, LocalDateTime start, LocalDateTime end
    );

    Optional<HouseQuest> findByHouseIdAndQuestTypeAndWeekStartDateBetween(
            Long houseId, QuestType questType, LocalDateTime start, LocalDateTime end
    );
}