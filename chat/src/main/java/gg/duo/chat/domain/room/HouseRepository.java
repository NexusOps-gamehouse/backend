package gg.duo.chat.domain.room;

import gg.duo.crew.entity.House; // domain 제거
import org.springframework.data.jpa.repository.JpaRepository;

public interface HouseRepository extends JpaRepository<House, Long> {
}