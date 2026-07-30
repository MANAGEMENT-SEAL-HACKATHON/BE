# BE Clean Architecture Audit

**Date:** 2026-07-28  
**Status:** **Full cleanup done** (Phases 1–5)  
**Verdict:** Package-by-feature layered architecture **compliant**. **0** controllers inject repositories.

## Target flow

```
Controller → Service (interface/impl) → Repository → Entity
```

Not full Clean Architecture (no domain/ports rings). Matches Spring Boot modular monolith + feature packages.

**SSOT API style:** [api-conventions.md](./api-conventions.md)

## Controller compliance (44)

| Area | Status |
|------|--------|
| All 44 REST controllers | Delegate to services only — **no repository injection** |
| `audit_logs`, `hackathons` | Critical leaks fixed (prior audit) |
| `presentation/queue` | Validation + audit in `PresentationQueueService` (Phase 1) |
| `rounds/progression` | Ranking warnings in `RoundProgressionService` (Phase 1) |
| `announcements` | Returns `AnnouncementResponse` DTO (Phase 1) |
| ~~`wildcard_reviews`~~ | **Removed (Phase 9)** — package/API deleted |
| `rbl/dashboard` | Native SQL in `RblDashboardQueryRepository` (Phase 1) |
| `rbl/calibration/*` | `ResponseEntity<ApiResponse<T>>` envelope (Phase 2) |
| `common/system/HealthCheckController` | Co-located with `SystemTimeController` (Phase 3) |

## Phase summary

| Phase | Focus | Commit |
|-------|--------|--------|
| 1 | Layer/boundary cleanup | `refactor(be): move orchestration to services (phase 1)` |
| 2 | API envelope (201, DELETE, traceId, mapping style) | `refactor(be): standardize API response envelope (phase 2)` |
| 3 | Deprecated wildcard PATCH, dead code | `chore(be): remove deprecated wildcard API and dead code (phase 3)` |
| 4 | Docs, tests, OpenAPI | `docs(be): audit cleanup and fix failing tests (phase 4)` |
| 5 | ArchUnit, CI verify, god-class split | `refactor(be): architecture hardening archunit portal ci (phase 5)` |

## Remaining deferred (not blocking)

| Item | Notes |
|------|-------|
| `JudgeAccessGuard` | Placeholder — judge scope enforced in `JudgePortalServiceImpl` |
| `RoundProgressionServiceImpl` size | Partially split via `RoundLockScoringService` / `RoundAdvanceService` (Phase 5) |
| N+1 in `TeamServiceImpl` | Performance — profile first |
| `config/seed/` refactor | Operational — works as-is |

## Package structure notes

- Standard slice: `controller / service / repository / entity / dto` per feature
- Internal-only (no REST): `appeals`, `notifications`, `oauth_accounts`, `tiebreak_evaluations`, `individual_rankings`
- Portal: `me/*` — role-based `/api/v1/me` aliases
- Nested feature: `rbl/calibration/`

## Automated guards

- **ArchUnit:** `LayeredArchitectureTest` — controllers must not depend on repositories (Phase 5)
- **CI PR:** full `mvn verify` job before merge (Phase 5)
