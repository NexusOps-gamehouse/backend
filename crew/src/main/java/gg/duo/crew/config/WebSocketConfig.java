package gg.duo.crew.config;

import gg.duo.common.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;

/**
 * House(Crew) 채팅용 STOMP 설정.
 *
 * [브로커 - RabbitMQ STOMP Relay]
 * enableStompBrokerRelay 를 통해 RabbitMQ(:61613)에 메시지 중계를 위임한다.
 * 파드가 여러 개일 때 파드마다 브로커가 따로 놀면 대화가 갈라지는 현상을 방지한다.
 *
 * [엔드포인트가 /ws 가 아니라 /ws-house 인 이유]
 * /ws 는 chat 서비스(:8083)가 이미 쓰고 있고, 프론트 프록시도 /ws → chat 이다.
 * 같은 경로를 쓰면 프록시에서 갈라낼 방법이 없다.
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtTokenProvider jwtTokenProvider;

    @Value("${spring.rabbitmq.host:localhost}")
    private String rabbitHost;

    @Value("${spring.rabbitmq.stomp.port:61613}")
    private int stompPort;

    @Value("${spring.rabbitmq.username:appuser}")
    private String rabbitUser;

    @Value("${spring.rabbitmq.password:apppass}")
    private String rabbitPass;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 클라이언트가 메시지를 보낼 때 사용할 prefix
        registry.setApplicationDestinationPrefixes("/pub");

        // External RabbitMQ STOMP Relay 설정 (/sub, /topic, /queue 구독 허용)
        registry.enableStompBrokerRelay("/sub", "/topic", "/queue")
                .setRelayHost(rabbitHost)
                .setRelayPort(stompPort)
                .setClientLogin(rabbitUser)
                .setClientPasscode(rabbitPass)
                .setSystemLogin(rabbitUser)
                .setSystemPasscode(rabbitPass);
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-house")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    /**
     * CONNECT 프레임에서 JWT 를 확인하고 세션에 사용자를 심는다.
     *
     * 이게 없으면 HouseChatController 가 클라이언트가 보낸 senderId 를 그대로
     * 믿어야 한다 — 남의 이름으로 메시지를 남길 수 있다는 뜻이다.
     * 심어둔 Principal 은 @MessageMapping 메서드에서 Principal 로 받는다.
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor =
                        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String header = accessor.getFirstNativeHeader("Authorization");
                    if (header == null || !header.startsWith("Bearer ")
                            || !jwtTokenProvider.validate(header.substring(7))) {
                        throw new IllegalArgumentException("유효하지 않은 토큰입니다.");
                    }
                    Long userId = jwtTokenProvider.getUserId(header.substring(7));
                    accessor.setUser(new UsernamePasswordAuthenticationToken(
                            String.valueOf(userId), null, List.of()));
                }
                return message;
            }
        });
    }
}
