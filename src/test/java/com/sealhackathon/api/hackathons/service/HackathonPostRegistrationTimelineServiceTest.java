package com.sealhackathon.api.hackathons.service;

import com.sealhackathon.api.config.seed.RoundScheduleSeedUtil;
import com.sealhackathon.api.events.service.MilestoneEventRescheduleService;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.presentation.service.PresentationSlotCascadeService;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HackathonPostRegistrationTimelineServiceTest {

    @Mock private HackathonRepository hackathonRepository;
    @Mock private RoundRepository roundRepository;
    @Mock private HackathonRoundTimelineSyncService hackathonRoundTimelineSyncService;
    @Mock private MilestoneEventRescheduleService milestoneEventRescheduleService;
    @Mock private PresentationSlotCascadeService presentationSlotCascadeService;

    @InjectMocks private HackathonPostRegistrationTimelineService service;

    @Test
    void compress_movesPrelimFinalAndMilestones() {
        LocalDate regEnd = LocalDate.of(2026, 7, 20);
        Hackathon h = Hackathon.builder()
                .id(9)
                .registrationEnd(regEnd)
                .eventStart(LocalDate.of(2026, 8, 10))
                .eventEnd(LocalDate.of(2026, 8, 12))
                .build();
        LocalDateTime oldPrelim = LocalDateTime.of(2026, 8, 10, 8, 0);
        Round prelim = Round.builder()
                .id(3)
                .isFinal(false)
                .isActive(false)
                .examAt(oldPrelim)
                .codingDurationHours(7)
                .hackathon(h)
                .build();
        Round finalR = Round.builder()
                .id(4)
                .isFinal(true)
                .isActive(false)
                .examAt(LocalDateTime.of(2026, 8, 10, 17, 0))
                .codingDurationHours(2)
                .hackathon(h)
                .build();

        when(hackathonRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(roundRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(roundRepository.findPreliminaryLikeByHackathonId(9)).thenReturn(List.of(prelim));
        when(roundRepository.findByHackathon_IdAndIsFinalTrue(9)).thenReturn(Optional.of(finalR));
        when(hackathonRepository.findById(9)).thenReturn(Optional.of(h));
        when(milestoneEventRescheduleService.repositionWorkshopKickoff(any())).thenReturn(2);
        when(milestoneEventRescheduleService.repositionAwardsAfterFinal(any(), any())).thenReturn(1);

        Map<String, Object> meta = service.compressAfterRegistrationClosed(h);

        LocalDate expectedStart = regEnd.plusDays(RoundScheduleSeedUtil.DAYS_REG_END_TO_EVENT_START);
        assertThat(meta.get("timelineCompressed")).isEqualTo(true);
        assertThat(h.getEventStart()).isEqualTo(expectedStart);
        assertThat(prelim.getExamAt().toLocalDate()).isEqualTo(expectedStart);
        assertThat(prelim.getExamAt().toLocalTime()).isEqualTo(oldPrelim.toLocalTime());
        assertThat(finalR.getExamAt())
                .isEqualTo(RoundScheduleSeedUtil.maxFinalExamAt(prelim.getExamAt(), 7));
        verify(milestoneEventRescheduleService).repositionWorkshopKickoff(h);
        verify(milestoneEventRescheduleService).repositionAwardsAfterFinal(h, finalR);
        verify(presentationSlotCascadeService).rescheduleForRound(3);
        verify(presentationSlotCascadeService).rescheduleForRound(4);
        verify(hackathonRoundTimelineSyncService).syncFromRounds(9);
    }

    @Test
    void compress_skipsActivePrelim() {
        LocalDate regEnd = LocalDate.of(2026, 7, 20);
        Hackathon h = Hackathon.builder().id(9).registrationEnd(regEnd).build();
        Round prelim = Round.builder()
                .id(3)
                .isFinal(false)
                .isActive(true)
                .examAt(LocalDateTime.of(2026, 8, 10, 8, 0))
                .codingDurationHours(7)
                .hackathon(h)
                .build();
        when(hackathonRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(roundRepository.findPreliminaryLikeByHackathonId(9)).thenReturn(List.of(prelim));
        when(roundRepository.findByHackathon_IdAndIsFinalTrue(9)).thenReturn(Optional.empty());
        when(hackathonRepository.findById(9)).thenReturn(Optional.of(h));
        when(milestoneEventRescheduleService.repositionWorkshopKickoff(any())).thenReturn(0);

        Map<String, Object> meta = service.compressAfterRegistrationClosed(h);

        assertThat(meta.get("skippedActivePrelimId")).isEqualTo(3);
        assertThat(prelim.getExamAt()).isEqualTo(LocalDateTime.of(2026, 8, 10, 8, 0));
    }
}
