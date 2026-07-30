package com.sealhackathon.api.hackathons.service;

import com.sealhackathon.api.config.seed.RoundScheduleSeedUtil;
import com.sealhackathon.api.events.repository.EventRepository;
import com.sealhackathon.api.events.service.MilestoneEventRescheduleService;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.notifications.service.StakeholderBroadcastService;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompetitionScheduleAdjustServiceTest {

    @Mock private HackathonRepository hackathonRepository;
    @Mock private RoundRepository roundRepository;
    @Mock private EventRepository eventRepository;
    @Mock private HackathonRoundTimelineSyncService hackathonRoundTimelineSyncService;
    @Mock private MilestoneEventRescheduleService milestoneEventRescheduleService;
    @Mock private PresentationSlotCascadeService presentationSlotCascadeService;
    @Mock private StakeholderBroadcastService stakeholderBroadcastService;

    @InjectMocks private CompetitionScheduleAdjustService service;

    @Test
    void apply_shiftsPrelimFinalAndMarksAdjusted() {
        LocalDate regEnd = LocalDate.now();
        LocalDateTime newExam = regEnd.plusDays(3).atTime(8, 0);
        Hackathon h = Hackathon.builder()
                .id(9)
                .name("SEAL")
                .registrationEnd(regEnd)
                .eventStart(regEnd.plusDays(10))
                .eventEnd(regEnd.plusDays(12))
                .build();
        Round prelim = Round.builder()
                .id(3).isFinal(false).isActive(false)
                .examAt(regEnd.plusDays(10).atTime(8, 0))
                .codingDurationHours(7).hackathon(h).build();
        Round finalR = Round.builder()
                .id(4).isFinal(true).isActive(false)
                .examAt(regEnd.plusDays(10).atTime(17, 0))
                .codingDurationHours(2).hackathon(h).build();

        when(hackathonRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(roundRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(roundRepository.findPreliminaryLikeByHackathonId(9)).thenReturn(List.of(prelim));
        when(roundRepository.findByHackathon_IdAndIsFinalTrue(9)).thenReturn(Optional.of(finalR));
        when(hackathonRepository.findById(9)).thenReturn(Optional.of(h));
        when(eventRepository.findByHackathonIdAndType(eq(9), any())).thenReturn(List.of());
        when(milestoneEventRescheduleService.setWorkshopKickoffTimes(any(), any(), any(), any(), any())).thenReturn(2);
        when(milestoneEventRescheduleService.setAwardsTimes(any(), any(), any())).thenReturn(1);
        doNothing().when(stakeholderBroadcastService).broadcast(
                anyInt(), anyString(), anyString(), anyString(), anyString(), any(), anyBoolean());

        Map<String, Object> meta = service.apply(h, newExam, true);

        assertThat(meta.get("scheduleAdjusted")).isEqualTo(true);
        assertThat(h.getScheduleAdjustedAt()).isNotNull();
        assertThat(prelim.getExamAt()).isEqualTo(newExam);
        assertThat(finalR.getExamAt())
                .isEqualTo(RoundScheduleSeedUtil.maxFinalExamAt(newExam, 7));
        verify(milestoneEventRescheduleService).setWorkshopKickoffTimes(any(), any(), any(), any(), any());
        verify(presentationSlotCascadeService).rescheduleForRound(3);
        verify(presentationSlotCascadeService).rescheduleForRound(4);
    }

    @Test
    void apply_rejectsSecondTime() {
        Hackathon h = Hackathon.builder()
                .id(9)
                .registrationEnd(LocalDate.now())
                .scheduleAdjustedAt(LocalDateTime.now().minusDays(1))
                .build();
        assertThatThrownBy(() -> service.apply(h, LocalDate.now().plusDays(5).atTime(8, 0), true))
                .hasMessageContaining("1 lần");
    }

    @Test
    void adjust_locksThenAppliesAndReturnsPreview() {
        LocalDate regEnd = LocalDate.now();
        LocalDateTime newExam = regEnd.plusDays(3).atTime(8, 0);
        Hackathon h = Hackathon.builder()
                .id(9)
                .name("SEAL")
                .registrationEnd(regEnd)
                .eventStart(regEnd.plusDays(10))
                .eventEnd(regEnd.plusDays(12))
                .build();
        Round prelim = Round.builder()
                .id(3).isFinal(false).isActive(false)
                .examAt(regEnd.plusDays(10).atTime(8, 0))
                .codingDurationHours(7).hackathon(h).build();
        Round finalR = Round.builder()
                .id(4).isFinal(true).isActive(false)
                .examAt(regEnd.plusDays(10).atTime(17, 0))
                .codingDurationHours(2).hackathon(h).build();

        when(hackathonRepository.findById(9)).thenReturn(Optional.of(h));
        when(hackathonRepository.findByIdForUpdate(9)).thenReturn(Optional.of(h));
        when(hackathonRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(roundRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(roundRepository.findPreliminaryLikeByHackathonId(9)).thenReturn(List.of(prelim));
        when(roundRepository.findByHackathon_IdAndIsFinalTrue(9)).thenReturn(Optional.of(finalR));
        when(eventRepository.findByHackathonIdAndType(eq(9), any())).thenReturn(List.of());
        when(milestoneEventRescheduleService.setWorkshopKickoffTimes(any(), any(), any(), any(), any())).thenReturn(2);
        when(milestoneEventRescheduleService.setAwardsTimes(any(), any(), any())).thenReturn(1);
        doNothing().when(stakeholderBroadcastService).broadcast(
                anyInt(), anyString(), anyString(), anyString(), anyString(), any(), anyBoolean());

        var preview = service.adjust(9, newExam, null);

        assertThat(preview.isCanAdjust()).isTrue();
        assertThat(h.getScheduleAdjustedAt()).isNotNull();
        verify(hackathonRepository).findByIdForUpdate(9);
    }
}
