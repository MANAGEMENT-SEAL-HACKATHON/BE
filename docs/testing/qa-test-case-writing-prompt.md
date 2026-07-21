# Test Case — Coord đóng ĐK sớm / chọn lịch / kích hoạt SL + GK rubric & timer

**Dự án:** SEAL Hackathon Management System  
**Phạm vi:** UI/UX + nghiệp vụ liên quan: đóng đăng ký sớm + preview lịch, modal kết quả, kích hoạt vòng Sơ loại (`START_NOW`/`KEEP` trên FE), rubric GK, timer Live Scoring.  
**Nguồn:** code FE/BE (đã audit đối chiếu)  
**Tài khoản seed:** `coord@fpt.edu.vn` / `Coordinator@dev1`  
**Status:** `Not Run` · **Test date:** trống  

**API đúng path (FE `endpoints.js` / BE controller):**

| Thao tác | Method + path |
|----------|----------------|
| Đóng ĐK sớm | `POST /api/v1/hackathons/{id}/close-registration-early` (`@CoordinatorOnly`) |
| Preview lịch | `POST /api/v1/hackathons/{id}/competition-schedule/preview?assumeCloseRegToday=` |
| Kích hoạt vòng | `PATCH /api/v1/rounds/{id}/activate` |

---

## A. Đóng đăng ký sớm + chọn lịch

FE: `CompetitionScheduleAdjustModal.jsx`, `HackathonGeneralConfig.jsx`  
BE: `HackathonRegistrationCloseServiceImpl`

