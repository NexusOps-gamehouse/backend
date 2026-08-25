-- ============================================================================
--  경계를 넘는 물리 FK 제거
--
--  V1 을 먼저 실행한 뒤에 돌린다.
--
--  [왜 지우나]
--  JPA 연관은 1단계에서 끊었다. Post.author 는 이제 Long authorId 다.
--  하지만 DB 에는 posts.author_id → users.id 라는 제약조건이 그대로 남아 있을 수
--  있다. Hibernate 의 ddl-auto: update 는 컬럼을 추가할 뿐 제약조건을 지우지 않는다.
--
--  이게 남아 있으면 두 테이블이 여전히 붙어 있다.
--    - user 서비스가 회원을 삭제하려면 posts 를 먼저 정리해야 한다
--    - 스키마를 나눠도 DB 는 둘을 한 덩어리로 본다
--
--  [로컬은 아마 이미 비어 있다]
--  빈 DB 에 새 엔티티(FK 없는 코드)로 테이블이 만들어졌다면 애초에 안 생겼다.
--  RDS 처럼 옛 스키마가 살아 있는 곳에서 의미가 있다.
--
--  [대신 무엇이 지켜주나]
--  아무것도 안 지켜준다. 그게 MSA 의 비용이다.
--  없는 사용자의 글이 남을 수 있고, 그건 이벤트(UserDeletedEvent 등)로 정리해야 한다.
--  DB 한 대가 대신 해주던 일을 이제 서비스가 명시적으로 해야 한다.
-- ============================================================================

\set ON_ERROR_STOP on
BEGIN;

DO $$
DECLARE
    r record;
    n int := 0;
BEGIN
    FOR r IN
        SELECT c.conname,
               tn.nspname AS from_schema, tc.relname AS from_table,
               sn.nspname AS to_schema,   rt.relname AS to_table
        FROM pg_constraint c
        JOIN pg_class tc     ON tc.oid = c.conrelid
        JOIN pg_namespace tn ON tn.oid = tc.relnamespace
        JOIN pg_class rt     ON rt.oid = c.confrelid
        JOIN pg_namespace sn ON sn.oid = rt.relnamespace
        WHERE c.contype = 'f'
          AND tn.nspname IN ('user_svc', 'post_svc', 'chat_svc')
          AND sn.nspname IN ('user_svc', 'post_svc', 'chat_svc')
          AND tn.nspname <> sn.nspname          -- 경계를 넘는 것만
    LOOP
        RAISE NOTICE '삭제: % (%.% → %.%)',
            r.conname, r.from_schema, r.from_table, r.to_schema, r.to_table;
        EXECUTE format('ALTER TABLE %I.%I DROP CONSTRAINT %I',
                       r.from_schema, r.from_table, r.conname);
        n := n + 1;
    END LOOP;

    IF n = 0 THEN
        RAISE NOTICE '경계를 넘는 FK 가 없다. 지울 것 없음.';
    ELSE
        RAISE NOTICE '총 % 개 삭제.', n;
    END IF;
END $$;

COMMIT;

-- 같은 서비스 안의 FK 는 그대로 둔다. 경계를 안 넘으니까.
--   post_svc.applications.post_id  → post_svc.posts        유지
--   chat_svc.chat_room_members.room_id → chat_svc.chat_rooms 유지
--   chat_svc.chat_messages.room_id     → chat_svc.chat_rooms 유지
--   user_svc.friends / notifications / user_champion_masteries → user_svc.users 유지
\echo ''
\echo '=== 남은 FK (전부 같은 스키마 안이어야 정상) ==='
SELECT tn.nspname || '.' || tc.relname AS 출발,
       sn.nspname || '.' || rt.relname AS 도착,
       CASE WHEN tn.nspname = sn.nspname THEN '내부' ELSE '⚠️ 경계넘음' END AS 판정
FROM pg_constraint c
JOIN pg_class tc     ON tc.oid = c.conrelid
JOIN pg_namespace tn ON tn.oid = tc.relnamespace
JOIN pg_class rt     ON rt.oid = c.confrelid
JOIN pg_namespace sn ON sn.oid = rt.relnamespace
WHERE c.contype = 'f'
  AND tn.nspname IN ('user_svc', 'post_svc', 'chat_svc')
ORDER BY 1, 2;
