# Hướng dẫn tích hợp API MF-03 (GĐ3 → GĐ5) cho Frontend

Tài liệu này mô tả **những gì FE cần gọi** khi tích hợp Thi & chấm (GĐ3), Chuyển vòng (GĐ4), Chung kết (GĐ5). Mỗi API có **request/response JSON mẫu** — FE không cần đọc code Java.

**Base URL:** `http://localhost:8080/api/v1`  
**Auth:** Bearer JWT — xem [mf02/fe-auth-integration.md](../mf02/fe-auth-integration.md)  
**Mock UI trước khi BE xong logic:** [11-fe-mock-landing-gd3-gd5.md](11-fe-mock-landing-gd3-gd5.md)  
**Luồng từng bước:** [07-fe-api-flow-gd3.md](07-fe-api-flow-gd3.md) · [08-fe-api-flow-gd4.md](08-fe-api-flow-gd4.md)

> **Phạm vi:** FR-15 … FR-30A. GĐ6 (trao giải, confirm) → [10-fe-api-flow-gd6.md](10-fe-api-flow-gd6.md).

---

## 1. Envelope response chung

### Thành công (2xx)

```json
{
  "success": true,
  "data": { },
  "warnings": [ ],
  "message": "Optional",
  "traceId": "...",
  "timestamp": "2026-06-10T14:00:00Z"
}
```

- `warnings[]` có thể xuất hiện trên lock scoring, ranking preview, assign judge CK — **vẫn là 2xx**, FE hiển thị toast/banner.
- Response là **mảng** khi `data` là list (vd ranking, submissions).

### Lỗi (4xx/5xx)

```json
{
  "success": false,
  "error": {
    "code": "ROUND_NOT_ACTIVE",
    "message": "...",
    "status": 422,
    "details": { }
  },
  "traceId": "...",
  "timestamp": "..."
}
```

FE branch theo `error.code`, không parse `message` để logic.

---

## 2. Trạng thái BE (cột “BE” trong bảng)

| Ký hiệu | Ý nghĩa | FE |
|---------|---------|-----|
| ✅ | Logic đã có | Gọi BE trực tiếp |
| 🔶 | Route có, thiếu side effect | Tích hợp + handle edge |
| ⏳ | Stub — thường `200` + `data: []` | Dùng mock [11-fe-mock](11-fe-mock-landing-gd3-gd5.md) |
| — | Public, không JWT | Bỏ header Authorization |

Theo dõi khi BE cập nhật: [09-be-backlog-gd4-gd5-gd6.md](09-be-backlog-gd4-gd5-gd6.md).

---

## 3. Danh sách API (GĐ3 → GĐ5)