| ID | Test Case Description | Pre-Condition | Test Case Procedure | Test Data | Expected Output | Status | Test date | Note |
|----|----------------------|---------------|---------------------|-----------|-----------------|--------|-----------|------|
| TC-CR-01 | Happy: đóng ĐK sớm + giờ SL hợp lệ | `seal-e2e-2026` ONGOING, ĐK còn mở, `scheduleAdjustedAt` null; login Coord | 1. Setup → Cấu hình chung. 2. **Kết thúc đăng ký sớm**. 3. Kiểm Alert + Collapse «Quy tắc lịch tự động». 4. Giữ/chọn giờ SL ≥ minDay và after now. 5. Đợi preview. 6. **Xác nhận đóng & lưu lịch**. | Slug `seal-e2e-2026` | Toast thành công; modal **Kết quả đóng đăng ký sớm**; có `lockedActiveTeams`, `withdrawnOrphans`, `rejectedIncompleteTeams`, `timelineCompressed` (nếu nén lịch) | Not Run | | `closeRegistrationEarly` |
| TC-CR-02 | Preview: label thân thiện + format ngày | Modal đóng ĐK mở | Chọn giờ SL; xem bảng preview | — | Cột Hạng mục map key `WORKSHOP`/`KICKOFF`/`PRELIM`/`FINAL`/`AWARDS` → label VN trong `FRIENDLY_LABELS`; giá trị patch/local format `DD/MM/YYYY HH:mm` | Not Run | | FE `displayChanges` |
| TC-CR-03 | Hủy modal — không đóng ĐK | Modal mở | **Hủy (không lưu)** | — | Modal đóng; ĐK vẫn mở; không set `registrationClosedEarlyAt` | Not Run | | |
| TC-CR-04 | Chỉnh chi tiết rồi xác nhận | Modal mở | 1. Mở chỉnh chi tiết. 2. Đổi giờ WS/KO trong đúng ngày (DatePicker `disabledDate` khóa ngày). 3. Đổi CK/Awards trong ràng buộc `validateLocal`. 4. Xác nhận. | WS ngày regEnd+1; KO regEnd+2; CK trong prelimEnd+1h…+2h; Awards > final+2h | `onConfirm` gửi `newPrelimExamAt` + `overrides` (ISO `YYYY-MM-DDTHH:mm:ss`); đóng thành công | Not Run | | Payload shape FE |
| TC-CR-05 | DatePicker chặn ngày SL không hợp lệ | Modal mở | Thử chọn ngày &lt; today hoặc &lt; minDay | — | `disabledDate` chặn; OK disabled nếu `!newExamAt.isAfter(now)` | Not Run | | FE |
| TC-CR-06 | validateLocal: CK ngoài cửa sổ +1–2h | Modal; mở chỉnh chi tiết | Đặt Chung kết ngoài `[prelimEnd+1h, prelimEnd+2h]` rồi OK | CK invalid | Alert lỗi đỏ từ `validateLocal`; không gọi `onConfirm` | Not Run | | FE only |
| TC-CR-07 | validateLocal: Awards ≤ hạn nộp CK | Modal; chỉnh Awards | Đặt Awards ≤ `finalExamAt+2h` rồi OK | Awards invalid | Alert «Lễ trao giải phải sau hạn nộp Chung kết…»; không submit | Not Run | | FE only |
| TC-CR-08 | BE: đóng khi đã đóng | Sau TC-CR-01 hoặc `seal-gd2-registration-closed` (`isRegistrationClosed`) | UI: không còn nút đóng (đã đóng). API: `POST .../close-registration-early` | Body có `newPrelimExamAt` | UI: tag/đã đóng. API: `REGISTRATION_ALREADY_CLOSED` | Not Run | | `HackathonRegistrationCloseServiceImpl` |
| TC-CR-09 | BE: không ONGOING | Hackathon DRAFT | Card đóng ĐK chỉ render khi `status===ONGOING`. API close trên DRAFT | — | FE không CTA. API: `HACKATHON_NOT_ONGOING` | Not Run | | |
| TC-CR-10 | BE: thiếu `newPrelimExamAt` | Gọi API | Body null/`newPrelimExamAt` null | — | `VALIDATION_FAILED` | Not Run | | |
| TC-CR-11 | BE: đã `scheduleAdjustedAt` | Hackathon đã adjust lịch 1 lần | `POST` close-registration-early | — | `SCHEDULE_ALREADY_ADJUSTED` | Not Run | | |
| TC-CR-12 | Modal kết quả: CTA theo response | Sau đóng ĐK thành công | Xem 3 thẻ số. Nếu `teamsAwaitingCoordinatorApproval` + `teamsInFormationGracePeriod` length &gt; 0 → **Xử lý N đội**. Nếu = 0 và có `onGoToLottery` → **Bốc thăm & Khai mạc** | Response thật sau close | Thẻ = `lockedActiveTeams` / `withdrawnOrphans` / `rejectedIncompleteTeams`. Navigate teams: `ROUTES.GLOBAL_TEAMS?hackathonId=&status=PENDING`. N = awaiting.length + grace.length | Not Run | | Không giả định slug nào chắc có awaiting — đọc response |
| TC-CR-13 | SV tạo đội sau đóng ĐK | Sau TC-CR-01 | Login SV; thử tạo đội trên cùng hackathon | — | BE `REGISTRATION_CLOSED` | Not Run | | `TeamServiceImpl` |

---

## B. Kích hoạt vòng Sơ loại

FE: `ActivateScheduleModal.jsx` — chỉ tự set `START_NOW` (examAt future) hoặc `KEEP` (không future).  
BE: `RoundActivationServiceImpl` · `PATCH /api/v1/rounds/{id}/activate`

