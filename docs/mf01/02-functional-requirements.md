# MF-01 — Functional Requirements (FR-01 … FR-07B)

**Nguồn:** Tách từ spec MF-01 v3.0. Chi tiết API: [api/](api/). Runbook: [04-quy-trinh-van-hanh.md](04-quy-trinh-van-hanh.md).

---

2. FR-01 — Tạo Hackathon mới
Mô tả: Coordinator tạo kỳ thi với các thông tin định nghĩa cơ bản. Trạng thái khởi tạo mặc định DRAFT. Sự kiện DRAFT không hiển thị với Student / Judge / Mentor.

Workflow v5.0 ref: GĐ1 — Bước 1 | DB v3.0 ref: hackathons

2.1 Bảng chính: hackathons
Trường	Kiểu / Ràng buộc	Ghi chú nghiệp vụ	Thay đổi v3.0
id	SERIAL PK	Auto-increment	—
name	VARCHAR(300) NOT NULL	Tên kỳ thi	—
slug	VARCHAR(150) UNIQUE NOT NULL	URL-friendly: "seal-spring-2026"	—
season	VARCHAR(20) CHECK IN ('Spring','Summer','Fall','Winter')	Mùa thi	—
year	INT NOT NULL	Năm tổ chức	—
status	VARCHAR(20) DEFAULT 'DRAFT' CHECK IN ('DRAFT','ONGOING','PENDING_CONFIRM','FINISHED')	State machine một chiều	—
description	TEXT	Mô tả cuộc thi; tuỳ chọn	—
rules	TEXT	Quy chế tham dự; tuỳ chọn	—
banner_url	TEXT	Ảnh banner; tuỳ chọn	—
registration_start	DATE	Ngày mở đăng ký đội	—
registration_end	DATE	Ngày đóng đăng ký đội	—
event_start	DATE	Ngày bắt đầu thi đấu	—
event_end	DATE	Ngày kết thúc / Lễ trao giải	—
wildcard_enabled	BOOLEAN NOT NULL DEFAULT FALSE	Global toggle — cả hackathon.wildcard_enabled VÀ round.wildcard_enabled phải TRUE thì Wild Card mới hoạt động	—
individual_ranking_enabled	BOOLEAN NOT NULL DEFAULT FALSE	Bật/tắt XH Cá nhân. Fall 2025=TRUE; Spring 2026=FALSE	—
chapter_scoring_formula	TEXT	JSON placeholder công thức XH Chapter (Pending #5)	—
created_by	INT FK users.id	Coordinator tạo kỳ thi	—
created_at / updated_at	DATETIME	Auto set; `updated_at` qua `@PreUpdate` / `ON UPDATE CURRENT_TIMESTAMP` (MySQL)	—
UNIQUE constraint: UNIQUE(name, season, year) — ngăn trùng kỳ thi.

2.2 Bảng liên quan
Bảng liên quan	Vai trò	Cách liên kết / Điều kiện
users	Xác thực Coordinator	role='COORDINATOR' AND status='APPROVED' — middleware JWT trước mọi request
audit_logs	Ghi lịch sử tạo	action='HACKATHON_CREATE', target_table='hackathons', detail=JSON snapshot
2.3 Ràng buộc nghiệp vụ
Ràng buộc	Tầng	Hành động khi vi phạm
UNIQUE(name, season, year)	DB + App	409 Conflict: "Kỳ thi đã tồn tại"
status chỉ được khởi tạo là DRAFT — không set ONGOING khi tạo	App	Enforce DEFAULT 'DRAFT'; ignore field status trong CREATE request
registration_end ≥ registration_start	App	422: validate date range trước INSERT
event_end ≥ event_start	App	422: validate date range trước INSERT
event_start ≥ registration_end (thi đấu sau khi đăng ký đóng)	App	422 kèm message rõ ràng
Coordinator phải role='COORDINATOR' AND status='APPROVED' — quyền cố định	API Guard (Middleware JWT)	403 Forbidden
wildcard_enabled ở hackathons = global toggle; phải AND với rounds.wildcard_enabled	App (logic)	Document rõ trong API spec; không block khi tạo
2.4 Luồng xử lý — Happy Path
1. POST /api/v1/hackathons  { name, slug, season, year, description, rules,
                              registration_start, registration_end,
                              event_start, event_end, wildcard_enabled,
                              individual_ranking_enabled }
2. Middleware: xác thực JWT → role=COORDINATOR, status=APPROVED
3. App validate:
   - UNIQUE(name, season, year) → 409 nếu trùng
   - registration_end ≥ registration_start → 422 nếu sai
   - event_start ≥ registration_end → 422 nếu sai
   - event_end ≥ event_start → 422 nếu sai
4. INSERT INTO hackathons (status='DRAFT', ...)
5. INSERT INTO audit_logs (action='HACKATHON_CREATE', ...)
6. Response: 201 Created { hackathon_id, slug }
2.5 Luồng xử lý — Alternative / Error Path
Tình huống	Xử lý
Trùng (name, season, year)	409 + message "Kỳ thi [name] [season] [year] đã tồn tại"
event_start < registration_end	422 + message "Ngày thi đấu phải sau ngày đóng đăng ký"
JWT invalid / hết hạn	401 Unauthorized
role != 'COORDINATOR'	403 Forbidden
3. FR-02 — Tạo / Cấu hình Round (Vòng thi)
Mô tả: Coordinator tạo các Round (vòng thi) trực tiếp dưới Hackathon. Round là cấp cha của Track. Mỗi Hackathon cần ít nhất 1 Round PRELIMINARY và đúng 1 Round FINAL.

[ARCH CHANGE v3.0] Round không còn là con của Track. rounds.hackathon_id thay rounds.track_id. Đây là thay đổi kiến trúc cốt lõi nhất của v5.0.

Workflow v5.0 ref: GĐ1 — Bước 2 | DB v3.0 ref: rounds [BC-01]

3.1 Bảng chính: rounds
Trường	Kiểu / Ràng buộc	Ghi chú nghiệp vụ	Thay đổi v3.0
id	SERIAL PK	Auto-increment	—
hackathon_id	INT FK hackathons.id ON DELETE CASCADE NOT NULL	[BC-01] Round thuộc Hackathon — thay track_id cũ	BREAKING
name	VARCHAR(100) NOT NULL	VD: "Vòng Sơ loại", "Vòng Chung kết"	—
exam_at	TIMESTAMP NOT NULL	Ngày giờ thi — thứ tự vòng sort theo exam_at (khác submission_deadline)	MỚI v3.1
is_final	BOOLEAN NOT NULL DEFAULT FALSE	TRUE = Round Chung kết — KHÔNG có Track con. FALSE = Sơ loại/Bán kết có Track	MỚI
round_type	VARCHAR(20) NOT NULL DEFAULT 'PRELIMINARY' CHECK IN ('PRELIMINARY','SEMIFINAL','FINAL')	Phân loại Round. FINAL: pool chung, 100% Judge EXTERNAL. SEMIFINAL: cùng rule PRELIMINARY (có Track con)	MỚI
coding_duration_hours	INT	Số giờ coding. Thực tế 2 mùa = 7 giờ	—
submission_open	TIMESTAMP	Thời điểm mở nhận bài (tuỳ chọn)	—
submission_deadline	TIMESTAMP NOT NULL	Hạn nộp bài. Bài sau deadline → LATE	—
late_submission_policy	VARCHAR(20) NOT NULL DEFAULT 'ALLOW_LATE_PENDING' CHECK IN ('ALLOW_LATE_PENDING','HARD_LOCK')	Sơ loại: ALLOW_LATE_PENDING. Chung kết: HARD_LOCK (không có LATE_PENDING)	MỚI
problem_statement_url	TEXT	URL đề bài — set khi phát đề (GĐ3)	—
problem_released_at	TIMESTAMP	Thời điểm phát đề chính thức (GĐ3)	—
top_n_advance	INT	Số đội top N MỖI BẢNG (assigned_group) vào Round tiếp. NULL nếu is_final=TRUE	—
min_teams_final	INT	Số đội tối thiểu vào Round tiếp — kích hoạt Wild Card nếu thiếu. NULL nếu is_final=TRUE	—
wildcard_enabled	BOOLEAN NOT NULL DEFAULT FALSE	Per-round override. Phải hackathon.wildcard_enabled=TRUE mới có hiệu lực	—
tiebreak_rule	VARCHAR(50) DEFAULT 'PENALTY_SCORE' CHECK IN ('PENALTY_SCORE','SUBMISSION_TIME','COORDINATOR_DECISION')	Luật xử lý đồng điểm	—
is_active	BOOLEAN NOT NULL DEFAULT FALSE	TRUE = Round đang diễn ra. Mỗi hackathon_id chỉ có 1 Round is_active=TRUE	—
scoring_locked	BOOLEAN NOT NULL DEFAULT FALSE	TRUE = đã khóa chấm điểm	—
scoring_locked_at / scoring_locked_by	TIMESTAMP / INT FK users.id	Thời điểm và người khóa	—
force_locked / force_lock_reason	BOOLEAN / TEXT	Force-lock kèm lý do bắt buộc	—
created_at	TIMESTAMP NOT NULL DEFAULT NOW()	Auto set	—
Constraints DB:

sql
-- Thứ tự vòng: ORDER BY exam_at ASC (không còn sequence_order)
CONSTRAINT chk_round_type_final_consistent CHECK (
    (is_final = TRUE  AND round_type = 'FINAL')
    OR (is_final = FALSE AND round_type IN ('PRELIMINARY', 'SEMIFINAL'))
)
-- Mỗi Hackathon chỉ có đúng 1 Round is_final=TRUE (MySQL — generated column):
ALTER TABLE rounds
  ADD COLUMN final_uk INT GENERATED ALWAYS AS (CASE WHEN is_final = TRUE THEN hackathon_id END) VIRTUAL,
  ADD UNIQUE KEY uk_rounds_one_final_per_hackathon (final_uk);

-- Trigger (MySQL):
-- trg_prevent_track_in_final_round_ins/upd — INSERT/UPDATE tracks khi round.is_final=TRUE
-- → SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'DESIGN_VIOLATION: Round Chung kết không được có Track con'.

3.2 Bảng liên quan
Bảng liên quan	Vai trò	Cách liên kết / Điều kiện
hackathons	Parent — validate trạng thái	Hackathon phải DRAFT hoặc ONGOING mới cho phép tạo/sửa Round
tracks	Child — thuộc Round Sơ loại	tracks.round_id FK → rounds.id. Round FINAL không có Track con (enforce bởi trigger)
criteria	Child — Criteria Chung kết	criteria.round_id dùng khi is_final=TRUE (XOR với criteria.track_id)
judge_assignments	Child — Judge Chung kết	judge_assignments.round_id dùng khi track_id IS NULL (Round FINAL)
submissions	Child — bài nộp Chung kết	submissions.round_id dùng khi track_id IS NULL (Round FINAL)
audit_logs	Ghi lịch sử	action='ROUND_CREATE' / 'ROUND_UPDATE' / 'ROUND_DELETE'
3.3 Ràng buộc nghiệp vụ
Ràng buộc	Tầng	Hành động khi vi phạm
hackathon_id phải trỏ Hackathon tồn tại và status IN ('DRAFT','ONGOING')	App	422 nếu Hackathon không đúng trạng thái
exam_at: vòng Sơ loại/Bán kết phải trước Chung kết; mỗi kỳ đúng 1 Round FINAL	App	422 / 409
is_final=TRUE ↔ round_type='FINAL' phải nhất quán	DB (CHECK constraint)	DB reject
Mỗi Hackathon đúng 1 Round FINAL (is_final=TRUE)	DB (`final_uk` generated + UNIQUE)	DB reject nếu cố tạo thêm Round FINAL
KHÔNG validate tổng weight Criteria tại bước này — Criteria chưa tồn tại	App	Không validate; chỉ warn tại Bước 4; gate cứng tại Bước 7
top_n_advance = NULL khi is_final=TRUE	App	422 nếu set top_n_advance cho Round FINAL
late_submission_policy='HARD_LOCK' bắt buộc với round_type='FINAL'	App	Warn hoặc auto-set khi Coordinator tạo Round FINAL
submission_deadline > submission_open (nếu có) và > NOW()	App	422 nếu sai thứ tự timestamp
force_lock_reason NOT NULL khi force_locked=TRUE	App	422 nếu thiếu lý do force-lock
Không xóa Round khi có submissions hoặc criteria liên quan	App	409 Conflict
Không xóa Round khi is_active=TRUE	App	409 Conflict
3.4 Thực tế 2 mùa — Cấu hình Round
Mùa	Round Sơ loại	Round Chung kết
Fall 2025	coding_duration_hours=7, top_n_advance=2 (per bảng), late_submission_policy='ALLOW_LATE_PENDING', wildcard_enabled=TRUE, min_teams_final=6	is_final=TRUE, late_submission_policy='HARD_LOCK', top_n_advance=NULL
Spring 2026	coding_duration_hours=7, top_n_advance=2 (per Track=per bảng), late_submission_policy='ALLOW_LATE_PENDING', wildcard_enabled=TRUE, min_teams_final=6	is_final=TRUE, late_submission_policy='HARD_LOCK', top_n_advance=NULL
3.5 Luồng xử lý — Happy Path (tạo 2 Round)
1. POST /api/v1/hackathons/{hackathon_id}/rounds
   Body Round 1: { name="Vòng Sơ loại", examAt="...",
                   is_final=false, round_type="PRELIMINARY",
                   submission_deadline=..., coding_duration_hours=7,
                   late_submission_policy="ALLOW_LATE_PENDING",
                   top_n_advance=2, min_teams_final=6,
                   wildcard_enabled=true, tiebreak_rule="PENALTY_SCORE" }

2. POST /api/v1/hackathons/{hackathon_id}/rounds
   Body Round 2: { name="Vòng Chung kết", examAt="..." (sau Round 1),
                   is_final=true, round_type="FINAL",
                   submission_deadline=...,
                   late_submission_policy="HARD_LOCK" }

3. App validate mỗi Round:
   - Hackathon tồn tại và DRAFT/ONGOING
   - examAt hợp lệ; Sơ loại trước Chung kết; đã có Sơ loại trước khi tạo Chung kết
   - is_final ↔ round_type nhất quán
   - Không tạo Round FINAL thứ 2 (generated column `final_uk` + UNIQUE — schema §3 rounds)
   - submission_deadline hợp lệ

4. INSERT INTO rounds (hackathon_id=..., ...)
5. INSERT INTO audit_logs (action='ROUND_CREATE', ...)
6. Response: 201 Created { round_id }
4. FR-03 — Tạo / Cấu hình Track (Bảng đấu trong Round)
Mô tả: Coordinator tạo các Track (bảng đấu thi) trong Round Sơ loại. Mỗi Track có Criteria riêng, Judge riêng, Mentor riêng. Round Chung kết (is_final=TRUE) tuyệt đối không có Track con.

[ARCH CHANGE v3.0] Track không còn thuộc Hackathon mà thuộc Round cụ thể. tracks.round_id thay tracks.hackathon_id. Track là bảng đấu trong vòng thi đó, không phải hạng mục cố định.

`assigned_group` (A/B/C/D) **không** còn trên `teams` — ghi tại `team_round_tracks.assigned_group` khi GĐ2 bốc thăm (BC-04, BC-05).

Workflow v5.0 ref: GĐ1 — Bước 3 | DB v3.0 ref: tracks [BC-02]

4.1 Bảng chính: tracks
Trường	Kiểu / Ràng buộc	Ghi chú nghiệp vụ	Thay đổi v3.0
id	SERIAL PK	Auto-increment	—
round_id	INT FK rounds.id ON DELETE CASCADE NOT NULL	[BC-02] Track thuộc Round cụ thể — thay hackathon_id cũ	BREAKING
name	VARCHAR(200) NOT NULL	VD: "Bảng A", "Track 1 — RAG Pipeline"	—
description	TEXT	Mô tả chủ đề thi đấu; tuỳ chọn	—
topic	VARCHAR(300)	[MỚI] Chủ đề bốc thăm tại KICKOFF. NULL/placeholder khi tạo; cập nhật sau bốc thăm	MỚI
max_teams	INT	Tổng đội tối đa trong Track (tất cả bảng cộng lại). Fall 2025: 3 bảng × 6 = 18 đội	—
max_teams_per_group	INT	Số đội tối đa MỖI BẢNG (assigned_group). Fall 2025=6, Spring 2026=8	Giữ từ v2.1
min_team_size	INT NOT NULL DEFAULT 3	Số thành viên tối thiểu. Thực tế 2 mùa = 3	—
max_team_size	INT NOT NULL DEFAULT 5	Số thành viên tối đa. Thực tế 2 mùa = 5	—
status	VARCHAR(20) NOT NULL DEFAULT 'OPEN' CHECK IN ('OPEN','CLOSED','CANCELLED')	Trạng thái Track	—
sequence_order	INT NOT NULL DEFAULT 1	[MỚI] Thứ tự Track trong Round. UNIQUE(round_id, sequence_order)	MỚI
Constraints DB:

sql
UNIQUE (round_id, sequence_order)
-- Trigger enforce: Round FINAL không có Track con
-- Xem docs/db/schema-v3.0-mysql.md §5.6 — trg_prevent_track_in_final_round_ins/upd
Lưu ý quan trọng: tracks.hackathon_id (cũ) đã bị XÓA. Muốn biết Track thuộc Hackathon nào: Track → Round → Hackathon (qua JOIN: tracks JOIN rounds ON tracks.round_id = rounds.id).

4.2 Phân biệt max_teams vs max_teams_per_group
max_teams	max_teams_per_group
Ý nghĩa	Tổng đội trong Track (tất cả bảng)	Số đội trong 1 bảng (assigned_group)
Fall 2025	18 (3 bảng × 6)	6
Spring 2026	8 (1 bảng = Track)	8
NULL	Không giới hạn tổng	Không giới hạn per bảng
Hai giá trị này độc lập nhau và đều có thể NULL.

4.3 Bảng liên quan
Bảng liên quan	Vai trò	Cách liên kết / Điều kiện
rounds	Parent — validate Round	Round phải tồn tại, thuộc Hackathon DRAFT/ONGOING, và is_final=FALSE. Trigger block Track vào Round FINAL
criteria	Child — Criteria Sơ loại	criteria.track_id FK → tracks.id. Không có Criteria gắn Track cho Round FINAL
mentor_assignments	Child — ON DELETE CASCADE	Notify Mentor bị hủy khi Track bị xóa/CANCELLED
judge_assignments	Child — ON DELETE CASCADE	Notify Judge bị hủy khi Track bị xóa/CANCELLED
team_round_tracks	Child — phân công đội	team_round_tracks.track_id FK → tracks.id. Không xóa Track khi có đội đã phân công
audit_logs	Ghi lịch sử	action='TRACK_CREATE' / 'TRACK_UPDATE' / 'TRACK_DELETE'
4.4 Ràng buộc nghiệp vụ
Ràng buộc	Tầng	Hành động khi vi phạm
round_id phải trỏ Round is_final=FALSE (không phải Round Chung kết)	DB (Trigger) + App	DB: DESIGN_VIOLATION: Round Chung kết không được có Track con. App: 422
UNIQUE(round_id, sequence_order)	DB + App	409 Conflict
Không xóa Track khi có đội trong team_round_tracks	App	409: "Track đang có đội được phân công"
Không xóa Track khi có Round is_active=TRUE liên quan	App	409
max_teams_per_group ≤ max_teams nếu cả hai có giá trị	App	422 warn
min_team_size ≤ max_team_size	DB (CHECK) + App	DB reject; App 422
Hackathon phải DRAFT hoặc ONGOING để tạo/sửa Track	App	422
topic: tạo trong GĐ1 có thể để NULL; cập nhật sau bốc thăm tại KICKOFF (GĐ2 Bước 6)	App	Không block nếu NULL khi tạo
4.5 Thực tế 2 mùa — Cấu hình Track
Mùa	Số Track	max_teams	max_teams_per_group	Ghi chú
Fall 2025	2 Track trong Round Sơ loại	18 (3 bảng × 6)	6	Mỗi Track nhiều bảng; bốc thăm bảng sau bốc thăm Track
Spring 2026	N Track (= số bảng đăng ký)	8	8	Mỗi Track = 1 bảng; assigned_group=NULL
4.6 Luồng xử lý — Happy Path (Fall 2025: 2 Track)
POST /api/v1/rounds/{round_id}/tracks  (round_id = Round Sơ loại)
Body Track 1: { name="Track 1 — SDLC nhóm 1", topic=NULL,
                max_teams=18, max_teams_per_group=6,
                min_team_size=3, max_team_size=5, sequence_order=1 }

Body Track 2: { name="Track 2 — SDLC nhóm 2", topic=NULL,
                max_teams=18, max_teams_per_group=6,
                min_team_size=3, max_team_size=5, sequence_order=2 }

App validate:
  - round_id tồn tại và is_final=FALSE → không phải Round FINAL
  - Hackathon trạng thái DRAFT/ONGOING
  - UNIQUE(round_id, sequence_order)
  - max_teams_per_group ≤ max_teams

INSERT INTO tracks (round_id=..., topic=NULL, ...)
INSERT INTO audit_logs (action='TRACK_CREATE', ...)
Response: 201 Created { track_id }
5. FR-04 — Thiết lập Criteria (Tiêu chí chấm điểm)
Mô tả: Coordinator tạo bộ tiêu chí chấm điểm. [BREAKING CHANGE v3.0] Criteria dùng XOR FK: track_id cho Round Sơ loại/Bán kết; round_id cho Round Chung kết (is_final=TRUE). Tuyệt đối không có cả hai hoặc cả hai NULL.

Workflow v5.0 ref: GĐ1 — Bước 4 | DB v3.0 ref: criteria [BC-03]

5.1 Bảng chính: criteria
Trường	Kiểu / Ràng buộc	Ghi chú nghiệp vụ	Thay đổi v3.0
id	SERIAL PK	Auto-increment	—
track_id	INT FK tracks.id ON DELETE CASCADE	[BC-03] Criteria gắn vào Track (Sơ loại). NULL khi là Criteria Chung kết	BREAKING
round_id	INT FK rounds.id ON DELETE CASCADE	[BC-03] CHỈ dùng cho Round Chung kết (is_final=TRUE). NULL khi là Criteria Sơ loại	MỚI
source_criteria_id	INT FK criteria.id ON DELETE SET NULL	Self-ref — kế thừa từ kỳ trước. Chỉ trace lịch sử, không cascade	—
name	VARCHAR(200) NOT NULL	Tên tiêu chí	—
type	VARCHAR(20) NOT NULL CHECK IN ('TECHNICAL','SOFT_SKILL','PENALTY')	PENALTY không tính vào weight tổng, chỉ dùng tiebreak	—
weight	FLOAT NOT NULL CHECK (weight > 0 AND weight <= 1)	Tổng weight (loại trừ PENALTY) = 1.0. Validate tại app-layer	—
max_score	INT NOT NULL DEFAULT 10	Điểm tối đa tiêu chí	—
description	TEXT	Mô tả tiêu chí; hướng dẫn chấm	—
rubric_url	TEXT	Link rubric chi tiết	—
display_order	INT NOT NULL DEFAULT 0	Thứ tự hiển thị trong UI chấm điểm	—
XOR Constraint DB:

sql
CONSTRAINT chk_criteria_xor_fk CHECK (
    (track_id IS NOT NULL AND round_id IS NULL)   -- Sơ loại
    OR
    (track_id IS NULL AND round_id IS NOT NULL)   -- Chung kết
)

-- Trigger: round_id phải trỏ Round is_final=TRUE
-- Xem schema §5.8 — trg_check_criteria_round_is_final_ins/upd (SIGNAL nếu round_id không FINAL)
-- Nếu track_id IS NULL mà round_id trỏ Round is_final=FALSE
-- → RAISE EXCEPTION 'INVALID_ROUND_FOR_CRITERIA'
5.2 Bảng liên quan
Bảng liên quan	Vai trò	Cách liên kết / Điều kiện
tracks	Parent (Sơ loại)	track_id FK → tracks.id. Track phải tồn tại
rounds	Parent (Chung kết)	round_id FK → rounds.id. Round phải is_final=TRUE (enforce bởi trigger)
scores	Guard — ngăn sửa/xóa	Nếu scores đã có criterion_id=id → không cho sửa weight/type hoặc xóa
criteria	Self-ref (kế thừa)	source_criteria_id → clone độc lập, không ảnh hưởng bản gốc
audit_logs	Ghi lịch sử	action='CRITERIA_CREATE' / 'CRITERIA_UPDATE' / 'CRITERIA_DELETE'
5.3 Validate tổng weight — 3 tầng
Tầng	Thời điểm	Hành động
Tầng 1 (Soft warn)	Khi nhập liệu realtime (Bước 4)	UI hiển thị: "Tổng weight hiện tại: 0.85 / 1.0" màu vàng. KHÔNG block
Tầng 2 (Gate cứng)	Khi chuyển DRAFT → ONGOING (Bước 7 / FR-07)	Block 422 kèm danh sách Track/Round vi phạm. Không UPDATE cho đến khi sửa xong
Tầng 3 (Safety net)	Khi set rounds.is_active=TRUE (FR-07B)	Block 422 nếu SUM(weight) ≠ 1.0. Ngăn bypass tầng 2 sau khi Hackathon ONGOING
sql
-- Query validate tổng weight (dùng ở Tầng 2 và 3):
-- Sơ loại: validate qua track_id
SELECT tr.id AS track_id, tr.name AS track_name,
       SUM(c.weight) AS total_weight
FROM criteria c JOIN tracks tr ON tr.id = c.track_id
WHERE c.type != 'PENALTY'
  AND tr.round_id = :preliminary_round_id
GROUP BY tr.id, tr.name
HAVING ABS(SUM(c.weight) - 1.0) > 0.001;

-- Chung kết: validate qua round_id
SELECT r.id AS round_id, SUM(c.weight) AS total_weight
FROM criteria c JOIN rounds r ON r.id = c.round_id
WHERE c.type != 'PENALTY'
  AND r.is_final = TRUE
  AND r.hackathon_id = :hackathon_id
GROUP BY r.id
HAVING ABS(SUM(c.weight) - 1.0) > 0.001;
5.4 Ràng buộc nghiệp vụ
Ràng buộc	Tầng	Hành động khi vi phạm
XOR FK: đúng 1 trong 2 (track_id hoặc round_id) phải có giá trị	DB (CHECK)	DB reject; App 422
round_id (Chung kết) phải trỏ Round is_final=TRUE	DB (Trigger)	INVALID_ROUND_FOR_CRITERIA; App 422
Tổng weight (không PENALTY) = 1.0 — chỉ validate cứng tại Tầng 2 và 3	App (Gate/Safety net)	Tầng 1: warn mềm; Tầng 2-3: 422
Không sửa weight / type / xóa khi Round đã có scores	App	409: "Criteria đã có điểm chấm, không thể sửa"
Criteria Sơ loại và Chung kết phải tạo riêng — không share	App	Mỗi criterion gắn 1 track_id hoặc 1 round_id, không dùng chung
Nếu nhiều Track dùng cùng bộ Criteria → vẫn tạo riêng cho từng Track (khác track_id)	App	Document rõ; không cho share bằng cách đặt track_id=NULL
type='PENALTY' không tính vào leaderboard — chỉ dùng tiebreak	App	Filter WHERE type != 'PENALTY' khi tính leaderboard và validate weight
Sửa bản Criteria kế thừa không ảnh hưởng bản gốc	App	Clone hoàn toàn độc lập; source_criteria_id chỉ để audit trail
5.5 Bộ Criteria thực tế 2 mùa
Fall 2025 — Sơ loại (mỗi Track, track_id khác nhau):

Tiêu chí	Type	Weight
Tính ứng dụng & khả thi	TECHNICAL	0.30
AI tự động hóa & tích hợp	TECHNICAL	0.30
Giao diện & trải nghiệm	SOFT_SKILL	0.20
Slide trình bày & demo	SOFT_SKILL	0.20
Tổng		1.00
Fall 2025 — Chung kết (round_id của Round FINAL):

Tiêu chí	Type	Weight
Hoàn thiện	TECHNICAL	0.30
Sáng tạo	TECHNICAL	0.25
Hiệu quả	TECHNICAL	0.20
Mở rộng	TECHNICAL	0.15
Trình bày & Phản biện	SOFT_SKILL	0.10
Tổng		1.00
Spring 2026 — Sơ loại (mỗi Track):

Tiêu chí	Type	Weight
Domain Accuracy	TECHNICAL	0.30
Kiến trúc RAG	TECHNICAL	0.30
Ý tưởng & Thuyết trình	SOFT_SKILL	0.15
Thực thi & Sáng tạo	TECHNICAL	0.15
UX & Giao diện	SOFT_SKILL	0.10
Tổng		1.00
Spring 2026 — Chung kết (round_id của Round FINAL):

Tiêu chí	Type	Weight
Xử lý & Truy xuất	TECHNICAL	0.30
Độ tin cậy	TECHNICAL	0.20
Tư duy Agent	TECHNICAL	0.20
Thực tế & Triển khai	TECHNICAL	0.20
Mở rộng	SOFT_SKILL	0.10
Tổng		1.00
5.6 Luồng kế thừa Criteria từ kỳ trước
1. Coordinator chọn nguồn kế thừa (hackathon kỳ trước, Track tương ứng)
2. GET /api/v1/criteria?track_id=:source_track_id
3. App clone:
   INSERT INTO criteria (track_id=:new_track_id,
                         source_criteria_id=:original_id,
                         name, type, weight, max_score, ...)
   -- Tạo bản copy hoàn toàn độc lập
4. Coordinator sửa name/weight/type tùy ý trong bản copy
5. UI hiển thị warn mềm nếu SUM(weight) ≠ 1.0 sau clone

---

6. FR-05 — Quản lý nhân sự giải đấu

Mô tả: Module tập trung 3 hành động con, thực hiện tại GĐ1 để đảm bảo conflict check có đủ dữ liệu hai chiều: (5a) Tạo tài khoản Judge EXTERNAL khách mời; (5b) Phân công Mentor vào Track Sơ loại; (5c) Phân công Judge sơ bộ vào Track Sơ loại.

[BREAKING CHANGE v3.0] Conflict Mentor↔Judge giờ là BLOCK CỨNG (cũ: chỉ warn). Enforce tại DB-layer bởi 2 trigger. Judge Chung kết: 100% EXTERNAL, enforce bởi trigger với ngoại lệ is_dept_head.

Workflow v5.0 ref: GĐ1 — Bước 5 | DB v3.0 ref: users, invitations, mentor_assignments, judge_assignments

6.1 Bước 5a — Tạo tài khoản Judge EXTERNAL (khách mời)
Trường	Giá trị	Ghi chú
users.role	'JUDGE'	Cố định
users.user_type	'EXTERNAL'	Judge khách mời luôn EXTERNAL
users.is_temp_account	TRUE	Flag phân biệt tài khoản tạm; giới hạn quyền qua middleware
users.is_dept_head	FALSE (default)	Trưởng khoa/bộ môn → set TRUE trước khi phân công Chung kết
users.status	'APPROVED'	Coordinator tạo và approve trực tiếp, không qua PENDING
users.institution	VARCHAR(300)	Tên công ty/tổ chức (VD: "Google Vietnam")
invitations.token	VARCHAR(128) UNIQUE	Audit/resend; không gửi cho user (login + MK tạm trong email)
invitations.expires_at	TIMESTAMP	**3 ngày** (72h) từ lúc tạo/resend. Hết hạn → Coordinator resend
Luồng email (loại 3 — spec product): Email + **mật khẩu tạm** + link accept. Judge xác nhận → **bắt buộc đổi MK**. TK chỉ trong thời gian sự kiện. Chi tiết: [../mf02/02-invitations.md](../mf02/02-invitations.md), [fr-05-personnel.md](api/fr-05-personnel.md).

POST /api/v1/users/temp-judges
Body: { fullName, email, institution, phone }
→ INSERT users (role='JUDGE', user_type='EXTERNAL', is_temp_account=TRUE, status='APPROVED', password_hash=temp)
→ INSERT invitations (role=JUDGE, token=uuid(), expires_at=NOW()+3 days)
→ Email: login + MK tạm + link accept
→ audit_logs: action='TEMP_ACCOUNT_CREATE'
→ Response: 201 { user_id }
6.2 Bước 5b — Phân công Mentor vào Track Sơ loại
Bảng chính: mentor_assignments
Trường	Kiểu / Ràng buộc	Ghi chú nghiệp vụ	Thay đổi v3.0
id	SERIAL PK	Auto-increment	—
mentor_id	INT FK users.id NOT NULL	Phải role='MENTOR' AND status='APPROVED'	—
track_id	INT FK tracks.id NOT NULL	Track trong Round Sơ loại. Track đã mang round_id → tự biết vòng nào. Không tạo Mentor cho Round Chung kết	FK giờ trỏ tracks mới (thuộc round)
assigned_at	TIMESTAMP NOT NULL DEFAULT NOW()	Auto set	—
assigned_by	INT FK users.id	Coordinator thực hiện phân công	—
Constraint: UNIQUE(mentor_id, track_id) — 1 Mentor / 1 Track trong 1 Round.

Trigger `trg_check_judge_mentor_conflict_ins` (BEFORE INSERT on mentor_assignments) — logic đầy đủ: [schema-v3.0-mysql.md](../db/schema-v3.0-mysql.md) §5.5:

Rule 1: Nếu người này đã là Judge của cùng track_id → BLOCK 'CONFLICT_SAME_TRACK'
Rule 2: Nếu người này đã là Judge Chung kết (FINAL_EXTERNAL) trong cùng Hackathon → BLOCK 'FINAL_JUDGE_CANNOT_BE_MENTOR'
sql
-- Rule 1: cùng Track → BLOCK
SELECT 1 FROM judge_assignments WHERE judge_id=:mentor_id AND track_id=:track_id;

-- Rule 2: đã là Judge Chung kết → BLOCK
SELECT 1 FROM judge_assignments ja
JOIN rounds r ON r.id = ja.round_id
WHERE ja.judge_id = :mentor_id
  AND ja.assignment_type = 'FINAL_EXTERNAL'
  AND r.hackathon_id = (SELECT r2.hackathon_id FROM tracks tr2
                        JOIN rounds r2 ON r2.id = tr2.round_id
                        WHERE tr2.id = :track_id);
6.3 Bước 5c — Phân công Judge Sơ loại vào Track
Bảng chính: judge_assignments (phần Sơ loại)
Trường	Kiểu / Ràng buộc	Ghi chú nghiệp vụ	Thay đổi v3.0
id	SERIAL PK	Auto-increment	—
judge_id	INT FK users.id NOT NULL	INTERNAL hoặc EXTERNAL, status='APPROVED'	—
track_id	INT FK tracks.id	[BC-07] Judge Sơ loại — gắn vào Track cụ thể. NOT NULL khi là Judge Sơ loại	BREAKING
round_id	INT FK rounds.id	NULL khi là Judge Sơ loại (track_id IS NOT NULL)	—
assignment_type	VARCHAR(20) CHECK IN ('NORMAL','HEAD','CALIBRATION','FINAL_EXTERNAL')	Sơ loại chỉ dùng 'NORMAL', 'HEAD', 'CALIBRATION'. Không dùng 'FINAL_EXTERNAL' ở đây	—
assigned_at / assigned_by	TIMESTAMP / INT FK users.id	Auto set / Coordinator	—
**UNIQUE qua generated column (MySQL)** — xem [schema-v3.0-mysql.md](../db/schema-v3.0-mysql.md) §2 `judge_assignments`:

```sql
-- track_uk / round_uk: GENERATED ALWAYS AS ... VIRTUAL (XOR pattern §11.2)
UNIQUE KEY uk_ja_judge_track (judge_id, track_uk),
UNIQUE KEY uk_ja_judge_final_round (judge_id, round_uk);
```

Trigger `trg_check_mentor_judge_conflict_ins` / `trg_check_mentor_judge_conflict_upd` (BEFORE INSERT/UPDATE on judge_assignments) — NHÁNH A (track_id IS NOT NULL); DDL §5.4:

Rule A: Nếu người này đã là Mentor của cùng track_id → BLOCK 'CONFLICT_SAME_TRACK'
Rule B: assignment_type='FINAL_EXTERNAL' không được dùng khi track_id IS NOT NULL → BLOCK 'INVALID_ASSIGNMENT_TYPE'
sql
-- Rule A: cùng Track → BLOCK CỨng
SELECT 1 FROM mentor_assignments
WHERE mentor_id = :judge_id AND track_id = :track_id;
-- Có kết quả → SIGNAL MESSAGE_TEXT 'CONFLICT_SAME_TRACK'
6.4 Quy tắc Conflict Mentor ↔ Judge đầy đủ (v3.0)
Ma trận conflict tổng hợp
Giảng viên / Nhân sự	Vai trò trong Sơ loại	Muốn Judge Track/Round	Kết quả	Mã lỗi
GV A — Mentor Track AI	Judge Track AI (cùng Round)	✗ BLOCK	CONFLICT_SAME_TRACK	
GV A — Mentor Track AI	Judge Track Web (cùng Round)	✓ HỢP LỆ	—	
GV A — Mentor Track AI	Judge Round Chung kết	✗ BLOCK	INTERNAL_MENTOR_NOT_ALLOWED_IN_FINAL	
GV B — không Mentor	Judge bất kỳ Track Sơ loại	✓ HỢP LỆ	—	
GV B — không Mentor (INTERNAL)	Judge Round Chung kết	✗ BLOCK	INTERNAL_JUDGE_NOT_ALLOWED_IN_FINAL	
Trưởng khoa (is_dept_head=TRUE) — không Mentor	Judge Round Chung kết	✓ NỌI LỆ (ghi audit)	DEPT_HEAD_FINAL_JUDGE_EXCEPTION	
Judge EXTERNAL (bất kỳ)	Judge Round Chung kết	✓ HỢP LỆ	—	
Judge đã phân công Chung kết	Mentor Track Sơ loại	✗ BLOCK	FINAL_JUDGE_CANNOT_BE_MENTOR	
Logic trigger MySQL `trg_check_mentor_judge_conflict_ins/upd` (trên judge_assignments — schema §5.4)
NHÁNH A: track_id IS NOT NULL (Judge Sơ loại)
  ├── Rule A: EXISTS mentor_assignments WHERE mentor_id=judge_id AND track_id=track_id
  │   → BLOCK: CONFLICT_SAME_TRACK
  └── Rule B: assignment_type='FINAL_EXTERNAL'
      → BLOCK: INVALID_ASSIGNMENT_TYPE (FINAL_EXTERNAL không dùng ở Sơ loại)

NHÁNH B: track_id IS NULL (Judge Chung kết)
  ├── Validate: round_id NOT NULL
  ├── Validate: rounds.is_final=TRUE (round_id phải là Round FINAL)
  ├── Validate: assignment_type='FINAL_EXTERNAL' (bắt buộc)
  └── Validate user_type:
      ├── EXTERNAL → ALLOW
      └── INTERNAL:
          ├── is_dept_head=TRUE AND NOT EXISTS mentor_assignments WHERE mentor_id=judge_id
          │   → ALLOW (ngoại lệ, ghi audit DEPT_HEAD_FINAL_JUDGE_EXCEPTION)
          ├── EXISTS mentor_assignments WHERE mentor_id=judge_id
          │   → BLOCK: INTERNAL_MENTOR_NOT_ALLOWED_IN_FINAL
          └── else
              → BLOCK: INTERNAL_JUDGE_NOT_ALLOWED_IN_FINAL
Logic trigger MySQL `trg_check_judge_mentor_conflict_ins` (trên mentor_assignments — schema §5.5)
Rule 1: EXISTS judge_assignments WHERE judge_id=mentor_id AND track_id=track_id
  → BLOCK: CONFLICT_SAME_TRACK

Rule 2: EXISTS judge_assignments ja JOIN rounds r
        WHERE ja.judge_id=mentor_id
          AND ja.assignment_type='FINAL_EXTERNAL'
          AND r.hackathon_id = <hackathon của track_id>
  → BLOCK: FINAL_JUDGE_CANNOT_BE_MENTOR
6.5 Phạm vi phân công tại GĐ1
Hành động	Thời điểm	Ghi chú
Tạo tài khoản Judge EXTERNAL	GĐ1 Bước 5a	Phải tạo trước khi phân công (để conflict check hoạt động)
Phân công Mentor vào Track Sơ loại	GĐ1 Bước 5b	mentor_assignments(mentor_id, track_id)
Phân công Judge vào Track Sơ loại	GĐ1 Bước 5c	judge_assignments(judge_id, track_id), assignment_type='NORMAL'
Phân công Judge vào Round Chung kết	GĐ4 Bước 6	KHÔNG làm ở GĐ1. judge_assignments(judge_id, round_id), assignment_type='FINAL_EXTERNAL'
Lý do dồn nhân sự Sơ loại về GĐ1: Nếu Judge chưa tồn tại khi phân công Mentor, conflict check chiều Judge→Mentor sẽ bị skip (không có dữ liệu). Dồn cả 3 bước về GĐ1 đảm bảo cả 2 chiều conflict đều có đủ dữ liệu khi cần.

6.6 Ràng buộc nghiệp vụ tổng hợp FR-05
Ràng buộc	Tầng	Hành động khi vi phạm
Conflict Mentor Track X ↔ Judge Track X → BLOCK CỨNG	DB (Trigger)	RAISE EXCEPTION CONFLICT_SAME_TRACK; App 422
Judge Chung kết phải assignment_type='FINAL_EXTERNAL'	DB (Trigger)	INVALID_ASSIGNMENT_TYPE; App 422
Judge Chung kết phải round_id trỏ Round is_final=TRUE	DB (Trigger)	INVALID_FINAL_ROUND; App 422
INTERNAL Judge không được vào Chung kết (trừ is_dept_head=TRUE + không Mentor)	DB (Trigger)	INTERNAL_JUDGE_NOT_ALLOWED_IN_FINAL; App 422
Ngoại lệ is_dept_head: Coordinator phải set users.is_dept_head=TRUE trước	App	Không tự suy diễn; Coordinator explicit action
mentor_id phải role='MENTOR' AND status='APPROVED'	App	422
judge_id phải role='JUDGE' AND status='APPROVED'	App	422
UNIQUE(mentor_id, track_id)	DB	409 Conflict
UNIQUE(judge_id, track_uk) / UNIQUE(judge_id, round_uk) — generated column	DB	409 (`JUDGE_ASSIGN_DUPLICATE`)
Track phân công Mentor phải status='OPEN' và không CANCELLED	App	422
Không phân công Judge Chung kết tại GĐ1 — chỉ ở GĐ4	App	Block 422 nếu gọi API với `round_id` FINAL + `FINAL_EXTERNAL` (trigger DB cũng block)
7. FR-06 — Lên lịch sự kiện
Mô tả: Coordinator lên lịch các sự kiện đi kèm Hackathon. Validate thời gian 3 lớp bắt buộc.

[BC-09 v3.0] Bỏ TEAM_MEETING khỏi events.type CHECK enum — loại sự kiện này không tồn tại trong thực tế vận hành 2 mùa.

Workflow v5.0 ref: GĐ1 — Bước 6 | DB v3.0 ref: events, notifications

7.1 Bảng chính: events
Trường	Kiểu / Ràng buộc	Ghi chú nghiệp vụ	Thay đổi v3.0
id	SERIAL PK	Auto-increment	—
hackathon_id	INT FK hackathons.id ON DELETE CASCADE NOT NULL	Sự kiện thuộc kỳ thi	—
title	VARCHAR(300) NOT NULL	Tên sự kiện	—
type	VARCHAR(30) NOT NULL CHECK IN ('KICKOFF','WORKSHOP','PRESENTATION','AWARDS','OTHER')	[BC-09] Bỏ TEAM_MEETING. 4 loại chính + OTHER	SỬA ENUM
description	TEXT	Mô tả nội dung; tuỳ chọn	—
location	VARCHAR(300)	Địa điểm vật lý (offline). NULL nếu online	—
meet_url	TEXT	Link Zoom/Teams/Meet. NULL nếu offline	—
starts_at	TIMESTAMP NOT NULL	Thời điểm bắt đầu	—
ends_at	TIMESTAMP	Thời điểm kết thúc. Phải ≥ starts_at nếu có	—
is_public	BOOLEAN NOT NULL DEFAULT TRUE	Hiển thị công khai với mọi người trong Hackathon	—
created_by	INT FK users.id	Coordinator tạo	—
7.2 Bảng hỗ trợ: notifications (type = REMINDER)
Trường	Kiểu	Ghi chú
type	'REMINDER'	Phân biệt với các loại notification khác
reference_type	'events'	Polymorphic FK logic
reference_id	INT	events.id
user_id	INT FK users.id	Người nhận (tất cả status='APPROVED' trong Hackathon)
is_read / read_at	BOOLEAN / TIMESTAMP	Tracking đọc
7.3 Validate thời gian sự kiện — 3 Lớp
Lớp	Tên	Điều kiện	Hành động khi vi phạm
Lớp 1a	WORKSHOP trong khung	`starts_at >= registration_start`; **được** trước `event_start`	BLOCK CỨNG 422
Lớp 1b	Milestone khác	`starts_at >= event_start`; `ends_at <= event_end + 1d`	BLOCK CỨNG 422
Lớp 2	Không chồng / trùng	Tối đa 1 milestone/type; không overlap cùng type; OTHER không chồng milestone	BLOCK CỨNG 422
Lớp 3	Thứ tự giai đoạn	WORKSHOP → KICKOFF → PRESENTATION → AWARDS (`effectiveEnd` trước `startsAt` sau)	BLOCK CỨNG 422
Lớp 3d	Gợi ý KICKOFF	`KICKOFF.starts_at ∈ [event_start, event_start+1d]`	WARN MỀM
**Runbook timeline (PDF, JSON, test):** [04-quy-trinh-van-hanh.md#timeline--events](04-quy-trinh-van-hanh.md#timeline--events). Chi tiết §7.5; `round.examAt` đồng bộ readiness/PUT milestone (FR-03).

7.4 Thực tế 2 mùa — Lịch sự kiện
Sự kiện	Fall 2025	Spring 2026
WORKSHOP	Online 19h30–21h30 ngày 29/10	Online 20h–21h30 ngày 9/4
KICKOFF	Offline 14h–17h ngày 1/11 — bốc thăm chia Track + họp đội	Offline 14h–17h ngày 11/4
PRESENTATION	Ngày thi Sơ loại (ngày 2/11)	Ngày thi Sơ loại (12/4 — cả 2 vòng)
AWARDS	Lễ trao giải Chung kết (cuối tháng 11)	Lễ trao giải Chung kết (cuối tháng 4)
7.5 Ràng buộc nghiệp vụ
Ràng buộc	Tầng	Hành động khi vi phạm
ends_at ≥ starts_at nếu có ends_at	App	422 Block cứng
Lớp 1a: WORKSHOP starts_at ≥ registration_start (được trước event_start)	App	422 Block cứng
Lớp 1b: KICKOFF/PRESENTATION/AWARDS starts_at ≥ event_start	App	422 Block cứng
Lớp 1: ends_at ≤ hackathons.event_end + 1d	App	422 Block cứng
Lớp 2: Không 2 KICKOFF/AWARDS song song	App	422 Block cứng
Lớp 3: WORKSHOP→KICKOFF→PRESENTATION→AWARDS (endsAt nối tiếp)	App	422 Block cứng
Lớp 3d: KICKOFF trong [event_start, event_start+1d]	App	Warn mềm
Mỗi hackathon 1 milestone/type	App	422 EVENT_MILESTONE_DUPLICATE
OTHER ↔ milestone không chồng giờ	App	422 EVENT_CONFLICTS_WITH_MILESTONE
Có round sơ loại → bắt buộc PRESENTATION	App	422 EVENT_PRESENTATION_MISSING
Có round CK → bắt buộc AWARDS	App	422 EVENT_AWARDS_MISSING
DELETE milestone → revalidate round.examAt	App	422 ROUND_EXAM_* / EVENT_*_MISSING
round.examAt vs events (readiness)	App	422 ROUND_EXAM_*
type chỉ trong ('KICKOFF','WORKSHOP','PRESENTATION','AWARDS','OTHER')	DB (CHECK)	DB reject
TEAM_MEETING không còn tồn tại	DB (CHECK)	DB reject nếu cố dùng
Gửi REMINDER đến tất cả APPROVED users khi tạo event public	App (sync)	`NotificationService` insert đồng bộ trong transaction (có thể chuyển async sau)
≥1 event type=KICKOFF phải tồn tại trước khi chuyển ONGOING	App (Gate FR-07)	422 tại Gate DRAFT→ONGOING
8. FR-07 — Chuyển trạng thái Hackathon (State Machine)
Mô tả: Coordinator chuyển trạng thái Hackathon theo chiều một chiều tuyến tính. DRAFT → ONGOING là gate quan trọng nhất với 5 điều kiện cứng.

Workflow v5.0 ref: GĐ1 — Bước 7 | GĐ5 — Bước 4 | GĐ6 — Bước 3 | DB v3.0 ref: hackathons.status

8.1 State Machine — hackathons.status
DRAFT ──[Gate 5 điều kiện]──► ONGOING ──[Round CK locked]──► PENDING_CONFIRM ──[Xác nhận]──► FINISHED
  ↑                                                                                                  
  └──────────────────── KHÔNG được quay lại (terminal transition) ───────────────────────────────────
Transition	Actor	Pre-condition bắt buộc
DRAFT → ONGOING	Coordinator	Gate 5 điều kiện [G1–G5] (xem §8.2)
ONGOING → PENDING_CONFIRM	Coordinator	Round Chung kết scoring_locked=TRUE; BTC họp thống nhất
PENDING_CONFIRM → FINISHED	Coordinator	Coordinator xác nhận; prizes đã ghi nhận
Bất kỳ → quay lại	—	KHÔNG CHO PHÉP
8.2 Gate cứng 5 điều kiện — DRAFT → ONGOING
Gate	Điều kiện	Query kiểm tra	Lỗi khi fail
[G1]	≥1 Round round_type='PRELIMINARY' và có ≥1 Track con (is_final=FALSE)	SELECT COUNT(*) FROM rounds WHERE hackathon_id=:id AND round_type='PRELIMINARY' + SELECT COUNT(*) FROM tracks WHERE round_id IN (...)	"Chưa có Vòng Sơ loại hoặc chưa có Track"
[G2]	Đúng 1 Round is_final=TRUE (round_type='FINAL')	SELECT COUNT(*) FROM rounds WHERE hackathon_id=:id AND is_final=TRUE = 1	"Thiếu hoặc dư Round Chung kết"
[G3]	Mọi Track của Round Sơ loại có Criteria, SUM(weight)=1.0 (±0.001)	SELECT track_id, SUM(weight) FROM criteria WHERE track_id IN (...Sơ loại tracks...) AND type!='PENALTY' GROUP BY track_id HAVING ABS(SUM(weight)-1.0)>0.001	"Track [X]: tổng weight = Y.YY (cần 1.0)"
[G4]	Round Chung kết có Criteria (gắn round_id), SUM(weight)=1.0	SELECT SUM(weight) FROM criteria WHERE round_id=:final_round_id AND type!='PENALTY' HAVING ABS(SUM(weight)-1.0)>0.001	"Round Chung kết: tổng weight = Z.ZZ"
[G5]	≥1 event type='KICKOFF' tồn tại và validate Lớp 1+2	SELECT COUNT(*) FROM events WHERE hackathon_id=:id AND type='KICKOFF' ≥ 1	"Thiếu sự kiện Khai mạc (KICKOFF)"
Round Chung kết không cần Track con — đây là thiết kế đúng của kiến trúc mới, không phải thiếu sót. Gate không check Track con cho Round FINAL.

8.3 Bảng liên quan — Validate DRAFT → ONGOING
Bảng	Vai trò	Query kiểm tra
rounds	[G1,G2] Kiểm tra Round PRELIMINARY và FINAL	Xem §8.2
tracks	[G1,G3] Kiểm tra Track trong Round Sơ loại	WHERE round_id IN (SELECT id FROM rounds WHERE hackathon_id=:id AND round_type='PRELIMINARY')
criteria	[G3,G4] Validate weight	XOR: track_id (Sơ loại) / round_id (Chung kết)
events	[G5] Validate KICKOFF	WHERE hackathon_id=:id AND type='KICKOFF'
audit_logs	Ghi lịch sử transition	action='HACKATHON_STATUS_CHANGE', detail={from:'DRAFT', to:'ONGOING', validated_at, validated_by}
notifications	Gửi thông báo ONGOING	type='HACKATHON_OPEN' đến tất cả users.status='APPROVED'
8.4 Ràng buộc nghiệp vụ
Ràng buộc	Tầng	Hành động khi vi phạm
Transition một chiều: không quay lại DRAFT	App	422: "Không thể quay lại trạng thái DRAFT"
Tất cả 5 Gate [G1–G5] phải pass — fail bất kỳ 1 gate → block toàn bộ	App	422 kèm errors array chi tiết từng gate fail
Chỉ Coordinator mới thực hiện transition	API Guard	403 Forbidden
Mọi transition ghi audit_logs trong cùng DB transaction	App	Rollback nếu audit fail
Sau ONGOING: gửi notification HACKATHON_OPEN async	App (Worker)	Async; không block transition
8.5 Luồng xử lý — DRAFT → ONGOING
PATCH /api/v1/hackathons/{id}/status  { "status": "ONGOING" }

1. Middleware: role=COORDINATOR, status=APPROVED
2. Validate current status = 'DRAFT' (không cho skip)
3. Chạy 5 Gate theo thứ tự [G1 → G5]:
   a. G1: ≥1 PRELIMINARY Round + ≥1 Track con → fail: collect error
   b. G2: đúng 1 FINAL Round → fail: collect error
   c. G3: mọi Track Sơ loại có Criteria, weight=1.0 → fail: collect errors per track
   d. G4: Round Chung kết có Criteria, weight=1.0 → fail: collect error
   e. G5: ≥1 KICKOFF event → fail: collect error
4. Nếu có bất kỳ error → return 422 { errors: [...] }  (KHÔNG UPDATE)
5. Nếu tất cả pass:
   BEGIN TRANSACTION
     UPDATE hackathons SET status='ONGOING', updated_at=NOW() WHERE id=:id
     INSERT audit_logs (action='HACKATHON_STATUS_CHANGE', detail={from:'DRAFT',to:'ONGOING',...})
   COMMIT
6. Async: gửi notification HACKATHON_OPEN
7. Response: 200 OK { status: 'ONGOING' }
9. FR-07B — Safety Net: Validate weight khi kích hoạt Round
Mô tả: Tầng bảo vệ thứ 3 — block rounds.is_active=TRUE nếu Criteria chưa hợp lệ. Ngăn trường hợp Coordinator thêm/sửa Criteria sau khi Hackathon đã ONGOING nhưng trước khi activate Round.

Workflow v5.0 ref: GĐ3 — Bước 1 (khi activate Round Sơ loại) | DB v3.0 ref: rounds.is_active, criteria

9.1 Logic validate (App-layer) — cập nhật cho kiến trúc XOR
PATCH /api/v1/rounds/{round_id}/activate

1. Lấy round (is_final, round_type)
2. NẾU is_final = FALSE (Round Sơ loại/Bán kết):
   a. Lấy tất cả tracks WHERE round_id = :round_id AND status != 'CANCELLED'
   b. Với MỖI track:
      total = SELECT SUM(weight) FROM criteria
              WHERE track_id = tr.id AND type != 'PENALTY'
      - Nếu total IS NULL → Block 422: "Track [name] chưa có Criteria"
      - Nếu ABS(total - 1.0) > 0.001 → Block 422: "Track [name]: weight = X.XX"
   c. Validate conflict Mentor↔Judge cho từng Track

3. NẾU is_final = TRUE (Round Chung kết):
   a. total = SELECT SUM(weight) FROM criteria
              WHERE round_id = :round_id AND type != 'PENALTY'
   b. Nếu total IS NULL → Block 422: "Round Chung kết chưa có Criteria"
   c. Nếu ABS(total - 1.0) > 0.001 → Block 422: "Round CK: weight = X.XX"
   d. Validate 100% Judge FINAL_EXTERNAL (không có Judge INTERNAL trừ ngoại lệ)

4. Kiểm tra: chỉ 1 Round is_active=TRUE per hackathon tại 1 thời điểm
   SELECT COUNT(*) FROM rounds WHERE hackathon_id=:hid AND is_active=TRUE
   Nếu > 0 → SET is_active=FALSE cho round đang active trước

5. Nếu tất cả pass:
   UPDATE rounds SET is_active=TRUE WHERE id=:round_id
   INSERT audit_logs (action='ROUND_ACTIVATE', ...)
   Gửi notification ROUND_STARTED cho Judge, Mentor, đội
9.2 Bảng liên quan
Bảng	Vai trò	Điều kiện
criteria	Nguồn validate weight	Sơ loại: WHERE track_id IN (Track của Round). Chung kết: WHERE round_id=:id
tracks	Lấy danh sách Track của Round	WHERE round_id=:id AND status != 'CANCELLED'
rounds	Target activate + deactivate others	UPDATE is_active=TRUE; deactivate round cũ
judge_assignments	Validate Judge Chung kết	100% assignment_type='FINAL_EXTERNAL' khi Round FINAL
notifications	Gửi ROUND_STARTED	Async: Judge, Mentor, Đội trong Hackathon
audit_logs	Ghi lịch sử	action='ROUND_ACTIVATE'

---

11. Điểm thiết kế quan trọng — Dev Notes
11.1 Thứ tự tạo cấu trúc — BẮT BUỘC
Hackathon (DRAFT)
   ↓
Rounds (hackathon_id) — tạo Sơ loại trước, Chung kết sau
   ↓
Tracks (round_id) — CHỈ tạo trong Round is_final=FALSE
   ↓
Criteria — XOR: track_id (Sơ loại) hoặc round_id (Chung kết)
Sai thứ tự → FK constraint fail. Không thể tạo Track trước Round.

11.2 XOR FK Pattern — Áp dụng ở 3 bảng
Cùng một pattern (X IS NOT NULL AND Y IS NULL) OR (X IS NULL AND Y IS NOT NULL) áp dụng tại:

criteria: (track_id, round_id)
submissions: (track_id, round_id)
judge_assignments: (track_id, round_id)
MySQL không hỗ trợ partial UNIQUE `WHERE`. Dùng **generated column** + UNIQUE (xem [schema-v3.0-mysql.md](../db/schema-v3.0-mysql.md) §1.1): `track_uk`, `round_uk` trên `judge_assignments`, `submissions`.

11.3 Conflict Rules — 3 lớp enforcement (FR-05)

| Lớp | Cơ chế | Hành vi |
|-----|--------|---------|
| **1 — DB** | `trg_check_mentor_judge_conflict_ins/upd`, `trg_check_judge_mentor_conflict_ins` | **BLOCK CỨNG** — `SIGNAL SQLSTATE '45000'`; Hibernate → 422 |
| **2 — App** | Pre-check trước INSERT (khuyến nghị) | Trả 422 sớm với `code` map từ MESSAGE_TEXT (§10.4) |
| **3 — UI preview** | Gợi ý trước submit (optional) | Chỉ preview; **không** trả 201 + warning cho conflict đã vi phạm |

**Không** dùng `warnings[]` cho Mentor↔Judge conflict sau khi DB trigger đã bật (v3.0). `warnings[]` chỉ cho: FR-04 weight, FR-06 Lớp 3, FR-05c Judge final-at-phase1 (nếu vẫn cho phép ở GĐ1 — khuyến nghị block).

**assignment_type** tại GĐ1 (Track Sơ loại): `NORMAL` (mặc định), `HEAD` (trưởng nhóm Judge), `CALIBRATION` (phiên hiệu chuẩn — GĐ3). `FINAL_EXTERNAL` **chỉ** khi `track_id IS NULL` và `round_id` = Round FINAL (GĐ4).
11.4 Validate weight — 3 tầng, không được skip
Tầng	Khi nào	Chế độ
1 — UI realtime	Khi Coordinator nhập từng criterion	Warn mềm (hiển thị tổng hiện tại)
2 — Gate ONGOING	PATCH /hackathons/:id/status	Block cứng — không UPDATE nếu fail
3 — Activate Round	PATCH /rounds/:id/activate	Block cứng — ngăn bypass tầng 2
Dev KHÔNG ĐƯỢC validate cứng ở Tầng 1 — sẽ block UX nhập liệu.

11.5 is_dept_head — Quy trình ngoại lệ Chung kết
1. Coordinator xác định GV là Trưởng khoa/bộ môn → PATCH /users/{id} { is_dept_head: true }
2. Đảm bảo GV không có mentor_assignment trong kỳ (NOT EXISTS check)
3. Phân công Judge Chung kết → trigger cho phép, ghi audit DEPT_HEAD_FINAL_JUDGE_EXCEPTION
Không có tự động — Coordinator phải explicit set is_dept_head=TRUE trước.

11.6 Round Chung kết — Không có Track, Judge phân công GĐ4
KHÔNG tạo Track cho Round Chung kết (trigger block)
KHÔNG phân công Judge Chung kết ở GĐ1 (làm ở GĐ4 Bước 6)
Criteria Chung kết gắn round_id (không phải track_id)
submissions Chung kết dùng round_id, track_id=NULL
11.7 Async Workers cần thiết ở GĐ1
Worker	Trigger	Công việc
email_judge_invitation	Sau INSERT users (Judge tạm)	Gửi link one-time-use; log invitations.email_sent_at
notification_reminder	Sau INSERT events	INSERT notifications (type=REMINDER) cho tất cả APPROVED users
notification_hackathon_open	Sau UPDATE hackathons status=ONGOING	INSERT notifications (type=HACKATHON_OPEN)
12. Pending Items — Chờ BTC xác nhận
Mã	Nội dung	Tác động	Trạng thái
Pending #5	Công thức tính điểm Chapter (chapter_scoring_formula)	chapter_rankings.total_score chưa tính được cho đến khi BTC định nghĩa công thức	Pending
Pending #6	Judge INTERNAL vs EXTERNAL có trọng số khác nhau không? (judge_weight FLOAT DEFAULT 1.0 trong judge_assignments)	Nếu có → cộng điểm weighted; cần thêm cột vào judge_assignments	Pending
Pending #1	Tiebreak Level 2 sau Penalty — BTC quyết định xử lý đồng điểm tiếp theo	Cần thêm tiebreak_level_2 vào rounds hoặc priority_score vào tiebreak_evaluations	Pending
Pending #3	Cơ chế khiếu nại (appeals) — BTC có cho phép không?	Cần thêm bảng appeals(team_id, round_id, reason, evidence_url, status, ...)	Pending
Pending: Phân quyền Coordinator	Nếu cần Coordinator chỉ quản lý Hackathon của mình	Thêm bảng hackathon_coordinators(hackathon_id, user_id). Hiện tại: 1 Coordinator = toàn quyền hệ thống	Pending
13. Trigger Summary — MySQL (đồng bộ schema-v3.0-mysql.md §5)

| Trigger | Bảng | Event | Mục đích |
|---------|------|-------|----------|
| `trg_prevent_track_in_final_round_ins` / `_upd` | tracks | BEFORE INSERT/UPDATE | Block Track trong Round FINAL |
| `trg_check_mentor_judge_conflict_ins` / `_upd` | judge_assignments | BEFORE INSERT/UPDATE | Mentor↔Judge; INTERNAL CK; FINAL_EXTERNAL |
| `trg_check_judge_mentor_conflict_ins` | mentor_assignments | BEFORE INSERT | Judge↔Mentor; FINAL Judge không Mentor |
| `trg_check_criteria_round_is_final_ins` / `_upd` | criteria | BEFORE INSERT/UPDATE | `round_id` chỉ khi Round FINAL |
| `trg_check_submission_round_is_final_ins` / `_upd` | submissions | BEFORE INSERT/UPDATE | XOR + HARD_LOCK |
| `trg_check_team_track_same_hackathon_ins` / `_upd` | team_round_tracks | BEFORE INSERT/UPDATE | Cùng hackathon; Track ∉ FINAL |
| `trg_lock_score_insert` / `_update` | scores | BEFORE INSERT/UPDATE | GĐ3 — scoring_locked |
| `trg_lock_member_insert` / `_update` | team_members | BEFORE INSERT/UPDATE | GĐ2 — is_locked |
| `trg_audit_team_status` | teams | AFTER UPDATE | Audit đổi status đội |

`updated_at`: `@PreUpdate` entity hoặc `ON UPDATE CURRENT_TIMESTAMP` (MySQL) — không dùng `fn_set_updated_at()` PostgreSQL.

---

## 14. Appendix A — Implementation drift (2026-05)

> **Cập nhật 2026-05:** Backend đã migrate theo §10 (route v3 primary + legacy delegate + readiness G1–G5). Bảng dưới ghi **đã xử lý** vs **còn lệch nhỏ**.

> **Không normative.** Spec target v3.0 là §10.

### 14.1 URL routing — đã xử lý

| Nghiệp vụ | Target v3.0 (§10) | Code |
|-----------|-------------------|------|
| Tạo Round | `POST .../hackathons/{hackathonId}/rounds` | **Primary** + legacy `POST .../tracks/{trackId}/rounds` delegate |
| Tạo Track | `POST .../rounds/{roundId}/tracks` | **Primary** + legacy `POST .../hackathons/{id}/tracks` |
| Criteria Sơ loại | `POST .../tracks/{trackId}/criteria` | **Có** (batch, weight-summary, clone) |
| List Judge Sơ loại | `GET .../tracks/{trackId}/judges` | **Primary**; legacy `GET .../rounds/{roundId}/judges` |

### 14.2 Readiness & activate — đã xử lý (audit 2026-05)

- **G1–G5:** `HackathonReadinessServiceImpl` — PRELIMINARY/SEMIFINAL + tracks, 1 FINAL, weight `criteria.track_id` / `round_id`, KICKOFF + validate Lớp 1+2 trên từng event KICKOFF.
- **Blocker codes:** `MISSING_PRELIMINARY_ROUND`, `MISSING_FINAL_ROUND`, `TRACK_CRITERIA_WEIGHT`, `FINAL_CRITERIA_WEIGHT`, `EVENT_KICKOFF_MISSING`, …
- **FR-07B:** `RoundActivationServiceImpl` — weight per track/final, conflict Mentor↔Judge per track, FINAL chỉ `FINAL_EXTERNAL`, `ROUND_STARTED` notification.

### 14.3 FR-05 conflict & DTO — đã xử lý

- Mentor↔Judge cùng track: **422** `CONFLICT_SAME_TRACK` (cả hai chiều Judge assign + Mentor assign).
- Judge Chung kết → Mentor: **422** `FINAL_JUDGE_CANNOT_BE_MENTOR`.
- Mentor assign: Track phải **OPEN**, không **CANCELLED**.

### 14.4 Còn lệch nhỏ (không chặn GĐ1)

| Mục | Ghi chú |
|-----|---------|
| Clone nguồn §5.6 | Không có `GET /criteria?track_id=` — dùng `sourceTrackId` trong `POST .../clone` |
| PATCH status body | Nhận cả `"status"` và `"targetStatus"` (`@JsonAlias`) |
| `PUT /tracks/{id}` topic | Cần body `topic`; chỉ cho gán topic khác rỗng sau khi đã có event KICKOFF |
| `ErrorCode` JavaDoc | Comment FR-02/03 có thể còn số v2.x — map theo §10.4 |
| JWT / email async | Stub `@CoordinatorOnly`; Judge invite email sync trong transaction |

### 14.5 ErrorCode.java — đánh số FR

Comment trong `ErrorCode.java` gán **FR-02 = Track**, **FR-03 = Round** (v2.x). mf01 v3.0: **FR-02 = Round**, **FR-03 = Track**. Map lỗi theo §10.4; đổi tên constant không bắt buộc cho doc.

### 14.6 Tham chiếu chéo workflow.md

| workflow.md (có thể lỗi thời) | mf01 v3.0 |
|-------------------------------|-----------|
| §1.3 `submissions(team_id, round_id)` | XOR `track_id` / `round_id` |
| §2.3 bảng `final_criteria` riêng | `criteria.round_id` khi FINAL |
| Header PostgreSQL 15+ | MySQL 8 — [schema-v3.0-mysql.md](../db/schema-v3.0-mysql.md) |

**Ưu tiên:** schema > mf01 > workflow đoạn cũ.

---

SEAL Hackathon Management System — MF-01 DB Analysis v3.0 — Workflow v5.0 · DB Schema v3.0 (MySQL 8) — Kiến trúc Hackathon → Round → Track — FPT University HCMC

