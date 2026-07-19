# Deep Audit — PHASE0

| Bước | Kỳ vọng | Thực tế | Nút OK? | UX thân thiện? | Popup đủ? | Trình tự đúng? | Data đủ? | Kết luận |
|------|---------|---------|---------|----------------|-----------|----------------|----------|----------|
| P0-WC | Không tab Vé vớt; ?tab=wildcard redirect | tabs=Kết quả, Danh sách Chung kết & Bị loại, Kiểm tra chấm, Đồng điểm (0) url=http://localhost:5173/hackathons/5/rounds/7/results?tab=ranking | Y | Y | — | Y | Y | PASS |
| P0-HEAD | Không UI Trưởng ban; điều khiển qua Chuyển quyền | noHead=true hasTransfer=true | Y | Y | — | Y | Y | PASS |
| Wildcard-HEAD regression | Không Vé vớt + không Trưởng ban | noWc=true noHead=true | Y | Y | — | Y | Y | PASS |
| P0-PG | JUDGE_ASSIGN_DUPLICATE + CONFLICT_MENTOR_JUDGE_SAME_TRACK | dup=true JUDGE_ASSIGN_DUPLICATE; conflict=PASS blocked HTTP 422 CONFLICT_SAME_TRACK | — | — | — | Y | Y | PASS |
