# FR-05 — Quản lý nhân sự giải đấu

> Workflow v3.1 ref: GĐ1 — Bước 5 | DB v2.1 ref: `users`, `invitations`, `mentor_assignments`, `judge_assignments`

> **LÝ DO DỜI VỀ GĐ1 (v2.2)**: Bước 5 cần cảnh báo conflict Mentor↔Judge — phải có dữ liệu
> cả hai bảng. Nên 5a (tạo Judge tạm) + 5b (gán Mentor) + 5c (gán Judge) đều ở GĐ1.

## Endpoint table

| FR | # | Method | Path | Status |
|---|---|---|---|---|
| 5a | 1 | POST | `/api/v1/users/temp-judges` | 201 |
| 5a | 2 | GET | `/api/v1/users/temp-judges` | 200 |
| 5a | 3 | POST | `/api/v1/invitations/{id}/resend` | 200 |
| 5b | 4 | POST | `/api/v1/mentor-assignments` | 201 |
| 5b | 5 | GET | `/api/v1/tracks/{trackId}/mentors` | 200 |
| 5b | 6 | GET | `/api/v1/users/{mentorId}/track-assignments` | 200 |
| 5b | 7 | DELETE | `/api/v1/mentor-assignments/{id}` | 200 |
| 5c | 8 | POST | `/api/v1/judge-assignments` | 201 |
| 5c | 9 | GET | `/api/v1/rounds/{roundId}/judges` | 200 |
| 5c | 10 | GET | `/api/v1/users/{judgeId}/round-assignments` | 200 |
| 5c | 11 | DELETE | `/api/v1/judge-assignments/{id}` | 200 |

---

# FR-05a — Tạo tài khoản Judge khách mời + Invitation

## 1. POST `/api/v1/users/temp-judges`

### Request
```json
{
  "fullName":    "Mr. Pham Duc Nhi",
  "email":       "nhi.pham@google.com",
  "institution": "Google Vietnam",
  "phone":       "+84 ..."
}
```

### Validation
| Field | Rule | Error |
|---|---|---|
| `fullName` | NotBlank, max 200 | 400 |
| `email` | NotBlank, valid email, max 320 | 400 |
| `email` | UNIQUE — chưa tồn tại trong `users` | 409 `USER_EMAIL_TAKEN` |
| `institution` | NotBlank cho EXTERNAL Judge | 400 |

### Logic
```
@Transactional
createTempJudge(req, coordinatorId):
  if userRepo.existsByEmail(req.email): throw 409 USER_EMAIL_TAKEN
  user = User.builder()
            .fullName(req.fullName).email(req.email)
            .role(JUDGE).userType(EXTERNAL).isTempAccount(true)
            .status(APPROVED)            // Coordinator approve trực tiếp, KHÔNG qua PENDING
            .institution(req.institution).phone(req.phone)
            .build()
  userRepo.save(user)

  token = UUID-based 64-char random
  invitation = Invitation.builder()
                 .email(user.email).role(JUDGE).invitedBy(coordinatorRef)
                 .token(token).expiresAt(NOW + 48h)
                 .build()
  invitationRepo.save(invitation)

  emailService.sendInvitation(user.email, token, expiresAt)        # interface stub — Dev triển khai sau

  audit.log(TEMP_ACCOUNT_CREATE, "users", user.id,
            {invitationId: invitation.id, institution: user.institution})
  return mapper.toResponse(user, invitation)
```

### Response 201
```json
{
  "success": true,
  "data": {
    "user": { "id": 102, "fullName": "...", "email": "...", "role": "JUDGE", "status": "APPROVED", "isTempAccount": true, "institution": "Google Vietnam" },
    "invitation": { "id": 17, "expiresAt": "2026-05-18T09:57:00Z", "tokenSent": true }
  },
  "message": "Created"
}
```

> `token` **KHÔNG** trả về cho Coordinator — chỉ gửi qua email.

### Audit
- `TEMP_ACCOUNT_CREATE` + detail

---

## 2. GET `/api/v1/users/temp-judges`

Query params:
- `institution` (LIKE)
- `q` (fullName/email)
- `page`, `size`

Response paged — kèm trạng thái invitation (`pendingAccept`, `accepted`, `expired`).

---

## 3. POST `/api/v1/invitations/{id}/resend`

### Logic
- 404 nếu invitation không tồn tại.
- 409 `INVITATION_ALREADY_ACCEPTED` nếu `accepted_at IS NOT NULL`.
- Regenerate `token` và set `expires_at = NOW + 48h`.
- Gửi email lại.
- Audit `INVITATION_RESEND`.

### Response 200
```json
{ "success": true, "data": { "id": 17, "expiresAt": "...", "tokenSent": true } }
```

---

# FR-05b — Phân công Mentor vào Track

## 4. POST `/api/v1/mentor-assignments`

### Request
```json
{ "mentorId": 55, "trackId": 4 }
```

### Validation
| Rule | Error |
|---|---|
| Mentor exists & role=MENTOR & status=APPROVED | 422 `USER_INVALID_ROLE` / `USER_NOT_APPROVED` |
| Track exists | 404 |
| Track.hackathon.status IN (DRAFT, ONGOING) | 409 `TRACK_HACKATHON_LOCKED` |
| UNIQUE(mentor_id, track_id) | 409 `MENTOR_ASSIGN_DUPLICATE` |

