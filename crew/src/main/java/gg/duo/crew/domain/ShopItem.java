package gg.duo.crew.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "shop_items")
@Getter
@NoArgsConstructor
public class ShopItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false, length = 100)
    private String code;

    private String name;

    @Enumerated(EnumType.STRING)
    private ItemCategory category; // BORDER, TITLE, BANNER, THEME 등

    private int priceHc; // 5의 배수 재화

    private String imageUrl;

    public enum ItemCategory {
        BORDER, BANNER, BADGE, THEME, TITLE, CHAT_SKIN, NICKNAME_DECO, HOUSE_ICON
    }
}
