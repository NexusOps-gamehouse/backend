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
import java.util.NoSuchElementException;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class RiotService {

    private final RiotApiClient riotApiClient;

    public RiotProfileResponseDTO fetchProfile(String gameName, String tagLine) {

        // 1. Riot ID -> PUUID
        AccountResponseDTO account =
                riotApiClient.getAccount(gameName, tagLine);

        // 없는 계정이면 여기서 끝낸다.
        //
        // NoSuchElementException 은 GlobalExceptionHandler 가 이미 404 로 매핑하고 있어
        // 새 예외 클래스를 만들 필요가 없다.
        //
        // 이 검사가 없으면 바로 아래 account.getPuuid() 에서 NPE 로 죽는다.
        // 결과적으로 뒤 3개 호출을 안 하는 것은 같지만, 그건 우연이지 보장이 아니다.
        // 명시적으로 끊어야 (1) 프론트가 이유를 알 수 있고
        // (2) 없는 계정 때문에 Riot 호출 예산(2분/100회)을 낭비하지 않는다.
        if (account == null) {
            throw new NoSuchElementException("소환사를 찾을 수 없습니다. 게임명과 태그를 확인해 주세요.");
        }

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