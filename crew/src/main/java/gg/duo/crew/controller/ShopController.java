package gg.duo.crew.controller;

import gg.duo.crew.domain.Inventory;
import gg.duo.crew.domain.ShopItem;
import gg.duo.crew.dto.ItemApplyRequestDto;
import gg.duo.crew.dto.ShopPurchaseRequestDto;
import gg.duo.crew.dto.ShopPurchaseResponseDto;
import gg.duo.crew.repository.ShopItemRepository;
import gg.duo.crew.service.ShopService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/shop")
@RequiredArgsConstructor
public class ShopController {

    private final ShopItemRepository shopItemRepository;
    private final ShopService shopService;

    private Long getUserId(Authentication auth) {
        return auth == null ? null : (Long) auth.getPrincipal();
    }

    // 기존 상품 목록 조회
    @GetMapping("/items")
    public ResponseEntity<List<ShopItem>> getItems() {
        return ResponseEntity.ok(shopItemRepository.findAll());
    }

    // 안전한 POST /api/shop/buy
    @PostMapping("/buy")
    public ResponseEntity<ShopPurchaseResponseDto> buyItem(
            @RequestParam Long houseId,
            @Valid @RequestBody ShopPurchaseRequestDto request,
            Authentication auth) {
        shopService.buyItem(houseId, getUserId(auth), request);
        return ResponseEntity.ok(new ShopPurchaseResponseDto(true, "아이템 구매가 완료되었습니다."));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getDefaultMessage())
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse("입력값을 확인해주세요.");
        return ResponseEntity.badRequest().body(Map.of("message", message));
    }

    // GET /api/shop/inventory
    @GetMapping("/inventory")
    public ResponseEntity<List<Inventory>> getInventory(
            @RequestParam Long houseId,
            Authentication auth) {
        return ResponseEntity.ok(shopService.getInventory(houseId, getUserId(auth)));
    }

    // 아이템 적용/해제 API
    @PostMapping("/inventory/{inventoryItemId}/toggle")
    public ResponseEntity<Void> toggleItem(
            @RequestParam Long houseId,
            @PathVariable Long inventoryItemId,
            @RequestBody ItemApplyRequestDto request,
            Authentication auth) {
        shopService.toggleItemApply(houseId, inventoryItemId, getUserId(auth), request);
        return ResponseEntity.ok().build();
    }
}
