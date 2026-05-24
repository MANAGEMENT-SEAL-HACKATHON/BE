SEAL HACKATHON MANAGEMENT SYSTEM
Workflow v5.0 & DB Schema v3.0
Kiến trúc Hackathon → Round → Track
Breaking Change — Thiết kế lại từ gốc dựa trên thực tế vận hành Fall 2025 & Spring 2026
System Analyst · Solution Architect · Business Analyst (Senior) · Enterprise Architect · QA/Risk Analyst
1. Tại sao phải thiết kế lại — Phân tích gốc rễ
[BREAKING CHANGE — v5.0] Toàn bộ schema và workflow được thiết kế lại từ gốc. Đây không phải patch mà là rethink kiến trúc. Mọi code và tài liệu cũ (v2.x, v3.0, v4.x) cần được thay thế hoàn toàn.
1.1 Vấn đề cốt lõi của kiến trúc cũ (Hackathon → Track → Round)
•        Track được hiểu là "hạng mục cố định" xuyên suốt Hackathon — nhưng thực tế Track là "bảng đấu trong một vòng thi cụ thể", thay đổi theo Round.
•        Chung kết không thuộc Track nào → phải hack track_id=NULL — vi phạm nguyên tắc thiết kế sạch.
•        Nếu thêm Round bán kết → phải tạo Track mới ở cấp Hackathon → vô lý, không scale.
•        Judge phân công "theo Track" mà Track nào? Track Round 1 hay Track Round 2? — Nhập nhằng.
•        Workflow không phản ánh đúng thực tế: BTC tổ chức theo vòng (Round) trước, rồi mới chia bảng (Track) trong vòng đó.
1.2 Kiến trúc mới — Hackathon → Round → Track
Round là vòng thi. Track là bảng đấu trong vòng đó. Mỗi Round có thể có số Track khác nhau, quy mô khác nhau. Đây là mô hình tournament bracket chuẩn, hoàn toàn phản ánh đúng thực tế vận hành.
HACKATHON  (VD: Fall 2025)
├── Round 1: Sơ loại  (round_type=PRELIMINARY, sequence_order=1)
│	├── Track 1  (chủ đề SDLC nhóm 1)
│	│    ├── Bảng A  →  ≤6 đội  [assigned_group]
│	│    └── Bảng B  →  ≤6 đội  [assigned_group]
│	│         top 2/bảng → 4 đội từ Track 1
│	└── Track 2  (chủ đề SDLC nhóm 2)
│     	├── Bảng C  →  ≤6 đội  [assigned_group]
│     	└── Bảng D  ...
│          	top 2/bảng → tổng đủ 6 đội vào Chung kết
│
└── Round 2: Chung kết  (round_type=FINAL, is_final=TRUE)
 	└── [KHÔNG CÓ TRACK — KHÔNG CÓ BẢNG]
      	6 đội pool chung  →  100% Judge EXTERNAL
 
Fall 2025: 2 Track × nhiều bảng ≤6 đội/bảng → top 2/bảng = 6 đội Chung kết. Spring 2026: số Track = số bảng, mỗi Track=1 bảng ≤8 đội → top 2/Track = 6 đội Chung kết. Round Chung kết is_final=TRUE không có Track con — hoàn toàn tự nhiên, không cần hack.
1.3 So sánh kiến trúc cũ vs mới
Điểm
v2.x/v4.x (cũ)
v5.0 (mới)
Loại
Cấu trúc DB
Hackathon → Track → Round Round là con của Track
Hackathon → Round → Track Track là con của Round
SỬA
Ý nghĩa Track
Hạng mục cố định xuyên Hackathon (VD: Track AI, Track Web)
Bảng đấu trong Round cụ thể (VD: Bảng A, Bảng B của Round Sơ loại)
SỬA
Round Chung kết
Phải hack track_id=NULL Vi phạm thiết kế
Round không có Track → hoàn toàn tự nhiên Không cần hack gì
SỬA
Thêm Round bán kết
Phải tạo Track mới ở cấp Hackathon Vô lý, không scale
Tạo Round mới, tạo Track trong Round Tự nhiên, scale tốt
SỬA
Phân công Judge
Judge theo Track (hackathon-level Track) Nhập nhằng khi có nhiều Round
Judge theo Track trong Round cụ thể Rõ ràng: judge_assignments(judge_id, track_id)
SỬA
Phân công Mentor
Mentor theo Track (hackathon-level) Không rõ vòng nào
Mentor theo Track trong Round Sơ loại Rõ ràng: mentor_assignments(mentor_id, track_id)
SỬA
Conflict rule
Nhập nhằng: "không Mentor Track X mà Judge Track X" Track X ở vòng nào?
Rõ ràng: không Mentor Track X (Round Sơ loại) mà Judge Track X cùng Round đó
SỬA
Team phân công
teams.registration_track_id + assigned_track_id (hackathon-level Track)
team_round_track: đội thi tại Track nào trong Round nào Flexible theo từng Round
SỬA
Submission
submissions(team_id, round_id) Không biết team thi ở Track nào trong Round
submissions(team_id, track_id) track_id đã mang thông tin round qua FK
SỬA
Criteria
criteria(round_id) Round thuộc Track thuộc Hackathon
criteria(track_id) track_id thuộc round thuộc hackathon — clean
SỬA


 
2. DB Schema v3.0 — Hackathon → Round → Track
DB Schema v3.0 — PostgreSQL 15+. Breaking change hoàn toàn với v2.x. Mọi bảng liên quan đến Track và Round đều thay đổi cấu trúc.
2.1 Bảng rounds — Cấp cha trực tiếp của Track
Trường / Thay đổi
Kiểu / Ràng buộc
Ghi chú nghiệp vụ
id
SERIAL PK
Auto-increment
hackathon_id
INT FK hackathons.id ON DELETE CASCADE NOT NULL
Round thuộc Hackathon
name
VARCHAR(100) NOT NULL
VD: "Vòng Sơ loại", "Vòng Chung kết"
sequence_order
INT NOT NULL
Thứ tự vòng: 1=Sơ loại, 2=Chung kết. UNIQUE per hackathon_id
is_final
BOOLEAN NOT NULL DEFAULT FALSE
TRUE = Round Chung kết — không có Track con. FALSE = có Track con (Sơ loại/Bán kết)
round_type
VARCHAR(20) NOT NULL DEFAULT 'PRELIMINARY' CHECK IN ('PRELIMINARY','SEMIFINAL','FINAL')
Phân loại Round. FINAL: không có Track, pool chung, 100% Judge EXTERNAL
coding_duration_hours
INT
Số giờ coding. Thực tế 2 mùa = 7 giờ
submission_open
TIMESTAMP
Thời điểm mở nhận bài (tuỳ chọn)
submission_deadline
TIMESTAMP NOT NULL
Hạn nộp bài. Bài sau deadline → LATE
late_submission_policy
VARCHAR(20) NOT NULL DEFAULT 'ALLOW_LATE_PENDING' CHECK IN ('ALLOW_LATE_PENDING','HARD_LOCK')
Sơ loại: ALLOW_LATE_PENDING. Chung kết: HARD_LOCK — không có LATE_PENDING
problem_statement_url
TEXT
URL đề bài — set khi phát đề
problem_released_at
TIMESTAMP
Thời điểm phát đề chính thức
top_n_advance
INT
Số đội top N MỖI BẢNG (assigned_group) vào Round tiếp. Fall 2025: top 2/bảng × số bảng = 6 đội. Spring 2026: top 2/Track (vì mỗi Track=1 bảng). NULL nếu is_final=TRUE
min_teams_final
INT
Số đội tối thiểu cần vào Round tiếp — kích hoạt Wild Card nếu thiếu
wildcard_enabled
BOOLEAN NOT NULL DEFAULT FALSE
Cho phép Wild Card ở Round này. Cả hackathon.wildcard_enabled VÀ round.wildcard_enabled phải TRUE
tiebreak_rule
VARCHAR(50) DEFAULT 'PENALTY_SCORE' CHECK IN ('PENALTY_SCORE','SUBMISSION_TIME','COORDINATOR_DECISION')
Luật xử lý đồng điểm
is_active
BOOLEAN NOT NULL DEFAULT FALSE
TRUE = Round đang diễn ra
scoring_locked
BOOLEAN NOT NULL DEFAULT FALSE
TRUE = đã khóa chấm điểm — mọi score → is_final=TRUE
scoring_locked_at / by
TIMESTAMP / INT FK users
Thời điểm và người khóa
force_locked / reason
BOOLEAN / TEXT
Force-lock kèm lý do bắt buộc
created_at
TIMESTAMP NOT NULL DEFAULT NOW()
Auto set

