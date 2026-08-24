package com.example.gamehouse.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HouseMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "house_id")
    private House house;

    private Long userId;

    @Enumerated(EnumType.STRING)
    private MemberRole role;

    @Enumerated(EnumType.STRING)
    private JoinStatus status;

    @Builder
    public HouseMember(House house, Long userId, MemberRole role, JoinStatus status) {
        this.house = house;
        this.userId = userId;
        this.role = role;
        this.status = status;
    }

    public void approve() {
        this.status = JoinStatus.APPROVED;
    }
}
