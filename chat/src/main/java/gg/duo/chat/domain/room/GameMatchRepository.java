package gg.duo.chat.domain.room;

import gg.duo.crew.entity.GameMatch; // domain 제거
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface GameMatchRepository extends JpaRepository<GameMatch, Long> {

    @Query("SELECT gm2.userId FROM GameMatch gm1 " +
            "JOIN GameMatch gm2 ON gm1.matchId = gm2.matchId " +
            "WHERE gm1.userId = :userId AND gm2.userId != :userId " +
            "AND gm1.createdAt >= :since " +
            "GROUP BY gm2.userId HAVING COUNT(gm2.userId) >= :minCount")
    List<Long> findFrequentPlaymates(@Param("userId") Long userId,
                                     @Param("since") LocalDateTime since,
                                     @Param("minCount") Long minCount);
}