# GĐ3 — Luồng API cho Frontend (Thi & chấm Sơ loại)

**Mục đích:** FE đọc theo thứ tự từ trên xuống — biết **đến bước nào gọi API nào**.

**Base URL:** `http://localhost:8080/api/v1`  
**Auth:** `Authorization: Bearer {{accessToken}}` (trừ endpoint ghi rõ public)  
**Envelope:** `{ "success", "data", "warnings?", "timestamp" }` — xem [mf01/api/_conventions.md](../mf01/api/_conventions.md)

**Chi tiết request/response:** [03-api-reference-gd3.md](03-api-reference-gd3.md)  
**WebSocket live scoring:** [06-live-scoring-websocket.md](06-live-scoring-websocket.md)

---

## Ký hiệu trạng thái BE

| Ký hiệu | Ý nghĩa |
|---------|---------|
| ✅ | Đã implement — FE có thể tích hợp |
| ⏳ | Route có; logic stub / trả `data` rỗng |
| ⚠️ | Deprecated — tránh dùng endpoint mới |

---

## Sơ đồ tổng quan GĐ3

```
[Tiền đề GĐ2] → Activate round → Phát đề → Nộp bài → (Duyệt muộn?)
      → Judge chấm (+ WS) → Theo dõi tiến độ → Khóa chấm → Xếp hạng preview
      → (Loại đội vi phạm?) → [Sang GĐ4: publish / advance / tiebreak…]
```

---

## Bước 0 — Tiền đề (hoàn thành ở GĐ2, không thuộc GĐ3)

Trước khi vào màn thi Sơ loại, FE cần xác nhận:

| Kiểm tra | API đọc | Kỳ vọng |
|----------|---------|---------|
| User đã duyệt | `GET /users/me` | `status = APPROVED` |
| Đội đã ACTIVE | `GET /teams/{teamId}` | `status = ACTIVE` |
| Hackathon đang diễn ra | `GET /hackathons/{id}` | `status = ONGOING` |
| Đội đã có track (lottery) | `GET /teams/{teamId}` → `members` + journey | Có `trackId` qua lottery GĐ2 |
| Round Sơ loại đã cấu hình | `GET /hackathons/{hackathonId}/rounds` | Round `isFinal=false`, có deadline |
| Criteria + Judge | `GET /tracks/{trackId}/criteria` · `GET /tracks/{trackId}/judges` | Coordinator setup MF-01 |

**Luồng GĐ2 đầy đủ:** [mf02/02-mainflow-gd2.md](../mf02/02-mainflow-gd2.md)

---

## Bước 1 — Coordinator kích hoạt vòng Sơ loại

**Ai:** Coordinator  
**Khi:** Sau khi MF-01 đã cấu hình criteria (weight=1), judge track, đội đã lottery.

### API hành động

```http
PATCH /rounds/{roundId}/activate
Authorization: Bearer <coordinator>
Content-Type: application/json

{ "note": "Mở vòng Sơ loại" }
```

| | |
|---|---|
| **Response** | `RoundSummaryResponse` — `isActive=true`, `activatedAt` |
| **Lỗi thường gặp** | `ROUND_NO_CRITERIA`, `JUDGE_NOT_ASSIGNED`, `NO_TEAMS_IN_ROUND` |
| **Trạng thái BE** | ✅ |

### API đọc trước / sau bước này

```http
GET /hackathons/{hackathonId}/rounds          # chọn round Sơ loại
GET /rounds/{roundId}                           # chi tiết round
GET /hackathons/{id}/readiness                # cảnh báo mềm trước activate (optional)
```

**FE gợi ý:** Chỉ 1 round `isActive=true` / hackathon. Sau activate → chuyển Coordinator sang màn "Phát đề".

---

## Bước 2 — Coordinator phát đề

**Ai:** Coordinator  
**Khi:** Round đã active; trước hoặc song song với deadline nộp bài.

### API hành động

```http
PATCH /rounds/{roundId}/release-problem
Authorization: Bearer <coordinator>
Content-Type: application/json

{
  "problemStatementUrl": "https://..."
}
```

