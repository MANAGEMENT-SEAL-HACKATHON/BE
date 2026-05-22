# MF-01 GĐ1 — QA/UAT Manual Test Package

**Dự án:** SEAL Hackathon Management System — Backend  
**Phiên bản tài liệu:** 1.0 · **Ngày:** 2026-05-19  
**Vai trò:** QA Engineer / API Tester / UAT (Coordinator GĐ1)  
**Phạm vi:** Giai đoạn Chuẩn bị sự kiện (MF-01) — không gồm GĐ2+ (team, submission, scoring, RBL)

**Tài liệu liên quan:**

| Tài liệu | Vai trò |
|----------|---------|
| [mf01-gd1-quy-trinh-api.md](mf01-gd1-quy-trinh-api.md) | Runbook API, ràng buộc |
| [mf01-gd1-test-happy-path.md](mf01-gd1-test-happy-path.md) | Happy path chi tiết (Luồng A/B) |
| [mf01-gd1-timeline-events.md](mf01-gd1-timeline-events.md) | Timeline & Events (validate, `examAt`) |
| [mf01.md](mf01.md) | Spec normative |
| [Gd1SeedConstants.java](../../src/main/java/com/sealhackathon/api/config/seed/Gd1SeedConstants.java) | Slug / email seed |

---

## 1. Executive summary — Đạt 90% chưa?

| Tiêu chí | Điểm | Ghi chú |
|----------|------|---------|
| Implementation vs MF-01 GĐ1 (FR-01…07, G1–G5, conflict) | **~92%** | Ma trận đối chiếu PASS |
| API surface & runbook | **~90%** | Đủ endpoint Coordinator GĐ1 |
| Automated test (JUnit API/controller) | **~25%** | Chủ yếu unit validator/sequence |
| Ma trận UAT negative / edge (tài liệu) | **~90%** | File này — 50+ TC |
| **Sẵn sàng ký UAT** (có evidence chạy tay) | **~84%** | Sau khi bạn điền §8 |

**Verdict:** Backend GĐ1 **đủ cho demo và UAT Coordinator**. Chưa gọi là “hoàn hảo >90%” cho đến khi bạn chạy §4–§7 và ghi kết quả §8 (và bổ sung automation sau).

**Không test trong GĐ1 (ghi rõ Out of scope):** JWT thật, multi-role một user (MENTOR+JUDGE), team/submission/scoring, Judge Chung kết GĐ4, RBL export.

---

## 2. Findings & gaps

### 2.1 P0 — Limitation (không chặn demo, phải ghi báo cáo)

| ID | Mô tả | Mitigation UAT |
|----|-------|----------------|
| QA-01 | Auth **stub** — mọi `@CoordinatorOnly` dùng user **id=1** | Không test 401/403 role |
| QA-02 | Một tài khoản không vừa `MENTOR` vừa `JUDGE` | Dùng `mentor@` + `judge1@`; cross-track TC-GD1-H11 |
| QA-03 | Coordinator seed **phải id=1** | DB trống lần đầu hoặc truncate |

### 2.2 P1 — Doc / coverage (đã bổ sung trong file này)

| ID | Mô tả | Trạng thái |
|----|-------|------------|
| QA-04 | Thiếu TC cross-track mentor/judge | TC-GD1-H11 |
| QA-05 | CONFLICT_SAME_TRACK khó test qua API GĐ1 (1 role/user) | TC-GD1-N10a/b |
| QA-06 | resend invitation, clone criteria, PUT locked | TC-GD1-E01–E06 |
| QA-07 | Legacy route (đã gỡ trong code) | Xem [mf01.md](mf01.md) §10 |

### 2.3 P2 — Sau UAT

| ID | Mô tả |
|----|-------|
| QA-08 | Thiếu `@WebMvcTest` / integration test cho controller GĐ1 |
| QA-09 | Test package `com.se194093.be` trùng với `com.sealhackathon.api` |
| QA-10 | Criteria template global (capstone) — ngoài MF-01 GĐ1 |

---

## 3. Môi trường & dữ liệu test

### 3.1 Cấu hình

