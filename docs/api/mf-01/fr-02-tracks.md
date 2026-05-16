# FR-02 — Quản lý Track (hạng mục thi đấu)

> Workflow v3.1 ref: GĐ1 — Bước 2 | DB v2.1 ref: `tracks` (FIX-02 `max_teams_per_group`)

## Endpoint table

| # | Method | Path | Status |
|---|---|---|---|
| 1 | POST | `/api/v1/hackathons/{hackathonId}/tracks` | 201 |
| 2 | GET | `/api/v1/hackathons/{hackathonId}/tracks` | 200 |
| 3 | GET | `/api/v1/tracks/{id}` | 200 |
| 4 | PUT | `/api/v1/tracks/{id}` | 200 |
| 5 | DELETE | `/api/v1/tracks/{id}` | 200 |

---

## 1. POST `/api/v1/hackathons/{hackathonId}/tracks`

### Request
```json
{
  "name": "Track A — AI for Education",
  "description": "...",
  "maxTeams": 24,
  "maxTeamsPerGroup": 8,
  "minTeamSize": 3,
  "maxTeamSize": 5
}
```

### Validation
| Field | Rule | Error |
|---|---|---|
| `name` | NotBlank, max 200 | 400 |
| `maxTeams` | nullable, ≥ 1 | 400 |
| `maxTeamsPerGroup` | nullable, ≥ 1, ≤ `maxTeams` nếu cả hai có | 400 / 422 `TRACK_INVALID_GROUP_CAP` |
| `minTeamSize` | NotNull, ≥ 1 | 400 |
| `maxTeamSize` | NotNull, ≥ `minTeamSize` | 422 `TRACK_INVALID_TEAM_SIZE` |
| Hackathon | phải `status IN (DRAFT, ONGOING)` | 409 `TRACK_HACKATHON_LOCKED` |

### Response 201 — `TrackResponse`

### Audit
- `TRACK_CREATE` + snapshot

---

## 2. GET `/api/v1/hackathons/{hackathonId}/tracks`

List tất cả Track thuộc Hackathon. Có filter optional `status` (OPEN/CLOSED/CANCELLED).

### Response 200 — `List<TrackSummaryResponse>` (không paging vì số lượng nhỏ).

---

## 3. GET `/api/v1/tracks/{id}` — chi tiết
404 nếu không tồn tại.

---

## 4. PUT `/api/v1/tracks/{id}`

Cùng schema POST. Bổ sung field `status` (OPEN/CLOSED/CANCELLED). Không cho đổi `hackathonId`.

### Constraint thêm
- Nếu chuyển status → CANCELLED: cảnh báo mềm `warnings:[{code:"TRACK_CANCELLED_HAS_TEAMS"}]` nếu vẫn còn team registered (nhưng KHÔNG block — Coordinator chủ động).
- Hackathon phải DRAFT hoặc ONGOING.

---

## 5. DELETE `/api/v1/tracks/{id}`

### Guard
| Điều kiện | Error |
|---|---|
| Còn team với `registration_track_id = id` AND `status IN (ACTIVE, PENDING)` | 409 `TRACK_HAS_TEAMS` |
| Còn Round với `is_active = TRUE` | 409 `TRACK_HAS_ACTIVE_ROUND` |
| Hackathon `status = FINISHED` hoặc `PENDING_CONFIRM` | 409 `TRACK_HACKATHON_LOCKED` |

### Side effect
- ON DELETE CASCADE từ DB: `rounds`, `mentor_assignments`, `criteria`, `judge_assignments` đều bị xóa theo.
- Notify Mentor bị hủy phân công qua `notifications`.

### Audit
- `TRACK_DELETE` + snapshot trước khi xóa + danh sách mentor bị notify.

---

## Mapping bảng DB

| Field DB | DTO | Note |
|---|---|---|
| `tracks.status` | enum `TrackStatus` (OPEN/CLOSED/CANCELLED) | mặc định OPEN |
| `tracks.max_teams_per_group` | `maxTeamsPerGroup` | FIX-02 v2.1 |
| `tracks.min_team_size`/`max_team_size` | 3/5 mặc định | DB default |

## Bảng liên quan
- `hackathons` (parent, validate status DRAFT/ONGOING)
- `rounds` (child, guard delete)
- `teams` (guard delete khi PENDING/ACTIVE)
- `mentor_assignments`, `judge_assignments`, `criteria` (cascade delete)
- `audit_logs`

## Test cases
1. Tạo Track hợp lệ → 201.
2. `maxTeamSize < minTeamSize` → 422.
3. `maxTeamsPerGroup > maxTeams` → 422.
4. Hackathon FINISHED → 409 `TRACK_HACKATHON_LOCKED`.
5. Xóa Track còn team ACTIVE → 409 `TRACK_HAS_TEAMS`.
6. Xóa Track còn Round active → 409 `TRACK_HAS_ACTIVE_ROUND`.
