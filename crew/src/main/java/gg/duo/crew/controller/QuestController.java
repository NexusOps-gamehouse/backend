package gg.duo.crew.controller;

import gg.duo.crew.domain.HouseQuest;
import gg.duo.crew.repository.HouseQuestRepository;
import gg.duo.crew.service.QuestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/houses/{houseId}/quests")
@RequiredArgsConstructor
public class QuestController {

    private final QuestService questService;
    private final HouseQuestRepository questRepository;

    @GetMapping
    public ResponseEntity<List<HouseQuest>> getWeeklyQuests(@PathVariable Long houseId) {
        List<HouseQuest> quests = questRepository.findByHouseIdAndWeekStartDate(
                houseId, questService.getStartOfCurrentWeek()
        );
        return ResponseEntity.ok(quests);
    }

    @PostMapping("/{questId}/claim")
    public ResponseEntity<Void> claimReward(@PathVariable Long houseId, @PathVariable Long questId) {
        questService.claimQuestReward(houseId, questId);
        return ResponseEntity.ok().build();
    }
}