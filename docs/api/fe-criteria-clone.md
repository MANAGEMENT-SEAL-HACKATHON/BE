# FE — Criteria clone & xóa/sửa độc lập

## Nghiệp vụ

- Clone = **sao chép** sang track/round khác; **không** gắn `source_criteria_id` → xóa/sửa từng dòng trên bảng đích tự do.
- Chuỗi: Bảng 1 → clone → Bảng 2 → clone → Bảng 3: mỗi bước chỉ đọc `criteria` **trên track nguồn** (`track_id` nguồn).

## Dropdown nguồn clone (Track)

```http
GET /api/v1/tracks/{targetTrackId}/criteria/clone-sources
```

- `targetTrackId` = bảng **đích** (vd. Bảng 3).
- Trả về mọi track **khác** trong **cùng round** có `criteriaCount > 0` (gồm Bảng 2 dù từng clone từ Bảng 1).
- **Không** lọc theo `sourceCriteriaId`.

## Thực hiện clone

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
| `sourceTrackId` | Bắt buộc; khác `targetTrackId`; cùng round |
| `replaceExisting` | **Nên `true`** khi bảng đích đã có criteria (nếu không → 422 `CRITERIA_TARGET_HAS_EXISTING`) |

## Xóa / sửa

| Hành động | Chặn khi |
|-----------|----------|
| DELETE / PUT `/api/v1/criteria/{id}` | Đã có **scores** (`CRITERIA_HAS_SCORES`) |
| DELETE | **Không** chặn vì track khác đã clone (dữ liệu độc lập) |

## Lỗi thường gặp

| Code | Ý nghĩa |
|------|---------|
| `CRITERIA_TARGET_HAS_EXISTING` | Clone khi bảng đích đã có tiêu chí mà không `replaceExisting: true` |
| `CRITERIA_CLONE_SOURCE_EMPTY` | Nguồn trống / trùng đích / khác round |
| `CRITERIA_HAS_SCORES` | Đã chấm điểm — không xóa/sửa |
| `DB_INTEGRITY_VIOLATION` | Dữ liệu cũ còn `source_criteria_id` — **restart BE** (chạy migration gỡ FK) |

## Dev seed (`profile=dev`)

Sau restart, hackathon `seal-spring-2026` có:

- **Track 1 / 2** — đủ 5 criteria (độc lập, không `sourceCriteriaId`).
- **Track 3** — *Track 3 — EV & Integration*, **chưa có criteria** → test `GET .../tracks/{track3Id}/criteria/clone-sources` (thấy Track 2) rồi `POST .../clone` với `sourceTrackId` = track 2.

## Round Chung kết

- Clone: `POST /api/v1/rounds/{finalRoundId}/criteria/clone` + `sourceRoundId` (khác round đích).
- Chưa có `GET .../clone-sources` cho round — FE có thể list round FINAL khác qua API hackathon/rounds rồi `GET .../rounds/{id}/criteria` kiểm tra `items.length > 0`.
