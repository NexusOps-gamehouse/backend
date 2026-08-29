package gg.duo.crew.service;

import gg.duo.crew.domain.chat.HouseChatMessage;
import gg.duo.crew.domain.chat.HouseChatMessageRepository;
import gg.duo.crew.dto.ChatMessageDto;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HouseChatService {

    private final HouseChatMessageRepository chatMessageRepository;
    private final HouseService houseService;
    private final JdbcTemplate jdbcTemplate;

    /**
     * 인증된 userId를 기준으로 실제 사용자 닉네임을 조회한다.
     *
     * senderName을 클라이언트가 보내게 하지 않고 서버에서 직접 결정하므로,
     * 다른 사용자의 닉네임으로 메시지를 보내는 위조를 막는다.
     */
    private String resolveSenderName(Long senderId, String storedSenderName) {
        if (storedSenderName != null && !storedSenderName.isBlank()) {
            return storedSenderName;
        }

        if (senderId == null) {
            return "House 멤버";
        }

        List<String> nicknames = jdbcTemplate.query(
                """
                SELECT nickname
                FROM user_svc.users
                WHERE id = ?
                """,
                (rs, rowNum) -> rs.getString("nickname"),
                senderId
        );

        if (!nicknames.isEmpty()) {
            String nickname = nicknames.get(0);

            if (nickname != null && !nickname.isBlank()) {
                return nickname;
            }
        }

        return "사용자 #" + senderId;
    }

    /**
     * STOMP로 들어온 메시지를 저장한다.
     *
     * senderId와 senderName은 클라이언트 값을 신뢰하지 않고
     * WebSocket 인증 Principal의 userId를 기준으로 서버에서 결정한다.
     */
    @Transactional
    public ChatMessageDto save(ChatMessageDto dto, Long senderId) {
        houseService.requireApprovedMember(dto.getHouseId(), senderId);

        String senderName = resolveSenderName(senderId, null);

        dto.setSenderId(senderId);
        dto.setSenderName(senderName);
        dto.setTimestamp(LocalDateTime.now());

        chatMessageRepository.save(HouseChatMessage.from(dto));

        return dto;
    }

    /** 최근 50개 메시지를 오래된 순으로 반환한다. */
    @Transactional(readOnly = true)
    public List<ChatMessageDto> recent(Long houseId, Long userId) {
        houseService.requireApprovedMember(houseId, userId);

        List<HouseChatMessage> found =
                new ArrayList<>(
                        chatMessageRepository.findTop50ByHouseIdOrderByIdDesc(houseId)
                );

        java.util.Collections.reverse(found);

        return found.stream()
                .map(message -> ChatMessageDto.builder()
                        .houseId(message.getHouseId())
                        .senderId(message.getSenderId())
                        .senderName(
                                resolveSenderName(
                                        message.getSenderId(),
                                        message.getSenderName()
                                )
                        )
                        .message(message.getMessage())
                        .timestamp(message.getTimestamp())
                        .build())
                .toList();
    }
}
