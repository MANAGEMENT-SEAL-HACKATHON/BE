package com.sealhackathon.api.rounds.support;

import com.sealhackathon.api.rounds.value_object.TiebreakRule;
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
    void tc1_bothOnTimeEarlierSubmittedAtWins() {
        List<TiebreakRuleOrdering.TiebreakCandidate> candidates = List.of(
                candidate(1, SubmissionStatus.SUBMITTED, T2, 0.0, 8.0),
                candidate(2, SubmissionStatus.SUBMITTED, T1, 0.0, 8.0));

        Optional<List<Integer>> order =
                TiebreakRuleOrdering.orderByRule(TiebreakRule.SUBMISSION_TIME, candidates);

        assertThat(order).contains(List.of(2, 1));
        assertThat(TiebreakRuleOrdering.canFullySeparate(TiebreakRule.SUBMISSION_TIME, candidates)).isTrue();
    }

    @Test
    void tc2_onTimeSubmittedBeatsLateApprovedEvenIfLater() {
        List<TiebreakRuleOrdering.TiebreakCandidate> candidates = List.of(
                candidate(1, SubmissionStatus.LATE_APPROVED, T1, 0.0, 8.0),
                candidate(2, SubmissionStatus.SUBMITTED, T2, 0.0, 8.0));

        Optional<List<Integer>> order =
                TiebreakRuleOrdering.orderByRule(TiebreakRule.SUBMISSION_TIME, candidates);

        assertThat(order).contains(List.of(2, 1));
    }

    @Test
    void tc3_lowerPenaltyScoreWins() {
        List<TiebreakRuleOrdering.TiebreakCandidate> candidates = List.of(
                candidate(1, SubmissionStatus.SUBMITTED, T1, 0.5, 8.0),
                candidate(2, SubmissionStatus.SUBMITTED, T2, 0.1, 8.0));

        Optional<List<Integer>> order =
                TiebreakRuleOrdering.orderByRule(TiebreakRule.PENALTY_SCORE, candidates);

        assertThat(order).contains(List.of(2, 1));
    }

    @Test
    void tc6c_identicalKeysCannotSeparate() {
        List<TiebreakRuleOrdering.TiebreakCandidate> candidates = List.of(
                candidate(1, SubmissionStatus.SUBMITTED, T1, 0.0, 8.0),
                candidate(2, SubmissionStatus.SUBMITTED, T1, 0.0, 8.0));

        assertThat(TiebreakRuleOrdering.orderByRule(TiebreakRule.SUBMISSION_TIME, candidates)).isEmpty();
        assertThat(TiebreakRuleOrdering.canFullySeparate(TiebreakRule.SUBMISSION_TIME, candidates)).isFalse();

        List<TiebreakRuleOrdering.TiebreakCandidate> penaltyTie = List.of(
                candidate(1, SubmissionStatus.SUBMITTED, T1, 0.2, 8.0),
                candidate(2, SubmissionStatus.SUBMITTED, T2, 0.2, 8.0));

        assertThat(TiebreakRuleOrdering.orderByRule(TiebreakRule.PENALTY_SCORE, penaltyTie)).isEmpty();
        assertThat(TiebreakRuleOrdering.canFullySeparate(TiebreakRule.PENALTY_SCORE, penaltyTie)).isFalse();
    }

    @Test
    void coordinatorDecisionReturnsEmpty() {
        List<TiebreakRuleOrdering.TiebreakCandidate> candidates = List.of(
                candidate(1, SubmissionStatus.SUBMITTED, T1, 0.0, 8.0),
                candidate(2, SubmissionStatus.SUBMITTED, T2, 0.0, 8.0));

        assertThat(TiebreakRuleOrdering.orderByRule(TiebreakRule.COORDINATOR_DECISION, candidates)).isEmpty();
        assertThat(TiebreakRuleOrdering.canFullySeparate(TiebreakRule.COORDINATOR_DECISION, candidates)).isFalse();
    }

    private static TiebreakRuleOrdering.TiebreakCandidate candidate(
            int teamId,
            SubmissionStatus status,
            LocalDateTime submittedAt,
            double penalty,
            double totalScore) {
        return new TiebreakRuleOrdering.TiebreakCandidate(teamId, status, submittedAt, penalty, totalScore);
    }
}