| | |
|---|---|
| **Response** | `RoundSummaryResponse` — có `problemStatementUrl`, `problemReleasedAt` |
| **Lưu ý** | One-way: đã phát thì không sửa URL |
| **Trạng thái BE** | ✅ |

### API đọc (Student / Mentor / Judge)

```http
GET /rounds/{roundId}                           # lấy URL đề trên dashboard đội
```

**FE gợi ý:** Student dashboard hiển thị link đề + countdown `submissionDeadline`.

---

## Bước 3 — Student nộp bài (Sơ loại)

**Ai:** Student APPROVED, thuộc đội ACTIVE, đã gán track  
**Khi:** Round active; trước hoặc sau deadline (tùy policy).

### API đọc trước khi nộp

```http
GET /users/me                                   # xác nhận APPROVED
GET /teams/{teamId}                             # team ACTIVE, isLocked?
GET /rounds/{roundId}                           # isActive, submissionDeadline, lateSubmissionPolicy
GET /submissions?teamId={teamId}&roundId={roundId}   # bài đã nộp chưa (upsert)
```

### API hành động — nộp / nộp lại (upsert)

```http
POST /submissions
Authorization: Bearer <student-approved>
Content-Type: application/json

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

| | |
|---|---|
| **Response** | `201` — `SubmissionResponse` |
| **Trạng thái sau nộp** | Trước deadline → `SUBMITTED` · Sau deadline + cho phép trễ → `LATE_PENDING` · HARD_LOCK → `REJECTED` |
| **Nộp lại** | Gọi lại `POST /submissions` cùng `teamId` + `trackId` (upsert) — **không** dùng `PATCH .../resubmit` ⚠️ |
| **Lỗi thường gặp** | `ROUND_NOT_ACTIVE`, `TEAM_NOT_IN_TRACK`, `TEAM_NOT_ACTIVE`, `INVALID_REPO_PLATFORM` (Google Drive repo) |
| **Trạng thái BE** | ✅ |

**FE gợi ý:**

| `submission.status` | UI |
|---------------------|-----|
| `SUBMITTED` | Xanh — đã nộp đúng hạn |
| `LATE_PENDING` | Vàng — chờ BTC duyệt (Bước 3b) |
| `LATE_APPROVED` | Xanh đậm — được chấm |
| `REJECTED` | Đỏ — không nộp lại được |

---

## Bước 3b — Coordinator duyệt bài nộp trễ (nhánh)

**Ai:** Coordinator  
**Khi:** Có submission `status = LATE_PENDING` (chỉ vòng Sơ loại).

### API đọc

```http
GET /submissions?roundId={roundId}              # lọc FE: status = LATE_PENDING
```

### API hành động

```http
PATCH /submissions/{submissionId}/review-late
Authorization: Bearer <coordinator>
Content-Type: application/json

{
  "decision": "APPROVE",
  "note": "Chấp nhận nộp trễ có lý do"
}
```

```json
// Từ chối — bắt buộc có note
{ "decision": "REJECT", "note": "Không đủ lý do trễ" }
```

| | |
|---|---|
| **Sau APPROVE** | `status → LATE_APPROVED` — Judge được chấm |
| **Sau REJECT** | `status → REJECTED` |
| **Trạng thái BE** | ✅ |

⚠️ `PATCH /submissions/{id}/review` — deprecated, dùng `review-late`.

---

## Bước 4 — Judge chấm điểm (+ Live WebSocket)

**Ai:** Judge APPROVED, đã phân công track/round  
**Khi:** Round active, **chưa** `scoringLocked`; submission gradable (`SUBMITTED`, `LATE_APPROVED`, `ACCEPTED`).

### API đọc

```http
GET /submissions?roundId={roundId}              # Judge bắt buộc roundId
GET /tracks/{trackId}/criteria                  # form chấm theo tiêu chí
GET /rounds/{roundId}                           # kiểm tra scoringLocked
```

### API hành động — upsert điểm

```http
POST /scores
Authorization: Bearer <judge-approved>
Content-Type: application/json

