package com.sealhackathon.api.rounds.service.impl;

import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.common.value_object.AssignmentResponseStatus;
import com.sealhackathon.api.criteria.repository.CriteriaRepository;
import com.sealhackathon.api.criteria.service.WeightSummaryService;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.judge_assignments.entity.JudgeAssignment;
import com.sealhackathon.api.judge_assignments.repository.JudgeAssignmentRepository;
import com.sealhackathon.api.judge_assignments.value_object.JudgeAssignmentType;
import com.sealhackathon.api.mentors.repository.MentorAssignmentRepository;
import com.sealhackathon.api.notifications.service.NotificationService;
import com.sealhackathon.api.rounds.dto.request.ActivateRoundRequest;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.mapper.RoundMapper;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.rounds.support.RoundScheduleShiftService;
import com.sealhackathon.api.teams.repository.TeamRoundParticipationRepository;
import com.sealhackathon.api.teams.repository.TeamRoundTrackRepository;
import com.sealhackathon.api.tracks.entity.Track;
import com.sealhackathon.api.tracks.repository.TrackRepository;
import com.sealhackathon.api.tracks.value_object.TrackStatus;
import com.sealhackathon.api.users.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RoundActivationServiceImplDeclineGateTest {

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
    void activate_excludesDeclinedJudgesFromTrackGate() {
        Round round = Round.builder()
                .id(5)
                .isFinal(false)
                .isActive(false)
                .hackathon(Hackathon.builder().id(1).build())
                .build();
        Track track = Track.builder().id(3).name("A").status(TrackStatus.OPEN).round(round).build();
        JudgeAssignment declined = JudgeAssignment.builder()
                .id(1)
                .judge(User.builder().id(9).build())
                .track(track)
                .assignmentType(JudgeAssignmentType.NORMAL)
                .responseStatus(AssignmentResponseStatus.DECLINED)
                .build();

        when(roundRepository.findByIdForUpdate(5)).thenReturn(Optional.of(round));
        doNothing().when(pendingTeamGateService).assertNoPendingTeams(1);
        when(trackRepository.findByRoundIdOrderBySequenceOrderAsc(5)).thenReturn(List.of(track));
        when(criteriaRepository.countNormalByTrackId(3)).thenReturn(1L);
        when(weightSummaryService.isValidForTrack(3)).thenReturn(true);
        when(judgeAssignmentRepository.findByTrackId(3)).thenReturn(List.of(declined));
        when(teamRoundTrackRepository.countByTrack_Id(3)).thenReturn(2L);
        assertThatThrownBy(() -> activationService.activate(5, ActivateRoundRequest.builder().build()))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(ex -> ((BusinessRuleException) ex).getCode())
                .isEqualTo(ErrorCode.JUDGE_NOT_ASSIGNED);
    }
}
