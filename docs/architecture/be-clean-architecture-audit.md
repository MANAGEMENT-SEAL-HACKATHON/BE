# BE Clean Architecture Audit (Critical-Only Fix)

**Date:** 2026-07-28  
**Scope:** Controllers must not call repositories; controllers only delegate to services; remove safe dead code.  
**Verdict:** Package-by-feature layered architecture ~95% compliant. After this fix: **0** controllers inject repositories.

## Target flow

```
Controller → Service (interface/impl) → Repository → Entity
```

Not full Clean Architecture (no domain/ports rings). Matches Spring Boot modular monolith + feature packages.

## Controller compliance (44)

| # | Controller | Status |
|---|------------|--------|
| 1 | `announcements/AnnouncementController` | OK (entity in response — low, deferred) |
| 2 | `audit_logs/AuditLogController` | **Critical fixed** — now `AuditLogService` |
| 3 | `auth/AuthController` | OK |
| 4 | `common/system/SystemTimeController` | OK (infra endpoint) |
| 5 | `criteria/CriteriaController` | OK |
| 6 | `criteria/CriteriaTemplateController` | OK |
| 7 | `events/EventController` | OK |
| 8 | `export_jobs/ExportJobController` | OK |
| 9 | `hackathons/HackathonClosureController` | OK |
| 10 | `hackathons/HackathonController` | **Critical fixed** — schedule adjust via service only |
| 11 | `hackathons/HackathonStatusController` | OK |
| 12 | `invitations/InvitationController` | OK |
| 13 | `judge_assignments/JudgeAssignmentController` | OK |
| 14–21 | `me/*` (8 controllers) | OK |
| 22 | `mentors/MentorAssignmentController` | OK |
| 23–26 | `presentation/*` (4) | OK; Queue has medium orchestration (deferred) |
| 27–28 | `prizes/*` (2) | OK |
| 29–30 | `rbl/calibration/*` (2) | OK |
| 31 | `rbl/RblDashboardController` | OK |
| 32 | `rounds/RoundActivationController` | OK |
| 33 | `rounds/RoundController` | OK |
| 34 | `rounds/RoundProgressionController` | OK; injects query service (medium, deferred) |
| 35 | `scores/ScoreController` | OK |
| 36 | `submissions/SubmissionController` | OK |
| 37 | `system/HealthCheckController` | OK |
| 38–39 | `teams/*` (2) | OK |
| 40 | `tracks/TrackController` | OK |
| 41–44 | `users/*` (4) | OK |
| — | `wildcard_reviews/WildcardReviewController` | OK layer (delegates `RoundProgressionService`; no own service — deferred) |

## Critical fixes applied

### 1. AuditLogController → AuditLogService

- Added `audit_logs/service/AuditLogService` + `service/impl/AuditLogServiceImpl`
- Moved ownership check + query routing + pagination map into service
- Controller: `currentUserId()` → `auditLogService.list(...)` only
- JSON shape unchanged: `items`, `page`, `size`, `total`

### 2. HackathonController schedule adjust

- Added `CompetitionScheduleAdjustService.adjust(hackathonId, newPrelimExamAt, overrides)`
- `@Transactional` on `adjust()` (self-invocation safe)
- Removed `HackathonRepository` from controller
- Close-reg path unchanged (`HackathonRegistrationCloseServiceImpl` still locks + `apply`)

## Dead code removed

| Item | Action |
|------|--------|
| `HackathonPostRegistrationTimelineService` | Deleted (deprecated shim, 0 production injects) |
| `HackathonPostRegistrationTimelineServiceTest` | Deleted |
| `calibration_sessions/` package | Verified absent / empty — no-op |

## Medium / low (deferred — out of critical-only scope)

| Finding | Severity | Notes |
|---------|----------|-------|
| `PresentationQueueController` force-ack validation + audit log | Medium | Move into `PresentationQueueService` |
| `RoundProgressionController` + `RoundRankingQueryService` | Medium | Fold warning into progression service |
| `RblDashboardServiceImpl` native SQL via `EntityManager` | Medium | Extract query repository |
| `AnnouncementController` returns JPA entity | Low | Introduce DTO |
| `wildcard_reviews` has no feature service | Low | Acceptable layer-wise; boundary smell |
| `database-schema.mdc` still says `com.se194093.be` | Hygiene | Update to `com.sealhackathon.api` |
| Deprecated `PATCH /wildcard-reviews/{id}` | Keep | Docs/tests still reference |

## Package structure notes

- Standard slice: `controller / service / repository / entity / dto` per feature
- Internal-only (no REST): `appeals`, `notifications`, `certificates`, `oauth_accounts`, `tiebreak_evaluations`, `individual_rankings`
- `presentation` services use `events.entity.PresentationSlot` (split ownership)
- Nested feature: `rbl/calibration/`

## Phase-after checklist

- [ ] Presentation queue: validation + audit inside service
- [ ] Round progression: incomplete-scoring warning inside service
- [ ] RBL analytics: dedicated query repository
- [ ] Announcement DTO
- [ ] Fix cursor rule package path
- [ ] Consider removing deprecated wildcard decide API when docs/tests updated
