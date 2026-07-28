# Session Changelog: 15/07 – 19/07/2026

**Bối cảnh:** Báo cáo tổng hợp các bản vá lỗi P0 về logic vận hành, workflow và UX/FE từ sáng 15/07 đến **19/07**. Các thay đổi tập trung vào đồng bộ state Coord↔Student, vá lỗ hổng nghiệp vụ (Timer, Chấm điểm), chuẩn hóa UI phi kỹ thuật, **luồng onboard giám khảo khách (PENDING → APPROVED)**, **3 màn giám sát Coordinator**, phiên **Enterprise Audit + Regression** (17/07), phiên **Audit Remediation (18/07)** — **bỏ hẳn Vé vớt/Wildcard (Top-N)**, **bỏ role Trưởng ban (HEAD)**, **Personnel Guard**, tối ưu N+1 bốc thăm, robust E2E — và phiên **Full System Deep Test 3 làn + Zero-skip E2E (19/07)**.

---

## 0. Phiên 19/07 — Full System Deep Test (3 làn) + Zero-skip E2E (MỚI NHẤT)

> Báo cáo chi tiết: [`ui-audit-2026-07-19/deep/REPORT.md`](ui-audit-2026-07-19/deep/REPORT.md) (+ `REPORT-gd1..gd6.md`, `REPORT-negative.md`, `L0..L6-SUMMARY.md`, log evidence cùng thư mục).

### 0.1 Deep test 3 làn (Happy / Bad / Sabotage)

* **[DONE]** Chạy full pyramid theo layer L0–L6 (unit → integration/API → UI/UX audit → mutating → sabotage → tổng thể 1 lần) trên **9 happy seed**; mỗi ID ghi PASS/FAIL/SKIP + lý do + evidence theo **template 8 cột**, tag làn `[HAPPY]/[BAD]/[SAB]`.
* **[DONE]** Kỷ luật Gap 5 (chụp **TRƯỚC → fix → chụp SAU**) cho bug UI phát hiện giữa chừng; **restart BE (create-drop)** giữa các phase mutating để không nhiễu dữ liệu.

### 0.2 🔴 Các FAIL/SKIP "động" đã đóng (chỉ nhận PASS)

* **[DONE]** **FE lint 9–12 error → 0 error** (còn 193 warning): bỏ `no-useless-assignment`; không gọi `Date.now()` / update ref trong render (snapshot qua `useState`/`useEffect` ở `CoordinatorActionCenter.jsx`, `RoundManagementPage.jsx`, `useLiveScoringV2.js`, `usePresentationQueueSocket.js`).
* **[DONE]** **THESIS-RBL-02:** variance RBL **ẩn danh** — DTO đổi `judgeId` → `anonymizedJudgeId` (`RblVarianceItemResponse`, `RblDashboardServiceImpl`, `ExportCsvBuilder`, FE `AnalyticsPage.jsx`); không còn lộ `judgeId` thô.
* **[DONE]** **LOTTERY-DATA-01:** deep-audit gọi đúng `GET /teams?hackathonId=` (không phải `/hackathons/{id}/teams`); pass khi queue đã shuffle (đọc panel «Tổng đội»).
* **[DONE]** **L3.5 catalog probe 17/17 PASS** (trước 14/17 + SKIP): TC-TB-01 kiểm **API thật** (no ghost tiebreak), `RESULT_NOT_PUBLISHED` dựng hackathon DRAFT ephemeral đúng ngày, `TC-WC-03` = student **FORBIDDEN**, `SCORING_NOT_OPEN` dùng submission `SUBMITTED`, `HARD_LOCK_LATE` dùng multipart.
* **[DONE]** `neg:duplicate-email` → **ACCOUNT_DUPLICATE_EMAIL** (thêm `userType/studentCode/institution` vào request probe).
* **[DONE]** `RoundSummaryResponse.isFinal` bổ sung cho list rounds (`RoundMapper`).
* **[DONE]** Revive **abuse-guards** (4/4) + **concurrent-race** (4/4) trên happy seed (bỏ hard-skip deprecated).
* **[VERIFY]** BE unit **420/420**, `probe:seeds` **29/29**.

