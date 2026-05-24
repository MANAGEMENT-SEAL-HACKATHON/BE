# MF-01 GĐ1 — Timeline & Events (SEAL FPT)

**Phạm vi:** Hackathon **1–2 ngày** (Spring 2026, Fall 2025). Local / GĐ1 — không mở rộng hackathon nhiều ngày.

**Code:** `EventScheduleValidatorImpl`, `HackathonTimelineServiceImpl`, `HackathonReadinessServiceImpl`, `EventServiceImpl`, `Gd1DataSeeder.computeDates`.

---

## 0. Giải thích thuật ngữ (đọc trước §3–§4)

Trong BE có **hai lớp** dữ liệu thời gian: **Event** (lịch công khai) và **Round** (vòng thi).

Cấu trúc domain:
- **Hackathon** → cha của **Round**; **Round** → cha của **Track / Criteria**
- **Event** thuộc về Hackathon (tham chiếu hackathon_id), được tạo dựa trên lịch của hackathon, round, track
- Không có ràng buộc validation chéo Event↔Round: Event không kiểm soát `examAt` của Round và ngược lại

### 0.1 Event `type` — không phải "slide thuyết trình"

| `type` (enum DB) | Hiểu trong SEAL FPT | Ví dụ Spring 2026 |
|------------------|---------------------|-------------------|
| `WORKSHOP` | Buổi workshop trước mùa | 9/4 20:00–21:30 |
| `KICKOFF` | **Khai mạc** (bốc thăm track, họp đội) | 11/4 14:00–17:00 |
| `PRESENTATION` | Sự kiện phụ — coordinator có thể dùng để ghi lịch trình bày. **Không còn là milestone.** | Tùy — trong `[eventStart, eventEnd]` |
| `AWARDS` | **Lễ trao giải** (milestone — đúng ngày `eventEnd`) | 12/4 17:30–19:00 |
| `OTHER` | Sự kiện phụ (họp mentor, …) — không thay milestone | Tùy coordinator |

> `PRESENTATION` giữ nguyên trong enum/DB (không breaking). Không còn là milestone, không bắt `endsAt`, không 1-per-hackathon, không thứ tự Layer 3.

### 0.2 Round — vòng thi (FR-03)

| Khái niệm | Trong DB/API | Ghi chú |
|-----------|--------------|---------|
| **Round sơ loại** | `round.isFinal = false` (PRELIMINARY / SEMIFINAL) | Thường 1 round; có **track** con |
| **CK (Chung kết)** | `round.isFinal = true` | **Đúng 1** round CK mỗi hackathon |
| **`examAt`** | `round.examAt` (bắt buộc) | **Một mốc thời gian** — "khi nào thi / chấm vòng này" |

**CK** = **Chung kết** (tiếng Việt), **không** phải `EventType`. CK là **Round**, không tạo bằng `POST .../events`.

### 0.3 `examAt` gắn với Event nào?

```text
Event (lịch)                    Round (vòng thi)
────────────                    ────────────────
WORKSHOP                        (mọi loại) examAt > KICKOFF.endsAt
KICKOFF                                    examAt ∈ [eventStart, eventEnd]
AWARDS (ngày eventEnd)         CK.examAt < AWARDS.startsAt  (nếu AWARDS đã tạo)
```

- **Không** so `examAt` với `PRESENTATION` — PRESENTATION là sự kiện phụ, không kiểm soát Round.
- **Có round CK** → readiness bắt **phải có** event `AWARDS` → `EVENT_AWARDS_MISSING` nếu thiếu (chỉ gate ONGOING, không block delete).
- **`examAt` lệch khung Hackathon** → `EVENT_OUT_OF_HACKATHON`.
- **CK.examAt ≥ AWARDS.startsAt** (khi AWARDS đã tạo) → `ROUND_EXAM_OUTSIDE_AWARDS`.

Mọi vòng: `examAt` phải **sau** `KICKOFF.endsAt` → `ROUND_EXAM_BEFORE_KICKOFF`.

### 0.4 Ví dụ đủ 3 milestone event + 2 round (Spring 2026)

| Thứ tự | Event | Round |
|--------|-------|-------|
| 1 | WORKSHOP 9/4 | — |
| 2 | KICKOFF 11/4 | — |
| 3 | AWARDS 12/4 17:30–19:00 | Sơ loại `examAt` = 12/4 **08:00** · CK `examAt` = 12/4 **10:00** |

Spring: **1 ngày thi (12/4)** — cả hai round exam diễn ra trước AWARDS (17:30).

---

## 1. Lịch tham chiếu (PDF)

| Giai đoạn | Fall 2025 | Spring 2026 |
|-----------|-----------|-------------|
| WORKSHOP | 29/10 19:30–21:30 | 9/4 20:00–21:30 |
| KICKOFF | 1/11 14:00–17:00 | 11/4 14:00–17:00 |
| AWARDS | 2/11 ~17:00–18:00 | 12/4 17:30–19:00 |

**Quy ước `hackathons`:**

| Field | Ý nghĩa |
|-------|---------|
| `event_start` | Ngày **khai mạc** |
| `event_end` | Ngày **thi / trao giải** (Spring: `event_start + 1` ngày) |

