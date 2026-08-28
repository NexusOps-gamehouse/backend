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

    private String name;

    @Enumerated(EnumType.STRING)
    private ItemCategory category; // BORDER, TITLE, BANNER, THEME 등

    private int priceHc; // 5의 배수 재화

    private String imageUrl;

    public enum ItemCategory {
        BORDER, TITLE, BANNER, THEME, CHAT_SKIN, NICKNAME_DECO, HOUSE_ICON
    }
}