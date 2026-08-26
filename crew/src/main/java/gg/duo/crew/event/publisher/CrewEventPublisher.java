package gg.duo.crew.event.publisher;

import gg.duo.common.event.CrewFormedEvent;
import gg.duo.common.event.DomainEventPublisher;
import gg.duo.common.event.NotificationRequestedEvent;
import gg.duo.crew.domain.house.House;
import gg.duo.crew.domain.house.HouseMember;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * crew 가 바깥에 알리는 사실들.
 *
 * CrewFormedEvent 는 common 에 계약만 먼저 들어와 있던 것이다
 * ("crew 서비스가 생기면 발행한다"). 이제 실제로 발행한다.
 */
@Component
@RequiredArgsConstructor
public class CrewEventPublisher {

    private final DomainEventPublisher publisher;

    public void crewFormed(House house) {
        List<Long> memberIds = house.getMembers().stream()
                .map(HouseMember::getUserId)
                .toList();
        publisher.publish(new CrewFormedEvent(house.getId(), house.getName(), memberIds));
    }

    /**
     * 알림 요청.
     *
     * notifications 테이블은 user 가 소유한다. crew 계정에는 user_svc 권한이
     * 없으므로 직접 INSERT 할 수 없고, 이벤트로 요청만 보낸다.
     */
    public void notify(Long userId, String message, String link) {
        publisher.publish(new NotificationRequestedEvent(userId, message, link));
    }
}
