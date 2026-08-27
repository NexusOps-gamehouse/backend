package gg.duo.crew.service;

import gg.duo.crew.domain.chat.HouseChatMessage;
import gg.duo.crew.domain.chat.HouseChatMessageRepository;
import gg.duo.crew.domain.house.HouseMember;
import gg.duo.crew.domain.house.MemberRole;
import gg.duo.crew.dto.ChatMessageDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class HouseChatService {

    private final HouseChatMessageRepository chatMessageRepository;
    private final HouseService houseService;

    /** STOMP 로 들어온 메시지를 저장한다. 보낸 사람은 세션에서 확인된 값으로 덮어쓴다. */
    @Transactional
    public ChatMessageDto save(ChatMessageDto dto, Long senderId) {
        HouseMember sender = houseService.requireApprovedMember(dto.getHouseId(), senderId);
        dto.setSenderId(senderId);
        LocalDateTime now = LocalDateTime.now();
        dto.setTimestamp(now);

        HouseChatMessage previous = chatMessageRepository
                .findTopByHouseIdAndSenderIdOrderByIdDesc(dto.getHouseId(), senderId);
        Duration elapsed = previous == null
                ? null
                : Duration.between(previous.getTimestamp(), now);
        boolean repeatedTooSoon = previous != null
                && !elapsed.isNegative()
                && elapsed.getSeconds() < 30
                && normalize(dto.getMessage()).equals(normalize(previous.getMessage()));
        boolean countableMessage = dto.getMessage() != null && !dto.getMessage().isBlank();

        chatMessageRepository.save(HouseChatMessage.from(dto));
        if (sender.getRole() == MemberRole.MEMBER
                && countableMessage && !repeatedTooSoon) {
            sender.recordChatActivity();
        }
        return dto;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    /** 최근 50개를 오래된 순으로 돌려준다. */
    @Transactional(readOnly = true)
    public List<ChatMessageDto> recent(Long houseId, Long userId) {
        houseService.requireApprovedMember(houseId, userId);
        List<HouseChatMessage> found =
                new ArrayList<>(chatMessageRepository.findTop50ByHouseIdOrderByIdDesc(houseId));
        java.util.Collections.reverse(found);
        return found.stream()
                .map(m -> new ChatMessageDto(m.getHouseId(), m.getSenderId(), m.getSenderName(),
                        m.getMessage(), m.getTimestamp()))
                .toList();
    }
}