### 0.3 🟢 Zero-skip toàn repo E2E — revive 10 suite seed deprecated

Xóa toàn bộ `test.skip(true, 'deprecated seed slug removed…')`; map từng suite sang 9 live seed. **28/28 test PASS, 0 hard-skip** (chỉ còn guard runtime BE/seed hợp lệ).

| Suite | Seed live | Kết quả |
|-------|-----------|---------|
| `final-round-smoke` | `seal-gd5-final-active` | 2/2 |
| `preliminary-results-progression` | `seal-gd4-tiebreak-manual` | 2/2 (nút **Chốt chuyển vòng** disabled khi còn đồng điểm, giữ sau reload) |
| `fall-track-select` (+`-mutating`) | `seal-e2e-2026` (Spring) | 3/3 — kiểm **gate mùa** FR-U-15-F: không render card Fall + API `422 NOT_APPLICABLE`, track không đổi |
| `mentor-track-bootstrap` | `mentor2@` track-only | 1/1 (precondition tracks>0, rounds==0) |
| `team-mentor-history` | `seal-gd3-prelim-open` | 2/2 (FR-13C `/teams/{id}/mentors` ≥1 row + panel UI) |
| `mentor-portal-mutating` | `seal-gd3-prelim-open` | 6/6 (happy, IDOR 403, student 403, bootstrap, 2× conflict `CONFLICT_SAME_TRACK`/`CONFLICT_MENTOR_JUDGE_SAME_TRACK`) |
| `event-notification-mutating` | `seal-gd1-incomplete` | 1/1 (fan-out `EVENT_REMINDER`) |
| `websocket-queue-timer` | `seal-gd3-prelim-open` | 3/3 (STOMP connect, shuffle/next/start/pause broadcast) |
| `5-secondary-portals-mutating` | `seal-e2e-2026` + `gd3` + `fall-finished` | 8/8 |

* **[FIX]** Radar orphan: `/teams?hackathonId={id}` để pin đúng sự kiện (header context mặc định có thể mở seed khác → radar rỗng).
* **[NOTE kỹ thuật]** Text tiếng Việt trong regex spec dùng **escape `\uXXXX`** (công cụ ghi file làm hỏng ký tự non-ASCII → mojibake). Suite Fall chuyển sang kiểm **gate mùa** (seed Fall ONGOING đã purge). Conflict mentor↔judge kiểm ở **tầng gán** (`POST /judge-assignments`) thay vì tầng chấm điểm (guard chặn sớm hơn).

---

## 0-bis. Phiên 18/07 — Audit Remediation

> Chi tiết test tay: [manual-ui-playbook-gd1-gd6.md](manual-ui-playbook-gd1-gd6.md) §4 (GĐ4) + §1F (Personnel Guard). Kế hoạch gốc: `audit_remediation_seal_1bc0cf36.plan.md`.

### 0.1 🔴 Bỏ hẳn Vé vớt (Wildcard) — advance chỉ Top-N

* **[DONE]** **Xóa toàn bộ luồng Vé vớt.** Advance vào Chung kết **chỉ theo Top-N mỗi bảng** (`round.topNAdvance`, cap tùy chọn), validate theo số đội active.
* **[DONE]** FE: trang Kết quả Sơ loại **bỏ tab «Vé vớt»**; URL cũ `?tab=wildcard` → tự nhảy về tab **Kết quả** (`PreliminaryResultsPage.jsx`). Stepper còn **6 bước**: Khóa chấm → Xem trước xếp hạng → Đồng điểm → Công bố → Chốt CK → Cấu hình CK (`PreliminaryResultsCoordinatorStepper.jsx`, comment *Phase 1: Vé vớt step removed*).
* **[DONE]** BE: `RoundProgressionServiceImpl.wildcardCandidates` luôn trả rỗng (`emptyWildcardResponse`, comment *Wildcard removed — Top-N only. WC-MIG*); `advanceTeams` chỉ Top-N, **không** chặn vì WC; `requireWildcardReadyForAdvance` = `@Deprecated` no-op.
* **[DONE]** `FinalistsCard.tsx` cột **Lý do vào CK**: mọi đội advance = tag **Top N**; tag «Vé vớt» chỉ còn fallback cho dữ liệu cũ.
* **[MIGRATION]** Không cần migration DB — đọc read-time. Slug `seal-gd4-wildcard-gap` vẫn tồn tại nhưng hành xử như Top-N (tab Vé vớt không hiện).

