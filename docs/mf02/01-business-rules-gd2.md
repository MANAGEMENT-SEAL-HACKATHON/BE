# MF-02 GĐ2 — Business Rules (Đội & vận hành)

**Nguồn:** `GD02_SEAL_MF02_MASTER_v3.5.docx` · **Trạng thái BE:** khung API + `TODO` (chưa implement logic).

**Phạm vi tài liệu này:** FR-11 … FR-13C (teams, members, lock, lottery, mentor). Auth/JWT xem [01-auth-users.md](01-auth-users.md).

---

## 1. Thực thể & ràng buộc DB

| Bảng | Mô tả |
|------|--------|
| `teams` | `UNIQUE(team_name, hackathon_id)` · `status` ∈ PENDING, ACTIVE, ELIMINATED, REJECTED · `is_locked` |
| `team_members` | Leader + Member · `status` PENDING / ACCEPTED / REJECTED / LEFT |
| `team_round_participation` | Đội tham gia vòng (tạo khi ACTIVE / lottery) |
| `team_round_tracks` | 1 đội → 1 track / 1 round |
| `mentor_team_assignments` | Mentor theo `(team, round)` |

**Quy tắc chung**

- Một user **chỉ thuộc tối đa một đội ACTIVE/PENDING** trong cùng hackathon (`USER_IN_ANOTHER_TEAM`).
- Tên đội **không trùng** trong hackathon (case-insensitive) → `TEAM_NAME_DUPLICATE`.
- Sau `registration_end`: cron FR-13A set `is_locked=true` → không mời / hủy invite / transfer leader.

---

## 2. FR-11 — Tạo đội

| Điều kiện | HTTP |
|-----------|------|
| Actor: STUDENT **APPROVED** | 403 nếu PENDING |
| Hackathon **ONGOING** | `HACKATHON_NOT_ONGOING` 422 |
| `registration_end` > hôm nay (theo TZ server) | `REGISTRATION_CLOSED` 422 |
| Leader = current user | — |
| Không chọn Track khi tạo | — |

**Side effect (khi implement):**

- `INSERT teams` status=`PENDING`, `chapter_id` từ user.
- `INSERT team_members` role=`LEADER`, status=`ACCEPTED`.
- Audit `TEAM_CREATE`.

---

## 3. FR-11C — Transfer Leader

- Chỉ **leader hiện tại** gọi API.
- Đội `PENDING`, `is_locked=false`.
- `newLeaderUserId` phải là member **ACCEPTED** → `NEW_LEADER_NOT_MEMBER`.
- Audit `LEADER_TRANSFERRED`.

---

## 4. FR-11D — Disband

| Actor | Giai đoạn | Hành vi |
|-------|-----------|---------|
| Leader | Trước mentor / lottery | Soft `REJECTED` |
| Coordinator | Mọi giai đoạn (theo policy) | Soft `REJECTED` |
| Leader | Đã có mentor per-round | `TEAM_HAS_MENTOR_CANNOT_DISBAND` 409 |

Đội **ACTIVE** (đã duyệt): leader **không** disband — Coordinator xử lý.

---

## 5. FR-12 — Thành viên

| Hành động | Ai gọi | Ghi chú |
|-----------|--------|---------|
| Invite (email) | Leader | User APPROVED + STUDENT · max 5 ACCEPTED |
| PATCH ACCEPT/REJECT | Invitee (chính user) | — |
| PATCH LEFT | Member (không phải leader) | Leader phải transfer trước |
| DELETE member | Leader | Chỉ `PENDING` invite |

`TEAM_LOCKED` → 403. `TEAM_MEMBER_FULL` → 409.

---

## 6. FR-13 — Phê duyệt đội

**ACTIVE (duyệt):**

- 3–5 thành viên `ACCEPTED`.
- Tất cả `users.status = APPROVED`.
- Hackathon ONGOING.

**REJECTED:**

- Bắt buộc `rejectionReason`.

Audit: `TEAM_APPROVE` / `TEAM_REJECT`.

---

## 7. FR-13A — Khóa đội

- Cron (`TeamLockScheduler`): mỗi phút, idempotent.
- `registration_end < today` → `is_locked=true` cho đội ACTIVE chưa khóa.
- Audit `TEAM_LOCKED` (actor SYSTEM).

---

## 8. FR-13B — Bốc thăm Track

- Batch: `PATCH /hackathons/{id}/lottery`.
- Đội phải **ACTIVE**, chưa có track cùng round.
- Tạo `team_round_participation` + `team_round_tracks`.

**FR-13B-R Re-lottery:** `PATCH /teams/{id}/rounds/{roundId}/track` — round **chưa** ACTIVE → `ROUND_ALREADY_ACTIVE` 423.

---

## 9. FR-13C — Mentor per-round

- Cần `team_round_participation` trước.
- `UNIQUE(team, round)` trên `mentor_team_assignments`.
- Xóa mentor: không có điểm round đó → `ROUND_HAS_SCORES`.

---

## 10. Error codes (teams)

Xem đầy đủ trong `ErrorCode.java` (prefix `TEAM_*`, `REGISTRATION_*`, …) và bảng map trong [03-api-reference-gd2.md](03-api-reference-gd2.md#error-codes).

---

## 11. Trạng thái implement BE

| FR | Controller | Service logic |
|----|------------|---------------|
| FR-11 … FR-13C | ✅ Khung | ⏳ `501 NOT_IMPLEMENTED` |
| FR-13A cron | ✅ Scheduler | ⏳ `TeamLockServiceImpl` return 0 |

Swagger: `http://localhost:8080/swagger-ui.html` → tag **Teams (GĐ2)**.