2.2 Bảng tracks — Bảng đấu con của Round
[THIẾT KẾ LẠI] Track không còn là hạng mục cố định của Hackathon mà là bảng đấu thuộc Round cụ thể. tracks.hackathon_id bị XÓA; thay bằng tracks.round_id.
Trường / Thay đổi
Kiểu / Ràng buộc
Ghi chú nghiệp vụ
id
SERIAL PK
Auto-increment
round_id
INT FK rounds.id ON DELETE CASCADE NOT NULL
[MỚI] Track thuộc Round cụ thể. Thay thế hackathon_id cũ
name
VARCHAR(200) NOT NULL
VD: "Bảng A", "Track AI RAG", "Track 1 — SDLC"
description
TEXT
Mô tả chủ đề/nội dung thi đấu của bảng này
max_teams
INT
Tổng số đội tối đa trong Track này (tất cả bảng cộng lại). VD: Fall 2025 Track 1 có 3 bảng × 6 đội = 18 đội tổng
max_teams_per_group
INT
[GIỮ TỪ v2.1] Số đội tối đa MỖI BẢNG (assigned_group) trong Track. Fall 2025=6, Spring 2026=8. Track có nhiều bảng → mỗi bảng ≤ max_teams_per_group
topic
VARCHAR(300)
[MỚI] Chủ đề bốc thăm của Track này trong Round. VD: "Business Analysis App"
sequence_order
INT NOT NULL DEFAULT 1
[MỚI] Thứ tự Track trong Round. UNIQUE per round_id
status
VARCHAR(20) NOT NULL DEFAULT 'OPEN' CHECK IN ('OPEN','CLOSED','CANCELLED')
Trạng thái Track
min_team_size
INT NOT NULL DEFAULT 3
Số thành viên tối thiểu mỗi đội (3 theo thực tế)
max_team_size
INT NOT NULL DEFAULT 5
Số thành viên tối đa mỗi đội (5 theo thực tế)

Lưu ý: tracks.hackathon_id (cũ) → XÓA. Muốn biết Track thuộc Hackathon nào: Track → Round → Hackathon (qua JOIN).
2.3 Bảng criteria — Tiêu chí chấm điểm theo Track
[THAY ĐỔI] criteria.round_id (cũ) → criteria.track_id (mới). Tiêu chí gắn vào Track cụ thể trong Round, không phải Round tổng. Track Chung kết (pool — không có Track con) → criteria vẫn gắn vào round_id trực tiếp qua bảng final_criteria riêng biệt.
Trường / Thay đổi
Kiểu / Ràng buộc
Ghi chú nghiệp vụ
id
SERIAL PK
 
track_id
INT FK tracks.id ON DELETE CASCADE NOT NULL
[MỚI] Thay round_id cũ. Criteria gắn vào Track trong Round cụ thể
round_id [FINAL only]
INT FK rounds.id ON DELETE CASCADE
[MỚI] Chỉ dùng cho Round Chung kết (is_final=TRUE) — không có Track con. NULL cho Round thường
source_criteria_id
INT FK criteria.id ON DELETE SET NULL
Self-ref — kế thừa từ kỳ trước. Chỉ trace lịch sử, không cascade
name
VARCHAR(200) NOT NULL
Tên tiêu chí
type
VARCHAR(20) NOT NULL CHECK IN ('TECHNICAL','SOFT_SKILL','PENALTY')
PENALTY không tính vào weight tổng, chỉ dùng tiebreak
weight
FLOAT NOT NULL CHECK (weight > 0 AND weight <= 1)
Tổng weight (không PENALTY) của mọi criteria trong Track = 1.0
max_score
INT NOT NULL DEFAULT 10
Điểm tối đa
description / rubric_url
TEXT / TEXT
Mô tả và link rubric chi tiết
display_order
INT NOT NULL DEFAULT 0
Thứ tự hiển thị trong UI chấm điểm

Constraint: CHECK (track_id IS NOT NULL OR round_id IS NOT NULL) AND NOT (track_id IS NOT NULL AND round_id IS NOT NULL) — đúng 1 trong 2 phải có giá trị.
2.4 Bảng team_round_tracks — Đội thi tại Track nào trong Round nào
[MỚI HOÀN TOÀN] Thay thế teams.registration_track_id, teams.assigned_track_id và teams.assigned_group. Một đội có thể thi ở Track khác nhau qua các Round (VD: Round 1 Track A, Round 2 Track X). assigned_group (tên bảng trong Track) cũng chuyển vào đây.
Trường / Thay đổi
Kiểu / Ràng buộc
Ghi chú nghiệp vụ
id
SERIAL PK
 
team_id
INT FK teams.id ON DELETE CASCADE NOT NULL
Đội thi
track_id
INT FK tracks.id ON DELETE CASCADE NOT NULL
Track của đội trong Round này. track_id đã mang round_id qua FK
assigned_group
VARCHAR(50)
[CHUYỂN TỪ teams] Tên bảng đấu trong Track (VD: "A", "B", "C"). NULL nếu Track chỉ có 1 bảng. Fall 2025: Track có nhiều bảng → cần. Spring 2026: Track=1 bảng → NULL hoặc tên Track
registration_type
VARCHAR(20) NOT NULL DEFAULT 'ASSIGNED' CHECK IN ('PREFERRED','ASSIGNED')
PREFERRED: đội đăng ký muốn thi Track này (Fall 2025 đội tự chọn); ASSIGNED: BTC gán sau bốc thăm (Spring 2026)
assigned_at
TIMESTAMP NOT NULL DEFAULT NOW()
Thời điểm phân công
assigned_by
INT FK users.id
Coordinator thực hiện phân công
(constraint)
UNIQUE(team_id, track_id)
Mỗi đội chỉ được phân công 1 lần vào 1 Track trong Round

