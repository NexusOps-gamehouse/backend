package gg.duo.crew.util;

import gg.duo.crew.dto.HouseRankingCandidate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class HouseRankingCalculator {

    private HouseRankingCalculator() {
    }

    public record RankedHouse(HouseRankingCandidate candidate, long safeXp, int rank) {
    }

    public static List<RankedHouse> rank(List<HouseRankingCandidate> candidates) {
        List<HouseRankingCandidate> sorted = new ArrayList<>(candidates == null ? List.of() : candidates);
        sorted.sort(Comparator
                .comparingLong((HouseRankingCandidate candidate) -> safeXp(candidate.xp())).reversed()
                .thenComparing(HouseRankingCandidate::createdAt,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(candidate -> candidate.name() == null ? "" : candidate.name())
                .thenComparing(candidate -> candidate.houseId() == null ? Long.MAX_VALUE : candidate.houseId()));

        Long previousXp = null;
        int previousRank = 0;
        List<RankedHouse> result = new ArrayList<>(sorted.size());
        for (int index = 0; index < sorted.size(); index++) {
            HouseRankingCandidate candidate = sorted.get(index);
            long xp = safeXp(candidate.xp());
            int rank = previousXp != null && xp == previousXp ? previousRank : index + 1;
            result.add(new RankedHouse(candidate, xp, rank));
            previousXp = xp;
            previousRank = rank;
        }
        return result;
    }

    public static long safeXp(Long xp) {
        return xp == null || xp < 0 ? 0 : xp;
    }
}
