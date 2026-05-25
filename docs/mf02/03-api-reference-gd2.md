# MF-02 GĐ2 — API Reference (Teams & Lottery) — cho FE

**Mục đích:** FE thiết kế UX/UI trước khi BE hoàn thiện logic. Mọi endpoint teams/lottery hiện trả **`501 NOT_IMPLEMENTED`** (khung + TODO) trừ khi ghi chú khác.

**Base:** `http://localhost:8080/api/v1`  
**Auth:** `Authorization: Bearer {{accessToken}}` (xem [01-auth-users.md](01-auth-users.md))  
**Envelope:** [mf01/api/_conventions.md](../mf01/api/_conventions.md)  
**JSON mẫu / Postman:** [05-test-data-gd2-teams.md](05-test-data-gd2-teams.md)

---

## Trạng thái implement

| Nhóm | Swagger tag | Logic |
|------|-------------|--------|
| Auth, Users, Temp judge | Auth, Users | ✅ |
| Teams FR-11…13C | **Teams (GĐ2)** | ⏳ 501 |
| Lottery FR-13B | Hackathons | ⏳ 501 |
| Cron lock FR-13A | — (nội bộ) | ⏳ no-op |

**501 mẫu:**

```json
{
  "success": false,
  "error": {
    "code": "NOT_IMPLEMENTED",
    "message": "API đã có khung; logic nghiệp vụ chưa implement (TODO)",
    "status": 501
  },
  "traceId": "..."
}
```

---

## Enum dùng trên FE

### `TeamStatus`

| Giá trị | UI gợi ý |
|---------|----------|
| `PENDING` | Vàng — Chờ BTC duyệt |
| `ACTIVE` | Xanh — Đang thi |
| `REJECTED` | Đỏ — Từ chối / Giải tán |
| `ELIMINATED` | Xám — Bị loại (GĐ sau) |

### `TeamMemberStatus` / `TeamMemberRole`

| Status | Ý nghĩa |
|--------|---------|
| `PENDING` | Lời mời chưa trả lời |
| `ACCEPTED` | Trong đội |
| `REJECTED` | Từ chối lời mời |
| `LEFT` | Đã rời |

| Role | Ý nghĩa |
|------|---------|
| `LEADER` | Trưởng nhóm |
| `MEMBER` | Thành viên |

### `TeamMemberAction` (PATCH member)

`ACCEPT` | `REJECT` | `LEFT`

---

## 1. Teams — CRUD & lifecycle

### 1.1 Tạo đội

```http
POST /teams
Authorization: Bearer <student-approved>
```

**Body**

```json
{
  "hackathonId": 1,
  "teamName": "Seal Warriors"
}
```

**201 — `data` (`TeamResponse`)**

```json
{
  "id": 10,
  "hackathonId": 1,
  "teamName": "Seal Warriors",
  "leaderId": 42,
  "chapterId": 1,
  "status": "PENDING",
  "isLocked": false,
  "createdAt": "2026-05-24T10:00:00"
}
```

**Header:** `Location: /api/v1/teams/10`

**FE:** wizard tạo đội → redirect `/teams/10` dashboard PENDING.

---

### 1.2 Danh sách đội

```http
GET /teams?hackathonId=1&status=PENDING
Authorization: Bearer <token>
```

**200 — `data`: `TeamDetailResponse[]`**

Coordinator: tất cả đội hackathon. Student: chỉ đội liên quan (khi implement).

**Query**

| Param | Bắt buộc | Mô tả |
|-------|----------|--------|
| `hackathonId` | ✅ | Lọc theo kỳ |
| `status` | ❌ | PENDING / ACTIVE / … |

---

### 1.3 Chi tiết đội

```http
GET /teams/{teamId}
```

**200 — `data` (`TeamDetailResponse`)**

```json
{
  "id": 10,
  "hackathonId": 1,
  "hackathonName": "SEAL Spring 2026",
  "teamName": "Seal Warriors",
  "leaderId": 42,
  "leaderName": "Nguyen Van A",
  "chapterId": 1,
  "status": "PENDING",
  "isLocked": false,
  "lockedAt": null,
  "rejectionReason": null,
  "createdAt": "2026-05-24T10:00:00",
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
      "email": "friend@gmail.com",
      "roleInTeam": "MEMBER",
      "status": "PENDING"
    }
  ]
}
```

**FE layout gợi ý**

```
┌─────────────────────────────────────┐
│ Seal Warriors          [PENDING]    │
│ Leader: Nguyen Van A                │
│ Members: 1/5 accepted · 2 pending   │
├─────────────────────────────────────┤
│ [Mời thành viên]  (disabled if lock)│
│ Table members + actions             │
└─────────────────────────────────────┘
```

---

### 1.4 Duyệt / từ chối đội

```http
PATCH /teams/{teamId}/status
Authorization: Bearer <coordinator>
```

**Duyệt**

```json
{ "status": "ACTIVE" }
```

**Từ chối**

```json
{
  "status": "REJECTED",
  "rejectionReason": "Thiếu thành viên đủ điều kiện"
}
```

**Shortcut duyệt**

```http
PATCH /teams/{teamId}/approve
```
(tương đương `status: ACTIVE`)

**Bulk (TODO)**

```http
POST /teams/bulk-approve
```

```json
{ "teamIds": [10, 11, 12] }
```

**200 — `BulkApproveTeamsResponse` (dự kiến)**

```json
{
  "approvedIds": [10, 11],
  "failed": [
    { "teamId": 12, "code": "TEAM_INVALID_MEMBER_COUNT", "message": "..." }
  ]
}
```

