# Giai đoạn 5 (GĐ5) — Ma trận test đầy đủ & Seed dev

> **Mục đích:** Tài liệu QA / FE / BE cho **GĐ5 Chung kết** — nộp CK, chấm guest judge, queue/timer, calibration, lock → `PENDING_CONFIRM`.  
> **Profile:** `dev` · **Password SV:** `Student@dev1` · **Guest judge:** `guestjudge@gmail.com` / `GuestJudge@dev1`  
> **Liên quan:** [gd4-full-test-matrix-and-seeds.md](gd4-full-test-matrix-and-seeds.md) · **GĐ6:** [gd6-full-test-matrix-and-seeds.md](gd6-full-test-matrix-and-seeds.md)

---

## Mục lục

1. [Phạm vi GĐ5 & điều kiện vào](#1-phạm-vi-gđ5--điều-kiện-vào)
2. [6 profile seed dev](#2-6-profile-seed-dev)
3. [Ma trận test theo chức năng (FR)](#3-ma-trận-test-theo-chức-năng-fr)
4. [Luồng end-to-end](#4-luồng-end-to-end)
5. [Business rules](#5-business-rules)
6. [Checklist smoke](#6-checklist-smoke)
7. [Phụ lục — Mã lỗi & warning](#7-phụ-lục--mã-lỗi--warning)

---

## 1. Phạm vi GĐ5 & điều kiện vào

### 1.1 GĐ5 bao gồm

| Hạng mục | Mô tả |
|----------|--------|
| **Nộp bài CK** | `roundId=final`, **không** `trackId` |
| **Đề CK** | Release one-way |
| **Chấm CK** | Guest judge `FINAL_EXTERNAL` |
| **Queue & timer CK** | Pool chung `trackId: null` |
| **Calibration CK** | Session OPEN trên round final |
| **Lock CK** | Side effect → `hackathon.status = PENDING_CONFIRM` |

### 1.2 Gate vào GĐ5 (từ GĐ4)

| Gate | Điều kiện |
|------|-----------|
| G-5.1 | Team `participation_status = ADVANCED` trên CK |
| G-5.2 | Round CK `is_active=true` (để nộp/chấm) |
| G-5.3 | Sơ loại đã `isPublished=true` |

---

## 2. 6 profile seed dev

Sau `mvn spring-boot:run` (profile `dev`), `DataInitializer` tạo **6 hackathon GĐ5** độc lập.

### Profile 0 — Chung kết active (mixed state)

| | |
|--|--|
| **Slug** | `seal-gd5-final-active` |
| **Seeder** | `Gd5FinalRoundDataSeeder` |
| **Config** | `app.seed.gd5.enabled=true` |

| Đội | CK submission | CK scores |
|-----|---------------|-----------|
| GD5-01 | Có | Guest judge, đủ criteria |
| GD5-02 | Có | Chưa chấm — demo POST `/scores` |
| GD5-03, GD5-04 | Chưa nộp | Demo POST `/submissions` |

**Account:** `student.gd5.leader01@fpt.edu.vn` … `leader04@`

**Dùng khi:** Demo mixed — nộp + chấm + lock CK trên cùng slug.

---

### Profile A — Submit open (sạch, chưa nộp CK)

| | |
|--|--|
| **Slug** | `seal-gd5-submit-open` |
| **Seeder** | `Gd5SubmitOpenDataSeeder` |
| **Config** | `app.seed.gd5.submit-open.enabled=true` |

4 đội ADVANCED, CK **active**, **0 submission CK** — test nộp lần đầu, upsert, multipart.

**Account:** `student.gd5s.leader01@fpt.edu.vn` … `leader04@`

---

### Profile B — Scoring live (queue CK)

| | |
|--|--|
| **Slug** | `seal-gd5-scoring-live` |
| **Seeder** | `Gd5ScoringLiveDataSeeder` |
| **Config** | `app.seed.gd5.scoring-live.enabled=true` |

| Đội | Queue | Scores |
|-----|-------|--------|
| GD5-L01 | DONE | Đủ criteria |
| GD5-L02 | DONE | 1 criterion |
| GD5-L03 | **PRESENTING** | Chưa chấm |
| GD5-L04 | WAITING | Chưa chấm |

**Account:** `student.gd5l.leader01@fpt.edu.vn` … `leader04@`

---

### Profile C — Calibration & timer CK

| | |
|--|--|
| **Slug** | `seal-gd5-calibration-timer` |
| **Seeder** | `Gd5CalibrationTimerDataSeeder` |
| **Config** | `app.seed.gd5.calibration-timer.enabled=true` |

Calibration session **OPEN** trên CK (sample = GD5-C01). Queue: GD5-C03 **PRESENTING** phase **QA**.

**Account:** `student.gd5c.leader01@fpt.edu.vn` … `leader04@`

---

### Profile D — Edge errors

| | |
|--|--|
| **Slug** | `seal-gd5-edge-errors` |
| **Seeder** | `Gd5EdgeErrorsDataSeeder` |
| **Config** | `app.seed.gd5.edge-errors.enabled=true` |

4 đội ADVANCED, CK **`is_active=false`** → `POST /submissions` → **422 `ROUND_NOT_ACTIVE`**.

**Account:** `student.gd5e.leader01@fpt.edu.vn` … `leader04@`

| Case khác | Slug / cách test |
|-----------|------------------|
| Gửi `trackId` trên CK | Tay trên Profile A |
| `SCORING_LOCKED` | `seal-gd6-pending-confirm` |
| Judge nội bộ chấm CK | Tay (judge1) |

---

### Profile E — CK late HARD_LOCK (`REJECTED`)

| | |
|--|--|
| **Slug** | `seal-gd5-late-hardlock` |
| **Seeder** | `Gd5LateHardlockDataSeeder` |
| **Config** | `app.seed.gd5.late-hardlock.enabled=true` |

| Thành phần | Giá trị |
|------------|---------|
| CK | **active**, `lateSubmissionPolicy=HARD_LOCK` |
| Deadline | **Đã qua** (~2h trước giờ máy) |
| Submission CK | **0** — chưa nộp |

4 đội ADVANCED. `POST /submissions` (multipart) → **201** với `status=REJECTED` (không `LATE_PENDING`). Nộp lại khi `REJECTED` → 422 `INVALID_STATE`.

**Account:** `student.gd5lh.leader01@fpt.edu.vn` … `leader04@`

> `repairForFeTesting` đồng bộ lại deadline qua khứ sau mỗi lần restart BE.

---

### Handoff GĐ6

Sau `PATCH /rounds/{finalId}/lock-scoring` → `PENDING_CONFIRM`. Test đóng giải trên **`seal-gd6-pending-confirm`**.

---

## 3. Ma trận test theo chức năng (FR)

### FR-33 — Nộp bài CK

| # | Case | Kỳ vọng | Seed |
|---|------|---------|------|
| F1 | Nộp lần đầu | 201 `SUBMITTED` | Profile 0 (GD5-03), A |
| F2 | Upsert khi đã có | 201 cập nhật URL | Profile 0 (GD5-01) |
| F3 | Gửi `trackId` | 422 `INVALID_STATE` | Tay |
| F4 | Sau deadline CK | 201 `REJECTED` HARD_LOCK | Profile E |
| F5 | `LATE_PENDING` CK | 422 `LATE_PENDING_NOT_ALLOWED` | Tay (duyệt trễ CK) |
| F6 | Đội chưa ADVANCED | 422 `TEAM_NOT_IN_ROUND` | Tay |
| F7 | CK chưa active | 422 `ROUND_NOT_ACTIVE` | Profile D |

### FR-21 — Đề CK

| # | Case | Kỳ vọng | Seed |
|---|------|---------|------|
| D1 | SV xem đề | `problemReleased=true` | Profile 0, A |
| D2 | Release đề | one-way | Tay nếu chưa release |

### FR-35 — Chấm điểm CK

| # | Case | Kỳ vọng | Seed |
|---|------|---------|------|
| C1 | Guest POST `/scores` | 201 | Profile 0 (GD5-02), B |
| C1b | Chấm CK **không** cần timer PRESENTING | 201 (round `isFinal=true` bỏ qua `SCORING_NOT_OPEN`) | Profile 0, B |
| C2 | Judge SL chấm CK | 403 `JUDGE_NOT_ASSIGNED` | Tay |
| C3 | Criteria CK | không qua trackId | Profile 0 |
| C4 | Sau lock CK | 423 `SCORING_LOCKED` | `seal-gd6-pending-confirm` |

### FR-23 — Queue & timer CK

| # | Case | Kỳ vọng | Seed |
|---|------|---------|------|
| Q1 | Shuffle CK | `POST /presentation/queue/shuffle` không trackId | Tay / sau repair B |
| Q2 | Controller CK | Coordinator grant | Tay |
| Q3 | Timer CK | `POST /presentation/timer/start?roundId=` | Profile B, C |
| Q4 | Queue state | PRESENTING / WAITING | Profile B |

### FR-29 — Calibration CK

| # | Case | API | Seed |
|---|------|-----|------|
| B1 | Session OPEN | `GET /calibration-sessions?roundId=` | Profile C |
| B2 | Chấm calibration | `POST /scores/calibration` | Profile C |
| B3 | Tạo session mới | `POST /calibration-sessions` | Tay |

### FR-30A — Lock CK

| # | Case | Kỳ vọng | Seed |
|---|------|---------|------|
| L1 | Lock bình thường | `scoring_locked=true` | Profile 0 |
| L2 | → PENDING_CONFIRM | `GET /hackathons/{id}` | Sau L1 |
| L3 | Force lock thiếu reason | 422 `FORCE_LOCK_REASON_REQUIRED` | Tay |
| L4 | Warning partial | `PARTIAL_SCORING_BEFORE_LOCK` | Tay |

### RBL (⏳ stub)

| API | Trạng thái |
|-----|------------|
| `GET /rounds/{id}/rbl/variance` | backlog |
| `GET /rounds/{id}/rbl/progress` | backlog |

---

## 4. Luồng end-to-end

1. Student Profile A → `POST /submissions` (round FINAL)
2. Guest judge Profile B → `POST /scores` trên GD5-L03/L04
3. (Tùy chọn) calibration Profile C + timer
4. Coordinator `PATCH /rounds/{finalId}/lock-scoring`
5. Verify `hackathon.status = PENDING_CONFIRM`
6. Chuyển GĐ6 trên `seal-gd6-pending-confirm`

| Bước | Slug |
|------|------|
| Nộp sạch | `seal-gd5-submit-open` |
| Chấm + queue | `seal-gd5-scoring-live` |
| Nộp trễ HARD_LOCK | `seal-gd5-late-hardlock` |
| Mixed demo | `seal-gd5-final-active` |

---

## 5. Business rules

| Hành động | Điều kiện |
|-----------|-----------|
| Nộp CK | FINAL active; team ADVANCED; không `trackId`; hackathon `ONGOING` |
| Chấm CK | Judge `FINAL_EXTERNAL`; **không** gate `SCORING_NOT_OPEN`/timer (khác sơ loại) |
| Nộp sau deadline CK | `HARD_LOCK` → `REJECTED`, không `LATE_PENDING` |
| Lock CK | → `PENDING_CONFIRM` |

---

## 6. Checklist smoke

```bash
cd BE && mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Profile 0 — `seal-gd5-final-active`

```text
1. GET /rounds/{finalId} → is_active=true
2. Login student.gd5.leader03 → POST /submissions
3. Login guestjudge → POST /scores (GD5-02)
4. PATCH /lock-scoring → PENDING_CONFIRM
```

### Profile A — `seal-gd5-submit-open`

```text
1. GET /me/submission?teamId=&roundId=final → NONE
2. POST /submissions → 201
```

### Profile D — `seal-gd5-edge-errors`

```text
1. POST /submissions → 422 ROUND_NOT_ACTIVE
```

### Profile E — `seal-gd5-late-hardlock`

```text
1. GET /rounds/{finalId} → is_active=true, submissionDeadline < now
2. Login student.gd5lh.leader01 → POST /submissions (multipart)
3. Response status=REJECTED (HARD_LOCK)
```

### Tắt seed

```properties
app.seed.gd5.enabled=false
app.seed.gd5.submit-open.enabled=false
app.seed.gd5.scoring-live.enabled=false
app.seed.gd5.calibration-timer.enabled=false
app.seed.gd5.edge-errors.enabled=false
app.seed.gd5.late-hardlock.enabled=false
```

---

## 7. Phụ lục — Mã lỗi & warning

### Error codes

| Mã | HTTP | Khi nào | Ma trận |
|----|------|---------|---------|
| `ROUND_NOT_ACTIVE` | 422 | Nộp khi CK tắt | F7 |
| `INVALID_STATE` | 422 | Gửi trackId CK; nộp lại khi REJECTED | F3, F4b |
| `LATE_PENDING_NOT_ALLOWED` | 422 | Duyệt trễ CK | F5 |
| `TEAM_NOT_IN_ROUND` | 422 | Chưa ADVANCED | F6 |
| `HACKATHON_NOT_ONGOING` | 422 | Nộp khi `PENDING_CONFIRM` | GĐ6 slug |
| `SCORING_LOCKED` | 423 | Chấm sau lock | C4 |
| `SCORING_NOT_OPEN` | 422 | **Chỉ sơ loại** — CK bỏ qua | — |
| `FORCE_LOCK_REASON_REQUIRED` | 422 | Force lock | L3 |
| `JUDGE_NOT_ASSIGNED` | 403/422 | Judge không panel CK | C2 |

### Warnings

| Code | Khi nào |
|------|---------|
| `PARTIAL_SCORING_BEFORE_LOCK` | Lock khi chưa chấm đủ |

### WebSocket (realtime CK)

| Topic | Khi publish |
|-------|-------------|
| `/topic/rounds/{roundId}/presentation-queue` | Shuffle, next, timer CK |

### API surface

| Method | Path |
|--------|------|
| POST | `/api/v1/submissions` |
| GET | `/api/v1/rounds/{finalId}/criteria` |
| POST | `/api/v1/scores` |
| POST | `/api/v1/scores/calibration` |
| POST | `/api/v1/presentation/queue/shuffle` |
| POST | `/api/v1/presentation/timer/start` |
| PATCH | `/api/v1/rounds/{finalId}/lock-scoring` |
| GET | `/api/v1/calibration-sessions` |

---

---

## Phụ lục — Map tài liệu

| Tài liệu | Nội dung |
|----------|----------|
| [gd4-full-test-matrix-and-seeds.md](gd4-full-test-matrix-and-seeds.md) | GĐ4 Chuyển vòng |
| [gd4-gd5-e2e-seed-data.md](gd4-gd5-e2e-seed-data.md) | Postman variables |
| [fe-checklist-gd2-gd4-gd5-gd6.md](fe-checklist-gd2-gd4-gd5-gd6.md) | Checklist FE |

*Cập nhật: 2026-06 — 6 profile seed GĐ5 (thêm late-hardlock) + doc chấm CK không gate timer.*
