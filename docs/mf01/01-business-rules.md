SEAL HACKATHON MANAGEMENT SYSTEM
MF-01 — Giai đoạn 1: Chuẩn bị sự kiện
Phân tích bảng DB theo từng Functional Requirement
Phiên bản: 3.0 — Đồng bộ Workflow v5.0 · DB Schema v3.0 (MySQL 8) · Kiến trúc: Hackathon → Round → Track (Breaking Change)

BA · SA · QA Engineer · SRE · Solution Architect · Data Engineer · PM

**Nguồn sự thật (normative):**

| Tài liệu | Vai trò |
|----------|---------|
| [../system/workflow.md](../system/workflow.md) v5.0 | Luồng vận hành 6 giai đoạn — GĐ1 Bước 1–7 |
| [schema-v3.0-mysql.md](../db/schema-v3.0-mysql.md) | DDL MySQL 8, trigger, index, BC-01..11 |
| [.cursor/rules/database-schema.mdc](../../.cursor/rules/database-schema.mdc) | Quy ước JPA / entity |

**Quy tắc ưu tiên khi mâu thuẫn:** `schema-v3.0-mysql.md` > `02-functional-requirements.md` > `../system/workflow.md` (đoạn cũ).

---

## 0. Ma trận audit FR-01 … FR-07B (v3.0)

| FR | Workflow GĐ1 | DB v3.0 | mf01 § | Trạng thái | Ghi chú chỉnh |
|----|--------------|---------|--------|------------|---------------|
| FR-01 Hackathon | Bước 1 | hackathons | §2 | **PASS** | JSON (không JSONB); `updated_at` MySQL |
| FR-02 Round | Bước 2 | BC-01 rounds | §3 | **PASS** | API target: `POST .../hackathons/{id}/rounds` |
| FR-03 Track | Bước 3 | BC-02 tracks | §4 | **PASS** | API target: `POST .../rounds/{id}/tracks`; `assigned_group` ở GĐ2 |
| FR-04 Criteria | Bước 4 | BC-03 XOR | §5 | **PASS** | 2 nhánh API track / final round |
| FR-05 Nhân sự | Bước 5 | BC-07, triggers | §6 | **PASS** | Conflict BLOCK ở DB; warn chỉ weight/events |
| FR-06 Events | Bước 6 | BC-09 | §7 | **PASS** | REMINDER sync trong transaction (hiện tại) |
| FR-07 Status | Bước 7 | Gate G1–G5 | §8 | **PASS** | Thêm `GET .../readiness` §10 |
| FR-07B Activate | GĐ3 ref | rounds.is_active | §9 | **PASS** | Phạm vi doc GĐ1; runtime GĐ3 |

**Gate [G1–G5]** khớp workflow Bước 7 và §1.4. **Implementation drift** (route URL cũ, readiness logic cũ): xem §14 — không là normative.

---

