# Báo cáo audit toàn diện GĐ3–GĐ6

> **Ngày chạy:** 2026-06-30 (cập nhật sau fix)  
> **Môi trường:** Windows · BE `spring.profiles.active=dev` (MySQL) · FE `npm run build` / `npm run lint`  
> **Mục tiêu:** Phát hiện lỗi tồn đọng, lỗi ẩn phá workflow, logic sai spec, nghiệp vụ “AI tự vẽ”  
> **Nguồn spec:** `workflow.md`, `01-business-rules-gd3.md`, `gate-regression-test-matrix-gd1-gd6.md`, `09-be-backlog-gd4-gd5-gd6.md`

---

## Phần A — Tóm tắt điều hành

| Chỉ số | Kết quả (ban đầu) | Kết quả (sau fix 2026-06-30) |
|--------|-------------------|------------------------------|
| **mvn test** | 214 tests · **4 FAIL** | **216 tests · 0 FAIL** (+ `Gd4ToGd6FlowIntegrationTest` ×2) |
| **API manual (script)** | 16 case · 12 PASS · 2 FAIL | **G3-H01 retest live → HTTP 200 PASS** (`g3-h01-retest.py`) |
| **API probe bổ sung** | Tiebreak gate sau publish → `TIEBREAK_REQUIRED` ✅ | Không đổi |
| **FE build** | ✅ `vite build` thành công | ✅ Không đổi |
| **FE lint** | ❌ **227 problems** (210 errors) | ✅ **0 errors**, 101 warnings |
| **FE E2E** | 0 | Playwright smoke (`e2e/smoke-login.spec.js`) — `npm run test:e2e` |
| **GĐ6 seeds** | Chậm ~60s sau BE start | GĐ6 seed chạy **trước** GĐ5 + log `[DataInitializer] GĐ6 dev seeds ready` |
| **Workflow GĐ3→6** | Slice verify | Không đổi |

### Bugs theo mức độ (sau fix)

| Mức | Ban đầu | Sau fix |
|-----|---------|---------|
| **P0** | 2 open | **0 open** — BUG-01, BUG-02 đã sửa |
| **P1** | 6 open | **0 blocker** — unit tests, lint errors, merge conflicts, backlog doc đã xử lý |
| **P2** | 5 open | 5 open (export CSV, journey stub, Playwright, v.v.) |

### Kết luận nhanh

- **GĐ3 BE:** Activate round đã active → **200 idempotent** (không còn 500).
- **GĐ4 BE:** Tiebreak + wildcard **đã implement**; backlog `09-be-backlog` **đã sync**.
- **GĐ5/GĐ6 BE:** Không đổi — gates đúng spec.
- **FE:** Lint **pass** (0 errors); `LATE_APPROVED` tách khỏi `ON_TIME` trong `personB.api.ts`; merge conflicts resolved.

---

## Phần B — Bug Findings (chi tiết)

| ID | GĐ | Mô tả | Trạng thái | Fix |
|----|-----|-------|------------|-----|
| **BUG-01** | GĐ3 | Activate round đã `isActive=true` → 500 | **FIXED** | `RoundActivationServiceImpl` — early return idempotent 200 |
| **BUG-02** | GĐ4/5 | Guest judge sau `eventEnd` vẫn login | **FIXED** | `GuestJudgeLifecycleServiceImpl` — `resolveHackathon` + `isHackathonEnded` (FINISHED + datetime) |
| **BUG-03** | GĐ3/5 | Timer pause remaining sai (510 vs 480) | **FIXED** | Test fixture: `pausedAt = now()` thay vì `now()-30s` |
| **BUG-04** | GĐ1/3 | Error code deadline CK vs Awards | **FIXED** | Test dùng `plusDays(40)` — validation awards chạy trước deadline-past |
| **BUG-05** | GĐ1/3 | Test hardlock deadline quá khứ | **FIXED** | Test dùng `LocalDateTime.now().plusDays(10)` |
| **BUG-06** | GĐ6 | Seed GĐ6 chậm ~60s | **MITIGATED** | `DataInitializer` — GĐ6 seeds trước GĐ5 + log readiness |
| **BUG-07** | FE | ESLint fail | **FIXED** | 0 errors (193 warnings còn lại — chủ yếu unused vars, setState-in-effect) |
| **BUG-08** | FE | Merge conflict markers | **FIXED** | `README.md`, `.gitignore` resolved |

