# MF-01 — API Conventions

## 1. Base path

```
/api/v1
```

## 2. Response envelope — 2xx

```json
{
  "success": true,
  "data": { ... } | [ ... ],
  "message": "Optional human-readable message",
  "warnings": [
    { "code": "WEIGHT_NOT_ONE", "message": "Tổng weight = 0.85, cần thêm 0.15", "details": { "currentTotal": 0.85, "missing": 0.15 } }
  ],
  "traceId": "uuid-v4",
  "timestamp": "2026-05-16T09:57:00Z"
}
```

- `data` luôn có (có thể là object, array, hoặc null).
- `warnings` chỉ xuất hiện khi có cảnh báo mềm. Client (UI) hiển thị toast vàng, không chặn flow.
- `message` tuỳ chọn.
- `traceId` sinh server-side mỗi request — đối chiếu log.

## 3. Response envelope — 4xx / 5xx

### 3.1 Business / not-found / conflict

```json
{
  "success": false,
  "error": {
    "code":    "HACKATHON_DUPLICATE",
    "message": "Kỳ thi đã tồn tại",
    "status":  409,
    "details": { "name": "SEAL Spring 2026", "season": "Spring", "year": 2026 }
  },
  "traceId":   "uuid-v4",
  "timestamp": "2026-05-16T09:57:00Z"
}
```

### 3.2 Bean Validation 400

```json
{
  "success": false,
  "error": {
    "code":    "VALIDATION_FAILED",
    "message": "Yêu cầu không hợp lệ",
    "status":  400,
    "fields": [
      { "field": "name", "message": "must not be blank", "rejectedValue": null },
      { "field": "year", "message": "must be >= 2024", "rejectedValue": 2020 }
    ]
  }
}
```

## 4. HTTP status mapping

| Status | Khi nào | Exception |
|---|---|---|
| 200 OK | GET / PUT / PATCH / DELETE thành công | — |
| 201 Created | POST tạo mới (kèm header `Location`) | — |
| 400 Bad Request | Bean Validation, body malformed, type mismatch | `MethodArgumentNotValidException`, `HttpMessageNotReadableException` |
| 403 Forbidden | Role không phải COORDINATOR, status ≠ APPROVED | (do module Auth xử lý) |
| 404 Not Found | Resource id không tồn tại | `ResourceNotFoundException` |
| 409 Conflict | UNIQUE violation, child resource đang dùng, state transition sai | `ConflictException`, `DataIntegrityViolationException` |
| 422 Unprocessable Entity | Business rule fail (date range sai, weight ≠ 1.0, thiếu KICKOFF...) | `BusinessRuleException` |
| 500 Internal Server Error | Unhandled exception | `Exception` |

## 5. Error codes (`ErrorCode.java`)

Xem [common/exception/ErrorCode.java](../../../src/main/java/com/se194093/be/common/exception/ErrorCode.java) — tập hợp đầy đủ. Trích yếu:

