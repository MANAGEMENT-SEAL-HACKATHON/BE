# Checklist ma trận nút GĐ1–GĐ6 — Nhóm K

> Mục tiêu: kiểm tra tuần tự các CTA theo lifecycle trên UI thật. Tester luôn ghi
> `slug`, `hackathonId`, `roundId`, role và thời điểm test. Sau mỗi thao tác phải
> quan sát UI cập nhật ngay, không nhấn F5.

## K1. Ma trận tuần tự GĐ3 (Sơ loại)

| # | Trạng thái đầu | Nút / thao tác | Gate cần đúng | Kỳ vọng ngay sau click | Bước được mở tiếp |
|---|----------------|----------------|-----------------|-------------------------|-------------------|
| 1 | Vòng chưa active, đủ lottery/readiness | **Kích hoạt sớm** | Activate prelim hợp lệ | Vòng thành Active; nếu `now < examAt` hiển thị Waiting/countdown | Chờ tới giờ thi |
| 2 | Active, `now < examAt` | **Phát đề** không được click (ẩn hoặc disabled đúng thiết kế) | `canReleaseProblem=false` | Không gọi API; tooltip/countdown nói chưa tới giờ thi | Khi `now >= examAt`, Phát đề mở |
| 3 | Active, tới `examAt`, chưa phát đề | **Phát đề** | `canReleaseProblem=true` | Ghi nhận `problemReleasedAt`; màn nộp bài và trạng thái vòng đổi ngay | Theo dõi trạng thái nộp |
| 4 | Đã phát đề, cổng nộp còn mở | Mở **Trạng thái bài nộp** | Round active + problem released | Danh sách đội/trạng thái nộp đúng dữ liệu mới nhất | **Kết thúc thời gian thi sớm** khả dụng |
| 5 | Đã phát đề, đã tới giờ thi, chưa đóng | **Kết thúc thời gian thi sớm** | `canCloseEarly=true` | Ghi nhận `submissionClosedEarlyAt`; cổng nộp đóng và UI đổi ngay | Mở hàng đợi / shuffle |
| 6 | Submission closed, không còn `LATE_PENDING` | **Khởi Động Máy Quay Số** / shuffle | `canShuffleQueue=true` | Sinh thứ tự thuyết trình, hiển thị queue ngay | Cấp quyền controller |
| 7 | Queue đã tạo | **Chuyển quyền** controller | Queue/session hợp lệ | Người nhận trở thành controller; các client nhận thay đổi không F5 | Điều khiển trình bày/chấm |
| 8 | Đang thuyết trình | Judge nhập điểm và **HOÀN TẤT & CHỐT SỔ ĐIỂM** | Đúng judge/assignment; presentation hợp lệ | Điểm và trạng thái hoàn tất cập nhật ngay | Khi mọi presentation hoàn tất, Lock mở |
| 9 | Submission closed + shuffled + presentations complete | **Khóa chấm điểm** | `canLockScoring=true` | `scoringLocked=true`, hiện trạng thái **Đã đóng sổ** | Trophy / trang kết quả GĐ4 |
| 10 | Điểm Sơ loại đã khóa | **Công bố kết quả** rồi **Chốt chuyển vòng** | Publish: locked, chưa publish; Advance: đã publish, ranking hợp lệ, không tiebreak chưa xử lý | Mỗi click cập nhật stepper/trạng thái ngay | Cấu hình và kích hoạt Chung kết |

### Điểm chết phải chặn trong GĐ3

| Tình huống | Nút phải khóa | Kỳ vọng |
|------------|---------------|---------|
| Chưa tới `examAt` | Phát đề | Không gọi API; lý do Waiting rõ ràng |
| Chưa phát đề hoặc chưa tới `examAt` | Kết thúc sớm | Không gọi API; không cho đi tắt |
| Còn đội `LATE_PENDING` | Shuffle / Máy quay số | Disabled; yêu cầu duyệt hoặc từ chối bài nộp muộn |
| Chưa shuffle hoặc còn presentation chưa hoàn tất | Khóa chấm | Disabled với đúng blocker |
| Đã khóa chấm | Release, Close early, Queue, Lock | Ẩn; chỉ SUPERADMIN thấy **Mở lại khóa chấm** |

## K2. Quy tắc chức năng chung

1. Nút chỉ clickable khi gate tương ứng trả về `true`; gate `false` thì ẩn hoặc
   disabled theo ma trận, không được gửi mutation.
2. Mutation thành công phải cập nhật cache/state, badge, stepper và CTA liên quan
   ngay trên UI, không cần F5.
3. Sau khi bước hiện tại thành công, CTA của bước kế tiếp phải tự mở; CTA cũ phải
   ẩn/khóa để không gửi lặp.
4. Mutation lỗi phải giữ nguyên bước hiện tại, hiển thị lỗi có thể hành động và
   không mở bước kế tiếp.

## K3. Ma trận ngắn các giai đoạn còn lại

