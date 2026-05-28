# MF-03 GĐ3–GĐ6 — API Reference (cho FE / QA)

**Base:** `http://localhost:8080/api/v1`  
**Auth:** `Authorization: Bearer {{accessToken}}` — [mf02/01-auth-users.md](../mf02/01-auth-users.md)  
**Envelope:** [mf01/api/_conventions.md](../mf01/api/_conventions.md)  
**JSON mẫu:** [04-test-data.md](04-test-data.md)

---

## Trạng thái implement

| Ký hiệu | Ý nghĩa |
|---------|---------|
| ✅ | Logic nghiệp vụ đã có |
| 🔶 | Một phần (vd. list có lọc role, submit chưa) |
| ⏳ | Route + DTO có; service `TODO` — có thể trả 200 với `data` rỗng |

| Endpoint | FR | Trạng thái |
|----------|-----|------------|
| `PATCH /rounds/{id}/activate` | 20/32 | ✅ |
| `PATCH /rounds/{id}/release-problem` | 21 | ⏳ |
| `POST /submissions` | 22/33 | ⏳ |
| `GET /submissions` | — | 🔶 |
| `PATCH /submissions/{id}/resubmit` | 22 | ⏳ |
| `PATCH /submissions/{id}/review` | 25 | ⏳ |
| `POST /scores` | 24/35 | ⏳ |
| `POST /scores/calibration` | 34 | ⏳ |
| `PATCH /rounds/{id}/lock-scoring` | 26/36 | ⏳ |
| `GET /rounds/{id}/scoring-progress` | — | ⏳ |
| `GET /rounds/{id}/ranking` | 27 | ⏳ |
| `GET /rounds/{id}/ranking/preview` | 27 | ⏳ |
| `GET /rounds/{id}/tiebreak` | 28 | ⏳ |
| `POST /rounds/{id}/tiebreak/resolve` | 28 | ⏳ |
| `GET /rounds/{id}/wildcard/candidates` | 29 | ⏳ |
| `POST /rounds/{id}/wildcard/approve` | 29 | ⏳ |
| `POST /rounds/{id}/wildcard/reject` | 29 | ⏳ |
| `POST /rounds/{id}/advance-teams` | 30 | ⏳ |
| `POST /rounds/{id}/judge-assignments` | 31 | ⏳ |
| `GET /rounds/{id}/scoreboard` | — | ⏳ (public ✅ route) |
| `PATCH /hackathons/{id}/status` | GĐ6 | ✅ |
| `POST /hackathons/{id}/prizes` | GĐ6 | ✅ |
| `GET /teams/{teamId}/journey` | — | ⏳ |

**Swagger tags:** Submissions (GĐ3-GĐ5), Scores, Round Progression, Prizes (GĐ6), Teams Journey.

---

## Enum

### `SubmissionStatus`

`SUBMITTED` | `LATE` | `LATE_PENDING` | `LATE_APPROVED` | `REJECTED` | `ACCEPTED`

### `ScoreType`

`NORMAL` | `CALIBRATION` | `PENALTY` (xem `ScoreType.java`)

### `ParticipationStatus` (journey / advance)

`PARTICIPATING` | `ADVANCED` | `ELIMINATED`

### `PrizeRank`

`FIRST` | `SECOND` | `THIRD` | `HONORABLE` | `SPECIAL`

---

## 1. Round — Activate (MF-01, dùng cho FR-20/32)

```http
PATCH /rounds/{id}/activate
Authorization: Bearer <coordinator>
Content-Type: application/json

{ "note": "Mở vòng Sơ loại" }
```

Chi tiết: [mf01/api/fr-06b-activate.md](../mf01/api/fr-06b-activate.md).

---

## 2. Round — Release problem (FR-21)

```http
PATCH /rounds/{id}/release-problem
Authorization: Bearer <coordinator>
```

**Body**

