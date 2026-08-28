package gg.duo.crew.service;

import gg.duo.crew.domain.HouseQuest;
import gg.duo.crew.domain.QuestType;
import gg.duo.crew.domain.house.HouseRepository;
import gg.duo.crew.repository.HouseQuestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import jakarta.annotation.PostConstruct;

@Service
@RequiredArgsConstructor
public class QuestService {

    private final HouseQuestRepository questRepository;
    private final HouseRepository houseRepository;

    public LocalDateTime getStartOfCurrentWeek() {
        return LocalDateTime.now()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .withHour(0).withMinute(0).withSecond(0).withNano(0);
    }

    public LocalDateTime getEndOfCurrentWeek() {
        return LocalDateTime.now()
                .with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
                .withHour(23).withMinute(59).withSecond(59).withNano(999999999);
    }

    public List<HouseQuest> getWeeklyQuests(Long houseId) {
        return questRepository.findByHouseIdAndWeekStartDateBetween(
                houseId, getStartOfCurrentWeek(), getEndOfCurrentWeek()
        );
    }

    @PostConstruct
    @Scheduled(cron = "0 0 0 * * MON")
    @Transactional
    public void resetWeeklyQuests() {
        LocalDateTime currentWeekStart = getStartOfCurrentWeek();
        houseRepository.findAll().forEach(house -> {
            for (QuestType type : QuestType.values()) {
                questRepository.save(new HouseQuest(house.getId(), type, currentWeekStart));
            }
        });
    }

    @Transactional
    public void updateQuestProgress(Long houseId, QuestType type, int increment) {
        LocalDateTime weekStart = getStartOfCurrentWeek();
        LocalDateTime weekEnd = getEndOfCurrentWeek();

        HouseQuest quest = questRepository.findByHouseIdAndQuestTypeAndWeekStartDateBetween(houseId, type, weekStart, weekEnd)
                .orElseGet(() -> questRepository.save(new HouseQuest(houseId, type, weekStart)));

        quest.addProgress(increment);
    }

    @Transactional
    public void claimQuestReward(Long houseId, Long questId) {
        HouseQuest quest = questRepository.findById(questId)
                .orElseThrow(() -> new IllegalArgumentException("퀘스트가 존재하지 않습니다."));

        if (!quest.isCompleted() || quest.isRewardClaimed()) {
            throw new IllegalStateException("보상을 수령할 수 없는 상태입니다.");
        }

        quest.claimReward();

        List<HouseQuest> weeklyQuests = questRepository.findByHouseIdAndWeekStartDateBetween(
                houseId, getStartOfCurrentWeek(), getEndOfCurrentWeek()
        );
        boolean allCompleted = weeklyQuests.stream().allMatch(HouseQuest::isCompleted);

        houseRepository.findById(houseId).ifPresent(house -> {
            // 추후 구현
        });
    }
}