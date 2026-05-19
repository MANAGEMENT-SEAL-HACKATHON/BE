#!/usr/bin/env python3
"""Build docs/workflow/mf01-gd1-doi-chieu.md from mf01.md + implementation notes."""

from __future__ import annotations

import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MF01 = ROOT / "docs/workflow/mf01.md"
OUT = ROOT / "docs/workflow/mf01-gd1-doi-chieu.md"

HEADER = """# MF-01 — Tài liệu đối chiếu Giai đoạn 1 (Chuẩn bị sự kiện)

**Dự án:** SEAL Hackathon Management System — Backend (Spring Boot)  
**Phiên bản tài liệu:** 2.0 · **MF-01 spec:** v3.0 · **DB schema:** v3.0 (MySQL 8)  
**Đồng bộ nội dung nghiệp vụ từ:** [mf01.md](mf01.md) (2026-05-18)  
**Mục đích:** Business rules, functional requirements, main flow và API **đầy đủ** (cùng cấu trúc mf01.md), kèm lớp **Implementation (SEAL BE)** để đối chiếu dự án khác.

**Quy tắc ưu tiên:** `schema-v3.0-mysql.md` > `mf01.md` > `workflow.md` (đoạn cũ).

| Tài liệu | Vai trò |
|----------|---------|
| [mf01.md](mf01.md) | Spec normative gốc |
| [schema-v3.0-mysql.md](../db/schema-v3.0-mysql.md) | DDL, trigger |
| [workflow.md](workflow.md) v5.0 | Luồng 6 giai đoạn |

> **Cách đọc:** Các mục `### X.Y` … `### X.5` là nghiệp vụ chuẩn (từ mf01). Mục **`### X.6+ Implementation`** mô tả code SEAL BE hiện tại.

---

"""

IMPL_FR01 = """
### 2.6 Implementation (SEAL BE — codebase)

| Thành phần | Chi tiết |
|------------|----------|
| Controller | `HackathonController` — `/api/v1/hackathons` |
| Service | `HackathonServiceImpl` |
| Auth | `StubCurrentUserAccessor` — coordinator **id=1** (dev); `@CoordinatorOnly` |
| Create | POST luôn `status=DRAFT`; không nhận status từ client |
| Errors | `HACKATHON_DUPLICATE` (409), `HACKATHON_DATE_RANGE` (422), `HACKATHON_HAS_CHILDREN` (409) |
"""

IMPL_BLOCKS = {
    "## 3. FR-02 — Tạo / Cấu hình Round (Vòng thi)": """
### 3.6 Implementation (SEAL BE — codebase)

| Thành phần | Chi tiết |
|------------|----------|
| Controller | `RoundController` — `POST/GET /api/v1/hackathons/{hackathonId}/rounds` |
| Service | `RoundServiceImpl` — `submissionDeadline > NOW()` |
| Legacy | `POST/GET /api/v1/tracks/{trackId}/rounds` delegate v3 |
| Errors | `ROUND_DEADLINE_INVALID`, `ROUND_HAS_SUBMISSIONS` |
""",
    "## 4. FR-03 — Tạo / Cấu hình Track (Bảng đấu trong Round)": """
### 4.7 Implementation (SEAL BE — codebase)

| Thành phần | Chi tiết |
|------------|----------|
| Controller | `TrackController` — `POST /api/v1/rounds/{roundId}/tracks` |
| Service | `TrackServiceImpl` — topic sau KICKOFF khi PUT |
| Legacy | `POST /api/v1/hackathons/{hackathonId}/tracks` |
| Errors | `TRACK_HAS_TEAMS`, `DESIGN_VIOLATION` (DB) |
""",
    "## 5. FR-04 — Thiết lập Criteria (Tiêu chí chấm điểm)": """
### 5.7 Implementation (SEAL BE — codebase)

| Thành phần | Chi tiết |
|------------|----------|
| Controller | `CriteriaController` — track + round paths; batch, clone, weight-summary |
| Service | `CriteriaService`, `WeightSummaryService` |
| Clone | `sourceTrackId` trong body — không `GET /criteria?track_id=` |
| Errors | `CRITERIA_HAS_SCORES`, `TRACK_CRITERIA_WEIGHT`, `FINAL_CRITERIA_WEIGHT` |
""",
    "## 6. FR-05 — Quản lý nhân sự giải đấu": """
### 6.7 Implementation (SEAL BE — codebase)

| Thành phần | Chi tiết |
|------------|----------|
| 5a | `TempJudgeController`, `InvitationController` resend |
| 5b | `MentorAssignmentController` |
| 5c | `JudgeAssignmentController` — GĐ1: track + NORMAL only |
| Conflict | 422 `CONFLICT_SAME_TRACK`, `FINAL_JUDGE_CANNOT_BE_MENTOR` (DB+App) |
""",
    "## 7. FR-06 — Lên lịch sự kiện": """
### 7.6 Implementation (SEAL BE — codebase)

| Thành phần | Chi tiết |
|------------|----------|
| Controller | `EventController` |
| Validator | `EventScheduleValidatorImpl` — L1+L2 block, L3 warn |
| Gate G5 | Re-validate KICKOFF trong `HackathonReadinessServiceImpl` |
| Side effect | REMINDER notification sync trong transaction |
""",
    "## 8. FR-07 — Chuyển trạng thái Hackathon (State Machine)": """
### 8.6 Implementation (SEAL BE — codebase)

| Thành phần | Chi tiết |
|------------|----------|
| Readiness | `HackathonReadinessServiceImpl` — G1–G5; mentor thiếu = warning |
| Status | `HackathonStatusServiceImpl` — one-way state machine |
| API | `GET .../readiness`, `PATCH .../status` (`status` / `targetStatus`) |
| Fail | 422 `READINESS_NOT_PASSED` + `blockers[]` |
""",
    "## 9. FR-07B — Safety Net: Validate weight khi kích hoạt Round": """
### 9.3 Implementation (SEAL BE — codebase)

| Thành phần | Chi tiết |
|------------|----------|
| Controller | `RoundActivationController` — `PATCH /api/v1/rounds/{id}/activate` |
| Service | `RoundActivationServiceImpl` — weight + conflict; runtime GĐ3 |
""",
}

