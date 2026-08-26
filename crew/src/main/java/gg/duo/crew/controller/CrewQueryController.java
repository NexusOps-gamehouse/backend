package gg.duo.crew.controller;

import gg.duo.crew.dto.ChatMessageDto;
import gg.duo.crew.service.HouseChatService;
import gg.duo.crew.service.HouseRecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 화면이 처음 그릴 때 필요한 조회들 — 채팅 기록, 함께할 사람 추천. */
@RestController
@RequestMapping("/api/crew")
@RequiredArgsConstructor
public class CrewQueryController {

    private final HouseChatService houseChatService;
    private final HouseRecommendationService recommendationService;

    private Long userId(Authentication auth) {
        return auth == null ? null : (Long) auth.getPrincipal();
    }

    /** 채팅방에 들어갈 때 최근 대화를 먼저 채운다. 이후 메시지는 STOMP 로 온다. */
    @GetMapping("/houses/{houseId}/chat/messages")
    public List<ChatMessageDto> chatHistory(@PathVariable Long houseId, Authentication auth) {
        return houseChatService.recent(houseId, userId(auth));
    }

    /** 최근 자주 같이 게임한 사람들(users.id). 표시 정보는 프론트가 user 서비스에서 채운다. */
    @GetMapping("/recommendations/playmates")
    public List<Long> recommendedPlaymates(Authentication auth) {
        return recommendationService.recommendedPlaymates(userId(auth));
    }
}
