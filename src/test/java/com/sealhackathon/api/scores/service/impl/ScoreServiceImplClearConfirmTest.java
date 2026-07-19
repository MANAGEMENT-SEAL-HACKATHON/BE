package com.sealhackathon.api.scores.service.impl;

import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.common.security.CurrentUserAccessor;
import com.sealhackathon.api.criteria.entity.Criteria;
import com.sealhackathon.api.criteria.repository.CriteriaRepository;
import com.sealhackathon.api.events.entity.PresentationSlot;
import com.sealhackathon.api.events.repository.JudgeSubmissionScoringConfirmationRepository;
import com.sealhackathon.api.events.repository.PresentationSlotRepository;
import com.sealhackathon.api.presentation.support.RoundPhaseResolver;
import com.sealhackathon.api.presentation.value_object.PresentationQueueStatus;
import com.sealhackathon.api.presentation.value_object.PresentationTimerPhase;
import com.sealhackathon.api.presentation.value_object.RoundPhase;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.scores.dto.request.SubmitScoreRequest;
import com.sealhackathon.api.scores.entity.Score;
import com.sealhackathon.api.scores.guard.JudgeAssignmentGuard;
import com.sealhackathon.api.scores.guard.MentorJudgeConflictGuard;
import com.sealhackathon.api.scores.repository.ScoreRepository;
import com.sealhackathon.api.scores.value_object.ScoreType;
import com.sealhackathon.api.submissions.entity.Submission;
import com.sealhackathon.api.submissions.repository.SubmissionRepository;
import com.sealhackathon.api.submissions.value_object.SubmissionStatus;
import com.sealhackathon.api.teams.entity.Team;
import com.sealhackathon.api.teams.repository.TeamRoundTrackRepository;
import com.sealhackathon.api.tracks.entity.Track;
import com.sealhackathon.api.users.entity.User;
import com.sealhackathon.api.users.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ScoreServiceImplClearConfirmTest {

    @Mock private ScoreRepository scoreRepository;
    @Mock private SubmissionRepository submissionRepository;
    @Mock private CriteriaRepository criteriaRepository;
    @Mock private RoundRepository roundRepository;
    @Mock private UserRepository userRepository;
    @Mock private CurrentUserAccessor currentUserAccessor;
    @Mock private JudgeAssignmentGuard judgeAssignmentGuard;
    @Mock private MentorJudgeConflictGuard mentorJudgeConflictGuard;
    @Mock private TeamRoundTrackRepository teamRoundTrackRepository;
    @Mock private PresentationSlotRepository presentationSlotRepository;
    @Mock private AuditService auditService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private RoundPhaseResolver roundPhaseResolver;
    @Mock private JudgeSubmissionScoringConfirmationRepository scoringConfirmationRepository;

    @InjectMocks private ScoreServiceImpl scoreService;

    @Test
    void submitScore_clearsConfirmationForJudge() {
        Round round = Round.builder().id(1).isFinal(false).scoringLocked(false).build();
        Track track = Track.builder().id(7).round(round).build();
        Criteria criterion = Criteria.builder().id(101).maxScore(10).track(track).build();
        Team team = Team.builder().id(1).build();
        Submission submission = Submission.builder()
                .id(10)
                .round(round)
                .track(track)
                .team(team)
                .status(SubmissionStatus.SUBMITTED)
                .build();
        User judge = User.builder().id(5).build();
        PresentationSlot slot = PresentationSlot.builder()
                .queueStatus(PresentationQueueStatus.PRESENTING)
                .timerPhase(PresentationTimerPhase.PRESENTING)
                .build();

        when(currentUserAccessor.currentUserId()).thenReturn(5);
        when(submissionRepository.findById(10)).thenReturn(Optional.of(submission));
        when(criteriaRepository.findById(101)).thenReturn(Optional.of(criterion));
        when(roundRepository.findByIdForUpdate(1)).thenReturn(Optional.of(round));
        when(roundPhaseResolver.resolve(round)).thenReturn(RoundPhase.JUDGING);
        when(presentationSlotRepository.findByRound_IdAndSubmission_Id(1, 10))
                .thenReturn(Optional.of(slot));
        when(userRepository.findById(5)).thenReturn(Optional.of(judge));
        when(scoreRepository.findBySubmission_IdAndJudge_IdAndCriterion_IdAndScoreType(
                10, 5, 101, ScoreType.NORMAL)).thenReturn(Optional.empty());
        when(scoreRepository.save(any(Score.class))).thenAnswer(inv -> {
            Score s = inv.getArgument(0);
            s.setId(99);
            return s;
        });

        scoreService.submitScore(SubmitScoreRequest.builder()
                .submissionId(10)
                .criterionId(101)
                .scoreValue(8f)
                .build());

        verify(scoringConfirmationRepository).deleteBySubmission_IdAndJudge_Id(10, 5);
    }
}
