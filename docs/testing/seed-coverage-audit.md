# Seed coverage audit — GĐ0 → GĐ6

**Cập nhật:** 2026-07-03 (53 slug — xem [master-slug-test-matrix.md](master-slug-test-matrix.md))  
**Profile:** `dev` (`spring.profiles.active=dev`)
**Nguồn sự thật code:** `DevSeedCatalog.ALL_DEV_HACKATHON_SLUGS` (53 slug)

---

## 1. Bản đồ hackathon seed

| Slug | Giai đoạn | Status | Mục đích test |
|------|-----------|--------|---------------|
| `seal-e2e-2026` | GĐ1–2 (+ happy GĐ3→6) | `ONGOING` | Setup GĐ1 + 7 đội `E2E-T*` + 3 orphan |
| `seal-gd1-incomplete` | GĐ1 negative | `DRAFT` | Readiness **FAIL** (không round) |
| `seal-gd1-no-kickoff` | GĐ1 partial | `DRAFT` | Thiếu KICKOFF → ONGOING fail |
| `seal-gd1-no-awards` | GĐ1 partial | `ONGOING` | Thiếu AWARDS event |
| `seal-gd1-judge-final-early` | GĐ1 gate | `ONGOING` | FINAL readiness — chưa guest judge CK (G1-N05), không phải lỗi gán sớm |
| `seal-gd1-event-order-bad` | GĐ1 gate | `ONGOING` | **0** milestone event — POST WS → `EVENT_KICKOFF_MISSING` (G1-N01) |
| `seal-gd1-event-order-violation` | GĐ1 gate | `ONGOING` | Chỉ KICKOFF — POST AWARDS → `EVENT_ORDER_VIOLATION` (G1-N02) |
| `seal-gd1-prelim-only` | GĐ1 gate | `DRAFT` | Có prelim + tracks, **không** round CK → `MISSING_FINAL_ROUND` (G1-N08) |
| `seal-gd2-teams-edge` | GĐ2 bad | `ONGOING` | 9 đội đa trạng thái |
| `seal-gd2-registration-closed` | GĐ2 bad | `ONGOING` | `REGISTRATION_CLOSED` |
| `seal-gd2-lottery-not-locked` | GĐ2 gate | `ONGOING` | Đội chưa lock → `TEAM_NOT_LOCKED` (G2-N02) |
| `seal-gd2-round-active` | GĐ2 gate | `ONGOING` | Prelim active + lottery xong → re-lottery → `ROUND_ALREADY_ACTIVE` (B-N2) |
| `seal-fall-ongoing-2026` | GĐ2 Fall | `ONGOING` | Fall track select FR-U-15-F |
| `seal-fall-2025-finished` | Archive | `FINISHED` | Read-only + `individual_rankings` FR-U-32 |
| `seal-gd3-prelim-open` | GĐ3 | `ONGOING` | Sơ loại active, chưa lock |
| `seal-gd3-late-review` | GĐ3 | `ONGOING` | LATE_PENDING duyệt trễ |
| `seal-gd3-scoring-live` | GĐ3 | `ONGOING` | Queue PRESENTING |
| `seal-gd3-scoring-gate` | GĐ3 | `ONGOING` | Slot WAITING → `SCORING_NOT_OPEN` |
| `seal-gd3-tiebreak-hybrid` | GĐ3 | `ONGOING` | Tiebreak + penalty |
| `seal-gd3-edge-errors` | GĐ3 bad | `ONGOING` | INCOMPLETE, ROUND_NOT_ACTIVE |
| `seal-gd3-calibration-timer` | GĐ3 | `ONGOING` | Calibration + timer PAUSED/QA |
| `seal-gd3-judge-mentor-conflict` | GĐ3 bad | `ONGOING` | `CONFLICT_MENTOR_JUDGE_SAME_TRACK` |
| `seal-gd3-round-config-edge` | GĐ3 bad | `ONGOING` | `ROUND_NO_CRITERIA` / `ROUND_WEIGHT_NOT_ONE` |
| `seal-gd3-no-lottery` | GĐ3 gate | `ONGOING` | 0 participation → `NO_TEAMS_IN_ROUND` (G3-N01) |
| `seal-gd3-mentor-portal` | GĐ3 happy | `ONGOING` | Mentor portal — 2 đội assigned (G3-H03) |
| `seal-gd3-mentor-track-only` | GĐ3 happy | `ONGOING` | Mentor track only — FR-M-05 bootstrap |
| `seal-gd3-team-mentor-history` | GĐ3 happy | `ONGOING` | Mentor history ≥2 vòng — FR-13C |
| `seal-gd4-advance-ready` | GĐ4 | `ONGOING` | Publish → advance |
| `seal-gd4-ck-unpublished` | GĐ4 gate | `ONGOING` | Activate CK → `RESULT_NOT_PUBLISHED` (G4-N01) |
| `seal-gd4-published` | GĐ4 | `ONGOING` | Đã publish |
| `seal-gd4-tiebreak-gate` | GĐ4 bad | `ONGOING` | TIEBREAK_REQUIRED |
| `seal-gd4-ck-activate-ready` | GĐ4 | `ONGOING` | Activate CK ready |
| `seal-gd4-edge-errors` | GĐ4 bad | `ONGOING` | JUDGE_NOT_ASSIGNED |
| `seal-gd4-wildcard-resolved` | GĐ4 | `ONGOING` | Wildcard resolved |
| `seal-gd4-tiebreak-resolved` | GĐ4 | `ONGOING` | Tiebreak resolved |
| `seal-gd4-wildcard-disabled` | GĐ4 bad | `ONGOING` | Wildcard off → empty candidates |
| `seal-gd4-judge-assign-warnings` | GĐ4 | `ONGOING` | Assign judge SL → warnings |
| `seal-gd4-ck-no-criteria` | GĐ4 bad | `ONGOING` | CK activate `ROUND_NO_CRITERIA` |
| `seal-gd5-final-active` | GĐ5 | `ONGOING` | CK active mixed |
| `seal-gd5-submit-open` | GĐ5 | `ONGOING` | CK submit sạch |
| `seal-gd5-scoring-live` | GĐ5 | `ONGOING` | CK queue live |
| `seal-gd5-calibration-timer` | GĐ5 | `ONGOING` | CK calibration |
| `seal-gd5-edge-errors` | GĐ5 bad | `ONGOING` | CK **`is_active=false`** → `ROUND_NOT_ACTIVE` khi nộp |
| `seal-gd5-late-hardlock` | GĐ5 bad | `ONGOING` | HARD_LOCK deadline |
| `seal-gd5-judge-edge` | GĐ5 bad | `ONGOING` | `JUDGE_NOT_ASSIGNED` CK |
| `seal-gd5-late-pending` | GĐ5 | `ONGOING` | CK `LATE_PENDING` duyệt trễ |
| `seal-gd5-not-advanced` | GĐ5 bad | `ONGOING` | `TEAM_NOT_IN_ROUND` |
| `seal-gd6-pending-confirm` | GĐ6 | `PENDING_CONFIRM` | Trao giải + confirm |
| `seal-gd6-prizes-empty` | GĐ6 bad | `PENDING_CONFIRM` | NO_PRIZES |
| `seal-gd6-confirm-ready` | GĐ6 | `PENDING_CONFIRM` | 3 giải, confirm OK |
| `seal-gd6-finished-export` | GĐ6 | `FINISHED` | Export CSV |
| `seal-gd6-edge-errors` | GĐ6 bad | `PENDING_CONFIRM` | CK not locked |
| `seal-gd6-prize-duplicate` | GĐ6 bad | `PENDING_CONFIRM` | `PRIZE_DUPLICATE` |

