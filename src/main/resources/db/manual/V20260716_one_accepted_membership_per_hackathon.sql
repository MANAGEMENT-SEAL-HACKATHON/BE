-- Guard: at most one ACCEPTED membership per user per hackathon among any teams.
-- Denormalized hackathon_id + partial unique index. Run on PostgreSQL before relying on race safety.

ALTER TABLE team_members
    ADD COLUMN IF NOT EXISTS hackathon_id INTEGER;

UPDATE team_members tm
SET hackathon_id = t.hackathon_id
FROM teams t
WHERE t.id = tm.team_id
  AND (tm.hackathon_id IS NULL OR tm.hackathon_id <> t.hackathon_id);

CREATE OR REPLACE FUNCTION team_members_sync_hackathon_id()
RETURNS TRIGGER AS $$
BEGIN
    SELECT t.hackathon_id INTO NEW.hackathon_id
    FROM teams t
    WHERE t.id = NEW.team_id;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_team_members_sync_hackathon_id ON team_members;
CREATE TRIGGER trg_team_members_sync_hackathon_id
    BEFORE INSERT OR UPDATE OF team_id, status ON team_members
    FOR EACH ROW
    EXECUTE PROCEDURE team_members_sync_hackathon_id();

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_team_members_hackathon'
    ) THEN
        ALTER TABLE team_members
            ADD CONSTRAINT fk_team_members_hackathon
            FOREIGN KEY (hackathon_id) REFERENCES hackathons(id);
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uq_one_accepted_membership_per_hackathon
    ON team_members (hackathon_id, user_id)
    WHERE status = 'ACCEPTED' AND hackathon_id IS NOT NULL;
