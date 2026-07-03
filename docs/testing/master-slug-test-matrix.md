# Master slug test matrix (SSOT)

**Cập nhật:** 2026-07-03  
**Nguồn code:** `DevSeedCatalog.ALL_DEV_HACKATHON_SLUGS` (**53 slug**)  
**Mirror FE:** `seal-hackathon-fe/e2e/helpers/devSeedCatalogSlugs.js`, `seedRegistry.js`

Ma trận này là **single source of truth** liên kết slug dev → seeder → từng tầng kim tự tháp kiểm thử. Chi tiết gate/happy từng GĐ: [gd1-full-test-matrix-and-seeds.md](gd1-full-test-matrix-and-seeds.md) … [gd6-full-test-matrix-and-seeds.md](gd6-full-test-matrix-and-seeds.md).

---

## Kim tự tháp kiểm thử

| Layer | Lệnh | Phạm vi |
|-------|------|---------|
| L1 Unit | `cd BE && mvn test` | ~231 test (service, gate helpers) |
| L2 Integration | `cd BE && mvn test -Dtest="*IntegrationTest"` | H2 inline — `Gd*GateIntegrationTest`, flow tests |
| L3 API probe | `cd seal-hackathon-fe && npm run probe:seeds` | 53 slug + 5 account + 15 neg probes |
| L4 Matrix UI | `npm run test:e2e:parity && npm run test:e2e:matrix` | 53 slug read-only |
| L5 Dedicated e2e | `npm run test:e2e:dedicated`, `npm run test:e2e:gd2` | Deep happy/bad |
| L6 Phase 2 | `npm run test:e2e:visual`, `npm run test:e2e:cross-browser` | Nightly / manual |

**CI PR:** [.github/workflows/ci-test-matrix.yml](../../.github/workflows/ci-test-matrix.yml) — L1 + L2 + L3 + L4 (không visual).

---

## Primary slug theo GĐ

| GĐ | Happy primary | Bad / gate primary |
|----|---------------|-------------------|
| GĐ1 | `seal-e2e-2026` | `seal-gd1-incomplete` |
| GĐ2 | `seal-e2e-2026` | `seal-gd2-lottery-not-locked` |
| GĐ2 Fall | `seal-fall-ongoing-2026` | — |
| GĐ3 | `seal-gd3-prelim-open` | `seal-gd3-edge-errors` |
| GĐ4 | `seal-gd4-advance-ready` | `seal-gd4-tiebreak-gate` |
| GĐ5 | `seal-gd5-final-active` | `seal-gd5-edge-errors` |
| GĐ6 | `seal-gd6-pending-confirm` | `seal-gd6-prizes-empty` |

---

## Ma trận 53 slug (rút gọn)

Cột `e2e_dedicated`: tên spec hoặc `matrix-only`. Dev passwords: [dev-seed-guide.md](dev-seed-guide.md).

