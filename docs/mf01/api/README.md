# MF-01 — API catalog (index)

## Swagger UI (OpenAPI 3)

| Mục | URL |
|-----|-----|
| Swagger UI | http://localhost:8080/swagger-ui.html |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |
| OpenAPI YAML | http://localhost:8080/v3/api-docs.yaml |

Bật mọi profile (SpringDoc 3.x). Auth JWT (`bearerAuth`) **đã wire** (MF-02): Try it out cần `POST /api/v1/auth/login` trước — xem [../../mf02/01-auth-users.md](../../mf02/01-auth-users.md).

---

> **Kiến trúc v3:** Hackathon → Round → Track.  
> **Nguồn chính khi implement / test API:**
> - [Quy trình API GĐ1 (7 bước)](../04-quy-trinh-van-hanh.md)
> - [Spec nghiệp vụ MF-01](../02-functional-requirements.md)
> - [Timeline & Events](../04-quy-trinh-van-hanh.md#timeline--events)

Thư mục này giữ **chi tiết request/validation** theo từng FR (conventions, security, từng module). Round và Track **không** có file riêng — xem runbook **Bước 2–3**.

## Cấu trúc

| File | Nội dung |
|------|----------|
| [_conventions.md](./_conventions.md) | Envelope, error/warning codes, paging, audit |
| [_security.md](./_security.md) | JWT, `@CoordinatorOnly` |
| [../../mf02/01-auth-users.md](../../mf02/01-auth-users.md) | MF-02 Auth / User API runbook |
| [../../mf02/02-invitations.md](../../mf02/02-invitations.md) | **Judge khách** (email + login + đổi MK) |
| [../../mf02/03-oauth-prep.md](../../mf02/03-oauth-prep.md) | Chuẩn bị OAuth Google (đợt 2) |
| [fr-01-hackathons.md](./fr-01-hackathons.md) | FR-01 Hackathon CRUD |
| [fr-04-criteria.md](./fr-04-criteria.md) | FR-04 Criteria (track + round FINAL) |
| [fr-05-personnel.md](./fr-05-personnel.md) | FR-05 Mentor / Judge / **judge khách (loại 3)** |
| [fr-06-status.md](./fr-06-status.md) | FR-07 readiness + PATCH status |
| [fr-06a-events.md](./fr-06a-events.md) | FR-06 Events → [timeline đầy đủ](../04-quy-trinh-van-hanh.md#timeline--events) |
| [fr-06b-activate.md](./fr-06b-activate.md) | FR-07B activate round |

## FR-02 Round & FR-03 Track

| FR | Runbook |
|----|---------|
| FR-02 Round | [04-quy-trinh-van-hanh.md — Bước 2](../04-quy-trinh-van-hanh.md) |
| FR-03 Track | [04-quy-trinh-van-hanh.md — Bước 3](../04-quy-trinh-van-hanh.md) |

Endpoint chính: `POST/GET /hackathons/{id}/rounds`, `POST/GET /rounds/{id}/tracks`, `GET /hackathons/{id}/tracks`, `GET/PUT/DELETE /tracks/{id}`.

## Đã gỡ (không còn trong code)

- `POST/GET /api/v1/tracks/{trackId}/rounds`
- `POST /api/v1/hackathons/{hackathonId}/tracks` (tạo track — dùng `POST /rounds/{roundId}/tracks`)
