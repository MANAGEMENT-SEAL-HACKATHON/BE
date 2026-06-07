# API Catalog — Request / Response JSON

**Playbook E2E:** [full-workflow-api-test-gd1-gd6.md](full-workflow-api-test-gd1-gd6.md)

Envelope 2xx: `{ success, data, message?, traceId, timestamp }` — JSON dưới là **`data`**.

---

## GĐ0 — System & Health

### 0.001 `GET /`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---


## GĐ0 — Auth & Users

### 0.001 `POST /api/v1/auth/change-password`

**Request:**
```json
{
  "oldPassword": "password12",
  "newPassword": "NewPass@123",
  "confirmPassword": "NewPass@123"
}
```

**Response `data`:** `null`

---

### 0.002 `POST /api/v1/auth/forgot-password`

**Request:**
```json
{
  "email": "user@fpt.edu.vn"
}
```

**Response `data`:** `null`

---

### 0.003 `POST /api/v1/auth/login`

**Request:**
```json
{
  "email": "coord@fpt.edu.vn",
  "password": "Coordinator@dev1"
}
```

**Response `data`:**
```json
{
  "accessToken": "eyJ...",
  "refreshToken": "...",
  "tokenType": "Bearer",
  "expiresInSeconds": 1800,
  "mustChangePassword": false
}
```

---

### 0.004 `POST /api/v1/auth/logout`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

### 0.005 `POST /api/v1/auth/logout-all`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

### 0.006 `POST /api/v1/auth/oauth/github/code`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

### 0.007 `POST /api/v1/auth/oauth/github/link/code`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

### 0.008 `POST /api/v1/auth/oauth/github/unlink`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

### 0.009 `POST /api/v1/auth/oauth/google`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

### 0.010 `POST /api/v1/auth/oauth/google/link`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

### 0.011 `POST /api/v1/auth/oauth/google/unlink`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

### 0.012 `POST /api/v1/auth/refresh`

**Request:**
```json
{
  "refreshToken": "{{refreshToken}}"
}
```

**Response `data`:**
```json
{
  "accessToken": "eyJ...",
  "refreshToken": "new-refresh...",
  "expiresInSeconds": 1800
}
```

---

### 0.013 `POST /api/v1/auth/register`

**Request:**
```json
{
  "email": "sv@fpt.edu.vn",
  "password": "password12",
  "confirmPassword": "password12"
}
```

**Response `data`:**
```json
{
  "userId": 42,
  "email": "sv@fpt.edu.vn",
  "status": "PENDING",
  "message": "Đăng ký thành công."
}
```

---

### 0.014 `POST /api/v1/auth/reset-password`

**Request:**
```json
{
  "token": "reset-token-from-email",
  "newPassword": "NewPass@123",
  "confirmPassword": "NewPass@123"
}
```

**Response `data`:** `null`

---

### 0.015 `POST /api/v1/invitations/{id}/resend`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

### 0.016 `GET /api/v1/users`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---

### 0.017 `POST /api/v1/users`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

### 0.018 `GET /api/v1/users/me`

**Request:** *(không body)*

**Response `data`:**
```json
{
  "id": 42,
  "email": "sv@fpt.edu.vn",
  "fullName": "Nguyen Van A",
  "role": "STUDENT",
  "status": "APPROVED"
}
```

---

### 0.019 `PATCH /api/v1/users/me`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

### 0.020 `GET /api/v1/users/me/student-card`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---

### 0.021 `GET /api/v1/users/temp-judges`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---

### 0.022 `POST /api/v1/users/temp-judges`

**Request:**
```json
{
  "email": "guest.judge@company.com",
  "fullName": "Guest Judge",
  "organization": "ACME"
}
```

**Response `data`:**
```json
{
  "userId": 8,
  "email": "guest.judge@company.com",
  "invitationId": 3
}
```

---

### 0.023 `GET /api/v1/users/{judgeId}/round-assignments`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---

### 0.024 `GET /api/v1/users/{mentorId}/track-assignments`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---

### 0.025 `GET /api/v1/users/{userId}`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---

### 0.026 `PATCH /api/v1/users/{userId}`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

### 0.027 `PATCH /api/v1/users/{userId}/status`

**Request:**
```json
{
  "status": "APPROVED"
}
```

**Response `data`:**
```json
{
  "id": 42,
  "status": "APPROVED"
}
```

---

### 0.028 `GET /api/v1/users/{userId}/student-card`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---


## GĐ1 — Chuẩn bị sự kiện

### 1.001 `DELETE /api/v1/criteria/{id}`