### 0.2 🔴 Bỏ role Trưởng ban (HEAD)

* **[DONE]** Bỏ cờ `isHead` khỏi UI vận hành; điều khiển đồng hồ **chỉ** qua cơ chế **controller-grant**: card **Phân quyền điều phối đồng hồ thời gian** → **Chuyển quyền (TRANSFER)** / **Gỡ quyền** (`PresentationControllerCard.tsx`). Không còn cờ «Trưởng ban» khi gán GK Chung kết.
* **[DONE]** Logic controller-grant robust: chuyển tiếp = Gỡ quyền → chọn người khác → Chuyển quyền; race `CONTROLLER_CONFLICT` (PESSIMISTIC_WRITE).

### 0.3 🔴 Personnel Guard (Phương án D)

* **[DONE]** **Chống phân thân (Anti-Ubiquity):** 1 giám khảo chỉ được gán **1 bảng trong 1 vòng** — gán bảng thứ 2 cùng vòng → `JUDGE_ASSIGN_DUPLICATE` (`existsByJudgeIdAndRoundScope`).
* **[DONE]** **Cô lập Mentor (Mentor-Isolation):** người đang mentor một **đội** thuộc bảng X → cấm làm Giám khảo bảng X → `CONFLICT_MENTOR_JUDGE_SAME_TRACK` (`assertNotMentorOfTeamInTrack`); mentor≡judge cùng bảng (đăng ký) → `CONFLICT_SAME_TRACK`.
* **[DONE]** **Staffing theo loại vòng:** Sơ loại chỉ `NORMAL`; Chung kết chỉ `FINAL_EXTERNAL` yêu cầu Judge `UserType.EXTERNAL`; cảnh báo mềm `MIN_FINAL_JUDGES_NOT_MET` nếu < 3 GK. Nguồn: `JudgeAssignmentServiceImpl`, `PersonnelAssignmentRules`.
* **[DONE]** UI: option vi phạm hiện **xám / disabled** trước khi bấm; lách API → trả đúng ErrorCode.

### 0.4 Tối ưu & độ ổn định

* **[DONE]** **N+1 bốc thăm (R12):** `HackathonLotteryServiceImpl` batch-load `TeamRoundTrack` theo round 1 lần (`findByTrack_Round_Id`) thay vì query từng đội; cập nhật `Set` trong request để tránh gán lại.
* **[DONE]** **E2E mode-b robust:** phân biệt skip hợp lệ vs lỗi thật (`isBenignFullyScoredUiError`); assert `finalSubmittedCount > 0` cho nộp bài Chung kết.
* **[DONE]** Các fix R3–R9/D2: UX «round đã kết thúc», dropdown Dashboard, điểm SV theo từng vòng, bỏ nút thừa. Button matrix GĐ1–6.
* **[REPORT]** Biên bản nghiệm thu: [`bien-ban-nghiem-thu-audit-remediation.md`](bien-ban-nghiem-thu-audit-remediation.md).

**Trạng thái nhánh (tham chiếu lúc cập nhật doc tối 17/07):**

| Repo | Branch | Head SHA (baseline) | Ghi chú |
|------|--------|---------------------|---------|
| BE | `dev` | `3e82bc8` | Working tree còn uncommitted (enterprise fix batch) |
| FE | `main` | `8e8b780` | Working tree còn uncommitted (enterprise fix batch) |

