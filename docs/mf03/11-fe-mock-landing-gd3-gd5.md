# MF-03 — Mock data & UI preview (GĐ3 → GĐ5) cho FE

**Mục đích:** BE đã có **khung API** (route + JSON shape); một số endpoint còn stub (`data: []`). FE dùng file này để **dựng UI trước** bằng MSW/fixture; khi BE xong → tắt mock, gọi API thật.

**JSON request/response từng API:** [12-fe-api-catalog-gd3-gd5.md](12-fe-api-catalog-gd3-gd5.md) — **đọc file này trước** (giống [mf02/fe-auth-integration.md](../mf02/fe-auth-integration.md), không dùng tên class Java).

**Auth / token:** [mf02/fe-auth-integration.md](../mf02/fe-auth-integration.md) · [04-test-data.md](04-test-data.md)

**Base URL:** `http://localhost:8080/api/v1`

---

## 1. Chiến lược mock

| Giai đoạn | BE hiện tại | FE |
|-----------|-------------|-----|
| GĐ3 Sơ loại | ✅ logic thật | Gọi BE trực tiếp (mock tùy chọn) |
| GĐ4 phase 1 (publish, advance, judge CK) | ✅ | Gọi BE trực tiếp |
| GĐ4 phase 2 (tiebreak, wildcard, scoreboard) | ⏳ `[]` | **Mock JSON từ file 12 §5** |
| GĐ5 (CK submit, calibration, RBL) | ⏳ / 🔶 | **Mock JSON từ file 12 §6** |
| WebSocket GĐ3 | ✅ | SockJS thật; fallback poll §7 file 12 |

```typescript
// MSW / axios — chỉ mock path stub
const USE_MOCK = import.meta.env.VITE_USE_MOCK === 'true';
const MOCK_PATHS = ['/tiebreak', '/wildcard', '/scoreboard', '/calibration', '/rbl', '/journey'];
```

Stub BE vẫn trả `200` + `data: []` — UI phải handle empty state. Mock cung cấp **data có ý nghĩa** để layout (copy JSON từ file 12 vào handler MSW).

---

## 2. Sơ đồ màn hình theo vai trò

### 2.1 Student

```
Dashboard đội
  ├── Thông tin đề          → GET /rounds/{id}           (§4.1 file 12)
  ├── Form nộp bài          → POST /submissions          (§4.2)
  ├── Trạng thái bài        → GET /submissions?teamId=   (§4.2)
  └── Timeline              → GET /teams/{id}/journey    (§4.8, ⏳ mock)

Chung kết (GĐ5)
  └── Form nộp CK           → POST /submissions (roundId, không trackId) (§6.1)
```

### 2.2 Judge

```
Danh sách bài             → GET /submissions?roundId=
Form chấm                 → POST /scores                 (§4.3)
Tiêu chí                  → GET /tracks/{id}/criteria hoặc /rounds/{id}/criteria
Calibration               → POST /scores/calibration     (§6.2, ⏳)
Live board                → WS /topic/rounds/{id}/leaderboard-preview (§7)
RBL (GĐ5)                 → GET /rounds/{id}/rbl/*       (§6.3, ⏳)
```

### 2.3 Coordinator

```
GĐ3: activate → release-problem → duyệt muộn → lock → ranking
GĐ4: publish → tiebreak? → wildcard? → advance → judge CK → activate CK
GĐ5: calibration → lock CK → hackathon PENDING_CONFIRM
Public: GET /rounds/{id}/scoreboard (no JWT, ⏳ mock §5.5 file 12)
```

---

## 3. Landing / trang công khai

| Block UI | API khi BE xong | JSON mock |
|----------|-----------------|-----------|
| Hero + countdown | `GET /hackathons/{id}` | §8 file 12 + hackathon seed |
| Bảng xếp hạng (public) | `GET /rounds/{id}/scoreboard` | §5.5 file 12 |
| Tiến độ chấm live | WS `/topic/rounds/{id}/scoring-progress` | §4.5 + §7 file 12 |
| Leaderboard live (BTC) | WS `/topic/rounds/{id}/leaderboard-preview` | §4.6 file 12 |
| Trạng thái vòng | `GET /rounds/{id}` | §4.1 file 12 |

---

## 4. Ví dụ MSW handler

Copy nguyên block JSON từ file 12 vào handler — ví dụ tiebreak stub:

```typescript
import { http, HttpResponse } from 'msw';

export const mf03Handlers = [
  http.get('*/api/v1/rounds/:id/tiebreak', () =>
    HttpResponse.json({
      success: true,
      data: [
        {
          partitionKey: 'track:1:group:A',
          cutoffRank: 2,
          candidateTeamIds: [4, 7],
        },
      ],
    })
  ),
  // ... thêm handler cho /scoreboard, /wildcard-candidates, /calibration-sessions, /rbl/*
];
```

Toàn bộ shape: [12-fe-api-catalog-gd3-gd5.md](12-fe-api-catalog-gd3-gd5.md).

---

## 5. ID seed gợi ý (dev local)

| Entity | ID | Ghi chú |
|--------|-----|---------|
| Hackathon | 1 | `seal-e2e-2026` ONGOING |
| Round Sơ loại | 1 | `isFinal=false` |
| Round CK | 2 | `isFinal=true` |
| Track 1 | 1 | Sau lottery GĐ2 |
| Team ACTIVE | 4 | Demo nộp bài |
| Coordinator | login | `coord@fpt.edu.vn` / `Coordinator@dev1` |

Chi tiết: [mf02/05-test-data-gd2-teams.md](../mf02/05-test-data-gd2-teams.md) · curl: [04-test-data.md](04-test-data.md).

---

## 6. Checklist trước khi gắn API thật

| # | Việc |
|---|------|
| 1 | UI handle `data: []` stub (không crash) |
| 2 | UI handle `warnings[]` trên lock / ranking / assign judges |
| 3 | Phân biệt SL (`trackId`) vs CK (`roundId`, không `trackId`) |
| 4 | Scoreboard public: không gửi Bearer |
| 5 | WS reconnect + fallback polling |
| 6 | Map `error.code` — không parse `message` |
| 7 | Theo [09-be-backlog-gd4-gd5-gd6.md](09-be-backlog-gd4-gd5-gd6.md) — biết endpoint ⏳ → ✅ |

---

## 7. Liên kết

| File | Vai trò |
|------|---------|
| **[12-fe-api-catalog-gd3-gd5.md](12-fe-api-catalog-gd3-gd5.md)** | **Request/response JSON từng API — file chính cho FE** |
| [07-fe-api-flow-gd3.md](07-fe-api-flow-gd3.md) | Luồng từng bước GĐ3 |
| [08-fe-api-flow-gd4.md](08-fe-api-flow-gd4.md) | Luồng GĐ4 |
| [06-live-scoring-websocket.md](06-live-scoring-websocket.md) | STOMP chi tiết |
| [api-authorization-matrix.md](../api-authorization-matrix.md) | Phân quyền |
