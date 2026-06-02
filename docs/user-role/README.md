# User role portals — Student / Judge / Mentor

**Cập nhật:** 2026-05-29 · Scaffold BE (additive-only)

Package Java: `com.sealhackathon.api.me` (+ `hackathon_registrations`, `appeals`, `certificates`).

| File | Vai trò |
|------|---------|
| [01-student-api-catalog.md](01-student-api-catalog.md) | FR-U — portal `/api/v1/me/*` + alias endpoint cũ |
| [02-judge-api-catalog.md](02-judge-api-catalog.md) | FR-J — portal judge |
| [03-mentor-api-catalog.md](03-mentor-api-catalog.md) | FR-M — portal mentor |
| [04-be-backlog-user-roles.md](04-be-backlog-user-roles.md) | TODO implement / delegate |

**Base URL:** `{API_HOST}/api/v1`

**Endpoint Coordinator / Student cũ (không đổi):** `POST /teams`, `POST /submissions`, `POST /scores`, … — xem `mf02/`, `mf03/`.

**Migration thủ công (bảng mới):** `src/main/resources/db/manual/V20260529_user_role_tables.sql`

## Alias path (user flow doc → BE thực tế)

| User flow (doc) | BE dùng | Ghi chú |
|-----------------|---------|---------|
| `GET/PATCH /api/v1/me` (profile) | `GET/PATCH /api/v1/users/me` | Package `users` |
| `GET /notifications` | `GET /api/v1/me/notifications` | Portal mới |
| `GET /hackathons?status=` | `GET /me/hackathons/browse?status=` | Portal mới |
| `POST /hackathons/{id}/register` | `POST /me/hackathons/{id}/register` | Portal mới |
| `POST /hackathons/{hid}/teams` | `POST /api/v1/teams` | Body có `hackathonId` |
| `GET /rounds/{id}/problem` | `GET /me/rounds/{id}/problem` | Portal mới |
| `GET /teams/{id}/submissions` (Judge/Mentor) | `GET /submissions?teamId=&roundId=` **hoặc** `/me/mentor/teams/{id}/submissions` | Mentor dùng `/me/...` |
| `PATCH /scores/{id}` (comment) | `PATCH /me/scores/{id}/comment` | Portal Judge |
| `GET /hackathons/{id}/prizes` | `GET /hackathons/{id}/prizes` (cũ) + `GET /me/prizes` | Cả hai |
| `GET /rounds/{final}/schedule` (Mentor) | `GET /me/mentor/rounds/{roundId}/schedule` | FR-M-16 stub |
