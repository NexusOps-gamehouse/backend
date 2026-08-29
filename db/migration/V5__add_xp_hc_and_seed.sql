-- 1. 컬럼 추가
ALTER TABLE crew_svc.houses ADD COLUMN IF NOT EXISTS xp BIGINT NOT NULL DEFAULT 0;
ALTER TABLE crew_svc.houses ADD COLUMN IF NOT EXISTS hc BIGINT NOT NULL DEFAULT 0;

-- 2. 상점 테스트 상품 데이터 (Seed)
INSERT INTO crew_svc.shop_items (name, category, price_hc, image_url)
VALUES ('하우스 테마 A', 'THEME', 100, 'https://example.com/theme_a.png')
    ON CONFLICT DO NOTHING;

INSERT INTO crew_svc.shop_items (name, category, price_hc, image_url)
VALUES ('하우스 깃발', 'DECORATION', 50, 'https://example.com/flag.png')
    ON CONFLICT DO NOTHING;

-- 3. 통합 테스트 데이터
INSERT INTO crew_svc.houses (id, name, description, type, leader_id, max_members, created_at, xp, hc)
VALUES (1, '테스트 하우스', '테스트용 하우스입니다.', 'PUBLIC', 1, 20, NOW(), 0, 10000)
    ON CONFLICT (id) DO UPDATE SET hc = 10000;

INSERT INTO crew_svc.house_members (house_id, user_id, status)
VALUES (1, 1, 'APPROVED')
    ON CONFLICT DO NOTHING;