# MF-02 GĐ2 — Auth, JWT & User API

**Frontend:** xem **[fe-auth-integration.md](./fe-auth-integration.md)** — checklist login, refresh rotation, forgot password, interceptor mẫu.

**Phạm vi đợt 1:** Email + mật khẩu, JWT access/refresh, đăng ký STUDENT **mở** (sân chơi chung), duyệt tài khoản (FR-07/08/09). **Đợt 2:** [Google OAuth — chuẩn bị](./03-oauth-prep.md).



**Chính sách duyệt:** FPT K19+ dùng email cá nhân — **không** auto-approve; **mọi** tài khoản chờ **Coordinator duyệt tay** sau verify email. Coordinator xem `institution`, `chapterCode`, `studentCode` khi duyệt (thủ công; bảng `participating_institutions` đợt sau).



**Invitation (3 loại):** xem **[02-invitations.md](./02-invitations.md)** — source of truth.



---



## 1. Breaking change (MF-01)



Mọi API MF-01 (trừ `/api/v1/auth/**`, Swagger) cần `Authorization: Bearer <accessToken>`.



| Profile | JWT |

|---------|-----|

| `security.jwt.enabled=true` (mặc định) | JWT thật |

| `security.jwt.enabled=false` | Stub Coordinator id=1 |



---



## 2. Dev login (seed)



Log đầy đủ khi start `dev`: `[Gd1DataSeeder] Dev login` + `Password=... | passwordHash=...`.



| Email | Password | Role |

|-------|----------|------|

| `coord@fpt.edu.vn` | `Coordinator@dev1` | COORDINATOR / APPROVED |

| `judge1@fpt.edu.vn`, `judge2@fpt.edu.vn` | `Judge@dev1` | JUDGE / APPROVED |

| `guestjudge@gmail.com` | `GuestJudge@dev1` | JUDGE (temp) |

| `mentor@fpt.edu.vn` | `Mentor@dev1` | MENTOR |

| `pending.judge@fpt.edu.vn` | `PendingJudge@dev1` | JUDGE / PENDING |



---



## 3. Ba bước sau đăng ký (quan trọng)



| Bước | API / Ai | Kết quả |

|------|----------|---------|

| **Đăng ký** | `POST /auth/register` | `PENDING`, chưa login |

| **Verify email** | `GET /auth/verify-email?token=` | `email_verified_at` set — **vẫn PENDING** |

| **Duyệt** | Coordinator `PATCH /users/{id}/status` | `APPROVED` → mới `POST /auth/login` |



Verify email **không** thay duyệt Coordinator.



---



## 4. API Auth (public)



| Method | Path | Mô tả |

|--------|------|--------|

| POST | `/api/v1/auth/register` | STUDENT mở: INTERNAL (`studentCode`, `chapterId`) hoặc EXTERNAL (`institution`, `studentCode`) — **không** `invitationToken` |

| GET | `/api/v1/auth/verify-email?token=` | Xác thực sở hữu email |

| POST | `/api/v1/auth/login` | Chỉ `status=APPROVED`; response `mustChangePassword` (judge khách) |

| POST | `/api/v1/auth/change-password` | Bearer — đổi MK (bắt buộc judge khách lần đầu) |

| POST | `/api/v1/auth/refresh` | Refresh token — **rotation**: response chứa `refreshToken` mới; FE phải ghi đè storage sau mỗi lần gọi |

| POST | `/api/v1/auth/logout` | Revoke refresh (một phiên) |

| POST | `/api/v1/auth/logout-all` | Bearer `APPROVED` — revoke mọi `user_sessions` của user hiện tại |

| POST | `/api/v1/auth/forgot-password` | Body `{ "email" }` — luôn `200` + message chung (không lộ email có/không); dev: token + URL trong log / `devResetUrl` nếu bật |

| POST | `/api/v1/auth/reset-password` | Body `{ "token", "newPassword" }` — JWT reset; revoke mọi phiên sau đổi MK |



**Mã SV:** trùng → `409 STUDENT_CODE_DUPLICATE`.



### Refresh rotation & phiên



- Mỗi `POST /auth/refresh`: server revoke session cũ, tạo refresh mới. Dùng lại refresh cũ → `401 REFRESH_TOKEN_INVALID` (có thể revoke toàn bộ phiên nếu phát hiện reuse).

- `POST /auth/change-password` và `POST /auth/reset-password` thành công → **revoke all sessions**; user cần đăng nhập lại trên mọi thiết bị.

- `POST /auth/logout-all`: client xóa token local và chuyển về login.



### Forgot / reset password (dev)



Giống verify-email: JWT ngắn hạn (`security.jwt.password-reset-ttl-hours`, mặc định 1h), `log.info` token + URL khi user `APPROVED` có `passwordHash`. Profile dev: `security.jwt.dev-expose-password-reset-token=true` trả `devResetToken` / `devResetUrl` trong response. Chưa gửi SMTP thật.



---



## 5. Invitation judge khách + sự kiện công khai



Chi tiết: **[02-invitations.md](./02-invitations.md)** (vòng đời 72h / resend 48h trước KICKOFF / khóa sau `event_end`).



| Hạng mục | Mô tả |

|----------|--------|

| **Sự kiện công khai** | Copy link trên FE ([seal-hackathon-fe.vercel.app](https://seal-hackathon-fe.vercel.app)) — **không** API mời email |

| **Judge khách (FR-05a)** | `POST /users/temp-judges` (`hackathonId` bắt buộc), `POST /invitations/{id}/resend`; invitation **72h**; login hết hạn → `INVITATION_EXPIRED`; resend chỉ **≥48h trước KICKOFF**; sau `event_end` → `TEMP_JUDGE_HACKATHON_ENDED` (không xóa user) |

| **Mời đội (FR-12)** | Backlog GĐ2 |



Cấu hình: `app.frontend-url` (mặc định Vercel FE).



---



## 6. API User



| Method | Path | Auth |

|--------|------|------|

| GET | `/api/v1/users/me` | APPROVED — có `mustChangePassword` |

| POST | `/api/v1/auth/change-password` | APPROVED — judge khách đổi MK lần đầu |

| PATCH | `/api/v1/users/me` | APPROVED — `phone`, `avatarUrl` only |

| GET | `/api/v1/users` | COORDINATOR |

| GET | `/api/v1/users/{id}` | COORDINATOR — có `chapterCode`, `chapterName`, `institution` |

| PATCH | `/api/v1/users/{id}/status` | COORDINATOR — duyệt/từ chối |

| PATCH | `/api/v1/users/{id}` | COORDINATOR — `is_dept_head` |



---



## 7. QA checklist



**Dữ liệu mẫu:** [04-test-data.md](04-test-data.md).



1. `POST /auth/register` EXTERNAL hoặc INTERNAL (đăng ký mở).

3. Verify email → Coordinator duyệt → login.

4. `PATCH /users/me` cập nhật phone.

5. Trùng `studentCode` → 409.



---



## 8. Đợt 2



Xem [03-oauth-prep.md](./03-oauth-prep.md).


