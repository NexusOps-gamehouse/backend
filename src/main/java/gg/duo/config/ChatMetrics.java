package gg.duo.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 채팅(WebSocket/STOMP) 지표.
 *
 * cAdvisor 는 컨테이너를 밖에서 보므로 "backend 가 CPU 를 얼마나 쓰는지"는 알아도
 * "그게 채팅 때문인지"는 알 수 없다. 앱 내부 사정은 앱이 직접 내보내야 한다.
 *
 * Counter 와 Gauge 의 구분:
 *   Counter — 올라가기만 하는 누적값(메시지 수). 조회할 때 rate() 로 감싼다.
 *   Gauge   — 오르내리는 현재값(접속자 수). 그대로 본다.
 * container_cpu_usage_seconds_total(counter) 과
 * container_memory_working_set_bytes(gauge) 의 관계와 동일하다.
 *
 * 주의: userId / IP / roomId 처럼 값의 종류가 무한한 것을 tag 로 넣으면
 * 시계열이 폭발한다(카디널리티). 그런 추적은 로그(Loki)의 역할이다.
 */
@Component
public class ChatMetrics {

    private final Counter messagesIn;
    private final Counter connects;
    private final Counter disconnects;
    private final AtomicInteger activeSessions;

    public ChatMetrics(MeterRegistry registry) {
        this.messagesIn = Counter.builder("chat_messages_total")
                .description("채팅 메시지 수신 건수")
                .tag("direction", "inbound")
                .register(registry);

        this.connects = Counter.builder("chat_connection_events_total")
                .description("WebSocket 연결/해제 이벤트")
                .tag("event", "connect")
                .register(registry);

        this.disconnects = Counter.builder("chat_connection_events_total")
                .description("WebSocket 연결/해제 이벤트")
                .tag("event", "disconnect")
                .register(registry);

        this.activeSessions = registry.gauge(
                "chat_active_sessions", new AtomicInteger(0));
    }

    /** @MessageMapping 핸들러에서 호출 */
    public void messageReceived() {
        messagesIn.increment();
    }

    public void sessionOpened() {
        connects.increment();
        activeSessions.incrementAndGet();
    }

    public void sessionClosed() {
        disconnects.increment();
        activeSessions.decrementAndGet();
    }
}
