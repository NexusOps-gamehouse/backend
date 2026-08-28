package gg.duo.crew.controller;

import gg.duo.crew.dto.HouseQuestResponseDto;
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

    @GetMapping
    public ResponseEntity<List<HouseQuestResponseDto>> getWeeklyQuests(@PathVariable Long houseId) {
        List<HouseQuestResponseDto> response = questService.getWeeklyQuests(houseId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{questId}/claim")
    public ResponseEntity<Void> claimReward(@PathVariable Long houseId, @PathVariable Long questId) {
        questService.claimQuestReward(houseId, questId);
        return ResponseEntity.ok().build();
    }
}