| Mục | Giá trị |
|-----|---------|
| Base URL | `http://localhost:8080` |
| API prefix | `/api/v1` |
| Profile | `spring.profiles.active=dev` |
| Auth | **Không** gửi JWT — stub user id=1 |
| Response OK | `{ "success": true, "data": ... }` |
| Response lỗi | `{ "success": false, "error": { "code": "...", "message": "..." }, "errors": [...] }` |

**Khởi động:** `mvn spring-boot:run -Dspring-boot.run.profiles=dev` (hoặc IDE profile `dev`). Đọc log:

```text
[Gd1DataSeeder] Seed MF-01 GĐ1 hoàn tất.
  Coordinator: id=1 email=coord@fpt.edu.vn
```

### 3.2 Seed cố định

| Biến | Giá trị | Mục đích |
|------|---------|----------|
| `slugIncomplete` | `seal-gd1-incomplete` | Readiness fail |
| `slugReady` | `seal-gd1-ready` | Readiness pass |
| `slugOngoing` | `seal-spring-2026` | Đã ONGOING |
| `emailCoord` | `coord@fpt.edu.vn` | id=1 |
| `emailMentor` | `mentor@fpt.edu.vn` | MENTOR APPROVED |
| `emailJudge1` | `judge1@fpt.edu.vn` | JUDGE APPROVED |
| `emailJudge2` | `judge2@fpt.edu.vn` | JUDGE APPROVED |
| `emailPending` | `pending.judge@fpt.edu.vn` | PENDING |
| `emailGuest` | `guestjudge@gmail.com` | EXTERNAL seed |

**Lấy ID:** `GET {{baseUrl}}/api/v1/hackathons?q=seal-gd1-ready` → `data.items[0].id`. Tiếp: rounds, tracks theo [mf01-gd1-test-happy-path.md §2](mf01-gd1-test-happy-path.md).

**Greenfield slug (Luồng A):** `seal-uat-20260519` — đổi suffix mỗi lần chạy.

### 3.3 Biến Postman (điền khi chạy)

| Biến | Nguồn |
|------|-------|
| `hackathonId` | POST hackathon hoặc GET seed |
| `prelimRoundId` | POST round 1 |
| `finalRoundId` | POST round 2 |
| `track1Id`, `track2Id` | POST tracks |
| `mentorId` | Log seeder / GET users |
| `judge1Id`, `judge2Id` | Log seeder |
| `mentorAssignmentId` | POST mentor-assignments |
| `judgeAssignmentId` | POST judge-assignments |
| `criterionId` | GET criteria list |
| `eventId` | POST event đầu |
| `invitationId` | POST temp-judges → `data.invitation.id` |

### 3.4 Gợi ý ngày (greenfield)

| Field | Gợi ý |
|-------|--------|
| `registrationStart` | Hôm nay |
| `registrationEnd` | Hôm nay + 13 ngày |
| `eventStart` | Hôm nay + 14 ngày |
| `eventEnd` | eventStart + 45 ngày |
| Round `submissionDeadline` | **> NOW** (vd. hôm nay + 30 ngày) |
| Events | WORKSHOP → KICKOFF → PRESENTATION → AWARDS (xem §5) |

---

## 4. Ma trận test case — tổng quan

| Nhóm | Số TC | Priority |
|------|-------|----------|
| Happy (H) | 15 | P0 |
| Negative (N) | 28 | P0–P1 |
| Edge / API phụ (E) | 10 | P1 |
| Seed smoke (B) | 11 | P0 |
| **Tổng** | **64** | |

Cột kết quả: điền tại **§8**.

---

## 5. Luồng A — Happy path (greenfield 7 bước)

> JSON đầy đủ từng bước: [mf01-gd1-test-happy-path.md §3](mf01-gd1-test-happy-path.md). Dưới đây là TC map + body tóm tắt.

### TC-GD1-H01 — Tạo Hackathon

| Field | Value |
|-------|-------|
| Method | `POST /api/v1/hackathons` |
| Expected | **201**, `data.status` = `DRAFT` |

