package gg.duo.crew.config;

import gg.duo.common.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
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
 * House 채팅용 STOMP 설정.
 *
 * [chat 서비스와 다른 점 — 브로커]
 * chat 은 enableStompBrokerRelay 로 RabbitMQ(:61613)에 전달을 위임한다.
 * 파드가 여러 개일 때 파드마다 브로커가 따로 놀면 대화가 갈라지기 때문이다.
 * crew 는 아직 단일 인스턴스라 서버 메모리 브로커로 시작한다. 릴레이로 바꾸려면
 *   1) application.yml 에 최상위 rabbitmq.{host,port,username,password} (STOMP :61613)
 *   2) build.gradle 에 io.projectreactor.netty:reactor-netty-http
 *      (없으면 "No compatible version of Reactor Netty" 로 부팅이 죽는다)
 * 두 가지가 함께 필요하다. chat/build.gradle 의 주석에 자세히 적혀 있다.
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

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/sub");
        registry.setApplicationDestinationPrefixes("/pub");
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
