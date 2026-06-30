package com.sealhackathon.api.rounds.query;

import com.sealhackathon.api.criteria.entity.Criteria;
import com.sealhackathon.api.criteria.repository.CriteriaRepository;
import com.sealhackathon.api.criteria.value_object.CriteriaType;
import com.sealhackathon.api.rounds.dto.response.RoundScoringProgressResponse;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.scores.repository.ScoreRepository;
import com.sealhackathon.api.scores.value_object.ScoreType;
import com.sealhackathon.api.submissions.entity.Submission;
import com.sealhackathon.api.submissions.repository.SubmissionRepository;
import com.sealhackathon.api.submissions.value_object.SubmissionStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScoringProgressQueryServiceTest {

    @Mock private SubmissionRepository submissionRepository;
    @Mock private ScoreRepository scoreRepository;
    @Mock private CriteriaRepository criteriaRepository;

    @InjectMocks private ScoringProgressQueryService service;

    @Test
    void progressForFinalRound_countsSubmissionsWithoutTrack() {
        Round finalRound = Round.builder().id(99).isFinal(true).scoringLocked(false).build();
        Submission sub = Submission.builder()
                .id(10)
                .status(SubmissionStatus.SUBMITTED)
                .round(finalRound)
                .track(null)
                .build();
        Criteria criterion = Criteria.builder().id(501).type(CriteriaType.TECHNICAL).build();

        when(submissionRepository.findByRound_Id(99)).thenReturn(List.of(sub));
        when(submissionRepository.findByTrack_Round_Id(99)).thenReturn(List.of());
        when(criteriaRepository.findByFinalRoundIdOrderByDisplayOrderAsc(99)).thenReturn(List.of(criterion));
        when(scoreRepository.countBySubmission_IdAndCriterion_IdAndScoreTypeAndIsFinal(
                10, 501, ScoreType.NORMAL, false)).thenReturn(1L);

        RoundScoringProgressResponse progress = service.progressForRound(finalRound);

        assertThat(progress.getTotalSubmissions()).isEqualTo(1);
        assertThat(progress.getScoredSubmissions()).isEqualTo(1);
        assertThat(progress.getPendingSubmissions()).isEqualTo(0);
    }

    @Test
    void progressForPrelimRound_stillUsesTrackCriteria() {
        Round prelimRound = Round.builder().id(1).isFinal(false).scoringLocked(false).build();
        com.sealhackathon.api.tracks.entity.Track track =
                com.sealhackathon.api.tracks.entity.Track.builder().id(7).round(prelimRound).build();
        Submission sub = Submission.builder()
                .id(20)
                .status(SubmissionStatus.SUBMITTED)
                .round(prelimRound)
                .track(track)
                .build();
        Criteria criterion = Criteria.builder().id(101).type(CriteriaType.TECHNICAL).build();

        when(submissionRepository.findByRound_Id(1)).thenReturn(List.of());
        when(submissionRepository.findByTrack_Round_Id(1)).thenReturn(List.of(sub));
        when(criteriaRepository.findByTrackIdOrderByDisplayOrderAsc(7)).thenReturn(List.of(criterion));
        when(scoreRepository.countBySubmission_IdAndCriterion_IdAndScoreTypeAndIsFinal(
                eq(20), eq(101), eq(ScoreType.NORMAL), eq(false))).thenReturn(1L);

        RoundScoringProgressResponse progress = service.progressForRound(prelimRound);

        assertThat(progress.getScoredSubmissions()).isEqualTo(1);
    }
}
