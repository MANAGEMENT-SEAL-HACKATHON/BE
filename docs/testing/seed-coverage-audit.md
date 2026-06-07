# Seed coverage audit — GĐ0 → GĐ6

**Cập nhật:** 2026-05-29  
**Profile:** `dev` (`spring.profiles.active=dev`)

Tài liệu này mô tả **dữ liệu seed tự động** sau mỗi lần start app, slug hackathon dùng cho test, và **script SQL** để QA verify.

---

## 1. Bản đồ hackathon seed

| Slug | Giai đoạn | Hackathon status | Mục đích test |
|------|-----------|------------------|---------------|
| `seal-gd1-incomplete` | GĐ1 | `DRAFT` | Readiness **FAIL** |
| `seal-gd1-ready` | GĐ1 | `DRAFT` | Readiness **PASS** → `PATCH` ONGOING |
| `seal-spring-2026` | GĐ1–2 | `ONGOING` | Teams GĐ2 (`GD2-*`), **không** bị Gd4 ghi đè (Gd4 tắt mặc định) |
| `seal-gd3-prelim-open` | GĐ3 | `ONGOING` | Sơ loại **active**, đã phát đề, **chưa** lock/publish |
| `seal-gd4-tiebreak-wildcard` | GĐ4 | `ONGOING` | Tiebreak 3-way + wildcard — **chỉ khi** `app.seed.gd4.enabled=true` |
| `seal-gd5-final-active` | **GĐ5** | `ONGOING` | CK **active**, chưa lock CK — nộp/chấm Chung kết |
| `seal-gd6-pending-confirm` | GĐ6 | `PENDING_CONFIRM` | CK locked, giải Nhất mẫu, confirm FINISHED |
| `seal-fall-2025-finished` | Archive | `FINISHED` | Read-only, rounds đã lock |

---

## 2. Seeder & thứ tự chạy

| Class | Khi chạy | Idempotent marker |
|-------|-----------|-------------------|
| `Gd1DataSeeder` | `CommandLineRunner` @Order(2) | slug `seal-spring-2026` |
| `Gd2DataSeeder` | sau GĐ1 | team `GD2-01...` |
| `Gd3DataSeeder` | sau GĐ2 | team `GD3-01 SUBMITTED...` |
| `Gd5FinalRoundDataSeeder` | sau GĐ3 | team `GD5-01 CK SUBMITTED + scored` |
| `Gd6PendingConfirmDataSeeder` | sau GĐ5 | team `GD6-01 ADVANCED CK` |
| `Gd4TestDataSeeder` | `ApplicationReady` | slug `seal-gd4-tiebreak-wildcard` + flag |

**Cấu hình GĐ4 (mặc định tắt):**

```properties
# application-dev.properties
app.seed.gd4.enabled=false
```

Bật tạm (PowerShell):

```powershell
$env:APP_SEED_GD4_ENABLED="true"
# restart Spring Boot
```

---

## 3. Chi tiết theo giai đoạn

### GĐ1 — Chuẩn bị (`Gd1DataSeeder`)

- Users: Coordinator, Judge×2, Guest, Mentor, Pending judge
- Password: xem log `[Gd1DataSeeder] Dev login credentials`

### GĐ2 — Đội (`Gd2DataSeeder` trên `seal-spring-2026`)

- 9 đội `GD2-*`: PENDING, ACTIVE, REJECTED, ELIMINATED, `isLocked`, member PENDING/LEFT
- Doc: [mf02/05-test-data-gd2-teams.md](../mf02/05-test-data-gd2-teams.md)

### GĐ3 — Sơ loại (`Gd3DataSeeder` → `seal-gd3-prelim-open`)

| Team | Submission status | Ghi chú |
|------|-------------------|---------|
| `GD3-01 SUBMITTED + scored` | `SUBMITTED` | 2 judge NORMAL |
| `GD3-02 LATE_PENDING` | `LATE_PENDING` | `isLate=true` |
| `GD3-03 LATE_APPROVED` | `LATE_APPROVED` | `isLate=true` |
| `GD3-04 chưa nộp bài` | — | Test POST submit |

- Calibration: 1 phiên `OPEN`, 1 `CLOSED`
- Prelim: `is_active=true`, `scoring_locked=false`, `is_published=false`

**Sinh viên:** `student.gd3.leader01@fpt.edu.vn` … `leader04@fpt.edu.vn` — password `Student@dev1`