**Legacy (purged on dev start):** `seal-spring-2026*`, `seal-gd1-ready` — không dùng.

---

## 2. Seeder & thứ tự chạy (`DataInitializer`)

| Class | Slug / phạm vi |
|-------|----------------|
| `DevSeedCleanup` | Xóa `DEPRECATED_SLUGS` |
| `Gd1DataSeeder` | `seal-e2e-2026`, `seal-fall-2025-finished`, `seal-gd1-incomplete` |
| `Gd1NoKickoffDataSeeder` | `seal-gd1-no-kickoff` |
| `Gd1NoAwardsDataSeeder` | `seal-gd1-no-awards` |
| `Gd1JudgeFinalEarlyDataSeeder` | `seal-gd1-judge-final-early` |
| `Gd1EventOrderBadDataSeeder` | `seal-gd1-event-order-bad` |
| `Gd1EventOrderViolationDataSeeder` | `seal-gd1-event-order-violation` |
| `Gd1PrelimOnlyDataSeeder` | `seal-gd1-prelim-only` |
| `E2eWorkflowDataSeeder` | GĐ2 teams/orphan trên `seal-e2e-2026` |
| `Gd2TeamsEdgeDataSeeder` | `seal-gd2-teams-edge` |
| `Gd2RegistrationClosedDataSeeder` | `seal-gd2-registration-closed` |
| `Gd2LotteryNotLockedDataSeeder` | `seal-gd2-lottery-not-locked` |
| `Gd2RoundActiveDataSeeder` | `seal-gd2-round-active` |
| `FallOngoingDataSeeder` | `seal-fall-ongoing-2026` |
| `Gd3*DataSeeder` ×13 | `seal-gd3-*` (incl. scoring-gate, mentor-track-only, team-mentor-history) |
| `Gd4*DataSeeder` ×11 | `seal-gd4-*` |
| `Gd5*DataSeeder` ×9 | `seal-gd5-*` |
| `Gd6*DataSeeder` ×6 | `seal-gd6-*` |

