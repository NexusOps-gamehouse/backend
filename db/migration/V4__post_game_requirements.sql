-- ============================================================================
--  FR-02 — 모집글 작성 개편 (게임별 조건)
--
--  세 가지를 한다.
--    1) posts.game 을 표시명("리그오브레전드")에서 코드("LOL")로 바꾼다
--    2) mic_required(boolean) → voice_chat(3단계)
--    3) 게임별 조건을 post_game_requirements 로 옮긴다
--
--  실행:
--    docker exec -i <postgres 컨테이너> psql -U duo -d duo -v ON_ERROR_STOP=1 \
--      < db/migration/V4__post_game_requirements.sql
--
--  ⚠️ 백업 후 실행할 것. 4·5번 절은 되돌릴 수 없다.
--  ⚠️ V3 와 마찬가지로 ddl-auto: update 는 컬럼을 추가만 한다. 드롭은 여기서 한다.
-- ============================================================================

\set ON_ERROR_STOP on
BEGIN;

-- ----------------------------------------------------------------------------
-- 1. 새 컬럼 (앱이 먼저 떴다면 이미 있다)
-- ----------------------------------------------------------------------------
ALTER TABLE post_svc.posts ADD COLUMN IF NOT EXISTS voice_chat varchar(16);

ALTER TABLE post_svc.post_game_requirements ADD COLUMN IF NOT EXISTS roles      varchar(255);
ALTER TABLE post_svc.post_game_requirements ADD COLUMN IF NOT EXISTS tier       varchar(255);
ALTER TABLE post_svc.post_game_requirements ADD COLUMN IF NOT EXISTS play_style varchar(255);

-- ----------------------------------------------------------------------------
-- 2. game: 표시명 → GameCode
--
-- 표시명을 그대로 저장하던 시절의 값들이다. 표시명은 바뀔 수 있고, 바뀌면
-- 이미 저장된 글이 필터에서 조용히 사라진다. 코드로 고정한다.
--
-- '기타'는 대응하는 코드가 없다. FR-02 표가 LOL·VALORANT 만 정의하고 있어
-- 게임 칩에서도 뺐다. 기존 '기타' 글은 게임 없음(NULL)으로 남긴다 —
-- 지우지 않는 이유는 본문과 신청 내역이 그대로 살아 있기 때문이다.
-- ----------------------------------------------------------------------------
UPDATE post_svc.posts SET game = 'LOL'
 WHERE game IN ('리그오브레전드', '리그 오브 레전드', 'LoL', 'lol');

UPDATE post_svc.posts SET game = 'VALORANT'
 WHERE game IN ('발로란트', 'Valorant', 'valorant');

UPDATE post_svc.posts SET game = NULL
 WHERE game IS NOT NULL AND game NOT IN ('LOL', 'VALORANT');

-- ----------------------------------------------------------------------------
-- 3. mic_required → voice_chat
--
-- boolean 에는 '있으면 좋음'에 해당하는 값이 없었다. 없던 정보를 지어내지 않고
-- true → REQUIRED, false → ANY 로만 옮긴다.
-- ----------------------------------------------------------------------------
UPDATE post_svc.posts
   SET voice_chat = CASE WHEN mic_required THEN 'REQUIRED' ELSE 'ANY' END
 WHERE voice_chat IS NULL;

UPDATE post_svc.posts SET voice_chat = 'ANY' WHERE voice_chat IS NULL;
ALTER TABLE post_svc.posts ALTER COLUMN voice_chat SET NOT NULL;

-- ----------------------------------------------------------------------------
-- 4. 게임별 조건 행 백필
--
-- 기존 글에는 posts.positions(찾는 포지션)만 있었다. 게임이 정해진 글에 한해
-- 조건 행을 만들어 옮긴다. 티어·플레이스타일은 받은 적이 없으므로 비워 둔다.
-- (없는 값을 추측해 채우면 그 글이 엉뚱한 추천에 걸린다)
--
-- 발로란트 글의 positions 에는 롤 포지션이 들어 있을 수 있다. 그 게임에 없는
-- 값이라 옮기지 않는다 — 옮기면 어떤 추천에도 안 걸리는 조건이 된다.
-- ----------------------------------------------------------------------------
INSERT INTO post_svc.post_game_requirements (post_id, game_code, roles)
SELECT p.id,
       p.game,
       CASE WHEN p.game = 'LOL' AND p.positions IS NOT NULL AND p.positions <> ''
            THEN NULLIF(p.positions, '상관없음')
            ELSE NULL END
  FROM post_svc.posts p
 WHERE p.game IN ('LOL', 'VALORANT')
   AND NOT EXISTS (SELECT 1 FROM post_svc.post_game_requirements r WHERE r.post_id = p.id);

-- ----------------------------------------------------------------------------
-- 5. 쓰지 않는 컬럼 제거
--
-- post_game_requirements 의 옛 컬럼들(min_tier/max_tier/game_modes/mic_required)은
-- 어떤 코드도 채운 적이 없다. 구조만 만들어두고 쓰이지 않던 테이블이라 데이터가 없다.
-- ----------------------------------------------------------------------------
ALTER TABLE post_svc.posts DROP COLUMN IF EXISTS mic_required;
ALTER TABLE post_svc.posts DROP COLUMN IF EXISTS positions;

ALTER TABLE post_svc.post_game_requirements DROP COLUMN IF EXISTS positions;
ALTER TABLE post_svc.post_game_requirements DROP COLUMN IF EXISTS min_tier;
ALTER TABLE post_svc.post_game_requirements DROP COLUMN IF EXISTS max_tier;
ALTER TABLE post_svc.post_game_requirements DROP COLUMN IF EXISTS game_modes;
ALTER TABLE post_svc.post_game_requirements DROP COLUMN IF EXISTS mic_required;

-- 글당 조건 한 행. 중복이 생기면 목록과 상세가 다른 조건을 보여준다.
CREATE UNIQUE INDEX IF NOT EXISTS ux_post_game_requirements_post
    ON post_svc.post_game_requirements (post_id);

COMMIT;

-- 확인
--   SELECT game, voice_chat, count(*) FROM post_svc.posts GROUP BY 1, 2;
--   SELECT * FROM post_svc.post_game_requirements LIMIT 20;