Staging neo: root [`compat.lock`](../../../compat.lock) — cặp BE↔FE.  
Baseline report: [`reports/regression-baseline-2026-07-17.md`](../../../reports/regression-baseline-2026-07-17.md).  
Enterprise matrix: [`docs/testing/enterprise-regression-matrix-gd1-gd6.md`](../../../docs/testing/enterprise-regression-matrix-gd1-gd6.md).  
Regression summary: [`reports/enterprise-regression-summary.md`](../../../reports/enterprise-regression-summary.md).

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
* **[DONE] (17/07 tối)** Round form: không Semifinal / không switch «Là vòng chung kết»; nhãn **Thời lượng thi (Giờ)**; ô mở/hạn nộp auto + disabled (`G1-FORM-02` / `G1-ROUND-03`).
* **[DONE] (17/07 tối)** Chống phân thân GK: gán 1 judge vào ≥2 bảng cùng vòng sơ loại → `JUDGE_ASSIGN_DUPLICATE` (`G1-JUDGE-04`, `JudgeAssignmentServiceImplMultiTrackTest`).
* **[DONE] (17/07 tối)** Copy status: `PENDING_CONFIRM` → «**Chờ chốt sổ**» trên Dashboard / list / confirm closure (`G1-SETUP-01`).
* **[DONE] (17/07 tối)** Early-wait Phát đề: trước `examAt` nút **Phát đề** vẫn hiện nhưng **disabled** + tooltip «Chưa tới giờ thi» (round + track) — không ẩn gây hiểu nhầm (`EARLY-WAIT-01`).

### GĐ1b: Giám khảo khách — onboard (17/07)

* **[DONE]** Tạo temp judge: `status=PENDING` + `mustChangePassword=true` (không còn APPROVED ngay sau gửi mail).
* **[DONE]** Auth deadlock fix: `assertApproved` cho phép login khi `isTempAccount && EXTERNAL && JUDGE && PENDING && mustChangePassword`.
* **[DONE]** `POST change-password` (cùng transaction): clear `mustChangePassword` + set `APPROVED`; đánh dấu invitation `acceptedAt`.
* **[DONE]** List temp-judges expose `mustChangePassword` + `invitation.expiresAt` / `tokenSent`.
* **[DONE]** Resend invitation: **luôn** tạo MK tạm mới; cho phép resend ngay khi `lastTokenSent=false` (email fail) dù token còn hạn 72h.
* **[DONE]** FE badge thứ tự: Email chưa gửi → Lời mời hết hạn → Chờ đổi mật khẩu (tooltip) → Đã duyệt.
* **[DONE]** Pool gán GK Chung kết: loại guest chưa `APPROVED` / còn `mustChangePassword`.
* **[DONE] (17/07 tối)** Wording temp judge: **Thu hồi lời mời** (không «xóa»); lifecycle khóa sau event, giữ audit.
* **[DONE] (17/07 tối)** `avatarUrl` trên `UserSummaryResponse` + mappers (`PEOPLE-AVATAR-01`).
* **SQL manual:** [`V20260717_invitation_last_token_sent.sql`](../../src/main/resources/db/manual/V20260717_invitation_last_token_sent.sql) — cột `invitations.last_token_sent` (ddl-auto=update cũng áp dụng).

### GĐ2: Đăng ký & Ghép đội

