package com.sealhackathon.api.rounds.support;

import com.sealhackathon.api.submissions.value_object.SubmissionStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class TiebreakRuleOrdering {

    public static final String TIER_PRIORITY_CRITERION = "PRIORITY_CRITERION";
    public static final String TIER_SUBMISSION_TIME = "SUBMISSION_TIME";
    public static final String TIER_MANUAL = "MANUAL";

    private TiebreakRuleOrdering() {
    }

    public record TiebreakCandidate(
            Integer teamId,
            SubmissionStatus submissionStatus,
            LocalDateTime submittedAt,
            Double penaltyScore,
            Double totalScore,
            Double priorityCriterionScore,
            String priorityCriterionName) {

        public TiebreakCandidate(
                Integer teamId,
                SubmissionStatus submissionStatus,
                LocalDateTime submittedAt,
                Double penaltyScore,
                Double totalScore) {
            this(teamId, submissionStatus, submittedAt, penaltyScore, totalScore, null, null);
        }
    }

    public record WaterfallResult(
            List<Integer> orderedTeamIds,
            String resolvedTier,
            String resolvedReasonLabel) {
    }

    public static Optional<WaterfallResult> resolveWaterfall(List<TiebreakCandidate> candidates) {
        if (candidates == null || candidates.size() < 2) {
            return Optional.empty();
        }

        if (canFullySeparatePriority(candidates)) {
            List<Integer> ordered = sortTeamIds(candidates, priorityComparator());
            String name = firstPriorityCriterionName(candidates);
            String label = name != null
                    ? "Thắng do điểm tiêu chí phụ \"" + name + "\" cao hơn"
                    : "Thắng do điểm tiêu chí phụ cao hơn";
            return Optional.of(new WaterfallResult(ordered, TIER_PRIORITY_CRITERION, label));
        }

        if (canFullySeparateSubmissionTime(candidates)) {
            List<Integer> ordered = sortTeamIds(candidates, submissionTimeComparator());
            return Optional.of(new WaterfallResult(ordered, TIER_SUBMISSION_TIME, "Thắng do nộp sớm hơn"));
        }

        return Optional.of(new WaterfallResult(null, TIER_MANUAL, "Cần Ban tổ chức phân xử"));
    }

    public static boolean canFullySeparatePriority(List<TiebreakCandidate> candidates) {
        if (candidates == null || candidates.size() < 2) {
            return false;
        }
        if (shouldSkipPriorityTier(candidates)) {
            return false;
        }
        for (int i = 0; i < candidates.size(); i++) {
            for (int j = i + 1; j < candidates.size(); j++) {
                if (Objects.equals(
                        normalizePriority(candidates.get(i).priorityCriterionScore()),
                        normalizePriority(candidates.get(j).priorityCriterionScore()))) {
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean canFullySeparateSubmissionTime(List<TiebreakCandidate> candidates) {
        if (candidates == null || candidates.size() < 2) {
            return false;
        }
        for (int i = 0; i < candidates.size(); i++) {
            for (int j = i + 1; j < candidates.size(); j++) {
                TiebreakCandidate a = candidates.get(i);
                TiebreakCandidate b = candidates.get(j);
                if (isOnTime(a.submissionStatus()) == isOnTime(b.submissionStatus())
                        && Objects.equals(a.submittedAt(), b.submittedAt())) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean shouldSkipPriorityTier(List<TiebreakCandidate> candidates) {
        boolean allNull = candidates.stream().allMatch(c -> c.priorityCriterionScore() == null);
        if (allNull) {
            return true;
        }
        Double first = normalizePriority(candidates.get(0).priorityCriterionScore());
        return candidates.stream()
                .allMatch(c -> Objects.equals(first, normalizePriority(c.priorityCriterionScore())));
    }

    private static Comparator<TiebreakCandidate> priorityComparator() {
        return Comparator
                .comparing((TiebreakCandidate c) -> normalizePriority(c.priorityCriterionScore()),
                        Comparator.reverseOrder())
                .thenComparing(submissionTimeComparator());
    }

    private static Comparator<TiebreakCandidate> submissionTimeComparator() {
        return Comparator
                .comparing((TiebreakCandidate c) -> isOnTime(c.submissionStatus()) ? 0 : 1)
                .thenComparing(TiebreakCandidate::submittedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(TiebreakCandidate::teamId, Comparator.nullsLast(Comparator.naturalOrder()));
    }

    private static List<Integer> sortTeamIds(
            List<TiebreakCandidate> candidates, Comparator<TiebreakCandidate> comparator) {
        List<TiebreakCandidate> sorted = new ArrayList<>(candidates);
        sorted.sort(comparator);
        return sorted.stream().map(TiebreakCandidate::teamId).toList();
    }

    private static String firstPriorityCriterionName(List<TiebreakCandidate> candidates) {
        return candidates.stream()
                .map(TiebreakCandidate::priorityCriterionName)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    static boolean isOnTime(SubmissionStatus status) {
        return status == SubmissionStatus.SUBMITTED || status == SubmissionStatus.ACCEPTED;
    }

    private static double normalizePriority(Double score) {
        return score == null ? 0.0 : score;
    }
}
