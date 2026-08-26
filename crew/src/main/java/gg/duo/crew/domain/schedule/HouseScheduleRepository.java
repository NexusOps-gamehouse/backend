package gg.duo.crew.domain.schedule;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HouseScheduleRepository extends JpaRepository<HouseSchedule, Long> {

    List<HouseSchedule> findByHouseIdOrderByScheduledAtAsc(Long houseId);
}