2.5 Bảng judge_assignments — Judge theo Track trong Round
Trường / Thay đổi
Kiểu / Ràng buộc
Ghi chú nghiệp vụ
id
SERIAL PK
 
judge_id
INT FK users.id ON DELETE CASCADE NOT NULL
Judge (INTERNAL hoặc EXTERNAL)
track_id
INT FK tracks.id ON DELETE CASCADE
[MỚI] NULL = Judge Chung kết (is_final Round). NOT NULL = Judge Sơ loại của Track đó
round_id [FINAL]
INT FK rounds.id ON DELETE CASCADE
[MỚI] Chỉ dùng khi track_id IS NULL (Round Chung kết is_final=TRUE)
assignment_type
VARCHAR(20) NOT NULL DEFAULT 'NORMAL' CHECK IN ('NORMAL','HEAD','CALIBRATION','FINAL_EXTERNAL')
FINAL_EXTERNAL: bắt buộc ở Round Chung kết. NORMAL/HEAD/CALIBRATION: ở Round Sơ loại/Bán kết
assigned_at / assigned_by
TIMESTAMP / INT FK users
Auto set / Coordinator phân công
(partial unique)
UNIQUE(judge_id, track_id) WHERE track_id IS NOT NULL
[MỚI] Sơ loại: 1 Judge / 1 Track. Dùng partial unique index vì NULL
(partial unique)
UNIQUE(judge_id, round_id) WHERE track_id IS NULL
[MỚI] Chung kết: 1 Judge phân công 1 lần / Round Chung kết

2.6 Bảng mentor_assignments — Mentor theo Track trong Round
[GIỮ NGUYÊN ý nghĩa, SỬA FK] Mentor vẫn theo Track — nhưng giờ track_id thuộc Round cụ thể nên tự nhiên mang đủ ngữ cảnh mà không cần thêm round_id riêng.
Trường / Thay đổi
Kiểu / Ràng buộc
Ghi chú nghiệp vụ
id
SERIAL PK
 
mentor_id
INT FK users.id ON DELETE CASCADE NOT NULL
Phải role=MENTOR AND status=APPROVED
track_id
INT FK tracks.id ON DELETE CASCADE NOT NULL
Track trong Round Sơ loại. Track đã mang round_id → tự biết vòng nào. Không có Mentor ở Round Chung kết (is_final=TRUE)
assigned_at / assigned_by
TIMESTAMP / INT FK users
Auto set / Coordinator phân công
(constraint)
UNIQUE(mentor_id, track_id)
1 Mentor chỉ phụ trách 1 Track trong 1 Round. Khác Round → tạo bản ghi mới

2.7 Bảng submissions — Nộp bài theo Track (không phải Round)
[THAY ĐỔI] submissions.round_id → submissions.track_id. Đội nộp bài cho Track cụ thể của mình. Từ track_id → round_id → hackathon_id qua JOIN. Round Chung kết (is_final) → submissions.track_id=NULL, dùng submissions.round_id trực tiếp.
Trường / Thay đổi
Kiểu / Ràng buộc
Ghi chú nghiệp vụ
id
SERIAL PK
 
team_id
INT FK teams.id NOT NULL
Đội nộp bài
track_id
INT FK tracks.id
[MỚI] Nộp bài cho Track nào (Sơ loại). NULL nếu là Round Chung kết
round_id [FINAL]
INT FK rounds.id
[MỚI] Chỉ dùng khi track_id IS NULL (Round Chung kết)
repo_url / demo_url
TEXT / TEXT
GitHub/GitLab / demo link
slide_url
TEXT
Bắt buộc dạng slide — không chấp nhận PDF/Confluence (thực tế 2 mùa)
report_url
TEXT
Optional — link báo cáo bổ sung
status
VARCHAR(20) NOT NULL DEFAULT 'SUBMITTED' CHECK IN ('SUBMITTED','LATE','LATE_PENDING','LATE_APPROVED','REJECTED','ACCEPTED')
LATE_PENDING chỉ có ở Round Sơ loại (late_submission_policy=ALLOW_LATE_PENDING). Chung kết: HARD_LOCK
is_late / late_reason
BOOLEAN / TEXT
Tự động set khi nộp sau deadline
reviewed_by / reviewed_at / review_note
INT FK / TIMESTAMP / TEXT
Coordinator xét duyệt LATE_PENDING
submitted_at
TIMESTAMP NOT NULL DEFAULT NOW()
Auto set
(constraint)
UNIQUE(team_id, track_id) WHERE track_id IS NOT NULL UNIQUE(team_id, round_id) WHERE track_id IS NULL
Mỗi đội chỉ nộp 1 bài / Track (Sơ loại) hoặc 1 bài / Round Chung kết

2.8 Bảng scores — Điểm chấm theo submission + criterion
[GIỮ NGUYÊN PHẦN LỚN] scores không thay đổi nhiều vì đã join qua submission → track/round. Criterion giờ gắn vào track_id → tự nhiên mang context Round.
Trường / Thay đổi
Kiểu / Ràng buộc
Ghi chú nghiệp vụ
id
SERIAL PK
 
submission_id
INT FK submissions.id ON DELETE CASCADE NOT NULL
Bài nộp được chấm
judge_id
INT FK users.id NOT NULL
Judge chấm
criterion_id
INT FK criteria.id NOT NULL
Tiêu chí. criterion → track → round → hackathon (chain rõ ràng)
score_value
FLOAT NOT NULL CHECK >= 0
Điểm thô. Không gộp — lưu riêng từng Judge (RBL)
comment
TEXT
Nhận xét của Judge
score_type
VARCHAR(20) NOT NULL DEFAULT 'NORMAL' CHECK IN ('NORMAL','CALIBRATION','PENALTY')
NORMAL: điểm chính thức. CALIBRATION: RBL. PENALTY: tiebreak
is_final
BOOLEAN NOT NULL DEFAULT FALSE
FALSE cho đến khi Coordinator lock Round → batch set TRUE
calibration_session_id
INT FK calibration_sessions.id ON DELETE SET NULL
Liên kết với phiên RBL nếu CALIBRATION
scored_at / updated_at
TIMESTAMP
Auto set
(constraint)
UNIQUE(submission_id, judge_id, criterion_id, score_type)
1 Judge chỉ chấm 1 lần / criterion / submission / score_type

