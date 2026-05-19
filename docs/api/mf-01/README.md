# MF-01 — API catalog (index)

> **Kiến trúc v3:** Hackathon → Round → Track.  
> **Nguồn chính khi implement / test API:**
> - [Quy trình API GĐ1 (7 bước)](../../workflow/mf01-gd1-quy-trinh-api.md)
> - [Spec nghiệp vụ MF-01](../../workflow/mf01.md) §10
> - [Đối chiếu + Implementation](../../workflow/mf01-gd1-doi-chieu.md)

Thư mục này giữ **chi tiết request/validation** theo từng FR (conventions, security, từng module). Round và Track **không** có file riêng — xem runbook **Bước 2–3**.

## Cấu trúc

| File | Nội dung |
|------|----------|
| [_conventions.md](./_conventions.md) | Envelope, error/warning codes, paging, audit |
| [_security.md](./_security.md) | JWT, `@CoordinatorOnly` |
| [fr-01-hackathons.md](./fr-01-hackathons.md) | FR-01 Hackathon CRUD |
| [fr-04-criteria.md](./fr-04-criteria.md) | FR-04 Criteria (track + round FINAL) |
| [fr-05-personnel.md](./fr-05-personnel.md) | FR-05 Mentor / Judge / temp judge |
| [fr-06-status.md](./fr-06-status.md) | FR-07 readiness + PATCH status |
| [fr-06a-events.md](./fr-06a-events.md) | FR-06 Events |
| [fr-06b-activate.md](./fr-06b-activate.md) | FR-07B activate round |

## FR-02 Round & FR-03 Track

| FR | Runbook |
|----|---------|
| FR-02 Round | [mf01-gd1-quy-trinh-api.md — Bước 2](../../workflow/mf01-gd1-quy-trinh-api.md) |
| FR-03 Track | [mf01-gd1-quy-trinh-api.md — Bước 3](../../workflow/mf01-gd1-quy-trinh-api.md) |

Endpoint chính: `POST/GET /hackathons/{id}/rounds`, `POST/GET /rounds/{id}/tracks`, `GET /hackathons/{id}/tracks`, `GET/PUT/DELETE /tracks/{id}`.

## Đã gỡ (không còn trong code)

- `POST/GET /api/v1/tracks/{trackId}/rounds`
- `POST /api/v1/hackathons/{hackathonId}/tracks` (tạo track — dùng `POST /rounds/{roundId}/tracks`)
