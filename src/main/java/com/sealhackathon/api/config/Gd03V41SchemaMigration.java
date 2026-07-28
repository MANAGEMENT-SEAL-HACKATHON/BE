package com.sealhackathon.api.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * GD03 v4.1 schema delta — idempotent; chạy trước seed GĐ1/GĐ2.
 *
 * @see src/main/resources/db/manual/V20260529_gd03_v4_1_delta.sql
 */
@Slf4j
@Component
@Order(0)
@RequiredArgsConstructor
public class Gd03V41SchemaMigration implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        migrateRoundsPublish();
        migratePresentationGd3();
        migrateSubmissionsV41();
        migrateParticipationStatusToTeamRoundTracks();
        migratePrizesHackathonUnique();
        createSubmissionMetadataTable();
        migrateSeasonEnumFpt();
        migrateHackathonBannerLegacyUrls();
        migrateRoundProblemStatementFiles();
        migrateTrackProblemStatementFiles();
        createHackathonRegistrationWithdrawalsTable();
        migrateTeamFormationSubmittedAt();
        migrateFormationGraceDeadlineAt();
        migrateRegistrationClosedEarlyAt();
        log.info("[Gd03V41SchemaMigration] GD03 v4.1 schema delta applied (idempotent)");
    }

    /** GĐ3 — presentation queue, timer, controller auth columns. */
    private void migratePresentationGd3() {
        addColumnIfMissing("rounds", "default_presentation_minutes", "INT NOT NULL DEFAULT 10");
        addColumnIfMissing("rounds", "default_qa_minutes", "INT NOT NULL DEFAULT 5");
        addColumnIfMissing("rounds", "controller_judge_id", "BIGINT NULL");
        addColumnIfMissing("rounds", "presentation_shuffled", "TINYINT(1) NOT NULL DEFAULT 0");

        addColumnIfMissing("tracks", "presentation_minutes", "INT NULL");
        addColumnIfMissing("tracks", "qa_minutes", "INT NULL");
        addColumnIfMissing("tracks", "controller_judge_id", "BIGINT NULL");
        addColumnIfMissing("tracks", "presentation_shuffled", "TINYINT(1) NOT NULL DEFAULT 0");

        addColumnIfMissing("presentation_slots", "submission_id", "BIGINT NULL");
        addColumnIfMissing("presentation_slots", "track_id", "BIGINT NULL");
        addColumnIfMissing("presentation_slots", "timer_phase", "VARCHAR(20) NOT NULL DEFAULT 'IDLE'");
        addColumnIfMissing("presentation_slots", "timer_phase_before_pause", "VARCHAR(20) NULL");
        addColumnIfMissing("presentation_slots", "presentation_started_at", "DATETIME(6) NULL");
        addColumnIfMissing("presentation_slots", "qa_started_at", "DATETIME(6) NULL");
        addColumnIfMissing("presentation_slots", "paused_at", "DATETIME(6) NULL");
        addColumnIfMissing("presentation_slots", "paused_accumulated_seconds", "INT NOT NULL DEFAULT 0");

        addColumnIfMissing("submissions", "slide_storage_key", "VARCHAR(512) NULL");
        addColumnIfMissing("submissions", "slide_original_filename", "VARCHAR(255) NULL");
        addColumnIfMissing("submissions", "slide_content_type", "VARCHAR(100) NULL");
        addColumnIfMissing("submissions", "slide_size_bytes", "BIGINT NULL");
        addColumnIfMissing("submissions", "slide_uploaded_at", "DATETIME(6) NULL");
    }

    private void migrateRoundsPublish() {
        addColumnIfMissing("rounds", "activated_at", "DATETIME(6) NULL");
        addColumnIfMissing("rounds", "is_published", "TINYINT(1) NOT NULL DEFAULT 0");
        addColumnIfMissing("rounds", "published_at", "DATETIME(6) NULL");
        addColumnIfMissing("rounds", "published_by", "BIGINT NULL");
    }

    private void migrateSubmissionsV41() {
        addColumnIfMissing("submissions", "hackathon_id", "BIGINT NULL");

        if (columnExists("submissions", "track_id") && columnExists("submissions", "round_id")) {
            jdbcTemplate.update("""
                    UPDATE submissions s
                    INNER JOIN tracks t ON t.id = s.track_id
                       SET s.round_id = t.round_id
                     WHERE s.track_id IS NOT NULL
                       AND (s.round_id IS NULL OR s.round_id <> t.round_id)
                    """);
            jdbcTemplate.update("""
                    UPDATE submissions s
                    INNER JOIN rounds r ON r.id = s.round_id
                       SET s.hackathon_id = r.hackathon_id
                     WHERE s.round_id IS NOT NULL
                       AND s.hackathon_id IS NULL
                    """);
        }

        if (!columnExists("submissions", "scoring_key")) {
            try {
                jdbcTemplate.execute("""
                        ALTER TABLE submissions
                        ADD COLUMN scoring_key VARCHAR(40) AS (
                            IF(track_id IS NOT NULL, CONCAT('T', track_id), CONCAT('R', round_id))
                        ) STORED
                        """);
                log.info("[Gd03V41SchemaMigration] Added submissions.scoring_key (generated)");
            } catch (Exception ex) {
                log.warn("[Gd03V41SchemaMigration] submissions.scoring_key: {}", ex.getMessage());
            }
        }

        dropIndexIfExists("submissions", "uk_subm_team_track");
        dropIndexIfExists("submissions", "uk_subm_team_round");
        addUniqueIndexIfMissing("submissions", "uk_sub_team_scoring", "team_id", "scoring_key");
    }

    private void migrateParticipationStatusToTeamRoundTracks() {
        addColumnIfMissing(
                "team_round_tracks",
                "participation_status",
                "VARCHAR(20) NOT NULL DEFAULT 'PARTICIPATING'");

        if (columnExists("team_round_participation", "participation_status")) {
            jdbcTemplate.update("""
                    UPDATE team_round_tracks trt
                    INNER JOIN tracks t ON t.id = trt.track_id
                    INNER JOIN team_round_participation trp
                            ON trp.team_id = trt.team_id AND trp.round_id = t.round_id
                       SET trt.participation_status = trp.participation_status
                    """);
            dropColumnIfExists("team_round_participation", "participation_status");
        }
    }

    private void migratePrizesHackathonUnique() {
        addColumnIfMissing("prizes", "hackathon_id", "BIGINT NULL");
        if (columnExists("prizes", "hackathon_id") && columnExists("prizes", "round_id")) {
            jdbcTemplate.update("""
                    UPDATE prizes p
                    INNER JOIN rounds r ON r.id = p.round_id
                       SET p.hackathon_id = r.hackathon_id
                     WHERE p.hackathon_id IS NULL
                    """);
        }
        dropIndexIfExists("prizes", "uk_prizes_round_team");
        dropIndexIfExists("prizes", "uk_prizes_round_rank");
        addUniqueIndexIfMissing("prizes", "uk_prizes_hackathon_team", "hackathon_id", "team_id");
        addUniqueIndexIfMissing("prizes", "uk_prizes_hackathon_rank", "hackathon_id", "prize_rank");
    }

    private void createSubmissionMetadataTable() {
        if (tableExists("submission_metadata")) {
            return;
        }
        try {
            jdbcTemplate.execute("""
                    CREATE TABLE submission_metadata (
                        submission_id BIGINT NOT NULL PRIMARY KEY,
                        repo_name VARCHAR(255) NULL,
                        repo_language VARCHAR(100) NULL,
                        repo_last_commit_at DATETIME(6) NULL,
                        metadata_fetch_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                        fetched_at DATETIME(6) NULL,
                        CONSTRAINT fk_submeta_submission
                            FOREIGN KEY (submission_id) REFERENCES submissions(id) ON DELETE CASCADE
                    ) ENGINE=InnoDB
                    """);
            log.info("[Gd03V41SchemaMigration] Created submission_metadata");
        } catch (Exception ex) {
            log.debug("[Gd03V41SchemaMigration] submission_metadata create skipped: {}", ex.getMessage());
        }
    }

    private void addColumnIfMissing(String table, String column, String ddl) {
        if (columnExists(table, column)) {
            return;
        }
        try {
            jdbcTemplate.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + ddl);
            log.info("[Gd03V41SchemaMigration] Added {}.{}", table, column);
        } catch (Exception ex) {
            log.warn("[Gd03V41SchemaMigration] Could not add {}.{}: {}", table, column, ex.getMessage());
        }
    }

    private void dropColumnIfExists(String table, String column) {
        if (!columnExists(table, column)) {
            return;
        }
        try {
            jdbcTemplate.execute("ALTER TABLE " + table + " DROP COLUMN " + column);
            log.info("[Gd03V41SchemaMigration] Dropped {}.{}", table, column);
        } catch (Exception ex) {
            log.warn("[Gd03V41SchemaMigration] Could not drop {}.{}: {}", table, column, ex.getMessage());
        }
    }

    private void addUniqueIndexIfMissing(String table, String indexName, String... columns) {
        if (indexExists(table, indexName)) {
            return;
        }
        if (columns.length > 0 && !columnExists(table, columns[0])) {
            return;
        }
        String cols = String.join(", ", columns);
        try {
            jdbcTemplate.execute("ALTER TABLE " + table + " ADD UNIQUE KEY " + indexName + " (" + cols + ")");
            log.info("[Gd03V41SchemaMigration] Added index {} on {}", indexName, table);
        } catch (Exception ex) {
            log.warn("[Gd03V41SchemaMigration] Index {} on {}: {}", indexName, table, ex.getMessage());
        }
    }

    private void dropIndexIfExists(String table, String indexName) {
        if (!indexExists(table, indexName)) {
            return;
        }
        try {
            jdbcTemplate.execute("ALTER TABLE " + table + " DROP INDEX " + indexName);
            log.info("[Gd03V41SchemaMigration] Dropped index {} on {}", indexName, table);
        } catch (Exception ex) {
            log.warn("[Gd03V41SchemaMigration] Could not drop index {}: {}", indexName, ex.getMessage());
        }
    }

    private boolean tableExists(String table) {
        try {
            Integer count = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*)
                      FROM information_schema.TABLES
                     WHERE TABLE_SCHEMA = DATABASE()
                       AND UPPER(TABLE_NAME) = UPPER(?)
                    """, Integer.class, table);
            if (count != null && count > 0) {
                return true;
            }
        } catch (Exception ex) {
            log.debug("[Gd03V41SchemaMigration] tableExists metadata query failed for {}: {}", table, ex.getMessage());
        }
        try {
            jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private boolean columnExists(String table, String column) {
        try {
            Integer count = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*)
                      FROM information_schema.COLUMNS
                     WHERE TABLE_SCHEMA = DATABASE()
                       AND UPPER(TABLE_NAME) = UPPER(?)
                       AND UPPER(COLUMN_NAME) = UPPER(?)
                    """, Integer.class, table, column);
            return count != null && count > 0;
        } catch (Exception ex) {
            log.debug("[Gd03V41SchemaMigration] columnExists skipped for {}.{}: {}", table, column, ex.getMessage());
            return false;
        }
    }

    private boolean indexExists(String table, String indexName) {
        try {
            Integer count = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*)
                      FROM information_schema.STATISTICS
                     WHERE TABLE_SCHEMA = DATABASE()
                       AND TABLE_NAME = ?
                       AND INDEX_NAME = ?
                    """, Integer.class, table, indexName);
            return count != null && count > 0;
        } catch (Exception ex) {
            log.debug("[Gd03V41SchemaMigration] indexExists skipped for {}.{}: {}", table, indexName, ex.getMessage());
            return false;
        }
    }

    /** FPT chỉ có 3 mùa: Spring, Summer, Fall. */
    private void migrateSeasonEnumFpt() {
        if (!tableExists("hackathons")) {
            return;
        }
        try {
            jdbcTemplate.update("UPDATE hackathons SET season = 'Spring' WHERE season = 'Winter'");
            jdbcTemplate.execute(
                    "ALTER TABLE hackathons MODIFY COLUMN season ENUM('Spring','Summer','Fall') NOT NULL");
            log.info("[Gd03V41SchemaMigration] hackathons.season → Spring/Summer/Fall");
        } catch (Exception ex) {
            log.debug("[Gd03V41SchemaMigration] migrateSeasonEnumFpt skipped: {}", ex.getMessage());
        }
    }

    /** Banner cũ dạng URL ngoài — xóa để seed tạo file upload nội bộ. */
    private void migrateHackathonBannerLegacyUrls() {
        if (!columnExists("hackathons", "banner_url")) {
            return;
        }
        try {
            int cleared = jdbcTemplate.update("""
                    UPDATE hackathons
                       SET banner_url = NULL
                     WHERE banner_url IS NOT NULL
                       AND (banner_url LIKE 'http://%' OR banner_url LIKE 'https://%')
                    """);
            if (cleared > 0) {
                log.info("[Gd03V41SchemaMigration] Cleared {} legacy http banner_url values", cleared);
            }
        } catch (Exception ex) {
            log.debug("[Gd03V41SchemaMigration] migrateHackathonBannerLegacyUrls skipped: {}", ex.getMessage());
        }
    }

    /** Đề bài: chuyển từ URL ngoài sang upload PDF nội bộ. */
    private void migrateRoundProblemStatementFiles() {
        if (!tableExists("rounds")) {
            return;
        }
        addColumnIfMissing("rounds", "problem_statement_storage_key", "VARCHAR(512) NULL");
        addColumnIfMissing("rounds", "problem_statement_original_filename", "VARCHAR(255) NULL");
        try {
            int cleared = jdbcTemplate.update("""
                    UPDATE rounds
                       SET problem_statement_url = NULL
                     WHERE problem_statement_url IS NOT NULL
                       AND (problem_statement_url LIKE 'http://%' OR problem_statement_url LIKE 'https://%')
                    """);
            if (cleared > 0) {
                log.info("[Gd03V41SchemaMigration] Cleared {} legacy http problem_statement_url values", cleared);
            }
        } catch (Exception ex) {
            log.debug("[Gd03V41SchemaMigration] migrateRoundProblemStatementFiles skipped: {}", ex.getMessage());
        }
    }

    /** Đề bài sơ loại — mỗi track một PDF. */
    private void migrateTrackProblemStatementFiles() {
        if (!tableExists("tracks")) {
            return;
        }
        addColumnIfMissing("tracks", "problem_statement_storage_key", "VARCHAR(512) NULL");
        addColumnIfMissing("tracks", "problem_statement_original_filename", "VARCHAR(255) NULL");
        addColumnIfMissing("tracks", "problem_statement_url", "TEXT NULL");
    }

    private void createHackathonRegistrationWithdrawalsTable() {
        if (tableExists("hackathon_registration_withdrawals")) {
            return;
        }
        try {
            jdbcTemplate.execute("""
                    CREATE TABLE hackathon_registration_withdrawals (
                        id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                        hackathon_id BIGINT NOT NULL,
                        user_id BIGINT NOT NULL,
                        withdrawn_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                        CONSTRAINT uk_hackathon_reg_withdrawal_user UNIQUE (hackathon_id, user_id),
                        CONSTRAINT fk_hackathon_reg_withdrawal_hackathon
                            FOREIGN KEY (hackathon_id) REFERENCES hackathons(id) ON DELETE CASCADE,
                        CONSTRAINT fk_hackathon_reg_withdrawal_user
                            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                    ) ENGINE=InnoDB
                    """);
            log.info("[Gd03V41SchemaMigration] Created hackathon_registration_withdrawals");
        } catch (Exception ex) {
            log.debug("[Gd03V41SchemaMigration] hackathon_registration_withdrawals create skipped: {}", ex.getMessage());
        }
    }

    private void migrateTeamFormationSubmittedAt() {
        addColumnIfMissing("teams", "formation_submitted_at", "DATETIME(6) NULL");
    }

    private void migrateFormationGraceDeadlineAt() {
        addColumnIfMissing("teams", "formation_grace_deadline_at", "DATETIME(6) NULL");
        try {
            int updated = jdbcTemplate.update("""
                    UPDATE teams t
                    INNER JOIN hackathons h ON h.id = t.hackathon_id
                    SET t.formation_grace_deadline_at = DATE_ADD(h.registration_closed_early_at, INTERVAL 24 HOUR)
                    WHERE t.status = 'PENDING'
                      AND t.formation_submitted_at IS NULL
                      AND t.formation_grace_deadline_at IS NULL
                      AND h.registration_closed_early_at IS NOT NULL
                    """);
            if (updated > 0) {
                log.info("[Gd03V41SchemaMigration] Backfilled formation_grace_deadline_at for {} teams", updated);
            }
        } catch (Exception ex) {
            log.debug("[Gd03V41SchemaMigration] formation grace backfill skipped: {}", ex.getMessage());
        }
    }

    private void migrateRegistrationClosedEarlyAt() {
        addColumnIfMissing("hackathons", "registration_closed_early_at", "DATETIME(6) NULL");
    }
}
