# 05 — Source Check (Kiểm tra nguồn)

> Inventory **DB**, **API Server**, **Frontend** — dùng cho phần Source check trong assignment.

---

## 1. Tổng quan kiến trúc

```
┌─────────────┐     REST /api/v1      ┌──────────────────┐     JDBC      ┌─────────┐
│  Frontend   │ ◄──────────────────► │  API Server (BE)  │ ◄──────────► │  MySQL  │
│  React SPA  │     WS /ws (STOMP)    │  Spring Boot 3    │              │  8.0    │
└─────────────┘                       └─────────┬────────┘              └─────────┘
                                                │
                                                │ S3 API
                                                ▼
                                        ┌───────────────┐
                                        │ MinIO / Local │
                                        │ File Storage  │
                                        └───────────────┘
```

| Layer | Repo / Path | Tech |
|-------|-------------|------|
| **Frontend** | `seal-hackathon-fe` (repo riêng) | React, TypeScript |
| **API Server** | `ManageSealHackathon/BE` | Java 21, Spring Boot 3.4+, Spring Security JWT |
| **Database** | MySQL schema `SealHackathon` | Hibernate JPA, ddl-auto dev |
| **Storage** | `docker-compose.minio.yml` | MinIO :19000 hoặc local dir |
| **Realtime** | `/ws` SockJS + STOMP | Spring WebSocket |

---

## 2. Database (DB)

**Source of truth:** `docs/db/schema-v3.0-mysql.md`  
**Connection:** `jdbc:mysql://localhost:3306/SealHackathon`

### 2.1 Nhóm bảng theo domain

| Nhóm | Bảng | Mô tả |
|------|------|-------|
| **Identity** | `users`, `user_sessions`, `oauth_accounts`, `chapters` | Tài khoản, OAuth, chapter SV |
| **Event config** | `hackathons`, `rounds`, `tracks`, `criteria`, `events` | Cấu hình sự kiện GĐ1 |
| **Teams** | `teams`, `team_members`, `team_round_tracks`, `team_round_participation`, `mentor_team_assignments`, `hackathon_registrations` | Đội, lottery, journey |
| **Personnel** | `judge_assignments`, `mentor_assignments`, `invitations` | Phân công judge/mentor |
| **Submissions** | `submissions`, `submission_metadata` | Bài nộp, slide storage keys |
| **Scoring** | `scores`, `presentation_slots`, `calibration_sessions` | Điểm, queue, RBL |
| **Progression** | `wildcard_reviews`, `tiebreak_evaluations` | Wild card (`PATCH /wildcard-reviews/{id}`), hòa điểm |
| **Closure** | `prizes`, `certificates`, `appeals`, `chapter_rankings`, `individual_rankings`, `export_jobs` | GĐ6 — trao/thu hồi giải, chứng nhận, export CSV |
| **System** | `audit_logs`, `notifications`, `notification_templates` | Audit, thông báo |

**Tổng:** ~35 entity JPA map 1-1 các bảng trên.

### 2.2 Breaking changes schema v3.0 (cần biết khi vẽ ER)

| BC | Thay đổi |
|----|----------|
| BC-01 | `rounds.hackathon_id` (Round thuộc Hackathon, không còn con Track) |
| BC-02 | `tracks.round_id` (Track thuộc Round) |
| BC-03 | `criteria` XOR: `track_id` OR `round_id` |
| BC-04 | `team_round_tracks` — lottery track |
| BC-06 | `submissions` XOR track/round + `LATE_APPROVED` |
| BC-07 | `judge_assignments` XOR + `FINAL_EXTERNAL` |

### 2.3 Trigger / constraint quan trọng

- Cấm Mentor + Judge cùng track
- Submission / Criteria phải khớp final vs preliminary
- Team và Track cùng hackathon

---

## 3. API Server (Backend)

**Root package:** `src/main/java/com/sealhackathon/api/`  
**Base URL:** `http://localhost:8080/api/v1`

