package com.example.gamehouse.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HouseNotice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long houseId;
    private Long authorId;
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    private Boolean isPinned;

    @Builder
    public HouseNotice(Long houseId, Long authorId, String title, String content, Boolean isPinned) {
        this.houseId = houseId;
        this.authorId = authorId;
        this.title = title;
        this.content = content;
        this.isPinned = isPinned != null ? isPinned : false;
    }
}
