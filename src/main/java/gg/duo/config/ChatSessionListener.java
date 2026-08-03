package gg.duo.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/**
 * WebSocket 세션 연결/해제를 받아 지표를 갱신한다.
 *
 * Spring 이 STOMP 세션 생명주기마다 이벤트를 발행하므로
 * WebSocketConfig 를 수정하지 않고 리스너만 추가하면 된다.
 *
 * 해제 빈도가 급증하면 네트워크 불안정 / 이상 종료 / 타임아웃 신호로 읽는다.
 */
@Component
@RequiredArgsConstructor
public class ChatSessionListener {

    private final ChatMetrics chatMetrics;

    @EventListener
    public void onConnected(SessionConnectedEvent event) {
        chatMetrics.sessionOpened();
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        chatMetrics.sessionClosed();
    }
}
