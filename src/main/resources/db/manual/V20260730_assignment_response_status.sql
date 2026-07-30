-- Hibernate ddl-auto=update also applies these on entity change;
-- run manually when ddl-auto is off.
-- CRITICAL: DEFAULT 'ACCEPTED' so existing rows keep working with activate/scoring gates.

ALTER TABLE judge_assignments
    ADD COLUMN response_status VARCHAR(20) NOT NULL DEFAULT 'ACCEPTED',
    ADD COLUMN responded_at DATETIME NULL,
    ADD COLUMN decline_reason VARCHAR(1000) NULL;

ALTER TABLE mentor_assignments
    ADD COLUMN response_status VARCHAR(20) NOT NULL DEFAULT 'ACCEPTED',
    ADD COLUMN responded_at DATETIME NULL,
    ADD COLUMN decline_reason VARCHAR(1000) NULL;

ALTER TABLE mentor_team_assignments
    ADD COLUMN response_status VARCHAR(20) NOT NULL DEFAULT 'ACCEPTED',
    ADD COLUMN responded_at DATETIME NULL,
    ADD COLUMN decline_reason VARCHAR(1000) NULL;
