package gg.duo.crew.domain.notice;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HouseNoticeRepository extends JpaRepository<HouseNotice, Long> {

    /** 고정 공지 먼저, 그 다음 최신순. */
    List<HouseNotice> findByHouseIdOrderByIsPinnedDescIdDesc(Long houseId);
}
