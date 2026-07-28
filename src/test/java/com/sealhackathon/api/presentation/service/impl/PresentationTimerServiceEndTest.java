package com.sealhackathon.api.presentation.service.impl;

import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.events.entity.PresentationSlot;
import com.sealhackathon.api.events.repository.PresentationSlotRepository;
import com.sealhackathon.api.live_scoring.PresentationQueuePublisher;
import com.sealhackathon.api.presentation.dto.response.PresentationQueueResponse;
import com.sealhackathon.api.presentation.guard.PresentationControllerGuard;
import com.sealhackathon.api.presentation.guard.PresentationForceAdvanceAckGuard;
import com.sealhackathon.api.presentation.service.PresentationQueueService;
import com.sealhackathon.api.presentation.support.PresentationDurationResolver;
import com.sealhackathon.api.presentation.support.PresentationNextScoringGuard;
import com.sealhackathon.api.presentation.support.RoundPhaseResolver;
import com.sealhackathon.api.presentation.value_object.PresentationQueueStatus;
import com.sealhackathon.api.presentation.value_object.PresentationTimerPhase;
import com.sealhackathon.api.presentation.value_object.RoundPhase;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.submissions.entity.Submission;
import com.sealhackathon.api.tracks.entity.Track;
import com.sealhackathon.api.tracks.repository.TrackRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PresentationTimerServiceEndTest {

  private static final int SLOT_ID = 42;

  @Mock private RoundRepository roundRepository;
  @Mock private TrackRepository trackRepository;
  @Mock private PresentationSlotRepository presentationSlotRepository;
  @Mock private PresentationControllerGuard controllerGuard;
  @Mock private PresentationForceAdvanceAckGuard forceAdvanceAckGuard;
  @Mock private PresentationNextScoringGuard nextScoringGuard;
  @Mock private PresentationDurationResolver durationResolver;
  @Mock private PresentationQueueService presentationQueueService;
  @Mock private PresentationQueuePublisher queuePublisher;
  @Mock private RoundPhaseResolver roundPhaseResolver;

  @InjectMocks private PresentationTimerServiceImpl service;

  private final Round round = Round.builder().id(1).build();
  private final Track track = Track.builder().id(7).round(round).build();
  private final Submission submission = Submission.builder().id(100).build();

  @BeforeEach
  void stubContext() {
    when(roundRepository.findById(1)).thenReturn(Optional.of(round));
    when(trackRepository.findById(7)).thenReturn(Optional.of(track));
    when(roundPhaseResolver.resolve(round)).thenReturn(RoundPhase.JUDGING);
    lenient().doNothing().when(controllerGuard).requireControllerForTrack(anyInt(), any(), any());
    lenient()
        .when(presentationQueueService.getQueue(1, 7))
        .thenReturn(PresentationQueueResponse.builder().build());
    lenient().when(durationResolver.presentationMinutes(any(), any())).thenReturn(10);
    lenient().when(durationResolver.qaMinutes(any(), any())).thenReturn(5);
    lenient().when(forceAdvanceAckGuard.resolveAcknowledge(anyBoolean(), any(), any())).thenReturn(false);
  }

  private void stubPresentingSlot(PresentationSlot slot) {
    slot.setId(SLOT_ID);
    when(presentationSlotRepository.findFirstByRound_IdAndTrack_IdAndQueueStatus(
            1, 7, PresentationQueueStatus.PRESENTING))
        .thenReturn(Optional.of(slot));
    when(presentationSlotRepository.findByIdForUpdate(SLOT_ID)).thenReturn(Optional.of(slot));
    lenient().when(presentationSlotRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
  }

  @Test
  void earlyEnd_incompleteScores_blockedByScoringGuard() {
    PresentationSlot slot = qaSlotStillRunning();
    stubPresentingSlot(slot);
    doThrow(new BusinessRuleException(ErrorCode.SCORING_INCOMPLETE_BEFORE_NEXT, "incomplete"))
        .when(nextScoringGuard)
        .validateBeforeNext(eq(submission), eq(7), eq(round), eq(false), eq(true));

    assertThatThrownBy(() -> service.end(1, 7, false))
        .isInstanceOf(BusinessRuleException.class)
        .satisfies(ex -> assertThat(((BusinessRuleException) ex).getCode())
            .isEqualTo(ErrorCode.SCORING_INCOMPLETE_BEFORE_NEXT));
    assertThat(slot.getTimerPhase()).isEqualTo(PresentationTimerPhase.QA);
    verify(queuePublisher, never()).publishTimerPhase(any(), any(), any(), any(), anyInt());
  }

  @Test
  void earlyEnd_completeScores_setsEndedAndQaEndedEarlyTrue() {
    PresentationSlot slot = qaSlotStillRunning();
    stubPresentingSlot(slot);
    doNothing().when(nextScoringGuard)
        .validateBeforeNext(eq(submission), eq(7), eq(round), eq(false), eq(true));

    var response = service.end(1, 7, false);

    assertThat(slot.getTimerPhase()).isEqualTo(PresentationTimerPhase.ENDED);
    assertThat(slot.getQaEndedEarly()).isTrue();
    assertThat(response.getTimer().getPhase()).isEqualTo("ENDED");
    verify(queuePublisher).publish(eq(1), eq(7), any());
    verify(queuePublisher).publishTimerPhase(eq(1), eq(7), eq(100), eq("ENDED"), anyInt());
  }

  @Test
  void autoTimeout_qaRemainingZero_endsWithoutScoringGuard_natural() {
    PresentationSlot slot =
        PresentationSlot.builder()
            .timerPhase(PresentationTimerPhase.QA)
            .qaStartedAt(LocalDateTime.now().minusMinutes(30))
            .pausedAccumulatedSeconds(0)
            .submission(submission)
            .build();
    stubPresentingSlot(slot);
    when(durationResolver.qaMinutes(track, round)).thenReturn(5);

    var response = service.end(1, 7, false);

    assertThat(slot.getTimerPhase()).isEqualTo(PresentationTimerPhase.ENDED);
    assertThat(slot.getQaEndedEarly()).isFalse();
    assertThat(response.getTimer().getPhase()).isEqualTo("ENDED");
    verify(nextScoringGuard, never()).validateBeforeNext(any(), any(), any(), anyBoolean(), anyBoolean());
    verify(queuePublisher).publishTimerPhase(eq(1), eq(7), eq(100), eq("ENDED"), anyInt());
  }

  private PresentationSlot qaSlotStillRunning() {
    return PresentationSlot.builder()
        .timerPhase(PresentationTimerPhase.QA)
        .qaStartedAt(LocalDateTime.now())
        .pausedAccumulatedSeconds(0)
        .submission(submission)
        .build();
  }
}
