package com.example.gamehouse.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HouseSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long houseId;
    private String title;
    private LocalDateTime scheduledAt;
    private Integer maxParticipants;

    @ElementCollection
    @CollectionTable(name = "schedule_participants", joinColumns = @JoinColumn(name = "schedule_id"))
    private Set<Long> participantUserIds = new HashSet<>();

    @Builder
    public HouseSchedule(Long houseId, String title, LocalDateTime scheduledAt, Integer maxParticipants) {
        this.houseId = houseId;
        this.title = title;
        this.scheduledAt = scheduledAt;
        this.maxParticipants = maxParticipants;
    }

    public void addParticipant(Long userId) {
        if (participantUserIds.size() >= maxParticipants) {
            throw new IllegalStateException("정원이 초과되었습니다.");
        }
        participantUserIds.add(userId);
    }
}