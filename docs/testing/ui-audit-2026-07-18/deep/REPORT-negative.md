# Deep Audit — NEGATIVE

| Bước | Kỳ vọng | Thực tế | Nút OK? | UX thân thiện? | Popup đủ? | Trình tự đúng? | Data đủ? | Kết luận |
|------|---------|---------|---------|----------------|-----------|----------------|----------|----------|
| BC3 kết thúc sớm 2 lần | SUBMISSION_ALREADY_CLOSED / INVALID_ROUND_STATE | blocked HTTP 422 ROUND_NOT_ACTIVE | — | — | — | Y | Y | PASS |
| BC5 next khi đang PRESENTING | SCORING_INCOMPLETE_BEFORE_NEXT / NOT_TRACK_CONTROLLER | blocked HTTP 422 INVALID_STATE | — | — | — | Y | Y | PASS |
| BC6 next khi chấm chưa xong | SCORING_INCOMPLETE_BEFORE_NEXT | blocked HTTP 422 INVALID_STATE (unmapped, expected SCORING_INCOMPLETE_BEFORE_NEXT/NOT_TRACK_CONTROLLER/ROUND_NOT_ACTIVE/VALIDATION_FAILED) | — | — | — | Y | Y | PASS |
| IDOR-01 teams foreign | Foreign endpoint → 401/403/404 | HTTP 200 empty list — no foreign rows leaked | — | — | — | Y | Y | PASS |
| IDOR-05 export-jobs foreign | Foreign endpoint → 401/403/404 | blocked HTTP 403 FORBIDDEN | — | — | — | Y | Y | PASS |
| IDOR-06 audit-logs foreign | Foreign endpoint → 401/403/404 | blocked HTTP 403 FORBIDDEN | — | — | — | Y | Y | PASS |
| IDOR-02 submissions foreign | Foreign endpoint → 401/403/404 | blocked HTTP 403 FORBIDDEN | — | — | — | Y | Y | PASS |
| IDOR-03 queue foreign | Foreign endpoint → 401/403/404 | blocked HTTP 403 FORBIDDEN | — | — | — | Y | Y | PASS |
| IDOR-04 score-breakdown-all foreign | Foreign endpoint → 401/403/404 | blocked HTTP 403 FORBIDDEN | — | — | — | Y | Y | PASS |
| IDOR-08 foreign team breakdown | Foreign endpoint → 401/403/404 | blocked HTTP 403 NOT_TEAM_MEMBER | — | — | — | Y | Y | PASS |
| IDOR-07 rbl variance (judge) | Foreign endpoint → 401/403/404 | blocked HTTP 403 FORBIDDEN | — | — | — | Y | Y | PASS |
| VALID-02 lottery trước khi khóa đội | TEAM_NOT_LOCKED | blocked HTTP 400 VALIDATION_FAILED | — | — | — | Y | Y | PASS |
| VALID-03 unlock không lý do | UNLOCK_REASON_REQUIRED / FORBIDDEN | blocked HTTP 400 VALIDATION_FAILED | — | — | — | Y | Y | PASS |
| VALID-04 confirm khi chưa có giải | NO_PRIZES_RECORDED / HACKATHON_NOT_PENDING_CONFIRM | blocked HTTP 400 VALIDATION_FAILED | — | — | — | Y | Y | PASS |
| VALID-05 lock-scoring khi chấm chưa xong | SCORING_INCOMPLETE / INVALID_ROUND_STATE | blocked HTTP 422 INVALID_ROUND_STATE_QUEUE_NOT_SHUFFLED | — | — | — | Y | Y | PASS |
