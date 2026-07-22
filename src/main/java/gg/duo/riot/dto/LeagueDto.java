package gg.duo.riot.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LeagueDto{
    private String tier;         // DIAMOND, PLATINUM, GOLD 등
    private String rank;         // I, II, III, IV
    private int leaguePoints;    // LP 점수
    private int wins;            // 승리 횟수
    private int losses;          // 패배 횟수
}