```json
{
  "problemStatementUrl": "https://drive.google.com/..."
}
```

**200 — `data`:** `RoundSummaryResponse` (id, name, examAt, isActive, scoringLocked, …).

---

## 3. Submissions

### 3.1 Nộp bài

```http
POST /submissions
Authorization: Bearer <student-approved>
```

**Body — Sơ loại**

```json
{
  "teamId": 1,
  "trackId": 1,
  "repoUrl": "https://github.com/org/repo",
  "demoUrl": "https://demo.example.com",
  "reportUrl": "https://...",
  "slideUrl": "https://...",
  "lateReason": null
}
```

**Body — Chung kết** (không `trackId`, có `roundId` FINAL)

```json
{
  "teamId": 1,
  "roundId": 2,
  "repoUrl": "https://github.com/org/repo-final"
}
```

**201 — `SubmissionResponse`**

```json
{
  "id": 100,
  "teamId": 1,
  "trackId": 1,
  "roundId": null,
  "repoUrl": "...",
  "status": "SUBMITTED",
  "isLate": false,
  "submittedAt": "2026-06-10T14:00:00"
}
```

### 3.2 Danh sách

```http
GET /submissions?teamId=1&roundId=1
Authorization: Bearer <coord|judge|student>
```

| Role | Query bắt buộc |
|------|----------------|
| STUDENT | `teamId` (đội mình) |
| JUDGE | `roundId` (đã phân công) |
| COORDINATOR | Tùy chọn; không param → `[]` |

### 3.3 Nộp lại

```http
PATCH /submissions/{id}/resubmit
Authorization: Bearer <student-approved>
```

```json
{
  "repoUrl": "https://github.com/org/repo-v2",
  "demoUrl": "https://demo-v2.example.com"
}
```

### 3.4 Duyệt muộn

```http
PATCH /submissions/{id}/review
Authorization: Bearer <coordinator>
```

```json
{
  "approved": true,
  "reviewNote": "Chấp nhận nộp trễ có lý do"
}
```

---

## 4. Scores

### 4.1 Chấm điểm

```http
POST /scores
Authorization: Bearer <judge-approved>
```

```json
{
  "submissionId": 100,
  "criterionId": 5,
  "scoreValue": 8.5,
  "comment": "Tốt",
  "scoreType": "NORMAL"
}
```

### 4.2 Calibration

```http
POST /scores/calibration
Authorization: Bearer <judge-approved>
```

Body tương tự `SubmitCalibrationScoreRequest` (có `calibrationSessionId` khi implement).

---

## 5. Round progression

Base: `/rounds/{id}/...` — **Coordinator** (trừ scoreboard).

### 5.1 Lock scoring

```http
PATCH /rounds/{id}/lock-scoring
```

```json
{
  "force": false,
  "reason": null
}
```

Force lock:

```json
{
  "force": true,
  "reason": "BTC yêu cầu khóa khẩn do thiếu giờ"
}
```

Response có thể kèm `warnings` (vd. `PARTIAL_SCORING_BEFORE_LOCK`).

### 5.2 Scoring progress

```http
GET /rounds/{id}/scoring-progress
```

### 5.3 Ranking

```http
GET /rounds/{id}/ranking
GET /rounds/{id}/ranking/preview
```

**`data`:** `RoundRankingItemResponse[]` — rank, teamId, teamName, totalScore, partition, …

### 5.4 Tiebreak

```http
GET /rounds/{id}/tiebreak
POST /rounds/{id}/tiebreak/resolve
```

**Resolve body** (`ResolveTiebreakRequest`): danh sách team + penalty / quyết định theo `tiebreak_rule`.

### 5.5 Wildcard

```http
GET /rounds/{id}/wildcard/candidates
POST /rounds/{id}/wildcard/approve
POST /rounds/{id}/wildcard/reject
```

**Body** (`WildcardDecisionRequest`): `teamIds`, `note`, …

