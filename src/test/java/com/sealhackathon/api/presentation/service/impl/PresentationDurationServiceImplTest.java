package com.sealhackathon.api.presentation.service.impl;

import com.sealhackathon.api.common.audit.AuditAction;
import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.events.entity.PresentationSlot;
import com.sealhackathon.api.events.repository.PresentationSlotRepository;
import com.sealhackathon.api.hackathons.support.HackathonArchiveGuard;
import com.sealhackathon.api.presentation.dto.request.PresentationDurationSetupRequest;
import com.sealhackathon.api.presentation.dto.response.PresentationDurationResponse;
import com.sealhackathon.api.presentation.dto.response.PresentationQueueResponse;
import com.sealhackathon.api.presentation.service.PresentationQueueService;
import com.sealhackathon.api.presentation.service.PresentationSlotCascadeService;
import com.sealhackathon.api.live_scoring.PresentationQueuePublisher;
import com.sealhackathon.api.presentation.support.PresentationDurationMutationGuard;
import com.sealhackathon.api.presentation.support.PresentationDurationResolver;
import com.sealhackathon.api.presentation.value_object.PresentationQueueStatus;
import com.sealhackathon.api.presentation.value_object.PresentationTimerPhase;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.tracks.entity.Track;
import com.sealhackathon.api.tracks.repository.TrackRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PresentationDurationServiceImplTest {

    @Mock RoundRepository roundRepository;
    @Mock TrackRepository trackRepository;
    @Mock PresentationSlotRepository presentationSlotRepository;
    @Spy PresentationDurationResolver durationResolver = new PresentationDurationResolver();
    @Spy PresentationDurationMutationGuard mutationGuard = new PresentationDurationMutationGuard();
    @Mock PresentationSlotCascadeService slotCascadeService;
    @Spy HackathonArchiveGuard archiveGuard = new HackathonArchiveGuard();
    @Mock AuditService auditService;
    @Mock PresentationQueueService presentationQueueService;
    @Mock PresentationQueuePublisher queuePublisher;

    @InjectMocks PresentationDurationServiceImpl service;

    @Test
    void updateDuration_roundScope_updatesFinalRoundDefaultsAndReschedulesSlots() {
        Round round = Round.builder()
                .id(5)
                .isFinal(true)
                .defaultPresentationMinutes(10)
                .defaultQaMinutes(5)
                .build();
        when(roundRepository.findById(5)).thenReturn(Optional.of(round));
        when(presentationSlotRepository.findByRound_IdAndTrackIsNullOrderBySequenceOrderAsc(5))
                .thenReturn(List.of());
        when(roundRepository.save(round)).thenAnswer(inv -> inv.getArgument(0));
        when(presentationQueueService.getQueue(5, null)).thenReturn(PresentationQueueResponse.builder().build());

        PresentationDurationSetupRequest req = PresentationDurationSetupRequest.builder()
                .roundId(5)
                .presentationMinutes(12)
                .qaMinutes(8)
                .build();

        PresentationDurationResponse response = service.updateDuration(req);

        assertThat(round.getDefaultPresentationMinutes()).isEqualTo(12);
        assertThat(round.getDefaultQaMinutes()).isEqualTo(8);
        assertThat(response.getScope()).isEqualTo("ROUND");
        verify(slotCascadeService).rescheduleForRound(5);
        verify(auditService).log(eq(AuditAction.PRESENTATION_DURATION_UPDATED), eq("rounds"), eq(5), eq(Map.of(
                "scope", "ROUND",
                "presentationMinutes", 12,
                "qaMinutes", 8)));
    }

    @Test
    void updateDuration_trackScope_updatesTrackOverrideAndReschedulesSlots() {
        Round round = Round.builder()
                .id(3)
                .isFinal(false)
                .defaultPresentationMinutes(10)
                .defaultQaMinutes(5)
                .build();
        Track track = Track.builder()
                .id(10)
                .round(round)
                .presentationMinutes(null)
                .qaMinutes(null)
                .build();
        when(roundRepository.findById(3)).thenReturn(Optional.of(round));
        when(trackRepository.findById(10)).thenReturn(Optional.of(track));
        when(presentationSlotRepository.findByRound_IdAndTrack_IdOrderBySequenceOrderAsc(3, 10))
                .thenReturn(List.of());
        when(trackRepository.save(track)).thenAnswer(inv -> inv.getArgument(0));
        when(presentationQueueService.getQueue(3, 10)).thenReturn(PresentationQueueResponse.builder().build());

        PresentationDurationSetupRequest req = PresentationDurationSetupRequest.builder()
                .roundId(3)
                .trackId(10)
                .presentationMinutes(15)
                .qaMinutes(7)
                .build();

        PresentationDurationResponse response = service.updateDuration(req);

        assertThat(track.getPresentationMinutes()).isEqualTo(15);
        assertThat(track.getQaMinutes()).isEqualTo(7);
        assertThat(response.getScope()).isEqualTo("TRACK");
        verify(slotCascadeService).rescheduleForRound(3);
    }

    @Test
    void updateDuration_whenTimerAlreadyStarted_rejected() {
        Round round = Round.builder().id(5).isFinal(true).scoringLocked(false).build();
        PresentationSlot slot = PresentationSlot.builder()
                .queueStatus(PresentationQueueStatus.PRESENTING)
                .timerPhase(PresentationTimerPhase.PRESENTING)
                .build();
        when(roundRepository.findById(5)).thenReturn(Optional.of(round));
        when(presentationSlotRepository.findByRound_IdAndTrackIsNullOrderBySequenceOrderAsc(5))
                .thenReturn(List.of(slot));

        PresentationDurationSetupRequest req = PresentationDurationSetupRequest.builder()
                .roundId(5)
                .presentationMinutes(12)
                .qaMinutes(8)
                .build();

        assertThatThrownBy(() -> service.updateDuration(req))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getCode())
                        .isEqualTo(ErrorCode.INVALID_STATE));
    }

    @Test
    void updateDuration_finalRoundWithTrackId_rejected() {
        Round round = Round.builder().id(5).isFinal(true).build();
        when(roundRepository.findById(5)).thenReturn(Optional.of(round));
        when(presentationSlotRepository.findByRound_IdAndTrack_IdOrderBySequenceOrderAsc(5, 10))
                .thenReturn(List.of());

        PresentationDurationSetupRequest req = PresentationDurationSetupRequest.builder()
                .roundId(5)
                .trackId(10)
                .presentationMinutes(12)
                .qaMinutes(8)
                .build();

        assertThatThrownBy(() -> service.updateDuration(req))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getCode())
                        .isEqualTo(ErrorCode.DESIGN_VIOLATION));
    }

    @Test
    void clearTrackOverride_resetsToRoundDefaultAndReschedules() {
        Round round = Round.builder()
                .id(3)
                .isFinal(false)
                .defaultPresentationMinutes(10)
                .defaultQaMinutes(5)
                .build();
        Track track = Track.builder()
                .id(10)
                .round(round)
                .presentationMinutes(15)
                .qaMinutes(7)
                .build();
        when(roundRepository.findById(3)).thenReturn(Optional.of(round));
        when(trackRepository.findById(10)).thenReturn(Optional.of(track));
        when(presentationSlotRepository.findByRound_IdAndTrack_IdOrderBySequenceOrderAsc(3, 10))
                .thenReturn(List.of());
        when(trackRepository.save(track)).thenAnswer(inv -> inv.getArgument(0));
        when(presentationQueueService.getQueue(3, 10)).thenReturn(PresentationQueueResponse.builder().build());

        PresentationDurationResponse response = service.clearTrackOverride(3, 10);

        assertThat(track.getPresentationMinutes()).isNull();
        assertThat(track.getQaMinutes()).isNull();
        assertThat(response.getEffectivePresentationMinutes()).isEqualTo(10);
        verify(slotCascadeService).rescheduleForRound(3);
    }
}
