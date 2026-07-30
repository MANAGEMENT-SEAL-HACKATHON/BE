-- Hibernate ddl-auto=update also drops via entity change when enabled;
-- run manually when ddl-auto is off.
DROP TABLE IF EXISTS wildcard_override_history;
DROP TABLE IF EXISTS wildcard_reviews;
DROP TABLE IF EXISTS certificates;
ALTER TABLE rounds DROP COLUMN wildcard_enabled;
ALTER TABLE rounds DROP COLUMN wildcard_proposal_confirmed_at;
ALTER TABLE hackathons DROP COLUMN wildcard_enabled;
