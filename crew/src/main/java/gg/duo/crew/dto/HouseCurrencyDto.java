package gg.duo.crew.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class HouseCurrencyDto {
    private Long houseId;
    private Long currentHc;
    private Long currentXp;
}