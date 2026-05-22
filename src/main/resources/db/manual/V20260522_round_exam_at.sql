-- Migration rounds: sequence_order → exam_at
-- Chạy THỦ CÔNG nếu app không start được (ddl-auto) HOẶC để app tự chạy RoundExamAtSchemaMigration sau khi Hibernate thêm cột nullable.

-- Bước 1: thêm cột NULL (không NOT NULL — tránh '0000-00-00' trên row cũ)
ALTER TABLE rounds ADD COLUMN exam_at DATETIME(6) NULL;

-- Bước 2: backfill từ thời gian nộp bài (hoặc NOW nếu thiếu)
UPDATE rounds
   SET exam_at = COALESCE(submission_open, submission_deadline, NOW(6))
 WHERE exam_at IS NULL;

-- Bước 3: bắt buộc NOT NULL
ALTER TABLE rounds MODIFY COLUMN exam_at DATETIME(6) NOT NULL;

-- Bước 4: bỏ sequence_order (PHẢI drop index trước — nếu không: Duplicate key uk_rounds_hackathon_sequence)
-- Xem tên index thực tế (có thể khác uk_rounds_hackathon_sequence):
-- SELECT DISTINCT INDEX_NAME FROM information_schema.STATISTICS
--  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'rounds' AND COLUMN_NAME = 'sequence_order';

ALTER TABLE rounds DROP INDEX uk_rounds_hackathon_sequence;
ALTER TABLE rounds DROP COLUMN sequence_order;
