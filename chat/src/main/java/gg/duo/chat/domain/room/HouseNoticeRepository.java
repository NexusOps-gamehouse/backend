package gg.duo.chat.domain.room;

import gg.duo.crew.entity.HouseNotice; // domain 제거
import org.springframework.data.jpa.repository.JpaRepository;

public interface HouseNoticeRepository extends JpaRepository<HouseNotice, Long> {
}