### 3.1 Controllers (37)

| # | Controller | Base path | Domain |
|---|------------|-----------|--------|
| 1 | `HealthCheckController` | `/` | System |
| 2 | `AuthController` | `/auth` | Auth |
| 3 | `UserMeController` | `/users` | Profile |
| 4 | `UserController` | `/users` | User admin |
| 5 | `TempJudgeController` | `/users/temp-judges` | Guest judge |
| 6 | `InvitationController` | `/invitations` | Invitations |
| 7 | `HackathonController` | `/hackathons` | Hackathon CRUD |
| 8 | `HackathonStatusController` | `/hackathons` | Status |
| 9 | `HackathonClosureController` | `/hackathons` | GĐ6 |
| 10 | `RoundController` | `/rounds`, `/hackathons/.../rounds` | Rounds |
| 11 | `RoundActivationController` | `/rounds` | Activate |
| 12 | `RoundProgressionController` | `/rounds` | GĐ3–5 |
| 13 | `TrackController` | `/tracks`, `/rounds/.../tracks` | Tracks |
| 14 | `CriteriaController` | `/criteria`, `/tracks/.../criteria` | Criteria |
| 15 | `EventController` | `/events`, `/hackathons/.../events` | Timeline |
| 16 | `JudgeAssignmentController` | `/judge-assignments` | Personnel |
| 17 | `MentorAssignmentController` | `/mentor-assignments` | Personnel |
| 18 | `TeamController` | `/teams` | Teams GĐ2 |
| 19 | `TeamJourneyController` | `/teams` | Journey |
| 20 | `SubmissionController` | `/submissions` | GĐ3 submit |
| 21 | `ScoreController` | `/scores` | Scoring |
| 22 | `WildcardReviewController` | `/wildcard-reviews` | GĐ4 |
| 23 | `CalibrationSessionController` | `/calibration-sessions` | GĐ5 RBL |
| 24 | `RblDashboardController` | `/rounds` | GĐ5 |
| 25 | `PresentationControllerController` | `/presentation` | Controller grant |
| 26 | `PresentationQueueController` | `/presentation/queue` | Queue |
| 27 | `PresentationTimerController` | `/presentation/timer` | Timer |
| 28 | `HackathonPrizeController` | `/hackathons` | Prizes |
| 29 | `PrizeController` | `/prizes` | Prize revoke |
| 30 | `ExportJobController` | `/export-jobs` | Export |
| 31 | `MeNotificationController` | `/me/notifications` | Notifications |
| 32 | `StudentMeController` | `/me` | Student portal |
| 33 | `StudentHackathonController` | `/me/hackathons` | Student |
| 34 | `StudentRoundController` | `/me/rounds` | Student |
| 35 | `StudentTeamController` | `/me` | Student |
| 36 | `JudgeMeController` | `/me` | Judge portal |
| 37 | `MentorMeController` | `/me` | Mentor portal |

### 3.2 Services & cross-cutting

| Layer | Package | Vai trò |
|-------|---------|---------|
| Service | `*/service/impl/` | Business logic |
| Guard | `*/guard/`, `presentation/guard/` | Authorization, scoring gate |
| Repository | `*/repository/` | JPA data access |
| DTO | `*/dto/` | Request/Response |
| Config | `config/`, `auth/config/` | Security, seed, storage |
| WS | `live_scoring/` | STOMP publishers |
| Storage | `storage/` | MinIO / local filesystem (slide, export, certificate) |

### 3.3 Security

| Role | Annotation | Scope |
|------|------------|-------|
| Coordinator | `@CoordinatorOnly` | Admin operations |
| Student | `@StudentOnly` | `/me/*` student |
| Judge | `@JudgeOnly` | Scoring, judge portal |
| Mentor | `@MentorOnly` | Mentor portal |
| Any approved | `@ApprovedOnly` | Presentation, notifications |

### 3.4 Test coverage (source check quality)

