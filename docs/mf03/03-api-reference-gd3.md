# MF-03 GĐ3–GĐ6 — API Reference (GD03 v4.1)

**Nguồn:** `GD03_05_SEAL_MF_v4_1.docx` · **Base:** `http://localhost:8080/api/v1`  
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

| Endpoint | FR v4.1 | Trạng thái |
|----------|---------|------------|
| `PATCH /rounds/{id}/activate` | 15/25 | ✅ (+ gate publish CK) |
| `PATCH /rounds/{id}/release-problem` | 15A | ✅ |
| `PATCH /rounds/{id}/publish` | 24 | ✅ GĐ4 phase 1 |
| `POST /submissions` | 16 | ✅ (Sơ loại; CK → GĐ5 ⏳) |
| `GET /submissions` | — | ✅ |
| `PATCH /submissions/{id}/review-late` | 16A | ✅ |
| `PATCH /submissions/{id}/review` | 16A | ⚠️ deprecated alias |
| `PATCH /submissions/{id}/resubmit` | 16 | ⚠️ deprecated — upsert POST |
| `POST /scores` | 18/18A | ✅ + WS push |
| `POST /scores/calibration` | 29 | ⏳ GĐ5 |
| `PATCH /rounds/{id}/lock-scoring` | 20A | ✅ |
| `GET /rounds/{id}/ranking/preview` | 20 | ✅ live |
| `GET /rounds/{id}/scoring-progress` | 20A | ✅ |
| `GET /rounds/{id}/ranking` | 20/22 | ✅ (cần lock) |
| `GET /rounds/{id}/wildcard-candidates` | 22A | ⏳ GĐ4 stub |
| `PATCH /wildcard-reviews/{id}` | 22A | ⏳ GĐ4 stub |
| `POST /rounds/{id}/advance` | 22/23 | ✅ GĐ4 phase 1 |
| `POST /rounds/{id}/advance-teams` | 22/23 | ⚠️ deprecated alias |
| `POST /rounds/{id}/judge-assignments` | 27 | ✅ GĐ4 phase 1 (round FINAL) |
| `PATCH /teams/{id}/eliminate` | 21 | ✅ |
| `POST /calibration-sessions` | 29 | ⏳ GĐ5 stub |
| `GET /rounds/{id}/rbl/variance` | 30 | ⏳ GĐ5 stub |
| `GET /rounds/{id}/scoreboard` | 20 | ⏳ GĐ4 |
| **WebSocket `/ws`** | 18A | ✅ — [06-live-scoring-websocket.md](06-live-scoring-websocket.md) |
| `POST /hackathons/{id}/prizes` | FR-32 | ✅ GĐ6 |
| `GET /hackathons/{id}/prizes` | FR-32 | ⏳ GĐ6 stub |
| `DELETE /prizes/{id}` | FR-32 | ⏳ GĐ6 stub |
| `PATCH /hackathons/{id}/confirm` | FR-33 | ⏳ GĐ6 stub |
| `GET /hackathons/{id}/team-rankings` | FR-31/33A | ⏳ GĐ6 stub |
| `GET /hackathons/{id}/chapter-rankings` | FR-33B | ⏳ GĐ6 stub |
| `GET /hackathons/{id}/individual-rankings` | FR-33C | ⏳ GĐ6 stub |
| `POST /hackathons/{id}/export-jobs` | FR-34/35 | ⏳ GĐ6 stub (202) |
| `GET /export-jobs/{id}` | FR-34 | ⏳ GĐ6 stub |
| `GET /export-jobs/{id}/download` | FR-34/35 | ⏳ GĐ6 stub |
| `PATCH /hackathons/{id}/status` | GĐ1 FR-06 | ✅ (generic; GĐ6 ưu tiên `/confirm`) |
| `GET /teams/{teamId}/journey` | — | ⏳ |
| `GET /presentation/duration` | GĐ3/GĐ5 timer config | ✅ |
| `PUT /presentation/duration` | GĐ3/GĐ5 timer config | ✅ |
| `DELETE /presentation/duration` | Gỡ override track GĐ3 | ✅ |
| `POST /presentation/queue/shuffle` | FR-23 | ✅ |
| `POST /presentation/timer/*` | FR-23 | ✅ |

