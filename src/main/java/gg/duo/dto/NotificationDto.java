package gg.duo.dto;

import gg.duo.entity.Notification;

import java.time.Instant;

public record NotificationDto(Long id, String message, String link, boolean read, Instant createdAt) {
    public static NotificationDto from(Notification n) {
        return new NotificationDto(n.getId(), n.getMessage(), n.getLink(), n.isRead(), n.getCreatedAt());
    }
}
