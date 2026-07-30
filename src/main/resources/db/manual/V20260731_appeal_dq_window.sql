-- Hibernate ddl-auto=update also applies these on entity change;
-- run manually when ddl-auto is off.

ALTER TABLE hackathons
    ADD COLUMN appeal_window_minutes INT NOT NULL DEFAULT 30;

ALTER TABLE rounds
    ADD COLUMN appeal_window_ends_at DATETIME NULL,
    ADD COLUMN results_revised_at DATETIME NULL,
    ADD COLUMN publish_revision INT NOT NULL DEFAULT 1,
    ADD COLUMN appeal_delay_minutes_applied INT NOT NULL DEFAULT 0;

ALTER TABLE appeals
    ADD COLUMN reviewed_by INT NULL,
    ADD COLUMN reviewed_at DATETIME NULL,
    ADD COLUMN decision_note TEXT NULL,
    ADD COLUMN updated_at DATETIME NULL,
    ADD COLUMN version INT NOT NULL DEFAULT 0;

ALTER TABLE appeals
    ADD CONSTRAINT fk_appeals_reviewed_by FOREIGN KEY (reviewed_by) REFERENCES users (id);

ALTER TABLE appeals
    ADD CONSTRAINT uq_appeals_team_round UNIQUE (team_id, round_id);

CREATE TABLE IF NOT EXISTS appeal_evidences (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    appeal_id     INT          NOT NULL,
    url           TEXT         NOT NULL,
    type          VARCHAR(20)  NOT NULL,
    caption       VARCHAR(500) NULL,
    display_order INT          NOT NULL DEFAULT 0,
    CONSTRAINT fk_appeal_evidences_appeal FOREIGN KEY (appeal_id) REFERENCES appeals (id)
);
