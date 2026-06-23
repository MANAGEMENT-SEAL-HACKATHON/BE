# E2E GĐ6 — Seed data & Postman variables

> **Ma trận test đầy đủ:** [gd6-full-test-matrix-and-seeds.md](gd6-full-test-matrix-and-seeds.md)  
> **GĐ5 handoff:** [gd5-full-test-matrix-and-seeds.md](gd5-full-test-matrix-and-seeds.md)

Profile **`dev`**. Sau mỗi lần start app, xem log các seeder `Gd6*` để lấy **ID thực tế**.

| Account | Email | Password |
|---------|-------|----------|
| Coordinator | `coord@fpt.edu.vn` | `Coordinator@dev1` |
| Student | xem bảng slug bên dưới | `Student@dev1` |

---

## Tất cả slug seed GĐ6

| Slug | Mục đích |
|------|----------|
| `seal-gd6-pending-confirm` | Happy path — FIRST prize, thêm SECOND, confirm |
| `seal-gd6-prizes-empty` | 0 prize — `NO_PRIZES_RECORDED` |
| `seal-gd6-confirm-ready` | 3 giải — confirm một lần |
| `seal-gd6-finished-export` | `FINISHED` — export + rankings |
| `seal-gd6-edge-errors` | CK chưa lock — `ROUND_NOT_SCORING_LOCKED` |

---

## Profile 0 — `seal-gd6-pending-confirm`

| Thành phần | Giá trị seed |
|------------|--------------|
| Hackathon | **`PENDING_CONFIRM`** |
| CK | active + **scoring locked** |
| Teams | 3× `GD6-0x ADVANCED CK` |
| Prizes | **FIRST** team 01 |

**Students:** `student.gd6.leader01@fpt.edu.vn` … `leader03@`

### Lấy ID

```http
GET /api/v1/hackathons?q=seal-gd6-pending-confirm
GET /api/v1/hackathons/{{hackathonId}}/rounds
```

### Postman variables

| Variable | Mô tả |
|----------|--------|
| `gd6HackathonSlug` | `seal-gd6-pending-confirm` |
| `hackathonId` | từ GET hackathon |
| `finalRoundId` | round `isFinal=true`, `scoringLocked=true` |
| `teamId` (t2) | team `GD6-02` — test POST prize SECOND |

### Luồng API

1. `GET /readiness?target=AWARDS`
2. `GET /team-rankings`
3. `POST /prizes` — team 02, `prizeRank: "SECOND"`
4. `PATCH /confirm` `{ "confirm": true }`
5. `POST /export-jobs` — chỉ sau FINISHED (dùng `seal-gd6-finished-export`)

**Reset:** restart BE — `repairForFullChainRetest` đưa slug Profile 0 về `PENDING_CONFIRM` nếu đã confirm.

---

## Profile A — `seal-gd6-prizes-empty`

**Students:** `student.gd6p.leader01@` … `leader03@`

Test: `PATCH /confirm` → `NO_PRIZES_RECORDED` → `POST /prizes` → confirm.

---

## Profile B — `seal-gd6-confirm-ready`

**Students:** `student.gd6r.leader01@` … `leader03@`

3 giải đã seed — `PATCH /confirm` trực tiếp.

---

## Profile C — `seal-gd6-finished-export`

**Students:** `student.gd6f.leader01@` … `leader03@`

```text
POST /export-jobs { "type": "CSV_RANKINGS" }
GET /chapter-rankings
GET /individual-rankings
```

---

## Profile D — `seal-gd6-edge-errors`

**Students:** `student.gd6e.leader01@` … `leader03@`

`PATCH /confirm` → `ROUND_NOT_SCORING_LOCKED` (CK `scoring_locked=false`).

---

## SQL verify nhanh

```sql
SELECT id, slug, status FROM hackathons WHERE slug LIKE 'seal-gd6-%';

SELECT t.team_name, p.prize_rank
FROM prizes p
JOIN teams t ON t.id = p.team_id
JOIN hackathons h ON h.id = p.hackathon_id
WHERE h.slug = 'seal-gd6-pending-confirm';
```
