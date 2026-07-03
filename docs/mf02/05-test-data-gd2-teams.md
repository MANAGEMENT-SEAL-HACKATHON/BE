# MF-02 GĐ2 — Test data (Teams & Lottery)

Dùng song song với [03-api-reference-gd2.md](03-api-reference-gd2.md) và PDF `Bao_Cao_API_Data_Test_MF02_GD2.pdf`.  
Auth / token: [04-test-data.md](04-test-data.md).

**Profile dev:** `E2eWorkflowDataSeeder` tự chạy sau `Gd1DataSeeder` — mỗi bảng GĐ2 có **≥ 5 bản ghi đa dạng** (trạng thái, chapter, track, mentor…).

---

## 0. Cấu hình nhanh

| Mục | Giá trị |
|-----|---------|
| Base URL | `http://localhost:8080/api/v1` |
| Hackathon test | slug `seal-e2e-2026` (ONGOING) — lấy `hackathonId` qua `GET /hackathons?q=seal-e2e-2026` |
| Vòng bốc thăm | Round **Sơ loại** (`PRELIMINARY`) — `roundId` từ `GET /hackathons/{id}/rounds` |
| Track | `trackId` 1–2 (Track 1 RAG, Track 2 AI Agent) trên cùng round sơ loại |

### Đăng nhập mẫu

| Vai trò | Email | Password |
|---------|-------|----------|
| Coordinator | `coord@fpt.edu.vn` | `Coordinator@dev1` |
| Mentor | `mentor@fpt.edu.vn` | `Mentor@dev1` |
| Sinh viên GĐ2 (mọi tài khoản seed) | xem §1 | `Student@dev1` |

---

## 1. Bảng `users` — 30 sinh viên APPROVED

Tất cả: `role=STUDENT`, `status=APPROVED`, password **`Student@dev1`**.

| # | Email | userType | Chapter | Mục đích test |
|---|-------|----------|---------|----------------|
| 1 | `student.gd2.hcm.leader01@fpt.edu.vn` | INTERNAL | FPT-HCM | Leader đội 01 — duyệt fail (1 người) |
| 2 | `student.gd2.hn.leader02@fpt.edu.vn` | INTERNAL | FPT-HN | Leader đội 02 — 2 ACCEPTED + 1 PENDING |
| 3 | `student.gd2.hcm.member03@fpt.edu.vn` | INTERNAL | FPT-HCM | Member đội 02 |
| 4 | `student.gd2.hcm.member04@fpt.edu.vn` | INTERNAL | FPT-HCM | Member đội 02 |
| 5 | `student.gd2.ext.pending@gmail.com` | EXTERNAL | EXT | PENDING invite đội 02 |
| 6 | `student.gd2.hcm.leader03@fpt.edu.vn` | INTERNAL | FPT-HCM | Leader đội 03 — sẵn duyệt (4 người) |
| 7–9 | `member06`, `hn.member07`, `ext.member08` | mix | — | Member đội 03 |
| 10 | `student.gd2.ext.leader04@gmail.com` | EXTERNAL | EXT | Leader đội 04 — ACTIVE + lottery + mentor |
| 11–12 | `hcm.member10`, `hn.member11` | mix | — | Member đội 04 |
| 13 | `student.gd2.hcm.leader05@fpt.edu.vn` | INTERNAL | FPT-HCM | Leader đội 05 — **isLocked** |
| 14–16 | `member12`, `ext.member13`, `hn.member14` | mix | — | Member đội 05 (4 ACCEPTED) |
| 17 | `student.gd2.hcm.leader06@fpt.edu.vn` | INTERNAL | FPT-HCM | Leader đội 06 — **REJECTED** |
| 18–20 | `member15`, `ext.member16` | mix | — | Member đội 06 |
| 21 | `student.gd2.hcm.leader07@fpt.edu.vn` | INTERNAL | FPT-HCM | Leader đội 07 — ACTIVE, **chưa mentor** |
| 22–23 | `hn.member17`, `ext.member18` | mix | — | Member đội 07 |
| 24 | `student.gd2.hcm.leader08@fpt.edu.vn` | INTERNAL | FPT-HCM | Leader đội 08 — **ELIMINATED** |
| 25–26 | `member19`, `hn.member20` | mix | — | Member đội 08 |
| 27 | `student.gd2.ext.leader09@gmail.com` | EXTERNAL | EXT | Leader đội 09 — ACTIVE track 2 |
| 28–29 | `hcm.member21`, `ext.member22` | mix | — | Member đội 09 |
| 30 | `student.gd2.ext.member23@gmail.com` | EXTERNAL | EXT | **LEFT** đội 02 |
| — | `student.gd2.pool.free@gmail.com` | EXTERNAL | EXT | **Chưa có đội** — test mời |
| — | `student.gd2.pool.busy@gmail.com` | EXTERNAL | EXT | **ACCEPTED đội 04** — test `USER_IN_ANOTHER_TEAM` |

**SQL kiểm tra:**