2.9 Bảng teams — Đơn giản hóa, bỏ track FK cứng
[THAY ĐỔI] Bỏ registration_track_id và assigned_track_id khỏi bảng teams. Thay bằng bảng team_round_tracks (mục 2.4) để linh hoạt theo từng Round.
Trường / Thay đổi
Kiểu / Ràng buộc
Ghi chú nghiệp vụ
id
SERIAL PK
 
hackathon_id
INT FK hackathons.id ON DELETE CASCADE NOT NULL
[MỚI] Đội thuộc Hackathon nào — giữ FK trực tiếp cho query nhanh
registration_track_id
[XÓA]
[XÓA] Dùng team_round_tracks thay thế
assigned_track_id
[XÓA]
[XÓA] Dùng team_round_tracks thay thế
team_name
VARCHAR(200) NOT NULL UNIQUE
Tên đội
leader_id
INT FK users.id NOT NULL
Team Leader
chapter_id
INT FK chapters.id ON DELETE SET NULL
Chapter/trường của đội
status
VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK IN ('PENDING','ACTIVE','ELIMINATED','REJECTED')
Trạng thái đội
rejection_reason
TEXT
Lý do reject
assigned_group
[XÓA — move to team_round_tracks]
[XÓA] Thứ tự/vị trí trong bảng được lưu ở team_round_tracks
is_locked / locked_at
BOOLEAN / TIMESTAMP
TRUE sau registration_end — trigger block thay đổi thành viên
eliminated_at / reason
TIMESTAMP / TEXT
Thời điểm và lý do loại
created_at
TIMESTAMP NOT NULL DEFAULT NOW()
Auto set

2.10 Tóm tắt các thay đổi DB — Migration guide
Bảng
Loại thay đổi
Nội dung cụ thể
rounds
SỬA BREAKING
hackathon_id thay track_id. Thêm is_final, round_type, late_submission_policy. Bỏ track_id FK.
tracks
SỬA BREAKING
round_id thay hackathon_id. Thêm topic, sequence_order. GIỮ max_teams và max_teams_per_group (cả 2 cần thiết cho Fall 2025 Track nhiều bảng).
criteria
SỬA BREAKING
track_id thay round_id (Sơ loại). Thêm round_id riêng cho Chung kết (is_final). CHECK constraint XOR.
team_round_tracks
MỚI HOÀN TOÀN
Thay thế teams.registration_track_id, assigned_track_id và teams.assigned_group. Nhiều-nhiều: team ↔ track (per round) + assigned_group (bảng trong Track).
teams
SỬA
Thêm hackathon_id. Bỏ registration_track_id, assigned_track_id, assigned_group.
submissions
SỬA BREAKING
track_id thay round_id (Sơ loại). round_id giữ lại chỉ cho Chung kết. Partial UNIQUE index.
judge_assignments
SỬA
track_id thay round_id (Sơ loại). round_id giữ cho Chung kết. Thêm FINAL_EXTERNAL type. Partial index.
mentor_assignments
GIỮ UNIQUE, SỬA FK
UNIQUE(mentor_id, track_id) giữ nguyên. track_id giờ FK vào tracks(round_id) → tự mang ngữ cảnh Round.
submissions
SỬA
UNIQUE(team_id, track_id) cho Sơ loại. UNIQUE(team_id, round_id) cho Chung kết.
prizes
SỬA
track_id FK vào tracks mới (thuộc round). round_id vẫn giữ.
tiebreak_evaluations
SỬA
round_id giữ nguyên — tiebreak tính ở cấp Round.
wildcard_reviews
SỬA
round_id giữ nguyên + thêm track_id để biết Wild Card từ Track nào.
events
GIỮ NGUYÊN
hackathon_id. Bỏ TEAM_MEETING khỏi type CHECK.
chapter_rankings / individual_rankings
GIỮ NGUYÊN
hackathon_id. Không đổi.


 
3. Workflow v5.0 — 6 Giai đoạn vận hành
Workflow v5.0 đồng bộ kiến trúc Hackathon→Round→Track. Các bước tạo cấu trúc tuân theo đúng thứ tự: Hackathon → Round → Track (trong Round) → Criteria (trong Track). Đồng bộ thực tế Fall 2025 & Spring 2026.
GĐ1 — Chuẩn bị sự kiện
Mục tiêu: Coordinator thiết lập cấu trúc Hackathon→Round→Track trước khi mở đăng ký.
Đầu ra: hackathons.status = ONGOING
STT
Actor
Hành động
Mô tả chi tiết
Exception / Edge Case
1
Coordinator
Tạo Hackathon
Tạo kỳ thi với: name, season (Spring/Summer/Fall), year, chủ đề, mô tả, quy chế. Trạng thái: DRAFT. Cấu hình toàn kỳ: wildcard_enabled (global toggle), individual_ranking_enabled (Fall 2025=TRUE; Spring 2026=FALSE), chapter_scoring_formula.
Sinh viên không thấy DRAFT. UNIQUE(name, season, year).
2
Coordinator
Tạo Round (Vòng thi)
Tạo các Round theo sequence_order: Round 1 (Sơ loại, round_type=PRELIMINARY), Round 2 (Chung kết, round_type=FINAL, is_final=TRUE). Cấu hình Round Sơ loại: submission_deadline, coding_duration_hours=7 (thực tế 2 mùa), top_n_advance=2 đội/Track, min_teams_final=6, tiebreak_rule, wildcard_enabled, late_submission_policy=ALLOW_LATE_PENDING. Cấu hình Round Chung kết: late_submission_policy=HARD_LOCK, is_final=TRUE.
Round Chung kết (is_final=TRUE) KHÔNG có Track con — thiết kế hoàn toàn tự nhiên. KHÔNG validate weight ở bước này. Mỗi hackathon cần ≥1 Round PRELIMINARY và đúng 1 Round FINAL.
3
Coordinator
Tạo Track trong Round Sơ loại
Trong Round Sơ loại, tạo các Track (bảng đấu): name, topic (chủ đề bốc thăm), max_teams, sequence_order. Thực tế Fall 2025: tạo đúng 2 Track — Track 1 (nhận 1 chủ đề từ Chủ đề 1–2 SDLC, max_teams=6 đội/bảng) và Track 2 (nhận 2 chủ đề từ Chủ đề 3–5, max_teams=6 đội/bảng). Spring 2026: "mỗi Track = 1 bảng, mỗi bảng tối đa 8 đội" — số Track = số bảng theo thực tế đăng ký, max_teams=8. Mỗi Track có Criteria riêng, Judge riêng, Mentor riêng.
Track KHÔNG tạo ở cấp Hackathon — tạo trong Round. Round Chung kết (is_final=TRUE) KHÔNG có Track con. Fall 2025: topic của Track được bốc thăm tại Khai mạc (ngày 1/11) — khi tạo Track trong GĐ1, topic để trống/placeholder, cập nhật sau bốc thăm.
4
Coordinator
Thiết lập Criteria cho từng Track & Round Chung kết
Tạo bộ tiêu chí chấm điểm. Lưu ý: Criteria gắn vào track_id (Sơ loại) hoặc round_id (Chung kết). UI cảnh báo mềm khi tổng weight ≠ 1.0 — không block. Thực tế: Fall 2025 — mỗi Track Sơ loại có cùng 4 tiêu chí (Tính ứng dụng & khả thi 30% + AI tự động hóa & tích hợp 30% + Giao diện & trải nghiệm 20% + Slide trình bày & demo 20%); Chung kết 5 tiêu chí (Hoàn thiện 30%+Sáng tạo 25%+Hiệu quả 20%+Mở rộng 15%+Trình bày & Phản biện 10%). Spring 2026 — Sơ loại 5 tiêu chí (Domain accuracy 30%+Kiến trúc RAG 30%+Ý tưởng & Thuyết trình 15%+Thực thi & Sáng tạo 15%+UX & Giao diện 10%); Chung kết 5 tiêu chí (Xử lý & Truy xuất 30%+Độ tin cậy 20%+Tư duy Agent 20%+Thực tế 20%+Mở rộng 10%). Hỗ trợ kế thừa qua source_criteria_id.
Criteria Sơ loại: gắn vào track_id. Criteria Chung kết: gắn vào round_id (Round không có Track). XOR constraint đảm bảo chỉ 1 trong 2 FK có giá trị. Nếu mọi Track cùng dùng 1 bộ Criteria → vẫn tạo riêng cho từng Track (track_id khác nhau), không share. Chỉ warn mềm khi nhập; gate cứng tại Bước 7.
5
Coordinator
Quản lý nhân sự giải đấu
5a) Tạo tài khoản Judge EXTERNAL (is_temp_account=TRUE) — email MK tạm + link accept, hiệu lực **3 ngày** (loại 3 invitation; xem mf02-invitations-spec.md). 5b) Phân công Mentor theo Track Sơ loại: UNIQUE(mentor_id, track_id). 1 Mentor/1 Track. Không có Mentor ở Round Chung kết. Conflict BLOCK CỨNG: không cho cùng người vừa Mentor Track X vừa Judge Track X. Khác Track = hợp lệ, khuyến khích. 5c) Phân công Judge sơ bộ theo Track Sơ loại: judge_assignments(judge_id, track_id). Judge Chung kết: 100% FINAL_EXTERNAL — phân công tại GĐ4.
[BLOCK CỨNG] Cùng người Mentor Track X mà Judge Track X → 422 CONFLICT_SAME_TRACK. Conflict check: SELECT 1 FROM mentor_assignments WHERE mentor_id=:judge_id AND track_id=:track_id. Judge Chung kết CHƯA phân công ở bước này.
6
Coordinator
Lên lịch sự kiện
Tạo sự kiện theo thứ tự: (1) WORKSHOP: Fall 2025 online 19h30–21h30 ngày 29/10; Spring 2026 online 20h–21h30 ngày 9/4; (2) KICKOFF: offline 14h–17h (Fall 2025: 1/11; Spring 2026: 11/4) — bốc thăm chia Track + họp đội; (3) PRESENTATION: ngày thi chính thức; (4) AWARDS: trao giải. Validate thời gian 3 lớp. Gửi REMINDER tự động.
TEAM_MEETING không tồn tại — đã bỏ khỏi enum. Thứ tự bắt buộc: WORKSHOP < KICKOFF < PRESENTATION < AWARDS.
7
Coordinator
Chuyển DRAFT → ONGOING
Gate cứng 5 điều kiện: (1) ≥1 Round PRELIMINARY có Track con; (2) đúng 1 Round FINAL (is_final=TRUE); (3) mọi Track có Criteria, tổng weight=1.0; (4) Round Chung kết có Criteria (gắn round_id), tổng weight=1.0; (5) ≥1 event type=KICKOFF. Validate fail → hiển thị chi tiết lỗi.
Không quay lại DRAFT. Round Chung kết không cần Track con — đây là thiết kế đúng, không phải lỗi.

