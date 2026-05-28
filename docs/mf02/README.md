# MF-02 — Giai đoạn 2 (Auth + Đội)

**Phạm vi GĐ2:** Đăng ký tài khoản, JWT, duyệt user, judge khách, **thành lập đội**, bốc thăm track, mentor per-round.

**Breaking change:** API MF-01 (trừ `/api/v1/auth/**`, Swagger) cần `Authorization: Bearer <accessToken>`.

---

## Tài liệu Auth & Users (đã implement)

| File | Nội dung |
|------|----------|
| [01-auth-users.md](01-auth-users.md) | Auth API, JWT, user admin, dev login |
| [02-invitations.md](02-invitations.md) | 3 loại invitation (source of truth) |
| [03-oauth-prep.md](03-oauth-prep.md) | Chuẩn bị Google OAuth (đợt 2) |
| [04-test-data.md](04-test-data.md) | JSON mẫu Postman — auth / users |

---

## Tài liệu Teams GĐ2 (khung API — logic TODO)

| File | Đối tượng | Nội dung |
|------|-----------|----------|
| **[03-api-reference-gd2.md](03-api-reference-gd2.md)** | **FE (ưu tiên)** | Endpoint, request/response, gợi ý màn hình UX |
| [05-test-data-gd2-teams.md](05-test-data-gd2-teams.md) | FE / QA | curl, Postman, JSON kỳ vọng & mock |
| [01-business-rules-gd2.md](01-business-rules-gd2.md) | BE / BA | Business rules FR-11 … FR-13C |
| [02-mainflow-gd2.md](02-mainflow-gd2.md) | PM / FE | 7 bước main flow + mermaid |

**Swagger:** `http://localhost:8080/swagger-ui.html` — tag **Teams (GĐ2)**.

**Trạng thái code:** Controller + DTO + service interface có; `TeamServiceImpl` / `HackathonLotteryServiceImpl` ném `UnsupportedOperationException` → HTTP **501** `NOT_IMPLEMENTED`.

---

## Liên kết MF-01

- Envelope & error codes: [mf01/api/_conventions.md](../mf01/api/_conventions.md)
- Timeline GĐ1: [workflow/mf01-gd1-timeline-events.md](../workflow/mf01-gd1-timeline-events.md)
- Runbook: [mf01/04-quy-trinh-van-hanh.md](../mf01/04-quy-trinh-van-hanh.md)

---

## Spec gốc

`GD02_SEAL_MF02_MASTER_v3.5.docx` (Workflow v5.0) — bản convert nội bộ: `BE/.cursor-tmp-mf02-master-v35.md`.

---

## Tiếp theo — MF-03

Sau GĐ2 (đội ACTIVE + lottery): [mf03/README.md](../mf03/README.md) — nộp bài, chấm điểm, ranking, trao giải.
