# MF-02 — Auth, JWT & Users (GĐ2)

**Phạm vi đợt 1:** Email + mật khẩu, JWT access/refresh, đăng ký STUDENT, duyệt tài khoản, invitation judge khách.

**Judge khách — vòng đời (đã có trên BE):** invitation **72h** → quá hạn chỉ **resend** khi còn **≥48h trước KICKOFF** (Coordinator tự check slot) → sau **`event_end`** không login lại (khóa mềm, không xóa user). Chi tiết: [02-invitations.md](02-invitations.md#nghiệp-vụ-vòng-đời-đã-implement-trên-be).

**Breaking change:** Mọi API MF-01 (trừ `/api/v1/auth/**`, Swagger) cần `Authorization: Bearer <accessToken>`.

## Tài liệu

| File | Nội dung |
|------|----------|
| [01-auth-users.md](01-auth-users.md) | Auth API, JWT, user admin, dev login |
| [02-invitations.md](02-invitations.md) | 3 loại invitation (source of truth) |
| [03-oauth-prep.md](03-oauth-prep.md) | Chuẩn bị Google OAuth (đợt 2) |
| [04-test-data.md](04-test-data.md) | JSON mẫu Postman / curl |

## Liên kết MF-01

- Envelope & error codes: [mf01/api/_conventions.md](../mf01/api/_conventions.md)
- Runbook GĐ1: [mf01/04-quy-trinh-van-hanh.md](../mf01/04-quy-trinh-van-hanh.md)