### GĐ4 — Chuyển vòng (opt-in `Gd4TestDataSeeder` → `seal-gd4-tiebreak-wildcard`)

- 5 đội `GD4-*`, Bảng A đồng điểm 10.0 (tiebreak), Bảng B wildcard
- Prelim: `scoring_locked=true`, `is_published=true`, `top_n_advance=1`, `min_teams_final=3`

### GĐ5 — Chung kết (`Gd5FinalRoundDataSeeder` → `seal-gd5-final-active`)

| Team | CK submission | Chấm CK (guest judge) |
|------|---------------|------------------------|
| `GD5-01 CK SUBMITTED + scored` | Có | Có |
| `GD5-02 CK SUBMITTED chưa chấm` | Có | — (test POST `/scores`) |
| `GD5-03 ADVANCED chưa nộp CK` | — | — (test POST `/submissions` roundId=CK) |
| `GD5-04 ADVANCED chưa nộp CK (dự phòng)` | — | — |

| Vòng / Hackathon | Trạng thái seed |
|------------------|-----------------|
| Hackathon | `ONGOING` (chưa `PENDING_CONFIRM`) |
| Sơ loại | published + scoring locked |
| Chung kết | **active**, đã phát đề, **scoring_locked = false** |
| Judge CK | `FINAL_EXTERNAL` (guest judge) — tự gán khi tạo structure |

**Luồng test gợi ý:** lock CK → `PATCH` hackathon `PENDING_CONFIRM` (hoặc chuyển sang slug `seal-gd6-pending-confirm`).

**Sinh viên:** `student.gd5.leader01@fpt.edu.vn` … `leader04@fpt.edu.vn` — `Student@dev1`

### GĐ6 — Kết thúc (`Gd6PendingConfirmDataSeeder` → `seal-gd6-pending-confirm`)

| Trạng thái | Giá trị seed |
|------------|--------------|
| Hackathon | `PENDING_CONFIRM` |
| Sơ loại | published + scoring locked |
| Chung kết | active + **scoring locked** |
| Teams | 3× `GD6-0x ADVANCED CK` |
| Submissions | prelim + final `SUBMITTED` mỗi đội |
| Prizes | `FIRST` cho team 01 — còn slot `SECOND` cho team 02 |

**Sinh viên:** `student.gd6.leader01@fpt.edu.vn` … `leader03@fpt.edu.vn` — `Student@dev1`

---

## 4. Script SQL verify

Chạy trên database `SealHackathon` (MySQL). Thay `:slug` nếu cần.

### 4.1 Tổng quan hackathon seed

```sql
SELECT id, slug, status, season, year
FROM hackathons
WHERE slug IN (
  'seal-gd1-incomplete',
  'seal-gd1-ready',
  'seal-spring-2026',
  'seal-gd3-prelim-open',
  'seal-gd4-tiebreak-wildcard',
  'seal-gd5-final-active',
  'seal-gd6-pending-confirm',
  'seal-fall-2025-finished'
)
ORDER BY slug;
```

**Kỳ vọng:** 8 dòng (hoặc 7 nếu chưa bật Gd4).

### 4.2 Round — trạng thái vòng theo slug

```sql
SELECT h.slug,
       r.name,
       r.is_final,
       r.is_active,
       r.scoring_locked,
       r.is_published,
       r.top_n_advance,
       r.min_teams_final
FROM rounds r
JOIN hackathons h ON h.id = r.hackathon_id
WHERE h.slug IN ('seal-spring-2026', 'seal-gd3-prelim-open', 'seal-gd4-tiebreak-wildcard', 'seal-gd5-final-active', 'seal-gd6-pending-confirm')
ORDER BY h.slug, r.sequence_order;
```

**Kỳ vọng nhanh:**

| slug | Sơ loại active | Sơ loại locked | CK active |
|------|----------------|----------------|-----------|
| `seal-gd3-prelim-open` | 1 | 0 | 0 |
| `seal-gd4-tiebreak-wildcard` | 0 | 1 | 0 |
| `seal-gd5-final-active` | 0 | 1 | 1 (CK **chưa** lock) |
| `seal-gd6-pending-confirm` | 0 | 1 | 1 (CK **đã** lock) |

### 4.3 Teams theo prefix

