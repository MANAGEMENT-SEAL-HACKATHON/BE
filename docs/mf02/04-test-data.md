# MF-02 — Dữ liệu test API (Auth, User, Invitation)

Dùng với **Postman**, **Swagger** (`http://localhost:8080/swagger-ui.html`) hoặc **curl**.  
Envelope response: xem [mf01/api/_conventions.md](../mf01/api/_conventions.md).

**Workflow:** [01-auth-users.md](01-auth-users.md)

**Invitation:** [02-invitations.md](02-invitations.md) (judge khách; sự kiện public trên FE).

---

## 0. Cấu hình chung

| Mục | Giá trị |
|-----|---------|
| Base URL | `http://localhost:8080` |
| Prefix | `/api/v1` |
| Header (API cần đăng nhập) | `Authorization: Bearer {{accessToken}}` |
| Content-Type | `application/json` |
| Profile dev | `spring.profiles.active=dev` (seed tự chạy) |
| JWT | `security.jwt.enabled=true` (mặc định) |
| Dev token | `security.jwt.dev-expose-verify-token=true` → response register có `devVerifyToken` |
| FE URL | `app.frontend-url` → mặc định `https://seal-hackathon-fe.vercel.app` |

### Biến Postman (gợi ý)

| Biến | Gán sau bước |
|------|----------------|
| `baseUrl` | `http://localhost:8080` |
| `accessToken` | Login Coordinator / Student |
| `refreshToken` | Login |
| `devVerifyToken` | `POST /auth/register` → `data.devVerifyToken` |
| `newUserId` | Register → `data.userId` |
| `invitationId` | `POST /users/temp-judges` → `data.invitation.id` |

### Tài khoản seed (sẵn APPROVED — login được ngay)

Sau mỗi lần start app (`profile=dev`), xem log **`[Gd1DataSeeder] Dev login`** và từng dòng `Password=... | passwordHash=$2a$...`.

| Email | Password | Role | Ghi chú |
|-------|----------|------|---------|
| `coord@fpt.edu.vn` | `Coordinator@dev1` | COORDINATOR | Duyệt user |
| `judge1@fpt.edu.vn` | `Judge@dev1` | JUDGE | APPROVED |
| `judge2@fpt.edu.vn` | `Judge@dev1` | JUDGE | APPROVED |
| `guestjudge@gmail.com` | `GuestJudge@dev1` | JUDGE / temp | APPROVED |
| `mentor@fpt.edu.vn` | `Mentor@dev1` | MENTOR | APPROVED |
| `pending.judge@fpt.edu.vn` | `PendingJudge@dev1` | JUDGE / PENDING | Login → `401 ACCOUNT_PENDING` |

### Chapter ID (INTERNAL register)
  
Sau khi start app, chapter seed theo code (xem log `[Gd1DataSeeder]`):

| `chapterId` (thường DB mới) | Code | Mô tả |
|-----------------------------|------|--------|
| `1` | `FPT-HCM` | Sinh viên FPT HCM |
| `2` | `FPT-HN` | Sinh viên FPT HN |
| `3` | `EXT` | Chapter external |

> Nếu DB đã có dữ liệu cũ, kiểm tra `SELECT id, code FROM chapters WHERE status = 'ACTIVE';`

---

## 1. Auth — public (không Bearer)

### 1.1 Login Coordinator

```http
POST {{baseUrl}}/api/v1/auth/login
Content-Type: application/json
```

```json
{
  "email": "coord@fpt.edu.vn",
  "password": "Coordinator@dev1"
}
```

**Kỳ vọng:** `200`, `data.accessToken`, `data.refreshToken`, `data.tokenType` = `"Bearer"`, `data.expiresInSeconds` = `1800`, `data.mustChangePassword` (judge khách lần đầu = `true`).

Lưu `accessToken` / `refreshToken` vào biến môi trường.

> Sự kiện công khai: copy link trên FE (VD `https://seal-hackathon-fe.vercel.app/hackathons/{slug}`) — không có API mời email.

---

### 1.3 Register INTERNAL (Gmail + mã SV)

```http
POST {{baseUrl}}/api/v1/auth/register
Content-Type: application/json
```

```json
{
  "fullName": "Trần Văn Internal QA",
  "email": "student.internal.qa@gmail.com",
  "password": "Student@qa123",
  "userType": "INTERNAL",
  "studentCode": "SE226001",
  "chapterId": 1
}
```

**Kỳ vọng:** `201`, `data.status` = `"PENDING"`, `data.devVerifyToken`, `data.devVerifyUrl`.

**Lỗi thường gặp:**

