# API Test Playbook — Giai đoạn 1 → 6

**Dành cho:** QA / Tester / Dev tích hợp  
**Phiên bản:** 2026-06-05 · **166** REST endpoint (theo Controller)  
**Base URL:** `http://localhost:8080/api/v1`  
**Swagger:** `http://localhost:8080/swagger-ui.html`

> Một file duy nhất: kịch bản E2E + catalog JSON đầy đủ. Bản tách `api-catalog-with-json.md` chỉ để tra cứu nhanh (cùng nội dung Phần III).

---

## Mục lục

| Phần | Nội dung |
|------|----------|
| [I. Hướng dẫn tester](#phần-i--hướng-dẫn-tester) | Postman, tài khoản, envelope, mẫu khối API |
| [II. E2E Happy path](#phần-ii--kịch-bản-e2e-happy-path) | Chạy tuần tự GĐ0→6 — JSON các bước chính |
| [II-B. Regression gate](#phần-ii-b--regression-gate-btc-3-tầng) | Test case negative + E2E full 6 GĐ — phát hiện code đá nhau |
| [III. Catalog 166 API](#phần-iii--catalog-api-requestresponse-json) | **Mọi endpoint** — request + response `data` |
| [IV. Checklist](#phần-iv--checklist-nhanh-166-api) | Đánh dấu đã test |

---

# Phần I — Hướng dẫn tester

## 1.1 Cách dùng tài liệu

1. **Lần đầu:** Đọc §1.2–1.4 → import biến Postman → chạy [Phần II](#phần-ii--kịch-bản-e2e-happy-path) từ trên xuống.
2. **Test lẻ một API:** Mở [Phần III](#phần-iii--catalog-api-requestresponse-json) → tìm theo GĐ hoặc `Ctrl+F` path (vd. `/submissions`).
3. **Mỗi khối API (Phần III)** theo mẫu:

| Trường | Ý nghĩa |
|--------|---------|
| `METHOD path` | Gọi đúng prefix `/api/v1` (trừ `GET /` health) |
| **Request** | Body JSON hoặc *(không body)* |
| **Response `data`** | Payload trong envelope 2xx |

4. **Role:** dùng token đúng biến (`coordToken`, `studentToken`, …) — xem [api-authorization-matrix.md](../api-authorization-matrix.md).

**Ký hiệu**

| Ký hiệu | Ý nghĩa |
|---------|---------|
| **Chính** | Bước bắt buộc trên happy path |
| **Hỗ trợ** | Tra cứu / preview / portal stub |
| **CRUD** | Sửa/xóa cấu hình |
| `{{biến}}` | Giá trị lấy từ response bước trước |

## 1.2 Chuẩn bị Postman

**Environment**

| Biến | Ví dụ | Ghi chú |
|------|-------|---------|
| `baseUrl` | `http://localhost:8080` | |
| `coordToken` | `eyJ...` | Sau `POST /auth/login` Coordinator |
| `studentToken` | `eyJ...` | Student APPROVED |
| `judgeToken` | `eyJ...` | Judge APPROVED |
| `mentorToken` | `eyJ...` | Mentor APPROVED |
| `hackathonId` | `2` | Log `[Gd1DataSeeder]` hoặc bước 1.1 |
| `prelimRoundId` | `3` | Round Sơ loại — bước 1.2 |
| `finalRoundId` | `4` | Round CK — bước 1.3 |
| `track1Id` | `5` | Track 1 — bước 1.4 |
| `track2Id` | `6` | (Tùy chọn) Track 2 |
| `mentorId` | `5` | `GET /users?q=mentor@fpt.edu.vn` |
| `judge1Id` | `2` | `GET /users?q=judge1@fpt.edu.vn` |
| `guestJudgeId` | `4` | `GET /users?q=guestjudge` hoặc bước 1.7 |
| `userId` | `42` | Sinh viên sau register / seed |
| `teamId` | `10` | Sau `POST /teams` |
| `memberUserId` | `43` | User được mời — accept bước 2.5 |
| `submissionId` | `7` | Sau POST submissions |
| `criterionId` | `1` | `data.items[0].id` sau criteria batch |
| `finalCriterionId` | `201` | criterion của round CK (bước 1.6) |
| `gd3HackathonSlug` | `seal-gd3-prelim-open` | Slug seed GĐ3 |
| `gd4HackathonSlug` | `seal-gd4-advance-ready` | Slug seed GĐ4 |
| `gd5HackathonSlug` | `seal-gd5-final-active` | Slug seed GĐ5 |
| `gd6HackathonSlug` | `seal-gd6-pending-confirm` | Slug seed GĐ6 |
| `lateSubmissionId` | `8` | Submission `LATE_PENDING` — log `[Gd3DataSeeder]` |
| `wildcardReviewId` | `1` | Từ `GET .../wildcard-candidates` (GĐ4) |
| `calibrationSessionId` | `1` | `GET /calibration-sessions?roundId=` (GĐ5) |

**Header mặc định (API protected)**

```http
Authorization: Bearer {{coordToken}}
Content-Type: application/json
```

## 1.3 Tài khoản dev (sau seed)

| Role | Email | Password |
|------|-------|----------|
| Coordinator | `coord@fpt.edu.vn` | `Coordinator@dev1` |
| Mentor | `mentor@fpt.edu.vn` | `Mentor@dev1` |
| Judge | `judge1@fpt.edu.vn` | `Judge@dev1` |
| Guest Judge (CK) | `guestjudge@gmail.com` | `GuestJudge@dev1` |
| Student (GĐ2) | `student.gd2.hcm.leader03@fpt.edu.vn` | `Student@dev1` |
| Student (GĐ3) | `student.gd3.leader01@fpt.edu.vn` … `leader06@` | `Student@dev1` |
| Student (GĐ4) | `student.gd4a.leader01@fpt.edu.vn` … `leader08@` | `Student@dev1` |
| Student (GĐ5) | `student.gd5.leader01@fpt.edu.vn` … `leader04@` | `Student@dev1` |
| Student (GĐ6) | `student.gd6.leader01@fpt.edu.vn` … `leader03@` | `Student@dev1` |

| Hackathon | Slug | Trạng thái | Dùng cho |
|-----------|------|------------|----------|
| SEAL Spring 2026 | `seal-spring-2026` | ONGOING | **GĐ2** nhanh (đội `GD2-*` đã seed) |
| GĐ1 sẵn sàng | `seal-gd1-ready` | DRAFT, đủ G1–G5 | Chỉ test bước 1.11–1.12 |
| GĐ1 thiếu | `seal-gd1-incomplete` | DRAFT, không round | Test readiness `ready=false` |
| **GĐ3 Sơ loại mở** | `seal-gd3-prelim-open` | ONGOING, prelim **active**, chưa lock | **GĐ3** E2E — nộp/chấm/late/calibration/presentation |
| **GĐ4 Advance ready** | `seal-gd4-advance-ready` | ONGOING, prelim **locked**, chưa publish | **GĐ4** E2E — ranking/wildcard/advance/activate CK |
| GĐ4 tiebreak (opt-in) | `seal-gd4-tiebreak-wildcard` | ONGOING | Tiebreak 3-way + wildcard — `app.seed.gd4.enabled=true` |
| **GĐ5 CK active** | `seal-gd5-final-active` | ONGOING, CK **active**, chưa lock CK | **GĐ5** E2E — nộp/chấm CK → `PENDING_CONFIRM` |
| GĐ6 sau lock CK | `seal-gd6-pending-confirm` | `PENDING_CONFIRM` | **GĐ6** — prizes/confirm (snapshot sau lock CK) |
| Archive mẫu | `seal-fall-2025-finished` | `FINISHED` | Tra cứu lịch sử / archive |

Lấy ID: `GET {{baseUrl}}/api/v1/hackathons?q=<slug>` → `data.content[0].id`, hoặc xem log khi start app (`spring.profiles.active=dev`):

| Log startup | Giai đoạn |
|-------------|-----------|
| `[Gd3DataSeeder]` | GĐ3 — `hackathonId`, `prelimRoundId`, `track1Id`, `track2Id`, team/submission IDs |
| `[Gd4AdvanceDataSeeder]` | GĐ4 — `prelimRoundId`, `finalRoundId`, 8 `teamId`, gợi ý advance |
| `[Gd5FinalRoundDataSeeder]` | GĐ5 — `finalRoundId`, `finalCriterionId`, `finalSubmissionId(t2)`, team t3 cho 5.1 |
| `[Gd6PendingConfirmDataSeeder]` | GĐ6 — hackathon đã `PENDING_CONFIRM` |

Chi tiết seed GĐ4/GĐ5: [gd4-gd5-e2e-seed-data.md](gd4-gd5-e2e-seed-data.md) · GĐ6: [gd6-e2e-seed-data.md](gd6-e2e-seed-data.md) · Audit coverage: [seed-coverage-audit.md](seed-coverage-audit.md).

**Công thức lịch round (Sơ loại, `codingDurationHours=7`):**

- `submissionOpen` = `examAt` + 4h40 (2/3 × 7h)
- `submissionDeadline` = `examAt` + 7h  
- Ví dụ `examAt=2026-06-10T08:00` → open `12:40`, deadline `15:00`
- **Chung kết:** `examAt` ≥ `examAt Sơ loại + codingDurationHours` → tối thiểu `2026-06-10T15:00`; `submissionDeadline` CK phải **trước** AWARDS (`17:30`)

## 1.4 Envelope response

Mọi API 2xx trả dạng:

```json
{
  "success": true,
  "data": { },
  "message": "Optional",
  "traceId": "uuid",
  "timestamp": "2026-05-29T10:00:00Z"
}
```

Lỗi 4xx/5xx:

```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_FAILED",
    "message": "Mô tả lỗi",
    "status": 400,
    "details": { }
  },
  "traceId": "...",
  "timestamp": "..."
}
```

> Trong Phần III, JSON mẫu thường chỉ mô tả **`data`** (bên trong envelope).

## 1.5 Điều kiện chuyển giai đoạn (3 gate BTC)

**Thứ tự POST event:** `KICKOFF` → `WORKSHOP` → `AWARDS` (AWARDS chỉ bắt buộc ở GĐ6, không block ONGOING). **Trên lịch:** `WORKSHOP` → `KICKOFF` → `AWARDS` (WS trước KO — khớp PDF Spring 2026).

| Gate | Chuyển GĐ | Điều kiện | API / Readiness |
|------|-----------|-----------|-----------------|
| **1** | GĐ1 → **GĐ2** | FR G1–G5 (có CK shell + criteria CK); **KICKOFF**; **không** cần AWARDS | `GET readiness?target=ONGOING` → `PATCH status ONGOING` |
| **2** | GĐ2 → **GĐ3** | Teams + lottery + `is_locked` (ngày **sau** `registrationEnd`) | `PATCH /rounds/{prelimId}/activate` |
| **3** | GĐ4 → **GĐ5** | Publish SL + advance + `FINAL_EXTERNAL` judge | `GET readiness?target=FINAL_ROUND` → `PATCH activate final` |
| — | GĐ5 → **GĐ6** | CK `scoring_locked=true` | `status = PENDING_CONFIRM` |
| — | GĐ6 kết thúc | Có **AWARDS** + prizes | `GET readiness?target=AWARDS` → `PATCH confirm` |

Chi tiết test case: [gate-regression-test-matrix-gd1-gd6.md](gate-regression-test-matrix-gd1-gd6.md) · FE: [fe-gd1-gd2-gd3-workflow-mapping.md](fe-gd1-gd2-gd3-workflow-mapping.md).

---

# Phần II — Kịch bản E2E (Happy path)

Chạy **theo thứ tự**. Đánh dấ `[x]` khi pass. Chi tiết API lẻ → [Phần III](#phần-iii--catalog-api-requestresponse-json).

## Bản đồ seed dev — test từng GĐ độc lập

Profile **`dev`**. Mỗi slug = hackathon riêng, idempotent mỗi lần start.

| GĐ | Slug | Log startup | Bước E2E bắt đầu | Bỏ qua (đã seed) |
|----|------|-------------|------------------|------------------|
| GĐ2 | `seal-spring-2026` | `[Gd2DataSeeder]` | §2.0 | Teams, lottery một phần |
| **GĐ3** | `seal-gd3-prelim-open` | `[Gd3DataSeeder]` | **3.0** | Activate, đề, 6 teams, scores draft, mentor, queue |
| **GĐ4** | `seal-gd4-advance-ready` | `[Gd4AdvanceDataSeeder]` | **4.0** | Prelim locked+scored, 8 teams, chưa publish |
| **GĐ4+** | `seal-gd4-tiebreak-wildcard` | `[Gd4TestDataSeeder]` | 4.x tiebreak | `app.seed.gd4.enabled=true` |
| **GĐ5** | `seal-gd5-final-active` | `[Gd5FinalRoundDataSeeder]` | **5.0** | CK active, 4 ADVANCED, guest judge CK |
| GĐ6 | `seal-gd6-pending-confirm` | `[Gd6PendingConfirmDataSeeder]` | §6.0 | Đã `PENDING_CONFIRM` sau lock CK |

> Greenfield (tạo hackathon mới GĐ1→6): xem [E2E một mạch](#e2e-một-mạch-6-gđ-greenfield) cuối Phần II-B.

## E2E — GĐ0 — Auth (trước mọi GĐ)

| # | API | Role | Header | Lưu biến Postman |
|---|-----|------|--------|-------------------|
| 0.1 | `POST /auth/login` | Public | — | `coordToken` ← `data.accessToken` |
| 0.2 | `POST /auth/login` | Public | — | `studentToken` |
| 0.3 | `GET /users/me` | STU | `Bearer {{studentToken}}` | Kiểm tra `data.status` |

> **Postman Tests (gợi ý):** tab Tests → `pm.environment.set("coordToken", pm.response.json().data.accessToken);`

### 0.1 Login Coordinator

```http
POST {{baseUrl}}/api/v1/auth/login
Content-Type: application/json
```

**Request**

```json
{
  "email": "coord@fpt.edu.vn",
  "password": "Coordinator@dev1"
}
```

**Response `200` — `data`**

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "base64url-refresh-token",
  "tokenType": "Bearer",
  "expiresInSeconds": 1800,
  "mustChangePassword": false
}
```

### 0.2 Login Student (seed GĐ2 hoặc sau register)

```http
POST {{baseUrl}}/api/v1/auth/login
Content-Type: application/json
```

**Request**

```json
{
  "email": "student.gd2.hcm.leader03@fpt.edu.vn",
  "password": "Student@dev1"
}
```

**Response `200` — `data`:** cùng shape như 0.1 → lưu `studentToken`.

### 0.3 GET /users/me (Student)

```http
GET {{baseUrl}}/api/v1/users/me
Authorization: Bearer {{studentToken}}
```

**Response `200` — `data` (rút gọn)**

```json
{
  "id": 42,
  "email": "student.gd2.hcm.leader03@fpt.edu.vn",
  "fullName": "GD2 Leader 03",
  "role": "STUDENT",
  "status": "APPROVED",
  "userType": "INTERNAL",
  "chapterId": 1
}
```

---

## E2E — GĐ1 — Chuẩn bị sự kiện

**Đầu ra:** `hackathons.status = ONGOING`  
**Header mọi bước (trừ khi ghi khác):** `Authorization: Bearer {{coordToken}}`

| # | Method | Path | Lưu biến |
|---|--------|------|----------|
| 1.1 | POST | `/hackathons` | `hackathonId` ← `data.id` |
| 1.2 | POST | `/hackathons/{{hackathonId}}/rounds` | `prelimRoundId` |
| 1.3 | POST | `/hackathons/{{hackathonId}}/rounds` | `finalRoundId` |
| 1.4 | POST | `/rounds/{{prelimRoundId}}/tracks` | `track1Id` |
| 1.5 | POST | `/tracks/{{track1Id}}/criteria/batch` | `criterionId` |
| 1.6 | POST | `/rounds/{{finalRoundId}}/criteria/batch` | — |
| 1.7 | POST | `/users/temp-judges` | `guestJudgeId` (nếu tạo mới) |
| 1.8 | POST | `/mentor-assignments` | — |
| 1.9 | POST | `/judge-assignments` | Chỉ **NORMAL/track** (FINAL_EXTERNAL → GĐ4) |
| 1.10a | POST | `/hackathons/{{hackathonId}}/events` | **KICKOFF** (bắt buộc readiness) |
| 1.10b | POST | `/hackathons/{{hackathonId}}/events` | WORKSHOP (sau KICKOFF) |
| 1.10c | POST | `/hackathons/{{hackathonId}}/events` | AWARDS — **GĐ6** (không blocker ONGOING) |
| 1.11 | GET | `/hackathons/{{hackathonId}}/readiness?target=ONGOING` | `data.ready === true` (không cần AWARDS) |
| 1.12 | PATCH | `/hackathons/{{hackathonId}}/status` | `data.status === "ONGOING"` |

**Lịch mẫu (khớp seed `Gd1DataSeeder` / `RoundScheduleSeedUtil`):** đăng ký 24/05–05/06 · **WS 06/06** · **KO 07/06** · thi 10/06 · AWARDS 10/06 (GĐ6). POST vẫn **KICKOFF trước WORKSHOP**.

**Thứ tự bắt buộc:** 1.2 Sơ loại **trước** 1.3 CK (tránh `ROUND_PRELIM_DEADLINE_AFTER_FINAL_EXAM` nếu tạo CK trước rồi chỉnh deadline Sơ loại).

### Đường tắt GĐ1 (đã seed `seal-gd1-ready`)

1. Chạy GĐ0.1 → lấy `coordToken`.
2. `GET {{baseUrl}}/api/v1/hackathons?q=seal-gd1-ready` → `hackathonId`.
3. Chỉ chạy **1.11** và **1.12** (đã có round/track/criteria/events/assignments).

---

### 1.1 Tạo Hackathon

```http
POST {{baseUrl}}/api/v1/hackathons
Authorization: Bearer {{coordToken}}
Content-Type: application/json
```

**Request** — đổi `slug` mỗi lần chạy (unique):

```json
{
  "name": "SEAL E2E Manual 2026",
  "slug": "seal-e2e-manual-2026",
  "season": "Spring",
  "year": 2026,
  "description": "Tạo từ playbook Postman — GĐ1",
  "registrationStart": "2026-05-24",
  "registrationEnd": "2026-06-05",
  "eventStart": "2026-06-10",
  "eventEnd": "2026-06-10",
  "wildcardEnabled": true,
  "individualRankingEnabled": false
}
```

**Response `201` — `data`**

```json
{
  "id": 12,
  "name": "SEAL E2E Manual 2026",
  "slug": "seal-e2e-manual-2026",
  "status": "DRAFT",
  "season": "Spring",
  "year": 2026,
  "registrationStart": "2026-05-24",
  "registrationEnd": "2026-06-05",
  "eventStart": "2026-06-10",
  "eventEnd": "2026-06-10"
}
```

---

### 1.2 Round Sơ loại

```http
POST {{baseUrl}}/api/v1/hackathons/{{hackathonId}}/rounds
Authorization: Bearer {{coordToken}}
Content-Type: application/json
```

**Request**

```json
{
  "name": "Vòng Sơ loại",
  "examAt": "2026-06-10T08:00:00",
  "isFinal": false,
  "roundType": "PRELIMINARY",
  "submissionOpen": "2026-06-10T12:40:00",
  "submissionDeadline": "2026-06-10T15:00:00",
  "codingDurationHours": 7,
  "lateSubmissionPolicy": "ALLOW_LATE_PENDING",
  "topNAdvance": 2,
  "minTeamsFinal": 6,
  "wildcardEnabled": true,
  "tiebreakRule": "PENALTY_SCORE"
}
```

**Response `201` — `data` (rút gọn)**

```json
{
  "id": 20,
  "hackathonId": 12,
  "name": "Vòng Sơ loại",
  "isFinal": false,
  "roundType": "PRELIMINARY",
  "examAt": "2026-06-10T08:00:00",
  "submissionOpen": "2026-06-10T12:40:00",
  "submissionDeadline": "2026-06-10T15:00:00",
  "codingDurationHours": 7
}
```

---

### 1.3 Round Chung kết

```http
POST {{baseUrl}}/api/v1/hackathons/{{hackathonId}}/rounds
Authorization: Bearer {{coordToken}}
Content-Type: application/json
```

**Request** — không gửi `topNAdvance` / `minTeamsFinal`; `lateSubmissionPolicy` phải `HARD_LOCK`.

> **`ROUND_FINAL_EXAM_ORDER`:** với Sơ loại `examAt=08:00` + `codingDurationHours=7`, CK **`examAt` ≥ `15:00`** (không dùng `13:00`).

```json
{
  "name": "Vòng Chung kết",
  "examAt": "2026-06-10T15:00:00",
  "isFinal": true,
  "roundType": "FINAL",
  "submissionOpen": "2026-06-10T15:30:00",
  "submissionDeadline": "2026-06-10T16:30:00",
  "lateSubmissionPolicy": "HARD_LOCK",
  "tiebreakRule": "PENALTY_SCORE"
}
```

**Response `201` — `data`:** `isFinal: true`, `roundType: "FINAL"`.

---

### 1.4 Track (Sơ loại)

```http
POST {{baseUrl}}/api/v1/rounds/{{prelimRoundId}}/tracks
Authorization: Bearer {{coordToken}}
Content-Type: application/json
```

**Request**

```json
{
  "name": "Track 1 — RAG Pipeline",
  "description": "E2E track — RAG",
  "topic": "RAG & Retrieval",
  "sequenceOrder": 1,
  "maxTeams": 8,
  "maxTeamsPerGroup": 8,
  "minTeamSize": 3,
  "maxTeamSize": 5
}
```

**Response `201` — `data`:** `id` → `track1Id`, `status: "OPEN"`.

*(Tùy chọn: lặp POST với `sequenceOrder: 2`, tên `Track 2 — AI Agent`.)*

---

### 1.5 Criteria batch (Track)

```http
POST {{baseUrl}}/api/v1/tracks/{{track1Id}}/criteria/batch
Authorization: Bearer {{coordToken}}
Content-Type: application/json
```

**Request** — tổng `weight` = **1.0**

```json
{
  "items": [
    { "name": "Domain Accuracy", "type": "TECHNICAL", "weight": 0.30, "maxScore": 10, "displayOrder": 1 },
    { "name": "Kiến trúc", "type": "TECHNICAL", "weight": 0.30, "maxScore": 10, "displayOrder": 2 },
    { "name": "Thuyết trình", "type": "SOFT_SKILL", "weight": 0.20, "maxScore": 10, "displayOrder": 3 },
    { "name": "Thực thi", "type": "TECHNICAL", "weight": 0.20, "maxScore": 10, "displayOrder": 4 }
  ]
}
```

**Response `201` — `data`**

```json
{
  "createdIds": [101, 102, 103, 104],
  "weightSummary": {
    "trackId": 25,
    "total": 1.0,
    "status": "OK"
  }
}
```

---

### 1.6 Criteria batch (Chung kết)

```http
POST {{baseUrl}}/api/v1/rounds/{{finalRoundId}}/criteria/batch
Authorization: Bearer {{coordToken}}
Content-Type: application/json
```

**Request**

```json
{
  "items": [
    {
      "name": "Tổng thể Chung kết",
      "type": "TECHNICAL",
      "weight": 1.0,
      "maxScore": 10,
      "displayOrder": 1
    }
  ]
}
```

---

### 1.7 Temp judge (khách — tùy chọn nếu đã có `guestjudge@gmail.com`)

```http
POST {{baseUrl}}/api/v1/users/temp-judges
Authorization: Bearer {{coordToken}}
Content-Type: application/json
```

**Request** — email **chưa** tồn tại:

```json
{
  "fullName": "Guest Judge E2E",
  "email": "guest.judge.e2e.manual@gmail.com",
  "institution": "Industry Partner",
  "phone": "0900000001",
  "hackathonId": "{{hackathonId}}"
}
```

**Response `201` — `data`:** `id`, `email`, `isTempAccount: true` → `guestJudgeId`.  
**Dev có sẵn:** `guestjudge@gmail.com` / `GuestJudge@dev1` — bỏ qua 1.7, dùng `GET /users?q=guestjudge`.

---

### 1.8 Gán Mentor → Track

```http
POST {{baseUrl}}/api/v1/mentor-assignments
Authorization: Bearer {{coordToken}}
Content-Type: application/json
```

**Request** — `mentorId`: user `mentor@fpt.edu.vn` (log seed, thường id≈5):

```json
{
  "mentorId": "{{mentorId}}",
  "trackId": "{{track1Id}}"
}
```

---

### 1.9 Gán Judge

**Sơ loại (NORMAL) — theo track:**

```http
POST {{baseUrl}}/api/v1/judge-assignments
Authorization: Bearer {{coordToken}}
Content-Type: application/json
```

```json
{
  "judgeId": "{{judge1Id}}",
  "trackId": "{{track1Id}}",
  "assignmentType": "NORMAL"
}
```

(`judge1Id` = user `judge1@fpt.edu.vn` sau seed.)

> **Judge Chung kết:** chỉ gán ở GĐ4 (sau publish/advance) — **không** `POST /judge-assignments` với `roundId` CK ở GĐ1 (`JUDGE_FINAL_AT_PHASE1`).

---

### 1.10 Events (POST: KICKOFF → WORKSHOP → AWARDS; lịch: WS → KO)

**1.10a KICKOFF** *(POST đầu tiên — blocker nếu thiếu; trên lịch sau WORKSHOP)*

```http
POST {{baseUrl}}/api/v1/hackathons/{{hackathonId}}/events
Authorization: Bearer {{coordToken}}
Content-Type: application/json
```

```json
{
  "title": "Lễ Khai mạc & Bốc thăm chia Track",
  "type": "KICKOFF",
  "location": "FPT HCM — Hội trường A",
  "startsAt": "2026-06-07T14:00:00",
  "endsAt": "2026-06-07T17:00:00",
  "isPublic": true
}
```

**1.10b WORKSHOP** *(POST sau KICKOFF; trên lịch **trước** KICKOFF, khác ngày)*

```json
{
  "title": "Workshop: RAG & AI Agent Fundamentals",
  "type": "WORKSHOP",
  "location": "Online (Teams)",
  "startsAt": "2026-06-06T20:00:00",
  "endsAt": "2026-06-06T21:30:00",
  "isPublic": true
}
```

**1.10c AWARDS** *(GĐ6 — dùng `GET .../readiness?target=AWARDS`)*

```json
{
  "title": "Vòng Chung kết & Trao giải",
  "type": "AWARDS",
  "location": "FPT HCM — Hội trường A",
  "startsAt": "2026-06-10T17:30:00",
  "endsAt": "2026-06-10T19:00:00",
  "isPublic": true
}
```

> ONGOING **không** yêu cầu AWARDS. Bước 1.10c có thể bỏ qua khi test GĐ1→GĐ2.

---

### 1.11 Readiness

```http
GET {{baseUrl}}/api/v1/hackathons/{{hackathonId}}/readiness?target=ONGOING
Authorization: Bearer {{coordToken}}
```

**Response `200` — kỳ vọng**

```json
{
  "ready": true,
  "targetStatus": "ONGOING",
  "blockers": [],
  "warnings": [],
  "summary": {
    "tracksCount": 1,
    "roundsCount": 2,
    "criteriaCount": 5,
    "eventsCount": 2
  }
}
```

Nếu `ready: false`, xem `blockers[].code` (vd. `EVENT_KICKOFF_MISSING`, `TRACK_CRITERIA_WEIGHT`, `ROUND_DEADLINE_INVALID`).

---

### 1.12 Chuyển ONGOING

```http
PATCH {{baseUrl}}/api/v1/hackathons/{{hackathonId}}/status
Authorization: Bearer {{coordToken}}
Content-Type: application/json
```

**Request**

```json
{
  "status": "ONGOING"
}
```

**Response `200` — `data.status`:** `"ONGOING"`.

---

## E2E — GĐ2 — Đăng ký & Đội

**Hackathon:** dùng hackathon vừa ONGOING ở GĐ1 **hoặc** seed `seal-spring-2026` (đã ONGOING + 9 đội `GD2-*`).

| # | API | Role | Header |
|---|-----|------|--------|
| 2.0 | `GET /hackathons?q=seal-spring-2026` | COORD | `coordToken` |
| 2.1 | `POST /auth/register` | Public | — |
| 2.1b | `PATCH /users/me` | STU (PENDING) | `studentToken` |
| 2.1c | `POST /users/me/student-card` | STU (PENDING) | **Bắt buộc** trước 2.2 |
| 2.2 | `PATCH /users/{userId}/status` | COORD | `coordToken` |
| 2.2a | `GET /users/{userId}/student-card` | COORD | Xem ảnh trước khi duyệt (tùy chọn) |
| 2.3 | `POST /teams` | STU APPROVED | `studentToken` |
| 2.4 | `POST /teams/{teamId}/members/invite` | Leader | `studentToken` |
| 2.5 | `PATCH /teams/{teamId}/members/{memberUserId}` | Member | token member |
| 2.6 | `PATCH /teams/{teamId}/approve` | COORD | `coordToken` |
| 2.6b | *(chờ cron hoặc SQL)* | — | `GET /teams/{id}` → `isLocked=true` |
| 2.7 | `PATCH /hackathons/{hackathonId}/lottery` | COORD | `coordToken` |

### Đường tắt GĐ2 (seed `seal-spring-2026`)

| Mục tiêu test | Tài khoản | API gợi ý |
|---------------|-----------|-----------|
| Duyệt đội 4 người | `coord@fpt.edu.vn` | `GET /teams?hackathonId=&status=PENDING` → team `GD2-03...` → **2.6** |
| Accept lời mời | `student.gd2.ext.pending@gmail.com` | **2.5** trên team `GD2-02...` |
| Bốc thăm đội 07 | Coordinator | **2.7** với `teamId` đội `GD2-07...` |
| Tạo đội mới | Leader seed / register | **2.3**–**2.7** full path bên dưới |

**2.0 Lấy ID hackathon ONGOING**

```http
GET {{baseUrl}}/api/v1/hackathons?q=seal-spring-2026&size=5
Authorization: Bearer {{coordToken}}
```

→ `hackathonId` = `data.content[0].id`  
→ `GET /hackathons/{{hackathonId}}/rounds` → `prelimRoundId` (round `isFinal=false`)  
→ `GET /rounds/{{prelimRoundId}}/tracks` → `track1Id`, `track2Id`

---

### 2.1 Đăng ký sinh viên mới

```http
POST {{baseUrl}}/api/v1/auth/register
Content-Type: application/json
```

**Request** — email unique mỗi lần:

```json
{
  "email": "student.e2e.manual01@gmail.com",
  "password": "Student@dev1",
  "confirmPassword": "Student@dev1"
}
```

**Response `201` — `data`:** `status: "PENDING"` → login → lưu `studentToken`, `userId` ← `data.userId` hoặc từ `/users/me`.

### 2.1b Hoàn thiện hồ sơ (PENDING → chờ duyệt)

```http
PATCH {{baseUrl}}/api/v1/users/me
Authorization: Bearer {{studentToken}}
Content-Type: application/json
```

```json
{
  "fullName": "SV E2E Manual 01",
  "userType": "INTERNAL",
  "studentCode": "SE226099",
  "chapterId": 1,
  "phone": "0901234999"
}
```

(`chapterId` 1 = FPT-HCM seed.)

---

### 2.1c Upload ảnh thẻ sinh viên (bắt buộc trước duyệt)

> Nếu bỏ qua bước này, Coordinator gọi **2.2** sẽ nhận `422`  
> `"Sinh viên phải upload ảnh thẻ sinh viên trước khi duyệt"`.

```http
POST {{baseUrl}}/api/v1/users/me/student-card
Authorization: Bearer {{studentToken}}
Content-Type: multipart/form-data
```

**Postman:** tab **Body** → **form-data**

| Key | Type | Value |
|-----|------|--------|
| `file` | **File** | Chọn ảnh `.jpg` / `.jpeg` / `.png` / `.webp` (≤ **5MB**) |

*(Không gửi JSON; field name phải đúng `file`.)*

**Response `200` — `data` (rút gọn)**

```json
{
  "id": 51,
  "email": "student.e2e.manual01@gmail.com",
  "status": "PENDING",
  "studentCode": "SE226099",
  "studentCardImagePath": "51/student-card-51-xxxxxxxx-xxxx.png"
}
```

**Kiểm tra (Student):**

```http
GET {{baseUrl}}/api/v1/users/me/student-card
Authorization: Bearer {{studentToken}}
```

→ `200`, body là file ảnh (binary).

---

### 2.2 Coordinator duyệt tài khoản

**Điều kiện trước khi duyệt (STUDENT, PENDING → APPROVED):**

| # | Điều kiện |
|---|-----------|
| 1 | `PATCH /users/me` — `userType`, `studentCode`, `chapterId` (INTERNAL) hoặc `institution` (EXTERNAL) |
| 2 | `POST /users/me/student-card` — `studentCardImagePath` khác null |

```http
PATCH {{baseUrl}}/api/v1/users/{{userId}}/status
Authorization: Bearer {{coordToken}}
Content-Type: application/json
```

**Request**

```json
{
  "status": "APPROVED"
}
```

**Response `200` — `data.status`:** `"APPROVED"`.

### 2.2a Coordinator xem ảnh thẻ (tùy chọn)

```http
GET {{baseUrl}}/api/v1/users/{{userId}}/student-card
Authorization: Bearer {{coordToken}}
```

→ `200` ảnh inline · `404` nếu chưa upload.

---

### 2.3 Tạo đội

```http
POST {{baseUrl}}/api/v1/teams
Authorization: Bearer {{studentToken}}
Content-Type: application/json
```

**Request**

```json
{
  "hackathonId": "{{hackathonId}}",
  "teamName": "E2E Manual Team 01"
}
```

**Response `201` — `data`**

```json
{
  "id": 50,
  "hackathonId": "{{hackathonId}}",
  "teamName": "E2E Manual Team 01",
  "leaderId": 42,
  "status": "PENDING",
  "isLocked": false
}
```

→ `teamId` = `data.id`. Cần **3–5** thành viên ACCEPTED trước khi duyệt ACTIVE.

---

### 2.4 Mời thành viên

```http
POST {{baseUrl}}/api/v1/teams/{{teamId}}/members/invite
Authorization: Bearer {{studentToken}}
Content-Type: application/json
```

**Request** — user chưa thuộc đội khác (seed: `student.gd2.pool.free@gmail.com`):

```json
{
  "email": "student.gd2.pool.free@gmail.com"
}
```

Lặp invite thêm 2–3 email APPROVED khác nếu cần đủ 3 người.

---

### 2.5 Accept lời mời

**`memberUserId` là gì?**

- Là **`users.id`** của **người được mời** (email trong bước 2.4), **không** phải `leaderId`, **không** phải id đội (`teamId`).
- Path: `PATCH /teams/{teamId}/members/{userId}` — `{userId}` **bắt buộc trùng** user đang đăng nhập (JWT). BE so sánh: `currentUserId == userId`.

**Vì sao `FORBIDDEN` — "Bạn chỉ có thể phản hồi lời mời của chính mình"?**

| Sai | Đúng |
|-----|------|
| Token **leader** (`leaderId=7`) gọi accept | Token **người được mời** (`pool.free`) |
| Path `/members/35` nhưng JWT là user **33** (`member21`) | Path `/members/33` khi login `member21` |
| Gõ nhầm id user khác (35) dù đã login đúng pool.free | `GET /users/me` → lấy `data.id` làm `memberUserId` |

**Quy trình Postman (ví dụ mời `student.gd2.pool.free@gmail.com`):**

1. Leader (bước 2.4) — `POST .../teams/22/members/invite` body `{ "email": "student.gd2.pool.free@gmail.com" }`.
2. **Đổi token** — `POST /auth/login` với **đúng email pool.free** (không dùng token leader / member21).
3. `GET /users/me` → lưu `memberUserId` = `data.id` (vd. `35` nếu pool.free có id 35).
4. `PATCH /teams/22/members/{{memberUserId}}` + `{ "action": "ACCEPT" }` với **Bearer token pool.free**.

**Tests tab (sau Login invitee):**

```javascript
const id = pm.response.json().data?.id; // hoặc từ GET /users/me
if (id) pm.environment.set("memberUserId", id);
```

```http
PATCH {{baseUrl}}/api/v1/teams/{{teamId}}/members/{{memberUserId}}
Authorization: Bearer {{memberToken}}
Content-Type: application/json
```

**Request**

```json
{
  "action": "ACCEPT"
}
```

**Response `200`:** cập nhật thành công (body `data` thường `null`).

**Kiểm tra:** `GET /teams/{{teamId}}` — member có `userId` = `memberUserId`, `status`: `"ACCEPTED"`.

---

### 2.6 Duyệt đội (Coordinator)

```http
PATCH {{baseUrl}}/api/v1/teams/{{teamId}}/approve
Authorization: Bearer {{coordToken}}
```

*(Không body.)* **Kỳ vọng `200`:** `data.status` = `"ACTIVE"` (đủ 3–5 ACCEPTED).

**Negative (seed):** duyệt `GD2-01 Chờ duyệt (1 người)` → `422` `TEAM_INVALID_MEMBER_COUNT`.

---

### 2.6b Khóa đội trước bốc thăm (FR-13A)

Lottery **chỉ** chấp nhận đội `status=ACTIVE` và **`is_locked=true`**.

**Timeline (không đảo lộn — khớp code hiện tại):**

| Giai đoạn | Điều kiện (`today` = ngày server) |
|-----------|-------------------------------------|
| Còn đăng ký / tạo đội / mời | `today` ≤ `registrationEnd` (`TeamServiceImpl`: chặn khi `today` **>** `registrationEnd`) |
| Cron khóa đội ACTIVE | `today` **>** `registrationEnd` (`registrationEnd.isBefore(today)`) |
| Bốc thăm | `is_locked=true` |

→ **Ngày cuối đăng ký** (`today == registrationEnd`, vd. 05/06): vẫn tạo đội/mời được, **chưa** khóa → lottery **đúng** báo `TEAM_NOT_LOCKED`. Khóa + bốc thăm từ **ngày hôm sau** (06/06).

| Cách test dev | Mô tả |
|---------------|--------|
| **Chờ ngày** | Sang ngày sau `registrationEnd`, cron ≤ 1 phút |
| **DRAFT** | Khi tạo hackathon (1.1), đặt `registrationEnd` **hôm qua** rồi mới PATCH ONGOING |
| **Seed** | `seal-spring-2026` — đội `GD2-04` / `GD2-05` đã `is_locked=true` |
| **SQL tạm** | `UPDATE teams SET is_locked=1, locked_at=NOW() WHERE id=22;` |

```http
GET {{baseUrl}}/api/v1/teams/{{teamId}}
Authorization: Bearer {{coordToken}}
```

→ `data.isLocked` phải **`true`** trước bước 2.7.

**Lỗi nếu chưa khóa:** `422` `TEAM_NOT_LOCKED`.

---

### 2.7 Bốc thăm Track

**Điều kiện:** đội **ACTIVE** + **`isLocked: true`** (xem **2.6b**) · round Sơ loại **chưa** `isActive` · hackathon **ONGOING**.

```http
PATCH {{baseUrl}}/api/v1/hackathons/{{hackathonId}}/lottery
Authorization: Bearer {{coordToken}}
Content-Type: application/json
```

**Request** — gán tay (thay ID thật sau seed):

```json
{
  "roundId": "{{prelimRoundId}}",
  "assignments": [
    {
      "teamId": "{{teamId}}",
      "trackId": "{{track1Id}}",
      "assignedGroup": "Bảng A"
    }
  ]
}
```

**Auto lottery** (BE chia ngẫu nhiên): gửi chỉ `roundId`, `assignments` = `null` hoặc `[]`:

```json
{
  "roundId": "{{prelimRoundId}}",
  "assignments": []
}
```

**Response `200` — `data` (rút gọn)**

```json
{
  "hackathonId": "{{hackathonId}}",
  "roundId": "{{prelimRoundId}}",
  "assignedCount": 1,
  "assignments": [
    {
      "teamId": 50,
      "trackId": 5,
      "assignedGroup": "Bảng A"
    }
  ]
}
```

---

## E2E — GĐ3 — Sơ loại

**Mục tiêu:** nhận bài, chấm, khóa điểm để có ranking chính thức cho GĐ4.  
**Header mặc định:** `Authorization: Bearer {{coordToken}}` (trừ bước student/judge).

| # | Method | API | Role | Kết quả mong muốn |
|---|--------|-----|------|-------------------|
| 3.0 | GET | `/hackathons?q=seal-gd3-prelim-open` | COORD | lấy `hackathonId`, rounds, tracks |
| 3.1 | PATCH | `/rounds/{{prelimRoundId}}/activate` | COORD | round prelim `isActive=true` |
| 3.2 | PATCH | `/rounds/{{prelimRoundId}}/release-problem` | COORD | có `problemStatementUrl` |
| 3.3 | POST | `/submissions` | STU | tạo/cập nhật `submissionId` |
| 3.4 | POST | `/scores` | JUD | có điểm theo criterion |
| 3.4b | GET | `/rounds/{{prelimRoundId}}/scoring-progress` | COORD | theo dõi tiến độ chấm |
| 3.5 | PATCH | `/rounds/{{prelimRoundId}}/lock-scoring` | COORD | `scoringLocked=true` |
| 3.6 | GET | `/rounds/{{prelimRoundId}}/ranking` | COORD | ranking chính thức |
| 3.7 | GET | `/me/mentor/rounds` | MENTOR | danh sách vòng (portal) |
| 3.8 | GET | `/me/mentor/rounds/{{prelimRoundId}}/assigned-teams` | MENTOR | đội + lịch thuyết trình |
| 3.9 | GET | `/me/rounds/current/deadline` | STU | countdown deadline |
| 3.10 | GET | `/me/submission?teamId=&roundId=` | STU | trạng thái bài nộp |
| 3.11 | GET | `/submissions?status=LATE_PENDING` | COORD | duyệt bài trễ |
| 3.12 | PATCH | `/submissions/{{lateSubmissionId}}/approve` | COORD | duyệt LATE_PENDING |
| 3.13 | GET | `/presentation/queue?roundId=` | ANY | hàng đợi thuyết trình |
| 3.14 | PATCH | `/presentation/queue/next?roundId=` | COORD | chuyển đội tiếp theo |

> Mapping FE ↔ BE: [`fe-gd3-api-mapping.md`](fe-gd3-api-mapping.md).

### Đường tắt GĐ3 (seed `seal-gd3-prelim-open`)

**Không cần** chạy GĐ1/GĐ2 trước. Start app profile `dev` → copy log `[Gd3DataSeeder]`.

| Thành phần seed | Giá trị |
|-----------------|---------|
| Hackathon | `ONGOING` |
| Sơ loại | **active**, đề đã phát, **`scoringLocked=false`**, chưa publish |
| Chung kết | chưa active |
| Teams | 6 đội, 2 track, đã lottery + mentor + presentation queue |

**Ma trận 6 đội — chọn bước test**

| Team seed | Student (login) | Submission | Scores | Test gợi ý |
|-----------|-----------------|------------|--------|------------|
| GD3-01 SUBMITTED + scored | `student.gd3.leader01@` | SUBMITTED | judge1+2, đủ (`isFinal=false`) | 3.10 · 3.4b progress |
| GD3-02 LATE_PENDING | `student.gd3.leader02@` | LATE_PENDING | chưa chấm | **3.11** · **3.12** approve |
| GD3-03 LATE_APPROVED | `student.gd3.leader03@` | LATE_APPROVED | judge1+2, đủ | Resubmit / score bổ sung |
| GD3-04 chưa nộp bài | `student.gd3.leader04@` | — | — | **3.3** POST submissions |
| GD3-05 Track2 SUBMITTED+scored | `student.gd3.leader05@` | SUBMITTED (track2) | judge1+2, đủ | Track2 scoring |
| GD3-06 Track2 chấm dở | `student.gd3.leader06@` | SUBMITTED (track2) | judge1, **2/4** criteria | **3.4** bổ sung điểm |

**Luồng tối thiểu:** 0.1 coord → **3.0** lấy ID → login `leader04` **3.3** → login `judge1` **3.4** (team 06) → **3.4b** → login `mentor` **3.7–3.8** → **3.11–3.12** (`lateSubmissionId` team 02) → **3.13–3.14** queue → **3.5–3.6** lock + ranking.

> **Scoring-progress:** prelim chưa lock → BE đếm scores `isFinal=false`. Seed GĐ3 đặt đúng flag này.

**3.0 Lấy ID**

```http
GET {{baseUrl}}/api/v1/hackathons?q=seal-gd3-prelim-open&size=5
Authorization: Bearer {{coordToken}}
```

→ `hackathonId` · `GET .../rounds` → `prelimRoundId` · `GET /rounds/{{prelimRoundId}}/tracks` → `track1Id`, `track2Id`

### 3.1 Activate round Sơ loại

```http
PATCH {{baseUrl}}/api/v1/rounds/{{prelimRoundId}}/activate
Authorization: Bearer {{coordToken}}
Content-Type: application/json
```

**Request** *(body optional)*:

```json
{
  "note": "Start prelim round for E2E"
}
```

**Response `200` — `data` (rút gọn):**

```json
{
  "id": "{{prelimRoundId}}",
  "isActive": true,
  "activatedAt": "2026-06-05T10:20:00"
}
```

### 3.2 Phát đề

```http
PATCH {{baseUrl}}/api/v1/rounds/{{prelimRoundId}}/release-problem
Authorization: Bearer {{coordToken}}
Content-Type: application/json
```

**Request**

```json
{
  "problemStatementUrl": "https://example.com/debai-so-loai.pdf"
}
```

### 3.3 Nộp bài (Student)

```http
POST {{baseUrl}}/api/v1/submissions
Authorization: Bearer {{studentToken}}
Content-Type: application/json
```

**Request** *(prelim: bắt buộc `trackId`, không cần `roundId`)*:

```json
{
  "teamId": "{{teamId}}",
  "trackId": "{{track1Id}}",
  "repoUrl": "https://github.com/org/repo",
  "demoUrl": "https://demo.example.com",
  "reportUrl": "https://docs.example.com/report",
  "slideUrl": "https://slides.example.com/deck"// file
}
```

**Response `201` — `data` (rút gọn):**

```json
{
  "id": 7,
  "teamId": "{{teamId}}",
  "trackId": "{{track1Id}}",
  "status": "SUBMITTED"
}
```

→ lưu `submissionId`.

### 3.4 Chấm điểm (Judge)

```http
POST {{baseUrl}}/api/v1/scores
Authorization: Bearer {{judgeToken}}
Content-Type: application/json
```

**Request**

```json
{
  "submissionId": "{{submissionId}}",
  "criterionId": "{{criterionId}}",
  "scoreValue": 8.5,
  "comment": "Good demo",
  "scoreType": "NORMAL"
}
```

(`scoreValue` range: 0..100 theo DTO.)

### 3.4b Xem tiến độ chấm

```http
GET {{baseUrl}}/api/v1/rounds/{{prelimRoundId}}/scoring-progress
Authorization: Bearer {{coordToken}}
```

### 3.5 Khóa chấm

```http
PATCH {{baseUrl}}/api/v1/rounds/{{prelimRoundId}}/lock-scoring
Authorization: Bearer {{coordToken}}
Content-Type: application/json
```

**Request**

```json
{
  "force": false,
  "reason": null
}
```

**Response `200` — `data.scoringLocked`:** `true`.

### 3.6 Ranking chính thức

```http
GET {{baseUrl}}/api/v1/rounds/{{prelimRoundId}}/ranking
Authorization: Bearer {{coordToken}}
```

### 3.7 Mentor — danh sách vòng

```http
GET {{baseUrl}}/api/v1/me/mentor/rounds
Authorization: Bearer {{mentorToken}}
```

### 3.8 Mentor — đội được phân công

```http
GET {{baseUrl}}/api/v1/me/mentor/rounds/{{prelimRoundId}}/assigned-teams
Authorization: Bearer {{mentorToken}}
```

### 3.9 Student — deadline countdown

```http
GET {{baseUrl}}/api/v1/me/rounds/current/deadline
Authorization: Bearer {{studentToken}}
```

**Response `200` — `data`:**

```json
{
  "roundId": "{{prelimRoundId}}",
  "deadline": "2026-06-07T15:00:00"
}
```

### 3.10 Student — bài nộp hiện tại

```http
GET {{baseUrl}}/api/v1/me/submission?teamId={{teamId}}&roundId={{prelimRoundId}}
Authorization: Bearer {{studentToken}}
```

### 3.11 Coordinator — danh sách LATE_PENDING

```http
GET {{baseUrl}}/api/v1/submissions?status=LATE_PENDING
Authorization: Bearer {{coordToken}}
```

### 3.12 Coordinator — duyệt bài trễ

```http
PATCH {{baseUrl}}/api/v1/submissions/{{lateSubmissionId}}/approve
Authorization: Bearer {{coordToken}}
```

*(Hoặc `PATCH .../reject` body `{ "reason": "Nộp quá hạn không lý do" }`.)*

### 3.13 Presentation queue

```http
GET {{baseUrl}}/api/v1/presentation/queue?roundId={{prelimRoundId}}
Authorization: Bearer {{coordToken}}
```

### 3.14 Chuyển đội tiếp theo

```http
PATCH {{baseUrl}}/api/v1/presentation/queue/next?roundId={{prelimRoundId}}
Authorization: Bearer {{coordToken}}
Content-Type: application/json
```

**Request** *(optional)*:

```json
{
  "currentTeamId": "{{teamId}}"
}
```

---

## E2E — GĐ4 — Chuyển vòng

**Mục tiêu:** publish kết quả Sơ loại, chốt ADVANCE/ELIMINATE, phân judge CK và activate CK.

| # | Method | API | Ghi chú |
|---|--------|-----|---------|
| 4.0 | GET | `/hackathons?q=seal-gd4-advance-ready` | lấy ID từ seed GĐ4 |
| 4.1 | GET | `/rounds/{{prelimRoundId}}/ranking` | sau lock-scoring |
| 4.2 | GET | `/rounds/{{prelimRoundId}}/wildcard-candidates` | nếu cần wildcard |
| 4.2b | PATCH | `/wildcard-reviews/{id}` | duyệt/reject từng wildcard |
| 4.3 | PATCH | `/rounds/{{prelimRoundId}}/publish` | công bố kết quả Sơ loại |
| 4.4 | POST | `/rounds/{{prelimRoundId}}/advance` | chốt danh sách vào CK |
| 4.5 | POST | `/rounds/{{finalRoundId}}/judge-assignments` | **GĐ4** — phân Judge CK (không dùng `/judge-assignments`) |
| 4.5b | GET | `/hackathons/{{hackathonId}}/readiness?target=FINAL_ROUND` | `ready: true` trước activate CK |
| 4.6 | PATCH | `/rounds/{{finalRoundId}}/activate` | kích hoạt round CK (Gate 3) |

### Đường tắt GĐ4 (seed `seal-gd4-advance-ready`)

**Không dùng** `seal-spring-2026`. Start app `dev` → log `[Gd4AdvanceDataSeeder]`.

| Thành phần seed | Giá trị |
|-----------------|---------|
| Hackathon | `ONGOING` |
| Sơ loại | `scoringLocked=true`, **`isPublished=false`**, `topNAdvance=1`, `minTeamsFinal=6` |
| Chung kết | chưa active, chưa advance |
| Teams | 8 đội, 4 bảng, điểm `isFinal=true` |

**Ma trận 8 đội** (log `t01=… t08=…`):

| Team seed | Bảng | Vai trò |
|-----------|------|---------|
| GD4-A01 Rank1 Bảng A | A | Top 1 → advance |
| GD4-A02 Rank2 Bảng A | A | Wildcard |
| GD4-A03 Rank1 Bảng B | B | Top 1 → advance |
| GD4-A04 Rank2 Bảng B | B | Eliminate |
| GD4-A05 Rank1 Bảng C | C | Top 1 → advance |
| GD4-A06 Rank2 Bảng C | C | Wildcard |
| GD4-A07 Rank1 Bảng D | D | Top 1 → advance |
| GD4-A08 Rank2 Bảng D | D | Eliminate |

Student: `student.gd4a.leader01@` … `leader08@` / pwd `Student@dev1`

**Luồng:** **4.0** lấy ID → **4.1** ranking (8 dòng) → **4.2** wildcard (**2** candidate) → **4.2b** approve → **4.3** publish → **4.4** advance (ID từ log) → **4.5** judge (409 duplicate → bỏ qua) → **4.5b** readiness → **4.6** activate CK.

Tiebreak nâng cao: `seal-gd4-tiebreak-wildcard` + `app.seed.gd4.enabled=true`.

### 4.2b Duyệt wildcard (nếu có)

```http
PATCH {{baseUrl}}/api/v1/wildcard-reviews/{{wildcardReviewId}}
Authorization: Bearer {{coordToken}}
Content-Type: application/json
```

```json
{
  "approved": true,
  "coordinatorNote": "Wildcard approved by committee"
}
```

### 4.3 Publish Sơ loại

```http
PATCH {{baseUrl}}/api/v1/rounds/{{prelimRoundId}}/publish
Authorization: Bearer {{coordToken}}
```

*(Không body.)*

### 4.4 Advance teams

```http
POST {{baseUrl}}/api/v1/rounds/{{prelimRoundId}}/advance
Authorization: Bearer {{coordToken}}
Content-Type: application/json
```

**Request** *(khớp DTO hiện tại — thay bằng `teamId` từ log `[Gd4AdvanceDataSeeder]`)*:

```json
{
  "advancedTeamIds": ["{{t01}}", "{{t03}}", "{{t05}}", "{{t07}}", "{{t02}}", "{{t06}}"],
  "eliminatedTeamIds": ["{{t04}}", "{{t08}}"],
  "note": "Advance based on official ranking + wildcard"
}
```

> Ví dụ số cứng `[10,11,12…]` chỉ minh họa DTO — ID thực phụ thuộc DB, luôn lấy từ log startup.

### 4.5 Judge Chung kết (GĐ4)

> **`POST /api/v1/judge-assignments`** với `roundId` + `FINAL_EXTERNAL` **luôn trả 422** `JUDGE_FINAL_AT_PHASE1` — đó là guard GĐ1. Ở GĐ4 dùng endpoint round-scoped bên dưới.

```http
POST {{baseUrl}}/api/v1/rounds/{{finalRoundId}}/judge-assignments
Authorization: Bearer {{coordToken}}
Content-Type: application/json
```

```json
{
  "judgeIds": [{{guestJudgeId}}]
}
```

**Response `200` — envelope (warnings chỉ ở top-level, không lặp trong `data`):**

```json
{
  "success": true,
  "data": {
    "roundId": 16,
    "judgeIds": [3]
  },
  "warnings": [
    {
      "code": "MIN_FINAL_JUDGES_NOT_MET",
      "message": "Panel Chung kết có 2 judge — khuyến nghị tối thiểu 3 trước activate"
    }
  ]
}
```

Nếu judge đã tham gia chấm Sơ loại → `warnings` có `JUDGE_PARTICIPATED_IN_PRELIM` (vẫn 200).  
Nếu đã gán trước (seed structure) → **409** `JUDGE_ASSIGN_DUPLICATE` → bỏ qua, chuyển **4.5b**.

*(Sơ loại vẫn dùng `POST /api/v1/judge-assignments` với `trackId` + `assignmentType=NORMAL` ở GĐ1.)*

### 4.5b Readiness FINAL_ROUND (dry-run Gate 3)

```http
GET {{baseUrl}}/api/v1/hackathons/{{hackathonId}}/readiness?target=FINAL_ROUND
Authorization: Bearer {{coordToken}}
```

**Kỳ vọng:** `ready: true` khi đã có judge `FINAL_EXTERNAL` + đội trong CK (`advance` xong). Nếu `ready: false` — xem `blockers` (thiếu judge / thiếu đội CK).

### 4.6 Activate round Chung kết

```http
PATCH {{baseUrl}}/api/v1/rounds/{{finalRoundId}}/activate
Authorization: Bearer {{coordToken}}
Content-Type: application/json
```

```json
{
  "note": "Start final round"
}
```

---

## E2E — GĐ5 — Chung kết

**Mục tiêu:** các đội ADVANCED nộp/chấm Chung kết, khóa điểm CK để chuyển `PENDING_CONFIRM`.

| # | Method | API | Role | Kết quả mong muốn |
|---|--------|-----|------|-------------------|
| 5.0 | GET | `/hackathons?q=seal-gd5-final-active` | COORD | lấy ID từ seed GĐ5 |
| 5.1 | POST | `/submissions` | STU | nộp bài CK (`roundId={{finalRoundId}}`, không `trackId`) |
| 5.2 | POST | `/scores` | JUD | chấm điểm CK |
| 5.2b | POST | `/scores/calibration` | JUD | (tùy chọn) chấm calibration |
| 5.3 | PATCH | `/rounds/{{finalRoundId}}/lock-scoring` | COORD | `scoringLocked=true` |
| 5.4 | GET | `/hackathons/{{hackathonId}}` | COORD | kiểm tra `status=PENDING_CONFIRM` |

### Đường tắt GĐ5 (seed `seal-gd5-final-active`)

Hackathon **riêng** — không cần GĐ4. Log `[Gd5FinalRoundDataSeeder]`.

| Thành phần seed | Giá trị |
|-----------------|---------|
| Hackathon | `ONGOING` (chưa `PENDING_CONFIRM`) |
| Sơ loại | published + locked |
| Chung kết | **active**, chưa `scoringLocked` |
| Guest judge | `FINAL_EXTERNAL` trên CK |

| Team seed | Student | CK submission | Bước test |
|-----------|---------|---------------|-----------|
| GD5-01 CK SUBMITTED + scored | `student.gd5.leader01@` | Có, đã chấm | Baseline |
| GD5-02 CK SUBMITTED chưa chấm | `student.gd5.leader02@` | Có | **5.2** (`finalSubmissionId(t2)`) |
| GD5-03 ADVANCED chưa nộp CK | `student.gd5.leader03@` | — | **5.1** (`teamId` t3) |
| GD5-04 ADVANCED (dự phòng) | `student.gd5.leader04@` | — | Dự phòng |

**Luồng:** 0.1 coord + login `guestjudge@gmail.com` → **5.0** lấy ID + `finalCriterionId` → login `leader03` **5.1** → guest judge **5.2** → *(tùy chọn)* **5.2b** → **5.3** lock → **5.4** `PENDING_CONFIRM`.

Snapshot GĐ6: slug `seal-gd6-pending-confirm`.

### 5.1 Nộp bài Chung kết

```http
POST {{baseUrl}}/api/v1/submissions
Authorization: Bearer {{studentToken}}
Content-Type: application/json
```

**Request** *(CK: dùng `roundId`, không gửi `trackId`)*:

```json
{
  "teamId": "{{teamId}}",
  "roundId": "{{finalRoundId}}",
  "repoUrl": "https://github.com/org/final-repo",
  "demoUrl": "https://demo.example.com/final",
  "reportUrl": "https://docs.example.com/final-report",
  "slideUrl": "https://slides.example.com/final"
}
```

**Response `201` — `data` (rút gọn):**

```json
{
  "id": 71,
  "teamId": "{{teamId}}",
  "roundId": "{{finalRoundId}}",
  "trackId": null,
  "status": "SUBMITTED"
}
```

### 5.2 Chấm điểm Chung kết

```http
POST {{baseUrl}}/api/v1/scores
Authorization: Bearer {{judgeToken}}
Content-Type: application/json
```

```json
{
  "submissionId": "{{submissionId}}",
  "criterionId": "{{finalCriterionId}}",
  "scoreValue": 9.0,
  "comment": "Strong final demo",
  "scoreType": "NORMAL"
}
```

### 5.2b Calibration (tùy chọn)

```http
POST {{baseUrl}}/api/v1/scores/calibration
Authorization: Bearer {{judgeToken}}
Content-Type: application/json
```

```json
{
  "submissionId": "{{submissionId}}",
  "criterionId": "{{finalCriterionId}}",
  "scoreValue": 8.8,
  "calibrationSessionId": 1,
  "comment": "Calibration sample"
}
```

### 5.3 Khóa chấm Chung kết

```http
PATCH {{baseUrl}}/api/v1/rounds/{{finalRoundId}}/lock-scoring
Authorization: Bearer {{coordToken}}
Content-Type: application/json
```

```json
{
  "force": false,
  "reason": null
}
```

**Response `200` — `data.scoringLocked`:** `true`.

### 5.4 Kiểm tra hackathon chuyển PENDING_CONFIRM

```http
GET {{baseUrl}}/api/v1/hackathons/{{hackathonId}}
Authorization: Bearer {{coordToken}}
```

**Kỳ vọng:** `data.status = "PENDING_CONFIRM"`.

---

## E2E — GĐ6 — Kết thúc

**Mục tiêu:** công bố bảng xếp hạng cuối, trao giải, confirm trạng thái FINISHED và xuất báo cáo.

| # | Method | API | Kết quả mong muốn |
|---|--------|-----|-------------------|
| 6.0a | POST | `/hackathons/{{hackathonId}}/events` | **AWARDS** (nếu chưa có từ GĐ1) |
| 6.0b | GET | `/hackathons/{{hackathonId}}/readiness?target=AWARDS` | `ready: true` |
| 6.1 | GET | `/hackathons/{{hackathonId}}/team-rankings` | bảng xếp hạng team CK |
| 6.1b | GET | `/hackathons/{{hackathonId}}/chapter-rankings` | bảng xếp hạng chapter |
| 6.1c | GET | `/hackathons/{{hackathonId}}/individual-rankings` | bảng cá nhân (nếu bật) |
| 6.2 | POST | `/hackathons/{{hackathonId}}/prizes` | tạo bản ghi trao giải |
| 6.2b | GET | `/hackathons/{{hackathonId}}/prizes` | kiểm tra danh sách giải |
| 6.3 | PATCH | `/hackathons/{{hackathonId}}/confirm` | `status=FINISHED` |
| 6.4 | POST | `/hackathons/{{hackathonId}}/export-jobs` | tạo export job |
| 6.5 | GET | `/export-jobs/{id}` | theo dõi trạng thái export |
| 6.6 | GET | `/export-jobs/{id}/download` | lấy URL file export |

### Đường tắt GĐ6 (seed `seal-gd6-pending-confirm`)

Log startup: **`[Gd6PendingConfirmDataSeeder]`** — chi tiết: [gd6-e2e-seed-data.md](gd6-e2e-seed-data.md).

| Thành phần seed | Giá trị |
|-----------------|---------|
| Hackathon | **`PENDING_CONFIRM`** |
| Sơ loại | published + locked |
| Chung kết | active + **scoring locked** |
| Events | KICKOFF + WORKSHOP + **AWARDS** |
| Prizes | **FIRST** trên team 01 — test **6.2** POST **SECOND** cho team 02 |

| Team seed | Student | Bước test |
|-----------|---------|-----------|
| GD6-01 ADVANCED CK | `student.gd6.leader01@` | Đã có giải Nhất |
| GD6-02 ADVANCED CK | `student.gd6.leader02@` | **6.2** trao giải Nhì |
| GD6-03 ADVANCED CK | `student.gd6.leader03@` | Dự phòng |

**Luồng:** login coord → **6.0b** readiness AWARDS → **6.2** prize SECOND (team 02) → **6.3** confirm → **6.4** export.

### 6.0a Tạo AWARDS (khi test greenfield bỏ bước 1.10c)

Phải đã có **KICKOFF** và **WORKSHOP** trước. JSON giống [§1.10c](#110c-awards-gđ6--dùng-get-readinesstargetawards).

### 6.0b Readiness AWARDS

```http
GET {{baseUrl}}/api/v1/hackathons/{{hackathonId}}/readiness?target=AWARDS
Authorization: Bearer {{coordToken}}
```

**Kỳ vọng:** `ready: true`, không blocker `EVENT_AWARDS_MISSING`.

### 6.1 Team rankings

```http
GET {{baseUrl}}/api/v1/hackathons/{{hackathonId}}/team-rankings
Authorization: Bearer {{coordToken}}
```

### 6.2 Trao giải

```http
POST {{baseUrl}}/api/v1/hackathons/{{hackathonId}}/prizes
Authorization: Bearer {{coordToken}}
Content-Type: application/json
```

```json
{
  "roundId": "{{finalRoundId}}",
  "teamId": "{{teamId}}",
  "prizeName": "Giải Nhất",
  "prizeRank": "FIRST",
  "prizeValue": "7000000 VND",
  "description": "Champion of SEAL Hackathon"
}
```

**Response `201` — `data` (rút gọn):**

```json
{
  "id": 5,
  "hackathonId": "{{hackathonId}}",
  "roundId": "{{finalRoundId}}",
  "teamId": "{{teamId}}",
  "prizeName": "Giải Nhất",
  "prizeRank": "FIRST"
}
```

### 6.3 Confirm FINISHED

```http
PATCH {{baseUrl}}/api/v1/hackathons/{{hackathonId}}/confirm
Authorization: Bearer {{coordToken}}
Content-Type: application/json
```

```json
{
  "confirm": true,
  "note": "Committee confirmed final results"
}
```

**Kỳ vọng:** `data.status = "FINISHED"`.

### 6.4 Tạo export job

```http
POST {{baseUrl}}/api/v1/hackathons/{{hackathonId}}/export-jobs
Authorization: Bearer {{coordToken}}
Content-Type: application/json
```

```json
{
  "type": "CSV_RANKINGS"
}
```

**Response `202` — `data` (rút gọn):**

```json
{
  "id": 12,
  "hackathonId": "{{hackathonId}}",
  "type": "CSV_RANKINGS",
  "status": "PENDING"
}
```

→ lưu `exportJobId`.

### 6.5 Poll trạng thái export

```http
GET {{baseUrl}}/api/v1/export-jobs/{{exportJobId}}
Authorization: Bearer {{coordToken}}
```

### 6.6 Lấy link tải file export

```http
GET {{baseUrl}}/api/v1/export-jobs/{{exportJobId}}/download
Authorization: Bearer {{coordToken}}
```

---

# Phần II-B — Regression gate (BTC 3 tầng)

Chạy **sau** happy path hoặc song song từng GĐ để bắt conflict code. Ma trận đầy đủ (ID, seed, negative): **[gate-regression-test-matrix-gd1-gd6.md](gate-regression-test-matrix-gd1-gd6.md)**.

## Checklist nhanh (đánh dấ `[x]`)

### GĐ1 — Events & Gate 1

- [ ] **G1-E01** KICKOFF → WORKSHOP (khác ngày) — 201
- [ ] **G1-E02** `readiness?target=ONGOING` **không** AWARDS → `ready: true`
- [ ] **G1-N01** WORKSHOP trước KICKOFF → 422
- [ ] **G1-N02** AWARDS trước WORKSHOP → 422
- [ ] **G1-N05** Judge `FINAL_EXTERNAL` ở GĐ1 → `JUDGE_FINAL_AT_PHASE1`
- [ ] **G1-N06** DELETE KICKOFF khi còn WORKSHOP → 422

### GĐ2 → GĐ3 — Gate 2

- [ ] **G2-N02** Lottery khi chưa `is_locked` → 422
- [ ] **G3-H01** Activate prelim sau lottery → 200
- [ ] **G3-T01** PUT `examAt` + `codingDurationHours` → window tự tính
- [ ] **G3-SEED** `seal-gd3-prelim-open` — scoring-progress > 0 (scores `isFinal=false`)
- [ ] **G3-SEED** LATE_PENDING team 02 → approve → chấm được
- [ ] **G3-SEED** Presentation queue next — 6 slots

### GĐ4 → GĐ5 — Gate 3

- [ ] **G4-R01** `readiness?target=FINAL_ROUND` sau advance+judge → `ready: true`
- [ ] **G4-N01** Activate final chưa publish → `RESULT_NOT_PUBLISHED`
- [ ] **G4-SEED** `seal-gd4-advance-ready` — wildcard-candidates = 2 → advance 6 đội
- [ ] **G5-H04** Lock CK → `PENDING_CONFIRM`
- [ ] **G5-SEED** `seal-gd5-final-active` — leader03 nộp CK, guest judge chấm t2, lock → PENDING_CONFIRM

### GĐ6 — AWARDS & kết thúc

- [ ] **G6-H01** POST AWARDS (sau WORKSHOP)
- [ ] **G6-R01** `readiness?target=AWARDS` → `ready: true`
- [ ] **G6-H03** Confirm → `FINISHED`

## E2E một mạch 6 GĐ (greenfield)

```text
GĐ1: 1.1→1.12 (1.10a KO → 1.10b WS, BỎ 1.10c) → ONGOING
GĐ2: teams → (ngày sau regEnd) lock → lottery
GĐ3: activate prelim → submit/score → lock-scoring prelim
GĐ4: publish → advance → judge FINAL → FINAL_ROUND ready → activate final
GĐ5: submit/score CK → lock-scoring → PENDING_CONFIRM
GĐ6: AWARDS → AWARDS ready → prizes → confirm → FINISHED
```

## E2E từng GĐ bằng seed (khuyến nghị QA lặp nhanh)

```text
GĐ3: slug seal-gd3-prelim-open     → 3.0 → 3.3–3.14 → 3.5–3.6
GĐ4: slug seal-gd4-advance-ready  → 4.0 → 4.1–4.6
GĐ5: slug seal-gd5-final-active    → 5.0 → 5.1–5.4
GĐ6: slug seal-gd6-pending-confirm → 6.0 → 6.1–6.3
```

SQL verify: [gd4-gd5-e2e-seed-data.md](gd4-gd5-e2e-seed-data.md#sql-verify-nhanh).

---

# Phần III — Catalog API (request/response JSON)

> **166 endpoint** — mỗi khối gồm Method + Path, Request JSON, Response `data`.  
> Envelope 2xx: `{ success, data, message?, traceId, timestamp }`.  
> Tìm nhanh: `Ctrl+F` path (vd. `POST /api/v1/submissions`) hoặc mã `3.012`.

## GĐ0 — System & Health

### 0.001 `GET /`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---


## GĐ0 — Auth & Users

### 0.001 `POST /api/v1/auth/change-password`

**Request:**
```json
{
  "oldPassword": "password12",
  "newPassword": "NewPass@123",
  "confirmPassword": "NewPass@123"
}
```

**Response `data`:** `null`

---

### 0.002 `POST /api/v1/auth/forgot-password`

**Request:**
```json
{
  "email": "user@fpt.edu.vn"
}
```

**Response `data`:** `null`

---

### 0.003 `POST /api/v1/auth/login`

**Request:**
```json
{
  "email": "coord@fpt.edu.vn",
  "password": "Coordinator@dev1"
}
```

**Response `data`:**
```json
{
  "accessToken": "eyJ...",
  "refreshToken": "...",
  "tokenType": "Bearer",
  "expiresInSeconds": 1800,
  "mustChangePassword": false
}
```

---

### 0.004 `POST /api/v1/auth/logout`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

### 0.005 `POST /api/v1/auth/logout-all`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

### 0.006 `POST /api/v1/auth/oauth/github/code`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

### 0.007 `POST /api/v1/auth/oauth/github/link/code`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

### 0.008 `POST /api/v1/auth/oauth/github/unlink`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

### 0.009 `POST /api/v1/auth/oauth/google`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

### 0.010 `POST /api/v1/auth/oauth/google/link`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

### 0.011 `POST /api/v1/auth/oauth/google/unlink`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

### 0.012 `POST /api/v1/auth/refresh`

**Request:**
```json
{
  "refreshToken": "{{refreshToken}}"
}
```

**Response `data`:**
```json
{
  "accessToken": "eyJ...",
  "refreshToken": "new-refresh...",
  "expiresInSeconds": 1800
}
```

---

### 0.013 `POST /api/v1/auth/register`

**Request:**
```json
{
  "email": "sv@fpt.edu.vn",
  "password": "password12",
  "confirmPassword": "password12"
}
```

**Response `data`:**
```json
{
  "userId": 42,
  "email": "sv@fpt.edu.vn",
  "status": "PENDING",
  "message": "Đăng ký thành công."
}
```

---

### 0.014 `POST /api/v1/auth/reset-password`

**Request:**
```json
{
  "token": "reset-token-from-email",
  "newPassword": "NewPass@123",
  "confirmPassword": "NewPass@123"
}
```

**Response `data`:** `null`

---

### 0.015 `POST /api/v1/invitations/{id}/resend`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

### 0.016 `GET /api/v1/users`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---

### 0.017 `POST /api/v1/users`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

### 0.018 `GET /api/v1/users/me`

**Request:** *(không body)*

**Response `data`:**
```json
{
  "id": 42,
  "email": "sv@fpt.edu.vn",
  "fullName": "Nguyen Van A",
  "role": "STUDENT",
  "status": "APPROVED"
}
```

---

### 0.019 `PATCH /api/v1/users/me`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

### 0.020 `POST /api/v1/users/me/student-card`

**Role:** Student (đã login, thường `PENDING`)

**Request:** `multipart/form-data` — field **`file`** (ảnh jpg/jpeg/png/webp, ≤ 5MB)

**Postman:** Body → form-data → `file` = File

**Response `200` — `data` (rút gọn):**
```json
{
  "id": 51,
  "status": "PENDING",
  "studentCardImagePath": "51/student-card-51-uuid.png"
}
```

**Lỗi thường gặp:**

| Tình huống | `error.code` |
|------------|----------------|
| Không chọn file | `VALIDATION_FAILED` |
| > 5MB / sai định dạng | `VALIDATION_FAILED` |

---

### 0.021 `GET /api/v1/users/me/student-card`

**Role:** Student — tải ảnh đã upload

**Request:** *(không body)*

**Response:** `200` binary (ảnh) · `404` chưa có ảnh

---

### 0.022 `GET /api/v1/users/temp-judges`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---

### 0.023 `POST /api/v1/users/temp-judges`

**Request:**
```json
{
  "email": "guest.judge@company.com",
  "fullName": "Guest Judge",
  "organization": "ACME"
}
```

**Response `data`:**
```json
{
  "userId": 8,
  "email": "guest.judge@company.com",
  "invitationId": 3
}
```

---

### 0.024 `GET /api/v1/users/{judgeId}/round-assignments`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---

### 0.025 `GET /api/v1/users/{mentorId}/track-assignments`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---

### 0.026 `GET /api/v1/users/{userId}`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---

### 0.027 `PATCH /api/v1/users/{userId}`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

### 0.028 `PATCH /api/v1/users/{userId}/status`

**Request:**
```json
{
  "status": "APPROVED"
}
```

**Response `data`:**
```json
{
  "id": 42,
  "status": "APPROVED"
}
```

---

### 0.029 `GET /api/v1/users/{userId}/student-card`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---


## GĐ1 — Chuẩn bị sự kiện

### 1.001 `DELETE /api/v1/criteria/{id}`

**Request:** *(không body)*

**Response `data`:** `null`

---

### 1.002 `GET /api/v1/criteria/{id}`

**Request:** *(không body)*

**Response `data`:**
```json
{
  "id": 1,
  "name": "..."
}
```

---

### 1.003 `PUT /api/v1/criteria/{id}`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

### 1.004 `DELETE /api/v1/events/{id}`

**Request:** *(không body)*

**Response `data`:** `null`

---

### 1.005 `GET /api/v1/events/{id}`

**Request:** *(không body)*

**Response `data`:**
```json
{
  "id": 1,
  "name": "..."
}
```

---

### 1.006 `PUT /api/v1/events/{id}`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

### 1.007 `GET /api/v1/hackathons`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---

### 1.008 `POST /api/v1/hackathons`

**Request:**
```json
{
  "name": "SEAL Test",
  "slug": "seal-test-2026",
  "season": "Spring",
  "year": 2026,
  "registrationStart": "2026-05-01",
  "registrationEnd": "2026-06-01",
  "eventStart": "2026-06-02",
  "eventEnd": "2026-07-17",
  "wildcardEnabled": true,
  "individualRankingEnabled": false
}
```

**Response `data`:**
```json
{
  "id": 1,
  "name": "SEAL Test",
  "slug": "seal-test-2026",
  "status": "DRAFT",
  "season": "Spring",
  "year": 2026
}
```

---

### 1.009 `GET /api/v1/hackathons/active`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---

### 1.010 `GET /api/v1/hackathons/{hackathonId}/events`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---

### 1.011 `POST /api/v1/hackathons/{hackathonId}/events`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

### 1.012 `PATCH /api/v1/hackathons/{hackathonId}/lottery`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

### 1.013 `GET /api/v1/hackathons/{hackathonId}/tracks`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---

### 1.014 `DELETE /api/v1/hackathons/{id}`

**Request:** *(không body)*

**Response `data`:** `null`

---

### 1.015 `GET /api/v1/hackathons/{id}`

**Request:** *(không body)*

**Response `data`:**
```json
{
  "id": 1,
  "name": "..."
}
```

---

### 1.016 `PUT /api/v1/hackathons/{id}`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

### 1.017 `GET /api/v1/hackathons/{id}/chapter-rankings`

**Request:** *(không body)*

**Response `data`:**
```json
{
  "id": 1,
  "name": "..."
}
```

---

### 1.018 `GET /api/v1/hackathons/{id}/individual-rankings`

**Request:** *(không body)*

**Response `data`:**
```json
{
  "id": 1,
  "name": "..."
}
```

---

### 1.019 `GET /api/v1/hackathons/{id}/readiness`

**Request:** *(không body)*

**Response `data`:**
```json
{
  "id": 1,
  "name": "..."
}
```

---

### 1.020 `PATCH /api/v1/hackathons/{id}/status`

**Request:**
```json
{
  "status": "ONGOING"
}
```

**Response `data`:**
```json
{
  "id": 1,
  "status": "ONGOING"
}
```

---

### 1.021 `POST /api/v1/judge-assignments`

**Request:**
```json
{
  "judgeId": 3,
  "trackId": 5,
  "assignmentType": "NORMAL"
}
```

**Response `data`:**
```json
{
  "id": 20,
  "judgeId": 3,
  "trackId": 5,
  "assignmentType": "NORMAL"
}
```

---

### 1.022 `DELETE /api/v1/judge-assignments/{id}`

**Request:** *(không body)*

**Response `data`:** `null`

---

### 1.023 `POST /api/v1/mentor-assignments`

**Request:**
```json
{
  "mentorId": 4,
  "trackId": 5
}
```

**Response `data`:**
```json
{
  "id": 15,
  "mentorId": 4,
  "trackId": 5
}
```

---

### 1.024 `DELETE /api/v1/mentor-assignments/{id}`

**Request:** *(không body)*

**Response `data`:** `null`

---

### 1.025 `DELETE /api/v1/tracks/{id}`

**Request:** *(không body)*

**Response `data`:** `null`

---

### 1.026 `GET /api/v1/tracks/{id}`

**Request:** *(không body)*

**Response `data`:**
```json
{
  "id": 1,
  "name": "..."
}
```

---

### 1.027 `PUT /api/v1/tracks/{id}`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

### 1.028 `GET /api/v1/tracks/{trackId}/criteria`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---

### 1.029 `POST /api/v1/tracks/{trackId}/criteria`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

### 1.030 `POST /api/v1/tracks/{trackId}/criteria/batch`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

### 1.031 `POST /api/v1/tracks/{trackId}/criteria/clone`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

### 1.032 `GET /api/v1/tracks/{trackId}/criteria/clone-sources`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---

### 1.033 `GET /api/v1/tracks/{trackId}/criteria/weight-summary`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---

### 1.034 `GET /api/v1/tracks/{trackId}/judges`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---

### 1.035 `GET /api/v1/tracks/{trackId}/mentors`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---


## GĐ2 — Đăng ký & Đội

### 2.001 `GET /api/v1/teams`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---

### 2.002 `POST /api/v1/teams`

**Request:**
```json
{
  "hackathonId": 1,
  "teamName": "Team Alpha"
}
```

**Response `data`:**
```json
{
  "id": 10,
  "teamName": "Team Alpha",
  "status": "PENDING",
  "hackathonId": 1
}
```

---

### 2.003 `POST /api/v1/teams/bulk-approve`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

### 2.004 `DELETE /api/v1/teams/{teamId}`

**Request:** *(không body)*

**Response `data`:** `null`

---

### 2.005 `GET /api/v1/teams/{teamId}`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---

### 2.006 `PATCH /api/v1/teams/{teamId}/approve`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

### 2.007 `PATCH /api/v1/teams/{teamId}/eliminate`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

### 2.008 `GET /api/v1/teams/{teamId}/journey`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---

### 2.009 `POST /api/v1/teams/{teamId}/members/invite`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

### 2.010 `DELETE /api/v1/teams/{teamId}/members/{userId}`

**Request:** *(không body)*

**Response `data`:** `null`

---

### 2.011 `PATCH /api/v1/teams/{teamId}/members/{userId}`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

### 2.012 `GET /api/v1/teams/{teamId}/mentors`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---

### 2.013 `DELETE /api/v1/teams/{teamId}/rounds/{roundId}/mentor`

**Request:** *(không body)*

**Response `data`:** `null`

---

### 2.014 `POST /api/v1/teams/{teamId}/rounds/{roundId}/mentor`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

### 2.015 `PATCH /api/v1/teams/{teamId}/rounds/{roundId}/track`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

### 2.016 `PATCH /api/v1/teams/{teamId}/status`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

### 2.017 `PATCH /api/v1/teams/{teamId}/transfer-leader`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---


## GĐ3/GĐ5 — Thi, nộp bài & Chấm

### 3.001 `GET /api/v1/calibration-sessions`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---

### 3.002 `POST /api/v1/calibration-sessions`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

### 3.003 `PATCH /api/v1/calibration-sessions/{id}`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---


## GĐ3/GĐ5 — Vòng thi (rounds)

### 3.001 `GET /api/v1/hackathons/{hackathonId}/rounds`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---

### 3.002 `POST /api/v1/hackathons/{hackathonId}/rounds`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

### 3.003 `DELETE /api/v1/rounds/{id}`

**Request:** *(không body)*

**Response `data`:** `null`

---

### 3.004 `GET /api/v1/rounds/{id}`

**Request:** *(không body)*

**Response `data`:**
```json
{
  "id": 1,
  "name": "..."
}
```

---

### 3.005 `PUT /api/v1/rounds/{id}`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

### 3.006 `PATCH /api/v1/rounds/{id}/activate`

**Request:**
```json
{
  "note": "Kích hoạt Sơ loại"
}
```

**Response `data`:**
```json
{
  "id": 3,
  "isActive": true
}
```

---

### 3.007 `POST /api/v1/rounds/{id}/judge-assignments`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

### 3.008 `PATCH /api/v1/rounds/{id}/lock-scoring`

**Request:**
```json
{
  "force": false
}
```

**Response `data`:**
```json
{
  "id": 3,
  "scoringLocked": true
}
```

---

### 3.009 `GET /api/v1/rounds/{id}/ranking`

**Request:** *(không body)*

**Response `data`:**
```json
{
  "id": 1,
  "name": "..."
}
```

---

### 3.010 `GET /api/v1/rounds/{id}/ranking/preview`

**Request:** *(không body)*

**Response `data`:**
```json
{
  "id": 1,
  "name": "..."
}
```

---


## GĐ3/GĐ5 — Thi, nộp bài & Chấm

### 3.001 `GET /api/v1/rounds/{id}/rbl/progress`

**Request:** *(không body)*

**Response `data`:**
```json
{
  "id": 1,
  "name": "..."
}
```

---

### 3.002 `GET /api/v1/rounds/{id}/rbl/variance`

**Request:** *(không body)*

**Response `data`:**
```json
{
  "id": 1,
  "name": "..."
}
```

---


## GĐ3/GĐ5 — Vòng thi (rounds)

### 3.001 `PATCH /api/v1/rounds/{id}/release-problem`

**Request:**
```json
{
  "problemStatementUrl": "https://example.com/de.pdf"
}
```

**Response `data`:**
```json
{
  "id": 3,
  "problemReleasedAt": "2026-05-29T07:00:00"
}
```

---

### 3.002 `GET /api/v1/rounds/{id}/scoreboard`

**Request:** *(không body)*

**Response `data`:**
```json
{
  "id": 1,
  "name": "..."
}
```

---

### 3.003 `GET /api/v1/rounds/{id}/scoring-progress`

**Request:** *(không body)*

**Response `data`:**
```json
{
  "id": 1,
  "name": "..."
}
```

---

### 3.004 `GET /api/v1/rounds/{roundId}/criteria`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---

### 3.005 `POST /api/v1/rounds/{roundId}/criteria`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

### 3.006 `POST /api/v1/rounds/{roundId}/criteria/batch`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

### 3.007 `POST /api/v1/rounds/{roundId}/criteria/clone`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

### 3.008 `GET /api/v1/rounds/{roundId}/criteria/weight-summary`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---

### 3.009 `GET /api/v1/rounds/{roundId}/judges`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---

### 3.010 `GET /api/v1/rounds/{roundId}/tracks`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---

### 3.011 `POST /api/v1/rounds/{roundId}/tracks`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---


## GĐ3/GĐ5 — Thi, nộp bài & Chấm

### 3.001 `POST /api/v1/scores`

**Request:**
```json
{
  "submissionId": 7,
  "criterionId": 1,
  "scoreValue": 8.5,
  "scoreType": "NORMAL",
  "comment": "Good"
}
```

**Response `data`:**
```json
{
  "id": 100,
  "submissionId": 7,
  "criterionId": 1,
  "scoreValue": 8.5
}
```

---

### 3.002 `POST /api/v1/scores/calibration`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

### 3.003 `GET /api/v1/submissions`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---

### 3.004 `POST /api/v1/submissions`

**Request:**
```json
{
  "teamId": 10,
  "trackId": 5,
  "repoUrl": "https://github.com/o/r",
  "demoUrl": "https://d.example.com",
  "slideUrl": "https://s.example.com"
}
```

**Response `data`:**
```json
{
  "id": 7,
  "teamId": 10,
  "trackId": 5,
  "status": "SUBMITTED"
}
```

---

### 3.005 `PATCH /api/v1/submissions/{id}/resubmit`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

### 3.006 `PATCH /api/v1/submissions/{id}/review`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

### 3.007 `PATCH /api/v1/submissions/{id}/review-late`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---


## GĐ4 — Chuyển vòng & Publish

### 4.001 `POST /api/v1/rounds/{id}/advance`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

### 4.002 `POST /api/v1/rounds/{id}/advance-teams`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

### 4.003 `PATCH /api/v1/rounds/{id}/publish`

**Request:**
```json
{
  "confirm": true
}
```

**Response `data`:**
```json
{
  "id": 3,
  "isPublished": true
}
```

---

### 4.004 `GET /api/v1/rounds/{id}/tiebreak`

**Request:** *(không body)*

**Response `data`:**
```json
{
  "id": 1,
  "name": "..."
}
```

---

### 4.005 `POST /api/v1/rounds/{id}/tiebreak/resolve`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

### 4.006 `GET /api/v1/rounds/{id}/wildcard-candidates`

**Request:** *(không body)*

**Response `data`:**
```json
{
  "id": 1,
  "name": "..."
}
```

---

### 4.007 `POST /api/v1/rounds/{id}/wildcard/approve`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

### 4.008 `GET /api/v1/rounds/{id}/wildcard/candidates`

**Request:** *(không body)*

**Response `data`:**
```json
{
  "id": 1,
  "name": "..."
}
```

---

### 4.009 `POST /api/v1/rounds/{id}/wildcard/reject`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

### 4.010 `PATCH /api/v1/wildcard-reviews/{id}`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---


## GĐ6 — Kết thúc & Trao giải

### 6.001 `GET /api/v1/export-jobs/{id}`

**Request:** *(không body)*

**Response `data`:**
```json
{
  "id": 1,
  "name": "..."
}
```

---

### 6.002 `GET /api/v1/export-jobs/{id}/download`

**Request:** *(không body)*

**Response `data`:**
```json
{
  "id": 1,
  "name": "..."
}
```

---

### 6.003 `GET /api/v1/hackathons/{hackathonId}/prizes`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---

### 6.004 `POST /api/v1/hackathons/{hackathonId}/prizes`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

### 6.005 `PATCH /api/v1/hackathons/{id}/confirm`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

### 6.006 `POST /api/v1/hackathons/{id}/export-jobs`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

### 6.007 `GET /api/v1/hackathons/{id}/team-rankings`

**Request:** *(không body)*

**Response `data`:**
```json
{
  "id": 1,
  "name": "..."
}
```

---

### 6.008 `DELETE /api/v1/prizes/{id}`

**Request:** *(không body)*

**Response `data`:** `null`

---


## GĐ7 — Portal /me (Student · Judge · Mentor)

### 7.001 `GET /api/v1/me/annual-awards`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---

### 7.002 `POST /api/v1/me/appeals`

**Request:**
```json
{
  "teamId": 10,
  "roundId": 3,
  "reason": "Kết quả chưa đúng",
  "evidenceUrl": "https://..."
}
```

**Response `data`:**
```json
{
  "id": 1,
  "status": "PENDING"
}
```

---

### 7.003 `GET /api/v1/me/certificates`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---

### 7.004 `GET /api/v1/me/certificates/{id}/download`

**Request:** *(không body)*

**Response `data`:**
```json
{
  "id": 1,
  "name": "..."
}
```

---

### 7.005 `GET /api/v1/me/hackathons/browse`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---

### 7.006 `GET /api/v1/me/hackathons/{hackathonId}/rankings`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---

### 7.007 `DELETE /api/v1/me/hackathons/{hackathonId}/register`

**Request:** *(không body)*

**Response `data`:** `null`

---

### 7.008 `POST /api/v1/me/hackathons/{hackathonId}/register`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

### 7.009 `GET /api/v1/me/history`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---

### 7.010 `GET /api/v1/me/judge-final-assignments`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---

### 7.011 `GET /api/v1/me/judge-history`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---

### 7.012 `GET /api/v1/me/judge-track-assignments`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---

### 7.013 `GET /api/v1/me/mentor-history`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---

### 7.014 `GET /api/v1/me/mentor-team-assignments`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---

### 7.015 `GET /api/v1/me/mentor-team-assignments/{teamId}/presentation-slot`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---

### 7.016 `GET /api/v1/me/mentor-track-assignments`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---

### 7.017 `GET /api/v1/me/mentor/hackathons/{hackathonId}/rankings`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---

### 7.018 `GET /api/v1/me/mentor/rounds/{roundId}/schedule`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---

### 7.019 `GET /api/v1/me/mentor/teams/{teamId}/scores`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---

### 7.020 `GET /api/v1/me/mentor/teams/{teamId}/submissions`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---

### 7.021 `GET /api/v1/me/notifications`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---

### 7.022 `PATCH /api/v1/me/notifications/read`

**Request:**
```json
{
  "notificationIds": [
    1,
    2,
    3
  ]
}
```

**Response `data`:** `null`

---

### 7.023 `GET /api/v1/me/prizes`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---

### 7.024 `GET /api/v1/me/rounds/{roundId}/leaderboard`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---

### 7.025 `GET /api/v1/me/rounds/{roundId}/problem`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---

### 7.026 `GET /api/v1/me/scores`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---

### 7.027 `PATCH /api/v1/me/scores/{id}/comment`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

### 7.028 `PATCH /api/v1/me/scoring-completion`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

### 7.029 `GET /api/v1/me/scoring-schedule`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---

### 7.030 `GET /api/v1/me/teams`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---

### 7.031 `PATCH /api/v1/me/teams/{teamId}/rounds/{roundId}/track`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

### 7.032 `GET /api/v1/me/teams/{teamId}/submissions`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---

### 7.033 `POST /api/v1/me/tiebreak-evaluations`

**Request:**
```json
{
  "roundId": 3,
  "orderedTeamIds": [
    10,
    12,
    8
  ]
}
```

**Response `data`:**
```json
{
  "roundId": 3,
  "orderedTeamIds": [
    10,
    12,
    8
  ],
  "status": "SUBMITTED"
}
```

---

### 7.034 `POST /api/v1/me/tracks/{trackId}/select`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

---

# Phần IV — Checklist nhanh (166 API)

Đánh dấ `[x]` khi đã gọi thành công (2xx). Chi tiết JSON → [Phần III](#phần-iii--catalog-api-requestresponse-json) (Ctrl+F path).

## GĐ0 — Auth & Users (25)

- [ ] POST `/auth/register`
- [ ] POST `/auth/login`
- [ ] POST `/auth/refresh`
- [ ] POST `/auth/logout`
- [ ] POST `/auth/logout-all`
- [ ] POST `/auth/change-password`
- [ ] POST `/auth/forgot-password`
- [ ] POST `/auth/reset-password`
- [ ] POST `/auth/oauth/google`
- [ ] POST `/auth/oauth/github/code`
- [ ] POST `/auth/oauth/google/link`
- [ ] POST `/auth/oauth/github/link/code`
- [ ] POST `/auth/oauth/google/unlink`
- [ ] POST `/auth/oauth/github/unlink`
- [ ] GET `/users`
- [ ] GET `/users/{userId}`
- [ ] PATCH `/users/{userId}/status`
- [ ] PATCH `/users/{userId}`
- [ ] GET `/users/{userId}/student-card`
- [ ] GET `/users/me`
- [ ] PATCH `/users/me`
- [ ] POST `/users/me/student-card`
- [ ] GET `/users/me/student-card`
- [ ] POST `/users/temp-judges`
- [ ] GET `/users/temp-judges`

## GĐ1 — Chuẩn bị (48)

- [ ] POST/GET/PUT/DELETE `/hackathons` (+ active, readiness, status)
- [ ] POST/GET/PUT/DELETE `/hackathons/{id}/rounds`
- [ ] POST/GET/PUT/DELETE `/rounds/{roundId}/tracks`, `/hackathons/{id}/tracks`
- [ ] Criteria track + round (batch, clone, weight-summary, …)
- [ ] Events CRUD
- [ ] judge-assignments, mentor-assignments
- [ ] POST `/invitations/{id}/resend`

## GĐ2 — Đội (22 + portal)

- [ ] Teams CRUD + members + mentor + lottery
- [ ] GET `/teams/{id}/journey`
- [ ] `/me/hackathons/*`, `/me/tracks/{id}/select`

## GĐ3 — Sơ loại (28)

- [ ] Round activate, release-problem, lock-scoring, ranking, scoreboard
- [ ] submissions, scores, calibration-sessions
- [ ] rbl/variance, rbl/progress
- [ ] Portal judge/student/mentor (submissions, schedule, …)

## GĐ4 — Chuyển vòng (14)

- [ ] publish, advance, wildcard, tiebreak, judge-assignments round
- [ ] `/me/tiebreak-evaluations`, leaderboard

## GĐ5 — CK (6)

- [ ] submissions CK, scores, lock final, `/me/mentor/rounds/{id}/schedule`

## GĐ6 — Kết thúc (12)

- [ ] rankings, prizes, confirm, export-jobs
- [ ] `/me/prizes`, certificates, appeals, history

## Public

- [ ] GET `/`
- [ ] GET `/rounds/{id}/scoreboard` (không JWT)
- [ ] WebSocket `/ws` (xem [mf03/06-live-scoring-websocket.md](../mf03/06-live-scoring-websocket.md))

---

**Changelog:**

- 2026-06-07 — Seed GĐ3/GĐ4/GĐ5: bảng slug, đường tắt E2E, ma trận teams/accounts, biến Postman; link `gd4-gd5-e2e-seed-data.md`.
- 2026-06-07 — POST KICKOFF→WORKSHOP; lịch WS→KO→AWARDS; readiness phased; Phần II-B; GĐ6 AWARDS.
- 2026-05-29 — Playbook dev chuẩn: E2E GĐ0→6 + catalog 166 API (request/response JSON) trong một file.
