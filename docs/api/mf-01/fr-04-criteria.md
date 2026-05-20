# FR-04 — Quản lý Criteria (tiêu chí chấm điểm) + kế thừa kỳ trước

> Workflow v3.1 ref: GĐ1 — Bước 4 | DB v2.1 ref: `criteria` (self-ref `source_criteria_id`)

> **QUAN TRỌNG v2.2**: Bước 4 chỉ CẢNH BÁO MỀM về tổng weight (UI hiển thị realtime
> "Tổng weight: 0.85/1.0" màu vàng). KHÔNG block khi nhập liệu. Validate cứng = 1.0
> chỉ ở: (1) Bước 7 — Gate chuyển ONGOING; (2) FR-06B — Safety net activate Round.

## Endpoint table

| # | Method | Path | Status |
|---|---|---|---|
| 1 | POST | `/api/v1/rounds/{roundId}/criteria` | 201 |
| 2 | POST | `/api/v1/rounds/{roundId}/criteria/batch` | 201 |
| 3 | GET | `/api/v1/rounds/{roundId}/criteria` | 200 |
| 4 | GET | `/api/v1/rounds/{roundId}/criteria/weight-summary` | 200 |
| 5 | GET | `/api/v1/criteria/{id}` | 200 |
| 6 | PUT | `/api/v1/criteria/{id}` | 200 |
| 7 | DELETE | `/api/v1/criteria/{id}` | 200 |
| 8 | POST | `/api/v1/rounds/{roundId}/criteria/clone` | 201 |

---

## 1. POST `/api/v1/rounds/{roundId}/criteria` — tạo 1 Criterion

### Request
```json
{
  "name": "Code quality",
  "type": "TECHNICAL",
  "weight": 0.30,
  "maxScore": 10,
  "description": "...",
  "rubricUrl": "https://...",
  "displayOrder": 1
}
```

### Validation
| Field | Rule | Error |
|---|---|---|
| `name` | NotBlank, max 200 | 400 |
| `type` | NotNull, enum TECHNICAL/SOFT_SKILL/PENALTY | 400 |
| `weight` | NotNull, 0 < weight ≤ 1 | 400 |
| `maxScore` | NotNull, ≥ 1 | 400 |

### Response 201 — kèm `warnings` nếu tổng weight ≠ 1.0
```json
{
  "success": true,
  "data": { "id": 17, "roundId": 11, "name": "Code quality", "type": "TECHNICAL", "weight": 0.30, ... },
  "message": "Created",
  "warnings": [
    {
      "code": "WEIGHT_NOT_ONE",
      "message": "Tổng weight hiện tại 0.85, cần thêm 0.15 để đủ 1.0",
      "details": { "currentTotal": 0.85, "missing": 0.15 }
    }
  ]
}
```

### Audit
- `CRITERIA_CREATE` — detail snapshot.

---

## 2. POST `/api/v1/rounds/{roundId}/criteria/batch` — tạo nhiều cùng lúc

### Request
```json
{
  "items": [
    { "name": "Code quality",   "type": "TECHNICAL",  "weight": 0.30, "maxScore": 10, "displayOrder": 1 },
    { "name": "Demo polish",    "type": "TECHNICAL",  "weight": 0.20, "maxScore": 10, "displayOrder": 2 },
    { "name": "Pitch quality",  "type": "SOFT_SKILL", "weight": 0.30, "maxScore": 10, "displayOrder": 3 },
    { "name": "Innovation",     "type": "SOFT_SKILL", "weight": 0.20, "maxScore": 10, "displayOrder": 4 }
  ]
}
```

### Constraint
- All-or-nothing trong 1 transaction.
- Sau khi insert: re-compute tổng weight; nếu ≠ 1.0 → vẫn 201 + warning (KHÔNG rollback).

### Response 201
```json
{
  "success": true,
  "data": { "createdIds": [17,18,19,20], "weightSummary": { "total": 1.0, "missing": 0.0, "status": "OK" } },
  "warnings": null
}
```

---

## 3. GET `/api/v1/rounds/{roundId}/criteria` — list

```json
{
  "success": true,
  "data": {
    "items": [ { ... CriterionResponse ... } ],
    "weightSummary": { "total": 1.0, "missing": 0.0, "status": "OK", "warning": null }
  }
}
```

---

## 4. GET `/api/v1/rounds/{roundId}/criteria/weight-summary` — endpoint riêng cho UI realtime

