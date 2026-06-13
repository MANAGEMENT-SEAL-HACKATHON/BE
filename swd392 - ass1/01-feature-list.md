# 01 — Feature List (Danh sách tính năng)

> **Hệ thống:** SEAL Hackathon Management System (BE)  
> **Phạm vi:** Toàn bộ tính năng đã implement trên API server tính đến hiện tại.

---

## Tổng quan số liệu

| Hạng mục | Số lượng |
|----------|----------|
| REST Controllers | 37 |
| REST Endpoints (ước tính) | ~161 |
| JPA Entities | 35 |
| User roles | 4 (Coordinator, Student, Judge, Mentor) |
| WebSocket topics | 5 |
| Giai đoạn nghiệp vụ | GĐ1 → GĐ6 |

---

## FL-00 — Hạ tầng & hệ thống

| ID | Tính năng | Mô tả | API / Ghi chú |
|----|-----------|-------|---------------|
| FL-00.1 | Health check | Kiểm tra server sống | `GET /` |
| FL-00.2 | Swagger / OpenAPI | Tài liệu API tự sinh | `/swagger-ui`, `/v3/api-docs` |
| FL-00.3 | Audit log | Ghi hành động quan trọng (create, assign, score…) | Service layer → `audit_logs` |
| FL-00.4 | Object storage | Lưu slide PDF, thẻ SV, export CSV, certificate PDF | MinIO hoặc local (`app.storage.*`) |
| FL-00.5 | Exception handling | Envelope lỗi chuẩn `{ success, error, traceId }` | `GlobalExceptionHandler` |
| FL-00.6 | Dev seed data | Seed GĐ1/GĐ2/GĐ3 cho test | `Gd1DataSeeder`, `Gd2DataSeeder`, `Gd3DataSeeder` |

---

## FL-01 — Xác thực & tài khoản (GĐ2)

| ID | Tính năng | Actor | API |
|----|-----------|-------|-----|
| FL-01.1 | Đăng ký tài khoản Student | Public | `POST /auth/register` |
| FL-01.2 | Đăng nhập email/password | Public | `POST /auth/login` |
| FL-01.3 | Refresh JWT (rotation) | Authenticated | `POST /auth/refresh` |
| FL-01.4 | Đăng xuất / thu hồi token | Approved | `POST /auth/logout`, `/logout-all` |
| FL-01.5 | Quên mật khẩu | Public | `POST /auth/forgot-password` |
| FL-01.6 | Đặt lại mật khẩu | Public | `POST /auth/reset-password` |
| FL-01.7 | Đổi mật khẩu | Approved | `POST /auth/change-password` |
| FL-01.8 | OAuth Google login | Public | `POST /auth/oauth/google` |
| FL-01.9 | OAuth GitHub login | Public | `POST /auth/oauth/github/code` |
| FL-01.10 | Liên kết / hủy liên kết OAuth | Approved | `POST /auth/oauth/*/link`, `/unlink` |
| FL-01.11 | Xem / cập nhật profile | Authenticated | `GET/PATCH /users/me` |
| FL-01.12 | Upload / tải thẻ sinh viên | Student | `POST/GET /users/me/student-card` |
| FL-01.13 | Duyệt / từ chối user | Coordinator | `PATCH /users/{id}/status` |
| FL-01.14 | Danh sách & chi tiết user | Coordinator | `GET /users`, `GET /users/{id}` |
| FL-01.15 | Tạo guest judge (temp) | Coordinator | `POST /users/temp-judges` |
| FL-01.16 | Gửi lại email mời judge | Coordinator | `POST /invitations/{id}/resend` |
| FL-01.17 | Thông báo in-app | Approved | `GET/PATCH /me/notifications` |

---

## FL-02 — Quản lý Hackathon & cấu hình sự kiện (GĐ1)

