# MF-02 FE Handover - Flow Register to Coordinator Approved

Tai lieu nay danh cho FE de implement dung logic moi sau khi BE refactor onboarding/auth.
Muc tieu la de FE biet ro:
- Logic cu da doi thanh gi.
- Vi sao doi.
- API nao FE can goi.
- Happy case can test tu dau den cuoi.

---

## 1) Tong quan "Logic cu -> Logic moi"

### Logic cu
- Register can nhieu field ngay tu dau.
- Co buoc `verify-email` (`GET /auth/verify-email?token=...`).
- Register response co `devVerifyToken`, `devVerifyUrl`.
- Mot so tai lieu/quy trinh van coi verify-email la buoc quan trong truoc duyet.

### Logic moi (dang dung)
- Register toi gian chi con `email`, `password`, `confirmPassword`.
- Bo hoan toan verify-email flow.
- Student `PENDING` login duoc de vao man hinh hoan thien ho so.
- Duyet `APPROVED` dua tren do day du profile + student card, khong dua tren verify-email.
- Social login (Google/GitHub) duoc dong bo cung 1 flow voi register thuong.

### Muc tieu nghiep vu
- Giam friction luc dang ky.
- Don gian hoa FE flow.
- Ep user hoan thien profile theo dung nghiep vu truoc khi vao nghiep vu chinh.
- Dam bao Coordinator van la gate approve cuoi cung.

---

## 2) API contract FE can biet

## 2.1 Register toi gian
- **Endpoint:** `POST /api/v1/auth/register`
- **Request:**
```json
{
  "email": "student@example.com",
  "password": "Student@123",
  "confirmPassword": "Student@123"
}
```
- **Response (chinh):**
  - `userId`
  - `email`
  - `status = PENDING`
  - `message`
- **Khong con:** `devVerifyToken`, `devVerifyUrl`.

## 2.2 Login
- **Endpoint:** `POST /api/v1/auth/login`
- Student `PENDING` login duoc de hoan thien profile.
- Non-student `PENDING` van bi chan nhu rule cu.

## 2.3 Hoan thien profile
- **Endpoint:** `PATCH /api/v1/users/me`
- FE gui cac field onboarding:
  - `fullName`
  - `userType` (`INTERNAL` / `EXTERNAL`)
  - `studentCode`
  - `chapterId` (neu `INTERNAL`)
  - `institution` (neu `EXTERNAL`)
  - `phone` (neu co)
- **Luu y:** Email la field xem, khong cho sua trong flow onboarding.

## 2.4 Upload student card
- **Endpoint:** `POST /api/v1/users/me/student-card`
- `multipart/form-data`, field file = `file`.
- FE can cho user upload xong va hien state da co anh.

## 2.5 Coordinator approve
- **Endpoint:** `PATCH /api/v1/users/{userId}/status`
- Body:
```json
{ "status": "APPROVED" }
```
- BE chi approve khi profile student hop le/day du theo rule moi.

---

## 3) Rule FE can the hien dung UI/UX

- Sau register thanh cong: dieu huong ve login (khong verify-email).
- Sau login neu user la `STUDENT` va `PENDING`: bat buoc vao onboarding profile.
- Chua du profile/chưa upload student card: hien thong bao hoan thien ho so.
- Da gui ho so day du: hien trang thai "Cho Coordinator duyet".
- Khi status thanh `APPROVED`: mo nghiep vu chinh.

---

## 4) Happy cases FE can test

## Happy case A - Register thuong -> approve
1. `POST /auth/register` toi gian -> `201`, `status=PENDING`.
2. `POST /auth/login` bang tai khoan vua tao -> `200`.
3. `PATCH /users/me` dien day du profile hop le -> `200`.
4. `POST /users/me/student-card` upload anh -> `200`.
5. Coordinator approve user -> `200`, `status=APPROVED`.
6. Student login lai va vao nghiep vu chinh -> `200`.

## Happy case B - Social login user moi -> approve
1. Login bang Google/GitHub user moi -> account tao o `PENDING`, `UNSPECIFIED`.
2. User dang nhap duoc vao flow onboarding.
3. Dien profile + upload student card.
4. Coordinator approve.
5. User vao nghiep vu chinh sau approve.

