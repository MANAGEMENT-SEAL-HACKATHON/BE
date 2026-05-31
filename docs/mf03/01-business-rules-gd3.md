# MF-03 GĐ3–GĐ5 — Business Rules

**Nguồn:** `GD03_05_SEAL_MF_v4_1.docx` · **Schema:** [schema-v3.0-mysql.md](../db/schema-v3.0-mysql.md)

**Trạng thái BE:** GĐ3 (FR-15..21 + WS) ✅; GĐ4 phase 1 ✅; GĐ4 phase 2 + GĐ5 ⏳ — xem [09-be-backlog-gd4-gd5.md](09-be-backlog-gd4-gd5.md).

---

## 1. Điều kiện vào GĐ3

| Gate | Mô tả |
|------|--------|
| G-3.1 | Hackathon `ONGOING`; đội `ACTIVE`; có `team_round_participation` + `team_round_tracks` (sau lottery GĐ2) |
| G-3.2 | `registration_end` đã qua; cron FR-13A khóa thành viên (`is_locked`) |
| EC-01 | Đội vắng Khai mạc → không có participation → Coordinator ELIMINATE trước activate |

---

## 2. Thực thể & bảng chính

| Bảng | Vai trò MF-03 |
|------|----------------|
| `rounds` | `is_active`, `activated_at`, `is_published`, `scoring_locked`, `top_n_advance`, `min_teams_final`, `wildcard_enabled` |
| `submissions` | `track_id` (Sơ loại) + `round_id` NOT NULL; UNIQUE `(team_id, scoring_key)` BUG-2 |
| `scores` | Chấm theo `(submission, judge, criterion, score_type)` |
| `tiebreak_evaluations` | Judge penalty tiebreak (FR-22B) |
| `wildcard_reviews` | Đề xuất / duyệt wild card (FR-22A) |
| `team_round_participation` | GĐ2 v3.5 — đội tham gia mọi Round (kể cả FINAL) |
| `team_round_tracks` | Gán track + `assigned_group` + `participation_status` (D-2 v4.1) |
| `judge_assignments` | Track (Sơ loại) hoặc Round FINAL (`FINAL_EXTERNAL`) |
| `prizes` | Trao giải GĐ6 |
| `calibration_sessions` | FR-29 (RBL) |
| `submission_metadata` | FR-17 (async, optional) |

**Submission XOR (BC-06)**

- Sơ loại: `track_id NOT NULL`, `round_id` denormalized từ track.
- Chung kết: `track_id NULL`, `round_id` = round FINAL.
- Trigger DB + UNIQUE `scoring_key` generated (BUG-2) — xem schema §6.2 v4.1.

---

## 3. FR-20 / FR-32 — Kích hoạt Round

| Quy tắc | HTTP |
|---------|------|
| Chỉ **1 round active** / hackathon | Auto-deactivate round khác |
| Sơ loại: mỗi track có criteria weight = 1, có judge | `ROUND_NO_CRITERIA`, `ROUND_WEIGHT_NOT_ONE`, `JUDGE_NOT_ASSIGNED` |
| Chung kết: criteria + judge `FINAL_EXTERNAL` | `INVALID_ASSIGNMENT_TYPE` |
| Set `is_active=true`, `activated_at=NOW()` | ✅ đã implement |
| Có đội trong round (participation) | `NO_TEAMS_IN_ROUND` (khi implement đủ) |

**API:** `PATCH /api/v1/rounds/{id}/activate` — [fr-06b-activate](../mf01/api/fr-06b-activate.md).

---

## 4. FR-21 — Phát đề

- One-way: sau `problem_released_at` không sửa URL (policy app).
- `PATCH /rounds/{id}/release-problem` body: `problemStatementUrl`.
- Notify mentor/judge (khi implement).

---

## 5. FR-22 / FR-33 — Nộp bài

