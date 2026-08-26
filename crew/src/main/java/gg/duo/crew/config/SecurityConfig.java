package gg.duo.crew.config;

import gg.duo.common.security.SecurityBaseConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends SecurityBaseConfig {

    @Override
    protected void configurePublicEndpoints(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {

        // House 목록·상세는 로그인 없이도 볼 수 있게 한다. 가입 버튼을 누르는
        // 순간부터 인증이 필요하다. (auth 가 null 이면 myRole/myStatus 가 null 로 나간다)
        auth.requestMatchers(HttpMethod.GET, "/api/crew/houses", "/api/crew/houses/*").permitAll();

        /*
         * SockJS 핸드셰이크는 permitAll 이어야 한다.
         *
         * 브라우저의 WebSocket 은 Authorization 헤더를 붙일 수 없다. 그래서 토큰은
         * 연결이 열린 뒤 STOMP CONNECT 프레임의 네이티브 헤더로 온다.
         * 검증은 WebSocketConfig 의 인바운드 채널 인터셉터가 한다 — 여기서 막으면
         * 토큰이 도착할 기회 자체가 없다.
         *
         * /** 까지 붙이는 이유: SockJS 는 /ws-house/info, /ws-house/{server}/{session}/xhr
         * 처럼 하위 경로를 여러 개 쓴다. /ws-house 만 열면 info 요청이 401 로 막혀
         * 클라이언트가 전송 방식을 고르지 못한다.
         */
        auth.requestMatchers("/ws-house/**").permitAll();

        // 서비스 간 호출. Ingress 에 /internal 규칙이 없어 클러스터 밖에서는 닿지 않는다.
        auth.requestMatchers("/internal/**").permitAll();
    }
}
