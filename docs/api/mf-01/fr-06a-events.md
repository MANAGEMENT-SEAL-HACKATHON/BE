# FR-06A — Lên lịch sự kiện (Workshop, Khai mạc, Trao giải)

> Workflow v3.1 ref: GĐ1 — Bước 6 | DB v2.1 ref: `events`, `notifications`

## Endpoint table

| # | Method | Path |
|---|---|---|
| 1 | POST | `/api/v1/hackathons/{hackathonId}/events` |
| 2 | GET | `/api/v1/hackathons/{hackathonId}/events` |
| 3 | GET | `/api/v1/events/{id}` |
| 4 | PUT | `/api/v1/events/{id}` |
| 5 | DELETE | `/api/v1/events/{id}` |

---

## Validate sự kiện — 3 LỚP (v2.2)

| Lớp | Loại | Quy tắc | Hành động |
|---|---|---|---|
| 1 | Trong khung Hackathon | `starts_at >= hackathon.event_start` AND `ends_at <= hackathon.event_end + 1d buffer` | **BLOCK 422 `EVENT_OUT_OF_HACKATHON`** |
| 2 | Không chồng lấn | 2 event cùng type `KICKOFF`/`AWARDS` overlap | **BLOCK 422 `EVENT_OVERLAP`** |
| 3a | KICKOFF bắt buộc | Hackathon phải có ≥ 1 event type=KICKOFF | **BLOCK 422 `EVENT_KICKOFF_MISSING`** (check ở Gate FR-06, không ở đây) |
| 3b | Thứ tự logic | `WORKSHOP.starts_at <= registration_end`; `KICKOFF.starts_at ∈ [event_start, event_start+1d]` | **WARN mềm** (`EVENT_ORDER_INVALID`) |
| 3c | Thứ tự logic | `PRESENTATION.starts_at > KICKOFF.ends_at`; `AWARDS.starts_at > max(PRESENTATION.starts_at)` | **WARN mềm** |

> Lớp 1+2 = block. Lớp 3 (trừ KICKOFF bắt buộc) = warning. KICKOFF bắt buộc được check ở **FR-06** Gate ONGOING — KHÔNG validate ở POST event lẻ.

---

## 1. POST `/api/v1/hackathons/{hackathonId}/events`

### Request
```json
{
  "title": "Khai mạc SEAL Spring 2026",
  "type":  "KICKOFF",
  "description": "...",
  "location": "Hội trường FPT HCMC",
  "meetUrl":  null,
  "startsAt": "2026-03-01T08:00:00Z",
  "endsAt":   "2026-03-01T10:00:00Z",
  "isPublic": true
}
```

### Validation Bean
| Field | Rule |
|---|---|
| `title` | NotBlank, max 300 |
| `type` | NotNull enum |
| `startsAt` | NotNull |
| `endsAt` | nullable; nếu có → ≥ `startsAt` (422 `EVENT_END_BEFORE_START`) |

### Validation 3 lớp (App-layer)
- Lớp 1: 422 `EVENT_OUT_OF_HACKATHON` kèm details `{ eventStart, eventEnd }`.
- Lớp 2: 422 `EVENT_OVERLAP` kèm conflict event id.
- Lớp 3: warning trả trong response 201.

### Logic
```
@Transactional
create(hackathonId, req):
  h = hackathonRepo.findById(hackathonId) or 404
  ensureInRange(req.startsAt, req.endsAt, h)               # Lớp 1
  ensureNoOverlap(h.id, req.type, req.startsAt, req.endsAt) # Lớp 2
  warnings = checkLogicOrder(h, req)                        # Lớp 3
  saved = eventRepo.save(toEntity(req, h, currentUser))
  audit.log(EVENT_CREATE, "events", saved.id, snapshot)
  for w in warnings: audit.log(WARNING_EVENT_ORDER, ..., w.details)
  enqueueRemindersAsync(saved)                              # FR-06A — REMINDER fan-out
  return Result(mapper.toResponse(saved), warnings)
```

### Side effect — `notifications`
- Sau khi insert event public (`isPublic = true`): async job INSERT `notifications` `type=REMINDER`,
  `reference_type='events'`, `reference_id=event.id`, cho mỗi user APPROVED trong Hackathon.

### Response 201 — kèm `warnings` (nếu có)

### Audit
- `EVENT_CREATE` + snapshot
- `WARNING_EVENT_ORDER` cho mỗi warning Lớp 3

---

## 2. GET `/api/v1/hackathons/{hackathonId}/events`

Query: `type`, `from` (date), `to` (date), `isPublic`.

Response: `List<EventResponse>` sort `startsAt ASC`.

---

## 3. GET `/api/v1/events/{id}` — chi tiết

---

## 4. PUT `/api/v1/events/{id}`

Re-validate 3 lớp như POST. Nếu chỉ thay đổi nội bộ (title/description), vẫn validate lại để đảm bảo nhất quán.

### Audit
- `EVENT_UPDATE` + diff

---

## 5. DELETE `/api/v1/events/{id}`

### Logic
- Delete event. Cảnh báo: nếu xóa event KICKOFF cuối cùng → vẫn cho phép (Coordinator có thể tạo lại), nhưng sau đó Gate FR-06 sẽ block chuyển ONGOING (422 `EVENT_KICKOFF_MISSING`).
- Xóa luôn các `notifications` đang `is_read=false` referencing event này (best-effort).

### Audit
- `EVENT_DELETE`

---

## Bảng liên quan
- `hackathons` (parent)
- `notifications` (REMINDER fan-out)
- `audit_logs`

## Test cases
1. `startsAt < hackathon.event_start` → 422 `EVENT_OUT_OF_HACKATHON`.
2. Tạo 2 event KICKOFF overlap → 422 `EVENT_OVERLAP`.
3. Tạo WORKSHOP với `startsAt > registrationEnd` → 201 + warning `EVENT_ORDER_INVALID`.
4. Tạo AWARDS với `startsAt` trước mọi PRESENTATION → 201 + warning.
5. Tạo event với `endsAt < startsAt` → 422 `EVENT_END_BEFORE_START`.
6. PUT đổi `startsAt` ra ngoài khung → 422.
7. DELETE event KICKOFF cuối cùng → 200; nhưng PATCH `/status` to ONGOING sau đó → 422 `EVENT_KICKOFF_MISSING`.
