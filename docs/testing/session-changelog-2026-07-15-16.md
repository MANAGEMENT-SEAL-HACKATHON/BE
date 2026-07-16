# Session Changelog: 15/07 - 16/07/2026

**Bối cảnh:** Báo cáo tổng hợp các bản vá lỗi P0 về logic vận hành, workflow và UX/FE từ sáng 15/07 đến tối 16/07. Các thay đổi tập trung vào việc đồng bộ state giữa Coordinator và Student, vá các lỗ hổng nghiệp vụ (Wildcard, Timer, Chấm điểm) và chuẩn hóa UI phi kỹ thuật.

**Trạng thái nhánh:** `BE HEAD 175acfe`, `FE HEAD a541205` (bao gồm các file modified/untracked trong working tree).

**Playbook liên quan:** [manual-ui-playbook-gd1-gd6.md](manual-ui-playbook-gd1-gd6.md) (cập nhật cùng phiên — happy path click-by-click non-IT).

---

## 1. Bảng Map Yêu Cầu → Trạng Thái (DONE)

### GĐ1: Khởi tạo & Cấu hình (UX/UI)

* **[DONE]** Loại bỏ các thuật ngữ IT jargon, thay bằng ngôn ngữ thường trên form tạo sự kiện.
* **[DONE]** Form tạo sự kiện: Ẩn/Disabled tự tính ngày "Bắt đầu/Kết thúc" và "BXH cá nhân".
* **[DONE]** Sửa lỗi tạo Vòng chung kết bị crash khi thay đổi thời lượng thi (form default duration bug).
* **[DONE]** Modal Activate: Chế độ "Đổi lịch" (Reschedule) đã chặn logic bắt đầu ngay lập tức, bắt buộc chờ đến ngày đã đổi.
* **[DONE]** Modal Activate: Chế độ "Bắt đầu sớm" (START_NOW) cho phép setup lead time (vài phút) và kích hoạt ngay.

### GĐ2: Đăng ký & Ghép đội

* **[DONE]** Auto-navigate: Tạo đội hoặc Chấp nhận lời mời tự động chuyển vào trang đội (Không cần F5).
* **[DONE]** Sửa logic Giải tán đội: Thành viên cũ có thể tạo đội mới (Fix lỗi kẹt state Accepted cũ).
* **[DONE]** Fix Bug UI Duyệt đội: Nút duyệt hàng loạt (Batch Approve) giờ chỉ chọn các đội "Đủ điều kiện", bỏ qua đội thiếu member.
* **[DONE]** Tối ưu thuật toán Bốc thăm (Lottery): Giảm thời gian chờ từ 5 phút xuống còn vài giây.
* **[DONE]** UI: Bổ sung hiển thị Avatar của Judge/Mentor khi thao tác chọn nhân sự.

### GĐ3: Sơ loại & Chấm điểm (Workflow)

* **[DONE]** Nút "Kết thúc đăng ký sớm" tự động ẩn/disable khi sự kiện đang diễn ra (GĐ3).
* **[DONE]** Đồng bộ Đề thi: Bấm "Phát đề" vòng thi tự động sync đề vào Bảng đấu (Track).
* **[DONE]** Màn hình Coordinator: Bổ sung hiển thị cột "Trạng thái nộp bài" của các đội.
* **[DONE]** Đồng bộ Trạng thái sự kiện: Khi GĐ3/5 đóng cửa, Student nộp bài sẽ nhận thông báo "Sự kiện đã kết thúc" (Thay vì lỗi vô lý "Chưa diễn ra").
* **[DONE]** Timer Sequence (Trình tự giám khảo): Bắt buộc đi theo luồng `Hết thuyết trình -> Hết Q&A -> Gọi đội kế tiếp`.
* **[DONE]** Ẩn nút "Reset Timer" sau khi chốt điểm/chuyển đội để đảm bảo công bằng.
* **[DONE]** Reset Điểm: Fix lỗi form chấm điểm bị dính cache điểm của đội trước đó khi chuyển đội.
* **[DONE]** Student UI: Thí sinh xem được "STT Thuyết trình" và "Mã số" (ẩn tên đội với Giám khảo) để chuẩn bị.

### GĐ4: Chốt Sơ loại & Wildcard (Vé vớt)

