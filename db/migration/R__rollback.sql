-- ============================================================================
--  되돌리기 — 테이블을 public 으로 되돌리고 서비스 계정을 지운다
--
--  실행:
--    docker exec -i <컨테이너> psql -U duo -d duo -v ON_ERROR_STOP=1 \
--      < db/migration/R__rollback.sql
--
--  ⚠️ 지워진 FK 는 되살아나지 않는다. V2 를 실행했다면 FK 는 그대로 없는 상태다.
--     완전한 복구가 필요하면 pg_dump 백업으로 복원할 것.
--
--  ⚠️ 앱 설정(application.yml 의 username · default_schema)도 함께 되돌려야 한다.
-- ============================================================================

\set ON_ERROR_STOP on
BEGIN;

-- 소유권을 원래 계정으로 되돌린다. 안 하면 duo 가 테이블을 못 옮긴다.
DO $$
DECLARE r record;
BEGIN
    FOR r IN SELECT schemaname, tablename FROM pg_tables
             WHERE schemaname IN ('user_svc','post_svc','chat_svc')
    LOOP
        EXECUTE format('ALTER TABLE %I.%I OWNER TO duo', r.schemaname, r.tablename);
    END LOOP;
    FOR r IN SELECT sequence_schema s, sequence_name n FROM information_schema.sequences
             WHERE sequence_schema IN ('user_svc','post_svc','chat_svc')
    LOOP
        EXECUTE format('ALTER SEQUENCE %I.%I OWNER TO duo', r.s, r.n);
    END LOOP;
    FOR r IN SELECT schemaname, tablename FROM pg_tables
             WHERE schemaname IN ('user_svc','post_svc','chat_svc')
    LOOP
        EXECUTE format('ALTER TABLE %I.%I SET SCHEMA public', r.schemaname, r.tablename);
    END LOOP;
END $$;

ALTER SCHEMA user_svc OWNER TO duo;
ALTER SCHEMA post_svc OWNER TO duo;
ALTER SCHEMA chat_svc OWNER TO duo;

DROP SCHEMA IF EXISTS user_svc CASCADE;
DROP SCHEMA IF EXISTS post_svc CASCADE;
DROP SCHEMA IF EXISTS chat_svc CASCADE;

DROP USER IF EXISTS duo_user;
DROP USER IF EXISTS duo_post;
DROP USER IF EXISTS duo_chat;

COMMIT;

\echo ''
\echo '=== 되돌린 뒤 테이블 배치 ==='
SELECT schemaname, count(*) FROM pg_tables
WHERE schemaname NOT IN ('pg_catalog','information_schema')
GROUP BY schemaname;
