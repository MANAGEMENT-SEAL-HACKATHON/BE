# MF-01 — Security Contract (chưa wire SecurityFilterChain)

## 1. Phạm vi

MF-01 thiết kế authorization annotation **theo spec**, nhưng KHÔNG cấu hình `SecurityFilterChain` trong scope phase này. Module Auth (do team khác làm sau) sẽ:
1. Cấu hình `SecurityFilterChain` (HTTP filter, CORS, CSRF disable for stateless).
2. Triển khai JWT filter giải mã token và set `Authentication` vào `SecurityContextHolder`.
3. Bật `@EnableMethodSecurity` để kích hoạt `@PreAuthorize`.
4. Cung cấp impl `CurrentUserAccessor` đọc principal từ `SecurityContextHolder`.

Trước khi Auth module ra mắt, mọi endpoint ở MF-01 **mở** ở runtime (vì chưa có filter), nhưng đã **đánh dấu** ý định kiểm soát bằng meta-annotation `@CoordinatorOnly`.

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

Mapping → [`CurrentUserStub`](../../../src/main/java/com/se194093/be/common/security/CurrentUserStub.java):
| Claim | Field |
|---|---|
| `sub` hoặc `userId` | `userId` |
| `email` | `email` |
| `role` | `role` (enum `UserRole`) |
| `status` | `status` (enum `UserStatus`) |
| `userType` | `userType` (enum `UserType`) |
| `isTempAccount` | `isTempAccount` |

## 4. Meta-annotation `@CoordinatorOnly`

File: [common/security/CoordinatorOnly.java](../../../src/main/java/com/se194093/be/common/security/CoordinatorOnly.java).

### 4.1 Trạng thái hiện tại
Pure marker annotation — chỉ document intent. Không enforce gì ở runtime.

### 4.2 Cách Auth module nâng cấp

Chỉ cần đổi thân annotation, KHÔNG cần sửa từng controller:

```java
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@org.springframework.security.access.prepost.PreAuthorize(
    "hasRole('COORDINATOR') and authentication.principal.status == 'APPROVED'"
)
public @interface CoordinatorOnly {}
```

Sau đó thêm dependency `spring-boot-starter-security` + cấu hình:
```java
@EnableMethodSecurity
public class SecurityConfig { ... }
```

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
