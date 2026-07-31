package com.sealhackathon.api.rounds.service.impl;

import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.common.security.CurrentUserAccessor;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.rounds.dto.response.CloseSubmissionEarlyResponse;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.guard.RoundAccessGuard;
import com.sealhackathon.api.rounds.mapper.RoundMapper;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.rounds.query.ScoringProgressQueryService;
import com.sealhackathon.api.rounds.value_object.RoundType;
import com.sealhackathon.api.submissions.entity.Submission;
import com.sealhackathon.api.teams.entity.Team;
import com.sealhackathon.api.teams.entity.TeamMember;
import com.sealhackathon.api.teams.entity.TeamRoundTrack;
import com.sealhackathon.api.teams.value_object.TeamMemberStatus;
import com.sealhackathon.api.teams.value_object.TeamStatus;
import com.sealhackathon.api.users.entity.User;
import com.sealhackathon.api.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoundProgressionServiceImplCloseSubmissionEarlyTest {

    @Mock private RoundRepository roundRepository;
    @Mock private RoundMapper roundMapper;
    @Mock private AuditService auditService;
    @Mock private RoundAccessGuard roundAccessGuard;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private ScoringProgressQueryService scoringProgressQueryService;
    @Mock private UserRepository userRepository;
    @Mock private CurrentUserAccessor currentUserAccessor;
    @Mock private com.sealhackathon.api.rounds.support.RoundPresentationReadiness roundPresentationReadiness;
    @Mock private com.sealhackathon.api.hackathons.repository.HackathonRepository hackathonRepository;
    @Mock private com.sealhackathon.api.notifications.service.NotificationService notificationService;
    @Mock private com.sealhackathon.api.tracks.repository.TrackRepository trackRepository;
    @Mock private com.sealhackathon.api.mentors.repository.MentorAssignmentRepository mentorAssignmentRepository;
    @Mock private com.sealhackathon.api.criteria.repository.CriteriaRepository criteriaRepository;
    @Mock private com.sealhackathon.api.judge_assignments.repository.JudgeAssignmentRepository judgeAssignmentRepository;
    @Mock private com.sealhackathon.api.scores.repository.ScoreRepository scoreRepository;
    @Mock private com.sealhackathon.api.rounds.query.RoundRankingQueryService roundRankingQueryService;
    @Mock private com.sealhackathon.api.teams.repository.TeamRoundTrackRepository teamRoundTrackRepository;
    @Mock private com.sealhackathon.api.teams.repository.TeamRoundParticipationRepository teamRoundParticipationRepository;
    @Mock private com.sealhackathon.api.judge_assignments.service.JudgeAssignmentService judgeAssignmentService;
    @Mock private com.sealhackathon.api.tiebreak_evaluations.repository.TiebreakEvaluationRepository tiebreakEvaluationRepository;
    @Mock private com.sealhackathon.api.teams.repository.TeamRepository teamRepository;
    @Mock private com.sealhackathon.api.rounds.support.RoundProblemStatementStorage problemStatementStorage;
    @Mock private com.sealhackathon.api.teams.repository.TeamMemberRepository teamMemberRepository;
    @Mock private com.sealhackathon.api.submissions.repository.SubmissionRepository submissionRepository;
    @Mock private com.sealhackathon.api.rounds.service.RoundLockScoringService roundLockScoringService;
    @Mock private com.sealhackathon.api.appeals.service.AppealWindowService appealWindowService;
    @Mock private com.sealhackathon.api.appeals.repository.AppealRepository appealRepository;

    @InjectMocks
    private RoundProgressionServiceImpl service;

    private Round round;
    private Team team1;
    private Team team2;

    @BeforeEach
    void setUp() {
        Hackathon hackathon = new Hackathon();
        hackathon.setId(1);

        LocalDateTime pastExam = LocalDateTime.now().minusMinutes(30);
        round = Round.builder()
                .id(10)
                .hackathon(hackathon)
                .roundType(RoundType.PRELIMINARY)
                .name("Sơ loại")
                .isActive(true)
                .isFinal(false)
                .isPublished(false)
                .problemReleasedAt(pastExam.minusMinutes(5))
                .examAt(pastExam)
                .submissionOpen(pastExam)
                .submissionDeadline(LocalDateTime.now().plusHours(2))
                .build();

        team1 = Team.builder().id(101).teamName("Alpha").status(TeamStatus.ACTIVE).build();
        team2 = Team.builder().id(102).teamName("Beta").status(TeamStatus.ACTIVE).build();
    }

    private void stubAllTeamsSubmitted() {
        when(teamRoundTrackRepository.findByTrack_Round_Id(10)).thenReturn(List.of(
                TeamRoundTrack.builder().team(team1).build(),
                TeamRoundTrack.builder().team(team2).build()));
        when(submissionRepository.findByRound_Id(10)).thenReturn(List.of(
                Submission.builder().team(team1).round(round).build(),
                Submission.builder().team(team2).round(round).build()));
        User student = User.builder().id(501).email("sv@test.edu").fullName("SV").build();
        when(teamMemberRepository.findByTeam_Id(101)).thenReturn(List.of(
                TeamMember.builder().user(student).status(TeamMemberStatus.ACCEPTED).build()));
        when(teamMemberRepository.findByTeam_Id(102)).thenReturn(List.of());
        when(trackRepository.findByRoundIdOrderBySequenceOrderAsc(10)).thenReturn(List.of());
    }

    @Test
    void closeSubmissionEarly_happyPath_setsClosedEarlyAtAndClampsDeadline() {
        when(roundAccessGuard.requireActiveRoundForUpdate(10)).thenReturn(round);
        when(roundRepository.save(any(Round.class))).thenAnswer(inv -> inv.getArgument(0));
        when(roundMapper.toSummary(any(Round.class), anyInt(), anyInt(), anyFloat()))
                .thenReturn(null);
        stubAllTeamsSubmitted();

        CloseSubmissionEarlyResponse response = service.closeSubmissionEarly(10);

        assertThat(response.getClosedAt()).isNotNull();
        assertThat(response.isDeadlineAdjusted()).isTrue();
        assertThat(response.isExamAtAdjusted()).isFalse();

        ArgumentCaptor<Round> captor = ArgumentCaptor.forClass(Round.class);
        verify(roundRepository).save(captor.capture());
        Round saved = captor.getValue();
        assertThat(saved.getSubmissionClosedEarlyAt()).isNotNull();
        assertThat(saved.getSubmissionDeadline()).isBeforeOrEqualTo(saved.getSubmissionClosedEarlyAt());
        assertThat(saved.getExamAt()).isEqualTo(round.getExamAt());
        verify(notificationService, atLeastOnce()).sendBatch(
                anyList(), eq("SUBMISSION_CLOSED_EARLY"), anyString(), anyString(), eq("rounds"), eq(10));
    }

    @Test
    void closeSubmissionEarly_missingSubmission_rejectsWithTeamsNotAllSubmitted() {
        when(roundAccessGuard.requireActiveRoundForUpdate(10)).thenReturn(round);
        when(teamRoundTrackRepository.findByTrack_Round_Id(10)).thenReturn(List.of(
                TeamRoundTrack.builder().team(team1).build(),
                TeamRoundTrack.builder().team(team2).build()));
        when(submissionRepository.findByRound_Id(10)).thenReturn(List.of(
                Submission.builder().team(team1).round(round).build()));

        assertThatThrownBy(() -> service.closeSubmissionEarly(10))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> {
                    BusinessRuleException bre = (BusinessRuleException) ex;
                    assertThat(bre.getCode()).isEqualTo(ErrorCode.TEAMS_NOT_ALL_SUBMITTED);
                    assertThat(bre.getDetails()).containsEntry("submitted", 1);
                    assertThat(bre.getDetails()).containsEntry("total", 2);
                    assertThat(bre.getDetails().get("missingTeamIds")).isEqualTo(List.of(102));
                });
    }

    @Test
    void closeSubmissionEarly_noEligibleTeams_rejects() {
        when(roundAccessGuard.requireActiveRoundForUpdate(10)).thenReturn(round);
        when(teamRoundTrackRepository.findByTrack_Round_Id(10)).thenReturn(List.of());

        assertThatThrownBy(() -> service.closeSubmissionEarly(10))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getCode())
                        .isEqualTo(ErrorCode.INVALID_STATE));
    }

    /** TC-GATE-01 */
    @Test
    void closeSubmissionEarly_unreleased_rejectsWithUnreleasedCode() {
        round.setProblemReleasedAt(null);
        when(roundAccessGuard.requireActiveRoundForUpdate(10)).thenReturn(round);

        assertThatThrownBy(() -> service.closeSubmissionEarly(10))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getCode())
                        .isEqualTo(ErrorCode.INVALID_ROUND_STATE_UNRELEASED));
    }

    /** TC-GATE-02 */
    @Test
    void closeSubmissionEarly_beforeExamAt_rejectsWithBeforeExamCode() {
        round.setExamAt(LocalDateTime.now().plusHours(1));
        when(roundAccessGuard.requireActiveRoundForUpdate(10)).thenReturn(round);

        assertThatThrownBy(() -> service.closeSubmissionEarly(10))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getCode())
                        .isEqualTo(ErrorCode.INVALID_ROUND_STATE_BEFORE_EXAM));
    }

    @Test
    void closeSubmissionEarly_nullExamAt_rejectsWithBeforeExamCode() {
        round.setExamAt(null);
        when(roundAccessGuard.requireActiveRoundForUpdate(10)).thenReturn(round);

        assertThatThrownBy(() -> service.closeSubmissionEarly(10))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getCode())
                        .isEqualTo(ErrorCode.INVALID_ROUND_STATE_BEFORE_EXAM));
    }

    @Test
    void closeSubmissionEarly_alreadyClosed_rejects() {
        round.setSubmissionClosedEarlyAt(LocalDateTime.now().minusMinutes(5));
        when(roundAccessGuard.requireActiveRoundForUpdate(10)).thenReturn(round);

        assertThatThrownBy(() -> service.closeSubmissionEarly(10))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getCode())
                        .isEqualTo(ErrorCode.SUBMISSION_ALREADY_CLOSED));
    }

    @Test
    void closeSubmissionEarly_withoutSubmissionDeadline_setsDeadlineToNow() {
        round.setSubmissionDeadline(null);
        when(roundAccessGuard.requireActiveRoundForUpdate(10)).thenReturn(round);
        when(roundRepository.save(any(Round.class))).thenAnswer(inv -> inv.getArgument(0));
        when(roundMapper.toSummary(any(Round.class), anyInt(), anyInt(), anyFloat()))
                .thenReturn(null);
        stubAllTeamsSubmitted();

        CloseSubmissionEarlyResponse response = service.closeSubmissionEarly(10);

        assertThat(response.isDeadlineAdjusted()).isTrue();
        ArgumentCaptor<Round> captor = ArgumentCaptor.forClass(Round.class);
        verify(roundRepository).save(captor.capture());
        assertThat(captor.getValue().getSubmissionDeadline())
                .isBeforeOrEqualTo(captor.getValue().getSubmissionClosedEarlyAt());
    }

    /** TC-GATE-04 — submissionOpen must not sit after submissionDeadline after close. */
    @Test
    void closeSubmissionEarly_normalizesSubmissionOpenNotAfterDeadline() {
        LocalDateTime pastExam = LocalDateTime.now().minusMinutes(30);
        round.setExamAt(pastExam);
        round.setSubmissionOpen(LocalDateTime.now().plusHours(5));
        round.setSubmissionDeadline(LocalDateTime.now().plusHours(2));
        when(roundAccessGuard.requireActiveRoundForUpdate(10)).thenReturn(round);
        when(roundRepository.save(any(Round.class))).thenAnswer(inv -> inv.getArgument(0));
        when(roundMapper.toSummary(any(Round.class), anyInt(), anyInt(), anyFloat()))
                .thenReturn(null);
        stubAllTeamsSubmitted();

        service.closeSubmissionEarly(10);

        ArgumentCaptor<Round> captor = ArgumentCaptor.forClass(Round.class);
        verify(roundRepository).save(captor.capture());
        Round saved = captor.getValue();
        assertThat(saved.getSubmissionOpen()).isNotNull();
        assertThat(saved.getSubmissionOpen()).isBeforeOrEqualTo(saved.getSubmissionDeadline());
    }
}