### GĐ1 — Setup và mở đăng ký

| Trạng thái | CTA chính | Gate / kết quả |
|------------|-----------|----------------|
| DRAFT, setup chưa đủ | **Xác nhận Kích hoạt** disabled | Readiness ONGOING liệt kê blocker |
| DRAFT, đủ round/track/criteria/KICKOFF | **Xác nhận Kích hoạt** enabled | Click chuyển `status=ONGOING`, UI đổi ngay |
| ONGOING | CTA kích hoạt ẩn | Mở luồng đăng ký GĐ2 |

### GĐ2 — Đóng đăng ký, lottery, mở Sơ loại

| Trạng thái | CTA chính | Gate / kết quả |
|------------|-----------|----------------|
| Đăng ký còn mở / đội chưa khóa | **Bốc thăm** disabled | Không được lottery sớm |
| Sau `registrationEnd`, đội hợp lệ và đã khóa | **Bốc thăm** enabled | Gán track/bảng và cập nhật danh sách ngay |
| Lottery hoàn tất, prelim inactive | **Kích hoạt Sơ loại** enabled | Active; chuyển GĐ3 Waiting hoặc Release-ready |

### GĐ4 — Công bố và chuyển vòng (không kiểm wildcard)

| Trạng thái | CTA chính | Gate / kết quả |
|------------|-----------|----------------|
| Sơ loại chưa khóa điểm | Công bố và chuyển vòng đều khóa | Không cho công bố sớm |
| Đã khóa, chưa publish, ranking hợp lệ | **Công bố kết quả** | Published; announcement/stepper cập nhật không F5 |
| Đã publish, chưa advance, không tiebreak tồn đọng | **Chốt chuyển vòng** | Tạo danh sách đội CK; mở **Cấu hình CK** |
| CK đủ readiness + judge | **Kích hoạt Chung kết** | Final active và phát đề theo lifecycle CK |

### GĐ5 — Chung kết (không có Phát đề)

| Trạng thái | CTA chính | Gate / kết quả |
|------------|-----------|----------------|
| CK inactive, readiness đạt | **Kích hoạt Chung kết** tại Final Config | Activate đồng thời release; không hiện nút Phát đề |
| CK active, cổng còn mở | Trạng thái nộp / **Kết thúc sớm** | Close early chỉ mở khi đúng gate |
| Submission closed, không `LATE_PENDING` | **Khởi Động Máy Quay Số** | Tạo queue; sau đó chuyển controller và chấm |
| Queue hoàn tất, mọi presentation complete | **Khóa chấm điểm** | Khóa CK; hackathon sang `PENDING_CONFIRM` |

### GĐ6 — Trao giải và kết thúc

| Trạng thái | CTA chính | Gate / kết quả |
|------------|-----------|----------------|
| `PENDING_CONFIRM` | **Trao giải mới** / **Thu hồi** | Danh sách giải cập nhật ngay |
| Có giải và awards readiness đạt | **Chốt sổ & Công bố kết quả** | Hackathon chuyển `FINISHED`; student nhận trạng thái mới |
| `FINISHED` | **Xuất CSV xếp hạng** | Tải CSV hợp lệ; các CTA mutation bị ẩn/khóa |

## K4. Acceptance criteria

### TC-BTN-G3-SEQ

- Chạy đúng thứ tự 10 bước ở K1 trên một prelim round sạch.
- Mỗi CTA chỉ mở khi gate của chính nó đúng; click thành công cập nhật UI không F5.
- Không thể bỏ qua Release, Close, Shuffle, controller/scoring hoặc Lock.
- Sau Lock, trang kết quả GĐ4 mở được và CTA cũ của GĐ3 biến mất.

### TC-BTN-DEAD

- Thử click/trigger bằng UI ở từng tình huống trong bảng “Điểm chết”.
- Không request mutation nào được gửi khi gate false.
- Tooltip/message nêu đúng blocker; refresh trang không làm nút mở sai.
- Nếu cố gọi API trực tiếp, BE từ chối và UI không tự mở bước kế tiếp.

### TC-BTN-G5-SEQ

- Activate CK tại Final Config; xác nhận không có nút **Phát đề**.
- Đi theo chuỗi Activate/release tự động → Submission status → Close/deadline →
  Shuffle → Controller → Scoring → Lock.
- Mọi chuyển bước phản ánh ngay không F5; Lock cuối chuỗi đưa hackathon sang
  `PENDING_CONFIRM`.

### TC-BTN-G4-SEQ

- Bắt đầu từ prelim `scoringLocked=true`; không thực hiện luồng wildcard.
- **Công bố kết quả** mở trước, **Chốt chuyển vòng** chỉ mở sau publish.
- Sau Advance, danh sách đội CK và bước **Cấu hình CK** xuất hiện ngay.
- Chỉ khi readiness CK và judge hợp lệ thì **Kích hoạt Chung kết** clickable.