```sql
SELECT h.slug, t.team_name, t.status, t.is_locked
FROM teams t
JOIN hackathons h ON h.id = t.hackathon_id
WHERE t.team_name LIKE 'GD2-%'
   OR t.team_name LIKE 'GD3-%'
   OR t.team_name LIKE 'GD4-%'
   OR t.team_name LIKE 'GD5-%'
   OR t.team_name LIKE 'GD6-%'
ORDER BY h.slug, t.team_name;
```

### 4.4 Submissions — đa status (GĐ3)

```sql
SELECT h.slug, t.team_name, s.status, s.is_late, r.name AS round_name
FROM submissions s
JOIN teams t ON t.id = s.team_id
JOIN rounds r ON r.id = s.round_id
JOIN hackathons h ON h.id = s.hackathon_id
WHERE h.slug = 'seal-gd3-prelim-open'
ORDER BY t.team_name;
```

**Kỳ vọng:** 3 dòng — SUBMITTED, LATE_PENDING, LATE_APPROVED; team `GD3-04` không có row.

### 4.5 Submissions Chung kết (GĐ5)

```sql
SELECT h.slug, t.team_name, s.status, s.track_id IS NULL AS is_final_shape, r.is_final
FROM submissions s
JOIN teams t ON t.id = s.team_id
JOIN rounds r ON r.id = s.round_id
JOIN hackathons h ON h.id = s.hackathon_id
WHERE h.slug = 'seal-gd5-final-active' AND r.is_final = 1
ORDER BY t.team_name;
```

**Kỳ vọng:** 2 dòng (team 01, 02); team `GD5-03`, `GD5-04` không có submission CK.

```sql
SELECT h.slug, r.name, r.is_active, r.scoring_locked, r.problem_released_at IS NOT NULL AS has_problem
FROM rounds r
JOIN hackathons h ON h.id = r.hackathon_id
WHERE h.slug = 'seal-gd5-final-active'
ORDER BY r.sequence_order;
```

**Kỳ vọng:** CK — `is_active=1`, `scoring_locked=0`, `has_problem=1`.

### 4.6 Participation ADVANCED (GĐ6)

```sql
SELECT h.slug, t.team_name, trt.participation_status, r.name AS round_name
FROM team_round_tracks trt
JOIN teams t ON t.id = trt.team_id
JOIN tracks tk ON tk.id = trt.track_id
JOIN rounds r ON r.id = tk.round_id
JOIN hackathons h ON h.id = t.hackathon_id
WHERE h.slug = 'seal-gd6-pending-confirm'
ORDER BY t.team_name;
```

**Kỳ vọng:** 3 đội `ADVANCED` trên Sơ loại; có `team_round_participation` cho round CK.

```sql
SELECT t.team_name, r.name, trp.id
FROM team_round_participation trp
JOIN teams t ON t.id = trp.team_id
JOIN rounds r ON r.id = trp.round_id
JOIN hackathons h ON h.id = trp.hackathon_id
WHERE h.slug = 'seal-gd6-pending-confirm' AND r.is_final = 1;
```

**Kỳ vọng:** 3 dòng (đội tham gia Chung kết).

### 4.7 Prizes (GĐ6)

```sql
SELECT h.slug, t.team_name, p.prize_rank, p.prize_name
FROM prizes p
JOIN teams t ON t.id = p.team_id
JOIN hackathons h ON h.id = p.hackathon_id
WHERE h.slug = 'seal-gd6-pending-confirm';
```

**Kỳ vọng:** 1 dòng `FIRST` — team `GD6-01`.

### 4.8 Calibration (GĐ3)

```sql
SELECT cs.id, cs.status, cs.target_score, r.name
FROM calibration_sessions cs
JOIN rounds r ON r.id = cs.round_id
JOIN hackathons h ON h.id = r.hackathon_id
WHERE h.slug = 'seal-gd3-prelim-open';
```

**Kỳ vọng:** 2 phiên — `OPEN` và `CLOSED`.

### 4.9 Điểm tiebreak (GĐ4 — khi bật flag)

```sql
SELECT t.team_name, trt.assigned_group, sc.score_value
FROM scores sc
JOIN submissions sub ON sub.id = sc.submission_id
JOIN teams t ON t.id = sub.team_id
JOIN rounds r ON r.id = sub.round_id
JOIN hackathons h ON h.id = r.hackathon_id
LEFT JOIN team_round_tracks trt ON trt.team_id = t.id
WHERE h.slug = 'seal-gd4-tiebreak-wildcard'
  AND sc.score_type = 'NORMAL'
ORDER BY trt.assigned_group, t.team_name;
```

