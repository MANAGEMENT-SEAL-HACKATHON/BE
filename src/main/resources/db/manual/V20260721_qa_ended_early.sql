-- Hibernate ddl-auto=update also applies this column on entity change;
-- run manually when ddl-auto is off.
ALTER TABLE presentation_slots ADD COLUMN qa_ended_early BOOLEAN NULL;
