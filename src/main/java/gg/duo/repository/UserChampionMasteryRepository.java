package gg.duo.repository;

import gg.duo.entity.User;
import gg.duo.entity.UserChampionMastery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface UserChampionMasteryRepository extends JpaRepository<UserChampionMastery, Long> {

    List<UserChampionMastery> findByUserOrderByRankingAsc(User user);

    void deleteByUser(User user);
}
