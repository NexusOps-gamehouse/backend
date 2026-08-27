-- House 신규회원 표시 계급과 활동 집계.
-- 기존 member_rank NULL 행은 legacy 일반 회원으로 응답한다.

\set ON_ERROR_STOP on
BEGIN;

ALTER TABLE crew_svc.house_members
    ADD COLUMN IF NOT EXISTS member_rank varchar(16);

ALTER TABLE crew_svc.house_members
    ADD COLUMN IF NOT EXISTS game_count integer NOT NULL DEFAULT 0;

ALTER TABLE crew_svc.house_members
    ADD COLUMN IF NOT EXISTS chat_count integer NOT NULL DEFAULT 0;

ALTER TABLE crew_svc.house_members
    ADD CONSTRAINT ck_house_member_game_count_non_negative
    CHECK (game_count >= 0);

ALTER TABLE crew_svc.house_members
    ADD CONSTRAINT ck_house_member_chat_count_non_negative
    CHECK (chat_count >= 0);

COMMIT;