**Request:** *(không body)*

**Response `data`:** `null`

---

### 1.002 `GET /api/v1/criteria/{id}`

**Request:** *(không body)*

**Response `data`:**
```json
{
  "id": 1,
  "name": "..."
}
```

---

### 1.003 `PUT /api/v1/criteria/{id}`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

### 1.004 `DELETE /api/v1/events/{id}`

**Request:** *(không body)*

**Response `data`:** `null`

---

### 1.005 `GET /api/v1/events/{id}`

**Request:** *(không body)*

**Response `data`:**
```json
{
  "id": 1,
  "name": "..."
}
```

---

### 1.006 `PUT /api/v1/events/{id}`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

### 1.007 `GET /api/v1/hackathons`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---

### 1.008 `POST /api/v1/hackathons`

**Request:**
```json
{
  "name": "SEAL Test",
  "slug": "seal-test-2026",
  "season": "Spring",
  "year": 2026,
  "registrationStart": "2026-05-01",
  "registrationEnd": "2026-06-01",
  "eventStart": "2026-06-02",
  "eventEnd": "2026-07-17",
  "wildcardEnabled": true,
  "individualRankingEnabled": false
}
```

**Response `data`:**
```json
{
  "id": 1,
  "name": "SEAL Test",
  "slug": "seal-test-2026",
  "status": "DRAFT",
  "season": "Spring",
  "year": 2026
}
```

---

### 1.009 `GET /api/v1/hackathons/active`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---

### 1.010 `GET /api/v1/hackathons/{hackathonId}/events`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---

### 1.011 `POST /api/v1/hackathons/{hackathonId}/events`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

### 1.012 `PATCH /api/v1/hackathons/{hackathonId}/lottery`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

### 1.013 `GET /api/v1/hackathons/{hackathonId}/tracks`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---

### 1.014 `DELETE /api/v1/hackathons/{id}`

**Request:** *(không body)*

**Response `data`:** `null`

---

### 1.015 `GET /api/v1/hackathons/{id}`

**Request:** *(không body)*

**Response `data`:**
```json
{
  "id": 1,
  "name": "..."
}
```

---

### 1.016 `PUT /api/v1/hackathons/{id}`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

### 1.017 `GET /api/v1/hackathons/{id}/chapter-rankings`

**Request:** *(không body)*

**Response `data`:**
```json
{
  "id": 1,
  "name": "..."
}
```

---

### 1.018 `GET /api/v1/hackathons/{id}/individual-rankings`

**Request:** *(không body)*

**Response `data`:**
```json
{
  "id": 1,
  "name": "..."
}
```

---

### 1.019 `GET /api/v1/hackathons/{id}/readiness`

**Request:** *(không body)*

**Response `data`:**
```json
{
  "id": 1,
  "name": "..."
}
```

---

### 1.020 `PATCH /api/v1/hackathons/{id}/status`

**Request:**
```json
{
  "status": "ONGOING"
}
```

**Response `data`:**
```json
{
  "id": 1,
  "status": "ONGOING"
}
```

---

### 1.021 `POST /api/v1/judge-assignments`

**Request:**
```json
{
  "judgeId": 3,
  "trackId": 5,
  "assignmentType": "NORMAL"
}
```

**Response `data`:**
```json
{
  "id": 20,
  "judgeId": 3,
  "trackId": 5,
  "assignmentType": "NORMAL"
}
```

---

### 1.022 `DELETE /api/v1/judge-assignments/{id}`

**Request:** *(không body)*

**Response `data`:** `null`

---

### 1.023 `POST /api/v1/mentor-assignments`

**Request:**
```json
{
  "mentorId": 4,
  "trackId": 5
}
```

**Response `data`:**
```json
{
  "id": 15,
  "mentorId": 4,
  "trackId": 5
}
```

---

### 1.024 `DELETE /api/v1/mentor-assignments/{id}`

**Request:** *(không body)*

**Response `data`:** `null`

---

### 1.025 `DELETE /api/v1/tracks/{id}`

**Request:** *(không body)*

**Response `data`:** `null`

---

### 1.026 `GET /api/v1/tracks/{id}`

**Request:** *(không body)*

**Response `data`:**
```json
{
  "id": 1,
  "name": "..."
}
```

---

### 1.027 `PUT /api/v1/tracks/{id}`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

### 1.028 `GET /api/v1/tracks/{trackId}/criteria`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---

### 1.029 `POST /api/v1/tracks/{trackId}/criteria`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

