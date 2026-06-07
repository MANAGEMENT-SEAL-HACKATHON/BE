# E2E GĐ4 & GĐ5 — Seed data & Postman variables

Profile **`dev`**. Sau mỗi lần start app, xem log `[Gd4AdvanceDataSeeder]` và `[Gd5FinalRoundDataSeeder]` để lấy **ID thực tế**.

Password chung student: **`Student@dev1`** (`GdExtendedSeedConstants.DEV_STUDENT_PASSWORD`).

| Account | Email | Password |
|---------|-------|----------|
| Coordinator | `coord@fpt.edu.vn` | `Coordinator@dev1` |
| Guest Judge (CK) | `guestjudge@gmail.com` | `GuestJudge@dev1` |
| Judge internal | `judge1@fpt.edu.vn` | `Judge@dev1` |

---

## GĐ4 — Chuyển vòng

**Slug:** `seal-gd4-advance-ready`

**Trạng thái seed:**

| Thành phần | Giá trị |
|------------|---------|
| Hackathon | `ONGOING` |
| Sơ loại | `scoringLocked=true`, **`isPublished=false`**, `topNAdvance=1`, `minTeamsFinal=6`, `wildcardEnabled=true` |
| Chung kết | chưa active, chưa advance |
| Teams | 8 đội, 4 bảng (A/B track1, C/D track2), đủ điểm `isFinal=true` |

### Lấy ID

```http
GET /api/v1/hackathons?slug=seal-gd4-advance-ready
GET /api/v1/hackathons/{{hackathonId}}/rounds
```

Hoặc copy từ log startup.

### Postman variables (thay bằng ID log)

| Variable | Mô tả |
|----------|--------|
| `gd4HackathonSlug` | `seal-gd4-advance-ready` |
| `hackathonId` | từ GET hackathon |
| `prelimRoundId` | round `isFinal=false` |
| `finalRoundId` | round `isFinal=true` |
| `track1Id`, `track2Id` | GET `/rounds/{prelimRoundId}/tracks` |
| `guestJudgeId` | user `guestjudge@gmail.com` (thường id=3) |

### 8 teams (tên seed → dùng `teamId` từ log)

| Team seed | Bảng | Vai trò gợi ý |
|-----------|------|----------------|
| GD4-A01 Rank1 Bảng A | A | Top 1 → advance |
| GD4-A02 Rank2 Bảng A | A | Wildcard candidate |
| GD4-A03 Rank1 Bảng B | B | Top 1 → advance |
| GD4-A04 Rank2 Bảng B | B | Eliminate |
| GD4-A05 Rank1 Bảng C | C | Top 1 → advance |
| GD4-A06 Rank2 Bảng C | C | Wildcard candidate |
| GD4-A07 Rank1 Bảng D | D | Top 1 → advance |
| GD4-A08 Rank2 Bảng D | D | Eliminate |

**Student:** `student.gd4a.leader01@fpt.edu.vn` … `leader08@fpt.edu.vn`

### Luồng API (4.1 → 4.6)

1. **4.1** `GET /rounds/{{prelimRoundId}}/ranking` — có 8 dòng, xếp theo bảng
2. **4.2** `GET /rounds/{{prelimRoundId}}/wildcard-candidates` — kỳ vọng **2** review (thiếu 2 slot so với `minTeamsFinal=6`)
3. **4.2b** `PATCH /wildcard-reviews/{{wildcardReviewId}}` body:
   ```json
   { "approved": true, "coordinatorNote": "Wildcard approved by committee" }
   ```
4. **4.3** `PATCH /rounds/{{prelimRoundId}}/publish` (no body)
5. **4.4** `POST /rounds/{{prelimRoundId}}/advance`:
   ```json
   {
     "advancedTeamIds": ["<t01>","<t03>","<t05>","<t07>","<t02>","<t06>"],
     "eliminatedTeamIds": ["<t04>","<t08>"],
     "note": "Advance based on official ranking + wildcard"
   }
   ```
   (Thay bằng `teamId` thực từ log.)
