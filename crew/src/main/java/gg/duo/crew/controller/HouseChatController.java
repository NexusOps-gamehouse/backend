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
 *   받기   : /topic/crew.houses.{houseId}
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

        // 메시지 저장 시 유저의 역할(방장/부방장/멤버)도 함께 DTO에 채워짐
        ChatMessageDto saved = houseChatService.save(message, senderId);

        // RabbitMQ 규격에 맞춘 /topic/ 주소로 브로드캐스트
        messagingTemplate.convertAndSend("/topic/crew.houses." + saved.getHouseId(), saved);
    }
}