| # | Method | Path | Auth | BE | Mô tả `data` trả về |
|---|--------|------|------|-----|---------------------|
| 1 | PATCH | `/rounds/{id}/activate` | Coord | ✅ | Tóm tắt vòng (§4.1) |
| 2 | PATCH | `/rounds/{id}/release-problem` | Coord | ✅ | Tóm tắt vòng (§4.1) |
| 3 | POST | `/submissions` | Student | ✅ SL / ⏳ CK | Một bài nộp (§4.2) |
| 4 | GET | `/submissions` | All* | ✅ | Mảng bài nộp (§4.2) |
| 5 | PATCH | `/submissions/{id}/review-late` | Coord | ✅ | Một bài nộp (§4.2) |
| 6 | POST | `/scores` | Judge | ✅ | Một điểm chấm (§4.3) |
| 7 | POST | `/scores/calibration` | Judge | ⏳ | Một điểm chấm (§6.2) |
| 8 | PATCH | `/rounds/{id}/lock-scoring` | Coord | ✅ / 🔶 CK | Vòng + warnings (§4.4) |
| 9 | GET | `/rounds/{id}/scoring-progress` | Coord | ✅ | Tiến độ chấm SL (§4.5) |
| 10 | GET | `/rounds/{id}/ranking/preview` | Coord | ✅ | Mảng hạng + warnings (§4.6) |
| 11 | GET | `/rounds/{id}/ranking` | Coord | ✅ / ⏳ CK | Mảng hạng (§4.6) |
| 12 | PATCH | `/teams/{teamId}/eliminate` | Coord | ✅ | Thông tin đội (§4.7) |
| 13 | PATCH | `/rounds/{id}/publish` | Coord | ✅ | Tóm tắt vòng (§4.1) |
| 14 | GET | `/rounds/{id}/tiebreak` | Coord | ⏳ | Mảng nhóm đồng hạng (§5.2) |
| 15 | POST | `/rounds/{id}/tiebreak/resolve` | Coord | ⏳ | Mảng hạng sau xử lý (§5.2) |
| 16 | GET | `/rounds/{id}/wildcard-candidates` | Coord | ⏳ | Mảng ứng viên WC (§5.3) |
| 17 | PATCH | `/wildcard-reviews/{id}` | Coord | ⏳ | Kết quả duyệt WC (§5.3) |
| 18 | POST | `/rounds/{id}/advance` | Coord | ✅ | Danh sách đội qua/về (§5.1) |
| 19 | POST | `/rounds/{id}/judge-assignments` | Coord | ✅ | Judge CK + warnings (§5.4) |
| 20 | GET | `/rounds/{id}/scoreboard` | — | ⏳ | Bảng điểm public (§5.5) |
| 21 | POST | `/calibration-sessions` | Coord | ⏳ | Phiên calibration (§6.2) |
| 22 | PATCH | `/calibration-sessions/{id}` | Coord | ⏳ | Phiên calibration (§6.2) |
| 23 | GET | `/calibration-sessions?roundId=` | Coord | ⏳ | Mảng phiên (§6.2) |
| 24 | GET | `/rounds/{id}/rbl/variance` | Coord | ⏳ | Mảng độ lệch judge (§6.3) |
| 25 | GET | `/rounds/{id}/rbl/progress` | Coord | ⏳ | Tiến độ chấm CK/RBL (§6.3) |
| 26 | GET | `/teams/{teamId}/journey` | Auth | ⏳ | Timeline đội (§4.8) |
| 27 | WS | `/ws` + 3 topic | Auth | ✅ GĐ3 | Payload JSON (§7) |
| 28 | GET | `/presentation/queue?roundId=&trackId=` | Auth | ✅ GĐ3/GĐ5 | Queue + timer block (§4.9) |
| 29 | POST | `/presentation/queue/shuffle` | Coord/Judge | ✅ | Shuffle slots (§4.9) |
| 30 | GET/PUT/DELETE | `/presentation/duration` | Coord | ✅ | Cấu hình phút thuyết trình/Q&A (§4.9) |
| 31 | POST | `/presentation/timer/{action}` | Judge/Coord | ✅ | Start/pause/resume/qa/reset/next (§4.9) |

\* Student: bắt buộc `?teamId=`; Judge: bắt buộc `?roundId=` + assigned.

**Deprecated (không dùng):** `PATCH /submissions/{id}/resubmit` → `POST /submissions`; `PATCH /submissions/{id}/review` → `review-late`; `POST .../wildcard/approve|reject` → `PATCH /wildcard-reviews/{id}`; `POST .../advance-teams` → `POST .../advance`.

**GĐ1 — field timer trên CRUD (thiết lập sớm):** `GET/PUT /rounds/{id}` (`defaultPresentationMinutes`, `defaultQaMinutes`); `GET/PUT /tracks/{id}` (`presentationMinutes`, `qaMinutes`). Xem [fe-gd1-gd2-structure-and-fields.md](../testing/fe-gd1-gd2-structure-and-fields.md).

---

## 4. GĐ3 — Sơ loại (Thi & chấm)

### 4.1 Kích hoạt vòng / phát đề / publish (`PATCH`)

**Activate** `PATCH /rounds/1/activate`

Request (body optional):

```json
{ "note": "Mở vòng Sơ loại" }
```

**Release đề** `PATCH /rounds/1/release-problem`

```json
{ "problemStatementUrl": "https://drive.google.com/file/d/example/view" }
```

**Response `200` — `data` (dùng chung cho activate, release-problem, publish):**

```json
{
  "id": 1,
  "name": "Vòng Sơ loại",
  "examAt": "2026-06-01T08:00:00",
  "submissionDeadline": "2026-06-10T23:59:59",
  "isActive": true,
  "scoringLocked": false,
  "isPublished": false,
  "trackCount": 3,
  "criteriaCount": 5,
  "currentWeightTotal": 1.0
}
```

**Chi tiết vòng (đọc màn dashboard):** `GET /rounds/1`