* **[DONE]** Auto-navigate: Tạo đội hoặc Chấp nhận lời mời tự động chuyển vào trang đội (Không cần F5).
* **[DONE]** Sửa logic Giải tán đội: Thành viên cũ có thể tạo đội mới (Fix lỗi kẹt state Accepted cũ).
* **[DONE]** Fix Bug UI Duyệt đội: Nút duyệt hàng loạt (Batch Approve) giờ chỉ chọn các đội "Đủ điều kiện", bỏ qua đội thiếu member.
* **[DONE]** Tối ưu thuật toán Bốc thăm (Lottery): Giảm thời gian chờ từ 5 phút xuống còn vài giây.
* **[DONE]** UI: Bổ sung hiển thị Avatar của Judge/Mentor khi thao tác chọn nhân sự.
* **[DONE] (17/07)** **Gate PENDING trước Bốc thăm / kích hoạt sơ loại:** `PendingTeamGateService` phân `awaitingApproval` / `inGrace` / `blockedOther`; error `TEAMS_PENDING_APPROVAL` kèm counts + `earliestGraceDeadlineAt`. Lottery + activate prelim (non-final) chặn cứng; pool lottery vẫn chỉ ACTIVE+locked khi gate sạch.
* **[DONE] (17/07)** **Race guard:** `findByIdForUpdate` trên hackathon trong lottery / close-early / createTeam / adminCreateTeam / adminMergeTeams; sau prelim active không tạo/gộp đội; adminCreate sau close → ACTIVE đã khóa.
* **[DONE] (17/07)** FE: banner lottery tách bucket + CTA «Xử lý danh sách đội thi»; modal close-early không CTA Lottery khi còn PENDING; Coord **từ chối sớm** đội grace (bỏ chặn `isInFormationGrace`).
* **[DONE] (17/07)** Student: `FormationGraceBanner` đầu trang đội (leader+member), countdown HH:MM:SS, màu leo thang (<6h đỏ), CTA xác nhận / nhắc trưởng nhóm.
* **[DONE] (17/07 tối)** Radar: copy **không** còn «đủ người → tự ACTIVE»; đúng nghiệp vụ «đủ điều kiện để Coordinator duyệt». Min/max **động** từ BE. `getIncompleteTeams` gồm đội **thiếu và thừa** (`G2-RADAR-02`).
* **[DONE] (17/07 tối)** Disband wording: giải tán / REJECTED / giải phóng — không «xóa đội».
* **[DONE] (17/07 tối)** fillPercent = số người đăng ký / `maxParticipants` (không nhầm số đội).

### GĐ3: Sơ loại & Chấm điểm (Workflow)

* **[DONE]** Nút "Kết thúc đăng ký sớm" tự động ẩn/disable khi sự kiện đang diễn ra (GĐ3).
* **[DONE]** Đồng bộ Đề thi: Bấm "Phát đề" vòng thi tự động sync đề vào Bảng đấu (Track).
* **[DONE]** Màn hình Coordinator: Bổ sung hiển thị cột "Trạng thái nộp bài" của các đội.
* **[DONE]** Đồng bộ Trạng thái sự kiện: Khi GĐ3/5 đóng cửa, Student nộp bài sẽ nhận thông báo "Sự kiện đã kết thúc" (Thay vì lỗi vô lý "Chưa diễn ra").
* **[DONE]** Timer Sequence (Trình tự giám khảo): Bắt buộc đi theo luồng `Hết thuyết trình -> Hết Q&A -> Gọi đội kế tiếp`.
* **[DONE]** Ẩn nút "Reset Timer" sau khi chốt điểm/chuyển đội để đảm bảo công bằng.
* **[DONE]** Reset Điểm: Fix lỗi form chấm điểm bị dính cache điểm của đội trước đó khi chuyển đội.
* **[DONE]** Student UI: Thí sinh xem được "STT Thuyết trình" và "Mã số" (ẩn tên đội với Giám khảo) để chuẩn bị.
* **[DONE] (17/07)** Panel **Tình trạng nộp bài** (live) trên tab Vòng thi khi round active: nhãn dùng chung `getSubmissionStatusMeta`, bucket nộp hợp lệ/chưa nộp/trễ, tag **Trực tiếp** (WS) / **Tự làm mới 30s**, nút **Kết thúc thi sớm**. BE `SubmissionRosterPublisher`.
* **[DONE] (17/07)** **Điểm thành phần** xem **lúc đang chấm** (trước Lock): CTA → `results?tab=scoring-check`; highlight ô lệch **> 2.0** so với TB GK khác.
* **[DONE] (17/07)** Timer control: bỏ **Takeover tạm** / `JUDGE_OFFLINE` takeover — chỉ **Chuyển quyền** (TRANSFER). Card label: **Phân quyền điều phối đồng hồ thời gian**.
* **[DONE] (17/07 tối)** Late pending chặn shuffle + tooltip; deep-link late-submissions kèm `trackId` / `roundId`.