APPENDIX = """
---

## 15. Checklist đối chiếu (cho team bạn)

| # | Hạng mục | SEAL BE | Dự án bạn | Ghi chú |
|---|----------|---------|-----------|---------|
| 1 | Kiến trúc Hackathon → Round → Track | Có | | |
| 2 | Round FINAL không có Track | Có | | |
| 3 | Criteria XOR | Có | | |
| 4 | Judge assign XOR | Có | | |
| 5–8 | Gate G1–G5 | Có | | |
| 9 | API readiness | Có | | |
| 10 | PATCH ONGOING + blockers | Có | | |
| 11 | Mentor↔Judge BLOCK | Có | | |
| 12–25 | (xem bản 1.0 checklist đầy đủ trong git history) | | | |

---

## Phụ lục A — Seed dev (`dev` profile)

| Slug | Mục đích |
|------|----------|
| `seal-gd1-incomplete` | DRAFT, readiness fail |
| `seal-gd1-ready` | DRAFT, đủ G1–G5 |
| `seal-spring-2026` | ONGOING, dataset đầy đủ |

**Email:** `coord@fpt.edu.vn`, `judge1@fpt.edu.vn`, `mentor@fpt.edu.vn`, `guestjudge@gmail.com`

---

## Phụ lục B — Cập nhật tài liệu

Normative: [mf01.md](mf01.md). Bản đối chiếu: re-sync khi đổi spec hoặc code.

---

*SEAL Hackathon BE — MF-01 đối chiếu v3.0 — FPT University HCMC*
"""

FR_ORDER = [
    "## 2. FR-01 — Tạo Hackathon mới",
    "## 3. FR-02 — Tạo / Cấu hình Round (Vòng thi)",
    "## 4. FR-03 — Tạo / Cấu hình Track (Bảng đấu trong Round)",
    "## 5. FR-04 — Thiết lập Criteria (Tiêu chí chấm điểm)",
    "## 6. FR-05 — Quản lý nhân sự giải đấu",
    "## 7. FR-06 — Lên lịch sự kiện",
    "## 8. FR-07 — Chuyển trạng thái Hackathon (State Machine)",
    "## 9. FR-07B — Safety Net: Validate weight khi kích hoạt Round",
    "## 10. API Specification (MF-01) — Target v3.0",
]


def transform_line(line: str) -> str:
    s = line.rstrip("\n")
    if s.startswith("## 0."):
        return s
    for pat, repl in [
        (r"^(\d+)\. (FR-\d+B? — .+)$", r"## \1. \2"),
        (r"^(\d+)\. (Tổng quan .+)$", r"## \1. \2"),
        (r"^(\d+)\. (API Specification .+)$", r"## \1. \2"),
        (r"^(\d+)\. (Điểm thiết kế .+)$", r"## \1. \2"),
        (r"^(\d+)\. (Pending Items .+)$", r"## \1. \2"),
        (r"^(\d+)\. (Trigger Summary .+)$", r"## \1. \2"),
        (r"^(\d+)\.(\d+) (.+)$", r"### \1.\2 \3"),
    ]:
        m = re.match(pat, s)
        if m:
            return m.expand(repl)
    if s.startswith("Changelog v2.2"):
        return "## Changelog v2.2 → v3.0"
    return s