### Conflict warning 2 chiều
```
# Query xem mentor này có đang là Judge của Round nào trong cùng Track không
conflicts = judgeAssignmentRepo
              .findByJudgeIdAndRoundTrackId(mentorId, trackId)
if conflicts.isEmpty() and judgeAssignmentRepo.countByJudgeId(mentorId) == 0:
   # Bảng đối chiếu hoàn toàn trống cho user này → check không cho kết quả ý nghĩa
   audit.log(WARNING_CONFLICT_CHECK_SKIPPED, "mentor_assignments", null,
             {mentorId, trackId, reason: "judge_assignments has no row for this user"})
elif !conflicts.isEmpty():
   warnings.add({code: "MENTOR_JUDGE_CONFLICT",
                 message: "User #{mentorId} đang là Judge của Round #{...} trong cùng Track #{trackId}",
                 details: {conflictRounds: [...], userId: mentorId, trackId}})
```

### Response 201 (kèm warnings nếu có)
```json
{
  "success": true,
  "data": { "id": 31, "mentorId": 55, "trackId": 4, "assignedAt": "..." },
  "warnings": [
    { "code": "MENTOR_JUDGE_CONFLICT", "message": "...", "details": { "conflictRounds": [11] } }
  ]
}
```

### Audit
- `MENTOR_ASSIGNED`

---

## 5. GET `/api/v1/tracks/{trackId}/mentors`
List mentor của Track. Response: `List<MentorAssignmentResponse>`.

## 6. GET `/api/v1/users/{mentorId}/track-assignments`
List Track mà 1 Mentor đang phụ trách (xuyên Hackathon hay 1 Hackathon).

## 7. DELETE `/api/v1/mentor-assignments/{id}`

### Logic
- Xóa assignment.
- Notify mentor qua `notifications` type `MENTOR_UNASSIGNED`.
- Audit `MENTOR_UNASSIGNED`.

---

# FR-05c — Phân công Judge sơ bộ vào Round

## 8. POST `/api/v1/judge-assignments`

### Request
```json
{
  "judgeId": 102,
  "roundId": 11,
  "assignmentType": "NORMAL"   // NORMAL | CALIBRATION | HEAD
}
```

### Validation
| Rule | Error |
|---|---|
| Judge exists & role=JUDGE & status=APPROVED | 422 `USER_INVALID_ROLE` / `USER_NOT_APPROVED` |
| Round exists | 404 |
| UNIQUE(judge_id, round_id) | 409 `JUDGE_ASSIGN_DUPLICATE` |

### Warnings (mềm — không block)
1. **`JUDGE_FINAL_ROUND_AT_PHASE1`**: nếu `round.sequenceOrder == max(sequenceOrder)` của Track → cảnh báo:
   > "Round Chung kết thường phân công Judge ở GĐ5 (FR-27). Bạn vẫn muốn phân công ở GĐ1?"
2. **`MENTOR_JUDGE_CONFLICT`**: query `mentor_assignments` xem user có đang là Mentor của Track chứa Round không.
3. **`CONFLICT_CHECK_SKIPPED`**: nếu bảng `mentor_assignments` cho user này empty.

### Response 201 — kèm warnings

### Audit
- `JUDGE_ASSIGNED` (kèm assignmentType)

---

## 9. GET `/api/v1/rounds/{roundId}/judges`
List Judge của Round + `assignmentType`.

## 10. GET `/api/v1/users/{judgeId}/round-assignments`
List Round mà Judge đã được phân công (xuyên Hackathon).

## 11. DELETE `/api/v1/judge-assignments/{id}`

### Logic
- Audit `JUDGE_UNASSIGNED`.
- Notify judge.

---

## Bảng liên quan
- `users` — validate role/status
- `invitations` — token one-time-use cho Judge tạm
- `mentor_assignments` ↔ `judge_assignments` — conflict check 2 chiều
- `tracks`, `rounds` — parent
- `audit_logs`
- `notifications` — notify khi assign/unassign

## Test cases
1. Tạo Judge tạm với email mới → 201 + invitation gửi email.
2. Email đã tồn tại → 409 `USER_EMAIL_TAKEN`.
3. Resend invitation đã accepted → 409 `INVITATION_ALREADY_ACCEPTED`.
4. Assign Mentor, sau đó assign Judge cùng user vào Round trong cùng Track → 201 + warning `MENTOR_JUDGE_CONFLICT`.
5. Assign Mentor đầu tiên (judge_assignments empty cho user) → 201 + audit `WARNING_CONFLICT_CHECK_SKIPPED`, không có warning trong response.
6. Phân công Judge cho Round Chung kết (sequenceOrder cao nhất) → 201 + warning `JUDGE_FINAL_ROUND_AT_PHASE1`.
7. DELETE assignment → 200 + notification gửi user.

## Email contract (interface `EmailService`)
- `sendInvitation(email, token, expiresAt)` — async preferable; lưu `email_sent_at` (nếu thêm cột) hoặc audit log.
- Body template: link `${frontend.url}/invitations/accept?token={token}`, expires_in 48h.
- Không gửi password plaintext.