| slug | gd | status | season | case | gate | be_seeder | primary_roles | e2e_dedicated | mutating |
|------|-----|--------|--------|------|------|-----------|---------------|---------------|----------|
| seal-e2e-2026 | GĐ1–6 | ONGOING | Spring | happy | — | Gd1DataSeeder + E2eWorkflow | coord, student | e2e-gd2-e2e-2026.spec.js | no |
| seal-fall-2025-finished | GĐ1 | FINISHED | Fall | happy | — | Gd1DataSeeder | coord, student.archive | student-portal-parity.spec.js | no |
| seal-gd1-incomplete | GĐ1 | DRAFT | Spring | bad | G1 readiness | Gd1DataSeeder | coord | matrix-only | no |
| seal-gd1-no-kickoff | GĐ1 | DRAFT | Spring | bad | G1-N01 | Gd1NoKickoffDataSeeder | coord | matrix-only | no |
| seal-gd1-no-awards | GĐ1 | ONGOING | Spring | bad | G1 | Gd1NoAwardsDataSeeder | coord | matrix-only | no |
| seal-gd1-judge-final-early | GĐ1 | ONGOING | Spring | gate | G1-N05 | Gd1JudgeFinalEarlyDataSeeder | coord | matrix-only | no |
| seal-gd1-event-order-bad | GĐ1 | ONGOING | Spring | gate | G1-N01 | Gd1EventOrderBadDataSeeder | coord | matrix-only | no |
| seal-gd1-event-order-violation | GĐ1 | ONGOING | Spring | gate | G1-N02 | Gd1EventOrderViolationDataSeeder | coord | matrix-only | no |
| seal-gd1-prelim-only | GĐ1 | DRAFT | Spring | gate | G1-N08 | Gd1PrelimOnlyDataSeeder | coord | matrix-only | no |
| seal-gd2-teams-edge | GĐ2 | ONGOING | Spring | bad | — | Gd2TeamsEdgeDataSeeder | coord | e2e-gd2-teams-edge.spec.js | no |
| seal-gd2-registration-closed | GĐ2 | ONGOING | Spring | bad | — | Gd2RegistrationClosedDataSeeder | student | matrix-only | no |
| seal-gd2-lottery-not-locked | GĐ2 | ONGOING | Spring | gate | G2-N02 | Gd2LotteryNotLockedDataSeeder | coord | matrix-only | no |
| seal-gd2-round-active | GĐ2 | ONGOING | Spring | gate | B-N2 | Gd2RoundActiveDataSeeder | coord | matrix-only | no |
| seal-fall-ongoing-2026 | GĐ2 | ONGOING | Fall | happy | — | FallOngoingDataSeeder | student.fall | fall-track-select.spec.js | yes* |
| seal-gd3-prelim-open | GĐ3 | ONGOING | Spring | happy | — | Gd3PrelimOpenDataSeeder | student | matrix-only | no |
| seal-gd3-late-review | GĐ3 | ONGOING | Spring | happy | — | Gd3LateReviewDataSeeder | coord | matrix-only | no |
| seal-gd3-scoring-live | GĐ3 | ONGOING | Spring | happy | — | Gd3ScoringLiveDataSeeder | judge | matrix-only | no |
| seal-gd3-scoring-gate | GĐ3 | ONGOING | Spring | bad | SCORING_NOT_OPEN | Gd3ScoringGateDataSeeder | judge | abuse-guards.spec.js | no |
| seal-gd3-tiebreak-hybrid | GĐ3 | ONGOING | Spring | hybrid | — | Gd3TiebreakHybridDataSeeder | coord | matrix-only | no |
| seal-gd3-edge-errors | GĐ3 | ONGOING | Spring | bad | — | Gd3EdgeErrorsDataSeeder | student | matrix-only | no |
| seal-gd3-calibration-timer | GĐ3 | ONGOING | Spring | happy | — | Gd3CalibrationTimerDataSeeder | judge | matrix-only | no |
| seal-gd3-judge-mentor-conflict | GĐ3 | ONGOING | Spring | bad | — | Gd3JudgeMentorConflictDataSeeder | coord | matrix-only | no |
| seal-gd3-round-config-edge | GĐ3 | ONGOING | Spring | bad | — | Gd3RoundConfigEdgeDataSeeder | coord | matrix-only | no |
| seal-gd3-no-lottery | GĐ3 | ONGOING | Spring | gate | G3-N01 | Gd3NoLotteryDataSeeder | coord | matrix-only | no |
| seal-gd3-mentor-portal | GĐ3 | ONGOING | Spring | happy | G3-H03 | Gd3MentorPortalDataSeeder | mentor | matrix-only | no |
| seal-gd3-mentor-track-only | GĐ1/GĐ3 | ONGOING | Spring | happy | — | Gd3MentorTrackOnlyDataSeeder | mentor | mentor-track-bootstrap.spec.js | no |
| seal-gd3-team-mentor-history | GĐ3 | ONGOING | Spring | happy | FR-13C | Gd3TeamMentorHistoryDataSeeder | coord, student | team-mentor-history.spec.js | no |
| seal-gd4-advance-ready | GĐ4 | ONGOING | Spring | happy | — | Gd4AdvanceReadyDataSeeder | coord | matrix-only | no |
| seal-gd4-ck-unpublished | GĐ4 | ONGOING | Spring | gate | G4-N01 | Gd4CkUnpublishedDataSeeder | coord | matrix-only | no |
| seal-gd4-published | GĐ4 | ONGOING | Spring | happy | — | Gd4PublishedDataSeeder | coord | matrix-only | no |
| seal-gd4-tiebreak-gate | GĐ4 | ONGOING | Spring | bad | tiebreak | Gd4TiebreakGateDataSeeder | coord | preliminary-results-progression.spec.js | E2E_MUTATING |
| seal-gd4-ck-activate-ready | GĐ4 | ONGOING | Spring | happy | — | Gd4CkActivateReadyDataSeeder | coord | matrix-only | no |
| seal-gd4-edge-errors | GĐ4 | ONGOING | Spring | bad | — | Gd4EdgeErrorsDataSeeder | coord | matrix-only | no |
| seal-gd4-wildcard-resolved | GĐ4 | ONGOING | Spring | happy | — | Gd4WildcardResolvedDataSeeder | coord | matrix-only | no |
| seal-gd4-tiebreak-resolved | GĐ4 | ONGOING | Spring | happy | — | Gd4TiebreakResolvedDataSeeder | coord | matrix-only | no |
| seal-gd4-wildcard-disabled | GĐ4 | ONGOING | Spring | bad | — | Gd4WildcardDisabledDataSeeder | coord | matrix-only | no |
| seal-gd4-judge-assign-warnings | GĐ4 | ONGOING | Spring | happy | — | Gd4JudgeAssignWarningsDataSeeder | coord | matrix-only | no |
| seal-gd4-ck-no-criteria | GĐ4 | ONGOING | Spring | bad | — | Gd4CkNoCriteriaDataSeeder | coord | matrix-only | no |
| seal-gd5-final-active | GĐ5 | ONGOING | Spring | happy | — | Gd5FinalRoundDataSeeder | coord | matrix-only | no |
| seal-gd5-submit-open | GĐ5 | ONGOING | Spring | happy | — | Gd5SubmitOpenDataSeeder | student | matrix-only | no |
| seal-gd5-scoring-live | GĐ5 | ONGOING | Spring | happy | — | Gd5ScoringLiveDataSeeder | judge | matrix-only | no |
| seal-gd5-calibration-timer | GĐ5 | ONGOING | Spring | happy | — | Gd5CalibrationTimerDataSeeder | judge | matrix-only | no |
| seal-gd5-edge-errors | GĐ5 | ONGOING | Spring | bad | — | Gd5EdgeErrorsDataSeeder | student | matrix-only | no |
| seal-gd5-late-hardlock | GĐ5 | ONGOING | Spring | bad | — | Gd5LateHardlockDataSeeder | student | matrix-only | no |
| seal-gd5-judge-edge | GĐ5 | ONGOING | Spring | bad | — | Gd5JudgeEdgeDataSeeder | judge | matrix-only | no |
| seal-gd5-late-pending | GĐ5 | ONGOING | Spring | happy | — | Gd5LatePendingDataSeeder | coord | matrix-only | no |
| seal-gd5-not-advanced | GĐ5 | ONGOING | Spring | bad | — | Gd5NotAdvancedDataSeeder | student | matrix-only | no |
| seal-gd6-pending-confirm | GĐ6 | PENDING_CONFIRM | Spring | happy | — | Gd6PendingConfirmDataSeeder | coord | matrix-only | no |
| seal-gd6-prizes-empty | GĐ6 | PENDING_CONFIRM | Spring | bad | — | Gd6PrizesEmptyDataSeeder | coord | matrix-only | no |
| seal-gd6-confirm-ready | GĐ6 | PENDING_CONFIRM | Spring | happy | — | Gd6ConfirmReadyDataSeeder | coord | matrix-only | no |
| seal-gd6-finished-export | GĐ6 | FINISHED | Spring | happy | — | Gd6FinishedExportDataSeeder | coord | matrix-only | no |
| seal-gd6-edge-errors | GĐ6 | PENDING_CONFIRM | Spring | bad | — | Gd6EdgeErrorsDataSeeder | coord | matrix-only | no |
| seal-gd6-prize-duplicate | GĐ6 | PENDING_CONFIRM | Spring | bad | — | Gd6PrizeDuplicateDataSeeder | coord | matrix-only | no |

