# Dev seed — Hướng dẫn dữ liệu test

> **Profile:** `dev` · **Seeder:** `Gd1DataSeeder` + `E2eWorkflowDataSeeder`  
> Sau khi start app, tìm log **`DEV SEED — 2 hackathon`**.

---

## Tổng quan — **2 hackathon**

| Slug | Tên | Trạng thái | Mục đích |
|------|-----|------------|----------|
| `seal-e2e-2026` | SEAL E2E 2026 | `ONGOING` | GĐ1 sẵn sàng · **7 đội** + **3 SV chưa có nhóm** → test GĐ2→GĐ6 |
| `seal-fall-2025-finished` | SEAL Fall 2025 (Completed) | `FINISHED` | Archive duy nhất |

**Đã xóa tự động khi start dev:** mọi slug cũ (`seal-spring-2026*`, `seal-gd1-incomplete`, …).

---

## Hackathon E2E — `seal-e2e-2026`

### GĐ1 (đã seed)

- Round Sơ loại + Chung kết, 3 track, criteria, events KICKOFF + WORKSHOP
- `GET /hackathons/{id}/readiness?target=ONGOING` → `ready: true`
- Vòng sơ loại **chưa active** (test GĐ2 trước)
- Đăng ký hackathon **còn mở** ~14 ngày

### GĐ2 (dữ liệu sẵn)

| Thành phần | Chi tiết |
|------------|----------|
| **7 đội** | `E2E-T01` … `E2E-T07` — `ACTIVE`, 3 người/đội, **chưa khóa**, **chưa lottery** |
| **3 orphan** | Đã đăng ký hackathon, chưa có đội |

**Email orphan (mời vào đội mới của bạn):**

| # | Email | Password |
|---|-------|----------|
| 1 | `student.e2e.orphan1@fpt.edu.vn` | `Student@dev1` |
| 2 | `student.e2e.orphan2@fpt.edu.vn` | `Student@dev1` |
| 3 | `student.e2e.orphan3@fpt.edu.vn` | `Student@dev1` |

**Luồng test GĐ2 gợi ý:**

1. Đăng ký / tạo tài khoản student mới (hoặc dùng account riêng).
2. `POST /me/hackathons/{id}/register` → `POST /me/teams` tạo đội.
3. Mời 3 orphan: `POST /teams/{id}/members/invite` với 3 email trên.
4. Orphan accept → đủ 3–4 người → Coord duyệt / lottery → tiếp GĐ3…

**Leader đội có sẵn (tham khảo):** `student.e2e.t01.leader@fpt.edu.vn` … `t07.leader@` / `Student@dev1`

### GĐ3 → GĐ6

Tiếp tục trên **cùng** hackathon `seal-e2e-2026` theo [`happy-path-gd1-gd6-responses.md`](./happy-path-gd1-gd6-responses.md) (đổi slug).

---

## Tài khoản hệ thống

| Role | Email | Password |
|------|-------|----------|
| Coordinator | `coord@fpt.edu.vn` | `Coordinator@dev1` |
| Judge | `judge1@fpt.edu.vn` | `Judge@dev1` |
| Mentor | `mentor@fpt.edu.vn` | `Mentor@dev1` |

---

## Cấu hình

```properties
app.seed.e2e.enabled=true
```

---

## Reset thủ công (nếu cleanup tự động lỗi FK)

```sql
DELETE FROM chapter_rankings WHERE hackathon_id IN (SELECT id FROM hackathons WHERE slug LIKE 'seal-spring-2026%' OR slug = 'seal-gd1-incomplete');
DELETE FROM hackathon_registrations WHERE hackathon_id IN (SELECT id FROM hackathons WHERE slug NOT IN ('seal-e2e-2026', 'seal-fall-2025-finished'));
DELETE FROM hackathons WHERE slug NOT IN ('seal-e2e-2026', 'seal-fall-2025-finished');
```

Restart app `profile=dev`.

---

## Tài liệu liên quan

- Happy path API: `happy-path-gd1-gd6-responses.md`
- FE God Mode: `fe-god-mode-e2e-test-flow.md`