Điều kiện chuyển GĐ2: hackathons.status=ONGOING. Cấu trúc Hackathon→Round→Track đầy đủ.
 
GĐ2 — Đăng ký tài khoản & Thành lập đội
Mục tiêu: Sinh viên đăng ký, lập đội, bốc thăm chia Track trong Round Sơ loại.
Đầu ra: Đội status=ACTIVE + team_round_tracks ghi nhận Track/Round + is_locked=TRUE
STT
Actor
Hành động
Mô tả chi tiết
Exception / Edge Case
1
Sinh viên
Đăng ký tài khoản
Email/Mật khẩu (JWT). INTERNAL (SV FPT — student_code FPT) hoặc EXTERNAL (SV trường khác TP.HCM — student_code + institution + thư mời BTC). Trạng thái: PENDING.
Trùng email → 409. SV tốt nghiệp không được tham gia. Fall 2025: đăng ký 1/10→19/10.
2
Coordinator
Xét duyệt tài khoản
Phân luồng: INTERNAL (email @fpt.edu.vn) → auto-approve sau xác thực email; EXTERNAL → duyệt thủ công trong 24–48h. Ghi audit_log + notification ACCOUNT_APPROVED/REJECTED.
Reminder nếu PENDING > 48h. Tài khoản PENDING không nhận lời mời vào đội.
3
Sinh viên
Thành lập đội
Leader (status=APPROVED) tạo đội: tên độc nhất, hackathon_id. Quy mô 3–5 thành viên. Mời thành viên (phải APPROVED); xác nhận/từ chối. Trạng thái đội: PENDING. Đội chưa gắn Track ở bước này — sẽ được phân công tại Khai mạc.
Tên trùng → 409. 1 thí sinh / 1 đội ACTIVE. Chỉ gửi lời mời cho APPROVED.
4
Coordinator
Phê duyệt đội
Phân luồng: đủ điều kiện kỹ thuật (3–5 thành viên APPROVED) → auto-approve; có thành viên EXTERNAL → duyệt thủ công. PENDING → ACTIVE.
<3 thành viên → REJECTED. >5 → không cho tạo. Bulk-approve hỗ trợ.
5
Hệ thống
Khóa thành viên sau deadline
Sau registration_end: tự động is_locked=TRUE, locked_at=NOW(). Trigger DB block mọi thay đổi thành viên sau lock.
Trigger fn_prevent_member_change_when_locked() bắt cả INSERT và UPDATE. Ghi audit MEMBER_CHANGE_DENIED.
6
Coordinator
Khai mạc & Bốc thăm chia Track
Tại KICKOFF (offline, 14h–17h). Fall 2025 (1/11): (1) Đội lần lượt TỰ CHỌN Track 1 hoặc Track 2 theo thứ tự; (2) BTC bốc thăm chủ đề cho từng Track (Track 1=1 chủ đề SDLC, Track 2=2 chủ đề); (3) BTC bốc thăm chia bảng ngẫu nhiên trong từng Track (mỗi bảng ≤6 đội). Lưu ý: Fall 2025 mỗi Track có thể có nhiều bảng nhỏ ≤6 đội. Spring 2026 (11/4): (1) BTC bốc thăm chia Track ngẫu nhiên cho đội; (2) "Mỗi Track = 1 bảng, tối đa 8 đội". Hệ thống cập nhật topic vào tracks và INSERT vào team_round_tracks: {team_id, track_id, registration_type=ASSIGNED}.
Đội vắng mặt → xử lý thủ công (quy chế: có thể loại). Tất cả thành viên phải có mặt để nhận chỗ ngồi, kiểm tra internet. Hệ thống hỗ trợ cả 2 luồng bốc thăm khác nhau giữa 2 mùa.

