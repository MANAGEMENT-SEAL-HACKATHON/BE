# MF-02 GĐ2 — Test data (Teams & Lottery)

Dùng song song với [03-api-reference-gd2.md](03-api-reference-gd2.md).  
**Lưu ý:** Các endpoint teams/lottery dưới đây hiện trả **`501 NOT_IMPLEMENTED`** cho đến khi implement service — FE vẫn mock UI theo JSON **kỳ vọng** bên dưới.

Auth / token: [04-test-data.md](04-test-data.md).

---

## 0. Biến Postman

| Biến | Gán từ |
|------|--------|
| `baseUrl` | `http://localhost:8080` |
| `accessTokenStudent` | Login student APPROVED |
| `accessTokenCoord` | `coord@fpt.edu.vn` |
| `hackathonId` | Seed GĐ1 (thường `1`) |
| `teamId` | Sau POST teams (khi implement) hoặc mock `10` |
| `roundId` | Round Sơ loại seed |
| `trackId` | Track seed |

---

## 1. Login (tiền đề)

### Student (sau khi register + approve)

```http
POST {{baseUrl}}/api/v1/auth/login
Content-Type: application/json
```

```json
{
  "email": "newstudent@gmail.com",
  "password": "Student@dev1"
}
```

Lưu `data.accessToken` → `accessTokenStudent`.

### Coordinator

```json
{
  "email": "coord@fpt.edu.vn",
  "password": "Coordinator@dev1"
}
```

---

## 2. Teams — kỳ vọng khi đã implement

### 2.1 Tạo đội — `POST /teams`

```http
POST {{baseUrl}}/api/v1/teams
Authorization: Bearer {{accessTokenStudent}}
Content-Type: application/json
```

```json
{
  "hackathonId": 1,
  "teamName": "Seal Warriors Dev"
}
```

**Hiện tại (khung):** `501`, body `NOT_IMPLEMENTED`.

**Kỳ vọng sau implement:** `201`

```json
{
  "success": true,
  "data": {
    "id": 10,
    "hackathonId": 1,
    "teamName": "Seal Warriors Dev",
    "leaderId": 42,
    "chapterId": 1,
    "status": "PENDING",
    "isLocked": false,
    "createdAt": "2026-05-24T14:30:00"
  },
  "message": null,
  "traceId": "..."
}
```

**Lỗi mẫu — trùng tên**

```json
{
  "success": false,
  "error": {
    "code": "TEAM_NAME_DUPLICATE",
    "message": "Tên đội đã tồn tại trong hackathon này",
    "status": 409
  }
}
```

---

### 2.2 Chi tiết đội — `GET /teams/10`

```http
GET {{baseUrl}}/api/v1/teams/10
Authorization: Bearer {{accessTokenStudent}}
```

**Kỳ vọng `200` (mock cho FE Storybook)**

```json
{
  "success": true,
  "data": {
    "id": 10,
    "hackathonId": 1,
    "hackathonName": "SEAL Hackathon Spring 2026",
    "teamName": "Seal Warriors Dev",
    "leaderId": 42,
    "leaderName": "Nguyen Van A",
    "chapterId": 1,
    "status": "PENDING",
    "isLocked": false,
    "lockedAt": null,
    "rejectionReason": null,
    "createdAt": "2026-05-24T14:30:00",
    "acceptedMemberCount": 1,
    "pendingInviteCount": 2,
    "members": [
      {
        "userId": 42,
        "fullName": "Nguyen Van A",
        "email": "student@fpt.edu.vn",
        "roleInTeam": "LEADER",
        "status": "ACCEPTED"
      },
      {
        "userId": 43,
        "fullName": "Tran Thi B",
        "email": "invite.b@gmail.com",
        "roleInTeam": "MEMBER",
        "status": "PENDING"
      },
      {
        "userId": 44,
        "fullName": "Le Van C",
        "email": "invite.c@gmail.com",
        "roleInTeam": "MEMBER",
        "status": "PENDING"
      }
    ]
  }
}
```

---

### 2.3 Coordinator — danh sách chờ duyệt

```http
GET {{baseUrl}}/api/v1/teams?hackathonId=1&status=PENDING
Authorization: Bearer {{accessTokenCoord}}
```

**Kỳ vọng `200`:** `data` là mảng 2–3 `TeamDetailResponse` (như trên).

---

### 2.4 Mời thành viên

```http
POST {{baseUrl}}/api/v1/teams/10/members/invite
Authorization: Bearer {{accessTokenStudent}}
```

