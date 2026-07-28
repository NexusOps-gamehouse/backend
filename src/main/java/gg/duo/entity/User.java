package gg.duo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
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

    // 아이디/비밀번호 찾기용 개인정보
    private String name;
    private String phone;

    // 설문 정보
    private String gender; // 남 / 여 / 비공개
    private String ageRange; // 10대 / 20대 / 30대 이상 / 비공개
    private String game; // 리그오브레전드 / 발로란트 / 기타
    private String playStyle; // 빡겜 / 즐겜
    private String position; // 탑 / 정글 / 미드 / 원딜 / 서폿
    private boolean mic;
    private String tier; // 티어 (목록 선택)
    private String playTimes; // 주 플레이 시간대, 콤마 구분 (예: "저녁,새벽")
    private String gameModes; // 주 게임 모드, 콤마 구분 (예: "랭크,칼바람")
    private String riotNickname; // 롤 인게임 닉네임 (예: "Hide on bush#KR1")

    // 라이엇 연동 정보
    private String puuid;
    private String gameName;
    private String tagLine;

    private Instant lastActiveAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}