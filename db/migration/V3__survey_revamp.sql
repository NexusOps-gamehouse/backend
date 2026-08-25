-- ============================================================================
--  FR-01 — 회원 프로필/설문 개편
--
--  ddl-auto: update 는 컬럼을 "추가"만 한다. 지우지도, 이름을 바꾸지도 않는다.
--  그래서 age / play_days / play_duration 과 새 축 컬럼들은 앱이 뜨면서 자동으로
--  생기지만, 아래 두 가지는 손으로 해야 한다.
--    1) 옛 데이터를 새 컬럼으로 옮기기 (age_range → age)
--    2) 더 이상 쓰지 않는 컬럼 지우기 (gender 는 민감정보라 남겨두면 안 된다)
--
--  실행:
--    docker exec -i <postgres 컨테이너> psql -U duo -d duo -v ON_ERROR_STOP=1 \
--      < db/migration/V3__survey_revamp.sql
--
--  ⚠️ 백업 후 실행할 것. 3번 절은 되돌릴 수 없다.
-- ============================================================================

\set ON_ERROR_STOP on
BEGIN;

-- ----------------------------------------------------------------------------
-- 1. 새 컬럼 (앱이 먼저 떴다면 이미 있다 — IF NOT EXISTS 로 양쪽 순서를 모두 허용)
-- ----------------------------------------------------------------------------
ALTER TABLE user_svc.users ADD COLUMN IF NOT EXISTS age           integer;
ALTER TABLE user_svc.users ADD COLUMN IF NOT EXISTS play_days     varchar(255);
ALTER TABLE user_svc.users ADD COLUMN IF NOT EXISTS play_duration varchar(255);

-- ----------------------------------------------------------------------------
-- 2. age_range → age
--
-- 구간을 숫자로 되돌릴 방법은 없다. 대표값(중간값)을 넣으면 "24세"라고 스스로
-- 적은 사람과 구분되지 않는 가짜 데이터가 생긴다. 그래서 옮기지 않고 비워 둔다.
-- 기존 회원은 프로필 수정에서 다시 입력하게 한다. (비어 있는 건 틀린 것보다 낫다)
--
-- 값을 꼭 채워야 한다면 아래 주석을 풀되, 그 값이 추정치임을 어딘가에 남길 것.
--   UPDATE user_svc.users SET age = CASE age_range
--       WHEN '10대' THEN 17 WHEN '20대' THEN 25 WHEN '30대 이상' THEN 35 END
--   WHERE age IS NULL AND age_range IS NOT NULL AND age_range <> '비공개';
-- ----------------------------------------------------------------------------

-- ----------------------------------------------------------------------------
-- 3. 쓰지 않는 컬럼 제거
--
-- gender 는 FR-01 에서 "민감정보라 수집하지 않는다"고 정한 항목이다. 엔티티에서만
-- 지우고 컬럼을 남겨두면 데이터는 그대로 DB 에 있다. 수집을 그만두는 것과
-- 갖고 있던 것을 지우는 것은 다른 작업이고, 여기서 해야 하는 건 후자다.
--
-- position 은 지우지 않는다. 프로필 수정과 파티 화면이 아직 쓴다.
-- (FR-01 은 "가입 설문에서 빼고 매칭/파티 생성 화면으로 옮긴다"까지다)
-- ----------------------------------------------------------------------------
ALTER TABLE user_svc.users DROP COLUMN IF EXISTS gender;
ALTER TABLE user_svc.users DROP COLUMN IF EXISTS age_range;

-- ----------------------------------------------------------------------------
-- 4. 성향 설문 축 교체 (6축 → 7축)
--
-- 옛 축(aggression·competitiveness·flexibility·patience·commitment)은 값이
-- 채워진 적이 없다. 1단계에서 구조만 만들고 환산은 TODO 로 비워뒀기 때문이다.
-- 그래서 옮길 데이터가 없고, 그냥 지우면 된다.
--
-- 원본 응답(answers)은 남는다. 다만 옛 12문항과 새 12문항은 문항이 달라
-- scoring_version = 1 인 행은 새 공식으로 재환산할 수 없다. 그 행들은 응답을
-- 다시 받아야 하므로 점수를 비워 "미응답"으로 되돌린다.
-- ----------------------------------------------------------------------------
ALTER TABLE user_svc.play_style_surveys ADD COLUMN IF NOT EXISTS win_orientation       integer;
ALTER TABLE user_svc.play_style_surveys ADD COLUMN IF NOT EXISTS mistake_tolerance     integer;
ALTER TABLE user_svc.play_style_surveys ADD COLUMN IF NOT EXISTS focus                 integer;
ALTER TABLE user_svc.play_style_surveys ADD COLUMN IF NOT EXISTS initiative            integer;
ALTER TABLE user_svc.play_style_surveys ADD COLUMN IF NOT EXISTS initiative_preference integer;
ALTER TABLE user_svc.play_style_surveys ADD COLUMN IF NOT EXISTS sociality             integer;

ALTER TABLE user_svc.play_style_surveys DROP COLUMN IF EXISTS aggression;
ALTER TABLE user_svc.play_style_surveys DROP COLUMN IF EXISTS competitiveness;
ALTER TABLE user_svc.play_style_surveys DROP COLUMN IF EXISTS flexibility;
ALTER TABLE user_svc.play_style_surveys DROP COLUMN IF EXISTS patience;
ALTER TABLE user_svc.play_style_surveys DROP COLUMN IF EXISTS commitment;

DELETE FROM user_svc.play_style_surveys WHERE scoring_version = 1;

COMMIT;

-- 확인
--   SELECT column_name FROM information_schema.columns
--    WHERE table_schema = 'user_svc' AND table_name = 'users' ORDER BY ordinal_position;