\* `seal-fall-ongoing-2026`: probe `POST /me/tracks/{id}/select` mutates `team_round_tracks` — chỉ chạy probe/e2e dedicated, không chạy lặp trên CI matrix workers.

---

## Slug mới (2026-07-01)

### seal-fall-ongoing-2026

- **Mục đích:** FR-U-15-F — Fall leader `POST /me/tracks/{id}/select`
- **Probe:** season=Fall, prelim inactive, select → 200
- **E2E read-only:** `fall-track-select.spec.js` — `student.fall.t01.leader@fpt.edu.vn` (chỉ assert card, không xác nhận)
- **E2E mutating:** `fall-track-select-mutating.spec.js` — `student.fall.t02.leader@fpt.edu.vn` (dedicated-e2e only)
- **Student read-only:** `student.fall.t01.leader@fpt.edu.vn` / `Student@dev1`

### seal-gd3-mentor-track-only

- **Mục đích:** FR-M-05 — mentor track bootstrap (`MentorRoundsPage`)
- **Probe:** `GET /me/mentor-track-assignments` non-empty; `GET /me/mentor/rounds` empty
- **E2E:** `mentor-track-bootstrap.spec.js`
- **Mentor:** `mentor@fpt.edu.vn` / `Mentor@dev1`

### seal-gd3-team-mentor-history

