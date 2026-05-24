# MF-01 GĐ1 — Test data (happy path)

**Mục đích:** JSON copy-paste, biến Postman, map seed dev theo mainflow GĐ1.

**Tài liệu liên quan:**

| Tài liệu | Vai trò |
|----------|---------|
| [03-mainflow-gd1.md](03-mainflow-gd1.md) | Luồng 7 bước |
| [04-quy-trinh-van-hanh.md](04-quy-trinh-van-hanh.md) | Runbook API, ràng buộc |
| [06-qa-uat.md](06-qa-uat.md) | Ma trận TC UAT |
| [02-functional-requirements.md](02-functional-requirements.md) | Spec FR |
| [Gd1SeedConstants.java](../../src/main/java/com/sealhackathon/api/config/seed/Gd1SeedConstants.java) | Slug / email seed |

---

## 1. Tiền đề

Xem [04-quy-trinh-van-hanh.md §0](04-quy-trinh-van-hanh.md#0-tiền-đề-khi-gọi-api) (base URL, auth, envelope).

Sau khi start app, tìm log:

```text
[Gd1DataSeeder] Seed MF-01 GĐ1 hoàn tất.
  Coordinator: id=1 email=coord@fpt.edu.vn
  Hackathons:
    - seal-gd1-incomplete (id=...) DRAFT — readiness FAIL
    - seal-gd1-ready (id=...) DRAFT — readiness PASS → PATCH ONGOING
    - seal-spring-2026 (id=...) ONGOING — prelim round id=... isActive=...
  Tracks (ready): id=... / id=...
  Users: judge1=..., guest=..., mentor=..., pending=...
```

Ghi các `id` vào bảng biến bên dưới.

### 1.1 Gợi ý ngày (Luồng A — greenfield)

Dùng ngày **tương lai** so với hôm nay (align seeder `computeDates()`):

| Field | Công thức (ví dụ) |
|-------|-------------------|
| `registrationStart` | Hôm nay |
| `registrationEnd` | Hôm nay + 13 ngày |
| `eventStart` | Hôm nay + 14 ngày |
| `eventEnd` | `eventStart` + 45 ngày |
| Round `submissionDeadline` | Hôm nay + 30 ngày (phải **> NOW**) |
| Event WORKSHOP | `eventStart` − 5 ngày, 20:00–21:30 |
| Event KICKOFF | `eventStart`, 14:00–17:00 |
| Event PRESENTATION | Hôm nay + 31 ngày, 06:00–19:00 |
| Event AWARDS | `eventEnd` − 5 ngày, 08:00–18:00 |

Thay `2026-05-20` trong JSON mẫu bằng ngày bạn tính.

### 1.2 Biến Postman / environment

| Biến | Lưu từ |
|------|--------|
| `baseUrl` | `http://localhost:8080` |
| `hackathonId` | Bước 1 POST hoặc GET list seed |
| `prelimRoundId` | Bước 2 POST round 1 |
| `finalRoundId` | Bước 2 POST round 2 |
| `track1Id`, `track2Id` | Bước 3 |
| `criterionId` | Bước 4 POST (lấy `data.id` item đầu) |
| `mentorId` | Seed `mentor@fpt.edu.vn` (log seeder) |
| `judge1Id`, `judge2Id` | Seed `judge1@fpt.edu.vn`, `judge2@fpt.edu.vn` |
| `mentorAssignmentId` | Bước 5 POST mentor |
| `judgeAssignmentId` | Bước 5 POST judge |
| `eventId` | Bước 6 POST event đầu |
| `invitationId` | Bước 5 POST temp-judges (nếu có) |

---

## 2. Hai luồng test

| Luồng | Khi dùng | Hackathon |
|-------|----------|-----------|
| **A — Greenfield** | Test đủ 7 bước từ đầu | Slug unique: `seal-test-20260520` (đổi mỗi lần) |
| **B — Seed** | Smoke GET + gate nhanh | `seal-gd1-ready`, `seal-gd1-incomplete`, `seal-spring-2026` |

**Luồng B — lấy ID:**

1. `GET {{baseUrl}}/api/v1/hackathons?q=seal-gd1-ready`
2. `GET {{baseUrl}}/api/v1/hackathons/{{hackathonId}}`
3. `GET {{baseUrl}}/api/v1/hackathons/{{hackathonId}}/rounds` → `prelimRoundId` (sequenceOrder=1), `finalRoundId` (isFinal=true)
4. `GET {{baseUrl}}/api/v1/rounds/{{prelimRoundId}}/tracks` → `track1Id`, `track2Id`

---

## 3. Luồng A — Checklist happy path (7 bước)

Đánh dấu `[x]` khi pass. Thứ tự **bắt buộc**: Round trước Track.

### Bước 1 — Hackathon

- [ ] **1.1** `POST /api/v1/hackathons` → **201**, `data.status` = `DRAFT`

```http
POST {{baseUrl}}/api/v1/hackathons
Content-Type: application/json
```

```json
{
  "name": "SEAL Test Happy Path",
  "slug": "seal-test-20260520",
  "season": "Spring",
  "year": 2026,
  "description": "Greenfield test GĐ1",
  "registrationStart": "2026-05-19",
  "registrationEnd": "2026-06-01",
  "eventStart": "2026-06-02",
  "eventEnd": "2026-07-17",
  "wildcardEnabled": true,
  "individualRankingEnabled": false
}
```

→ Lưu `hackathonId` = `data.id`.

**API phụ (cùng bước):**

- [ ] **1.2** `GET /api/v1/hackathons?status=DRAFT&q=seal-test` → thấy hackathon vừa tạo
- [ ] **1.3** `GET /api/v1/hackathons/{{hackathonId}}` → **200**
- [ ] **1.4** (tuỳ chọn) `PUT /api/v1/hackathons/{{hackathonId}}` — body giống POST, sửa `description`

---

### Bước 2 — Round (Sơ loại + Chung kết)

- [ ] **2.1** `POST /api/v1/hackathons/{{hackathonId}}/rounds` — Sơ loại → **201**

```json
{
  "name": "Vòng Sơ loại",
  "examAt": "2026-06-10T08:00:00",
  "isFinal": false,
  "roundType": "PRELIMINARY",
  "submissionOpen": "2026-06-02T06:00:00",
  "submissionDeadline": "2026-06-19T23:59:59",
  "codingDurationHours": 7,
  "lateSubmissionPolicy": "ALLOW_LATE_PENDING",
  "topNAdvance": 2,
  "minTeamsFinal": 6,
  "wildcardEnabled": true,
  "tiebreakRule": "PENALTY_SCORE"
}
```

→ Lưu `prelimRoundId` = `data.id`.

- [ ] **2.2** `POST /api/v1/hackathons/{{hackathonId}}/rounds` — Chung kết → **201**

```json
{
  "name": "Vòng Chung kết",
  "examAt": "2026-07-05T08:00:00",
  "isFinal": true,
  "roundType": "FINAL",
  "submissionDeadline": "2026-07-12T23:59:59",
  "lateSubmissionPolicy": "HARD_LOCK",
  "tiebreakRule": "PENALTY_SCORE"
}
```

→ Lưu `finalRoundId` = `data.id`.

**API phụ:**

- [ ] **2.3** `GET /api/v1/hackathons/{{hackathonId}}/rounds` → 2 round; prelim có `trackCount` = 0; final có `criteriaCount` / weight (sau Bước 4)
- [ ] **2.4** `GET /api/v1/rounds/{{prelimRoundId}}` → `isFinal` = false
- [ ] **2.5** `GET /api/v1/rounds/{{finalRoundId}}` → `isFinal` = true

---

### Bước 3 — Track (trong Round Sơ loại)

- [ ] **3.1** `POST /api/v1/rounds/{{prelimRoundId}}/tracks` — Track 1 → **201**

```json
{
  "name": "Track 1 — RAG Pipeline",
  "description": "Xây dựng hệ thống RAG",
  "topic": null,
  "maxTeams": 8,
  "maxTeamsPerGroup": 8,
  "minTeamSize": 3,
  "maxTeamSize": 5,
  "sequenceOrder": 1
}
```

→ Lưu `track1Id`.

- [ ] **3.2** `POST /api/v1/rounds/{{prelimRoundId}}/tracks` — Track 2

```json
{
  "name": "Track 2 — AI Agent",
  "description": "Thiết kế AI Agent",
  "topic": null,
  "maxTeams": 8,
  "maxTeamsPerGroup": 8,
  "minTeamSize": 3,
  "maxTeamSize": 5,
  "sequenceOrder": 2
}
```

→ Lưu `track2Id`.

**API phụ:**

- [ ] **3.3** `GET /api/v1/rounds/{{prelimRoundId}}/tracks` → 2 track, có `roundId`, `sequenceOrder`
- [ ] **3.4** `GET /api/v1/hackathons/{{hackathonId}}/tracks` → 2 track, cùng `roundId` = prelim
- [ ] **3.5** `GET /api/v1/tracks/{{track1Id}}` → `roundId` = prelim, `hackathonId` khớp

---

### Bước 4 — Criteria (XOR: track / round FINAL)

Tổng weight (không tính PENALTY) = **1.0** mỗi track và mỗi round FINAL.

- [ ] **4.1** `POST /api/v1/tracks/{{track1Id}}/criteria/batch` → **201**

```json
{
  "items": [
    { "name": "Domain Accuracy", "type": "TECHNICAL", "weight": 0.30, "maxScore": 10, "displayOrder": 1 },
    { "name": "Kiến trúc RAG", "type": "TECHNICAL", "weight": 0.30, "maxScore": 10, "displayOrder": 2 },
    { "name": "Ý tưởng & Thuyết trình", "type": "SOFT_SKILL", "weight": 0.15, "maxScore": 10, "displayOrder": 3 },
    { "name": "Thực thi & Sáng tạo", "type": "TECHNICAL", "weight": 0.15, "maxScore": 10, "displayOrder": 4 },
    { "name": "UX & Giao diện", "type": "SOFT_SKILL", "weight": 0.10, "maxScore": 10, "displayOrder": 5 }
  ]
}
```

- [ ] **4.2** `POST /api/v1/tracks/{{track2Id}}/criteria/batch` — cùng bộ weight (hoặc đổi tên)

- [ ] **4.3** `POST /api/v1/rounds/{{finalRoundId}}/criteria/batch` → **201**

```json
{
  "items": [
    { "name": "Xử lý & Truy xuất", "type": "TECHNICAL", "weight": 0.30, "maxScore": 10, "displayOrder": 1 },
    { "name": "Độ tin cậy", "type": "TECHNICAL", "weight": 0.20, "maxScore": 10, "displayOrder": 2 },
    { "name": "Tư duy Agent", "type": "TECHNICAL", "weight": 0.20, "maxScore": 10, "displayOrder": 3 },
    { "name": "Thực tế & Triển khai", "type": "TECHNICAL", "weight": 0.20, "maxScore": 10, "displayOrder": 4 },
    { "name": "Mở rộng & Scale", "type": "SOFT_SKILL", "weight": 0.10, "maxScore": 10, "displayOrder": 5 }
  ]
}
```

**API phụ:**

- [ ] **4.4** `GET /api/v1/tracks/{{track1Id}}/criteria/weight-summary` → `currentTotal` ≈ 1.0 (có thể `warnings` nếu lệch nhẹ)
- [ ] **4.5** `GET /api/v1/tracks/{{track1Id}}/criteria` → list 5 criterion
- [ ] **4.6** `GET /api/v1/rounds/{{finalRoundId}}/criteria/weight-summary` → total ≈ 1.0
- [ ] **4.7** `GET /api/v1/criteria/{{criterionId}}` — lấy `id` từ list Bước 4.5
- [ ] **4.8** (tuỳ chọn) `PUT /api/v1/criteria/{{criterionId}}` — sửa `description`, giữ weight

---

### Bước 5 — Nhân sự (GĐ1: không Judge Chung kết)

Dùng user seed (log `[Gd1DataSeeder]`) hoặc tạo judge tạm.

| Email seed | Vai trò |
|------------|---------|
| `coord@fpt.edu.vn` | Coordinator id=1 |
| `judge1@fpt.edu.vn` | Judge INTERNAL |
| `judge2@fpt.edu.vn` | Judge INTERNAL |
| `mentor@fpt.edu.vn` | Mentor |
| `guestjudge@gmail.com` | EXTERNAL (đã seed) |
| `pending.judge@fpt.edu.vn` | PENDING — test negative |

- [ ] **5.1** `POST /api/v1/mentor-assignments` → **201**

```json
{
  "mentorId": {{mentorId}},
  "trackId": {{track1Id}}
}
```

→ Lưu `mentorAssignmentId` = `data.id`.

- [ ] **5.2** `POST /api/v1/judge-assignments` → **201**

```json
{
  "judgeId": {{judge1Id}},
  "trackId": {{track1Id}},
  "assignmentType": "NORMAL"
}
```

→ Lưu `judgeAssignmentId`. **Không** gán cùng `judgeId` làm mentor track đó (CONFLICT).

- [ ] **5.3** (tuỳ chọn) `POST /api/v1/users/temp-judges` → **201**

```json
{
  "fullName": "Guest Judge Test",
  "email": "guest-test-{{timestamp}}@company.com",
  "institution": "Google Vietnam",
  "phone": "+84901234567",
  "hackathonId": {{hackathonId}}
}
```

Invitation **loại 3** (judge khách): TTL spec **3 ngày**; email gồm MK tạm + link accept — xem [mf02-invitations-spec.md](./mf02-invitations-spec.md).

**API phụ:**

- [ ] **5.4** `GET /api/v1/tracks/{{track1Id}}/mentors` → có mentor
- [ ] **5.5** `GET /api/v1/tracks/{{track1Id}}/judges` → có judge NORMAL
- [ ] **5.6** `GET /api/v1/users/temp-judges?hackathonId={{hackathonId}}`
- [ ] **5.7** `GET /api/v1/users/{{mentorId}}/track-assignments`
- [ ] **5.8** `GET /api/v1/users/{{judge1Id}}/round-assignments` — thường rỗng ở GĐ1
- [ ] **5.9** **Không test GĐ1:** `POST /judge-assignments` với `roundId` + `FINAL_EXTERNAL` → **422** `JUDGE_FINAL_AT_PHASE1`

---

### Bước 6 — Events (thứ tự bắt buộc)

Ít nhất một trong `location` hoặc `meetUrl` phải có.

- [ ] **6.1** WORKSHOP

```json
{
  "title": "Workshop: RAG & AI Agent Fundamentals",
  "type": "WORKSHOP",
  "description": "Buổi workshop trước khai mạc",
  "location": "Online (Teams)",
  "startsAt": "2026-05-28T20:00:00",
  "endsAt": "2026-05-28T21:30:00",
  "isPublic": true
}
```

- [ ] **6.2** KICKOFF (bắt buộc cho gate G5)

```json
{
  "title": "Lễ Khai mạc & Bốc thăm chia Track",
  "type": "KICKOFF",
  "location": "FPT HCM — Hội trường A",
  "startsAt": "2026-06-02T14:00:00",
  "endsAt": "2026-06-02T17:00:00",
  "isPublic": true
}
```

- [ ] **6.3** PRESENTATION

```json
{
  "title": "Ngày thi Sơ loại & Thuyết trình",
  "type": "PRESENTATION",
  "location": "FPT HCM — Hội trường B",
  "startsAt": "2026-06-20T06:00:00",
  "endsAt": "2026-06-20T19:00:00",
  "isPublic": true
}
```

- [ ] **6.4** AWARDS

```json
{
  "title": "Vòng Chung kết & Trao giải",
  "type": "AWARDS",
  "location": "FPT HCM — Hội trường A",
  "startsAt": "2026-07-12T08:00:00",
  "endsAt": "2026-07-12T18:00:00",
  "isPublic": true
}
```

Tất cả: `POST /api/v1/hackathons/{{hackathonId}}/events`

**API phụ:**

- [ ] **6.5** `GET /api/v1/hackathons/{{hackathonId}}/events` → 4 events đúng thứ tự thời gian
- [ ] **6.6** `GET /api/v1/events/{{eventId}}` → **200**

---

### Bước 7 — Readiness + ONGOING

- [ ] **7.1** `GET /api/v1/hackathons/{{hackathonId}}/readiness?target=ONGOING` → **200**, `data.ready` = **true**, `blockers` rỗng

Gate G1–G5 tóm tắt: có prelim + track; 1 FINAL; criteria weight=1 mọi track + FINAL; có KICKOFF.

- [ ] **7.2** `PATCH /api/v1/hackathons/{{hackathonId}}/status` → **200**

```json
{
  "status": "ONGOING"
}
```

- [ ] **7.3** `GET /api/v1/hackathons/{{hackathonId}}` → `data.status` = `ONGOING`

---

## 4. Luồng B — Test với seed có sẵn

### 4.1 Hackathon theo slug

| Slug | Status | Mục đích |
|------|--------|----------|
| `seal-gd1-incomplete` | DRAFT | `GET readiness` → **fail** (thiếu round/track) |
| `seal-gd1-ready` | DRAFT | `GET readiness` → **pass**; chỉ cần PATCH ONGOING |
| `seal-spring-2026` | ONGOING | Test GET read-only; PATCH ONGOING → lỗi state |

**Tra ID:** `GET /api/v1/hackathons?q=seal-gd1-ready` → lấy phần tử đầu `data.items[].id`.

### 4.2 Checklist API phụ (data đã có — slug `seal-gd1-ready`)

Sau khi có `hackathonId`, `prelimRoundId`, `track1Id`, `track2Id` từ GET:

| # | Method | Path | Kỳ vọng |
|---|--------|------|---------|
| B1 | GET | `/hackathons/{{hackathonId}}/rounds` | 2 rounds; prelim `trackCount` ≥ 2 |
| B2 | GET | `/rounds/{{prelimRoundId}}/tracks` | ≥ 2 tracks |
| B3 | GET | `/hackathons/{{hackathonId}}/tracks` | mỗi item có `roundId` |
| B4 | GET | `/tracks/{{track1Id}}/criteria` | 5 criteria |
| B5 | GET | `/tracks/{{track1Id}}/criteria/weight-summary` | total ≈ 1.0 |
| B6 | GET | `/rounds/{{finalRoundId}}/criteria` | 5 criteria FINAL |
| B7 | GET | `/tracks/{{track1Id}}/mentors` | ≥ 1 |
| B8 | GET | `/tracks/{{track1Id}}/judges` | ≥ 1 NORMAL |
| B9 | GET | `/hackathons/{{hackathonId}}/events` | có KICKOFF |
| B10 | GET | `/hackathons/{{hackathonId}}/readiness?target=ONGOING` | `ready: true` |
| B11 | PATCH | `/hackathons/{{hackathonId}}/status` `{ "status": "ONGOING" }` | **200** |

### 4.3 `seal-gd1-incomplete` — negative readiness

- [ ] `GET /api/v1/hackathons?q=seal-gd1-incomplete` → `hackathonId`
- [ ] `GET /api/v1/hackathons/{{hackathonId}}/readiness?target=ONGOING` → `ready: false`, `blockers` không rỗng (G1)

### 4.4 `seal-spring-2026` — đã ONGOING

- [ ] GET list rounds, tracks, events — **200**
- [ ] `PATCH .../status` `{ "status": "ONGOING" }` → lỗi state (đã ONGOING)

---

## 5. Bảng assert nhanh

| Bước | HTTP | Field kiểm tra |
|------|------|----------------|
| 1 | 201 | `data.status` = `DRAFT` |
| 2 | 201 ×2 | `data.isFinal` false / true |
| 3 | 201 ×2 | `data.roundId` = prelim |
| 4 | 201 | weight-summary total ≈ 1.0 |
| 5 | 201 | assignment có `trackId` |
| 6 | 201 ×4 | types đủ 4 loại |
| 7 | 200 | `readiness.ready` = true → PATCH → `status` = `ONGOING` |

---

## 6. Phụ lục

### 6.1 curl mẫu

```bash
# Tạo hackathon (Luồng A)
curl -s -X POST "http://localhost:8080/api/v1/hackathons" \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"SEAL Test\",\"slug\":\"seal-test-curl-1\",\"season\":\"Spring\",\"year\":2026,\"registrationStart\":\"2026-05-19\",\"registrationEnd\":\"2026-06-01\",\"eventStart\":\"2026-06-02\",\"eventEnd\":\"2026-07-17\",\"wildcardEnabled\":true,\"individualRankingEnabled\":false}"

# Readiness (Luồng B — thay HACKATHON_ID)
curl -s "http://localhost:8080/api/v1/hackathons/HACKATHON_ID/readiness?target=ONGOING"
```

### 6.2 Lỗi thường gặp

| Code | Nguyên nhân |
|------|-------------|
| `HACKATHON_DUPLICATE` | Trùng slug hoặc (name, season, year) |
| `ROUND_DEADLINE_INVALID` | `submissionDeadline` ≤ NOW |
| `ROUND_FINAL_SEQUENCE_ORDER` | FINAL `sequenceOrder` không lớn hơn prelim |
| `DESIGN_VIOLATION` | POST track vào round FINAL |
| `EVENT_ORDER_VIOLATION` | Thứ tự event sai (v3.1 block) |
| `EVENT_LOCATION_REQUIRED` | Thiếu cả `location` và `meetUrl` |
| `READINESS_NOT_PASSED` | PATCH ONGOING khi gate fail |
| `CONFLICT_SAME_TRACK` | Cùng user vừa mentor vừa judge một track |

### 6.3 FR-07B (ngoài happy GĐ1)

`PATCH /api/v1/rounds/{{prelimRoundId}}/activate` — dùng ở GĐ3 khi bắt đầu vòng thi; không bắt buộc để PATCH hackathon ONGOING.

### 6.4 Coverage API GĐ1 (checklist tổng)

| Module | Đã cover Luồng A/B |
|--------|---------------------|
| Hackathon CRUD + readiness + status | Bước 1, 7 |
| Round CRUD + list | Bước 2 |
| Track CRUD + list by round/hackathon | Bước 3 |
| Criteria batch/GET/weight-summary | Bước 4 |
| Mentor / Judge assignments | Bước 5 |
| Temp judges | Bước 5.3 (optional) |
| Events CRUD list | Bước 6 |
| Invitations resend | Chưa happy (cần invitation hết hạn) |
| Round activate | Phụ lục 6.3 |

---

*SEAL Hackathon BE — Test playbook GĐ1 — FPT University HCMC*
