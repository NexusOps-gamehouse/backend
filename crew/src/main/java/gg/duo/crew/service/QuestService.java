package gg.duo.crew.service;

import gg.duo.crew.domain.HouseQuest;
import gg.duo.crew.domain.QuestType;
import gg.duo.crew.domain.house.House;
import gg.duo.crew.domain.house.HouseRepository;
import gg.duo.crew.dto.HouseQuestResponseDto;
import gg.duo.crew.repository.HouseQuestRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
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

    // 엔티티 대신 DTO 리스트를 반환
    public List<HouseQuestResponseDto> getWeeklyQuests(Long houseId) {
        List<HouseQuest> quests = questRepository.findByHouseIdAndWeekStartDateBetween(
                houseId, getStartOfCurrentWeek(), getEndOfCurrentWeek()
        );

        return quests.stream()
                .map(HouseQuestResponseDto::from)
                .collect(Collectors.toList());
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

        House house = houseRepository.findById(houseId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 하우스입니다."));

        QuestType questType = quest.getQuestType();

        // 1. 실제 House에 XP 및 HC 적립
        if (questType != null) {
            house.addReward(questType.getRewardXp(), questType.getRewardHc());
        }

        // 2. 보상 수령 처리
        quest.claimReward();
    }
}