# MF-01 GĐ1 — Timeline & Events (SEAL FPT)

**Phạm vi:** Hackathon **1–2 ngày** (Spring 2026, Fall 2025). Local / GĐ1 — không mở rộng hackathon nhiều ngày.

**Code:** `EventScheduleValidatorImpl`, `HackathonTimelineServiceImpl`, `HackathonReadinessServiceImpl`, `EventServiceImpl`, `Gd1DataSeeder.computeDates`.

---

## 0. Giải thích thuật ngữ (đọc trước §3–§4)

Trong BE có **hai lớp** dữ liệu thời gian: **Event** (lịch công khai) và **Round** (vòng thi). Rule timeline **khóa cho khớp nhau**.

### 0.1 Event `type` — không phải “slide thuyết trình”

| `type` (enum DB) | Hiểu trong SEAL FPT | Ví dụ Spring 2026 |
|------------------|---------------------|-------------------|
| `WORKSHOP` | Buổi workshop trước mùa | 9/4 20:00–21:30 |
| `KICKOFF` | **Khai mạc** (bốc thăm track, họp đội) | 11/4 14:00–17:00 |
| **`PRESENTATION`** | **Ngày thi** — khung giờ thi / thuyết trình **sơ loại** (một event trên lịch) | 12/4 06:00–17:00 |
| `AWARDS` | **Lễ trao giải** (buổi chung kết + trao giải) | 12/4 17:30–19:00 |
| `OTHER` | Sự kiện phụ (họp mentor, …) — không thay milestone | Tùy coordinator |

> **`PRESENTATION`** = tên kỹ thuật trong code/API. Trên UI nên hiển thị **“Ngày thi”** cho coordinator. **Không** bắt buộc là buổi chỉ pitch PowerPoint.

### 0.2 Round — vòng thi (FR-03)

| Khái niệm | Trong DB/API | Ghi chú |
|-----------|--------------|---------|
| **Round sơ loại** | `round.isFinal = false` (PRELIMINARY / SEMIFINAL) | Thường 1 round; có **track** con |
| **CK (Chung kết)** | `round.isFinal = true` | **Đúng 1** round CK mỗi hackathon |
| **`examAt`** | `round.examAt` (bắt buộc) | **Một mốc thời gian** — “khi nào thi / chấm vòng này” |

**CK** = **Chung kết** (tiếng Việt), **không** phải `EventType`. CK là **Round**, không tạo bằng `POST .../events`.

### 0.3 `examAt` gắn với Event nào?

```text
Event (lịch)                    Round (vòng thi)
────────────                    ────────────────
WORKSHOP
KICKOFF
PRESENTATION  ◄──────────────  Round sơ loại.examAt  ∈ [startsAt, endsAt] của event PRESENTATION
AWARDS        ◄──────────────  Round CK.examAt       ∈ [startsAt, endsAt] của event AWARDS
```

- **Có round sơ loại** → readiness bắt **phải có** event `PRESENTATION` → `EVENT_PRESENTATION_MISSING` nếu thiếu.
- **Có round CK** → readiness bắt **phải có** event `AWARDS` → `EVENT_AWARDS_MISSING` nếu thiếu.
- **`examAt` lệch khung** (có event nhưng giờ sai) → `ROUND_EXAM_OUTSIDE_PRESENTATION` hoặc `ROUND_EXAM_OUTSIDE_AWARDS`.

Mọi vòng: `examAt` phải **sau** `KICKOFF.endsAt` → `ROUND_EXAM_BEFORE_KICKOFF`.

### 0.4 Ví dụ đủ 4 event + 2 round (Spring 2026)

| Thứ tự | Event | Round |
|--------|-------|-------|
| 1 | WORKSHOP 9/4 | — |
| 2 | KICKOFF 11/4 | — |
| 3 | PRESENTATION 12/4 06:00–17:00 | Sơ loại `examAt` = 12/4 **08:00** |
| 4 | AWARDS 12/4 17:30–19:00 | CK `examAt` = 12/4 **17:30** (trong buổi lễ) |

Spring: **hai event cùng ngày 12/4** — PRESENTATION **kết thúc** (17:00) trước AWARDS **bắt đầu** (17:30).

---

## 1. Lịch tham chiếu (PDF)

| Giai đoạn | Fall 2025 | Spring 2026 |
|-----------|-----------|-------------|
| WORKSHOP | 29/10 19:30–21:30 | 9/4 20:00–21:30 |
| KICKOFF | 1/11 14:00–17:00 | 11/4 14:00–17:00 |
| PRESENTATION (ngày thi) | 2/11 06:00–21:00 | 12/4 06:00–17:00 |
| AWARDS | 2/11 ~17:00–18:00 | 12/4 17:30–19:00 |

**Quy ước `hackathons`:**

| Field | Ý nghĩa |
|-------|---------|
| `event_start` | Ngày **khai mạc** |
| `event_end` | Ngày **thi / trao giải** (Spring: `event_start + 1` ngày) |

WORKSHOP **được** đặt trước `event_start`. PRESENTATION phải **kết thúc** trước AWARDS **bắt đầu** (Spring: 17:00 → 17:30).

---

## 2. Validate event — 3 lớp

