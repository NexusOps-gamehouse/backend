package gg.duo.riot.service;

import gg.duo.riot.client.RiotApiClient;
import gg.duo.riot.dto.response.AccountResponseDTO;
import gg.duo.riot.dto.ChampionMasteryDTO;
import gg.duo.riot.dto.response.ChampionMasteryResponseDTO;
import gg.duo.riot.dto.response.LeagueResponseDTO;
import gg.duo.riot.dto.response.RiotProfileResponseDTO;
import gg.duo.riot.dto.response.SummonerResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class RiotService {

    private final RiotApiClient riotApiClient;

    public RiotProfileResponseDTO fetchProfile(String gameName, String tagLine) {

        // 1. Riot ID -> PUUID
        AccountResponseDTO account =
                riotApiClient.getAccount(gameName, tagLine);

        // 2. 소환사 정보 조회
        SummonerResponseDTO summoner =
                riotApiClient.getSummoner(account.getPuuid());

        // 3. 티어 정보 조회
        LeagueResponseDTO league =
                riotApiClient.getLeague(account.getPuuid());

        // 4. 챔피언 숙련도 조회
        List<ChampionMasteryResponseDTO> masteries =
                riotApiClient.getChampionMasteries(account.getPuuid());

        // 5. ChampionMasteryResponseDTO -> ChampionMasteryDTO 변환
        List<ChampionMasteryDTO> championMasteries =
                IntStream.range(0, masteries.size())
                        .mapToObj(i -> {

                            ChampionMasteryResponseDTO mastery = masteries.get(i);

                            return ChampionMasteryDTO.builder()
                                    .ranking(i + 1)
                                    .championId(mastery.getChampionId())
                                    .championMasteryLevel(mastery.getChampionMasteryLevel())
                                    .championMasteryPoints(mastery.getChampionMasteryPoints())
                                    .build();
                        })
                        .toList();

        // 6. 하나의 DTO로 조합
        return RiotProfileResponseDTO.builder()

                // Account API
                .puuid(account.getPuuid())
                .gameName(account.getGameName())
                .tagLine(account.getTagLine())

                // Summoner API
                .profileIconId(summoner.getProfileIconId())
                .summonerLevel(summoner.getSummonerLevel())

                // League API
                .tier(league != null ? league.getTier() : null)
                .rank(league != null ? league.getRank() : null)
                .leaguePoints(league != null ? league.getLeaguePoints() : null)
                .wins(league != null ? league.getWins() : null)
                .losses(league != null ? league.getLosses() : null)

                // Champion Mastery API
                .championMasteries(championMasteries)

                .build();
    }
}