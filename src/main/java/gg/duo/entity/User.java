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

    // 본인 확인용 필드
    private String name;
    private String phone;

    // 설문 정보
    private String gender;
    private String ageRange;
    private String game;
    private String playStyle;
    private String position;
    private boolean mic;
    private String tier;
    private String playTimes;
    private String gameModes;
    private String riotNickname;

    // 라이엇 연동 정보
    private String puuid;
    private String gameName;
    private String tagLine;

    private Instant lastActiveAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}