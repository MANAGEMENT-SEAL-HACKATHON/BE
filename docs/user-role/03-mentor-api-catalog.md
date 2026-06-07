# Mentor API catalog (portal)

**Auth:** `@MentorOnly` (role MENTOR, APPROVED)

## Endpoint Coordinator (giữ nguyên)

| FR | Ghi chú |
|----|---------|
| M assign | Coordinator: `mentor_assignments`, `PATCH /teams/{id}/mentor` |
| M xem đội | `GET /teams/{id}` + `@ApprovedOnly` |

## Portal mới

### GET `/me/mentor/rounds` (GĐ3)

```json
[
  {
    "roundId": 12,
    "roundName": "Vòng Sơ loại",
    "status": "ACTIVE",
    "description": "...",
    "teamCount": 4,
    "teams": [{ "teamId": 41, "teamName": "Team Alpha" }]
  }
]
```

### GET `/me/mentor/rounds/{roundId}/assigned-teams` (GĐ3)

```json
{
  "roundName": "Vòng Sơ loại",
  "roundStatus": "ACTIVE",
  "teams": [
    {
      "teamId": 41,
      "teamName": "Team Alpha",
      "groupNumber": 1,
      "status": "ACTIVE",
      "presentationSchedule": "08:00 - 08:15 ngày 07/06",
      "location": "Online (Teams) - Phòng 2"
    }
  ]
}
```

### GET `/me/mentor-track-assignments`

```json
[
  {
    "assignmentId": 1,
    "trackId": 3,
    "trackName": "Track A"
  }
]
```

### GET `/me/mentor/teams/{teamId}/submissions`

```json
[
  {
    "submissionId": 7,
    "roundId": 5,
    "status": "SUBMITTED",
    "submittedAt": "2026-05-29T10:00:00"
  }
]
```

> Spec user flow: `GET /teams/{id}/submissions` — BE dùng `/me/mentor/teams/{teamId}/submissions` để không đụng `SubmissionController`.

### GET `/me/mentor/hackathons/{hackathonId}/rankings`

```json
{
  "hackathonId": 1,
  "teamRankings": [],
  "chapterRankings": []
}
```

### GET `/me/mentor/rounds/{roundId}/schedule` (FR-M-16)

```json
{
  "roundId": 9,
  "roundName": "Chung kết",
  "slots": [
    {
      "teamId": 10,
      "teamName": "Team Alpha",
      "startAt": "2026-05-29T14:00:00",
      "endAt": "2026-05-29T14:10:00"
    }
  ]
}
```

Query: `GET /me/mentor-team-assignments?roundId=`, `.../submissions?roundId=`, `.../scores?roundId=`, `GET /me/mentor-history?year=`.

Xem thêm Swagger tag **Mentor Portal**.
