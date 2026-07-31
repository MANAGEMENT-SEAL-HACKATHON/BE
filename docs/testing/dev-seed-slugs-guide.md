# Dev seed slugs — hướng dẫn (1 happy path)

**Nguồn SSOT code:** `DevSeedCatalog.ALL_DEV_HACKATHON_SLUGS`  
**Nạp lúc start:** `DataInitializer` (`@Profile("dev")`)  
**Cập nhật:** 2026-07-31  

Chỉ **1 hackathon slug** còn lại: `seal-e2e-2026` — GĐ1 structure + GĐ2 baseline → **continuous GĐ3–GĐ6**.  
Lỗi/gate cố tình: tái tạo tay trên happy slug — xem [intentional-errors-catalog.md](intentional-errors-catalog.md).  
Account / password: [dev-seed-guide.md](dev-seed-guide.md).  
Chạy tay UI: [manual-ui-playbook-gd1-gd6.md](manual-ui-playbook-gd1-gd6.md) · Defense: [../defense-panel/README.md](../defense-panel/README.md).

---

## Happy slug duy nhất

| # | Slug | Giai đoạn | Làm gì |
|---|------|-----------|--------|
| 1 | `seal-e2e-2026` | **GĐ1–GĐ6 continuous** | Structure + **6 đội × 2 track** (không orphan); ĐK mở, prelim inactive — demo Setup → Lottery → SL → CK → Confirm trên **cùng** kỳ |

**Không phải slug hackathon:** `AccountStatesDataSeeder` (user trạng thái duyệt / email…).

### Flags (`application-dev.properties`)

| Flag | Mặc định | Mục đích |
|------|----------|----------|
| `app.seed.e2e.enabled` | `true` | Bật `Gd1DataSeeder` + `E2eWorkflowDataSeeder` |
| `app.seed.e2e.force-gd2-reset` | `false` | `true` = ép reset về GĐ2 baseline dù đang GĐ3+ |

~~`app.seed.gd3` / `gd4` / `gd5` / `gd6`~~ — **đã gỡ**.

---

## `E2eDevFlowGuard`

Freeze guard cho `seal-e2e-2026`: khi coordinator đã đi qua GĐ2 (lottery / activate SL / nộp bài / …) hoặc status `PENDING_CONFIRM` / `FINISHED`, mọi **repair startup** phá workflow bị bỏ qua — trừ khi `app.seed.e2e.force-gd2-reset=true`.

→ Demo hội đồng A–Z với `ddl-auto=update`: **restart BE không mất dữ liệu GĐ3–GĐ6**.

---

## Chi tiết `seal-e2e-2026`

| | |
|--|--|
| **Status (baseline)** | `ONGOING`, prelim inactive |
| **Seeder** | `Gd1DataSeeder` + `E2eWorkflowDataSeeder` |
| **Profile** | `DevSeedCatalog.PROFILE_E2E` — 6 đội, 2 track, `topNAdvance=2` |
| **Hỗ trợ** | Happy GĐ1 verify; GĐ2 close-reg → lock → lottery → activate; continuous GĐ3–GĐ6 |
| **Account** | `coord@fpt.edu.vn` · `student.e2e.t01.leader@…` … `t06` / `Student@dev1` |

---

## Former happy (purged)

Các slug sau **không còn seed**; nằm trong `DevSeedCatalog.DEPRECATED_SLUGS` — xóa mỗi lần start `dev`:

| Slug cũ | GĐ (trước đây) |
|---------|----------------|
| `seal-fall-2025-finished` | Archive |
| `seal-gd3-prelim-open` | GĐ3 snapshot |
| `seal-gd4-advance-ready` | GĐ4 snapshot |
| `seal-gd5-final-active` | GĐ5 snapshot |
| `seal-gd6-pending-confirm` | GĐ6 snapshot |
| `seal-gd4-tiebreak-submission-time` / `tiebreak-manual` / `wildcard-gap` | GĐ4 phụ |

Slug bad/mid-stage khác (~40+) cũng purge cùng `DEPRECATED_SLUGS`.

---

## Liên kết

- [intentional-errors-catalog.md](intentional-errors-catalog.md)
- [master-slug-test-matrix.md](master-slug-test-matrix.md)
- [manual-ui-playbook-gd1-gd6.md](manual-ui-playbook-gd1-gd6.md)
