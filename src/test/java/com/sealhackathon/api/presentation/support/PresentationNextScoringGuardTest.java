package com.sealhackathon.api.presentation.support;

import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.judge_assignments.entity.JudgeAssignment;
import com.sealhackathon.api.judge_assignments.repository.JudgeAssignmentRepository;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.scores.entity.Score;
import com.sealhackathon.api.scores.repository.ScoreRepository;
import com.sealhackathon.api.scores.value_object.ScoreType;
import com.sealhackathon.api.submissions.entity.Submission;
import com.sealhackathon.api.users.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PresentationNextScoringGuardTest {

    @Mock private ScoreRepository scoreRepository;
    @Mock private JudgeAssignmentRepository judgeAssignmentRepository;

    @InjectMocks private PresentationNextScoringGuard guard;

    private final Round round = Round.builder().id(1).build();
    private final Submission submission = Submission.builder().id(10).build();

    @Test
    void validateBeforeNext_blocksWhenNoScores() {
        when(judgeAssignmentRepository.findByTrackId(5)).thenReturn(List.of(assignment(1)));
        when(scoreRepository.countBySubmission_IdAndScoreType(10, ScoreType.NORMAL)).thenReturn(0L);

        assertThatThrownBy(() -> guard.validateBeforeNext(submission, 5, round, false))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getCode())
                        .isEqualTo(ErrorCode.SCORING_INCOMPLETE_BEFORE_NEXT));
    }

    @Test
    void validateBeforeNext_requiresAcknowledgeWhenNotAllJudgesScored() {
        when(judgeAssignmentRepository.findByTrackId(5))
                .thenReturn(List.of(assignment(1), assignment(2)));
        when(scoreRepository.countBySubmission_IdAndScoreType(10, ScoreType.NORMAL)).thenReturn(2L);
        when(scoreRepository.findBySubmission_IdAndScoreType(10, ScoreType.NORMAL))
                .thenReturn(List.of(score(1)));

        assertThatThrownBy(() -> guard.validateBeforeNext(submission, 5, round, false))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getCode())
                        .isEqualTo(ErrorCode.SCORING_INCOMPLETE_BEFORE_NEXT));

        guard.validateBeforeNext(submission, 5, round, true);
    }

    @Test
    void validateBeforeNext_passesWhenSingleJudgeScored() {
        when(judgeAssignmentRepository.findByTrackId(5)).thenReturn(List.of(assignment(1)));
        when(scoreRepository.countBySubmission_IdAndScoreType(10, ScoreType.NORMAL)).thenReturn(1L);
        when(scoreRepository.findBySubmission_IdAndScoreType(10, ScoreType.NORMAL))
                .thenReturn(List.of(score(1)));

        guard.validateBeforeNext(submission, 5, round, false);
    }

    @Test
    void snapshot_marksIncompleteWhenMissingJudgeScores() {
        when(judgeAssignmentRepository.findByTrackId(5))
                .thenReturn(List.of(assignment(1), assignment(2)));
        when(scoreRepository.countBySubmission_IdAndScoreType(10, ScoreType.NORMAL)).thenReturn(1L);
        when(scoreRepository.findBySubmission_IdAndScoreType(10, ScoreType.NORMAL))
                .thenReturn(List.of(score(1)));

        var snap = guard.snapshot(submission, 5, round);
        assertThat(snap.isIncomplete()).isTrue();
        assertThat(snap.getJudgesAssigned()).isEqualTo(2);
        assertThat(snap.getJudgesScored()).isEqualTo(1);
    }

    private JudgeAssignment assignment(int judgeId) {
        return JudgeAssignment.builder()
                .judge(User.builder().id(judgeId).build())
                .build();
    }

    private Score score(int judgeId) {
        return Score.builder()
                .judge(User.builder().id(judgeId).build())
                .scoreType(ScoreType.NORMAL)
                .build();
    }
}