| | Sơ loại | Chung kết |
|---|---------|-----------|
| Actor | STUDENT APPROVED | STUDENT APPROVED |
| Điều kiện | Round active; team ACTIVE; có `team_round_tracks` | Round FINAL active; **không** gửi `trackId` |
| Deadline | Trước `submission_deadline` → `SUBMITTED` | Sau deadline → `REJECTED` (HARD_LOCK) |
| Trễ (Sơ loại) | `LATE_PENDING` nếu policy `ALLOW_LATE_PENDING` | Không cho `LATE_PENDING` |

**1 bài / đội / scoring scope** — UNIQUE qua generated key ở DB.

---

## 6. FR-25 — Duyệt bài muộn

- Coordinator `PATCH /submissions/{id}/review` → APPROVE (`LATE_APPROVED` / `ACCEPTED`) hoặc REJECT.
- Round FINAL HARD_LOCK → `LATE_PENDING_NOT_ALLOWED`.

---

## 7. FR-24 / FR-35 — Chấm điểm

| Quy tắc | Code |
|---------|------|
| Judge đã phân công track/round | `JUDGE_NOT_ASSIGNED_TO_TRACK` 403 |
| Submission gradable | `SUBMISSION_NOT_GRADABLE` 422 |
| `score_value ≤ criterion.max_score` | `SCORE_EXCEEDS_MAX` |
| Round chưa `scoring_locked` | Cho phép POST score |
| Round đã lock | `SCORING_LOCKED` 423 |
| Mentor ≠ Judge cùng track | `CONFLICT_MENTOR_JUDGE_SAME_TRACK` |
| Criterion đúng track submission | `CRITERION_WRONG_TRACK` |

**BUG-5:** Transaction + pessimistic lock khi chấm (tránh race với lock-scoring).

---

## 8. FR-26 / FR-36 — Khóa chấm điểm

| Loại | Mô tả |
|------|--------|
| Normal lock | `scoring_locked=true`, `scoring_locked_at`, `scoring_locked_by` |
| Force lock | `force_locked=true`, **bắt buộc** `reason` → `FORCE_LOCK_REASON_REQUIRED` |
| Pre-check | Track chưa chấm đủ → `warnings[]` `PARTIAL_SCORING_BEFORE_LOCK` (không block) |
| Side effect | `scores.is_final=1` cho submission thuộc round |

**FR-36 (Chung kết):** Sau lock round FINAL → `hackathons.status = PENDING_CONFIRM` (khi implement nối luồng).

**Gate GĐ4:** Chỉ chạy ranking/advance khi `scoring_locked=true` → `ROUND_NOT_SCORING_LOCKED` nếu chưa lock.

---

## 9. FR-27 — Xếp hạng

- Per **partition** (`assigned_group`) trong từng Track (Sơ loại).
- Chung kết: pool chung, không partition.
- **BUG-4:** `COALESCE(AVG(score), 0)` — không bỏ qua criterion chưa chấm âm thầm.
- Warning: `INCOMPLETE_SCORING_IN_RANKING` khi thiếu điểm.

---

## 10. FR-28 — Tiebreak

- Khi đồng điểm tại ranh giới Top N → `TIEBREAK_REQUIRED` cho đến khi resolve.
- Rule theo `rounds.tiebreak_rule`: `PENALTY_SCORE` | `SUBMISSION_TIME` | `COORDINATOR_DECISION`.
- Persist `tiebreak_evaluations` (penalty per judge per team).

---

## 11. FR-29 — Wild Card

- Khi tổng đội advance &lt; `min_teams_final` và `wildcard_enabled`.
- Coordinator duyệt `wildcard_reviews` (approve/reject).
- Warning: `MIN_TEAMS_NOT_REACHED`.

---

## 12. FR-30 — Advance / Eliminate

- Batch `POST /rounds/{id}/advance` sau ranking + tiebreak (+ wildcard).
- Cập nhật `team_round_tracks.participation_status`:
  - Sơ loại: `ADVANCED` / `ELIMINATED`
- Tạo `team_round_participation` cho round Chung kết (upsert idempotent).
- **BUG-6:** Idempotent — `UNIQUE(team_id, round_id)` + upsert.

---

## 13. FR-31 — Judge Chung kết

