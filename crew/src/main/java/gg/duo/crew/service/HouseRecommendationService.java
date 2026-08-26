package gg.duo.crew.service;

import gg.duo.crew.domain.gamematch.GameMatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 최근 자주 같이 게임한 사람 추천.
 *
 * ⚠️ game_matches 는 MatchFoundConsumer 가 채운다. match 서비스가 아직
 *    MatchFoundEvent 를 발행하지 않으면 결과는 항상 빈 목록이다.
 */
@Service
@RequiredArgsConstructor
public class HouseRecommendationService {

    private static final int WINDOW_DAYS = 7;
    private static final long MIN_TOGETHER = 3L;

    private final GameMatchRepository gameMatchRepository;

    @Transactional(readOnly = true)
    public List<Long> recommendedPlaymates(Long userId) {
        LocalDateTime since = LocalDateTime.now().minusDays(WINDOW_DAYS);
        return gameMatchRepository.findFrequentPlaymates(userId, since, MIN_TOGETHER);
    }
}