**Kỳ vọng:** Bảng A — 3 đội điểm 10.0; Bảng B — 9.5 và 9.0.

---

## 5. Map slug → API test (Postman)

| Mục tiêu | Slug | Token gợi ý |
|----------|------|-------------|
| Duyệt đội / lottery | `seal-spring-2026` | Coordinator |
| Activate / submit / score | `seal-gd3-prelim-open` | Coord + `student.gd3.leader04@...` |
| Tiebreak / wildcard / advance | `seal-gd4-tiebreak-wildcard` | Coordinator (bật Gd4) |
| Nộp/chấm CK, lock CK | `seal-gd5-final-active` | `student.gd5.*` + guest judge |
| Trao giải / confirm | `seal-gd6-pending-confirm` | Coordinator |
| E2E greenfield | slug mới (playbook Phần II) | — |

Playbook API: [full-workflow-api-test-gd1-gd6.md](full-workflow-api-test-gd1-gd6.md)

---

## 6. Reset seed (dev)

### GĐ2 (giữ GĐ1)

Xem [mf02/05-test-data-gd2-teams.md §9](../mf02/05-test-data-gd2-teams.md#9-reset--seed-lại-gđ2).

### GĐ3 / GĐ5 / GĐ6 / GĐ4

```sql
-- Thay slug tương ứng
SET @slug = 'seal-gd3-prelim-open';
SET @hid = (SELECT id FROM hackathons WHERE slug = @slug LIMIT 1);

DELETE sc FROM scores sc
JOIN submissions s ON s.id = sc.submission_id
WHERE s.hackathon_id = @hid;

DELETE FROM submissions WHERE hackathon_id = @hid;
DELETE FROM calibration_sessions WHERE round_id IN (SELECT id FROM rounds WHERE hackathon_id = @hid);
DELETE tm FROM team_members tm JOIN teams t ON t.id = tm.team_id WHERE t.hackathon_id = @hid;
DELETE FROM team_round_tracks WHERE team_id IN (SELECT id FROM teams WHERE hackathon_id = @hid);
DELETE FROM team_round_participation WHERE hackathon_id = @hid;
DELETE FROM mentor_team_assignments WHERE hackathon_id = @hid;
DELETE FROM teams WHERE hackathon_id = @hid;
DELETE FROM users WHERE email LIKE 'student.gd3.%' OR email LIKE 'student.gd5.%' OR email LIKE 'student.gd6.%' OR email LIKE 'student.gd4.%';
DELETE FROM prizes WHERE hackathon_id = @hid;
DELETE ja FROM judge_assignments ja
  LEFT JOIN tracks tk ON ja.track_id = tk.id
  LEFT JOIN rounds r ON ja.round_id = r.id OR tk.round_id = r.id
WHERE r.hackathon_id = @hid OR ja.round_id IN (SELECT id FROM rounds WHERE hackathon_id = @hid);
DELETE FROM criteria WHERE round_id IN (SELECT id FROM rounds WHERE hackathon_id = @hid)
   OR track_id IN (SELECT id FROM tracks WHERE round_id IN (SELECT id FROM rounds WHERE hackathon_id = @hid));
DELETE FROM tracks WHERE round_id IN (SELECT id FROM rounds WHERE hackathon_id = @hid);
DELETE FROM rounds WHERE hackathon_id = @hid;
DELETE FROM events WHERE hackathon_id = @hid;
DELETE FROM hackathons WHERE id = @hid;
```

Restart app (`profile=dev`) → seeder tạo lại.

---

## 7. Checklist QA sau start

- [ ] Log `[Gd3DataSeeder] Seed GĐ3` xuất hiện
- [ ] Log `[Gd5FinalRoundDataSeeder]` xuất hiện
- [ ] Log `[Gd6PendingConfirmDataSeeder]` xuất hiện
- [ ] **Không** log `[Gd4TestDataSeeder]` khi `app.seed.gd4.enabled=false`
- [ ] SQL §4.1 trả đủ slug
- [ ] `seal-spring-2026` — prelim **không** bị lock (nếu chưa từng bật Gd4 cũ trên slug này)
- [ ] `seal-gd3-prelim-open` — §4.4 đúng 3 submission status
- [ ] `seal-gd5-final-active` — §4.5 CK active, chưa lock, 2 submission CK

---

**Changelog:** 2026-05-29 — Thêm Gd3/Gd5/Gd6 seed, Gd4 opt-in, tách slug, SQL verify.
