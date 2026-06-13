# SWD392 — Assignment 1 | SEAL Hackathon Management System

> Tài liệu phục vụ vẽ **Feature list**, **Context Diagram**, **Use Case**, **Activity Diagram** và **Source check**.  
> Quét từ source BE tại thời điểm **2026-06** — 37 REST controllers, ~161 endpoints, 35 entities, 6 giai đoạn (GĐ1–GĐ6).

## Cấu trúc folder

| File | Mục đích | Dùng để vẽ |
|------|----------|------------|
| [01-feature-list.md](01-feature-list.md) | Danh sách tính năng đầy đủ theo module & giai đoạn | Feature list |
| [02-context-diagram.md](02-context-diagram.md) | Actor, hệ thống, hệ thống ngoài | Context Diagram (C0) |
| [03-use-case-diagram.md](03-use-case-diagram.md) | Actor × use case theo package | Use Case Diagram |
| [04-activity-diagrams.md](04-activity-diagrams.md) | Luồng hoạt động chính (mermaid) | Activity Diagram |
| [05-source-check.md](05-source-check.md) | DB / API / Frontend inventory | Source check |

## Actors chính

| Actor | Role BE |
|-------|---------|
| Coordinator (BTC) | `COORDINATOR` |
| Student | `STUDENT` |
| Judge | `JUDGE` |
| Mentor | `MENTOR` |
| Guest Judge | `JUDGE` + `EXTERNAL` + temp account |

## Giai đoạn nghiệp vụ

| GĐ | Tên | Trọng tâm |
|----|-----|-----------|
| GĐ1 | Thiết lập sự kiện | Hackathon, Round, Track, Criteria, Events, Personnel |
| GĐ2 | Đăng ký & đội | Auth, Teams, Lottery, User approval |
| GĐ3 | Vòng Sơ loại | Submit, Presentation queue, Scoring, Late review |
| GĐ4 | Publish & Wildcard | Publish kết quả, Wild card |
| GĐ5 | Chung kết & RBL | Final round, Calibration, Tiebreak |
| GĐ6 | Kết thúc | Rankings, Prizes, Certificates, Export |

## Tech stack (Source check)

| Layer | Công nghệ |
|-------|-----------|
| API Server | Java 21, Spring Boot 3, JWT, STOMP/WebSocket |
| Database | MySQL 8 (`SealHackathon`) |
| Object storage | MinIO / local filesystem (slide PDF, thẻ SV, export CSV, certificate PDF) |
| Frontend | React (repo riêng — `seal-hackathon-fe`) |

## Ghi chú khi vẽ

- **Context Diagram:** 1 hệ thống trung tâm “SEAL Hackathon MS”, 4–5 actor, 3–4 external system.
- **Use Case:** Tách diagram theo actor (Coordinator / Student / Judge / Mentor) hoặc theo GĐ.
- **Activity:** Ưu tiên 5 luồng trong `04-activity-diagrams.md` — đủ cover assignment.
- Chi tiết API: `docs/` trong repo BE; schema DB: `docs/db/schema-v3.0-mysql.md`.
