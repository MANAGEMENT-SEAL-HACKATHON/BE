package com.sealhackathon.api.rounds.support;

import com.sealhackathon.api.submissions.value_object.SubmissionStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class TiebreakRuleOrderingTest {

    private static final LocalDateTime T1 = LocalDateTime.of(2026, 3, 1, 10, 0);
    private static final LocalDateTime T2 = LocalDateTime.of(2026, 3, 1, 11, 0);

    @Test
    void priorityCriterion_higherScoreWins() {
        List<TiebreakRuleOrdering.TiebreakCandidate> candidates = List.of(
                candidate(1, SubmissionStatus.SUBMITTED, T1, 7.0, "Creativity"),
                candidate(2, SubmissionStatus.SUBMITTED, T2, 8.5, "Creativity"));

        Optional<TiebreakRuleOrdering.WaterfallResult> result =
                TiebreakRuleOrdering.resolveWaterfall(candidates);

        assertThat(result).isPresent();
        assertThat(result.get().resolvedTier()).isEqualTo(TiebreakRuleOrdering.TIER_PRIORITY_CRITERION);
        assertThat(result.get().orderedTeamIds()).containsExactly(2, 1);
        assertThat(result.get().resolvedReasonLabel())
                .isEqualTo("Thắng do điểm tiêu chí phụ \"Creativity\" cao hơn");
        assertThat(TiebreakRuleOrdering.canFullySeparatePriority(candidates)).isTrue();
    }

    @Test
    void equalPriority_fallsThroughToSubmissionTime() {
        List<TiebreakRuleOrdering.TiebreakCandidate> candidates = List.of(
                candidate(1, SubmissionStatus.SUBMITTED, T2, 8.0, "Creativity"),
                candidate(2, SubmissionStatus.SUBMITTED, T1, 8.0, "Creativity"));

        Optional<TiebreakRuleOrdering.WaterfallResult> result =
                TiebreakRuleOrdering.resolveWaterfall(candidates);

        assertThat(result).isPresent();
        assertThat(result.get().resolvedTier()).isEqualTo(TiebreakRuleOrdering.TIER_SUBMISSION_TIME);
        assertThat(result.get().orderedTeamIds()).containsExactly(2, 1);
        assertThat(result.get().resolvedReasonLabel()).isEqualTo("Thắng do nộp sớm hơn");
        assertThat(TiebreakRuleOrdering.canFullySeparatePriority(candidates)).isFalse();
        assertThat(TiebreakRuleOrdering.canFullySeparateSubmissionTime(candidates)).isTrue();
    }

    @Test
    void nullPriority_skipsToSubmissionTime_onTimeBeatsLate() {
        List<TiebreakRuleOrdering.TiebreakCandidate> candidates = List.of(
                candidate(1, SubmissionStatus.LATE_APPROVED, T1, null, null),
                candidate(2, SubmissionStatus.SUBMITTED, T2, null, null));

        Optional<TiebreakRuleOrdering.WaterfallResult> result =
                TiebreakRuleOrdering.resolveWaterfall(candidates);

        assertThat(result).isPresent();
        assertThat(result.get().resolvedTier()).isEqualTo(TiebreakRuleOrdering.TIER_SUBMISSION_TIME);
        assertThat(result.get().orderedTeamIds()).containsExactly(2, 1);
    }

    @Test
    void identicalSubmissionKeys_returnsManual() {
        List<TiebreakRuleOrdering.TiebreakCandidate> candidates = List.of(
                candidate(1, SubmissionStatus.SUBMITTED, T1, 8.0, "Creativity"),
                candidate(2, SubmissionStatus.SUBMITTED, T1, 8.0, "Creativity"));

        Optional<TiebreakRuleOrdering.WaterfallResult> result =
                TiebreakRuleOrdering.resolveWaterfall(candidates);

        assertThat(result).isPresent();
        assertThat(result.get().resolvedTier()).isEqualTo(TiebreakRuleOrdering.TIER_MANUAL);
        assertThat(result.get().orderedTeamIds()).isNull();
        assertThat(result.get().resolvedReasonLabel()).isEqualTo("Cần Ban tổ chức phân xử");
        assertThat(TiebreakRuleOrdering.canFullySeparateSubmissionTime(candidates)).isFalse();
    }

    @Test
    void fewerThanTwoCandidates_returnsEmpty() {
        assertThat(TiebreakRuleOrdering.resolveWaterfall(List.of(
                candidate(1, SubmissionStatus.SUBMITTED, T1, 8.0, "X")))).isEmpty();
    }

    private static TiebreakRuleOrdering.TiebreakCandidate candidate(
            int teamId,
            SubmissionStatus status,
            LocalDateTime submittedAt,
            Double priorityScore,
            String priorityName) {
        return new TiebreakRuleOrdering.TiebreakCandidate(
                teamId, status, submittedAt, 0.0, 8.0, priorityScore, priorityName);
    }
}