6. **4.5** `POST /rounds/{{finalRoundId}}/judge-assignments` body `{ "judgeIds": [guestJudgeId] }` — 409 duplicate → bỏ qua
   ```json
   {
     "judgeId": "{{guestJudgeId}}",
     "roundId": "{{finalRoundId}}",
     "assignmentType": "FINAL_EXTERNAL"
   }
   ```
7. **4.5b** `GET /hackathons/{{hackathonId}}/readiness?target=FINAL_ROUND` → `ready: true`
8. **4.6** `PATCH /rounds/{{finalRoundId}}/activate` body `{ "note": "Start final round" }`

**Tiebreak/wildcard nâng cao (opt-in):** slug `seal-gd4-tiebreak-wildcard` — bật `app.seed.gd4.enabled=true`.

---

## GĐ5 — Chung kết

**Slug:** `seal-gd5-final-active`

**Trạng thái seed:**

| Thành phần | Giá trị |
|------------|---------|
| Hackathon | `ONGOING` (chưa `PENDING_CONFIRM`) |
| Sơ loại | published + locked |
| Chung kết | **active**, đề đã phát, **chưa** `scoringLocked` |
| Guest judge | đã có `FINAL_EXTERNAL` trên CK (từ structure seed) |

### 4 teams

| Team | Student | CK submission | CK scores |
|------|---------|---------------|-----------|
| GD5-01 CK SUBMITTED + scored | `student.gd5.leader01@fpt.edu.vn` | Có | Guest judge, đủ criteria (draft `isFinal=false`) |
| GD5-02 CK SUBMITTED chưa chấm | `student.gd5.leader02@fpt.edu.vn` | Có | — test **5.2 POST /scores** |
| GD5-03 ADVANCED chưa nộp CK | `student.gd5.leader03@fpt.edu.vn` | — | test **5.1 POST /submissions** |
| GD5-04 ADVANCED chưa nộp CK (dự phòng) | `student.gd5.leader04@fpt.edu.vn` | — | dự phòng |

### Postman variables

| Variable | Cách lấy |
|----------|----------|
| `hackathonId` | GET hackathon slug `seal-gd5-final-active` |
| `finalRoundId` | round `isFinal=true` |
| `finalCriterionId` | `GET /rounds/{{finalRoundId}}/criteria` → `items[0].id` |
| `teamId` | log `t2=` hoặc `t3=` |
| `submissionId` | log `finalSubmissionId(t2)=` cho 5.2 |
| `judgeToken` | login `guestjudge@gmail.com` |

### Luồng API (5.1 → 5.4)

1. **5.1** Login `student.gd5.leader03@...` → POST `/submissions`:
   ```json
   {
     "teamId": "{{teamId}}",
     "roundId": "{{finalRoundId}}",
     "repoUrl": "https://github.com/org/final-repo",
     "demoUrl": "https://demo.example.com/final",
     "reportUrl": "https://docs.example.com/final-report",
     "slideUrl": "https://slides.example.com/final"
   }
   ```
2. **5.2** Login guest judge → POST `/scores` với `submissionId` team 02, `criterionId` CK
3. **5.2b** (tùy chọn) POST `/scores/calibration` — `calibrationSessionId` từ GET calibration sessions round CK
4. **5.3** `PATCH /rounds/{{finalRoundId}}/lock-scoring` `{ "force": false, "reason": null }`
5. **5.4** `GET /hackathons/{{hackathonId}}` → sau lock CK tự chuyển **`PENDING_CONFIRM`** (nếu business rule bật)

**Snapshot sau lock:** slug `seal-gd6-pending-confirm` (GĐ6 seed).

---

## SQL verify nhanh

```sql
SELECT h.slug, h.status, r.name, r.is_final, r.scoring_locked, r.is_published, r.is_active
FROM hackathons h
JOIN rounds r ON r.hackathon_id = h.id
WHERE h.slug IN ('seal-gd4-advance-ready', 'seal-gd5-final-active')
ORDER BY h.slug, r.is_final;
```

```sql
SELECT t.team_name, s.status, s.round_id, s.track_id
FROM teams t
LEFT JOIN submissions s ON s.team_id = t.id
JOIN hackathons h ON h.id = t.hackathon_id
WHERE h.slug = 'seal-gd5-final-active'
ORDER BY t.team_name;
```
