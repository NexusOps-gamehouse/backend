package gg.duo.crew.dto;

import java.time.Instant;
import java.util.List;

public final class HouseRankingDto {

    private HouseRankingDto() {
    }

    public record RankChange(String type, Integer amount) {
    }

    public record Item(
            int rank,
            Long houseId,
            String name,
            String representativeGame,
            Long xp,
            Long currentMembers,
            Integer maxMembers,
            RankChange rankChange) {
    }

    public record Response(
            List<Item> items,
            List<Item> topHouses,
            long totalElements,
            long totalPages,
            int page,
            int size,
            String weekId,
            Instant updatedAt) {
    }
}
