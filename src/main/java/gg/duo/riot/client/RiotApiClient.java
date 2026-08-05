package gg.duo.riot.client;

import gg.duo.riot.dto.response.AccountResponseDTO;
import gg.duo.riot.dto.response.ChampionMasteryResponseDTO;
import gg.duo.riot.dto.response.LeagueResponseDTO;
import gg.duo.riot.dto.response.SummonerResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

@Component
public class RiotApiClient {

    private final WebClient regionalWebClient;
    private final WebClient platformWebClient;

    @Value("${riot.api.key}")
    private String apiKey;

    public RiotApiClient(
            @Qualifier("regionalWebClient") WebClient regionalWebClient,
            @Qualifier("platformWebClient") WebClient platformWebClient) {

        this.regionalWebClient = regionalWebClient;
        this.platformWebClient = platformWebClient;
    }

    /**
     * Riot ID -> 계정(PUUID) 조회.
     *
     * 없는 계정이면 null 을 돌려준다. 호출하는 쪽에서 판단하라는 뜻이다.
     *
     * retrieve() 는 4xx/5xx 를 예외로 던지는데, 이 404 는 "서버가 고장났다"가 아니라
     * "사용자가 없는 게임명/태그를 넣었다"이다. 그대로 두면 잡는 곳이 없어
     * 500 Internal Server Error 가 되고, 프론트에는 "서버 오류"가 뜨며
     * 대시보드의 5xx 패널에도 장애 1건으로 기록된다.
     *
     * 5xx 패널은 "우리 코드가 터졌다"를 알리는 자리라 평소 0 이어야 값어치가 있다.
     * 오타마다 5xx 가 쌓이면 평소 상태를 믿을 수 없게 되고, 알림을 걸어도
     * 오타 알림이 계속 울려 결국 무시하게 되며, 그러다 진짜 장애를 놓친다.
     *
     * 그래서 404 만 Mono.empty() 로 바꿔 null 로 흘린다. 429(레이트 리밋)나
     * 5xx 는 진짜 이상 신호이므로 손대지 않고 그대로 예외로 남긴다.
     */
    public AccountResponseDTO getAccount(String gameName, String tagLine) {

        // URI 는 반드시 템플릿 문자열 형태로 넘긴다.
        // uriBuilder 람다로 만들면 Micrometer 가 원래 템플릿을 알 수 없어
        // uri 태그가 붙지 않거나 실제 값이 그대로 들어가 시계열이 폭발한다.
        return regionalWebClient.get()
                .uri("/riot/account/v1/accounts/by-riot-id/{gameName}/{tagLine}", gameName, tagLine)
                .header("X-Riot-Token", apiKey)
                .retrieve()
                .bodyToMono(AccountResponseDTO.class)
                .onErrorResume(WebClientResponseException.NotFound.class, e -> Mono.empty())
                .block();
    }

    public SummonerResponseDTO getSummoner(String puuid) {

        return platformWebClient.get()
                .uri("/lol/summoner/v4/summoners/by-puuid/{puuid}", puuid)
                .header("X-Riot-Token", apiKey)
                .retrieve()
                .bodyToMono(SummonerResponseDTO.class)
                .block();
    }

    public LeagueResponseDTO getLeague(String puuid) {

        List<LeagueResponseDTO> leagues =
                platformWebClient.get()
                        .uri("/lol/league/v4/entries/by-puuid/{puuid}", puuid)
                        .header("X-Riot-Token", apiKey)
                        .retrieve()
                        .bodyToFlux(LeagueResponseDTO.class)
                        .collectList()
                        .block();

        if (leagues == null || leagues.isEmpty()) {
            return null;
        }

        return leagues.stream()
                .filter(l -> "RANKED_SOLO_5x5".equals(l.getQueueType()))
                .findFirst()
                .orElse(null);
    }

    public List<ChampionMasteryResponseDTO> getChampionMasteries(String puuid) {

        List<ChampionMasteryResponseDTO> masteries =
                platformWebClient.get()
                        .uri("/lol/champion-mastery/v4/champion-masteries/by-puuid/{puuid}", puuid)
                        .header("X-Riot-Token", apiKey)
                        .retrieve()
                        .bodyToFlux(ChampionMasteryResponseDTO.class)
                        .collectList()
                        .block();

        if (masteries == null) {
            return Collections.emptyList();
        }

        return masteries.stream()
                .limit(3)
                .toList();
    }



}