# BE backlog — User roles (sau scaffold)

## Trạng thái scaffold (2026-05-29)

| Module | Entity/Repo | Service impl | Controller `/me` |
|--------|:-----------:|:------------:|:----------------:|
| `me/student` | — | TODO stub | ✅ |
| `me/judge` | — | TODO stub | ✅ |
| `me/mentor` | — | TODO stub | ✅ |
| `hackathon_registrations` | ✅ | TODO | via `StudentHackathonController` |
| `appeals` | ✅ | TODO | via `StudentMeController` |
| `certificates` | ✅ | TODO | via `StudentMeController` |
| `me` notifications | dùng `notifications` | TODO | ✅ |

## Delegate vs implement (phase 2)

| FR | Ưu tiên | Ghi chú |
|----|---------|---------|
| U-07..13 đội | **Delegate** | Giữ `teams` package — không duplicate |
| U-18 nộp bài | **Delegate** | `POST /submissions` |
| J scoring | **Delegate** | `POST /scores`, `POST /scores/calibration` |
| U-06 register | **Implement** | `hackathon_registrations` |
| U-30 appeal | **Implement** | `appeals` + 24h rule |
| U-29 certificate | **Implement** | S3 / file_url |
| J completion_status | **Schema** | ALTER `judge_assignments` — backlog riêng |

## Không làm trong scaffold (đã tuân)

- Sửa `TeamServiceImpl`, `ScoreController`, entity cũ
- ALTER bảng hiện có
