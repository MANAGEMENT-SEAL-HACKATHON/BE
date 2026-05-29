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
        migrateSubmissionsV41();
        migrateParticipationStatusToTeamRoundTracks();
        migratePrizesHackathonUnique();
        createSubmissionMetadataTable();
        log.info("[Gd03V41SchemaMigration] GD03 v4.1 schema delta applied (idempotent)");
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
    }

    private void addColumnIfMissing(String table, String column, String ddl) {
        if (columnExists(table, column)) {
            return;
        }
        jdbcTemplate.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + ddl);
        log.info("[Gd03V41SchemaMigration] Added {}.{}", table, column);
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
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                  FROM information_schema.TABLES
                 WHERE TABLE_SCHEMA = DATABASE()
                   AND TABLE_NAME = ?
                """, Integer.class, table);
        return count != null && count > 0;
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
