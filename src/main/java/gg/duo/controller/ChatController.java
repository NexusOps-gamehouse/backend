package gg.duo.controller;

import gg.duo.dto.ChatDtos.RoomDetail;
import gg.duo.dto.ChatDtos.RoomDto;
import gg.duo.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @GetMapping("/rooms")
    public List<RoomDto> myRooms(Authentication auth) {
        return chatService.myRooms((Long) auth.getPrincipal());
    }

    @GetMapping("/rooms/{roomId}")
    public RoomDetail roomDetail(@PathVariable Long roomId, Authentication auth) {
        return chatService.roomDetail(roomId, (Long) auth.getPrincipal());
    }

    /** 방장: 멤버 강퇴 (신청 거절 처리 포함) */
    @DeleteMapping("/rooms/{roomId}/members/{userId}")
    public void kick(@PathVariable Long roomId, @PathVariable Long userId, Authentication auth) {
        chatService.kick(roomId, userId, (Long) auth.getPrincipal());
    }
}
