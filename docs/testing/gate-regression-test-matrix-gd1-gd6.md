# Ma trận regression — Gate BTC & workflow GĐ1→GĐ6

> **Mục đích:** Test đủ 6 giai đoạn sau refactor readiness / event order / timeline sync.  
> **Chạy sau:** `spring.profiles.active=dev` + seed xong.  
> **Playbook E2E:** [full-workflow-api-test-gd1-gd6.md](full-workflow-api-test-gd1-gd6.md) · **FE mapping:** [fe-gd1-gd2-gd3-workflow-mapping.md](fe-gd1-gd2-gd3-workflow-mapping.md)

---

## 0. Xác nhận thứ tự milestone (Events)

**Hai chiều — không nhầm lẫn:**

| Chiều | Thứ tự | Ghi chú |
|-------|--------|---------|
| **POST (API)** | KICKOFF → WORKSHOP → AWARDS | KICKOFF làm gốc; WORKSHOP cần đã có KICKOFF trong DB |
| **Lịch thực tế** | WORKSHOP → KICKOFF → AWARDS | Khớp PDF Spring 2026 (WS 9/4 → KO 11/4) và seed dev |

```text
POST:     1. KICKOFF   2. WORKSHOP   3. AWARDS (GĐ6)
Lịch:     WS (sớm hơn) → KO → AWARDS
```

| Bước POST | Điều kiện tiên quyết | Ngày mẫu trên lịch (seed 2026) |
|-----------|----------------------|--------------------------------|
| KICKOFF | Sau `registrationEnd` (05/06), trước ngày thi SL (10/06) | **07/06** 14:00–17:00 |
| WORKSHOP | Đã có KICKOFF (POST); **trên lịch trước** KICKOFF, khác ngày | **06/06** 20:00–21:30 |
| AWARDS | Đã có WORKSHOP; sau `final.submissionDeadline` | **10/06** 17:30–19:00 |

> **GĐ1 → GĐ2:** Chỉ cần KICKOFF (+ WORKSHOP khuyến nghị). **Không** cần AWARDS để `PATCH ONGOING`.

---

## 1. Ba gate kích hoạt (tóm tắt workflow 6 GĐ)

| Gate | Giai đoạn | API kích hoạt | Readiness dry-run |
|------|-----------|---------------|-------------------|
| **1 — Mở đăng ký** | GĐ1→GĐ2 | `PATCH /hackathons/{id}/status` → `ONGOING` | `?target=ONGOING` |
| **2 — Mở thi Sơ loại** | GĐ3 | `PATCH /rounds/{prelimId}/activate` | (activate tự validate) |
| **3 — Mở thi CK** | GĐ4→GĐ5 | `PATCH /rounds/{finalId}/activate` | `?target=FINAL_ROUND` |

| GĐ | Vai trò chính | Đầu ra bắt buộc để sang GĐ sau |
|----|---------------|--------------------------------|
| **GĐ1** | COORD | `status=ONGOING`; có CK **shell** + criteria CK (FR G2/G4) |
| **GĐ2** | STU/COORD | Teams + lottery + `is_locked` (ngày **sau** `registrationEnd`) |
| **GĐ3** | ALL | Prelim `scoring_locked=true`; submissions/scores |
| **GĐ4** | COORD | Publish SL + advance + judge `FINAL_EXTERNAL` |
| **GĐ5** | STU/JUD | CK submissions/scores + `lock-scoring` → `PENDING_CONFIRM` |
| **GĐ6** | COORD | AWARDS event + prizes + `confirm` → `FINISHED` |

---

## 2. Seed shortcut theo GĐ

| Slug | Dùng test |
|------|-----------|
| `seal-gd1-ready` | GĐ1 bước 1.11–1.12 (đủ G1–G5) |
| `seal-gd1-incomplete` | TC-G1-N08 readiness fail |
| `seal-spring-2026` | GĐ2 teams |
| `seal-gd3-prelim-open` | GĐ3 portal + submit |
| `seal-gd4-tiebreak-wildcard` | GĐ4 (cần `app.seed.gd4.enabled=true`) |
| `seal-gd5-final-active` | GĐ5 CK |
| `seal-gd6-pending-confirm` | GĐ6 trao giải |

---

## 3. Test case — GĐ1 (Events + Readiness + Gate1)

