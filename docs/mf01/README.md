# MF-01 — Chuẩn bị sự kiện (GĐ1)

**Phiên bản:** 3.0 · Kiến trúc **Hackathon → Round → Track**

**Quy tắc ưu tiên khi mâu thuẫn:** [`schema-v3.0-mysql.md`](../db/schema-v3.0-mysql.md) > [`02-functional-requirements.md`](02-functional-requirements.md) > [`system/workflow.md`](../system/workflow.md) (đoạn cũ)

## Thứ tự đọc (dev / QA)

| # | File | Nội dung |
|---|------|----------|
| 1 | [01-business-rules.md](01-business-rules.md) | Gate G1–G5, actor, XOR, conflict, ma trận audit |
| 2 | [02-functional-requirements.md](02-functional-requirements.md) | FR-01…07B — nghiệp vụ + DB |
| 3 | [03-mainflow-gd1.md](03-mainflow-gd1.md) | Luồng 7 bước (tóm tắt) |
| 4 | [04-quy-trinh-van-hanh.md](04-quy-trinh-van-hanh.md) | Runbook API + Timeline & Events |
| 5 | [05-test-data.md](05-test-data.md) | Seed, Postman, JSON mẫu |
| 6 | [06-qa-uat.md](06-qa-uat.md) | Ma trận TC, UAT |
| — | [api/README.md](api/README.md) | Chi tiết request/validation từng FR |

## Nguồn hệ thống

- [Workflow v5.0](../system/workflow.md) — 6 giai đoạn
- [MF-02 Auth](../mf02/README.md) — JWT cho mọi API MF-01 (trừ `/auth/**`)
- [MF-03 Thi & chấm](../mf03/README.md) — GĐ3–GĐ6 sau khi hoàn thành GĐ2