**Deprecated alias (1 sprint):** `/review`, `/resubmit`, `/advance-teams`, `/wildcard/candidates`, `/wildcard/approve|reject`.

**Swagger tags:** Submissions, Scores, Round Progression, **Presentation Duration (GĐ3/GĐ5)**, Wildcard Reviews, Calibration Sessions, RBL Dashboard, **Hackathon Closure (GĐ6)**, **Prizes (GĐ6)**, **Export Jobs (GĐ6)**, Status, Teams Journey.

**GĐ1 — field timer:** `defaultPresentationMinutes` / `defaultQaMinutes` trên Round; `presentationMinutes` / `qaMinutes` trên Track (GET/PUT CRUD). Chi tiết: [fe-gd1-gd2-structure-and-fields.md](../testing/fe-gd1-gd2-structure-and-fields.md).

**GĐ6 chi tiết:** §6 · Luồng FE: [10-fe-api-flow-gd6.md](10-fe-api-flow-gd6.md) · Business rules: [01-business-rules-gd6.md](01-business-rules-gd6.md) · Backlog: [09-be-backlog-gd4-gd5.md](09-be-backlog-gd4-gd5.md).

---

## Enum

### `SubmissionStatus`

`SUBMITTED` | `LATE` | `LATE_PENDING` | `LATE_APPROVED` | `REJECTED` | `ACCEPTED`

### `ScoreType`

`NORMAL` | `CALIBRATION` | `PENALTY` (xem `ScoreType.java`)

### `ParticipationStatus` (team_round_tracks — D-2 v4.1)

`PARTICIPATING` | `ADVANCED` | `ELIMINATED`

### `PrizeRank`

`FIRST` | `SECOND` | `THIRD` | `HONORABLE` | `SPECIAL`

---

## 1. Round — Activate (MF-01, FR-15/25)

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

## 6. Hackathon — Kết thúc & Trao giải (GĐ6 / MF-06)

**Nguồn:** `GD06_SEAL_MF06_v3_2.docx` · **Role:** Coordinator (`@CoordinatorOnly`) trừ khi ghi chú khác.

**Tiền đề:** Hackathon `PENDING_CONFIRM` (GĐ5 FR-30A — lock scoring round FINAL). Luồng đóng sự kiện:

```
GET team-rankings → POST/GET prizes → PATCH confirm → (async) chapter/individual rankings → POST export-jobs
```

**Stub hiện tại:** endpoint trả **200/201/202** với `data` rỗng hoặc builder tối thiểu — chưa gate/worker/query thật. Xem [09-be-backlog-gd4-gd5.md](09-be-backlog-gd4-gd5.md) mục G6-*.

---

### 6.1 Bảng XH Team — round Chung kết (FR-31 / FR-33A) ⏳

```http
GET /hackathons/{hackathonId}/team-rankings
Authorization: Bearer <coordinator>
```

**Điều kiện (phase 2):** hackathon `PENDING_CONFIRM` hoặc `FINISHED`; query `v_round_leaderboard` round `is_final=TRUE`.

**200 — `FinalTeamRankingItemResponse[]`**

```json
[
  {
    "rank": 1,
    "teamId": 1,
    "teamName": "Seal Warriors",
    "chapterId": 2,
    "chapterName": "FPT HCM",
    "weightedAvgScore": 8.75,
    "judgeCount": 3
  }
]
```

**Hiện tại:** `[]` (stub).

---

### 6.2 Trao giải (FR-32)

#### 6.2.1 Trao giải — ✅

```http
POST /hackathons/{hackathonId}/prizes
Authorization: Bearer <coordinator>
```

**Điều kiện:** hackathon `PENDING_CONFIRM` (không `FINISHED`).