{
  "submissionId": 100,
  "criterionId": 5,
  "scoreValue": 8.5,
  "comment": "Tốt",
  "scoreType": "NORMAL"
}
```

| | |
|---|---|
| **Response** | `ScoreResponse` — `isFinal=false` (nháp live) |
| **Lỗi thường gặp** | `SUBMISSION_NOT_GRADABLE`, `JUDGE_NOT_ASSIGNED_TO_TRACK`, `SCORE_EXCEEDS_MAX`, `423 SCORING_LOCKED` |
| **Trạng thái BE** | ✅ |

### WebSocket (FR-18A) — song song REST

```
CONNECT  ws://localhost:8080/ws
         Header: Authorization: Bearer <token>

SUBSCRIBE /topic/rounds/{roundId}/leaderboard-preview   → ranking live
SUBSCRIBE /topic/rounds/{roundId}/scoring-progress      → tiến độ chấm
SUBSCRIBE /topic/tracks/{trackId}/score-saved           → echo sau mỗi POST /scores
```

Chi tiết code mẫu: [06-live-scoring-websocket.md](06-live-scoring-websocket.md)

**FE gợi ý:** Disable form khi `round.scoringLocked=true` hoặc nhận `423`. Không chấm bài `LATE_PENDING`.

---

## Bước 5 — Coordinator theo dõi tiến độ chấm

**Ai:** Coordinator (Judge cùng track cũng subscribe WS được)  
**Khi:** Đang trong giai đoạn chấm, trước lock.

### API đọc (polling fallback)

```http
GET /rounds/{roundId}/scoring-progress
Authorization: Bearer <coordinator>
```

**Response mẫu:**

```json
{
  "roundId": 1,
  "totalSubmissions": 12,
  "scoredSubmissions": 8,
  "pendingSubmissions": 4,
  "scoringLocked": false
}
```

### API đọc — xếp hạng tạm (live)

```http
GET /rounds/{roundId}/ranking/preview
Authorization: Bearer <coordinator>
```

| | |
|---|---|
| **Response** | `RoundRankingItemResponse[]` — rank, teamId, teamName, totalScore, assignedGroup |
| **Warning** | Có thể kèm `INCOMPLETE_SCORING_IN_RANKING` nếu còn tiêu chí chưa chấm |
| **Trạng thái BE** | ✅ |

**FE gợi ý:** Ưu tiên WS; polling `scoring-progress` mỗi 10–15s nếu WS lỗi.

---

## Bước 6 — Coordinator khóa chấm điểm

**Ai:** Coordinator  
**Khi:** Hết thời gian chấm hoặc đã chấm đủ; bắt buộc trước ranking chính thức GĐ4.

### API hành động

```http
PATCH /rounds/{roundId}/lock-scoring
Authorization: Bearer <coordinator>
Content-Type: application/json

{
  "force": false,
  "reason": null
}
```

Force lock (còn bài chưa chấm):

```json
{
  "force": true,
  "reason": "BTC yêu cầu khóa khẩn do thiếu giờ"
}
```

| | |
|---|---|
| **Response** | `RoundSummaryResponse` + optional `warnings[]` (`PARTIAL_SCORING_BEFORE_LOCK`) |
| **Sau lock** | `POST /scores` → `423 SCORING_LOCKED`; điểm được finalize (`isFinal=true`) |
| **WS** | Broadcast scoring-progress lần cuối; không còn leaderboard-preview |
| **Trạng thái BE** | ✅ |

**FE gợi ý:** Nếu có `warnings`, hiện dialog xác nhận trước khi coi lock thành công.

---

## Bước 7 — Xem xếp hạng sau khóa

**Ai:** Coordinator  
**Khi:** `round.scoringLocked = true`.

### API đọc

```http
GET /rounds/{roundId}/ranking
Authorization: Bearer <coordinator>
```

| | |
|---|---|
| **Khác preview** | Ranking chính thức dùng điểm `isFinal=true`; **bắt buộc** đã lock |
| **Lỗi** | `ROUND_NOT_SCORING_LOCKED` nếu chưa lock |
| **Trạng thái BE** | ✅ |

**FE gợi ý:** Sau bước này → điều hướng sang wizard GĐ4 (tiebreak / wild card / advance).

---

## Bước 8 — Loại đội vi phạm (nhánh, FR-21)

**Ai:** Coordinator  
**Khi:** Đội vi phạm quy chế trong lúc thi (vắng mặt, gian lận…).

### API hành động

```http
PATCH /teams/{teamId}/eliminate
Authorization: Bearer <coordinator>
Content-Type: application/json

