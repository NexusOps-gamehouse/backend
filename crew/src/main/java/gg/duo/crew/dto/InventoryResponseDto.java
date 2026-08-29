package gg.duo.crew.dto;

import gg.duo.crew.domain.Inventory;

public record InventoryResponseDto(
        Long id,
        Long userId,
        Long houseId,
        ShopItemResponseDto item,
        Integer quantity,
        boolean isApplied
) {

    public static InventoryResponseDto from(Inventory inventory) {
        return new InventoryResponseDto(
                inventory.getId(),
                inventory.getUserId(),
                inventory.getHouseId(),
                ShopItemResponseDto.from(inventory.getItem()),
                inventory.getQuantity(),
                Boolean.TRUE.equals(inventory.getIsApplied())
        );
    }
}