```json
{
  "roundId": 2,
  "trackId": null,
  "teamId": 1,
  "prizeName": "Giải Nhất",
  "prizeRank": "FIRST",
  "prizeValue": "7000000",
  "description": "SEAL E2E 2026"
}
```

| Field | Bắt buộc | Ghi chú |
|-------|----------|---------|
| `roundId` | ✅ | Round FINAL của hackathon |
| `trackId` | — | Nullable — round CK không bắt buộc track |
| `teamId` | ✅ | Đội thuộc cùng hackathon |
| `prizeRank` | — | `FIRST`, `SECOND`, `THIRD`, `CONSOLATION`, … |

**201 — `PrizeResponse`**

```json
{
  "id": 10,
  "hackathonId": 1,
  "roundId": 2,
  "trackId": null,
  "teamId": 1,
  "prizeName": "Giải Nhất",
  "prizeRank": "FIRST",
  "prizeValue": "7000000",
  "description": "SEAL E2E 2026",
  "awardedAt": "2026-05-29T14:00:00",
  "awardedById": 5
}
```

**409** `PRIZE_DUPLICATE` — trùng đội hoặc loại giải trong hackathon.  
**422** `HACKATHON_NOT_PENDING_CONFIRM` — trao giải sai phase.

#### 6.2.2 Danh sách giải — ⏳

```http
GET /hackathons/{hackathonId}/prizes
Authorization: Bearer <coordinator>
```

**200 — `PrizeResponse[]`** · **Hiện tại:** `[]` (stub; phase 2 đọc DB + gate `PENDING_CONFIRM`+).

#### 6.2.3 Thu hồi giải — ⏳

```http
DELETE /prizes/{prizeId}
Authorization: Bearer <coordinator>
```

**200** — body `null` trong envelope.  
**Phase 2:** chặn khi hackathon `FINISHED`; audit FR-36.

---

### 6.3 Xác nhận kết thúc (FR-33) ⏳

```http
PATCH /hackathons/{hackathonId}/confirm
Authorization: Bearer <coordinator>
```

```json
{
  "confirm": true,
  "note": "BTC xác nhận kết quả SEAL E2E 2026"
}
```

| Field | Bắt buộc | Ghi chú |
|-------|----------|---------|
| `confirm` | ✅ | `true` để chuyển `PENDING_CONFIRM → FINISHED` |
| `note` | — | Ghi chú audit |

**200 — `HackathonResponse`** (stub hiện chỉ trả `id`).

**Phase 2 gates:**

- Hackathon `PENDING_CONFIRM`
- Round FINAL đã `scoring_locked`
- ≥ 1 bản ghi `prizes` → nếu không: **422** `NO_PRIZES_RECORDED`
- `SELECT … FOR UPDATE` chống race confirm

**Side effect (async):** `HackathonFinishedEvent` → worker chapter/individual rankings + notify `RESULT_PUBLISHED`.

> **Khác `/status`:** GĐ6 workflow dùng **`/confirm`** (FR-33). `PATCH /hackathons/{id}/status` (§6.7) là API generic GĐ1 — không thay thế gate prizes của MF-06.

---

### 6.4 Bảng XH Chapter (FR-33B) ⏳

```http
GET /hackathons/{hackathonId}/chapter-rankings
Authorization: Bearer <coordinator>
```

**200 — `ChapterRankingItemResponse[]`**

```json
[
  {
    "chapterId": 2,
    "chapterName": "FPT HCM",
    "bestTeamScore": 8.75,
    "totalScore": 12.5,
    "rank": 1,
    "teamsParticipated": 4,
    "prizesWon": 2
  }
]
```

**Phase 2:** đọc `chapter_rankings` sau worker; gate hackathon `FINISHED` (hoặc đang processing).

**Hiện tại:** `[]`.

---

### 6.5 Bảng XH Cá nhân (FR-33C / FR-33D) ⏳

```http
GET /hackathons/{hackathonId}/individual-rankings
Authorization: Bearer <coordinator>
```

