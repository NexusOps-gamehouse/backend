package gg.duo.crew.dto;

import java.time.Instant;

/** 랭킹 계산에 필요한 House와 승인 멤버 수의 조회 결과. */
public record HouseRankingCandidate(
        Long houseId,
        String name,
        Instant createdAt,
        Long xp,
        String representativeGame,
        Integer maxMembers,
        Long currentMembers) {
}