- **Mục đích:** FR-13C — `GET /teams/{id}/mentors` (prelim + semifinal)
- **Probe:** ≥2 mentor history rows
- **E2E:** `team-mentor-history.spec.js`

### seal-fall-2025-finished (enhance)

- **Thêm:** `individual_rankings` cho `student.archive.fall2025@fpt.edu.vn` — FR-U-32
- **E2E:** `student-portal-parity.spec.js`

---

## Integration tests (L2) — traceability

| Class | Slug mirror |
|-------|-------------|
| `Gd1ReadinessGateIntegrationTest` | incomplete, prelim-only, event-order-bad |
| `Gd2LotteryGateIntegrationTest` | lottery-not-locked, round-active |
| `Gd3RoundGateIntegrationTest` | no-lottery |
| `Gd4AdvanceGateIntegrationTest` | ck-unpublished, tiebreak-gate |
| `StudentPortalParityIntegrationTest` | fall-ongoing, fall-finished |

---

## Lệnh developer (tóm tắt)

```bash
# Layer 1–2
cd BE && mvn test
cd BE && mvn test -Dtest="*IntegrationTest"

# Layer 3–4 (BE dev :8080)
cd seal-hackathon-fe && npm run probe:seeds
cd seal-hackathon-fe && npm run test:e2e:parity && npm run test:e2e:matrix

# Layer 5
npm run test:e2e:gd2
npm run test:e2e:dedicated
E2E_MUTATING=1 npm run test:e2e:mutating

# Layer 6 (Phase 2)
npm run test:e2e:visual
npm run test:e2e:cross-browser
npm run test:pyramid   # parity + matrix + gd2
```

---

## E2E bổ sung (không gắn slug riêng)

| Spec | Project | Mô tả |
|------|---------|--------|
| `notifications-smoke.spec.js` | default | Bell panel coordinator — `GET /me/notifications` |
| `event-notification-mutating.spec.js` | mutating-e2e | Tạo event public (`OTHER`) trên `seal-gd1-incomplete` → `EVENT_REMINDER` + hiển thị bell |
| `people-mentor-pool.spec.js` | dedicated-e2e | Tab Nhân sự `seal-e2e-2026` — dropdown mentor gồm INTERNAL judge (`judge1@fpt.edu.vn`) |

**BE notification:** `EVENT_REMINDER` (fan-out lúc tạo/sửa lịch) + `EVENT_UPCOMING` (scheduler `EventReminderScheduler`, lead 24h). API: `GET/PATCH /api/v1/me/notifications`.

---

## Cross-links

- [dev-seed-guide.md](dev-seed-guide.md) — accounts, startup
- [seed-coverage-audit.md](seed-coverage-audit.md) — seeder map
- [test-strategy-gd1-gd6-unit-to-e2e.md](test-strategy-gd1-gd6-unit-to-e2e.md) — chiến lược tổng