| Code | Status | Mô tả |
|---|---|---|
| `VALIDATION_FAILED` | 400 | Bean Validation |
| `RESOURCE_NOT_FOUND` | 404 | Resource không tồn tại |
| `HACKATHON_DUPLICATE` | 409 | UNIQUE(name, season, year) |
| `HACKATHON_DATE_RANGE` | 422 | Date range sai logic |
| `HACKATHON_NOT_DRAFT` | 409 | Hành động chỉ cho phép ở DRAFT |
| `HACKATHON_HAS_CHILDREN` | 409 | Hackathon còn Track/Round/Event khi xóa |
| `TRACK_HAS_TEAMS` | 409 | Xóa Track còn team ACTIVE/PENDING |
| `TRACK_HAS_ACTIVE_ROUND` | 409 | Xóa Track còn Round is_active |
| `ROUND_DEADLINE_INVALID` | 422 | submission_deadline < submission_open hoặc < NOW |
| `ROUND_FORCE_LOCK_REASON` | 422 | force_locked=TRUE thiếu reason |
| `ROUND_HAS_SUBMISSIONS` | 409 | Xóa Round còn submission |
| `ROUND_NO_CRITERIA` | 422 | Activate Round chưa có Criteria |
| `ROUND_WEIGHT_NOT_ONE` | 422 | Activate Round / Gate ONGOING có tổng weight ≠ 1.0 |
| `CRITERIA_HAS_SCORES` | 409 | Sửa/xóa Criteria đã có scores |
| `USER_INVALID_ROLE` | 422 | Mentor/Judge sai role |
| `USER_NOT_APPROVED` | 422 | Mentor/Judge chưa APPROVED |
| `USER_EMAIL_TAKEN` | 409 | Tạo Judge tạm với email đã tồn tại |
| `MENTOR_ASSIGN_DUPLICATE` | 409 | UNIQUE(mentor_id, track_id) |
| `JUDGE_ASSIGN_DUPLICATE` | 409 | UNIQUE(judge_id, round_id) |
| `STATUS_TRANSITION_INVALID` | 409 | Transition sai chiều |
| `READINESS_NOT_PASSED` | 422 | Gate cứng fail khi chuyển ONGOING |
| `EVENT_OUT_OF_HACKATHON` | 422 | Lớp 1 — ngoài khung event_start/event_end |
| `EVENT_OVERLAP` | 422 | Lớp 2 — chồng giờ với KICKOFF/AWARDS |
| `EVENT_KICKOFF_MISSING` | 422 | Gate FR-06 — thiếu KICKOFF |

## 6. Warning codes (mềm — gắn trong field `warnings`)

| Code | FR | Mô tả |
|---|---|---|
| `WEIGHT_NOT_ONE` | FR-04, FR-06B | Tổng weight Criteria ≠ 1.0 (kèm `currentTotal`, `missing`) |
| `MENTOR_JUDGE_CONFLICT` | FR-05b, FR-05c | User đang là Mentor của Track / Judge của Round trong cùng Track |
| `CONFLICT_CHECK_SKIPPED` | FR-05b, FR-05c | Bảng đối chiếu rỗng (vd phân công Mentor trước khi có Judge nào) |
| `JUDGE_FINAL_ROUND_AT_PHASE1` | FR-05c | Phân công Judge cho Round Chung kết ở GĐ1 (khuyến nghị làm ở GĐ5) |
| `EVENT_ORDER_INVALID` | FR-06A | Lớp 3 — thứ tự event sai (vd AWARDS trước PRESENTATION) |
| `READINESS_WARNING` | FR-06 | Cảnh báo mềm tổng hợp khi check readiness |

## 7. Audit action codes

Xem [common/audit/AuditAction.java](../../../src/main/java/com/se194093/be/common/audit/AuditAction.java).

Mỗi mutation gọi:
```java
auditService.log(AuditAction.HACKATHON_CREATE, "hackathons", hackathon.getId(), detailMap);
```

## 8. Paging

Query params chuẩn cho list endpoint:
- `page` (int, default `0`, 0-based)
- `size` (int, default `20`, max `100`)
- `sort` (string, default theo nghiệp vụ; format `field,direction` vd `createdAt,desc`)

Response:
```json
{
  "success": true,
  "data": {
    "items": [ ... ],
    "page": 0,
    "size": 20,
    "totalElements": 124,
    "totalPages": 7
  }
}
```

## 9. Header convention

| Header | Mục đích |
|---|---|
| `Authorization: Bearer <jwt>` | (module Auth) |
| `X-Request-Id` | Optional — nếu client gửi, server echo lại trong response `traceId` |
| `Accept-Language` | Optional — i18n message (chưa implement ở MF-01) |
| `Location` | Server set khi 201 Created — URL chi tiết resource vừa tạo |

## 10. Timezone & date format

- Tất cả timestamp request/response dùng ISO-8601 với offset `Z` (UTC).
- Date (no time) dùng `YYYY-MM-DD`.
- Server lưu UTC; client tự convert sang local.

## 11. Idempotency

MF-01 chưa yêu cầu idempotency token. POST tạo trùng → 409 dựa trên UNIQUE constraint.