```json
{ "email": "friend@gmail.com" }
```

**Kỳ vọng:** `202` + `message` "Đã gửi lời mời".

---

### 2.5 Invitee accept

```http
PATCH {{baseUrl}}/api/v1/teams/10/members/43
Authorization: Bearer {{accessTokenInvitee}}
```

```json
{ "action": "ACCEPT" }
```

**Kỳ vọng:** `200`, `acceptedMemberCount` tăng khi GET lại.

---

### 2.6 Duyệt đội

```http
PATCH {{baseUrl}}/api/v1/teams/10/status
Authorization: Bearer {{accessTokenCoord}}
```

```json
{ "status": "ACTIVE" }
```

**Lỗi — thiếu thành viên**

```json
{
  "success": false,
  "error": {
    "code": "TEAM_INVALID_MEMBER_COUNT",
    "message": "Đội cần từ 3 đến 5 thành viên đã chấp nhận",
    "status": 422,
    "details": { "accepted": 2, "min": 3, "max": 5 }
  }
}
```

*(chi tiết `details` có thể thay khi implement)*

---

### 2.7 Từ chối đội

```json
{
  "status": "REJECTED",
  "rejectionReason": "Không đủ thành viên FPT chapter"
}
```

---

## 3. Lottery — `PATCH /hackathons/1/lottery`

```http
PATCH {{baseUrl}}/api/v1/hackathons/1/lottery
Authorization: Bearer {{accessTokenCoord}}
```

```json
{
  "roundId": 2,
  "assignments": [
    { "teamId": 10, "trackId": 1, "assignedGroup": "A" },
    { "teamId": 11, "trackId": 2, "assignedGroup": "B" }
  ]
}
```

**Kỳ vọng `200`**

```json
{
  "success": true,
  "data": {
    "hackathonId": 1,
    "roundId": 2,
    "assignedCount": 2,
    "teamIds": [10, 11]
  }
}
```

---

## 4. Re-lottery track

```http
PATCH {{baseUrl}}/api/v1/teams/10/rounds/2/track
Authorization: Bearer {{accessTokenCoord}}
```

```json
{ "trackId": 3, "assignedGroup": "C" }
```

---

## 5. Mentor

### Gán

```http
POST {{baseUrl}}/api/v1/teams/10/rounds/2/mentor
Authorization: Bearer {{accessTokenCoord}}
```

```json
{ "mentorId": 5 }
```

### Lịch sử

```http
GET {{baseUrl}}/api/v1/teams/10/mentors
Authorization: Bearer {{accessTokenCoord}}
```

**Kỳ vọng `200`:** xem [03-api-reference-gd2.md §4.3](03-api-reference-gd2.md#43-lịch-sử).

---

## 6. Kịch bản E2E (manual checklist)

| # | Bước | Actor | API |
|---|------|-------|-----|
| 1 | Login student | Student | auth/login |
| 2 | Tạo đội | Student | POST `/teams` |
| 3 | Mời 2 người | Leader | POST invite ×2 |
| 4 | Accept cả 2 | Invitees | PATCH member ACCEPT |
| 5 | Duyệt đội | Coordinator | PATCH status ACTIVE |
| 6 | (Sau registration_end) Khóa | System | cron — `isLocked=true` |
| 7 | Bốc thăm | Coordinator | PATCH lottery |
| 8 | Gán mentor | Coordinator | POST mentor |

---

## 7. Mock JSON cho FE (copy vào `mocks/teams.json`)

File gộp 3 trạng thái UI:

```json
{
  "teamPending": {
    "id": 10,
    "teamName": "Seal Warriors",
    "status": "PENDING",
    "isLocked": false,
    "acceptedMemberCount": 1,
    "pendingInviteCount": 2,
    "members": []
  },
  "teamActive": {
    "id": 10,
    "teamName": "Seal Warriors",
    "status": "ACTIVE",
    "isLocked": false,
    "acceptedMemberCount": 4,
    "pendingInviteCount": 0
  },
  "teamLocked": {
    "id": 10,
    "teamName": "Seal Warriors",
    "status": "ACTIVE",
    "isLocked": true,
    "lockedAt": "2026-06-05T23:59:59",
    "acceptedMemberCount": 4
  }
}
```

---

## 8. Swagger

`http://localhost:8080/swagger-ui.html` → **Teams (GĐ2)**, **Hackathons** (lottery).

Thử endpoint → expect `501` + `NOT_IMPLEMENTED` (xác nhận route đúng).
