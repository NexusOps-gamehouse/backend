package gg.duo.crew.service;

import gg.duo.crew.domain.Inventory;
import gg.duo.crew.domain.ShopItem;
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

    @Transactional
    public void buyItem(Long userId, Long houseId, Long itemId) {
        ShopItem item = shopItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("상품이 존재하지 않습니다."));

        // 유저/하우스의 HC 차감 확인 로직
        // if (user.getHc() < item.getPriceHc()) throw new IllegalStateException("HC가 부족합니다.");
        // user.deductHc(item.getPriceHc());

        inventoryRepository.save(new Inventory(userId, houseId, item));
    }
}