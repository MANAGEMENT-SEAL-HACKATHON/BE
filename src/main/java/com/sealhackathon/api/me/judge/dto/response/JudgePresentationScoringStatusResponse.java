package com.sealhackathon.api.me.judge.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JudgePresentationScoringStatusResponse {

    private Integer roundId;
    private Integer trackId;
    private Integer submissionId;
    private String displayCode;
    private int judgesAssigned;
    private int judgesScored;
    private int judgesFullyScored;
    private int judgesConfirmed;
    private boolean myConfirmed;
    private boolean myScored;
    /** true khi mọi judge phân công đã Chốt điểm — có thể bấm Đội tiếp. */
    private boolean canAdvanceQueue;
    /** true khi judge hiện tại được phép điều khiển timer/hàng đợi (theo controller grant hoặc mặc định). */
    private boolean canControlPresentation;
}
