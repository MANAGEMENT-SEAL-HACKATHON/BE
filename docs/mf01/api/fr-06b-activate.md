# FR-06B — Activate Round (Safety net validate weight = 1.0)

> Workflow v3.1 ref: GĐ3 — Bước 1 | DB v2.1 ref: `rounds.is_active`, `criteria.weight`

> **FR-06B là Safety net TẦNG 3** — ngăn bypass Gate FR-06 trong trường hợp Coordinator
> thêm/sửa Criteria SAU khi Hackathon đã ONGOING nhưng TRƯỚC khi activate Round.

## Endpoint

| # | Method | Path |
|---|---|---|
| 1 | PATCH | `/api/v1/rounds/{id}/activate` |

---

## 1. PATCH `/api/v1/rounds/{id}/activate`

### Request
```json
{ "note": "Mở chấm Round Sơ loại" }
```

Hoặc body rỗng `{}`.

### Logic xử lý (atomic)
```
@Transactional
activate(roundId, note):
  round = repo.findById(roundId) or 404
  total = criteriaRepo.sumWeightByRoundIdExcludingPenalty(roundId)   # SQL: SUM(weight) WHERE round_id=:id AND type<>'PENALTY'
  if total IS NULL:
      throw BusinessRuleException(ROUND_NO_CRITERIA,
            "Round chưa có tiêu chí chấm điểm")
  if ABS(total - 1.0) > 0.001:
      throw BusinessRuleException(ROUND_WEIGHT_NOT_ONE,
            "Tổng trọng số = {total}, cần chỉnh về 1.0",
            details = {"currentTotal": total, "missing": 1.0 - total})
  # auto deactivate các Round khác cùng track
  roundRepo.deactivateOtherRoundsInTrack(round.track.id, roundId)
  round.isActive = TRUE
  repo.save(round)
  audit.log(ROUND_ACTIVATE, "rounds", roundId,
            {"trackId": round.track.id, "note": note, "weightTotal": total})
  return mapper.toResponse(round)
```

### Response 200
```json
{
  "success": true,
  "data": {
    "id": 11, "name": "Sơ loại", "isActive": true,
    "scoringLocked": false, "sequenceOrder": 1,
    "trackId": 4
  },
  "message": "Round activated"
}
```

### Errors
| Status | Code | Trigger |
|---|---|---|
| 404 | `RESOURCE_NOT_FOUND` | Round id sai |
| 422 | `ROUND_NO_CRITERIA` | Round chưa có Criteria nào (type ≠ PENALTY) |
| 422 | `ROUND_WEIGHT_NOT_ONE` | ABS(sum - 1.0) > 0.001 |

### Audit
- `ROUND_ACTIVATE` cho Round vừa activate
- `ROUND_DEACTIVATE` cho mỗi Round bị tắt cờ trong cùng Track (nếu có)

### Bảng liên quan
- `criteria` — nguồn validate weight (`SUM(weight) WHERE round_id=:id AND type<>'PENALTY'`)
- `rounds` — UPDATE is_active=TRUE; UPDATE is_active=FALSE cho mọi round khác cùng track_id
- `audit_logs`

## Test cases
1. Round chưa có Criteria → 422 `ROUND_NO_CRITERIA`.
2. Round có Criteria nhưng tổng weight = 0.85 → 422 `ROUND_WEIGHT_NOT_ONE` (details có currentTotal 0.85, missing 0.15).
3. Round có Criteria + 1 PENALTY (weight 0.1) — tổng các NORMAL = 1.0 → 200 (PENALTY không tính).
4. Activate Round 2 trong khi Round 1 đang is_active → Round 1 tự deactivate; Round 2 active; cả hai audit.
5. Activate Round đã active → 200 (idempotent OK; vẫn audit để tracking).