```json
{
  "success": true,
  "data": {
    "roundId": 11,
    "total":    0.85,
    "missing":  0.15,
    "status":   "WARN",            // OK | WARN
    "items": [
      { "criterionId": 17, "weight": 0.30 },
      { "criterionId": 18, "weight": 0.20 },
      { "criterionId": 19, "weight": 0.35 }
    ]
  }
}
```

### Logic
- Bao gồm cả Criteria type ≠ PENALTY (PENALTY không tính vào tổng).
- `status = OK` nếu `ABS(total - 1.0) <= 0.001`, ngược lại `WARN`.

> **KHÔNG ném exception** — endpoint này luôn 200 dù sum lệch. Mục đích là cho UI poll/render.

---

## 5. GET `/api/v1/criteria/{id}` — chi tiết

---

## 6. PUT `/api/v1/criteria/{id}` — sửa

### Constraint
| Điều kiện | Error |
|---|---|
| Tồn tại scores với `criterion_id = id` | 409 `CRITERIA_HAS_SCORES` (block — không cho sửa weight/type vì làm sai điểm đã chấm) |

> Bản clone (có `source_criteria_id`) sửa độc lập, không ảnh hưởng bản gốc.

### Response 200 — kèm warnings nếu tổng weight ≠ 1.0

### Audit
- `CRITERIA_UPDATE` — detail `{ before, after }`

---

## 7. DELETE `/api/v1/criteria/{id}`

### Constraint
- 409 `CRITERIA_HAS_SCORES` nếu có scores reference.

### Audit
- `CRITERIA_DELETE` — snapshot

---

## 8. POST `/api/v1/rounds/{roundId}/criteria/clone` — kế thừa từ Round nguồn

### Request
```json
{
  "sourceRoundId": 5,
  "replaceExisting": false        // optional; nếu true → xóa hết Criteria hiện tại trước khi clone
}
```

### Constraint
- `sourceRoundId` ≠ `roundId`.
- 404 nếu source không tồn tại.
- 422 `CRITERIA_CLONE_SOURCE_EMPTY` nếu source không có Criteria.
- Nếu `replaceExisting = true` và `roundId` đang có scores → 409 `CRITERIA_HAS_SCORES`.

### Logic
```
sources = criteriaRepo.findByRoundIdOrderByDisplayOrder(sourceRoundId)
if sources.isEmpty(): throw 422 CRITERIA_CLONE_SOURCE_EMPTY

if replaceExisting:
    # Block nếu có scores
    if criteriaScoresExist(roundId): throw 409
    criteriaRepo.deleteByRoundId(roundId)

for src in sources:
    INSERT criteria( round_id=roundId, source_criteria_id=src.id,
                     name, type, weight, max_score, description, rubric_url, display_order )

audit.log(CRITERIA_CLONE, "criteria", roundId,
          {"sourceRoundId": ..., "count": ..., "replaceExisting": ...})
```

### Response 201 — kèm warnings tổng weight
```json
{
  "success": true,
  "data": { "createdIds": [...], "sourceRoundId": 5, "count": 4 },
  "warnings": [
    { "code": "WEIGHT_NOT_ONE", "message": "...", "details": { "currentTotal": 1.0, "missing": 0.0 } }
  ]
}
```

### Audit
- `CRITERIA_CLONE` + chi tiết source/count

---

## Mapping bảng DB

| Field DB | DTO | Note |
|---|---|---|
| `criteria.source_criteria_id` | tự động set khi clone | self-ref FK |
| `criteria.type=PENALTY` | KHÔNG tính vào weight sum | dùng cho tiebreak FR-15 (out-of-scope MF-01) |

## Bảng liên quan
- `rounds` (parent)
- `scores` (guard sửa/xóa)
- `criteria` (self-ref clone)
- `audit_logs`

## Test cases
1. Tạo criterion đầu tiên weight 0.3 → 201 + warning `WEIGHT_NOT_ONE` (currentTotal 0.3).
2. Tạo đủ 4 criterion tổng = 1.0 → 201, KHÔNG warning.
3. PUT criterion khi đã có scores → 409 `CRITERIA_HAS_SCORES`.
4. Clone từ source Round không có criteria → 422 `CRITERIA_CLONE_SOURCE_EMPTY`.
5. Clone với `replaceExisting=true` khi đích có scores → 409.
6. GET weight-summary luôn trả 200 (kể cả tổng = 0).
