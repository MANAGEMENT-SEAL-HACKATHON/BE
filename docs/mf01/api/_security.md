# MF-01 — Security Contract (**đã wire** MF-02 JWT)

## 1. Phạm vi

**MF-02 (GĐ2)** đã triển khai:

1. `SecurityFilterChain` stateless + CORS + CSRF off (`SecurityConfig`).
2. `JwtAuthenticationFilter` → `SealAuthentication` / `CurrentUserStub` trong `SecurityContextHolder`.
3. `@EnableMethodSecurity` + `@CoordinatorOnly` / `@ApprovedOnly` (`@PreAuthorize`).
4. `JwtCurrentUserAccessor` (`security.jwt.enabled=true`, mặc định).

Runbook đầy đủ: **[01-auth-users.md](../../mf02/01-auth-users.md)**.

Dev tắt JWT: `security.jwt.enabled=false` → `StubCurrentUserAccessor` + `DevStubAuthenticationFilter`.

## 2. Coordinator quyền CỐ ĐỊNH

> Theo MF-01 v2.2 §1.1: `users.role = COORDINATOR` AND `users.status = APPROVED`. Coordinator có quyền trên MỌI Hackathon trong hệ thống.

## 3. JWT claim contract (mà Auth module phải cung cấp)

```json
{
  "sub":   "42",
  "email": "alice@fpt.edu.vn",
  "role":  "COORDINATOR",
  "status": "APPROVED",
  "userType": "INTERNAL",
  "isTempAccount": false,
  "iat": 1747380000,
  "exp": 1747416000
}
```

Mapping → [`CurrentUserStub`](../../../src/main/java/com/sealhackathon/api/common/security/CurrentUserStub.java):
| Claim | Field |
|---|---|
| `sub` hoặc `userId` | `userId` |
| `email` | `email` |
| `role` | `role` (enum `UserRole`) |
| `status` | `status` (enum `UserStatus`) |
| `userType` | `userType` (enum `UserType`) |
| `isTempAccount` | `isTempAccount` |

## 4. Meta-annotation `@CoordinatorOnly`

File: [common/security/CoordinatorOnly.java](../../../src/main/java/com/sealhackathon/api/common/security/CoordinatorOnly.java).

### 4.1 Trạng thái hiện tại

`@PreAuthorize("hasRole('COORDINATOR') and authentication.principal.status.name() == 'APPROVED'")` — enforce runtime.

Profile `@ApprovedOnly`: mọi role nhưng `status=APPROVED` (vd `GET /users/me`).

## 5. Cách lấy current user trong Service

```java
@Service
public class HackathonServiceImpl implements HackathonService {
    private final CurrentUserAccessor currentUser;
    
    public HackathonResponse create(CreateHackathonRequest req) {
        Integer creatorId = currentUser.currentUserId();
        // ... gán Hackathon.createdBy = creatorId
    }
}
```

`CurrentUserAccessor` có 2 impl:
- `StubCurrentUserAccessor` (hiện tại) — luôn trả Coordinator id=1.
- `JwtCurrentUserAccessor` (Auth module sẽ làm) — đọc `SecurityContextHolder`.

## 6. 403 Forbidden response (sau khi wire Auth)

```json
{
  "success": false,
  "error": {
    "code":    "FORBIDDEN",
    "message": "Yêu cầu role COORDINATOR đã APPROVED",
    "status":  403
  }
}
```

Module Auth phải `ExceptionHandler` cho `AccessDeniedException` → trả format trên (đồng nhất với `GlobalExceptionHandler` hiện có).

## 7. Endpoint phân loại

| Loại | Áp dụng | Endpoint mẫu |
|---|---|---|
| **`@CoordinatorOnly`** | Toàn bộ 42 endpoint MF-01 | POST/PUT/DELETE/PATCH + mọi GET |

MF-01 chưa có endpoint public hoặc role khác. Khi sang MF-02 (đăng ký Student) mới có `STUDENT`-only endpoint.

## 8. Lưu ý cho QA

Vì chưa wire SecurityFilterChain, mọi endpoint hiện tại **không** yêu cầu Authorization header trong dev. Stub `CurrentUserAccessor` sẽ gắn `created_by = 1`. Khi test integration:
- Seed user id=1 với role=COORDINATOR, status=APPROVED trong DB.
- Hoặc khi Auth module ra, mock JWT trong test.