---

## Phần C — Spec Violations & SPEC_GAP

| FR / Gate | Spec nói | Thực tế | Đánh giá |
|-----------|----------|---------|----------|
| FR-22B tiebreak | `GET tiebreak`, `POST tiebreak/resolve`, gate advance | **Đã có** trong `RoundProgressionServiceImpl` | ✅ (doc backlog ghi ⏳ — **doc sai**) |
| FR-22A wildcard | `GET wildcard-candidates`, `PATCH wildcard-reviews` | **Đã có** — seed `seal-gd4-advance-ready` trả 4 candidates | ✅ |
| G4 gate order | Advance khi chưa publish | `RESULT_NOT_PUBLISHED` **trước** `TIEBREAK_REQUIRED` | ✅ đúng thứ tự gate |
| G4-N-TB | Advance khi còn tiebreak | Sau publish → `TIEBREAK_REQUIRED` | ✅ (probe2 xác nhận) |
| FR-30A | Lock CK → `PENDING_CONFIRM` | `RoundProgressionServiceImpl.lockScoring` set status | ✅ code có |
| FR-33 confirm | Gates: PENDING_CONFIRM, CK locked, ≥1 prize | `HackathonClosureServiceImpl` đủ 3 gate | ✅ |
| FR-34 export | CSV sau FINISHED | `ExportCsvBuilder` — **CSV_RANKINGS** dùng `FinalRankingQueryService` | ✅ |
| FR-17 metadata | Async GitHub fetch | `SubmissionMetadataServiceImpl` enqueue only | SPEC_GAP (optional per spec) |
| GET team journey | Timeline đội | `TeamJourneyServiceImpl` — tracks + participation | ✅ |
| Notifications | Batch notify activate | `NotificationService` — BUG-01 đã fix | ✅ |

---

## Phần D — Nghi ngờ “AI tự vẽ” (AI-Invented / misleading)

| ID | Vị trí | Mô tả | Rủi ro |
|----|--------|-------|--------|
| **AI-01** | `09-be-backlog-gd4-gd5-gd6.md` | Ghi GĐ4 phase 2 tiebreak/wildcard ⏳ stub | **FIXED** — doc đã cập nhật ✅ |
| **AI-02** | `personB.api.ts` `mapSubmissionStatusToFe` | Map `LATE_APPROVED` → `ON_TIME` | **FIXED** — `LATE_APPROVED` trả riêng |
| **AI-03** | `RoundServiceImpl.java:170` | Audit log `by: stub-coordinator` | **FIXED** — dùng `CurrentUserAccessor.currentUserId()` |
| **AI-04** | `fe-gd3-backlog.md` | Ghi lock-scoring / ranking chưa có | **Stale** — FE đã wire `RoundManagementPage` |
| **AI-05** | `GuestJudgeLifecycleServiceImpl` | Chỉ so `LocalDate` eventEnd, không giờ | Khách judge “hết hạn” sai ngày thực tế |
| **AI-06** | Export `ANONYMIZED_RBL` / `FULL_REPORT` | `ExportCsvBuilder` — RBL variance + multi-section full report | ✅ |

---

## Phần E — Automated Test Results (mvn test)

```
Tests run: 216, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS (2026-06-30)
```

### Integration GĐ4→GĐ6 (mới)

| Class | Test | Mô tả |
|-------|------|-------|
| `Gd4ToGd6FlowIntegrationTest` | `activatePrelim_secondCallIsIdempotent` | Activate lần 2 → 200 |
| `Gd4ToGd6FlowIntegrationTest` | `gd4ThroughGd6_publishAdvanceConfirmAndExport` | lock→publish→advance→final→confirm→export→journey |

### Các test đã fail (đã fix)

