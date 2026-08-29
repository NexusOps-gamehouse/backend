package gg.duo.crew.service;

import gg.duo.crew.domain.Inventory;
import gg.duo.crew.domain.ShopItem;
import gg.duo.crew.domain.house.House;
import gg.duo.crew.domain.house.HouseRepository;
import gg.duo.crew.repository.InventoryRepository;
import gg.duo.crew.repository.ShopItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ShopService {

    private final ShopItemRepository shopItemRepository;
    private final InventoryRepository inventoryRepository;
    private final HouseRepository houseRepository;

    @Transactional
    public void buyItem(Long userId, Long houseId, Long itemId) {
        // 1. 비관적 락(Pessimistic Lock)으로 하우스 조회
        House house = houseRepository.findByIdWithLock(houseId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 하우스입니다."));

        // 2. 상점 상품 조회
        ShopItem item = shopItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("상품이 존재하지 않습니다."));

        // 3. 하우스 코인(HC) 차감 (House.java 내부 deductHc에서 잔액 부족 여부 자동 검사)
        long totalCost = (long) item.getPriceHc();
        house.deductHc(totalCost);

        // 4. 인벤토리에 아이템 저장
        inventoryRepository.save(new Inventory(userId, houseId, item));
    }
}