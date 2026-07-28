package com.sealhackathon.api.presentation.support;

import com.sealhackathon.api.criteria.repository.CriteriaRepository;
import com.sealhackathon.api.events.repository.JudgeSubmissionScoringConfirmationRepository;
import com.sealhackathon.api.judge_assignments.entity.JudgeAssignment;
import com.sealhackathon.api.judge_assignments.repository.JudgeAssignmentRepository;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.scores.entity.Score;
import com.sealhackathon.api.scores.repository.ScoreRepository;
import com.sealhackathon.api.scores.value_object.ScoreType;
import com.sealhackathon.api.submissions.entity.Submission;
import com.sealhackathon.api.tracks.entity.Track;
import com.sealhackathon.api.criteria.entity.Criteria;
import com.sealhackathon.api.users.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PresentationScoringCompletionHelperTest {

    @Mock private JudgeAssignmentRepository judgeAssignmentRepository;
    @Mock private ScoreRepository scoreRepository;
    @Mock private CriteriaRepository criteriaRepository;
    @Mock private JudgeSubmissionScoringConfirmationRepository scoringConfirmationRepository;

    @InjectMocks private PresentationScoringCompletionHelper helper;

    @Test
    void hasJudgeFullyScored_finalRound_oneCriterionScored_returnsTrue() {
        Round finalRound = Round.builder().id(99).isFinal(true).build();
        Submission submission = Submission.builder()
                .id(10)
                .round(finalRound)
                .track(null)
                .build();

        when(criteriaRepository.countNormalByFinalRoundId(99)).thenReturn(1L);
        when(scoreRepository.findBySubmission_IdAndScoreType(10, ScoreType.NORMAL)).thenReturn(List.of(
                score(10, 5, 501)
        ));

        assertThat(helper.hasJudgeFullyScored(5, submission)).isTrue();
    }

    @Test
    void hasJudgeFullyScored_finalRound_ignoresLegacyTrackOnSubmission() {
        Round finalRound = Round.builder().id(99).isFinal(true).build();
        Track prelimTrack = Track.builder().id(7).build();
        Submission submission = Submission.builder()
                .id(10)
                .round(finalRound)
                .track(prelimTrack)
                .build();

        when(criteriaRepository.countNormalByFinalRoundId(99)).thenReturn(1L);
        when(scoreRepository.findBySubmission_IdAndScoreType(10, ScoreType.NORMAL)).thenReturn(List.of(
                score(10, 5, 501)
        ));

        assertThat(helper.hasJudgeFullyScored(5, submission)).isTrue();
    }

    @Test
    void hasJudgeFullyScored_prelimTrack_requiresAllTrackCriteria() {
        Round prelimRound = Round.builder().id(1).isFinal(false).build();
        Track track = Track.builder().id(7).round(prelimRound).build();
        Submission submission = Submission.builder()
                .id(10)
                .round(prelimRound)
                .track(track)
                .build();

        when(criteriaRepository.countNormalByTrackId(7)).thenReturn(3L);
        when(scoreRepository.findBySubmission_IdAndScoreType(10, ScoreType.NORMAL)).thenReturn(List.of(
                score(10, 5, 101)
        ));

        assertThat(helper.hasJudgeFullyScored(5, submission)).isFalse();
    }

    @Test
    void canAdvanceQueue_finalRound_requiresConfirmation_evenForSingleJudge() {
        Round finalRound = Round.builder().id(99).isFinal(true).build();
        Submission submission = Submission.builder().id(10).round(finalRound).build();

        when(judgeAssignmentRepository.findByRoundId(99)).thenReturn(List.of());
        when(scoringConfirmationRepository.countDistinctJudgesBySubmissionId(10)).thenReturn(0);
        assertThat(helper.canAdvanceQueue(submission, null, 99)).isFalse();

        when(scoringConfirmationRepository.countDistinctJudgesBySubmissionId(10)).thenReturn(1);
        assertThat(helper.canAdvanceQueue(submission, null, 99)).isTrue();
    }

    @Test
    void canAdvanceQueue_prelimSingleJudge_usesFullyScoredWithoutConfirm() {
        Round prelimRound = Round.builder().id(1).isFinal(false).build();
        Track track = Track.builder().id(7).round(prelimRound).build();
        Submission submission = Submission.builder().id(10).round(prelimRound).track(track).build();

        when(judgeAssignmentRepository.findByTrackId(7)).thenReturn(List.of());
        when(criteriaRepository.countNormalByTrackId(7)).thenReturn(1L);
        when(scoreRepository.findBySubmission_IdAndScoreType(10, ScoreType.NORMAL)).thenReturn(List.of(
                score(10, 5, 101)
        ));

        assertThat(helper.canAdvanceQueue(submission, 7, 1)).isTrue();
    }

    @Test
    void canAdvanceQueue_prelimTwoJudges_requiresAllConfirmations() {
        Round prelimRound = Round.builder().id(1).isFinal(false).build();
        Track track = Track.builder().id(7).round(prelimRound).build();
        Submission submission = Submission.builder().id(10).round(prelimRound).track(track).build();
        List<JudgeAssignment> twoJudges = List.of(JudgeAssignment.builder().build(), JudgeAssignment.builder().build());

        when(judgeAssignmentRepository.findByTrackId(7)).thenReturn(twoJudges);
        when(scoringConfirmationRepository.countDistinctJudgesBySubmissionId(10)).thenReturn(1);
        assertThat(helper.canAdvanceQueue(submission, 7, 1)).isFalse();

        when(scoringConfirmationRepository.countDistinctJudgesBySubmissionId(10)).thenReturn(2);
        assertThat(helper.canAdvanceQueue(submission, 7, 1)).isTrue();
    }

    @Test
    void canAdvanceQueue_finalRoundTwoJudges_sameConfirmGateAsPrelim() {
        Round finalRound = Round.builder().id(99).isFinal(true).build();
        Submission submission = Submission.builder().id(10).round(finalRound).build();
        List<JudgeAssignment> twoJudges = List.of(JudgeAssignment.builder().build(), JudgeAssignment.builder().build());

        when(judgeAssignmentRepository.findByRoundId(99)).thenReturn(twoJudges);
        when(scoringConfirmationRepository.countDistinctJudgesBySubmissionId(10)).thenReturn(1);
        assertThat(helper.canAdvanceQueue(submission, null, 99)).isFalse();

        when(scoringConfirmationRepository.countDistinctJudgesBySubmissionId(10)).thenReturn(2);
        assertThat(helper.canAdvanceQueue(submission, null, 99)).isTrue();
    }

    private static Score score(int submissionId, int judgeId, int criterionId) {
        return Score.builder()
                .submission(Submission.builder().id(submissionId).build())
                .judge(User.builder().id(judgeId).build())
                .criterion(Criteria.builder().id(criterionId).build())
                .scoreType(ScoreType.NORMAL)
                .build();
    }
}
