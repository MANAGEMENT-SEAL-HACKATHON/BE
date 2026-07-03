# Dev seed — Hướng dẫn dữ liệu test

> **Profile:** `dev` · **Seeder:** `Gd1DataSeeder` + `E2eWorkflowDataSeeder` + `Gd3..Gd6*DataSeeder`  
> **Danh mục slug:** `DevSeedCatalog.ALL_DEV_HACKATHON_SLUGS` (**53** hackathon) — ma trận SSOT: [master-slug-test-matrix.md](master-slug-test-matrix.md)  
> Sau khi start app, tìm log **`[DataInitializer] Dev seed GĐ3–GĐ6 hoàn tất`**.

---

## Schema & database (dev)

Profile `dev` dùng **`spring.jpa.hibernate.ddl-auto=update`** ([`application-dev.properties`](../../src/main/resources/application-dev.properties)) — Hibernate **không** drop/recreate schema mỗi lần restart. Seed 53 hackathon chạy idempotent qua `DataInitializer` + `Gd03V41SchemaMigration`.

**Nếu DB lệch** (lỗi DDL, `Table … doesn't exist`, deadlock khi drop FK):

1. Dừng mọi instance BE (chỉ một process trên port 8080).
2. Reset database một lần:

```sql
DROP DATABASE IF EXISTS SealHackathon;
CREATE DATABASE SealHackathon CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

3. Start lại: `mvn spring-boot:run -Dspring-boot.run.profiles=dev`

**Không** dùng `create-drop` trên dev — kết hợp Spring DevTools sẽ drop schema lúc shutdown/startup và dễ deadlock MySQL trên schema lớn.

---

## Tổng quan — **10 hackathon GĐ1/GĐ2 + 39 profile GĐ3–6**

### Hackathon GĐ1 / GĐ2 / archive

| Slug | Tên | Trạng thái | Mục đích |
|------|-----|------------|----------|
| `seal-e2e-2026` | SEAL E2E 2026 | `ONGOING` | GĐ1 sẵn sàng + **GĐ2 happy** (7 đội + 3 orphan) → happy path GĐ1→6 |
| `seal-gd1-incomplete` | [Dev] GĐ1 Readiness FAIL | `DRAFT` | Negative readiness — **không** có round |
| `seal-gd1-no-kickoff` | [Dev] GĐ1 — No KICKOFF | `DRAFT` | Partial blocker — thiếu KICKOFF |
| `seal-gd1-no-awards` | [Dev] GĐ1 — No AWARDS | `ONGOING` | Partial blocker — thiếu AWARDS |
| `seal-gd1-judge-final-early` | [Dev] GĐ1 — Judge final early | `ONGOING` | FINAL readiness blocker — chưa guest judge CK (G1-N05) |
| `seal-gd1-event-order-bad` | [Dev] GĐ1 — Event order bad | `ONGOING` | **0** milestone event — POST WS → `EVENT_KICKOFF_MISSING` (G1-N01) |
| `seal-gd1-event-order-violation` | [Dev] GĐ1 — Event order violation | `ONGOING` | Chỉ KICKOFF — POST AWARDS → `EVENT_ORDER_VIOLATION` (G1-N02) |
| `seal-gd1-prelim-only` | [Dev] GĐ1 — Prelim only | `DRAFT` | Prelim + tracks, **không** round CK → `MISSING_FINAL_ROUND` (G1-N08) |
| `seal-gd2-teams-edge` | SEAL GĐ2 — Teams edge | `ONGOING` | 9 đội đa trạng thái (mf02 matrix) |
| `seal-gd2-registration-closed` | SEAL GĐ2 — Registration closed | `ONGOING` | `registration_end` đã qua |
| `seal-gd2-lottery-not-locked` | SEAL GĐ2 — Lottery not locked | `ONGOING` | 3 đội ACTIVE chưa lock — lottery gate (G2-N02) |
| `seal-gd2-round-active` | SEAL GĐ2 — Round active | `ONGOING` | Prelim active + lottery xong — re-lottery → `ROUND_ALREADY_ACTIVE` (B-N2) |
| `seal-fall-2025-finished` | SEAL Fall 2025 (Completed) | `FINISHED` | Archive read-only |

### Profile GĐ3–GĐ6 (slug riêng, xem ma trận từng GĐ)

| GĐ | Số slug | Ví dụ |
|----|---------|-------|
| GĐ3 | 10 | `seal-gd3-prelim-open`, `seal-gd3-no-lottery`, `seal-gd3-mentor-portal`, … |
| GĐ4 | 11 | `seal-gd4-advance-ready`, `seal-gd4-ck-unpublished`, … |
| GĐ5 | 9 | `seal-gd5-final-active`, `seal-gd5-judge-edge`, `seal-gd5-late-pending`, … |
| GĐ6 | 6 | `seal-gd6-pending-confirm`, `seal-gd6-prize-duplicate`, … |

**Đã xóa tự động khi start dev:** slug legacy (`seal-spring-2026*`, `seal-gd1-ready`, …) — xem `DevSeedCatalog.DEPRECATED_SLUGS`.

### Ma trận trạng thái tài khoản (Module 5 — xác thực email + duyệt tài khoản)

> **Seeder:** `AccountStatesDataSeeder` · **Hằng số:** `AccountStatesSeedConstants` · **Mật khẩu chung:** `Account@dev1`
> Đây là seed ở mức **tài khoản** (không phải slug hackathon trong `DevSeedCatalog`). FE mirror: `e2e/helpers/accountStates.js`.

| Email | Role | Status | Verify email | Login → `error.code` | Mục đích |
|-------|------|--------|--------------|----------------------|----------|
| `account.student.unverified@fpt.edu.vn` | STUDENT | PENDING | ❌ | `EMAIL_NOT_VERIFIED` | Gate xác thực email + nút "Gửi lại email xác thực" |
| `account.mentor.pending@fpt.edu.vn` | MENTOR | PENDING | ✅ | `ACCOUNT_PENDING` | Hàng chờ "Duyệt tài khoản" của Coordinator |
| `account.judge.pending@fpt.edu.vn` | JUDGE | PENDING | ✅ | `ACCOUNT_PENDING` | Hàng chờ "Duyệt tài khoản" của Coordinator |
| `account.judge.rejected@fpt.edu.vn` | JUDGE | REJECTED | ✅ | `REJECTED_NOT_ALLOWED_LOGIN` | Login bị chặn + hiển thị ở filter "Đã từ chối" |
| `account.mentor.approved-unverified@fpt.edu.vn` | MENTOR | APPROVED | ❌ | `EMAIL_NOT_VERIFIED` | Đã duyệt nhưng chưa verify — chứng minh 2 cổng độc lập |

Các tài khoản PENDING đóng góp vào badge todo Coordinator ("Duyệt tài khoản") và trang `/admin/users`.

### Negative abuse probes (P1 — không phải slug)

> **FE:** `e2e/helpers/negativeProbes.js` · **CLI:** `npm run probe:seeds` (prefix `neg:`)

| Key | ErrorCode | Mô tả |
|-----|-----------|--------|
| `neg:team-on-draft` | `HACKATHON_NOT_ONGOING` | Orphan tạo đội trên `seal-gd1-incomplete` (DRAFT) |
| `neg:user-in-another-team` | `USER_IN_ANOTHER_TEAM` | Leader tạo đội lần 2 cùng hackathon |
| `neg:registration-elsewhere` | `REGISTRATION_ALREADY_ACTIVE_ELSEWHERE` | SV đã đăng ký giải khác |
| `neg:invalid-repo-platform` | `INVALID_REPO_PLATFORM` | Nộp `drive.google.com` trên `seal-gd3-prelim-open` |
| `neg:scoring-not-open` | `SCORING_NOT_OPEN` | Judge chấm slot WAITING trên `seal-gd3-scoring-gate` |
| `neg:archived-mutation` | `HACKATHON_ARCHIVED` | Mutation trên `seal-fall-2025-finished` |
| `neg:oauth-token-invalid` | `OAUTH_TOKEN_INVALID` | Google idToken rác |
| `neg:duplicate-email` | `ACCOUNT_DUPLICATE_EMAIL` | Register email đã tồn tại |
| `neg:invalid-credentials` | `INVALID_CREDENTIALS` | Login sai mật khẩu |

**E2E UI:** `e2e/abuse-guards.spec.js`, `e2e/account-states.spec.js` (approved-unverified).

---

## Hackathon E2E — `seal-e2e-2026`

### GĐ1 (đã seed)

- Round Sơ loại + Chung kết, 3 track, criteria, events KICKOFF + WORKSHOP
- `GET /hackathons/{id}/readiness?target=ONGOING` → `ready: true`
- Vòng sơ loại **chưa active** (test GĐ2 trước)
- Đăng ký hackathon **còn mở** (~14 ngày, repair mỗi restart)

### GĐ2 (dữ liệu sẵn — `E2eWorkflowDataSeeder`)

| Thành phần | Chi tiết |
|------------|----------|
| **7 đội** | `E2E-T01` … `E2E-T07` — `ACTIVE`, 3 người/đội, **chưa khóa**, **chưa lottery** |
| **3 orphan** | Đã đăng ký hackathon, chưa có đội |

**Email orphan:**

| # | Email | Password |
|---|-------|----------|
| 1 | `student.e2e.orphan1@fpt.edu.vn` | `Student@dev1` |
| 2 | `student.e2e.orphan2@fpt.edu.vn` | `Student@dev1` |
| 3 | `student.e2e.orphan3@fpt.edu.vn` | `Student@dev1` |

**Leader đội:** `student.e2e.t01.leader@fpt.edu.vn` … `t07.leader@` / `Student@dev1`

**Archive Fall 2025 (FR-U-32):** `student.archive.fall2025@fpt.edu.vn` / `Student@dev1`

**Fall track select (FR-U-15-F):** `student.fall.t01.leader@fpt.edu.vn` … `t03.leader@` / `Student@dev1`

**FE E2E:** `npm run test:e2e:gd2` (Playwright `e2e-gd2-e2e-2026.spec.js`, `e2e-gd2-teams-edge.spec.js`)

### GĐ2 negative (slug riêng)

| Slug | Seeder | Mục đích |
|------|--------|----------|
| `seal-gd2-teams-edge` | `Gd2TeamsEdgeDataSeeder` | 9 đội PENDING/REJECTED/ELIMINATED/locked/mentor |
| `seal-gd2-registration-closed` | `Gd2RegistrationClosedDataSeeder` | `REGISTRATION_CLOSED` sau `registration_end` |
| `seal-gd2-lottery-not-locked` | `Gd2LotteryNotLockedDataSeeder` | Đội ACTIVE chưa lock → lottery gate (G2-N02) |
| `seal-gd2-round-active` | `Gd2RoundActiveDataSeeder` | Prelim active + lottery xong → `ROUND_ALREADY_ACTIVE` (B-N2) |

Toggle: `app.seed.gd2.teams-edge.enabled`, `app.seed.gd2.registration-closed.enabled`, `app.seed.gd2.lottery-not-locked.enabled`, `app.seed.gd2.round-active.enabled`

### GĐ3 → GĐ6 (happy path trên cùng slug)

Tiếp tục trên **`seal-e2e-2026`** theo [`happy-path-gd1-gd6-responses.md`](./happy-path-gd1-gd6-responses.md).

Edge case / profile riêng: dùng slug `seal-gd3-*` … `seal-gd6-*` (ma trận từng GĐ).

---

## GĐ1 partial — `seal-gd1-no-kickoff` / `seal-gd1-no-awards`

| Slug | Seeder | Trạng thái | Test |
|------|--------|------------|------|
| `seal-gd1-no-kickoff` | `Gd1NoKickoffDataSeeder` | `DRAFT`, có WORKSHOP, **không** KICKOFF | `readiness?target=ONGOING` → `ready: false` |
| `seal-gd1-no-awards` | `Gd1NoAwardsDataSeeder` | `ONGOING`, **không** AWARDS | `readiness?target=AWARDS` → `ready: false` |

Toggle: `app.seed.gd1.no-kickoff.enabled`, `app.seed.gd1.no-awards.enabled`

---

## GĐ1 negative — `seal-gd1-incomplete`

| | |
|--|--|
| **Seeder** | `Gd1DataSeeder.ensureIncompleteSeed()` |
| **Trạng thái** | `DRAFT`, **không** round/track |
| **Test** | `GET .../readiness?target=ONGOING` → `ready: false` |
| **FE** | `/hackathons/{id}/setup?tab=review` — blockers |

---

## Tài khoản hệ thống

| Role | Email | Password |
|------|-------|----------|
| Coordinator | `coord@fpt.edu.vn` | `Coordinator@dev1` |
| Judge | `judge1@fpt.edu.vn` | `Judge@dev1` |
| Mentor | `mentor@fpt.edu.vn` | `Mentor@dev1` |
| Guest judge | `guestjudge@gmail.com` | `GuestJudge@dev1` |

---

## Cấu hình

```properties
app.seed.e2e.enabled=true
```

---

## FE API gap — màn hình ↔ seed gợi ý

| API / tính năng | Màn FE | Seed / tài khoản gợi ý |
|-----------------|--------|-------------------------|
| `POST /auth/forgot-password`, `reset-password` | `/forgot-password`, `/reset-password` | Không cần seed — public routes |
| `POST /auth/logout-all` | `/profile` → Bảo mật; menu Đăng xuất tất cả thiết bị | Bất kỳ user đã login |
| `GET /me/history` (FR-U-31) | `/student/hackathons/history` | `seal-fall-2025-finished` + student đã tham gia |
| `GET /me/annual-awards` (FR-U-32) | `/student/annual-awards` | `seal-fall-2025-finished` + `student.archive.fall2025@fpt.edu.vn` |
| `POST /me/tracks/{id}/select` (FR-U-15-F) | `/student/team` Fall card | `seal-fall-ongoing-2026` |
| `POST /me/tracks/{id}/select` (FR-U-15-F) | `/student/team` → Fall leader chọn track | Hackathon `season=Fall`, prelim chưa active, đăng ký còn mở |
| `GET /me/rounds/{id}/leaderboard` | `/student/results/:roundId` | Sau publish — `RESULT_NOT_PUBLISHED` khi chưa publish |
| `GET /teams/{id}/mentors` (FR-13C) | Coordinator teams expand + student team collapse | `seal-e2e-2026` sau GĐ3 mentor assignment |
| `GET /me/mentor-track-assignments` (FR-M-05) | `/mentor/rounds` fallback khi chưa có round assignment | Mentor đã gán track GĐ1 |
| `GET /users/me/student-card` | `/onboarding` preview ảnh cũ | Fallback khi Cloudinary 404 |
| `PATCH /me/teams/.../track` (re-lottery) | `/student/team` → Đổi track | `seal-gd2-lottery-not-locked` (prelim chưa active) |
| `GET /me/mentor/rounds/{id}/schedule` (FR-M-16) | `/mentor/support` | Mentor gán đội ở vòng CK |
| `GET /me/mentor/hackathons/{id}/rankings` (FR-M-18) | `/mentor/support` | Sau publish kết quả |
| `GET /me/mentor-history` (FR-M-19) | `/mentor/history` | Mentor đã tham gia mùa trước |
| `GET /teams/{id}/journey` | Coordinator teams expand + student team collapse | `seal-e2e-2026` |
| WS `score-saved`, `leaderboard-preview` | Live Scoring + Round Ranking Preview | Vòng đang chấm / preview |
| WS `presentation-queue` | Presentation Queue (`PresentationQueuePage`) | Prelim có track / CK round-wide — poll fallback 10s khi disconnect |
| `POST /presentation/timer/reset` | Live Scoring — nút Reset Timer (controller) | `seal-gd3-prelim-open` |
| `GET /export-jobs/{id}` poll | Hackathon Results export CSV | Sau `POST …/export-jobs` |
| `POST /me/teams` | Student tạo đội | `seal-e2e-2026` leader chưa có đội |

---

## Playwright FE (dev)

```bash
npm run test:e2e:parity   # FE ↔ BE slug list
npm run probe:seeds       # API state (cần BE dev :8080)
npm run test:e2e:matrix   # 53 slug read-only UI
npm run test:pyramid      # parity + matrix + gd2 (CI subset)
npm run test:e2e:gd2      # GĐ2 trên seal-e2e-2026 + seal-gd2-teams-edge
```

---

## Reset thủ công (nếu cleanup tự động lỗi FK)

Giữ slug trong `DevSeedCatalog.ALL_DEV_HACKATHON_SLUGS`; xóa phần còn lại. Xem SQL mẫu trong [`gd1-full-test-matrix-and-seeds.md`](gd1-full-test-matrix-and-seeds.md).

---

## Tài liệu liên quan

- Ma trận: `gd1-full-test-matrix-and-seeds.md` … `gd6-full-test-matrix-and-seeds.md`
- Happy path API: `happy-path-gd1-gd6-responses.md`
- Audit seed: `seed-coverage-audit.md`
