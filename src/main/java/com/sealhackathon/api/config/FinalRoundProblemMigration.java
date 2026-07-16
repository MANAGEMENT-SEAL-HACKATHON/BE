package com.sealhackathon.api.config;

import com.sealhackathon.api.common.audit.AuditAction;
import com.sealhackathon.api.common.audit.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * CK-05 — clear PDF đề riêng trên vòng Chung kết; backup URL vào audit; backfill problem_released_at.
 *
 * @see src/main/resources/db/manual/V20260716_final_round_clear_problem_pdf.sql
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class FinalRoundProblemMigration implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;
    private final AuditService auditService;

    @Override
    public void run(String... args) {
        if (!tableExists("rounds")) {
            return;
        }
        addColumnIfMissing("rounds", "final_problem_migration_cleared_at", "DATETIME(6) NULL");
        addColumnIfMissing("rounds", "final_problem_migration_banner_dismissed_at", "DATETIME(6) NULL");
        clearFinalRoundProblemPdfs();
        backfillFinalProblemReleasedAt();
        log.info("[FinalRoundProblemMigration] Final-round problem PDF migration applied (idempotent)");
    }

    private void clearFinalRoundProblemPdfs() {
        List<Map<String, Object>> rows;
        try {
            rows = jdbcTemplate.queryForList("""
                    SELECT id,
                           problem_statement_url AS url,
                           problem_statement_storage_key AS storage_key,
                           problem_statement_original_filename AS filename
                      FROM rounds
                     WHERE is_final = TRUE
                       AND (
                            problem_statement_url IS NOT NULL
                         OR problem_statement_storage_key IS NOT NULL
                         OR problem_statement_original_filename IS NOT NULL
                       )
                    """);
        } catch (Exception ex) {
            log.debug("[FinalRoundProblemMigration] clear query skipped: {}", ex.getMessage());
            return;
        }
        if (rows.isEmpty()) {
            return;
        }
        for (Map<String, Object> row : rows) {
            Integer roundId = ((Number) row.get("id")).intValue();
            Map<String, Object> backup = new LinkedHashMap<>();
            backup.put("problemStatementUrl", row.get("url"));
            backup.put("problemStatementStorageKey", row.get("storage_key"));
            backup.put("problemStatementOriginalFilename", row.get("filename"));
            try {
                auditService.log(AuditAction.ROUND_FINAL_PROBLEM_PDF_CLEARED, "rounds", roundId, backup);
            } catch (Exception ex) {
                log.warn("[FinalRoundProblemMigration] audit backup failed for round {}: {}",
                        roundId, ex.getMessage());
            }
        }
        try {
            int cleared = jdbcTemplate.update("""
                    UPDATE rounds
                       SET problem_statement_url = NULL,
                           problem_statement_storage_key = NULL,
                           problem_statement_original_filename = NULL,
                           final_problem_migration_cleared_at =
                               COALESCE(final_problem_migration_cleared_at, CURRENT_TIMESTAMP(6))
                     WHERE is_final = TRUE
                       AND (
                            problem_statement_url IS NOT NULL
                         OR problem_statement_storage_key IS NOT NULL
                         OR problem_statement_original_filename IS NOT NULL
                       )
                    """);
            if (cleared > 0) {
                log.info("[FinalRoundProblemMigration] Cleared legacy final-round problem PDF on {} round(s)",
                        cleared);
            }
        } catch (Exception ex) {
            log.warn("[FinalRoundProblemMigration] clear update failed: {}", ex.getMessage());
        }
    }

    private void backfillFinalProblemReleasedAt() {
        try {
            int updated = jdbcTemplate.update("""
                    UPDATE rounds
                       SET problem_released_at = activated_at
                     WHERE is_final = TRUE
                       AND problem_released_at IS NULL
                       AND activated_at IS NOT NULL
                    """);
            if (updated > 0) {
                log.info("[FinalRoundProblemMigration] Backfilled problem_released_at for {} final round(s)",
                        updated);
            }
        } catch (Exception ex) {
            log.debug("[FinalRoundProblemMigration] backfill skipped: {}", ex.getMessage());
        }
    }

    private void addColumnIfMissing(String table, String column, String ddl) {
        if (columnExists(table, column)) {
            return;
        }
        try {
            jdbcTemplate.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + ddl);
            log.info("[FinalRoundProblemMigration] Added {}.{}", table, column);
        } catch (Exception ex) {
            log.warn("[FinalRoundProblemMigration] Could not add {}.{}: {}", table, column, ex.getMessage());
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
            log.debug("[FinalRoundProblemMigration] tableExists metadata failed: {}", ex.getMessage());
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
            return false;
        }
    }
}
