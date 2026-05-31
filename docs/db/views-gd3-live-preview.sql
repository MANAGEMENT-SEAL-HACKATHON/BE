-- GD03 GĐ3 — Live leaderboard preview (FR-18A / FR-20 preview)
-- Chạy thủ công; dùng is_final = FALSE cho điểm nháp live scoring.

CREATE OR REPLACE VIEW v_round_leaderboard_live AS
SELECT s.round_id,
       s.team_id,
       t.team_name,
       s.track_id,
       trt.assigned_group,
       COUNT(DISTINCT sc.judge_id) AS judge_count,
       ROUND(SUM(COALESCE(avg_sc.avg_score, 0) * c.weight), 4) AS weighted_avg_score,
       MIN(s.submitted_at) AS submitted_at
  FROM submissions s
  JOIN teams t ON t.id = s.team_id
  LEFT JOIN team_round_tracks trt ON trt.team_id = s.team_id AND trt.track_id = s.track_id
  JOIN criteria c ON c.track_id = s.track_id
                 AND (c.type IS NULL OR c.type <> 'PENALTY')
  LEFT JOIN (
        SELECT submission_id, criterion_id, AVG(score_value) AS avg_score
          FROM scores
         WHERE score_type = 'NORMAL'
           AND is_final = FALSE
         GROUP BY submission_id, criterion_id
       ) avg_sc ON avg_sc.submission_id = s.id AND avg_sc.criterion_id = c.id
 WHERE s.track_id IS NOT NULL
 GROUP BY s.round_id, s.team_id, t.team_name, s.track_id, trt.assigned_group;
