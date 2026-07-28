package com.sealhackathon.api.rounds.service.impl;

import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.criteria.repository.CriteriaRepository;
import com.sealhackathon.api.criteria.service.WeightSummaryService;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.judge_assignments.entity.JudgeAssignment;
import com.sealhackathon.api.judge_assignments.repository.JudgeAssignmentRepository;
import com.sealhackathon.api.judge_assignments.value_object.JudgeAssignmentType;
import com.sealhackathon.api.mentors.repository.MentorAssignmentRepository;
import com.sealhackathon.api.notifications.service.NotificationService;
import com.sealhackathon.api.rounds.dto.request.ActivateRoundRequest;
import com.sealhackathon.api.rounds.dto.response.RoundResponse;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.mapper.RoundMapper;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.rounds.support.RoundScheduleShiftService;
import com.sealhackathon.api.rounds.value_object.ActivateScheduleMode;
import com.sealhackathon.api.teams.repository.TeamRoundParticipationRepository;
import com.sealhackathon.api.teams.repository.TeamRoundTrackRepository;
import com.sealhackathon.api.tracks.entity.Track;
import com.sealhackathon.api.tracks.repository.TrackRepository;
import com.sealhackathon.api.tracks.value_object.TrackStatus;
import com.sealhackathon.api.users.entity.User;
import com.sealhackathon.api.users.value_object.UserType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RoundActivationServiceImplTest {

    @Mock private RoundRepository roundRepository;
    @Mock private TrackRepository trackRepository;
    @Mock private CriteriaRepository criteriaRepository;
    @Mock private MentorAssignmentRepository mentorAssignmentRepository;
    @Mock private JudgeAssignmentRepository judgeAssignmentRepository;
    @Mock private RoundMapper roundMapper;
    @Mock private AuditService auditService;
    @Mock private WeightSummaryService weightSummaryService;
    @Mock private NotificationService notificationService;
    @Mock private TeamRoundParticipationRepository teamRoundParticipationRepository;
    @Mock private TeamRoundTrackRepository teamRoundTrackRepository;
    @Mock private RoundScheduleShiftService roundScheduleShiftService;
    @Mock private com.sealhackathon.api.hackathons.support.PendingTeamGateService pendingTeamGateService;

    @InjectMocks
    private RoundActivationServiceImpl activationService;

    @Test
    void activate_whenAlreadyActive_returnsIdempotentResponse() {
        Round round = Round.builder()
                .id(5)
                .isActive(true)
                .isFinal(false)
                .hackathon(Hackathon.builder().id(1).build())
                .build();
        RoundResponse response = RoundResponse.builder().id(5).isActive(true).build();

        when(roundRepository.findByIdForUpdate(5)).thenReturn(Optional.of(round));
        when(roundMapper.toResponse(round)).thenReturn(response);

        RoundResponse result = activationService.activate(5,
                ActivateRoundRequest.builder().note("re-activate").build());

        assertEquals(5, result.getId());
        assertEquals(true, result.getIsActive());
        verifyNoInteractions(notificationService);
        verify(roundScheduleShiftService, never()).applyOnActivate(any(), any(), any(), any());
    }

    /** J3 — vòng đã active + START_NOW phải nén lịch (examAt/deadline) thay vì no-op. */
    @Test
    void activate_alreadyActive_startNow_compressesSchedule() {
        LocalDateTime oldExam = LocalDateTime.now().plusHours(5);
        Round round = Round.builder()
                .id(5)
                .isActive(true)
                .isFinal(false)
                .codingDurationHours(7)
                .examAt(oldExam)
                .hackathon(Hackathon.builder().id(1).build())
                .build();

        when(roundRepository.findByIdForUpdate(5)).thenReturn(Optional.of(round));
        when(roundScheduleShiftService.applyOnActivate(
                eq(round), eq(ActivateScheduleMode.START_NOW), any(), eq(10)))
                .thenAnswer(inv -> {
                    Round r = inv.getArgument(0);
                    r.setExamAt(LocalDateTime.now().plusMinutes(10));
                    return true;
                });
        when(roundRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(roundMapper.toResponse(any())).thenAnswer(inv -> {
            Round r = inv.getArgument(0);
            return RoundResponse.builder().id(r.getId()).isActive(r.getIsActive()).examAt(r.getExamAt()).build();
        });

        RoundResponse result = activationService.activate(5, ActivateRoundRequest.builder()
                .scheduleMode(ActivateScheduleMode.START_NOW)
                .setupLeadMinutes(10)
                .build());

        assertNotNull(result.getExamAt());
        assertFalse(result.getExamAt().isAfter(oldExam));
        verify(roundScheduleShiftService).applyOnActivate(
                eq(round), eq(ActivateScheduleMode.START_NOW), any(), eq(10));
        verify(roundRepository).save(round);
        verifyNoInteractions(notificationService);
    }

    /** J3 — đã phát đề thì không cho nén lịch nữa. */
    @Test
    void activate_alreadyActive_startNow_blockedAfterProblemReleased() {
        Round round = Round.builder()
                .id(5)
                .isActive(true)
                .isFinal(false)
                .codingDurationHours(7)
                .examAt(LocalDateTime.now().plusHours(5))
                .problemReleasedAt(LocalDateTime.now().minusMinutes(1))
                .hackathon(Hackathon.builder().id(1).build())
                .build();
        when(roundRepository.findByIdForUpdate(5)).thenReturn(Optional.of(round));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> activationService.activate(5, ActivateRoundRequest.builder()
                        .scheduleMode(ActivateScheduleMode.START_NOW)
                        .build()));
        assertEquals(ErrorCode.INVALID_STATE, ex.getCode());
        verify(roundScheduleShiftService, never()).applyOnActivate(any(), any(), any(), any());
    }

    @Test
    void activate_keep_doesNotShiftSchedule() {
        Round round = Round.builder()
                .id(5)
                .isFinal(false)
                .isActive(false)
                .hackathon(Hackathon.builder().id(1).build())
                .build();
        Track track = Track.builder().id(10).name("Track A").status(TrackStatus.OPEN).round(round).build();

        when(roundRepository.findByIdForUpdate(5)).thenReturn(Optional.of(round));
        when(teamRoundParticipationRepository.countByRound_Id(5)).thenReturn(3L);
        when(trackRepository.findByRoundIdOrderBySequenceOrderAsc(5)).thenReturn(List.of(track));
        when(teamRoundTrackRepository.countByTrack_Id(10)).thenReturn(1L);
        when(criteriaRepository.countNormalByTrackId(10)).thenReturn(2L);
        when(weightSummaryService.isValidForTrack(10)).thenReturn(true);
        when(judgeAssignmentRepository.findByTrackId(10)).thenReturn(List.of(
                JudgeAssignment.builder().judge(User.builder().id(1).build()).build()));
        when(mentorAssignmentRepository.findByTrackId(10)).thenReturn(List.of());
        when(roundScheduleShiftService.applyOnActivate(
                eq(round), eq(ActivateScheduleMode.KEEP), any(), any()))
                .thenReturn(false);
        when(roundRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(roundMapper.toResponse(any())).thenReturn(RoundResponse.builder().id(5).isActive(true).build());

        RoundResponse result = activationService.activate(5,
                ActivateRoundRequest.builder().scheduleMode(ActivateScheduleMode.KEEP).build());

        assertEquals(true, result.getIsActive());
        verify(roundScheduleShiftService).applyOnActivate(
                eq(round), eq(ActivateScheduleMode.KEEP), any(), any());
    }

    @Test
    void activatePreliminary_failsWhenTrackHasNoJudge() {
        Round round = Round.builder()
                .id(5)
                .isFinal(false)
                .hackathon(Hackathon.builder().id(1).build())
                .build();
        Track track = Track.builder().id(10).name("Track A").status(TrackStatus.OPEN).round(round).build();

        when(roundRepository.findByIdForUpdate(5)).thenReturn(Optional.of(round));
        when(teamRoundParticipationRepository.countByRound_Id(5)).thenReturn(3L);
        when(trackRepository.findByRoundIdOrderBySequenceOrderAsc(5)).thenReturn(List.of(track));
        when(teamRoundTrackRepository.countByTrack_Id(10)).thenReturn(1L);
        when(criteriaRepository.countNormalByTrackId(10)).thenReturn(2L);
        when(weightSummaryService.isValidForTrack(10)).thenReturn(true);
        when(judgeAssignmentRepository.findByTrackId(10)).thenReturn(List.of());
        when(teamRoundTrackRepository.findByTrack_Round_Id(5)).thenReturn(List.of());

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> activationService.activate(5, ActivateRoundRequest.builder().note("test").build()));
        assertEquals(ErrorCode.JUDGE_NOT_ASSIGNED, ex.getCode());
    }

    /** LOT-04 — activate prelim bị chặn khi còn đội PENDING. */
    @Test
    void activatePreliminary_failsWhenPendingTeamsRemain() {
        Round round = Round.builder()
                .id(5)
                .isFinal(false)
                .isActive(false)
                .hackathon(Hackathon.builder().id(1).build())
                .build();
        when(roundRepository.findByIdForUpdate(5)).thenReturn(Optional.of(round));
        org.mockito.Mockito.doThrow(new BusinessRuleException(
                        ErrorCode.TEAMS_PENDING_APPROVAL,
                        "Còn đội PENDING",
                        java.util.Map.of("pendingTotal", 2)))
                .when(pendingTeamGateService).assertNoPendingTeams(1);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> activationService.activate(5, ActivateRoundRequest.builder().build()));
        assertEquals(ErrorCode.TEAMS_PENDING_APPROVAL, ex.getCode());
        verify(roundRepository, never()).save(any());
    }

    @Test
    void activateFinal_allowsHeadPlusFinalExternal() {
        Round round = Round.builder()
                .id(9)
                .isFinal(true)
                .isActive(false)
                .hackathon(Hackathon.builder().id(1).build())
                .build();
        User head = User.builder().id(1).userType(UserType.INTERNAL).build();
        User guest = User.builder().id(2).userType(UserType.EXTERNAL).build();
        List<JudgeAssignment> assignments = List.of(
                JudgeAssignment.builder().judge(head).assignmentType(JudgeAssignmentType.HEAD).build(),
                JudgeAssignment.builder().judge(guest).assignmentType(JudgeAssignmentType.FINAL_EXTERNAL).build());

        when(roundRepository.findByIdForUpdate(9)).thenReturn(Optional.of(round));
        when(teamRoundParticipationRepository.countByRound_Id(9)).thenReturn(2L);
        when(criteriaRepository.countNormalByFinalRoundId(9)).thenReturn(1L);
        when(criteriaRepository.sumWeightExcludingPenaltyByFinalRoundId(9)).thenReturn(Optional.of(1.0));
        when(judgeAssignmentRepository.findByRoundId(9)).thenReturn(assignments);
        when(roundScheduleShiftService.applyOnActivate(any(), any(), any(), any())).thenReturn(false);
        when(roundMapper.toResponse(any())).thenReturn(RoundResponse.builder().id(9).isActive(true).build());
        when(roundRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        when(roundRepository.findByHackathon_IdOrderByExamAtAsc(1)).thenReturn(List.of(
                Round.builder().id(1).isFinal(false).isPublished(true).build(),
                round));

        RoundResponse result = activationService.activate(9,
                ActivateRoundRequest.builder().note("test").build());
        assertEquals(9, result.getId());
        assertEquals(true, Boolean.TRUE.equals(round.getIsActive()));
        assertNotNull(round.getProblemReleasedAt());
    }

    @Test
    void activateFinal_stampsProblemReleasedAtWhenNull() {
        Round round = Round.builder()
                .id(9)
                .isFinal(true)
                .isActive(false)
                .problemReleasedAt(null)
                .hackathon(Hackathon.builder().id(1).build())
                .build();
        User head = User.builder().id(1).userType(UserType.INTERNAL).build();
        User guest = User.builder().id(2).userType(UserType.EXTERNAL).build();
        List<JudgeAssignment> assignments = List.of(
                JudgeAssignment.builder().judge(head).assignmentType(JudgeAssignmentType.HEAD).build(),
                JudgeAssignment.builder().judge(guest).assignmentType(JudgeAssignmentType.FINAL_EXTERNAL).build());

        when(roundRepository.findByIdForUpdate(9)).thenReturn(Optional.of(round));
        when(criteriaRepository.countNormalByFinalRoundId(9)).thenReturn(1L);
        when(criteriaRepository.sumWeightExcludingPenaltyByFinalRoundId(9)).thenReturn(Optional.of(1.0));
        when(judgeAssignmentRepository.findByRoundId(9)).thenReturn(assignments);
        when(roundScheduleShiftService.applyOnActivate(any(), any(), any(), any())).thenReturn(false);
        when(roundMapper.toResponse(any())).thenAnswer(inv -> {
            Round r = inv.getArgument(0);
            return RoundResponse.builder()
                    .id(r.getId())
                    .isActive(r.getIsActive())
                    .problemReleasedAt(r.getProblemReleasedAt())
                    .build();
        });
        when(roundRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(roundRepository.findByHackathon_IdOrderByExamAtAsc(1)).thenReturn(List.of(
                Round.builder().id(1).isFinal(false).isPublished(true).build(),
                round));

        RoundResponse result = activationService.activate(9,
                ActivateRoundRequest.builder().note("ck").build());

        assertNotNull(result.getProblemReleasedAt());
        assertNotNull(round.getProblemReleasedAt());
        assertEquals(round.getActivatedAt(), round.getProblemReleasedAt());
    }

    @Test
    void activate_startNow_passesSetupLeadMinutesToShiftService() {
        Round round = Round.builder()
                .id(5)
                .isFinal(false)
                .isActive(false)
                .hackathon(Hackathon.builder().id(1).build())
                .build();
        Track track = Track.builder().id(10).name("Track A").status(TrackStatus.OPEN).round(round).build();

        when(roundRepository.findByIdForUpdate(5)).thenReturn(Optional.of(round));
        when(teamRoundParticipationRepository.countByRound_Id(5)).thenReturn(3L);
        when(trackRepository.findByRoundIdOrderBySequenceOrderAsc(5)).thenReturn(List.of(track));
        when(teamRoundTrackRepository.countByTrack_Id(10)).thenReturn(1L);
        when(criteriaRepository.countNormalByTrackId(10)).thenReturn(2L);
        when(weightSummaryService.isValidForTrack(10)).thenReturn(true);
        when(judgeAssignmentRepository.findByTrackId(10)).thenReturn(List.of(
                JudgeAssignment.builder().judge(User.builder().id(1).build()).build()));
        when(mentorAssignmentRepository.findByTrackId(10)).thenReturn(List.of());
        when(roundScheduleShiftService.applyOnActivate(
                eq(round), eq(ActivateScheduleMode.START_NOW), any(), eq(10)))
                .thenReturn(true);
        when(roundRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(roundMapper.toResponse(any())).thenReturn(RoundResponse.builder().id(5).isActive(true).build());

        activationService.activate(5, ActivateRoundRequest.builder()
                .scheduleMode(ActivateScheduleMode.START_NOW)
                .setupLeadMinutes(10)
                .build());

        verify(roundScheduleShiftService).applyOnActivate(
                eq(round), eq(ActivateScheduleMode.START_NOW), any(), eq(10));
    }

    /** TC-BE-RESCHEDULE — sống còn: inactive, activatedAt không đổi, không notifyRoundStarted */
    @Test
    void reschedule_keepsInactive_doesNotTouchActivatedAt_doesNotNotifyStarted() {
        LocalDateTime newExam = LocalDateTime.now().plusDays(3).withSecond(0).withNano(0);
        Round round = Round.builder()
                .id(5)
                .isFinal(false)
                .isActive(false)
                .activatedAt(null)
                .codingDurationHours(7)
                .examAt(LocalDateTime.now().plusDays(10))
                .hackathon(Hackathon.builder().id(1).build())
                .build();

        when(roundRepository.findByIdForUpdate(5)).thenReturn(Optional.of(round));
        when(roundScheduleShiftService.applyOnActivate(
                eq(round), eq(ActivateScheduleMode.RESCHEDULE), eq(newExam), any()))
                .thenAnswer(inv -> {
                    Round r = inv.getArgument(0);
                    r.setExamAt(newExam);
                    return true;
                });
        when(roundRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(roundMapper.toResponse(any())).thenAnswer(inv -> {
            Round r = inv.getArgument(0);
            return RoundResponse.builder()
                    .id(r.getId())
                    .isActive(r.getIsActive())
                    .examAt(r.getExamAt())
                    .build();
        });

        LocalDateTime activatedAtBefore = round.getActivatedAt();

        RoundResponse result = activationService.activate(5, ActivateRoundRequest.builder()
                .scheduleMode(ActivateScheduleMode.RESCHEDULE)
                .newExamAt(newExam)
                .note("Dời lịch")
                .build());

        assertFalse(Boolean.TRUE.equals(result.getIsActive()));
        assertFalse(Boolean.TRUE.equals(round.getIsActive()));
        assertNull(round.getActivatedAt());
        assertEquals(activatedAtBefore, round.getActivatedAt());
        assertEquals(newExam, round.getExamAt());

        verify(roundScheduleShiftService).applyOnActivate(
                eq(round), eq(ActivateScheduleMode.RESCHEDULE), eq(newExam), any());
        verifyNoInteractions(notificationService);
        verify(teamRoundParticipationRepository, never()).countByRound_Id(anyInt());
        verify(roundRepository, never()).deactivateOtherActiveRoundsInHackathon(anyInt(), anyInt());
    }
}