```json
{
  "name": "SEAL UAT Greenfield",
  "slug": "seal-uat-20260519",
  "season": "Spring",
  "year": 2026,
  "description": "UAT Luồng A",
  "registrationStart": "2026-05-19",
  "registrationEnd": "2026-06-01",
  "eventStart": "2026-06-02",
  "eventEnd": "2026-07-17",
  "wildcardEnabled": true,
  "individualRankingEnabled": false
}
```

### TC-GD1-H02 — Tạo 2 Round

**2.1 Sơ loại** — `POST /api/v1/hackathons/{{hackathonId}}/rounds`

```json
{
  "name": "Vòng Sơ loại",
  "sequenceOrder": 1,
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

**2.2 Chung kết** — cùng path

```json
{
  "name": "Vòng Chung kết",
  "sequenceOrder": 2,
  "isFinal": true,
  "roundType": "FINAL",
  "submissionDeadline": "2026-07-12T23:59:59",
  "lateSubmissionPolicy": "HARD_LOCK",
  "tiebreakRule": "PENALTY_SCORE"
}
```

Expected: **201** ×2; lưu `prelimRoundId`, `finalRoundId`.

### TC-GD1-H03 — Tạo 2 Track (prelim)

`POST /api/v1/rounds/{{prelimRoundId}}/tracks`

**Track 1:**

```json
{
  "name": "Track 1 — RAG Pipeline",
  "description": "RAG",
  "topic": null,
  "maxTeams": 8,
  "maxTeamsPerGroup": 8,
  "minTeamSize": 3,
  "maxTeamSize": 5,
  "sequenceOrder": 1
}
```

**Track 2:** đổi `name` / `sequenceOrder: 2`.

Expected: **201** ×2; `GET /rounds/{{prelimRoundId}}/tracks` → 2 items, có `roundId`.

### TC-GD1-H04 — Criteria batch

`POST /api/v1/tracks/{{track1Id}}/criteria/batch` và track2 (cùng bộ):

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

`POST /api/v1/rounds/{{finalRoundId}}/criteria/batch`:

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

### TC-GD1-H05 — Weight summary

| Method | Path | Expected |
|--------|------|----------|
| GET | `/tracks/{{track1Id}}/criteria/weight-summary` | **200**, total ≈ 1.0 |
| GET | `/rounds/{{finalRoundId}}/criteria/weight-summary` | **200**, total ≈ 1.0 |

### TC-GD1-H06 — Mentor assignment

`POST /api/v1/mentor-assignments`

```json
{
  "mentorId": {{mentorId}},
  "trackId": {{track1Id}}
}
```

Expected: **201**.

### TC-GD1-H07 — Judge assignment (track 1)

```json
{
  "judgeId": {{judge1Id}},
  "trackId": {{track1Id}},
  "assignmentType": "NORMAL"
}
```

Expected: **201**.

### TC-GD1-H11 — Cross-track (capstone § mentor A / judge B)

**Pre:** H06 mentor → `track1Id`. **Không** gán judge1 vào track1 nếu muốn tách user — hoặc dùng judge2.

`POST /api/v1/judge-assignments`

```json
{
  "judgeId": {{judge2Id}},
  "trackId": {{track2Id}},
  "assignmentType": "NORMAL"
}
```

Expected: **201** (mentor track1 + judge track2 — hợp lệ mf01 §6.4).

### TC-GD1-H08 — Events (đúng thứ tự)

`POST /api/v1/hackathons/{{hackathonId}}/events` ×4:

1. **WORKSHOP** — `2026-05-28T20:00:00` … `21:30:00`, `location`: `"Online (Teams)"`
2. **KICKOFF** — `2026-06-02T14:00:00` … `17:00:00`, `location`: `"FPT HCM — Hội trường A"`
3. **PRESENTATION** — `2026-06-20T06:00:00` … `19:00:00`
4. **AWARDS** — `2026-07-12T08:00:00` … `18:00:00`

Mỗi body: `{ "title": "...", "type": "...", "location": "...", "startsAt": "...", "endsAt": "...", "isPublic": true }`

### TC-GD1-H09 — Readiness

`GET /api/v1/hackathons/{{hackathonId}}/readiness?target=ONGOING`

Expected: **200**, `data.ready` = **true**, `blockers` = `[]`.

### TC-GD1-H10 — PATCH ONGOING

`PATCH /api/v1/hackathons/{{hackathonId}}/status`

```json
{ "status": "ONGOING" }
```

Expected: **200**, `GET /hackathons/{{hackathonId}}` → `status` = `ONGOING`.

### TC-GD1-H12 — (Luồng B) Seed ready → ONGOING

Dùng `slugReady`; chỉ H09 + H10. Expected: pass.

### TC-GD1-H13 — GET nested

| GET | Expected |
|-----|----------|
| `/hackathons/{{id}}/rounds` | prelim có `trackCount` ≥ 2 |
| `/rounds/{{prelimRoundId}}/tracks` | `roundId` khớp |
| `/hackathons/{{id}}/tracks` | cùng tracks |

### TC-GD1-H14 — Temp judge

`POST /api/v1/users/temp-judges`

```json
{
  "fullName": "Guest UAT",
  "email": "guest-uat-unique-001@company.com",
  "institution": "Partner Corp",
  "phone": "+84901234567",
  "hackathonId": {{hackathonId}}
}
```

Expected: **201**; lưu `invitationId` từ `data.invitation.id` (nếu có).

### TC-GD1-H15 — List hackathons

`GET /api/v1/hackathons?status=DRAFT&q=seal-uat` → **200**, có hackathon vừa tạo.

---

## 6. Luồng B — Seed smoke (11 case)

**Pre:** App đã seed; `GET /api/v1/hackathons?q=seal-gd1-ready` → `hackathonId`, lấy `prelimRoundId`, `finalRoundId`, `track1Id`, `track2Id`.

| TC | Method | Path | Expected |
|----|--------|------|----------|
| TC-GD1-B01 | GET | `/hackathons/{{hackathonId}}/rounds` | 2 rounds |
| TC-GD1-B02 | GET | `/rounds/{{prelimRoundId}}/tracks` | ≥ 2 tracks |
| TC-GD1-B03 | GET | `/hackathons/{{hackathonId}}/tracks` | mỗi item có `roundId` |
| TC-GD1-B04 | GET | `/tracks/{{track1Id}}/criteria` | 5 criteria |
| TC-GD1-B05 | GET | `/tracks/{{track1Id}}/criteria/weight-summary` | total ≈ 1.0 |
| TC-GD1-B06 | GET | `/rounds/{{finalRoundId}}/criteria` | 5 criteria |
| TC-GD1-B07 | GET | `/tracks/{{track1Id}}/mentors` | ≥ 1 |
| TC-GD1-B08 | GET | `/tracks/{{track1Id}}/judges` | ≥ 1 NORMAL |
| TC-GD1-B09 | GET | `/hackathons/{{hackathonId}}/events` | có KICKOFF |
| TC-GD1-B10 | GET | `/hackathons/{{hackathonId}}/readiness?target=ONGOING` | `ready: true` |
| TC-GD1-B11 | PATCH | `/hackathons/{{hackathonId}}/status` `{ "status": "ONGOING" }` | **200** (nếu vẫn DRAFT) |

**Incomplete:** `q=seal-gd1-incomplete` → readiness `ready: false` (TC-GD1-N08).

**Ongoing:** `seal-spring-2026` → PATCH ONGOING lại → TC-GD1-N09.

---

## 7. Luồng C — Negative & edge

### 7.1 Hackathon & Round

#### TC-GD1-N01 — Duplicate slug

`POST /api/v1/hackathons` — body H01 nhưng `slug` trùng hackathon đã tồn tại.

Expected: **409** `HACKATHON_DUPLICATE`

#### TC-GD1-N02 — Date range

```json
{
  "name": "SEAL Bad Dates",
  "slug": "seal-uat-bad-dates-1",
  "season": "Spring",
  "year": 2026,
  "registrationStart": "2026-06-10",
  "registrationEnd": "2026-06-01",
  "eventStart": "2026-06-02",
  "eventEnd": "2026-07-17",
  "wildcardEnabled": false,
  "individualRankingEnabled": false
}
```

Expected: **422** `HACKATHON_DATE_RANGE`

#### TC-GD1-N03 — Round deadline quá khứ

Trên hackathon DRAFT mới, `POST .../rounds`:

```json
{
  "name": "Bad deadline",
  "sequenceOrder": 1,
  "isFinal": false,
  "roundType": "PRELIMINARY",
  "submissionDeadline": "2020-01-01T00:00:00",
  "lateSubmissionPolicy": "ALLOW_LATE_PENDING",
  "tiebreakRule": "PENALTY_SCORE"
}
```

Expected: **422** `ROUND_DEADLINE_INVALID`

#### TC-GD1-N04 — FINAL sequenceOrder

Tạo prelim `sequenceOrder: 2`, final `sequenceOrder: 1` (hoặc final ≤ prelim).

Expected: **422** `ROUND_FINAL_SEQUENCE_ORDER`

#### TC-GD1-N05 — Track trong round FINAL

`POST /api/v1/rounds/{{finalRoundId}}/tracks` — body track bất kỳ.

Expected: **422** `DESIGN_VIOLATION`

#### TC-GD1-N21 — Hai round FINAL

Thêm round thứ 3 `isFinal: true` trên cùng hackathon (sau khi đã có 1 FINAL).

Expected: readiness blocker `MISSING_FINAL_ROUND` (đếm >1) hoặc lỗi tạo round tùy validation.

### 7.2 Criteria

#### TC-GD1-N06 — Weight ≠ 1.0

`POST /tracks/{{track1Id}}/criteria/batch` (track mới chưa có criteria):

```json
{
  "items": [
    { "name": "Only one", "type": "TECHNICAL", "weight": 0.50, "maxScore": 10, "displayOrder": 1 }
  ]
}
```

Sau đó `GET readiness` hoặc `GET weight-summary` → blocker / warning.

Expected: readiness **fail** `TRACK_CRITERIA_WEIGHT` khi PATCH ONGOING.

#### TC-GD1-N22 — Criteria trên round không FINAL

`POST /api/v1/rounds/{{prelimRoundId}}/criteria/batch` — body batch hợp lệ.

Expected: **422** `ROUND_NOT_FINAL_FOR_CRITERIA` hoặc tương đương.

### 7.3 Gate & status

#### TC-GD1-N07 — ONGOING thiếu KICKOFF

Greenfield: tạo hackathon + round + track + criteria + assign, **không** tạo event KICKOFF → `PATCH status ONGOING`.

Expected: **422** `READINESS_NOT_PASSED`

#### TC-GD1-N08 — Incomplete seed

`GET /api/v1/hackathons?q=seal-gd1-incomplete` → `GET .../readiness?target=ONGOING`

Expected: `ready: false`, blockers chứa `MISSING_PRELIMINARY_ROUND` hoặc tương tự.

#### TC-GD1-N09 — PATCH ONGOING khi đã ONGOING

Hackathon `seal-spring-2026`: `PATCH .../status` `{ "status": "ONGOING" }`

Expected: **409** `STATUS_TRANSITION_INVALID`

#### TC-GD1-N16 — PUT hackathon không DRAFT

Sau H10 (ONGOING): `PUT /api/v1/hackathons/{{hackathonId}}` — body sửa `description`.

Expected: **409** `HACKATHON_NOT_DRAFT`

#### TC-GD1-N17 — DELETE hackathon có con

Trên hackathon đã có round (DRAFT): `DELETE /api/v1/hackathons/{{hackathonId}}`

Expected: **409** `HACKATHON_HAS_CHILDREN`

### 7.4 Personnel

#### TC-GD1-N10a — Judge user làm Mentor cùng track (GĐ1)

1. `POST judge-assignments` — `judge1Id` + `track1Id`
2. `POST mentor-assignments` — `mentorId: judge1Id` + `track1Id`

Expected bước 2: **422** `USER_INVALID_ROLE` (một role/user).

#### TC-GD1-N10b — CONFLICT_SAME_TRACK (cùng person)

**Chỉ khi GĐ2 multi-role** hoặc seed DB có cùng `user_id` trong `mentor_assignments` và `judge_assignments` cùng `track_id`.

Expected: **422** `CONFLICT_SAME_TRACK` (trigger/app).

#### TC-GD1-N11 — Mentor role sai

`POST mentor-assignments` — `mentorId: judge1Id`, `trackId: track2Id`

Expected: **422** `USER_INVALID_ROLE`

#### TC-GD1-N12 — User PENDING

Lấy id `pending.judge@fpt.edu.vn` → `POST judge-assignments` với `judgeId` đó.

Expected: **422** `USER_NOT_APPROVED`

#### TC-GD1-N13 — Judge Chung kết GĐ1

```json
{
  "judgeId": {{judge1Id}},
  "roundId": {{finalRoundId}},
  "assignmentType": "FINAL_EXTERNAL"
}
```

Expected: **422** `JUDGE_FINAL_AT_PHASE1`

#### TC-GD1-N18 — Duplicate mentor

Lặp lại H06 cùng `mentorId` + `track1Id`.

Expected: **409** `MENTOR_ASSIGN_DUPLICATE`

#### TC-GD1-N19 — Duplicate judge

Lặp lại H07 cùng `judgeId` + `track1Id`.

Expected: **409** `JUDGE_ASSIGN_DUPLICATE`

#### TC-GD1-N20 — Email trùng temp judge

`POST temp-judges` với `email: "judge1@fpt.edu.vn"`.

Expected: **409** `USER_EMAIL_TAKEN`

#### TC-GD1-N23 — Mentor track CANCELLED

1. `PUT /api/v1/tracks/{{track2Id}}` — `{ "status": "CANCELLED" }` (các field khác giữ nguyên theo GET track)
2. `POST mentor-assignments` — mentor → `track2Id`

Expected: **422** `INVALID_STATE`

#### TC-GD1-N24 — GET judges round FINAL GĐ1

`GET /api/v1/rounds/{{finalRoundId}}/judges` — không assign CK ở GĐ1.

Expected: **200**, list rỗng `[]`.

### 7.5 Events

#### TC-GD1-N14 — Thứ tự event sai

Tạo **AWARDS** trước **KICKOFF** (hackathon mới đến bước 6).

Expected: **422** `EVENT_ORDER_VIOLATION`

#### TC-GD1-N15 — Thiếu location và meetUrl

```json
{
  "title": "Bad event",
  "type": "WORKSHOP",
  "startsAt": "2026-06-01T10:00:00",
  "endsAt": "2026-06-01T11:00:00",
  "isPublic": true
}
```

Expected: **422** `EVENT_LOCATION_REQUIRED`

### 7.6 Edge — API phụ (P1)

#### TC-GD1-E01 — Criteria clone

`POST /api/v1/tracks/{{track2Id}}/criteria/clone`

```json
{ "sourceTrackId": {{track1Id}} }
```

Expected: **201**; track2 có criteria giống track1.

#### TC-GD1-E02 — Clone source rỗng

`POST /tracks/{{track2Id}}/criteria/clone` — `sourceTrackId` trỏ track chưa có criteria.

Expected: **422** `CRITERIA_CLONE_SOURCE_EMPTY`

#### TC-GD1-E03 — PUT criterion

`PUT /api/v1/criteria/{{criterionId}}` — `{ "description": "UAT updated" }` (giữ weight).

Expected: **200**.

#### TC-GD1-E04 — PATCH is_dept_head

`PATCH /api/v1/users/{{judge1Id}}`

```json
{ "isDeptHead": true }
}
```

Expected: **200** (GĐ4 dùng sau).

#### TC-GD1-E05 — Invitation resend còn hạn

Sau H14, ngay lập tức: `POST /api/v1/invitations/{{invitationId}}/resend`

Expected: **422** `INVITATION_STILL_VALID`

#### TC-GD1-E06 — Invitation resend sau hết hạn

**Manual:** UPDATE DB `invitations.expires_at = NOW() - 1 day` cho invitation test, rồi resend.

Expected: **200**, token mới.

#### TC-GD1-E07 — DELETE track OPEN (một bước, không cần CANCELLED)

`DELETE /api/v1/tracks/{{track1Id}}` — track `status=OPEN`, không có team active, round cha không `is_active`.

Expected: **200**, `data.deletedId` khớp. **Không** còn `TRACK_NOT_CANCELLED`.

#### TC-GD1-E07b — DELETE mentor assignment

`DELETE /api/v1/mentor-assignments/{{mentorAssignmentId}}`

Expected: **204** hoặc **200** (theo implementation).

#### TC-GD1-E08 — GET user assignments

| GET | Expected |
|-----|----------|
| `/users/{{mentorId}}/track-assignments` | có track1 |
| `/users/{{judge1Id}}/round-assignments` | rỗng hoặc có nếu assign round |

#### TC-GD1-E09 — Round activate (GĐ3, smoke)

`PATCH /api/v1/rounds/{{prelimRoundId}}/activate` trên hackathon ONGOING đủ criteria.

Expected: **200** hoặc lỗi weight/conflict nếu thiếu — ghi actual.

#### TC-GD1-E10 — 404 resource

`GET /api/v1/hackathons/999999` → **404** `RESOURCE_NOT_FOUND`

---

## 8. Biểu mẫu ghi kết quả UAT

**Tester:** _______________  **Ngày:** _______________  **Build/Commit:** _______________

| TC ID | Priority | Pass/Fail | Actual HTTP | error.code (nếu Fail) | Ghi chú |
|-------|----------|-----------|-------------|---------------------|---------|
| TC-GD1-H01 | P0 | | | | |
| TC-GD1-H02 | P0 | | | | |
| TC-GD1-H03 | P0 | | | | |
| TC-GD1-H04 | P0 | | | | |
| TC-GD1-H05 | P0 | | | | |
| TC-GD1-H06 | P0 | | | | |
| TC-GD1-H07 | P0 | | | | |
| TC-GD1-H08 | P0 | | | | |
| TC-GD1-H09 | P0 | | | | |
| TC-GD1-H10 | P0 | | | | |
| TC-GD1-H11 | P0 | | | | |
| TC-GD1-H12 | P0 | | | | |
| TC-GD1-H13 | P0 | | | | |
| TC-GD1-H14 | P0 | | | | |
| TC-GD1-H15 | P0 | | | | |
| TC-GD1-B01 … B11 | P0 | | | | |
| TC-GD1-N01 … N24 | P0/P1 | | | | |
| TC-GD1-E01 … E10 | P1 | | | | |

**Tiêu chí đạt UAT GĐ1:** Tất cả **P0** Pass; **P1** Fail ≤ 2 và có ticket; không P0 blocker mở.

**Thống kê sau chạy:**

| | Số lượng |
|---|----------|
| Pass P0 | / 26 |
| Fail P0 | |
| Pass P1 | / 38 |
| Blocked | |

---

## 9. curl smoke (copy nhanh)

```bash
# Readiness seed ready (thay HACKATHON_ID)
curl -s "http://localhost:8080/api/v1/hackathons?q=seal-gd1-ready"
curl -s "http://localhost:8080/api/v1/hackathons/HACKATHON_ID/readiness?target=ONGOING"

# Negative incomplete
curl -s "http://localhost:8080/api/v1/hackathons?q=seal-gd1-incomplete"
```

---

## 10. Checklist coverage API GĐ1

| Module | Endpoints | Covered by |
|--------|-----------|------------|
| Hackathon | CRUD, readiness, status | H01, H09–10, N01–02, N07–09, N16–17 |
| Round | CRUD, activate | H02, N03–04, N21, E09 |
| Track | CRUD, list | H03, H13, N05, N23 |
| Criteria | batch, GET, PUT, clone, delete | H04–05, N06, N22, E01–03 |
| Personnel | temp-judge, mentor, judge, PATCH user | H06–07, H11, H14, N10–20, E04–08 |
| Invitations | resend | E05–06 |
| Events | CRUD | H08, N14–15 |
| FR-07B activate | PATCH round | E09 |

---

*SEAL Hackathon BE — QA/UAT GĐ1 — FPT University HCMC*
