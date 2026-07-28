# Biên bản nghiệm thu — Audit Remediation SEAL (v3.3)

> Ngày nghiệm thu: 18/07/2026
> Phạm vi: Plan `audit_remediation_seal_1bc0cf36` (Phase 0 → 6 + Phase 5.5 Button matrix).
> Phương pháp: rà soát trực tiếp code (file:dòng) + chạy lại toàn bộ test, không dựa vào trạng thái tick.

## 1. Kết quả nghiệm thu 8 hạng mục

| # | Hạng mục | Bằng chứng đã kiểm (file:dòng) | Kết quả |
|---|----------|--------------------------------|---------|
| 1 | **P0 baseline** | Smoke REGRESSION P0 chạy trước khi sửa (TB/REL/CSV/I3/AR) | ✅ Đạt |
| 2 | **Phase 1 — Bỏ Wildcard** | `RoundProgressionServiceImpl`: `requireWildcardReadyForAdvance` no-op (L1312), `wildcardCandidates` → `emptyWildcardResponse` (L796); API thật trả `candidates:[]`; FE `useRoundResults.showWildcardTab=false`; `RoundFormModal` ẩn `wildcard_enabled`, giữ `top_n_advance` | ✅ Đạt |
| 3 | **Phase 2 — Bỏ HEAD + Personnel Guard** | `JudgeAssignmentServiceImpl`: reject HEAD cả 3 đường (L95/154/175); anti-ubiquity round-scope (L122–129); mentor-isolation `assertNotMentorOfTeamInTrack` (L116) + `existsByMentorIdAndTrackId` (L110); staffing prelim=NORMAL, final=NORMAL/FINAL_EXTERNAL; API thật reject HEAD → 422 | ✅ Đạt |
| 4 | **Phase 3 — J1/J2/J3** | J1: `PresentationQueuePage` `hasLatePending` gate + `totalParticipatingCount`; J2: `LateSubmissionReviewPage` đọc `hackathonId`+`roundId` + breadcrumb; J3: `RoundActivationServiceImpl.startAlreadyActiveRoundEarly` nén examAt/submissionDeadline | ✅ Đạt |
| 5 | **Phase 4 — I + REGRESSION P0** | I1: `TempJudgesPage` xử lý `tokenSent` false-success + nút "Gửi lại"; AR: `SubmissionStatusPanel` refetch qua `useEffect` (L116/125/128); I2/I3/CSV/REL verify chạy đúng | ✅ Đạt |
| 6 | **Phase 5.5 — Button matrix** | `button-matrix-gd1-gd6.md` (117 dòng, GĐ1–6 + TC-BTN-*); `roundLifecycleGates.test.js` (6 khối, pass) | ✅ Đạt |
| 7 | **Phase 5 — R3/R4/R5/R6/R7/R9/D2** | R3 `JudgeScoringWorkspace` dùng `Collapse` rubric; R4 calibration đã gỡ (0 match trong `FinalRoundConfigPage`); R5 cột "Thời gian nộp"; R6 `RoundAccessGuard` phân biệt "chưa kích hoạt" vs "đã kết thúc" (L53/54); R7 `Dashboard` có `Select` chọn sự kiện (L241/249); R9 tab `round_scoreboards`; D2 chỉ còn 1 nút nộp | ✅ Đạt |
| 8 | **Phase 6 — H15/H19/R8/R12** | H15 sidebar còn 6 mục (bỏ 3 dư) — `MainLayout` L105–110; H19 modal "Các đội trong bảng" — `TrackManagementPage` L271–374; R8 badge "Sơ loại" — `RoundManagementPage` L817/1369; R12 lottery batch-load chống N+1 — `HackathonLotteryServiceImpl` | ✅ Đạt |

## 2. Tiêu chí phụ (dễ sót) — đã kiểm

| Tiêu chí | Bằng chứng | Kết quả |
|----------|-----------|---------|
| TC-WC-01: N ≤ số đội thực tế/bảng | `roundAdvancementRules.js` L146 `if (topN > track.teamCount)` báo lỗi rõ | ✅ Đạt |
| I5: Giữ menu "Phân tích & dữ liệu" | `MainLayout.jsx` L108 vẫn còn | ✅ Đạt (đúng quyết định) |
| Đổi nhãn "Luật Tiebreak" → "Luật xử lý đồng điểm" | `RoundFormModal.jsx` L468 | ✅ Đạt |
| Đổi nhãn "Vào chung kết mỗi bảng (dự tính)" | `RoundFormModal.jsx` L429 | ✅ Đạt |

## 3. Kết quả kiểm thử tự động (chạy lại 18/07/2026)

| Bộ test | Phạm vi | Kết quả |
|---------|---------|---------|
| BE full test suite | `mvnw test` toàn bộ | ✅ pass |
| FE production build | `npm run build` | ✅ pass |
| FE unit | `test:unit:all` (29 test, 11 suite) | ✅ 29/29 |
| E2E seed-parity | FE↔BE slug parity | ✅ 3/3 |
| E2E seed-matrix | 9 slug read-only GĐ1–GĐ6 | ✅ 9/9 |
| E2E gd2 | teams / orphan / lottery | ✅ 3/3 |
| E2E mode-b continuous | Tạo sự kiện → FINISHED (GĐ1–GĐ6) | ✅ 6/6 |
| API smoke thủ công | WC rỗng, HEAD reject 422, tiebreak, ranking | ✅ đúng kỳ vọng |

## 4. Ghi chú kỹ thuật

- **Wildcard migration (WC-MIG):** advance chỉ theo Top-N; proposal cũ bị bỏ qua nhờ `requireWildcardReadyForAdvance` no-op + `wildcardCandidates` trả rỗng — hackathon live không bị kẹt GĐ4.
- **Rollback HEAD:** xóa thẳng (không feature-flag) theo quyết định đã chốt.
- **Mode-b smoke UI phòng chấm (GĐ3/GĐ5):** có thể bỏ qua hợp lệ khi queue đã chấm đủ qua API (phòng chấm không còn mở). `catch` đã được thu hẹp: chỉ nuốt timeout hiển thị/ẩn; lỗi thật (thiếu assignment, API 4xx/5xx, crash) sẽ **fail test**. GĐ5 thêm assert "≥ 1 đội nộp CK thành công".

## 5. Kết luận

Cả 8 hạng mục trong plan đều có code thật đúng mô tả, không mục nào bị tick khống hoặc bỏ sót. Toàn bộ test tự động xanh. **Đủ điều kiện nghiệm thu.**