### GĐ4: Chốt Sơ loại → Đồng điểm → advance Top-N

* **[DONE] (18/07)** 🔴 **BỎ HẲN Vé vớt (Wildcard)** — advance chỉ Top-N mỗi bảng. Xem §0.1. Trang Kết quả SL còn tab **Kết quả / Danh sách CK & Bị loại / Kiểm tra chấm / Đồng điểm**; **không** còn tab Vé vớt.
* **[DONE]** Tách biệt rạch ròi logic Tiebreak (Đồng điểm) tại biên Top-N — reorder tay, casting-vote (`TiebreakEvaluation`), `TIEBREAK_ALREADY_RESOLVED` chống confirm 2 lần.
* **[DONE] (17/07)** Enterprise CTA hardening: Skip/No-show UI; lock luôn modal; CK activate cùng ActivateScheduleModal (bỏ step Phát đề CK); lottery/publish confirm; force-ack + lý do; thu hồi giải category+note+audit; unlock chỉ SUPERADMIN; student WS announcements GĐ4/GĐ6; timer PESSIMISTIC_WRITE race guard.
* **[DONE]** Sync Student: Bấm Công bố điểm, tab Quản lý đội của SV hiện ngay Điểm cá nhân, Thứ hạng và Trạng thái Đi tiếp.
* **[DONE] (18/07)** Stepper Kết quả SL 6 bước: **Khóa chấm → Xem trước xếp hạng → Đồng điểm → Công bố → Chốt danh sách vào Chung kết → Cấu hình vòng Chung kết** (bước Vé vớt đã gỡ).

### GĐ5: Chung kết (Kế thừa & Bảo mật)

* **[DONE]** Kế thừa Đề thi: Chung kết tái sử dụng đề của Track GĐ3. Coordinator KHÔNG CÓ nút Phát đề/Upload PDF mới. Student tự xem đề cũ.
* **[DONE] (18/07)** Card **Các đội vào Chung kết** trên `tab=final-config` (`FinalistsCard`): cột Lý do vào CK = **Top N** (Vé vớt đã bỏ; tag «Vé vớt» chỉ fallback dữ liệu cũ) + Hạng/điểm.
* **[DONE]** Bảo mật thao tác: Đội bị loại (Eliminated) chuyển sang mode Read-only.
* **[DONE]** Xáo trộn (Shuffle) & HARD_LOCK: Khóa cứng không cho nộp trễ ở Chung kết.
* **[DONE] (17/07)** **Điểm thành phần** CK: CTA trên FinalRoundConfig + round active trước Lock.
* **[DONE] (17/07 tối)** Final config dropdown standalone đổi sự kiện đúng (không pin sai `?hackathonId=`).
* **[DONE] (17/07 tối)** **Gỡ Calibration** khỏi UI/API vận hành: không còn panel «Phiên Calibration», không E2E `calibration-*-mutating`, không `test:unit:calib`. Enum/schema migration cleanup còn trên BE — **không** test manual Calibration nữa (`CALIB-01`).

### GĐ6: Tổng kết & Trao giải

* **[DONE]** Data Seed Fix: Vá lỗi probe báo "Chưa chấm đủ điểm Chung kết" trên seed GĐ6.
* **[DONE]** Bảng xếp hạng Cơ sở (chapter ranking) hoạt động đúng.
* **[DONE]** Nâng cấp UI Trao giải + thu hồi giải (category/note + audit).
* **[DONE]** Xuất CSV: đủ cột xếp hạng qua các vòng (không chỉ top 3) — `ExportCsvBuilderRankingsTest`.
* **[DONE]** DataInitializer Repair: Seed GĐ4,5,6 nạp đủ điểm + STT thuyết trình GĐ3.

### Cross-cutting — Security & Portal (17/07 tối)

