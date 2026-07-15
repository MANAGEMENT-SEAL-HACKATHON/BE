# Dev seed slugs — hướng dẫn (6 happy path)

**Nguồn SSOT code:** `DevSeedCatalog.ALL_DEV_HACKATHON_SLUGS`  
**Nạp lúc start:** `DataInitializer` (`@Profile("dev")`)  
**Cập nhật:** 2026-07-14  

Chỉ **6 hackathon slug** còn lại. Lỗi/gate cố tình: tái tạo tay trên happy slug — xem [intentional-errors-catalog.md](intentional-errors-catalog.md).  
Account / password: [dev-seed-guide.md](dev-seed-guide.md).  
Chạy tay UI: [manual-ui-playbook-gd1-gd6.md](manual-ui-playbook-gd1-gd6.md).

---

## Bộ 6 slug

| # | Slug | Giai đoạn | Làm gì |
|---|------|-----------|--------|
| 1 | `seal-e2e-2026` | GĐ1–2 | Full structure + 7 đội + 3 orphan; ĐK mở, prelim inactive — happy Setup/Lottery |
| 2 | `seal-fall-2025-finished` | Archive | Kỳ **FINISHED** (complete) — portal xem kết quả / export |
| 3 | `seal-gd3-prelim-open` | GĐ3 | Coding mở, 5/6 đã nộp, mentors gán, **chưa** queue — full: nộp → close-early → shuffle → chấm → lock |
| 4 | `seal-gd4-advance-ready` | GĐ4 | SL locked, unpublished, CK có criteria/judge — Publish → WC → Advance → Activate CK |
| 4a | `seal-gd4-tiebreak-submission-time` | GĐ4 | Tiebreak `SUBMISSION_TIME` — Team2 nộp sớm hơn Team3 tại biên Top-2 |
| 4b | `seal-gd4-tiebreak-manual` | GĐ4 | Tiebreak `COORDINATOR_DECISION` — Advance → `TIEBREAK_REQUIRED` |
| 4c | `seal-gd4-wildcard-gap` | GĐ4 | 2 bảng, topN=1, minFinal=4 → `availableSlots=2` |
| 5 | `seal-gd5-final-active` | GĐ5 | CK active, 4 ADVANCED, submit mở, **0** queue — Submit → close → queue → chấm → lock |
| 6 | `seal-gd6-pending-confirm` | GĐ6 | PENDING_CONFIRM + FIRST/SECOND/THIRD — Confirm → FINISHED |

**Không phải slug hackathon:** `AccountStatesDataSeeder` (user trạng thái duyệt / email…).

**Slug cũ (~47)** nằm trong `DevSeedCatalog.DEPRECATED_SLUGS` và bị **purge** mỗi lần start `dev`.

---

## Chi tiết từng slug

### 1. `seal-e2e-2026`

| | |
|--|--|
| **Status** | `ONGOING` |
| **Seeder** | `Gd1DataSeeder` + `E2eWorkflowDataSeeder` |
| **Hỗ trợ** | Happy GĐ1 verify; GĐ2 close-reg → lock → lottery → activate; Mode A continuous |
| **Account** | `coord@fpt.edu.vn` · `student.e2e.t01.leader@…` · orphans `student.e2e.orphan1@…` / `Student@dev1` |

### 2. `seal-fall-2025-finished`

| | |
|--|--|
| **Status** | `FINISHED` |
| **Seeder** | `Gd1DataSeeder` (archive) |
| **Hỗ trợ** | FR-U-32 xem kết quả cũ; export CSV; không mutate workflow |
| **Account** | `student.archive.fall2025@fpt.edu.vn` |

### 3. `seal-gd3-prelim-open`

| | |
|--|--|
| **Status** | `ONGOING` — prelim active, đề released |
| **Seeder** | `Gd3PrelimOpenDataSeeder` |
| **Hỗ trợ** | Full GĐ3 UI + mentor portal (mentor đã gán mọi đội) |
| **Account** | `student.gd3.leader06@…` (demo nộp) · `mentor@`–`mentor3@` · `judge1@`–`judge4@` (INTERNAL; không guest prelim) |

### 4. `seal-gd4-advance-ready`

| | |
|--|--|
| **Status** | `ONGOING` — scoring locked, chưa publish |
| **Seeder** | `Gd4AdvanceReadyDataSeeder` |
| **Hỗ trợ** | Full GĐ4 — ranking / wildcard / advance / activate CK |
| **Account** | `coord@fpt.edu.vn` · leaders `student.gd4a.leader0N@…` |

### 5. `seal-gd5-final-active`

| | |
|--|--|
| **Status** | `ONGOING` — CK active, submit window mở |
| **Seeder** | `Gd5FinalRoundDataSeeder` |
| **Hỗ trợ** | Full GĐ5 — nộp CK → close-early → queue → chấm → lock |
| **Account** | `student.gd5.leader0N@…` · `guestjudge@`–`guestjudge3@` (FINAL_EXTERNAL) · `judge1@` HEAD CK |

### 6. `seal-gd6-pending-confirm`

| | |
|--|--|
| **Status** | `PENDING_CONFIRM` — CK locked + đủ 3 giải |
| **Seeder** | `Gd6PendingConfirmDataSeeder` |
| **Hỗ trợ** | Confirm → FINISHED (+ xem giải) |
| **Account** | `coord@fpt.edu.vn` · `student.gd6.leader0N@…` |

---

## Liên kết

- [intentional-errors-catalog.md](intentional-errors-catalog.md)
- [master-slug-test-matrix.md](master-slug-test-matrix.md)
- [manual-ui-playbook-gd1-gd6.md](manual-ui-playbook-gd1-gd6.md)
