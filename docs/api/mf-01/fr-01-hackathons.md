# FR-01 — Tạo / quản lý Hackathon

> Workflow v3.1 ref: GĐ1 — Bước 1 | DB v2.1 ref: `hackathons`

## Endpoint table

| # | Method | Path | Auth | Status thành công |
|---|---|---|---|---|
| 1 | POST | `/api/v1/hackathons` | COORDINATOR | 201 |
| 2 | GET | `/api/v1/hackathons` | COORDINATOR | 200 |
| 3 | GET | `/api/v1/hackathons/{id}` | COORDINATOR | 200 |
| 4 | PUT | `/api/v1/hackathons/{id}` | COORDINATOR | 200 |
| 5 | DELETE | `/api/v1/hackathons/{id}` | COORDINATOR | 200 |

---

## 1. POST `/api/v1/hackathons` — Tạo Hackathon mới

### Request
```json
{
  "name":               "SEAL Spring 2026",
  "slug":               "seal-spring-2026",
  "season":             "Spring",
  "year":               2026,
  "description":        "...",
  "rules":              "...",
  "bannerUrl":          "https://...",
  "registrationStart":  "2026-02-01",
  "registrationEnd":    "2026-02-28",
  "eventStart":         "2026-03-01",
  "eventEnd":           "2026-04-15",
  "wildcardEnabled":    false,
  "individualRankingEnabled": false,
  "chapterScoringFormula": null
}
```

### Validation (Bean Validation + business)
| Field | Rule | Error |
|---|---|---|
| `name` | NotBlank, max 300 | 400 |
| `slug` | NotBlank, regex `^[a-z0-9-]+$`, max 150 | 400 |
| `season` | NotNull, enum Spring/Summer/Fall/Winter | 400 |
| `year` | NotNull, ≥ 2024 (configurable) | 400 |
| `registrationEnd` | ≥ `registrationStart` (annotation `@DateRange`) | 400 |
| `eventEnd` | ≥ `eventStart` | 400 |
| `eventStart` | ≥ `registrationEnd` (business) | 422 `HACKATHON_DATE_RANGE` |
| `(name, season, year)` | UNIQUE | 409 `HACKATHON_DUPLICATE` |
| `slug` | UNIQUE | 409 `HACKATHON_DUPLICATE` |
| `status` | KHÔNG nhận từ client — luôn DRAFT | (ignored) |

### Response 201
```json
{
  "success": true,
  "data": {
    "id": 42,
    "name": "SEAL Spring 2026",
    "slug": "seal-spring-2026",
    "season": "Spring",
    "year": 2026,
    "status": "DRAFT",
    "registrationStart": "2026-02-01",
    "registrationEnd":   "2026-02-28",
    "eventStart":        "2026-03-01",
    "eventEnd":          "2026-04-15",
    "wildcardEnabled":   false,
    "individualRankingEnabled": false,
    "createdById": 1,
    "createdAt": "2026-05-16T09:57:00Z"
  },
  "message": "Created"
}
```

`Location: /api/v1/hackathons/42`

### Audit
- Action: `HACKATHON_CREATE`
- Detail: snapshot toàn bộ payload + `createdById`

### Errors
| Status | Code | Trigger |
|---|---|---|
| 400 | `VALIDATION_FAILED` | Bean Validation |
| 409 | `HACKATHON_DUPLICATE` | UNIQUE(name,season,year) hoặc slug |
| 422 | `HACKATHON_DATE_RANGE` | `eventStart < registrationEnd` |

### Pseudocode service
```
@Transactional
HackathonResponse create(req, currentUserId):
  if exists(name, season, year): throw ConflictException(HACKATHON_DUPLICATE)
  if exists(slug):                throw ConflictException(HACKATHON_DUPLICATE)
  if req.eventStart < req.registrationEnd:
      throw BusinessRuleException(HACKATHON_DATE_RANGE)
  entity = mapper.toEntity(req)
  entity.status = DRAFT
  entity.createdBy = userRef(currentUserId)
  saved = repo.save(entity)
  audit.log(HACKATHON_CREATE, "hackathons", saved.id, snapshot(saved))
  return mapper.toResponse(saved)
```

---

## 2. GET `/api/v1/hackathons` — List + filter

### Query params
| Param | Type | Default | Mô tả |
|---|---|---|---|
| `status` | enum | — | Filter status |
| `year` | int | — | Filter year |
| `season` | enum | — | Filter season |
| `q` | string | — | Full-text search name/slug (LIKE) |
| `page` | int | 0 | |
| `size` | int | 20 | max 100 |
| `sort` | string | `createdAt,desc` | |

### Response 200 — paged
```json
{
  "success": true,
  "data": {
    "items": [
      { "id": 42, "name": "...", "slug": "...", "season": "Spring", "year": 2026, "status": "DRAFT", ... }
    ],
    "page": 0, "size": 20, "totalElements": 3, "totalPages": 1
  }
}
```

---

## 3. GET `/api/v1/hackathons/{id}` — Chi tiết

### Response 200
Full `HackathonResponse` (như POST response).

### Errors
- 404 `RESOURCE_NOT_FOUND`

---

## 4. PUT `/api/v1/hackathons/{id}` — Sửa info

### Constraint
- Chỉ cho phép khi `status = DRAFT`.
- KHÔNG được sửa `status` qua endpoint này (dùng FR-06).
- Sửa `(name, season, year)` → re-validate UNIQUE.

### Request
Body giống POST (trừ `status`).

### Errors
| Status | Code | Trigger |
|---|---|---|
| 409 | `HACKATHON_NOT_DRAFT` | status ≠ DRAFT |
| 409 | `HACKATHON_DUPLICATE` | đổi name/season/year trùng record khác |

### Audit
- `HACKATHON_UPDATE` + detail `{ before, after }`

---

## 5. DELETE `/api/v1/hackathons/{id}` — Xóa

### Constraint
- Chỉ cho phép khi `status = DRAFT` AND không có Track / Round / Event nào.

### Response 200
```json
{ "success": true, "data": { "deletedId": 42 } }
```

### Errors
| Status | Code | Trigger |
|---|---|---|
| 409 | `HACKATHON_NOT_DRAFT` | status ≠ DRAFT |
| 409 | `HACKATHON_HAS_CHILDREN` | còn Track/Round/Event |

### Audit
- `HACKATHON_DELETE` + detail snapshot

---

## Mapping bảng DB

| Field DB | DTO field | Note |
|---|---|---|
| `hackathons.status` | luôn DRAFT khi tạo; sửa qua FR-06 PATCH `/status` | — |
| `hackathons.created_by` | từ `CurrentUserAccessor` | — |
| `hackathons.created_at` | DB default NOW() | — |

## Bảng liên quan
- `users` (validate Coordinator qua `@CoordinatorOnly` — module Auth)
- `tracks`, `rounds`, `events` (guard khi DELETE)
- `audit_logs` (ghi mọi mutation)

## Test cases gợi ý
1. Tạo Hackathon hợp lệ → 201, response có id, status=DRAFT.
2. Tạo trùng (name,season,year) → 409 `HACKATHON_DUPLICATE`.
3. `eventStart` < `registrationEnd` → 422 `HACKATHON_DATE_RANGE`.
4. PUT khi status=ONGOING → 409 `HACKATHON_NOT_DRAFT`.
5. DELETE khi đã tạo Track → 409 `HACKATHON_HAS_CHILDREN`.
6. List filter `status=DRAFT&year=2026` → trả paged.
