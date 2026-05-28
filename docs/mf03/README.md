# MF-03 — Giai đoạn 3–5 (Thi, chấm điểm, chuyển vòng) + GĐ6 (Kết thúc)

**Nguồn spec:** `GD03_SEAL_MF03_v2_2.docx` (Workflow v5.0 · GĐ3 → GĐ6)

**Phạm vi:** FR-20 … FR-36 — nộp bài, chấm điểm, khóa điểm, xếp hạng, tiebreak, wild card, advance, judge CK, trao giải.

**Tiền đề:** MF-01 (hackathon / round / track / criteria) + MF-02 (auth JWT, đội ACTIVE, lottery, `team_round_participation`).

**Quy tắc ưu tiên khi mâu thuẫn:** [`schema-v3.0-mysql.md`](../db/schema-v3.0-mysql.md) > GD03 docx > tài liệu này.

---

## Thứ tự đọc (dev / FE / QA)

| # | File | Đối tượng | Nội dung |
|---|------|-----------|----------|
| 1 | [01-business-rules-gd3.md](01-business-rules-gd3.md) | BE / BA | FR-20…36, gate, XOR submission, lock scoring |
| 2 | [02-mainflow-gd3.md](02-mainflow-gd3.md) | PM / FE | Luồng GĐ3 → GĐ6 + mermaid |
| 3 | **[03-api-reference-gd3.md](03-api-reference-gd3.md)** | **FE (ưu tiên)** | Endpoint, auth, body, trạng thái implement |
| 4 | [04-test-data.md](04-test-data.md) | FE / QA | curl, Postman, token mẫu |
| 5 | [05-fe-handover-gd3.md](05-fe-handover-gd3.md) | FE | Màn hình gợi ý + happy path GĐ3→GĐ5 |

**Swagger:** `http://localhost:8080/swagger-ui.html` — tag **Submissions**, **Scores**, **Round Progression**, **Prizes**, **Teams Journey**.

**Migration MF-03:** `src/main/resources/db/manual/V20260528_mf03_schema_delta.sql` · auto: `Mf03SchemaMigration` khi start app.

---

## Trạng thái implement (tóm tắt)

| Nhóm | Logic |
|------|--------|
| `PATCH /rounds/{id}/activate` | ✅ (MF-01, dùng cho FR-20/32) |
| `PATCH /hackathons/{id}/status` | ✅ |
| `POST /hackathons/{id}/prizes` | ✅ |
| `GET /submissions` (lọc role) | ✅ |
| Submissions submit / resubmit / review | ⏳ TODO |
| Scores / calibration | ⏳ TODO |
| Round progression (ranking, tiebreak, wildcard, advance, …) | ⏳ TODO (route có, trả stub) |
| `GET /teams/{id}/journey` | ⏳ TODO |

Chi tiết từng endpoint: [03-api-reference-gd3.md](03-api-reference-gd3.md).

---

## Liên kết MF-01 / MF-02

| Chủ đề | Tài liệu |
|--------|----------|
| Envelope & error | [mf01/api/_conventions.md](../mf01/api/_conventions.md) |
| Activate round | [mf01/api/fr-06b-activate.md](../mf01/api/fr-06b-activate.md) |
| Hackathon status | [mf01/api/fr-06-status.md](../mf01/api/fr-06-status.md) |
| Auth JWT | [mf02/01-auth-users.md](../mf02/01-auth-users.md) |
| Đội & lottery | [mf02/03-api-reference-gd2.md](../mf02/03-api-reference-gd2.md) |

---

## Package code (BE)

```
com.sealhackathon.api.submissions.*
com.sealhackathon.api.scores.*
com.sealhackathon.api.rounds.controller.RoundProgressionController
com.sealhackathon.api.rounds.service.RoundProgressionService
com.sealhackathon.api.teams.controller.TeamJourneyController
com.sealhackathon.api.prizes.*
com.sealhackathon.api.tiebreak_evaluations.entity
com.sealhackathon.api.wildcard_reviews.entity
```