| Body đổi | HTTP | `error.code` |
|----------|------|----------------|
| Trùng `email` | 409 | `ACCOUNT_DUPLICATE_EMAIL` |
| Trùng `studentCode` | 409 | `STUDENT_CODE_DUPLICATE` |
| Thiếu `studentCode` | 422 | `STUDENT_CODE_REQUIRED` |
| Thiếu `chapterId` | 422 | `INVALID_CHAPTER` |
| `chapterId` = 999 | 422 | `INVALID_CHAPTER` |

---

### 1.4 Register EXTERNAL (đăng ký mở — không cần lời mời)

```http
POST {{baseUrl}}/api/v1/auth/register
Content-Type: application/json
```

```json
{
  "fullName": "Nguyễn Văn External QA",
  "email": "student.external.qa@gmail.com",
  "password": "Student@qa123",
  "userType": "EXTERNAL",
  "studentCode": "EXT2026001",
  "institution": "Đại học Bách Khoa"
}
```

**Lỗi thường gặp:**

| Tình huống | HTTP | `error.code` |
|------------|------|----------------|
| Thiếu `institution` | 422 | `INSTITUTION_REQUIRED` |
| Thiếu `studentCode` | 422 | `STUDENT_CODE_REQUIRED` |
| Trùng email / mã SV | 409 | `ACCOUNT_DUPLICATE_EMAIL` / `STUDENT_CODE_DUPLICATE` |

> **Loại 2 — mời vào đội** (FR-12, **mọi thành viên đội**) — xem [02-invitations.md](02-invitations.md) §2. **Chưa có API** — không test Postman ở đợt này.

---

### 1.5 Verify email — là gì? (không thay duyệt tài khoản)

| Câu hỏi | Trả lời |
|---------|---------|
| Verify email làm gì? | Chứng minh bạn **sở hữu** địa chỉ email (`email_verified_at` được set). |
| Có được login ngay không? | **Không** — `status` vẫn `PENDING` cho đến khi Coordinator **APPROVED**. |
| Khác gì duyệt Coordinator? | Verify = email thật; Duyệt = BTC xem hồ sơ (trường, chapter, SV tốt nghiệp, …) và cho phép dùng hệ thống. |

```http
GET {{baseUrl}}/api/v1/auth/verify-email?token={{devVerifyToken}}
```

**Kỳ vọng:** `200`, message *"Email đã xác thực"*.  
Gọi lại lần 2 vẫn `200` (idempotent).

**Lỗi:** token sai → `400` `EMAIL_VERIFY_TOKEN_INVALID`.

**Sau verify:** Coordinator `PATCH /users/{id}/status` → `APPROVED` (xem `chapterCode`, `institution` trong `GET /users/{id}`).

---

### 1.6 Login student — trước khi Coordinator duyệt (negative)

```http
POST {{baseUrl}}/api/v1/auth/login
Content-Type: application/json
```

```json
{ 
  "email": "student.internal.qa@gmail.com",
  "password": "Student@qa123"
}
```

**Kỳ vọng:** `401`, `error.code` = `ACCOUNT_PENDING` (*Tài khoản đang chờ duyệt*).

---

### 1.7 Refresh token

```http
POST {{baseUrl}}/api/v1/auth/refresh
Content-Type: application/json
```

```json
{
  "refreshToken": "{{refreshToken}}"
}
```

**Kỳ vọng:** `200`, `data.accessToken` mới, `data.refreshToken` giữ nguyên.

---

### 1.8 Logout

```http
POST {{baseUrl}}/api/v1/auth/logout
Content-Type: application/json
```

```json
{
  "refreshToken": "{{refreshToken}}"
}
```

**Kỳ vọng:** `200`. Sau đó refresh cùng token → lỗi session.

---

## 2. User admin (Coordinator + Bearer)

Dùng `accessToken` từ **1.1**.

### 2.1 Danh sách user chờ duyệt

```http
GET {{baseUrl}}/api/v1/users?status=PENDING&role=STUDENT&q=student.internal
Authorization: Bearer {{accessToken}}
```

Query tùy chọn: `status`, `role`, `userType`, `q`, `page`, `size`.

---

### 2.2 Chi tiết user (form duyệt)

```http
GET {{baseUrl}}/api/v1/users/{{newUserId}}
Authorization: Bearer {{accessToken}}
```

Kiểm tra trước khi duyệt:

| Field | Ý nghĩa khi duyệt |
|-------|-------------------|
| `emailVerifiedAt` | Bắt buộc đã verify (không null) |
| `userType` | `INTERNAL` (FPT/chapter) vs `EXTERNAL` (trường khác) |
| `chapterCode` / `chapterName` | SV FPT — đối chiếu chapter |
| `institution` | SV trường khác / đã tốt nghiệp — so **danh sách trường được phép** (thủ công; bảng `participating_institutions` đợt sau) |
| `studentCode` | Trùng lặp / hợp lệ |

