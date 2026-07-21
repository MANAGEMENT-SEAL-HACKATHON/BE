# Test case — SEAL Hackathon Management System

| Field | Value |
|-------|--------|
| **Module Code** | GĐ2-GĐ3 Coord/Judge |
| **Module name** | Đóng ĐK sớm / Bốc thăm / Dời lịch / Kích hoạt SL / Rubric / Timer |
| **Tester** | |
| **Pass** | 0 |
| **Fail** | 0 |
| **Percent Complete** | 0% |
| **Untested** | 60 |
| **N/A** | 0 |
| **Number of cases** | 60 |

**Tài khoản seed:** Coord `coord@fpt.edu.vn` / `Coordinator@dev1`  
**Slug gợi ý:** `seal-e2e-2026`, `seal-gd2-registration-closed`, `seal-gd2-lottery-not-locked`, `seal-gd2-round-active`, `seal-gd3-scoring-live`  
**API:** `POST .../close-registration-early` · `PATCH .../lottery` · `POST .../competition-schedule/preview|adjust` · `PATCH .../rounds/{id}/activate`  
**Status mặc định:** `Not Run` · **Test date:** để trống  

**6 features × 10 TC:** A Đóng ĐK · B Kích hoạt SL · C Rubric · D Timer · **E Bốc thăm** · **F Dời lịch thi**

---

## Feature A — Đóng đăng ký sớm + chọn / preview lịch + modal kết quả

