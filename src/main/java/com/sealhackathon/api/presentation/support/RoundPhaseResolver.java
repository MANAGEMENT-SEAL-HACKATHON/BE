package com.sealhackathon.api.presentation.support;

import com.sealhackathon.api.presentation.value_object.RoundPhase;
import com.sealhackathon.api.rounds.entity.Round;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Phase vòng thi live scoring.
 *
 * <p>CODING = vòng active và <strong>còn trong cửa sổ nộp</strong>
 * ({@code now < submissionDeadline}). Hết hạn nộp hoặc kết thúc sớm
 * (deadline đã clamp ≤ now) → JUDGING.
 *
 * <p>Fallback khi thiếu deadline: dùng {@code examAt} (chỉ CODING khi chưa đến giờ thi).
 */
@Component
public class RoundPhaseResolver {

    public RoundPhase resolve(Round round) {
        if (Boolean.TRUE.equals(round.getIsPublished())) {
            return RoundPhase.PUBLISHED;
        }
        if (Boolean.TRUE.equals(round.getScoringLocked())) {
            return RoundPhase.SCORING_LOCKED;
        }
        if (!Boolean.TRUE.equals(round.getIsActive())) {
            return RoundPhase.SETUP;
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deadline = round.getSubmissionDeadline();
        if (deadline != null) {
            if (now.isBefore(deadline)) {
                return RoundPhase.CODING;
            }
            return RoundPhase.JUDGING;
        }
        LocalDateTime examAt = round.getExamAt();
        if (examAt != null && now.isBefore(examAt)) {
            return RoundPhase.CODING;
        }
        return RoundPhase.JUDGING;
    }
}
