package gg.duo.crew.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ItemApplyRequestDto {
    private String action; // "APPLY" 또는 "UNAPPLY"
}