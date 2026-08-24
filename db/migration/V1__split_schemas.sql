-- ============================================================================
--  3단계 — Schema per Service
--
--  서비스마다 자기 스키마와 DB 계정을 갖게 하고, 남의 테이블에는 접근 자체가
--  거부되도록 만든다. 지금까지 "코드가 지키던 경계"를 DB 가 강제하게 하는 작업이다.
--
--  실행 (컨테이너 안 psql 로):
--    docker exec -i <컨테이너> psql -U duo -d duo -v ON_ERROR_STOP=1 \
--      -v user_pw="'비번1'" -v post_pw="'비번2'" -v chat_pw="'비번3'" \
--      < db/migration/V1__split_schemas.sql
--
--  ⚠️ 반드시 백업 후 실행할 것. DB 상태가 바뀌는 유일한 단계다.
--  ⚠️ 로컬 전용. RDS 는 4단계 배포와 함께.
--
--  riot 은 대상이 아니다 — 소유 테이블이 0개라 DB 를 아예 쓰지 않는다.
-- ============================================================================

\set ON_ERROR_STOP on
BEGIN;

-- ----------------------------------------------------------------------------
-- 1. 스키마 생성
--
-- 이름에 _svc 를 붙인 이유: user 는 PostgreSQL 예약어다(SELECT user 는 현재
-- 접속 계정을 돌려준다). 스키마 이름을 user 로 두면 모든 참조를 "user" 로
-- 따옴표 처리해야 하고, 한 곳만 빠뜨려도 엉뚱하게 동작한다. 셋 다 같은 규칙으로 맞춘다.
-- ----------------------------------------------------------------------------
CREATE SCHEMA IF NOT EXISTS user_svc;
CREATE SCHEMA IF NOT EXISTS post_svc;
CREATE SCHEMA IF NOT EXISTS chat_svc;

-- ----------------------------------------------------------------------------
-- 2. 서비스별 DB 계정
--
-- 여기서 만드는 계정은 애플리케이션이 PostgreSQL 에 접속할 때 쓰는 것이다.
-- users 테이블의 회원 계정과는 아무 관계가 없다.
-- ----------------------------------------------------------------------------
CREATE USER duo_user WITH PASSWORD :user_pw;
CREATE USER duo_post WITH PASSWORD :post_pw;
CREATE USER duo_chat WITH PASSWORD :chat_pw;

-- 접속 계정의 기본 검색 경로를 자기 스키마로 고정한다.
--
-- Hibernate 의 default_schema 만으로는 부족할 수 있다. 그건 Hibernate 가
-- 만드는 쿼리에만 적용되고, 나중에 누가 네이티브 쿼리를 쓰면 빠져나간다.
-- (지금은 네이티브 쿼리가 0개지만, 앞으로 생길 수 있다)
ALTER ROLE duo_user SET search_path TO user_svc;
ALTER ROLE duo_post SET search_path TO post_svc;
ALTER ROLE duo_chat SET search_path TO chat_svc;

-- ----------------------------------------------------------------------------
-- 3. 테이블 이동
--
-- ALTER TABLE ... SET SCHEMA 는 테이블에 딸린 인덱스 · 제약조건 · 시퀀스를
-- 함께 옮긴다. id 컬럼의 시퀀스(bigserial)도 같이 따라가므로 따로 손댈 필요가 없다.
--
-- 데이터는 그대로 유지된다. 폴더만 옮기는 것이다.
-- ----------------------------------------------------------------------------
ALTER TABLE IF EXISTS public.users                   SET SCHEMA user_svc;
ALTER TABLE IF EXISTS public.friends                 SET SCHEMA user_svc;
ALTER TABLE IF EXISTS public.notifications           SET SCHEMA user_svc;
ALTER TABLE IF EXISTS public.user_champion_masteries SET SCHEMA user_svc;
ALTER TABLE IF EXISTS public.user_game_profiles      SET SCHEMA user_svc;
ALTER TABLE IF EXISTS public.play_style_surveys      SET SCHEMA user_svc;

ALTER TABLE IF EXISTS public.posts                   SET SCHEMA post_svc;
ALTER TABLE IF EXISTS public.applications            SET SCHEMA post_svc;
ALTER TABLE IF EXISTS public.post_game_requirements  SET SCHEMA post_svc;

