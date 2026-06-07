# Student API catalog (portal + alias)

**Auth:** `Authorization: Bearer {accessToken}` · `@StudentOnly` (role STUDENT, status APPROVED)

## Endpoint cũ (giữ nguyên — FE tiếp tục dùng)

| FR | Method | Path |
|----|--------|------|
| U-07 | POST | `/teams` |
| U-08..13 | POST/PATCH | `/teams/{id}/invite`, `/members`, … |
| U-18 | POST | `/submissions` |
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

### POST `/me/appeals`

**Request:**

```json
{
  "teamId": 10,
  "roundId": 5,
  "reason": "...",
  "evidenceUrl": "https://..."
}
```

**Response `data`:**

```json
{
  "id": null,
  "teamId": 10,
  "roundId": 5,
  "reason": "...",
  "evidenceUrl": "https://...",
  "status": "PENDING"
}
```

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
