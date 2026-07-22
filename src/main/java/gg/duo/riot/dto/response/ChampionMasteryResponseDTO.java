package gg.duo.riot.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChampionMasteryResponseDTO {
    private long championId;

    @JsonProperty("championLevel")
    private int chmapionMasteryLevel;

    @JsonProperty("championPoints")
    private int championMasteryPoints;
}
