-- Wildcard Plan C: proposal lock + override history
-- Hibernate ddl-auto=update also applies these columns on entity change;
-- run manually when ddl-auto is off.

ALTER TABLE rounds
    ADD COLUMN wildcard_proposal_confirmed_at DATETIME(6) NULL;

ALTER TABLE wildcard_reviews
    ADD COLUMN system_proposed TINYINT(1) NULL,
    ADD COLUMN submitted_at DATETIME(6) NULL,
    ADD COLUMN proposal_rank INT NULL,
    ADD COLUMN override_reason_category VARCHAR(40) NULL,
    ADD COLUMN override_note TEXT NULL,
    ADD COLUMN is_override TINYINT(1) NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS wildcard_override_history (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    round_id        INT NOT NULL,
    review_id       INT NOT NULL,
    team_id         INT NOT NULL,
    category        VARCHAR(40) NOT NULL,
    note            TEXT NULL,
    before_approved TINYINT(1) NULL,
    after_approved  TINYINT(1) NOT NULL,
    by_user_id      INT NULL,
    overridden_at   DATETIME(6) NOT NULL,
    CONSTRAINT fk_woh_round FOREIGN KEY (round_id) REFERENCES rounds (id),
    CONSTRAINT fk_woh_review FOREIGN KEY (review_id) REFERENCES wildcard_reviews (id),
    CONSTRAINT fk_woh_team FOREIGN KEY (team_id) REFERENCES teams (id),
    CONSTRAINT fk_woh_user FOREIGN KEY (by_user_id) REFERENCES users (id),
    INDEX idx_woh_round_at (round_id, overridden_at)
);
