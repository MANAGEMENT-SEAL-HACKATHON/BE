-- Hibernate ddl-auto=update also applies these columns on entity change;
-- run manually when ddl-auto is off.
ALTER TABLE hackathons ADD COLUMN registration_extension_count INT NOT NULL DEFAULT 0;
ALTER TABLE hackathons ADD COLUMN registration_extended_at DATETIME NULL;
