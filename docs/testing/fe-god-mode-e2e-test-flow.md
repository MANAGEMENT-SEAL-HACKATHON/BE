# FE God Mode — Luồng test E2E (GĐ1 → GĐ6 + Smoke God Mode)

> Tích hợp FE theo `TeamManagement_API_Documentation.pdf`  
> Build verify: `npm run build` trong `seal-hackathon-fe`

## Chuẩn bị

```bash
# BE
git pull origin dev --rebase
# Chạy Spring Boot profile dev

# FE
cd seal-hackathon-fe
npm install
npm run dev
```

| Vai trò | Email | Password |
|---------|-------|----------|
| Coordinator | `coord@fpt.edu.vn` | `Coordinator@dev1` |
| Student GĐ1/GĐ2 | `student.sp26.t01.leader@fpt.edu.vn` | `Student@dev1` |
| God Mode seed | `test.user1@fpt.edu.vn` … `test.user11@fpt.edu.vn` | `Student@dev1` |

---

## A. Smoke test tính năng mới

### A1 — maxParticipants (Coordinator)

1. Login coord → `/hackathons/create`
2. Nhập **Số lượng người tham gia tối đa** (vd: `100`) + các field bắt buộc khác → Tạo
3. Vào Setup → tab **Cấu hình chung** → sửa cap khi `DRAFT`, read-only khi `ONGOING`
4. Danh sách hackathon hiển thị dòng `Tối đa: N người tham gia`

### A2 — Đăng ký SV + lỗi đầy chỗ

1. Login student APPROVED → Dashboard → panel **Đăng ký Hackathon**
2. Bấm **Đăng ký tham gia** hackathon ONGOING → success
3. (Stress) Khi giải đầy → `422 INVALID_STATE`, nút disabled + message PDF
4. **Hủy đăng ký** khi chưa có đội

### A3 — Bảng tin ghép đội (Student)

1. Menu **Bảng tin ghép đội** → `/student/matchmaking`
2. Thấy card grid: tên đội, `x/5`, leader, email, **Sao chép email**
3. Read-only — không có nút xin vào

### A4 — Radar & God Mode (Coordinator)

1. `/teams` → tab **Radar & Giải cứu**
2. Tra hackathon có seed matchmaking (log `[DataSeeder] Hoàn tất bộ Data` hoặc `GET .../orphans` ≠ `[]`)
3. **Thêm thành viên** orphan → đội 2 người → sau action đội **ACTIVE** (3 người), không PATCH status
4. **Gộp đội** A(2)+B(2) → 4 người ACTIVE; đội nguồn REJECTED
5. **Gom đội mới** — chọn 3–5 orphan, leader, tên đội
6. Negative: thêm vào đội 5 người → `TEAM_MEMBER_FULL`

---

## B. E2E chuỗi A — GĐ1 → GĐ6

| GĐ | Slug | Coordinator | Student |
|----|------|-------------|---------|
| GĐ1 fail | `seal-gd1-incomplete` | Readiness FAIL | — |
| GĐ1 happy | `seal-e2e-2026` | Setup, maxParticipants, ONGOING | Đăng ký, đội, matchmaking |
| GĐ1 teams | `seal-e2e-2026` | `/teams` duyệt đội | Lời mời |
| GĐ2 | `seal-e2e-2026` | Bốc thăm 24/3 track | Xem track |
| GĐ3 | `seal-gd3-prelim-open` | Chấm, LATE T19–T21 | Nộp bài, kết quả |
| GĐ4 | `seal-gd4-advance-ready` | Ranking, wildcard | — |
| GĐ5 | `seal-gd5-final-active` | Calibration, CK | Nộp CK |
| GĐ6 | `seal-gd6-pending-confirm` | Kết quả, RBL, giải | Student results |

Chi tiết seed: [`dev-seed-guide.md`](./dev-seed-guide.md)

---

## C. Regression (nguyên tắc vàng)

- [ ] Tab **Duyệt đội** trên `/teams` giữ nguyên hành vi
- [ ] `StudentTeamOnboarding` vẫn **3 card** (không thêm card thứ 4)
- [ ] Không đổi màu/gradient màn dashboard & onboarding cũ
- [ ] `npm run build` pass

---

## Ghi chú BE khi test

- `MatchmakingDataSeeder` dùng `hackathonId=7` — nếu không có data, tra `GET /api/v1/hackathons` tìm hackathon ONGOING
- Response team dùng field `id` + `acceptedMemberCount` (FE mapper xử lý)
- Error codes: `TEAM_MEMBER_FULL`, `TEAM_INVALID_MEMBER_COUNT`, `USER_IN_ANOTHER_TEAM`