```json
{
  "id": 1,
  "hackathonId": 1,
  "trackId": null,
  "name": "Vòng Sơ loại",
  "examAt": "2026-06-01T08:00:00",
  "isFinal": false,
  "roundType": "PRELIMINARY",
  "lateSubmissionPolicy": "ALLOW_WITH_REVIEW",
  "submissionOpen": "2026-06-01T09:00:00",
  "submissionDeadline": "2026-06-10T23:59:59",
  "codingDurationHours": 48,
  "problemStatementUrl": "https://drive.google.com/file/d/example/view",
  "problemReleasedAt": "2026-06-01T09:00:00",
  "topNAdvance": 2,
  "wildcardEnabled": true,
  "minTeamsFinal": 6,
  "tiebreakRule": "SUBMISSION_TIME",
  "isActive": true,
  "activatedAt": "2026-06-01T08:00:00",
  "scoringLocked": false,
  "scoringLockedAt": null,
  "isPublished": false,
  "publishedAt": null,
  "createdAt": "2026-05-20T10:00:00"
}
```

---

### 4.2 Nộp bài & duyệt trễ

**Nộp Sơ loại** `POST /submissions` — bắt buộc `teamId` + `trackId`

```json
{
  "teamId": 4,
  "trackId": 1,
  "repoUrl": "https://github.com/seal-warriors/demo",
  "demoUrl": "https://demo.example.com",
  "slideUrl": "https://slides.example.com/deck",
  "lateReason": null
}
```

**Response `200`/`201` — `data`:**

```json
{
  "id": 100,
  "teamId": 4,
  "trackId": 1,
  "roundId": null,
  "repoUrl": "https://github.com/seal-warriors/demo",
  "demoUrl": "https://demo.example.com",
  "reportUrl": null,
  "slideUrl": "https://slides.example.com/deck",
  "status": "SUBMITTED",
  "isLate": false,
  "lateReason": null,
  "reviewedBy": null,
  "reviewedAt": null,
  "reviewNote": null,
  "submittedAt": "2026-06-10T14:00:00"
}
```

**Status badge:**

| `status` | UI |
|----------|-----|
| `SUBMITTED` | Đã nộp |
| `LATE_PENDING` | Chờ duyệt trễ |
| `LATE_APPROVED` | Nộp trễ — được chấm |
| `REJECTED` | Từ chối |

**Danh sách bài** `GET /submissions?teamId=4&roundId=1` → `data` là **mảng** cùng shape trên.

**Duyệt trễ** `PATCH /submissions/100/review-late`

```json
{ "decision": "APPROVE", "note": "Lý do hợp lệ" }
```

`decision`: `APPROVE` | `REJECT`. Response `data` = một object bài nộp (shape trên).

---

### 4.3 Chấm điểm

**Ghi điểm** `POST /scores`

```json
{
  "submissionId": 100,
  "criterionId": 5,
  "scoreValue": 8.5,
  "comment": "Tốt",
  "scoreType": "NORMAL"
}
```

**Response `200` — `data`:**

```json
{
  "id": 501,
  "submissionId": 100,
  "judgeId": 201,
  "criterionId": 5,
  "scoreValue": 8.5,
  "comment": "Tốt",
  "scoreType": "NORMAL",
  "isFinal": false,
  "calibrationSessionId": null,
  "scoredAt": "2026-06-10T15:30:00",
  "updatedAt": "2026-06-10T15:30:00"
}
```

Sau lock scoring: `isFinal: true`.

**Tiêu chí form chấm:** `GET /tracks/{trackId}/criteria` → `data.items[]` mỗi phần tử:

```json
{
  "id": 5,
  "trackId": 1,
  "roundId": null,
  "name": "Technical Excellence",
  "description": "...",
  "type": "NUMERIC",
  "weight": 0.3,
  "maxScore": 10,
  "displayOrder": 1
}
```

---

### 4.4 Khóa chấm

`PATCH /rounds/1/lock-scoring`

```json
{ "force": false, "reason": null }
```

**Response `200` — `data`:**

```json
{
  "round": {
    "id": 1,
    "name": "Vòng Sơ loại",
    "examAt": "2026-06-01T08:00:00",
    "submissionDeadline": "2026-06-10T23:59:59",
    "isActive": true,
    "scoringLocked": true,
    "isPublished": false,
    "trackCount": 3,
    "criteriaCount": 5,
    "currentWeightTotal": 1.0
  },
  "warnings": [
    {
      "code": "PARTIAL_SCORING_BEFORE_LOCK",
      "message": "3 submission chưa được chấm đủ"
    }
  ]
}
```

---

### 4.5 Tiến độ chấm (REST)