| ID | Tính năng | Actor | API |
|----|-----------|-------|-----|
| FL-02.1 | Tạo hackathon | Coordinator | `POST /hackathons` |
| FL-02.2 | Sửa / xóa / tìm hackathon | Coordinator | `PUT/DELETE/GET /hackathons` |
| FL-02.3 | Hackathon đang diễn ra | Authenticated | `GET /hackathons/active` |
| FL-02.4 | Kiểm tra readiness trước đổi status | Coordinator | `GET /hackathons/{id}/readiness` |
| FL-02.5 | Chuyển trạng thái hackathon | Coordinator | `PATCH /hackathons/{id}/status` |
| FL-02.6 | State machine: DRAFT → ONGOING → PENDING_CONFIRM → FINISHED | Coordinator | Business rule |
| FL-02.7 | Tạo / sửa / xóa Round | Coordinator | `/hackathons/{id}/rounds`, `/rounds/{id}` |
| FL-02.8 | Round Sơ loại vs Chung kết (`is_final`) | Coordinator | Entity `rounds` |
| FL-02.9 | Kích hoạt vòng thi (validate weight criteria = 1) | Coordinator | `PATCH /rounds/{id}/activate` |
| FL-02.10 | Tạo / sửa / xóa Track | Coordinator | `/rounds/{id}/tracks`, `/tracks/{id}` |
| FL-02.11 | Tạo / clone / batch Criteria | Coordinator | `/tracks/{id}/criteria`, `/rounds/{id}/criteria` |
| FL-02.12 | Weight summary criteria | Coordinator | `GET .../criteria/weight-summary` |
| FL-02.13 | Timeline sự kiện (workshop, kickoff…) | Coordinator | `/hackathons/{id}/events` |
| FL-02.14 | Phân công Judge cho Track / Final | Coordinator | `/judge-assignments` |
| FL-02.15 | Phân công Mentor cho Track | Coordinator | `/mentor-assignments` |
| FL-02.16 | Judge HEAD / NORMAL trên track | Coordinator | `JudgeAssignmentType` |
| FL-02.17 | Cấm Judge + Mentor cùng track | System | DB trigger + guard |

---

## FL-03 — Đội & đăng ký (GĐ2)

| ID | Tính năng | Actor | API |
|----|-----------|-------|-----|
| FL-03.1 | Student tạo đội | Student | `POST /teams` |
| FL-03.2 | Coordinator duyệt / từ chối đội | Coordinator | `PATCH /teams/{id}/status`, `/approve` |
| FL-03.3 | Mời / chấp nhận / rời thành viên | Student | `/teams/{id}/members/*` |
| FL-03.4 | Chuyển leader | Team leader | `PATCH /teams/{id}/transfer-leader` |
| FL-03.5 | Giải tán đội | Leader / Coord | `DELETE /teams/{id}` |
| FL-03.6 | Lock đội sau hạn đăng ký | System | `teams.is_locked` |
| FL-03.7 | Bốc thăm track (lottery) | Coordinator | `PATCH /hackathons/{id}/lottery` |
| FL-03.8 | Gán track / bảng cho đội | Coordinator | Lottery assignments |
| FL-03.9 | Đăng ký hackathon (browse/register) | Student | `/me/hackathons/browse`, `/register` |
| FL-03.10 | Xem hành trình đội qua các vòng | Authenticated | `GET /teams/{id}/journey` |
| FL-03.11 | Gán mentor theo round cho đội | Coordinator | `POST /teams/{id}/rounds/{roundId}/mentor` |
| FL-03.12 | Loại đội thủ công | Coordinator | `PATCH /teams/{id}/eliminate` |
| FL-03.13 | Duyệt hàng loạt đội đủ điều kiện | Coordinator | `POST /teams/bulk-approve` (3–5 thành viên) |

---

## FL-04 — Nộp bài & submission (GĐ3)

| ID | Tính năng | Actor | API |
|----|-----------|-------|-----|
| FL-04.1 | Nộp bài multipart (PDF + GitHub repo) | Student | `POST /submissions` |
| FL-04.2 | Validate repo GitHub public | System | `GitHubRepoValidator` |
| FL-04.3 | Nộp trễ → LATE_PENDING | Student | Auto khi quá deadline |
| FL-04.4 | Coordinator duyệt / từ chối bài trễ | Coordinator | `PATCH /submissions/{id}/approve\|reject` |
| FL-04.5 | Xem / tải slide PDF | Student/Judge/Mentor/Coord | `GET /submissions/{id}/slide` |
| FL-04.6 | Danh sách submission theo status | Coordinator | `GET /submissions?status=` |
| FL-04.7 | Student xem bài nộp của đội | Student | `GET /me/submission` |
| FL-04.8 | Phát đề bài round | Coordinator | `PATCH /rounds/{id}/release-problem` |
| FL-04.9 | Student xem đề & deadline | Student | `GET /me/rounds/{id}/problem`, `/current/deadline` |
| FL-04.10 | Nộp lại (upsert) | Student | `POST /submissions` cùng teamId + trackId |

---

## FL-05 — Presentation queue & timer (GĐ3)

