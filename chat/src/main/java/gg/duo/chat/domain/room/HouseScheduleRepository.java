package gg.duo.chat.domain.room;

import gg.duo.crew.entity.HouseSchedule; // domain 제거
import org.springframework.data.jpa.repository.JpaRepository;

public interface HouseScheduleRepository extends JpaRepository<HouseSchedule, Long> {
}