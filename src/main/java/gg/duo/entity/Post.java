package gg.duo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "posts")
@Getter
@Setter
@NoArgsConstructor
public class Post {

    public enum Status { RECRUITING, CLOSED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id")
    private User author;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    // 모집 조건
    private String game;        // 어떤 게임
    private String gameMode;    // 게임 모드 (일반/랭크/칼바람 등)
    private String playTime;    // 같이 할 시간 (자유 입력, 예: "오늘 21시")
    private boolean micRequired;
    private String positions;   // 찾는 포지션, 콤마 구분 (예: "정글,서폿")

    @Column(nullable = false)
    private int targetMembers = 2; // 희망 파티원 수 (방장 포함)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.RECRUITING;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
