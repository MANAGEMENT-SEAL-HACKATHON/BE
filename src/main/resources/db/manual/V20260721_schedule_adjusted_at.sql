-- Hibernate ddl-auto=update also applies this column on entity change;
-- run manually when ddl-auto is off.
ALTER TABLE hackathons ADD COLUMN schedule_adjusted_at DATETIME NULL;