| Loại | File / Count |
|------|--------------|
| Integration GĐ2→GĐ3 | `Gd2Gd3FlowIntegrationTest` — 11 tests |
| Unit GĐ3 | 14 classes, ~45 tests |
| Full workflow doc | `docs/testing/full-workflow-api-test-gd1-gd6.md` |

### 3.5 Config files quan trọng

| File | Nội dung |
|------|----------|
| `application-dev.properties` | DB, JWT, MinIO, storage |
| `docker-compose.minio.yml` | MinIO local :19000 |
| `.env` | Secrets dev (không commit) |
| `pom.xml` | Dependencies Spring Boot |

---

## 4. Frontend (FE)

**Repo:** `seal-hackathon-fe` (ngoài folder BE hiện tại)  
**Doc tham chiếu BE:** `docs/testing/fe-gd3-api-mapping.md`

### 4.1 Portal theo role (cần implement / đã có)

| Portal | Màn hình chính | API chính |
|--------|----------------|-----------|
| **Coordinator** | Hackathon setup, Teams, Lottery, Late review, Shuffle, Progress, Publish | `/hackathons`, `/teams`, `/submissions`, `/presentation`, `/rounds` |
| **Student** | Register, Team, Submit PDF, Deadline, Problem, Leaderboard | `/me/*`, `/submissions` |
| **Judge** | Anonymous list, Score form, Timer control, Queue | `/me/judge/*`, `/scores`, `/presentation/timer` |
| **Mentor** | Assigned teams, Submissions, Schedule | `/me/mentor/*` |

### 4.2 Integration points FE ↔ BE

| Concern | BE contract |
|---------|-------------|
| Auth | JWT Bearer, refresh rotation |
| API prefix | `/api/v1/` (không `/api/`) |
| Multipart | `slideFile` + `repoUrl` |
| Binary download | Export CSV (`/export-jobs/{id}/download`), certificate PDF (`/me/certificates/{id}/download?download=true`), slide PDF |
| WS | SockJS `http://localhost:8080/ws` |
| Error | `{ success: false, error: { code, message } }` |

### 4.3 FE docs trong repo BE (handover)

| File | Mục đích |
|------|----------|
| `docs/testing/fe-gd3-api-mapping.md` | Contract GĐ3 đầy đủ |
| `docs/testing/fe-gd1-gd2-gd3-workflow-mapping.md` | Gate GĐ1→2 |
| `docs/mf02/fe-auth-integration.md` | Auth FE |
| `docs/mf03/07-fe-api-flow-gd3.md` | Flow GĐ3 |

---

## 5. Checklist Source Check (tick khi hoàn thành assignment)

### Database
- [ ] Vẽ ER diagram (Hackathon → Round → Track → Team → Submission → Score)
- [ ] Liệt kê ≥10 bảng chính với PK/FK
- [ ] Ghi chú BC-01, BC-03 (XOR criteria/submission)

### API Server
- [ ] Liệt kê ≥5 controller nhóm theo domain
- [ ] Mô tả auth JWT + 4 roles
- [ ] Nêu 1 guard nghiệp vụ (vd. SCORING_NOT_OPEN)
- [ ] Endpoint count ~161

### Frontend
- [ ] 4 portal theo role
- [ ] Base URL + WS endpoint
- [ ] 1 breaking change vs doc cũ (`/api/v1`, multipart PDF)

---

## 6. Đường dẫn source tham khảo nhanh

```
BE/
├── src/main/java/com/sealhackathon/api/   ← API source
├── src/test/java/                          ← Tests
├── docs/db/schema-v3.0-mysql.md            ← DB schema
├── docs/mf01/                              ← GĐ1 specs
├── docs/mf02/                              ← GĐ2 auth/teams
├── docs/mf03/                              ← GĐ3–6 scoring/presentation
├── docs/testing/fe-gd3-api-mapping.md      ← FE contract
└── swd392 - ass1/                          ← Folder assignment này
```