Changelog v2.2 → v3.0
Mã thay đổi	Loại	Nội dung
[ARCH-01]	BREAKING	Đảo kiến trúc hoàn toàn: Hackathon → Round → Track (cũ: Hackathon → Track → Round). Toàn bộ luồng tạo cấu trúc ở GĐ1 thay đổi thứ tự.
[ARCH-02]	BREAKING	Round là con trực tiếp của Hackathon (rounds.hackathon_id). Track là con của Round (tracks.round_id).
[FR-02↔03]	BREAKING	FR-02 và FR-03 hoán đổi vị trí: FR-02 nay là Tạo Round (dưới Hackathon); FR-03 nay là Tạo Track (trong Round).
[FR-04-XOR]	BREAKING	criteria dùng XOR FK: track_id cho Sơ loại, round_id cho Chung kết. Không còn round_id duy nhất. Trigger trg_check_criteria_round_is_final enforce tại DB-layer.
[FR-05-BLOCK]	BREAKING	Conflict Mentor↔Judge BLOCK CỨNG (cũ: chỉ warn). Trigger trg_check_mentor_judge_conflict_ins/upd + trg_check_judge_mentor_conflict_ins enforce tại DB-layer.
[FR-05-FINAL]	MỚI	Rule Chung kết: 100% Judge EXTERNAL. INTERNAL chỉ được ngoại lệ nếu is_dept_head=TRUE AND không có mentor_assignment trong kỳ, kèm xác nhận tường minh.
[FR-05-DEPT]	MỚI	Trường users.is_dept_head BOOLEAN — flag Trưởng khoa/bộ môn. Dùng trong logic ngoại lệ Judge Chung kết.
[FR-06-ENUM]	SỬA	events.type bỏ TEAM_MEETING khỏi CHECK enum (không tồn tại thực tế).
[FR-07-GATE]	BREAKING	Gate DRAFT→ONGOING cập nhật: 5 điều kiện theo kiến trúc mới. Criteria Sơ loại validate qua track_id; Criteria Chung kết validate qua round_id.
[FR-07B-NET]	SỬA	Safety net khi activate Round: validate weight qua criteria.track_id (Sơ loại) hoặc criteria.round_id (Chung kết).
[NEW-TRT]	MỚI	Bảng team_round_tracks (mới hoàn toàn) thay thế teams.registration_track_id, assigned_track_id, assigned_group.
[NEW-TRIGGERS]	MỚI	9+ trigger GĐ1-relevant (MySQL): trg_prevent_track_in_final_round, trg_check_mentor_judge_conflict_ins/upd, trg_check_judge_mentor_conflict_ins, trg_check_criteria_round_is_final_ins/upd, trg_check_submission_round_is_final_ins/upd, trg_check_team_track_same_hackathon_ins/upd (+ score/member lock GĐ2/3 — §13).
1. Tổng quan MF-01 — Giai đoạn Chuẩn bị Sự kiện
MF-01 bao phủ toàn bộ giai đoạn chuẩn bị — từ khi Coordinator tạo Hackathon cho đến khi chuyển trạng thái ONGOING mở cổng đăng ký. Giai đoạn này chỉ có Actor là Coordinator. Sinh viên, Judge, Mentor chưa tham gia.

1.1 Kiến trúc Hackathon → Round → Track
HACKATHON  (VD: Fall 2025 hoặc Spring 2026)
├── Round 1: Sơ loại  (round_type=PRELIMINARY, sequence_order=1)
│    ├── Track 1  (topic = chủ đề bốc thăm)
│    │    ├── Bảng A  ≤6 đội  [assigned_group="A"]
│    │    └── Bảng B  ≤6 đội  [assigned_group="B"]
│    └── Track 2  (topic = chủ đề bốc thăm)
│         ├── Bảng C  ≤6 đội
│         └── Bảng D  ≤6 đội
│
└── Round 2: Chung kết  (round_type=FINAL, is_final=TRUE)
     └── [KHÔNG CÓ TRACK — KHÔNG CÓ BẢNG]
          Pool chung 6 đội → 100% Judge EXTERNAL
Nguyên tắc thiết kế cốt lõi:

Track không phải hạng mục cố định của Hackathon — Track là bảng đấu trong một Round cụ thể.
Round Chung kết is_final=TRUE không có Track con — đây là thiết kế đúng, không phải lỗi.
criteria.track_id (Sơ loại) / criteria.round_id (Chung kết) — XOR constraint tại DB.
submissions.track_id (Sơ loại) / submissions.round_id (Chung kết) — XOR constraint tại DB.
judge_assignments.track_id (Sơ loại) / judge_assignments.round_id (Chung kết) — XOR + generated column UNIQUE (MySQL — xem schema §1.1).
1.2 Actor và phân quyền
Coordinator là quyền CỐ ĐỊNH: users.role='COORDINATOR' AND users.status='APPROVED'. Không phải quyền tạm thời theo sự kiện. Một Coordinator có quyền trên mọi Hackathon trong hệ thống. Mọi hành động đều qua middleware JWT kiểm tra role + status trước khi xử lý.