---

### 2.3 Duyệt tài khoản APPROVED

```http
PATCH {{baseUrl}}/api/v1/users/{{newUserId}}/status
Authorization: Bearer {{accessToken}}
Content-Type: application/json
```

```json
{
  "status": "APPROVED"
}
```

**Kỳ vọng:** `200`, `data.status` = `"APPROVED"`.

**Lỗi:**

| Tình huống | HTTP | Ghi chú |
|------------|------|---------|
| Chưa verify email | 422 | `VALIDATION_FAILED` — *Phải xác thực email trước khi duyệt* |
| User đã APPROVED | 422 | `INVALID_STATUS_TRANSITION` |
| Duyệt khi chưa PENDING | 422 | `INVALID_STATUS_TRANSITION` |

---

### 2.4 Từ chối tài khoản

```http
PATCH {{baseUrl}}/api/v1/users/{{newUserId}}/status
Authorization: Bearer {{accessToken}}
Content-Type: application/json
```

```json
{
  "status": "REJECTED",
  "rejectionReason": "Mã SV không khớp hồ sơ FPT — liên hệ Coordinator."
}
```

Thiếu `rejectionReason` → `422` `REJECTION_REASON_REQUIRED`.

---

### 2.5 Gán Trưởng khoa (judge)

```http
PATCH {{baseUrl}}/api/v1/users/2
Authorization: Bearer {{accessToken}}
Content-Type: application/json
```

```json
{
  "isDeptHead": true
}
```

> `userId` = judge seed (ví dụ `judge1@fpt.edu.vn`).

---

## 3. Profile user (Student APPROVED + Bearer)

Sau **1.5** + **2.3**, login student:

```http
POST {{baseUrl}}/api/v1/auth/login
Content-Type: application/json
```

```json
{
  "email": "student.internal.qa@gmail.com",
  "password": "Student@qa123"
}
```

Lưu `accessToken` student → `{{studentAccessToken}}`.

### 3.1 GET profile

```http
GET {{baseUrl}}/api/v1/users/me
Authorization: Bearer {{studentAccessToken}}
```

**Kỳ vọng:** `200`, `data.email`, `data.role` = `STUDENT`, `data.status` = `APPROVED`.

---

### 3.2 PATCH profile (phone, avatar)

```http
PATCH {{baseUrl}}/api/v1/users/me
Authorization: Bearer {{studentAccessToken}}
Content-Type: application/json
```

```json
{
  "phone": "0901234567",
  "avatarUrl": "https://cdn.example/avatars/qa-student.png"
}
```

Chỉ gửi field cần đổi; field khác giữ nguyên.

---

## 4. Judge khách (GĐ1 FR-05a) & negative

