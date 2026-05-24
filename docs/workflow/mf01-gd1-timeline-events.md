# MF-01 GĐ1 — Timeline & Events (SEAL FPT)

**Phạm vi:** Hackathon **1 ngày thi** (ví dụ GĐ1 seed: 10/06/2026). Local / GĐ1.

**Code:** `EventScheduleValidatorImpl`, `HackathonTimelineServiceImpl`, `HackathonReadinessServiceImpl`, `EventServiceImpl`, `Gd1DataSeeder.computeDates`.

---

## 0. Giải thích thuật ngữ (đọc trước §3–§4)

Trong BE có **hai lớp** dữ liệu thời gian: **Event** (lịch công khai) và **Round** (vòng thi).

Cấu trúc domain:
- **Hackathon** → cha của **Round**; **Round** → cha của **Track** (Sơ loại) hoặc **Criteria** (Chung kết trực tiếp trên `round_id`)
- **Event** thuộc Hackathon — không validate chéo `examAt` với AWARDS (ngoài KICKOFF + khung ngày)

### 0.1 Event `type`

| `type` | Ý nghĩa | Ví dụ seed GĐ1 (2026) |
|--------|---------|------------------------|
| `WORKSHOP` | Workshop trong gap sau đăng ký | 06/06 20:00–21:30 |
| `KICKOFF` | Khai mạc trong gap, **khác ngày** với WORKSHOP | 07/06 14:00–17:00 |
| `AWARDS` | Trao giải — đúng ngày `eventEnd` | 10/06 17:30–19:00 |
| `PRESENTATION` | Sự kiện phụ — không milestone | Tùy |
| `OTHER` | Sự kiện phụ | Tùy |

### 0.2 Round — vòng thi

| Khái niệm | DB/API |
|-----------|--------|
| Sơ loại | `isFinal=false`, criteria trên **Track** (`POST /tracks/{id}/criteria`) |
| Chung kết | `isFinal=true`, **không có Track**, criteria trên **Round** (`POST /rounds/{id}/criteria`) |

### 0.3 Timeline tổng (5 giai đoạn)

```text
Đăng ký (registrationStart … registrationEnd)
  → WORKSHOP (gap, sau regEnd, trước eventStart)
  → KICKOFF (cùng gap, ngày khác WORKSHOP)
  → Thi (examAt + nộp bài trong [eventStart, eventEnd])
  → AWARDS (đúng ngày eventEnd)
```

**Event / Round:**

```text
WORKSHOP, KICKOFF: (registrationEnd, eventStart) exclusive
WORKSHOP ↔ KICKOFF: hai ngày lịch khác nhau
examAt: sau KICKOFF.endsAt; ngày ∈ [eventStart, eventEnd]
examAt < submissionOpen < submissionDeadline (mỗi vòng)
Hạn Sơ loại < examAt Chung kết
Hạn Chung kết < AWARDS.startsAt (khi lưu Round — không bắt AWARDS sau deadline CK)
```

**Không** bắt `CK.examAt < AWARDS.startsAt` ở `HackathonTimelineService` — AWARDS không kiểm soát `examAt`.

### 0.4 Ví dụ seed GĐ1 (24/05 – 10/06/2026)

| Giai đoạn | Thời gian |
|-----------|-----------|
| Đăng ký | 24/05 – 05/06 |
| WORKSHOP | 06/06 20:00–21:30 |
| KICKOFF | 07/06 14:00–17:00 |
| Thi (cùng ngày) | 10/06: SL exam 08:00, mở nộp 09:00, hạn 11:30 · CK exam 13:00, mở nộp 14:00, hạn 16:30 |
| AWARDS | 10/06 17:30–19:00 |

`eventStart = eventEnd = 10/06`.

---

## 1. Validate event — 3 lớp

| Lớp | Loại | Quy tắc | Code |
|-----|------|---------|------|
| 1 | WORKSHOP | `registrationEnd < date < eventStart` | `EVENT_OUT_OF_HACKATHON` |
| 1 | KICKOFF | Cùng gap như WORKSHOP (không bắt `== eventStart`) | `EVENT_OUT_OF_HACKATHON` |
| 1 | AWARDS | `date == eventEnd` | `EVENT_OUT_OF_HACKATHON` |
| 2 | * | 1 milestone / type | `EVENT_MILESTONE_DUPLICATE` |
| 3 | * | WORKSHOP → KICKOFF → AWARDS (`endsAt` trước `startsAt` sau) | `EVENT_ORDER_VIOLATION` |
| 3b | WS+KO | **Khác calendar day** | `EVENT_ORDER_VIOLATION` |

---

## 2. `round.examAt` & deadline (FR-03)

| Vòng | Quy tắc | Code |
|------|---------|------|
| Mọi vòng | Sau `KICKOFF.endsAt` | `ROUND_EXAM_BEFORE_KICKOFF` |
| Mọi vòng | `examAt` trong `[eventStart, eventEnd]` | `EVENT_OUT_OF_HACKATHON` |
| Mọi vòng | `examAt` **trước** `submissionOpen` | `ROUND_EXAM_BEFORE_SUBMISSION_OPEN` |
| Sơ loại | `examAt` trước CK; `submissionDeadline` trước `final.examAt` | `ROUND_PRELIM_*` / `ROUND_PRELIM_DEADLINE_AFTER_FINAL_EXAM` |
| Chung kết | `submissionDeadline` trước `AWARDS.startsAt` (nếu AWARDS có) | `ROUND_FINAL_DEADLINE_AFTER_AWARDS` |

**DELETE KICKOFF** → revalidate round examAt. **DELETE AWARDS** → không revalidate round.

---

## 3. Criteria Chung kết

- **Không** tạo Track trong round FINAL (`DESIGN_VIOLATION`).
- Criteria CK: `POST /api/v1/rounds/{finalRoundId}/criteria` — `track_id` null, `round_id` set.
- Readiness ONGOING: criteria trên từng Track Sơ loại **và** trên Round CK — không yêu cầu Track CK.

---

## 4. Kiểm thử

```bash
mvn test -Dtest=EventScheduleValidatorImplTest,HackathonTimelineServiceImplTest,RoundServiceImplExamValidationTest,RoundServiceImplSequenceTest,CriteriaServiceImplFinalRoundTest
```

---

## 5. Tài liệu liên quan

| File | Vai trò |
|------|---------|
| [mf01-gd1-quy-trinh-api.md](mf01-gd1-quy-trinh-api.md) | Runbook API GĐ1 |
| [mf01.md](mf01.md) | Spec MF-01 |

*Cập nhật: KICKOFF trong gap; WS/KO khác ngày; examAt trước mở nộp; deadline CK trước AWARDS; seed 24/05–10/06/2026.*
