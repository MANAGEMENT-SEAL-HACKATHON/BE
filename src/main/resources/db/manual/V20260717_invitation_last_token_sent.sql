-- Hibernate ddl-auto=update also applies this column on entity change;
-- run manually when ddl-auto is off.
ALTER TABLE invitations
    ADD COLUMN last_token_sent TINYINT(1) NULL;
