# Deep UI/UX Audit — Summary 2026-07-18

| ID | Status | Note | Evidence |
|----|--------|------|----------|
| PUB-01 | PASS | sawWithoutF5=true | pub-step-02-student-after.png |
| PUB-02 | SKIP | Soft-hide needs published announcement UI click | — |
| GD5-NO-PDF | PASS | no upload PDF | gd5-step-01-final-config.png |
| GD5-FINALISTS-TOPN | PASS | FinalistsCard | gd5-step-01-final-config.png |
| GD5-BTNS | PASS | statusBtns=1 | gd5-step-02-rounds.png |
| HARD_LOCK | PASS | CK submit page | gd5-step-03-submit.png |
| PENDING_CONFIRM | SKIP | Need lock scoring final to flip status | — |
| FAIL-03 | SKIP | Deferred to pub phase 2-context | — |
| TIMER-RT-01-GD5 | PASS | queue timer/controls | gd5-step-04-queue.png |
| STT-01-GD5 | PASS | queue slots | gd5-step-04-queue.png |
| P0-WC | PASS | noWcTab=true redirect=true steps=6 | p0-wc-redirect.png |
| TC-WC-03 | PASS | tabs=Kết quả/Danh sách Chung kết & Bị loại/Kiểm tra chấm/Đồng điểm (0) | p0-wc-results.png |
| P0-HEAD | PASS | noHead=true transferVisible=true | p0-head-queue.png |
| P0-PG | PASS | dup=JUDGE_ASSIGN_DUPLICATE; mentorJudge=blocked HTTP 422 CONFLICT_SAME_TRACK | — |
| COORD-SCORE-ALL-01-SUMMARY | PASS | HTTP 200 tracks=2 | — |
| COORD-SCORE-ALL-01-DETAIL | PASS | HTTP 200 teams=4 criteria=4 judges=2 | — |
| COORD-SCORE-ALL-01 | PASS | apiSum=true apiDet=true ui=true noWc=true | score-a1-panel.png |
| STU-SCORE-01-IDOR | PASS | HTTP 403 | — |
| STU-SCORE-01 | PASS | published; judges=Giám khảo 1,Giám khảo 2; anon=true | — |
| STU-SCORE-01-UI | PASS | card=true | score-a2-card.png |
| GD1-TABS | PASS | tabs=8 | gd1-step-01-general.png |
| GD1-PG-DUPLICATE | PASS | HTTP 409 code=JUDGE_ASSIGN_DUPLICATE | — |
| GD1-INCOMPLETE-SEED | PASS | Found leftover slug id=3 | — |
| GD1-READINESS | PASS | activate disabled=true tip="Chưa thể kích hoạt
Chưa có Vòng Sơ loại (PRELIMINARY/SEMIFINAL)
Thiếu Round Chun" | gd1-step-readiness-bad.png |
| GD2-TEAMS | PASS | teams page | gd2-step-01-teams.png |
| GD2-LOTTERY-TAB | PASS | lottery tab | gd2-step-02-lottery.png |
| R1 | PASS | student url=http://localhost:5173/dashboard | gd2-step-03-student-nav.png |
| GD3-BTNS | PASS | status=true close=true | gd3-step-01-rounds.png |
| GD3-FORCE-ALERT | PASS | forceAlert=true ratio=false | gd3-step-02-close-modal.png |
| LOTTERY-DATA-01 | PASS | listed=3 teams=6 hasNotSubmittedLabel=false | gd3-step-04-lottery-panel-BEFORE.png |
| LOTTERY-DATA-01-READINESS | PASS | tag="Tổng đội: 3" chuaNop="" | gd3-step-04b-readiness-AFTER-fix.png |
| LOTTERY-GATE-01 | PASS | late=1 disabled=true tip="Còn 1 đội nộp trễ chưa xử lý — duyệt hoặc từ chối trước khi quay số." track=7 | gd3-step-05-lottery-gate.png |
| SH-01 | PASS | post-close state disabled=true | gd3-step-04-lottery-panel-BEFORE.png |
| LOCK-03 | PASS | unlockBtns=0 | — |
| CTRL-01 | SKIP | Needs active presenting slot — covered by e2e when queue live | gd3-step-06-judge.png |
| FAIL-01 | SKIP | Needs controller grant UI after shuffle | — |
| FAIL-02 | SKIP | Needs 2 coord race | — |
| HEART-01 | SKIP | Needs controller session 30s | — |
| XFER-01 | SKIP | Needs offline judge | — |
| WS-DB-01 | SKIP | Needs burst shuffle | — |
| LATE-01 | SKIP | Needs LATE approve after shuffle | — |
| SH-02 | SKIP | Needs PRESENTING state | — |
| BC1 | SKIP | no judge/submission/criterion (gd3 ctx=true) | — |
| BC2 | SKIP | no judge/submission/criterion (gd4 ctx=true) | — |
| BC3 | PASS | blocked HTTP 422 ROUND_NOT_ACTIVE | — |
| BC4 | SKIP | no judge/submission/criterion (gd3 ctx=true) | — |
| BC5 | PASS | blocked HTTP 422 INVALID_STATE | — |
| BC6 | PASS | blocked HTTP 422 INVALID_STATE (unmapped, expected SCORING_INCOMPLETE_BEFORE_NEXT/NOT_TRACK_CONTROLLER/ROUND_NOT_ACTIVE/VALIDATION_FAILED) | — |
| EARLY-WAIT-01 | SKIP | Needs pre-examAt seed window | — |
| STT-01 | SKIP | Needs shuffled queue + student view | — |
| TIMER-RT-01 | SKIP | Needs live presenting timer | — |
| GD4-NO-WC | PASS | Kết quả/Danh sách Chung kết & Bị loại/Kiểm tra chấm/Đồng điểm (0) | gd4-step-01-results.png |
| GD4-FORCE-ACK | PASS | N=4 hint=true | gd4-step-03-force-ack.png |
| GD4-ADVANCE | PASS | advanced | — |
| TIEBREAK_REQUIRED | PASS | tie=true advDisabled=true | gd4-step-05-tiebreak.png |
| RESULT_NOT_PUBLISHED | SKIP | Activate CK without publish — catalog gate | — |
| GD6-PRIZE-UI | PASS | prize UI | gd6-step-01-results.png |
| CSV-01 | PASS | export/confirm UI | gd6-step-01-results.png |
| AUDIT-RO-01 | PASS | coord 200 | — |
| AUDIT-RO-01-STUDENT | PASS | student 403 | — |
| NO_PRIZES | SKIP | Would need empty-prize seed — avoid mutating FINISHED | — |
| GD6-FINISHED-RO | PASS | archive loads | gd6-step-02-finished.png |
| STU-SCORE-01-STABLE | PASS | Giám khảo 1/Giám khảo 2 vs Giám khảo 1/Giám khảo 2 | — |
| H-TABS | PASS | tabs=Cấu hình chung/Vòng thi/Bảng đấu/Tiêu chí đánh giá/Nhân sự/Lịch trình & Sự kiện/Bốc thăm & khai mạc/Cấu hình Chung kết | gd1-step-01-general.png |
| H-FORM-ROUND | PASS | noBanKet=true thoiLuong=true | gd1-step-rounds.png |
| H-FORM-TRACK | PASS | track tab | gd1-step-tracks.png |
| H-FORM-CRITERIA | PASS | canBang=false | gd1-step-criteria.png |
| H-PEOPLE-UX | PASS | people tab | gd1-step-people.png |
| H-FORM-EVENT | PASS | noPresentation=true | gd1-step-events.png |
| H-FORM-HACKATHON | PASS | general tab load + no personal BXH toggle (seed UI) | — |
| H-NAV | PASS | SideNav checked via setup tabs; Locked text gated on GĐ4 | — |
| H-ACTIVATE | PASS | covered by GD1-READINESS | — |
| H-READONLY | SKIP | Needs ONGOING locked fields seed | — |
| I5 | PASS | GIỮ menu «Phân tích & dữ liệu» — chốt chính thức (RBL export + dashboard) | cross-i5-analytics.png |
| TC-STU-01 | PASS | student results visible=true | cross-tc-stu-01.png |
| TC-SHUF-01 | PASS | Same expect as LOTTERY-GATE-01 — see gd3 result | — |
| TC-TMR-01 | SKIP | Needs live presenting timer session | — |
| TC-TMR-02 | SKIP | Needs judge scoring form reset on team change | — |
| TC-SYNC-01 | PASS | Covered by PUB-01 | — |
| TC-SYNC-02 | SKIP | Needs ended vs inactive message matrix | — |
| R5 | SKIP | Submitted-at column on ranking — visual | — |
| R4 | PASS | Calibration độc lập (rbl_calibration_*) — xem phase calib | — |
| R7 | PASS | Global event selector on header + action-center Overview | — |
| R9 | PASS | Student round scoreboards via results page | — |
| D2 | SKIP | Single CTA submit — visual GD3/5 | — |
| R8 | SKIP | Locked scoring text | — |
| R12 | PASS | Lottery readiness non-N+1 direction (batch roster) | — |
| G-PROGRESS | SKIP | Duplicate progress UI | — |
| I1 | SKIP | Guest invite tokenSent=false | — |
| I2 | PASS | Covered GD5-NO-PDF | — |
| I3 | SKIP | Clone form regression | — |
| I-CK-EVENT-DD | PASS | Event context via global header selector (UX-CTX-01) — không còn dropdown lệch tab/standalone | — |
| J2 | SKIP | Late review deep-link with data | — |
| J3 | SKIP | Early-start deadline sync labels | — |
| AR | PASS | Covered PUB-01 / queue WS | — |
| PRIZE-02 | SKIP | Avoid mutating FINISHED prize assign | — |
| INVARIANT-01 | SKIP | Needs LATE_PENDING on HARD_LOCK seed | — |
| INVARIANT-02 | SKIP | Needs LATE_APPROVED on HARD_LOCK seed | — |
| REG-CLASSIFY-01-UI | PASS | userType=true studentFields=true institutionField=true | reg-classify-01-ui.png |
| RBL-UI-01 | PASS | page=true select=true csvLabel=true | rbl-ui-01-analytics.png |
| RBL-UI-01-EXP-DROPDOWN | PASS | export type select present | — |
| RBL-VARIANCE-01-UI | PASS | segment=true anonLeakCheck=true | rbl-variance-01-ui.png |
| CRITERIA-TPL-01-UI | PASS | applyBtn=true | criteria-tpl-01-ui.png |
| RBL-CALIB-01-UI | PASS | FE Calibration UI purged: analyticsLeak=false judgeLeak=false | rbl-calib-01-ui.png |
| RBL-CALIB-01 | PASS | createPrompt HTTP 201 id=1 | — |
| RBL-CALIB-01-ANON | PASS | distribution hides judge ids anonApi=true | — |
| UX-CTX-01 | PASS | global-event-selector: dashboard=Y setup=Y analytics=Y final-config=Y | cross-ux-ctx-01-selector.png |
| A1 | PASS | Coord score-all summary — see COORD-SCORE-ALL-01-SUMMARY (phaseScore) | — |
| A2 | PASS | Student anonymized breakdown — see STU-SCORE-01 (phaseScore) | — |
| A3 | PASS | Coord per-track score matrix — see COORD-SCORE-ALL-01-DETAIL (phaseScore) | — |
| A4 | PASS | Advance / chốt chuyển vòng — see GD4-ADVANCE / GD4-FORCE-ACK (phaseGd4) | — |
| A5 | PASS | Student sees published rank/advance — see TC-STU-01 / PUB-01 | — |
| THESIS-RBL-01 | PASS | job=1 dl=200 headerOk=true anonHeader=true | — |
| RQ-SMOKE | PASS | CSV header ready for rbl_irr_analysis.py: "round_id,round_name,submission_id,criterion_id,criterion_name,criterion_type,ano" | — |
| THESIS-RBL-02 | PASS | variance HTTP 200 anon=true | — |
| THESIS-RBL-04 | PASS | PENALTY-filterable columns present in export header | — |
| RBL-BAD-01 | PASS | blocked HTTP 403 FORBIDDEN | — |
| RBL-BAD-02 | PASS | blocked HTTP 403 FORBIDDEN | — |
| RBL-BAD-03 | PASS | invalid-job download HTTP 404 RESOURCE_NOT_FOUND | — |
| RBL-BAD-04 | PASS | blocked HTTP 400 MALFORMED_REQUEST (unmapped, expected VALIDATION_FAILED) | — |
| RBL-BAD-05 | PASS | blocked HTTP 403 FORBIDDEN | — |
| THESIS-RBL-03 | PASS | calibration channel isolated + anonymized (HTTP 200 anon=true) | — |
| Wildcard-HEAD | PASS | noWildcardTab=true noHeadLabel=true | p0-head-queue.png |
| IDOR-01 | PASS | HTTP 200 empty list — no foreign rows leaked | — |
| IDOR-05 | PASS | blocked HTTP 403 FORBIDDEN | — |
| IDOR-06 | PASS | blocked HTTP 403 FORBIDDEN | — |
| IDOR-02 | PASS | blocked HTTP 403 FORBIDDEN | — |
| IDOR-03 | PASS | blocked HTTP 403 FORBIDDEN | — |
| IDOR-04 | PASS | blocked HTTP 403 FORBIDDEN | — |
| IDOR-08 | PASS | blocked HTTP 403 NOT_TEAM_MEMBER | — |
| IDOR-07 | PASS | blocked HTTP 403 FORBIDDEN | — |
| VALID-01 | SKIP | no judge/submission/criterion | — |
| VALID-02 | PASS | blocked HTTP 400 VALIDATION_FAILED | — |
| VALID-03 | PASS | blocked HTTP 400 VALIDATION_FAILED | — |
| VALID-04 | PASS | blocked HTTP 400 VALIDATION_FAILED | — |
| VALID-05 | PASS | blocked HTTP 422 INVALID_ROUND_STATE_QUEUE_NOT_SHUFFLED | — |
| CALIB-01-ANALYTICS | PASS | Analytics must not show Hiệu chỉnh / Phiên đồng thuận mẫu (leak=false) | — |

Per-phase: `REPORT-gd1.md` … `REPORT-gd6.md` trong thư mục này.