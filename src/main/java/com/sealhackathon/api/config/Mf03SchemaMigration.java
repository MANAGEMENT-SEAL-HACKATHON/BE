package com.sealhackathon.api.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Bổ sung cột MF-03 khi Hibernate ddl-auto chưa tạo (prod / DB có sẵn từ GĐ1–GĐ2).
 */
@Slf4j
@Component
@Order(0)
@RequiredArgsConstructor
public class Mf03SchemaMigration implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        addColumnIfMissing("rounds", "activated_at", "DATETIME(6) NULL");
        addColumnIfMissing(
                "team_round_participation",
                "participation_status",
                "VARCHAR(20) NOT NULL DEFAULT 'PARTICIPATING'");
        addUniqueIndexIfMissing("prizes", "uk_prizes_round_team", "round_id", "team_id");
        addUniqueIndexIfMissing("prizes", "uk_prizes_round_rank", "round_id", "prize_rank");
    }

    private void addColumnIfMissing(String table, String column, String ddl) {
        if (columnExists(table, column)) {
            return;
        }
        jdbcTemplate.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + ddl);
        log.info("[Mf03SchemaMigration] Added {}.{}", table, column);
    }

    private void addUniqueIndexIfMissing(String table, String indexName, String... columns) {
        if (indexExists(table, indexName)) {
            return;
        }
        String cols = String.join(", ", columns);
        jdbcTemplate.execute("ALTER TABLE " + table + " ADD UNIQUE KEY " + indexName + " (" + cols + ")");
        log.info("[Mf03SchemaMigration] Added index {} on {}", indexName, table);
    }

    private boolean columnExists(String table, String column) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                  FROM information_schema.COLUMNS
                 WHERE TABLE_SCHEMA = DATABASE()
                   AND TABLE_NAME = ?
                   AND COLUMN_NAME = ?
                """, Integer.class, table, column);
        return count != null && count > 0;
    }

    private boolean indexExists(String table, String indexName) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                  FROM information_schema.STATISTICS
                 WHERE TABLE_SCHEMA = DATABASE()
                   AND TABLE_NAME = ?
                   AND INDEX_NAME = ?
                """, Integer.class, table, indexName);
        return count != null && count > 0;
    }
}