`GET /rounds/1/scoring-progress`

```json
{
  "roundId": 1,
  "totalSubmissions": 12,
  "scoredSubmissions": 9,
  "pendingSubmissions": 3,
  "scoringLocked": false
}
```

---

### 4.6 Xếp hạng

**Preview (trước lock)** `GET /rounds/1/ranking/preview`

```json
{
  "success": true,
  "data": [
    {
      "rank": 1,
      "teamId": 4,
      "teamName": "Seal Warriors",
      "trackId": 1,
      "assignedGroup": "A",
      "totalScore": 8.75,
      "tiebreakRequired": false
    },
    {
      "rank": 2,
      "teamId": 7,
      "teamName": "Byte Masters",
      "trackId": 1,
      "assignedGroup": "A",
      "totalScore": 8.70,
      "tiebreakRequired": true
    }
  ],
  "warnings": [
    {
      "code": "INCOMPLETE_SCORING_IN_RANKING",
      "message": "Còn 2 bài chưa chấm đủ tiêu chí"
    }
  ]
}
```

**Sau lock** `GET /rounds/1/ranking` → `data` cùng shape mảng (thường không warnings).

---

### 4.7 Loại đội

`PATCH /teams/6/eliminate`

```json
{ "reason": "Vi phạm quy chế thi" }
```

**Response `data`:**

```json
{
  "id": 6,
  "hackathonId": 1,
  "teamName": "E2E-T06 Dropped",
  "leaderId": 106,
  "chapterId": 1,
  "status": "ELIMINATED",
  "isLocked": true,
  "createdAt": "2026-05-15T10:00:00"
}
```

---

### 4.8 Timeline đội ⏳

`GET /teams/4/journey`

```json
{
  "teamId": 4,
  "teamName": "Seal Warriors",
  "steps": [
    {
      "roundId": 1,
      "roundName": "Vòng Sơ loại",
      "trackId": 1,
      "trackName": "Track A",
      "participationStatus": "ADVANCED"
    },
    {
      "roundId": 2,
      "roundName": "Chung kết",
      "trackId": null,
      "trackName": null,
      "participationStatus": "ACTIVE"
    }
  ]
}
```

---

## 5. GĐ4 — Chuyển vòng → Chung kết

### 5.1 Publish & advance ✅

**Publish** `PATCH /rounds/1/publish` — body optional `{ "note": "..." }`.  
Response `data` = tóm tắt vòng (§4.1) với `isPublished: true`.

**Advance** `POST /rounds/1/advance`

```json
{
  "advancedTeamIds": [4, 7, 9],
  "eliminatedTeamIds": [6, 8],
  "note": "Top 2 mỗi bảng + wild card"
}
```

**Response `data`:**

```json
{
  "roundId": 1,
  "advancedTeamIds": [4, 7, 9],
  "eliminatedTeamIds": [6, 8]
}
```

---

### 5.2 Tiebreak ⏳

**Danh sách đồng hạng** `GET /rounds/1/tiebreak`

```json
[
  {
    "partitionKey": "track:1:group:A",
    "cutoffRank": 2,
    "candidateTeamIds": [4, 7]
  }
]
```

**Giải quyết** `POST /rounds/1/tiebreak/resolve`

```json
{
  "orderedTeamIds": [4, 7],
  "note": "Team 4 nộp sớm hơn"
}
```

Response `data` = mảng hạng (shape §4.6).

---

### 5.3 Wild card ⏳

**Ứng viên** `GET /rounds/1/wildcard-candidates`

```json
{
  "success": true,
  "data": [
    {
      "teamId": 8,
      "teamName": "Almost There",
      "totalScore": 7.85,
      "reason": "Xếp hạng 3 bảng A — thiếu slot CK"
    }
  ],
  "warnings": [
    {
      "code": "MIN_TEAMS_NOT_REACHED",
      "message": "Chưa đủ min_teams_final"
    }
  ]
}
```

**Duyệt** `PATCH /wildcard-reviews/1`

```json
{ "approved": true, "coordinatorNote": "BTC duyệt wild card" }
```

**Response `data`:**

```json
{
  "id": 1,
  "roundId": 1,
  "teamId": 8,
  "avgScore": 7.85,
  "coordinatorApproved": true,
  "coordinatorNote": "BTC duyệt wild card",
  "reviewedAt": "2026-06-11T11:00:00"
}
```

---

### 5.4 Gán judge Chung kết ✅

