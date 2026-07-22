package gg.duo.riot.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class RiotConfig {

    @Value("${riot.api.base-url}")
    private String baseUrl;

    @Bean
    public WebClient riotWebClient() {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }
}