| Lớp | Quy tắc | Code |
|-----|---------|------|
| 1a | WORKSHOP: `startsAt >= registrationStart` | `EVENT_OUT_OF_HACKATHON` |
| 1b | KO / PRESENTATION / AWARDS: `startsAt >= eventStart`; `effectiveEnd <= eventEnd + 1d` | `EVENT_OUT_OF_HACKATHON` |
| 2 | Tối đa **1** milestone / type | `EVENT_MILESTONE_DUPLICATE` |
| 2b | OTHER ↔ milestone không chồng giờ (hai chiều) | `EVENT_CONFLICTS_WITH_MILESTONE` |
| 3 | WS → KO → PRESENTATION → AWARDS (`endsAt` trước `startsAt` giai đoạn sau) | `EVENT_ORDER_VIOLATION`, `EVENT_END_REQUIRED` |
| 3d | Gợi ý KICKOFF trong `[eventStart, eventStart+1d]` | WARN `EVENT_ORDER_INVALID` |

Milestone **bắt buộc `endsAt`**. Cần `location` hoặc `meetUrl`.

---

## 3. `round.examAt` (FR-03)

*(Thuật ngữ: §0 — PRESENTATION = ngày thi; CK = round `isFinal=true`.)*

| Vòng | Quy tắc | Code |
|------|---------|------|
| Mọi vòng | Sau `KICKOFF.endsAt` | `ROUND_EXAM_BEFORE_KICKOFF` |
| Sơ loại | Trong `[PRESENTATION.start, PRESENTATION.end]` | `ROUND_EXAM_OUTSIDE_PRESENTATION` |
| Chung kết (CK) | Trong `[AWARDS.start, AWARDS.end]` (inclusive) | `ROUND_EXAM_OUTSIDE_AWARDS` |

- Có **round sơ loại** → phải có event **`PRESENTATION`** (`EVENT_PRESENTATION_MISSING`).
- Có **round CK** → phải có event **`AWARDS`** (`EVENT_AWARDS_MISSING`).
- **PUT/DELETE** milestone → revalidate mọi round (`HackathonTimelineService`).

---

## 4. Readiness ONGOING (gate sự kiện)

*(Ý “có round X → bắt event Y”: §0.3.)*

| Điều kiện | Code |
|-----------|------|
| ≥1 KICKOFF hợp lệ | `EVENT_KICKOFF_MISSING` |
| Có **round sơ loại** → phải có event **PRESENTATION** (ngày thi) | `EVENT_PRESENTATION_MISSING` (một lần) |
| Có **round CK** → phải có event **AWARDS** (lễ trao giải) | `EVENT_AWARDS_MISSING` (một lần) |
| Mọi milestone đã tạo pass validator 3 lớp | `EVENT_*` / `EVENT_ORDER_*` |
| `examAt` lệch khung (không trùng blocker thiếu event ở trên) | `ROUND_EXAM_*` |

---

## 5. API & JSON mẫu (Spring 2026)

**Thứ tự POST** (block nếu sai thứ tự):

1. WORKSHOP → 2. KICKOFF → 3. PRESENTATION → 4. AWARDS  

`POST /api/v1/hackathons/{hackathonId}/events`

```json
{ "title": "Workshop RAG", "type": "WORKSHOP", "location": "Online (Teams)",
  "startsAt": "2026-04-09T20:00:00", "endsAt": "2026-04-09T21:30:00", "isPublic": true }
```

```json
{ "title": "Khai mạc", "type": "KICKOFF", "location": "Hội trường",
  "startsAt": "2026-04-11T14:00:00", "endsAt": "2026-04-11T17:00:00", "isPublic": true }
```

```json
{ "title": "Ngày thi Sơ loại", "type": "PRESENTATION", "location": "Hội trường",
  "startsAt": "2026-04-12T06:00:00", "endsAt": "2026-04-12T17:00:00", "isPublic": true }
```

```json
{ "title": "Lễ trao giải", "type": "AWARDS", "location": "Hội trường",
  "startsAt": "2026-04-12T17:30:00", "endsAt": "2026-04-12T19:00:00", "isPublic": true }
```

**Gợi ý `examAt`:** sơ loại `2026-04-12T08:00:00`; chung kết `2026-04-12T17:30:00`.

**Seed dev:** `seal-gd1-ready`, `seal-spring-2026` — `Gd1DataSeeder` căn theo `eventStart`/`eventEnd` tương tự.

---

## 6. Kiểm thử nhanh

```bash
mvn test -Dtest=EventScheduleValidatorImplTest,HackathonTimelineServiceImplTest,RoundServiceImplExamValidationTest
```

---

## 7. Tài liệu liên quan

| File | Vai trò |
|------|---------|
| [mf01-gd1-quy-trinh-api.md](mf01-gd1-quy-trinh-api.md) | Runbook 7 bước GĐ1 (Bước 6–7) |
| [mf01.md](mf01.md) | Spec MF-01 đầy đủ |
| [schema-v3.0-mysql.md](../db/schema-v3.0-mysql.md) | DDL |
| [fr-06a-events.md](../api/mf-01/fr-06a-events.md) | Index ngắn → trỏ về file này |

*Cập nhật: §0 thuật ngữ; timeline PDF Spring/Fall; gate round↔event; OTHER hai chiều; DELETE milestone.*
