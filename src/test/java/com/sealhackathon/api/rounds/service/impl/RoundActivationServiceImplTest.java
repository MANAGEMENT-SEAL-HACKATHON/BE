package com.sealhackathon.api.rounds.service.impl;

import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.criteria.repository.CriteriaRepository;
import com.sealhackathon.api.criteria.service.WeightSummaryService;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.judge_assignments.repository.JudgeAssignmentRepository;
import com.sealhackathon.api.mentor_assignments.repository.MentorAssignmentRepository;
import com.sealhackathon.api.notifications.service.NotificationService;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.mapper.RoundMapper;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.team_round_participation.repository.TeamRoundParticipationRepository;
import com.sealhackathon.api.team_round_tracks.repository.TeamRoundTrackRepository;
import com.sealhackathon.api.tracks.entity.Track;
import com.sealhackathon.api.tracks.repository.TrackRepository;
import com.sealhackathon.api.tracks.value_object.TrackStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    @InjectMocks
    private RoundActivationServiceImpl activationService;

    @Test
    void activatePreliminary_failsWhenTrackHasNoJudge() {
        Round round = Round.builder()
                .id(5)
                .isFinal(false)
                .hackathon(Hackathon.builder().id(1).build())
                .build();
        Track track = Track.builder().id(10).name("Track A").status(TrackStatus.OPEN).round(round).build();

        when(roundRepository.findById(5)).thenReturn(Optional.of(round));
        when(teamRoundParticipationRepository.countByRound_Id(5)).thenReturn(3L);
        when(trackRepository.findByRoundIdOrderBySequenceOrderAsc(5)).thenReturn(List.of(track));
        when(criteriaRepository.countNormalByTrackId(10)).thenReturn(2L);
        when(weightSummaryService.isValidForTrack(10)).thenReturn(true);
        when(judgeAssignmentRepository.findByTrackId(10)).thenReturn(List.of());
        when(teamRoundTrackRepository.findByTrack_Round_Id(5)).thenReturn(List.of());

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> activationService.activate(5, "test"));
        assertEquals(ErrorCode.JUDGE_NOT_ASSIGNED, ex.getCode());
    }
}