ALTER TABLE IF EXISTS public.chat_rooms              SET SCHEMA chat_svc;
ALTER TABLE IF EXISTS public.chat_room_members       SET SCHEMA chat_svc;
ALTER TABLE IF EXISTS public.chat_messages           SET SCHEMA chat_svc;

-- ----------------------------------------------------------------------------
-- 4. 소유권 이전
--
-- GRANT 로 읽기·쓰기만 주면 부족하다. ddl-auto: update 를 그대로 쓰고 있어서
-- Hibernate 가 부팅할 때마다 컬럼을 추가하거나 인덱스를 만들려 한다.
-- 그건 소유자만 할 수 있다. 그래서 스키마와 테이블의 주인을 넘긴다.
--
-- 소유자가 되면 그 스키마 안에서는 전권을 갖고, 밖에서는 아무 권한도 없다.
-- 정확히 우리가 원하는 경계다.
-- ----------------------------------------------------------------------------
ALTER SCHEMA user_svc OWNER TO duo_user;
ALTER SCHEMA post_svc OWNER TO duo_post;
ALTER SCHEMA chat_svc OWNER TO duo_chat;

DO $$
DECLARE
    r record;
BEGIN
    FOR r IN
        SELECT schemaname, tablename,
               CASE schemaname
                   WHEN 'user_svc' THEN 'duo_user'
                   WHEN 'post_svc' THEN 'duo_post'
                   WHEN 'chat_svc' THEN 'duo_chat'
               END AS owner
        FROM pg_tables
        WHERE schemaname IN ('user_svc', 'post_svc', 'chat_svc')
    LOOP
        EXECUTE format('ALTER TABLE %I.%I OWNER TO %I', r.schemaname, r.tablename, r.owner);
    END LOOP;

    -- 시퀀스는 테이블과 함께 옮겨졌지만 소유자는 따로 지정해야 한다.
    -- 안 하면 INSERT 할 때 nextval 권한이 없어 터진다.
    FOR r IN
        SELECT sequence_schema AS schemaname, sequence_name AS seqname,
               CASE sequence_schema
                   WHEN 'user_svc' THEN 'duo_user'
                   WHEN 'post_svc' THEN 'duo_post'
                   WHEN 'chat_svc' THEN 'duo_chat'
               END AS owner
        FROM information_schema.sequences
        WHERE sequence_schema IN ('user_svc', 'post_svc', 'chat_svc')
    LOOP
        EXECUTE format('ALTER SEQUENCE %I.%I OWNER TO %I', r.schemaname, r.seqname, r.owner);
    END LOOP;
END $$;

-- ----------------------------------------------------------------------------
-- 5. public 스키마 잠그기
--
-- 새 테이블이 실수로 public 에 만들어지면 경계 밖에 놓인다.
-- 아무도 여기에 못 만들게 막아둔다. (PostgreSQL 15+ 는 이미 기본값이다)
-- ----------------------------------------------------------------------------
REVOKE CREATE ON SCHEMA public FROM PUBLIC;

COMMIT;

-- ============================================================================
--  검증
-- ============================================================================
\echo ''
\echo '=== 스키마별 테이블 배치 ==='
SELECT schemaname AS 스키마, count(*) AS 테이블수, string_agg(tablename, ', ' ORDER BY tablename) AS 테이블
FROM pg_tables
WHERE schemaname IN ('public', 'user_svc', 'post_svc', 'chat_svc')
GROUP BY schemaname
ORDER BY schemaname;

\echo ''
\echo '=== 경계를 넘는 물리 FK (여기가 비어 있어야 정상) ==='
SELECT c.conname AS 제약조건,
       tn.nspname || '.' || tc.relname AS 출발,
       sn.nspname || '.' || rt.relname AS 도착
FROM pg_constraint c
JOIN pg_class tc ON tc.oid = c.conrelid
JOIN pg_namespace tn ON tn.oid = tc.relnamespace
JOIN pg_class rt ON rt.oid = c.confrelid
JOIN pg_namespace sn ON sn.oid = rt.relnamespace
WHERE c.contype = 'f'
  AND tn.nspname <> sn.nspname
  AND tn.nspname IN ('user_svc', 'post_svc', 'chat_svc');

\echo ''
\echo '=== 계정과 검색 경로 ==='
SELECT rolname AS 계정, rolconfig AS 설정
FROM pg_roles
WHERE rolname IN ('duo', 'duo_user', 'duo_post', 'duo_chat')
ORDER BY rolname;
