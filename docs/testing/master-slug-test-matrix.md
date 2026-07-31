# Master slug test matrix (SSOT)

**Cập nhật:** 2026-07-31  
**Nguồn code:** `DevSeedCatalog.ALL_DEV_HACKATHON_SLUGS` (**1 slug** continuous)  
**Mirror FE:** `seal-hackathon-fe/e2e/helpers/devSeedCatalogSlugs.js`, `seedRegistry.js`

**Mô tả chi tiết:** [dev-seed-slugs-guide.md](dev-seed-slugs-guide.md)  
**Lỗi cố tình (tái tạo tay):** [intentional-errors-catalog.md](intentional-errors-catalog.md)  
**UI playbook:** [manual-ui-playbook-gd1-gd6.md](manual-ui-playbook-gd1-gd6.md)  
**Defense:** [../defense-panel/README.md](../defense-panel/README.md)

**Tóm tắt:** Demo / test happy path = **`seal-e2e-2026` continuous GĐ1→GĐ6**. Mode A snapshot slug đã **deprecated/purged**. Guard: `E2eDevFlowGuard` + `app.seed.e2e.force-gd2-reset=false`.

---

## Kim tự tháp kiểm thử

| Layer | Lệnh | Phạm vi |
|-------|------|---------|
| L1 Unit | `cd BE && mvn test` | service, gate helpers |
| L2 Integration | `cd BE && mvn test -Dtest="*IntegrationTest"` | Fixture-based gate ITs (không phụ thuộc bad seed) |
| L3 API probe | `cd seal-hackathon-fe && npm run probe:seeds` | **1** slug + negative probes trên happy |
| L4 Matrix UI | `npm run test:e2e:parity && npm run test:e2e:matrix` | **1** slug read-only |
| L5 Dedicated e2e | dedicated specs còn maps tới happy | Deep happy / continuous |

---

## Primary slug theo GĐ

| GĐ | Happy slug | Negative |
|----|------------|----------|
| GĐ1–GĐ6 | `seal-e2e-2026` (continuous) | [intentional-errors-catalog.md](intentional-errors-catalog.md) |

---

## Ma trận 1 slug

| slug | gd | status (baseline) | seeder | primary_roles |
|------|-----|-------------------|--------|---------------|
| seal-e2e-2026 | GĐ1–GĐ6 continuous | ONGOING, prelim inactive | Gd1DataSeeder + E2eWorkflow (+ E2eDevFlowGuard) | coord, student.e2e, judge, mentor, guest |

---

## Former happy — deprecated / purged

| slug | gd (cũ) | trạng thái |
|------|---------|------------|
| seal-fall-2025-finished | Archive | **purged** (`DEPRECATED_SLUGS`) |
| seal-gd3-prelim-open | GĐ3 | **purged** |
| seal-gd4-advance-ready | GĐ4 | **purged** |
| seal-gd5-final-active | GĐ5 | **purged** |
| seal-gd6-pending-confirm | GĐ6 | **purged** |
| seal-gd4-tiebreak-* / wildcard-gap | GĐ4 phụ | **purged** |

Slug cũ (~47) khác → `DevSeedCatalog.DEPRECATED_SLUGS` (purge mỗi start `dev`).
