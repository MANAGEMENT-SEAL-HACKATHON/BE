# SEAL Hackathon — Database Schema v3.0 (MySQL 8)

> **Source of truth** cho toàn bộ tầng dữ liệu của SEAL Hackathon Management System.
> File này được port 1-1 từ PostgreSQL v3.0 (kiến trúc `Hackathon → Round → Track`) sang **MySQL 8**.
> Mọi entity JPA, repository, schema migration phải đồng bộ với file này.

- **DB engine**: MySQL 8.0+ (InnoDB, utf8mb4)
- **Datasource**: `jdbc:mysql://localhost:3306/SealHackathon`
- **Ngày phát hành**: 2026-05
- **Phiên bản**: 3.0 (Breaking Change so với v2.1)

---

## 0. CHANGELOG v2.1 → v3.0 — 11 Breaking Changes

| BC | Bảng | Tóm tắt |
|----|------|---------|
| **BC-01** | `rounds` | FK đảo: `track_id → tracks` thay bằng `hackathon_id → hackathons`. Thêm 3 cột nghiệp vụ: `is_final`, `round_type`, `late_submission_policy`. Thêm `UNIQUE(hackathon_id, sequence_order)` + CHECK nhất quán `is_final ↔ round_type`. |
| **BC-02** | `tracks` | FK đảo: `hackathon_id → hackathons` thay bằng `round_id → rounds`. Thêm `topic`, `sequence_order` + `UNIQUE(round_id, sequence_order)`. Giữ `max_teams`, `max_teams_per_group`. |
| **BC-03** | `criteria` | XOR FK: `track_id` (Sơ loại) **hoặc** `round_id` (Chung kết) — đúng 1 trong 2 NOT NULL. Enforce bằng CHECK + trigger. |
| **BC-04** | `team_round_tracks` | **Bảng mới** thay 3 cột cũ trong `teams`. Phân biệt `registration_type ∈ {PREFERRED, ASSIGNED}` cho 2 mùa. |
| **BC-05** | `teams` | Bỏ `registration_track_id`, `assigned_track_id`, `assigned_group`. Thêm `hackathon_id` trực tiếp. |
| **BC-06** | `submissions` | XOR FK: `track_id` (Sơ loại) hoặc `round_id` (Chung kết) + partial UNIQUE (mô phỏng bằng generated column trên MySQL). Status thêm `LATE_APPROVED`. |
| **BC-07** | `judge_assignments` | XOR FK như criteria. Thêm `FINAL_EXTERNAL` vào `assignment_type` (bắt buộc ở Chung kết). |
| **BC-08** | `mentor_assignments` | Giữ nguyên — `track_id` tự mang ngữ cảnh Round qua FK mới. |
| **BC-09** | `events` | Bỏ `TEAM_MEETING` khỏi enum `type`. |
| **BC-10** | `wildcard_reviews` | Thêm `track_id` để biết Wild Card đề xuất từ Track nào. |
| **BC-11** | Triggers | 3 trigger DB-layer mới: `fn_check_mentor_judge_conflict`, `fn_check_judge_mentor_conflict`, `fn_prevent_track_in_final_round` + các trigger guard mới (`fn_check_submission_round_is_final`, `fn_check_criteria_round_is_final`, `fn_check_team_track_same_hackathon`). |

---

## 1. Mapping PostgreSQL → MySQL (Cheat Sheet)

