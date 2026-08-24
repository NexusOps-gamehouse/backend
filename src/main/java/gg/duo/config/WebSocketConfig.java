package gg.duo.config;

import gg.duo.security.JwtTokenProvider;
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

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // /sub (또는 기존 /topic) 접두사 메시지 브로커 등록
        registry.enableSimpleBroker("/sub", "/topic");
        // 클라이언트에서 보낼 때 사용할 접두사
        registry.setApplicationDestinationPrefixes("/pub", "/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 기존 /ws 엔드포인트와 House 전용 /ws-house 엔드포인트를 모두 지원
        registry.addEndpoint("/ws", "/ws-house")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

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