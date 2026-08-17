# Dev seed — Hướng dẫn dữ liệu test

> **Profile:** `dev` · **Seeder:** `Gd1DataSeeder` + `E2eWorkflowDataSeeder` + `E2eDevFlowGuard`  
> **Danh mục slug:** `DevSeedCatalog.ALL_DEV_HACKATHON_SLUGS` (**1** hackathon) — [dev-seed-slugs-guide.md](dev-seed-slugs-guide.md)  
> Sau khi start app, tìm log **`[DataInitializer] Dev seed sẵn sàng — 1 happy slug: seal-e2e-2026`**.

---

## Schema & database (dev)

Profile `dev` dùng **`spring.jpa.hibernate.ddl-auto=update`** — Hibernate **không** drop schema mỗi lần restart. Seed chạy idempotent qua `DataInitializer` (+ purge `DEPRECATED_SLUGS`).

**Nếu DB lệch** (lỗi DDL, deadlock):

```sql
DROP DATABASE IF EXISTS SealHackathon;
CREATE DATABASE SealHackathon CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Rồi start lại: `mvn spring-boot:run -Dspring-boot.run.profiles=dev`

**Không** dùng `create-drop` khi demo A–Z (GĐ2→GĐ6) — shutdown sẽ mất dữ liệu.

---

## Hackathon duy nhất

| Slug | Tên | Trạng thái | Mục đích |
|------|-----|------------|----------|
| `seal-e2e-2026` | SEAL E2E 2026 | `ONGOING` | **GĐ2 pre-lottery**: 6 đội ACTIVE × 2 track, chưa khóa — continuous GĐ2→GĐ6 |

**Không còn** snapshot slug GĐ3–GĐ6 / archive FINISHED — đã chuyển vào `DEPRECATED_SLUGS` và purge khi start.

### Trạng thái sau seed

- Đăng ký **đang mở**; prelim **inactive**; 2 track (RAG + AI Agent), **topic seed sẵn** (RAG Pipeline / AI Agent)
- 6 đội `E2E-T01`…`E2E-T06` (3 thành viên + `hackathon_registrations`), **chưa khóa**, **chưa lottery**
- `topNAdvance=2`, `minTeamsFinal=4`
- Events: WORKSHOP, KICKOFF, BUFFET, AWARDS + kits

`force-gd2-reset` **giữ topic** trên tracks; chỉ clear PDF đề bài prelim.

### Freeze guard (demo hội đồng)

Khi coordinator đã đi qua GĐ2 (lottery / activate / nộp / lock đội / …), restart BE **không** reset dữ liệu:

| Property | Mặc định | Ý nghĩa |
|----------|----------|---------|
| `app.seed.e2e.force-gd2-reset` | `false` | Giữ tiến độ GĐ3–GĐ6 |
| `app.seed.e2e.enabled` | `true` | Bật seed E2E |

Log khi frozen: `[E2eDevFlowGuard] Bỏ qua … — flow đang GĐ3+`.

**Ép reset về GĐ2** (sau demo):

```properties
app.seed.e2e.force-gd2-reset=true
```

Restart một lần → tắt cờ về `false`.

---

## Tài khoản staff

| Role | Email | Password |
|------|-------|----------|
| Coordinator | `coord@fpt.edu.vn` | `Coordinator@dev1` |
| Judge 1–4 | `judge1@`…`judge4@fpt.edu.vn` | `Judge@dev1` |
| Guest judge | `guestjudge@gmail.com` | `GuestJudge@dev1` |
| Mentor | `mentor@fpt.edu.vn` | `Mentor@dev1` |
| Student team N | `student.e2e.t0N.leader@fpt.edu.vn` | `Student@dev1` |
| Free-agent orphan 1–3 | `student.e2e.orphan1@`…`orphan3@fpt.edu.vn` | `Student@dev1` |

**Free-agent orphans:** APPROVED, **chưa ĐK sự kiện**, chưa vào đội. Dùng để test sự kiện mới:

1. Orphan login → Đăng ký sự kiện vừa tạo  
2. Leader (đã ĐK cùng sự kiện) → mời email orphan  

(Rule: chỉ mời được SV đã ĐK cùng sự kiện — chưa ĐK thì lookup/invite sẽ fail `INVITEE_NOT_REGISTERED`.)

---

## Luồng test tay

1. Login Coord → mở `SEAL E2E 2026`
2. GĐ2: đóng ĐK → khóa đội → bốc thăm (3+3) → kích hoạt Sơ loại
3. GĐ3→GĐ6 trên **cùng slug** (có thể chỉnh giờ máy; restart BE với `update` + `force-gd2-reset=false` **không** mất dữ liệu)

Chi tiết playbook: [../defense-panel/README.md](../defense-panel/README.md).
