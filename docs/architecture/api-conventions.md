# API Conventions — Seal Hackathon BE

Single source of truth for REST API style in `com.sealhackathon.api`.

## Base path

- All REST controllers use prefix **`/api/v1`** via class-level `@RequestMapping("/api/v1")` (preferred) or feature sub-path (e.g. `/api/v1/me`, `/api/v1/teams`).
- Exception: `HealthCheckController` at `/` (ops probe).

## Response envelope

### Success (JSON)

```json
{
  "success": true,
  "data": { },
  "message": "optional",
  "warnings": [],
  "timestamp": "2026-07-28T12:00:00Z"
}
```

- Return type: `ResponseEntity<ApiResponse<T>>` (always wrap, including calibration controllers).
- Use `ApiResponse.created(data)` for **201 Created**.
- Use `ApiResponse.okWithWarnings(data, warnings)` when business warnings apply.

### Errors (4xx / 5xx)

```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_FAILED",
    "message": "Yêu cầu không hợp lệ",
    "status": 400,
    "details": null
  },
  "traceId": "uuid",
  "timestamp": "2026-07-28T12:00:00Z"
}
```

- Handled by `GlobalExceptionHandler`.
- **`traceId`** is set on every error path (auth, validation, business, 500).
- Bean validation uses `ValidationErrorResponse` with `error.fields[]`.

### Binary downloads

- PDF/CSV/banner: `ResponseEntity<Resource>` or `byte[]` — **do not** wrap in `ApiResponse`.

## HTTP status codes

| Operation | Status | Body |
|-----------|--------|------|
| GET single/list | 200 | `ApiResponse` |
| POST create | **201** | `ApiResponse.created(...)` |
| PUT/PATCH update | 200 | `ApiResponse` |
| DELETE | **200** | `ApiResponse` with `{ "deletedId": id }` (or composite keys when no single id) |
| Command POST (`/advance`, `/lock-scoring`, …) | 200 | `ApiResponse` |

Avoid `204 No Content` for JSON APIs — use 200 + envelope for FE interceptor consistency.

## HTTP verbs

| Verb | Use |
|------|-----|
| **PUT** | Full replace of admin resources (Hackathon, Round, Event, Criterion) |
| **PATCH** | Partial update / status transitions (Team status, member accept/reject) |
| **POST** | Create resources and **commands** (non-idempotent actions) |

Do not mass-migrate PUT↔PATCH without FE coordination (breaking).

## Pagination defaults

| Context | Default `size` |
|---------|----------------|
| Admin lists (hackathons, audit) | **10** |
| User search / portal lists | **20** |

Document per-endpoint overrides in OpenAPI `@Parameter` when non-default.

## Layering

```
Controller → Service (+ impl) → Repository
```

- Controllers: HTTP mapping, auth annotations, delegate to service.
- No repository injection in controllers (enforced by ArchUnit in Phase 5).
- Portal controllers (`/api/v1/me/*`) may call feature services directly; prefer `*PortalService` facade for new code.

## Naming

- Nested resources: `/hackathons/{id}/rounds`, `/rounds/{id}/tracks`.
- Inverse lookups: `/tracks/{id}/judges`, `/users/{id}/round-assignments`.
- Deprecated APIs: remove in major cleanup; do not leave stale `@Deprecated` endpoints without sunset date.

## Related docs

- [be-clean-architecture-audit.md](./be-clean-architecture-audit.md)
- OpenAPI: `/swagger-ui.html` (see `OpenApiConfig`)