```sql
SELECT id, email, user_type, status, institution
FROM users
WHERE email LIKE 'student.gd2.%' OR email LIKE 'student.gd2.pool.%'
ORDER BY id;
```

---

## 2. Bảng `teams` — 9 đội (≥ 5 trạng thái khác nhau)

Hackathon: **SEAL E2E 2026** (`seal-e2e-2026`). Tên đội prefix `E2E-T`.

| # | teamName | status | isLocked | accepted | Kịch bản |
|---|----------|--------|----------|----------|----------|
| 01 | `E2E-T01 Chờ duyệt (1 người)` | PENDING | false | 1 | `PATCH .../status` ACTIVE → `422 TEAM_INVALID_MEMBER_COUNT` |
| 02 | `E2E-T02 Chờ duyệt (2 ACCEPTED + 1 PENDING)` | PENDING | false | 2 | Thiếu người; có invite PENDING |
| 03 | `E2E-T03 Sẵn duyệt ACTIVE (4 người)` | PENDING | false | 4 | Coordinator duyệt OK → ACTIVE |
| 04 | `E2E-T04 ACTIVE + bốc thăm Track 1` | ACTIVE | false | 4 | Lottery bảng A; có mentor |
| 05 | `E2E-T05 ACTIVE đã khóa + bốc thăm` | ACTIVE | **true** | 4 | `TEAM_LOCKED` khi mời/sửa |
| 06 | `E2E-T06 REJECTED` | REJECTED | false | 3 | Có `rejectionReason` |
| 07 | `E2E-T07 ACTIVE chưa mentor (bốc thăm)` | ACTIVE | false | 3 | `POST .../mentor` |
| 08 | `E2E-T08 ELIMINATED` | ELIMINATED | false | 3 | Loại cuộc thi |
| 09 | `E2E-T09 ACTIVE bốc thăm Track 2` | ACTIVE | false | 3 | Track 2, bảng B |

```sql
SELECT t.id, t.team_name, t.status, t.is_locked, t.rejection_reason, u.email AS leader_email
FROM teams t
JOIN users u ON u.id = t.leader_id
JOIN hackathons h ON h.id = t.hackathon_id
WHERE h.slug = 'seal-e2e-2026' AND t.team_name LIKE 'E2E-T%'
ORDER BY t.team_name;
```

---

## 3. Bảng `team_members` — đa dạng role / status

| status | Ví dụ | Test |
|--------|-------|------|
| ACCEPTED + LEADER | Mỗi đội | Chi tiết đội, duyệt |
| ACCEPTED + MEMBER | Đội 03–09 | Đủ 3–5 khi duyệt |
| PENDING | `ext.pending@gmail.com` ở đội 02 | Accept / Reject invite |
| LEFT | `ext.member23@gmail.com` ở đội 02 | Rời đội |

