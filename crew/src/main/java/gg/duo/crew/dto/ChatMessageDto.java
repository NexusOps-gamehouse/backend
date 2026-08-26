package gg.duo.crew.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * House 채팅 메시지 (STOMP payload).
 *
 * record 가 아닌 이유: STOMP 로 들어온 뒤 서버가 timestamp/senderId 를 덮어쓴다.
 * (클라이언트가 보낸 값을 그대로 믿으면 남의 이름으로 글을 남길 수 있다)
 */
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