| ID | Tính năng | Actor | API |
|----|-----------|-------|-----|
| FL-05.1 | Shuffle thứ tự thuyết trình (Fisher-Yates) | Coordinator | `POST /presentation/queue/shuffle` |
| FL-05.2 | Xem queue theo track / final | Approved | `GET /presentation/queue` |
| FL-05.3 | Chuyển đội tiếp theo (next) | Judge controller | `PATCH /presentation/queue/next` |
| FL-05.4 | Guard next: phải có điểm trước khi chuyển | System | `PresentationNextScoringGuard` |
| FL-05.5 | Acknowledge thiếu judge chấm | Judge controller | `acknowledgeIncompleteScoring` |
| FL-05.6 | Timer start / pause / resume / QA / reset | Judge controller | `POST /presentation/timer/*` |
| FL-05.7 | Phase SETUP sau next (chờ setup máy) | System | `PresentationTimerPhase.SETUP` |
| FL-05.8 | Grant / revoke presentation controller | Coordinator | `/presentation/tracks/{id}/controller` |
| FL-05.9 | Judge HEAD mặc định là controller | System | `PresentationControllerGuard` |
| FL-05.10 | WS đồng bộ queue realtime | Subscribers | `/topic/.../presentation-queue` |

---

## FL-06 — Chấm điểm (GĐ3–GĐ5)