### 1.030 `POST /api/v1/tracks/{trackId}/criteria/batch`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

### 1.031 `POST /api/v1/tracks/{trackId}/criteria/clone`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

### 1.032 `GET /api/v1/tracks/{trackId}/criteria/clone-sources`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---

### 1.033 `GET /api/v1/tracks/{trackId}/criteria/weight-summary`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---

### 1.034 `GET /api/v1/tracks/{trackId}/judges`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---

### 1.035 `GET /api/v1/tracks/{trackId}/mentors`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---


## GĐ2 — Đăng ký & Đội

### 2.001 `GET /api/v1/teams`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---

### 2.002 `POST /api/v1/teams`

**Request:**
```json
{
  "hackathonId": 1,
  "teamName": "Team Alpha"
}
```

**Response `data`:**
```json
{
  "id": 10,
  "teamName": "Team Alpha",
  "status": "PENDING",
  "hackathonId": 1
}
```

---

### 2.003 `POST /api/v1/teams/bulk-approve`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

### 2.004 `DELETE /api/v1/teams/{teamId}`

**Request:** *(không body)*

**Response `data`:** `null`

---

### 2.005 `GET /api/v1/teams/{teamId}`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---

### 2.006 `PATCH /api/v1/teams/{teamId}/approve`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

### 2.007 `PATCH /api/v1/teams/{teamId}/eliminate`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

### 2.008 `GET /api/v1/teams/{teamId}/journey`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---

### 2.009 `POST /api/v1/teams/{teamId}/members/invite`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

### 2.010 `DELETE /api/v1/teams/{teamId}/members/{userId}`

**Request:** *(không body)*

**Response `data`:** `null`

---

### 2.011 `PATCH /api/v1/teams/{teamId}/members/{userId}`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

### 2.012 `GET /api/v1/teams/{teamId}/mentors`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---

### 2.013 `DELETE /api/v1/teams/{teamId}/rounds/{roundId}/mentor`

**Request:** *(không body)*

**Response `data`:** `null`

---

### 2.014 `POST /api/v1/teams/{teamId}/rounds/{roundId}/mentor`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

### 2.015 `PATCH /api/v1/teams/{teamId}/rounds/{roundId}/track`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

### 2.016 `PATCH /api/v1/teams/{teamId}/status`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

### 2.017 `PATCH /api/v1/teams/{teamId}/transfer-leader`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---


## GĐ3/GĐ5 — Thi, nộp bài & Chấm

### 3.001 `GET /api/v1/calibration-sessions`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---

### 3.002 `POST /api/v1/calibration-sessions`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

### 3.003 `PATCH /api/v1/calibration-sessions/{id}`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---


## GĐ3/GĐ5 — Vòng thi (rounds)

### 3.001 `GET /api/v1/hackathons/{hackathonId}/rounds`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---

### 3.002 `POST /api/v1/hackathons/{hackathonId}/rounds`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

### 3.003 `DELETE /api/v1/rounds/{id}`

**Request:** *(không body)*

**Response `data`:** `null`

---

### 3.004 `GET /api/v1/rounds/{id}`

**Request:** *(không body)*

**Response `data`:**
```json
{
  "id": 1,
  "name": "..."
}
```

---

### 3.005 `PUT /api/v1/rounds/{id}`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

### 3.006 `PATCH /api/v1/rounds/{id}/activate`

**Request:**
```json
{
  "note": "Kích hoạt Sơ loại"
}
```

**Response `data`:**
```json
{
  "id": 3,
  "isActive": true
}
```

---

### 3.007 `POST /api/v1/rounds/{id}/judge-assignments`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

### 3.008 `PATCH /api/v1/rounds/{id}/lock-scoring`

**Request:**
```json
{
  "force": false
}
```

**Response `data`:**
```json
{
  "id": 3,
  "scoringLocked": true
}
```

---

### 3.009 `GET /api/v1/rounds/{id}/ranking`

**Request:** *(không body)*

**Response `data`:**
```json
{
  "id": 1,
  "name": "..."
}
```

---

### 3.010 `GET /api/v1/rounds/{id}/ranking/preview`

**Request:** *(không body)*

**Response `data`:**
```json
{
  "id": 1,
  "name": "..."
}
```

---


## GĐ3/GĐ5 — Thi, nộp bài & Chấm

### 3.001 `GET /api/v1/rounds/{id}/rbl/progress`

**Request:** *(không body)*

**Response `data`:**
```json
{
  "id": 1,
  "name": "..."
}
```

---

### 3.002 `GET /api/v1/rounds/{id}/rbl/variance`

