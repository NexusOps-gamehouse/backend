package gg.duo.crew.service;

import gg.duo.crew.domain.Inventory;
import gg.duo.crew.domain.ShopItem;
import gg.duo.crew.domain.house.House;
import gg.duo.crew.domain.house.HouseRepository;
import gg.duo.crew.dto.ItemApplyRequestDto;
import gg.duo.crew.dto.InventoryResponseDto;
import gg.duo.crew.dto.ShopPurchaseRequestDto;
import gg.duo.crew.repository.InventoryRepository;
import gg.duo.crew.repository.ShopItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ShopService {

    private final ShopItemRepository shopItemRepository;
    private final InventoryRepository inventoryRepository;
    private final HouseRepository houseRepository;
    private final HouseService houseService;

    // 1. 아이템 구매 (findByIdWithLock 적용)
    @Transactional
    public void buyItem(Long houseId, Long userId, ShopPurchaseRequestDto request) {
        houseService.requireApprovedMember(houseId, userId);

        // findById 대신 비관적 락이 적용된 findByIdWithLock 사용
        House house = houseRepository.findByIdWithLock(houseId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 하우스입니다."));

        ShopItem item = shopItemRepository.findById(request.getItemId())
                .orElseThrow(() -> new IllegalArgumentException("상품이 존재하지 않습니다."));

        long totalCost = (long) item.getPriceHc() * request.getQuantity();

        // 코인 차감 (deductHc 내부에서 잔액 부족 예외 처리 수행)
        house.deductHc(totalCost);

        Inventory inventory = inventoryRepository.findByHouseIdAndItemId(houseId, item.getId())
                .orElseGet(() -> Inventory.builder()
                        .userId(userId)
                        .houseId(houseId)
                        .item(item)
                        .quantity(0)
                        .isApplied(false)
                        .build());

        inventory.addQuantity(request.getQuantity());
        inventoryRepository.save(inventory);
    }

    // 2. 인벤토리 목록 조회
    @Transactional(readOnly = true)
    public List<InventoryResponseDto> getInventory(Long houseId, Long userId) {
        houseService.requireApprovedMember(houseId, userId);
        return inventoryRepository.findAllByHouseId(houseId).stream()
                .map(InventoryResponseDto::from)
                .toList();
    }

    // 3. 아이템 적용/해제
    @Transactional
    public void toggleItemApply(Long houseId, Long inventoryItemId, Long userId, ItemApplyRequestDto request) {
        houseService.requireApprovedMember(houseId, userId);

        Inventory inventory = inventoryRepository.findByIdAndHouseId(inventoryItemId, houseId)
                .orElseThrow(() -> new IllegalArgumentException("인벤토리 아이템을 찾을 수 없습니다."));

        boolean isApply = request != null && "APPLY".equalsIgnoreCase(request.getAction());
        inventory.setApplied(isApply);
    }
}
