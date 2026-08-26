package gg.duo.crew.repository;

import gg.duo.crew.domain.house.House; // house 패키지 경로 추가
import org.springframework.data.jpa.repository.JpaRepository;

public interface HouseRepository extends JpaRepository<House, Long> {
}