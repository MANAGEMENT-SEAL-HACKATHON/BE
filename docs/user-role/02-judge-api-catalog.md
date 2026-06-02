# Judge API catalog (portal + alias)

**Auth:** `@JudgeOnly` (role JUDGE, APPROVED)

## Endpoint cũ (giữ nguyên)

| FR | Method | Path |
|----|--------|------|
| J scoring | POST | `/scores` |
| J calibration | POST | `/scores/calibration` |
| J submissions list | GET | `/submissions?roundId=` |

## Portal mới

### GET `/me/judge-track-assignments`

```json
[
  {
    "assignmentId": 1,
    "trackId": 3,
    "trackName": "Track A",
    "roundId": 5,
    "assignmentType": "HEAD",
    "completionStatus": "IN_PROGRESS"
  }
]
```

> `assignmentType` (FR-J-07): `NORMAL` | `HEAD`. `completionStatus` — DB `judge_assignments` chưa có cột (backlog).

Query tùy chọn: `GET /me/scoring-schedule?roundId=`, `GET /me/scores?roundId=`, `GET /me/judge-history?year=`.

### PATCH `/me/scores/{id}/comment`

**Request:** `{ "comment": "..." }`

**Response `data`:**

```json
{
  "scoreId": 42,
  "submissionId": null,
  "teamId": null,
  "totalScore": null,
  "comment": "..."
}
```

### POST `/me/tiebreak-evaluations`

**Request (spec):** `orderedTeamIds` — path BE thực tế (tránh đụng Coordinator).

```json
{
  "roundId": 5,
  "orderedTeamIds": [10, 12, 8]
}
```

**Response `data`:**

```json
{
  "roundId": 5,
  "orderedTeamIds": [10, 12, 8],
  "status": "SUBMITTED"
}
```

Xem thêm Swagger tag **Judge Portal**.
