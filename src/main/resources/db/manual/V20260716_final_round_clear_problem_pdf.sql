-- CK-05: Chung kết không giữ PDF đề riêng trên round — mỗi đội tiếp tục đề track sơ loại.
-- Runtime idempotent: FinalRoundProblemMigration (CommandLineRunner). Chạy thủ công nếu cần.

ALTER TABLE rounds ADD COLUMN final_problem_migration_cleared_at DATETIME(6) NULL;
ALTER TABLE rounds ADD COLUMN final_problem_migration_banner_dismissed_at DATETIME(6) NULL;

-- Backup URL/filename vào audit_logs (ROUND_FINAL_PROBLEM_PDF_CLEARED) được ghi bởi Java migration.
-- Clear PDF đề CK legacy (URL + storage key + filename).
UPDATE rounds
   SET problem_statement_url = NULL,
       problem_statement_storage_key = NULL,
       problem_statement_original_filename = NULL,
       final_problem_migration_cleared_at = COALESCE(final_problem_migration_cleared_at, CURRENT_TIMESTAMP(6))
 WHERE is_final = 1
   AND (
        problem_statement_url IS NOT NULL
     OR problem_statement_storage_key IS NOT NULL
     OR problem_statement_original_filename IS NOT NULL
   );

-- Backfill: vòng CK đã activate nhưng chưa stamp phát đề → dùng activated_at.
UPDATE rounds
   SET problem_released_at = activated_at
 WHERE is_final = 1
   AND problem_released_at IS NULL
   AND activated_at IS NOT NULL;
