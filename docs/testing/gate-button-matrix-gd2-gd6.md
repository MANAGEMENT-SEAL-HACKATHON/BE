# Ma trận nút Coordinator theo lifecycle gate (GĐ2 - GĐ6)

> Canonical matrix — "nút nào hiện lúc nào" cho Coordinator/SUPERADMIN. Dựa trên gate sẵn có
> ([roundLifecycleGates.js](../../../seal-hackathon-fe/src/features/rounds/utils/roundLifecycleGates.js),
> `canActivateRound.js`, `useRoundResults.js`, `useHackathonResults.js`). **Không** đổi thứ tự nghiệp vụ.
>
> GĐ2 `PENDING → lottery → activate` xem plan **Gate lottery pending teams** (không lặp ở đây).

## Nguyên tắc chung

- **Progressive disclosure:** nút chỉ *xuất hiện* khi tới đúng bước lifecycle; disabled chỉ khi
  đã hợp ngữ cảnh nhưng còn thiếu điều kiện phụ (kèm tooltip đúng lý do).
- **Không disabled gây hiểu nhầm:** nếu lý do "chưa tới bước" thì **ẩn hẳn**, không hiện disabled
  với tooltip sai (vd `Close early` trước khi phát đề).
- **Tách SL vs CK:** chỉ SL (`is_final=false`) có state «chưa release» + nút `Phát đề`. CK
  (`is_final=true`) activate = release ngay (`problemReleasedAt` stamp tức thì, reuse đề track SL)
  → **không** có state «chưa release», **không** có nút `Phát đề`.

## Gate function tham chiếu (đã xác nhận từ code)

| Gate | Điều kiện |
|------|-----------|
| `canReleaseProblem` | `active && now>=examAt && !problemReleasedAt` |
| `canCloseEarly` | `active && problemReleasedAt && now>=examAt && !closedEarly && !locked` |
| `isSubmissionClosed` | `closedEarly || now>=submissionDeadline` |
| `canOpenPresentationQueue` / `canShuffleQueue` | `submissionClosed && !locked` |
| `canLockScoring` | `submissionClosed && shuffled && presentationsComplete && !locked` |
| `canActivateRound` | inactive; mọi track không huỷ có đội; criteria/judge > 0 khi biết |

## GĐ3 (Sơ loại) — action column Round Management

| State vòng SL | Nút hiện | Ẩn hẳn |
|---------------|----------|--------|
| Inactive, chưa ended | Ranking, Edit, **Play activate**, People, Delete | Close early, Queue, Lock, Trophy |
| Active, **chưa tới examAt** (Waiting) | Ranking, People, Lock (disabled) | **Phát đề** (ẩn + countdown), Close early, Queue, Trophy |
| Active, **đã tới examAt, chưa release** | Ranking, **Phát đề**, People, Lock (disabled) | **Close early**, Queue, Trophy |
| Active, đã release, chưa đóng nộp | Ranking, **Close early** (disabled tới examAt, tooltip đúng), People, Lock (disabled) | Phát đề, Queue, Trophy |
| Submission closed, chưa shuffle | Ranking, **Open queue**, **Điểm TP**, People, Lock (disabled) | Phát đề, Close early, Trophy |
| Shuffled + presentations complete | … + **Lock enabled** | |
| `scoringLocked` | Ranking, **Trophy → results (GĐ4)**, label "Đã đóng sổ" | Release, Close early, Queue, Lock, People |
| `scoringLocked` + SUPERADMIN | … + **Unlock scoring** (modal lý do) | |

## GĐ5 (Chung kết) — action column + Final Config

| State vòng CK | Nút hiện | Ẩn hẳn |
|---------------|----------|--------|
| Inactive | **Kích hoạt CK ở Final Config** (readiness API) — Play trên Round Mgmt **ẩn/disable khi `is_final`** | Phát đề, Close early |
| Active sau activate (= đã release) | Ranking, **Close early** (disabled tới examAt), People, Lock (disabled) | **Phát đề** (không tồn tại cho CK), **state "chưa release"** |
| Submission closed | Ranking, Open queue, **Điểm TP** (khi `closed`), People, Lock (disabled) | Phát đề |
| Shuffled + complete | … + Lock enabled | |
| `scoringLocked` | Ranking, **Trophy → /hackathons/{id}/results (GĐ6)**, label "Đã đóng sổ" | Queue, Lock, People |
| `scoringLocked` + SUPERADMIN | … + **Unlock scoring** | |

## GĐ4 (Công bố & chuyển vòng) — Preliminary Results

| Flag | Điều kiện | Nút |
|------|-----------|-----|
| `canPublish` | `locked && !published && ranking ok` | **Công bố kết quả** (ẩn sau khi published) |
| `canAdvance` | `locked && published && !advanced && wildcardReady && !unresolvedTiebreak` | **Chốt chuyển vòng** |
| `showWildcardTab` | `roundEnabled && slots>0` | Tab Wild Card (Plan C: Xác nhận + Override) |

## GĐ6 (Trao giải & kết thúc) — Hackathon Results

| Flag | Điều kiện | Nút |
|------|-----------|-----|
| `canAwardPrize` / `canRevokePrize` | `status === PENDING_CONFIRM` | Trao giải / Thu hồi |
| `canConfirm` | `PENDING_CONFIRM && prizes>0 && awardsReady` | **Chốt sổ & Công bố** |
| `canExport` | `status === FINISHED` | **Xuất CSV** |

## Dual-CTA cross-page (phải đồng bộ gate + label + modal + API)

| CTA | Chốt |
|-----|------|
| Activate CK | **CTA duy nhất ở Final Config** (readiness API). Round Mgmt Play ẩn/disable khi `is_final`. |
| Close early | Icon Round Mgmt **và** nút Submission panel → cùng `canCloseEarly`, cùng label "Kết thúc thời gian thi sớm", cùng modal, cùng API. |
| Điểm thành phần | Final Config chỉ hiện khi `closed` (giống Round Mgmt), không chỉ `finalRoundActive`. |
| Open queue | Dual entry (Round Mgmt + Final Config) nhưng cùng `canOpenPresentationQueue`. |

## Unlock scoring (SUPERADMIN)

- BE: `PATCH /api/v1/rounds/{id}/unlock-scoring` + `@SuperAdminOnly` + `reason` bắt buộc + audit.
- FE: nút chỉ hiện khi `scoringLocked && role === 'SUPERADMIN'`; modal nhập lý do bắt buộc.
- Coordinator: **không** có nút; gọi API trực tiếp → 403.
