-- Manual only — do NOT run via Flyway auto-migrate.
-- Drops DQ appeals tables and related columns after the appeals feature is removed.
-- Optional: clean MinIO prefix appeal-evidences/ separately if objects remain.

DROP TABLE IF EXISTS appeal_evidences;
DROP TABLE IF EXISTS appeals;

ALTER TABLE rounds
    DROP COLUMN IF EXISTS appeal_window_ends_at,
    DROP COLUMN IF EXISTS results_revised_at,
    DROP COLUMN IF EXISTS publish_revision,
    DROP COLUMN IF EXISTS appeal_delay_minutes_applied;

ALTER TABLE hackathons
    DROP COLUMN IF EXISTS appeal_window_minutes;
