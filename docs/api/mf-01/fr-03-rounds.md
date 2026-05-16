# FR-03 — Tạo Round trong Track

> Workflow v3.1 ref: GĐ1 — Bước 3 | DB v2.1 ref: `rounds` (FIX-03 `min_teams_final`)

> **QUAN TRỌNG v2.2**: Bước 3 KHÔNG validate tổng weight Criteria vì Criteria chưa tồn tại
> tại thời điểm tạo Round. Validate weight chỉ có ở:
> - Bước 4 — cảnh báo mềm realtime (FR-04 GET `/weight-summary`, POST/PUT criteria)
> - Bước 7 — Gate cứng khi chuyển ONGOING (FR-06 PATCH `/status`, GET `/readiness`)
> - Activate Round — FR-06B `PATCH /rounds/{id}/activate`

## Endpoint table (CRUD)

| # | Method | Path | Status |
|---|---|---|---|
| 1 | POST | `/api/v1/tracks/{trackId}/rounds` | 201 |
| 2 | GET | `/api/v1/tracks/{trackId}/rounds` | 200 |
| 3 | GET | `/api/v1/rounds/{id}` | 200 |
| 4 | PUT | `/api/v1/rounds/{id}` | 200 |
| 5 | DELETE | `/api/v1/rounds/{id}` | 200 |

> Endpoint **`PATCH /api/v1/rounds/{id}/activate`** — xem [fr-06b-activate.md](./fr-06b-activate.md).

---

## 1. POST `/api/v1/tracks/{trackId}/rounds`

### Request
```json
{
  "name": "Sơ loại",
  "sequenceOrder": 1,
  "submissionOpen":     "2026-03-05T08:00:00Z",
  "submissionDeadline": "2026-03-10T17:00:00Z",
  "codingDurationHours": 120,
  "problemStatementUrl": "https://...",
  "topNAdvance": 16,
  "wildcardEnabled": false,
  "minTeamsFinal": null,
  "tiebreakRule": "PENALTY_SCORE"
}
```

### Validation
| Field | Rule | Error |
|---|---|---|
| `name` | NotBlank, max 100 | 400 |
| `sequenceOrder` | NotNull, ≥ 1 | 400 |
| `submissionDeadline` | NotNull | 400 |
| `submissionDeadline` | > `submissionOpen` nếu có; > NOW() khi tạo | 422 `ROUND_DEADLINE_INVALID` |
| `tiebreakRule` | enum PENALTY_SCORE/SUBMISSION_TIME/COORDINATOR_DECISION | 400 |
| `topNAdvance` | bắt buộc NOT NULL ở Round không phải cuối; NULL ở Round Chung kết — warn nếu sai (không block) | warning |
| `forceLocked=true` + `forceLockReason` rỗng | — | 422 `ROUND_FORCE_LOCK_REASON` |
| Track | phải tồn tại (404) | — |

> **KHÔNG validate weight Criteria** ở endpoint này — Criteria chưa tồn tại.

### Audit
- `ROUND_CREATE`

---

## 2. GET `/api/v1/tracks/{trackId}/rounds`
List theo Track, sort `sequenceOrder ASC`. Response field thêm `criteriaCount`, `currentWeightTotal` để UI hiển thị nhanh.

```json
{
  "success": true,
  "data": [
    {
      "id": 1, "name": "Sơ loại", "sequenceOrder": 1,
      "submissionDeadline": "...",
      "isActive": false, "scoringLocked": false,
      "criteriaCount": 4, "currentWeightTotal": 1.0
    }
  ]
}
```

---

## 3. GET `/api/v1/rounds/{id}` — chi tiết

---

## 4. PUT `/api/v1/rounds/{id}`

### Constraint thêm
- Không cho phép sửa `trackId` (không có field này trong request).
- Sửa `submissionDeadline` khi đã có submissions → cho phép nhưng audit detail rõ ràng.
- Sửa `forceLocked = true` → required `forceLockReason`. Audit action `ROUND_FORCE_LOCK`.
- Sửa `scoringLocked = true` → audit `ROUND_LOCK`. Tránh để Coordinator unlock ở MF-01 (về sau ở GĐ scoring).

---

## 5. DELETE `/api/v1/rounds/{id}`

### Guard
| Điều kiện | Error |
|---|---|
| Tồn tại submission `round_id=id` | 409 `ROUND_HAS_SUBMISSIONS` |
| `is_active = TRUE` | 409 `ROUND_ANOTHER_ACTIVE` (yêu cầu deactivate trước) |

### Side effect cascade
- DB CASCADE: `criteria`, `judge_assignments`. Cảnh báo Judge bị hủy phân công qua `notifications`.

### Audit
- `ROUND_DELETE` snapshot

---

## Mapping bảng DB

| Field | DTO | Note |
|---|---|---|
| `rounds.is_active` | KHÔNG nhận trong POST/PUT — chỉ qua PATCH `/activate` (FR-06B) | — |
| `rounds.scoring_locked` | nhận trong PUT (để Coordinator lock thủ công) | mặc định FALSE |
| `rounds.force_locked` | nhận trong PUT, bắt buộc reason | — |
| `rounds.min_teams_final` | FIX-03 — số đội tối thiểu vào vòng tiếp | — |

## Bảng liên quan
- `tracks` (parent)
- `criteria` (child — tạo riêng ở Bước 4, sau Bước 3)
- `judge_assignments` (cascade)
- `submissions` (guard delete)
- `hackathons` (grandparent — AND wildcard logic)
- `audit_logs`

## Test cases
1. Tạo Round không có Criteria → 201 (KHÔNG block).
2. `submissionDeadline < submissionOpen` → 422 `ROUND_DEADLINE_INVALID`.
3. `forceLocked=true` thiếu `forceLockReason` → 422 `ROUND_FORCE_LOCK_REASON`.
4. Delete Round còn submissions → 409 `ROUND_HAS_SUBMISSIONS`.
5. PUT đổi `scoringLocked=true` → 200 + audit `ROUND_LOCK`.
