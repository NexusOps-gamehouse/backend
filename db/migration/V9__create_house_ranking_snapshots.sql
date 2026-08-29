-- Current policy compares the current ranking with a KST week-start baseline,
-- not with the final ranking of the previous week.
BEGIN;

CREATE TABLE IF NOT EXISTS crew_svc.house_ranking_snapshot_batches (
    week_id VARCHAR(16) PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    snapshot_complete BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS crew_svc.house_ranking_snapshots (
    id BIGSERIAL PRIMARY KEY,
    week_id VARCHAR(16) NOT NULL,
    house_id BIGINT NOT NULL,
    baseline_rank INTEGER NOT NULL CHECK (baseline_rank > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    snapshot_complete BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_house_ranking_snapshot_week_house UNIQUE (week_id, house_id),
    CONSTRAINT fk_house_ranking_snapshot_house
        FOREIGN KEY (house_id) REFERENCES crew_svc.houses(id)
);

CREATE INDEX IF NOT EXISTS idx_house_ranking_snapshot_week
    ON crew_svc.house_ranking_snapshots (week_id);

INSERT INTO crew_svc.house_ranking_snapshot_batches (week_id, created_at, snapshot_complete)
SELECT week_id, MIN(created_at), BOOL_AND(snapshot_complete)
FROM crew_svc.house_ranking_snapshots
GROUP BY week_id
ON CONFLICT (week_id) DO NOTHING;

COMMIT;
