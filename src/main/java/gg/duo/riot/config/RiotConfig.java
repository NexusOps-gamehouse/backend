package gg.duo.riot.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class RiotConfig {

    @Value("${riot.api.regional-url}")
    private String regionalUrl;

    @Value("${riot.api.platform-url}")
    private String platformUrl;

    @Bean("regionalWebClient")
    public WebClient regionalWebClient() {
        return WebClient.builder()
                .baseUrl(regionalUrl)
                .build();
    }

    @Bean("platformWebClient")
    public WebClient platformWebClient() {
        return WebClient.builder()
                .baseUrl(platformUrl)
                .build();
    }

}
