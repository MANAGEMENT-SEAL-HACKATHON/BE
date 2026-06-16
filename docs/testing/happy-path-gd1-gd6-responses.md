# Dev seed — Hướng dẫn dữ liệu test

> **Profile:** `dev`  
> **Slug ONGOING:** `seal-e2e-2026` (xem [`dev-seed-guide.md`](./dev-seed-guide.md))

---

## 0. Chuẩn bị

### 0.1 Tài khoản

| Role | Email | Password |
|------|-------|----------|
| Coordinator | `coord@fpt.edu.vn` | `Coordinator@dev1` |
| Judge | `judge1@fpt.edu.vn` | `Judge@dev1` |
| Mentor | `mentor@fpt.edu.vn` | `Mentor@dev1` |
| Guest Judge | `guestjudge@gmail.com` | `GuestJudge@dev1` |
| Student (GĐ1/GĐ2) | `student.sp26.t01.leader@fpt.edu.vn` | `Student@dev1` |
| Student (GĐ3) | `student.sp23.t01.leader@fpt.edu.vn` | `Student@dev1` |
| Student (GĐ4) | `student.sp25.t01.leader@fpt.edu.vn` | `Student@dev1` |
| Student (GĐ5) | `student.sp24.t01.leader@fpt.edu.vn` | `Student@dev1` |
| Student (GĐ6) | `student.sp30.t01.leader@fpt.edu.vn` | `Student@dev1` |
| God Mode test | `test.user1@fpt.edu.vn` … `test.user11@fpt.edu.vn` | `Student@dev1` |

### 0.2 Hackathon theo giai đoạn (slug seed 2026)

| GĐ | Slug | Trạng thái seed | Ghi chú |
|----|------|-----------------|--------|
| GĐ1→GĐ6 | `seal-e2e-2026` | `ONGOING` | 7 đội + 3 orphan; tiến GĐ2→GĐ6 trên cùng hackathon |
| Archive | `seal-fall-2025-finished` | `FINISHED` | Chỉ xem lịch sử |

### 0.3 Biến Postman (lấy từ response bước trước)

| Biến | Cách lấy |
|------|----------|
| `coordToken` | `POST /auth/login` Coordinator |
| `studentToken` | `POST /auth/login` Student |
| `judgeToken` | `POST /auth/login` Judge |
| `hackathonId` | `GET /hackathons?q=<slug>` → `data.content[0].id` |
| `prelimRoundId` | `GET /hackathons/{id}/rounds` → round `isFinal=false` |
| `finalRoundId` | `GET /hackathons/{id}/rounds` → round `isFinal=true` |
| `track1Id` | `GET /rounds/{prelimRoundId}/tracks` → item đầu |
| `teamId` | `GET /teams?hackathonId=&status=ACTIVE` |

### 0.4 Login (GĐ0)

```http
POST /api/v1/auth/login
Content-Type: application/json
```

```json
{
  "email": "coord@fpt.edu.vn",
  "password": "Coordinator@dev1"
}
```

