# Testing — SEAL Hackathon BE

## Bắt đầu

| Tài liệu | Mục đích |
|----------|----------|
| **[full-workflow-api-test-gd1-gd6.md](full-workflow-api-test-gd1-gd6.md)** | **File chính** — hướng dẫn tester, E2E GĐ0→6, catalog 166 API (request/response JSON) |
| **[gate-regression-test-matrix-gd1-gd6.md](gate-regression-test-matrix-gd1-gd6.md)** | **Regression GĐ1–6** — testcase gate, negative, E2E full path |
| **[fe-gd1-gd2-gd3-workflow-mapping.md](fe-gd1-gd2-gd3-workflow-mapping.md)** | **FE GĐ1–3** — gate BTC, readiness targets, timeline sync |
| **[fe-gd1-gd2-structure-and-fields.md](fe-gd1-gd2-structure-and-fields.md)** | **FE GĐ1/GĐ2** — Round/Track/Bảng, field từng form |
| **[fe-gd3-api-mapping.md](fe-gd3-api-mapping.md)** | **FE GĐ3 — file gửi FE** (portal + coordinator + judge + seed + PersonB) |
| **[postman-playbook-gd2-gd3-integration.md](postman-playbook-gd2-gd3-integration.md)** | **Postman GĐ2+GĐ3** — 7 integration test + **§0.5 quy trình Presentation/timer cho FE** |
| **[e2e-gd2-gd3-v41-manual-test.md](e2e-gd2-gd3-v41-manual-test.md)** | E2E manual GĐ2→GĐ3 v4.1 (multipart, shuffle, timer, negative) |
| **[test-strategy-gd1-gd6-unit-to-e2e.md](test-strategy-gd1-gd6-unit-to-e2e.md)** | Chiến lược unit → integration → E2E GĐ1–6 |
| **[gd1-full-test-matrix-and-seeds.md](gd1-full-test-matrix-and-seeds.md)** | **GĐ1** — ma trận test + seed (events, rounds, tracks, timer defaults) |
| **[gd2-full-test-matrix-and-seeds.md](gd2-full-test-matrix-and-seeds.md)** | **GĐ2** — ma trận test + seed (teams, lottery, orphan) |
| **[gd3-full-test-matrix-and-seeds.md](gd3-full-test-matrix-and-seeds.md)** | **GĐ3** — ma trận test + 6 profile seed |
| **[gd5-full-test-matrix-and-seeds.md](gd5-full-test-matrix-and-seeds.md)** | **GĐ5** — ma trận test + 6 profile seed |
| **[dev-seed-guide.md](dev-seed-guide.md)** | **Seed dev** — 40 slug (`seal-e2e-2026`, `seal-gd1-incomplete`, `seal-gd2-teams-edge`, `seal-gd3-*`…`seal-gd6-*`) |
| **[gd4-gd5-e2e-seed-data.md](gd4-gd5-e2e-seed-data.md)** | **GĐ4/GĐ5** — ma trận teams, Postman vars, SQL verify |
| [api-catalog-with-json.md](api-catalog-with-json.md) | Bản rút Phần III (cùng nội dung) — mở tab riêng khi chỉ tra API |

**Swagger:** `http://localhost:8080/swagger-ui.html`

## Quy trình gợi ý

1. Đọc **Phần I** → tạo Postman environment (`baseUrl`, tokens, `hackathonId`, …).
2. Chạy **Phần II** — happy path GĐ1→6 **hoặc** đường tắt seed GĐ3/GĐ4/GĐ5 (bảng đầu Phần II).
3. Regression gate → **Phần II-B** playbook hoặc **[gate-regression-test-matrix-gd1-gd6.md](gate-regression-test-matrix-gd1-gd6.md)**.
4. Test lẻ / catalog → **Phần III** (`Ctrl+F` path) hoặc **Phần IV** checklist.
5. Dữ liệu seed chi tiết: [mf01/05-test-data.md](../mf01/05-test-data.md), [mf02/05-test-data-gd2-teams.md](../mf02/05-test-data-gd2-teams.md).

## Seed theo giai đoạn (dev)

| GĐ | Ma trận + seed |
|----|----------------|
| GĐ1 | [gd1-full-test-matrix-and-seeds.md](gd1-full-test-matrix-and-seeds.md) — `seal-e2e-2026`, `seal-gd1-incomplete`, `seal-fall-2025-finished` |
| GĐ2 | [gd2-full-test-matrix-and-seeds.md](gd2-full-test-matrix-and-seeds.md) — 7 đội + 3 orphan trên `seal-e2e-2026` |
| GĐ3 | [gd3-full-test-matrix-and-seeds.md](gd3-full-test-matrix-and-seeds.md) |
| GĐ4 | [gd4-full-test-matrix-and-seeds.md](gd4-full-test-matrix-and-seeds.md) |
| GĐ5 | [gd5-full-test-matrix-and-seeds.md](gd5-full-test-matrix-and-seeds.md) |
| GĐ6 | [gd6-full-test-matrix-and-seeds.md](gd6-full-test-matrix-and-seeds.md) |

Xem thêm **[dev-seed-guide.md](dev-seed-guide.md)** — tóm tắt 40 slug dev.

**Legacy (đã xóa khi start dev):** `seal-spring-2026*`, `seal-gd1-ready`.

## Cập nhật catalog (dev)

Khi thêm/sửu Controller, cập nhật thủ công Phần III trong `full-workflow-api-test-gd1-gd6.md` hoặc chạy script sinh catalog (nếu có trong repo).

## Tham chiếu

| Tài liệu | Nội dung |
|----------|----------|
| [api-authorization-matrix.md](../api-authorization-matrix.md) | Role / quyền |
| [system/workflow.md](../system/workflow.md) | Luồng nghiệp vụ GĐ1→6 |
| [user-role/](../user-role/) | Portal Student / Judge / Mentor |