---

## 2. Validate event — 3 lớp (v3.3)

Lớp 1 tách theo từng loại — mỗi rule trong `events/service/impl/window/`:

| Lớp | Loại | Quy tắc | Code |
|-----|------|---------|------|
| 1   | WORKSHOP | `registrationEnd < date < eventStart` (exclusive cả hai đầu) | `EVENT_OUT_OF_HACKATHON` |
| 1   | KICKOFF | `date == eventStart` (đúng ngày khai mạc) | `EVENT_OUT_OF_HACKATHON` |
| 1   | AWARDS | `date == eventEnd`; `effectiveEnd.date ≤ eventEnd` | `EVENT_OUT_OF_HACKATHON` |
| 1   | PRESENTATION / OTHER | Trong `[eventStart, eventEnd]` | `EVENT_OUT_OF_HACKATHON` |
| 2   | * | Tối đa **1** milestone / type (WORKSHOP, KICKOFF, AWARDS) | `EVENT_MILESTONE_DUPLICATE` |
| 2b  | * | OTHER/PRESENTATION ↔ milestone không chồng giờ (hai chiều) | `EVENT_CONFLICTS_WITH_MILESTONE` |
| 3   | * | `WORKSHOP → KICKOFF → AWARDS` (`endsAt` trước `startsAt` giai đoạn sau) | `EVENT_ORDER_VIOLATION`, `EVENT_END_REQUIRED` |
| 3d  | * | Gợi ý KICKOFF đúng `eventStart` | WARN `EVENT_ORDER_INVALID` |

Milestone (WORKSHOP, KICKOFF, AWARDS) **bắt buộc `endsAt`**. Cần `location` hoặc `meetUrl`.

**Round uniqueness (FR-03):** Mỗi `roundType` (PRELIMINARY / SEMIFINAL / FINAL) chỉ tạo **1 lần** mỗi Hackathon — `ROUND_TYPE_DUPLICATE`.

---

## 3. `round.examAt` (FR-03 v3.3)

| Vòng | Quy tắc | Code |
|------|---------|------|
| Mọi vòng | Sau `KICKOFF.endsAt` | `ROUND_EXAM_BEFORE_KICKOFF` |
| Mọi vòng | `examAt.toLocalDate() ∈ [eventStart, eventEnd]` | `EVENT_OUT_OF_HACKATHON` |
| Chung kết (CK) | **Trước** `AWARDS.startsAt` (nếu AWARDS đã tạo) | `ROUND_EXAM_OUTSIDE_AWARDS` |

- **PRESENTATION không ràng buộc `examAt`** — sự kiện phụ, không kiểm soát vòng thi.
- Có **round CK** → vẫn phải có event **`AWARDS`** (`EVENT_AWARDS_MISSING`) khi chuyển trạng thái readiness — **không** block khi delete AWARDS.
- **PUT/DELETE** milestone → revalidate mọi round (`HackathonTimelineService`).

---

## 4. Readiness ONGOING (gate sự kiện) — v3.3

| Điều kiện | Code |
|-----------|------|
| ≥1 KICKOFF hợp lệ | `EVENT_KICKOFF_MISSING` |
| Có **round CK** → phải có event **AWARDS** (lễ trao giải) | `EVENT_AWARDS_MISSING` |
| Mọi milestone đã tạo pass validator 3 lớp | `EVENT_*` / `EVENT_ORDER_*` |
| `examAt` lệch khung | `ROUND_EXAM_*` / `EVENT_OUT_OF_HACKATHON` |
| Round trùng `roundType` | `ROUND_TYPE_DUPLICATE` |

> **Đã bỏ:** gate `EVENT_PRESENTATION_MISSING` — PRESENTATION không còn là milestone.

---

## 5. API & JSON mẫu (Spring 2026)

**Thứ tự POST milestone** (block nếu sai thứ tự):

1. WORKSHOP → 2. KICKOFF → 3. AWARDS

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
{ "title": "Lễ trao giải", "type": "AWARDS", "location": "Hội trường",
  "startsAt": "2026-04-12T17:30:00", "endsAt": "2026-04-12T19:00:00", "isPublic": true }
```

PRESENTATION (tùy chọn — bất kỳ lúc nào trong `[eventStart, eventEnd]`):

```json
{ "title": "Thuyết trình Sơ loại", "type": "PRESENTATION", "location": "Hội trường",
  "startsAt": "2026-04-12T08:00:00", "endsAt": "2026-04-12T12:00:00", "isPublic": true }
```

**Gợi ý `examAt`:** sơ loại `2026-04-12T08:00:00`; chung kết `2026-04-12T10:00:00`.

**Seed dev:** `seal-gd1-ready`, `seal-spring-2026` — `Gd1DataSeeder` căn theo `eventStart`/`eventEnd`.

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

*Cập nhật v3.3: PRESENTATION bỏ khỏi milestone chain; AWARDS chỉ validate theo `eventEnd`; fix circular-delete AWARDS↔Final round; Layer 3 = 3 milestone (WORKSHOP → KICKOFF → AWARDS); Round uniqueness giữ nguyên.*
