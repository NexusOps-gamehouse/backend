package gg.duo.crew.controller;

import gg.duo.crew.domain.ShopItem;
import gg.duo.crew.repository.ShopItemRepository;
import gg.duo.crew.service.ShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shop")
@RequiredArgsConstructor
public class ShopController {

    private final ShopItemRepository shopItemRepository;
    private final ShopService shopService;

    @GetMapping("/items")
    public ResponseEntity<List<ShopItem>> getItems() {
        return ResponseEntity.ok(shopItemRepository.findAll());
    }

    @PostMapping("/buy")
    public ResponseEntity<Void> buyItem(
            @RequestParam Long userId,
            @RequestParam(required = false) Long houseId,
            @RequestParam Long itemId) {
        shopService.buyItem(userId, houseId, itemId);
        return ResponseEntity.ok().build();
    }
}