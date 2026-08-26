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
 *
 * 원본 코드는 페이로드의 senderId 를 그대로 믿고 저장했다. 클라이언트가 보낸
 * 값이라 남의 id 를 적어 보낼 수 있었다. 여기서는 CONNECT 때 JWT 로 확인해
 * 세션에 심어둔 Principal 을 쓴다(WebSocketConfig 참고).
 */
@Controller
@RequiredArgsConstructor
public class HouseChatController {

    private final SimpMessageSendingOperations messagingTemplate;
    private final HouseChatService houseChatService;

    @MessageMapping("/house/chat")
    public void sendMessage(ChatMessageDto message, Principal principal) {
        Long senderId = Long.valueOf(principal.getName());

        // 저장 안에서 House 멤버인지도 확인한다. 멤버가 아니면 예외가 나고
        // 브로드캐스트까지 가지 않는다.
        ChatMessageDto saved = houseChatService.save(message, senderId);

        messagingTemplate.convertAndSend("/sub/house/" + saved.getHouseId(), saved);
    }
}
