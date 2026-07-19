# RBL IRR analysis (RQ1–RQ3)

Script ngoài runtime SEAL — đọc CSV `ANONYMIZED_RBL` long-format từ Analytics export.

## Cài đặt

```bash
pip install -r requirements-rbl.txt
```

## Chạy

```bash
python rbl_irr_analysis.py path/to/anonymized_rbl.csv [--out irr_report.md]
```

## Quan trọng

- **Seed / test CSV chỉ để smoke pipeline** — không dùng số ICC/α từ seed cho luận văn.
- Số chính thức: sau hackathon **FINISHED**, Coord export `ANONYMIZED_RBL` → chạy lại script trên file đó.
- PENALTY bị lọc khi tính IRR (vẫn có trong CSV raw).
- Cần ≥ ~3 giám khảo/bài để ước lượng ổn định (ràng buộc vận hành, ngoài code).
