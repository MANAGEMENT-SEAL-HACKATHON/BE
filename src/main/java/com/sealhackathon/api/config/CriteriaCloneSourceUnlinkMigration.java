package com.sealhackathon.api.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Bỏ liên kết FK {@code source_criteria_id} giữa track/round khác nhau — clone chỉ là bản sao, xóa từng dòng độc lập.
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class CriteriaCloneSourceUnlinkMigration implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        if (!columnExists("criteria", "source_criteria_id")) {
            return;
        }
        int unlinked = jdbcTemplate.update("""
                UPDATE criteria c
                INNER JOIN criteria src ON c.source_criteria_id = src.id
                   SET c.source_criteria_id = NULL
                 WHERE (c.track_id IS NOT NULL AND src.track_id IS NOT NULL AND c.track_id <> src.track_id)
                    OR (c.round_id IS NOT NULL AND src.round_id IS NOT NULL AND c.round_id <> src.round_id)
                    OR (c.track_id IS NOT NULL AND src.track_id IS NULL)
                    OR (c.track_id IS NULL AND src.track_id IS NOT NULL)
                    OR (c.round_id IS NOT NULL AND src.round_id IS NULL)
                    OR (c.round_id IS NULL AND src.round_id IS NOT NULL)
                """);
        if (unlinked > 0) {
            log.info("[CriteriaCloneSourceUnlinkMigration] Đã bỏ source_criteria_id cho {} criterion (clone khác track/round)",
                    unlinked);
        }
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
}
