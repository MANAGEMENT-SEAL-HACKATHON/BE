-- Hibernate ddl-auto=update also applies these on entity change;
-- run manually when ddl-auto is off.

ALTER TABLE events
    ADD COLUMN buffet_location VARCHAR(300) NULL;

ALTER TABLE events
    ADD COLUMN buffet_starts_at DATETIME NULL;

ALTER TABLE events
    ADD COLUMN buffet_ends_at DATETIME NULL;
