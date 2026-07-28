package com.sealhackathon.api.presentation.support;

import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.events.entity.PresentationSlot;
import com.sealhackathon.api.presentation.value_object.PresentationQueueStatus;
import com.sealhackathon.api.presentation.value_object.PresentationTimerPhase;
import com.sealhackathon.api.rounds.entity.Round;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PresentationDurationMutationGuardTest {

    private final PresentationDurationMutationGuard guard = new PresentationDurationMutationGuard();

    @Test
    void allowsWhenNoSlots() {
        Round round = Round.builder().id(3).scoringLocked(false).build();
        assertThatCode(() -> guard.assertMutableBeforePresentation(round, null, List.of()))
                .doesNotThrowAnyException();
    }

    @Test
    void allowsWhenShuffledButTimerNotStarted() {
        Round round = Round.builder().id(3).scoringLocked(false).build();
        PresentationSlot slot = PresentationSlot.builder()
                .queueStatus(PresentationQueueStatus.PRESENTING)
                .timerPhase(PresentationTimerPhase.SETUP)
                .build();
        assertThatCode(() -> guard.assertMutableBeforePresentation(round, 10, List.of(slot)))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsWhenTimerPresenting() {
        Round round = Round.builder().id(3).scoringLocked(false).build();
        PresentationSlot slot = PresentationSlot.builder()
                .queueStatus(PresentationQueueStatus.PRESENTING)
                .timerPhase(PresentationTimerPhase.PRESENTING)
                .build();
        assertThatThrownBy(() -> guard.assertMutableBeforePresentation(round, 10, List.of(slot)))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getCode())
                        .isEqualTo(ErrorCode.INVALID_STATE));
    }

    @Test
    void rejectsWhenSlotDone() {
        Round round = Round.builder().id(3).scoringLocked(false).build();
        PresentationSlot slot = PresentationSlot.builder()
                .queueStatus(PresentationQueueStatus.DONE)
                .timerPhase(PresentationTimerPhase.ENDED)
                .build();
        assertThatThrownBy(() -> guard.assertMutableBeforePresentation(round, null, List.of(slot)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void rejectsWhenScoringLocked() {
        Round round = Round.builder().id(3).scoringLocked(true).build();
        assertThatThrownBy(() -> guard.assertMutableBeforePresentation(round, null, List.of()))
                .isInstanceOf(BusinessRuleException.class);
    }
}
