# MF-03 FE Handover — GĐ3 → GĐ5 (Thi & chấm)

Tài liệu cho **Frontend**: màn hình gợi ý, thứ tự gọi API, và lưu ý khi BE còn stub.

**Contract đầy đủ:** [03-api-reference-gd3.md](03-api-reference-gd3.md)  
**Auth:** [mf02/fe-auth-integration.md](../mf02/fe-auth-integration.md)

---

## 1. Trước khi vào GĐ3

| Điều kiện | Cách kiểm tra FE |
|-----------|------------------|
| User `APPROVED` | JWT / `GET /users/me` |
| Đội `ACTIVE` | `GET /teams/{id}` |
| Đã lottery | `team_round_tracks` hiển thị track |
| Hackathon `ONGOING` | `GET /hackathons/{id}` |

Coordinator: round Sơ loại đã cấu hình criteria + judge (MF-01).

---

## 2. Màn hình đề xuất

### 2.1 Student — Nộp bài

| Màn | API |
|-----|-----|
| Thông tin đề | Đọc từ hackathon/round (sau release-problem có URL) |
| Form nộp | `POST /submissions` |
| Lịch sử / trạng thái | `GET /submissions?teamId=&roundId=` |
| Nộp lại | `PATCH /submissions/{id}/resubmit` |

**Body Sơ loại:** có `trackId`. **Chung kết:** chỉ `roundId` (FINAL), **không** gửi `trackId`.

**UI status:**

| `status` | Màu gợi ý |
|----------|------------|
| `SUBMITTED` | Xanh |
| `LATE_PENDING` | Vàng — chờ BTC |
| `LATE_APPROVED` / `ACCEPTED` | Xanh đậm |
| `REJECTED` | Đỏ |

### 2.2 Coordinator — Vận hành round

| Màn | API |
|-----|-----|
| Kích hoạt vòng | `PATCH /rounds/{id}/activate` |
| Phát đề | `PATCH /rounds/{id}/release-problem` |
| Duyệt nộp muộn | `GET /submissions?roundId=` + `PATCH .../review` |
| Tiến độ chấm | `GET /rounds/{id}/scoring-progress` |
| Khóa chấm | `PATCH /rounds/{id}/lock-scoring` |
| Xếp hạng | `GET .../ranking/preview` → `.../ranking` |
| Tiebreak / Wild card | endpoints `tiebreak`, `wildcard` |
| Chốt danh sách | `POST .../advance-teams` |
| Phân judge CK | `POST .../judge-assignments` |

**Lock scoring:** Nếu response có `warnings`, hiển thị dialog xác nhận (partial scoring).

### 2.3 Judge — Chấm điểm

| Màn | API |
|-----|-----|
| Danh sách bài | `GET /submissions?roundId=` |
| Form điểm | `POST /scores` |
| Calibration (nếu có) | `POST /scores/calibration` |

Disable form khi nhận `423 SCORING_LOCKED`.

### 2.4 Public — Bảng điểm

`GET /rounds/{id}/scoreboard` — **không** cần header Authorization (sau khi BTC công bố).

### 2.5 GĐ6 — Trao giải

Chỉ khi hackathon `PENDING_CONFIRM`:

`POST /hackathons/{id}/prizes` → sau đó `PATCH .../status` `FINISHED`.

---

## 3. Happy path (QA / demo)

Giả sử seed MF-01 + MF-02 đã có hackathon id `1`, round SL id `1`, round CK id `2`.

1. **Coordinator** login → `PATCH /rounds/1/activate`
2. `PATCH /rounds/1/release-problem` `{ "problemStatementUrl": "https://..." }`
3. **Student** login → `POST /submissions` (teamId, trackId, urls…)
4. **Judge** login → `POST /scores` (submissionId, criterionId, scoreValue)
5. **Coordinator** → `PATCH /rounds/1/lock-scoring` `{ "force": false }`
6. `GET /rounds/1/ranking/preview` → tiebreak/wildcard nếu cần → `POST /rounds/1/advance-teams`
7. `POST /rounds/2/judge-assignments` → `PATCH /rounds/2/activate`
8. Student nộp CK → Judge chấm → `PATCH /rounds/2/lock-scoring`
9. `POST /hackathons/1/prizes` → `PATCH /hackathons/1/status` `{ "targetStatus": "FINISHED" }`
10. `GET /rounds/2/scoreboard` (không token)

**Lưu ý:** Các bước 3–8 có thể trả data rỗng/stub cho đến khi BE hoàn thiện logic — FE nên handle `data` null/[] và theo dõi [README.md](README.md).

---

## 4. Xử lý lỗi thường gặp

| Code | FE |
|------|-----|
| `ROUND_NOT_ACTIVE` | Ẩn nút nộp; toast “Chưa mở vòng” |
| `SCORING_LOCKED` | Khóa form chấm |
| `SUBMISSION_NOT_GRADABLE` | Không cho chấm bài LATE_PENDING |
| `TIEBREAK_REQUIRED` | Điều hướng màn tiebreak |
| `PRIZE_DUPLICATE` | Toast trùng giải / đội |
| `HACKATHON_NOT_PENDING_CONFIRM` | Chặn màn trao giải |

Danh sách đầy đủ: [01-business-rules-gd3.md](01-business-rules-gd3.md) §16.

---

## 5. Token & env

Giống MF-02 — xem [04-test-data.md](04-test-data.md).
