package com.sealhackathon.api.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * B3 — Remove calibration feature: archive legacy rows, drop FK/column/table.
 */
@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class CalibrationRemovalSchemaMigration implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        if (!tableExists("calibration_sessions")) {
            dropCalibrationColumnIfExists();
            log.info("[CalibrationRemovalSchemaMigration] calibration_sessions already removed");
            return;
        }

        createArchiveTablesIfMissing();
        archiveCalibrationSessions();
        archiveCalibrationScores();
        deleteCalibrationScores();
        dropCalibrationFkAndColumn();
        dropCalibrationSessionsTable();
        log.info("[CalibrationRemovalSchemaMigration] calibration feature removed (idempotent)");
    }

    private void createArchiveTablesIfMissing() {
        if (!tableExists("calibration_sessions_archive")) {
            try {
                jdbcTemplate.execute("""
                        CREATE TABLE calibration_sessions_archive (
                            id                   INT         NOT NULL PRIMARY KEY,
                            round_id             INT         NOT NULL,
                            track_id             INT         NULL,
                            sample_submission_id INT         NULL,
                            status               VARCHAR(20) NOT NULL,
                            target_score         FLOAT       NULL,
                            instructions         TEXT        NULL,
                            started_at           DATETIME    NULL,
                            ended_at             DATETIME    NULL,
                            created_by           INT         NULL,
                            archived_at          DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP
                        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                        """);
                log.info("[CalibrationRemovalSchemaMigration] Created calibration_sessions_archive");
            } catch (Exception ex) {
                log.warn("[CalibrationRemovalSchemaMigration] calibration_sessions_archive: {}", ex.getMessage());
            }
        }

        if (!tableExists("scores_calibration_archive")) {
            try {
                jdbcTemplate.execute("""
                        CREATE TABLE scores_calibration_archive (
                            id                     INT          NOT NULL PRIMARY KEY,
                            submission_id          INT          NOT NULL,
                            judge_id               INT          NOT NULL,
                            criterion_id           INT          NOT NULL,
                            score_value            FLOAT        NOT NULL,
                            comment                TEXT         NULL,
                            score_type             VARCHAR(20)  NOT NULL,
                            is_final               BOOLEAN      NOT NULL,
                            calibration_session_id INT          NULL,
                            scored_at              DATETIME     NULL,
                            updated_at             DATETIME     NULL,
                            archived_at            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
                        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                        """);
                log.info("[CalibrationRemovalSchemaMigration] Created scores_calibration_archive");
            } catch (Exception ex) {
                log.warn("[CalibrationRemovalSchemaMigration] scores_calibration_archive: {}", ex.getMessage());
            }
        }
    }

    private void archiveCalibrationSessions() {
        if (!tableExists("calibration_sessions") || !tableExists("calibration_sessions_archive")) {
            return;
        }
        try {
            int archived = jdbcTemplate.update("""
                    INSERT INTO calibration_sessions_archive (
                        id, round_id, track_id, sample_submission_id, status,
                        target_score, instructions, started_at, ended_at, created_by
                    )
                    SELECT cs.id, cs.round_id, cs.track_id, cs.sample_submission_id, cs.status,
                           cs.target_score, cs.instructions, cs.started_at, cs.ended_at, cs.created_by
                      FROM calibration_sessions cs
                     WHERE NOT EXISTS (
                           SELECT 1 FROM calibration_sessions_archive a WHERE a.id = cs.id
                     )
                    """);
            if (archived > 0) {
                log.info("[CalibrationRemovalSchemaMigration] Archived {} calibration_sessions rows", archived);
            }
        } catch (Exception ex) {
            log.warn("[CalibrationRemovalSchemaMigration] archive calibration_sessions: {}", ex.getMessage());
        }
    }

    private void archiveCalibrationScores() {
        if (!tableExists("scores") || !tableExists("scores_calibration_archive")) {
            return;
        }
        try {
            int archived = jdbcTemplate.update("""
                    INSERT INTO scores_calibration_archive (
                        id, submission_id, judge_id, criterion_id, score_value, comment,
                        score_type, is_final, calibration_session_id, scored_at, updated_at
                    )
                    SELECT s.id, s.submission_id, s.judge_id, s.criterion_id, s.score_value, s.comment,
                           s.score_type, s.is_final, s.calibration_session_id, s.scored_at, s.updated_at
                      FROM scores s
                     WHERE s.score_type = 'CALIBRATION'
                       AND NOT EXISTS (
                           SELECT 1 FROM scores_calibration_archive a WHERE a.id = s.id
                       )
                    """);
            if (archived > 0) {
                log.info("[CalibrationRemovalSchemaMigration] Archived {} CALIBRATION scores", archived);
            }
        } catch (Exception ex) {
            log.warn("[CalibrationRemovalSchemaMigration] archive CALIBRATION scores: {}", ex.getMessage());
        }
    }

    private void deleteCalibrationScores() {
        if (!tableExists("scores")) {
            return;
        }
        try {
            int deleted = jdbcTemplate.update("DELETE FROM scores WHERE score_type = 'CALIBRATION'");
            if (deleted > 0) {
                log.info("[CalibrationRemovalSchemaMigration] Deleted {} CALIBRATION scores", deleted);
            }
        } catch (Exception ex) {
            log.warn("[CalibrationRemovalSchemaMigration] delete CALIBRATION scores: {}", ex.getMessage());
        }
    }

    private void dropCalibrationFkAndColumn() {
        dropFkIfExists("scores", "fk_scores_calib");
        dropColumnIfExists("scores", "calibration_session_id");
    }

    private void dropCalibrationColumnIfExists() {
        dropColumnIfExists("scores", "calibration_session_id");
    }

    private void dropCalibrationSessionsTable() {
        if (!tableExists("calibration_sessions")) {
            return;
        }
        try {
            jdbcTemplate.execute("DROP TABLE calibration_sessions");
            log.info("[CalibrationRemovalSchemaMigration] Dropped calibration_sessions");
        } catch (Exception ex) {
            log.warn("[CalibrationRemovalSchemaMigration] drop calibration_sessions: {}", ex.getMessage());
        }
    }

    private void dropColumnIfExists(String table, String column) {
        if (!columnExists(table, column)) {
            return;
        }
        try {
            jdbcTemplate.execute("ALTER TABLE " + table + " DROP COLUMN " + column);
            log.info("[CalibrationRemovalSchemaMigration] Dropped {}.{}", table, column);
        } catch (Exception ex) {
            log.warn("[CalibrationRemovalSchemaMigration] drop {}.{}: {}", table, column, ex.getMessage());
        }
    }

    private void dropFkIfExists(String table, String constraintName) {
        if (!constraintExists(table, constraintName)) {
            return;
        }
        try {
            jdbcTemplate.execute("ALTER TABLE " + table + " DROP FOREIGN KEY " + constraintName);
            log.info("[CalibrationRemovalSchemaMigration] Dropped FK {}", constraintName);
        } catch (Exception ex) {
            log.warn("[CalibrationRemovalSchemaMigration] drop FK {}: {}", constraintName, ex.getMessage());
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
            log.debug("[CalibrationRemovalSchemaMigration] tableExists metadata failed for {}: {}", table, ex.getMessage());
        }
        try {
            jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private boolean columnExists(String table, String column) {
        if (!tableExists(table)) {
            return false;
        }
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
            try {
                jdbcTemplate.queryForObject("SELECT " + column + " FROM " + table + " WHERE 1=0", Object.class);
                return true;
            } catch (Exception ignored) {
                return false;
            }
        }
    }

    private boolean constraintExists(String table, String constraintName) {
        try {
            Integer count = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*)
                      FROM information_schema.TABLE_CONSTRAINTS
                     WHERE TABLE_SCHEMA = DATABASE()
                       AND TABLE_NAME = ?
                       AND CONSTRAINT_NAME = ?
                    """, Integer.class, table, constraintName);
            return count != null && count > 0;
        } catch (Exception ex) {
            return false;
        }
    }
}
