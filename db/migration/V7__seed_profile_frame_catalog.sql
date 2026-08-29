-- 실제 프론트엔드 프로필 테두리 23종 catalog seed.
-- 상품 ID는 지정하지 않고 영구 code를 충돌 기준으로 사용한다.
BEGIN;

INSERT INTO crew_svc.shop_items (code, name, category, price_hc, image_url)
VALUES
    ('FRAME_ADVENTURERS_SPRING', '모험자의 봄', 'BORDER', 0, NULL),
    ('FRAME_FOREST_FRIEND', '숲 속 친구', 'BORDER', 0, NULL),
    ('FRAME_SHINING_BUTTER', '샤이닝 버터', 'BORDER', 0, NULL),
    ('FRAME_NIGHT_FIREFLY', '밤빛 반디', 'BORDER', 0, NULL),
    ('FRAME_APPRENTICE_MAGE', '견습 마법사', 'BORDER', 0, NULL),
    ('FRAME_AMETHYST', '자수정', 'BORDER', 0, NULL),
    ('FRAME_INK_WASH', '수묵화', 'BORDER', 0, NULL),
    ('FRAME_LANTERN', '연등', 'BORDER', 150, NULL),
    ('FRAME_COIN_RUSH', '코인 러시', 'BORDER', 0, NULL),
    ('FRAME_MINI_ARCADE', '미니 아케이드', 'BORDER', 0, NULL),
    ('FRAME_SPACE', '스페이스', 'BORDER', 150, NULL),
    ('FRAME_SUNSET_MATCH', '선셋 매치', 'BORDER', 250, NULL),
    ('FRAME_STARLIGHT_GALAXY', '별빛 은하', 'BORDER', 250, NULL),
    ('FRAME_CHERRY_BLOSSOM', '체리 블러썸', 'BORDER', 250, NULL),
    ('FRAME_FLAME_PLAY', '플레임 플레이', 'BORDER', 150, NULL),
    ('FRAME_DICE_PARTY', '다이스 파티', 'BORDER', 250, NULL),
    ('FRAME_ROYAL_HOUSE', '로열 하우스', 'BORDER', 250, NULL),
    ('FRAME_MOON_DREAM', '달의 꿈', 'BORDER', 250, NULL),
    ('FRAME_OLYMPIA', '올림피아', 'BORDER', 250, NULL),
    ('FRAME_OCEAN_WAVE', '오션 웨이브', 'BORDER', 250, NULL),
    ('FRAME_SNOW_TOWN', '스노우 타운', 'BORDER', 250, NULL),
    ('FRAME_ARCADE_PIXEL', '아케이드 픽셀', 'BORDER', 150, NULL),
    ('FRAME_MUSIC_NIGHT', '뮤직 나이트', 'BORDER', 250, NULL)
ON CONFLICT (code) DO UPDATE
SET name = EXCLUDED.name,
    category = EXCLUDED.category,
    price_hc = EXCLUDED.price_hc,
    image_url = COALESCE(EXCLUDED.image_url, shop_items.image_url);

COMMIT;