Panel Chung kết gồm **Judge khách mời** (chính) và **ngoại lệ trưởng ban nội bộ** — không phải 100% guest.

| Loại | Điều kiện | Ghi chú |
|------|-----------|---------|
| Judge khách | `user_type=EXTERNAL`, thường `is_temp_account=true` | Tạo qua `POST /users/temp-judges` (GĐ1/GĐ4) |
| Trưởng ban | `is_dept_head=true` (Coordinator `PATCH /users/{id}`), **không** mentor kỳ này | DB trigger `trg_check_mentor_judge_conflict` + audit `DEPT_HEAD_FINAL_JUDGE_EXCEPTION` |
| Judge Sơ loại | Không tự động lên CK | Warning `JUDGE_PARTICIPATED_IN_PRELIM` khi assign GĐ4 |

**Naming:** `assignment_type=FINAL_EXTERNAL` = phân công panel **round Chung kết** — **không** có nghĩa user phải `EXTERNAL`.

- `POST /rounds/{finalRoundId}/judge-assignments` (GĐ4) hoặc `POST /judge-assignments` với `roundId` + `FINAL_EXTERNAL` (GĐ1 vẫn block `JUDGE_FINAL_AT_PHASE1`).

### Playbook — thiếu judge khách mời

1. **Trước CK (≥48h KICKOFF):** `POST /users/temp-judges` + resend invitation ([mf02/02-invitations.md](../mf02/02-invitations.md)).
2. **Bổ sung slot nội bộ:** Trưởng ban (`is_dept_head`) nếu chưa mentor kỳ này.
3. **Readiness:** Cảnh báo nếu panel &lt; tối thiểu (khuyến nghị 3); **không activate CK** cho đến khi có ≥1 judge (`JUDGE_NOT_ASSIGNED`).
4. **Không làm:** Tự động kéo judge Sơ loại lên CK — chỉ assign chủ động + chấp nhận warning.

---

## 14. FR-34 — Calibration

- Tùy chọn trước chấm CK; `POST /scores/calibration` + `calibration_sessions`.

---

## 15. GĐ6 — Kết thúc

Chi tiết MF-06: [01-business-rules-gd6.md](01-business-rules-gd6.md) · API: [03-api-reference-gd3.md §6](03-api-reference-gd3.md#6-hackathon--kết-thúc--trao-giải-gđ6--mf-06)

| Bước | Hành động | Trạng thái BE |
|------|-----------|---------------|
| 1 | Lock CK → `PENDING_CONFIRM` (GĐ5 FR-30A) | 🔶 side effect chưa đủ |
| 2 | `GET /hackathons/{id}/team-rankings` | ⏳ stub |
| 3 | `POST /hackathons/{id}/prizes` | ✅ |
| 4 | `PATCH /hackathons/{id}/confirm` → `FINISHED` | ⏳ stub |
| 5 | `GET .../chapter-rankings`, `individual-rankings` (async worker) | ⏳ stub |
| 6 | `POST /hackathons/{id}/export-jobs` | ⏳ stub |

**Prize:** Chỉ khi `PENDING_CONFIRM`; chặn trùng đội / `prizeRank` → `PRIZE_DUPLICATE`.

---

## 16. Error & warning codes

Blockers: xem `ErrorCode.java` nhóm `MF-03 GĐ3–GĐ5`.

Warnings: `WarningCode.java` — `JUDGE_PARTICIPATED_IN_PRELIM`, `MIN_TEAMS_NOT_REACHED`, `PARTIAL_SCORING_BEFORE_LOCK`, `INCOMPLETE_SCORING_IN_RANKING`.

Envelope: [mf01/api/_conventions.md](../mf01/api/_conventions.md).

---

## 17. FR không có REST riêng

| FR | Cách xử lý |
|----|------------|
| FR-23 Thuyết trình Sơ loại | `events` + audit |
| FR-35 Thuyết trình CK | `events` + audit |

Xem [mf01/api/fr-06a-events.md](../mf01/api/fr-06a-events.md).
