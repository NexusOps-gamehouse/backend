package gg.duo.crew.service;

import gg.duo.crew.domain.house.HouseActivityType;
import gg.duo.crew.domain.house.HouseMemberRepository;
import gg.duo.crew.domain.house.HouseRepository;
import gg.duo.crew.domain.house.HouseType;
import gg.duo.crew.domain.house.JoinStatus;
import gg.duo.crew.domain.ranking.HouseRankingSnapshot;
import gg.duo.crew.domain.ranking.HouseRankingSnapshotBatch;
import gg.duo.crew.domain.ranking.HouseRankingSnapshotBatchRepository;
import gg.duo.crew.domain.ranking.HouseRankingSnapshotRepository;
import gg.duo.crew.dto.HouseRankingCandidate;
import gg.duo.crew.dto.HouseRankingDto;
import gg.duo.crew.util.HouseRankingCalculator;
import gg.duo.crew.util.HouseWeekUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HouseRankingService {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final HouseRepository houseRepository;
    private final HouseMemberRepository houseMemberRepository;
    private final HouseRankingSnapshotRepository snapshotRepository;
    private final HouseRankingSnapshotBatchRepository snapshotBatchRepository;

    @Transactional
    public HouseRankingDto.Response getRankings(int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = normalizeSize(size);
        List<HouseRankingCalculator.RankedHouse> ranked = rankedHouses();
        String weekId = HouseWeekUtil.currentWeekId();
        Map<Long, Integer> baseline = ensureSnapshot(weekId, ranked);

        List<HouseRankingDto.Item> allItems = toItems(ranked, baseline);
        long offset = (long) safePage * safeSize;
        int from = (int) Math.min(offset, allItems.size());
        int to = Math.min(from + safeSize, allItems.size());
        long totalPages = allItems.isEmpty() ? 0 : (allItems.size() + safeSize - 1L) / safeSize;

        return new HouseRankingDto.Response(
                allItems.subList(from, to),
                allItems.subList(0, Math.min(10, allItems.size())),
                allItems.size(), totalPages, safePage, safeSize, weekId, Instant.now());
    }

    @Transactional
    public List<HouseRankingDto.Item> getMyRankings(Long userId) {
        if (userId == null) {
            return List.of();
        }
        List<HouseRankingCalculator.RankedHouse> ranked = rankedHouses();
        Map<Long, Integer> baseline = ensureSnapshot(HouseWeekUtil.currentWeekId(), ranked);
        Set<Long> myHouseIds = Set.copyOf(houseMemberRepository
                .findHouseIdsByUserIdAndStatus(userId, JoinStatus.APPROVED));
        return toItems(ranked, baseline).stream()
                .filter(item -> myHouseIds.contains(item.houseId()))
                .toList();
    }

    private List<HouseRankingCalculator.RankedHouse> rankedHouses() {
        List<HouseRankingCandidate> candidates = houseRepository.findEligibleRankingCandidates(
                HouseType.PUBLIC, HouseActivityType.COMPETITIVE, JoinStatus.APPROVED);
        return HouseRankingCalculator.rank(candidates);
    }

    private Map<Long, Integer> ensureSnapshot(String weekId,
                                               List<HouseRankingCalculator.RankedHouse> ranked) {
        snapshotRepository.lockSnapshotCreation();
        HouseRankingSnapshotBatch batch = snapshotBatchRepository.findByWeekId(weekId).orElse(null);
        List<HouseRankingSnapshot> snapshots = snapshotRepository.findByWeekId(weekId);
        if (batch == null) {
            batch = snapshotBatchRepository.save(new HouseRankingSnapshotBatch(weekId));
            snapshotRepository.saveAll(ranked.stream()
                    .map(house -> new HouseRankingSnapshot(weekId, house.candidate().houseId(), house.rank()))
                    .toList());
            batch.complete();
            snapshotBatchRepository.save(batch);
            return ranked.stream().collect(Collectors.toMap(
                    house -> house.candidate().houseId(), HouseRankingCalculator.RankedHouse::rank));
        }
        if (!batch.isSnapshotComplete()
                || snapshots.stream().anyMatch(snapshot -> !snapshot.isSnapshotComplete())) {
            throw new IllegalStateException("House 랭킹 snapshot이 완전히 생성되지 않았습니다.");
        }
        return snapshots.stream().collect(Collectors.toMap(
                HouseRankingSnapshot::getHouseId, HouseRankingSnapshot::getBaselineRank));
    }

    private List<HouseRankingDto.Item> toItems(List<HouseRankingCalculator.RankedHouse> ranked,
                                                Map<Long, Integer> baseline) {
        return ranked.stream().map(house -> {
            Long houseId = house.candidate().houseId();
            Integer previousRank = baseline.get(houseId);
            HouseRankingDto.RankChange change;
            if (previousRank == null) {
                change = new HouseRankingDto.RankChange("NEW", null);
            } else if (house.rank() < previousRank) {
                change = new HouseRankingDto.RankChange("UP", previousRank - house.rank());
            } else if (house.rank() > previousRank) {
                change = new HouseRankingDto.RankChange("DOWN", house.rank() - previousRank);
            } else {
                change = new HouseRankingDto.RankChange("SAME", 0);
            }
            HouseRankingCandidate candidate = house.candidate();
            return new HouseRankingDto.Item(house.rank(), houseId, candidate.name(),
                    candidate.representativeGame(), house.safeXp(),
                    candidate.currentMembers() == null ? 0L : Math.max(0L, candidate.currentMembers()),
                    candidate.maxMembers(), change);
        }).toList();
    }

    private int normalizeSize(int size) {
        if (size <= 0) return DEFAULT_SIZE;
        return Math.min(size, MAX_SIZE);
    }
}
