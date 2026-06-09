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
| **[seed-coverage-audit.md](seed-coverage-audit.md)** | **Seed DB dev** — slug theo GĐ, SQL verify, bật/tắt Gd4 |
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

| Slug | GĐ |
|------|-----|
| `seal-spring-2026` | GĐ2 teams (`GD2-*`) |
| `seal-gd3-prelim-open` | GĐ3 — submit/late/calibration/presentation |
| `seal-gd4-advance-ready` | **GĐ4** — ranking/wildcard/advance/activate CK |
| `seal-gd5-final-active` | **GĐ5** — Chung kết active, nộp/chấm, lock CK |
| `seal-gd6-pending-confirm` | GĐ6 — trao giải, confirm |
| `seal-gd4-tiebreak-wildcard` | GĐ4 — chỉ khi `app.seed.gd4.enabled=true` |

Chi tiết + SQL: **[seed-coverage-audit.md](seed-coverage-audit.md)**.

## Cập nhật catalog (dev)

Khi thêm/sửu Controller, cập nhật thủ công Phần III trong `full-workflow-api-test-gd1-gd6.md` hoặc chạy script sinh catalog (nếu có trong repo).

## Tham chiếu

| Tài liệu | Nội dung |
|----------|----------|
| [api-authorization-matrix.md](../api-authorization-matrix.md) | Role / quyền |
| [system/workflow.md](../system/workflow.md) | Luồng nghiệp vụ GĐ1→6 |
| [user-role/](../user-role/) | Portal Student / Judge / Mentor |