`POST /rounds/2/judge-assignments`

```json
{ "judgeIds": [201, 202, 203] }
```

**Response `200` — `data`:**

```json
{
  "roundId": 2,
  "judgeIds": [201, 202, 203],
  "warnings": [
    {
      "code": "JUDGE_PARTICIPATED_IN_PRELIM",
      "message": "Judge #201 đã chấm Sơ loại — cân nhắc đổi"
    }
  ]
}
```

**Activate CK** `PATCH /rounds/2/activate` → `data` tóm tắt vòng (§4.1), `isActive: true`.

---

### 5.5 Bảng điểm public ⏳

`GET /rounds/1/scoreboard` — **không** gửi `Authorization`.

```json
{
  "roundId": 1,
  "roundName": "Vòng Sơ loại",
  "ranking": [
    {
      "rank": 1,
      "teamId": 4,
      "teamName": "Seal Warriors",
      "trackId": 1,
      "assignedGroup": "A",
      "totalScore": 8.75,
      "tiebreakRequired": false
    }
  ]
}
```

---

## 6. GĐ5 — Chung kết

### 6.1 Nộp bài CK ⏳

`POST /submissions` — **không** `trackId`, **có** `roundId` (vòng FINAL):

```json
{
  "teamId": 4,
  "roundId": 2,
  "repoUrl": "https://github.com/seal-warriors/final",
  "demoUrl": "https://final-demo.example.com",
  "reportUrl": "https://report.example.com/final.pdf"
}
```

Response `data`: shape §4.2 với `trackId: null`, `roundId: 2`.

**Tiêu chí CK:** `GET /rounds/2/criteria` (không qua track).

**Chấm CK** `POST /scores` — cùng request §4.3; judge phải `FINAL_EXTERNAL` và assigned round FINAL (BE 🔶).

**Lock CK** `PATCH /rounds/2/lock-scoring` — response §4.4; side effect 🔶: `GET /hackathons/1` → `status: "PENDING_CONFIRM"`.

```json
{ "id": 1, "name": "SEAL E2E 2026", "status": "PENDING_CONFIRM" }
```

---

### 6.2 Calibration ⏳

**Tạo phiên** `POST /calibration-sessions`

```json
{
  "roundId": 2,
  "sampleSubmissionId": 150,
  "targetScore": 8.0,
  "instructions": "Chấm thử bài mẫu trước khi chấm chính thức"
}
```

**Response `data`:**

```json
{
  "id": 1,
  "roundId": 2,
  "sampleSubmissionId": 150,
  "status": "OPEN",
  "targetScore": 8.0,
  "instructions": "Chấm thử bài mẫu trước khi chấm chính thức",
  "startedAt": "2026-06-14T08:00:00",
  "endedAt": null,
  "createdById": 1
}
```

`status`: `OPEN` | `CLOSED`.

**Đóng phiên** `PATCH /calibration-sessions/1`

```json
{ "status": "CLOSED" }
```

**Danh sách** `GET /calibration-sessions?roundId=2` → `data` mảng cùng shape.

**Chấm calibration** `POST /scores/calibration`

```json
{
  "calibrationSessionId": 1,
  "submissionId": 150,
  "criterionId": 10,
  "scoreValue": 7.5,
  "comment": "Calibration lần 1"
}
```

Response `data` = shape §4.3.

---

### 6.3 RBL Dashboard ⏳

**Variance** `GET /rounds/2/rbl/variance`

```json
[
  {
    "criterionId": 10,
    "criterionName": "Technical Excellence",
    "criterionType": "NUMERIC",
    "judgeId": 201,
    "judgeType": "FINAL_EXTERNAL",
    "meanScore": 7.8,
    "stdDev": 1.2
  }
]
```

**Tiến độ RBL** `GET /rounds/2/rbl/progress`

```json
{
  "roundId": 2,
  "totalSubmissions": 6,
  "scoredSubmissions": 4,
  "completionPct": 66.67
}
```

---

## 7. WebSocket live scoring (GĐ3 ✅)

**Connect:** SockJS `http://localhost:8080/ws` — gửi Bearer tại frame STOMP CONNECT.

| Subscribe | Payload nhận được |
|-----------|-------------------|
| `/topic/rounds/{roundId}/leaderboard-preview` | Mảng hạng (§4.6) |
| `/topic/rounds/{roundId}/scoring-progress` | Object tiến độ (§4.5) |
| `/topic/tracks/{trackId}/score-saved` | Echo điểm vừa lưu (shape §4.3) |

