# FE GĐ3 API mapping (Person B → BE canonical)

> Đối chiếu [`BE_API_Requirements_PersonB.md`](../../../seal-hackathon-fe/src/docs/BE_API_Requirements_PersonB.md) với BE.  
> **Prefix BE:** `/api/v1/` · **Envelope:** `{ success, data, timestamp }` · **Fields:** camelCase trong `data`.

**Seed dev:** slug `seal-gd3-prelim-open` · mentor `mentor@fpt.edu.vn` · student GD3 `student.gd3.leader02@fpt.edu.vn` · coord `coord@fpt.edu.vn` · password seed: xem `GdExtendedSeedConstants.DEV_STUDENT_PASSWORD`.

---

## 1. Mentor Support

| FE doc | BE canonical | Ghi chú |
|--------|--------------|---------|
| `GET /api/mentor/rounds` | `GET /api/v1/me/mentor/rounds` | JWT mentorId — không cần path param |
| `GET /api/mentor/{id}/assigned-teams?roundId=` | `GET /api/v1/me/mentor/rounds/{roundId}/assigned-teams` | Enriched: `groupNumber`, `presentationSchedule`, `location` |

**Response `data` — rounds (mẫu):**

```json
[
  {
    "roundId": 12,
    "roundName": "Vòng Sơ loại",
    "status": "ACTIVE",
    "description": "Vòng đấu loại trực tiếp...",
    "teamCount": 4,
    "teams": [{ "teamId": 41, "teamName": "GD3-01 SUBMITTED + scored" }]
  }
]
```

**Response `data` — assigned-teams (mẫu):**

```json
{
  "roundName": "Vòng Sơ loại",
  "roundStatus": "ACTIVE",
  "teams": [
    {
      "teamId": 41,
      "teamName": "GD3-01 SUBMITTED + scored",
      "groupNumber": 1,
      "status": "ACTIVE",
      "presentationSchedule": "08:00 - 08:15 ngày 07/06",
      "location": "Online (Teams) - Phòng 2"
    }
  ]
}
```

---

## 2. Student Submission

| FE doc | BE canonical | Ghi chú |
|--------|--------------|---------|
| `GET /api/student/{id}/submission` | `GET /api/v1/me/submission?teamId={teamId}&roundId={roundId}` | 404 nếu chưa nộp |
| `POST /api/student/{id}/submission` | `POST /api/v1/submissions` | Body: `teamId`, `trackId` (prelim), `repoUrl`, `demoUrl`, `slideUrl` |
| `GET /api/round/current/deadline` | `GET /api/v1/me/rounds/current/deadline` | Vòng prelim `isActive=true` của hackathon ONGOING |

**POST submit — Response `201` `data`:**

```json
{
  "id": 15,
  "teamId": 42,
  "trackId": 8,
  "status": "LATE_PENDING",
  "submittedAt": "2026-06-07T16:30:00"
}
```

**GET submission — Response `200` `data` (status map cho FE):**

| BE `status` | FE hiển thị |
|-------------|-------------|
| `SUBMITTED`, `LATE`, `LATE_APPROVED`, `ACCEPTED` | `ON_TIME` |
| `LATE_PENDING` | `LATE_PENDING` |
| `REJECTED` | `REJECTED` |

```json
{
  "submissionId": 15,
  "roundId": 12,
  "repoUrl": "https://github.com/org/repo",
  "demoUrl": "https://demo.example.com",
  "slideUrl": "https://docs.google.com/presentation/d/abc",
  "status": "LATE_PENDING",
  "submittedAt": "2026-06-07T16:30:00"
}
```

**Deadline:**

```json
{
  "roundId": 12,
  "deadline": "2026-06-07T15:00:00"
}
```

**Validation:** `slideUrl` kết thúc `.pdf` → `400` code `INVALID_SLIDE_FORMAT`.  
**LATE_PENDING:** BE tự set sau `submissionDeadline` — FE không gửi flag.

---

## 3. Late Submission Review (Coordinator)

