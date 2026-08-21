-- 상태 확인 전용. 아무것도 바꾸지 않는다. 언제든 다시 돌려도 안전하다.
--   docker exec -i <컨테이너> psql -U duo -d duo < db/migration/check.sql

\echo '=== 1. 스키마별 테이블 배치 (public 이 없어야 정상) ==='
SELECT schemaname AS 스키마, count(*) AS 개수,
       string_agg(tablename, ', ' ORDER BY tablename) AS 테이블
FROM pg_tables
WHERE schemaname IN ('public','user_svc','post_svc','chat_svc')
GROUP BY schemaname ORDER BY schemaname;

\echo ''
\echo '=== 2. FK 목록 (판정이 전부 내부여야 정상) ==='
SELECT c.conname AS 제약조건,
       tn.nspname || '.' || tc.relname AS 출발,
       sn.nspname || '.' || rt.relname AS 도착,
       CASE WHEN tn.nspname = sn.nspname THEN '내부' ELSE '⚠️ 경계넘음' END AS 판정
FROM pg_constraint c
JOIN pg_class     tc ON tc.oid = c.conrelid
JOIN pg_namespace tn ON tn.oid = tc.relnamespace
JOIN pg_class     rt ON rt.oid = c.confrelid
JOIN pg_namespace sn ON sn.oid = rt.relnamespace
WHERE c.contype = 'f'
  AND tn.nspname IN ('user_svc','post_svc','chat_svc')
ORDER BY 4 DESC, 2, 3;

\echo ''
\echo '=== 3. 테이블 소유자 (스키마와 짝이 맞아야 정상) ==='
SELECT schemaname AS 스키마, tableowner AS 소유자, count(*) AS 개수
FROM pg_tables
WHERE schemaname IN ('user_svc','post_svc','chat_svc')
GROUP BY 1,2 ORDER BY 1;

\echo ''
\echo '=== 4. 계정과 search_path ==='
SELECT rolname AS 계정, rolconfig AS 설정
FROM pg_roles WHERE rolname LIKE 'duo%' ORDER BY 1;