Vòng đời: [02-invitations.md](02-invitations.md#nghiệp-vụ-vòng-đời-đã-implement-trên-be). API spec: [fr-05-personnel.md](../mf01/api/fr-05-personnel.md).

| Rule | Mã lỗi (ví dụ) |
|------|----------------|
| Invitation **72h**, chưa `accepted_at` | `401 INVITATION_EXPIRED` khi login |
| Resend chỉ sau khi **hết hạn**, **≥48h trước KICKOFF** | `422 INVITATION_STILL_VALID`, `422 INVITATION_RESEND_AFTER_KICKOFF_CUTOFF` |
| Sau **`event_end`** | `401 TEMP_JUDGE_HACKATHON_ENDED` (login/refresh) |
| `hackathonId` bắt buộc khi tạo | `400` validation |

### 4.0 Tạo judge khách (Coordinator)

```http
POST {{baseUrl}}/api/v1/users/temp-judges
Authorization: Bearer {{accessToken}}
Content-Type: application/json
```

```json
{
  "fullName": "Mr. Guest Judge QA",
  "email": "guest.judge.qa@gmail.com",
  "institution": "Partner Corp",
  "phone": "+84901234567",
  "hackathonId": 1
}
```

`hackathonId`: id hackathon seed GĐ1 (lấy từ `GET /api/v1/hackathons` khi dev). Hackathon phải có event **KICKOFF** (`starts_at`) nếu sau này test resend.

**Kỳ vọng:** `201`; user `isTempAccount=true`, `mustChangePassword=true`, `status=APPROVED`; invitation `role=JUDGE` + `hackathonId`; email stub (log MK tạm ở DEBUG).

### 4.1 Login judge khách → đổi mật khẩu

```http
POST {{baseUrl}}/api/v1/auth/login
```

Dùng email + MK tạm từ log email stub (hoặc tạo judge mới).

**Kỳ vọng:** `mustChangePassword: true`.

```http
POST {{baseUrl}}/api/v1/auth/change-password
Authorization: Bearer {{guestJudgeAccessToken}}
Content-Type: application/json
```

```json
{
  "currentPassword": "<mk-tam-tu-email>",
  "newPassword": "MyNewSecurePass1"
}
```

**Kỳ vọng:** `200`; `GET /users/me` → `mustChangePassword: false`.

### 4.2 Resend judge (Coordinator only)

**Trước khi gọi API:** Coordinator kiểm tra **thủ công** còn slot judge / judge vẫn muốn tham gia (BE không đếm slot).

```http
POST {{baseUrl}}/api/v1/invitations/{{invitationId}}/resend
Authorization: Bearer {{accessTokenCoordinator}}
```

| Điều kiện | Kỳ vọng |
|-----------|---------|
| Invitation **JUDGE**, `expires_at` **đã qua**, chưa `accepted_at` | `200`; MK tạm mới trong email stub; `expires_at` +72h |
| Token còn hiệu lực | `422 INVITATION_STILL_VALID` |
| Trong vòng **48h** trước KICKOFF hoặc sau khi KICKOFF đã bắt đầu | `422 INVITATION_RESEND_AFTER_KICKOFF_CUTOFF` |
| Chưa có KICKOFF / thiếu `starts_at` | `422 EVENT_KICKOFF_NOT_FOUND` |
| Hackathon đã qua `event_end` | `422 TEMP_JUDGE_HACKATHON_ENDED` |

### 4.3 Login sau `event_end` (temp judge)

Dùng user judge khách gắn hackathon có `event_end` **trước hôm nay** (cần seed/SQL hoặc hackathon test).

```http
POST {{baseUrl}}/api/v1/auth/login
```

**Kỳ vọng:** `401` `TEMP_JUDGE_HACKATHON_ENDED` (cả `POST /auth/refresh` với refresh token còn hiệu lực).

> BE **không xóa** bản ghi `users` — chỉ chặn đăng nhập (khóa mềm, một cuộc thi / một lần).

---

## 5. Bảo mật / role (smoke)

| Request | Token | Kỳ vọng |
|---------|-------|---------|
| `GET /api/v1/users` | Student | `403` FORBIDDEN |
| `GET /api/v1/users/me` | Student PENDING (chưa duyệt) | `403` |
| `GET /api/v1/hackathons` | Không header | `401` |
| `POST /api/v1/auth/register` | Không cần token | `201` |
| Bearer JWT hết hạn / sai | API protected | `401` |

---

## 6. Flow E2E gợi ý (copy theo thứ tự)

```text
1. (Tuỳ chọn) Chia sẻ link sự kiện trên FE (không API)
2. POST /auth/register       (INTERNAL hoặc EXTERNAL — không invitationToken)
3. GET  /auth/verify-email?token=...
4. Coordinator login → GET /users?status=PENDING → GET /users/{id}  (xem institution/chapter)
5. PATCH /users/{id}/status  { "status": "APPROVED" }
6. POST /auth/login          (student)
7. GET  /users/me
8. PATCH /users/me           (phone)
9. POST /auth/refresh
10. POST /auth/logout
```

---

## 7. Bộ email test (tránh trùng seed)

| Mục đích | Email gợi ý |
|----------|-------------|
| INTERNAL QA | `student.internal.qa@gmail.com` |
| EXTERNAL QA | `student.external.qa@gmail.com` |
| INTERNAL duplicate SV | `student.internal2.qa@gmail.com` + cùng `studentCode` |
| Login fail | `notexist@gmail.com` / password sai → `INVALID_CREDENTIALS` |

Đổi suffix (`qa2`, `qa3`…) nếu chạy lại test trên DB không reset.

---

## 8. curl nhanh (Windows PowerShell)

**Login Coordinator:**

```powershell
$login = Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/v1/auth/login" `
  -ContentType "application/json" `
  -Body '{"email":"coord@fpt.edu.vn","password":"Coordinator@dev1"}'
$token = $login.data.accessToken
```

**Register INTERNAL:**

```powershell
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/v1/auth/register" `
  -ContentType "application/json" `
  -Body '{"fullName":"QA Internal","email":"student.internal.qa@gmail.com","password":"Student@qa123","userType":"INTERNAL","studentCode":"SE226001","chapterId":1}'
```

**List users (Coordinator):**

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/users?status=PENDING" `
  -Headers @{ Authorization = "Bearer $token" }
```

---

*Tài liệu sinh cho MF-02 (auth JWT, register/login, users, invitations STUDENT). Cập nhật khi đổi DTO hoặc error code.*
