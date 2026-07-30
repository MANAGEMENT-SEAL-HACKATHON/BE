# Demo Flow GĐ1–GĐ6 (tóm tắt click)

> **Nguồn:** [manual-ui-playbook-gd1-gd6.md](manual-ui-playbook-gd1-gd6.md) (cập nhật header **2026-07-21**).  
> **Mục đích:** phiếu demo ngắn — chuỗi nút theo giai đoạn. Chi tiết modal/ErrorCode xem playbook.  
> **Tài khoản:** Coord `coord@fpt.edu.vn` / `Coordinator@dev1` · SV / Judge / Guest theo seed từng GĐ.

---

## Kiểm tra nhanh playbook đã “mới” chưa?

| Đã có trong playbook (§0.4c / GĐ) | Còn lệch / thiếu so với FE gần đây |
|-----------------------------------|-------------------------------------|
| Nhân bản năm editable; clone không copy lịch vòng | Modal kích hoạt: **KEEP only** (START_NOW đã gỡ phase 2); dời lịch qua «Dời lịch thi» / đóng ĐK sớm |
| Trao giải BXH CK + `PRIZE_TEAM_NOT_FINALIST` | UX đóng ĐK: Collapse «Quy tắc lịch», thẻ số modal kết quả — **chưa** ghi rõ §0.4c |
| Quay lại deep-link; post-lock ẩn BXH tạm | Timer: Progress «Giám khảo đã chốt X/Y»; copy VN panel — **chưa** ghi rõ |
| Cảnh báo Q&A ≤ **1/3** thời lượng | Seed `Criteria.description` + Tag trọng số trên Collapse rubric — **chưa** ghi rõ |
| Lists pageSize 10 / card 9; guestjudge ×2 | API lottery đúng **PATCH** `/lottery` (một số đoạn cũ dễ nhầm POST) |

**Kết luận:** Playbook đủ để demo GĐ1–GĐ6 happy; phần UX Coord/Judge vừa polish (đóng ĐK / rubric description / timer Progress) nên bổ sung playbook sau nếu cần audit UI chi tiết.

---

## Chuẩn bị demo (1 lần)

1. Start BE (`dev`) → đợi seed xong  
2. Start FE → `http://localhost:5173`  
3. **Mode A (nhanh):** mở đúng slug từng GĐ bên dưới  
4. **Mode B (full):** Login Coord → tạo 1 sự kiện → đi GĐ1→GĐ6 trên cùng kỳ (hoặc `seal-e2e-2026` rồi nhảy snapshot)

---

## GĐ1 — Setup & mở đăng ký (DRAFT → ONGOING)

**Slug Mode A:** `seal-e2e-2026` (chỉ verify tab) · **Mode B:** tạo mới

```
Login Coord
  → (press) Tạo sự kiện / Tạo sự kiện mới
  → (create) Hackathon (tên, mùa, năm, ngày ĐK / sự kiện…) → Lưu
  → Thiết lập
  → Tab Vòng thi → Thêm vòng thi ×2 (Sơ loại + Chung kết) → Lưu
  → Tab Bảng đấu → Thêm bảng (track) → Upload PDF đề (SL)
  → Tab Tiêu chí đánh giá → Thêm tiêu chí (tổng trọng số = 1.0)
  → Tab Nhân sự → Gán Judge/Mentor Sơ loại
  → Tab Lịch trình & Sự kiện → Thêm KICKOFF → (khuyến nghị) WORKSHOP → AWARDS
  → (press) Xác nhận Kích hoạt (header)
  → Status ONGOING — đăng ký mở
```

**Nhớ:** Nút này = mở hackathon, **không** phải kích hoạt vòng thi.

---

## GĐ2 — Đội · đóng ĐK · bốc thăm · kích hoạt Sơ loại

**Slug:** `seal-e2e-2026` (sau GĐ1 / repair ĐK còn mở)

```
Login Coord
  → /teams?hackathonId=… → Duyệt đội PENDING (ACTIVE)
  → Setup → Cấu hình chung
  → (press) Kết thúc đăng ký sớm
  → (chọn) Giờ thi Sơ loại + xem preview lịch → Xác nhận đóng & lưu lịch
  → Modal kết quả → (nếu còn) Xử lý N đội đang chờ → duyệt/từ chối
  → (press) Bốc thăm & Khai mạc  (hoặc tab Bốc thăm & khai mạc)
  → (press) Bốc thăm Tự động (Cho đội chưa có)
  → Tab Vòng thi
  → (press) Kích hoạt Vòng thi (Sơ loại)
  → (chọn) KEEP — xác nhận kích hoạt (examAt ≤ now; nếu còn future → «Dời lịch thi» trước). ~~START_NOW đã gỡ (phase 2).~~
  → Xác nhận → vòng Sơ loại Active
```

**Tuỳ chọn:** Tab Vòng thi → **Dời lịch thi** (1 lần, ≥4 ngày trước Khai mạc) nếu chưa `scheduleAdjustedAt`.

---

## GĐ3 — Sơ loại: phát đề · nộp · queue · chấm · khóa

**Slug Mode A:** `seal-gd3-prelim-open`

