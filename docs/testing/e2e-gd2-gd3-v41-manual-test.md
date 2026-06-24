# E2E Manual Test — GĐ2 → GĐ3 v4.1 (đầu tới cuối)

> **Mục đích:** Chạy tay từng bước để xác nhận luồng mới (multipart PDF, shuffle, timer, judge ẩn danh, gate chấm) **đúng hay sai**.  
> **Đối tượng:** QA / dev / coordinator test trên Postman, Thunder Client hoặc `curl`.  
> **Liên quan:** [fe-gd3-api-mapping.md](fe-gd3-api-mapping.md) (contract FE) · [postman-playbook-gd2-gd3-integration.md](postman-playbook-gd2-gd3-integration.md) (IT-01→08) · [gd3-v41-implementation-changelog.md](gd3-v41-implementation-changelog.md) · WS: [../mf03/06-live-scoring-websocket.md](../mf03/06-live-scoring-websocket.md)

---

## Mục lục

1. [Chuẩn bị môi trường](#1-chuẩn-bị-môi-trường)
2. [Tài khoản & biến Postman](#2-tài-khoản--biến-postman)
3. [Phần A — GĐ2 (tiên quyết tới GĐ3)](#3-phần-a--gđ2-tiên-quyết-tới-gđ3)
4. [Phần B — GĐ3 v4.1 (luồng mới đầy đủ)](#4-phần-b--gđ3-v41-luồng-mới-đầy-đủ)
5. [Phần C — Test âm tính (phải FAIL đúng)](#5-phần-c--test-âm-tính-phải-fail-đúng)
6. [Phần D — WebSocket (tùy chọn)](#6-phần-d--websocket-tùy-chọn)
7. [Bảng tổng kết Pass/Fail](#7-bảng-tổng-kết-passfail)
8. [Troubleshooting](#8-troubleshooting)

---

## 1. Chuẩn bị môi trường

### 1.1 Khởi động BE

```bash
# Tại thư mục BE
mvn spring-boot:run -Dspring-boot.run.profiles=dev
# hoặc run IDE với Active profiles: dev
```

| Kiểm tra | Kỳ vọng | ☐ |
|----------|---------|---|
| Port | `http://localhost:8080` | |
| Log `[Gd2DataSeeder]` | `đăng ký ĐANG MỞ` (hoặc repair timeline) | |
| Log `[Gd3DataSeeder]` | In `hackathonId`, `prelimRoundId`, `track1Id`, `track2Id`, submission IDs | |
| `mvn test` (tuỳ chọn) | 160/160 pass | |

### 1.2 Storage (nộp PDF)

| `app.storage.type` | File lưu ở đâu |
|--------------------|----------------|
| `local` (mặc định) | `{app.storage.local-dir}/submissions/{hackathonId}/{roundId}/{submissionId}/slide.pdf` |
| | Mặc định: `uploads/submissions/submissions/...` |

**MinIO (tuỳ chọn):**

```bash
docker compose -f docker-compose.minio.yml up -d
# Set APP_STORAGE_TYPE=minio, restart BE
```

### 1.3 File PDF mẫu cho multipart

Tạo file `sample-slide.pdf` (tối thiểu 5 byte):

```text
%PDF-1.4
```

Hoặc dùng bất kỳ file `.pdf` thật (< 25MB).

### 1.4 GitHub repoUrl mẫu

| Trường hợp | `repoUrl` |
|------------|-----------|
| Pass (public check **tắt**) | `https://github.com/octocat/Hello-World` |
| Pass (public check **bật**) | Repo public thật của bạn |
| Fail | `https://gitlab.com/foo/bar` |

Tắt check dev (nếu không có mạng):

```properties
app.submission.github-public-check-enabled=false
```

---

## 2. Tài khoản & biến Postman

### 2.1 Login

```http
POST http://localhost:8080/api/v1/auth/login
Content-Type: application/json

{
  "email": "coord@fpt.edu.vn",
  "password": "Coordinator@dev1"
}
```

→ Lưu `data.accessToken` vào biến tương ứng.

### 2.2 Tài khoản dev

| Role | Email | Password |
|------|-------|----------|
| Coordinator | `coord@fpt.edu.vn` | `Coordinator@dev1` |
| Judge 1 | `judge1@fpt.edu.vn` | `Judge@dev1` |
| Judge 2 | `judge2@fpt.edu.vn` | `Judge@dev1` |
| Mentor | `mentor@fpt.edu.vn` | `Mentor@dev1` |
| Student GĐ3-04 (chưa nộp) | `student.gd3.leader04@fpt.edu.vn` | `Student@dev1` |
| Student GĐ3-02 (late) | `student.gd3.leader02@fpt.edu.vn` | `Student@dev1` |
| Student GĐ3-06 (chấm dở) | `student.gd3.leader06@fpt.edu.vn` | `Student@dev1` |
| Student GĐ2 | `student.gd2.hcm.leader03@fpt.edu.vn` | `Student@dev1` |

### 2.3 Biến lấy từ log `[Gd3DataSeeder]`

Sau bước **B.0**, điền:

| Biến | Nguồn |
|------|-------|
| `{{baseUrl}}` | `http://localhost:8080` |
| `{{gd3HackathonId}}` | log seed |
| `{{prelimRoundId}}` | log seed |
| `{{track1Id}}` | log seed |
| `{{track2Id}}` | log seed |
| `{{lateSubmissionId}}` | submission `LATE_PENDING` team 02 |
| `{{sub6Id}}` | submission team 06 (track 2, chấm dở) |
| `{{criterionIdT1}}` | `GET /tracks/{{track1Id}}/criteria` → id tiêu chí đầu |
| `{{criterionIdT2}}` | `GET /tracks/{{track2Id}}/criteria` → id tiêu chí đầu |

---

## 3. Phần A — GĐ2 (tiên quyết tới GĐ3)

**Hackathon GĐ2:** `seal-spring-2026`  
**Mục tiêu:** Xác nhận gate GĐ1→2 vẫn hoạt động — **không** test nộp bài sơ loại ở slug này (prelim có thể chưa/khác trạng thái seed).

> GĐ3 v4.1 test chính ở **Phần B** (`seal-gd3-prelim-open`). Phần A đảm bảo workflow GĐ2 không bị phá.

### A.1 Bootstrap GĐ2

```http
GET {{baseUrl}}/api/v1/hackathons?q=seal-spring-2026&size=5
Authorization: Bearer {{coordToken}}
```

| # | Kiểm tra | Pass khi | ☐ |
|---|----------|----------|---|
| A.1.1 | `data.content[0].status` | `ONGOING` | |
| A.1.2 | `GET .../hackathons/{id}/rounds` | Có round `isFinal=false` (prelim) | |
| A.1.3 | `GET .../rounds/{prelimId}/tracks` | ≥ 1 track, có criteria | |

### A.2 Student — đội & lottery

Login `student.gd2.hcm.leader03@fpt.edu.vn`:

```http
GET {{baseUrl}}/api/v1/me/teams
Authorization: Bearer {{studentGd2Token}}
```

| # | Kiểm tra | Pass khi | ☐ |
|---|----------|----------|---|
| A.2.1 | Có `teamId` | Team `ACTIVE` | |
| A.2.2 | `trackId` | Có hoặc null (tuỳ đã lottery) | |

**Lottery (Coordinator)** — nếu cần demo bốc thăm:

```http
PATCH {{baseUrl}}/api/v1/hackathons/{{gd2HackathonId}}/lottery
Authorization: Bearer {{coordToken}}
Content-Type: application/json

{ "roundId": "{{gd2PrelimRoundId}}" }
```

| # | Kiểm tra | Pass khi | ☐ |
|---|----------|----------|---|
| A.2.3 | Lottery | `200`; team locked mới lottery được | |

### A.3 Gate 2 — Activate prelim (GĐ2)

```http
PATCH {{baseUrl}}/api/v1/rounds/{{gd2PrelimRoundId}}/activate
Authorization: Bearer {{coordToken}}
Content-Type: application/json

{ "note": "E2E GĐ2 gate" }
```

| # | Kiểm tra | Pass khi | ☐ |
|---|----------|----------|---|
| A.3.1 | Response | `isActive: true` | |
| A.3.2 | **Không** tạo slot tự động | `GET /presentation/queue?roundId=` có thể **trống** hoặc slot cũ seed — **không** bắt buộc có slot mới sau activate | |

### A.4 Mentor GĐ2

```http
GET {{baseUrl}}/api/v1/me/mentor/rounds
Authorization: Bearer {{mentorToken}}
```

| # | Kiểm tra | Pass khi | ☐ |
|---|----------|----------|---|
| A.4.1 | Có round prelim | Danh sách không rỗng | |

**Kết luận Phần A:** Nếu A.1–A.4 pass → GĐ2 gate **OK**, chuyển Phần B.

---

## 4. Phần B — GĐ3 v4.1 (luồng mới đầy đủ)

**Hackathon:** `seal-gd3-prelim-open`  
**Thời gian ước tính:** 30–45 phút (chạy tuần tự).

---

### B.0 Bootstrap ID

```http
GET {{baseUrl}}/api/v1/hackathons?q=seal-gd3-prelim-open&size=5
Authorization: Bearer {{coordToken}}
```

```http
GET {{baseUrl}}/api/v1/hackathons/{{gd3HackathonId}}/rounds
Authorization: Bearer {{coordToken}}
```

```http
GET {{baseUrl}}/api/v1/rounds/{{prelimRoundId}}/tracks
Authorization: Bearer {{coordToken}}
```

| # | Kiểm tra | Pass khi | ☐ |
|---|----------|----------|---|
| B.0.1 | Prelim | `isActive: true`, `scoringLocked: false` | |
| B.0.2 | Tracks | 2 track (`track1Id`, `track2Id`) | |
| B.0.3 | Deadline | `submissionDeadline` đã qua hoặc sắp tới (seed repair theo ngày) | |

---

### B.1 Coordinator — xác nhận round & đề bài

```http
GET {{baseUrl}}/api/v1/rounds/{{prelimRoundId}}
Authorization: Bearer {{coordToken}}
```

| # | Kiểm tra | Pass khi | ☐ |
|---|----------|----------|---|
| B.1.1 | `problemReleasedAt` hoặc problem URL | Đã phát đề (seed) | |
| B.1.2 | `defaultPresentationMinutes` / `defaultQaMinutes` | `10` / `5` (hoặc giá trị seed) | |
| B.1.3 | Track `GET /tracks/{track1Id}` | `presentationMinutes` / `qaMinutes` null hoặc override | |

**Tùy chọn — chỉnh sớm (GĐ1 field trên CRUD):**

```http
PUT {{baseUrl}}/api/v1/rounds/{{prelimRoundId}}
Authorization: Bearer {{coordToken}}
Content-Type: application/json
```

Thêm vào body round (cùng các field bắt buộc khác): `"defaultPresentationMinutes": 10`, `"defaultQaMinutes": 5`.

---

### B.2 Student — nộp bài **multipart** (luồng mới)

Login `student.gd3.leader04@fpt.edu.vn`:

```http
GET {{baseUrl}}/api/v1/me/teams
Authorization: Bearer {{student04Token}}
```

→ Lưu `teamId`, `trackId`.

```http
POST {{baseUrl}}/api/v1/submissions
Authorization: Bearer {{student04Token}}
Content-Type: multipart/form-data
```

**Form-data:**

| Key | Value |
|-----|-------|
| `teamId` | `{{teamId}}` |
| `trackId` | `{{trackId}}` |
| `repoUrl` | `https://github.com/octocat/Hello-World` |
| `slideFile` | file `sample-slide.pdf` |

| # | Kiểm tra | Pass khi | ☐ |
|---|----------|----------|---|
| B.2.1 | HTTP | `201` | |
| B.2.2 | `data.slideFile` | Có tên file (vd. `slide.pdf`) | |
| B.2.3 | `data.displayCode` | `#` + submission id | |
| B.2.4 | `data.status` | `SUBMITTED` hoặc `LATE_PENDING` (nếu sau deadline) | |
| B.2.5 | File disk | Tồn tại dưới `uploads/submissions/.../slide.pdf` | |

Lưu `{{newSubmissionId}}` = `data.id`.

**Xem / tải slide (cùng endpoint):**

```http
GET {{baseUrl}}/api/v1/submissions/{{newSubmissionId}}/slide
Authorization: Bearer {{student04Token}}
```

```http
GET {{baseUrl}}/api/v1/submissions/{{newSubmissionId}}/slide?download=true
Authorization: Bearer {{student04Token}}
```

| # | Kiểm tra | Pass khi | ☐ |
|---|----------|----------|---|
| B.2.6 | Xem (mặc định) | `200`, PDF, `Content-Disposition: inline` | |
| B.2.7 | Tải (`?download=true`) | `200`, `Content-Disposition: attachment` | |

---

### B.3 Presentation — **shuffle** tạo queue (luồng mới)

> Shuffle **xóa** slot cũ của track và tạo lại từ bài gradable. Dùng **Coordinator** (hoặc HEAD judge nếu đã grant).

**Track 1:**

```http
POST {{baseUrl}}/api/v1/presentation/queue/shuffle
Authorization: Bearer {{coordToken}}
Content-Type: application/json

{
  "roundId": {{prelimRoundId}},
  "trackIds": [{{track1Id}}]
}
```

| # | Kiểm tra | Pass khi | ☐ |
|---|----------|----------|---|
| B.3.1 | HTTP | `200` | |
| B.3.2 | `data.tracks[0].slotCount` | ≥ 1 (có bài gradable track 1) | |
| B.3.3 | `data.tracks[0].shuffled` | `true` | |

**Xem queue — cấu trúc mới `tracks[]`:**

```http
GET {{baseUrl}}/api/v1/presentation/queue?roundId={{prelimRoundId}}&trackId={{track1Id}}
Authorization: Bearer {{coordToken}}
```

| # | Kiểm tra | Pass khi | ☐ |
|---|----------|----------|---|
| B.3.4 | Shape | Có `tracks[]`, **không** có `groups[]` | |
| B.3.5 | Item đầu | `status: "PRESENTING"` | |
| B.3.6 | Item đầu | Có `submissionId`, `displayCode`, `timer` | |
| B.3.7 | Coordinator thấy team | `teamId`, `teamName` **có giá trị** | |

Lưu `{{presentingSubmissionId}}` = `submissionId` của item `PRESENTING` track 1.

**Shuffle track 2 (tuỳ chọn):**

```http
POST .../shuffle  body: { "roundId": ..., "trackIds": [{{track2Id}}] }
```

| # | Kiểm tra | Pass khi | ☐ |
|---|----------|----------|---|
| B.3.8 | Track 2 slotCount | ≥ 1 (team 05, 06 gradable) | |

### B.3b Coordinator — cấu hình thời lượng (trước start timer)

> Chỉ `PUT` **sau shuffle, trước** `timer/start`. Sau start → `422 INVALID_STATE`. BE cascade `presentationSchedule`.

```http
PUT {{baseUrl}}/api/v1/presentation/duration
Authorization: Bearer {{coordToken}}
Content-Type: application/json

{
  "roundId": {{prelimRoundId}},
  "trackId": {{track1Id}},
  "presentationMinutes": 12,
  "qaMinutes": 8
}
```

| # | Kiểm tra | Pass khi | ☐ |
|---|----------|----------|---|
| B.3b.1 | HTTP | `200` | |
| B.3b.2 | `data.effectivePresentationMinutes` | `12` | |
| B.3b.3 | `GET .../presentation/queue` | `presentationSchedule` khớp khung mới | |
| B.3b.4 | Sau `timer/start`, gọi lại PUT | `422` | |

**GĐ5:** body `{ "roundId": {{finalRoundId}}, "presentationMinutes", "qaMinutes" }` — không `trackId`.

---

### B.4 Judge ẩn danh — không thấy tên đội

Login `judge1@fpt.edu.vn`:

```http
GET {{baseUrl}}/api/v1/me/judge/submissions?roundId={{prelimRoundId}}&trackId={{track1Id}}
Authorization: Bearer {{judge1Token}}
```

| # | Kiểm tra | Pass khi | ☐ |
|---|----------|----------|---|
| B.4.1 | HTTP | `200` | |
| B.4.2 | Mỗi item | Có `submissionId`, `displayCode` (`#nn`) | |
| B.4.3 | Mỗi item | **Không** có field `teamName` / `teamId` | |
| B.4.4 | Có `repoUrl`, `slideFile` | `slideFile` khác null khi đã nộp PDF | |

```http
GET {{baseUrl}}/api/v1/presentation/queue?roundId={{prelimRoundId}}&trackId={{track1Id}}
Authorization: Bearer {{judge1Token}}
```

| # | Kiểm tra | Pass khi | ☐ |
|---|----------|----------|---|
| B.4.5 | Queue ẩn danh | `teamId: null`, `teamName: null` trên items | |
| B.4.6 | Vẫn thấy | `submissionId`, `displayCode`, `order`, `status` | |

---

### B.5 Gate chấm — **SCORING_NOT_OPEN** rồi mở

Lấy `{{criterionIdT2}}` từ track 2 (team 06 chấm dở).

**B.5.1 — Chấm khi slot WAITING (phải FAIL):**

Tìm submission track 2 đang `WAITING` (không phải PRESENTING) sau shuffle, hoặc reset timer:

```http
POST {{baseUrl}}/api/v1/scores
Authorization: Bearer {{judge1Token}}
Content-Type: application/json

{
  "submissionId": {{waitingSubmissionId}},
  "criterionId": {{criterionIdT2}},
  "scoreValue": 7,
  "scoreType": "NORMAL"
}
```

| # | Kiểm tra | Pass khi | ☐ |
|---|----------|----------|---|
| B.5.1 | HTTP | `422` | |
| B.5.2 | `error.code` | `SCORING_NOT_OPEN` | |

**B.5.2 — Start timer → chấm được:**

> Production: dùng `{{controllerToken}}` (judge HEAD track). `coordToken` vẫn bypass BE (dev) — không dùng trên UI judge.

```http
POST {{baseUrl}}/api/v1/presentation/timer/start?roundId={{prelimRoundId}}&trackId={{track2Id}}
Authorization: Bearer {{controllerToken}}
```

| # | Kiểm tra | Pass khi | ☐ |
|---|----------|----------|---|
| B.5.3 | Timer start | `200`, `data.timer.phase` = `PRESENTING` | |
| B.5.4 | `presentationStartedAt` | Có giá trị | |

Chấm team 06 (submission đang PRESENTING track 2):

```http
POST {{baseUrl}}/api/v1/scores
Authorization: Bearer {{judge1Token}}
Content-Type: application/json

{
  "submissionId": {{sub6Id}},
  "criterionId": {{criterionIdT2}},
  "scoreValue": 7.5,
  "comment": "E2E after timer start",
  "scoreType": "NORMAL"
}
```

| # | Kiểm tra | Pass khi | ☐ |
|---|----------|----------|---|
| B.5.5 | HTTP | `200` hoặc `201` | |
| B.5.6 | Score lưu | `GET /me/scores?roundId=` judge thấy `displayCode` | |

---

### B.6 Timer — pause / resume

```http
POST {{baseUrl}}/api/v1/presentation/timer/pause?roundId={{prelimRoundId}}&trackId={{track2Id}}
Authorization: Bearer {{controllerToken}}
```

| # | Kiểm tra | Pass khi | ☐ |
|---|----------|----------|---|
| B.6.1 | `timer.phase` | `PAUSED` | |
| B.6.2 | `pausedAt` | Có giá trị | |

```http
POST .../timer/resume?roundId=...&trackId=...
```

| # | Kiểm tra | Pass khi | ☐ |
|---|----------|----------|---|
| B.6.3 | `timer.phase` | `PRESENTING` hoặc `QA` | |
| B.6.4 | `pausedAccumulatedSeconds` | ≥ 0 | |

---

### B.7 Queue next — chuyển bài

**B.7.0 — Next khi chưa chấm (negative):**

Gọi `PATCH queue/next` **trước** B.5.5 → `422` `SCORING_INCOMPLETE_BEFORE_NEXT`. FE nên confirm trước khi gửi lại với `acknowledgeIncompleteScoring: true` (khi thiếu judge, không phải khi chưa có điểm).

**B.7.1 — Next sau khi đã chấm:**

```http
PATCH {{baseUrl}}/api/v1/presentation/queue/next?roundId={{prelimRoundId}}&trackId={{track2Id}}
Authorization: Bearer {{controllerToken}}
Content-Type: application/json

{
  "currentSubmissionId": {{sub6Id}}
}
```

| # | Kiểm tra | Pass khi | ☐ |
|---|----------|----------|---|
| B.7.1 | HTTP | `200` | |
| B.7.2 | Slot cũ | `DONE` trong GET queue | |
| B.7.3 | Slot mới | `PRESENTING` (nếu còn WAITING) | |
| B.7.4 | Slot mới `timer.phase` | **`SETUP`** (chưa chấm / chưa start) | |
| B.7.5 | `nextSubmissionId` | Khớp bài tiếp theo hoặc null hết queue | |
| B.7.6 | Sau `timer/start` đội mới | `timer.phase` = `PRESENTING` → chấm được | |

---

### B.8 Late submission — duyệt bài trễ

```http
GET {{baseUrl}}/api/v1/submissions?status=LATE_PENDING&roundId={{prelimRoundId}}
Authorization: Bearer {{coordToken}}
```

| # | Kiểm tra | Pass khi | ☐ |
|---|----------|----------|---|
| B.8.1 | Có team 02 | `lateSubmissionId` trong list | |

```http
PATCH {{baseUrl}}/api/v1/submissions/{{lateSubmissionId}}/approve
Authorization: Bearer {{coordToken}}
Content-Type: application/json

{}
```

| # | Kiểm tra | Pass khi | ☐ |
|---|----------|----------|---|
| B.8.2 | Status | `LATE_APPROVED` | |
| B.8.3 | Shuffle lại track 1 | Bài late có thể xuất hiện cuối queue (nếu gradable) | |

---

### B.9 Mentor portal

```http
GET {{baseUrl}}/api/v1/me/mentor/rounds
Authorization: Bearer {{mentorToken}}
```

```http
GET {{baseUrl}}/api/v1/me/mentor/rounds/{{prelimRoundId}}/assigned-teams
Authorization: Bearer {{mentorToken}}
```

| # | Kiểm tra | Pass khi | ☐ |
|---|----------|----------|---|
| B.9.1 | Rounds | Có prelim | |
| B.9.2 | Assigned teams | ≥ 1 team, có presentation info | |

---

### B.10 Coordinator — tiến độ chấm & khóa sổ (kết thúc GĐ3)

```http
GET {{baseUrl}}/api/v1/rounds/{{prelimRoundId}}/scoring-progress
Authorization: Bearer {{coordToken}}
```

| # | Kiểm tra | Pass khi | ☐ |
|---|----------|----------|---|
| B.10.1 | `gradable` | ≥ 4 (loại LATE_PENDING chưa duyệt) | |
| B.10.2 | Progress | Phản ánh scores `isFinal=false` | |

```http
PATCH {{baseUrl}}/api/v1/rounds/{{prelimRoundId}}/lock-scoring
Authorization: Bearer {{coordToken}}
Content-Type: application/json

{ "force": false }
```

| # | Kiểm tra | Pass khi | ☐ |
|---|----------|----------|---|
| B.10.3 | `scoringLocked` | `true` | |
| B.10.4 | Chấm thêm | Judge `POST /scores` → `ScoringLockedException` / 403 | |

```http
GET {{baseUrl}}/api/v1/rounds/{{prelimRoundId}}/ranking
Authorization: Bearer {{coordToken}}
```

| # | Kiểm tra | Pass khi | ☐ |
|---|----------|----------|---|
| B.10.5 | Ranking | Có danh sách team + điểm | |
| B.10.6 | **Chưa** publish | `isPublished` vẫn `false` (GĐ4 mới publish) | |

---

## 5. Phần C — Test âm tính (phải FAIL đúng)

| # | Kịch bản | API | Kỳ vọng | ☐ |
|---|----------|-----|---------|---|
| C.1 | Nộp thiếu PDF | `POST /submissions` multipart không `slideFile` | `400` `SLIDE_FILE_REQUIRED` | |
| C.2 | Nộp file `.txt` | `slideFile` = text | `400` `INVALID_SLIDE_FILE` | |
| C.3 | repoUrl GitLab | `repoUrl` không phải GitHub | `400` `INVALID_REPO_PLATFORM` | |
| C.4 | Judge chưa assign | Judge khác login `GET /me/judge/submissions` round chưa gán | `403` | |
| C.5 | Timer judge thường | `judge2` (không HEAD) gọi `timer/start` không grant | `403` | |
| C.6 | Coordinator timer | `coord` gọi `timer/start` | `200` (bypass dev — UI không expose) | |
| C.7 | Next chưa chấm | `PATCH queue/next` trước score | `422` `SCORING_INCOMPLETE_BEFORE_NEXT` | |
| C.7 | Next thiếu trackId | `PATCH .../next?roundId=` không `trackId` (prelim) | `422` validation | |
| C.8 | Shuffle khi locked | Sau B.10 lock, gọi shuffle | `422` round đã khóa | |

---

## 6. Phần D — WebSocket (tùy chọn)

1. Kết nối STOMP tới endpoint WS của BE — xem [06-live-scoring-websocket.md](../mf03/06-live-scoring-websocket.md).
2. Subscribe:

```text
/topic/rounds/{{prelimRoundId}}/tracks/{{track1Id}}/presentation-queue
```

3. Gọi `POST .../shuffle` hoặc `timer/start`.
4. Nhận message body dạng `PresentationQueueResponse` (có `tracks`, `timer`).

| # | Kiểm tra | Pass khi | ☐ |
|---|----------|----------|---|
| D.1 | Sau shuffle | Client nhận 1 message queue đầy đủ | |
| D.2 | Sau timer/start | `timer.remainingSeconds` cập nhật | |

---

## 7. Bảng tổng kết Pass/Fail

Điền ngày test: _______________  
Người test: _______________

| Phần | Mô tả | Pass | Fail | Ghi chú |
|------|-------|------|------|---------|
| **A** | GĐ2 gate (`seal-spring-2026`) | ☐ | ☐ | |
| **B.0–B.1** | Bootstrap GĐ3 | ☐ | ☐ | |
| **B.2** | Multipart submit + GET slide | ☐ | ☐ | |
| **B.3** | Shuffle + queue `tracks[]` | ☐ | ☐ | |
| **B.4** | Judge ẩn danh | ☐ | ☐ | |
| **B.5** | Gate SCORING_NOT_OPEN + timer | ☐ | ☐ | |
| **B.6** | Pause / resume | ☐ | ☐ | |
| **B.7** | Queue next | ☐ | ☐ | |
| **B.8** | Late approve | ☐ | ☐ | |
| **B.9** | Mentor | ☐ | ☐ | |
| **B.10** | Lock + ranking | ☐ | ☐ | |
| **C** | Âm tính (≥ 6/8 pass) | ☐ | ☐ | |
| **D** | WebSocket (optional) | ☐ | ☐ | N/A |

**Kết luận:**

- ☐ **PASS** — Toàn bộ B.2–B.10 pass, C không có false positive  
- ☐ **FAIL** — Ghi step fail: _______________

---

## 8. Troubleshooting

| Triệu chứng | Nguyên nhân | Cách xử lý |
|-------------|-------------|------------|
| `403 SCORING_NOT_OPEN` khi đã start timer | Slot không phải `PRESENTING` hoặc sai `submissionId` | `GET queue` → shuffle lại → `timer/start` |
| Queue trống sau activate | Đúng spec — chưa shuffle | `POST .../shuffle` |
| `GET queue` lỗi parse FE | Dùng `groups` cũ | Đổi sang `data.tracks[].items[]` |
| Multipart 415 | Sai Content-Type | Phải `multipart/form-data`, không JSON |
| GitHub `REPO_NOT_PUBLIC` | Repo private / mạng | Tắt `github-public-check-enabled` hoặc dùng repo public |
| File slide không thấy trên disk | `app.storage.type=minio` | Kiểm tra MinIO console hoặc đổi `local` |
| GĐ2 lottery `TEAM_NOT_LOCKED` | Team chưa khóa | Dùng `GD2-05` hoặc đợi `registrationEnd` |
| Seed deadline khiến mọi bài `LATE_PENDING` | Repair ngày lệch | Restart app dev → xem log `[Gd3DataSeeder] FE repair` |

---

## Luồng tối thiểu (~15 phút) — smoke GĐ3 v4.1

Nếu thiếu thời gian, chỉ chạy:

```
B.0 → B.2 (leader04 multipart) → B.3 (shuffle track1)
→ B.4 (judge submissions ẩn danh)
→ B.5.1 (403) → B.5.2 timer start → B.5.5 (score OK)
→ B.10.3 (lock-scoring) → B.10.5 (ranking)
```

Pass smoke = luồng mới **cốt lõi đúng**.

---

*Cập nhật file này khi thêm bước test hoặc đổi seed slug.*
