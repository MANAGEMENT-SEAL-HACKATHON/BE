-- GD03 v4.1 schema delta — tham chiếu thủ công; runtime: Gd03V41SchemaMigration (idempotent).

-- FR-24: publish round results
ALTER TABLE rounds ADD COLUMN is_published TINYINT(1) NOT NULL DEFAULT 0;
ALTER TABLE rounds ADD COLUMN published_at DATETIME(6) NULL;
ALTER TABLE rounds ADD COLUMN published_by BIGINT NULL;

-- FR-16 / BUG-2: submissions
ALTER TABLE submissions ADD COLUMN hackathon_id BIGINT NULL;
-- Backfill trước NOT NULL: round_id từ track; hackathon_id từ round
ALTER TABLE submissions ADD COLUMN scoring_key VARCHAR(40) AS (
    IF(track_id IS NOT NULL, CONCAT('T', track_id), CONCAT('R', round_id))
) STORED;
ALTER TABLE submissions ADD UNIQUE KEY uk_sub_team_scoring (team_id, scoring_key);

-- D-2: participation_status trên team_round_tracks (không team_round_participation)
ALTER TABLE team_round_tracks ADD COLUMN participation_status VARCHAR(20) NOT NULL DEFAULT 'PARTICIPATING';

-- BUG-7: prizes theo hackathon
ALTER TABLE prizes ADD COLUMN hackathon_id BIGINT NULL;

-- FR-17: metadata repo (async)
CREATE TABLE submission_metadata (
    submission_id BIGINT NOT NULL PRIMARY KEY,
    repo_name VARCHAR(255) NULL,
    repo_language VARCHAR(100) NULL,
    repo_last_commit_at DATETIME(6) NULL,
    metadata_fetch_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    fetched_at DATETIME(6) NULL,
    CONSTRAINT fk_submeta_submission FOREIGN KEY (submission_id) REFERENCES submissions(id) ON DELETE CASCADE
) ENGINE=InnoDB;
