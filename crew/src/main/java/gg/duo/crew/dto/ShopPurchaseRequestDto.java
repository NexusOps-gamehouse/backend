package gg.duo.crew.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ShopPurchaseRequestDto {
    @NotNull(message = "상품은 필수입니다.")
    @Positive(message = "상품 ID는 1 이상이어야 합니다.")
    private Long itemId;

    @NotNull(message = "수량은 필수입니다.")
    @Positive(message = "수량은 1 이상이어야 합니다.")
    private Integer quantity;
}
