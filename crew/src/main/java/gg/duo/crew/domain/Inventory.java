package gg.duo.crew.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "inventories")
@Getter
@NoArgsConstructor
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private Long houseId; // House 전용 아이템일 경우 사용

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")
    private ShopItem item;

    public Inventory(Long userId, Long houseId, ShopItem item) {
        this.userId = userId;
        this.houseId = houseId;
        this.item = item;
    }
}