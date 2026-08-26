package gg.duo.crew.service;

import gg.duo.crew.domain.chat.HouseChatMessage;
import gg.duo.crew.domain.chat.HouseChatMessageRepository;
import gg.duo.crew.dto.ChatMessageDto;
import lombok.RequiredArgsConstructor;
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

    /** STOMP 로 들어온 메시지를 저장한다. 보낸 사람은 세션에서 확인된 값으로 덮어쓴다. */
    @Transactional
    public ChatMessageDto save(ChatMessageDto dto, Long senderId) {
        houseService.requireApprovedMember(dto.getHouseId(), senderId);
        dto.setSenderId(senderId);
        dto.setTimestamp(LocalDateTime.now());
        chatMessageRepository.save(HouseChatMessage.from(dto));
        return dto;
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