Fallback nếu WS lỗi: poll `GET /rounds/{id}/scoring-progress` mỗi 10–15s. Chi tiết: [06-live-scoring-websocket.md](06-live-scoring-websocket.md).

---

## 8. API đọc hỗ trợ (GĐ1/GĐ2)

FE gọi khi load màn — không implement BE mới.

| Method | Path | Dùng khi |
|--------|------|----------|
| GET | `/users/me` | Profile sau login |
| GET | `/teams/{teamId}` | Dashboard đội |
| GET | `/teams?hackathonId=` | Danh sách đội |
| GET | `/hackathons/{id}` | Phase hackathon |
| GET | `/hackathons/{id}/readiness` | Pre-flight Coordinator |
| GET | `/hackathons/{hackathonId}/rounds` | Chọn vòng |
| GET | `/rounds/{id}` | Chi tiết vòng (§4.1) |
| GET | `/hackathons/{hackathonId}/tracks` | Track sau lottery |
| GET | `/tracks/{trackId}/criteria` | Form chấm SL |
| GET | `/rounds/{roundId}/criteria` | Form chấm CK |
| GET | `/tracks/{trackId}/judges` | Panel judge track |
| GET | `/rounds/{roundId}/judges` | Panel judge round |
| POST | `/users/temp-judges` | Tạo judge khách CK |

Teams/lottery: [mf02/03-api-reference-gd2.md](../mf02/03-api-reference-gd2.md).

---

## 9. Lỗi thường gặp

| `error.code` | HTTP | FE |
|--------------|------|-----|
| `ROUND_NOT_ACTIVE` | 422 | Ẩn nút nộp |
| `SCORING_LOCKED` | 423 | Khóa form judge |
| `SUBMISSION_NOT_GRADABLE` | 422 | Không chấm LATE_PENDING |
| `ROUND_NOT_SCORING_LOCKED` | 422 | Redirect preview |
| `RESULT_NOT_PUBLISHED` | 422 | Chặn activate CK |
| `TIEBREAK_REQUIRED` | 422 | Màn tiebreak |
| `JUDGE_NOT_ASSIGNED` | 422 | Toast thiếu judge |

**Warnings (2xx):** `PARTIAL_SCORING_BEFORE_LOCK`, `INCOMPLETE_SCORING_IN_RANKING`, `MIN_TEAMS_NOT_REACHED`, `JUDGE_PARTICIPATED_IN_PRELIM`, `MIN_FINAL_JUDGES_NOT_MET`.

Ma trận đầy đủ: [api-authorization-matrix.md](../api-authorization-matrix.md).

---

## 10. Phân quyền tóm tắt

| Nhóm | STUDENT | JUDGE | COORDINATOR | Public |
|------|---------|-------|-------------|--------|
| POST `/submissions` | ✅ | — | — | — |
| GET `/submissions` | ✅* | ✅** | ✅ | — |
| POST `/scores` | — | ✅ | — | — |
| Round progression | — | — | ✅ | — |
| GET ranking/preview, WS | — | ✅*** | ✅ | — |
| GET `/scoreboard` | — | — | — | ✅ |

\* `?teamId=` bắt buộc. \** `?roundId=` + assigned. \*** Subscribe khi assigned.

---

## 11. Happy path QA

| # | GĐ | API |
|---|-----|-----|
| 1 | 3 | `PATCH /rounds/1/activate` |
| 2 | 3 | `PATCH /rounds/1/release-problem` |
| 3 | 3 | `POST /submissions` (có `trackId`) |
| 4 | 3 | `POST /scores` + WS |
| 5 | 3 | `PATCH /rounds/1/lock-scoring` |
| 6 | 3 | `GET /rounds/1/ranking` |
| 7 | 4 | `PATCH /rounds/1/publish` |
| 8 | 4 | `POST /rounds/1/advance` |
| 9 | 4 | `POST /rounds/2/judge-assignments` |
| 10 | 4 | `PATCH /rounds/2/activate` |
| 11 | 5 | `POST /submissions` (có `roundId`, không `trackId`) |
| 12 | 5 | `PATCH /rounds/2/lock-scoring` → hackathon `PENDING_CONFIRM` |

Token mẫu + curl: [04-test-data.md](04-test-data.md) · Seed đội: [mf02/05-test-data-gd2-teams.md](../mf02/05-test-data-gd2-teams.md).
