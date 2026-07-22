package gg.duo.riot.client;

import gg.duo.riot.dto.response.AccountResponseDTO;
import gg.duo.riot.dto.response.SummonerResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class RiotApiClient {

    private final WebClient riotWebClient;

    @Value("${riot.api.key}")
    private String apiKey;

    public AccountResponseDTO getAccount(String gameName, String tagLine) {

        return riotWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/riot/account/v1/accounts/by-riot-id/{gameName}/{tagLine}")
                        .build(gameName, tagLine))
                .header("X-Riot-Token", apiKey)
                .retrieve()
                .bodyToMono(AccountResponseDTO.class)
                .block();
    }

    public SummonerResponseDTO getSummoner(String puuid) {

        return riotWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/lol/summoner/v4/summoners/by-puuid/{puuid}")
                        .build(puuid))
                .header("X-Riot-Token", apiKey)
                .retrieve()
                .bodyToMono(SummonerResponseDTO.class)
                .block();
    }
}