| ID | Test Case Description | Pre-Condition | Test Case Procedure | Test Data | Expected Output | Status | Test date (dd/mm/yyyy) | Note |
|----|----------------------|---------------|---------------------|-----------|-----------------|--------|------------------------|------|
| TC-A01 | Kiểm tra đóng đăng ký sớm thành công kèm chọn giờ Sơ loại | BE seed sẵn; hackathon `seal-e2e-2026` status ONGOING; đăng ký còn mở; `scheduleAdjustedAt` = null; đã login Coordinator | 1. Mở trang thiết lập hackathon → tab Cấu hình chung. 2. Bấm **Kết thúc đăng ký sớm**. 3. Kiểm tra Alert và Collapse «Quy tắc lịch tự động». 4. Giữ hoặc chọn giờ thi Sơ loại ≥ minDay và sau thời điểm hiện tại. 5. Đợi bảng preview tải xong. 6. Bấm **Xác nhận đóng & lưu lịch**. | email: `coord@fpt.edu.vn`; password: `Coordinator@dev1`; slug: `seal-e2e-2026`; `newPrelimExamAt` hợp lệ (ISO) | Toast: «Đã đóng ĐK sớm, cập nhật lịch và gửi thông báo mentor / giám khảo / sinh viên / BTC»; mở modal **Kết quả đóng đăng ký sớm**; response có `lockedActiveTeams`, `withdrawnOrphans`, `rejectedIncompleteTeams` | Not Run | | Happy Path · `HackathonRegistrationCloseServiceImpl` |
| TC-A02 | Kiểm tra bảng preview lịch hiển thị nhãn tiếng Việt và format ngày giờ | Đang mở modal đóng ĐK sớm (như bước 1–2 TC-A01) | 1. Chọn giờ thi Sơ loại hợp lệ. 2. Quan sát cột Hạng mục / Hiện tại / Sau khi lưu trên bảng preview. | Giờ SL bất kỳ hợp lệ trong DatePicker | Key `WORKSHOP`/`KICKOFF`/`PRELIM`/`FINAL`/`AWARDS` map sang label VN (Workshop, Khai mạc, Thi Sơ loại…); giá trị ngày giờ dạng `DD/MM/YYYY HH:mm` | Not Run | | FE `displayChanges` / `FRIENDLY_LABELS` |
| TC-A03 | Kiểm tra hủy modal đóng ĐK — không lưu thay đổi | Modal đóng ĐK sớm đang mở | 1. Có thể đổi giờ SL (không bắt buộc). 2. Bấm **Hủy (không lưu)** hoặc đóng modal. | — | Modal đóng; đăng ký vẫn mở; không có `registrationClosedEarlyAt` | Not Run | | Alternative |
| TC-A04 | Kiểm tra chỉnh chi tiết Workshop / Khai mạc / Chung kết / Trao giải rồi xác nhận | Modal đóng ĐK mở; giờ SL đã chọn | 1. Bấm link chỉnh chi tiết Workshop / Khai mạc / Chung kết / Lễ trao giải. 2. Đổi giờ WS/KO trong đúng ngày (DatePicker khóa ngày). 3. Đặt CK trong cửa sổ +1–2h sau khi SL kết thúc; Awards sau hạn nộp CK. 4. Bấm **Xác nhận đóng & lưu lịch**. | WS ngày regEnd+1; KO ngày regEnd+2; CK ∈ [prelimEnd+1h, prelimEnd+2h]; Awards > finalExamAt+2h | FE gửi `newPrelimExamAt` + `overrides` (ISO `YYYY-MM-DDTHH:mm:ss`); đóng ĐK thành công như TC-A01 | Not Run | | Payload `CloseRegistrationEarlyRequest` |
| TC-A05 | Kiểm tra DatePicker chặn chọn ngày Sơ loại quá khứ hoặc trước minDay | Modal đóng ĐK đang mở | 1. Mở DatePicker giờ thi Sơ loại. 2. Thử chọn ngày trước hôm nay hoặc trước minDay (regEnd+3 khi close-reg). | Ngày &lt; today hoặc &lt; minDay | Ngày bị `disabledDate`; nút OK disabled nếu `newExamAt` không after now | Not Run | | FE validation |
| TC-A06 | Kiểm tra validateLocal chặn Chung kết ngoài cửa sổ +1–2h sau Sơ loại | Modal mở; đã bật chỉnh chi tiết | 1. Đặt giờ Chung kết trước `prelimEnd+1h` hoặc sau `prelimEnd+2h`. 2. Bấm OK xác nhận. | `finalExamAt` ngoài khoảng | Alert lỗi đỏ từ `validateLocal`; không gọi `onConfirm` / không gọi API đóng | Not Run | | FE only |
| TC-A07 | Kiểm tra validateLocal chặn Lễ trao giải trước hoặc bằng hạn nộp Chung kết | Modal mở; chỉnh Awards | 1. Đặt Awards ≤ `finalExamAt + 2 giờ`. 2. Bấm OK. | Awards ≤ finalDeadline | Alert: Lễ trao giải phải sau hạn nộp Chung kết…; không submit | Not Run | | FE only |
| TC-A08 | Kiểm tra BE chặn đóng ĐK khi đăng ký đã đóng | Hackathon đã đóng ĐK (sau TC-A01 hoặc slug `seal-gd2-registration-closed`) | 1. UI: mở Cấu hình chung — kiểm tra không còn nút **Kết thúc đăng ký sớm** (đã đóng). 2. (API) `POST /api/v1/hackathons/{id}/close-registration-early` với body có `newPrelimExamAt`. | Body hợp lệ có `newPrelimExamAt` | UI: tag/đã đóng đăng ký. API: `error.code` = `REGISTRATION_ALREADY_CLOSED` | Not Run | | `isRegistrationClosed` |
| TC-A09 | Kiểm tra BE trả lỗi khi thiếu newPrelimExamAt hoặc hackathon không ONGOING | Case 1: hackathon DRAFT. Case 2: gọi API thiếu field | 1. DRAFT: xác nhận FE không hiện CTA đóng ĐK (`status !== ONGOING`). 2. API close trên DRAFT → kỳ vọng `HACKATHON_NOT_ONGOING`. 3. API close thiếu/null `newPrelimExamAt` → `VALIDATION_FAILED`. | Body `{}` hoặc `newPrelimExamAt: null`; hackathon DRAFT | FE không CTA khi không ONGOING; API đúng ErrorCode như trên | Not Run | | Controller có `@RequestBody` |
| TC-A10 | Kiểm tra modal kết quả CTA theo response và SV bị chặn tạo đội sau đóng ĐK | Sau TC-A01 thành công | 1. Xem 3 thẻ số trên modal kết quả. 2. Nếu awaiting+grace &gt; 0 → bấm **Xử lý N đội đang chờ**; nếu = 0 và có lottery → **Bốc thăm & Khai mạc**. 3. Login SV; thử tạo đội trên cùng hackathon. | Response thật; SV seed cùng hackathon | Thẻ = `lockedActiveTeams` / `withdrawnOrphans` / `rejectedIncompleteTeams`. Navigate teams `?hackathonId=&status=PENDING` khi có pending. SV: `REGISTRATION_CLOSED` | Not Run | | Không giả định slug nào chắc có awaiting |