Điều kiện chuyển GĐ3: ≥1 đội ACTIVE có bản ghi trong team_round_tracks (biết Track/Round Sơ loại). is_locked=TRUE.
 
GĐ3 — Vòng Sơ loại (Song song theo Track trong Round 1)
Mục tiêu: Các Track thi đấu đồng thời, chấm điểm độc lập theo Track.
Đầu ra: Round Sơ loại scoring_locked=TRUE. Bảng điểm từng Track hoàn chỉnh.
Thực tế Fall 2025: 07h–14h coding, 14h–15h30 thuyết trình (5p+3p Q&A). Spring 2026: 06h–19h cả 2 vòng trong 1 ngày. Các Track thi đồng thời, mỗi Track có phòng và nhóm Judge riêng.
STT
Actor
Hành động
Mô tả chi tiết
Exception / Edge Case
1
Coordinator
Kích hoạt Round Sơ loại
Set rounds.is_active=TRUE cho Round Sơ loại. Safety net: validate tổng weight=1.0 cho mọi Track (criteria.track_id) và Criteria Chung kết (criteria.round_id). Gửi notification ROUND_STARTED cho Judge, Mentor và các đội. Sau khi kích hoạt, các Track thi đấu đồng thời — mỗi Track có phòng và nhóm Judge riêng biệt.
Mỗi sequence_order chỉ có 1 Round is_active=TRUE. Thực tế: cả 2 Track thi đồng thời trong cùng ngày — không phải lần lượt. Validate conflict Mentor↔Judge theo track_id trước khi activate.
2
Coordinator
Phát đề bài theo Track
BTC phát đề lúc 07h00. Hệ thống set rounds.problem_released_at. Đề bài mỗi Track khác nhau theo chủ đề đã bốc thăm. 7 tiếng coding (07h–14h) theo quy định 2 mùa.
Đội trễ >60 phút → bỏ cuộc. Mã nguồn: GitHub/Jira/Confluence/Notion. Không chấp nhận Google Drive.
3
Mentor
Hỗ trợ chuyên môn trong Track
Mentor hỗ trợ đội trong Track mình phụ trách suốt thời gian coding. 1 Mentor có thể hỗ trợ nhiều đội trong cùng Track. Phạm vi: kỹ thuật, kiến trúc — không làm thay. Mentor KHÔNG được vào giao diện chấm điểm của Track mình.
HARD RULE: Mentor Track X không được Judge Track X cùng Round. Đã block từ GĐ1 — không cần kiểm tra lại ở đây.
4
Sinh viên
Nộp bài
Đội nộp bài cho Track của mình: submissions(team_id, track_id). repo_url + demo_url + slide_url (bắt buộc dạng slide). Hệ thống set LATE nếu nộp sau submission_deadline → LATE_PENDING chờ Coordinator xét.
LATE_PENDING → Coordinator xét LATE_APPROVED/REJECTED. Slide bắt buộc. UNIQUE(team_id, track_id).
5
Sinh viên / Judge
Thuyết trình & Live Scoring theo Track
Mỗi Track trình bày trong phòng riêng với nhóm Judge của Track đó. Các đội trình bày LẦN LƯỢT theo thứ tự — 1 đội/1 lần (quy định cả 2 mùa: "thuyết trình theo khu vực tương ứng của track và lần lượt theo thứ tự"). Thực tế: 5p trình bày + 3p Q&A. Judge chấm Live Scoring ngay trong khi đội thuyết trình — không chờ hết bảng mới chấm.
Đội vắng mặt → có thể ELIMINATE. Các Track thuyết trình đồng thời (khác phòng), trong mỗi Track đội trình bày lần lượt. Spring 2026: thuyết trình ngay sau coding trong cùng ngày 12/4.
6
Judge
Chấm điểm độc lập
Judge chấm theo Criteria của Track mình: scores(submission_id, judge_id, criterion_id). score_type=NORMAL, is_final=FALSE. Lưu riêng từng Judge — không gộp — phục vụ RBL (ICC/Krippendorff).
Trigger DB block sửa điểm sau scoring_locked=TRUE.
7
Hệ thống
Tổng hợp & Xếp hạng trong Track
Tính weighted_avg_score: Σ(avg_judge_score × criterion.weight). Xếp hạng đội trong Track theo score. Realtime view v_round_leaderboard (join qua track_id → round_id).
Chỉ tính score_type=NORMAL. Loại CALIBRATION và PENALTY.
8
Coordinator
Khóa chấm điểm Round Sơ loại
Khi tất cả Judge hoàn thành: scoring_locked=TRUE → batch set scores.is_final=TRUE. Cảnh báo trước nếu Judge chưa chấm đủ. Force-lock phải nhập force_lock_reason.
scoring_locked=TRUE là điều kiện chuyển GĐ — kể cả force-lock. Có thể lock từng Track riêng nếu Track đó xong trước.
9
Coordinator
Loại đội vi phạm
Gian lận, sao chép, dùng tool không được phép → teams.status=ELIMINATED. Audit TEAM_ELIMINATE + notification.
Hệ thống auto re-rank sau ELIMINATE. Nếu đội ELIMINATED trong Top N → re-rank, có thể trigger Wild Card.

Điều kiện chuyển GĐ4: Round Sơ loại scoring_locked=TRUE (tất cả Track, kể cả force-lock).
 
