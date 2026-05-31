-- GD03 v4.1 — SQL views (scaffold; chạy thủ công sau khi có data scores/submissions).
-- FR-20: v_round_leaderboard | FR-30: v_judge_score_variance, v_scoring_progress

-- TODO: align column names with live schema before production deploy.

CREATE OR REPLACE VIEW v_round_leaderboard AS
SELECT s.round_id,
       s.team_id,
       t.team_name,
       trt.assigned_group,
       COUNT(DISTINCT sc.judge_id) AS judge_count,
       ROUND(SUM(COALESCE(sc.score_value, 0) * c.weight)
           / NULLIF(COUNT(DISTINCT sc.judge_id), 0), 4) AS weighted_avg_score,
       MIN(s.submitted_at) AS submitted_at
  FROM submissions s
  JOIN teams t ON t.id = s.team_id
  LEFT JOIN team_round_tracks trt ON trt.team_id = s.team_id
  JOIN scores sc ON sc.submission_id = s.id
  JOIN criteria c ON c.id = sc.criterion_id
 WHERE sc.score_type = 'NORMAL'
   AND sc.is_final = TRUE
   AND (c.type IS NULL OR c.type <> 'PENALTY')
 GROUP BY s.round_id, s.team_id, t.team_name, trt.assigned_group;
