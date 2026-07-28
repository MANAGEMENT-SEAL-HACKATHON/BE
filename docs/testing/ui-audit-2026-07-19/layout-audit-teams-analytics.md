# Layout audit — Teams & Analytics (Gap 3)

Date: 2026-07-19

## Quản lý đội thi (CoordinatorTeamPage)
- Triệu chứng Overview (card lệch hàng / trắng bốc): **không** — trang vốn header + tabs, không có hàng 4 stat card lệch.
- Đã áp: headerGradient + title accent + icon surfaceSoft từ coordinatorTheme (token màu đồng bộ setup).

## Phân tích dữ liệu (CoordinatorAnalyticsPage)
- Triệu chứng Overview: **không** — nội dung chính là dashboard/chart/table.
- Đã áp: cùng header polish + Việt hoá phụ đề (bỏ jargon "RBL variance").
- EventContextBanner → dạng dòng gọn + tooltip (chia sẻ component).

## Overview (CoordinatorActionCenter)
- Đã polish đầy đủ 2b: stretch cards, header gradient, icon badge, Deadline→Hạn chót.