---

## 5) Error cases FE can map nhanh

- Register:
  - `ACCOUNT_DUPLICATE_EMAIL`
  - `VALIDATION_FAILED` (vd confirm password khong khop)
- Login:
  - `INVALID_CREDENTIALS`
  - `ACCOUNT_PENDING` (cho role khong duoc login khi pending)
  - `REJECTED_NOT_ALLOWED_LOGIN`
- Profile:
  - `STUDENT_CODE_DUPLICATE`
  - `INVALID_CHAPTER`
  - `INSTITUTION_REQUIRED`
  - `STUDENT_CODE_REQUIRED`

---

## 6) Nhung thay doi BE da lam de ho tro flow moi

- Xoa verify-email endpoint va logic token verify-email.
- Xoa field verify trong register response va config verify lien quan.
- Chuyen register sang toi gian + set user onboarding state phu hop.
- Mo flow `PENDING STUDENT` login de cap nhat profile.
- Them upload student card va rule validate truoc approve.
- Dong bo social login voi register thuong ve cung onboarding path.
- Cap nhat unit test + integration test E2E cho flow moi.

---

## 7) Danh sach 34 files da thay doi (tham chieu cho team FE/QA)

### A. Config va docs
- `.env.example`
- `src/main/resources/application-dev.properties`
- `docs/mf02/01-auth-users.md`
- `docs/mf02/03-oauth-prep.md`
- `docs/mf02/04-test-data.md`
- `docs/mf02/fe-auth-integration.md`
- `pom.xml`

### B. Auth module
- `src/main/java/com/sealhackathon/api/auth/config/JwtProperties.java`
- `src/main/java/com/sealhackathon/api/auth/controller/AuthController.java`
- `src/main/java/com/sealhackathon/api/auth/dto/request/RegisterRequest.java`
- `src/main/java/com/sealhackathon/api/auth/dto/response/RegisterResponse.java`
- `src/main/java/com/sealhackathon/api/auth/service/AuthService.java`
- `src/main/java/com/sealhackathon/api/auth/service/JwtTokenService.java`
- `src/main/java/com/sealhackathon/api/auth/service/RegistrationService.java`
- `src/main/java/com/sealhackathon/api/auth/service/SocialAuthService.java`

### C. Common
- `src/main/java/com/sealhackathon/api/common/audit/AuditAction.java`
- `src/main/java/com/sealhackathon/api/common/exception/ErrorCode.java`

### D. Users module
- `src/main/java/com/sealhackathon/api/users/controller/UserController.java`
- `src/main/java/com/sealhackathon/api/users/controller/UserMeController.java`
- `src/main/java/com/sealhackathon/api/users/dto/request/PatchMeRequest.java`
- `src/main/java/com/sealhackathon/api/users/dto/response/UserDetailResponse.java`
- `src/main/java/com/sealhackathon/api/users/dto/response/UserResponse.java`
- `src/main/java/com/sealhackathon/api/users/entity/User.java`
- `src/main/java/com/sealhackathon/api/users/mapper/UserResponseMapper.java`
- `src/main/java/com/sealhackathon/api/users/repository/UserRepository.java`
- `src/main/java/com/sealhackathon/api/users/service/UserAdminService.java`
- `src/main/java/com/sealhackathon/api/users/service/impl/UserAdminServiceImpl.java`
- `src/main/java/com/sealhackathon/api/users/value_object/UserType.java`
- `src/main/java/com/sealhackathon/api/users/service/StudentCardStorageService.java` (new)

### E. Tests
- `src/test/java/com/sealhackathon/api/auth/controller/AuthControllerTest.java`
- `src/test/java/com/sealhackathon/api/auth/service/AuthServiceTest.java`
- `src/test/java/com/sealhackathon/api/auth/service/JwtTokenServiceTest.java`
- `src/test/java/com/sealhackathon/api/auth/service/RegistrationServiceTest.java`
- `src/test/java/com/sealhackathon/api/auth/integration/AuthOnboardingFlowIntegrationTest.java` (new)

---

## 8) One-line summary cho FE

Flow FE can lam dung theo BE hien tai:
`register toi gian -> login pending -> patch profile -> upload student card -> cho coordinator approve -> login approved`.
