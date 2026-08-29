package gg.duo.crew.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ShopPurchaseRequestDto {
    private Long itemId;
    private Integer quantity;
}