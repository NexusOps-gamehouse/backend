package gg.duo.crew.controller;

import gg.duo.crew.dto.HouseQuestResponseDto;
import gg.duo.crew.service.QuestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/houses/{houseId}/quests")
@RequiredArgsConstructor
public class QuestController {

    private final QuestService questService;

    private Long getUserId(Authentication auth) {
        return auth == null ? null : (Long) auth.getPrincipal();
    }

    @GetMapping
    public ResponseEntity<List<HouseQuestResponseDto>> getWeeklyQuests(
            @PathVariable Long houseId,
            Authentication auth) {
        return ResponseEntity.ok(questService.getWeeklyQuests(houseId, getUserId(auth)));
    }

    @PostMapping("/{questId}/claim")
    public ResponseEntity<Void> claimReward(
            @PathVariable Long houseId,
            @PathVariable Long questId,
            Authentication auth) {
        questService.claimQuestReward(houseId, questId, getUserId(auth));
        return ResponseEntity.ok().build();
    }
}