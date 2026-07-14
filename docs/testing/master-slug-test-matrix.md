# Master slug test matrix (SSOT)

**Cập nhật:** 2026-07-14  
**Nguồn code:** `DevSeedCatalog.ALL_DEV_HACKATHON_SLUGS` (**6 slug**)  
**Mirror FE:** `seal-hackathon-fe/e2e/helpers/devSeedCatalogSlugs.js`, `seedRegistry.js`

**Mô tả chi tiết:** [dev-seed-slugs-guide.md](dev-seed-slugs-guide.md)  
**Lỗi cố tình (tái tạo tay):** [intentional-errors-catalog.md](intentional-errors-catalog.md)  
**UI playbook:** [manual-ui-playbook-gd1-gd6.md](manual-ui-playbook-gd1-gd6.md)

---

## Kim tự tháp kiểm thử

| Layer | Lệnh | Phạm vi |
|-------|------|---------|
| L1 Unit | `cd BE && mvn test` | service, gate helpers |
| L2 Integration | `cd BE && mvn test -Dtest="*IntegrationTest"` | Fixture-based gate ITs (không phụ thuộc bad seed) |
| L3 API probe | `cd seal-hackathon-fe && npm run probe:seeds` | **6** slug + negative probes trên happy |
| L4 Matrix UI | `npm run test:e2e:parity && npm run test:e2e:matrix` | **6** slug read-only |
| L5 Dedicated e2e | dedicated specs còn maps tới happy | Deep happy |

---

## Primary slug theo GĐ

| GĐ | Happy slug | Negative |
|----|------------|----------|
| GĐ1–2 | `seal-e2e-2026` | [intentional-errors-catalog.md](intentional-errors-catalog.md) §GĐ1–2 |
| Archive | `seal-fall-2025-finished` | — |
| GĐ3 | `seal-gd3-prelim-open` | catalog §GĐ3 |
| GĐ4 | `seal-gd4-advance-ready` | catalog §GĐ4 |
| GĐ5 | `seal-gd5-final-active` | catalog §GĐ5 |
| GĐ6 | `seal-gd6-pending-confirm` | catalog §GĐ6 |

---

## Ma trận 6 slug

| slug | gd | status | seeder | primary_roles |
|------|-----|--------|--------|---------------|
| seal-e2e-2026 | GĐ1–2 | ONGOING | Gd1DataSeeder + E2eWorkflow | coord, student |
| seal-fall-2025-finished | Archive | FINISHED | Gd1DataSeeder | coord, student.archive |
| seal-gd3-prelim-open | GĐ3 | ONGOING | Gd3PrelimOpenDataSeeder | student, coord, mentor, judge |
| seal-gd4-advance-ready | GĐ4 | ONGOING | Gd4AdvanceReadyDataSeeder | coord |
| seal-gd5-final-active | GĐ5 | ONGOING | Gd5FinalRoundDataSeeder | student, guest judge |
| seal-gd6-pending-confirm | GĐ6 | PENDING_CONFIRM | Gd6PendingConfirmDataSeeder | coord |

Slug cũ (~47) → `DevSeedCatalog.DEPRECATED_SLUGS` (purge mỗi start `dev`).