**Request:** *(không body)*

**Response `data`:**
```json
{
  "id": 1,
  "name": "..."
}
```

---


## GĐ3/GĐ5 — Vòng thi (rounds)

### 3.001 `PATCH /api/v1/rounds/{id}/release-problem`

**Request:**
```json
{
  "problemStatementUrl": "https://example.com/de.pdf"
}
```

**Response `data`:**
```json
{
  "id": 3,
  "problemReleasedAt": "2026-05-29T07:00:00"
}
```

---

### 3.002 `GET /api/v1/rounds/{id}/scoreboard`

**Request:** *(không body)*

**Response `data`:**
```json
{
  "id": 1,
  "name": "..."
}
```

---

### 3.003 `GET /api/v1/rounds/{id}/scoring-progress`

**Request:** *(không body)*

**Response `data`:**
```json
{
  "id": 1,
  "name": "..."
}
```

---

### 3.004 `GET /api/v1/rounds/{roundId}/criteria`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---

### 3.005 `POST /api/v1/rounds/{roundId}/criteria`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

### 3.006 `POST /api/v1/rounds/{roundId}/criteria/batch`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

### 3.007 `POST /api/v1/rounds/{roundId}/criteria/clone`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

### 3.008 `GET /api/v1/rounds/{roundId}/criteria/weight-summary`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---

### 3.009 `GET /api/v1/rounds/{roundId}/judges`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---

### 3.010 `GET /api/v1/rounds/{roundId}/tracks`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---

### 3.011 `POST /api/v1/rounds/{roundId}/tracks`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---


## GĐ3/GĐ5 — Thi, nộp bài & Chấm

### 3.001 `POST /api/v1/scores`

**Request:**
```json
{
  "submissionId": 7,
  "criterionId": 1,
  "scoreValue": 8.5,
  "scoreType": "NORMAL",
  "comment": "Good"
}
```

**Response `data`:**
```json
{
  "id": 100,
  "submissionId": 7,
  "criterionId": 1,
  "scoreValue": 8.5
}
```

---

### 3.002 `POST /api/v1/scores/calibration`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

### 3.003 `GET /api/v1/submissions`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---

### 3.004 `POST /api/v1/submissions`

**Request:**
```json
{
  "teamId": 10,
  "trackId": 5,
  "repoUrl": "https://github.com/o/r",
  "demoUrl": "https://d.example.com",
  "slideUrl": "https://s.example.com"
}
```

**Response `data`:**
```json
{
  "id": 7,
  "teamId": 10,
  "trackId": 5,
  "status": "SUBMITTED"
}
```

---

### 3.005 `PATCH /api/v1/submissions/{id}/resubmit`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

### 3.006 `PATCH /api/v1/submissions/{id}/review`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

### 3.007 `PATCH /api/v1/submissions/{id}/review-late`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---


## GĐ4 — Chuyển vòng & Publish

### 4.001 `POST /api/v1/rounds/{id}/advance`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

### 4.002 `POST /api/v1/rounds/{id}/advance-teams`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

### 4.003 `PATCH /api/v1/rounds/{id}/publish`

**Request:**
```json
{
  "confirm": true
}
```

**Response `data`:**
```json
{
  "id": 3,
  "isPublished": true
}
```

---

### 4.004 `GET /api/v1/rounds/{id}/tiebreak`

**Request:** *(không body)*

**Response `data`:**
```json
{
  "id": 1,
  "name": "..."
}
```

---

### 4.005 `POST /api/v1/rounds/{id}/tiebreak/resolve`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

### 4.006 `GET /api/v1/rounds/{id}/wildcard-candidates`

**Request:** *(không body)*

**Response `data`:**
```json
{
  "id": 1,
  "name": "..."
}
```

---

### 4.007 `POST /api/v1/rounds/{id}/wildcard/approve`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

### 4.008 `GET /api/v1/rounds/{id}/wildcard/candidates`

**Request:** *(không body)*

**Response `data`:**
```json
{
  "id": 1,
  "name": "..."
}
```

---

### 4.009 `POST /api/v1/rounds/{id}/wildcard/reject`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

### 4.010 `PATCH /api/v1/wildcard-reviews/{id}`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---


## GĐ6 — Kết thúc & Trao giải

### 6.001 `GET /api/v1/export-jobs/{id}`

**Request:** *(không body)*

**Response `data`:**
```json
{
  "id": 1,
  "name": "..."
}
```

---

### 6.002 `GET /api/v1/export-jobs/{id}/download`

