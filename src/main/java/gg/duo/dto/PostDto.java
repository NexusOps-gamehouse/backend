package gg.duo.dto;

import java.time.Instant;

public record PostDto(
        Long id,
        String title,
        String content,
        Instant createdAt,
        UserDto author,
        long pendingCount,
        String myApplicationStatus, // null | PENDING | APPROVED | CONFIRMED | REJECTED
        boolean mine,
        // 모집 조건
        String game,
        String gameMode,
        String playTime,
        boolean micRequired,
        String positions,
        int targetMembers,
        long currentMembers,        // 파티 채팅방 인원 (방장 포함, 방 없으면 1)
        String status,              // RECRUITING | CLOSED
        Long chatRoomId             // 내가 멤버인 경우에만 세팅
) {
    public record WriteRequest(String title, String content, String game, String gameMode,
                               String playTime, boolean micRequired, String positions,
                               Integer targetMembers) {}
}
