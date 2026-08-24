package gg.duo.entity;

import gg.duo.dto.ChatMessageDto;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "house_chat_messages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HouseChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long houseId;
    private Long senderId;
    private String senderName;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String message;

    private LocalDateTime timestamp;

    @Builder
    public HouseChatMessage(Long houseId, Long senderId, String senderName, String message, LocalDateTime timestamp) {
        this.houseId = houseId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.message = message;
        this.timestamp = timestamp;
    }

    public static HouseChatMessage from(ChatMessageDto dto) {
        return HouseChatMessage.builder()
                .houseId(dto.getHouseId())
                .senderId(dto.getSenderId())
                .senderName(dto.getSenderName())
                .message(dto.getMessage())
                .timestamp(dto.getTimestamp() != null ? dto.getTimestamp() : LocalDateTime.now())
                .build();
    }
}