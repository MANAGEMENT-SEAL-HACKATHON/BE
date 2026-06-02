-- User-role scaffold tables (FR-U-06, FR-U-29, FR-U-30) — chạy THỦ CÔNG.
-- Chỉ CREATE TABLE mới; không ALTER bảng hiện có.

CREATE TABLE IF NOT EXISTS hackathon_registrations (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    hackathon_id INT NOT NULL,
    user_id      INT NOT NULL,
    registered_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_hackathon_reg_user UNIQUE (hackathon_id, user_id),
    CONSTRAINT fk_hr_hackathon FOREIGN KEY (hackathon_id) REFERENCES hackathons(id),
    CONSTRAINT fk_hr_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS appeals (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    team_id       INT NOT NULL,
    round_id      INT NOT NULL,
    submitted_by  INT NOT NULL,
    reason        TEXT NOT NULL,
    evidence_url  TEXT NULL,
    status        VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    created_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_appeal_team FOREIGN KEY (team_id) REFERENCES teams(id),
    CONSTRAINT fk_appeal_round FOREIGN KEY (round_id) REFERENCES rounds(id),
    CONSTRAINT fk_appeal_user FOREIGN KEY (submitted_by) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS certificates (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    user_id       INT NOT NULL,
    hackathon_id  INT NOT NULL,
    file_url      TEXT NULL,
    issued_at     DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_cert_user_hackathon UNIQUE (user_id, hackathon_id),
    CONSTRAINT fk_cert_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_cert_hackathon FOREIGN KEY (hackathon_id) REFERENCES hackathons(id)
);
