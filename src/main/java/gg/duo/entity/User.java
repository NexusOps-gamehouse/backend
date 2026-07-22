package gg.duo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;

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
    private String gender;
    private String ageRange;
    private String game;
    private String playStyle;
    private String position;
    private Boolean mic;
    private String playTimes;
    private String gameModes;

    // 라이엇 API 연동 정보
    private String tier;
    private String rank;
    private Integer leaguePoints;

    @Column(unique = true)
    private String puuid;        // Riot 계정 고유 ID (UNIQUE)

    private String gameName;     // Riot 닉네임
    private String tagLine;      // Riot 태그
    private Integer profileIconId;
    private Long summonerLevel;

    private Instant riotSyncedAt; // Riot 정보 마지막 동기화 시간
    private Instant lastActiveAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    /**
     * 라이엇 프로필 정보 업데이트
     */
    public void updateRiotInfo(String puuid, String gameName, String tagLine,
                               Integer profileIconId, Long summonerLevel,
                               String tier, String rank, Integer leaguePoints) {
        this.puuid = puuid;
        this.gameName = gameName;
        this.tagLine = tagLine;
        this.profileIconId = profileIconId;
        this.summonerLevel = summonerLevel;
        this.tier = tier;
        this.rank = rank;
        this.leaguePoints = leaguePoints;
        this.riotSyncedAt = Instant.now();
    }
}