---

## Feature B — Kích hoạt vòng Sơ loại (START_NOW / KEEP)

| ID | Test Case Description | Pre-Condition | Test Case Procedure | Test Data | Expected Output | Status | Test date (dd/mm/yyyy) | Note |
|----|----------------------|---------------|---------------------|-----------|-----------------|--------|------------------------|------|
| TC-B01 | Kiểm tra kích hoạt và bắt đầu sớm START_NOW kèm thời gian chuẩn bị | Đã đóng ĐK; lottery xong; prelim inactive; đủ criteria/judge; mọi track có đội; `examAt` còn tương lai; login Coord | 1. Tab **Vòng thi**. 2. Bấm kích hoạt vòng (`data-testid=round-activate-btn`). 3. Đặt thời gian chuẩn bị = 5 (hoặc giữ mặc định). 4. Bấm **Kích hoạt & bắt đầu sớm**. | `setupLeadMinutes=5`; `scheduleMode=START_NOW` | Gọi `PATCH /api/v1/rounds/{id}/activate` với `scheduleMode=START_NOW` và `setupLeadMinutes`; vòng `isActive`; giờ thi theo `calculateStartTime` | Not Run | | Happy · `ActivateScheduleModal` |
| TC-B02 | Kiểm tra preview giờ thi cập nhật theo lead minutes 1–30 | Modal kích hoạt mở; `examAt` future | 1. Đổi InputNumber lead lần lượt 1, 5, 30. 2. Thử nhập ngoài khoảng (nếu UI cho phép). | Lead 1 / 5 / 30 | Text helper «Giờ thi sẽ là …» đổi theo `formatExamPreview(calculateStartTime(...))`; giá trị clamp trong 1–30 | Not Run | | FE |
| TC-B03 | Kiểm tra chế độ KEEP khi examAt không còn ở tương lai | Round prelim `examAt` ≤ now; đủ điều kiện activate | 1. Mở modal kích hoạt. 2. Xác nhận không hiện ô lead START_NOW. 3. Bấm **Kích hoạt**. | `scheduleMode=KEEP` | Body không gửi `setupLeadMinutes`; kích hoạt giữ lịch đã xếp | Not Run | | FE `handleOk` |
| TC-B04 | Kiểm tra hủy modal kích hoạt vòng | Modal kích hoạt đang mở | 1. Bấm **Hủy**. | — | Modal đóng; không đổi `isActive` / không gọi activate thành công | Not Run | | Alternative |
| TC-B05 | Kiểm tra bắt đầu thi sớm khi vòng đã active và examAt còn tương lai | Round `isActive=true`; `examAt` future | 1. Mở modal (title kiểu «Bắt đầu thi sớm…»). 2. Set lead. 3. Xác nhận. | Lead 5; `START_NOW` | FE gửi `START_NOW` + `setupLeadMinutes`; BE nén exam/submission window (nhánh early-start) | Not Run | | `RoundActivationServiceImpl` |
| TC-B06 | Kiểm tra copy Alert modal không dùng viết tắt WS/KO/CK | Modal START_NOW đang mở | 1. Đọc toàn bộ Alert và dòng helper giờ thi. | — | Có chữ đủ «Workshop», «Khai mạc», «đóng đăng ký sớm», «Chung kết» — không viết tắt WS/KO/CK | Not Run | | UX |
| TC-B07 | Kiểm tra BE trả TRACK_EMPTY_TEAMS khi bảng đấu không có đội | Prelim có track nhưng track đó 0 đội (`teamRoundTrack` rỗng) | 1. Gọi `PATCH .../activate` (hoặc bấm kích hoạt trên UI nếu tới được gate). | Round/track seed gate | `error.code` = `TRACK_EMPTY_TEAMS` | Not Run | | `validateTeamsInRound` |
| TC-B08 | Kiểm tra BE trả ROUND_NO_CRITERIA khi track chưa có tiêu chí | Track không có criteria thường | 1. Thử kích hoạt vòng Sơ loại. | Track 0 criteria | `error.code` = `ROUND_NO_CRITERIA` | Not Run | | `validatePreliminaryRoundTracks` |
| TC-B09 | Kiểm tra BE trả ROUND_WEIGHT_NOT_ONE khi tổng weight track ≠ 1 | Track có criteria nhưng tổng weight (trừ PENALTY) không ≈ 1.0 | 1. Thử kích hoạt. | Weight invalid | `error.code` = `ROUND_WEIGHT_NOT_ONE` | Not Run | | `weightSummaryService.isValidForTrack` |
| TC-B10 | Kiểm tra BE trả JUDGE_NOT_ASSIGNED khi track chưa gán giám khảo | Track không có `JudgeAssignment` | 1. Thử kích hoạt. | Không judge trên track | `error.code` = `JUDGE_NOT_ASSIGNED` | Not Run | | |