| PostgreSQL | MySQL 8 | Ghi chú |
|---|---|---|
| `SERIAL PRIMARY KEY` | `INT NOT NULL AUTO_INCREMENT PRIMARY KEY` | |
| `BIGSERIAL` | `BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY` | |
| `TIMESTAMP` | `DATETIME` | TIMESTAMP MySQL chỉ tới 2038-01-19 |
| `TIMESTAMP NOT NULL DEFAULT NOW()` | `DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP` | |
| `JSONB` | `JSON` | MySQL JSON tự validate, lưu binary |
| `INET` | `VARCHAR(45)` | đủ cho IPv6 |
| `BOOLEAN` | `BOOLEAN` (alias `TINYINT(1)`) | |
| `TEXT` | `TEXT` / `LONGTEXT` | |
| `CHECK (col IN (...))` | `CHECK (col IN (...))` | MySQL 8 enforce thực sự |
| `CHECK (...)` cross-column | `CHECK (...)` | MySQL 8 hỗ trợ |
| `crypt(pwd, gen_salt('bf'))` | bỏ → app dùng BCrypt | |
| `CREATE EXTENSION` | bỏ | |
| `CREATE OR REPLACE VIEW` | `CREATE OR REPLACE VIEW` | hỗ trợ |
| `RAISE EXCEPTION 'msg'` | `SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'msg'` | |
| `jsonb_build_object(...)` | `JSON_OBJECT(...)` | |
| **Partial UNIQUE INDEX** `WHERE` | **Generated column + UNIQUE** | xem mục [Generated UNIQUE patterns](#11-generated-unique-patterns) |
| `RETURNS TRIGGER AS $$ ... $$ LANGUAGE plpgsql` | `CREATE TRIGGER ... BEGIN ... END` | MySQL trigger không tách function/trigger |
| `COALESCE`, `NULLIF`, `STDDEV`, `AVG`, `ROUND` | giữ nguyên | |
| `RANK() OVER (PARTITION BY ...)` | giữ nguyên | MySQL 8+ window function |

### 1.1 Generated UNIQUE patterns (mô phỏng partial unique index)

MySQL **không** hỗ trợ `CREATE UNIQUE INDEX ... WHERE`. Để mô phỏng:

```sql
-- Pattern: 1 đội chỉ nộp 1 bài / track HOẶC 1 bài / round chung kết
CREATE TABLE submissions (
    id        INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    team_id   INT NOT NULL,
    track_id  INT NULL,
    round_id  INT NULL,
    -- VIRTUAL generated column: chỉ có giá trị khi track_id NOT NULL
    track_uk  INT GENERATED ALWAYS AS (track_id) VIRTUAL,
    -- VIRTUAL generated column: chỉ có giá trị khi track_id NULL (Chung kết)
    round_uk  INT GENERATED ALWAYS AS (
                  CASE WHEN track_id IS NULL THEN round_id END
              ) VIRTUAL,
    ...
    UNIQUE KEY uk_subm_team_track       (team_id, track_uk),
    UNIQUE KEY uk_subm_team_final_round (team_id, round_uk)
);
```

`NULL` không tham gia UNIQUE check trong MySQL — nên 2 dòng cùng `(team_id, NULL)` vẫn được phép, đáp ứng đúng semantic của partial unique index.

---

## 2. DDL — Toàn bộ 27 bảng

```sql
-- ============================================================
-- SEAL HACKATHON MANAGEMENT SYSTEM — SCHEMA v3.0 (MySQL 8)
-- Kiến trúc: Hackathon → Round → Track
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- NHÓM 1: NGƯỜI DÙNG & PHÂN QUYỀN
-- ============================================================

CREATE TABLE chapters (
    id         INT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(200) NOT NULL,
    code       VARCHAR(20)  NOT NULL UNIQUE,
    university VARCHAR(300),
    city       VARCHAR(100),
    status     VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_chapters_status CHECK (status IN ('ACTIVE','INACTIVE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE users (
    id                INT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    full_name         VARCHAR(200) NOT NULL,
    email             VARCHAR(320) NOT NULL UNIQUE,
    password_hash     VARCHAR(255),
    role              VARCHAR(20)  NOT NULL,
    user_type         VARCHAR(20)  NOT NULL,
    student_code      VARCHAR(50),
    is_temp_account   BOOLEAN      NOT NULL DEFAULT FALSE,
    is_dept_head      BOOLEAN      NOT NULL DEFAULT FALSE,                 -- FIX-02
    status            VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    rejection_reason  TEXT,
    chapter_id        INT,
    phone             VARCHAR(30),
    avatar_url        TEXT,
    institution       VARCHAR(300),
    email_verified_at DATETIME,
    last_login_at     DATETIME,
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT chk_users_role     CHECK (role      IN ('COORDINATOR','JUDGE','MENTOR','STUDENT')),
    CONSTRAINT chk_users_type     CHECK (user_type IN ('INTERNAL','EXTERNAL')),
    CONSTRAINT chk_users_status   CHECK (status    IN ('PENDING','APPROVED','REJECTED')),
    CONSTRAINT fk_users_chapter   FOREIGN KEY (chapter_id) REFERENCES chapters(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE oauth_accounts (
    id            INT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id       INT          NOT NULL,
    provider      VARCHAR(30)  NOT NULL,
    provider_uid  VARCHAR(255) NOT NULL,
    access_token  TEXT,
    refresh_token TEXT,
    expires_at    DATETIME,
    UNIQUE KEY uk_oauth_provider (provider, provider_uid),
    CONSTRAINT fk_oauth_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE user_sessions (
    id         INT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id    INT          NOT NULL,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    ip_address VARCHAR(45),
    user_agent TEXT,
    expires_at DATETIME     NOT NULL,
    revoked_at DATETIME,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_session_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE invitations (
    id           INT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    email        VARCHAR(320) NOT NULL,
    role         VARCHAR(20)  NOT NULL,
    hackathon_id INT,
    invited_by   INT,
    token        VARCHAR(128) NOT NULL UNIQUE,
    expires_at   DATETIME     NOT NULL,
    accepted_at  DATETIME,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_inv_invited_by FOREIGN KEY (invited_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ============================================================
-- NHÓM 2: HACKATHON → ROUND → TRACK
-- ============================================================

CREATE TABLE hackathons (
    id                         INT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name                       VARCHAR(300) NOT NULL,
    slug                       VARCHAR(150) NOT NULL UNIQUE,
    season                     VARCHAR(20)  NOT NULL,
    `year`                     INT          NOT NULL,
    status                     VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    description                TEXT,
    rules                      TEXT,
    banner_url                 TEXT,
    registration_start         DATE,
    registration_end           DATE,
    event_start                DATE,
    event_end                  DATE,
    wildcard_enabled           BOOLEAN      NOT NULL DEFAULT FALSE,
    individual_ranking_enabled BOOLEAN      NOT NULL DEFAULT FALSE,
    chapter_scoring_formula    TEXT,
    created_by                 INT,
    created_at                 DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                 DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_hackathons_name_season_year (name, season, `year`),
    CONSTRAINT chk_hackathons_season CHECK (season IN ('Spring','Summer','Fall','Winter')),
    CONSTRAINT chk_hackathons_status CHECK (status IN ('DRAFT','ONGOING','PENDING_CONFIRM','FINISHED')),
    CONSTRAINT fk_hackathons_creator FOREIGN KEY (created_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- [BC-01] rounds — CON TRỰC TIẾP của hackathon (đảo FK)
CREATE TABLE rounds (
    id                      INT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    hackathon_id            INT          NOT NULL,
    name                    VARCHAR(100) NOT NULL,
    sequence_order          INT          NOT NULL,
    is_final                BOOLEAN      NOT NULL DEFAULT FALSE,
    round_type              VARCHAR(20)  NOT NULL DEFAULT 'PRELIMINARY',
    coding_duration_hours   INT,
    submission_open         DATETIME,
    submission_deadline     DATETIME     NOT NULL,
    late_submission_policy  VARCHAR(20)  NOT NULL DEFAULT 'ALLOW_LATE_PENDING',
    problem_statement_url   TEXT,
    problem_released_at     DATETIME,
    top_n_advance           INT,
    min_teams_final         INT,
    wildcard_enabled        BOOLEAN      NOT NULL DEFAULT FALSE,
    tiebreak_rule           VARCHAR(50)  DEFAULT 'PENALTY_SCORE',
    is_active               BOOLEAN      NOT NULL DEFAULT FALSE,
    scoring_locked          BOOLEAN      NOT NULL DEFAULT FALSE,
    scoring_locked_at       DATETIME,
    scoring_locked_by       INT,
    force_locked            BOOLEAN      NOT NULL DEFAULT FALSE,
    force_lock_reason       TEXT,
    created_at              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_rounds_hackathon_sequence (hackathon_id, sequence_order),
    CONSTRAINT chk_rounds_round_type    CHECK (round_type IN ('PRELIMINARY','SEMIFINAL','FINAL')),
    CONSTRAINT chk_rounds_late_policy   CHECK (late_submission_policy IN ('ALLOW_LATE_PENDING','HARD_LOCK')),
    CONSTRAINT chk_rounds_tiebreak_rule CHECK (tiebreak_rule IN ('PENALTY_SCORE','SUBMISSION_TIME','COORDINATOR_DECISION')),
    CONSTRAINT chk_rounds_final_consistent CHECK (
        (is_final = TRUE  AND round_type = 'FINAL')
        OR
        (is_final = FALSE AND round_type IN ('PRELIMINARY','SEMIFINAL'))
    ),
    CONSTRAINT fk_rounds_hackathon FOREIGN KEY (hackathon_id) REFERENCES hackathons(id) ON DELETE CASCADE,
    CONSTRAINT fk_rounds_locker    FOREIGN KEY (scoring_locked_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- [BC-02] tracks — CON của round (đảo FK)
CREATE TABLE tracks (
    id                  INT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    round_id            INT          NOT NULL,
    name                VARCHAR(200) NOT NULL,
    description         TEXT,
    topic               VARCHAR(300),
    max_teams           INT,
    max_teams_per_group INT,
    min_team_size       INT          NOT NULL DEFAULT 3,
    max_team_size       INT          NOT NULL DEFAULT 5,
    status              VARCHAR(20)  NOT NULL DEFAULT 'OPEN',
    sequence_order      INT          NOT NULL DEFAULT 1,
    UNIQUE KEY uk_tracks_round_sequence (round_id, sequence_order),
    CONSTRAINT chk_tracks_status CHECK (status IN ('OPEN','CLOSED','CANCELLED')),
    CONSTRAINT fk_tracks_round   FOREIGN KEY (round_id) REFERENCES rounds(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- [BC-03] criteria — XOR FK
CREATE TABLE criteria (
    id                 INT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    track_id           INT NULL,
    round_id           INT NULL,
    source_criteria_id INT NULL,
    name               VARCHAR(200) NOT NULL,
    type               VARCHAR(20)  NOT NULL,
    weight             FLOAT        NOT NULL,
    max_score          INT          NOT NULL DEFAULT 10,
    description        TEXT,
    rubric_url         TEXT,
    display_order      INT          NOT NULL DEFAULT 0,
    CONSTRAINT chk_criteria_type   CHECK (type IN ('TECHNICAL','SOFT_SKILL','PENALTY')),
    CONSTRAINT chk_criteria_weight CHECK (weight > 0 AND weight <= 1),
    -- BC-03 XOR constraint
    CONSTRAINT chk_criteria_xor_fk CHECK (
        (track_id IS NOT NULL AND round_id IS NULL)
        OR
        (track_id IS NULL AND round_id IS NOT NULL)
    ),
    CONSTRAINT fk_criteria_track  FOREIGN KEY (track_id) REFERENCES tracks(id) ON DELETE CASCADE,
    CONSTRAINT fk_criteria_round  FOREIGN KEY (round_id) REFERENCES rounds(id) ON DELETE CASCADE,
    CONSTRAINT fk_criteria_source FOREIGN KEY (source_criteria_id) REFERENCES criteria(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ============================================================
-- NHÓM 3: ĐỘI THI & THÀNH VIÊN
-- ============================================================

-- [BC-05] teams — bỏ 3 cột cũ, thêm hackathon_id
CREATE TABLE teams (
    id                 INT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    hackathon_id       INT          NOT NULL,
    team_name          VARCHAR(200) NOT NULL UNIQUE,
    leader_id          INT          NOT NULL,
    chapter_id         INT,
    status             VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    rejection_reason   TEXT,
    is_locked          BOOLEAN      NOT NULL DEFAULT FALSE,
    locked_at          DATETIME,
    eliminated_at      DATETIME,
    elimination_reason TEXT,
    created_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_teams_status CHECK (status IN ('PENDING','ACTIVE','ELIMINATED','REJECTED')),
    CONSTRAINT fk_teams_hackathon FOREIGN KEY (hackathon_id) REFERENCES hackathons(id) ON DELETE CASCADE,
    CONSTRAINT fk_teams_leader    FOREIGN KEY (leader_id)    REFERENCES users(id),
    CONSTRAINT fk_teams_chapter   FOREIGN KEY (chapter_id)   REFERENCES chapters(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE team_members (
    team_id      INT         NOT NULL,
    user_id      INT         NOT NULL,
    role_in_team VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
    status       VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    joined_at    DATETIME,
    left_at      DATETIME,
    PRIMARY KEY (team_id, user_id),
    CONSTRAINT chk_tm_role   CHECK (role_in_team IN ('LEADER','MEMBER')),
    CONSTRAINT chk_tm_status CHECK (status IN ('PENDING','ACCEPTED','REJECTED','LEFT')),
    CONSTRAINT fk_tm_team FOREIGN KEY (team_id) REFERENCES teams(id) ON DELETE CASCADE,
    CONSTRAINT fk_tm_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- [BC-04] team_round_tracks — bảng mới
CREATE TABLE team_round_tracks (
    id                INT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    team_id           INT          NOT NULL,
    track_id          INT          NOT NULL,
    assigned_group    VARCHAR(50),
    registration_type VARCHAR(20)  NOT NULL DEFAULT 'ASSIGNED',
    assigned_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    assigned_by       INT,
    UNIQUE KEY uk_trt_team_track (team_id, track_id),
    CONSTRAINT chk_trt_reg_type CHECK (registration_type IN ('PREFERRED','ASSIGNED')),
    CONSTRAINT fk_trt_team        FOREIGN KEY (team_id)     REFERENCES teams(id)  ON DELETE CASCADE,
    CONSTRAINT fk_trt_track       FOREIGN KEY (track_id)    REFERENCES tracks(id) ON DELETE CASCADE,
    CONSTRAINT fk_trt_assigned_by FOREIGN KEY (assigned_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ============================================================
-- NHÓM 4: JUDGE & MENTOR
-- ============================================================

-- [BC-07] judge_assignments — XOR FK + FINAL_EXTERNAL
CREATE TABLE judge_assignments (
    id              INT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    judge_id        INT          NOT NULL,
    track_id        INT NULL,
    round_id        INT NULL,
    assignment_type VARCHAR(20)  NOT NULL DEFAULT 'NORMAL',
    assigned_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    assigned_by     INT,

    -- Generated cols mô phỏng partial unique
    track_uk        INT GENERATED ALWAYS AS (track_id) VIRTUAL,
    round_uk        INT GENERATED ALWAYS AS (CASE WHEN track_id IS NULL THEN round_id END) VIRTUAL,
    UNIQUE KEY uk_ja_judge_track       (judge_id, track_uk),
    UNIQUE KEY uk_ja_judge_final_round (judge_id, round_uk),

    CONSTRAINT chk_ja_type CHECK (assignment_type IN ('NORMAL','HEAD','CALIBRATION','FINAL_EXTERNAL')),
    CONSTRAINT chk_ja_xor_fk CHECK (
        (track_id IS NOT NULL AND round_id IS NULL)
        OR
        (track_id IS NULL AND round_id IS NOT NULL)
    ),
    CONSTRAINT chk_ja_final_external_requires_round CHECK (
        assignment_type <> 'FINAL_EXTERNAL'
        OR (track_id IS NULL AND round_id IS NOT NULL)
    ),
    CONSTRAINT fk_ja_judge       FOREIGN KEY (judge_id)    REFERENCES users(id)  ON DELETE CASCADE,
    CONSTRAINT fk_ja_track       FOREIGN KEY (track_id)    REFERENCES tracks(id) ON DELETE CASCADE,
    CONSTRAINT fk_ja_round       FOREIGN KEY (round_id)    REFERENCES rounds(id) ON DELETE CASCADE,
    CONSTRAINT fk_ja_assigned_by FOREIGN KEY (assigned_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- [BC-08] mentor_assignments — không đổi struct, track_id giờ thuộc round mới
CREATE TABLE mentor_assignments (
    id          INT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    mentor_id   INT      NOT NULL,
    track_id    INT      NOT NULL,
    assigned_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    assigned_by INT,
    UNIQUE KEY uk_ma_mentor_track (mentor_id, track_id),
    CONSTRAINT fk_ma_mentor      FOREIGN KEY (mentor_id)   REFERENCES users(id)  ON DELETE CASCADE,
    CONSTRAINT fk_ma_track       FOREIGN KEY (track_id)    REFERENCES tracks(id) ON DELETE CASCADE,
    CONSTRAINT fk_ma_assigned_by FOREIGN KEY (assigned_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ============================================================
-- NHÓM 5: SỰ KIỆN & LỊCH TRÌNH  ([BC-09] bỏ TEAM_MEETING)
-- ============================================================
CREATE TABLE events (
    id           INT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    hackathon_id INT          NOT NULL,
    title        VARCHAR(300) NOT NULL,
    type         VARCHAR(30)  NOT NULL,
    description  TEXT,
    location     VARCHAR(300),
    meet_url     TEXT,
    starts_at    DATETIME     NOT NULL,
    ends_at      DATETIME,
    is_public    BOOLEAN      NOT NULL DEFAULT TRUE,
    created_by   INT,
    CONSTRAINT chk_events_type    CHECK (type IN ('KICKOFF','WORKSHOP','PRESENTATION','AWARDS','OTHER')),
    CONSTRAINT fk_events_hackathon FOREIGN KEY (hackathon_id) REFERENCES hackathons(id) ON DELETE CASCADE,
    CONSTRAINT fk_events_creator   FOREIGN KEY (created_by)   REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ============================================================
-- NHÓM 6: SUBMISSIONS, SCORES, CALIBRATION  ([BC-06] XOR)
-- ============================================================
CREATE TABLE submissions (
    id           INT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    team_id      INT          NOT NULL,
    track_id     INT NULL,
    round_id     INT NULL,
    repo_url     TEXT,
    demo_url     TEXT,
    report_url   TEXT,
    slide_url    TEXT,
    status       VARCHAR(20)  NOT NULL DEFAULT 'SUBMITTED',
    is_late      BOOLEAN      NOT NULL DEFAULT FALSE,
    late_reason  TEXT,
    reviewed_by  INT,
    reviewed_at  DATETIME,
    review_note  TEXT,
    submitted_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    track_uk     INT GENERATED ALWAYS AS (track_id) VIRTUAL,
    round_uk     INT GENERATED ALWAYS AS (CASE WHEN track_id IS NULL THEN round_id END) VIRTUAL,
    UNIQUE KEY uk_subm_team_track       (team_id, track_uk),
    UNIQUE KEY uk_subm_team_final_round (team_id, round_uk),

    CONSTRAINT chk_subm_status CHECK (status IN ('SUBMITTED','LATE','LATE_PENDING','LATE_APPROVED','REJECTED','ACCEPTED')),
    CONSTRAINT chk_subm_xor_fk CHECK (
        (track_id IS NOT NULL AND round_id IS NULL)
        OR
        (track_id IS NULL AND round_id IS NOT NULL)
    ),
    CONSTRAINT fk_subm_team       FOREIGN KEY (team_id)     REFERENCES teams(id),
    CONSTRAINT fk_subm_track      FOREIGN KEY (track_id)    REFERENCES tracks(id),
    CONSTRAINT fk_subm_round      FOREIGN KEY (round_id)    REFERENCES rounds(id),
    CONSTRAINT fk_subm_reviewer   FOREIGN KEY (reviewed_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE calibration_sessions (
    id                   INT         NOT NULL AUTO_INCREMENT PRIMARY KEY,
    round_id             INT         NOT NULL,
    sample_submission_id INT,
    status               VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    target_score         FLOAT,
    instructions         TEXT,
    started_at           DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ended_at             DATETIME,
    created_by           INT,
    CONSTRAINT chk_cs_status CHECK (status IN ('OPEN','CLOSED')),
    CONSTRAINT fk_cs_round         FOREIGN KEY (round_id)             REFERENCES rounds(id) ON DELETE CASCADE,
    CONSTRAINT fk_cs_sample        FOREIGN KEY (sample_submission_id) REFERENCES submissions(id),
    CONSTRAINT fk_cs_creator       FOREIGN KEY (created_by)           REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE scores (
    id                     INT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    submission_id          INT          NOT NULL,
    judge_id               INT          NOT NULL,
    criterion_id           INT          NOT NULL,
    score_value            FLOAT        NOT NULL,
    comment                TEXT,
    score_type             VARCHAR(20)  NOT NULL DEFAULT 'NORMAL',
    is_final               BOOLEAN      NOT NULL DEFAULT FALSE,
    calibration_session_id INT,
    scored_at              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_scores_subm_judge_crit_type (submission_id, judge_id, criterion_id, score_type),
    CONSTRAINT chk_scores_value CHECK (score_value >= 0),
    CONSTRAINT chk_scores_type  CHECK (score_type IN ('NORMAL','CALIBRATION','PENALTY')),
    CONSTRAINT fk_scores_subm      FOREIGN KEY (submission_id)          REFERENCES submissions(id) ON DELETE CASCADE,
    CONSTRAINT fk_scores_judge     FOREIGN KEY (judge_id)               REFERENCES users(id),
    CONSTRAINT fk_scores_criterion FOREIGN KEY (criterion_id)           REFERENCES criteria(id),
    CONSTRAINT fk_scores_calib     FOREIGN KEY (calibration_session_id) REFERENCES calibration_sessions(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE tiebreak_evaluations (
    id            INT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    round_id      INT      NOT NULL,
    team_id       INT      NOT NULL,
    judge_id      INT      NOT NULL,
    penalty_score FLOAT    NOT NULL DEFAULT 0,
    notes         TEXT,
    evaluated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_te_round_team_judge (round_id, team_id, judge_id),
    CONSTRAINT fk_te_round FOREIGN KEY (round_id) REFERENCES rounds(id),
    CONSTRAINT fk_te_team  FOREIGN KEY (team_id)  REFERENCES teams(id),
    CONSTRAINT fk_te_judge FOREIGN KEY (judge_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- [BC-10] wildcard_reviews — thêm track_id
CREATE TABLE wildcard_reviews (
    id                   INT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    round_id             INT      NOT NULL,
    team_id              INT      NOT NULL,
    track_id             INT,
    avg_score            FLOAT,
    coordinator_approved BOOLEAN,
    coordinator_note     TEXT,
    reviewed_by          INT,
    reviewed_at          DATETIME,
    UNIQUE KEY uk_wr_round_team (round_id, team_id),
    CONSTRAINT fk_wr_round    FOREIGN KEY (round_id)    REFERENCES rounds(id),
    CONSTRAINT fk_wr_team     FOREIGN KEY (team_id)     REFERENCES teams(id),
    CONSTRAINT fk_wr_track    FOREIGN KEY (track_id)    REFERENCES tracks(id) ON DELETE SET NULL,
    CONSTRAINT fk_wr_reviewer FOREIGN KEY (reviewed_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ============================================================
-- NHÓM 7: KẾT QUẢ — GIẢI THƯỞNG & XẾP HẠNG
-- ============================================================
CREATE TABLE prizes (
    id          INT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    track_id    INT,
    round_id    INT          NOT NULL,
    team_id     INT          NOT NULL,
    prize_name  VARCHAR(200) NOT NULL,
    prize_rank  VARCHAR(50),
    prize_value VARCHAR(300),
    description TEXT,
    awarded_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    awarded_by  INT,
    CONSTRAINT chk_prizes_rank CHECK (prize_rank IS NULL OR prize_rank IN ('FIRST','SECOND','THIRD','HONORABLE','SPECIAL')),
    CONSTRAINT fk_prizes_track   FOREIGN KEY (track_id)   REFERENCES tracks(id),
    CONSTRAINT fk_prizes_round   FOREIGN KEY (round_id)   REFERENCES rounds(id),
    CONSTRAINT fk_prizes_team    FOREIGN KEY (team_id)    REFERENCES teams(id),
    CONSTRAINT fk_prizes_awarder FOREIGN KEY (awarded_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE chapter_rankings (
    id                 INT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    hackathon_id       INT      NOT NULL,
    chapter_id         INT      NOT NULL,
    best_team_score    FLOAT    NOT NULL DEFAULT 0,
    total_score        FLOAT    NOT NULL DEFAULT 0,
    `rank`             INT,
    teams_participated INT      NOT NULL DEFAULT 0,
    prizes_won         INT      NOT NULL DEFAULT 0,
    formula_snapshot   TEXT,
    calculated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_cr_hackathon_chapter (hackathon_id, chapter_id),
    CONSTRAINT fk_cr_hackathon FOREIGN KEY (hackathon_id) REFERENCES hackathons(id),
    CONSTRAINT fk_cr_chapter   FOREIGN KEY (chapter_id)   REFERENCES chapters(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE individual_rankings (
    id                   INT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    hackathon_id         INT      NOT NULL,
    user_id              INT      NOT NULL,
    score_this_hackathon FLOAT    NOT NULL DEFAULT 0,
    cumulative_score     FLOAT    NOT NULL DEFAULT 0,
    `rank`               INT,
    is_enabled           BOOLEAN  NOT NULL DEFAULT TRUE,
    calculated_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_ir_hackathon_user (hackathon_id, user_id),
    CONSTRAINT fk_ir_hackathon FOREIGN KEY (hackathon_id) REFERENCES hackathons(id),
    CONSTRAINT fk_ir_user      FOREIGN KEY (user_id)      REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ============================================================
-- NHÓM 8: THÔNG BÁO & TRUYỀN THÔNG
-- ============================================================
CREATE TABLE notifications (
    id             INT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id        INT          NOT NULL,
    type           VARCHAR(50)  NOT NULL,
    title          VARCHAR(300) NOT NULL,
    body           TEXT,
    reference_type VARCHAR(100),
    reference_id   INT,
    is_read        BOOLEAN      NOT NULL DEFAULT FALSE,
    sent_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at        DATETIME,
    CONSTRAINT fk_notif_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE notification_templates (
    id            INT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    code          VARCHAR(100) NOT NULL UNIQUE,
    title         VARCHAR(300) NOT NULL,
    body_template TEXT         NOT NULL,
    channel       VARCHAR(20)  NOT NULL DEFAULT 'IN_APP',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_nt_channel CHECK (channel IN ('IN_APP','EMAIL','ALL'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ============================================================
-- NHÓM 9: NHẬT KÝ HỆ THỐNG
-- ============================================================
CREATE TABLE audit_logs (
    id           INT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id      INT,
    action       VARCHAR(100) NOT NULL,
    target_table VARCHAR(100),
    target_id    INT,
    detail       JSON,
    ip_address   VARCHAR(45),
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE export_jobs (
    id            INT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    hackathon_id  INT          NOT NULL,
    type          VARCHAR(50)  NOT NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    file_url      TEXT,
    error_message TEXT,
    requested_by  INT,
    started_at    DATETIME,
    finished_at   DATETIME,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_ej_type   CHECK (type   IN ('CSV_SCORES','CSV_RANKINGS','ANONYMIZED_RBL','FULL_REPORT')),
    CONSTRAINT chk_ej_status CHECK (status IN ('PENDING','PROCESSING','DONE','FAILED')),
    CONSTRAINT fk_ej_hackathon  FOREIGN KEY (hackathon_id) REFERENCES hackathons(id),
    CONSTRAINT fk_ej_requester  FOREIGN KEY (requested_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET FOREIGN_KEY_CHECKS = 1;
```

---

## 3. Indexes

```sql
-- Users
CREATE INDEX idx_users_email        ON users(email);
CREATE INDEX idx_users_role_status  ON users(role, status);
CREATE INDEX idx_users_chapter      ON users(chapter_id);
CREATE INDEX idx_users_student_code ON users(student_code);

-- Hackathons
CREATE INDEX idx_hackathons_status ON hackathons(status);
CREATE INDEX idx_hackathons_year   ON hackathons(`year`);

-- Rounds — [BC-01] truy cập theo hackathon
CREATE INDEX idx_rounds_hackathon ON rounds(hackathon_id);
CREATE INDEX idx_rounds_active    ON rounds(is_active);
CREATE INDEX idx_rounds_final     ON rounds(is_final);
CREATE INDEX idx_rounds_type      ON rounds(round_type);
-- Mỗi hackathon đúng 1 round FINAL — mô phỏng partial unique
ALTER TABLE rounds
  ADD COLUMN final_uk INT GENERATED ALWAYS AS (CASE WHEN is_final = TRUE THEN hackathon_id END) VIRTUAL,
  ADD UNIQUE KEY uk_rounds_one_final_per_hackathon (final_uk);

-- Tracks — [BC-02] truy cập theo round
CREATE INDEX idx_tracks_round  ON tracks(round_id);
CREATE INDEX idx_tracks_status ON tracks(status);

-- Criteria — [BC-03]
CREATE INDEX idx_criteria_track       ON criteria(track_id);
CREATE INDEX idx_criteria_round_final ON criteria(round_id);

-- team_round_tracks
CREATE INDEX idx_trt_team           ON team_round_tracks(team_id);
CREATE INDEX idx_trt_track          ON team_round_tracks(track_id);
CREATE INDEX idx_trt_assigned_group ON team_round_tracks(track_id, assigned_group);

-- Teams
CREATE INDEX idx_teams_hackathon   ON teams(hackathon_id);
CREATE INDEX idx_teams_status      ON teams(status);
CREATE INDEX idx_teams_leader      ON teams(leader_id);
CREATE INDEX idx_teams_locked      ON teams(is_locked);
CREATE INDEX idx_team_members_user ON team_members(user_id);

-- Submissions — [BC-06]
CREATE INDEX idx_submissions_team        ON submissions(team_id);
CREATE INDEX idx_submissions_track       ON submissions(track_id);
CREATE INDEX idx_submissions_round_final ON submissions(round_id);
CREATE INDEX idx_submissions_status      ON submissions(status);

-- Judge assignments — [BC-07]
CREATE INDEX idx_judge_assign_judge ON judge_assignments(judge_id);
CREATE INDEX idx_judge_assign_track ON judge_assignments(track_id);
CREATE INDEX idx_judge_assign_round ON judge_assignments(round_id);
CREATE INDEX idx_judge_assign_type  ON judge_assignments(assignment_type);

-- Mentor assignments — [BC-08]
CREATE INDEX idx_mentor_assign_mentor ON mentor_assignments(mentor_id);
CREATE INDEX idx_mentor_assign_track  ON mentor_assignments(track_id);

-- Scores
CREATE INDEX idx_scores_submission ON scores(submission_id);
CREATE INDEX idx_scores_judge      ON scores(judge_id);
CREATE INDEX idx_scores_criterion  ON scores(criterion_id);
CREATE INDEX idx_scores_type_final ON scores(score_type, is_final);
CREATE INDEX idx_scores_final      ON scores(is_final);

-- Rankings
CREATE INDEX idx_chapter_rank_hackathon ON chapter_rankings(hackathon_id);
CREATE INDEX idx_individual_rank_user   ON individual_rankings(user_id);

-- Audit logs
CREATE INDEX idx_audit_user     ON audit_logs(user_id);
CREATE INDEX idx_audit_table_id ON audit_logs(target_table, target_id);
CREATE INDEX idx_audit_created  ON audit_logs(created_at DESC);

-- Notifications
CREATE INDEX idx_notif_user_unread ON notifications(user_id, is_read);
```

---

## 4. Views

```sql
-- ============================================================
-- v_round_leaderboard — Sơ loại JOIN qua track, Chung kết trực tiếp
-- ============================================================
CREATE OR REPLACE VIEW v_round_leaderboard AS
SELECT
    COALESCE(tr.round_id, s.round_id)           AS round_id,
    s.track_id,
    s.team_id,
    t.team_name,
    trt.assigned_group,
    COUNT(DISTINCT sc.judge_id)                 AS judge_count,
    ROUND(
        SUM(sc.score_value * c.weight)
        / NULLIF(COUNT(DISTINCT sc.judge_id), 0),
    4)                                          AS weighted_avg_score,
    MAX(s.submitted_at)                         AS submitted_at
FROM submissions s
JOIN teams t              ON t.id  = s.team_id
JOIN scores sc            ON sc.submission_id = s.id
JOIN criteria c           ON c.id  = sc.criterion_id
LEFT JOIN tracks tr       ON tr.id = s.track_id
LEFT JOIN team_round_tracks trt
                          ON trt.team_id = s.team_id
                         AND trt.track_id = s.track_id
WHERE sc.is_final   = TRUE
  AND sc.score_type = 'NORMAL'
GROUP BY
    COALESCE(tr.round_id, s.round_id),
    s.track_id, s.team_id, t.team_name, trt.assigned_group;

-- ============================================================
-- v_judge_score_variance
-- ============================================================
CREATE OR REPLACE VIEW v_judge_score_variance AS
SELECT
    COALESCE(tr.round_id, s.round_id) AS round_id,
    s.track_id,
    sc.criterion_id,
    c.name                            AS criterion_name,
    c.type                            AS criterion_type,
    u.user_type                       AS judge_type,
    COUNT(DISTINCT sc.judge_id)       AS judge_count,
    ROUND(AVG(sc.score_value),    3)  AS mean_score,
    ROUND(STDDEV(sc.score_value), 3)  AS std_dev,
    MIN(sc.score_value)               AS min_score,
    MAX(sc.score_value)               AS max_score
FROM scores sc
JOIN submissions s   ON s.id  = sc.submission_id
JOIN criteria c      ON c.id  = sc.criterion_id
JOIN users u         ON u.id  = sc.judge_id
LEFT JOIN tracks tr  ON tr.id = s.track_id
WHERE sc.score_type = 'NORMAL'
GROUP BY
    COALESCE(tr.round_id, s.round_id),
    s.track_id, sc.criterion_id, c.name, c.type, u.user_type;

-- ============================================================
-- v_scoring_progress
-- ============================================================
CREATE OR REPLACE VIEW v_scoring_progress AS
SELECT
    r.id        AS round_id,
    r.name      AS round_name,
    r.is_final,
    COUNT(DISTINCT ja.judge_id)  AS total_judges,
    COUNT(DISTINCT sc.judge_id)  AS judges_scored,
    COUNT(DISTINCT s.id)         AS total_submissions,
    COUNT(DISTINCT CASE WHEN sc.judge_id IS NOT NULL THEN s.id END) AS scored_submissions,
    ROUND(
        100.0 * COUNT(DISTINCT sc.judge_id) / NULLIF(COUNT(DISTINCT ja.judge_id), 0),
    1) AS completion_pct
FROM rounds r
LEFT JOIN tracks tr            ON tr.round_id = r.id
LEFT JOIN judge_assignments ja ON (
        (r.is_final = FALSE AND ja.track_id = tr.id)
     OR (r.is_final = TRUE  AND ja.round_id = r.id)
)
LEFT JOIN submissions s        ON (
        (r.is_final = FALSE AND s.track_id = tr.id)
     OR (r.is_final = TRUE  AND s.round_id = r.id)
)
LEFT JOIN scores sc            ON sc.submission_id = s.id
                              AND sc.score_type = 'NORMAL'
GROUP BY r.id, r.name, r.is_final;

-- ============================================================
-- v_active_team_members
-- ============================================================
CREATE OR REPLACE VIEW v_active_team_members AS
SELECT
    tm.team_id, t.team_name, t.hackathon_id, t.is_locked,
    tm.user_id, u.full_name, u.email, u.user_type, u.student_code,
    tm.role_in_team
FROM team_members tm
JOIN teams t ON t.id = tm.team_id
JOIN users u ON u.id = tm.user_id
WHERE tm.status  = 'ACCEPTED'
  AND tm.left_at IS NULL;

-- ============================================================
-- v_team_track_assignment — Tra cứu đội đang thi ở Track/Round nào
-- ============================================================
CREATE OR REPLACE VIEW v_team_track_assignment AS
SELECT
    trt.team_id,
    t.team_name,
    t.hackathon_id,
    trt.track_id,
    tr.name             AS track_name,
    tr.topic            AS track_topic,
    tr.round_id,
    r.name              AS round_name,
    r.round_type,
    r.sequence_order    AS round_sequence,
    trt.assigned_group,
    trt.registration_type,
    trt.assigned_at
FROM team_round_tracks trt
JOIN teams t   ON t.id  = trt.team_id
JOIN tracks tr ON tr.id = trt.track_id
JOIN rounds r  ON r.id  = tr.round_id;

-- ============================================================
-- v_rbl_anonymized — Dataset ẩn danh
-- ============================================================
CREATE OR REPLACE VIEW v_rbl_anonymized AS
SELECT
    COALESCE(tr.round_id, s.round_id) AS round_id,
    s.track_id,
    sc.criterion_id,
    c.name                            AS criterion_name,
    c.type                            AS criterion_type,
    u.user_type                       AS judge_type,
    sc.score_value,
    sc.scored_at
FROM scores sc
JOIN submissions s   ON s.id  = sc.submission_id
JOIN criteria c      ON c.id  = sc.criterion_id
JOIN users u         ON u.id  = sc.judge_id
LEFT JOIN tracks tr  ON tr.id = s.track_id
WHERE sc.score_type = 'NORMAL'
  AND sc.is_final   = TRUE;
```

---

## 5. Triggers

> MySQL trigger không tách function/trigger. Mỗi rule là 1 `CREATE TRIGGER ... BEGIN ... END`.
> `RAISE EXCEPTION 'msg'` → `SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'msg'`.

```sql
DELIMITER //

-- ============================================================
-- 5.1 Ngăn ghi điểm khi round đã scoring_locked = TRUE
-- ============================================================
DROP TRIGGER IF EXISTS trg_lock_score_insert//
CREATE TRIGGER trg_lock_score_insert
BEFORE INSERT ON scores
FOR EACH ROW
BEGIN
    DECLARE v_locked BOOLEAN;
    SELECT r.scoring_locked INTO v_locked
    FROM submissions s
    LEFT JOIN tracks tr ON tr.id = s.track_id
    JOIN rounds r       ON r.id  = COALESCE(tr.round_id, s.round_id)
    WHERE s.id = NEW.submission_id
    LIMIT 1;
    IF v_locked = TRUE THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Không thể ghi điểm: vòng thi đã bị khóa chấm điểm (scoring_locked = TRUE).';
    END IF;
END//

DROP TRIGGER IF EXISTS trg_lock_score_update//
CREATE TRIGGER trg_lock_score_update
BEFORE UPDATE ON scores
FOR EACH ROW
BEGIN
    DECLARE v_locked BOOLEAN;
    SELECT r.scoring_locked INTO v_locked
    FROM submissions s
    LEFT JOIN tracks tr ON tr.id = s.track_id
    JOIN rounds r       ON r.id  = COALESCE(tr.round_id, s.round_id)
    WHERE s.id = NEW.submission_id
    LIMIT 1;
    IF v_locked = TRUE THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Không thể ghi điểm: vòng thi đã bị khóa chấm điểm (scoring_locked = TRUE).';
    END IF;
END//

-- ============================================================
-- 5.2 Khóa thay đổi team_members khi teams.is_locked = TRUE
-- ============================================================
DROP TRIGGER IF EXISTS trg_lock_member_insert//
CREATE TRIGGER trg_lock_member_insert
BEFORE INSERT ON team_members
FOR EACH ROW
BEGIN
    DECLARE v_locked BOOLEAN;
    SELECT is_locked INTO v_locked FROM teams WHERE id = NEW.team_id;
    IF v_locked = TRUE THEN
        INSERT INTO audit_logs (action, target_table, target_id, detail)
        VALUES ('MEMBER_CHANGE_DENIED', 'team_members', NEW.team_id,
                JSON_OBJECT('user_id', NEW.user_id, 'operation', 'INSERT'));
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Không thể thay đổi thành viên: đội đã bị khóa sau deadline đăng ký.';
    END IF;
END//

DROP TRIGGER IF EXISTS trg_lock_member_update//
CREATE TRIGGER trg_lock_member_update
BEFORE UPDATE ON team_members
FOR EACH ROW
BEGIN
    DECLARE v_locked BOOLEAN;
    SELECT is_locked INTO v_locked FROM teams WHERE id = NEW.team_id;
    IF v_locked = TRUE THEN
        INSERT INTO audit_logs (action, target_table, target_id, detail)
        VALUES ('MEMBER_CHANGE_DENIED', 'team_members', NEW.team_id,
                JSON_OBJECT('user_id', NEW.user_id, 'operation', 'UPDATE'));
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Không thể thay đổi thành viên: đội đã bị khóa sau deadline đăng ký.';
    END IF;
END//

-- ============================================================
-- 5.3 Audit team status change
-- ============================================================
DROP TRIGGER IF EXISTS trg_audit_team_status//
CREATE TRIGGER trg_audit_team_status
AFTER UPDATE ON teams
FOR EACH ROW
BEGIN
    IF NOT(OLD.status <=> NEW.status) THEN
        INSERT INTO audit_logs (action, target_table, target_id, detail)
        VALUES ('TEAM_STATUS_CHANGE', 'teams', NEW.id,
                JSON_OBJECT('from', OLD.status, 'to', NEW.status, 'reason', NEW.elimination_reason));
    END IF;
    IF NOT(OLD.is_locked <=> NEW.is_locked) AND NEW.is_locked = TRUE THEN
        INSERT INTO audit_logs (action, target_table, target_id, detail)
        VALUES ('TEAM_LOCKED', 'teams', NEW.id,
                JSON_OBJECT('locked_at', NEW.locked_at));
    END IF;
END//

-- ============================================================
-- 5.4 [FIX-01/02/03] judge_assignments — Mentor↔Judge conflict + EXTERNAL requirement
-- ============================================================
DROP TRIGGER IF EXISTS trg_check_mentor_judge_conflict_ins//
CREATE TRIGGER trg_check_mentor_judge_conflict_ins
BEFORE INSERT ON judge_assignments
FOR EACH ROW
BEGIN
    DECLARE v_user_type    VARCHAR(20);
    DECLARE v_is_dept_head BOOLEAN;
    DECLARE v_round_final  BOOLEAN;
    DECLARE v_mentor_exists INT;

    -- Nhánh A: Judge Sơ loại
    IF NEW.track_id IS NOT NULL THEN
        SELECT COUNT(*) INTO v_mentor_exists
        FROM mentor_assignments
        WHERE mentor_id = NEW.judge_id AND track_id = NEW.track_id;
        IF v_mentor_exists > 0 THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'CONFLICT_SAME_TRACK: User is Mentor of the same Track — cannot be Judge.';
        END IF;
        IF NEW.assignment_type = 'FINAL_EXTERNAL' THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'INVALID_ASSIGNMENT_TYPE: FINAL_EXTERNAL only for Final Round (track_id must be NULL).';
        END IF;
    ELSE
        -- Nhánh B: Judge Chung kết
        IF NEW.round_id IS NULL THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'MISSING_ROUND_ID: Final Judge requires round_id.';
        END IF;
        SELECT is_final INTO v_round_final FROM rounds WHERE id = NEW.round_id;
        IF v_round_final IS NOT TRUE THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'INVALID_FINAL_ROUND: round_id is not a FINAL round (is_final=FALSE).';
        END IF;
        IF NEW.assignment_type <> 'FINAL_EXTERNAL' THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'INVALID_ASSIGNMENT_TYPE: Final Judge must be FINAL_EXTERNAL.';
        END IF;
        SELECT user_type, is_dept_head INTO v_user_type, v_is_dept_head FROM users WHERE id = NEW.judge_id;
        IF v_user_type = 'INTERNAL' THEN
            SELECT COUNT(*) INTO v_mentor_exists FROM mentor_assignments WHERE mentor_id = NEW.judge_id;
            IF v_is_dept_head = TRUE AND v_mentor_exists = 0 THEN
                INSERT INTO audit_logs (action, target_table, target_id, detail)
                VALUES ('DEPT_HEAD_FINAL_JUDGE_EXCEPTION', 'judge_assignments', NEW.judge_id,
                        JSON_OBJECT('round_id', NEW.round_id, 'assigned_by', NEW.assigned_by,
                                    'note', 'Dept head exception for Final Judge'));
            ELSEIF v_mentor_exists > 0 THEN
                SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'INTERNAL_MENTOR_NOT_ALLOWED_IN_FINAL: INTERNAL user who has been Mentor cannot be Final Judge.';
            ELSE
                SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'INTERNAL_JUDGE_NOT_ALLOWED_IN_FINAL: Final must be 100% EXTERNAL (unless dept head not mentoring).';
            END IF;
        END IF;
    END IF;
END//

-- (UPDATE trigger same logic — duplicated for brevity in production drop+create)
DROP TRIGGER IF EXISTS trg_check_mentor_judge_conflict_upd//
CREATE TRIGGER trg_check_mentor_judge_conflict_upd
BEFORE UPDATE ON judge_assignments
FOR EACH ROW
BEGIN
    DECLARE v_user_type    VARCHAR(20);
    DECLARE v_is_dept_head BOOLEAN;
    DECLARE v_round_final  BOOLEAN;
    DECLARE v_mentor_exists INT;
    IF NEW.track_id IS NOT NULL THEN
        SELECT COUNT(*) INTO v_mentor_exists FROM mentor_assignments
        WHERE mentor_id = NEW.judge_id AND track_id = NEW.track_id;
        IF v_mentor_exists > 0 THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'CONFLICT_SAME_TRACK: User is Mentor of the same Track.';
        END IF;
        IF NEW.assignment_type = 'FINAL_EXTERNAL' THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'INVALID_ASSIGNMENT_TYPE: FINAL_EXTERNAL only for Final Round.';
        END IF;
    ELSE
        IF NEW.round_id IS NULL THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'MISSING_ROUND_ID: Final Judge requires round_id.';
        END IF;
        SELECT is_final INTO v_round_final FROM rounds WHERE id = NEW.round_id;
        IF v_round_final IS NOT TRUE THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'INVALID_FINAL_ROUND: round_id is not a FINAL round.';
        END IF;
        IF NEW.assignment_type <> 'FINAL_EXTERNAL' THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'INVALID_ASSIGNMENT_TYPE: Final Judge must be FINAL_EXTERNAL.';
        END IF;
        SELECT user_type, is_dept_head INTO v_user_type, v_is_dept_head FROM users WHERE id = NEW.judge_id;
        IF v_user_type = 'INTERNAL' THEN
            SELECT COUNT(*) INTO v_mentor_exists FROM mentor_assignments WHERE mentor_id = NEW.judge_id;
            IF v_is_dept_head = TRUE AND v_mentor_exists = 0 THEN
                INSERT INTO audit_logs (action, target_table, target_id, detail)
                VALUES ('DEPT_HEAD_FINAL_JUDGE_EXCEPTION', 'judge_assignments', NEW.judge_id,
                        JSON_OBJECT('round_id', NEW.round_id, 'note', 'Dept head exception'));
            ELSEIF v_mentor_exists > 0 THEN
                SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'INTERNAL_MENTOR_NOT_ALLOWED_IN_FINAL.';
            ELSE
                SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'INTERNAL_JUDGE_NOT_ALLOWED_IN_FINAL.';
            END IF;
        END IF;
    END IF;
END//

-- ============================================================
-- 5.5 mentor_assignments — chiều ngược: block người đã là Judge Final
-- ============================================================
DROP TRIGGER IF EXISTS trg_check_judge_mentor_conflict_ins//
CREATE TRIGGER trg_check_judge_mentor_conflict_ins
BEFORE INSERT ON mentor_assignments
FOR EACH ROW
BEGIN
    DECLARE v_same_track INT;
    DECLARE v_final_judge INT;
    SELECT COUNT(*) INTO v_same_track FROM judge_assignments
    WHERE judge_id = NEW.mentor_id AND track_id = NEW.track_id;
    IF v_same_track > 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'CONFLICT_SAME_TRACK: User is Judge of the same Track.';
    END IF;
    SELECT COUNT(*) INTO v_final_judge
    FROM judge_assignments ja
    JOIN rounds r ON r.id = ja.round_id
    WHERE ja.judge_id = NEW.mentor_id
      AND ja.assignment_type = 'FINAL_EXTERNAL'
      AND ja.track_id IS NULL
      AND r.hackathon_id = (
          SELECT r2.hackathon_id FROM tracks tr2 JOIN rounds r2 ON r2.id = tr2.round_id
          WHERE tr2.id = NEW.track_id
      );
    IF v_final_judge > 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'FINAL_JUDGE_CANNOT_BE_MENTOR: User is already Final Judge of this Hackathon.';
    END IF;
END//

-- ============================================================
-- 5.6 [BC-01] Ngăn tạo Track con cho Round FINAL
-- ============================================================
DROP TRIGGER IF EXISTS trg_prevent_track_in_final_round_ins//
CREATE TRIGGER trg_prevent_track_in_final_round_ins
BEFORE INSERT ON tracks
FOR EACH ROW
BEGIN
    DECLARE v_is_final BOOLEAN;
    SELECT is_final INTO v_is_final FROM rounds WHERE id = NEW.round_id;
    IF v_is_final = TRUE THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'DESIGN_VIOLATION: FINAL round cannot have child Tracks.';
    END IF;
END//

DROP TRIGGER IF EXISTS trg_prevent_track_in_final_round_upd//
CREATE TRIGGER trg_prevent_track_in_final_round_upd
BEFORE UPDATE ON tracks
FOR EACH ROW
BEGIN
    DECLARE v_is_final BOOLEAN;
    SELECT is_final INTO v_is_final FROM rounds WHERE id = NEW.round_id;
    IF v_is_final = TRUE THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'DESIGN_VIOLATION: FINAL round cannot have child Tracks.';
    END IF;
END//

-- ============================================================
-- 5.7 [FIX-06A] submissions — track NULL ⇒ round FINAL + HARD_LOCK
-- ============================================================
DROP TRIGGER IF EXISTS trg_check_submission_round_is_final_ins//
CREATE TRIGGER trg_check_submission_round_is_final_ins
BEFORE INSERT ON submissions
FOR EACH ROW
BEGIN
    DECLARE v_is_final BOOLEAN;
    IF NEW.track_id IS NULL THEN
        IF NEW.round_id IS NULL THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'MISSING_ROUND_ID: Final submission requires round_id.';
        END IF;
        SELECT is_final INTO v_is_final FROM rounds WHERE id = NEW.round_id;
        IF v_is_final IS NOT TRUE THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'INVALID_ROUND_FOR_SUBMISSION: round_id not FINAL.';
        END IF;
        IF NEW.status = 'LATE_PENDING' THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'LATE_PENDING_NOT_ALLOWED_IN_FINAL: HARD_LOCK enforced.';
        END IF;
    END IF;
END//

DROP TRIGGER IF EXISTS trg_check_submission_round_is_final_upd//
CREATE TRIGGER trg_check_submission_round_is_final_upd
BEFORE UPDATE ON submissions
FOR EACH ROW
BEGIN
    DECLARE v_is_final BOOLEAN;
    IF NEW.track_id IS NULL THEN
        IF NEW.round_id IS NULL THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'MISSING_ROUND_ID: Final submission requires round_id.';
        END IF;
        SELECT is_final INTO v_is_final FROM rounds WHERE id = NEW.round_id;
        IF v_is_final IS NOT TRUE THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'INVALID_ROUND_FOR_SUBMISSION: round_id not FINAL.';
        END IF;
        IF NEW.status = 'LATE_PENDING' THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'LATE_PENDING_NOT_ALLOWED_IN_FINAL.';
        END IF;
    END IF;
END//

-- ============================================================
-- 5.8 [FIX-06B] criteria — track NULL ⇒ round FINAL
-- ============================================================
DROP TRIGGER IF EXISTS trg_check_criteria_round_is_final_ins//
CREATE TRIGGER trg_check_criteria_round_is_final_ins
BEFORE INSERT ON criteria
FOR EACH ROW
BEGIN
    DECLARE v_is_final BOOLEAN;
    IF NEW.track_id IS NULL THEN
        IF NEW.round_id IS NULL THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'MISSING_ROUND_ID: Final criterion requires round_id.';
        END IF;
        SELECT is_final INTO v_is_final FROM rounds WHERE id = NEW.round_id;
        IF v_is_final IS NOT TRUE THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'INVALID_ROUND_FOR_CRITERIA: round_id not FINAL.';
        END IF;
    END IF;
END//

DROP TRIGGER IF EXISTS trg_check_criteria_round_is_final_upd//
CREATE TRIGGER trg_check_criteria_round_is_final_upd
BEFORE UPDATE ON criteria
FOR EACH ROW
BEGIN
    DECLARE v_is_final BOOLEAN;
    IF NEW.track_id IS NULL THEN
        IF NEW.round_id IS NULL THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'MISSING_ROUND_ID: Final criterion requires round_id.';
        END IF;
        SELECT is_final INTO v_is_final FROM rounds WHERE id = NEW.round_id;
        IF v_is_final IS NOT TRUE THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'INVALID_ROUND_FOR_CRITERIA: round_id not FINAL.';
        END IF;
    END IF;
END//

-- ============================================================
-- 5.9 [FIX-04] team_round_tracks — đội và Track cùng Hackathon, Track ≠ FINAL round
-- ============================================================
DROP TRIGGER IF EXISTS trg_check_team_track_same_hackathon_ins//
CREATE TRIGGER trg_check_team_track_same_hackathon_ins
BEFORE INSERT ON team_round_tracks
FOR EACH ROW
BEGIN
    DECLARE v_team_hackathon  INT;
    DECLARE v_track_hackathon INT;
    DECLARE v_is_final        BOOLEAN;
    SELECT hackathon_id INTO v_team_hackathon FROM teams WHERE id = NEW.team_id;
    SELECT r.hackathon_id, r.is_final INTO v_track_hackathon, v_is_final
    FROM tracks tr JOIN rounds r ON r.id = tr.round_id WHERE tr.id = NEW.track_id;
    IF NOT(v_team_hackathon <=> v_track_hackathon) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'CROSS_HACKATHON_VIOLATION: Team and Track must belong to same Hackathon.';
    END IF;
    IF v_is_final = TRUE THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'DESIGN_VIOLATION: team_round_tracks invalid for FINAL track.';
    END IF;
END//

DROP TRIGGER IF EXISTS trg_check_team_track_same_hackathon_upd//
CREATE TRIGGER trg_check_team_track_same_hackathon_upd
BEFORE UPDATE ON team_round_tracks
FOR EACH ROW
BEGIN
    DECLARE v_team_hackathon  INT;
    DECLARE v_track_hackathon INT;
    DECLARE v_is_final        BOOLEAN;
    SELECT hackathon_id INTO v_team_hackathon FROM teams WHERE id = NEW.team_id;
    SELECT r.hackathon_id, r.is_final INTO v_track_hackathon, v_is_final
    FROM tracks tr JOIN rounds r ON r.id = tr.round_id WHERE tr.id = NEW.track_id;
    IF NOT(v_team_hackathon <=> v_track_hackathon) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'CROSS_HACKATHON_VIOLATION.';
    END IF;
    IF v_is_final = TRUE THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'DESIGN_VIOLATION.';
    END IF;
END//

DELIMITER ;
```

---

## 6. Sample Data — SEAL E2E 2026 (minimal)

> Demo flow: 2 đội → 2 Track → 1 Round Sơ loại → 1 Round Chung kết.
> Mật khẩu plain `password` ở dòng dưới — production phải hash BCrypt.

```sql
INSERT INTO chapters (name, code, university, city) VALUES
  ('FPT University Ho Chi Minh City', 'FPT-HCM', 'FPT University', 'Ho Chi Minh City'),
  ('FPT University Hanoi',            'FPT-HN',  'FPT University', 'Hanoi'),
  ('External Participants',           'EXT',      NULL,             NULL);

INSERT INTO users
  (full_name, email, password_hash, role, user_type, student_code,
   is_temp_account, is_dept_head, status, chapter_id) VALUES
  ('Nguyễn Văn Coordinator', 'coord@fpt.edu.vn',     'bcrypt-placeholder', 'COORDINATOR','INTERNAL', NULL,        FALSE, FALSE, 'APPROVED', 1),
  ('Trần Thị Judge Internal','judge1@fpt.edu.vn',    'bcrypt-placeholder', 'JUDGE',      'INTERNAL', NULL,        FALSE, FALSE, 'APPROVED', 1),
  ('Lê Văn Judge External',  'guestjudge@gmail.com', 'bcrypt-placeholder', 'JUDGE',      'EXTERNAL', NULL,        TRUE,  FALSE, 'APPROVED', 3),
  ('Phạm Minh Mentor',       'mentor@fpt.edu.vn',    'bcrypt-placeholder', 'MENTOR',     'INTERNAL', NULL,        FALSE, FALSE, 'APPROVED', 1),
  ('Team A Leader',          'teama@fpt.edu.vn',     'bcrypt-placeholder', 'STUDENT',    'INTERNAL', 'FPT0001',   FALSE, FALSE, 'APPROVED', 1),
  ('Team A Member 1',        'teama1@fpt.edu.vn',    'bcrypt-placeholder', 'STUDENT',    'INTERNAL', 'FPT0002',   FALSE, FALSE, 'APPROVED', 1),
  ('Team A Member 2',        'teama2@fpt.edu.vn',    'bcrypt-placeholder', 'STUDENT',    'INTERNAL', 'FPT0003',   FALSE, FALSE, 'APPROVED', 1),
  ('Team B Leader',          'teamb@gmail.com',      'bcrypt-placeholder', 'STUDENT',    'EXTERNAL', 'HUST-2001', FALSE, FALSE, 'APPROVED', 3),
  ('Team B Member 1',        'teamb1@gmail.com',     'bcrypt-placeholder', 'STUDENT',    'EXTERNAL', 'HUST-2002', FALSE, FALSE, 'APPROVED', 3);

INSERT INTO hackathons
  (name, slug, season, `year`, status, description,
   registration_start, registration_end, event_start, event_end,
   wildcard_enabled, individual_ranking_enabled, created_by) VALUES
  ('SEAL E2E 2026', 'seal-e2e-2026', 'Spring', 2026, 'ONGOING',
   'Cuộc thi lập trình SEAL — Kỳ Spring 2026',
   '2026-01-01', '2026-01-20', '2026-02-01', '2026-03-15',
   TRUE, FALSE, 1);
-- hackathon_id = 1

INSERT INTO rounds
  (hackathon_id, name, sequence_order, is_final, round_type,
   submission_deadline, coding_duration_hours, late_submission_policy,
   top_n_advance, wildcard_enabled, min_teams_final, tiebreak_rule, is_active) VALUES
  (1, 'Vòng Sơ loại',   1, FALSE, 'PRELIMINARY',
   '2026-02-15 23:59:00', 7, 'ALLOW_LATE_PENDING', 2, TRUE,  6,    'PENALTY_SCORE', TRUE),
  (1, 'Vòng Chung kết', 2, TRUE,  'FINAL',
   '2026-03-01 23:59:00', NULL, 'HARD_LOCK',        NULL, FALSE, NULL, 'PENALTY_SCORE', FALSE);

INSERT INTO tracks
  (round_id, name, description, topic, max_teams, max_teams_per_group,
   min_team_size, max_team_size, sequence_order) VALUES
  (1, 'Track 1 — RAG Pipeline', 'Xây dựng hệ thống RAG',     'Business Analysis App',     8, 8, 3, 5, 1),
  (1, 'Track 2 — AI Agent',     'Thiết kế AI Agent',          'Process Automation Agent',  8, 8, 3, 5, 2);

INSERT INTO criteria (track_id, round_id, name, type, weight, max_score, display_order) VALUES
  (1, NULL, 'Domain Accuracy',         'TECHNICAL', 0.30, 10, 1),
  (1, NULL, 'Kiến trúc RAG',           'TECHNICAL', 0.30, 10, 2),
  (1, NULL, 'Ý tưởng & Thuyết trình',  'SOFT_SKILL',0.15, 10, 3),
  (1, NULL, 'Thực thi & Sáng tạo',     'TECHNICAL', 0.15, 10, 4),
  (1, NULL, 'UX & Giao diện',          'SOFT_SKILL',0.10, 10, 5),
  (2, NULL, 'Domain Accuracy',         'TECHNICAL', 0.30, 10, 1),
  (2, NULL, 'Kiến trúc RAG',           'TECHNICAL', 0.30, 10, 2),
  (2, NULL, 'Ý tưởng & Thuyết trình',  'SOFT_SKILL',0.15, 10, 3),
  (2, NULL, 'Thực thi & Sáng tạo',     'TECHNICAL', 0.15, 10, 4),
  (2, NULL, 'UX & Giao diện',          'SOFT_SKILL',0.10, 10, 5),
  (NULL, 2, 'Xử lý & Truy xuất',       'TECHNICAL', 0.30, 10, 1),
  (NULL, 2, 'Độ tin cậy',              'TECHNICAL', 0.20, 10, 2),
  (NULL, 2, 'Tư duy Agent',            'TECHNICAL', 0.20, 10, 3),
  (NULL, 2, 'Thực tế & Triển khai',    'TECHNICAL', 0.20, 10, 4),
  (NULL, 2, 'Mở rộng & Scale',         'SOFT_SKILL',0.10, 10, 5);

INSERT INTO teams (hackathon_id, team_name, leader_id, chapter_id, status) VALUES
  (1, 'FPT AI Warriors',  5, 1, 'ACTIVE'),
  (1, 'External Builders',8, 3, 'ACTIVE');

INSERT INTO team_members (team_id, user_id, role_in_team, status, joined_at) VALUES
  (1, 5, 'LEADER', 'ACCEPTED', NOW()),
  (1, 6, 'MEMBER', 'ACCEPTED', NOW()),
  (1, 7, 'MEMBER', 'ACCEPTED', NOW()),
  (2, 8, 'LEADER', 'ACCEPTED', NOW()),
  (2, 9, 'MEMBER', 'ACCEPTED', NOW());

INSERT INTO team_round_tracks (team_id, track_id, assigned_group, registration_type, assigned_by) VALUES
  (1, 1, NULL, 'ASSIGNED', 1),
  (2, 2, NULL, 'ASSIGNED', 1);

INSERT INTO judge_assignments (judge_id, track_id, round_id, assignment_type, assigned_by) VALUES
  (2, 1, NULL, 'NORMAL', 1),
  (3, 1, NULL, 'NORMAL', 1),
  (2, 2, NULL, 'NORMAL', 1),
  (3, 2, NULL, 'NORMAL', 1);

INSERT INTO mentor_assignments (mentor_id, track_id, assigned_by) VALUES
  (4, 1, 1),
  (4, 2, 1);

INSERT INTO events (hackathon_id, title, type, location, starts_at, ends_at) VALUES
  (1, 'Workshop: RAG & AI Agent Fundamentals', 'WORKSHOP',     'Online (Teams)',         '2026-02-05 20:00:00','2026-02-05 21:30:00'),
  (1, 'Lễ Khai mạc & Bốc thăm chia Track',     'KICKOFF',      'FPT HCM — Hội trường A', '2026-02-10 14:00:00','2026-02-10 17:00:00'),
  (1, 'Ngày thi Sơ loại & Thuyết trình',       'PRESENTATION', 'FPT HCM — Hội trường B', '2026-02-16 06:00:00','2026-02-16 19:00:00'),
  (1, 'Vòng Chung kết & Trao giải',            'AWARDS',       'FPT HCM — Hội trường A', '2026-03-10 08:00:00','2026-03-10 18:00:00');
```

---

## 7. Verification Queries

```sql
-- 1) Leaderboard Track 1
SELECT * FROM v_round_leaderboard WHERE track_id = 1 ORDER BY weighted_avg_score DESC;

-- 2) Leaderboard theo bảng (Fall 2025 nhiều bảng)
SELECT * FROM v_round_leaderboard WHERE round_id = 1 ORDER BY assigned_group, weighted_avg_score DESC;

-- 3) Đội thi ở Track/Round nào
SELECT * FROM v_team_track_assignment WHERE hackathon_id = 1;

-- 4) Phương sai điểm Judge INTERNAL vs EXTERNAL
SELECT * FROM v_judge_score_variance WHERE round_id = 1 ORDER BY criterion_id, judge_type;

-- 5) Tiến độ chấm điểm
SELECT * FROM v_scoring_progress WHERE round_id = 1;

-- 6) Dataset RBL ẩn danh
SELECT * FROM v_rbl_anonymized WHERE round_id = 1;

-- 7) Top 2 mỗi Track cho Chung kết
SELECT team_id, track_id, weighted_avg_score,
       RANK() OVER (PARTITION BY track_id ORDER BY weighted_avg_score DESC) AS rank_in_track
FROM v_round_leaderboard
WHERE round_id = 1;

-- 8) Phân công Judge Chung kết (chỉ EXTERNAL)
INSERT INTO judge_assignments (judge_id, track_id, round_id, assignment_type, assigned_by)
VALUES (3, NULL, 2, 'FINAL_EXTERNAL', 1);

-- 9) Nộp bài Chung kết
INSERT INTO submissions (team_id, track_id, round_id, repo_url, slide_url, status)
VALUES (1, NULL, 2, 'https://github.com/...', 'https://slides...', 'SUBMITTED');
```

---

## 8. JPA Convention — Bảng tham chiếu nhanh cho Entity

| Schema feature | JPA mapping |
|---|---|
| `SERIAL` PK | `@Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id` |
| `FK col INT NOT NULL` | `@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "col", nullable = false) private Parent parent` |
| `FK col INT NULL` | `@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "col") private Parent parent` (nullable mặc định = true) |
| Enum `VARCHAR(20)` | `@Enumerated(EnumType.STRING) @Column(name = "...", length = 20)` |
| `BOOLEAN NOT NULL DEFAULT FALSE` | `@Column(nullable = false) private Boolean flag = false` |
| `JSON` (audit_logs.detail) | `@JdbcTypeCode(SqlTypes.JSON) private tools.jackson.databind.JsonNode detail` |
| `DATETIME NOT NULL DEFAULT NOW()` | `private LocalDateTime createdAt = LocalDateTime.now()` |
| `DATETIME ON UPDATE NOW()` | thêm `@PreUpdate void preUpdate(){this.updatedAt = LocalDateTime.now();}` |
| `UNIQUE KEY (...)` | `@Table(uniqueConstraints = @UniqueConstraint(columnNames = {...}))` |
| `CHECK (...)` | KHÔNG biểu diễn ở JPA — enforce ở DB |
| XOR FK (`track_id` XOR `round_id`) | Cả 2 `@ManyToOne` nullable=true; doc comment trigger enforce |
| Generated UNIQUE column | KHÔNG khai báo ở entity (DB auto-compute) |

---

## 9. Quick recap — 11 BC ảnh hưởng entity Java

```
Round.java       :: drop FK track  -> add @ManyToOne Hackathon hackathon (BC-01)
                    add isFinal, roundType, lateSubmissionPolicy
Track.java       :: drop FK hackathon -> add @ManyToOne Round round (BC-02)
                    add topic, sequenceOrder
Criteria.java    :: round nullable=true; add @ManyToOne Track track nullable=true (BC-03)
Submission.java  :: track + round XOR FK (BC-06) — entity mới
JudgeAssignment.java :: round nullable=true; add @ManyToOne Track track nullable=true (BC-07)
                       enum thêm FINAL_EXTERNAL, HEAD, CALIBRATION
MentorAssignment.java:: giữ nguyên (BC-08)
Event.java       :: enum EventType bỏ TEAM_MEETING (BC-09)
WildcardReview.java :: thêm track_id (BC-10) — entity mới
User.java        :: thêm isDeptHead (FIX-02)
Team.java        :: bỏ 3 cột registration/assigned, thêm hackathonId (BC-05) — entity mới
TeamRoundTrack.java :: entity mới (BC-04)
```

---

## 10. Notes vận hành

1. **Hibernate `ddl-auto=update` KHÔNG migrate được v2.1 → v3.0**.
   Lý do: đảo FK Track ↔ Round cần DROP/ADD column + drop trigger cũ. Hibernate `update` chỉ thêm cột mới chứ không drop cột cũ và không tạo trigger.
   Cách xử lý:
   - DROP & CREATE lại DB `SealHackathon` bằng đúng file này, hoặc
   - Migrate thủ công bằng Flyway/Liquibase.

2. **DataInitializer dev profile** (file `config/DataInitializer.java`) đang seed theo schema v2.1 — sẽ FAIL khi entity được port v3.0. Phải rewrite sau khi entity hoàn tất.

3. **Triggers throw SIGNAL SQLSTATE '45000'** → Hibernate wrap thành `org.springframework.dao.DataIntegrityViolationException`. `GlobalExceptionHandler` đã có handler cho exception này — kiểm tra mapping trước khi production.

4. **Generated VIRTUAL column** không chiếm storage. Mọi UNIQUE check thực thi qua hidden index trên column ảo — performance ~UNIQUE thường.