```
Login Coord → mở slug → Vòng thi
  → (press) Phát đề bài / Phát tất cả   (sau khi tới giờ thi; early-wait thì nút disabled)
Login SV (leader đội)
  → /student/submit → tab Sơ loại
  → (điền) repo + upload PDF slide
  → (press) Nộp bài Sơ loại
Login Coord → Vòng thi
  → (press) Kết thúc thời gian thi sớm → Xác nhận (KHÔNG HOÀN TÁC)
  → (press) Mở hàng đợi thuyết trình / vào /presentation/queue
  → (press) Khởi Động Máy Quay Số (sau đóng cổng nộp)
  → (press) Phân quyền / Chuyển quyền điều phối đồng hồ (controller)
Login Judge (controller / GK)
  → /judge/dashboard → Vào phòng chấm thi
  → (press) Bắt đầu tính giờ → … → Hỏi đáp → HOÀN TẤT & CHỐT SỔ ĐIỂM
  → (press) Kết thúc & gọi đội kế tiếp  (khi phase ENDED + đủ điều kiện)
  → Lặp đủ đội (optional: Bỏ qua đội no-show)
Login Coord → Vòng thi
  → (press) Khóa chấm điểm → Xác nhận Khóa
```

---

## GĐ4 — Kết quả SL · Top-N · cấu hình & kích hoạt CK

**Slug Mode A:** `seal-gd4-advance-ready` · Tiebreak: `seal-gd4-tiebreak-*`

```
Login Coord
  → /hackathons/{id}/rounds/{prelimId}/results
  → (nếu banner đỏ) Tab Đồng điểm → kéo-thả biên Top-N → Lưu
  → (press) Công bố kết quả
  → (press) Chốt chuyển vòng → xác nhận
      → Top-N mỗi bảng = ADVANCED; ngoài Top-N = ELIMINATED
  → Cấu hình Chung kết (setup tab /final-config)
  → Nhân sự → gán Guest Judge FINAL_EXTERNAL (guestjudge@ / guestjudge2@)
  → Tab Vòng thi → (press) Kích hoạt Vòng thi (Chung kết)
  → KEEP → CK Active (tự đóng dấu đề — không Phát đề CK). ~~START_NOW đã gỡ (phase 2).~~
```

**Không còn:** tab Vé vớt / Wildcard.

---

## GĐ5 — Chung kết: nộp · queue · guest chấm · khóa → PENDING_CONFIRM

**Slug Mode A:** `seal-gd5-final-active`

```
Login SV (đội đã ADVANCED)
  → /student/submit → tab Chung kết
  → (điền) repo + PDF → (press) Gửi Bài Dự Thi Chung Kết
Login Coord → Vòng thi (CK)
  → (press) Kết thúc thời gian thi sớm → Xác nhận
  → Mở hàng đợi thuyết trình → Khởi Động Máy Quay Số
  → Chuyển quyền controller (nếu cần)
Login Guest Judge
  → /judge/dashboard → Vào phòng chấm thi
  → Chấm đủ đội → HOÀN TẤT & CHỐT → Next (cùng FSM timer như GĐ3)
Login Coord
  → (press) Khóa chấm điểm (CK) → Xác nhận
  → Hackathon status = PENDING_CONFIRM
```

---

## GĐ6 — Trao giải · chốt sổ · FINISHED · xuất CSV

**Slug Mode A:** `seal-gd6-pending-confirm`

```
Login Coord
  → /hackathons/{id}/results
  → (press) Trao giải mới → chọn đội từ BXH CK → Lưu (≥1 giải)
  → (press) Chốt sổ & Công bố kết quả → xác nhận (KHÔNG HOÀN TÁC)
  → Status FINISHED
  → (press) Xuất CSV xếp hạng
Login SV
  → /student/results → xem BXH / lifecycle banner
```

---

## Một dòng nhớ nhanh (full chain)

| GĐ | Flow một dòng |
|----|----------------|
| **1** | Login → Tạo sự kiện → Setup (vòng/bảng/tiêu chí/người/sự kiện) → **Xác nhận Kích hoạt** → ONGOING |
| **2** | Duyệt đội → **Kết thúc ĐK sớm** → **Bốc thăm** → **Kích hoạt vòng SL** (KEEP; dời lịch trước nếu examAt còn future) |
| **3** | **Phát đề** → SV **Nộp SL** → **Kết thúc thi sớm** → Shuffle queue → Judge chấm/chốt → **Khóa chấm** |
| **4** | Results → (Đồng điểm) → **Công bố** → **Chốt chuyển vòng** → Cấu hình CK → **Kích hoạt CK** |
| **5** | SV **Nộp CK** → End-early → Queue + Guest chấm → **Khóa chấm CK** → PENDING_CONFIRM |
| **6** | **Trao giải** → **Chốt sổ & Công bố** → FINISHED → **Xuất CSV** |

---

## Slug demo theo GĐ (Mode A)

| GĐ | Slug |
|----|------|
| 1–2 | `seal-e2e-2026` |
| 3 | `seal-gd3-prelim-open` |
| 4 | `seal-gd4-advance-ready` |
| 5 | `seal-gd5-final-active` |
| 6 | `seal-gd6-pending-confirm` |