* **[DONE]** Tách biệt rạch ròi logic Tiebreak (Đồng điểm) và Wildcard.
* **[DONE]** Cập nhật Wildcard Plan C: Bổ sung cột "Thời gian nộp bài" và luồng "Giám khảo bình chọn" (Mentor của đội bị vô hiệu hóa quyền vote) để tránh thiên vị từ Coordinator.
* **[DONE]** Sync Student: Bấm Công bố điểm, tab Quản lý đội của SV hiện ngay Điểm cá nhân, Thứ hạng và Trạng thái Đi tiếp.

### GĐ5: Chung kết (Kế thừa & Bảo mật)

* **[DONE]** Kế thừa Đề thi: Chung kết tái sử dụng đề của Track GĐ3. Coordinator KHÔNG CÓ nút Phát đề/Upload PDF mới. Student tự xem đề cũ.
* **[DONE]** Bổ sung Màn hình: Coordinator xem danh sách "Các đội vào Chung kết".
* **[DONE]** Bảo mật thao tác: Đội bị loại (Eliminated) chuyển sang mode Read-only, không thể thao tác nộp bài/sửa thông tin.
* **[DONE]** Xáo trộn (Shuffle) & HARD_LOCK: Đồng bộ chặt chẽ với Trạng thái nộp bài. Khóa cứng không cho nộp trễ ở Chung kết.
* **[DONE]** Coordinator UI: Thêm màn hình xem "Điểm thành phần" (Component Scores) của từng giám khảo để check thiên vị/quên chấm.

### GĐ6: Tổng kết & Trao giải

* **[DONE]** Data Seed Fix: Vá lỗi probe báo "Chưa chấm đủ điểm Chung kết" trên seed GĐ6 (đã nạp đủ điểm).
* **[DONE]** Bảng xếp hạng Cơ sở (Base Ranking) đã hoạt động đúng logic.
* **[DONE]** Nâng cấp UI Trao giải: Thực tế hơn, bớt đơn điệu.
* **[DONE]** Xuất CSV: Bổ sung đầy đủ các cột data bị thiếu.
* **[DONE]** DataInitializer Repair: Seed GĐ4,5,6 đã nạp đầy đủ điểm số và STT thuyết trình của GĐ3 để phục vụ E2E và Manual Test.

### 1b. Neo kỹ thuật (không dump diff)

| Mảng | Neo |
|------|-----|
| Harness full-chain GĐ3→GĐ6 | `scripts/gd3-gd4-gd5-full-chain-api.mjs` |
| Probe seed (hint FINISHED → repair) | `seal-hackathon-fe/e2e/helpers/seedApiProbe.js` |
| Seed repair thứ tự | `DataInitializer`: `repairForGd5FullChainRetest` **trước** `repairForGd2Testing` |
| GĐ2 membership / giải tán | `TeamMembershipReleaseService` / `TeamMembershipReleaseServiceImpl` |

---

## 2. Các Mảng Ngoài Scope (KNOWN_GAP — xử lý sau)

* Chưa xóa API/Logic Wildcard dựa trên top-max của tài liệu cũ (để bảo vệ GĐ4).
* Đưa cấu hình Timer Thuyết trình/Q&A từ cấp Track lên cấp Round.
* Invite Giám khảo tự động duyệt (chưa có luồng gửi Email 72h).

---

## 3. Verify nhanh

| Kiểm | Kỳ vọng |
|-------|---------|
| `npm run probe:seeds` (FE, BE đã seed) | **26/26** |
| Mode A | Mở đúng happy slug theo [manual-ui-playbook-gd1-gd6.md](manual-ui-playbook-gd1-gd6.md) §0.6 |
| Mode B / full-chain API | `node scripts/gd3-gd4-gd5-full-chain-api.mjs` |
| Sau Confirm GĐ6 trên `seal-e2e-2026` | **Restart BE** (create-drop + repair) rồi mới `probe:seeds` |

Happy slug seed (`DevSeedCatalog.ALL_DEV_HACKATHON_SLUGS`): **9** — `seal-e2e-2026`, `seal-fall-2025-finished`, `seal-gd3-prelim-open`, `seal-gd4-advance-ready`, `seal-gd4-tiebreak-submission-time`, `seal-gd4-tiebreak-manual`, `seal-gd4-wildcard-gap`, `seal-gd5-final-active`, `seal-gd6-pending-confirm`.
