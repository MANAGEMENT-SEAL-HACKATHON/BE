# Chiến lược kiểm thử GĐ1 → GĐ6 — Unit → Integration → E2E

> **Mục đích:** Hướng dẫn test **từ cục bộ đến toàn cục**, **từ tổng thể đến chi tiết** — kiểm tra logic nghiệp vụ và workflow 6 giai đoạn hackathon.  
> **Đối tượng:** Dev / QA / reviewer trước merge hoặc demo.  
> **Cập nhật:** 2026-06-09 · **160** automated tests (`mvn test` pass)

---

## Mục lục

| § | Nội dung |
|---|----------|
| [1](#1-kim-tự-tháp-kiểm-thử) | Kim tự tháp kiểm thử (4 tầng) |
| [2](#2-phạm-vi-cục-bộ--toàn-cục) | Cục bộ vs toàn cục |
| [3](#3-lệnh-chạy-nhanh) | Lệnh Maven / profile |
| [4](#4-bản-đồ-coverage-theo-giai-đoạn) | Bản đồ coverage GĐ0→GĐ6 |
| [5](#5-chi-tiết-theo-giai-đoạn) | Chi tiết từng GĐ — logic, unit, integration, manual |
| [6](#6-ma-trận-traceability) | Ma trận traceability (ID testcase ↔ test class) |
| [7](#7-lộ-trình-chạy-theo-mục-đích) | Lộ trình: commit / PR / release |
| [8](#8-gap--đề-xuất-integration-tiếp-theo) | Gap & đề xuất `@SpringBootTest` tiếp theo |
| [9](#9-tài-liệu-liên-quan) | Tài liệu liên quan |

---

## 1. Kim tự tháp kiểm thử

```text
                    ┌─────────────────────────┐
                    │  Tầng 4 — E2E toàn cục │  1 luồng GĐ0→GĐ6 (Postman/manual)
                    │  ~2–4 giờ, 166 API    │
                    └───────────┬─────────────┘
                ┌───────────────┴───────────────┐
                │  Tầng 3 — API slice / manual  │  Theo GĐ + negative gate
                │  gate-regression + e2e-gd2-gd3│
                └───────────────┬───────────────┘
            ┌───────────────────┴───────────────────┐
            │  Tầng 2 — Integration (@SpringBootTest)│  Context + HTTP slice (hiện: 1 flow)
            └───────────────────┬───────────────────┘
        ┌───────────────────────┴───────────────────────┐
        │  Tầng 1 — Unit (Mockito, pure logic)          │  160 tests — chạy ~50s
        └───────────────────────────────────────────────┘
```

| Tầng | Phạm vi | Khi chạy | Bằng chứng pass |
|------|---------|-----------|-----------------|
| **1 — Unit** | 1 class / 1 rule | Mỗi commit, CI | `mvn test` → `Tests run: 160` |
| **2 — Integration** | Nhiều bean + DB (H2) | PR quan trọng | `AuthOnboardingFlowIntegrationTest` |
| **3 — API manual** | 1 GĐ hoặc 1 feature | Sau refactor GĐ | Checkbox trong gate matrix / e2e doc |
| **4 — E2E full** | GĐ0→GĐ6 liên tiếp | Trước release / demo | `full-workflow-api-test-gd1-gd6.md` Phần II |

**Nguyên tắc:** Đi **từ dưới lên** (unit pass trước) rồi **mở rộng phạm vi** (seed slug → API → full path). Không bỏ qua tầng 1 chỉ vì tầng 4 “chạy được một lần”.

---

## 2. Phạm vi: cục bộ → toàn cục

| Mức | Ý nghĩa | Ví dụ trong repo |
|-----|---------|------------------|
| **Cục bộ (local)** | Một hàm, validator, mapper, guard | `GitHubRepoValidatorTest`, `RoundPhaseResolverTest` |
| **Module (service)** | Service + mock repo | `RoundServiceImplExamValidationTest`, `ScoringWindowTest` |
| **Slice (vertical)** | Một use-case qua HTTP + DB | `AuthOnboardingFlowIntegrationTest` |
| **Giai đoạn (phase)** | Toàn bộ gate + role của 1 GĐ | Seed `seal-gd3-prelim-open` + gate matrix G3-* |
| **Toàn cục (global)** | Workflow 6 GĐ, conflict code giữa GĐ | `full-workflow` Phần II + `gate-regression` §9 |

**Tổng thể → chi tiết:** Chạy **E2E full path** (§9 gate matrix) một lần để thấy workflow; khi fail, **hạ xuống** GĐ cụ thể → unit test class tương ứng → sửa → `mvn test` → chạy lại slice GĐ đó.

---

## 3. Lệnh chạy nhanh

### 3.1 Unit + integration mặc định (H2 in-memory)

```powershell
cd D:\FPT\SU26\SWP\ManageSealHackathon\BE
mvn test
# Kỳ vọng: Tests run: 160, Failures: 0, BUILD SUCCESS
```

### 3.2 Chạy theo package / class

```powershell
# GĐ1 — events & timeline
mvn test -Dtest=EventScheduleValidatorImplTest,HackathonTimelineServiceImplTest

# GĐ1 — rounds & tracks
mvn test -Dtest=RoundServiceImpl*Test,TrackServiceImpl*Test,CriteriaServiceImplFinalRoundTest

# GĐ2 — teams & lottery
mvn test -Dtest=HackathonLotteryServiceImplGroupTest

# GĐ3 — submission, presentation, scoring
mvn test -Dtest=SubmissionSlideStorageTest,GitHubRepoValidatorTest,PresentationQueue*Test,ScoringWindowTest,JudgePortalServiceTest,PresentationControllerAuthTest,PresentationTimerPauseTest,RoundPhaseResolverTest,PresentationDurationResolverTest

# GĐ0 — auth
mvn test -Dtest=Auth*Test,RegistrationServiceTest,JwtTokenServiceTest

# Integration only
mvn test -Dtest=AuthOnboardingFlowIntegrationTest
```

### 3.3 BE dev + seed (manual / E2E)

```powershell
mvn spring-boot:run -Dspring-boot.run.profiles=dev
# Swagger: http://localhost:8080/swagger-ui.html
# Base API: http://localhost:8080/api/v1
```

| Profile | DB | Seed |
|---------|-----|------|
| `dev` | MySQL (local) | Gd1→Gd6 seeders |
| test (JUnit) | H2 | Không seed — test tự setup |

### 3.4 Storage khi test multipart GĐ3

| `app.storage.type` | Kiểm tra file |
|--------------------|---------------|
| `local` (mặc định) | `uploads/submissions/submissions/{hackathonId}/{roundId}/{submissionId}/slide.pdf` |
| `minio` | `docker compose -f docker-compose.minio.yml up -d` |

---

## 4. Bản đồ coverage theo giai đoạn

| GĐ | Gate / đầu ra | Unit (auto) | Integration (auto) | Manual E2E | Seed slug |
|----|---------------|-------------|--------------------|------------|-----------|
| **GĐ0** | Auth, onboarding | ✅ Mạnh (~40 tests) | ✅ 1 flow | Login Postman | Users trong Gd1 seeder |
| **GĐ1** | Events, readiness, ONGOING | ✅ Mạnh (~55 tests) | ❌ | G1-E*, G1-N* | `seal-gd1-ready`, `seal-gd1-incomplete` |
| **GĐ2** | Teams, lock, lottery | ⚠️ Mỏng (3 tests) | ❌ | G2-H*, G2-N* | `seal-spring-2026` |
| **GĐ3** | Submit, score, queue, timer | ✅ Khá (~25 tests) | ❌ | [e2e-gd2-gd3-v41](e2e-gd2-gd3-v41-manual-test.md) | `seal-gd3-prelim-open` |
| **GĐ4** | Publish, advance, activate CK | ⚠️ Rất mỏng | ❌ | G4-H*, G4-N* | `seal-gd4-advance-ready` / `seal-gd4-tiebreak-wildcard`* |
| **GĐ5** | CK submit/score, lock → PENDING_CONFIRM | ⚠️ Mỏng (criteria CK) | ❌ | G5-H* | `seal-gd5-final-active` |
| **GĐ6** | AWARDS, prizes, FINISHED | ❌ Không có unit | ❌ | G6-H* | `seal-gd6-pending-confirm` |

\* `seal-gd4-tiebreak-wildcard` cần `app.seed.gd4.enabled=true`.

**Tóm tắt:** Automated coverage **mạnh ở GĐ0, GĐ1, GĐ3**; **GĐ2, GĐ4, GĐ5, GĐ6** chủ yếu dựa **manual + seed** — cần bổ sung integration theo §8.

---

## 5. Chi tiết theo giai đoạn

### GĐ0 — Xác thực & onboarding

**Logic nghiệp vụ cần đúng**

- Register → pending → coordinator approve → login JWT.
- Refresh token, logout, đổi mật khẩu, forgot/reset.
- OAuth stub / social link (dev).

**Unit test (cục bộ → service)**

| Class | Số @Test | Kiểm tra |
|-------|----------|----------|
| `AuthServiceTest` | 8 | Login, credential |
| `AuthControllerTest` | 10 | Controller contract |
| `RegistrationServiceTest` | 3 | Đăng ký |
| `JwtTokenServiceTest` | 1 | Token |
| `UserSessionServiceTest` | 4 | Session |
| `PasswordResetServiceTest` | 4 | Reset flow |
| `SocialAuthServiceTest` | 12 | OAuth |

**Integration (toàn slice)**

| Class | Kiểm tra |
|-------|----------|
| `AuthOnboardingFlowIntegrationTest` | `register` → approve → `login` qua `MockMvc` + H2 |

**Manual**

- `POST /auth/login` với tài khoản §1.3 [full-workflow](full-workflow-api-test-gd1-gd6.md).
- Negative: login sai password → 401.

---

### GĐ1 — Chuẩn bị hackathon (Gate 1)

**Workflow tổng thể**

```text
Tạo hackathon DRAFT → rounds (prelim + final shell) → tracks → criteria
→ personnel (mentor/judge PRELIM only) → events (POST: KO → WS → AWARDS)
→ readiness ?target=ONGOING → PATCH status ONGOING
```

**Logic chi tiết cần verify**

| # | Rule | Code lỗi thường gặp |
|---|------|---------------------|
| 1 | POST event: KICKOFF trước WORKSHOP | `EVENT_KICKOFF_MISSING` |
| 2 | Lịch: WORKSHOP trước KICKOFF (khác ngày) | `EVENT_ORDER_VIOLATION` |
| 3 | ONGOING **không** cần AWARDS | (đã fix `EVENT_AWARDS_MISSING`) |
| 4 | Phải có round CK + criteria CK shell | `MISSING_FINAL_ROUND` |
| 5 | Không gán judge `FINAL_EXTERNAL` ở GĐ1 | `JUDGE_FINAL_AT_PHASE1` |
| 6 | `examAt` + `codingDurationHours` → auto deadline | `ROUND_DEADLINE_INVALID` |
| 7 | Archive hackathon FINISHED → chặn sửa | `HackathonArchiveGuard` |

**Unit test**

| Class | GĐ1 focus |
|-------|-----------|
| `EventScheduleValidatorImplTest` (22) | Thứ tự event, ngày, prerequisite |
| `HackathonTimelineServiceImplTest` (12) | Timeline sync |
| `EventServiceImplArchiveGuardTest` (2) | Archive |
| `RoundServiceImplExamValidationTest` (17) | Deadline, exam |
| `RoundServiceImplSequenceTest` | Thứ tự round |
| `RoundServiceImplRoundTypeUniqueTest` (3) | 1 prelim + 1 final |
| `RoundServiceImplDeleteTest` (2) | Xóa round |
| `TrackServiceImplCreateSequenceTest` (3) | Track sequence |
| `TrackServiceImplUpdateTest` (2) | Sửa track |
| `TrackServiceImplDeleteTest` (3) | Xóa track |
| `TrackServiceImplListByRoundTest` (3) | List |
| `CriteriaServiceImplFinalRoundTest` (1) | Criteria CK |
| `PersonnelAssignmentRulesTest` (3) | Rule gán người |
| `PersonnelAssignmentCrossTrackTest` (3) | Cross-track |
| `TempJudgeServiceImplTest` (1) | Temp judge |
| `InvitationServiceImplTest` (2) | Invite |
| `GuestJudgeLifecycleServiceImplTest` (4) | Guest judge lifecycle |
| `HackathonArchiveGuardTest` (3) | Guard |

**Manual / gate matrix**

- Happy: **G1-E01 → G1-E03** ([gate-regression](gate-regression-test-matrix-gd1-gd6.md) §3).
- Negative: **G1-N01 → G1-N08**, readiness **G1-R01, G1-R02**.
- Seed: `seal-gd1-ready` (pass), `seal-gd1-incomplete` (fail readiness).

**Integration gap:** Chưa có test `PATCH ONGOING` end-to-end với readiness.

---

### GĐ2 — Đăng ký đội & lottery

**Workflow**

```text
Hackathon ONGOING → POST /teams → duyệt member → registrationEnd qua
→ lock teams → POST lottery → team_round_tracks
```

**Logic chi tiết**

| # | Rule | Code |
|---|------|------|
| 1 | Chỉ tạo team khi ONGOING | 422 |
| 2 | Lottery cần `is_locked=true` | `TEAM_NOT_LOCKED` |
| 3 | Lottery sau `registrationEnd` (hoặc seed đã lock) | timeline |

**Unit test**

| Class | Ghi chú |
|-------|---------|
| `HackathonLotteryServiceImplGroupTest` (3) | Nhóm lottery — **chưa** cover lock gate đầy đủ |

**Manual**

- **G2-H01, G2-H02, G2-N01, G2-N02** — seed `seal-spring-2026`, teams `GD2-*`.
- Doc teams: [mf02/05-test-data-gd2-teams.md](../mf02/05-test-data-gd2-teams.md).

**Integration gap:** Cần test `POST /teams` + `POST lottery` với mockMvc.

---

### GĐ3 — Sơ loại (Gate 2) — v4.1

**Workflow tổng thể**

```text
activate prelim → (optional) shuffle queue per track → timer start
→ student multipart submit (PDF + repoUrl) → judge anonymous list
→ score (gate: JUDGING hoặc slot PRESENTING) → lock-scoring prelim
→ late review / calibration (seed có sẵn)
```

**Logic chi tiết (v4.1)**

| # | Rule | Code / hành vi |
|---|------|----------------|
| 1 | Activate cần đội + judge/track + criteria weight | `NO_TEAMS_IN_ROUND`, `JUDGE_NOT_ASSIGNED` |
| 2 | Submit: multipart `repoUrl` + `slide` PDF | 201, `slideStorageKey` |
| 3 | Judge `GET /submissions` ẩn danh | Không lộ team name |
| 4 | Queue response `tracks[]` (không `groups[]`) | Breaking FE |
| 5 | `POST shuffle` **tạo** slot WAITING, Fisher-Yates | Không activate team |
| 6 | Score khi slot WAITING (không timer) | `SCORING_NOT_OPEN` |
| 7 | Score khi PRESENTING hoặc phase JUDGING | 201 |
| 8 | Timer: chỉ presentation controller | `PresentationControllerAuthTest` |
| 9 | WS topic `.../presentation-queue` | Manual optional |

**Unit test (GĐ3 — chạy gói §3.2)**

| Class | Logic |
|-------|-------|
| `RoundActivationServiceImplTest` | Activate thiếu judge → `JUDGE_NOT_ASSIGNED` |
| `SubmissionSlideStorageTest` | Upload key, delete |
| `GitHubRepoValidatorTest` | URL GitHub |
| `JudgePortalServiceTest` | Anonymous submission list |
| `PresentationQueueShuffleTest` | Shuffle tạo slot, thứ tự |
| `PresentationQueueAnonymityTest` | Queue ẩn danh cho JUDGE |
| `ScoringWindowTest` | `SCORING_NOT_OPEN`, locked |
| `PresentationControllerAuthTest` | Grant/revoke controller |
| `PresentationTimerPauseTest` | Pause/resume calculator |
| `RoundPhaseResolverTest` | PRELIM vs FINAL phase |
| `PresentationDurationResolverTest` | Duration từ round |

**Manual E2E (khuyến nghị cho GĐ3)**

→ **[e2e-gd2-gd3-v41-manual-test.md](e2e-gd2-gd3-v41-manual-test.md)** — đầy đủ GĐ2 tiên quyết + GĐ3 multipart/shuffle/timer/negative.

**Gate matrix:** **G3-H01 → G3-H04**, **G3-N01**, **G3-T01**.

**Seed:** `seal-gd3-prelim-open` — teams `GD3-01`…`04`, calibration OPEN/CLOSED.

---

### GĐ4 — Publish, advance, mở CK (Gate 3)

**Workflow**

```text
lock + scores SL xong → PATCH publish prelim → ranking/wildcard
→ POST advance → POST judge-assignments FINAL_EXTERNAL
→ GET readiness ?target=FINAL_ROUND → PATCH activate final
```

**Logic chi tiết**

| # | Rule | Code |
|---|------|------|
| 1 | Publish khi đã lock scoring | |
| 2 | Advance tạo `team_round_participation` CK | |
| 3 | Activate CK cần publish SL | `RESULT_NOT_PUBLISHED` |
| 4 | Activate CK cần judge FINAL | activate gate |
| 5 | Tiebreak / wildcard (opt-in seed) | GĐ4 seed |

**Unit test:** Gần như **không có** — chỉ liên quan gián tiếp (`CriteriaServiceImplFinalRoundTest`, personnel).

**Manual:** **G4-H01 → G4-H04**, **G4-N01, G4-N02**, **G4-R01**.

| Seed | Khi nào |
|------|---------|
| `seal-gd4-advance-ready` | Happy advance (trong full-workflow) |
| `seal-gd4-tiebreak-wildcard` | Tiebreak — `APP_SEED_GD4_ENABLED=true` |

---

### GĐ5 — Chung kết

**Workflow**

```text
final active → POST submit (roundId=final, không trackId)
→ judge FINAL score → PATCH lock-scoring final
→ hackathon status PENDING_CONFIRM
```

**Logic chi tiết**

| # | Rule | Ghi chú |
|---|------|---------|
| 1 | Submission CK không gắn track | Round-level |
| 2 | Guest judge `FINAL_EXTERNAL` | Seed Gd5 |
| 3 | Lock CK → chuyển trạng thái hackathon | `PENDING_CONFIRM` |

**Unit test:** `CriteriaServiceImplFinalRoundTest` (criteria CK) — **không** có test lock CK / PENDING_CONFIRM.

**Manual:** **G5-H01 → G5-H04** — seed `seal-gd5-final-active`, students `student.gd5.leader*`.

---

### GĐ6 — Trao giải & kết thúc

**Workflow**

```text
PENDING_CONFIRM → (nếu thiếu) POST AWARDS event
→ readiness ?target=AWARDS → POST /prizes → PATCH confirm → FINISHED
```

**Logic chi tiết**

| # | Rule | Code |
|---|------|------|
| 1 | AWARDS sau WORKSHOP trên lịch | `EVENT_ORDER_VIOLATION` |
| 2 | Readiness AWARDS | `EVENT_AWARDS_MISSING` nếu thiếu |
| 3 | Confirm chỉ từ PENDING_CONFIRM | |
| 4 | FINISHED → archive guard | Không sửa cấu hình |

**Unit test:** **Không có** — dựa `EventScheduleValidatorImplTest` cho phần event AWARDS.

**Manual:** **G6-H01 → G6-H03**, **G6-R01** — seed `seal-gd6-pending-confirm`.

**Archive verify:** `seal-fall-2025-finished` — read-only.

---

## 6. Ma trận traceability

Ánh xạ **gate ID** (gate-regression) ↔ **automated test** ↔ **tài liệu manual**.

| Gate ID | Mô tả ngắn | Unit / integration | Manual doc |
|---------|------------|-------------------|------------|
| G1-E01–E03 | Events + ONGOING | `EventScheduleValidatorImplTest`, `HackathonTimelineServiceImplTest` | full-workflow §1.x |
| G1-N01–N07 | Event negative | `EventScheduleValidatorImplTest` | gate §3 |
| G1-N08 | Readiness fail | — | seed `seal-gd1-incomplete` |
| G2-H01 | Tạo team | — | full-workflow §2 |
| G2-N02 | Lottery chưa lock | `HackathonLotteryServiceImplGroupTest` (partial) | gate §4 |
| G3-H01 | Activate prelim | `RoundActivationServiceImplTest` | e2e-gd2-gd3 §B |
| G3-H02 | Submit | `SubmissionSlideStorageTest`, `GitHubRepoValidatorTest` | e2e-gd2-gd3 §B |
| G3-H03 | Mentor portal | — | fe-gd3-api-mapping |
| G3-H04 | Lock scoring | — | gate §5 |
| G3-N01 | No teams | — | gate §5 |
| G3 (score gate) | SCORING_NOT_OPEN | `ScoringWindowTest` | e2e-gd2-gd3 §C |
| G3 (queue) | shuffle + anonymous | `PresentationQueueShuffleTest`, `PresentationQueueAnonymityTest` | e2e-gd2-gd3 |
| G3 (timer) | controller + pause | `PresentationControllerAuthTest`, `PresentationTimerPauseTest` | e2e-gd2-gd3 §D |
| G4-* | Publish/advance/CK | — | full-workflow §4, gate §6 |
| G5-* | CK scoring | `CriteriaServiceImplFinalRoundTest` (partial) | gate §7 |
| G6-* | Prizes/FINISHED | `HackathonArchiveGuardTest` (sau confirm) | gate §8 |
| GĐ0 | Onboarding | `AuthOnboardingFlowIntegrationTest` | full-workflow GĐ0 |

---

## 7. Lộ trình chạy theo mục đích

### 7.1 Sau mỗi commit (≤ 1 phút)

```powershell
mvn test
```

### 7.2 Sau thay đổi một GĐ (10–30 phút)

| Nếu sửa… | Chạy |
|----------|------|
| Events / readiness | `mvn test -Dtest=EventScheduleValidatorImplTest,HackathonTimelineServiceImplTest` + manual G1-E01–E03 |
| Rounds / tracks | `mvn test -Dtest=RoundServiceImpl*Test,TrackServiceImpl*Test` |
| GĐ3 presentation | Gói GĐ3 §3.2 + [e2e-gd2-gd3-v41](e2e-gd2-gd3-v41-manual-test.md) Phần B–C |
| Auth | `mvn test -Dtest=Auth*Test` + `AuthOnboardingFlowIntegrationTest` |

### 7.3 Trước PR / demo (2–4 giờ)

1. `mvn test` — 160/160.
2. Start `dev`, verify log seeders ([seed-coverage-audit](seed-coverage-audit.md)).
3. Chạy **gate matrix §9** E2E full path (checkbox từng ID).
4. GĐ3: thêm smoke [e2e-gd2-gd3](e2e-gd2-gd3-v41-manual-test.md) §8 (~15 phút).

### 7.4 Regression “code đá nhau”

Ưu tiên các cặp trong [gate-regression §10](gate-regression-test-matrix-gd1-gd6.md):

- ONGOING vs AWARDS  
- WS trước KO  
- Judge CK ở GĐ1  
- Activate SL không đội  
- Activate CK chưa publish  
- Team chưa lock mà lottery  

---

## 8. Gap & đề xuất integration tiếp theo

Hiện chỉ có **1** integration test thực sự (`AuthOnboardingFlowIntegrationTest`). `BeApplicationTests` trỏ MySQL local — **không** chạy trên CI mặc định.

**Đề xuất thứ tự bổ sung** (`@SpringBootTest` + H2 + `MockMvc`):

| Ưu tiên | Class đề xuất | GĐ | Scenario |
|---------|---------------|-----|----------|
| P1 | `Gd1OnboardingIntegrationTest` | GĐ1 | Seed minimal → readiness ONGOING → PATCH ONGOING |
| P1 | `Gd3SubmissionMultipartIntegrationTest` | GĐ3 | Multipart submit → GET slide → 200 |
| P2 | `Gd2TeamLotteryIntegrationTest` | GĐ2 | Team create → lock → lottery |
| P2 | `Gd3ScoringGateIntegrationTest` | GĐ3 | Slot WAITING → POST score → 422 `SCORING_NOT_OPEN` |
| P3 | `Gd4AdvanceIntegrationTest` | GĐ4 | Publish → advance → readiness FINAL_ROUND |
| P3 | `Gd5FinalLockIntegrationTest` | GĐ5 | Lock CK → status `PENDING_CONFIRM` |
| P4 | `Gd6ConfirmIntegrationTest` | GĐ6 | Prizes → confirm → `FINISHED` + archive |

Mẫu kỹ thuật: copy `@TestPropertySource` từ `AuthOnboardingFlowIntegrationTest` (H2, `NON_KEYWORDS=YEAR`, JWT tắt hoặc stub).

---

## 9. Tài liệu liên quan

| File | Dùng khi |
|------|----------|
| [full-workflow-api-test-gd1-gd6.md](full-workflow-api-test-gd1-gd6.md) | Catalog 166 API + E2E happy path |
| [gate-regression-test-matrix-gd1-gd6.md](gate-regression-test-matrix-gd1-gd6.md) | Negative + full 6 GĐ checklist |
| [e2e-gd2-gd3-v41-manual-test.md](e2e-gd2-gd3-v41-manual-test.md) | GĐ2→GĐ3 v4.1 chi tiết |
| [gd3-v41-implementation-changelog.md](gd3-v41-implementation-changelog.md) | Thay đổi code GĐ3 |
| [fe-gd3-api-mapping.md](fe-gd3-api-mapping.md) | FE ↔ API GĐ3 |
| [seed-coverage-audit.md](seed-coverage-audit.md) | Slug, SQL verify, seeder order |
| [../api-authorization-matrix.md](../api-authorization-matrix.md) | Role từng endpoint |

---

## Phụ lục A — Danh sách 38 test classes (160 tests)

| # | Class | GĐ chính |
|---|-------|----------|
| 1 | `AuthServiceTest` | GĐ0 |
| 2 | `AuthControllerTest` | GĐ0 |
| 3 | `RegistrationServiceTest` | GĐ0 |
| 4 | `JwtTokenServiceTest` | GĐ0 |
| 5 | `UserSessionServiceTest` | GĐ0 |
| 6 | `PasswordResetServiceTest` | GĐ0 |
| 7 | `SocialAuthServiceTest` | GĐ0 |
| 8 | `AuthOnboardingFlowIntegrationTest` | GĐ0 |
| 9 | `EventScheduleValidatorImplTest` | GĐ1 |
| 10 | `HackathonTimelineServiceImplTest` | GĐ1 |
| 11 | `EventServiceImplArchiveGuardTest` | GĐ1/6 |
| 12 | `HackathonArchiveGuardTest` | GĐ6 |
| 13 | `RoundServiceImplExamValidationTest` | GĐ1/3 |
| 14 | `RoundServiceImplSequenceTest` | GĐ1 |
| 15 | `RoundServiceImplRoundTypeUniqueTest` | GĐ1 |
| 16 | `RoundServiceImplDeleteTest` | GĐ1 |
| 17 | `RoundActivationServiceImplTest` | GĐ3 |
| 18 | `TrackServiceImplCreateSequenceTest` | GĐ1 |
| 19 | `TrackServiceImplUpdateTest` | GĐ1 |
| 20 | `TrackServiceImplDeleteTest` | GĐ1 |
| 21 | `TrackServiceImplListByRoundTest` | GĐ1 |
| 22 | `CriteriaServiceImplFinalRoundTest` | GĐ1/5 |
| 23 | `PersonnelAssignmentRulesTest` | GĐ1 |
| 24 | `PersonnelAssignmentCrossTrackTest` | GĐ1 |
| 25 | `TempJudgeServiceImplTest` | GĐ1 |
| 26 | `InvitationServiceImplTest` | GĐ1 |
| 27 | `GuestJudgeLifecycleServiceImplTest` | GĐ1/4 |
| 28 | `HackathonLotteryServiceImplGroupTest` | GĐ2 |
| 29 | `SubmissionSlideStorageTest` | GĐ3 |
| 30 | `GitHubRepoValidatorTest` | GĐ3 |
| 31 | `JudgePortalServiceTest` | GĐ3 |
| 32 | `PresentationQueueShuffleTest` | GĐ3 |
| 33 | `PresentationQueueAnonymityTest` | GĐ3 |
| 34 | `ScoringWindowTest` | GĐ3 |
| 35 | `PresentationControllerAuthTest` | GĐ3 |
| 36 | `PresentationTimerPauseTest` | GĐ3 |
| 37 | `RoundPhaseResolverTest` | GĐ3/5 |
| 38 | `PresentationDurationResolverTest` | GĐ3 |
| — | `BeApplicationTests` | Context (MySQL local, optional) |

---

**Kết luận vận hành:** Dùng **`mvn test`** làm hàng rào cục bộ hàng ngày; dùng **seed slug + gate matrix** để kiểm tra từng GĐ; dùng **full-workflow Phần II + §9 gate** để xác nhận workflow toàn cục. Khi fail ở tầng 4, truy ngược ma trận §6 xuống unit class tương ứng trước khi sửa production code.
