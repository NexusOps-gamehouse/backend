package gg.duo.crew.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "inventories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private Long houseId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")
    private ShopItem item;

    @Builder.Default
    private Integer quantity = 0;

    @Builder.Default
    private Boolean isApplied = false;

    // 수량 추가 메서드
    public void addQuantity(int count) {
        this.quantity = (this.quantity == null ? 0 : this.quantity) + count;
    }

    // 적용 여부 변경 메서드
    public void setApplied(boolean applied) {
        this.isApplied = applied;
    }
}