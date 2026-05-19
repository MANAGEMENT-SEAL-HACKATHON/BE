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
     * Nếu TRUE: xóa toàn bộ Criteria hiện tại của Round đích trước khi clone (block nếu có scores).
     * Mặc định FALSE — chỉ append vào danh sách hiện tại.
     */
    private Boolean replaceExisting;
}
