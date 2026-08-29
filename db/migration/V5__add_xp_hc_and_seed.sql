-- 1. DB 영구 Migration: houses 테이블에 xp, hc 컬럼 추가
ALTER TABLE crew_svc.houses ADD COLUMN IF NOT EXISTS xp BIGINT NOT NULL DEFAULT 0;
ALTER TABLE crew_svc.houses ADD COLUMN IF NOT EXISTS hc BIGINT NOT NULL DEFAULT 0;

-- 2. 개발환경 ShopItem seed 추가 (테스트 상품 4종)
INSERT INTO crew_svc.shop_items (name, category, price_hc, image_url)
VALUES ('기본 프로필 테두리', 'BORDER', 100, 'https://example.com/border.png')
    ON CONFLICT DO NOTHING;

INSERT INTO crew_svc.shop_items (name, category, price_hc, image_url)
VALUES ('골드 프로필 배너', 'BANNER', 200, 'https://example.com/banner.png')
    ON CONFLICT DO NOTHING;

INSERT INTO crew_svc.shop_items (name, category, price_hc, image_url)
VALUES ('크루 전용 휘장', 'BADGE', 150, 'https://example.com/badge.png')
    ON CONFLICT DO NOTHING;

INSERT INTO crew_svc.shop_items (name, category, price_hc, image_url)
VALUES ('다크 모드 채팅 테마', 'THEME', 300, 'https://example.com/theme.png')
    ON CONFLICT DO NOTHING;

-- 3. 테스트 House HC 부여 (House 1번에 10,000 HC 주입)
UPDATE crew_svc.houses
SET hc = 10000
WHERE id = 1;