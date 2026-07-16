-- Repair stale ACCEPTED memberships on REJECTED teams (GĐ2 membership fix)
-- Run BEFORE or WITH BE deploy, BEFORE FE deploy.

CREATE TABLE IF NOT EXISTS team_member_migration_backup (
    id SERIAL PRIMARY KEY,
    team_member_user_id INTEGER NOT NULL,
    team_member_team_id INTEGER NOT NULL,
    old_status VARCHAR(32) NOT NULL,
    team_status VARCHAR(32) NOT NULL,
    backed_up_at TIMESTAMP NOT NULL DEFAULT NOW()
);

INSERT INTO team_member_migration_backup (team_member_user_id, team_member_team_id, old_status, team_status)
SELECT tm.user_id, tm.team_id, tm.status, t.status
FROM team_members tm
JOIN teams t ON t.id = tm.team_id
WHERE tm.status = 'ACCEPTED' AND t.status = 'REJECTED';

UPDATE team_members tm
SET status = 'LEFT', left_at = NOW()
FROM teams t
WHERE t.id = tm.team_id
  AND tm.status = 'ACCEPTED'
  AND t.status = 'REJECTED';

-- Rollback (if needed):
-- UPDATE team_members tm SET status = b.old_status, left_at = NULL
-- FROM team_member_migration_backup b
-- WHERE tm.user_id = b.team_member_user_id AND tm.team_id = b.team_member_team_id;
