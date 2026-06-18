package com.sealhackathon.api.presentation.support;

import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.scores.repository.ScoreRepository;
import com.sealhackathon.api.scores.value_object.ScoreType;
import com.sealhackathon.api.submissions.entity.Submission;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PresentationNextScoringGuardTest {

    @Mock private ScoreRepository scoreRepository;
    @Mock private PresentationScoringCompletionHelper scoringCompletionHelper;

    @InjectMocks private PresentationNextScoringGuard guard;

    private final Round round = Round.builder().id(1).build();
    private final Submission submission = Submission.builder().id(10).build();

    @Test
    void validateBeforeNext_blocksWhenNoScores() {
        when(scoringCompletionHelper.countAssignedJudges(5, round)).thenReturn(1);
        when(scoreRepository.countBySubmission_IdAndScoreType(10, ScoreType.NORMAL)).thenReturn(0L);
        when(scoringCompletionHelper.isScoringIncomplete(submission, 5, round)).thenReturn(true);

        assertThatThrownBy(() -> guard.validateBeforeNext(submission, 5, round, false))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getCode())
                        .isEqualTo(ErrorCode.SCORING_INCOMPLETE_BEFORE_NEXT));
    }

    @Test
    void validateBeforeNext_requiresAcknowledgeWhenNotAllJudgesFullyScored() {
        when(scoringCompletionHelper.countAssignedJudges(5, round)).thenReturn(2);
        when(scoreRepository.countBySubmission_IdAndScoreType(10, ScoreType.NORMAL)).thenReturn(4L);
        when(scoringCompletionHelper.countDistinctJudgesWithAnyScore(10)).thenReturn(2);
        when(scoringCompletionHelper.countJudgesFullyScored(submission)).thenReturn(1);
        when(scoringCompletionHelper.isScoringIncomplete(submission, 5, round)).thenReturn(true);

        assertThatThrownBy(() -> guard.validateBeforeNext(submission, 5, round, false))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getCode())
                        .isEqualTo(ErrorCode.SCORING_INCOMPLETE_BEFORE_NEXT));

        guard.validateBeforeNext(submission, 5, round, true);
    }

    @Test
    void validateBeforeNext_passesWhenAllJudgesFullyScored() {
        when(scoringCompletionHelper.countAssignedJudges(5, round)).thenReturn(2);
        when(scoreRepository.countBySubmission_IdAndScoreType(10, ScoreType.NORMAL)).thenReturn(8L);
        when(scoringCompletionHelper.countDistinctJudgesWithAnyScore(10)).thenReturn(2);
        when(scoringCompletionHelper.countJudgesFullyScored(submission)).thenReturn(2);
        when(scoringCompletionHelper.isScoringIncomplete(submission, 5, round)).thenReturn(false);

        guard.validateBeforeNext(submission, 5, round, false);
    }

    @Test
    void snapshot_marksIncompleteWhenMissingFullScores() {
        when(scoringCompletionHelper.countAssignedJudges(5, round)).thenReturn(2);
        when(scoreRepository.countBySubmission_IdAndScoreType(10, ScoreType.NORMAL)).thenReturn(4L);
        when(scoringCompletionHelper.countDistinctJudgesWithAnyScore(10)).thenReturn(2);
        when(scoringCompletionHelper.countJudgesFullyScored(submission)).thenReturn(1);
        when(scoringCompletionHelper.isScoringIncomplete(submission, 5, round)).thenReturn(true);

        var snap = guard.snapshot(submission, 5, round);
        assertThat(snap.isIncomplete()).isTrue();
        assertThat(snap.getJudgesAssigned()).isEqualTo(2);
        assertThat(snap.getJudgesFullyScored()).isEqualTo(1);
    }

    @Test
    void validateBeforeNext_passesForSingleJudgeWithFullScores() {
        when(scoringCompletionHelper.countAssignedJudges(5, round)).thenReturn(1);
        when(scoreRepository.countBySubmission_IdAndScoreType(10, ScoreType.NORMAL)).thenReturn(4L);
        when(scoringCompletionHelper.countDistinctJudgesWithAnyScore(10)).thenReturn(1);
        when(scoringCompletionHelper.countJudgesFullyScored(submission)).thenReturn(1);
        when(scoringCompletionHelper.isScoringIncomplete(submission, 5, round)).thenReturn(false);

        guard.validateBeforeNext(submission, 5, round, false);
    }

    @Test
    void snapshot_completeWhenAllJudgesFullyScored() {
        when(scoringCompletionHelper.countAssignedJudges(5, round)).thenReturn(2);
        when(scoreRepository.countBySubmission_IdAndScoreType(10, ScoreType.NORMAL)).thenReturn(8L);
        when(scoringCompletionHelper.countDistinctJudgesWithAnyScore(10)).thenReturn(2);
        when(scoringCompletionHelper.countJudgesFullyScored(submission)).thenReturn(2);
        when(scoringCompletionHelper.isScoringIncomplete(submission, 5, round)).thenReturn(false);

        var snap = guard.snapshot(submission, 5, round);
        assertThat(snap.isIncomplete()).isFalse();
    }
}