**Request:** *(không body)*

**Response `data`:**
```json
{
  "id": 1,
  "name": "..."
}
```

---

### 6.003 `GET /api/v1/hackathons/{hackathonId}/prizes`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---

### 6.004 `POST /api/v1/hackathons/{hackathonId}/prizes`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

### 6.005 `PATCH /api/v1/hackathons/{id}/confirm`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

### 6.006 `POST /api/v1/hackathons/{id}/export-jobs`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

### 6.007 `GET /api/v1/hackathons/{id}/team-rankings`

**Request:** *(không body)*

**Response `data`:**
```json
{
  "id": 1,
  "name": "..."
}
```

---

### 6.008 `DELETE /api/v1/prizes/{id}`

**Request:** *(không body)*

**Response `data`:** `null`

---


## GĐ7 — Portal /me (Student · Judge · Mentor)

### 7.001 `GET /api/v1/me/annual-awards`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---

### 7.002 `POST /api/v1/me/appeals`

**Request:**
```json
{
  "teamId": 10,
  "roundId": 3,
  "reason": "Kết quả chưa đúng",
  "evidenceUrl": "https://..."
}
```

**Response `data`:**
```json
{
  "id": 1,
  "status": "PENDING"
}
```

---

### 7.003 `GET /api/v1/me/certificates`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---

### 7.004 `GET /api/v1/me/certificates/{id}/download`

**Request:** *(không body)*

**Response `data`:**
```json
{
  "id": 1,
  "name": "..."
}
```

---

### 7.005 `GET /api/v1/me/hackathons/browse`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---

### 7.006 `GET /api/v1/me/hackathons/{hackathonId}/rankings`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---

### 7.007 `DELETE /api/v1/me/hackathons/{hackathonId}/register`

**Request:** *(không body)*

**Response `data`:** `null`

---

### 7.008 `POST /api/v1/me/hackathons/{hackathonId}/register`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

### 7.009 `GET /api/v1/me/history`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---

### 7.010 `GET /api/v1/me/judge-final-assignments`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---

### 7.011 `GET /api/v1/me/judge-history`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---

### 7.012 `GET /api/v1/me/judge-track-assignments`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---

### 7.013 `GET /api/v1/me/mentor-history`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---

### 7.014 `GET /api/v1/me/mentor-team-assignments`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---

### 7.015 `GET /api/v1/me/mentor-team-assignments/{teamId}/presentation-slot`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---

### 7.016 `GET /api/v1/me/mentor-track-assignments`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---

### 7.017 `GET /api/v1/me/mentor/hackathons/{hackathonId}/rankings`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---

### 7.018 `GET /api/v1/me/mentor/rounds/{roundId}/schedule`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---

### 7.019 `GET /api/v1/me/mentor/teams/{teamId}/scores`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---

### 7.020 `GET /api/v1/me/mentor/teams/{teamId}/submissions`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---

### 7.021 `GET /api/v1/me/notifications`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---

### 7.022 `PATCH /api/v1/me/notifications/read`

**Request:**
```json
{
  "notificationIds": [
    1,
    2,
    3
  ]
}
```

**Response `data`:** `null`

---

### 7.023 `GET /api/v1/me/prizes`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---

### 7.024 `GET /api/v1/me/rounds/{roundId}/leaderboard`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---

### 7.025 `GET /api/v1/me/rounds/{roundId}/problem`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---

### 7.026 `GET /api/v1/me/scores`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---

### 7.027 `PATCH /api/v1/me/scores/{id}/comment`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

### 7.028 `PATCH /api/v1/me/scoring-completion`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

### 7.029 `GET /api/v1/me/scoring-schedule`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---

### 7.030 `GET /api/v1/me/teams`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---

### 7.031 `PATCH /api/v1/me/teams/{teamId}/rounds/{roundId}/track`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---

### 7.032 `GET /api/v1/me/teams/{teamId}/submissions`

**Request:** *(không body)*

**Response `data`:**
```json
[]
```

---

### 7.033 `POST /api/v1/me/tiebreak-evaluations`

**Request:**
```json
{
  "roundId": 3,
  "orderedTeamIds": [
    10,
    12,
    8
  ]
}
```

**Response `data`:**
```json
{
  "roundId": 3,
  "orderedTeamIds": [
    10,
    12,
    8
  ],
  "status": "SUBMITTED"
}
```

---

### 7.034 `POST /api/v1/me/tracks/{trackId}/select`

**Request:**
```json
{}
```

**Response `data`:**
```json
{
  "id": 1
}
```

---
