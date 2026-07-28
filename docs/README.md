# SEAL Hackathon — Tài liệu Backend

## Đọc nhanh

1. [Kiến trúc hệ thống](system/workflow.md) — Workflow v5.0, 6 giai đoạn
2. [DDL MySQL](db/schema-v3.0-mysql.md) — Source of truth schema
2b. **[Business Rules Catalog (GĐ1–GĐ6, bảng 11 cột)](business-rules-catalog.md)** — catalog thống nhất B1 từ code BE (bổ sung, không thay `mf0x/01-business-rules*.md`)
3. [MF-01 Chuẩn bị sự kiện](mf01/README.md) — GĐ1: business rules, FR, runbook, test
4. [MF-02 Auth & Users](mf02/README.md) — JWT, đăng ký, đội, lottery
5. [MF-03 Thi & chấm điểm](mf03/README.md) — GĐ3–GĐ6: submission, score, ranking, prizes
6. [Ma trận phân quyền MF-03 v4.1](api-authorization-matrix.md)
7. [User role portals (Student / Judge / Mentor)](user-role/README.md)
8. **[Workflow test API GĐ0→GĐ6 (playbook + JSON)](testing/full-workflow-api-test-gd1-gd6.md)** — E2E happy path + catalog **166** endpoint (request/response)
8b. **[FE mapping GĐ1–3 (gate BTC)](testing/fe-gd1-gd2-gd3-workflow-mapping.md)** — readiness targets, timeline sync
9. **[Seed coverage & SQL verify](testing/seed-coverage-audit.md)** — Dữ liệu dev theo GĐ3/GĐ4/GĐ6 + script kiểm tra DB

## Cấu trúc thư mục

```
docs/
├── system/          # Tài liệu toàn hệ thống
├── db/              # Schema DDL
├── mf01/            # Mainflow 01 — Chuẩn bị sự kiện (GĐ1)
│   └── api/         # Chi tiết API theo FR
├── mf02/            # Mainflow 02 — Auth, users, teams (GĐ2)
└── mf03/            # Mainflow 03 — Thi, chấm, chuyển vòng (GĐ3–GĐ6)
└── user-role/       # Portal Student / Judge / Mentor (/api/v1/me/*)
└── testing/         # QA — workflow test toàn API theo 6 giai đoạn
```

Thư mục `workflow/` và `api/` cũ chỉ còn stub redirect — dùng `mf01/`, `mf02/`, `mf03/`.
