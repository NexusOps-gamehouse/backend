package gg.duo.dto;

import java.time.Instant;

public record ApplicationDto(
        Long id,
        String status,
        Instant createdAt,
        Long postId,
        String postTitle,
        UserDto applicant,
        Long chatRoomId // 승인된 경우에만
) {}
