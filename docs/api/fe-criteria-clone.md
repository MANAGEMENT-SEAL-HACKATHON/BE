# FE — Criteria clone (Track ↔ Chung kết)

## Ma trận nghiệp vụ

| Nguồn → Đích | Cho phép? | API |
|--------------|-----------|-----|
| **Track → Track** (cùng hoặc **khác** hackathon / vòng Sơ loại) | Có | `POST /api/v1/tracks/{trackĐích}/criteria/clone` + `sourceTrackId` |
| **Chung kết → Chung kết** (cùng hoặc **khác** hackathon) | Có | `POST /api/v1/rounds/{finalRoundId}/criteria/clone` + `sourceRoundId` |
| **Track → Chung kết** | **Không** | `422` `CRITERIA_CLONE_CROSS_SCOPE` |
| **Chung kết → Track** | **Không** | `422` `CRITERIA_CLONE_CROSS_SCOPE` |

Clone = **sao chép** (không gắn `source_criteria_id`) → xóa/sửa từng dòng độc lập trên đích.

### `replaceExisting` (Track & Chung kết)

| Giá trị | Hành vi |
|---------|---------|
| `false` / **bỏ qua** (mặc định) | **Cộng dồn** — giữ criteria đích, append bản sao từ nguồn (`displayOrder` nối sau max hiện tại) |
| `true` | **Thay thế** — xóa hết criteria đích rồi clone (lỗi nếu đích đã có scores) |

Dùng cộng dồn khi gộp template từ nhiều track/round, rồi chỉnh/xóa thủ công. Dùng thay thế khi muốn đồng bộ lại toàn bộ từ một nguồn.

---

## Track → Track

### Dropdown nguồn

```http
GET /api/v1/tracks/{targetTrackId}/criteria/clone-sources
```

- `targetTrackId` = bảng **đích** (vd. Track 3).
- Trả về mọi track **khác** (mọi hackathon) có `criteriaCount > 0`, kèm `hackathonId`, `hackathonName`, `roundId`.

### Clone

```http
POST /api/v1/tracks/{targetTrackId}/criteria/clone
```

```json
{
  "sourceTrackId": 2
}
```

Cộng dồn lần 2 (track đích đã có criteria):

```json
{
  "sourceTrackId": 1,
  "replaceExisting": false
}
```

Thay toàn bộ:

```json
{
  "sourceTrackId": 2,
  "replaceExisting": true
}
```

| Field | Ghi chú |
|--------|---------|
| `sourceTrackId` | Bắt buộc; **không** gửi `sourceRoundId` |
| `replaceExisting` | Mặc định `false` = cộng dồn; `true` = xóa hết đích rồi clone |

**Lỗi thường gặp:** gọi `POST .../tracks/6/criteria/clone` trong khi `6` là **roundId** Chung kết → dùng API round bên dưới.

---

## Chung kết → Chung kết

```http
POST /api/v1/rounds/{finalRoundId}/criteria/clone
```

```json
{
  "sourceRoundId": 2
}
```

| Field | Ghi chú |
|--------|---------|
| `sourceRoundId` | Round **Chung kết** nguồn (`is_final=true`); **không** gửi `sourceTrackId` |
| `replaceExisting` | Giống Track — mặc định cộng dồn |
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
| `CRITERIA_CLONE_CROSS_SCOPE` | Track↔Chung kết hoặc gửi nhầm `sourceRoundId` / `sourceTrackId` |
| `CRITERIA_CLONE_SOURCE_EMPTY` | Thiếu nguồn / round CK nguồn trống criteria |
| `CRITERIA_HAS_SCORES` | Đã chấm điểm — không xóa được khi `replaceExisting: true` |

---

## Dev seed (`profile=dev`)

Hackathon `seal-spring-2026`: Track 1/2 có criteria; Track 3 trống → test clone Track 2 → 3.
