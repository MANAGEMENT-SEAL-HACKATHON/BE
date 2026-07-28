# Deep Audit — GD3

| Bước | Kỳ vọng | Thực tế | Nút OK? | UX thân thiện? | Popup đủ? | Trình tự đúng? | Data đủ? | Kết luận |
|------|---------|---------|---------|----------------|-----------|----------------|----------|----------|
| 01 Nút tình trạng + kết thúc sớm | 2 nút hiện khi đang thi (hoặc đã đóng sớm) | status=true close=true | Y | Y | — | Y | Y | PASS |
| 02 Modal kết thúc sớm cưỡng ép | Alert đỏ khi còn đội chưa nộp | Alert OK | Y | Y | Y | Y | N | PASS |
| 03 Confirm end-early | Đóng cổng → JUDGING | confirmed | Y | Y | Y | Y | Y | PASS |
| 04 LOTTERY-DATA-01 panel đủ đội | Hiển thị đủ đội track kể cả chưa nộp | listed=3/6 labelChuaNop=false | Y | Y | — | Y | Y | PASS |
| 04b Readiness đủ đội (sau fix) | Panel Tình trạng bài nộp = đủ đội track | Tổng đội: 3 | Y | Y | — | Y | Y | PASS |
| 05 LOTTERY-GATE-01 LATE_PENDING blocks shuffle | Disable Máy Quay Số + tooltip | disabled=true tip=Còn 1 đội nộp trễ chưa xử lý — duyệt hoặc từ chối trước khi quay số. | Y | Y | — | Y | Y | PASS |