---

## Feature C — Hướng dẫn chấm & rubric

| ID | Test Case Description | Pre-Condition | Test Case Procedure | Test Data | Expected Output | Status | Test date (dd/mm/yyyy) | Note |
|----|----------------------|---------------|---------------------|-----------|-----------------|--------|------------------------|------|
| TC-C01 | Kiểm tra Collapse rubric hiện description và Tag trọng số / thang điểm sau create-drop | BE đã restart create-drop; slug dùng helper criteria (vd. `seal-gd3-scoring-live`); login Judge vào Live Scoring | 1. Mở Collapse «Hướng dẫn chấm điểm & rubric». 2. Expand từng tiêu chí. | Criteria seed helper (4 tiêu chí) | Mỗi tiêu chí có `description` từ API; Tag «Trọng số x%»; Tag «Thang điểm 0–{maxScore}» | Not Run | | `JudgeScoringWorkspace` |
| TC-C02 | Kiểm tra trọng số / maxScore / số lượng criteria không đổi sau khi bổ sung description seed | Track đã có 4 criteria mặc định helper | 1. Coord mở Quản lý tiêu chí của track. 2. Kiểm tra weight, maxScore, số dòng. | Weights 0.30 / 0.30 / 0.20 / 0.20; maxScore 10 | Tổng weight = 1.0; maxScore = 10; vẫn đúng 4 criteria | Not Run | | Không regress scoring |
| TC-C03 | Kiểm tra Coord sửa description thì Judge thấy sau khi tải lại criteria | Cùng track; Coord + Judge | 1. Coord sửa `description` một tiêu chí và lưu. 2. Judge refresh / mở lại Collapse rubric. | Mô tả mới (text bất kỳ hợp lệ) | Collapse hiện đúng text mới từ API | Not Run | | Field `Criteria.description` |
| TC-C04 | Kiểm tra hiển thị link rubric khi criteria có rubricUrl | Coord gán `rubricUrl` hợp lệ cho một tiêu chí | 1. Judge mở Collapse tiêu chí đó. 2. Click «Xem rubric chi tiết». | URL https hợp lệ | Link hiện và mở tab mới | Not Run | | |
| TC-C05 | Kiểm tra fallback khi description null | Criteria không có mô tả (fixture / data cũ) | 1. Expand tiêu chí đó trên Live Scoring. | `description=null` | Hiện: «Chưa có mô tả tiêu chí. Coord có thể bổ sung tại màn Quản lý tiêu chí.» | Not Run | | Copy FE hiện tại |
| TC-C06 | Kiểm tra chốt điểm thành công trong giai đoạn Q&A | Đang Live Scoring; phase Q&A; Judge chưa chốt đội hiện tại | 1. Nhập điểm từng tiêu chí trong 0–10. 2. Bấm **HOÀN TẤT & CHỐT SỔ ĐIỂM**. | Điểm hợp lệ step 0.1 | Chốt thành công; form khóa theo trạng thái đã chốt (hành vi submit hiện có) | Not Run | | |
| TC-C07 | Kiểm tra FE chặn nhập điểm ngoài thang 0–10 | Form chấm đang mở; chưa chốt | 1. Thử kéo Slider / nhập InputNumber &gt; 10 hoặc &lt; 0. | Giá trị ngoài [0,10] | `InputNumber` và `Slider` có `min={0}` `max={10}` (hardcode FE) — không nhận ngoài khoảng | Not Run | | Không dynamic theo maxScore |
| TC-C08 | Kiểm tra seed Gd1 đủ description cho track 5 / track3 4 / final 5 | BE create-drop; mở slug Gd1 có criteria | 1. Coord xem tiêu chí track 1 hoặc 2 (5 dòng). 2. Xem track 3 (4 dòng). 3. Xem criteria Chung kết (5 dòng). | `Gd1DataSeeder` | Mỗi dòng có description; name/weight/order giữ như seed | Not Run | | Inventory 5+4+5 |
| TC-C09 | Kiểm tra ensureDefaultTrackCriteria idempotent — không backfill description lên criteria cũ | Track đã có criteria trước khi thêm field description vào seed | 1. Restart / re-seed **không** create-drop (hoặc gọi lại ensure khi đã có rows). 2. Kiểm tra criteria cũ. | Track đã có rows | Method return sớm khi `findByTrackId` không empty — **không** insert thêm; **không** tự cập nhật description cũ | Not Run | | Code `if (!…isEmpty()) return` |
| TC-C10 | Kiểm tra Alert hướng dẫn chấm nhắc chốt trong Q&A và cảnh báo ~1/3 thời gian | Judge không phải controller; đang Live Scoring | 1. Đọc Alert trong Collapse hướng dẫn. 2. Khi Q&A còn ≤ 1/3 thời lượng và chưa chốt — quan sát banner cảnh báo (nếu đủ điều kiện). | `qaMinutes` từ slot | Alert nhắc nhập đủ tiêu chí và HOÀN TẤT & CHỐT; banner `qa-scoring-deadline-warn` khi `shouldWarnQaScoringDeadline` true | Not Run | | FE workspace + gates |

