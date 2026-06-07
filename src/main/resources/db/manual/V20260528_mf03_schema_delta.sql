-- MF-03 schema delta (GD03 §6) — LEGACY; dùng V20260529_gd03_v4_1_delta.sql hoặc Gd03V41SchemaMigration.
-- An toàn: chỉ ADD nullable / DEFAULT, không đổi cột hiện có.

-- FR-20/32: thời điểm kích hoạt Round
ALTER TABLE rounds
    ADD COLUMN activated_at DATETIME(6) NULL AFTER is_active;

-- FR-30: trạng thái đội trong Round (team_round_participation — MF-02 v3.5)
ALTER TABLE team_round_participation
    ADD COLUMN participation_status VARCHAR(20) NOT NULL DEFAULT 'PARTICIPATING'
        AFTER hackathon_id;

-- BUG-7 (schema v3.0 prizes): chặn trao trùng trong cùng round
-- Bỏ qua nếu index đã tồn tại.
ALTER TABLE prizes
    ADD UNIQUE KEY uk_prizes_round_team (round_id, team_id);

ALTER TABLE prizes
    ADD UNIQUE KEY uk_prizes_round_rank (round_id, prize_rank);
