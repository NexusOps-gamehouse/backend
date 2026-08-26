package gg.duo.crew.domain.chat;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 원래 이 인터페이스는 존재하지 않는 entity.ChatMessage 를 가리키고 있었다.
 * (파일 이름은 HouseChatMessageRepository 인데 타입은 ChatMessage)
 * crew 모듈이 컴파일 대상에 없어서 그 에러가 드러나지 않았을 뿐이다.
 */
public interface HouseChatMessageRepository extends JpaRepository<HouseChatMessage, Long> {

    /** 최근 메시지부터 50개. 화면에서 뒤집어 쓴다. */
    List<HouseChatMessage> findTop50ByHouseIdOrderByIdDesc(Long houseId);
}
