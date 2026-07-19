# RBL / Analytics — Methodology Note (I5 GIỮ)

**Status:** I5 Analytics menu = **GIỮ** (chốt 2026-07-18).  
**Nguyên tắc:** additive — không phá ranking, scoring, advance, audit hiện có.

---

## 1. I5 Analytics

Menu «Phân tích & dữ liệu» là đường xuất CSV nghiên cứu + dashboard RBL duy nhất trong hệ thống. Giữ menu; nâng chất lượng export/dashboard lên chuẩn nghiên cứu (RQ1–RQ3).

## 2. Calibration (luồng độc lập)

- UI calibration cũ đã gỡ vì bug (điểm mẫu có nguy cơ nhiễm bảng `scores` dùng cho RQ).
- Luồng mới: bảng `rbl_calibration_prompts` / `rbl_calibration_scores` **tách hoàn toàn** khỏi `scores`.
- **Cả Judge và Coordinator** xem phân bố điểm trên bài mẫu (ẩn danh `Giám khảo 1/2/3`) để đồng thuận giữa giám khảo — đúng câu chữ đề tài.
- `ANONYMIZED_RBL`, ranking, ICC script **không bao giờ** đọc bảng calibration → baseline IRR sạch.

## 3. ICC / Krippendorff's α — ngoài hệ thống

- Hệ thống chỉ xuất CSV long-format + dashboard phương sai.
- Phân tích IRR chạy bằng script Python (`BE/docs/testing/rbl/rbl_irr_analysis.py`) ngoài runtime Spring.
- **Seed data = smoke pipeline only.** Số ICC/α viết vào luận văn phải **chạy lại script trên CSV export từ hackathon FINISHED thật** sau sự kiện.

## 4. PENALTY — chỉ lọc khi tính IRR

- `ScoreType.PENALTY` / `CriteriaType.PENALTY` vẫn dùng cho tiebreak/đồng điểm (`RoundRankingQueryService`, v.v.) — **không sửa**.
- Export RBL vẫn **xuất đủ** dòng PENALTY.
- Chỉ methodology + script IRR + metric dashboard inter-rater **lọc** `criterion_type != PENALTY` / `score_type != PENALTY` khi tính ICC/α.

## 5. Phạm vi có ý thức (#6 / #10)

| Mục | Quyết định | Rủi ro checklist |
|-----|------------|------------------|
| #6 Nộp bài (~95%) | Chấp nhận đủ (repo/demo/slide) | Thấp |
| #10 Email mặc định / chat 2 chiều | Hoãn — đã có in-app notify + announcement | Trung bình thấp |

## 6. Mapping judge_type (RQ3)

Ổn định theo user (không đổi mỗi assignment):

```
GUEST   = role JUDGE AND (isTempAccount OR userType == EXTERNAL)
FACULTY = role JUDGE AND userType == INTERNAL AND !isTempAccount
OTHER   = không vào phân tích RQ3
```

Header CSV / report bắt buộc:

```
# excluded_from_rq3: N judges unclassified (OTHER)
# rq3_faculty_n: …
# rq3_guest_n: …
```

Nhãn narrative hội đồng: **Faculty/Internal Judge vs Guest Judge** — không suy diễn «mọi INTERNAL = giảng viên».

## 7. Pre-event (ngoài phạm vi code)

ICC/Krippendorff's α ổn định thường cần **≥ ~3 giám khảo / bài**. Xác nhận với BTC / lịch phân công GK trước hackathon thật. Nếu chỉ 2 GK/bài, CI của RQ1 sẽ rất rộng dù pipeline hoàn hảo.

## Checklist sau sự kiện

1. Hackathon status = FINISHED  
2. Coord → Analytics → export `ANONYMIZED_RBL`  
3. Chạy `rbl_irr_analysis.py` trên CSV đó  
4. Dùng số ICC/α từ bước 3 cho luận văn (không dùng số từ seed)
