# Ma trận phân quyền API — MF-03 v4.1 (bổ sung)

**Cập nhật:** 2026-05-29 · GD03 v4.1 scaffold

Xem phân quyền MF-01/MF-02 trong tài liệu tương ứng. Bảng dưới chỉ **endpoint mới/đổi** MF-03 v4.1.

| Method | Path | JWT | Annotation | COORD | JUDGE | MENTOR | STUDENT | Public |
|--------|------|-----|------------|:-----:|:-----:|:------:|:-------:|:------:|
| PATCH | `/submissions/{id}/review-late` | ✅ | COORDINATOR | ✅ | — | — | — | — |
| PATCH | `/rounds/{id}/publish` | ✅ | COORDINATOR | ✅ | — | — | — | — |
| POST | `/rounds/{id}/advance` | ✅ | COORDINATOR | ✅ | — | — | — | — |
| PATCH | `/wildcard-reviews/{id}` | ✅ | COORDINATOR | ✅ | — | — | — | — |
| PATCH | `/teams/{id}/eliminate` | ✅ | COORDINATOR | ✅ | — | — | — | — |
| POST | `/calibration-sessions` | ✅ | COORDINATOR | ✅ | — | — | — | — |
| PATCH | `/calibration-sessions/{id}` | ✅ | COORDINATOR | ✅ | — | — | — | — |
| GET | `/calibration-sessions?roundId=` | ✅ | COORDINATOR | ✅ | — | — | — | — |
| GET | `/rounds/{id}/rbl/variance` | ✅ | COORDINATOR | ✅ | — | — | — | — |
| GET | `/rounds/{id}/rbl/progress` | ✅ | COORDINATOR | ✅ | — | — | — | — |
| POST | `/submissions` | ✅ | STUDENT | — | — | — | ✅ | — |
| POST | `/scores` | ✅ | JUDGE+APPROVED | — | ✅ | — | — | — |
| POST | `/scores/calibration` | ✅ | JUDGE+APPROVED | — | ✅ | — | — | — |
| GET | `/rounds/{id}/scoreboard` | — | permitAll | ✅ | ✅ | ✅ | ✅ | ✅ |

## WebSocket (FR-18A)

| Subscribe | JWT CONNECT | COORD | JUDGE (assigned) |
|-----------|-------------|:-----:|:----------------:|
| `/topic/rounds/{id}/leaderboard-preview` | ✅ Bearer | ✅ | ✅ |
| `/topic/rounds/{id}/scoring-progress` | ✅ Bearer | ✅ | ✅ |
| `/topic/tracks/{id}/score-saved` | ✅ Bearer | ✅ | ✅ |

Handshake: `GET /ws/**` permitAll — auth tại STOMP CONNECT. Chi tiết: [mf03/06-live-scoring-websocket.md](mf03/06-live-scoring-websocket.md).

---

## User role portals (`/api/v1/me/*`) — additive 2026-05-29

Chi tiết JSON: [user-role/README.md](user-role/README.md). Endpoint Coordinator/Student/Judge **cũ không đổi**.

| Method | Path | JWT | Annotation | COORD | JUDGE | MENTOR | STUDENT |
|--------|------|-----|------------|:-----:|:-----:|:------:|:-------:|
| GET/PATCH | `/me/notifications`, `/me/notifications/read` | ✅ | APPROVED | ✅* | ✅* | ✅* | ✅* |
| GET | `/me/hackathons/browse` | ✅ | STUDENT | — | — | — | ✅ |
| POST/DELETE | `/me/hackathons/{id}/register` | ✅ | STUDENT | — | — | — | ✅ |
| GET | `/me/teams`, `/me/teams/{id}/submissions`, … | ✅ | STUDENT | — | — | — | ✅ |
| GET | `/me/rounds/{id}/problem`, `.../leaderboard` | ✅ | STUDENT | — | — | — | ✅ |
| POST | `/me/appeals` | ✅ | STUDENT | — | — | — | ✅ |
| GET | `/me/judge-track-assignments`, `/me/scoring-schedule`, … | ✅ | JUDGE | — | ✅ | — | — |
| PATCH | `/me/scores/{id}/comment` | ✅ | JUDGE | — | ✅ | — | — |
| POST | `/me/tiebreak-evaluations` | ✅ | JUDGE | — | ✅ | — | — |
| GET | `/me/mentor-track-assignments`, `/me/mentor/teams/{id}/…` | ✅ | MENTOR | — | — | ✅ | — |
| GET | `/me/mentor/rounds/{id}/schedule` | ✅ | MENTOR | — | — | ✅ | — |
| GET | `/me/scoring-schedule?roundId=` · `/me/scores?roundId=` | ✅ | JUDGE | — | ✅ | — | — |
| GET | `/me/annual-awards?year=` · `/me/judge-history?year=` · `/me/mentor-history?year=` | ✅ | theo role | — | — | — | — |

\* Mọi role **APPROVED** (COORDINATOR/JUDGE/MENTOR/STUDENT) đều có thể gọi notifications nếu có JWT hợp lệ.
