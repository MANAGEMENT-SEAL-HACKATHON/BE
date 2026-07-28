package com.sealhackathon.api.presentation.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PresentationTimerEndRequest {

    /**
     * Chỉ COORDINATOR / HEAD — force kết thúc sớm Q&A khi chưa đủ judge Chốt điểm.
     */
    private Boolean acknowledgeIncompleteScoring;
}
