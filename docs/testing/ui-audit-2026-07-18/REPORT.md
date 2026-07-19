# UI/UX Audit Report — 2026-07-18

Playwright audit theo plan đã chắt lọc + playbook `manual-ui-playbook-gd1-gd6.md`.  
Ảnh: thư mục này (`*.png`). Script: `seal-hackathon-fe/scripts/ui-ux-audit-run.mjs`.

## Tóm tắt

| Hạng mục | Kết quả |
|----------|---------|
| Phase 0 kiến trúc (Wildcard / HEAD / TRANSFER) | **PASS** |
| GĐ3 2 nút + modal kết thúc sớm | **PASS** (đã fix Alert cưỡng ép) |
| GĐ4 chuyển vòng “kẹt” | **Giải thích + UX fix** (phải gõ N force-ack) |
| Personnel Guard (R6) | **PASS** (unit `JudgeAssignmentServiceImplMultiTrackTest`) |
| Bug `?status=ADVANCED` → 400 | **ĐÃ FIX** |
| FE build | **PASS** |

---

## Phase 0 — Kiến trúc lớn (Gap 1)

| ID | Status | Note | Evidence |
|----|--------|------|----------|
| A1 | PASS | Tabs: Kết quả \| Danh sách CK & Bị loại \| Kiểm tra chấm \| Đồng điểm (0) — **không** Vé vớt | `A1-results-tabs.png` |
| A2 | PASS | `?tab=wildcard` → active tab **Kết quả** | `A2-wildcard-redirect.png` |
| A3 | PASS | Không UI Trưởng ban / HEAD trên people + final-config | `A3-people.png` |
| A4 | PASS | Chỉ Chuyển quyền / không Takeover tạm | `A4-presentation-queue.png` |
| FAIL-03 | PASS* | Transfer + chụp judge room (*quan sát screenshot; timing ≤1s cần re-check tay khi 2 controller thật) | `A5-*.png` |

> Ghi chú: seed vẫn còn `assignmentType: HEAD` trong DB response list judges — UI không hiện “Trưởng ban”, nhưng enum HEAD còn trên dữ liệu seed (cleanup seed tùy chọn, không chặn vận hành TRANSFER).

---

## Điểm nóng 1 — GĐ3 kết thúc sớm (Gap 5: trước → fix → sau)

| ID | Status | Note | Evidence |
|----|--------|------|----------|
| GD3-BTN-STATUS | PASS | `round-submission-status-btn` hiện | `GD3-rounds-before.png` |
| GD3-BTN-CLOSE | PASS | `round-close-submission-early-btn` + nút card **Kết thúc thời gian thi sớm** | `GD3-rounds-before.png` |
| GD3-STATUS-PANEL | PASS | Panel Tình trạng nộp bài live (5/6 đội) | `GD3-submission-status-panel.png` |
| GD3-FORCE-ALERT (TRƯỚC) | FAIL→fixed | Modal có 5/6 + list chưa nộp + KHÔNG THỂ HOÀN TÁC — **thiếu** Alert đỏ cưỡng ép | `GD3-close-early-BEFORE-fix.png` |
| GD3-FORCE-ALERT (SAU) | PASS | Alert đỏ: «Còn 1 đội CHƯA nộp bài» + «cưỡng ép kết thúc» | `GD3-close-early-AFTER-fix.png` |

**Fix:** [`RoundManagementPage.jsx`](../../../seal-hackathon-fe/src/features/rounds/pages/RoundManagementPage.jsx) — `Alert type="error"` khi `submitted < total`; `Alert type="success"` khi đủ.

---

## Điểm nóng 2 — GĐ4 → GĐ5 chuyển vòng