| FE doc | BE canonical |
|--------|--------------|
| `GET /api/submissions?status=LATE_PENDING` | `GET /api/v1/submissions?status=LATE_PENDING` |
| `PATCH /api/submissions/{id}/approve` | `PATCH /api/v1/submissions/{id}/approve` (alias) |
| `PATCH /api/submissions/{id}/reject` | `PATCH /api/v1/submissions/{id}/reject` body `{ "reason": "..." }` |

**Canonical (vẫn hỗ trợ):** `PATCH /api/v1/submissions/{id}/review-late` body `{ "decision": "APPROVE"|"REJECT", "note": "..." }`.

**List response `data[]` (rút gọn):**

```json
[
  {
    "id": 16,
    "teamId": 42,
    "teamName": "GD3-02 LATE_PENDING",
    "repoUrl": "https://github.com/seed/gd3-42",
    "slideUrl": "https://docs.google.com/presentation/d/seed-gd3-42",
    "status": "LATE_PENDING",
    "submittedAt": "2026-06-07T16:00:00"
  }
]
```

**Sau approve:** BE status `LATE_APPROVED` (FE có thể map `ON_TIME`).

---

## 4. Presentation Queue

| FE doc | BE canonical |
|--------|--------------|
| `GET /api/presentation/queue` | `GET /api/v1/presentation/queue?roundId=` |
| `PATCH /api/presentation/queue/next` | `PATCH /api/v1/presentation/queue/next?roundId=` body optional `{ "currentTeamId": 41 }` |

Trạng thái `WAITING` / `PRESENTING` / `DONE` / `ELIMINATED` lưu DB cột `presentation_slots.queue_status`.

```json
{
  "groups": [
    {
      "groupName": "Bảng A",
      "teams": [
        {
          "teamId": 41,
          "teamName": "GD3-01 SUBMITTED + scored",
          "order": 1,
          "status": "PRESENTING",
          "presentationSchedule": "08:00 - 08:15 ngày 07/06",
          "location": "Online (Teams) - Phòng 2"
        }
      ]
    }
  ],
  "roomStats": { "total": 4, "done": 0, "absent": 0 }
}
```

---

## 5. Trả lời câu hỏi FE (§5)

| # | Câu hỏi | Trả lời BE |
|---|---------|------------|
| 1 | `studentId` trong JWT? | Claim `sub` hoặc `userId` → `CurrentUserAccessor.currentUserId()` |
| 2 | `mentorId` trong JWT? | Cùng claim `sub`/`userId` |
| 3 | Role trong JWT? | Claim `role` → `UserRole` (STUDENT, MENTOR, COORDINATOR, JUDGE) |
| 4 | `PRESENTING` lưu DB? | Có — `presentation_slots.queue_status` |
| 5 | LATE_PENDING ai set? | BE tự động khi nộp sau deadline (prelim `ALLOW_LATE_PENDING`) |
| 6 | Mentor stats endpoint? | Ngoài phạm vi GĐ3 — backlog |
| 7 | slide PDF reject? | Có — `INVALID_SLIDE_FORMAT` |

---

## 6. Error format

FE doc `{ error, message, timestamp }` — BE dùng:

```json
{
  "success": false,
  "error": {
    "code": "INVALID_SLIDE_FORMAT",
    "message": "slideUrl không chấp nhận file PDF...",
    "status": 400
  },
  "timestamp": "2026-06-07T10:00:00Z"
}
```

HTTP status: 200/201 success · 400 validation · 401/403 auth · 404 not found · 409 conflict · 500 server.

---

## Postman variables (GĐ3 portal)

| Variable | Nguồn |
|----------|--------|
| `gd3HackathonSlug` | `seal-gd3-prelim-open` |
| `prelimRoundId` | Login coord → hackathon rounds hoặc seed log |
| `gd3TeamId` | `GET /me/teams` (student) hoặc mentor assigned-teams |
| `lateSubmissionId` | `GET /submissions?status=LATE_PENDING` |
