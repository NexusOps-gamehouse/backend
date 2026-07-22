package gg.duo.riot.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MasteryResponseDTO {
    private long championId;

    @JsonProperty("championLevel")
    private int masteryLevel;

    @JsonProperty("championPoints")
    private int masteryPoints;
}