| ID | Status | Note | Evidence |
|----|--------|------|----------|
| GD4-CAN-ADVANCE | PASS | Nút Chốt chuyển vòng sáng sau publish | `GD4-advance-after-publish.png` |
| GD4-ADVANCE-FORCE-ACK | PASS | Modal yêu cầu gõ đúng số N (`advance-confirm-n-input`) — đây là lý do “đã xong nhưng không chốt được” nếu bỏ qua bước gõ | `GD4-advance-modal-typed.png` |
| GD4-ADVANCE | PASS | Chốt được sau khi gõ N=4 | `GD4-after-advance.png` |
| GD4-TIEBREAK-GATE | PASS | Seed tiebreak: tab Đồng điểm + advance disabled | `GD4-tiebreak-manual.png` |
| PUB-01 | PARTIAL | Lần 1 student login sai email; lần 2 seed đã publish. Cần re-run 2-tab trên seed tươi | — |

**Fix UX:** [`PreliminaryResultsPage.jsx`](../../../seal-hackathon-fe/src/features/rounds/pages/PreliminaryResultsPage.jsx) — hint dưới ô nhập: nút chỉ sáng sau khi nhập đúng số N.

---

## Gap 2 — Regression lịch sử

| ID | Status | Note |
|----|--------|------|
| R1 | PASS | Student login navigate không kẹt |
| R2 | SKIP | Cần invite mới |
| R3 | PASS | Tab lottery reachable |
| R4 | PASS | Không UI upload PDF đề CK |
| R5 | PASS* | Tiebreak UI/tabs quan sát được (*ghost banner cần case resolve tay) |
| R6 | PASS | Unit `JudgeAssignmentServiceImplMultiTrackTest` PASS; API đúng = `POST /judge-assignments` (không POST `/tracks/{id}/judges`) |

---

## Gap 3 — Chương L

| ID | Status | Note |
|----|--------|------|
| SH-01 | PASS | Shuffle disabled + tooltip «Chờ hết hạn nộp bài» |
| LOCK-03 | PASS | Coord không còn nút unlock |
| CSV-01 | PASS | Export UI trên GĐ6 |
| PRIZE-02 | PASS | Prize UI visible |
| AUDIT-RO-01 | PASS | Coord GET audit-logs → 200 |
| SH-02, LATE-01, INVARIANT-01/02, PUB-02, CTRL-01, FAIL-01/02, HEART-01, XFER-01, WS-DB-01 | SKIP | Cần mutating dài / đã cover e2e-unit sẵn |

---

## Expand GĐ1/2/5/6

| ID | Status | Note |
|----|--------|------|
| GD1-ACTIVATE | N/A→PASS* | `seal-e2e-2026` đã ONGOING — không hiện nút activate (đúng) |
| GD5-BTNS | PASS | status + close-early buttons trên CK |
| GD6-FINISHED-RO | PASS | Archive results load |

---

## Bug phát hiện & đã sửa trong phiên

1. **GĐ3 modal cưỡng ép thiếu Alert riêng** → thêm Alert error/success có điều kiện.  
2. **GĐ4 “không chuyển vòng”** → user chưa gõ số N force-ack; thêm hint UX.  
3. **`GET /teams?status=ADVANCED` → 400** (spam log) → sửa `SubmissionStatusPanel` + `PresentationQueuePage` dùng `ACTIVE` + derive CK từ submissions.

## Không sửa (ghi nhận)

- Seed vẫn gán `assignmentType=HEAD` trong list judges API — UI không còn label Trưởng ban; cleanup seed tùy chọn.  
- PUB-01 2-tab WS: cần 1 lần chạy tay/seed tươi để xác nhận toast Student không F5.  
- Một số ID Chương L SKIP (mutating dài) — dựa e2e pyramid sẵn.

## Cách tái chạy

```powershell
# BE + FE đã chạy
cd seal-hackathon-fe
node scripts/ui-ux-audit-run.mjs --phase=arch
node scripts/ui-ux-audit-run.mjs --phase=gd3
node scripts/ui-ux-audit-run.mjs --phase=gd3-after
node scripts/ui-ux-audit-run.mjs --phase=gd4
node scripts/ui-ux-audit-run.mjs --phase=hist
node scripts/ui-ux-audit-run.mjs --phase=l
node scripts/ui-ux-audit-run.mjs --phase=expand
```
