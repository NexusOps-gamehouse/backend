package gg.duo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessageDto {
    private Long houseId;
    private Long senderId;
    private String senderName;
    private String message;
    private LocalDateTime timestamp;
}