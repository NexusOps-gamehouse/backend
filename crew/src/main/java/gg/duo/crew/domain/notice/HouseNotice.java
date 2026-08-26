package gg.duo.crew.domain.notice;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "house_notices")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HouseNotice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * House 를 @ManyToOne 으로 걸지 않은 이유.
     *
     * 공지는 House 를 통해서만 조회한다(houseId 로 직접 where). 연관을 걸면
     * 목록 조회마다 House 를 함께 끌고 오게 되고, 삭제 권한 검사에서 다시
     * 멤버 컬렉션까지 초기화된다. 같은 서비스 안이라 FK 를 걸 수는 있지만
     * 얻는 게 없어 ID 참조로 둔다.
     */
    @Column(name = "house_id", nullable = false)
    private Long houseId;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "text")
    private String content;

    @Column(name = "is_pinned", nullable = false)
    private Boolean isPinned;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Builder
    public HouseNotice(Long houseId, Long authorId, String title, String content, Boolean isPinned) {
        this.houseId = houseId;
        this.authorId = authorId;
        this.title = title;
        this.content = content;
        this.isPinned = isPinned != null ? isPinned : false;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public void pin(boolean pinned) {
        this.isPinned = pinned;
    }
}