* **[DONE]** Cloudinary: secret **không** trên FE; Onboarding upload qua BE sign; `npm run test:sec:cloudinary` / `scripts/scan-cloudinary-secret.mjs` (`SEC-CLOU-02` / CLOUD-01).
* **[DONE]** Leaderboard auth: `StudentAccessGuard.assertParticipatedInHackathon` — không thuộc → **403** (`SEC-AUTH-01` / LBAUTH-01/02); `StudentAccessGuardParticipatedTest`.
* **[DONE]** Student Events GET (bỏ `@CoordinatorOnly` cứng trên list/get); Matchmaking whitelist sidebar; submission `teamId` bắt buộc (không `teams[0]`).
* **[DONE]** History/results gồm ELIMINATED + ONGOING đã tham gia.

### Enterprise regression harness (17/07 tối)

* **[DONE]** Traceability matrix GĐ1–GĐ6 + SEC IDs → `docs/testing/enterprise-regression-matrix-gd1-gd6.md`.
* **[DONE]** FE `npm run test:unit:all` (29 pass) + build PASS; BE high-risk suites PASS.
* **[DONE]** CI: `BE/.github/workflows/ci-pr.yml`; FE `ci-pr.yml` + `nightly-e2e.yml`; deploy BE dùng `mvn clean verify` (không `-DskipTests`).
* **[NOTE]** Root `.github/ci-test-matrix.yml` vẫn orphan (root không phải git repo) — dùng CI trong từng repo con. Playwright mutating/full pyramid = **nightly** (cần BE seed).

### 1b. Neo kỹ thuật (không dump diff)

| Mảng | Neo |
|------|-----|
| Harness full-chain GĐ3→GĐ6 | `scripts/gd3-gd4-gd5-full-chain-api.mjs` |
| Probe seed | `seal-hackathon-fe/e2e/helpers/seedApiProbe.js` |
| Seed repair thứ tự | `DataInitializer`: `repairForGd5FullChainRetest` **trước** `repairForGd2Testing` |
| GĐ2 membership / giải tán | `TeamMembershipReleaseService` |
| Guest judge create / list | `TempJudgeServiceImpl`, `UserMapper.toSummary` |
| Guest login / đổi MK | `AuthService.assertApproved` + `changePassword` |
| Resend MK tạm mới | `InvitationServiceImpl.resend` + `Invitation.lastTokenSent` |
| Activate header tooltip | `HackathonSetupPage.jsx` |
| Panel nộp bài live | `SubmissionStatusPanel.jsx` + `submissionRoster.js` |
| WS invalidate nộp bài | `SubmissionRosterPublisher` |
| Điểm thành phần trước Lock | `useRoundResults` + `ScoreBreakdownDrawer` |
| Card đội vào CK | `FinalistsCard.tsx` |
| Leaderboard guard | `StudentAccessGuard.assertParticipatedInHackathon` |
| Early-wait Phát đề | `roundLifecycleGates.js` (`canReleaseProblem` / tooltip) |
| Radar over-max | `TeamServiceImpl.getIncompleteTeams` |
| Secret scan | `scripts/scan-cloudinary-secret.mjs` |
| Compat lock | root `compat.lock` |

---

## 2. Các Mảng Ngoài Scope (KNOWN_GAP — xử lý sau)

* ~~Chưa xóa API/Logic Wildcard.~~ → **Đóng (18/07)** — đã bỏ hẳn Vé vớt, advance chỉ Top-N (§0.1). Endpoint `wildcard-*` còn tồn tại nhưng no-op (trả rỗng) để không vỡ client cũ.
* Guest xóa email **trong** cửa sổ 72h còn hạn + `lastTokenSent=true`: Resend API vẫn chặn (`INVITATION_STILL_VALID`) — Coord chờ hết hạn rồi Resend (MK mới), hoặc đợi edge case «force resend» nếu PO yêu cầu.
* Record guest **cũ** (đã `APPROVED` trước fix 17/07): **không backfill**.
* Playwright mutating / Mode B full trên CI PR: **nightly only** — PR gate = unit + build + secret scan (+ BE verify).
* Một số TC matrix còn `Status: Planned` (E2E sync G3-FLOW / G4-SYNC) — tự động hóa dần theo nightly.

