-- Guarded add of user default shirt columns (skip if Hibernate ddl-auto already created them).
SET @db := DATABASE();

SET @exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'users' AND COLUMN_NAME = 'default_shirt_size'
);
SET @sql := IF(@exists = 0,
  'ALTER TABLE users ADD COLUMN default_shirt_size VARCHAR(10) NULL',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'users' AND COLUMN_NAME = 'default_shirt_fit'
);
SET @sql := IF(@exists = 0,
  'ALTER TABLE users ADD COLUMN default_shirt_fit VARCHAR(20) NULL',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
