package com.sealhackathon.api.rounds.query;

import com.sealhackathon.api.criteria.entity.Criteria;
import com.sealhackathon.api.criteria.repository.CriteriaRepository;
import com.sealhackathon.api.criteria.value_object.CriteriaType;
import com.sealhackathon.api.judge_assignments.entity.JudgeAssignment;
import com.sealhackathon.api.judge_assignments.repository.JudgeAssignmentRepository;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.scores.repository.ScoreRepository;
import com.sealhackathon.api.scores.value_object.ScoreType;
import com.sealhackathon.api.submissions.entity.Submission;
import com.sealhackathon.api.submissions.repository.SubmissionRepository;
import com.sealhackathon.api.submissions.value_object.SubmissionStatus;
import com.sealhackathon.api.teams.entity.Team;
import com.sealhackathon.api.teams.repository.TeamRoundTrackRepository;
import com.sealhackathon.api.tiebreak_evaluations.repository.TiebreakEvaluationRepository;
import com.sealhackathon.api.tracks.entity.Track;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MultiJudgeScoringCompletenessTest {

    @Mock private SubmissionRepository submissionRepository;
    @Mock private ScoreRepository scoreRepository;
    @Mock private CriteriaRepository criteriaRepository;
    @Mock private TeamRoundTrackRepository teamRoundTrackRepository;
    @Mock private TiebreakEvaluationRepository tiebreakEvaluationRepository;
    @Mock private RoundRepository roundRepository;
    @Mock private JudgeAssignmentRepository judgeAssignmentRepository;

    @InjectMocks private RoundRankingQueryService rankingQueryService;
    @InjectMocks private ScoringProgressQueryService progressQueryService;

    @Test
    void hasIncompleteScoring_singleJudgeFullyScored_returnsFalse() {
        Round round = Round.builder().id(1).isFinal(false).scoringLocked(false).build();
        Track track = Track.builder().id(7).round(round).build();
        Submission submission = submission(10, track, round);
        Criteria c1 = Criteria.builder().id(101).type(CriteriaType.TECHNICAL).build();

        when(roundRepository.findById(1)).thenReturn(Optional.of(round));
        when(submissionRepository.findByRound_Id(1)).thenReturn(List.of());
        when(submissionRepository.findByTrack_Round_Id(1)).thenReturn(List.of(submission));
        when(criteriaRepository.findByTrackIdOrderByDisplayOrderAsc(7)).thenReturn(List.of(c1));
        when(judgeAssignmentRepository.findByTrackId(7)).thenReturn(List.of(JudgeAssignment.builder().build()));
        when(scoreRepository.countBySubmission_IdAndCriterion_IdAndScoreTypeAndIsFinal(
                10, 101, ScoreType.NORMAL, false)).thenReturn(1L);

        assertThat(rankingQueryService.hasIncompleteScoring(1, true)).isFalse();
    }

    @Test
    void hasIncompleteScoring_twoJudges_oneScore_returnsTrue() {
        Round round = Round.builder().id(1).isFinal(false).scoringLocked(false).build();
        Track track = Track.builder().id(7).round(round).build();
        Submission submission = submission(10, track, round);
        Criteria c1 = Criteria.builder().id(101).type(CriteriaType.TECHNICAL).build();

        when(roundRepository.findById(1)).thenReturn(Optional.of(round));
        when(submissionRepository.findByRound_Id(1)).thenReturn(List.of());
        when(submissionRepository.findByTrack_Round_Id(1)).thenReturn(List.of(submission));
        when(criteriaRepository.findByTrackIdOrderByDisplayOrderAsc(7)).thenReturn(List.of(c1));
        when(judgeAssignmentRepository.findByTrackId(7)).thenReturn(List.of(
                JudgeAssignment.builder().build(), JudgeAssignment.builder().build()));
        when(scoreRepository.countBySubmission_IdAndCriterion_IdAndScoreTypeAndIsFinal(
                10, 101, ScoreType.NORMAL, false)).thenReturn(1L);

        assertThat(rankingQueryService.hasIncompleteScoring(1, true)).isTrue();
    }

    @Test
    void hasIncompleteScoring_twoJudges_bothScored_returnsFalse() {
        Round round = Round.builder().id(1).isFinal(false).scoringLocked(false).build();
        Track track = Track.builder().id(7).round(round).build();
        Submission submission = submission(10, track, round);
        Criteria c1 = Criteria.builder().id(101).type(CriteriaType.TECHNICAL).build();

        when(roundRepository.findById(1)).thenReturn(Optional.of(round));
        when(submissionRepository.findByRound_Id(1)).thenReturn(List.of());
        when(submissionRepository.findByTrack_Round_Id(1)).thenReturn(List.of(submission));
        when(criteriaRepository.findByTrackIdOrderByDisplayOrderAsc(7)).thenReturn(List.of(c1));
        when(judgeAssignmentRepository.findByTrackId(7)).thenReturn(List.of(
                JudgeAssignment.builder().build(), JudgeAssignment.builder().build()));
        when(scoreRepository.countBySubmission_IdAndCriterion_IdAndScoreTypeAndIsFinal(
                10, 101, ScoreType.NORMAL, false)).thenReturn(2L);

        assertThat(rankingQueryService.hasIncompleteScoring(1, true)).isFalse();
    }

    @Test
    void progress_twoJudges_partial_notFullyScored() {
        Round round = Round.builder().id(1).isFinal(false).scoringLocked(false).build();
        Track track = Track.builder().id(7).round(round).build();
        Submission submission = submission(10, track, round);
        Criteria c1 = Criteria.builder().id(101).type(CriteriaType.TECHNICAL).build();

        when(submissionRepository.findByRound_Id(1)).thenReturn(List.of());
        when(submissionRepository.findByTrack_Round_Id(1)).thenReturn(List.of(submission));
        when(criteriaRepository.findByTrackIdOrderByDisplayOrderAsc(7)).thenReturn(List.of(c1));
        when(judgeAssignmentRepository.findByTrackId(7)).thenReturn(List.of(
                JudgeAssignment.builder().build(), JudgeAssignment.builder().build()));
        when(scoreRepository.countBySubmission_IdAndCriterion_IdAndScoreTypeAndIsFinal(
                10, 101, ScoreType.NORMAL, false)).thenReturn(1L);

        var progress = progressQueryService.progressForRound(round);
        assertThat(progress.getScoredSubmissions()).isZero();
        assertThat(progress.getPendingSubmissions()).isEqualTo(1);
    }

    private static Submission submission(int id, Track track, Round round) {
        return Submission.builder()
                .id(id)
                .track(track)
                .round(round)
                .team(Team.builder().id(1).teamName("T").build())
                .status(SubmissionStatus.SUBMITTED)
                .build();
    }
}
