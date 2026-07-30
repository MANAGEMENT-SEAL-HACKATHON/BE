package com.sealhackathon.api.presentation.support;

import com.sealhackathon.api.presentation.value_object.RoundPhase;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.support.RoundSubmissionWindow;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Phase vòng thi live scoring.
 *
 * <p>CODING = vòng active và <strong>còn trong cửa sổ nộp</strong>
 * ({@code now < submissionDeadline} và chưa {@code submissionClosedEarlyAt}).
 * Hết hạn nộp hoặc kết thúc sớm → JUDGING.
 *
 * <p>Fallback khi thiếu deadline và chưa đóng sớm: dùng {@code examAt}
 * (chỉ CODING khi chưa đến giờ thi).
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
        if (RoundSubmissionWindow.isClosed(round, now)) {
            return RoundPhase.JUDGING;
        }
        LocalDateTime deadline = round.getSubmissionDeadline();
        if (deadline != null) {
            return RoundPhase.CODING;
        }
        LocalDateTime examAt = round.getExamAt();
        if (examAt != null && now.isBefore(examAt)) {
            return RoundPhase.CODING;
        }
        return RoundPhase.JUDGING;
    }
}
