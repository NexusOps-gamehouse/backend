package gg.duo.crew.dto;

import gg.duo.crew.domain.ShopItem;

public record ShopItemResponseDto(
        Long id,
        String code,
        String name,
        ShopItem.ItemCategory category,
        int priceHc,
        String imageUrl
) {

    public static ShopItemResponseDto from(ShopItem item) {
        return new ShopItemResponseDto(
                item.getId(),
                item.getCode(),
                item.getName(),
                item.getCategory(),
                item.getPriceHc(),
                item.getImageUrl()
        );
    }
}
