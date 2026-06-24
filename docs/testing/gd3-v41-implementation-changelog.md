# GĐ3 v4.1 — Nhật ký thay đổi triển khai (BE)

> **Phạm vi:** Lưu bài nộp (PDF + GitHub), mã bài chấm ẩn danh, random thuyết trình theo track, đồng hồ presentation, WebSocket đồng bộ, gate chấm điểm.  
> **Cập nhật:** 2026-06-09  
> **Kết quả test:** `mvn test` — **160/160 pass**  
> **Liên quan:** [fe-gd3-api-mapping.md](fe-gd3-api-mapping.md) · [e2e-gd2-gd3-v41-manual-test.md](e2e-gd2-gd3-v41-manual-test.md) · [full-workflow-api-test-gd1-gd6.md](full-workflow-api-test-gd1-gd6.md)

---

## Mục lục

1. [Tổng quan](#1-tổng-quan)
2. [Danh mục file thay đổi (~78 file)](#2-danh-mục-file-thay-đổi-78-file)
3. [Lưu trữ slide PDF (MinIO / local)](#3-lưu-trữ-slide-pdf-minio--local)
4. [Nộp bài multipart + GET slide](#4-nộp-bài-multipart--get-slide)
5. [Judge ẩn danh theo submissionId](#5-judge-ẩn-danh-theo-submissionid)
6. [Presentation queue — shuffle, next, cấu trúc mới](#6-presentation-queue--shuffle-next-cấu-trúc-mới)
7. [Đồng hồ thuyết trình (timer)](#7-đồng-hồ-thuyết-trình-timer)
8. [Phân quyền presentation controller](#8-phân-quyền-presentation-controller)
9. [WebSocket presentation-queue](#9-websocket-presentation-queue)
10. [Gate chấm điểm (SCORING_NOT_OPEN)](#10-gate-chấm-điểm-scoring_not_open)
11. [Schema DB & migration](#11-schema-db--migration)
12. [Seed dev (Gd3DataSeeder)](#12-seed-dev-gd3dataseeder)
13. [Error codes & audit mới](#13-error-codes--audit-mới)
14. [Hướng dẫn test](#14-hướng-dẫn-test)
15. [Breaking changes & tương thích](#15-breaking-changes--tương-thích)
16. [Phụ lục — API surface đầy đủ](#16-phụ-lục--api-surface-đầy-đủ)

---

## 1. Tổng quan

### 1.1 Mục tiêu nghiệp vụ

| # | Yêu cầu | Trạng thái |
|---|---------|------------|
| 1 | Student nộp `repoUrl` GitHub public + file PDF slide | ✅ Multipart + storage |
| 2 | Judge chỉ thấy `submissionId` / `displayCode` (`#31`), không tên đội | ✅ Portal + list ẩn danh |
| 3 | Random thuyết trình **theo track** (Fisher-Yates), tạo slot khi shuffle | ✅ `POST .../shuffle` |
| 4 | Đồng hồ presentation + Q&A, pause/resume | ✅ Timer service |
| 5 | HEAD judge / Coordinator điều khiển queue & timer | ✅ `PresentationControllerGuard` |
| 6 | WS đồng bộ queue sau shuffle/next/timer | ✅ `PresentationQueuePublisher` |
| 7 | Chấm NORMAL chỉ khi slot `PRESENTING` + round `JUDGING` | ✅ `ScoreServiceImpl` |

### 1.2 Luồng GĐ3 sau thay đổi

```
activate round (GĐ2 gate)
    → student POST /submissions (multipart PDF + repoUrl)
    → coordinator/judge HEAD: POST /presentation/queue/shuffle
    → POST /presentation/timer/start (hoặc next)
    → judge GET /me/judge/submissions (ẩn danh)
    → judge POST /scores (khi slot PRESENTING)
    → coordinator PATCH /rounds/{id}/lock-scoring
```

**Lưu ý:** `activate` **không** tạo `presentation_slots` — slot chỉ có sau **shuffle** (hoặc seed dev).

---

## 2. Danh mục file thay đổi (~78 file)

### 2.1 Infrastructure & config (4)

| File | Loại | Nội dung thay đổi |
|------|------|-------------------|
| `docker-compose.minio.yml` | **Mới** | MinIO ports 9000/9001, volume `minio_data` |
| `pom.xml` | Sửa | Thêm `software.amazon.awssdk:s3` (MinIO S3-compatible) |
| `src/main/resources/application-dev.properties` | Sửa | `app.storage.*`, `app.submission.github-public-check-enabled` |
| `src/main/java/.../config/Gd03V41SchemaMigration.java` | Sửa | Cột GĐ3 presentation + slide; H2-safe `indexExists`/`tableExists` |

### 2.2 Object storage (7 file mới)

| File | Vai trò |
|------|---------|
| `storage/ObjectStorageService.java` | Interface `put` / `get` / `delete` |
| `storage/LocalFilesystemObjectStorageService.java` | Dev default — ghi file local |
| `storage/MinioObjectStorageService.java` | Production — bucket S3 API |
| `storage/StorageProperties.java` | Bind `app.storage.*` |
| `storage/StorageConfig.java` | `@EnableConfigurationProperties` |
| `storage/StoredObject.java` | DTO stream + contentType |

### 2.3 Submissions (10)

| File | Thay đổi |
|------|----------|
| `submissions/entity/Submission.java` | Cột `slide_storage_key`, `slide_original_filename`, `slide_content_type`, `slide_size_bytes`, `slide_uploaded_at` |
| `submissions/support/GitHubRepoValidator.java` | **Mới** — validate URL GitHub + HEAD check public |
| `submissions/support/SubmissionSlideStorage.java` | **Mới** — validate PDF, upload, build key |
| `submissions/service/SubmissionService.java` | `submitMultipart`, `getSlide` |
| `submissions/service/impl/SubmissionServiceImpl.java` | Multipart flow; `toResponse(s, anonymous)` cho JUDGE |
| `submissions/controller/SubmissionController.java` | `POST` multipart + `GET /{id}/slide`; giữ `POST` JSON legacy |
| `submissions/dto/response/SubmissionResponse.java` | `displayCode`, `hasSlide` |

### 2.4 Judge portal (6)

| File | Thay đổi |
|------|----------|
| `me/controller/JudgeMeController.java` | `GET /me/judge/submissions` |
| `me/judge/dto/response/JudgeSubmissionListItemResponse.java` | **Mới** — không có `teamName` |
| `me/judge/dto/response/JudgeScoreSummaryResponse.java` | Bỏ `teamId`, thêm `displayCode` |
| `me/judge/service/JudgePortalService.java` | `listSubmissions(roundId, trackId)` |
| `me/judge/service/impl/JudgePortalServiceImpl.java` | Lọc gradable + assignment scope |
| `judge_assignments/repository/JudgeAssignmentRepository.java` | `existsByJudgeIdAndRoundScope` |

### 2.5 Presentation module (28+)

**Controllers (mới / sửa):**

| File | Endpoints |
|------|-----------|
| `presentation/controller/PresentationQueueController.java` | `GET` queue, `POST /shuffle`, `PATCH /next` |
| `presentation/controller/PresentationTimerController.java` | **Mới** — `start/pause/resume/qa/reset/next` |
| `presentation/controller/PresentationControllerController.java` | **Mới** — grant/revoke controller track/round |

**Services:**

| File | Logic chính |
|------|-------------|
| `PresentationQueueServiceImpl.java` | Queue theo `tracks[]`; shuffle tạo slot; Fisher-Yates; ẩn danh JUDGE |
| `PresentationTimerServiceImpl.java` | Timer state machine trên slot `PRESENTING` |
| `PresentationControllerServiceImpl.java` | Grant HEAD / FINAL_EXTERNAL lên `Track` / `Round` |
| `PresentationSlotCascadeServiceImpl.java` | Cascade khi xóa round (giữ nhóm nội bộ) |

**Support / guard / value objects (mới):**

| File | Mô tả |
|------|-------|
| `guard/PresentationControllerGuard.java` | Coordinator bypass; HEAD mặc định track; round controller CK |
| `support/RoundPhaseResolver.java` | SETUP → CODING → JUDGING → SCORING_LOCKED → PUBLISHED |
| `support/PresentationDurationResolver.java` | Track override → round default → 10+5 phút |
| `support/PresentationTimerCalculator.java` | `remainingSeconds` khi PRESENTING/QA/PAUSED |
| `value_object/PresentationTimerPhase.java` | IDLE, PRESENTING, QA, PAUSED, ENDED |
| `value_object/RoundPhase.java` | Enum phase round |

**DTOs (mới / sửa):** `PresentationShuffleRequest/Response`, `PresentationTimerBlock`, `PresentationControllerGrantRequest`, `PresentationQueueResponse` (đổi `groups` → `tracks[].items[]`), ...

**Entity / repository:**

| File | Cột / method mới |
|------|------------------|
| `events/entity/PresentationSlot.java` | `submission_id`, `track_id`, `timer_phase`, `presentation_started_at`, `qa_started_at`, `paused_at`, `paused_accumulated_seconds` |
| `events/repository/PresentationSlotRepository.java` | Query theo `round+track`, `deleteByRound_IdAndTrack_Id`, ... |
| `rounds/entity/Round.java` | `default_presentation_minutes`, `default_qa_minutes`, `controller_judge_id` |
| `tracks/entity/Track.java` | `presentation_minutes`, `qa_minutes`, `controller_judge_id`, `presentation_shuffled` |

### 2.6 Scoring & live (3)

| File | Thay đổi |
|------|----------|
| `scores/service/impl/ScoreServiceImpl.java` | `requireScoringOpen()` trước `POST /scores` NORMAL |
| `live_scoring/PresentationQueuePublisher.java` | **Mới** — broadcast WS |
| `live_scoring/security/StompSubscribeAuthorizationInterceptor.java` | Cho phép subscribe topic `presentation-queue` |

### 2.7 Common (2)

| File | Thêm |
|------|------|
| `common/exception/ErrorCode.java` | `REPO_NOT_PUBLIC`, `SLIDE_FILE_REQUIRED`, `INVALID_SLIDE_FILE`, `SCORING_NOT_OPEN`, `NOT_TRACK_CONTROLLER` |
| `common/audit/AuditAction.java` | `PRESENTATION_QUEUE_SHUFFLE`, `PRESENTATION_CONTROLLER_GRANTED`, `PRESENTATION_CONTROLLER_REVOKED` |

### 2.8 Seed & docs (3)

| File | Thay đổi |
|------|----------|
| `config/seed/Gd3DataSeeder.java` | Slot `PRESENTING` cho test chấm; gắn `submission`/`track` trên slot |
| `docs/testing/fe-gd3-api-mapping.md` | Multipart, shuffle, timer, judge ẩn danh, error codes |
| `docs/testing/full-workflow-api-test-gd1-gd6.md` | Cập nhật kịch bản GĐ3 |

### 2.9 Tests (18+)

**Test GĐ3 mới (10 class):**

| File | Nội dung verify |
|------|-----------------|
| `submissions/support/SubmissionSlideStorageTest.java` | Upload PDF → load round-trip |
| `submissions/support/GitHubRepoValidatorTest.java` | Reject non-GitHub, Drive; accept khi tắt public check |
| `me/judge/.../JudgePortalServiceTest.java` | List không expose `teamName` |
| `presentation/guard/PresentationControllerAuthTest.java` | HEAD OK; Coordinator OK; judge khác 403 |
| `presentation/support/RoundPhaseResolverTest.java` | 5 phase từ cờ round |
| `presentation/support/PresentationDurationResolverTest.java` | Track override vs round default |
| `presentation/support/PresentationTimerPauseTest.java` | PAUSED freeze `remainingSeconds` |
| `presentation/service/impl/PresentationQueueShuffleTest.java` | Shuffle tạo slot, slot đầu PRESENTING |
| `presentation/service/impl/PresentationQueueAnonymityTest.java` | JUDGE queue ẩn team |
| `scores/service/impl/ScoringWindowTest.java` | WAITING→403; PRESENTING→OK; locked→exception |

**Test sửa (mock thiếu — không đổi logic prod):**  
`RoundServiceImplExamValidationTest`, `RoundServiceImplDeleteTest`, `RoundServiceImplRoundTypeUniqueTest`, `RoundServiceImplSequenceTest`, `RoundActivationServiceImplTest`, `TrackServiceImplCreateSequenceTest`, `PersonnelAssignmentCrossTrackTest`, `CriteriaServiceImplFinalRoundTest`, `AuthOnboardingFlowIntegrationTest`.

---

## 3. Lưu trữ slide PDF (MinIO / local)

### 3.1 Cấu hình

```properties
# application-dev.properties
app.storage.type=local                    # hoặc minio
app.storage.local-dir=uploads/submissions # thư mục gốc khi type=local
app.storage.submission-slide-max-mb=25

# MinIO (khi type=minio)
app.storage.minio.endpoint=http://localhost:9000
app.storage.minio.access-key=minioadmin
app.storage.minio.secret-key=minioadmin
app.storage.minio.bucket=seal-submissions
```

**Chạy MinIO:**

```bash
docker compose -f docker-compose.minio.yml up -d
```

### 3.2 Vị trí file upload

| Môi trường | Backend | Đường dẫn object key |
|------------|---------|----------------------|
| **Local (default dev)** | `LocalFilesystemObjectStorageService` | `{app.storage.local-dir}/submissions/{hackathonId}/{roundId}/{submissionId}/slide.pdf` |
| **MinIO** | `MinioObjectStorageService` | Bucket `seal-submissions`, key giống pattern trên |

**Pattern key (canonical):**

```
submissions/{hackathonId}/{roundId}/{submissionId}/slide.pdf
```

Ví dụ local: `uploads/submissions/submissions/1/12/31/slide.pdf`  
*(prefix `submissions/` nằm trong key — full path = `local-dir` + key)*

### 3.3 Metadata DB (`submissions`)

| Cột | Mô tả |
|-----|-------|
| `slide_storage_key` | Key object storage |
| `slide_original_filename` | Tên file gốc |
| `slide_content_type` | `application/pdf` |
| `slide_size_bytes` | Kích thước |
| `slide_uploaded_at` | Thời điểm upload |
| `slide_url` | Legacy — set `null` khi upload PDF mới |

### 3.4 Validation PDF (`SubmissionSlideStorage`)

- Bắt buộc có file → `SLIDE_FILE_REQUIRED`
- Content-Type / đuôi `.pdf` / magic bytes `%PDF` → `INVALID_SLIDE_FILE`
- Max size → `app.storage.submission-slide-max-mb` (default 25MB)
- Resubmit: xóa object cũ trước khi ghi mới

---

## 4. Nộp bài multipart + GET slide

### 4.1 API

**Canonical (GĐ3 v4.1):**

```http
POST /api/v1/submissions
Content-Type: multipart/form-data
Authorization: Bearer {studentToken}
```

| Part | Bắt buộc | Ghi chú |
|------|----------|---------|
| `teamId` | ✅ | |
| `trackId` | ✅ (prelim) | |
| `repoUrl` | ✅ | `https://github.com/{owner}/{repo}` |
| `slideFile` | ✅ | PDF |
| `roundId` | ❌ | BE suy từ track |
| `lateReason` | ❌ | Khi nộp trễ |

**Xem / tải slide (cùng endpoint):**

```http
GET /api/v1/submissions/{submissionId}/slide
→ 200 application/pdf, Content-Disposition: inline

GET /api/v1/submissions/{submissionId}/slide?download=true
→ 200 application/pdf, Content-Disposition: attachment
```

**Legacy (giữ tương thích):**

```http
POST /api/v1/submissions
Content-Type: application/json
{ "teamId", "trackId", "repoUrl", "slideUrl", ... }
```

### 4.2 GitHub validation (`GitHubRepoValidator`)

- Regex: `https://github.com/{owner}/{repo}`
- Từ chối `drive.google.com` → `INVALID_REPO_PLATFORM`
- Nếu `app.submission.github-public-check-enabled=true`: HTTP HEAD repo → 404 → `REPO_NOT_PUBLIC`

### 4.3 Code path

```
SubmissionController.submitMultipart()
  → SubmissionServiceImpl.submitMultipart()
      → GitHubRepoValidator.validatePublicGitHubRepo(repoUrl)
      → SubmissionSlideStorage.validatePdf(slideFile)
      → save submission
      → SubmissionSlideStorage.storeSlide() → ObjectStorageService.put()
```

---

## 5. Judge ẩn danh theo submissionId

### 5.1 API mới

```http
GET /api/v1/me/judge/submissions?roundId={id}&trackId={id}
Authorization: Bearer {judgeToken}
```

**Response mẫu:**

```json
{
  "submissionId": 31,
  "displayCode": "#31",
  "trackId": 8,
  "trackName": "EVSWAP",
  "status": "SUBMITTED",
  "hasSlide": true,
  "repoUrl": "https://github.com/org/repo"
}
```

**Không có:** `teamId`, `teamName`.

### 5.2 Ẩn danh ở các API khác (chỉ role `JUDGE`)

| API | Field bị ẩn |
|-----|-------------|
| `GET /api/v1/submissions` | `teamId`, `teamName`, `slideUrl`, `demoUrl`, `reportUrl`, late review fields |
| `GET /api/v1/presentation/queue` | `teamId`, `teamName` trên từng `items[]` |

**Coordinator / Mentor / Student:** không bị ẩn.

### 5.3 `JudgeScoreSummaryResponse`

- **Trước:** có `teamId`
- **Sau:** `displayCode` (`#` + `submissionId`), không `teamId`

---

## 6. Presentation queue — shuffle, next, cấu trúc mới

### 6.1 Thay đổi cấu trúc response

**Trước (doc cũ):**

```json
{ "groups": [{ "groupName": "Bảng A", "teams": [...] }] }
```

**Sau:**

```json
{
  "roundId": 12,
  "tracks": [
    {
      "trackId": 8,
      "trackName": "EVSWAP",
      "shuffled": true,
      "items": [
        {
          "submissionId": 31,
          "displayCode": "#31",
          "teamId": 41,
          "teamName": "GD3-01",
          "order": 1,
          "status": "PRESENTING",
          "timer": { "phase": "IDLE", "remainingSeconds": 600, ... }
        }
      ]
    }
  ],
  "roomStats": { "total": 4, "done": 0, "absent": 0 }
}
```

- Nhóm theo **track**, không theo `assignedGroup` / bảng đấu.
- Chung kết: một nhóm `trackId: null`, `trackName: "Chung kết"`.

### 6.2 Shuffle — tạo slot (`PresentationQueueServiceImpl.shuffle`)

**API:**

```http
POST /api/v1/presentation/queue/shuffle
{ "roundId": 12, "trackIds": [8] }   // trackIds optional = tất cả track
```

**Logic:**

1. Xóa slot cũ của track/round (`deleteByRound_IdAndTrack_Id`)
2. Lấy submission **gradable** (`SubmissionGradablePolicy`) theo track
3. Fisher-Yates shuffle
4. Tạo `presentation_slots`: slot #1 → `PRESENTING`, còn lại → `WAITING`
5. Gán `submission_id`, `track_id`, `timer_phase=IDLE`
6. Set `tracks.presentation_shuffled = true`
7. Audit `PRESENTATION_QUEUE_SHUFFLE`
8. WS publish (§9)

**Chung kết:** nguồn từ `team_round_participation` + submission mới nhất mỗi team.

### 6.3 Next

```http
PATCH /api/v1/presentation/queue/next?roundId=12&trackId=8
Body (optional): {
  "currentSubmissionId": 31,
  "acknowledgeIncompleteScoring": false
}
```

**Guard (`PresentationNextScoringGuard`):**

- Chưa có điểm NORMAL → `422` `SCORING_INCOMPLETE_BEFORE_NEXT` (`NO_SCORES`)
- Nhiều judge, chưa đủ người chấm ≥1 lần → cần `acknowledgeIncompleteScoring: true` sau confirm FE (`MISSING_JUDGE_SCORES`)
- Response có `completedSubmissionScoring` snapshot

- Slot hiện tại `PRESENTING` → `DONE`, timer `ENDED`
- Slot `WAITING` tiếp theo → `PRESENTING`, `timer.phase=SETUP`
- **Sơ loại:** `trackId` bắt buộc

**IT Java:** `Gd2Gd3FlowIntegrationTest` — IT-08 (next chưa chấm), IT-09 (next → SETUP).

### 6.4 LATE_APPROVED sau shuffle

Submission `LATE_APPROVED` duyệt sau shuffle → append cuối queue (logic trong service khi có submission mới gradable).

---

## 7. Đồng hồ thuyết trình (timer)

### 7.1 Thời lượng (`PresentationDurationResolver`)

Ưu tiên:

1. `tracks.presentation_minutes` / `qa_minutes` (override)
2. `rounds.default_presentation_minutes` / `default_qa_minutes` (default **10** / **5**)
3. Hardcode 10 / 5 nếu null

### 7.2 State machine slot (`PresentationTimerPhase`)

```
IDLE → PRESENTING → QA → ENDED
         ↕ PAUSED (pause/resume, cộng paused_accumulated_seconds)
```

### 7.3 API timer

| Method | Path | Hành vi |
|--------|------|---------|
| POST | `/api/v1/presentation/timer/start?roundId=&trackId=` | Set `presentation_started_at`, phase PRESENTING |
| POST | `.../pause` | phase PAUSED, set `paused_at` |
| POST | `.../resume` | Cộng pause vào `paused_accumulated_seconds`, restore phase |
| POST | `.../qa` | phase QA, set `qa_started_at` |
| POST | `.../reset` | Clear mốc thời gian, phase IDLE |
| POST | `.../next` | Alias queue next + publish WS |

**Điều kiện:** slot đang `queue_status=PRESENTING`; caller phải pass `PresentationControllerGuard`.

### 7.4 Client đếm ngược

- Server trả `remainingSeconds`, `presentationStartedAt`, `pausedAt` trong `timer` block
- Client tự đếm; khi PAUSED dừng đếm (không spam tick từ server)

### 7.5 API cấu hình thời lượng (Coordinator — GĐ3/GĐ5)

| Method | Path | Hành vi |
|--------|------|---------|
| GET | `/api/v1/presentation/duration?roundId=&trackId=` | Đọc cấu hình + `effective*` sau resolve |
| PUT | `/api/v1/presentation/duration` | Ghi `rounds.default_*` hoặc `tracks.presentation_*` / `qa_*` |
| DELETE | `/api/v1/presentation/duration?roundId=&trackId=` | Gỡ override track (GĐ3) |

**Body PUT:** `{ roundId, trackId?, presentationMinutes, qaMinutes }` — `@Min(1)` cả hai phút.

**Guard (`PresentationDurationMutationGuard`):**

- Round `scoringLocked` → `422`
- Slot `DONE` hoặc timer phase `PRESENTING`/`QA`/`PAUSED`/`ENDED` trong phạm vi → `422` *"Buổi thuyết trình đã bắt đầu"*
- Cho phép sau shuffle nếu timer chưa start (`SETUP`/`IDLE`)

**Cascade:** sau PUT/DELETE thành công → `PresentationSlotCascadeService.rescheduleForRound` cập nhật `startsAt`/`endsAt`.

**GĐ1 (thiết lập sớm):** cùng field trên `GET/PUT /rounds/{id}` (`defaultPresentationMinutes`, `defaultQaMinutes`) và `GET/PUT /tracks/{id}` (`presentationMinutes`, `qaMinutes`).

**Audit:** `PRESENTATION_DURATION_UPDATED`.

---

## 8. Phân quyền presentation controller

> **Không có “operator” riêng.** Quyền điều khiển timer/next gắn **một judge** trên track (trong số **N** judge được phân công — không cố định số lượng); judge đó **vẫn chấm** `POST /scores`. Các judge khác cùng track chỉ chấm.

### 8.1 Mặc định

| Phạm vi | Ai giữ quyền điều khiển (đồng thời là judge chấm) |
|---------|-----------------------------------------------------|
| Track (sơ loại) | Judge **HEAD** của track (`JudgeAssignmentType.HEAD`) |
| Round (chung kết) | `rounds.controller_judge_id` (Coordinator gán judge CK) |

### 8.2 API grant (Coordinator only)

```http
PUT  /api/v1/presentation/tracks/{trackId}/controller
PUT  /api/v1/presentation/rounds/{roundId}/controller
GET  /api/v1/presentation/tracks/{trackId}/controller
DELETE /api/v1/presentation/tracks/{trackId}/controller
```

Body grant: `{ "judgeId": 12 }`

### 8.3 `PresentationControllerGuard`

| Role | shuffle / next / timer |
|------|------------------------|
| `COORDINATOR` | Luôn OK |
| Judge = `controller_judge_id` hoặc HEAD | OK |
| Judge khác | `403` (message: không có quyền điều khiển) |

---

## 9. WebSocket presentation-queue

### 9.1 Topics

| Topic | Khi nào |
|-------|---------|
| `/topic/rounds/{roundId}/tracks/{trackId}/presentation-queue` | Shuffle/next/timer theo track |
| `/topic/rounds/{roundId}/presentation-queue` | Round-level (final / coordinator) |

### 9.2 Publisher

`PresentationQueuePublisher.publish(roundId, trackId, PresentationQueueResponse)` — gọi sau shuffle, next, mọi action timer.

### 9.3 Subscribe auth

`StompSubscribeAuthorizationInterceptor` — pattern mới cho `presentation-queue`; judge phải được assign track/round tương ứng.

---

## 10. Gate chấm điểm (SCORING_NOT_OPEN)

### 10.1 Round phase (`RoundPhaseResolver`)

| Phase | Điều kiện |
|-------|-----------|
| `SETUP` | `!isActive` |
| `CODING` | `isActive` && `now < examAt` |
| `JUDGING` | `isActive` && `now >= examAt` && `!scoringLocked` |
| `SCORING_LOCKED` | `scoringLocked` |
| `PUBLISHED` | `isPublished` |

### 10.2 Ma trận `POST /api/v1/scores` (NORMAL)

| Round phase | Slot `queue_status` | Kết quả |
|-------------|---------------------|---------|
| `JUDGING` | `PRESENTING` | ✅ Cho chấm |
| `JUDGING` | `WAITING` | `403 SCORING_NOT_OPEN` |
| `CODING` / `SETUP` | bất kỳ | `403 SCORING_NOT_OPEN` |
| `SCORING_LOCKED` | bất kỳ | `ScoringLockedException` (cũ) |

### 10.3 Ngoại lệ

- `POST /api/v1/scores/calibration` — **không** qua gate PRESENTING
- Calibration chạy trước thi (GĐ5), không phụ thuộc presentation

### 10.4 Code

`ScoreServiceImpl.requireScoringOpen()` — tìm slot theo `submission_id` (fallback `team_id`).

---

## 11. Schema DB & migration

### 11.1 Cột mới (Gd03V41SchemaMigration)

**`rounds`**

- `default_presentation_minutes` INT DEFAULT 10
- `default_qa_minutes` INT DEFAULT 5
- `controller_judge_id` BIGINT NULL

**`tracks`**

- `presentation_minutes`, `qa_minutes` INT NULL
- `controller_judge_id` BIGINT NULL
- `presentation_shuffled` TINYINT DEFAULT 0

**`presentation_slots`**

- `submission_id`, `track_id` BIGINT NULL
- `timer_phase` VARCHAR DEFAULT 'IDLE'
- `timer_phase_before_pause` VARCHAR NULL
- `presentation_started_at`, `qa_started_at`, `paused_at` DATETIME NULL
- `paused_accumulated_seconds` INT DEFAULT 0

**`submissions`**

- `slide_storage_key`, `slide_original_filename`, `slide_content_type`
- `slide_size_bytes`, `slide_uploaded_at`

### 11.2 Chạy migration

- Tự chạy `@Order(0)` `CommandLineRunner` lúc startup
- Idempotent trên MySQL prod
- H2 test: skip lỗi `information_schema`; rely `ddl-auto=create-drop`

---

## 12. Seed dev (Gd3DataSeeder)

**Hackathon slug:** `seal-gd3-prelim-open`

| Thiết lập | Mục đích test |
|-----------|---------------|
| Slot order=1 → `PRESENTING` | Judge có thể `POST /scores` ngay trên seed |
| Gắn `submission` + `track` trên slot | Queue API trả `submissionId` / `displayCode` |
| Round active, deadline đã qua | Phase `JUDGING` |

**Bootstrap IDs:** log `[Gd3DataSeeder]` khi start `profile=dev`.

---

## 13. Error codes & audit mới

### 13.1 Error codes

| Code | HTTP | Khi nào |
|------|------|---------|
| `SLIDE_FILE_REQUIRED` | 400 | Thiếu `slideFile` |
| `INVALID_SLIDE_FILE` | 400 | Không phải PDF / quá size |
| `REPO_NOT_PUBLIC` | 400 | GitHub private/404 |
| `SCORING_NOT_OPEN` | 403 | Chấm khi chưa PRESENTING / chưa JUDGING |
| `NOT_TRACK_CONTROLLER` | 403 | Judge không có quyền timer/shuffle |

### 13.2 Audit actions

- `PRESENTATION_QUEUE_SHUFFLE`
- `PRESENTATION_CONTROLLER_GRANTED`
- `PRESENTATION_CONTROLLER_REVOKED`

---

## 14. Hướng dẫn test

### 14.1 Unit / integration (Maven)

```bash
# Toàn bộ suite
mvn test

# Chỉ test GĐ3
mvn test -Dtest="SubmissionSlideStorageTest,GitHubRepoValidatorTest,JudgePortalServiceTest,PresentationControllerAuthTest,PresentationTimerPauseTest,ScoringWindowTest,PresentationQueueShuffleTest,PresentationQueueAnonymityTest,RoundPhaseResolverTest,PresentationDurationResolverTest"
```

### 14.2 Manual — storage local

1. Start BE `profile=dev`
2. Login student → `POST /submissions` multipart
3. Kiểm tra file: `uploads/submissions/submissions/{hId}/{rId}/{sId}/slide.pdf`
4. `GET /submissions/{id}/slide` → PDF inline

### 14.3 Manual — MinIO

```bash
docker compose -f docker-compose.minio.yml up -d
# Set APP_STORAGE_TYPE=minio
```

Console: http://localhost:9001 — bucket `seal-submissions`

### 14.4 Manual — presentation E2E

```
1. Coord: POST /presentation/queue/shuffle { roundId, trackIds }
2. GET /presentation/queue?roundId=&trackId= → items[0].status=PRESENTING
3. HEAD judge: POST /presentation/timer/start
4. Judge: GET /me/judge/submissions → chỉ #id
5. Judge: POST /scores → 200
6. (Nếu chưa start) POST /scores → 403 SCORING_NOT_OPEN
```

### 14.5 WebSocket

Subscribe (STOMP):

```
/topic/rounds/{roundId}/tracks/{trackId}/presentation-queue
```

Sau `shuffle` / `timer/start` → nhận payload `PresentationQueueResponse` đầy đủ.

### 14.6 Postman / doc

- Chi tiết request/response: [fe-gd3-api-mapping.md](fe-gd3-api-mapping.md) §6.4, §9, §10, §18–19
- Full workflow GĐ1–6: [full-workflow-api-test-gd1-gd6.md](full-workflow-api-test-gd1-gd6.md)

---

## 15. Breaking changes & tương thích

| Thay đổi | Ảnh hưởng | Giảm thiểu |
|----------|-----------|------------|
| Queue JSON `groups` → `tracks` | FE cũ parse lỗi | Cập nhật FE theo §6.1 |
| Slot chỉ tạo khi shuffle | GET queue trống sau activate | Gọi shuffle trước khi vận hành |
| `POST /scores` cần PRESENTING | Luồng chấm “ngay sau deadline” | `timer/start` hoặc seed PRESENTING |
| Multipart PDF canonical | JSON-only thiếu slide | Dùng multipart production |
| `PATCH .../next` cần `trackId` (prelim) | Script cũ chỉ `roundId` | Thêm query `trackId` |
| Judge ẩn danh | FE judge dùng `teamName` | Dùng `displayCode` / `submissionId` |

**Không đổi:** GĐ1/GĐ2 lottery, activate, late review, lock-scoring, publish, advance, calibration endpoint.

---

## 16. Phụ lục — API surface đầy đủ

### Submissions

| Method | Path | Role |
|--------|------|------|
| POST | `/api/v1/submissions` (multipart) | STUDENT |
| POST | `/api/v1/submissions` (JSON legacy) | STUDENT |
| GET | `/api/v1/submissions/{id}/slide` | STUDENT/JUDGE/COORD |
| GET | `/api/v1/submissions` | Theo role (JUDGE ẩn danh) |

### Judge portal

| Method | Path |
|--------|------|
| GET | `/api/v1/me/judge/submissions?roundId=&trackId=` |

### Presentation

| Method | Path |
|--------|------|
| GET | `/api/v1/presentation/queue?roundId=&trackId=` |
| POST | `/api/v1/presentation/queue/shuffle` |
| PATCH | `/api/v1/presentation/queue/next?roundId=&trackId=` |
| POST | `/api/v1/presentation/timer/{start\|pause\|resume\|qa\|reset\|next}` |
| GET/PUT/DELETE | `/api/v1/presentation/tracks/{trackId}/controller` |
| GET/PUT/DELETE | `/api/v1/presentation/rounds/{roundId}/controller` |

### Scoring

| Method | Path | Gate |
|--------|------|------|
| POST | `/api/v1/scores` | JUDGING + PRESENTING |
| POST | `/api/v1/scores/calibration` | Không gate PRESENTING |

### WebSocket

| Subscribe |
|-----------|
| `/topic/rounds/{roundId}/tracks/{trackId}/presentation-queue` |
| `/topic/rounds/{roundId}/presentation-queue` |

---

*Tài liệu này mô tả trạng thái codebase sau triển khai GĐ3 v4.1. Khi có thay đổi tiếp theo, cập nhật file này cùng `fe-gd3-api-mapping.md`.*
