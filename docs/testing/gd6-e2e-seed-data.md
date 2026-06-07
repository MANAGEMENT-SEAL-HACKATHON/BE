# E2E GĐ6 — Seed data & Postman variables

Profile **`dev`**. Sau mỗi lần **restart app**, tìm log **`[Gd6PendingConfirmDataSeeder]`** để lấy ID thực tế.

| Account | Email | Password |
|---------|-------|----------|
| Coordinator | `coord@fpt.edu.vn` | `Coordinator@dev1` |
| Student | `student.gd6.leader01@fpt.edu.vn` … `leader03@` | `Student@dev1` |

---

## Slug & trạng thái

**Slug:** `seal-gd6-pending-confirm`

| Thành phần | Giá trị seed |
|------------|--------------|
| Hackathon | **`PENDING_CONFIRM`** |
| Sơ loại | published + scoring locked |
| Chung kết | active + **scoring locked** |
| Events | KICKOFF + WORKSHOP + **AWARDS** (repair startup) |
| Teams | 3× `GD6-0x ADVANCED CK` |
| Submissions | prelim + final `SUBMITTED` mỗi đội |
| Scores CK | `isFinal=true`, guest judge — t1 > t2 > t3 |
| Prizes | **FIRST** trên team 01 — còn slot **SECOND** cho team 02 (test 6.2) |

---

## Lấy ID nhanh

```http
GET /api/v1/hackathons?q=seal-gd6-pending-confirm
Authorization: Bearer {{coordToken}}
```

```http
GET /api/v1/hackathons/{{hackathonId}}/rounds
Authorization: Bearer {{coordToken}}
```

Hoặc copy từ log startup.

---

## Postman variables

| Variable | Mô tả |
|----------|--------|
| `gd6HackathonSlug` | `seal-gd6-pending-confirm` |
| `hackathonId` | từ GET hackathon |
| `prelimRoundId` | round `isFinal=false` |
| `finalRoundId` | round `isFinal=true`, `scoringLocked=true` |
| `finalCriterionId` | log seeder hoặc `GET /rounds/{finalRoundId}/criteria` |
| `teamId` (t2) | team `GD6-02 ADVANCED CK` — test POST prize SECOND |

---

## Teams & điểm CK (gợi ý xếp hạng)

| Team seed | Student | CK score (seed) | Prize |
|-----------|---------|-----------------|-------|
| GD6-01 ADVANCED CK | `student.gd6.leader01@` | ~9.2 | **FIRST** (đã seed) |
| GD6-02 ADVANCED CK | `student.gd6.leader02@` | ~8.6 | — (test **6.2** SECOND) |
| GD6-03 ADVANCED CK | `student.gd6.leader03@` | ~8.1 | — |

---

## Luồng API GĐ6 (6.0 → 6.4)

1. **6.0b** `GET /hackathons/{{hackathonId}}/readiness?target=AWARDS` → `ready: true`
2. **6.1** `GET /hackathons/{{hackathonId}}/team-rankings` *(stub — có thể `[]` cho đến khi implement FR-31)*
3. **6.2** `POST /hackathons/{{hackathonId}}/prizes` — team 02, `prizeRank: "SECOND"`
4. **6.2b** `GET /hackathons/{{hackathonId}}/prizes` — 2 giải (FIRST + SECOND)
5. **6.3** `PATCH /hackathons/{{hackathonId}}/confirm` → `FINISHED` *(stub — kiểm tra response)*
6. **6.4** `POST /hackathons/{{hackathonId}}/export-jobs` body `{ "type": "CSV_RANKINGS" }`

**Lưu ý:** `chapter-rankings` chỉ trả data khi hackathon **`FINISHED`**.

---

## Reset seed GĐ6

Xem [seed-coverage-audit.md §6](seed-coverage-audit.md) — đặt `@slug = 'seal-gd6-pending-confirm'`, chạy SQL xóa, restart app.

---

## SQL verify nhanh

```sql
SELECT id, slug, status FROM hackathons WHERE slug = 'seal-gd6-pending-confirm';

SELECT r.name, r.is_final, r.scoring_locked, r.is_published
FROM rounds r
JOIN hackathons h ON h.id = r.hackathon_id
WHERE h.slug = 'seal-gd6-pending-confirm';

SELECT t.team_name, p.prize_rank
FROM prizes p
JOIN teams t ON t.id = p.team_id
JOIN hackathons h ON h.id = p.hackathon_id
WHERE h.slug = 'seal-gd6-pending-confirm';
```