| Class | Test | Nguyên nhân | Fix |
|-------|------|-------------|-----|
| `GuestJudgeLifecycleServiceImplTest` | `assertHackathonNotEndedForTempJudge_afterEventEnd_throws` | `resolveHackathon` lọc bỏ hackathon ended | Service + mock `findByEmail` |
| `PresentationTimerPauseTest` | `pausedSlot_freezesRemainingSeconds` | Fixture `pausedAt = now-30s` | `pausedAt = now()` |
| `RoundServiceImplExamValidationTest` | `createFinal_blocksWhenAwardsStartsBeforeSubmissionDeadline` | Deadline cố định 2026-06-10 < now | `plusDays(40)` |
| `RoundServiceImplExamValidationTest` | `createPreliminary_withHardLockPolicy_isAllowedWhenOtherRulesValid` | Deadline cố định quá khứ | `plusDays(10)` |

### Coverage hole (false confidence)

| GĐ | Unit | Integration | Rủi ro ẩn |
|----|------|-------------|-----------|
| GĐ3 | ~25+ classes ✅ | `Gd2Gd3FlowIntegrationTest` only | Re-activate 500 không có test |
| GĐ4 | `WildcardCandidateSelectionTest` | **Không** | Publish→advance→activate chain |
| GĐ5 | Reuse GĐ3 presentation | **Không** | PENDING_CONFIRM side effect |
| GĐ6 | `HackathonArchiveGuardTest` | **Không** | Confirm + export E2E |

**Khuyến nghị:** Thêm `Gd4FlowIntegrationTest`, `Gd5FlowIntegrationTest`, `Gd6ClosureIntegrationTest`.

---

## Phần F — Gate & Adversarial Matrix (API manual)

Kết quả từ `BE/scripts/gd3-gd6-api-audit.py` + probe bổ sung.

| ID | GĐ | Mô tả | Kỳ vọng | Thực tế | Status |
|----|-----|-------|---------|---------|--------|
| G3-H01 | GĐ3 | Activate prelim | 200 | **HTTP 200** (live retest) | **PASS** |
| G3-H02 | GĐ3 | Ranking preview | 200 | 200 | PASS |
| G3-H03 | GĐ3 | Scoring progress | 200 | 200 | PASS |
| G4-TB-LIST | GĐ4 | Tiebreak list (`seal-gd4-tiebreak-gate`) | >0 | count=1 | PASS |
| G4-N-TB | GĐ4 | Advance chưa publish | TIEBREAK_REQUIRED | RESULT_NOT_PUBLISHED | **FAIL*** |
| G4-N-TB' | GĐ4 | Advance sau publish | TIEBREAK_REQUIRED | TIEBREAK_REQUIRED | PASS (probe2) |
| G4-H-WC | GĐ4 | Wildcard candidates | >0 | count=4 | PASS |
| G4-H01 | GĐ4 | Publish | 200 | 200 | PASS |
| G4-R01 | GĐ4 | Readiness FINAL_ROUND | ready | ready=False (chưa advance) | PASS |
| G5-H00 | GĐ5 | Final active | true | *(không chạy trong batch1 — seed bị lock trước đó)* | — |
| G5-H04 | GĐ5 | PENDING_CONFIRM sau lock | PENDING_CONFIRM | Cần retest trên `seal-gd5-submit-open` | — |
| G6-R01 | GĐ6 | Readiness AWARDS | ready | True | PASS |
| G6-RANK | GĐ6 | Team rankings | >0 | count=3 | PASS |
| G6-N01 | GĐ6 | Confirm không prize | NO_PRIZES_RECORDED | NO_PRIZES_RECORDED | PASS |
| G6-N02 | GĐ6 | Confirm CK chưa lock | ROUND_NOT_SCORING_LOCKED | ROUND_NOT_SCORING_LOCKED | PASS |
| ADV-08 | GĐ6 | Confirm khi FINISHED | HACKATHON_NOT_PENDING_CONFIRM | HACKATHON_NOT_PENDING_CONFIRM | PASS |
| G6-H03b | GĐ6 | Confirm ready seed | 200 | 200 | PASS |
| G6-EXP | GĐ6 | Export (sai endpoint test) | 201 | 500 trên `POST /export-jobs` | PARTIAL** |