GĐ4 — Chuyển vòng & Công bố kết quả Sơ loại
Mục tiêu: Chọn 6 đội vào Chung kết, gom về pool chung, kích hoạt Round Chung kết.
Đầu ra: 6 đội xác nhận, Round Chung kết is_active=TRUE
Thực tế 2 mùa: top 2 đội/bảng × số bảng = 6 đội tổng. Fall 2025: mỗi Track có thể có nhiều bảng nhỏ (≤6 đội/bảng) → top 2/bảng trong Track. Spring 2026: mỗi Track=1 bảng → top 2/Track. Round Chung kết (is_final=TRUE) không có Track con — 6 đội thi pool chung. Judge Chung kết: 100% EXTERNAL mới tinh.
STT
Actor
Hành động
Mô tả chi tiết
Exception / Edge Case
1
Hệ thống
Xác định Top N mỗi bảng (trong từng Track)
Tự động chọn top_n_advance=2 đội cao điểm nhất trong MỖI BẢNG (assigned_group). Fall 2025: mỗi Track có nhiều bảng nhỏ (≤6 đội) → PARTITION BY (track_id, assigned_group). Spring 2026: mỗi Track=1 bảng → top 2/Track. Kết quả: 2 đội × số bảng tổng cộng = 6 đội.
Filter teams.status=ACTIVE. Xếp hạng PER BẢNG (assigned_group) trong Track — không cross-Track, không cross-bảng ở bước này.
2
Hệ thống
Tiebreak đồng điểm (nếu có)
Phát hiện đồng điểm ở vị trí ranh giới Top N trong Track → Penalty Evaluation: SUBMISSION_TIME (tự động) hoặc PENALTY_SCORE (Judge mini test 10 phút — thực tế 2 mùa) hoặc COORDINATOR_DECISION. Penalty không cộng vào điểm Sơ loại.
Tiebreak TRƯỚC Wild Card. Vẫn đồng điểm → escalate BTC.
3
Hệ thống
Gợi ý Wild Card cross-Track
Kiểm tra tổng đội chọn vs min_teams_final=6. Nếu thiếu và wildcard_enabled=TRUE → gợi ý Wild Card: đội chưa chọn, sắp xếp theo weighted_avg_score cross-Track.
wildcard_enabled phải TRUE cả hackathon VÀ round. Wild Card từ bất kỳ Track nào — hợp lệ.
4
Coordinator
Xác nhận Wild Card & Danh sách Chung kết
Xem và xác nhận 6 đội. BTC toàn quyền. Cập nhật: không đủ → ELIMINATED; vào Chung kết → ACTIVE. Notification cả 2 nhóm.
Ghi audit_log mọi thay đổi. BTC có thể chọn ngoài gợi ý — ghi lý do.
5
Coordinator
Công bố kết quả Sơ loại
Công bố bảng điểm từng Track và danh sách 6 đội Chung kết. Notification RESULT_PUBLISHED. Bảng điểm Sơ loại frozen.
Sau công bố: scoring_locked bảo vệ — không thay đổi được.
6
Coordinator
Phân công Judge Chung kết & Kích hoạt
Phân công 100% Judge EXTERNAL cho Round Chung kết (assignment_type=FINAL_EXTERNAL, track_id=NULL, round_id=final_round.id). Không có Judge Internal — kể cả Judge đã tham gia Sơ loại. Lý do: góc nhìn thị trường, khách quan tuyệt đối. Kích hoạt Round Chung kết: is_active=TRUE. Notification cho 6 đội.
BLOCK nếu cố phân công Judge Internal → 422 INTERNAL_JUDGE_NOT_ALLOWED_IN_FINAL. Ngoại lệ: Trưởng khoa/bộ môn không mentor ai — xác nhận tường minh checkbox.

Điều kiện chuyển GĐ5: Round Chung kết is_active=TRUE. 6 đội xác nhận. Kết quả Sơ loại đã công bố.
 
GĐ5 — Vòng Chung kết (Pool chung — 100% Judge EXTERNAL)
Mục tiêu: 6 đội thi chung, chấm điểm bởi 100% Judge EXTERNAL.
Đầu ra: hackathons.status = PENDING_CONFIRM
Round Chung kết is_final=TRUE — không có Track con. submissions(team_id, round_id). criteria(round_id). scores vẫn join qua criterion_id → round_id. Clean và nhất quán.
STT
Actor
Hành động
Mô tả chi tiết
Exception / Edge Case
1
Sinh viên
Nộp bài Chung kết
6 đội nộp submissions(team_id, round_id=final_round.id) — không dùng track_id vì Round này không có Track. Hard-lock tuyệt đối: không có LATE_PENDING. Fall 2025 Criteria Chung kết: Hoàn thiện (30%)+Sáng tạo (25%)+Hiệu quả (20%)+Mở rộng (15%)+Trình bày & Phản biện (10%). Spring 2026: Xử lý & Truy xuất (30%)+Độ tin cậy (20%)+Tư duy Agent (20%)+Thực tế (20%)+Mở rộng (10%).
LATE_PENDING không có ở Chung kết. Bài sau deadline → REJECTED tự động. late_submission_policy=HARD_LOCK.
2
Judge
[RBL] Calibration Round (tùy chọn)
Judge EXTERNAL chấm bài mẫu để thống nhất thang điểm: score_type=CALIBRATION, không tính kết quả. Đặc biệt quan trọng với Judge mới hoàn toàn (chưa quen thang điểm).
Tùy chọn — không block nếu bỏ qua.
3
Sinh viên / Judge
Thuyết trình & Chấm điểm Chung kết
6 đội thuyết trình trước hội đồng EXTERNAL: 7 phút + 3 phút Q&A. Judge chấm Live Scoring theo Criteria Chung kết (criteria.round_id). RBL Dashboard phương sai điểm realtime.
Không có Judge Internal trong phòng Chung kết. Mọi Mentor Sơ loại ngồi ngoài.
4
Coordinator
Khóa chấm & Chuyển PENDING_CONFIRM
scoring_locked=TRUE cho Round Chung kết. BTC họp thống nhất. Chuyển hackathons.status=PENDING_CONFIRM — buffer review trước công bố.
PENDING_CONFIRM: kết quả chưa public. Chỉ Coordinator xem.

Điều kiện chuyển GĐ6: hackathons.status=PENDING_CONFIRM. BTC đã thống nhất.
 