| ID | Mô tả | Các bước | Kỳ vọng |
|----|-------|----------|---------|
| **G1-E01** | Happy — thứ tự event đúng | POST KICKOFF → POST WORKSHOP (bỏ AWARDS) | 201 cả hai |
| **G1-E02** | Happy — ONGOING không cần AWARDS | Sau G1-E01 + đủ round/track/criteria → `GET readiness?target=ONGOING` | `ready: true` |
| **G1-E03** | Happy — ONGOING | `PATCH status` → `ONGOING` | 200, `status=ONGOING` |
| **G1-N01** | WORKSHOP trước KICKOFF | POST WORKSHOP (chưa có KICKOFF) | 422 `EVENT_KICKOFF_MISSING` hoặc `EVENT_OUT_OF_HACKATHON` |
| **G1-N02** | AWARDS trước WORKSHOP | POST AWARDS (chỉ có KICKOFF) | 422 `EVENT_ORDER_VIOLATION` |
| **G1-N03** | WORKSHOP cùng ngày KICKOFF | KO 07/06 + WS 07/06 tối | 422 `EVENT_ORDER_VIOLATION` |
| **G1-N04** | Thiếu KICKOFF → ONGOING | Greenfield thiếu KO → `PATCH ONGOING` | 422 `READINESS_NOT_PASSED` / `EVENT_KICKOFF_MISSING` |
| **G1-N05** | Judge CK ở GĐ1 | `POST judge-assignments` `FINAL_EXTERNAL` + `roundId` CK | 422 `JUDGE_FINAL_AT_PHASE1` |
| **G1-N06** | DELETE KICKOFF khi còn WS | Tạo KO+WS → `DELETE` KICKOFF | 422 `EVENT_ORDER_VIOLATION` |
| **G1-N07** | DELETE WORKSHOP khi còn AWARDS | Tạo KO+WS+AWARDS → `DELETE` WORKSHOP | 422 `EVENT_ORDER_VIOLATION` |
| **G1-N08** | Thiếu round CK | Chỉ prelim → `readiness?target=ONGOING` | `ready: false`, `MISSING_FINAL_ROUND` |
| **G1-R01** | Readiness FINAL_ROUND (sớm) | Hackathon GĐ1 xong, chưa judge CK | `FINAL_ROUND` → `ready: false` |
| **G1-R02** | Readiness AWARDS (sớm) | Chưa có AWARDS | `AWARDS` → `ready: false`, `EVENT_AWARDS_MISSING` |

---

## 4. Test case — GĐ2 (Đăng ký & lottery)

| ID | Mô tả | Các bước | Kỳ vọng |
|----|-------|----------|---------|
| **G2-H01** | Tạo đội khi ONGOING | `POST /teams` trên hackathon ONGOING | 201 |
| **G2-N01** | Tạo đội khi DRAFT | `POST /teams` hackathon DRAFT | 422 hackathon không ONGOING |
| **G2-H02** | Lottery sau lock | Đợi ngày **sau** `registrationEnd` hoặc seed đã lock → `POST lottery` | 200, `team_round_tracks` |
| **G2-N02** | Lottery khi chưa lock | `is_locked=false` → lottery | 422 `TEAM_NOT_LOCKED` |

---

## 5. Test case — GĐ3 (Sơ loại — Gate2)

| ID | Mô tả | Các bước | Kỳ vọng |
|----|-------|----------|---------|
| **G3-H01** | Activate prelim | Sau lottery → `PATCH .../activate` prelim | 200, `isActive=true` |
| **G3-H02** | Student submission | `GET /me/submission`, `POST /submissions` | 200/201 |
| **G3-H03** | Mentor portal | `GET /me/mentor/rounds`, assigned-teams | 200 — xem [fe-gd3-api-mapping.md](fe-gd3-api-mapping.md) |
| **G3-H04** | Lock scoring SL | `PATCH lock-scoring` prelim | `scoringLocked=true` |
| **G3-N01** | Activate prelim không đội | Round không có lottery | 422 `NO_TEAMS_IN_ROUND` |
| **G3-T01** | Timeline cascade | `PUT /rounds/{prelimId}` đổi `examAt` + `codingDurationHours` | `submissionOpen`/`deadline` tự tính; `presentation_slots` đổi (nếu chưa DONE) |

**Seed:** `seal-gd3-prelim-open`

---