Mọi seeder GĐ3–6 có `repairForFeTesting()` sau restart.

---

## 3. FE E2E coverage

| Suite | Lệnh | Phạm vi |
|-------|------|---------|
| Parity FE ↔ BE | `npm run test:e2e:parity` | 53 slug — order + count khớp `DevSeedCatalog` |
| API state probe | `npm run probe:seeds` | 53 slug + 5 account + 15 neg abuse probes |
| Ma trận 53 slug | `npm run test:e2e:matrix` | Read-only UI per slug |
| Abuse guards UI | `npx playwright test e2e/abuse-guards.spec.js` | P1 bad-path UI |
| Account states UI | `npx playwright test e2e/account-states.spec.js` | Account gates (email/duyệt) |
| Dedicated e2e | `npm run test:e2e:dedicated` | fall-track (t01 read-only + t02 mutating), portal-parity, mentor-bootstrap, mentor-history, **people-mentor-pool** (mentor dropdown gồm INTERNAL judge) |
| Mutating e2e | `npm run test:e2e:mutating` | hackathon-progression-mutating, **event-notification-mutating** (tạo event `OTHER` public → `EVENT_REMINDER`) |
| Notifications smoke | `npx playwright test e2e/notifications-smoke.spec.js` | Bell panel coordinator (default project) |
| GĐ2 E2E | `npm run test:e2e:gd2` | Happy `seal-e2e-2026` + read-only `seal-gd2-teams-edge` |
| Pyramid | `npm run test:pyramid` | parity + matrix + gd2 |

---

## 4. Verify nhanh sau restart BE

```http
GET /api/v1/hackathons?size=50
```

Kỳ vọng **COUNT ≥ 49** hackathon dev slug `seal-*` (có thể thêm hackathon tay tạo).

```http
GET /api/v1/hackathons?q=seal-gd1-incomplete
GET /api/v1/hackathons/{id}/readiness?target=ONGOING
```

Kỳ vọng `ready: false`, có blockers.

---

## 5. Tài liệu chi tiết

- [dev-seed-guide.md](dev-seed-guide.md)
- [gd1-full-test-matrix-and-seeds.md](gd1-full-test-matrix-and-seeds.md) … [gd6-full-test-matrix-and-seeds.md](gd6-full-test-matrix-and-seeds.md)
