package com.sealhackathon.api.presentation.support;

import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.events.entity.PresentationSlot;
import com.sealhackathon.api.presentation.value_object.PresentationQueueStatus;
import com.sealhackathon.api.presentation.value_object.PresentationTimerPhase;
import com.sealhackathon.api.rounds.entity.Round;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Chỉ cho phép đổi thời lượng timer trước khi buổi thuyết trình thực sự bắt đầu
 * (timer chưa PRESENTING/QA và chưa có slot DONE).
 */
@Component
public class PresentationDurationMutationGuard {

    private static final Set<PresentationTimerPhase> STARTED_TIMER_PHASES = EnumSet.of(
            PresentationTimerPhase.PRESENTING,
            PresentationTimerPhase.QA,
            PresentationTimerPhase.PAUSED,
            PresentationTimerPhase.ENDED);

    public void assertMutableBeforePresentation(Round round, Integer trackId, List<PresentationSlot> slots) {
        if (Boolean.TRUE.equals(round.getScoringLocked())) {
            throw new BusinessRuleException(ErrorCode.INVALID_STATE,
                    "Round đã khóa — không thể đổi thời lượng timer",
                    Map.of("roundId", round.getId()));
        }
        for (PresentationSlot slot : slots) {
            if (slot.getQueueStatus() == PresentationQueueStatus.DONE) {
                throw presentationStarted(round.getId(), trackId);
            }
            PresentationTimerPhase phase = slot.getTimerPhase();
            if (phase != null && STARTED_TIMER_PHASES.contains(phase)) {
                throw presentationStarted(round.getId(), trackId);
            }
        }
    }

    private static BusinessRuleException presentationStarted(Integer roundId, Integer trackId) {
        Map<String, Object> details = new HashMap<>();
        details.put("roundId", roundId);
        if (trackId != null) {
            details.put("trackId", trackId);
        }
        return new BusinessRuleException(ErrorCode.INVALID_STATE,
                "Buổi thuyết trình đã bắt đầu — chỉ được đổi thời lượng trước khi start timer",
                details);
    }
}