---

## Feature D — Điều khiển timer Live Scoring

| ID | Test Case Description | Pre-Condition | Test Case Procedure | Test Data | Expected Output | Status | Test date (dd/mm/yyyy) | Note |
|----|----------------------|---------------|---------------------|-----------|-----------------|--------|------------------------|------|
| TC-D01 | Kiểm tra luồng Controller Start → QA → ENDED → gọi đội tiếp | User là Controller; có presentation queue; slot SETUP/IDLE | 1. Bấm **Bắt đầu tính giờ** (`START_OR_RESUME`). 2. Chuyển sang hỏi đáp (`QA`). 3. Đợi hết giờ hoặc early-end (`END`) tới phase `ENDED`. 4. Khi đủ điều kiện, bấm **Kết thúc & gọi đội kế tiếp** (`NEXT`). | Phase PRESENTING → QA → ENDED | Action đúng tên; nút Next chỉ khi `localTimerPhase === 'ENDED'` và `canCallNextTeam` true | Not Run | | `timerControlGates.canCallNextTeam` |
| TC-D02 | Kiểm tra Progress giám khảo đã chốt X/Y | Controller; phase QA hoặc ENDED; `judgesAssigned ≥ 1` | 1. Quan sát khối tiến độ chốt. | `judgesConfirmed` / `judgesAssigned` từ status | Hiện «Giám khảo đã chốt: X/Y» và Progress %; có `data-testid=judge-confirm-progress` | Not Run | | `JudgeTimerAndControls` |
| TC-D03 | Kiểm tra cảnh báo còn khoảng 1/3 thời gian hỏi đáp | Phase QA; remaining ≤ ceil(qaTotal/3); Controller; chưa đủ điều kiện early-end; có ≥1 GK | 1. Đưa đồng hồ Q&A về ngưỡng ≤ 1/3. 2. Quan sát Alert. | vd. qa 3 phút → ngưỡng ~60s | Alert `data-testid=qa-controller-deadline-warn` hiện | Not Run | | `shouldWarnQaScoringDeadline` |
| TC-D04 | Kiểm tra ẩn nút kết thúc sớm hỏi đáp khi chưa đủ giám khảo chốt | Phase QA; remaining &gt; 0; `allJudgesSubmitted !== true` | 1. Tìm nút **Kết thúc sớm hỏi đáp** (`early-end-qa-btn`). | — | Không render nút early-end (`canEarlyEndQa` = false vì chưa `allJudgesSubmitted`) | Not Run | | Code có check `allJudgesSubmitted` |
| TC-D05 | Kiểm tra hiện và dùng được kết thúc sớm hỏi đáp khi mọi GK đã chốt | Phase QA; remaining &gt; 0; `allJudgesSubmitted === true` | 1. Xác nhận nút early-end hiện. 2. Confirm Popconfirm **Kết thúc**. | — | Gọi `handleTimerAction('END')` | Not Run | | |
| TC-D06 | Kiểm tra nút Next theo canCallNextTeam khi phase ENDED | Phase `ENDED`; có `presentationScoringStatus` | 1. Quan sát nút Next. 2. Nếu không đủ điều kiện và `judgesAssigned ≥ 2` — đọc text chờ chốt. | `allJudgesSubmitted` hoặc `canAdvanceQueue` hoặc (`qaEndedEarly===false` và `judgesScored>0`) | Next hiện khi `canCallNextTeam` true; không hiện khi false (có thể kèm text chờ X/Y) | Not Run | | Không khẳng định «luôn ẩn nếu thiếu chốt» |
| TC-D07 | Kiểm tra tạm dừng và tiếp tục đồng hồ | Phase PRESENTING; remaining &gt; 0; Controller | 1. Bấm **Tạm dừng** (`PAUSE`). 2. Bấm **Tiếp tục đồng hồ** (`START_OR_RESUME`). | — | Phase PAUSED rồi trở lại PRESENTING; đồng hồ chạy lại | Not Run | | |
| TC-D08 | Kiểm tra Đặt lại đồng hồ chỉ hiện ở phase hợp lệ | Lần lượt các phase IDLE/SETUP/PRESENTING/QA/ENDED | 1. Ở PRESENTING: mở Popconfirm **Đặt lại đồng hồ** (`RESET`). 2. Ở QA hoặc ENDED hoặc IDLE/SETUP: xác nhận không hiện nút Reset. | — | Reset chỉ khi phase không thuộc IDLE, SETUP, QA, ENDED (khớp UI hiện tại) | Not Run | | |
| TC-D09 | Kiểm tra bỏ qua đội không có mặt (Skip no-show) | Controller; có `submissionId`; phase thuộc WAITING/PRESENTING/SETUP/IDLE/PAUSED | 1. Bấm **Bỏ qua đội này (không có mặt)**. 2. Confirm. | — | `handleTimerAction('SKIP_NOSHOW')`; có `data-testid=presentation-skip-noshow-btn` | Not Run | | |
| TC-D10 | Kiểm tra nhãn điều khiển timer bằng tiếng Việt | Panel Controller đang hiện | 1. Đọc tiêu đề panel và các nút chính. | — | «ĐIỀU KHIỂN THỜI GIAN»; «Đặt lại đồng hồ»; «hỏi đáp» — không còn nhãn «Reset Timer» tiếng Anh | Not Run | | UX |