Ghi chú tương lai: Nếu cần phân quyền Coordinator theo từng Hackathon riêng → bổ sung bảng hackathon_coordinators(hackathon_id, user_id). DB v3.0 hiện chưa có.

1.3 Luồng chính GĐ1 (tóm tắt theo kiến trúc mới)
Bước	Hành động	Đầu ra DB
1	Tạo Hackathon	hackathons (status=DRAFT)
2	Tạo Rounds (Sơ loại + Chung kết)	rounds (hackathon_id FK)
3	Tạo Tracks trong Round Sơ loại	tracks (round_id FK)
4	Thiết lập Criteria cho Track & Round Chung kết	criteria (XOR: track_id hoặc round_id)
5	Quản lý nhân sự	users, invitations, mentor_assignments, judge_assignments
6	Lên lịch sự kiện	events, notifications
7	Chuyển DRAFT → ONGOING (gate cứng 5 điều kiện)	hackathons.status=ONGOING
Thứ tự bắt buộc: Bước 2 (Round) trước Bước 3 (Track). Track phải biết mình thuộc Round nào. Bước 4 (Criteria) sau Bước 3. Không validate weight ở Bước 2 và 3.

1.4 Điều kiện Gate chuyển GĐ2 (Gate Condition)
hackathons.status = ONGOING
  ├── [G1] ≥1 Round PRELIMINARY tồn tại VÀ có ≥1 Track con
  ├── [G2] Đúng 1 Round FINAL (is_final=TRUE) — không cần Track con
  ├── [G3] Mọi Track (Round Sơ loại) có Criteria, SUM(weight) = 1.0 (±0.001)
  ├── [G4] Round Chung kết có Criteria (criteria.round_id), SUM(weight) = 1.0 (±0.001)
  └── [G5] ≥1 event type=KICKOFF tồn tại và hợp lệ
1.5 Các bảng DB tham gia MF-01 (v3.0)
Bảng	Vai trò trong MF-01	Ghi chú DB v3.0
hackathons	Bảng chính — định nghĩa kỳ thi	status: DRAFT→ONGOING→PENDING_CONFIRM→FINISHED
rounds	[BC-01] Vòng thi — con của Hackathon	hackathon_id FK; is_final, round_type, late_submission_policy mới
tracks	[BC-02] Bảng đấu — con của Round	round_id FK (thay hackathon_id); topic, sequence_order mới
criteria	[BC-03] Tiêu chí chấm điểm	XOR FK: track_id (Sơ loại) / round_id (Chung kết)
users	Xác thực Coordinator; tạo Judge tạm	is_dept_head BOOLEAN mới; is_temp_account cho Judge EXTERNAL
invitations	Thư mời judge khách (TTL 3 ngày)	FR-12 team — backlog; xem ../mf02/02-invitations.md
mentor_assignments	Phân công Mentor → Track Sơ loại	UNIQUE(mentor_id, track_id); track_id FK → tracks mới
judge_assignments	[BC-07] Phân công Judge → Track/Round	XOR FK; FINAL_EXTERNAL type; generated column UNIQUE (`track_uk`, `round_uk`)
events	Lịch sự kiện	[BC-09] Bỏ TEAM_MEETING khỏi enum
notifications	REMINDER tự động khi tạo event	type=REMINDER
audit_logs	Lịch sử mọi hành động	action, target_table, detail JSON (MySQL)
team_round_tracks	[BC-04] Phân công đội → Track (GĐ2)	Bảng mới; `assigned_group` tại đây (không còn trên teams). GĐ1 chỉ tham chiếu
wildcard_reviews	[BC-10] Wild Card	GĐ4+; có `track_id` — ngoài phạm vi API MF-01
