-- 실제 프론트엔드 프로필 배너, 휘장, 채팅방 테마 catalog seed.
-- 상품 ID는 지정하지 않고 영구 code를 충돌 기준으로 사용한다.
BEGIN;

INSERT INTO crew_svc.shop_items (code, name, category, price_hc, image_url)
VALUES
    ('BANNER_GUARDIAN_NAME', '수호의 명', 'BANNER', 300, NULL),
    ('BANNER_AMETHYST', '애머시스트', 'BANNER', 200, NULL),
    ('BANNER_DISCO_POP', '디스코 팝', 'BANNER', 200, NULL),
    ('BANNER_FOREST_WARRIOR', '숲의 전사', 'BANNER', 200, NULL),
    ('BANNER_STARRY_NIGHT', '별 헤는 밤', 'BANNER', 200, NULL),
    ('BANNER_VOYAGE_ROAD', '항해의 길', 'BANNER', 300, NULL),
    ('BANNER_FAIRY_GARDEN', '요정의 정원', 'BANNER', 300, NULL),
    ('BANNER_MERMAID_OCEAN', '머메이드 오션', 'BANNER', 300, NULL),

    ('EMBLEM_CRESCENT_MOON', '초승달', 'HOUSE_ICON', 150, NULL),
    ('EMBLEM_LAUREL', '월계수', 'HOUSE_ICON', 150, NULL),
    ('EMBLEM_WAVE', '파도', 'HOUSE_ICON', 150, NULL),
    ('EMBLEM_CHERRY_BLOSSOM', '벚꽃', 'HOUSE_ICON', 150, NULL),
    ('EMBLEM_PIXEL_HEART', '픽셀 하트', 'HOUSE_ICON', 150, NULL),
    ('EMBLEM_ACORN', '도토리', 'HOUSE_ICON', 150, NULL),
    ('EMBLEM_CROSSED_SWORDS', '교차 검', 'HOUSE_ICON', 250, NULL),
    ('EMBLEM_SPEAR', '창', 'HOUSE_ICON', 250, NULL),
    ('EMBLEM_CROSSED_AXES', '교차 도끼', 'HOUSE_ICON', 250, NULL),
    ('EMBLEM_ARCHER', '궁수', 'HOUSE_ICON', 250, NULL),
    ('EMBLEM_ONI_MASK', '도깨비 가면', 'HOUSE_ICON', 250, NULL),
    ('EMBLEM_TRIDENT', '삼지창', 'HOUSE_ICON', 250, NULL),
    ('EMBLEM_HAMMER_ANVIL', '망치와 모루', 'HOUSE_ICON', 250, NULL),
    ('EMBLEM_KNIGHT_SHIELD', '기사 방패', 'HOUSE_ICON', 250, NULL),

    ('CHAT_THEME_MOONLIGHT_LOUNGE', '달빛 라운지', 'CHAT_SKIN', 150, NULL),
    ('CHAT_THEME_CHERRY_GARDEN', '벚꽃 정원', 'CHAT_SKIN', 150, NULL),
    ('CHAT_THEME_GREEN_FOREST', '초록빛 숲', 'CHAT_SKIN', 150, NULL),
    ('CHAT_THEME_PIXEL_ARCADE', '픽셀 아케이드', 'CHAT_SKIN', 250, NULL),
    ('CHAT_THEME_MAGIC_LIBRARY', '마법 서재', 'CHAT_SKIN', 250, NULL),
    ('CHAT_THEME_OCEAN_WALK', '바다 산책', 'CHAT_SKIN', 150, NULL)
ON CONFLICT (code) DO UPDATE
SET name = EXCLUDED.name,
    category = EXCLUDED.category,
    price_hc = EXCLUDED.price_hc,
    image_url = EXCLUDED.image_url;

COMMIT;