---

## Feature E — Bốc thăm chia bảng (Lottery)

BE: `HackathonLotteryServiceImpl` · FE: tab **Bốc thăm & khai mạc** (`HackathonSetupPage`) · Gate: `PendingTeamGateService` (`TEAMS_PENDING_APPROVAL`)

| ID | Test Case Description | Pre-Condition | Test Case Procedure | Test Data | Expected Output | Status | Test date (dd/mm/yyyy) | Note |
|----|----------------------|---------------|---------------------|-----------|-----------------|--------|------------------------|------|
| TC-E01 | Kiểm tra bốc thăm tự động thành công cho đội ACTIVE đã khóa | ONGOING; ĐK đã đóng; đội ACTIVE `isLocked=true`; prelim chưa active; không còn đội PENDING chặn gate; login Coord | 1. Vào setup → tab **Bốc thăm & khai mạc**. 2. Chọn vòng Sơ loại. 3. Chạy bốc thăm tự động (không gửi assignments thủ công / để FE auto). | slug `seal-e2e-2026` sau đóng ĐK + xử lý pending; `roundId` prelim | Đội được gán track/group; API lottery thành công; không lỗi gate | Not Run | | Auto-lottery khi `assignments` empty |
| TC-E02 | Kiểm tra điều hướng từ modal kết quả đóng ĐK sang tab bốc thăm | Sau đóng ĐK; awaiting+grace = 0; có `onGoToLottery` | 1. Trên modal kết quả bấm **Bốc thăm & Khai mạc**. | — | Chuyển tab `lottery` trên setup page | Not Run | | `HackathonGeneralConfig` / `changeTab('lottery')` |
| TC-E03 | Kiểm tra BE chặn bốc thăm khi hackathon không ONGOING | Hackathon DRAFT hoặc không ONGOING | 1. Gọi API lottery với `roundId` hợp lệ. | `PATCH /api/v1/hackathons/{id}/lottery` | `error.code` = `HACKATHON_NOT_ONGOING` | Not Run | | |
| TC-E04 | Kiểm tra BE chặn bốc thăm khi đăng ký chưa kết thúc | ĐK còn mở (`canRunLottery` false vì period chưa end) | 1. Thử bốc thăm trên UI hoặc API. | Hackathon ĐK mở | `REGISTRATION_CLOSED` với message chưa hết hạn / cần đóng ĐK sớm (theo `HackathonLotteryServiceImpl`) | Not Run | | Message code dùng `REGISTRATION_CLOSED` khi period chưa end |
| TC-E05 | Kiểm tra BE chặn khi còn đội ACTIVE chưa khóa | Seed `seal-gd2-lottery-not-locked` (hoặc đội ACTIVE `isLocked=false` sau hết hạn ĐK) | 1. Thử bốc thăm. | Đội ACTIVE chưa lock | `error.code` = `ACTIVE_TEAMS_NOT_LOCKED` | Not Run | | |
| TC-E06 | Kiểm tra BE chặn khi còn đội PENDING (awaiting / grace) | Có đội PENDING đủ điều kiện gate | 1. Thử bốc thăm. | `assertNoPendingTeams` fail | `error.code` = `TEAMS_PENDING_APPROVAL` | Not Run | | `PendingTeamGateService` |
| TC-E07 | Kiểm tra BE chặn bốc thăm vòng Chung kết | `roundId` là final (`isFinal=true`) | 1. Gọi lottery với round CK. | Final round id | `error.code` = `INVALID_FINAL_ROUND` | Not Run | | |
| TC-E08 | Kiểm tra BE chặn bốc thăm khi vòng Sơ loại đã kích hoạt | Seed `seal-gd2-round-active` hoặc prelim `isActive=true` | 1. Thử bốc thăm / re-lottery. | Prelim active | `error.code` = `ROUND_ALREADY_ACTIVE` | Not Run | | |
| TC-E09 | Kiểm tra BE chặn gán đội không ACTIVE hoặc chưa khóa khi gửi assignment thủ công | Body có assignment team PENDING hoặc `isLocked=false` | 1. Gọi lottery với `assignments` chỉ định teamId/trackId. | Team không đủ điều kiện | `TEAM_NOT_ACTIVE` hoặc `TEAM_NOT_LOCKED` | Not Run | | Nhánh có assignments |
| TC-E10 | Kiểm tra BE chặn đội đã có track trong vòng (không gán trùng) | Đội đã có `TeamRoundTrack` trong round | 1. Gọi lottery gán lại cùng đội vào track khác. | Team already in round | `TEAM_ALREADY_IN_TRACK_THIS_ROUND` | Not Run | | ConflictException |

