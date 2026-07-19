# Danh mục Lỗi Cố tình & Cách tái tạo (Intentional Errors Catalog)

Tài liệu này tổng hợp các mã lỗi (blockers, gate rules) ở từng giai đoạn của Hackathon.
Thay vì dùng seed database riêng cho từng lỗi, QA/Dev dùng **Happy Path Slugs** và thao tác tay để ép hệ thống văng lỗi phục vụ kiểm thử.

**6 slug chuẩn:** xem [dev-seed-slugs-guide.md](dev-seed-slugs-guide.md).

---

## Giai đoạn 1 (Setup & Readiness)

*Dùng slug:* `seal-e2e-2026` (tạo mới draft hoặc sửa trực tiếp)

| Mã lỗi / Blocker | Cách tái tạo bằng tay trên UI/API | Tham khảo (Slug cũ) |
| :--- | :--- | :--- |
| `EVENT_KICKOFF_MISSING` | Tạo / sửa hackathon thiếu event KICKOFF, cố Publish / check readiness | `seal-gd1-no-kickoff`, `seal-gd1-event-order-bad` |
| `EVENT_ORDER_VIOLATION` | Tạo AWARDS/WS khi thiếu milestone trước, hoặc đảo thứ tự ngày milestone | `seal-gd1-event-order-violation` |
| `MISSING_FINAL_ROUND` | Xóa Final Round trong tab cấu hình vòng → Check Readiness | `seal-gd1-prelim-only` |
| `JUDGE_FINAL_AT_PHASE1` | Gán Judge vào vòng Chung kết khi còn phase sơ loại / chưa tới GĐ4 | `seal-gd1-judge-final-early` |
| `EXTERNAL_JUDGE_NOT_ALLOWED_IN_PRELIM` | Gán Judge EXTERNAL (guest) vào Track sơ loại — message **không** nhắc «Trưởng ban»/HEAD | People → assign guest → track |
| `INVALID_ASSIGNMENT_TYPE` (CK) | Gán INTERNAL làm `FINAL_EXTERNAL`, hoặc EXTERNAL/loại phân công không hợp lệ trên CK (Personnel Guard: SL=`NORMAL`, CK=`FINAL_EXTERNAL`) — message **không** nhắc «Trưởng ban»/HEAD | GĐ4 assign judges |
| `READINESS_INCOMPLETE` | DRAFT thiếu round/track/criteria/AWARDS rồi Publish / readiness | `seal-gd1-incomplete`, `seal-gd1-no-awards` |

---

## Giai đoạn 2 (Registration & Lottery)

*Dùng slug:* `seal-e2e-2026`

| Mã lỗi / Blocker | Cách tái tạo bằng tay trên UI/API | Tham khảo (Slug cũ) |
| :--- | :--- | :--- |
| `TEAM_NOT_LOCKED` | ≥1 team chưa LOCKED → Admin Chốt danh sách & Lottery | `seal-gd2-lottery-not-locked`, `seal-gd2-teams-edge` |
| `REGISTRATION_CLOSED` | Đưa Registration End về quá khứ → user tạo team mới | `seal-gd2-registration-closed` |
| `RE_LOTTERY_DENIED` | Lottery xong + activate prelim (GĐ3) → gọi Re-lottery | `seal-gd2-round-active` |

---

## Giai đoạn 3 (Prelim — Sơ loại)

*Dùng slug:* `seal-gd3-prelim-open`

**Rule nhân sự (Personnel Guard):** sơ loại chỉ Judge **INTERNAL** `NORMAL` (1 GK = 1 bảng/vòng). Guest EXTERNAL chỉ trên CK (`FINAL_EXTERNAL`). Seed pool: `judge1–4`, `mentor1–3`, `guestjudge`/`guestjudge2–3`.

| Mã lỗi / Blocker | Cách tái tạo bằng tay trên UI/API | Tham khảo (Slug cũ) |
| :--- | :--- | :--- |
| `SCORING_NOT_OPEN` | Judge chấm khi chưa hết coding / chưa close-early / chưa mở scoring | `seal-gd3-scoring-gate` |
| `MENTOR_JUDGE_CONFLICT` | Gán User A mentor team/track rồi gán cùng User A làm Judge xung đột | `seal-gd3-judge-mentor-conflict` |
| `LOTTERY_MISSING` | (Can thiệp DB hoặc clone) xóa phân bảng → activate / mở coding | `seal-gd3-no-lottery` |
| Round config lệch / edge nộp | Đổi lịch deadline / hard-lock lệch → nộp/chấm | `seal-gd3-edge-errors`, `seal-gd3-round-config-edge` |

---

## Giai đoạn 4 (Advance — Chuyển tiếp)

*Dùng slug:* `seal-gd4-advance-ready`

| Mã lỗi / Blocker | Cách tái tạo bằng tay trên UI/API | Tham khảo (Slug cũ) |
| :--- | :--- | :--- |
| `RESULT_NOT_PUBLISHED` | Không Publish kết quả SL → Advance / Activate CK | `seal-gd4-ck-unpublished` |
| `TIEBREAK_UNRESOLVED` | Chỉnh điểm 2 team bằng nhau ở ranh giới cắt → Advance không resolve | `seal-gd4-tiebreak-gate` |
| `FINAL_CRITERIA_MISSING` | Xóa hết Criteria của Chung kết → Activate Final | `seal-gd4-ck-no-criteria` |
| `TC-WC-03` (thay WILDCARD_OFF) | **Wildcard đã bỏ hẳn.** Verify FE/BE: `grep` = 0 tab/route/label «Vé vớt\|Wildcard» trên UI Kết quả (trừ redirect legacy `?tab=wildcard` → tab Kết quả). **Không** tái tạo cấp vé Wildcard. | — |

---

## Giai đoạn 5 (Final — Chung kết)

*Dùng slug:* `seal-gd5-final-active`

| Mã lỗi / Blocker | Cách tái tạo bằng tay trên UI/API | Tham khảo (Slug cũ) |
| :--- | :--- | :--- |
| `HARD_LOCK_LATE` | Sau submission deadline CK (+ hard lock) → team Submit | `seal-gd5-late-hardlock` |
| `TEAM_NOT_ADVANCED` | Token đội không ADVANCED → Submit bài Chung kết | `seal-gd5-not-advanced` |
| `FINAL_JUDGE_MISSING` | Xóa hết Judge CK → Queue / Open Scoring | `seal-gd5-judge-edge` |

---

## Giai đoạn 6 (Post-event & Prizes)

*Dùng slug:* `seal-gd6-pending-confirm` (và `seal-gd5-final-active` khi premature)

| Mã lỗi / Blocker | Cách tái tạo bằng tay trên UI/API | Tham khảo (Slug cũ) |
| :--- | :--- | :--- |
| `NO_PRIZES_RECORDED` | Xóa hết giải thưởng → Confirm Final Results | `seal-gd6-prizes-empty` |
| `CONFIRM_BEFORE_LOCK` | Trên slug GĐ5 chưa Lock Scoring → gọi Confirm | `seal-gd6-edge-errors` |
| `DUPLICATE_PRIZE` | Gán 2× First Prize cùng team (nếu rule cấm) | `seal-gd6-prize-duplicate` |

---

## Liên kết

- [dev-seed-slugs-guide.md](dev-seed-slugs-guide.md) — 6 happy slug
- [manual-ui-playbook-gd1-gd6.md](manual-ui-playbook-gd1-gd6.md) — hướng dẫn UI happy path
- [master-slug-test-matrix.md](master-slug-test-matrix.md) — ma trận rút gọn
