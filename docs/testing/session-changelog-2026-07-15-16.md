# Session Changelog: 15/07 – 17/07/2026

**Bối cảnh:** Báo cáo tổng hợp các bản vá lỗi P0 về logic vận hành, workflow và UX/FE từ sáng 15/07 đến sáng 17/07. Các thay đổi tập trung vào đồng bộ state Coord↔Student, vá lỗ hổng nghiệp vụ (Wildcard, Timer, Chấm điểm), chuẩn hóa UI phi kỹ thuật, và **luồng onboard giám khảo khách (PENDING → APPROVED)**.

**Trạng thái nhánh (tham chiếu lúc cập nhật doc 17/07):** BE gần `5b47087` / FE gần `818da5e` — working tree có thể còn uncommitted (guest-judge PENDING, activate tooltip, …).

**Playbook liên quan:** [manual-ui-playbook-gd1-gd6.md](manual-ui-playbook-gd1-gd6.md) (cập nhật cùng phiên — happy path click-by-click non-IT).

---

## 1. Bảng Map Yêu Cầu → Trạng Thái (DONE)

### GĐ1: Khởi tạo & Cấu hình (UX/UI)

* **[DONE]** Loại bỏ các thuật ngữ IT jargon, thay bằng ngôn ngữ thường trên form tạo sự kiện.
* **[DONE]** Form tạo sự kiện: Ẩn/Disabled tự tính ngày "Bắt đầu/Kết thúc" và "BXH cá nhân".
* **[DONE]** Sửa lỗi tạo Vòng chung kết bị crash khi thay đổi thời lượng thi (form default duration bug).
* **[DONE]** Modal Activate: Chế độ "Đổi lịch" (Reschedule) đã chặn logic bắt đầu ngay lập tức, bắt buộc chờ đến ngày đã đổi.
* **[DONE]** Modal Activate: Chế độ "Bắt đầu sớm" (START_NOW) cho phép setup lead time (vài phút) và kích hoạt ngay.
* **[DONE] (17/07)** Bỏ tab **Đánh giá & Kiểm tra** — nút **Xác nhận Kích hoạt** nằm góc phải header setup; blockers hiện trong **Tooltip** (icon ℹ️ + hover nút), không còn Alert vàng full-width.
* **[DONE] (17/07)** Events: không còn tạo loại **PRESENTATION** từ modal; timeline tạo = KICKOFF → WORKSHOP → AWARDS (+ OTHER).
* **[DONE] (17/07)** Timer thuyết trình / Q&A: gửi `defaultPresentationMinutes` / `defaultQaMinutes` từ FE lên BE cho cả vòng Sơ loại (không chỉ CK).
* **[DONE] (17/07)** Trạng thái vòng trên UI: **Badge** (không Switch) + gate `canActivateRound` FE/BE.
* **[DONE] (17/07)** Nhân sự: loading nút gán mentor/GK; dropdown xám người đã gán / conflict mentor↔judge cùng bảng; refresh assignment không full-page reload.

### GĐ1b: Giám khảo khách — onboard (17/07)

* **[DONE]** Tạo temp judge: `status=PENDING` + `mustChangePassword=true` (không còn APPROVED ngay sau gửi mail).
* **[DONE]** Auth deadlock fix: `assertApproved` cho phép login khi `isTempAccount && EXTERNAL && JUDGE && PENDING && mustChangePassword`.
* **[DONE]** `POST change-password` (cùng transaction): clear `mustChangePassword` + set `APPROVED`; đánh dấu invitation `acceptedAt`.
* **[DONE]** List temp-judges expose `mustChangePassword` + `invitation.expiresAt` / `tokenSent`.
* **[DONE]** Resend invitation: **luôn** tạo MK tạm mới; cho phép resend ngay khi `lastTokenSent=false` (email fail) dù token còn hạn 72h.
* **[DONE]** FE badge thứ tự: Email chưa gửi → Lời mời hết hạn → Chờ đổi mật khẩu (tooltip) → Đã duyệt.
* **[DONE]** Pool gán GK Chung kết: loại guest chưa `APPROVED` / còn `mustChangePassword`.
* **SQL manual:** [`V20260717_invitation_last_token_sent.sql`](../../src/main/resources/db/manual/V20260717_invitation_last_token_sent.sql) — cột `invitations.last_token_sent` (ddl-auto=update cũng áp dụng).

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
| Guest judge create / list | `TempJudgeServiceImpl`, `UserMapper.toSummary(u, inv)` |
| Guest login / đổi MK | `AuthService.assertApproved` + `changePassword` |
| Resend MK tạm mới | `InvitationServiceImpl.resend` + `Invitation.lastTokenSent` |
| Activate header tooltip | `HackathonSetupPage.jsx` (không tab review) |

---

## 2. Các Mảng Ngoài Scope (KNOWN_GAP — xử lý sau)

* Chưa xóa API/Logic Wildcard dựa trên top-max của tài liệu cũ (để bảo vệ GĐ4).
* Guest xóa email **trong** cửa sổ 72h còn hạn + `lastTokenSent=true`: Resend API vẫn chặn (`INVITATION_STILL_VALID`) — Coord chờ hết hạn rồi Resend (MK mới), hoặc đợi edge case mở rộng “force resend khi user quên MK” nếu PO yêu cầu.
* Record guest **cũ** (đã `APPROVED` trước fix 17/07): **không backfill** — UI vẫn dùng `mustChangePassword` để phân biệt «Chờ đổi mật khẩu» nếu còn flag.

~~* Invite Giám khảo tự động duyệt (chưa có luồng gửi Email 72h).~~ → **Đóng (17/07):** PENDING sau mời; APPROVED sau đổi MK; invitation expiry 72h + Resend rotate MK.

~~* Đưa cấu hình Timer Thuyết trình/Q&A từ cấp Track lên cấp Round.~~ → **Đóng (17/07):** FE gửi `defaultPresentationMinutes` / `defaultQaMinutes` theo Round (prelim + final).

---

## 3. Verify nhanh

| Kiểm | Kỳ vọng |
|-------|---------|
| `npm run probe:seeds` (FE, BE đã seed) | **26/26** |
| Mode A | Mở đúng happy slug theo [manual-ui-playbook-gd1-gd6.md](manual-ui-playbook-gd1-gd6.md) §0.6 |
| Mode B / full-chain API | `node scripts/gd3-gd4-gd5-full-chain-api.mjs` |
| Sau Confirm GĐ6 trên `seal-e2e-2026` | **Restart BE** (create-drop + repair) rồi mới `probe:seeds` |
| Unit guest judge (BE) | `mvn -Dtest=TempJudgeServiceImplTest,AuthServiceTest,InvitationServiceImplTest test` |
| Manual GĐ1 activate | DRAFT thiếu điều kiện → hover ℹ️ / nút → danh sách blockers; đủ điều kiện → nút sáng → ONGOING |
| Manual mời guest | Tab Nhân sự → badge **Chờ đổi mật khẩu** (không «Đã duyệt» ngay); login + đổi MK → **Đã duyệt** → mới gán CK |

Happy slug seed (`DevSeedCatalog.ALL_DEV_HACKATHON_SLUGS`): **9** — `seal-e2e-2026`, `seal-fall-2025-finished`, `seal-gd3-prelim-open`, `seal-gd4-advance-ready`, `seal-gd4-tiebreak-submission-time`, `seal-gd4-tiebreak-manual`, `seal-gd4-wildcard-gap`, `seal-gd5-final-active`, `seal-gd6-pending-confirm`.
