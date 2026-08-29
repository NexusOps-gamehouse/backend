-- Crew House activity classification and representative game.
-- This repository does not auto-run db/migration files. Apply manually before
-- starting Crew, for example:
--   psql ... -v ON_ERROR_STOP=1 -f db/migration/V8__add_house_activity_type_and_game.sql
BEGIN;

ALTER TABLE crew_svc.houses
    ADD COLUMN IF NOT EXISTS activity_type VARCHAR(16);

UPDATE crew_svc.houses
SET activity_type = 'SOCIAL'
WHERE activity_type IS NULL;

ALTER TABLE crew_svc.houses
    ALTER COLUMN activity_type SET DEFAULT 'SOCIAL',
    ALTER COLUMN activity_type SET NOT NULL;

ALTER TABLE crew_svc.houses
    ADD COLUMN IF NOT EXISTS representative_game VARCHAR(100);

COMMIT;
