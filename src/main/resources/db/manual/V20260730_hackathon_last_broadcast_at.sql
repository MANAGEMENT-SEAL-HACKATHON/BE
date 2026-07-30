-- Hibernate ddl-auto=update also applies this column on entity change;
-- run manually when ddl-auto is off.
ALTER TABLE hackathons ADD COLUMN last_broadcast_at DATETIME NULL;
