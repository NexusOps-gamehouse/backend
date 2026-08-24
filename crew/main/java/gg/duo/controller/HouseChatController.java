package gg.duo.controller;

import gg.duo.dto.ChatMessageDto;
import gg.duo.entity.HouseChatMessage;
import gg.duo.repository.HouseChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;

@Controller
@RequiredArgsConstructor
public class HouseChatController {

    private final SimpMessageSendingOperations messagingTemplate;
    private final HouseChatMessageRepository houseChatMessageRepository;

    @MessageMapping("/house/chat")
    public void sendMessage(ChatMessageDto message) {
        message.setTimestamp(LocalDateTime.now());

        // House 전용 채팅 메시지 DB 저장
        houseChatMessageRepository.save(HouseChatMessage.from(message));

        // House 채널(/sub/house/{houseId}) 구독 유저들에게 메시지 실시간 전송
        messagingTemplate.convertAndSend("/sub/house/" + message.getHouseId(), message);
    }
}