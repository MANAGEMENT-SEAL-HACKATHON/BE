# Checklist test FE nhanh — GĐ2, GĐ4, GĐ5, GĐ6

> Mục tiêu: cho tester chỉ cần mở đúng slug + login đúng account + đi đúng luồng.
> Profile: `dev`.
> Sau mỗi lần restart BE, seed tự repair timeline theo giờ máy.

---

## 1) GĐ2 — Tạo team / mời thành viên

**Hackathon:** `seal-e2e-2026`

### Luồng của bạn ↔ trạng thái seed hiện tại

| Bước (luồng bạn test) | Seed hiện tại | Việc trên FE |
|---|---|---|
| 1. Mở đăng ký hackathon | Đăng ký còn mở (~14 ngày) | Student vào trang hackathon và đăng ký |
| 2. Tạo đội | Có 7 đội mẫu, nhưng bạn có thể tạo đội mới | Student tạo team mới để test flow thật |
| 3. Mời thành viên | Có sẵn 3 orphan chưa có đội | Mời orphan vào đội để test invite/accept |
| 4. Khóa đội / chuyển tiếp | Chưa khóa, chưa lottery | Coord duyệt theo flow FE của bạn |

### Account dùng test

| Vai trò | Email | Password |
|---|---|---|
| Coordinator | `coord@fpt.edu.vn` | `Coordinator@dev1` |
| Student orphan #1 | `student.e2e.orphan1@fpt.edu.vn` | `Student@dev1` |
| Student orphan #2 | `student.e2e.orphan2@fpt.edu.vn` | `Student@dev1` |
| Student orphan #3 | `student.e2e.orphan3@fpt.edu.vn` | `Student@dev1` |

### Checklist FE (5-10 phút)

1. Coord vào `seal-e2e-2026`, xác nhận registration đang mở.
2. Student tạo team mới.
3. Mời 1-3 orphan vào team.
4. Orphan login accept lời mời.
5. Coord kiểm tra team đủ member để qua bước tiếp theo.

---

## 2) GĐ4 — Ranking / Wildcard / Advance

**Hackathon:** `seal-gd4-advance-ready`  
**Chi tiết ma trận:** [gd4-full-test-matrix-and-seeds.md](gd4-full-test-matrix-and-seeds.md)

### Luồng của bạn ↔ trạng thái seed hiện tại

| Bước (luồng bạn test) | Seed hiện tại | Việc trên FE |
|---|---|---|
| 1. Xem ranking sơ loại | Đã có 8 đội + điểm đầy đủ | Mở bảng ranking và kiểm tra thứ hạng |
| 2. Xử lý wildcard | Có candidate sẵn | Approve/reject wildcard theo UI |
| 3. Publish kết quả sơ loại | Chưa publish | Coord bấm publish |
| 4. Advance sang CK | CK chưa active, chưa advance | Coord chọn team đi tiếp và advance |

### Account dùng test

| Vai trò | Email | Password |
|---|---|---|
| Coordinator | `coord@fpt.edu.vn` | `Coordinator@dev1` |
| Judge nội bộ | `judge1@fpt.edu.vn` | `Judge@dev1` |
| Judge nội bộ | `judge2@fpt.edu.vn` | `Judge@dev1` |

### Checklist FE (10-15 phút)

1. Coord mở `seal-gd4-advance-ready`.
2. Vào phần ranking sơ loại, xác nhận đủ 8 đội.
3. Vào wildcard, xử lý candidate.
4. Publish kết quả.
5. Advance đội sang CK.

---

## 3) GĐ5 — Chung kết đang diễn ra (nộp + chấm + lock)

**Hackathon:** `seal-gd5-final-active`  
**Chi tiết ma trận:** [gd5-full-test-matrix-and-seeds.md](gd5-full-test-matrix-and-seeds.md)

### Luồng của bạn ↔ trạng thái seed hiện tại

| Bước (luồng bạn test) | Seed hiện tại | Việc trên FE |
|---|---|---|
| 1. Vào CK | CK đã active, đề đã phát | Kiểm tra trạng thái round CK đang mở |
| 2. Student nộp CK | Có đội đã nộp + có đội chưa nộp | Dùng team chưa nộp để test submit |
| 3. Judge chấm CK | Có đội đã chấm mẫu | Judge chấm thêm cho đội khác |
| 4. Coord khóa chấm CK | Chưa lock | Coord lock scoring để kết thúc GĐ5 |

### Account dùng test

| Vai trò | Email | Password |
|---|---|---|
| Coordinator | `coord@fpt.edu.vn` | `Coordinator@dev1` |
| Guest judge | `guestjudge@gmail.com` | `GuestJudge@dev1` |
| Student (gợi ý) | `student.gd5.leader03@fpt.edu.vn` | `Student@dev1` |

### Checklist FE (10-15 phút)

1. Coord mở `seal-gd5-final-active`, xác nhận CK active.
2. Student leader03 nộp bài CK.
3. Guest judge vào Live Scoring chấm bài.
4. Coord lock scoring CK.
5. Kiểm tra trạng thái hackathon chuyển về `PENDING_CONFIRM` (nếu UI có hiển thị status).

---

## 4) GĐ6 — Confirm & đóng giải

**Hackathon:** xem [gd6-full-test-matrix-and-seeds.md](gd6-full-test-matrix-and-seeds.md) — slug chính `seal-gd6-pending-confirm`

**Chi tiết ma trận:** [gd6-full-test-matrix-and-seeds.md](gd6-full-test-matrix-and-seeds.md)

### Luồng của bạn ↔ trạng thái seed hiện tại

| Bước (luồng bạn test) | Seed hiện tại | Việc trên FE |
|---|---|---|
| 1. Xem trạng thái pre-close | Hackathon ở `PENDING_CONFIRM` | Coord mở màn hình Awards/Confirm |
| 2. Kiểm tra ranking + prize | Có dữ liệu ranking, đã có FIRST prize | Xem bảng xếp hạng / danh sách giải |
| 3. Thêm giải còn thiếu | Có thể thêm SECOND/THIRD | Test thêm giải thưởng từ UI |
| 4. Confirm đóng hackathon | Sẵn sàng confirm | Coord bấm confirm để về `FINISHED` |

### Account dùng test

| Vai trò | Email | Password |
|---|---|---|
| Coordinator | `coord@fpt.edu.vn` | `Coordinator@dev1` |
| Student | `student.gd6.leader01@fpt.edu.vn` | `Student@dev1` |

### Checklist FE (8-12 phút)

1. Coord mở `seal-gd6-pending-confirm`.
2. Kiểm tra ranking/prize hiện có.
3. Thêm giải (nếu cần test create prize).
4. Bấm confirm đóng hackathon.
5. Kiểm tra status thành `FINISHED`.

---

## Lưu ý vận hành cho tester

- Nếu test xong muốn quay lại trạng thái seed ban đầu: chỉ cần **restart BE (profile `dev`)**.
- Mỗi giai đoạn nên test trên **đúng slug** ở trên; đừng test nhầm giữa `seal-e2e-2026` và `seal-gd*-*`.
- Password student seed mặc định: `Student@dev1`.
