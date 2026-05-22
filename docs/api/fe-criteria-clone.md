# FE — Criteria clone (Track ↔ Chung kết)

## Ma trận nghiệp vụ

| Nguồn → Đích | Cho phép? | API |
|--------------|-----------|-----|
| **Track → Track** (cùng vòng Sơ loại/Bán kết) | Có | `POST /api/v1/tracks/{trackĐích}/criteria/clone` + `sourceTrackId` |
| **Chung kết → Chung kết** (cùng hoặc **khác** hackathon) | Có | `POST /api/v1/rounds/{finalRoundId}/criteria/clone` + `sourceRoundId` |
| **Track → Chung kết** | **Không** | `422` `CRITERIA_CLONE_CROSS_SCOPE` |
| **Chung kết → Track** | **Không** | `422` `CRITERIA_CLONE_CROSS_SCOPE` |

Clone = **sao chép** (không gắn `source_criteria_id`) → xóa/sửa từng dòng độc lập trên đích.

---

## Track → Track

### Dropdown nguồn

```http
GET /api/v1/tracks/{targetTrackId}/criteria/clone-sources
```

- `targetTrackId` = bảng **đích** (vd. Track 3).
- Trả về track **khác** trong **cùng round** có `criteriaCount > 0`.

### Clone

```http
POST /api/v1/tracks/{targetTrackId}/criteria/clone
```

```json
{
  "sourceTrackId": 2,
  "replaceExisting": true
}
```

| Field | Ghi chú |
|--------|---------|
| `sourceTrackId` | Bắt buộc; **không** gửi `sourceRoundId` |
| `replaceExisting` | `true` nếu đích đã có criteria |

**Lỗi thường gặp:** gọi `POST .../tracks/6/criteria/clone` trong khi `6` là **roundId** Chung kết → dùng API round bên dưới.

---

## Chung kết → Chung kết

```http
POST /api/v1/rounds/{finalRoundId}/criteria/clone
```

```json
{
  "sourceRoundId": 2,
  "replaceExisting": true
}
```

| Field | Ghi chú |
|--------|---------|
| `sourceRoundId` | Round **Chung kết** nguồn (`is_final=true`); **không** gửi `sourceTrackId` |
| Hackathon | **Được** khác hackathon (kỳ trước → kỳ mới) |

Nguồn phải có criteria gắn `round_id` (criteria Chung kết), không phải criteria trên Track Sơ loại.

Kiểm tra trước clone: `GET /api/v1/rounds/{sourceRoundId}/criteria` → `items.length > 0`.

---

## Xóa / sửa

| Hành động | Chặn khi |
|-----------|----------|
| DELETE / PUT `/api/v1/criteria/{id}` | Đã có **scores** |

---

## Mã lỗi

| Code | Ý nghĩa |
|------|---------|
| `CRITERIA_CLONE_CROSS_SCOPE` | Track↔Chung kết, sai API, hoặc track khác vòng |
| `CRITERIA_CLONE_SOURCE_EMPTY` | Thiếu nguồn / round CK nguồn trống criteria |
| `CRITERIA_TARGET_HAS_EXISTING` | Đích đã có criteria, thiếu `replaceExisting: true` |
| `CRITERIA_HAS_SCORES` | Đã chấm điểm |

---

## Dev seed (`profile=dev`)

Hackathon `seal-spring-2026`: Track 1/2 có criteria; Track 3 trống → test clone Track 2 → 3.
