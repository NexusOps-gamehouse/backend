package gg.duo.crew.controller;

import gg.duo.crew.dto.ChatMessageDto;
import gg.duo.crew.service.HouseChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * House 채팅 (STOMP).
 *
 *   보내기 : /pub/house/chat
 *   받기   : /sub/house/{houseId}
 */
@Controller
@RequiredArgsConstructor
public class HouseChatController {

    private final SimpMessageSendingOperations messagingTemplate;
    private final HouseChatService houseChatService;

    @MessageMapping("/house/chat")
    public void sendMessage(ChatMessageDto message, Principal principal) {
        // WebSocketConfig 에서 JWT 로 검증한 Principal 사용 (보안)
        Long senderId = Long.valueOf(principal.getName());

        // 1. 멤버 검증 및 DB 저장
        ChatMessageDto saved = houseChatService.save(message, senderId);

        // 2. STOMP Broker Relay(RabbitMQ)를 통해 해당 하우스 구독자들에게 브로드캐스트
        messagingTemplate.convertAndSend("/sub/house/" + saved.getHouseId(), saved);
    }
}