Chỉ áp dụng khi `hackathons.individual_ranking_enabled = true` (Fall 2025). Spring 2026 thường **không** có bảng này.

**200 — `IndividualRankingItemResponse[]`**

```json
[
  {
    "userId": 42,
    "fullName": "Nguyen Van A",
    "scoreThisHackathon": 9.2,
    "cumulativeScore": 27.5,
    "rank": 1
  }
]
```

**Phase 2:** **404** `INDIVIDUAL_RANKING_NOT_AVAILABLE` nếu cờ tắt.

**Hiện tại:** `[]` (chưa gate cờ).

---

### 6.6 Export jobs (FR-34 / FR-35) ⏳

#### 6.6.1 Tạo job

```http
POST /hackathons/{hackathonId}/export-jobs
Authorization: Bearer <coordinator>
```

```json
{
  "type": "CSV_RANKINGS"
}
```

**`ExportJobType`:** `CSV_SCORES` · `CSV_RANKINGS` · `ANONYMIZED_RBL` · `FULL_REPORT`

**202 — `ExportJobResponse`** (stub: `status=PENDING`, chưa persist DB)

```json
{
  "hackathonId": 1,
  "type": "CSV_RANKINGS",
  "status": "PENDING"
}
```

**Phase 2:** gate hackathon `FINISHED`; INSERT `export_jobs`; enqueue worker; `expires_at` + cleanup.

#### 6.6.2 Trạng thái job

```http
GET /export-jobs/{jobId}
Authorization: Bearer <coordinator>
```

**200 — `ExportJobResponse`**

```json
{
  "id": 7,
  "hackathonId": 1,
  "type": "ANONYMIZED_RBL",
  "status": "DONE",
  "fileUrl": "https://s3.../export-7.zip",
  "errorMessage": null,
  "createdAt": "2026-05-29T15:00:00",
  "finishedAt": "2026-05-29T15:02:30"
}
```

**`ExportJobStatus`:** `PENDING` · `PROCESSING` · `DONE` · `FAILED`

#### 6.6.3 Download

```http
GET /export-jobs/{jobId}/download
Authorization: Bearer <coordinator>
```

**200 — `String`** (presigned URL hoặc path) trong envelope `data`.

**Phase 2:** gate `status=DONE`, chưa hết `expires_at`; audit download FR-36.

**Hiện tại:** `data: null`.

---

### 6.7 Đổi trạng thái generic (GĐ1 FR-06) ✅

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

**Ghi chú GĐ6:** MF-06 khuyến nghị Coordinator dùng **`PATCH /confirm`** (§6.3) thay vì set `FINISHED` trực tiếp qua `/status`, để đảm bảo gate prizes và audit đầy đủ.

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
| `NO_PRIZES_RECORDED` | 422 | Confirm GĐ6 khi chưa trao giải (phase 2) |
| `HACKATHON_NOT_FINISHED` | 422 | Export / chapter rankings trước FINISHED (phase 2) |
| `INDIVIDUAL_RANKING_NOT_AVAILABLE` | 404 | Cờ `individual_ranking_enabled=false` (phase 2) |
| `EXPORT_JOB_NOT_READY` | 422 | Download khi job chưa DONE (phase 2) |

Đầy đủ: `ErrorCode.java`, [01-business-rules-gd3.md](01-business-rules-gd3.md), [01-business-rules-gd6.md](01-business-rules-gd6.md).

---

## 9. MF-03 warning codes

| Code | Khi nào |
|------|---------|
| `PARTIAL_SCORING_BEFORE_LOCK` | Lock khi chưa chấm đủ |
| `INCOMPLETE_SCORING_IN_RANKING` | Ranking thiếu điểm |
| `MIN_TEAMS_NOT_REACHED` | Cần wild card |
| `JUDGE_PARTICIPATED_IN_PRELIM` | Gợi ý đổi judge CK |

`WarningCode.java` — trả trong `warnings[]` của envelope 2xx.
