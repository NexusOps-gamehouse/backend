-- crew 서비스의 DB 계정 · 스키마 준비.
--
-- ⚠️ 비밀번호가 들어 있지 않다. 실행할 때 -v svc_pw 로 주입한다.
--    ./db/init-crew.sh 를 쓰면 application-secret.yml 에서 읽어 넘겨준다.
--
-- crew 는 아직 backend 레포에 있어서(기능 개발 중) 다른 서비스처럼
-- 자기 레포의 db/init.sh 가 없다. 분리되면 이 파일도 그쪽으로 간다.
--
-- 몇 번을 돌려도 안전하다. 계정이 있으면 비밀번호만 맞춘다.

\set ON_ERROR_STOP on

SELECT 'CREATE ROLE duo_crew LOGIN'
 WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'duo_crew') \gexec

ALTER ROLE duo_crew WITH PASSWORD :'svc_pw';

-- AUTHORIZATION 이 핵심이다. 소유자가 되어야 ddl-auto: update 가 그 안에
-- 테이블을 만들 수 있다. GRANT 만 주면 부팅 중 테이블 생성에서 막힌다.
CREATE SCHEMA IF NOT EXISTS crew_svc AUTHORIZATION duo_crew;
ALTER ROLE duo_crew SET search_path TO crew_svc;

\echo 'crew_svc / duo_crew 준비 완료'