| ID | Tính năng | Actor | API |
|----|-----------|-------|-----|
| FL-06.1 | Judge chấm điểm NORMAL (upsert) | Judge/Mentor | `POST /scores` |
| FL-06.2 | Gate chấm: round JUDGING + slot PRESENTING + timer mở | System | `SCORING_NOT_OPEN` |
| FL-06.3 | Judge list submission ẩn danh (#displayCode) | Judge | `GET /me/judge/submissions` |
| FL-06.4 | Xem điểm đã chấm | Judge | `GET /me/scores` |
| FL-06.5 | Sửa comment điểm | Judge | `PATCH /me/scores/{id}/comment` |
| FL-06.6 | Khóa chấm round | Coordinator | `PATCH /rounds/{id}/lock-scoring` |
| FL-06.7 | Tiến độ chấm | Coordinator | `GET /rounds/{id}/scoring-progress` |
| FL-06.8 | Ranking preview / official | Coordinator | `GET /rounds/{id}/ranking`, `/ranking/preview` |
| FL-06.9 | Chấm calibration (RBL) | Judge | `POST /scores/calibration` |
| FL-06.10 | Phiên calibration CRUD | Coordinator | `/calibration-sessions` |
| FL-06.11 | RBL variance & progress dashboard | Coordinator | `GET /rounds/{id}/rbl/*` |
| FL-06.12 | WS live score broadcast | Subscribers | `/topic/tracks/{id}/score-saved` |
| FL-06.13 | WS leaderboard preview | Subscribers | `/topic/rounds/{id}/leaderboard-preview` |

---

## FL-07 — Tiến triển vòng & Wildcard (GĐ4)

| ID | Tính năng | Actor | API |
|----|-----------|-------|-----|
| FL-07.1 | Publish kết quả vòng sơ loại | Coordinator | `PATCH /rounds/{id}/publish` |
| FL-07.2 | Danh sách ứng viên Wild Card | Coordinator | `GET /rounds/{id}/wildcard-candidates` |
| FL-07.3 | Duyệt / từ chối Wild Card | Coordinator | `PATCH /wildcard-reviews/{id}` |
| FL-07.4 | Tiebreak — danh sách hòa điểm | Coordinator | `GET /rounds/{id}/tiebreak` |
| FL-07.5 | Giải quyết tiebreak | Coordinator | `POST /rounds/{id}/tiebreak/resolve` |
| FL-07.6 | Vote tiebreak (dept head judge) | Judge dept head | `POST /me/tiebreak-evaluations` |
| FL-07.7 | Advance đội (ADVANCE / ELIMINATE) | Coordinator | `POST /rounds/{id}/advance` |
| FL-07.8 | Gán judge vòng chung kết | Coordinator | `POST /rounds/{id}/judge-assignments` |
| FL-07.9 | Public scoreboard | Public | `GET /rounds/{id}/scoreboard` |

---

## FL-08 — Portal theo vai trò

### Student (`/me/*`)

| ID | Tính năng | API |
|----|-----------|-----|
| FL-08.S1 | Xem đội, giải, danh sách chứng nhận | `/me/teams`, `/me/prizes`, `/me/certificates` |
| FL-08.S2 | Xem / tải chứng nhận PDF | `GET /me/certificates/{id}/download` (inline hoặc `?download=true`) |
| FL-08.S3 | Nộp khiếu nại (appeal) | `POST /me/appeals` |
| FL-08.S4 | Lịch sử tham gia | `GET /me/history` |
| FL-08.S5 | Xem BXH hackathon | `GET /me/hackathons/{id}/rankings` |
| FL-08.S6 | Leaderboard round đã publish | `GET /me/rounds/{id}/leaderboard` |
| FL-08.S7 | Chọn track (Fall season) | `POST /me/tracks/{id}/select` |

### Judge (`/me/*`)

| ID | Tính năng | API |
|----|-----------|-----|
| FL-08.J1 | Track / final assignments | `/me/judge-track-assignments`, `/judge-final-assignments` |
| FL-08.J2 | Lịch chấm | `GET /me/scoring-schedule` |
| FL-08.J3 | Cập nhật trạng thái hoàn thành chấm | `PATCH /me/scoring-completion` |
| FL-08.J4 | Lịch sử chấm | `GET /me/judge-history` |

### Mentor (`/me/*`)

| ID | Tính năng | API |
|----|-----------|-----|
| FL-08.M1 | Track & team assignments | `/me/mentor-track-assignments`, `/mentor-team-assignments` |
| FL-08.M2 | Danh sách đội được gán | `GET /me/mentor/rounds/{id}/assigned-teams` |
| FL-08.M3 | Xem submission / điểm đội (sau lock) | `/me/mentor/teams/{id}/submissions`, `/scores` |
| FL-08.M4 | Lịch thuyết trình final | `GET /me/mentor/rounds/{id}/schedule` |
| FL-08.M5 | BXH read-only | `GET /me/mentor/hackathons/{id}/rankings` |

---

## FL-09 — Kết thúc sự kiện (GĐ6)

| ID | Tính năng | Actor | API |
|----|-----------|-------|-----|
| FL-09.1 | Xác nhận FINISHED | Coordinator | `PATCH /hackathons/{id}/confirm` |
| FL-09.2 | BXH đội cuối cùng | Coordinator | `GET /hackathons/{id}/team-rankings` |
| FL-09.3 | BXH chapter | Coordinator | `GET /hackathons/{id}/chapter-rankings` |
| FL-09.4 | BXH cá nhân | Coordinator | `GET /hackathons/{id}/individual-rankings` |
| FL-09.5 | Trao giải | Coordinator | `POST /hackathons/{id}/prizes` |
| FL-09.6 | Thu hồi giải | Coordinator | `DELETE /prizes/{id}` |
| FL-09.7 | Export CSV / report | Coordinator | `POST /hackathons/{id}/export-jobs` |
| FL-09.8 | Tải file export (stream CSV) | Coordinator | `GET /export-jobs/{id}/download` |

---

## FL-10 — Realtime (WebSocket)

| ID | Topic | Mục đích |
|----|-------|----------|
| FL-10.1 | `/topic/rounds/{roundId}/leaderboard-preview` | BXH preview live |
| FL-10.2 | `/topic/rounds/{roundId}/scoring-progress` | Tiến độ chấm |
| FL-10.3 | `/topic/tracks/{trackId}/score-saved` | Echo khi judge chấm |
| FL-10.4 | `/topic/rounds/{roundId}/tracks/{trackId}/presentation-queue` | Queue track |
| FL-10.5 | `/topic/rounds/{roundId}/presentation-queue` | Queue chung kết |

---

## Ma trận tính năng × Giai đoạn

| Module | GĐ1 | GĐ2 | GĐ3 | GĐ4 | GĐ5 | GĐ6 |
|--------|:---:|:---:|:---:|:---:|:---:|:---:|
| Auth & Users | | ✓ | | | | |
| Hackathon config | ✓ | | | | | |
| Teams & Lottery | | ✓ | | | | |
| Submissions | | | ✓ | | | |
| Presentation | | | ✓ | | ✓ | |
| Scoring | | | ✓ | ✓ | ✓ | |
| Wildcard / Advance | | | | ✓ | ✓ | |
| Calibration / RBL | | | | | ✓ | |
| Closure / Awards | | | | | | ✓ |

---

## Backlog / stub (đã xử lý)

| ID | Trạng thái | Ghi chú |
|----|------------|---------|
| BL-01 | ✅ Done | `DELETE /prizes/{id}` — thu hồi giải (audit + guard FINISHED) |
| BL-02 | ✅ Done | `GET /export-jobs/{id}/download` — stream CSV attachment từ MinIO/local storage |
| BL-03 | ✅ Done | `GET /me/certificates/{id}/download` — xem inline / `?download=true` tải PDF; list qua `GET /me/certificates` |
| BL-04 | ✅ Done | `POST /teams/bulk-approve` — duyệt hàng loạt đội 3–5 thành viên |
| BL-05 | ✅ Done | Đã xóa deprecated: `resubmit`, `review`, `wildcard/candidates`, `wildcard/approve\|reject`, `advance-teams` |
