package gg.duo.crew.domain.chat;

import gg.duo.crew.dto.ChatMessageDto;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * House 전용 채팅 메시지.
 *
 * chat 서비스의 ChatRoom / ChatMessage 와는 별개다. 저쪽은 모집글에 딸린
 * 파티 채팅이고, 이쪽은 House 라는 상시 모임의 채널이다. 수명도 소유자도
 * 달라서 같은 테이블에 섞지 않는다.
 */
@Entity
@Table(name = "house_chat_messages",
        indexes = @Index(name = "idx_house_chat_house_id", columnList = "house_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HouseChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "house_id", nullable = false)
    private Long houseId;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    @Column(name = "sender_name")
    private String senderName;

    @Column(columnDefinition = "text", nullable = false)
    private String message;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Builder
    public HouseChatMessage(Long houseId, Long senderId, String senderName,
                            String message, LocalDateTime timestamp) {
        this.houseId = houseId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.message = message;
        this.timestamp = timestamp != null ? timestamp : LocalDateTime.now();
    }

    public static HouseChatMessage from(ChatMessageDto dto) {
        return HouseChatMessage.builder()
                .houseId(dto.getHouseId())
                .senderId(dto.getSenderId())
                .senderName(dto.getSenderName())
                .message(dto.getMessage())
                .timestamp(dto.getTimestamp())
                .build();
    }
}
