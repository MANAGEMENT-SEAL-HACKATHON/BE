package com.sealhackathon.api.rounds.support;

import com.sealhackathon.api.rounds.value_object.TiebreakRule;
import com.sealhackathon.api.submissions.value_object.SubmissionStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class TiebreakRuleOrdering {

    private TiebreakRuleOrdering() {
    }

    public record TiebreakCandidate(
            Integer teamId,
            SubmissionStatus submissionStatus,
            LocalDateTime submittedAt,
            Double penaltyScore,
            Double totalScore) {
    }

    public static Optional<List<Integer>> orderByRule(TiebreakRule rule, List<TiebreakCandidate> candidates) {
        if (rule == TiebreakRule.COORDINATOR_DECISION || candidates == null || candidates.isEmpty()) {
            return Optional.empty();
        }
        if (!canFullySeparate(rule, candidates)) {
            return Optional.empty();
        }
        List<TiebreakCandidate> sorted = new ArrayList<>(candidates);
        sorted.sort(comparatorFor(rule));
        return Optional.of(sorted.stream().map(TiebreakCandidate::teamId).toList());
    }

    public static boolean canFullySeparate(TiebreakRule rule, List<TiebreakCandidate> candidates) {
        if (rule == TiebreakRule.COORDINATOR_DECISION || candidates == null || candidates.size() < 2) {
            return false;
        }
        for (int i = 0; i < candidates.size(); i++) {
            for (int j = i + 1; j < candidates.size(); j++) {
                if (sameSortKey(rule, candidates.get(i), candidates.get(j))) {
                    return false;
                }
            }
        }
        return true;
    }

    private static Comparator<TiebreakCandidate> comparatorFor(TiebreakRule rule) {
        return switch (rule) {
            case SUBMISSION_TIME -> Comparator
                    .comparing((TiebreakCandidate c) -> isOnTime(c.submissionStatus()) ? 0 : 1)
                    .thenComparing(TiebreakCandidate::submittedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(TiebreakCandidate::teamId, Comparator.nullsLast(Comparator.naturalOrder()));
            case PENALTY_SCORE -> Comparator
                    .comparing((TiebreakCandidate c) -> normalizePenalty(c.penaltyScore()))
                    .thenComparing(TiebreakCandidate::teamId, Comparator.nullsLast(Comparator.naturalOrder()));
            case COORDINATOR_DECISION -> Comparator.comparing(TiebreakCandidate::teamId);
        };
    }

    private static boolean sameSortKey(TiebreakRule rule, TiebreakCandidate a, TiebreakCandidate b) {
        return switch (rule) {
            case SUBMISSION_TIME -> isOnTime(a.submissionStatus()) == isOnTime(b.submissionStatus())
                    && Objects.equals(a.submittedAt(), b.submittedAt());
            case PENALTY_SCORE -> Objects.equals(normalizePenalty(a.penaltyScore()), normalizePenalty(b.penaltyScore()));
            case COORDINATOR_DECISION -> false;
        };
    }

    static boolean isOnTime(SubmissionStatus status) {
        return status == SubmissionStatus.SUBMITTED || status == SubmissionStatus.ACCEPTED;
    }

    private static Double normalizePenalty(Double penaltyScore) {
        return penaltyScore == null ? 0.0 : penaltyScore;
    }
}