def main() -> None:
    lines = MF01.read_text(encoding="utf-8").splitlines()
    start = next(i for i, ln in enumerate(lines) if ln.startswith("## 0."))

    body: list[str] = []
    for idx in range(start, len(lines)):
        ln = lines[idx]
        if ln.startswith("SEAL Hackathon Management System — MF-01 DB Analysis"):
            break
        body.append(transform_line(ln))

    text = "\n".join(body)

    # §0 audit: add Implementation column (replace full rows)
    audit_rows = [
        ("FR-01 Hackathon", "Bước 1", "hackathons", "§2", "JSON (không JSONB); `updated_at` MySQL", "`HackathonServiceImpl`"),
        ("FR-02 Round", "Bước 2", "BC-01 rounds", "§3", "API target: `POST .../hackathons/{id}/rounds`", "`RoundServiceImpl`"),
        ("FR-03 Track", "Bước 3", "BC-02 tracks", "§4", "API target: `POST .../rounds/{id}/tracks`; `assigned_group` ở GĐ2", "`TrackServiceImpl`"),
        ("FR-04 Criteria", "Bước 4", "BC-03 XOR", "§5", "2 nhánh API track / final round", "`CriteriaService`"),
        ("FR-05 Nhân sự", "Bước 5", "BC-07, triggers", "§6", "Conflict BLOCK ở DB; warn chỉ weight/events", "Temp/Mentor/Judge services"),
        ("FR-06 Events", "Bước 6", "BC-09", "§7", "REMINDER sync trong transaction (hiện tại)", "`EventScheduleValidatorImpl`"),
        ("FR-07 Status", "Bước 7", "Gate G1–G5", "§8", "Thêm `GET .../readiness` §10", "`HackathonReadinessServiceImpl`"),
        ("FR-07B Activate", "GĐ3 ref", "rounds.is_active", "§9", "Phạm vi doc GĐ1; runtime GĐ3", "`RoundActivationServiceImpl`"),
    ]
    text = text.replace(
        "| FR | Workflow GĐ1 | DB v3.0 | mf01 § | Trạng thái | Ghi chú chỉnh |",
        "| FR | Workflow GĐ1 | DB v3.0 | mf01 § | Trạng thái | Ghi chú chỉnh | Implementation (SEAL BE) |",
    )
    text = text.replace(
        "|----|--------------|---------|--------|------------|---------------|",
        "|----|--------------|---------|--------|------------|---------------|---------------------------|",
        1,
    )
    for fr, wf, db, sec, note, impl in audit_rows:
        old = f"| {fr} | {wf} | {db} | {sec} | **PASS** | {note} |"
        new = f"| {fr} | {wf} | {db} | {sec} | **PASS** | {note} | {impl} |"
        text = text.replace(old, new, 1)

    # Insert implementation sections before next FR
    impl_for_fr = {FR_ORDER[0]: IMPL_FR01}
    for hdr in FR_ORDER[1:]:
        if hdr in IMPL_BLOCKS:
            impl_for_fr[hdr] = IMPL_BLOCKS[hdr]

    for i, hdr in enumerate(FR_ORDER):
        if hdr not in text:
            continue
        next_hdr = FR_ORDER[i + 1] if i + 1 < len(FR_ORDER) else "## 14. Appendix"
        pos = text.find(f"\n{next_hdr}")
        if pos == -1:
            pos = text.find(next_hdr)
        if pos == -1:
            continue
        segment = text[:pos]
        if hdr in impl_for_fr and "Implementation (SEAL BE" not in segment.split(hdr, 1)[-1]:
            text = segment.rstrip() + "\n\n" + impl_for_fr[hdr].strip() + "\n\n---\n\n" + text[pos:].lstrip("\n")

    # §10.2: add Legacy column note at end of table header row
    text = text.replace(
        "| Method | Path | FR | Ghi chú |",
        "| Method | Path | FR | Ghi chú | Legacy |",
        1,
    )
    legacy_map = {
        "POST | `/api/v1/tracks/{trackId}/rounds`": "Yes",
        "GET | `/api/v1/tracks/{trackId}/rounds`": "Yes",
        "POST | `/api/v1/hackathons/{hackathonId}/tracks`": "Yes",
    }
    for path_fragment, leg in legacy_map.items():
        text = text.replace(
            f"| {path_fragment} | FR-02 |",
            f"| {path_fragment} | FR-02 | Delegate v3 | {leg} |",
        )
        text = text.replace(
            f"| {path_fragment} | FR-03 |",
            f"| {path_fragment} | FR-03 | Resolve prelim | {leg} |",
        )

    # Append §15 + appendices; remove old footer
    text = re.sub(r"\nSEAL Hackathon Management System — MF-01 DB Analysis.*", "", text, flags=re.DOTALL)
    if "## 15. Checklist" not in text:
        text = text.rstrip() + "\n" + APPENDIX

    OUT.write_text(HEADER + text.strip() + "\n", encoding="utf-8")
    print(f"Wrote {OUT} ({len((HEADER + text).splitlines())} lines)")


if __name__ == "__main__":
    main()
