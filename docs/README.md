# SEAL Hackathon — Tài liệu Backend

## Đọc nhanh

1. [Kiến trúc hệ thống](system/workflow.md) — Workflow v5.0, 6 giai đoạn
2. [DDL MySQL](db/schema-v3.0-mysql.md) — Source of truth schema
3. [MF-01 Chuẩn bị sự kiện](mf01/README.md) — GĐ1: business rules, FR, runbook, test
4. [MF-02 Auth & Users](mf02/README.md) — JWT, đăng ký, đội, lottery
5. [MF-03 Thi & chấm điểm](mf03/README.md) — GĐ3–GĐ6: submission, score, ranking, prizes

## Cấu trúc thư mục

```
docs/
├── system/          # Tài liệu toàn hệ thống
├── db/              # Schema DDL
├── mf01/            # Mainflow 01 — Chuẩn bị sự kiện (GĐ1)
│   └── api/         # Chi tiết API theo FR
├── mf02/            # Mainflow 02 — Auth, users, teams (GĐ2)
└── mf03/            # Mainflow 03 — Thi, chấm, chuyển vòng (GĐ3–GĐ6)
```

Thư mục `workflow/` và `api/` cũ chỉ còn stub redirect — dùng `mf01/`, `mf02/`, `mf03/`.