### 5.6 Advance teams

```http
POST /rounds/{id}/advance-teams
```

```json
{
  "advancedTeamIds": [1, 2, 3],
  "eliminatedTeamIds": [4, 5],
  "note": "Chốt top 2 mỗi bảng"
}
```

### 5.7 Judge Chung kết

```http
POST /rounds/{id}/judge-assignments
```

```json
{
  "judgeIds": [10, 11, 12]
}
```

Có thể dùng thay thế: `POST /judge-assignments` với `roundId` + `assignmentType: FINAL_EXTERNAL` ([mf01/api/fr-05-personnel.md](../mf01/api/fr-05-personnel.md)).

### 5.8 Scoreboard (public)

```http
GET /rounds/{id}/scoreboard
```

**Không** gửi `Authorization` (permitAll trong `JwtSecurityConfig`).

---

## 6. Hackathon — Status & Prizes (GĐ6)

### 6.1 Đổi trạng thái

```http
PATCH /hackathons/{id}/status
Authorization: Bearer <coordinator>
```

```json
{
  "targetStatus": "FINISHED",
  "note": "Kết thúc SEAL 2026"
}
```

Machine: `DRAFT → ONGOING → PENDING_CONFIRM → FINISHED`.  
Chi tiết: [mf01/api/fr-06-status.md](../mf01/api/fr-06-status.md).

### 6.2 Trao giải

```http
POST /hackathons/{hackathonId}/prizes
Authorization: Bearer <coordinator>
```

**Điều kiện:** hackathon `PENDING_CONFIRM`.

```json
{
  "roundId": 2,
  "trackId": null,
  "teamId": 1,
  "prizeName": "Giải Nhất",
  "prizeRank": "FIRST",
  "prizeValue": "7000000",
  "description": "SEAL Spring 2026"
}
```

**409** `PRIZE_DUPLICATE` nếu trùng đội hoặc `prizeRank` trong hackathon.

---

## 7. Team journey

```http
GET /teams/{teamId}/journey
Authorization: Bearer <any authenticated>
```

**200 — `TeamJourneyResponse`**

```json
{
  "teamId": 1,
  "teamName": "Seal Warriors",
  "steps": [
    {
      "roundId": 1,
      "roundName": "Vòng Sơ loại",
      "trackId": 1,
      "trackName": "Track 1",
      "participationStatus": "ADVANCED"
    }
  ]
}
```

---

## 8. MF-03 error codes (trích)

| Code | HTTP | Khi nào |
|------|------|---------|
| `ROUND_NOT_ACTIVE` | 422 | Nộp bài khi round chưa active |
| `SCORING_LOCKED` | 423 | Chấm/sửa sau lock |
| `SUBMISSION_NOT_GRADABLE` | 422 | Status không cho chấm |
| `JUDGE_NOT_ASSIGNED_TO_TRACK` | 403 | Judge sai track |
| `TIEBREAK_REQUIRED` | 422 | Chưa resolve tie |
| `ROUND_NOT_SCORING_LOCKED` | 422 | Ranking trước lock |
| `PRIZE_DUPLICATE` | 409 | Trùng giải |
| `HACKATHON_NOT_PENDING_CONFIRM` | 422 | Trao giải sai phase |

Đầy đủ: `ErrorCode.java`, [01-business-rules-gd3.md](01-business-rules-gd3.md).

---

## 9. MF-03 warning codes

| Code | Khi nào |
|------|---------|
| `PARTIAL_SCORING_BEFORE_LOCK` | Lock khi chưa chấm đủ |
| `INCOMPLETE_SCORING_IN_RANKING` | Ranking thiếu điểm |
| `MIN_TEAMS_NOT_REACHED` | Cần wild card |
| `JUDGE_PARTICIPATED_IN_PRELIM` | Gợi ý đổi judge CK |

`WarningCode.java` — trả trong `warnings[]` của envelope 2xx.