{
  "reason": "Vắng mặt không lý do tại vòng Sơ loại"
}
```

| | |
|---|---|
| **Hệ quả** | `team.status → ELIMINATED`; `team_round_tracks.participationStatus → ELIMINATED` |
| **Sau eliminate** | Student không nộp bài; Judge không chấm bài đội đó |
| **Trạng thái BE** | ✅ |

---

## Bước 9 — Hành trình đội (cross-cutting, optional UI)

**Ai:** Student / Coordinator / Judge (authenticated)  
**Khi:** Màn "Lịch sử thi" / timeline đội.

```http
GET /teams/{teamId}/journey
Authorization: Bearer <token>
```

| | |
|---|---|
| **Response** | `TeamJourneyResponse` — các round, track, `participationStatus` |
| **Trạng thái BE** | ⏳ stub |

---

## Happy path — checklist QA (copy cho FE)

Giả sử `hackathonId=1`, round Sơ loại `roundId=1`, `teamId=1`, `trackId=1`:

| # | Actor | API | Ghi chú |
|---|-------|-----|---------|
| 0 | Student | `GET /users/me` · `GET /teams/1` | APPROVED + ACTIVE |
| 1 | Coordinator | `PATCH /rounds/1/activate` | |
| 2 | Coordinator | `PATCH /rounds/1/release-problem` | |
| 3 | Student | `POST /submissions` | body có `trackId` |
| 3b | Coordinator | `PATCH /submissions/{id}/review-late` | chỉ nếu `LATE_PENDING` |
| 4 | Judge | `GET /submissions?roundId=1` → `POST /scores` | + WS subscribe |
| 5 | Coordinator | `GET /rounds/1/scoring-progress` hoặc WS | |
| 6 | Coordinator | `PATCH /rounds/1/lock-scoring` | |
| 7 | Coordinator | `GET /rounds/1/ranking` | sau lock |
| 8 | Coordinator | `PATCH /teams/{id}/eliminate` | optional |

---

## Phân quyền nhanh — GĐ3

| API | STUDENT | JUDGE | COORDINATOR |
|-----|---------|-------|-------------|
| `POST /submissions` | ✅ | — | — |
| `GET /submissions` | ✅ `teamId` bắt buộc | ✅ `roundId` + assigned | ✅ |
| `PATCH .../review-late` | — | — | ✅ |
| `POST /scores` | — | ✅ assigned | — |
| `PATCH /rounds/.../activate` | — | — | ✅ |
| `PATCH /rounds/.../release-problem` | — | — | ✅ |
| `PATCH /rounds/.../lock-scoring` | — | — | ✅ |
| `GET /rounds/.../ranking/preview` | — | — | ✅ |
| `GET /rounds/.../scoring-progress` | — | — | ✅ |
| `GET /rounds/.../ranking` | — | — | ✅ |
| `PATCH /teams/.../eliminate` | — | — | ✅ |
| WS `/topic/rounds/*` | — | ✅ assigned | ✅ |

Ma trận đầy đủ: [api-authorization-matrix.md](../api-authorization-matrix.md)

---

## GĐ4 → GĐ6 — API tiếp theo

Sau khi hoàn thành GĐ3 (round Sơ loại đã lock), FE chuẩn bị các màn sau.

| Giai đoạn | Luồng | API chính | BE |
|-----------|-------|-----------|-----|
| GĐ4 | Công bố + advance | `PATCH /rounds/{prelimId}/publish` · `POST .../advance` | ✅ phase 1 |
| GĐ4 | Tiebreak + wild card | `GET/POST .../tiebreak` · `GET/POST .../wildcard/*` | ⏳ phase 2 |
| GĐ4 | Phân judge CK + activate CK | `POST /rounds/{finalId}/judge-assignments` · `PATCH /rounds/{finalId}/activate` | ✅ |
| GĐ5 | Nộp CK → calibration → chấm → lock | `POST /submissions` (không `trackId`) · `POST /scores/calibration` · `POST /scores` · lock | ⏳ submit CK |
| GĐ6 | Trao giải → kết thúc | `POST /hackathons/{id}/prizes` · `PATCH /hackathons/{id}/status` | ✅ prizes |
| Public | Bảng điểm | `GET /rounds/{id}/scoreboard` (no JWT) | ⏳ |

Luồng chi tiết GĐ4: **[08-fe-api-flow-gd4.md](08-fe-api-flow-gd4.md)** · GĐ4–6 tổng quan: [02-mainflow-gd3.md](02-mainflow-gd3.md)

### Judge Chung kết (preview FE)

- Panel = **guest EXTERNAL** (`POST /users/temp-judges`) + **trưởng ban** (`PATCH /users/{id}` → `is_dept_head=true`, không mentor kỳ này).
- `FINAL_EXTERNAL` = loại phân công round CK — **không** = user phải external.
- Warning `JUDGE_PARTICIPATED_IN_PRELIM` nếu assign judge đã Sơ loại.
- Thiếu judge khách: xem playbook §13 [01-business-rules-gd3.md](01-business-rules-gd3.md).

---

## Lỗi & warning FE nên handle (GĐ3)

| Code | HTTP | Khi nào | FE |
|------|------|---------|-----|
| `ROUND_NOT_ACTIVE` | 422 | Nộp bài khi round chưa mở | Ẩn nút nộp |
| `SUBMISSION_NOT_GRADABLE` | 422 | Chấm bài LATE_PENDING | Disable ô điểm |
| `SCORING_LOCKED` | 423 | Chấm sau lock | Khóa form judge |
| `ROUND_NOT_SCORING_LOCKED` | 422 | Ranking chính thức trước lock | Redirect preview |
| `JUDGE_NOT_ASSIGNED_TO_TRACK` | 403 | Judge sai track | Toast phân quyền |
| `PARTIAL_SCORING_BEFORE_LOCK` | warning | Lock khi chưa chấm đủ | Dialog xác nhận |
| `INCOMPLETE_SCORING_IN_RANKING` | warning | Preview thiếu điểm | Badge vàng trên bảng |

---

## Liên kết

| Tài liệu | Nội dung |
|----------|----------|
| [03-api-reference-gd3.md](03-api-reference-gd3.md) | Contract JSON đầy đủ |
| [05-fe-handover-gd3.md](05-fe-handover-gd3.md) | Màn hình gợi ý |
| [04-test-data.md](04-test-data.md) | curl + token mẫu |
| [mf02/03-api-reference-gd2.md](../mf02/03-api-reference-gd2.md) | Auth, teams, lottery (GĐ2) |

---

## Bảng tra cứu nhanh — 100% endpoint GĐ3 ✅

Danh sách **đầy đủ** API liên quan GĐ3 (Sơ loại: thi & chấm), kể cả **ngoài happy path**, deprecated, và API đọc hỗ trợ từ GĐ1/GĐ2.

**Base:** `/api/v1` · **Auth:** `Bearer` (trừ ghi rõ public / WS CONNECT)

### A. Nghiệp vụ GĐ3 — ghi / thay đổi dữ liệu

| # | Method | Path | FR | Actor | Trong happy path? | BE | Ghi chú |
|---|--------|------|-----|-------|-------------------|-----|---------|
| 1 | `PATCH` | `/rounds/{id}/activate` | FR-15 | Coordinator | ✅ Bước 1 | ✅ | Body optional `{ "note" }` |
| 2 | `PATCH` | `/rounds/{id}/release-problem` | FR-15A | Coordinator | ✅ Bước 2 | ✅ | `{ "problemStatementUrl" }` |
| 3 | `POST` | `/submissions` | FR-16 | Student | ✅ Bước 3 | ✅ | Upsert — nộp lại cùng `teamId`+`trackId` |
| 4 | `PATCH` | `/submissions/{id}/review-late` | FR-16A | Coordinator | Nhánh 3b | ✅ | `{ "decision": "APPROVE"\|"REJECT", "note" }` |
| 5 | `POST` | `/scores` | FR-18/19 | Judge | ✅ Bước 4 | ✅ | Upsert điểm live (`isFinal=false`) |
| 6 | `PATCH` | `/rounds/{id}/lock-scoring` | FR-20A | Coordinator | ✅ Bước 6 | ✅ | `{ "force", "reason" }` + warnings |
| 7 | `PATCH` | `/teams/{teamId}/eliminate` | FR-21 | Coordinator | Nhánh 8 | ✅ | `{ "reason" }` — loại đội vi phạm |

### B. Nghiệp vụ GĐ3 — đọc dữ liệu (REST)

| # | Method | Path | FR | Actor | Trong happy path? | BE | Ghi chú |
|---|--------|------|-----|-------|-------------------|-----|---------|
| 8 | `GET` | `/submissions?teamId=&roundId=` | — | Student / Judge / Coord | ✅ 3, 3b, 4 | ✅ | Student **bắt buộc** `teamId`; Judge **bắt buộc** `roundId` |
| 9 | `GET` | `/rounds/{id}/scoring-progress` | FR-20A | Coordinator | ✅ Bước 5 | ✅ | Polling fallback nếu WS lỗi |
| 10 | `GET` | `/rounds/{id}/ranking/preview` | FR-20 | Coordinator | ✅ Bước 5 | ✅ | Live — trước lock; có warning `INCOMPLETE_SCORING_IN_RANKING` |
| 11 | `GET` | `/rounds/{id}/ranking` | FR-20/22 | Coordinator | ✅ Bước 7 | ✅ | **Sau lock** — lỗi `ROUND_NOT_SCORING_LOCKED` nếu chưa lock |

> **Judge** không gọi REST #9–11 (CoordinatorOnly) — dùng WebSocket mục C.

### C. WebSocket — Live scoring (FR-18A)

| # | Hành động | Path / Topic | Actor | Trong happy path? | BE | Ghi chú |
|---|-----------|--------------|-------|-------------------|-----|---------|
| 12 | Handshake | `GET /ws` (SockJS) | All authenticated | ✅ Bước 4 | ✅ | `Authorization: Bearer` tại STOMP **CONNECT** |
| 13 | SUBSCRIBE | `/topic/rounds/{roundId}/leaderboard-preview` | Coord + Judge assigned | ✅ | ✅ | Payload: `RoundRankingItemResponse[]` |
| 14 | SUBSCRIBE | `/topic/rounds/{roundId}/scoring-progress` | Coord + Judge assigned | ✅ | ✅ | Payload: `RoundScoringProgressResponse` |
| 15 | SUBSCRIBE | `/topic/tracks/{trackId}/score-saved` | Coord + Judge assigned | ✅ | ✅ | Echo sau mỗi `POST /scores` |
| 16 | Event | Scoring locked | — | ✅ Bước 6 | ✅ | Sau lock: broadcast progress lần cuối; **không** còn leaderboard-preview |

Chi tiết client: [06-live-scoring-websocket.md](06-live-scoring-websocket.md)

### D. Deprecated — vẫn tồn tại, FE không nên dùng

> **Không cấp thiết implement BE GĐ3** — giữ endpoint 1 sprint rồi xóa; FE dùng API thay thế.

| # | Method | Path | Thay bằng | BE |
|---|--------|------|-----------|-----|
| 17 | `PATCH` | `/submissions/{id}/resubmit` | `POST /submissions` (upsert) | ⚠️ |
| 18 | `PATCH` | `/submissions/{id}/review` | `PATCH /submissions/{id}/review-late` | ⚠️ |

### E. Có route MF-03 nhưng chưa logic GĐ3

> **Không chặn demo GĐ3** — FE có thể dùng `GET /teams/{id}` + lottery data thay timeline.

| # | Method | Path | Actor | BE | Ghi chú |
|---|--------|------|-------|-----|---------|
| 19 | `GET` | `/teams/{teamId}/journey` | Authenticated | ⏳ stub | Timeline round/track — implement sau GĐ4 nếu cần màn timeline |

### F. API đọc hỗ trợ — không phải API mới GĐ3, nhưng FE cần khi build màn Sơ loại

> **Không cần code thêm BE GĐ3** — chỉ index tham chiếu từ GĐ1/GĐ2.

| # | Method | Path | Dùng khi | Nguồn | Ghi chú |
|---|--------|------|----------|-------|---------|
| 20 | `GET` | `/users/me` | Mọi màn | GĐ2 | Xác nhận `APPROVED`, role |
| 21 | `GET` | `/teams/{teamId}` | Dashboard đội | GĐ2 | `ACTIVE`, `isLocked`, members |
| 22 | `GET` | `/teams?hackathonId=` | Student tìm đội mình | GĐ2 | PENDING invite + ACCEPTED |
| 23 | `GET` | `/hackathons/{id}` | Kiểm tra phase | GĐ1 | `status = ONGOING` |
| 24 | `GET` | `/hackathons/{id}/readiness` | Coordinator pre-flight | GĐ1 | Cảnh báo mềm trước activate |
| 25 | `GET` | `/hackathons/{hackathonId}/rounds` | Chọn round Sơ loại | GĐ1 | `isFinal=false`, deadline |
| 26 | `GET` | `/rounds/{id}` | Chi tiết round | GĐ1 | `isActive`, `scoringLocked`, URL đề |
| 27 | `GET` | `/hackathons/{hackathonId}/tracks` | Track sau lottery | GĐ1 | Student lấy `trackId` |
| 28 | `GET` | `/tracks/{trackId}` | Chi tiết track | GĐ1 | Tên track, round cha |
| 29 | `GET` | `/tracks/{trackId}/criteria` | Form chấm điểm | GĐ1 | `maxScore`, `weight`, thứ tự |
| 30 | `GET` | `/tracks/{trackId}/criteria/weight-summary` | Validate trước activate | GĐ1 | Tổng weight = 1.0 |
| 31 | `GET` | `/tracks/{trackId}/judges` | Danh sách judge track | GĐ1 | Coordinator / debug |
| 32 | `GET` | `/rounds/{roundId}/judges` | Judge round FINAL / scope | GĐ1 | Chủ yếu CK; SL dùng track judges |
| 33 | `GET` | `/users/{judgeId}/round-assignments` | Dashboard judge | GĐ1 | Round nào được phân công |
| 34 | `GET` | `/teams/{teamId}/mentors` | Mentor theo vòng | GĐ2 | FR-13C — mentor xem đội được gán |
| 35 | `GET` | `/hackathons/{hackathonId}/events` | Lịch thuyết trình | GĐ1 | FR-23 — không chấm qua API này |
| 36 | `GET` | `/events/{id}` | Chi tiết sự kiện | GĐ1 | Presentation slot |

### G. Không có API public cho FE (GĐ3 nội bộ BE)

| FR | Mô tả | Ghi chú |
|----|--------|---------|
| FR-17 | Metadata repo (stars, language…) | BE enqueue sau `POST /submissions` — **không** expose GET |

---

### Tổng hợp nhanh

| Nhóm | Số endpoint | Ghi chú |
|------|-------------|---------|
| A — Ghi GĐ3 ✅ | 7 | Happy path + nhánh duyệt muộn / eliminate |
| B — Đọc GĐ3 ✅ | 4 | REST progression + submissions list |
| C — WebSocket ✅ | 5 hành động | Judge live scoring |
| D — Deprecated ⚠️ | 2 | Dùng upsert / review-late |
| E — Stub ⏳ | 1 | Journey |
| F — Đọc hỗ trợ | 17 | GĐ1/GĐ2 — load màn hình GĐ3 |

**Tổng endpoint nghiệp vụ GĐ3 đã implement:** **16** (7 ghi + 4 đọc REST + 5 WS) — khớp [03-api-reference-gd3.md](03-api-reference-gd3.md) mục trạng thái ✅.

**Không thuộc GĐ3** (GĐ4/GĐ5/GĐ6 — xem mục trên): `publish`, `tiebreak`, `wildcard`, `advance`, `judge-assignments`, `scoreboard`, `calibration`, `rbl`, `prizes` — **không** liệt kê ở bảng này.