## 6. Test case — GĐ4 (Publish + Advance + Gate3 setup)

| ID | Mô tả | Các bước | Kỳ vọng |
|----|-------|----------|---------|
| **G4-H01** | Publish Sơ loại | `PATCH /rounds/{prelimId}/publish` | 200 |
| **G4-H02** | Advance teams | `POST .../advance` | `team_round_participation` CK |
| **G4-H03** | Judge FINAL_EXTERNAL | `POST judge-assignments` CK | 201 |
| **G4-R01** | Readiness FINAL_ROUND | Sau advance + judge | `FINAL_ROUND` → `ready: true` |
| **G4-H04** | Activate CK | `PATCH /rounds/{finalId}/activate` | 200 |
| **G4-N01** | Activate CK chưa publish | Bỏ publish → activate final | 422 `RESULT_NOT_PUBLISHED` |
| **G4-N02** | Activate CK thiếu judge | Bỏ 4.5 → activate final | 422 (activate gate) |

---

## 7. Test case — GĐ5 (Chung kết)

| ID | Mô tả | Các bước | Kỳ vọng |
|----|-------|----------|---------|
| **G5-H01** | Nộp bài CK | `POST /submissions` `roundId=final` (không `trackId`) | 201 |
| **G5-H02** | Chấm CK | `POST /scores` judge FINAL | 201 |
| **G5-H03** | Lock CK | `PATCH lock-scoring` final | `scoringLocked=true` |
| **G5-H04** | Chuyển PENDING_CONFIRM | `GET /hackathons/{id}` | `status=PENDING_CONFIRM` |

**Seed:** `seal-gd5-final-active`

---

## 8. Test case — GĐ6 (AWARDS + Kết thúc)

| ID | Mô tả | Các bước | Kỳ vọng |
|----|-------|----------|---------|
| **G6-H01** | Tạo AWARDS (nếu chưa có) | POST AWARDS sau WORKSHOP | 201 |
| **G6-R01** | Readiness AWARDS | `GET readiness?target=AWARDS` | `ready: true` |
| **G6-H02** | Trao giải | `POST /hackathons/{id}/prizes` | 201 |
| **G6-H03** | Confirm FINISHED | `PATCH /hackathons/{id}/confirm` | `status=FINISHED` |

**Seed:** `seal-gd6-pending-confirm`

---

## 9. E2E full path (1 lần chạy tuần tự)

```text
GĐ0  Login coord + student + judge + mentor
GĐ1  1.1→1.12 (KO → WS, bỏ 1.10c) → ONGOING
GĐ2  teams → lock → lottery
GĐ3  activate prelim → submit → score → lock-scoring prelim
GĐ4  publish → advance → judge FINAL → readiness FINAL_ROUND → activate final
GĐ5  submit CK → score CK → lock-scoring final → PENDING_CONFIRM
GĐ6  POST AWARDS (nếu thiếu) → readiness AWARDS → prizes → confirm → FINISHED
```

Đánh dấ `[x]` từng ID khi pass. Nếu fail — ghi `blockers[].code` / HTTP status để trace conflict code.

---

## 10. Bảng “code đá nhau” thường gặp

| Triệu chứng | Code | Nguyên nhân | GĐ |
|-------------|------|-------------|-----|
| Không ONGOING được dù đủ round | `EVENT_AWARDS_MISSING` | **Đã fix** — không còn block ONGOING | GĐ1 |
| WS trước KO | `EVENT_KICKOFF_MISSING` | Sai thứ tự event | GĐ1 |
| Judge CK sớm | `JUDGE_FINAL_AT_PHASE1` | Gán FINAL ở GĐ1 | GĐ1 |
| Activate SL không đội | `NO_TEAMS_IN_ROUND` | Chưa lottery/lock | GĐ3 |
| Activate CK sớm | `RESULT_NOT_PUBLISHED` | Chưa publish SL | GĐ4 |
| Deadline submission sai | `ROUND_DEADLINE_INVALID` | Thiếu auto-recalc — gửi `examAt`+`codingDurationHours` | GĐ1/3 |
| Team chưa lock mà lottery | `TEAM_NOT_LOCKED` | Chạy GĐ2 cùng ngày `registrationEnd` | GĐ2 |

---

**Phiên bản:** 2026-06-07 · POST KICKOFF→WORKSHOP; lịch WS→KO→AWARDS; readiness phased.
