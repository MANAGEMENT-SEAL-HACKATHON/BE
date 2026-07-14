package com.sealhackathon.api.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Idempotent: calibration_sessions.track_id (nullable FK → tracks).
 * Pattern mirrors {@link Gd03V41SchemaMigration}.
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class CalibrationSessionTrackSchemaMigration implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        addColumnIfMissing("calibration_sessions", "track_id", "INT NULL");
        addFkIfMissing(
                "calibration_sessions",
                "fk_cs_track",
                "ALTER TABLE calibration_sessions ADD CONSTRAINT fk_cs_track FOREIGN KEY (track_id) REFERENCES tracks(id)");
        log.info("[CalibrationSessionTrackSchemaMigration] track_id delta applied (idempotent)");
    }

    private void addColumnIfMissing(String table, String column, String ddl) {
        if (columnExists(table, column)) {
            return;
        }
        try {
            jdbcTemplate.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + ddl);
            log.info("[CalibrationSessionTrackSchemaMigration] Added {}.{}", table, column);
        } catch (Exception ex) {
            log.warn("[CalibrationSessionTrackSchemaMigration] Could not add {}.{}: {}", table, column, ex.getMessage());
        }
    }

    private void addFkIfMissing(String table, String constraintName, String ddl) {
        if (!columnExists(table, "track_id") || constraintExists(table, constraintName)) {
            return;
        }
        try {
            jdbcTemplate.execute(ddl);
            log.info("[CalibrationSessionTrackSchemaMigration] Added FK {}", constraintName);
        } catch (Exception ex) {
            log.warn("[CalibrationSessionTrackSchemaMigration] FK {}: {}", constraintName, ex.getMessage());
        }
    }

    private boolean columnExists(String table, String column) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    """
                    SELECT COUNT(*) FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?
                    """,
                    Integer.class,
                    table,
                    column);
            return count != null && count > 0;
        } catch (Exception ex) {
            // H2 / tests may lack information_schema shape — try DESCRIBE-style fallback
            try {
                jdbcTemplate.queryForObject(
                        "SELECT " + column + " FROM " + table + " WHERE 1=0", Object.class);
                return true;
            } catch (Exception ignored) {
                return false;
            }
        }
    }

    private boolean constraintExists(String table, String constraintName) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    """
                    SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
                    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND CONSTRAINT_NAME = ?
                    """,
                    Integer.class,
                    table,
                    constraintName);
            return count != null && count > 0;
        } catch (Exception ex) {
            return false;
        }
    }
}
