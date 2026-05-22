package com.sealhackathon.api.criteria.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * FR-04 POST /api/v1/rounds/{roundId}/criteria/clone — kế thừa Criteria từ Round nguồn.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CloneCriteriaRequest {

    private Integer sourceRoundId;

    private Integer sourceTrackId;

    /**
     * {@code false} / không gửi (mặc định): <b>cộng dồn</b> — giữ criteria đích, thêm bản sao nguồn (displayOrder nối tiếp).
     * {@code true}: <b>thay thế</b> — xóa hết criteria đích trước khi clone (chặn nếu đích đã có scores).
     */
    private Boolean replaceExisting;
}
