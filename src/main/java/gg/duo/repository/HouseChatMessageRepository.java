package gg.duo.repository; // com.example.gamehouse -> gg.duo로 변경

import gg.duo.entity.HouseChatMessage; // com.example.gamehouse -> gg.duo로 변경
import org.springframework.data.jpa.repository.JpaRepository;

public interface HouseChatMessageRepository extends JpaRepository<HouseChatMessage, Long> {
}