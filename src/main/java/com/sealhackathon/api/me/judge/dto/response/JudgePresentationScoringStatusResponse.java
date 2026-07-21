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
    /**
     * true khi mọi judge phân công đã Chốt điểm (scoring complete).
     * FE dùng field này cho Next UX — không tự derive từ judgesConfirmed.
     */
    private boolean allJudgesSubmitted;
    /** true khi scoring complete (+ timer ready phía BE nếu final). Server gate queue/next. */
    private boolean canAdvanceQueue;
    /**
     * true = Q&A kết thúc sớm; false = hết giờ tự nhiên; null = chưa ENDED / slot cũ.
     * FE: early-end button + Next sau natural incomplete.
     */
    private Boolean qaEndedEarly;
    /** true khi judge hiện tại được phép điều khiển timer/hàng đợi (theo controller grant hoặc mặc định). */
    private boolean canControlPresentation;
    /** G5-G: thời điểm chấm gần nhất trên submission đang PRESENTING (null nếu chưa ai chấm). */
    private java.time.LocalDateTime lastJudgeScoredAt;
}
