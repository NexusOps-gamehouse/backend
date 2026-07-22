package gg.duo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true)
    private String nickname;

    private String profileImageUrl;

    // 설문 정보
    private String gender;     // 남 / 여 / 비공개
    private String ageRange;   // 10대 / 20대 / 30대 이상 / 비공개
    private String game;       // 리그오브레전드 / 발로란트 / 기타
    private String playStyle;  // 빡겜 / 즐겜
    private String position;   // 탑 / 정글 / 미드 / 원딜 / 서폿
    private boolean mic;
    private String playTimes;    // 주 플레이 시간대, 콤마 구분 (예: "저녁,새벽")
    private String gameModes;    // 주 게임 모드, 콤마 구분 (예: "랭크,칼바람")
    private String riotNickname; // 롤 인게임 닉네임 (예: "Hide on bush#KR1")

    private Instant lastActiveAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    // ================= [라이엇 API 정보] =================
    private String puuid;
    private String gameName;
    private String tagLine;
    private Integer profileIconId;
    private Long summonerLevel;
    private String tier;         // 티어 (랭크 티어)
    private String rank;         // 세부 랭크 (I, II, III, IV 등)
    private Integer leaguePoints;
    private Integer wins;
    private Integer losses;

    /**
     * 라이엇 프로필 정보 업데이트 메서드
     */
    public void updateRiotInfo(String puuid, String gameName, String tagLine,
                               Integer profileIconId, Long summonerLevel,
                               String tier, String rank, Integer leaguePoints,
                               Integer wins, Integer losses) {
        this.puuid = puuid;
        this.gameName = gameName;
        this.tagLine = tagLine;
        this.profileIconId = profileIconId;
        this.summonerLevel = summonerLevel;
        this.tier = tier;
        this.rank = rank;
        this.leaguePoints = leaguePoints;
        this.wins = wins;
        this.losses = losses;
    }
}