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

        // 1. H2 콘솔 접근 허용
        auth.requestMatchers("/h2-console/**").permitAll();

        // 기존 설정들
        auth.requestMatchers("/api/shop/**", "/api/houses/**").permitAll();
        auth.requestMatchers(HttpMethod.GET, "/api/crew/houses", "/api/crew/houses/*").permitAll();
        auth.requestMatchers("/ws-house/**").permitAll();
        auth.requestMatchers("/internal/**").permitAll();
    }
}