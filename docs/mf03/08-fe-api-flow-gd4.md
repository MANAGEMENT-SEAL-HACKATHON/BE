# MF-03 — Luồng API GĐ4 (Chuyển vòng → Chung kết) cho FE

**Tiền đề:** Hoàn thành GĐ3 Sơ loại — lock scoring, xem ranking. Xem [07-fe-api-flow-gd3.md](07-fe-api-flow-gd3.md).

**Base:** `/api/v1` · **Auth:** `Bearer` Coordinator (trừ ghi rõ).

---

## Sơ đồ tổng quan GĐ4 (phase 1)

```
Lock Sơ loại (GĐ3)
  → GET ranking
  → PATCH publish (FR-24)
  → POST advance (FR-30)
  → POST judge-assignments trên round FINAL (FR-27)
  → PATCH activate round FINAL (FR-25)
```

Tiebreak, wild card, scoreboard public — **phase 2** (stub).

---

## Bước 1 — Công bố kết quả Sơ loại (FR-24)

**Khi:** Sau `PATCH .../lock-scoring`; trước advance.

```http
PATCH /api/v1/rounds/{prelimRoundId}/publish
Authorization: Bearer {coordinatorToken}
```

**Gate BE:**

- Round **không** phải FINAL (`isFinal=false`)
- `scoringLocked=true`
- Chưa `isPublished`

**Response:** `RoundSummaryResponse` với `isPublished=true`, `publishedAt`, `publishedBy`.

**Lỗi:** `ROUND_NOT_SCORING_LOCKED` · `INVALID_STATE` (đã publish / round FINAL)

---

## Bước 2 — Chốt danh sách thăng vòng (FR-30)

**Khi:** Sau publish; Coordinator đã quyết định top N (tiebreak/wildcard phase 2 tùy chọn).

```http
POST /api/v1/rounds/{prelimRoundId}/advance
Authorization: Bearer {coordinatorToken}
Content-Type: application/json

{
  "advancedTeamIds": [101, 102, 103],
  "eliminatedTeamIds": [104, 105],
  "note": "optional"
}
```

**Gate BE:**

- Round Sơ loại
- `scoringLocked=true` + `isPublished=true`
- Hackathon có round FINAL

**Hành vi:**

- `team_round_tracks.participation_status` → `ADVANCED` / `ELIMINATED`
- Upsert `team_round_participation` cho round Chung kết (idempotent)

**Lỗi:** `RESULT_NOT_PUBLISHED` · `TEAM_NOT_IN_ROUND` · overlap advance/eliminate

---

## Bước 3 — Phân Judge Chung kết (FR-27)

**Path `{id}` = round Chung kết** (`isFinal=true`), không phải round Sơ loại.

```http
POST /api/v1/rounds/{finalRoundId}/judge-assignments
Authorization: Bearer {coordinatorToken}
Content-Type: application/json

{
  "judgeIds": [201, 202, 203]
}
```

### Panel Judge CK

| Loại | Cách tạo / điều kiện |
|------|----------------------|
| Judge khách | `POST /users/temp-judges` (GĐ1/GĐ4) — `EXTERNAL`, thường temp account |
| Trưởng ban | `PATCH /users/{id}` set `is_dept_head=true` — không được mentor kỳ này (DB trigger) |

`FINAL_EXTERNAL` = **assignment_type** panel CK — không bắt buộc user EXTERNAL.

**Warnings (không block 2xx):**

- `JUDGE_PARTICIPATED_IN_PRELIM` — judge đã chấm Sơ loại
- `MIN_FINAL_JUDGES_NOT_MET` — panel &lt; 3 (khuyến nghị)

**GĐ1:** `POST /judge-assignments` với `roundId` FINAL vẫn trả `JUDGE_FINAL_AT_PHASE1` — dùng endpoint GĐ4 trên.

### Playbook thiếu judge khách

1. Tạo/resend temp judges ([mf02/02-invitations.md](../mf02/02-invitations.md))
2. Thêm trưởng ban (`is_dept_head`) nếu hợp lệ
3. Không activate CK cho đến khi có judge (`JUDGE_NOT_ASSIGNED` on activate)

---

## Bước 4 — Kích hoạt round Chung kết (FR-25)

```http
PATCH /api/v1/rounds/{finalRoundId}/activate
Authorization: Bearer {coordinatorToken}
Content-Type: application/json

{ "note": "optional" }
```

**Gate BE (đã có):**

- Mọi round Sơ loại đã `isPublished=true` — nếu không: `RESULT_NOT_PUBLISHED`
- Criteria + weight round FINAL
- ≥1 judge `FINAL_EXTERNAL` trên round FINAL — nếu không: `JUDGE_NOT_ASSIGNED`

---

## Happy path — checklist QA GĐ3→GĐ4

| # | Bước | API | Kỳ vọng |
|---|------|-----|---------|
| 1 | Lock Sơ loại | `PATCH /rounds/{prelimId}/lock-scoring` | 200 |
| 2 | Ranking chính thức | `GET /rounds/{prelimId}/ranking` | 200 |
| 3 | Publish | `PATCH /rounds/{prelimId}/publish` | `isPublished=true` |
| 4 | Advance | `POST /rounds/{prelimId}/advance` | participation updated |
| 5 | Assign judges CK | `POST /rounds/{finalId}/judge-assignments` | judgeIds assigned |
| 6 | Activate CK | `PATCH /rounds/{finalId}/activate` | round active |
| 7 | Activate CK sớm (negative) | Bỏ bước 3 | `RESULT_NOT_PUBLISHED` |

---

## Liên kết

- Business rules: [01-business-rules-gd3.md](01-business-rules-gd3.md) §12–13
- API reference: [03-api-reference-gd3.md](03-api-reference-gd3.md)
- GĐ3 flow: [07-fe-api-flow-gd3.md](07-fe-api-flow-gd3.md)