| ID | Test Case Description | Pre-Condition | Test Case Procedure | Test Data | Expected Output | Status | Test date | Note |
|----|----------------------|---------------|---------------------|-----------|-----------------|--------|-----------|------|
| TC-ACT-01 | Happy START_NOW + lead | ĐK đóng, lottery xong, prelim inactive, đủ criteria/judge/track có đội; examAt future | Tab Vòng thi → `round-activate-btn` → set lead (mặc định 5) → **Kích hoạt & bắt đầu sớm** | `setupLeadMinutes` 1–30 | Body: `scheduleMode=START_NOW`, `setupLeadMinutes`; vòng active; giờ thi theo `calculateStartTime`; Alert/helper không viết tắt WS/KO/CK | Not Run | | FE payload |
| TC-ACT-02 | Preview giờ theo lead | Modal; examInFuture | Đổi InputNumber 1 / 5 / 30 | — | Text helper cập nhật `formatExamPreview(calculateStartTime(...))`; clamp 1–30 | Not Run | | FE |
| TC-ACT-03 | KEEP khi examAt không future | `examAt` ≤ now | Mở modal → không có lead → **Kích hoạt** | — | Body `scheduleMode=KEEP`; không gửi `setupLeadMinutes` | Not Run | | FE `handleOk` |
| TC-ACT-04 | Hủy modal | Modal mở | **Hủy** | — | Không đổi round | Not Run | | |
| TC-ACT-05 | BE prelim: track không có đội | Prelim có track nhưng `countByTrack` = 0 | Activate | — | `TRACK_EMPTY_TEAMS` | Not Run | | `validateTeamsInRound` |
| TC-ACT-06 | BE prelim: track không có criteria | Track 0 criteria thường | Activate | — | `ROUND_NO_CRITERIA` | Not Run | | `validatePreliminaryRoundTracks` |
| TC-ACT-07 | BE prelim: weight track ≠ 1 | Track weight invalid | Activate | — | `ROUND_WEIGHT_NOT_ONE` | Not Run | | |
| TC-ACT-08 | BE prelim: track chưa gán judge | Track không assignment | Activate | — | `JUDGE_NOT_ASSIGNED` | Not Run | | |
| TC-ACT-09 | Bắt đầu thi sớm (round đã active, examAt future) | Round `isActive`, examAt future | Modal title «Bắt đầu thi sớm…» → OK | Lead minutes | FE vẫn `START_NOW` + `setupLeadMinutes`; BE nhánh early-start nén exam/submission | Not Run | | `RoundActivationServiceImpl` |
| TC-ACT-10 | Copy Alert không viết tắt | Modal START_NOW | Đọc Alert + helper | — | Có «Workshop», «Khai mạc», «đóng đăng ký sớm», «Chung kết» | Not Run | | UX |

---

## C. Rubric / description seed

FE: `JudgeScoringWorkspace.jsx` · Seed: `HackathonDevSeedHelper.ensureDefaultTrackCriteria` (4) + `ensureFinalCriteria` (1); `Gd1DataSeeder` (5+4+5)

| ID | Test Case Description | Pre-Condition | Test Case Procedure | Test Data | Expected Output | Status | Test date | Note |
|----|----------------------|---------------|---------------------|-----------|-----------------|--------|-----------|------|
| TC-RB-01 | Collapse hiện description + Tag sau create-drop | BE create-drop; slug dùng helper criteria (vd. `seal-gd3-scoring-live`) | Live Scoring → Collapse hướng dẫn → expand tiêu chí | — | Có `description` từ API; Tag trọng số `(weight*100)%`; Tag `Thang điểm 0–{maxScore}` | Not Run | | Criteria đã tồn tại trước khi thêm description **không** tự backfill (idempotent return sớm) |
| TC-RB-02 | Weight/order/maxScore không đổi | Track helper 4 criteria | Coord Quản lý tiêu chí | 0.30/0.30/0.20/0.20, maxScore 10 | Tổng weight 1.0; số criteria không đổi | Not Run | | |
| TC-RB-03 | Coord sửa description → GK thấy sau reload criteria | Cùng track | Coord PUT description → Judge mở lại Collapse | — | Text mới từ API | Not Run | | |
| TC-RB-04 | `rubricUrl` hiện link | Criteria có `rubricUrl` | Expand tiêu chí | URL | Link «Xem rubric chi tiết» | Not Run | | |
| TC-RB-05 | description null | Criteria không mô tả | Expand | null | Fallback: «Chưa có mô tả tiêu chí. Coord có thể bổ sung tại màn Quản lý tiêu chí.» | Not Run | | Đúng copy FE hiện tại |
| TC-RB-06 | Chốt điểm trong Q&A | Đang Q&A; chưa chốt | Nhập điểm → **HOÀN TẤT & CHỐT SỔ ĐIỂM** | 0–10 step 0.1 | Chốt thành công (hành vi submit hiện có) | Not Run | | |
| TC-RB-07 | FE chặn điểm ngoài 0–10 | Form chấm | Thử nhập &gt; 10 hoặc &lt; 0 | — | `InputNumber`/`Slider` `min={0}` `max={10}` (hardcode FE, không đọc dynamic maxScore) | Not Run | | Code thật |
| TC-RB-08 | Gd1 seed đủ description | create-drop; slug Gd1 có track/CK criteria | Coord xem tiêu chí track1/2 (5), track3 (4), final (5) | — | Mỗi dòng có description; name/weight/order giữ như seed | Not Run | | |

