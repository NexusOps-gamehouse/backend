package gg.duo.crew.event.consumer;

import gg.duo.common.event.MatchFoundEvent;
import gg.duo.crew.domain.gamematch.GameMatch;
import gg.duo.crew.domain.gamematch.GameMatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 매칭이 성사됐다 → "누가 누구와 같이 했는지"를 crew_svc 안에 복제한다.
 *
 * 이 복제가 없으면 House 추천 쿼리가 match_svc 를 읽어야 하는데, duo_crew
 * 계정에는 그 권한이 없어 permission denied 로 막힌다. 지난 판 기록은 한 번
 * 정해지면 바뀌지 않으므로 복제해도 어긋나지 않는다.
 */
@Component
@RequiredArgsConstructor
public class MatchFoundConsumer {

    private final GameMatchRepository gameMatchRepository;

    @EventListener
    @Transactional
    public void on(MatchFoundEvent event) {
        if (event.memberIds() == null) return;
        for (Long userId : event.memberIds()) {
            // 브로커는 최소 1회 배달이다. 같은 이벤트가 두 번 와도 유니크 제약에
            // 걸려 트랜잭션 전체가 롤백되지 않도록 먼저 확인한다.
            if (gameMatchRepository.existsByMatchIdAndUserId(event.matchId(), userId)) continue;
            gameMatchRepository.save(GameMatch.of(event.matchId(), userId, event.gameCode()));
        }
    }
}
