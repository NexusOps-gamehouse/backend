package gg.duo.riot.dto.response;

import gg.duo.riot.dto.ChampionMasteryDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiotProfileResponseDTO {

    // Account API
    private String puuid;
    private String gameName;
    private String tagLine;

    // Summoner API
    private Integer profileIconId;
    private Long summonerLevel;

    // League API
    private String tier;
    private String rank;
    private Integer leaguePoints;
    private Integer wins;
    private Integer losses;

    // Champion Mastery API
    private List<ChampionMasteryDTO> championMasteries;
}