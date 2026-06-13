# 04 — Activity Diagrams (Sơ đồ hoạt động)

> 6 luồng chính — copy mermaid sang [mermaid.live](https://mermaid.live) hoặc vẽ lại trên Draw.io.  
> Mỗi diagram = 1 activity diagram cho assignment.

---

## AD-01 — Đăng ký & thiết lập sự kiện (GĐ1 → GĐ2)

**Mục tiêu:** Coordinator tạo hackathon → Student đăng ký đội → Lottery.

```mermaid
flowchart TD
    Start([Bắt đầu]) --> A[Coordinator đăng nhập]
    A --> B[Tạo Hackathon DRAFT]
    B --> C[Tạo Round Sơ loại + Track]
    C --> D[Tạo Criteria weight=1]
    D --> E[Phân công Judge/Mentor]
    E --> F[Chuyển Hackathon → ONGOING]
    F --> G[Student đăng ký tài khoản]
    G --> H{Coordinator duyệt?}
    H -->|Reject| End1([Kết thúc])
    H -->|Approve| I[Student tạo đội]
    I --> J[Coordinator duyệt đội ACTIVE]
    J --> K{Đã hết hạn đăng ký?}
    K -->|Chưa| L[Đội is_locked=false]
    K -->|Rồi| M[Đội is_locked=true]
    L --> M
    M --> N[Coordinator chạy Lottery]
    N --> O[Gán track + bảng cho đội]
    O --> End2([Sẵn sàng GĐ3])
```

**Swimlane gợi ý:** Coordinator | Student | System

---

## AD-02 — Kích hoạt vòng & nộp bài (GĐ3)

```mermaid
flowchart TD
    Start([Bắt đầu GĐ3]) --> A[Coordinator activate Round]
    A --> B[Release problem statement]
    B --> C[Student nộp multipart]
    C --> D{PDF hợp lệ?}
    D -->|Không| E[400 INVALID_SLIDE]
    E --> C
    D -->|Có| F{Repo GitHub public?}
    F -->|Không| G[400 REPO_NOT_PUBLIC]
    G --> C
    F -->|Có| H{Quá deadline?}
    H -->|Không| I[status=SUBMITTED]
    H -->|Có| J[status=LATE_PENDING]
    J --> K{Coord duyệt late?}
    K -->|Reject| End1([Không chấm])
    K -->|Approve| L[status=LATE_APPROVED]
    I --> M[Lưu slide → MinIO]
    L --> M
    M --> End2([Submission gradable])
```

---

## AD-03 — Presentation & chấm điểm (GĐ3 core)

```mermaid
flowchart TD
    Start([Round đang thi]) --> A[Coordinator shuffle queue]
    A --> B[Slot #1 = PRESENTING]
    B --> C[Judge HEAD timer/start]
    C --> D[timer.phase = PRESENTING]
    D --> E[Judge chấm POST /scores]
    E --> F{Gate mở?}
    F -->|SCORING_NOT_OPEN| G[Báo lỗi FE]
    G --> C
    F -->|OK| H[Lưu điểm + WS broadcast]
    H --> I{Còn criterion?}
    I -->|Có| E
    I -->|Không| J[Judge HEAD gọi next]
    J --> K{Có điểm NORMAL?}
    K -->|Không| L[422 NO_SCORES]
    L --> E
    K -->|Có| M{Đủ judge chấm?}
    M -->|Không| N{acknowledge?}
    N -->|Không| O[422 MISSING_JUDGE]
    O --> P[FE dialog confirm]
    P --> N
    N -->|Có| Q[Đội cũ DONE]
    M -->|Có| Q
    Q --> R[Đội kế PRESENTING + SETUP]
    R --> S{Hết queue?}
    S -->|Không| C
    S -->|Có| End([Hết đội])
```

**Swimlane:** Coordinator (shuffle) | Judge Controller (timer/next) | Judge (score) | System (gate)

---

## AD-04 — Khóa chấm & publish (GĐ3 → GĐ4)

```mermaid
flowchart TD
    Start([Sau khi chấm xong]) --> A[Coord xem scoring-progress]
    A --> B{100% gradable?}
    B -->|Chưa| C[Tiếp tục chấm/nhắc judge]
    C --> A
    B -->|Rồi| D[PATCH lock-scoring]
    D --> E[GET ranking preview]
    E --> F[Coord xác nhận]
    F --> G[PATCH publish]
    G --> H[Student xem leaderboard]
    H --> I{Wildcard enabled?}
    I -->|Có| J[Coord review wildcard]
    J --> K[Approve/Reject wildcard]
    K --> L[POST /rounds/{id}/advance]
    I -->|Không| L
    L --> End([Chuyển vòng kế / CK])
```

---

## AD-05 — Đăng nhập & OAuth (Auth)

```mermaid
flowchart TD
    Start([User mở app]) --> A{Đã có token?}
    A -->|Có| B{Token valid?}
    B -->|Có| Home([Vào portal theo role])
    B -->|Không| C[POST /auth/refresh]
    C --> D{Refresh OK?}
    D -->|Có| Home
    D -->|Không| E[Form login]
    A -->|Không| E
    E --> F{OAuth or Email?}
    F -->|Email| G[POST /auth/login]
    F -->|Google/GitHub| H[POST /auth/oauth/*]
    G --> I{Approved?}
    H --> I
    I -->|PENDING| J[Chờ duyệt]
    I -->|APPROVED| K[Lưu JWT]
    K --> Home
```

---

## AD-06 — Kết thúc sự kiện (GĐ6)

```mermaid
flowchart TD
    Start([Chung kết xong]) --> A[Coord lock-scoring final]
    A --> B[GET team/chapter/individual rankings]
    B --> C[Coord trao giải POST prizes]
    C --> D[Student xem prizes/certificates]
    D --> E[Coord tạo export job]
    E --> F[Poll export status]
    F --> G[GET /export-jobs/{id}/download — stream CSV]
    G --> H[PATCH confirm FINISHED]
    H --> End([Hackathon FINISHED])
```

---

## Bảng chọn diagram cho assignment

| Ưu tiên | Diagram | Lý do |
|---------|---------|-------|
| ⭐⭐⭐ | AD-03 | Core GĐ3 — presentation + scoring + next guard |
| ⭐⭐⭐ | AD-02 | Submit multipart — tính năng mới v4.1 |
| ⭐⭐ | AD-01 | End-to-end từ setup → lottery |
| ⭐⭐ | AD-04 | Publish workflow |
| ⭐ | AD-05 | Auth — nếu assignment yêu cầu |
| ⭐ | AD-06 | Closure — nếu cover full lifecycle |

---

## Ký hiệu Activity Diagram (UML)

| Ký hiệu | Ý nghĩa |
|---------|---------|
| `( )` / stadium | Start / End node |
| `[]` / rectangle | Action / Activity |
| `{ }` / diamond | Decision / Merge |
| Swimlane | Partition theo actor |
| Fork/Join | Song song (ít dùng trong doc này) |

---

## Decision points quan trọng (ghi chú khi vẽ)

| # | Điều kiện | Nhánh |
|---|-----------|-------|
| D1 | `teams.is_locked` | Lottery được / không |
| D2 | `submission.status` | SUBMITTED / LATE_PENDING |
| D3 | `timer.phase` | Chấm được / SCORING_NOT_OPEN |
| D4 | `scoreCount > 0` | Next được / 422 |
| D5 | `round.scoring_locked` | Chấm được / locked |
| D6 | `user.status` | APPROVED / PENDING |