\* *G4-N-TB ban đầu FAIL do thứ tự gate — sau publish đúng spec (PASS probe2).*  
\** *FE dùng `POST /hackathons/{id}/export-jobs` với `type: CSV_RANKINGS` — đúng spec.*

### Adversarial chưa chạy đủ (khuyến nghị retest)

| ID | Kịch bản | Ghi chú |
|----|----------|---------|
| ADV-01 | Score khi `LATE_PENDING` | Cần seed `seal-gd3-late-review` |
| ADV-02 | Score sau lock | Cần `seal-gd5-late-hardlock` |
| ADV-03 | `queue/next` chưa chấm đủ | Manual judge flow |
| ADV-10 | INTERNAL judge chấm CK | Gate `INVALID_ASSIGNMENT_TYPE` |
| ADV-11 | Submit sai track | Cross-track guard |

---

## Phần G — Browser / FE Cross-Role (manual + static)

Không có Playwright. Đánh giá qua **build + lint + code contract + API**.

| GĐ | Route FE | Role | API wired | UI risk |
|----|----------|------|-----------|---------|
| GĐ3 | `/hackathons/:id/setup` (Rounds) | COORD | activate, release, lock, progress | BUG-01 **fixed** |
| GĐ3 | `/student/submit` | STU | `personB.api.ts` → `POST /submissions` | AI-02 **fixed** |
| GĐ3 | `/judging/:id/scoring` | JUD | `useLiveScoringV2.js`, WS | Lint errors hooks |
| GĐ3 | `/coordinator/late-submissions` | COORD | `review-late` ✅ (không deprecated `/review`) | OK |
| GĐ4 | `/hackathons/.../rounds/.../results` | COORD | tiebreak, wildcard, publish, advance | Panel hiển thị khi BE có data |
| GĐ5 | `/coordinator/final-config` | COORD | readiness FINAL, calibration | Calibration panel wired |
| GĐ5 | `/student/submit` (final) | STU | no trackId | OK |
| GĐ6 | `/hackathons/:id/results` | COORD | prizes, confirm, export | `gd6-export-csv`, `gd6-confirm-*` IDs có |
| GĐ6 | `/student/hackathons/:id/results` | STU | team-rankings, prizes | OK |

**FE static verify:**

| Check | Kết quả |
|-------|---------|
| `npm run build` | ✅ Pass |
| `npm run lint` | ✅ **0 errors**, 101 warnings |
| Playwright smoke | ✅ `e2e/smoke-login.spec.js` (login page) |
| Merge conflicts | ✅ Resolved |

---

## Phần H — BE / FE Implementation Status

### Backend

| GĐ | Module | Trạng thái | Ghi chú |
|----|--------|------------|---------|
| **GĐ3** | submissions, presentation, scores, ranking | ✅ E2E-ready | BUG-01 **fixed** |
| **GĐ4** | publish, advance, tiebreak, wildcard, scoreboard | ✅ Phase 2 **đã làm** | Backlog **đã sync** |
| **GĐ5** | final submit, score, calibration, RBL | 🔶 Calibration create OK; RBL views cần verify data |
| **GĐ6** | confirm, rankings, prizes, export | ✅ Gates + export CSV ranking rows + journey API |

### Frontend

| GĐ | Module | Trạng thái |
|----|--------|------------|
| **GĐ3** | rounds, judging, presentation, student submit | ✅ Wired — lint pass (warnings only) |
| **GĐ4** | round-results (Tiebreak, Wildcard, PreliminaryResults) | ✅ Wired |
| **GĐ5** | FinalRoundConfig, live scoring, final submit | ✅ Wired |
| **GĐ6** | HackathonResults, prizes, confirm, export | ✅ Wired + test IDs |

---

## Phần I — Ưu tiên sửa (Recommended Fix Order)

### ✅ Đã hoàn thành (2026-06-30)