**Response `200` — `data` (rút gọn):**

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "tokenType": "Bearer",
  "expiresIn": 86400,
  "user": {
    "id": 1,
    "email": "coord@fpt.edu.vn",
    "role": "COORDINATOR",
    "status": "APPROVED"
  }
}
```

---

## GĐ1 — Chuẩn bị sự kiện

**Đường tắt (khuyến nghị):** dùng seed `seal-spring-2026` — đã có round/track/criteria/events. Chỉ **verify**, không cần tạo lại.

### GĐ1-A — Verify readiness (happy)

```http
GET /api/v1/hackathons?q=seal-spring-2026&size=5
Authorization: Bearer {{coordToken}}
```

**Response `200` — `data.content[0]` (rút gọn):**

```json
{
  "id": 2,
  "name": "SEAL Spring 2026",
  "slug": "seal-spring-2026",
  "status": "ONGOING",
  "season": "Spring",
  "year": 2026,
  "maxParticipants": 120
}
```

→ Lưu `hackathonId`.

```http
GET /api/v1/hackathons/{{hackathonId}}/readiness?target=ONGOING
Authorization: Bearer {{coordToken}}
```

**Response `200` — kỳ vọng happy:**

```json
{
  "ready": true,
  "targetStatus": "ONGOING",
  "blockers": [],
  "warnings": [],
  "summary": {
    "tracksCount": 3,
    "roundsCount": 2,
    "criteriaCount": 12,
    "eventsCount": 2
  }
}
```

### GĐ1-B — Tạo hackathon mới (greenfield, tùy chọn)

```http
POST /api/v1/hackathons
Authorization: Bearer {{coordToken}}
```

```json
{
  "name": "SEAL E2E Happy 2026",
  "slug": "seal-e2e-happy-2026",
  "season": "Spring",
  "year": 2026,
  "description": "Happy path manual test",
  "registrationStart": "2026-05-24",
  "registrationEnd": "2026-06-05",
  "eventStart": "2026-06-10",
  "eventEnd": "2026-06-10",
  "wildcardEnabled": true,
  "individualRankingEnabled": true,
  "maxParticipants": 100
}
```

**Response `201` — `data`:**

```json
{
  "id": 15,
  "name": "SEAL E2E Happy 2026",
  "slug": "seal-e2e-happy-2026",
  "status": "DRAFT",
  "maxParticipants": 100,
  "registrationStart": "2026-05-24",
  "registrationEnd": "2026-06-05"
}
```

Tiếp theo: tạo round Sơ loại + CK, track, criteria batch, events KICKOFF + WORKSHOP → `readiness?target=ONGOING` → `PATCH status ONGOING` (chi tiết JSON xem [`full-workflow-api-test-gd1-gd6.md`](./full-workflow-api-test-gd1-gd6.md) §1.2–1.12).

```http
PATCH /api/v1/hackathons/{{hackathonId}}/status
Authorization: Bearer {{coordToken}}
```

```json
{ "status": "ONGOING", "note": "Mở đăng ký" }
```

**Response `200` — `data.status`:** `"ONGOING"`.

### GĐ1-C — Readiness FAIL (smoke phụ, slug `seal-gd1-incomplete`)

```http
GET /api/v1/hackathons?q=seal-gd1-incomplete
GET /api/v1/hackathons/{{hackathonId}}/readiness?target=ONGOING
```

**Kỳ vọng:** `ready: false`, `blockers` không rỗng (thiếu round/track/criteria).

---

## GĐ2 — Đăng ký, đội, God Mode, bốc thăm

**Hackathon:** `seal-spring-2026` (`hackathonId` từ GĐ1-A).

### GĐ2-1 — Student tạo đội

```http
POST /api/v1/me/teams
Authorization: Bearer {{studentToken}}
```

```json
{
  "hackathonId": 2,
  "teamName": "SP26-T99"
}
```

**Response `201` — `data`:**

```json
{
  "id": 99,
  "hackathonId": 2,
  "teamName": "SP26-T99",
  "leaderId": 50,
  "chapterId": 1,
  "status": "PENDING",
  "isLocked": false,
  "createdAt": "2026-06-16T11:15:00"
}
```

> Alias portal của `POST /api/v1/teams` — cùng logic nghiệp vụ.

### GĐ2-2 — Student đăng ký hackathon

```http
GET /api/v1/me/hackathons/browse?status=ONGOING
Authorization: Bearer {{studentToken}}
```

**Response `200` — `data`:**

```json
[
  {
    "id": 2,
    "name": "SEAL Spring 2026",
    "status": "ONGOING",
    "registered": false
  }
]
```

```http
POST /api/v1/me/hackathons/{{hackathonId}}/register
Authorization: Bearer {{studentToken}}
```

**Response `200` — `data`:** `null` (hoặc message thành công).

Browse lại → `registered: true`.

### GĐ2-2 — Bảng tin ghép đội (Student)

```http
GET /api/v1/teams/hackathons/{{hackathonId}}/matchmaking
Authorization: Bearer {{studentToken}}
```

**Response `200` — `data` (mẫu 1 đội thiếu người):**

```json
[
  {
    "id": 45,
    "teamName": "SP26-T12",
    "leaderId": 88,
    "leaderName": "Nguyễn Văn Leader",
    "status": "PENDING",
    "acceptedMemberCount": 2,
    "members": [
      {
        "userId": 88,
        "fullName": "Nguyễn Văn Leader",
        "email": "student.sp26.t12.leader@fpt.edu.vn",
        "roleInTeam": "LEADER",
        "status": "ACCEPTED"
      }
    ]
  }
]
```

> FE map `id` → `teamId`, lấy `leaderEmail` từ `members[]`.

### GĐ2-3 — Radar God Mode (Coordinator)

```http
GET /api/v1/teams/hackathons/{{hackathonId}}/orphans
Authorization: Bearer {{coordToken}}
```

**Response `200` — `data`:**

```json
[
  {
    "id": 120,
    "fullName": "SV Test 1",
    "email": "test.user1@fpt.edu.vn",
    "role": "STUDENT",
    "status": "APPROVED"
  }
]
```

```http
GET /api/v1/teams/hackathons/{{hackathonId}}/incomplete-teams
Authorization: Bearer {{coordToken}}
```

**Response `200` — `data`:** danh sách đội `PENDING` + `acceptedMemberCount < 3` (cấu trúc giống matchmaking).

**Thêm thành viên (happy):**

```http
POST /api/v1/teams/{{teamId}}/admin-add-member
Authorization: Bearer {{coordToken}}
```

```json
{ "userId": 120 }
```

**Response `200` — `data` (rút gọn):**

```json
{
  "id": 45,
  "teamName": "Đội Test A",
  "status": "ACTIVE",
  "acceptedMemberCount": 3,
  "isLocked": false
}
```

> Không gọi `PATCH /teams/{id}/approve` — BE tự ACTIVE khi đủ 3 người.

**Gộp 2 đội (happy: 2+2=4):**

```http
POST /api/v1/teams/{{targetTeamId}}/admin-merge
Authorization: Bearer {{coordToken}}
```

```json
{ "sourceTeamId": 46 }
```

**Response `200` — `data`:** target team `acceptedMemberCount: 4`, `status: "ACTIVE"`.

### GĐ2-4 — Danh sách đội & duyệt (nếu còn PENDING)

```http
GET /api/v1/teams?hackathonId={{hackathonId}}&status=PENDING
Authorization: Bearer {{coordToken}}
```

```http
PATCH /api/v1/teams/{{teamId}}/approve
Authorization: Bearer {{coordToken}}
```

**Response `200` — `data.status`:** `"ACTIVE"` (khi đủ 3–5 ACCEPTED).

### GĐ2-5 — Bốc thăm (lottery)

**Điều kiện:** đội `ACTIVE` + `isLocked: true` (seed `seal-spring-2026` thường đã khóa).

```http
GET /api/v1/teams/{{teamId}}
Authorization: Bearer {{coordToken}}
```

→ `data.isLocked === true`.

```http
PATCH /api/v1/hackathons/{{hackathonId}}/lottery
Authorization: Bearer {{coordToken}}
```

```json
{
  "roundId": "{{prelimRoundId}}",
  "assignments": []
}
```

**Response `200` — `data` (auto round-robin):**

```json
{
  "hackathonId": 2,
  "roundId": 5,
  "assignedCount": 24,
  "assignments": [
    {
      "teamId": 10,
      "trackId": 7,
      "assignedGroup": "Bảng A"
    }
  ]
}
```

**Checkpoint GĐ2:** 24 đội ACTIVE, đã gán track, sẵn sàng activate Sơ loại (GĐ3).

---

## GĐ3 — Sơ loại

**Hackathon:** `seal-spring-2026-gd3`  
**Student mẫu:** `student.sp23.t01.leader@fpt.edu.vn`

### GĐ3-0 — Lấy ID

```http
GET /api/v1/hackathons?q=seal-spring-2026-gd3
Authorization: Bearer {{coordToken}}
```

### GĐ3-1 — Activate Sơ loại

```http
PATCH /api/v1/rounds/{{prelimRoundId}}/activate
Authorization: Bearer {{coordToken}}
```

```json
{ "note": "Start prelim GĐ3" }
```

**Response `200` — `data`:**

```json
{
  "id": 12,
  "isActive": true,
  "activatedAt": "2026-06-10T08:00:00"
}
```

### GĐ3-2 — Phát đề

```http
PATCH /api/v1/rounds/{{prelimRoundId}}/release-problem
Authorization: Bearer {{coordToken}}
```

```json
{
  "problemStatementUrl": "https://example.com/debai-gd3.pdf"
}
```

### GĐ3-3 — Nộp bài (team chưa nộp — SP23-T21)

```http
POST /api/v1/submissions
Authorization: Bearer {{studentToken}}
```

```json
{
  "teamId": "{{teamId}}",
  "trackId": "{{track1Id}}",
  "repoUrl": "https://github.com/org/sp23-repo",
  "demoUrl": "https://demo.example.com",
  "reportUrl": "https://docs.example.com/report",
  "slideUrl": "https://slides.example.com/deck"
}
```

**Response `201` — `data`:**

```json
{
  "id": 301,
  "teamId": 55,
  "trackId": 8,
  "status": "SUBMITTED"
}
```

### GĐ3-4 — Chấm điểm (Judge)

```http
POST /api/v1/scores
Authorization: Bearer {{judgeToken}}
```

```json
{
  "submissionId": 301,
  "criterionId": 41,
  "scoreValue": 8.5,
  "comment": "Good implementation",
  "scoreType": "NORMAL"
}
```

**Response `201` — `data`:** có `id`, `submissionId`, `criterionId`, `scoreValue`.

### GĐ3-5 — Tiến độ chấm

```http
GET /api/v1/rounds/{{prelimRoundId}}/scoring-progress
Authorization: Bearer {{coordToken}}
```

**Response `200` — `data` (rút gọn):**

```json
{
  "roundId": 12,
  "totalTeams": 23,
  "scoredTeams": 18,
  "completionPct": 78.26
}
```

### GĐ3-6 — Khóa chấm Sơ loại

```http
PATCH /api/v1/rounds/{{prelimRoundId}}/lock-scoring
Authorization: Bearer {{coordToken}}
```

```json
{ "force": false, "reason": null }
```

**Response `200` — `data.scoringLocked`:** `true`.

### GĐ3-7 — Ranking Sơ loại

```http
GET /api/v1/rounds/{{prelimRoundId}}/ranking
Authorization: Bearer {{coordToken}}
```

**Response `200` — `data` (rút gọn):**

```json
[
  {
    "teamId": 10,
    "teamName": "SP23-T01",
    "trackId": 8,
    "trackName": "Track 1",
    "totalScore": 8.75,
    "rankInTrack": 1
  }
]
```

**Checkpoint GĐ3:** prelim `scoringLocked=true`, ranking có dữ liệu → chuyển GĐ4.

---

## GĐ4 — Advance & kích hoạt Chung kết

**Hackathon:** `seal-spring-2026-gd4`  
**Student mẫu:** `student.sp25.t01.leader@fpt.edu.vn`

### GĐ4-1 — Ranking preview (đã lock)

```http
GET /api/v1/rounds/{{prelimRoundId}}/ranking
Authorization: Bearer {{coordToken}}
```

→ 25 dòng, top N mỗi track theo seed (9+8+8).

### GĐ4-2 — Wildcard candidates (nếu có)

```http
GET /api/v1/rounds/{{prelimRoundId}}/wildcard-candidates
Authorization: Bearer {{coordToken}}
```

**Response `200` — `data` (rút gọn):**

```json
[
  {
    "reviewId": 1,
    "teamId": 62,
    "teamName": "SP25-T08",
    "status": "PENDING"
  }
]
```

```http
PATCH /api/v1/wildcard-reviews/1
Authorization: Bearer {{coordToken}}
```

```json
{
  "approved": true,
  "coordinatorNote": "Approved wildcard"
}
```

### GĐ4-3 — Publish Sơ loại

```http
PATCH /api/v1/rounds/{{prelimRoundId}}/publish
Authorization: Bearer {{coordToken}}
```

**Response `200` — `data.isPublished`:** `true`.

### GĐ4-4 — Advance teams

```http
POST /api/v1/rounds/{{prelimRoundId}}/advance
Authorization: Bearer {{coordToken}}
```

```json
{
  "advancedTeamIds": [10, 11, 12, 13, 14, 15, 16, 17],
  "eliminatedTeamIds": [18, 19],
  "note": "Advance top + wildcard"
}
```

**Response `200` — `data`:** danh sách team `status: "ADVANCED"`.

### GĐ4-5 — Gán Judge CK

```http
POST /api/v1/rounds/{{finalRoundId}}/judge-assignments
Authorization: Bearer {{coordToken}}
```

```json
{
  "judgeIds": [4]
}
```

**Response `200` — `data`:**

```json
{
  "roundId": 14,
  "judgeIds": [4]
}
```

### GĐ4-6 — Readiness FINAL_ROUND

```http
GET /api/v1/hackathons/{{hackathonId}}/readiness?target=FINAL_ROUND
Authorization: Bearer {{coordToken}}
```

**Kỳ vọng happy:**

```json
{
  "ready": true,
  "targetStatus": "FINAL_ROUND",
  "blockers": []
}
```

### GĐ4-7 — Activate Chung kết

```http
PATCH /api/v1/rounds/{{finalRoundId}}/activate
Authorization: Bearer {{coordToken}}
```

```json
{ "note": "Start final round" }
```

**Response `200` — `data.isActive`:** `true`.

**Checkpoint GĐ4:** CK active, đội ADVANCED sẵn sàng nộp bài GĐ5.

---

## GĐ5 — Chung kết

**Hackathon:** `seal-spring-2026-gd5`  
**Student mẫu:** `student.sp24.t01.leader@fpt.edu.vn`

### GĐ5-1 — Nộp bài CK

```http
POST /api/v1/submissions
Authorization: Bearer {{studentToken}}
```

```json
{
  "teamId": "{{teamId}}",
  "roundId": "{{finalRoundId}}",
  "repoUrl": "https://github.com/org/final-sp24",
  "demoUrl": "https://demo.example.com/final",
  "reportUrl": "https://docs.example.com/final-report",
  "slideUrl": "https://slides.example.com/final"
}
```

**Response `201` — `data`:**

```json
{
  "id": 501,
  "teamId": 80,
  "roundId": 20,
  "trackId": null,
  "status": "SUBMITTED"
}
```

### GĐ5-2 — Chấm CK (Guest Judge)

```http
POST /api/v1/scores
Authorization: Bearer {{judgeToken}}
```

```json
{
  "submissionId": 501,
  "criterionId": 201,
  "scoreValue": 9.0,
  "comment": "Strong final demo",
  "scoreType": "NORMAL"
}
```

### GĐ5-3 — Khóa chấm CK

```http
PATCH /api/v1/rounds/{{finalRoundId}}/lock-scoring
Authorization: Bearer {{coordToken}}
```

```json
{ "force": false, "reason": null }
```

**Response `200` — `data.scoringLocked`:** `true`.

### GĐ5-4 — Hackathon → PENDING_CONFIRM

```http
GET /api/v1/hackathons/{{hackathonId}}
Authorization: Bearer {{coordToken}}
```

**Kỳ vọng happy:**

```json
{
  "id": 6,
  "slug": "seal-spring-2026-gd5",
  "status": "PENDING_CONFIRM",
  "name": "SEAL Spring 2026 — GĐ5"
}
```

**Checkpoint GĐ5:** status `PENDING_CONFIRM` → chuyển GĐ6.

---

## GĐ6 — Kết thúc, RBL, trao giải

**Hackathon:** `seal-spring-2026-gd6`  
**Trạng thái seed:** `PENDING_CONFIRM`  
**Student mẫu:** `student.sp30.t01.leader@fpt.edu.vn`

### GĐ6-1 — Readiness AWARDS

```http
GET /api/v1/hackathons/{{hackathonId}}/readiness?target=AWARDS
Authorization: Bearer {{coordToken}}
```

**Kỳ vọng happy:**

```json
{
  "ready": true,
  "targetStatus": "AWARDS",
  "blockers": []
}
```

### GĐ6-2 — Kết quả chung cuộc (team rankings)

```http
GET /api/v1/hackathons/{{hackathonId}}/team-rankings
Authorization: Bearer {{coordToken}}
```

**Response `200` — `data` (mẫu, seed ~10 đội CK):**

```json
[
  {
    "rank": 1,
    "teamId": 101,
    "teamName": "SP30-T01",
    "chapterId": 1,
    "chapterName": "FPT-HCM",
    "weightedAvgScore": 9.12,
    "judgeCount": 2
  },
  {
    "rank": 2,
    "teamId": 102,
    "teamName": "SP30-T02",
    "chapterId": 2,
    "chapterName": "FPT-HN",
    "weightedAvgScore": 8.85,
    "judgeCount": 2
  }
]
```

### GĐ6-3 — Chapter rankings

```http
GET /api/v1/hackathons/{{hackathonId}}/chapter-rankings
Authorization: Bearer {{coordToken}}
```

**Response `200` — `data` (rút gọn):**

```json
[
  {
    "rank": 1,
    "chapterId": 1,
    "chapterName": "FPT-HCM",
    "totalScore": 45.6,
    "teamCount": 5
  }
]
```

### GĐ6-4 — RBL Dashboard

```http
GET /api/v1/rounds/{{finalRoundId}}/rbl/progress
Authorization: Bearer {{coordToken}}
```

**Response `200` — `data`:**

```json
{
  "roundId": 25,
  "totalSubmissions": 10,
  "scoredSubmissions": 10,
  "completionPct": 100.0
}
```

```http
GET /api/v1/rounds/{{finalRoundId}}/rbl/variance
Authorization: Bearer {{coordToken}}
```

**Response `200` — `data`:** mảng variance theo criterion (có phần tử nếu seed đủ điểm).

### GĐ6-5 — Trao giải

```http
POST /api/v1/hackathons/{{hackathonId}}/prizes
Authorization: Bearer {{coordToken}}
```

```json
{
  "roundId": "{{finalRoundId}}",
  "teamId": 102,
  "prizeName": "Giải Nhì",
  "prizeRank": "SECOND",
  "prizeValue": "5000000 VND",
  "description": "Runner-up"
}
```

**Response `201` — `data`:**

```json
{
  "id": 8,
  "hackathonId": 7,
  "roundId": 25,
  "teamId": 102,
  "teamName": "SP30-T02",
  "prizeName": "Giải Nhì",
  "prizeRank": "SECOND"
}
```

```http
GET /api/v1/hackathons/{{hackathonId}}/prizes
Authorization: Bearer {{coordToken}}
```

→ Danh sách giải đã trao (seed + mới tạo).

### GĐ6-6 — Confirm FINISHED

```http
PATCH /api/v1/hackathons/{{hackathonId}}/confirm
Authorization: Bearer {{coordToken}}
```

```json
{
  "confirm": true,
  "note": "BTC xác nhận kết quả cuối"
}
```

**Response `200` — `data.status`:** `"FINISHED"`.

### GĐ6-7 — Student xem kết quả

```http
GET /api/v1/me/hackathons/{{hackathonId}}/rankings
Authorization: Bearer {{studentToken}}
```

**Response `200` — `data`:** ranking team CK (cấu trúc tương tự GĐ6-2).

**Checkpoint GĐ6:** Hackathon `FINISHED`, team-rankings + chapter-rankings + prizes hiển thị trên FE.

---

## Checklist happy path (đánh dấ `[x]`)

| # | GĐ | API chính | Kỳ vọng |
|---|-----|-----------|---------|
| 1 | GĐ1 | `readiness?target=ONGOING` | `ready: true` |
| 2 | GĐ2 | `POST /me/hackathons/{id}/register` | 200 |
| 3 | GĐ2 | `GET .../matchmaking` | Mảng đội thiếu người |
| 4 | GĐ2 | `POST .../admin-add-member` | Đội → `ACTIVE` (3 người) |
| 5 | GĐ2 | `PATCH .../lottery` | `assignedCount` > 0 |
| 6 | GĐ3 | `PATCH .../activate` prelim | `isActive: true` |
| 7 | GĐ3 | `POST /submissions` + `POST /scores` | `SUBMITTED` + score |
| 8 | GĐ3 | `lock-scoring` + `ranking` | `scoringLocked`, có ranking |
| 9 | GĐ4 | `publish` + `advance` | teams ADVANCED |
| 10 | GĐ4 | `readiness FINAL_ROUND` + activate CK | `ready: true` |
| 11 | GĐ5 | CK submit + score + lock | `PENDING_CONFIRM` |
| 12 | GĐ6 | `team-rankings` + `chapter-rankings` | Mảng không rỗng |
| 13 | GĐ6 | `rbl/progress` | `completionPct` ~ 100 |
| 14 | GĐ6 | `prizes` + `confirm` | `FINISHED` |

---

## Ghi chú khi so response

1. **ID số** (`hackathonId`, `teamId`, …) thay đổi theo DB — chỉ so **cấu trúc field** và **status**.
2. Mọi GĐ dùng **một** slug `seal-e2e-2026` — xem [`dev-seed-guide.md`](./dev-seed-guide.md).
3. Chi tiết API đầy đủ 166 endpoint: [`full-workflow-api-test-gd1-gd6.md`](./full-workflow-api-test-gd1-gd6.md).  
4. Test FE God Mode: [`fe-god-mode-e2e-test-flow.md`](./fe-god-mode-e2e-test-flow.md).
