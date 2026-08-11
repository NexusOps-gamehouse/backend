package gg.duo.config;

import gg.duo.security.JwtAuthFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Value("${app.cors-allowed-origins}")
    private List<String> allowedOrigins;

    /**
     * Actuator 전용 보안 규칙.
     *
     * 기존 filterChain 은 마지막이 anyRequest().authenticated() 라서 actuator 도 401 로 막힌다.
     * Prometheus 는 JWT 를 갖고 있지 않으므로 수집이 불가능해진다.
     *
     * Spring Security 는 @Order 순서대로 검사해 처음 매칭되는 체인 하나만 적용하므로,
     * actuator 요청은 여기서 통과되고 일반 API 요청은 아래 filterChain(@Order 2) 으로 간다.
     * 즉 기존 API 보안 정책은 그대로 유지된다.
     *
     * permitAll 이지만 management.server.port=8081 이고 compose 에서 이 포트를
     * 호스트에 게시하지 않으므로, 같은 docker network 안에서만 접근할 수 있다.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain actuatorFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher(EndpointRequest.toAnyEndpoint())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .csrf(csrf -> csrf.disable());

        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                /*
                 * 인증 정보가 없는 요청에 401 을 반환한다.
                 *
                 * 기본값이 403 인 것이 문제였다. JwtAuthFilter 는 토큰이 유효하지 않으면
                 * (만료 포함) SecurityContext 를 비워둔 채 그냥 통과시키고, 그 요청이
                 * anyRequest().authenticated() 에 걸린다. 이때 Spring Security 는
                 * httpBasic / formLogin 이 없으면 Http403ForbiddenEntryPoint 를 써서 403 을 낸다.
                 *
                 * 그런데 프론트(api/client.js)의 인터셉터는 401 만 처리한다.
                 *   → 403 은 아무도 안 잡는다
                 *   → 토큰을 지우지도, /login 으로 보내지도 않는다
                 *   → 사용자는 죽은 토큰을 들고 "아무것도 안 되는데 로그아웃도 안 되는" 상태가 된다
                 * JWT 만료가 24시간이므로 하루 이상 쓰는 사용자는 반드시 겪는다.
                 *
                 * 401(인증 안 됨)과 403(권한 없음)은 원래 의미가 다르다. 여기서 잡는 것은
                 * 전자뿐이다. 로그인은 했는데 권한이 없는 경우(방장이 아닌데 승인 시도 등)는
                 * SecurityException → GlobalExceptionHandler 경로라 403 그대로 유지된다.
                 *
                 * sendError() 를 쓰지 않는 이유: HTML 에러 페이지를 반환한다.
                 * 이 앱의 에러 응답은 전부 {"message": ...} 이고 프론트 errMsg() 가 그것을 읽는다.
                 */
                .exceptionHandling(ex -> ex.authenticationEntryPoint((req, res, e) -> {
                    res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    res.setContentType("application/json;charset=UTF-8");
                    res.getWriter().write("{\"message\":\"로그인이 필요합니다.\"}");
                }))
                .authorizeHttpRequests(auth -> auth

                        // 로그인 없이 접근 가능한 API
                        .requestMatchers(
                                "/api/auth/**",
                                "/api/users/find-email",
                                "/api/users/reset-password",
                                "/ws/**",
                                "/uploads/**"
                        ).permitAll()

                        // 게시글 조회는 로그인 없이 허용
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/posts",
                                "/api/posts/*"
                        ).permitAll()

                        // 나머지는 로그인 필요
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}