GĐ6 — Công bố kết quả & Trao giải
Mục tiêu: Hoàn tất sự kiện, cập nhật bảng xếp hạng tích lũy, lưu trữ.
Đầu ra: hackathons.status = FINISHED
STT
Actor
Hành động
Mô tả chi tiết
Exception / Edge Case
1
Hệ thống
Tính bảng xếp hạng chính thức
Từ Round Chung kết (is_final=TRUE): scores WHERE score_type=NORMAL AND is_final=TRUE.
Filter teams.status=ACTIVE. Loại CALIBRATION và PENALTY.
2
Coordinator
Ghi nhận giải thưởng
prizes linh hoạt: Fall 2025 — 6 giải (Nhất 7tr+Nhì 5tr+Ba 3tr+Ý tưởng 1tr+Ứng dụng 1tr+Cá nhân 1tr) + Giấy chứng nhận cho TẤT CẢ. Spring 2026 — 4 giải (Nhất 7tr+Nhì 5tr+Ba 3tr+Ý tưởng 1.5tr) + Giấy chứng nhận.
Giải thưởng linh hoạt theo kỳ. Giấy chứng nhận = quyền lợi mặc định.
3
Coordinator
Xác nhận → FINISHED + Công bố
PENDING_CONFIRM → FINISHED. Trigger async: (1) RESULT_PUBLISHED notification; (2) Tính Team XH, Chapter XH, Cá nhân XH; (3) Export jobs.
FINISHED = terminal state.
4
Hệ thống
Cập nhật 3 Bảng XH
Team XH (không cộng dồn). Chapter XH (cumulative — Pending #5). Cá nhân XH nếu individual_ranking_enabled=TRUE (Fall 2025=TRUE; Spring 2026=FALSE).
Pending #5: công thức Chapter chờ BTC.
5
Coordinator
Xuất báo cáo & Dataset RBL
CSV/Excel điểm chi tiết. RBL dataset ẩn danh (ẩn judge_id, team_id; giữ user_type, criterion_type). Phục vụ RQ1/RQ2/RQ3.
Chỉ export khi FINISHED. Async worker.
6
Hệ thống
Lưu trữ & Kế thừa
Dữ liệu FROZEN. Criteria kế thừa qua source_criteria_id. Ngoại lệ ghi: prize_claims, export_jobs, audit_logs.
—

Điều kiện hoàn tất: hackathons.status=FINISHED. Tất cả bảng XH cập nhật. Dataset RBL đã xuất.

 
4. Mô hình tránh thiên vị — Thiết kế chống conflict
Kiến trúc Hackathon→Round→Track cho phép conflict rule cực kỳ rõ ràng và tự nhiên: Mentor/Judge đều gắn vào Track (trong Round). Track đã tự mang round_id → ngữ cảnh đầy đủ.
4.1 Vòng Sơ loại — Tách biệt Mentor và Judge theo Track
Giảng viên
Vai trò trong Round Sơ loại
Quy tắc & Kết quả
GV A — Mentor Track AI (Round Sơ loại)
Judge Track Web hoặc Mobile (cùng Round)
✓ HỢP LỆ — không chấm học trò mình
GV A — Mentor Track AI (Round Sơ loại)
Judge Track AI (cùng Round)
✗ BLOCK 422 CONFLICT_SAME_TRACK
GV B — Mentor Track Web (Round Sơ loại)
Judge Track AI hoặc Mobile (cùng Round)
✓ HỢP LỆ — khuyến khích
GV C — không Mentor ai
Judge bất kỳ Track nào (Round Sơ loại)
✓ HỢP LỆ — không conflict
GV A hoặc B — đã Mentor Round Sơ loại
Judge Round Chung kết
✗ BLOCK — Chung kết 100% EXTERNAL
Trưởng khoa/bộ môn — KHÔNG mentor ai
Judge Round Chung kết (xác nhận tường minh)
✓ HỢP LỆ — ngoại lệ duy nhất Internal

4.2 SQL conflict check trong hệ thống
-- ┌─ KHI PHÂN CÔNG JUDGE VÀO TRACK SƠ LOẠI ─────────────────────┐
-- Rule: Cùng người Mentor Track X mà Judge Track X → BLOCK
SELECT 1 FROM mentor_assignments
WHERE mentor_id = :judge_id AND track_id = :track_id;
-- Có kết quả → 422 CONFLICT_SAME_TRACK (BLOCK cứng)
-- Note: track_id đã unique trong toàn DB → tự động đúng Round
-- vì track_id thuộc duy nhất 1 Round qua FK.
 
-- ┌─ KHI PHÂN CÔNG MENTOR VÀO TRACK ────────────────────────────┐
-- Rule: Chiều ngược lại
SELECT 1 FROM judge_assignments
WHERE judge_id = :mentor_id AND track_id = :track_id;
-- Có kết quả → 422 CONFLICT_SAME_TRACK (BLOCK cứng)
 
-- ┌─ KHI PHÂN CÔNG JUDGE CHUNG KẾT (Round FINAL) ───────────────┐
-- Rule: 100% EXTERNAL — Internal Mentor đã Sơ loại bị BLOCK
IF user.user_type != 'EXTERNAL'  THEN
   -- Ngoại lệ: Trưởng khoa/bộ môn không Mentor ai
   IF NOT EXISTS (
 	SELECT 1 FROM mentor_assignments WHERE mentor_id = :judge_id
   ) AND confirmed_exception = TRUE THEN
 	ALLOW;
   ELSE
 	RAISE 422 INTERNAL_JUDGE_NOT_ALLOWED_IN_FINAL;
   END IF;
END IF;
-- assignment_type phải = 'FINAL_EXTERNAL'
 
-- ┌─ TRƯỜNG HỢP HỢP LỆ CROSS-ROUND ─────────────────────────────┐
-- GV A Mentor Track AI (Round Sơ loại) ĐƯỢC Judge Track Web
-- (cùng Round Sơ loại) — vì khác track_id. KHÔNG bị block.
-- Nếu có Round Bán kết: GV A vẫn có thể Judge Track ở Bán kết
-- nếu KHÔNG mentor đội nào trong Track đó — track_id khác.
4.3 Tính đúng đắn của kiến trúc mới với các câu hỏi thực tế
Câu hỏi thực tế
Trả lời với kiến trúc Hackathon→Round→Track
Fall 2025: 2 Track, mỗi Track nhiều bảng ≤6 đội
Round 1 (PRELIMINARY) tạo 2 Track. Trong mỗi Track, phân chia đội thành nhiều bảng qua team_round_tracks.assigned_group. top_n_advance=2 đội/bảng → tổng 6 đội vào Round 2.
Spring 2026: Track = 1 bảng, ≤8 đội
Round 1 tạo N Track (= số bảng theo đăng ký). Mỗi Track có 1 bảng (assigned_group có thể NULL hoặc trùng tên Track). top_n_advance=2 đội/Track = 6 đội Chung kết.
Mùa nào đó có 3 Round (giả định mở rộng)
Tạo Round 1, 2, 3 với sequence_order tương ứng. Round 1 + 2 có Track con, Round 3 (is_final=TRUE) không có. Scale tự nhiên.
Round Chung kết không có bảng đấu
Round FINAL is_final=TRUE — không có Track con. submissions(team_id, round_id). criteria(round_id). judge_assignments(judge_id, round_id, track_id=NULL).
Judge Sơ loại có judge được Chung kết không?
Không — Chung kết 100% EXTERNAL. assignment_type=FINAL_EXTERNAL bắt buộc. BLOCK Judge Internal kể cả đã Sơ loại Track khác.
Mentor Track AI judge được Track AI cùng vòng?
Không — 422 CONFLICT_SAME_TRACK. BLOCK cứng theo track_id. Judge Track Web/Mobile thì được.
Criteria 2 vòng có khác nhau không?
Có — Sơ loại: criteria.track_id (mỗi Track có bộ riêng). Chung kết: criteria.round_id. Hoàn toàn độc lập, kế thừa qua source_criteria_id.
Đội thi Track AI ở Round 1 có thi Track AI ở Round 2 không?
Theo thiết kế: team_round_tracks lưu Track mỗi Round riêng. Nếu BTC muốn giữ ngữ cảnh "đội này thuộc nhóm AI", BTC tạo Track tên giống nhau ở Round 2. Nhưng đây là Track khác (track_id khác).

SEAL Hackathon Management System — Workflow v5.0 & DB Schema v3.0 — Kiến trúc Hackathon→Round→Track — FPT University HCMC