---

### 1.5 Transfer leader

```http
PATCH /teams/{teamId}/transfer-leader
```

```json
{ "newLeaderUserId": 43 }
```

---

### 1.6 Giải tán

```http
DELETE /teams/{teamId}
```

**200** + `message`: "Đội đã giải tán" (soft REJECTED khi implement).

---

## 2. Thành viên (FR-12)

### 2.1 Mời

```http
POST /teams/{teamId}/members/invite
```

```json
{ "email": "friend@gmail.com" }
```

**202** — đã nhận yêu cầu (async email TODO).

---

### 2.2 Phản hồi lời mời

```http
PATCH /teams/{teamId}/members/{userId}
```

```json
{ "action": "ACCEPT" }
```

`REJECT` | `LEFT` — cùng schema.

**FE:** notification bell → 2 nút Accept / Decline.

---

### 2.3 Hủy lời mời (Leader)

```http
DELETE /teams/{teamId}/members/{userId}
```

**204** — chỉ khi member `PENDING`.

---

## 3. Lottery & Track (FR-13B)

### 3.1 Batch bốc thăm

```http
PATCH /hackathons/{hackathonId}/lottery
Authorization: Bearer <coordinator>
```

```json
{
  "roundId": 2,
  "assignments": [
    { "teamId": 10, "trackId": 5, "assignedGroup": "A" },
    { "teamId": 11, "trackId": 6, "assignedGroup": "B" }
  ]
}
```

**200 — `HackathonLotteryResponse`**

```json
{
  "hackathonId": 1,
  "roundId": 2,
  "assignedCount": 2,
  "teamIds": [10, 11]
}
```

**FE:** màn Coordinator — grid đội ACTIVE × tracks, kéo thả hoặc auto-shuffle UI (logic client), submit một lần.

---

### 3.2 Re-lottery (đổi track)

```http
PATCH /teams/{teamId}/rounds/{roundId}/track
```

```json
{ "trackId": 7, "assignedGroup": "B" }
```

---

## 4. Mentor per-round (FR-13C)

### 4.1 Gán mentor

```http
POST /teams/{teamId}/rounds/{roundId}/mentor
```

```json
{ "mentorId": 8 }
```

**201**

---

### 4.2 Gỡ mentor

```http
DELETE /teams/{teamId}/rounds/{roundId}/mentor
```

**204**

---

### 4.3 Lịch sử

```http
GET /teams/{teamId}/mentors
```

**200**

```json
{
  "teamId": 10,
  "items": [
    {
      "roundId": 2,
      "roundName": "Vòng Sơ loại",
      "mentorId": 8,
      "mentorName": "Mentor Nguyen",
      "assignedAt": "2026-06-01T09:00:00"
    }
  ]
}
```

---

## Error codes

| Code | HTTP | Khi nào (tóm tắt) |
|------|------|-------------------|
| `NOT_IMPLEMENTED` | 501 | Khung API, chưa code logic |
| `TEAM_NAME_DUPLICATE` | 409 | Trùng tên trong hackathon |
| `USER_IN_ANOTHER_TEAM` | 409 | Đã ở đội khác |
| `REGISTRATION_CLOSED` | 422 | Quá hạn đăng ký |
| `HACKATHON_NOT_ONGOING` | 422 | Hackathon không ONGOING |
| `TEAM_LOCKED` | 403 | Đã khóa sau deadline |
| `TEAM_MEMBER_FULL` | 409 | Đủ 5 ACCEPTED |
| `TEAM_INVALID_MEMBER_COUNT` | 422 | Duyệt khi <3 hoặc >5 |
| `LEADER_CANNOT_LEAVE_TEAM` | 422 | Leader dùng LEFT |
| `TEAM_HAS_MENTOR_CANNOT_DISBAND` | 409 | Đã gán mentor |
| `ROUND_ALREADY_ACTIVE` | 423 | Re-lottery muộn |
| `ROUND_HAS_SCORES` | 409 | Không xóa mentor |
| `UNAUTHORIZED` / `FORBIDDEN` | 401/403 | JWT / role |

Danh sách đầy đủ: `ErrorCode.java` (MF-02 GĐ2 TEAMS block).

---

## Security annotations (BE)

| Annotation | Role |
|------------|------|
| `@StudentOnly` | `STUDENT` + `APPROVED` |
| `@ApprovedOnly` | Mọi role đã APPROVED |
| `@CoordinatorOnly` | `COORDINATOR` |

---

## Màn hình FE đề xuất (checklist)

| # | Route FE (gợi ý) | API chính |
|---|------------------|-----------|
| 1 | `/hackathons/:id/register/team/new` | POST `/teams` |
| 2 | `/teams/:id` | GET `/teams/:id` |
| 3 | `/teams/:id/members` | invite, patch, delete member |
| 4 | `/coordinator/hackathons/:id/teams` | GET `/teams?status=PENDING` |
| 5 | `/coordinator/hackathons/:id/lottery` | PATCH lottery |
| 6 | `/coordinator/teams/:id/mentors` | POST/DELETE/GET mentors |
| 7 | `/invitations` (student) | PATCH member ACCEPT |

---

## Tài liệu liên quan

- [02-mainflow-gd2.md](02-mainflow-gd2.md) — sequence nghiệp vụ
- [01-business-rules-gd2.md](01-business-rules-gd2.md) — rules chi tiết
- [05-test-data-gd2-teams.md](05-test-data-gd2-teams.md) — curl/Postman + response mẫu