---

## D. Timer Live Scoring

FE: `JudgeTimerAndControls.jsx`, `timerControlGates.js`

| ID | Test Case Description | Pre-Condition | Test Case Procedure | Test Data | Expected Output | Status | Test date | Note |
|----|----------------------|---------------|---------------------|-----------|-----------------|--------|-----------|------|
| TC-TM-01 | Controller Start → QA → ENDED → Next | Controller; có queue; đủ điều kiện chấm | 1. **Bắt đầu tính giờ** (`START_OR_RESUME`). 2. Tới QA (`QA`). 3. GK chốt. 4. Hết giờ hoặc early-end (`END`) → phase `ENDED`. 5. **Kết thúc & gọi đội kế tiếp** (`NEXT`) khi `canCallNextTeam` true | — | Action đúng tên trên; Next **chỉ khi** `localTimerPhase === 'ENDED'` (`canCallNextTeam`) | Not Run | | Không Next lúc còn QA |
| TC-TM-02 | Progress chốt X/Y | Controller; QA hoặc ENDED; `judgesAssigned ≥ 1` | Quan sát `data-testid=judge-confirm-progress` | `judgesConfirmed` / `judgesAssigned` | Hiện «Giám khảo đã chốt: X/Y» + Progress % | Not Run | | |
| TC-TM-03 | Cảnh báo ≤ 1/3 Q&A | QA; chưa chốt; remaining ≤ `ceil(qaTotal/3)` | Đợi/quan sát tới ngưỡng | `shouldWarnQaScoringDeadline` | Alert `qa-controller-deadline-warn` | Not Run | | `timerControlGates.js` |
| TC-TM-04 | Early-end ẩn khi chưa đủ chốt | QA; remaining &gt; 0; `allJudgesSubmitted !== true` | Kiểm `early-end-qa-btn` | — | Không render early-end (`canEarlyEndQa` false) | Not Run | | |
| TC-TM-05 | Early-end khi đã đủ chốt | QA; remaining &gt; 0; `allJudgesSubmitted === true` | **Kết thúc sớm hỏi đáp** → confirm | — | `handleTimerAction('END')` | Not Run | | |
| TC-TM-06 | Next khi ENDED — đủ chốt hoặc đã có điểm (hết giờ tự nhiên) | Phase `ENDED`; có `presentationScoringStatus` | Quan sát nút Next | `allJudgesSubmitted` hoặc `canAdvanceQueue` hoặc (`qaEndedEarly===false` và `judgesScored>0`) | Nút Next hiện khi `canCallNextTeam` true; nếu false và `judgesAssigned≥2` → text chờ chốt X/Y | Not Run | | Đúng gate code; không nói «ẩn mọi trường hợp thiếu chốt» |
| TC-TM-07 | Pause / Resume / Reset | PRESENTING | Pause → Resume; Reset (Popconfirm) khi phase không thuộc IDLE/SETUP/QA/ENDED | — | `PAUSE` / `START_OR_RESUME` / `RESET` | Not Run | | `shouldHideResetTimer` |
| TC-TM-08 | Skip no-show | Phase WAITING/PRESENTING/SETUP/IDLE/PAUSED; có `submissionId` | **Bỏ qua đội này** → confirm | — | `SKIP_NOSHOW`; `presentation-skip-noshow-btn` | Not Run | | |
| TC-TM-09 | Copy nút tiếng Việt | Controller panel | Đọc nhãn | — | «Đặt lại đồng hồ»; «Điều khiển thời gian»; «hỏi đáp» | Not Run | | UX |

