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

    /**
     * WebClient.builder() (static) 대신 주입받은 WebClient.Builder 를 사용한다.
     *
     * static builder 는 아무것도 붙지 않은 빈 빌더라 Micrometer 계측이 적용되지 않는다.
     * Spring Boot 가 자동 구성해 두는 WebClient.Builder 빈을 써야
     * http_client_requests_seconds_* (호출량 / 응답시간 / 상태코드) 지표가 기록된다.
     * Riot API 429(레이트 리밋) 감지가 여기에 달려 있다.
     */
    @Bean("regionalWebClient")
    public WebClient regionalWebClient(WebClient.Builder builder) {
        return builder
                .baseUrl(regionalUrl)
                .build();
    }

    @Bean("platformWebClient")
    public WebClient platformWebClient(WebClient.Builder builder) {
        return builder
                .baseUrl(platformUrl)
                .build();
    }

}
