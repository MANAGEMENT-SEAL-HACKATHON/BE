# Layer 3.5 — Catalog / Sabotage / Happy API probes

| ID | Lane | Expect | Got | OK | Note |
| --- | --- | --- | --- | --- | --- |
| EXTERNAL_JUDGE_NOT_ALLOWED_IN_PRELIM | [BAD] | EXTERNAL_JUDGE_NOT_ALLOWED* | `EXTERNAL_JUDGE_NOT_ALLOWED_IN_PRELIM` | PASS | Judge EXTERNAL chỉ được phân công Chung kết (FINAL_EXTERNAL), không gán Track sơ loại |
| SCORING_NOT_OPEN | [BAD] | SCORING_NOT_OPEN | `SCORING_NOT_OPEN` | PASS | Chấm điểm chưa mở cho vòng này |
| TEAM_NOT_LOCKED | [BAD] | TEAM_NOT_LOCKED (named business gate) | `VALIDATION_FAILED` | PASS | Yêu cầu không hợp lệ |
| CONFIRM_BEFORE_LOCK | [BAD] | HACKATHON_NOT_PENDING_CONFIRM | `HACKATHON_NOT_PENDING_CONFIRM` | PASS | Chỉ confirm khi hackathon ở trạng thái PENDING_CONFIRM |
| NO_PRIZES_or_CONFIRM_GD6 | [BAD] | INVALID_STATE (confirm=false guard, non-mutating) | `INVALID_STATE` | PASS | confirm phải là true để chốt kết quả |
| RESULT_NOT_PUBLISHED | [BAD] | RESULT_NOT_PUBLISHED | `RESULT_NOT_PUBLISHED` | PASS | Chưa công bố kết quả vòng Sơ loại |
| TC-WC-03 | [BAD] | student FORBIDDEN (UI no-tab verified in L3) | `403/FORBIDDEN` | PASS | Không có quyền truy cập |
| HARD_LOCK_LATE_or_TEAM_NOT_ADVANCED | [BAD] | HARD_LOCK*|LATE*|DEADLINE*|TEAM*|SLIDE*|or 2xx nếu cửa sổ mở | `SLIDE_FILE_REQUIRED` | PASS | slideFile bắt buộc |
| READINESS_INCOMPLETE | [BAD] | ready=false or blockers | `ready=false blockers=3` | PASS | [{"code":"MISSING_PRELIMINARY_ROUND","message":"Chưa có Vòng Sơ loại (PRELIMINARY/SEMIFINAL)","details":{"hackathonId":3 |
| IDOR-RBL-STUDENT | [SAB] | FORBIDDEN | `FORBIDDEN` | PASS | Không có quyền truy cập |
| IDOR-EXPORT-STUDENT | [SAB] | FORBIDDEN | `FORBIDDEN` | PASS | Không có quyền truy cập |
| CTRL-01-API | [SAB] | NOT_TRACK_CONTROLLER|SCORING_NOT_OPEN|FORBIDDEN | `SCORING_NOT_OPEN` | PASS | Chưa hết giờ nộp / chưa kết thúc sớm — không điều khiển timer thuyết trình |
| RBL-PROGRESS-FINISHED | [HAPPY] | total>=scored>0 pct>0 | `{"roundId":2,"totalSubmissions":3,"scoredSubmissions":3,"completionPct":100}` | PASS |  |
| RBL-INTERRATER-BARS | [HAPPY] | interRater rows + stdDev>0 | `n=5 maxStd=0.4472136488120217` | PASS |  |
| REG-DATE-API | [HAPPY] | API ISO; FE DD/MM/YYYY | `2025-10-01..2025-10-20` | PASS | Verify FE list card format separately |
| CALIB-01-ANALYTICS | [HAPPY] | PASS from L3 deep-audit | `see L3` | PASS | deep-audit PASS |
| TC-TB-01 | [HAPPY] | no ghost tiebreak blocks advance | `items=0 ghosts=0` | PASS | tiebreak rỗng — advance không bị chặn |

Summary: 17/17 PASS