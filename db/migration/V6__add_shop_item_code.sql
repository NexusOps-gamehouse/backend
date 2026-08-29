-- ShopItem의 숫자 ID와 분리된 영구 식별자 추가.
BEGIN;

ALTER TABLE crew_svc.shop_items
    ADD COLUMN IF NOT EXISTS code VARCHAR(100);

-- V5 seed 상품은 이름으로 식별한다. 같은 이름이 중복된 경우 첫 행만 표준 code를 받고,
-- 나머지 행과 기존 상품은 ID 기반 legacy code를 사용해 UNIQUE 충돌을 피한다.
WITH seed_candidates AS (
    SELECT id, name,
           ROW_NUMBER() OVER (PARTITION BY name ORDER BY id) AS row_number
    FROM crew_svc.shop_items
    WHERE (code IS NULL OR btrim(code) = '')
      AND name IN (
          '기본 프로필 테두리',
          '골드 프로필 배너',
          '크루 전용 휘장',
          '다크 모드 채팅 테마'
      )
)
UPDATE crew_svc.shop_items AS item
SET code = CASE candidates.name
    WHEN '기본 프로필 테두리' THEN 'BASIC_PROFILE_BORDER'
    WHEN '골드 프로필 배너' THEN 'GOLD_PROFILE_BANNER'
    WHEN '크루 전용 휘장' THEN 'CREW_BADGE'
    WHEN '다크 모드 채팅 테마' THEN 'DARK_CHAT_THEME'
    END
FROM seed_candidates AS candidates
WHERE item.id = candidates.id
  AND candidates.row_number = 1;

UPDATE crew_svc.shop_items
SET code = 'LEGACY_ITEM_' || id::text
WHERE code IS NULL OR btrim(code) = '';

ALTER TABLE crew_svc.shop_items
    ALTER COLUMN code SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_shop_items_code
    ON crew_svc.shop_items (code);

COMMIT;
