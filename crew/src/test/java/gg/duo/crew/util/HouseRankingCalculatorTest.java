package gg.duo.crew.util;

import gg.duo.crew.dto.HouseRankingCandidate;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HouseRankingCalculatorTest {

    @Test
    void usesCompetitionRankingAndStableTieBreakers() {
        Instant createdAt = Instant.parse("2026-08-01T00:00:00Z");
        List<HouseRankingCalculator.RankedHouse> result = HouseRankingCalculator.rank(List.of(
                candidate(1L, "first", createdAt, 1000L),
                candidate(2L, "second", createdAt, 900L),
                candidate(3L, "third", createdAt.plusSeconds(1), 900L),
                candidate(4L, "fourth", createdAt, 800L)));

        assertThat(result).extracting(HouseRankingCalculator.RankedHouse::rank)
                .containsExactly(1, 2, 2, 4);
        assertThat(result).extracting(house -> house.candidate().houseId())
                .containsExactly(1L, 2L, 3L, 4L);
    }

    @Test
    void normalizesInvalidXpToZero() {
        Instant createdAt = Instant.parse("2026-08-01T00:00:00Z");
        List<HouseRankingCalculator.RankedHouse> result = HouseRankingCalculator.rank(List.of(
                candidate(1L, "negative", createdAt, -10L),
                candidate(2L, "valid", createdAt, 1L),
                candidate(3L, "null", createdAt, null)));

        assertThat(result).extracting(HouseRankingCalculator.RankedHouse::safeXp)
                .containsExactly(1L, 0L, 0L);
        assertThat(result).extracting(HouseRankingCalculator.RankedHouse::rank)
                .containsExactly(1, 2, 2);
    }

    private HouseRankingCandidate candidate(Long id, String name, Instant createdAt, Long xp) {
        return new HouseRankingCandidate(id, name, createdAt, xp, "게임", 20, 2L);
    }
}
