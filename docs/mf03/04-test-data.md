# MF-03 — Test data & Postman

**Profile:** `dev` (seed MF-01 + MF-02).  
**Swagger:** `http://localhost:8080/swagger-ui.html`

---

## 1. Tài khoản mẫu (sau seed)

| Role | Email (gợi ý seed) | Mật khẩu |
|------|-------------------|----------|
| Coordinator | `coord@seal.local` | xem [mf02/04-test-data.md](../mf02/04-test-data.md) |
| Student (leader) | `student1@seal.local` | `Student@123` |
| Judge | `judge1@seal.local` | `Judge@123` |

**Login**

```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "coord@seal.local",
  "password": "Coordinator@123"
}
```

Lưu `data.accessToken` → biến Postman `{{accessToken}}`.

---

## 2. ID tham chiếu (seed Gd1 — có thể khác máy bạn)

| Entity | Id gợi ý |
|--------|----------|
| Hackathon | `1` |
| Round Sơ loại | `1` |
| Round Chung kết | `2` |
| Track 1 | `1` |
| Team ACTIVE | `1` |

Kiểm tra: `GET /api/v1/hackathons/1`, `GET /api/v1/teams?hackathonId=1`.

---

## 3. curl — Happy path (Coordinator)

```bash
# Activate Sơ loại
curl -s -X PATCH "http://localhost:8080/api/v1/rounds/1/activate" \
  -H "Authorization: Bearer $COORD_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"note":"Test GĐ3"}'

# Phát đề
curl -s -X PATCH "http://localhost:8080/api/v1/rounds/1/release-problem" \
  -H "Authorization: Bearer $COORD_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"problemStatementUrl":"https://example.com/debai.pdf"}'

# Lock scoring
curl -s -X PATCH "http://localhost:8080/api/v1/rounds/1/lock-scoring" \
  -H "Authorization: Bearer $COORD_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"force":false}'

# Live ranking preview (polling fallback)
curl -s "http://localhost:8080/api/v1/rounds/1/ranking/preview" \
  -H "Authorization: Bearer $COORD_TOKEN"
```

**WebSocket:** xem [06-live-scoring-websocket.md](06-live-scoring-websocket.md).

---

## 4. curl — Student nộp bài

```bash
curl -s -X POST "http://localhost:8080/api/v1/submissions" \
  -H "Authorization: Bearer $STUDENT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "teamId": 1,
    "trackId": 1,
    "repoUrl": "https://github.com/example/seal-demo",
    "demoUrl": "https://demo.example.com"
  }'
```

**List (student)**

```bash
curl -s "http://localhost:8080/api/v1/submissions?teamId=1&roundId=1" \
  -H "Authorization: Bearer $STUDENT_TOKEN"
```

---

## 5. curl — Judge chấm

```bash
curl -s -X POST "http://localhost:8080/api/v1/scores" \
  -H "Authorization: Bearer $JUDGE_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "submissionId": 1,
    "criterionId": 1,
    "scoreValue": 8.0,
    "scoreType": "NORMAL"
  }'
```

---

## 6. curl — Trao giải (GĐ6)

**Chuẩn bị:** `PATCH /hackathons/1/status` → `PENDING_CONFIRM` (sau lock CK khi logic đủ).

```bash
curl -s -X POST "http://localhost:8080/api/v1/hackathons/1/prizes" \
  -H "Authorization: Bearer $COORD_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "roundId": 2,
    "teamId": 1,
    "prizeName": "Giải Nhất",
    "prizeRank": "FIRST",
    "prizeValue": "7000000"
  }'
```

**Trùng giải (kỳ vọng 409)**

```bash
# Gọi lại cùng teamId hoặc cùng prizeRank FIRST
```

---

## 7. Scoreboard public

```bash
curl -s "http://localhost:8080/api/v1/rounds/2/scoreboard"
# Không header Authorization
```

---

## 8. Migration MF-03

Chạy thủ công (MySQL):

```bash
mysql -u ... -p seal_db < src/main/resources/db/manual/V20260528_mf03_schema_delta.sql
```

Hoặc start app với profile dev — `Mf03SchemaMigration` tự thêm cột nếu thiếu.

---

## 9. Postman collection (gợi ý folder)

```
MF-03 GĐ3
├── Auth (import từ MF-02)
├── GĐ3 Sơ loại
│   ├── Activate round
│   ├── Release problem
│   ├── Submit (student)
│   ├── List submissions
│   ├── Score (judge)
│   └── Lock scoring
├── GĐ4 Chuyển vòng
│   ├── Ranking preview
│   ├── Advance teams
│   └── Assign final judges
├── GĐ5 Chung kết
│   ├── Submit final
│   └── Lock final
└── GĐ6
    ├── Award prize
    ├── Status FINISHED
    └── Scoreboard (no auth)
```

---

## 10. Kiểm tra stub vs logic thật

| API | Kỳ vọng hiện tại |
|-----|------------------|
| activate | Có `isActive`, `activatedAt` |
| prizes | 201 + `PrizeResponse`; 409 trùng |
| GET submissions | Mảng thật nếu DB có row |
| POST submissions | 200/201 body rỗng hoặc stub — **chưa persist** |
| ranking / advance | 200 + `[]` hoặc echo — **chưa persist** |

Cập nhật bảng này khi BE merge logic MF-03.