---

## Feature F — Dời lịch thi (Competition schedule adjust)

FE: `CompetitionScheduleAdjustModal` `mode="adjust"` · `RoundManagementPage` nút **Dời lịch thi** · API: `POST .../competition-schedule/preview` + `.../adjust`

| ID | Test Case Description | Pre-Condition | Test Case Procedure | Test Data | Expected Output | Status | Test date (dd/mm/yyyy) | Note |
|----|----------------------|---------------|---------------------|-----------|-----------------|--------|------------------------|------|
| TC-F01 | Kiểm tra dời lịch thi thành công một lần | ONGOING; `scheduleAdjustedAt` null; prelim chưa active / chưa phát đề; còn ≥ 4 ngày trước Khai mạc; login Coord | 1. Tab **Vòng thi** → **Dời lịch thi**. 2. Chọn giờ SL hợp lệ (≥ regEnd+3, after now). 3. Xem preview. 4. Bấm xác nhận dời lịch. | `newPrelimExamAt` + optional overrides | `adjustCompetitionSchedule` thành công; lịch cascade cập nhật; `scheduleAdjustedAt` được set | Not Run | | Happy |
| TC-F02 | Kiểm tra Alert mode adjust: dời 1 lần, ≥ 4 ngày trước Khai mạc | Modal dời lịch mở (`mode=adjust`) | 1. Đọc Alert đầu modal. | — | Message kiểu dời lịch một lần, ít nhất 4 ngày trước Khai mạc; có Collapse quy tắc lịch | Not Run | | UX copy mode adjust |
| TC-F03 | Kiểm tra hủy modal dời lịch — không lưu | Modal dời lịch mở | 1. Đổi giờ SL. 2. **Hủy (không lưu)**. | — | Không gọi adjust; lịch cũ giữ nguyên | Not Run | | |
| TC-F04 | Kiểm tra FE chặn OK khi preview `canAdjust=false` | Preview trả `canAdjust=false` / `blockReason` | 1. Mở modal; chọn giờ sao cho bị gate (hoặc quan sát khi block). 2. Kiểm tra nút OK. | `canAdjust === false` | Nút OK disabled; có thể hiện Alert `blockReason` | Not Run | | FE `okButtonProps` |
| TC-F05 | Kiểm tra BE / preview chặn khi đã dời lịch một lần | `scheduleAdjustedAt` ≠ null | 1. Mở dời lịch hoặc gọi adjust lần 2. | — | `SCHEDULE_ALREADY_ADJUSTED` (hoặc preview không cho adjust) | Not Run | | |
| TC-F06 | Kiểm tra chặn dời khi quá sát Khai mạc (&lt; 4 ngày) | Kickoff còn &lt; 4 ngày | 1. Thử dời lịch. | Now ≥ kickoff − 4 ngày | `SCHEDULE_ADJUST_TOO_LATE` (message chứa Khai mạc) | Not Run | | `gateException` |
| TC-F07 | Kiểm tra chặn giờ SL quá sớm so với registrationEnd | `newPrelimExamAt` trước regEnd+3 ngày | 1. Chọn ngày SL trước minDay. 2. Xác nhận / API. | examAt &lt; regEnd+3 | FE `disabledDate` và/hoặc BE `SCHEDULE_ADJUST_PRELIM_TOO_SOON` / VALIDATION | Not Run | | |
| TC-F08 | Kiểm tra chặn dời khi vòng Sơ loại đã kích hoạt hoặc đã phát đề | Prelim `isActive` hoặc `problemReleasedAt` ≠ null | 1. Thử dời lịch. | Round đã active / đã release đề | Preview/apply bị chặn (message: đã kích hoạt / đã phát đề — `VALIDATION_FAILED` qua gate) | Not Run | | `blockReason` trong `CompetitionScheduleAdjustService` |
| TC-F09 | Kiểm tra chỉnh chi tiết overrides rồi adjust thành công | Modal adjust; điều kiện TC-F01 | 1. Mở chỉnh chi tiết. 2. Đổi WS/KO/CK/Awards trong `validateLocal`. 3. Xác nhận. | overrides ISO hợp lệ | Lịch mới theo overrides; không phá lottery/khóa đội | Not Run | | Cùng modal với close-reg, mode khác |
| TC-F10 | Kiểm tra validateLocal CK / Awards trên mode adjust giống đóng ĐK | Modal adjust; mở chỉnh chi tiết | 1. Đặt CK ngoài +1–2h. 2. OK → Alert. 3. Sửa lại; đặt Awards ≤ final+2h → Alert. | CK/Awards invalid | Không gọi `onConfirm`; Alert đỏ từ `validateLocal` | Not Run | | FE |

---

## Ghi chú nộp bài

- Chữ mẫu xanh trong template Excel gốc (login/submit mẫu) **không** dùng ở đây.
- Expected Output ghi validation message hoặc output hệ thống (toast / ErrorCode / UI).
- Không kèm bug report riêng.
- Không đánh Pass/Fail cho đến khi chạy tay thật.
- **Vì sao trước đó 4 features?** Plan gốc chỉ phủ cụm UX vừa làm (đóng ĐK / activate / rubric / timer). Đã bổ sung **E Bốc thăm** và **F Dời lịch** — liền kề GĐ2 trên cùng luồng Coord.
