-- ============================================================================
--  로컬 개발 환경 초기 세팅 — 빈 DB 에서 시작하는 사람용
--
--  실행:
--    docker exec -i <컨테이너> psql -U duo -d duo -v ON_ERROR_STOP=1 \
--      < db/migration/local-bootstrap.sql
--
--  [V1__split_schemas.sql 과 뭐가 다른가]
--    V1              이미 데이터가 있는 DB 를 나눈다. 테이블을 옮기고 소유권을 넘긴다.
--    local-bootstrap 빈 DB 에 그릇만 만든다. 테이블은 앱이 뜨면서 만든다.
--
--  새로 합류하는 사람은 옮길 데이터가 없으므로 이 파일 하나면 된다.
--  스키마와 계정만 만들어두면, 각 서비스가 부팅하면서 ddl-auto: update 로
--  자기 스키마 안에 테이블을 알아서 만든다.
--
--  비밀번호는 application-secret-example.yml 의 기본값과 맞춰 두었다.
--  로컬 전용이라 그대로 써도 되고, 바꾸려면 양쪽을 같이 바꾸면 된다.
-- ============================================================================

\set ON_ERROR_STOP on
BEGIN;

-- 서비스별 접속 계정.
-- users 테이블의 회원 계정과는 무관하다. 앱이 PostgreSQL 에 로그인할 때 쓰는 것이다.
CREATE USER duo_user WITH PASSWORD 'local-user-pw';
CREATE USER duo_post WITH PASSWORD 'local-post-pw';
CREATE USER duo_chat WITH PASSWORD 'local-chat-pw';
CREATE USER duo_match WITH PASSWORD 'local-match-pw';
CREATE USER duo_crew WITH PASSWORD 'local-crew-pw';

-- 스키마를 각 계정 소유로 만든다.
--
-- AUTHORIZATION 이 핵심이다. 소유자가 되어야 ddl-auto: update 가 그 안에서
-- 테이블을 만들 수 있다. GRANT 로 읽기·쓰기만 주면 부팅할 때 테이블 생성에서 막힌다.
--
-- 이름에 _svc 를 붙인 이유: user 는 PostgreSQL 예약어다(SELECT user 는 현재
-- 접속 계정을 돌려준다). 셋 다 같은 규칙으로 맞춘다.
CREATE SCHEMA user_svc AUTHORIZATION duo_user;
CREATE SCHEMA post_svc AUTHORIZATION duo_post;
CREATE SCHEMA chat_svc AUTHORIZATION duo_chat;
CREATE SCHEMA match_svc AUTHORIZATION duo_match;
CREATE SCHEMA crew_svc  AUTHORIZATION duo_crew;

-- 접속하면 자기 스키마를 먼저 보게 한다.
-- Hibernate 의 default_schema 는 Hibernate 가 만든 쿼리에만 적용되므로,
-- 네이티브 쿼리까지 덮으려면 이쪽도 걸어둔다.
ALTER ROLE duo_user SET search_path TO user_svc;
ALTER ROLE duo_post SET search_path TO post_svc;
ALTER ROLE duo_chat SET search_path TO chat_svc;
ALTER ROLE duo_match SET search_path TO match_svc;
ALTER ROLE duo_crew  SET search_path TO crew_svc;

-- 새 테이블이 실수로 public 에 만들어지면 경계 밖에 놓인다. 아무도 못 만들게 막는다.
REVOKE CREATE ON SCHEMA public FROM PUBLIC;

COMMIT;

\echo ''
\echo '=== 준비된 스키마와 소유자 ==='
SELECT nspname AS 스키마, pg_get_userbyid(nspowner) AS 소유자
FROM pg_namespace WHERE nspname LIKE '%_svc' ORDER BY 1;

\echo ''
\echo '완료. 이제 user / post / chat / match / crew 를 실행하면 각자 자기 스키마에 테이블을 만든다.'