~~* Invite Giám khảo tự động duyệt.~~ → **Đóng (17/07)**  
~~* Timer TT/Q&A chỉ CK.~~ → **Đóng (17/07)**  
~~* Calibration UI/API vận hành.~~ → **Đóng (17/07 tối)** — đã gỡ; không reopen manual Chương C cũ.  
~~* Takeover tạm controller.~~ → **Đóng (17/07)** — chỉ TRANSFER.  
~~* Wildcard (Vé vớt).~~ → **Đóng (18/07)** — bỏ hẳn, advance chỉ Top-N (§0.1).  
~~* Role Trưởng ban (HEAD).~~ → **Đóng (18/07)** — chỉ controller-grant / Chuyển quyền (§0.2).

---

## 3. Verify nhanh

| Kiểm | Kỳ vọng |
|-------|---------|
| `npm run probe:seeds` (FE, BE đã seed) | **29/29** (mới nhất 19/07; số probe tăng dần theo phiên) |
| Mode A | Mở đúng happy slug theo [manual-ui-playbook-gd1-gd6.md](manual-ui-playbook-gd1-gd6.md) §0.6 |
| Mode B / full-chain API | `node scripts/gd3-gd4-gd5-full-chain-api.mjs` |
| Sau Confirm GĐ6 trên `seal-e2e-2026` | **Restart BE** rồi mới `probe:seeds` |
| Unit guest judge (BE) | `mvn -Dtest=TempJudgeServiceImplTest,AuthServiceTest,InvitationServiceImplTest test` |
| Security / portal (BE) | `mvn -Dtest=StudentAccessGuardParticipatedTest,*StudentPortal*,JudgeAssignmentServiceImplMultiTrackTest test` |
| FE unit + secret | `npm run test:unit:all` → **29 pass** + SEC-CLOU-02 PASS |
| FE build | `npm run build` → không chứa Cloudinary API secret trong `dist` |
| Manual GĐ1 activate | DRAFT thiếu điều kiện → tooltip blockers; đủ → **Xác nhận Kích hoạt** → ONGOING |
| Manual early-wait | START_NOW + lead 1 phút → **Phát đề** disabled + «Chưa tới giờ thi» |
| Manual mời guest | Badge **Chờ đổi mật khẩu** → đổi MK → **Đã duyệt** → mới gán CK |
| Manual Radar | Đội thừa maxMembers hiện «thừa»; copy không nói tự ACTIVE |
| Manual GĐ4 (18/07) | Results **không** có tab Vé vớt; Đồng điểm reorder → **Chốt chuyển vòng** = Top-N |
| Manual Personnel Guard (18/07) | Gán GK bảng 2 cùng vòng → `JUDGE_ASSIGN_DUPLICATE`; mentor đội bảng X làm GK bảng X → `CONFLICT_MENTOR_JUDGE_SAME_TRACK` |

Happy slug seed (`DevSeedCatalog.ALL_DEV_HACKATHON_SLUGS`): **9** — `seal-e2e-2026`, `seal-fall-2025-finished`, `seal-gd3-prelim-open`, `seal-gd4-advance-ready`, `seal-gd4-tiebreak-submission-time`, `seal-gd4-tiebreak-manual`, `seal-gd4-wildcard-gap`, `seal-gd5-final-active`, `seal-gd6-pending-confirm`.

---

## 4. Tài liệu liên quan (enterprise)

| File | Vai trò |
|------|---------|
| [manual-ui-playbook-gd1-gd6.md](manual-ui-playbook-gd1-gd6.md) | Click-by-click manual (non-IT) |
| [enterprise-regression-matrix-gd1-gd6.md](../../../docs/testing/enterprise-regression-matrix-gd1-gd6.md) | Ma trận TC ID GĐ1–GĐ6 + SEC |
| [ci-hardening.md](../../../docs/testing/ci-hardening.md) | CI từng repo (BE/FE) |
| [gate-regression-test-matrix-gd1-gd6.md](gate-regression-test-matrix-gd1-gd6.md) | Gate regression |
| [gate-button-matrix-gd2-gd6.md](gate-button-matrix-gd2-gd6.md) | Nút theo gate |