---

## E. Smoke / auth

| ID | Test Case Description | Pre-Condition | Test Case Procedure | Test Data | Expected Output | Status | Test date | Note |
|----|----------------------|---------------|---------------------|-----------|-----------------|--------|-----------|------|
| TC-SM-01 | Chuỗi đóng ĐK → lottery → START_NOW (hành vi không đổi) | `seal-e2e-2026` | Theo playbook GĐ2 B→D | Coord | API/CTA hoạt động; khác chủ yếu copy/layout | Not Run | | Regression |
| TC-SM-02 | Seed criteria idempotent | Track đã có criteria | Gọi lại `ensureDefaultTrackCriteria` (re-seed/restart không create-drop) | — | Return sớm; không insert thêm; **không** cập nhật description cũ | Not Run | | Code `if (!…isEmpty()) return` |
| TC-SM-03 | Non-Coord gọi close-reg | Login STUDENT hoặc JUDGE | `POST .../close-registration-early` | Token không COORDINATOR APPROVED | Bị chặn `@CoordinatorOnly` (`hasRole('COORDINATOR')` + status APPROVED) — Spring Security deny (thường 403) | Not Run | | Annotation thật |

---

## ErrorCode / field (đã có trong `ErrorCode.java` + service)

| Mã / Field | Nơi trong code |
|------------|----------------|
| `VALIDATION_FAILED` | Close khi thiếu `newPrelimExamAt` |
| `HACKATHON_NOT_ONGOING` | Close khi status ≠ ONGOING |
| `REGISTRATION_ALREADY_CLOSED` | `isRegistrationClosed` |
| `SCHEDULE_ALREADY_ADJUSTED` | `scheduleAdjustedAt != null` |
| `REGISTRATION_CLOSED` | SV tạo đội sau đóng ĐK |
| `TRACK_EMPTY_TEAMS` | Track count đội = 0 |
| `ROUND_NO_CRITERIA` / `ROUND_WEIGHT_NOT_ONE` / `JUDGE_NOT_ASSIGNED` | `validatePreliminaryRoundTracks` |
| Close response | `lockedActiveTeams`, `withdrawnOrphans`, `rejectedIncompleteTeams`, `teamsAwaitingCoordinatorApproval`, `teamsInFormationGracePeriod`, `timelineCompressed`, `hoursUntilPrelimExam`, `prelimExamAt` |

**Không đưa vào TC (tránh lẫn):** `NO_TEAMS_IN_ROUND` = **không có track** (không phải «chưa lottery»). `RESULT_NOT_PUBLISHED` = gate **Chung kết**, không phải activate Sơ loại trên FE modal này.

---

## Đã loại / sửa khi audit (không bịa / không overthink)

| Vấn đề cũ | Xử lý |
|-----------|--------|
| Sai path preview `preview-competition-schedule` | Đổi đúng `competition-schedule/preview` |
| Ghi activate như POST | Đúng `PATCH /api/v1/rounds/{id}/activate` |
| TC WS «sai ngày» dù DatePicker đã khóa ngày | Bỏ — khó/không reproduce qua UI |
| Giả định slug chắc có đội awaiting | TC-CR-12 đọc response thật |
| Next lúc QA / «ẩn khi thiếu chốt» tuyệt đối | Sửa theo `canCallNextTeam` (chỉ `ENDED` + điều kiện scored) |
| Action tên `START` | Đúng `START_OR_RESUME` |
| Slider theo `maxScore` động | FE hardcode `max={10}` |
| TC «mất sync tạm» tạo badge | Bỏ — không có bước reproduce ổn định trong code |
| «Form điểm trắng» | Bỏ khỏi expected (không verify trong diff UX này) |
