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

    public AccountResponseDTO getAccount(String gameName, String tagLine) {

        // URI 는 반드시 템플릿 문자열 형태로 넘긴다.
        // uriBuilder 람다로 만들면 Micrometer 가 원래 템플릿을 알 수 없어
        // uri 태그가 붙지 않거나 실제 값이 그대로 들어가 시계열이 폭발한다.
        return regionalWebClient.get()
                .uri("/riot/account/v1/accounts/by-riot-id/{gameName}/{tagLine}", gameName, tagLine)
                .header("X-Riot-Token", apiKey)
                .retrieve()
                .bodyToMono(AccountResponseDTO.class)
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