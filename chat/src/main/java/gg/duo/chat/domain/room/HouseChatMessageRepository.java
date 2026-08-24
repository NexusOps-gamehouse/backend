package gg.duo.chat.domain.room;

import gg.duo.crew.entity.HouseChatMessage; // domain 제거
import org.springframework.data.jpa.repository.JpaRepository;

public interface HouseChatMessageRepository extends JpaRepository<HouseChatMessage, Long> {
}