# Business Rules — GĐ6 (MF-06 v3.2)

**Nguồn:** `GD06_SEAL_MF06_v3_2.docx` · **Trạng thái BE:** scaffold (service `// TODO`).

## Vòng đời

```
PENDING_CONFIRM ──[Prizes + Confirm]──► FINISHED
```

- GĐ5 (FR-30A) đã set `PENDING_CONFIRM`.
- GĐ6 chỉ làm `PENDING_CONFIRM → FINISHED` (một chiều, terminal).

## FR → Package BE

| FR | Mô tả | Package / class |
|----|--------|-----------------|
| FR-31 | XH Team tự động (view) | `hackathons.query.FinalRankingQueryService` |
| FR-32 | Ghi nhận / thu hồi giải | `prizes` — `award()` ✅; `listByHackathon`, `revoke` TODO |
| FR-33 | Confirm FINISHED | `hackathons.service.HackathonClosureService` |
| FR-33A | GET bảng XH Team | `HackathonClosureController` → `FinalRankingQueryService` |
| FR-33B | XH Chapter (persist) | `chapter_rankings` + worker `calculateAsync` |
| FR-33C | XH Cá nhân (Fall 2025) | `individual_rankings` + worker `calculateAsync` |
| FR-33D | Cờ `individual_ranking_enabled` | field trên `Hackathon` — gate trong FR-33C |
| FR-34 | Export CSV/Excel | `export_jobs` |
| FR-35 | Export RBL ẩn danh | `export_jobs` type `ANONYMIZED_RBL` |
| FR-36 | Audit append-only | cross-cutting trong confirm / export / revoke |

## Async sau confirm

`HackathonFinishedEvent` → `HackathonFinishedEventListener`:

1. Notification `RESULT_PUBLISHED`
2. `ChapterRankingService.calculateAsync`
3. `IndividualRankingService.calculateAsync` (nếu cờ bật)

## Gate chính (phase 2)

| Hành động | Gate |
|-----------|------|
| Trao giải | `PENDING_CONFIRM` |
| Confirm | `PENDING_CONFIRM`, round FINAL locked, ≥1 prize |
| XH Team/Chapter | `PENDING_CONFIRM`+ / `FINISHED` |
| Export | `FINISHED` |
| Revoke prize | không `FINISHED` |

## Error codes dự kiến

`NO_PRIZES_RECORDED`, `STATUS_NOT_PENDING_CONFIRM`, `HACKATHON_NOT_FINISHED`, `INDIVIDUAL_RANKING_NOT_AVAILABLE`, `EXPORT_JOB_NOT_READY`

## Spring 2026 vs Fall 2025

- **Spring 2026:** 2 bảng XH (Team + Chapter).
- **Fall 2025:** thêm Individual khi `individual_ranking_enabled=TRUE`.

## BUG-7 (Fall 2025 phase 2)

Migration `prizes`: `scope`, `prize_type`, `user_id`; UNIQUE `(hackathon_id, prize_type, scope)`.

## Liên kết

- API reference §6: [03-api-reference-gd3.md](03-api-reference-gd3.md#6-hackathon--kết-thúc--trao-giải-gđ6--mf-06)
- Luồng FE: [10-fe-api-flow-gd6.md](10-fe-api-flow-gd6.md)
- Backlog implement: [09-be-backlog-gd4-gd5.md](09-be-backlog-gd4-gd5.md)
