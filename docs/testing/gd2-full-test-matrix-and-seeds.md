# Giai đoạn 2 (GĐ2) — Ma trận test đầy đủ & Seed dev

> **Mục đích:** Tài liệu QA / FE / BE cho **GĐ2 Đăng ký & bốc thăm** — tạo đội, mời thành viên, duyệt, khóa roster, lottery track/bảng, mentor per-round.  
> **Profile:** `dev` · **Password SV:** `Student@dev1` · **Coordinator:** `coord@fpt.edu.vn` / `Coordinator@dev1`  
> **Liên quan:** **GĐ1:** [gd1-full-test-matrix-and-seeds.md](gd1-full-test-matrix-and-seeds.md) · **GĐ3:** [gd3-full-test-matrix-and-seeds.md](gd3-full-test-matrix-and-seeds.md)

---

## Mục lục

1. [Phạm vi GĐ2 & điều kiện vào](#1-phạm-vi-gđ2--điều-kiện-vào)
2. [Profile seed dev](#2-profile-seed-dev)
3. [Ma trận test theo chức năng (FR)](#3-ma-trận-test-theo-chức-năng-fr)
4. [Luồng end-to-end](#4-luồng-end-to-end)
5. [Business rules (tóm tắt)](#5-business-rules-tóm-tắt)
6. [Checklist smoke sau restart BE](#6-checklist-smoke-sau-restart-be)
7. [Phụ lục — Mã lỗi & API surface](#7-phụ-lục--mã-lỗi--api-surface)

---

## 1. Phạm vi GĐ2 & điều kiện vào

### 1.1 GĐ2 bao gồm

| Hạng mục | Mô tả |
|----------|--------|
| **Đăng ký hackathon** | Student APPROVED đăng ký mùa |
| **Tạo đội** | Leader `POST /teams` → `PENDING` |
| **Mời / accept thành viên** | FR-12 |
| **Coordinator duyệt đội** | `PENDING` → `ACTIVE` / `REJECTED` |
| **Khóa roster** | Sau `registration_end` → `is_locked=true` |
| **Lottery (bốc thăm)** | Gán track + `assignedGroup` (bảng) |
| **Re-lottery** | Đổi track trước khi round active |
| **Mentor per-round** | Gán mentor theo `(team, round)` |
| **Orphan students** | SV đăng ký nhưng chưa có đội — test invite |

### 1.2 Gate vào GĐ2 (từ GĐ1)

| Gate | Điều kiện | Verify |
|------|-----------|--------|
| G-2.1 | Hackathon `ONGOING` | `GET /hackathons/{id}` |
| G-2.2 | `registration_end` > now (seed repair) | Timeline |
| G-2.3 | Round SL tồn tại, **chưa** `is_active` | `GET /rounds/{prelimId}` |

### 1.3 Gate ra GĐ3

| Gate | Điều kiện |
|------|-----------|
| G-3.0 | Đội `ACTIVE`, `is_locked=true` (sau registration_end hoặc seed repair) |
| G-3.1 | `team_round_participation` + `team_round_tracks` sau lottery |
| G-3.2 | Coordinator `PATCH /rounds/{prelimId}/activate` |

### 1.4 Track vs Bảng (lottery output)

| Khái niệm | DB / API |
|-----------|----------|
| **Track** | Chủ đề bốc thăm — `team_round_tracks.track_id` |
| **Bảng** | `assignedGroup` (vd. `BANG-A`) trong cùng track |
| **Capacity** | `maxTeams`, `maxTeamsPerGroup` trên track (GĐ1) |

Chi tiết: [fe-gd1-gd2-structure-and-fields.md](fe-gd1-gd2-structure-and-fields.md) §1–3.

---

## 2. Profile seed dev

Dữ liệu GĐ2 nằm trên hackathon GĐ1 **`seal-e2e-2026`** — seeder `E2eWorkflowDataSeeder` (`app.seed.e2e.enabled=true`).

### Profile 0 — Teams sẵn (chưa lock, chưa lottery)

| | |
|--|--|
| **Slug** | `seal-e2e-2026` |
| **Seeder** | `E2eWorkflowDataSeeder` |
| **Config** | `app.seed.e2e.enabled=true` |
| **Trạng thái** | 7 đội `ACTIVE`, 3 người/đội, **chưa** `is_locked`, **chưa** lottery |

| Đội | Track (sau lottery) | Ghi chú |
|-----|---------------------|---------|
| E2E-T01 … T07 | *(chưa gán)* | Phân bổ seed: 3+2+2 trên 3 track |

**Leader accounts:** `student.e2e.t01.leader@fpt.edu.vn` … `t07.leader@` / `Student@dev1`

**Dùng khi:** Test duyệt đội, invite, lottery tay, re-lottery.

**API chính:**

```http
GET /api/v1/teams?hackathonId={id}&status=ACTIVE
PATCH /api/v1/hackathons/{id}/lottery
GET /api/v1/me/teams
```

---

### Profile A — Orphan students (invite flow)

| | |
|--|--|
| **Slug** | `seal-e2e-2026` (cùng hackathon) |
| **Số lượng** | 3 SV đã `hackathon_registrations`, **chưa** có đội |

| # | Email | Password |
|---|-------|----------|
| 1 | `student.e2e.orphan1@fpt.edu.vn` | `Student@dev1` |
| 2 | `student.e2e.orphan2@fpt.edu.vn` | `Student@dev1` |
| 3 | `student.e2e.orphan3@fpt.edu.vn` | `Student@dev1` |

**Luồng test:** Leader tạo đội mới → `POST .../members/invite` ×3 → orphan accept → Coord duyệt.

---

### Profile B — Post-lottery (sau lottery tay trên Profile 0)

Không có slug riêng — sau khi chạy lottery trên `seal-e2e-2026`:

| Kiểm tra | Kỳ vọng |
|----------|---------|
| `team_round_tracks` | Mỗi đội có `trackId` + `assignedGroup` |
| `GET /me/teams` | Student thấy track |
| `PATCH /rounds/{prelimId}/activate` | 200 — sang GĐ3 |

**Repair dev:** `E2eWorkflowDataSeeder.repairForGd2Testing()` sync lịch + unlock nếu cần retest lottery.

---

### Profile C — Negative / edge

| Case | Cách test |
|------|-----------|
| Tạo đội khi DRAFT | Hackathon không ONGOING → 422 |
| Lottery chưa lock | `is_locked=false` → 422 `TEAM_NOT_LOCKED` |
| Lottery khi round active | 422 `ROUND_ALREADY_ACTIVE` |
| User PENDING login | 401 |

---

## 3. Ma trận test theo chức năng (FR)

### FR-07–10 — Auth & đăng ký (MF-02)

| # | Case | Kỳ vọng | Profile |
|---|------|---------|---------|
| A1 | Student APPROVED login | 200 JWT | orphan, leader |
| A2 | PENDING judge login | 401 | GĐ1 pending judge |
| A3 | POST hackathon register | 201 | Tay / orphan |

### FR-11 — Tạo đội

| # | Case | Kỳ vọng | Profile |
|---|------|---------|---------|
| T1 | POST /teams ONGOING | 201 `PENDING` | Tay |
| T2 | Leader = current user | team_members LEADER | T1 |
| N1 | Hackathon DRAFT | 422 `HACKATHON_NOT_ONGOING` | C |
| N2 | Sau registration_end | 422 `REGISTRATION_CLOSED` | C |
| N3 | Tên đội trùng | 409 `TEAM_NAME_DUPLICATE` | Tay |
| N4 | User đã có đội ACTIVE | 409 `USER_IN_ANOTHER_TEAM` | 0 |

### FR-12 — Thành viên

| # | Case | Kỳ vọng | Profile |
|---|------|---------|---------|
| M1 | Invite email hợp lệ | 201 | A |
| M2 | Invitee ACCEPT | 200 | A |
| M3 | Invitee REJECT | 200 | Tay |
| N1 | Invite khi team locked | 403 `TEAM_LOCKED` | B (sau lock) |
| N2 | >5 ACCEPTED | 409 `TEAM_MEMBER_FULL` | Tay |

### FR-13 — Duyệt đội

| # | Case | Kỳ vọng | Profile |
|---|------|---------|---------|
| P1 | PATCH ACTIVE (3–5 members) | 200 | 0, A |
| P2 | PATCH REJECTED + reason | 200 | Tay |
| P3 | Bulk approve | 200 | Tay |
| N1 | ACTIVE thiếu thành viên | 422 | Tay |

### FR-13A — Khóa đội

| # | Case | Kỳ vọng | Profile |
|---|------|---------|---------|
| L1 | Cron sau registration_end | `is_locked=true` | B / repair |
| L2 | Invite sau lock | 403 | B |

### FR-13B — Lottery

| # | Case | Kỳ vọng | Profile |
|---|------|---------|---------|
| B1 | PATCH lottery (auto) | 200, có `team_round_tracks` | 0 → B |
| B2 | Lottery manual assignments | 200 | Tay |
| B3 | GET /me/teams — có track | 200 | B |
| N1 | Lottery chưa lock | 422 `TEAM_NOT_LOCKED` | 0 (trước lock) |
| N2 | Re-lottery round active | 423 `ROUND_ALREADY_ACTIVE` | GĐ3 slug |

### FR-13C — Mentor

| # | Case | Kỳ vọng | Profile |
|---|------|---------|---------|
| MT1 | POST mentor (team, round) | 201 | B |
| MT2 | GET team mentors | 200 | B |
| N1 | Mentor trước lottery | 422 (chưa participation) | 0 |

---

## 4. Luồng end-to-end

### 4.1 Happy path — orphan → lottery (Profile A + 0)

```text
1. student mới: POST /me/hackathons/{id}/register
2. POST /teams  → PENDING
3. POST /teams/{id}/members/invite  (orphan1..3)
4. orphan PATCH accept
5. Coordinator PATCH /teams/{id}/status ACTIVE
6. (Chờ registration_end hoặc seed repair lock)
7. PATCH /hackathons/{id}/lottery  { "roundId": prelimId }
8. GET /me/teams  → có trackId, assignedGroup
9. POST /teams/{id}/rounds/{prelimId}/mentor  (optional)
10. Sang GĐ3: PATCH /rounds/{prelimId}/activate
```

### 4.2 Đường tắt — 7 đội có sẵn (Profile 0)

```text
GET /teams?hackathonId=&status=ACTIVE  → 7 đội E2E-T*
→ (repair lock nếu cần)
→ PATCH /lottery
→ activate prelim → [gd3-full-test-matrix-and-seeds.md](gd3-full-test-matrix-and-seeds.md)
```

---

## 5. Business rules (tóm tắt)

| Rule | Chi tiết |
|------|----------|
| Một SV / một đội ACTIVE-PENDING / hackathon | |
| Không chọn track lúc tạo đội | Lottery GĐ2 |
| `topNAdvance` trên round SL | Ranking GĐ3/GĐ4 per bảng |
| Lottery cần `is_locked=true` | |
| Re-lottery chỉ khi round **chưa** active | |

Chi tiết: [mf02/01-business-rules-gd2.md](../mf02/01-business-rules-gd2.md) · [mf02/02-mainflow-gd2.md](../mf02/02-mainflow-gd2.md)

---

## 6. Checklist smoke sau restart BE

```text
# 1. E2E teams
GET /api/v1/hackathons?q=seal-e2e-2026 → hackathonId
GET /api/v1/teams?hackathonId={id}&status=ACTIVE → 7 teams E2E-T*

# 2. Orphans registered
Login student.e2e.orphan1@fpt.edu.vn → GET /me/teams (chưa có hoặc empty team)

# 3. Chưa lottery
GET /me/teams (leader t01) → chưa trackId (trước lottery)

# 4. Lottery (coord)
PATCH /api/v1/hackathons/{id}/lottery  { "roundId": prelimRoundId }
GET /me/teams (leader) → có trackId

# 5. Config
app.seed.e2e.enabled=true trong application-dev.properties
```

---

## 7. Phụ lục — Mã lỗi & API surface

### Error codes (GĐ2)

| Mã | HTTP | Khi nào | Ma trận |
|----|------|---------|---------|
| `HACKATHON_NOT_ONGOING` | 422 | Tạo đội khi không ONGOING | T N1 |
| `REGISTRATION_CLOSED` | 422 | Sau registration_end | T N2 |
| `TEAM_NAME_DUPLICATE` | 409 | Tên trùng | T N3 |
| `USER_IN_ANOTHER_TEAM` | 409 | Đã có đội | T N4 |
| `TEAM_LOCKED` | 403 | Mời/sửa sau lock | M N1, L2 |
| `TEAM_NOT_LOCKED` | 422 | Lottery sớm | B N1 |
| `ROUND_ALREADY_ACTIVE` | 423 | Re-lottery | B N2 |
| `TEAM_MEMBER_FULL` | 409 | Quá 5 thành viên | M N2 |

### API surface (GĐ2)

| Method | Path | Actor |
|--------|------|-------|
| POST | `/api/v1/me/hackathons/{id}/register` | Student |
| POST | `/api/v1/teams` | Student |
| GET | `/api/v1/teams`, `/api/v1/me/teams` | All |
| PATCH | `/api/v1/teams/{id}/status`, `/approve` | Coordinator |
| POST | `/api/v1/teams/{id}/members/invite` | Leader |
| PATCH | `/api/v1/teams/{id}/members/{userId}` | Invitee |
| PATCH | `/api/v1/hackathons/{id}/lottery` | Coordinator |
| PATCH | `/api/v1/teams/{id}/rounds/{roundId}/track` | Coordinator |
| POST/GET | `/api/v1/teams/{id}/rounds/{roundId}/mentor` | Coordinator |

---

## Phụ lục — Map tài liệu

| Tài liệu | Nội dung |
|----------|----------|
| [gd1-full-test-matrix-and-seeds.md](gd1-full-test-matrix-and-seeds.md) | GĐ1 — prerequisite |
| [gd3-full-test-matrix-and-seeds.md](gd3-full-test-matrix-and-seeds.md) | GĐ3 sau activate |
| [fe-gd1-gd2-structure-and-fields.md](fe-gd1-gd2-structure-and-fields.md) | Track vs bảng, form field |
| [mf02/05-test-data-gd2-teams.md](../mf02/05-test-data-gd2-teams.md) | JSON mẫu teams |
| [postman-playbook-gd2-gd3-integration.md](postman-playbook-gd2-gd3-integration.md) | Integration GĐ2→GĐ3 |
| [dev-seed-guide.md](dev-seed-guide.md) | Orphan emails, config |

---

*Cập nhật: 2026-06-24 — ma trận GĐ2 + profile `seal-e2e-2026` (7 đội + 3 orphan).*