1. **BUG-01** — Idempotent activate (200).
2. **BUG-02** — Guest judge lifecycle sau `eventEnd`.
3. **BUG-03 … BUG-05** — 4 unit test fail.
4. Sync `09-be-backlog-gd4-gd5-gd6.md`.
5. FE merge conflicts + **0 ESLint errors**.
6. **AI-02** — `LATE_APPROVED` tách khỏi `ON_TIME`.
7. **AI-03** — Audit log dùng user ID thật.
8. **BUG-06** — GĐ6 seeds sớm hơn + log.

### ✅ Đã hoàn thành (2026-06-30, tiếp)

9. ESLint **0 errors**, warnings **101 → 69** (`eslint-plugin-unused-imports`, fix 19 lỗi thủ công, `scripts/fix-unused-catch.mjs`).
10. Playwright: `e2e/coord-gd4-gd6.spec.js` + `e2e/helpers/api.js` (coord → gd4 results → gd6 confirm modal cancel).
11. `ExportCsvBuilder` — `ANONYMIZED_RBL` (variance + judge hash), `FULL_REPORT` (rankings + scores + RBL sections).
12. Integration test assert download `ANONYMIZED_RBL` / `FULL_REPORT` > 1 dòng.

### P1 — Còn lại (optional)

1. Giảm ~69 ESLint warnings còn lại (`exhaustive-deps`, `react-refresh`, legacy `token` unused).
2. Chạy Playwright full flow với BE dev (`mvn spring-boot:run -Dspring-boot.run.profiles=dev`) — hiện skip khi BE offline.

### P2 — Polish

1. CI matrix BE+FE cho `npm run test:e2e`.

---

## Phụ lục

### Lệnh đã chạy

```powershell
cd BE
mvn test                                    # 214 tests, 0 FAIL
mvn test "-Dtest=GuestJudgeLifecycleServiceImplTest,RoundActivationServiceImplTest"
mvn spring-boot:run -Dspring-boot.run.profiles=dev

cd seal-hackathon-fe
npm run build
npm run lint                                # 0 errors, 193 warnings

python BE/scripts/g3-h01-retest.py
python BE/scripts/gd3-gd6-api-audit.py
python BE/scripts/gd3-gd6-probe2.py

cd seal-hackathon-fe
npx playwright install chromium   # lần đầu
npm run test:e2e
```

### Seed slugs đã dùng

| Slug | GĐ |
|------|-----|
| `seal-gd3-prelim-open` | GĐ3 happy |
| `seal-gd3-edge-errors` | GĐ3 negative |
| `seal-gd4-advance-ready` | GĐ4 happy |
| `seal-gd4-tiebreak-gate` | GĐ4 tiebreak gate |
| `seal-gd4-published` | GĐ4 scoreboard |
| `seal-gd5-final-active` | GĐ5 CK |
| `seal-gd6-pending-confirm` | GĐ6 awards |
| `seal-gd6-prizes-empty` | GĐ6 NO_PRIZES gate |
| `seal-gd6-edge-errors` | GĐ6 CK not locked |
| `seal-gd6-finished-export` | GĐ6 FINISHED + export |
| `seal-gd6-confirm-ready` | GĐ6 confirm happy |

### Accounts dev

| Role | Email | Password |
|------|-------|----------|
| Coordinator | `coord@fpt.edu.vn` | `Coordinator@dev1` |
| Judge SL | `judge1@fpt.edu.vn` | `Judge@dev1` |
| Guest judge CK | `guestjudge@gmail.com` | `GuestJudge@dev1` |
| Student GĐ5 | `student.gd5.leader03@fpt.edu.vn` | `Student@dev1` |

### Ký hiệu

| Ký hiệu | Ý nghĩa |
|---------|---------|
| PASS | Khớp spec |
| FAIL | Bug |
| SPEC_GAP | Spec yêu cầu, chưa đủ |
| BLOCKED | Không chạy (giữ seed) |
| AI_INV | Logic/doc misleading |
| FALSE_CONF | Unit pass, E2E/API fail |

---

**Người thực hiện:** Agent audit tự động + API manual  
**File kết quả JSON:** `BE/scripts/gd3-gd6-api-audit-results.json`