**API mẫu — accept invite (đăng nhập user #5):**

```http
PATCH {{baseUrl}}/api/v1/teams/{{teamId02}}/members/{{userIdPending}}
Authorization: Bearer {{accessTokenInvitee}}
Content-Type: application/json

{ "action": "ACCEPT" }
```

**API — mời (đăng nhập leader đội 01, user pool free):**

```http
POST {{baseUrl}}/api/v1/teams/{{teamId01}}/members/invite
Authorization: Bearer {{accessTokenLeader01}}
Content-Type: application/json

{ "email": "student.gd2.pool.free@gmail.com" }
```

**API — mời trùng (pool busy → 409):**

```json
{ "email": "student.gd2.pool.busy@gmail.com" }
```

Kỳ vọng: `409` `USER_IN_ANOTHER_TEAM`.

---

## 4. Bảng `team_round_participation` — 5 bản ghi

| Đội | Round | Ghi chú |
|-----|-------|---------|
| E2E-T04 | Sơ loại | ACTIVE |
| E2E-T05 | Sơ loại | ACTIVE locked |
| E2E-T07 | Sơ loại | Chưa mentor |
| E2E-T08 | Sơ loại | ELIMINATED (vẫn có participation để đọc lịch sử) |
| E2E-T09 | Sơ loại | Track 2 |

---

## 5. Bảng `team_round_tracks` — 5 bản ghi

| Đội | track (seed) | assignedGroup |
|-----|--------------|---------------|
| E2E-T04 | Track 1 — RAG | Bảng A |
| E2E-T05 | Track 1 — RAG | Bảng B |
| E2E-T07 | Track 2 — AI Agent | Bảng A |
| E2E-T08 | Track 1 — RAG | Bảng C |
| E2E-T09 | Track 2 — AI Agent | Bảng B |

**API lottery (Coordinator) — thêm đội mới:**

```http
PATCH {{baseUrl}}/api/v1/hackathons/{{hackathonId}}/lottery
Authorization: Bearer {{accessTokenCoord}}
Content-Type: application/json
```

```json
{
  "roundId": 1,
  "assignments": [
    { "teamId": 10, "trackId": 1, "assignedGroup": "Bảng A" }
  ]
}
```

*(Thay `roundId` / `teamId` / `trackId` bằng ID thật sau seed.)*

---

## 6. Bảng `mentor_team_assignments` — 5 bản ghi

Mentor seed: `mentor@fpt.edu.vn` — vòng **Sơ loại** (không gán FINAL).

| Đội | Ghi chú |
|-----|---------|
| E2E-T03 | PENDING (mentor sớm — edge case) |
| E2E-T04 | ACTIVE đầy đủ |
| E2E-T05 | ACTIVE locked |
| E2E-T07 | — |
| E2E-T09 | — |

**E2E-T07** cố ý **không** có mentor trong seed → test:

```http
POST {{baseUrl}}/api/v1/teams/{{teamId07}}/rounds/{{roundIdPrelim}}/mentor
Authorization: Bearer {{accessTokenCoord}}
Content-Type: application/json

{ "mentorId": 5 }
```

**Disband có mentor (đội 04) → 409:**

```http
DELETE {{baseUrl}}/api/v1/teams/{{teamId04}}
```

Kỳ vọng: `409` `TEAM_HAS_MENTOR_CANNOT_DISBAND`.

---

## 7. Map API ↔ Error code (từ PDF)

| API | Body / điều kiện | Error |
|-----|------------------|-------|
| `POST /teams` trùng tên `E2E-T01...` | — | `409 TEAM_NAME_DUPLICATE` |
| `PATCH .../status` ACTIVE đội 01 | `{ "status": "ACTIVE" }` | `422 TEAM_INVALID_MEMBER_COUNT` |
| `POST .../invite` pool.busy | email busy | `409 USER_IN_ANOTHER_TEAM` |
| `PATCH .../status` ACTIVE đội 03 | `{ "status": "ACTIVE" }` | `200` |
| `PATCH .../status` REJECTED đội 06 | + `rejectionReason` | `200` |
| `DELETE /teams/{id}` đội 04 | có mentor | `409 TEAM_HAS_MENTOR_CANNOT_DISBAND` |
| `POST .../rounds/{finalRoundId}/mentor` | round FINAL | `422 MENTOR_ASSIGNMENT_NOT_FOR_FINAL_ROUND` |

---

## 8. Luồng E2E gợi ý (manual)

| # | Bước | Actor | Ghi chú |
|---|------|-------|---------|
| 1 | Login `student.gd2.hcm.leader03@fpt.edu.vn` | Student | |
| 2 | `GET /teams?hackathonId=&status=PENDING` | Student | Thấy đội 03 |
| 3 | Login Coordinator | Coord | |
| 4 | `PATCH /teams/{03}/status` ACTIVE | Coord | `200` |
| 5 | Login `student.gd2.pool.free@gmail.com` | Student | |
| 6 | Nhận invite / tạo đội mới | — | |
| 7 | `PATCH /hackathons/{id}/lottery` | Coord | Đội chưa có track |
| 8 | `POST .../mentor` đội 07 | Coord | |

---

## 9. Reset / seed lại GĐ2

Seed chỉ chạy **một lần** (kiểm tra tên `E2E-T01...`). Để seed lại:

```sql
DELETE tm FROM team_members tm
JOIN teams t ON t.id = tm.team_id
JOIN hackathons h ON h.id = t.hackathon_id
WHERE h.slug = 'seal-e2e-2026' AND t.team_name LIKE 'E2E-T%';

DELETE FROM teams WHERE team_name LIKE 'E2E-T%'
  AND hackathon_id = (SELECT id FROM hackathons WHERE slug = 'seal-e2e-2026' LIMIT 1);

DELETE FROM users WHERE email LIKE 'student.gd2.%' OR email LIKE 'student.gd2.pool.%';
```

Restart app (`profile=dev`) → `E2eWorkflowDataSeeder` chạy lại.

---

## 10. Mock JSON FE (`mocks/teams-gd2.json`)

```json
{
  "teamsPendingList": [
    { "id": 1, "teamName": "E2E-T01 Chờ duyệt (1 người)", "status": "PENDING", "acceptedMemberCount": 1 },
    { "id": 2, "teamName": "E2E-T02 Chờ duyệt (2 ACCEPTED + 1 PENDING)", "status": "PENDING", "acceptedMemberCount": 2, "pendingInviteCount": 1 },
    { "id": 3, "teamName": "E2E-T03 Sẵn duyệt ACTIVE (4 người)", "status": "PENDING", "acceptedMemberCount": 4 }
  ],
  "teamActiveLocked": {
    "teamName": "E2E-T05 ACTIVE đã khóa + bốc thăm",
    "status": "ACTIVE",
    "isLocked": true,
    "acceptedMemberCount": 4
  },
  "teamRejected": {
    "teamName": "E2E-T06 REJECTED",
    "status": "REJECTED",
    "rejectionReason": "Hồ sơ không khớp quy chế chapter FPT-HCM"
  },
  "teamEliminated": {
    "teamName": "E2E-T08 ELIMINATED",
    "status": "ELIMINATED",
    "eliminationReason": "Không nộp bài sơ loại"
  }
}
```

---

## 11. Swagger

`http://localhost:8080/swagger-ui.html` → **Teams**, **Hackathons** (lottery).
