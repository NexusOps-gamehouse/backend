package gg.duo.crew.domain.gamematch;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * "누가 누구와 같이 게임했는가" 기록.
 *
 * 원본은 match 서비스가 소유한다. 여기 있는 것은 House 추천에 쓰려고 받아둔
 * 사본이다(MatchFoundConsumer 가 MatchFoundEvent 를 받아 채운다).
 * 같이 자주 하는 사람을 찾는 쿼리는 자기 스키마 안에서 끝나야 한다 —
 * duo_crew 계정은 match_svc 를 읽을 권한이 없다.
 *
 * ⚠️ match 서비스가 MatchFoundEvent 를 아직 발행하지 않으면 이 테이블은 비어
 *    있고 추천 API 는 빈 배열을 돌려준다. 에러가 아니라 데이터가 없는 것이다.
 */
@Entity
@Table(name = "game_matches",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_game_match_user", columnNames = {"match_id", "user_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GameMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "match_id", nullable = false)
    private Long matchId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "game_code")
    private String gameCode;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private GameMatch(Long matchId, Long userId, String gameCode) {
        this.matchId = matchId;
        this.userId = userId;
        this.gameCode = gameCode;
    }

    public static GameMatch of(Long matchId, Long userId, String gameCode) {
        return new GameMatch(matchId, userId, gameCode);
    }

    /**
     * createdAt 을 여기서 채운다.
     *
     * 원본 코드에는 이 필드를 채우는 곳이 아무 데도 없었다. null 이면
     * findFrequentPlaymates 의 createdAt >= :since 비교가 항상 거짓이라
     * 추천이 조용히 빈 결과만 돌려준다.
     */
    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
