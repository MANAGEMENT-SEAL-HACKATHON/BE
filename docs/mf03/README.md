# MF-03 — Giai đoạn 3–5 (Thi, chấm điểm, chuyển vòng) + GĐ6 (Kết thúc)

**Nguồn spec:** `GD03_05_SEAL_MF_v4_1.docx` (Workflow v5.0 · GĐ3 → GĐ6)

**Phạm vi:** FR-15 … FR-30A — nộp bài, chấm điểm, khóa điểm, xếp hạng, publish, tiebreak, wild card, advance, calibration, RBL, judge CK, trao giải.

**Tiền đề:** MF-01 + MF-02 (auth JWT, đội ACTIVE, lottery, `team_round_participation` + `team_round_tracks`).

**Quy tắc ưu tiên:** [`schema-v3.0-mysql.md`](../db/schema-v3.0-mysql.md) > GD03 v4.1 docx > tài liệu này.

---

## Ánh xạ FR v2.2 → v4.1 (tham khảo)

| v4.1 | v2.2 cũ | Tên ngắn |
|------|---------|----------|
| FR-15 | FR-20 | Activate + judge |
| FR-15A | FR-21 | Phát đề |
| FR-16 | FR-22 | Nộp bài |
| FR-16A | FR-25 | Duyệt trễ |
| FR-18/18A | FR-24 | Chấm / live scoring |
| FR-20A | FR-26 | Khóa chấm |
| FR-20/22 | FR-27 | Leaderboard / Top N |
| FR-22A | FR-29 | Wild Card |
| FR-22B | FR-28 | Tiebreak |
| FR-24 | — | Publish kết quả |
| FR-25 | FR-32 | Activate CK |
| FR-26 | FR-33 | Nộp CK |
| FR-27 | FR-31 | Judge CK |
| FR-29 | FR-34 | Calibration |
| FR-30 | — | RBL Dashboard |
| FR-30A | FR-36 | PENDING_CONFIRM |

---

## Thứ tự đọc (dev / FE / QA)

| # | File | Đối tượng | Nội dung |
|---|------|-----------|----------|
| 1 | [01-business-rules-gd3.md](01-business-rules-gd3.md) | BE / BA | FR-15…30A, gate, BUG-1…9 |
| 2 | [02-mainflow-gd3.md](02-mainflow-gd3.md) | PM / FE | Luồng GĐ3 → GĐ6 + FR-24 publish |
| 3 | **[03-api-reference-gd3.md](03-api-reference-gd3.md)** | **FE** | Endpoint v4.1 + alias deprecated |
| 4 | [04-test-data.md](04-test-data.md) | FE / QA | curl, token mẫu |
| 5 | [05-fe-handover-gd3.md](05-fe-handover-gd3.md) | FE | Breaking path + màn hình |
| 6 | [06-live-scoring-websocket.md](06-live-scoring-websocket.md) | FE | STOMP/SockJS FR-18A |

**Swagger:** tag Submissions, Scores, Round Progression, Live Scoring (WebSocket), Wildcard Reviews, Calibration Sessions, RBL Dashboard, Prizes.

**Migration v4.1:** `V20260529_gd03_v4_1_delta.sql` · auto: `Gd03V41SchemaMigration` @Order(0).

---

## Trạng thái implement (tóm tắt — GĐ3)

| Nhóm | Logic |
|------|--------|
| `PATCH /rounds/{id}/activate` | ✅ + `NO_TEAMS_IN_ROUND` + gate CK publish |
| `PATCH /rounds/{id}/release-problem` | ✅ FR-15A |
| `POST /submissions` (Sơ loại upsert) | ✅ FR-16 |
| `PATCH /submissions/{id}/review-late` | ✅ FR-16A |
| `POST /scores` | ✅ FR-18/19 + WS push |
| **WebSocket `/ws`** | ✅ FR-18A STOMP |
| `GET .../ranking/preview`, `scoring-progress` | ✅ FR-20 live |
| `PATCH /rounds/{id}/lock-scoring` | ✅ FR-20A (+ warnings) |
| `PATCH /teams/{id}/eliminate` | ✅ FR-21 |
| `GET /submissions` | ✅ lọc role |
| GĐ4/GĐ5: publish, advance, wildcard, CK submit, calibration, RBL | ⏳ stub |
| `POST /hackathons/{id}/prizes` | ✅ (GĐ6) |

Chi tiết: [03-api-reference-gd3.md](03-api-reference-gd3.md) · Phân quyền: [api-authorization-matrix.md](../api-authorization-matrix.md).

---

## Package code (BE)

```
com.sealhackathon.api.submissions.*
com.sealhackathon.api.scores.*
com.sealhackathon.api.rounds.*
com.sealhackathon.api.live_scoring.*
com.sealhackathon.api.wildcard_reviews.*
com.sealhackathon.api.calibration_sessions.*
com.sealhackathon.api.rbl.*
com.sealhackathon.api.prizes.*
com.sealhackathon.api.config.Gd03V41SchemaMigration
```
