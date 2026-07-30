# Student API catalog (portal + alias)

**Auth:** `Authorization: Bearer {accessToken}` · `@StudentOnly` (role STUDENT, status APPROVED)

## Endpoint cũ (giữ nguyên — FE tiếp tục dùng)

| FR | Method | Path |
|----|--------|------|
| U-07 | POST | `/teams` |
| U-08..13 | POST/PATCH | `/teams/{id}/invite`, `/members`, … |
| U-18 | POST | `/submissions` |
| U-30 | POST/GET | `/me/appeals`, `/me/appeals/evidence` |
| U-01..04 | — | `/auth/*`, `/users/me` |

## Portal mới (`/api/v1/me/*`)

### GET `/me/hackathons/browse?status=ONGOING`

**Response `data`:**

```json
[
  {
    "id": 1,
    "name": "SEAL Hackathon 2026",
    "status": "ONGOING",
    "registered": false
  }
]
```

### POST `/me/hackathons/{hackathonId}/register`

**Response:** `{ "success": true, "data": null, "message": "Registered" }`

### GET `/me/teams`

```json
[
  {
    "teamId": 10,
    "teamName": "Team Alpha",
    "hackathonId": 1,
    "trackId": 3,
    "trackName": "Track A",
    "lotteryStatus": "ASSIGNED"
  }
]
```

### GET `/me/rounds/current/deadline` (GĐ3)

```json
{
  "roundId": 12,
  "deadline": "2026-06-07T15:00:00"
}
```

### GET `/me/submission?teamId=&roundId=` (GĐ3)

```json
{
  "submissionId": 15,
  "roundId": 12,
  "repoUrl": "https://github.com/org/repo",
  "demoUrl": "https://demo.example.com",
  "slideUrl": "https://docs.google.com/presentation/d/abc",
  "status": "ON_TIME",
  "submittedAt": "2026-06-07T10:00:00"
}
```

### GET `/me/teams/{teamId}/submissions?roundId=` (GĐ3)

Danh sách bài nộp theo đội (cùng schema như trên, mảng).

### GET `/me/rounds/{roundId}/problem`

```json
{
  "roundId": 5,
  "problemStatement": "...",
  "problemUrl": "https://...",
  "released": true
}
```

### POST `/me/appeals/evidence` (Phase 10)

Upload minh chứng trước khi tạo đơn. **multipart/form-data** field `file`.

**Response `data`:**

```json
{
  "storageKey": "appeals/u12/….png",
  "contentType": "image/png",
  "sizeBytes": 204800
}
```

### POST `/me/appeals`

Leader only · đội manual DQ (`ELIMINATED` + `eliminationReason`) · trong cửa sổ `appeal_window_ends_at` · ≥1 evidence · unique `(teamId, roundId)`.

**Request:**

```json
{
  "teamId": 10,
  "roundId": 5,
  "reason": "DQ không đúng quy trình…",
  "evidences": [
    {
      "url": "appeals/u12/….png",
      "type": "IMAGE",
      "caption": "Ảnh biên bản",
      "displayOrder": 0
    },
    {
      "url": "https://drive.example/clip",
      "type": "LINK",
      "displayOrder": 1
    }
  ]
}
```

`evidenceUrl` (legacy single URL) vẫn được chấp nhận nếu `evidences` trống — hệ thống suy ra `type` và lưu vào `appeal_evidences`.

**Response `data`:**

```json
{
  "id": 1,
  "teamId": 10,
  "teamName": "Team Alpha",
  "roundId": 5,
  "roundName": "Sơ loại",
  "reason": "DQ không đúng quy trình…",
  "evidenceUrl": "appeals/u12/….png",
  "evidences": [
    {
      "id": 1,
      "url": "appeals/u12/….png",
      "type": "IMAGE",
      "caption": "Ảnh biên bản",
      "displayOrder": 0
    }
  ],
  "status": "PENDING",
  "decisionNote": null,
  "reviewedById": null,
  "reviewedAt": null,
  "createdAt": "2026-07-31T10:00:00",
  "updatedAt": "2026-07-31T10:00:00",
  "version": 0
}
```

`status` ∈ `PENDING` | `UNDER_REVIEW` | `APPROVED` | `REJECTED` | `EXPIRED`.

### GET `/me/appeals`

Danh sách đơn khiếu nại của các đội mà user đang là thành viên ACCEPTED (mới nhất trước). Schema phần tử giống response `POST /me/appeals`.

### GET `/me/annual-awards?year=2025` (FR-U-32)

```json
[
  {
    "year": 2025,
    "awardName": "Best Innovator",
    "category": "INDIVIDUAL"
  }
]
```

Xem thêm route trong Swagger tag **Student Portal**.
