# Testing — SEAL Hackathon BE

## Bắt đầu

| Tài liệu | Mục đích |
|----------|----------|
| **[full-workflow-api-test-gd1-gd6.md](full-workflow-api-test-gd1-gd6.md)** | **File chính** — hướng dẫn tester, E2E GĐ0→6, catalog 166 API (request/response JSON) |
| **[seed-coverage-audit.md](seed-coverage-audit.md)** | **Seed DB dev** — slug theo GĐ, SQL verify, bật/tắt Gd4 |
| [api-catalog-with-json.md](api-catalog-with-json.md) | Bản rút Phần III (cùng nội dung) — mở tab riêng khi chỉ tra API |

**Swagger:** `http://localhost:8080/swagger-ui.html`

## Quy trình gợi ý

1. Đọc **Phần I** → tạo Postman environment (`baseUrl`, tokens, `hackathonId`, …).
2. Chạy **Phần II** (E2E) từ trên xuống — happy path GĐ1→6.
3. Test lẻ / regression → **Phần III** (`Ctrl+F` path) hoặc **Phần IV** checklist.
4. Dữ liệu seed chi tiết: [mf01/05-test-data.md](../mf01/05-test-data.md), [mf02/05-test-data-gd2-teams.md](../mf02/05-test-data-gd2-teams.md).

## Seed theo giai đoạn (dev)

| Slug | GĐ |
|------|-----|
| `seal-spring-2026` | GĐ2 teams (`GD2-*`) |
| `seal-gd3-prelim-open` | GĐ3 — submit/late/calibration |
| `seal-gd5-final-active` | **GĐ5** — Chung kết active, nộp/chấm, lock CK |
| `seal-gd6-pending-confirm` | GĐ6 — trao giải, confirm |
| `seal-gd4-tiebreak-wildcard` | GĐ4 — chỉ khi `app.seed.gd4.enabled=true` |

Chi tiết + SQL: **[seed-coverage-audit.md](seed-coverage-audit.md)**.

## Cập nhật catalog (dev)

Khi thêm/sửa Controller:

```bash
python docs/testing/_gen_catalog_rich.py
python docs/testing/_merge_playbook.py
```

Script đọc `*Controller.java`, gán giai đoạn GĐ0–7, sinh JSON mẫu (ưu tiên `SAMPLES` trong `_gen_catalog_rich.py`).

## Tham chiếu

| Tài liệu | Nội dung |
|----------|----------|
| [api-authorization-matrix.md](../api-authorization-matrix.md) | Role / quyền |
| [system/workflow.md](../system/workflow.md) | Luồng nghiệp vụ GĐ1→6 |
| [user-role/](../user-role/) | Portal Student / Judge / Mentor |
