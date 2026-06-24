# Giai đoạn 1 (GĐ1) — Ma trận test đầy đủ & Seed dev

> **Mục đích:** Tài liệu QA / FE / BE cho **GĐ1 Chuẩn bị sự kiện** — hackathon, events, rounds, tracks, criteria, judge/mentor, readiness, PATCH `ONGOING`, **thời lượng timer** (default round / override track).  
> **Profile:** `dev` · **Coordinator:** `coord@fpt.edu.vn` / `Coordinator@dev1`  
> **Liên quan:** **GĐ2:** [gd2-full-test-matrix-and-seeds.md](gd2-full-test-matrix-and-seeds.md) · **GĐ3:** [gd3-full-test-matrix-and-seeds.md](gd3-full-test-matrix-and-seeds.md)

---

## Mục lục

1. [Phạm vi GĐ1 & điều kiện ra](#1-phạm-vi-gđ1--điều-kiện-ra)
2. [Profile seed dev](#2-profile-seed-dev)
3. [Ma trận test theo chức năng (FR)](#3-ma-trận-test-theo-chức-năng-fr)
4. [Luồng end-to-end](#4-luồng-end-to-end)
5. [Business rules (tóm tắt)](#5-business-rules-tóm-tắt)
6. [Checklist smoke sau restart BE](#6-checklist-smoke-sau-restart-be)
7. [Phụ lục — Mã lỗi & API surface](#7-phụ-lục--mã-lỗi--api-surface)

---

## 1. Phạm vi GĐ1 & điều kiện ra

### 1.1 GĐ1 bao gồm

| Hạng mục | Mô tả |
|----------|--------|
| **Hackathon CRUD** | Tạo/sửa mùa giải, timeline đăng ký |
| **Events** | KICKOFF → WORKSHOP → (AWARDS GĐ6) — thứ tự POST vs lịch |
| **Round SL + CK** | PRELIMINARY + FINAL (`isFinal=true`, CK **không** có track con) |
| **Track (SL only)** | 2–3 track / round sơ loại |
| **Criteria** | Per track (SL) + per round (CK); SUM(weight)=1.0 |
| **Judge / Mentor** | Gán track (SL); **không** gán FINAL judge ở GĐ1 |
| **Readiness** | `GET .../readiness?target=ONGOING` |
| **Gate 1** | `PATCH /hackathons/{id}/status` → `ONGOING` |
| **Thời lượng timer** | `defaultPresentationMinutes` / `defaultQaMinutes` (round); `presentationMinutes` / `qaMinutes` (track SL, optional) |

### 1.2 Gate G1–G5 (readiness)

| Gate | Điều kiện |
|------|-----------|
| G1 | ≥1 Round PRELIMINARY + ≥1 Track |
| G2 | Đúng 1 Round FINAL |
| G3 | Mọi track SL: criteria, SUM(weight)=1.0 |
| G4 | Round CK: criteria, SUM(weight)=1.0 |
| G5 | ≥1 event KICKOFF hợp lệ |

### 1.3 Gate ra GĐ2

| Gate | Điều kiện | Verify |
|------|-----------|--------|
| G-2.0 | Hackathon `status=ONGOING` | `GET /hackathons/{id}` |
| G-2.1 | `registration_end` chưa qua (hoặc seed repair mở đăng ký) | Timeline hackathon |
| G-2.2 | Có round SL + CK, track, criteria | Readiness đã pass |

### 1.4 Thời lượng thuyết trình (thiết lập GĐ1)

| Layer | Field | Default | Ghi chú |
|-------|-------|---------|---------|
| Round SL / CK | `defaultPresentationMinutes` | **10** | CK dùng cho GĐ5 |
| Round SL / CK | `defaultQaMinutes` | **5** | |
| Track SL | `presentationMinutes` | `null` | Override — optional |
| Track SL | `qaMinutes` | `null` | |

**API GĐ1:** `GET/PUT /api/v1/rounds/{id}`, `GET/PUT /api/v1/tracks/{id}` — xem [fe-gd1-gd2-structure-and-fields.md](fe-gd1-gd2-structure-and-fields.md) §4–5.

**API vận hành GĐ3/GĐ5:** `PUT /api/v1/presentation/duration` — xem [fe-gd3-api-mapping.md](fe-gd3-api-mapping.md) §9.4.1.

---

## 2. Profile seed dev

Sau `mvn spring-boot:run` (profile `dev`), `Gd1DataSeeder` + `E2eWorkflowDataSeeder` tạo dữ liệu GĐ1/GĐ2 trên cùng hackathon chính.

> **Lưu ý:** Slug legacy (`seal-gd1-ready`, `seal-gd1-incomplete`, `seal-spring-2026*`) bị **xóa tự động** khi start dev — xem [dev-seed-guide.md](dev-seed-guide.md).

### Profile 0 — E2E ONGOING (happy path GĐ1→GĐ6)

| | |
|--|--|
| **Slug** | `seal-e2e-2026` |
| **Seeder** | `Gd1DataSeeder` (+ `E2eWorkflowDataSeeder` cho GĐ2) |
| **Config** | Mặc định profile `dev` |
| **Trạng thái** | `ONGOING` — đủ G1–G5, prelim **chưa active** |

| Thành phần | Chi tiết |
|------------|----------|
| Rounds | Sơ loại + Chung kết |
| Tracks | 3 track SL (Track 3 = clone demo) |
| Events | KICKOFF + WORKSHOP |
| Criteria | Đủ weight trên track + round CK |
| Timer defaults | Round: 10+5 phút; track override: null |
| GĐ2 data | 7 đội `E2E-T01`…`T07` + 3 orphan (xem GĐ2 doc) |

**Dùng khi:** Smoke GĐ1 structure + chuyển sang GĐ2 trên cùng slug.

**Verify nhanh:**

```http
GET /api/v1/hackathons?q=seal-e2e-2026
GET /api/v1/hackathons/{id}/readiness?target=ONGOING
GET /api/v1/rounds/{prelimId}
GET /api/v1/rounds/{prelimId}/tracks
```

---

### Profile A — FINISHED archive (read-only)

| | |
|--|--|
| **Slug** | `seal-fall-2025-finished` |
| **Seeder** | `Gd1DataSeeder.seedFinishedHackathon` |
| **Trạng thái** | `FINISHED` — full structure, **không** mutation |

**Dùng khi:** Test `HACKATHON_ARCHIVED` / UI read-only / lịch sử mùa cũ.

---

### Profile B — Greenfield (tạo tay full GĐ1)

Không có slug riêng — Coordinator tạo mới:

1. `POST /hackathons`
2. `POST /hackathons/{id}/rounds` ×2 (SL + CK)
3. `PUT /rounds/{finalId}` — set `defaultPresentationMinutes`, `defaultQaMinutes`
4. `POST /rounds/{prelimId}/tracks` ×N
5. `PUT /tracks/{id}` — optional override timer track
6. `POST .../criteria` (batch) — weight = 1.0
7. `POST /events` KICKOFF → WORKSHOP
8. `GET .../readiness?target=ONGOING` → `PATCH .../status` ONGOING

**Dùng khi:** Demo onboarding Coordinator từ đầu; regression G1-E01…G1-E03.

---

### Profile C — Readiness fail (negative)

| Cách test | Mô tả |
|-----------|--------|
| **Unit / integration** | `RoundReadinessServiceTest`, `EventOrderValidatorTest` |
| **Tay** | Hackathon `DRAFT` không round → `readiness` blockers |
| **Tay** | Thiếu KICKOFF → `PATCH ONGOING` fail |

Slug `seal-gd1-incomplete` **không còn** seed tự động — dùng Profile B bỏ bước round hoặc testcase **G1-N04** trong [gate-regression-test-matrix-gd1-gd6.md](gate-regression-test-matrix-gd1-gd6.md).

---

## 3. Ma trận test theo chức năng (FR)

### FR-02 — Hackathon

| # | Case | Kỳ vọng | Profile |
|---|------|---------|---------|
| H1 | POST hackathon hợp lệ | 201 | B |
| H2 | GET list / detail | 200 | 0, A |
| N1 | Trùng slug | 409 `HACKATHON_DUPLICATE` | Tay |
| N2 | `eventStart < registrationEnd` | 422 `HACKATHON_DATE_RANGE` | Tay |

### FR-02 — Events

| # | Case | Kỳ vọng | Profile |
|---|------|---------|---------|
| E1 | POST KICKOFF → WORKSHOP | 201 | 0, B |
| N1 | WORKSHOP trước KICKOFF | 422 | C / unit |
| N2 | AWARDS trước WORKSHOP | 422 `EVENT_ORDER_VIOLATION` | unit |
| N3 | DELETE KICKOFF khi còn WS | 422 | unit G1-N06 |

### FR-02/03 — Round & Track

| # | Case | Kỳ vọng | Profile |
|---|------|---------|---------|
| R1 | POST round SL + CK | 201 | B |
| R2 | GET round — có `defaultPresentationMinutes` | 10 (default) | 0 |
| R3 | PUT round CK — set timer 12+8 | 200 | B |
| T1 | POST track SL | 201 | B |
| T2 | GET track — `presentationMinutes` null | fallback round | 0 |
| T3 | PUT track — override 15+7 | 200 | B |
| N1 | POST track trong round CK | 422 `DESIGN_VIOLATION` | Tay |
| N2 | Criteria weight ≠ 1.0 | 422 / warning | Tay |

### FR-03 — Criteria / Judge / Mentor (GĐ1)

| # | Case | Kỳ vọng | Profile |
|---|------|---------|---------|
| C1 | POST criteria track — batch | 201, weight sum OK | 0, B |
| C2 | POST criteria round CK | 201 | 0, B |
| J1 | POST judge assignment track SL | 201 | B |
| J2 | POST judge FINAL CK ở GĐ1 | 422 `JUDGE_FINAL_AT_PHASE1` | C |
| M1 | POST mentor track | 201 | B |

### FR-06 — Readiness & Gate ONGOING

| # | Case | Kỳ vọng | Profile |
|---|------|---------|---------|
| G1 | `readiness?target=ONGOING` | `ready: true` | 0 |
| G2 | `PATCH status` → ONGOING | 200 | 0, B |
| N1 | Thiếu final round | `ready: false` | C |
| N2 | Thiếu KICKOFF | block PATCH | C |

---

## 4. Luồng end-to-end

### 4.1 Happy path GĐ1 (Profile B — greenfield)

```text
POST /hackathons
→ POST /rounds (SL + CK)
→ PUT /rounds/{finalId}  (defaultPresentationMinutes, defaultQaMinutes)
→ POST /tracks (SL)
→ POST /criteria (track + final)
→ POST /judge-assignments, /mentor-assignments
→ POST /events (KICKOFF, WORKSHOP)
→ GET /readiness?target=ONGOING
→ PATCH /hackathons/{id}/status  { "status": "ONGOING" }
→ Sang GĐ2: [gd2-full-test-matrix-and-seeds.md](gd2-full-test-matrix-and-seeds.md)
```

### 4.2 Đường tắt (Profile 0)

```text
GET /hackathons?q=seal-e2e-2026
→ GET /readiness?target=ONGOING  (đã ONGOING — verify ready)
→ GET /rounds, /tracks  (lấy ID cho GĐ2/GĐ3)
```

---

## 5. Business rules (tóm tắt)

| Rule | Chi tiết |
|------|----------|
| Kiến trúc | Hackathon → Round → Track (chỉ SL) |
| CK | Không track con; criteria trên round |
| `topNAdvance` / `minTeamsFinal` | Trên **round SL**, không trên track |
| Timer CK | `defaultPresentationMinutes` / `defaultQaMinutes` trên round FINAL |
| Timer SL | Default round + optional override per track |
| ONGOING | Cần readiness pass; không cần AWARDS event |
| FINISHED | Mọi mutation → `HACKATHON_ARCHIVED` |

Chi tiết: [mf01/01-business-rules.md](../mf01/01-business-rules.md) · [workflow/mf01-gd1-quy-trinh-api.md](../workflow/mf01-gd1-quy-trinh-api.md)

---

## 6. Checklist smoke sau restart BE

```text
# 1. Log seeder
grep "Gd1DataSeeder" logs — có seal-e2e-2026 + seal-fall-2025-finished

# 2. Readiness
GET /api/v1/hackathons?q=seal-e2e-2026 → id
GET /api/v1/hackathons/{id}/readiness?target=ONGOING → ready: true

# 3. Timer fields
GET /api/v1/rounds/{prelimId} → defaultPresentationMinutes=10
GET /api/v1/rounds/{finalId} → defaultQaMinutes=5
GET /api/v1/tracks/{track1Id} → presentationMinutes null hoặc override

# 4. Archive
PATCH /hackathons/{finishedId} body bất kỳ → 409 HACKATHON_ARCHIVED
```

---

## 7. Phụ lục — Mã lỗi & API surface

### Error codes (GĐ1)

| Mã | HTTP | Khi nào | Ma trận |
|----|------|---------|---------|
| `READINESS_NOT_PASSED` | 422 | PATCH ONGOING khi chưa đủ gate | G2, N2 |
| `EVENT_KICKOFF_MISSING` | 422 | WORKSHOP / ONGOING thiếu KO | E N1 |
| `EVENT_ORDER_VIOLATION` | 422 | Thứ tự event sai | E N2 |
| `JUDGE_FINAL_AT_PHASE1` | 422 | Gán judge CK sớm | J2 |
| `HACKATHON_ARCHIVED` | 409 | Mutation trên FINISHED | A |
| `DESIGN_VIOLATION` | 422 | Track trong round CK | T N1 |
| `HACKATHON_DUPLICATE` | 409 | Slug/name trùng | H N1 |

### API surface (Coordinator — GĐ1)

| Method | Path |
|--------|------|
| POST/GET/PUT | `/api/v1/hackathons`, `/api/v1/hackathons/{id}` |
| GET | `/api/v1/hackathons/{id}/readiness` |
| PATCH | `/api/v1/hackathons/{id}/status` |
| POST/GET/PUT/DELETE | `/api/v1/hackathons/{id}/rounds`, `/api/v1/rounds/{id}` |
| POST/GET/PUT/DELETE | `/api/v1/rounds/{roundId}/tracks`, `/api/v1/tracks/{id}` |
| POST/GET | `/api/v1/tracks/{id}/criteria`, `/api/v1/rounds/{id}/criteria` |
| POST/GET/DELETE | `/api/v1/tracks/{id}/judges`, `/api/v1/tracks/{id}/mentors` |
| POST/GET/PUT/DELETE | `/api/v1/hackathons/{id}/events` |

---

## Phụ lục — Map tài liệu

| Tài liệu | Nội dung |
|----------|----------|
| [gd2-full-test-matrix-and-seeds.md](gd2-full-test-matrix-and-seeds.md) | GĐ2 — đội, lottery |
| [fe-gd1-gd2-structure-and-fields.md](fe-gd1-gd2-structure-and-fields.md) | Field form FE |
| [workflow/mf01-gd1-quy-trinh-api.md](../workflow/mf01-gd1-quy-trinh-api.md) | Quy trình API 7 bước |
| [dev-seed-guide.md](dev-seed-guide.md) | 2 hackathon dev |
| [gate-regression-test-matrix-gd1-gd6.md](gate-regression-test-matrix-gd1-gd6.md) | Testcase G1-* |
| [gd3-full-test-matrix-and-seeds.md](gd3-full-test-matrix-and-seeds.md) | GĐ3 tiếp theo |

---

*Cập nhật: 2026-06-24 — ma trận GĐ1 + field timer + profile seed `seal-e2e-2026` / `seal-fall-2025-finished`.*
