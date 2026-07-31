package com.sealhackathon.api.rounds.query;

import com.sealhackathon.api.common.util.ScoreScale;
import com.sealhackathon.api.criteria.entity.Criteria;
import com.sealhackathon.api.criteria.repository.CriteriaRepository;
import com.sealhackathon.api.criteria.value_object.CriteriaType;
import com.sealhackathon.api.judge_assignments.repository.JudgeAssignmentRepository;
import com.sealhackathon.api.rounds.dto.response.RoundRankingItemResponse;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.scores.entity.Score;
import com.sealhackathon.api.scores.repository.ScoreRepository;
import com.sealhackathon.api.scores.value_object.ScoreType;
import com.sealhackathon.api.submissions.entity.Submission;
import com.sealhackathon.api.submissions.policy.SubmissionGradablePolicy;
import com.sealhackathon.api.submissions.repository.SubmissionRepository;
import com.sealhackathon.api.teams.entity.TeamRoundTrack;
import com.sealhackathon.api.teams.repository.TeamRoundTrackRepository;
import com.sealhackathon.api.teams.value_object.ParticipationStatus;
import com.sealhackathon.api.tiebreak_evaluations.entity.TiebreakEvaluation;
import com.sealhackathon.api.tiebreak_evaluations.repository.TiebreakEvaluationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/** FR-20 — xếp hạng có trọng số, BUG-4 COALESCE cho criterion chưa chấm. */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoundRankingQueryService {

    private final SubmissionRepository submissionRepository;
    private final ScoreRepository scoreRepository;
    private final CriteriaRepository criteriaRepository;
    private final TeamRoundTrackRepository teamRoundTrackRepository;
    private final TiebreakEvaluationRepository tiebreakEvaluationRepository;
    private final RoundRepository roundRepository;
    private final JudgeAssignmentRepository judgeAssignmentRepository;

    public List<RoundRankingItemResponse> rankingForRound(Integer roundId, boolean livePreview) {
        Round round = roundRepository.findById(roundId).orElse(null);
        boolean isFinalRound = round != null && Boolean.TRUE.equals(round.getIsFinal());

        List<Submission> submissions = mergeRoundSubmissions(roundId);
        if (submissions.isEmpty()) {
            return List.of();
        }

        Map<Integer, TeamRoundTrack> trackAssignmentByTeam = new HashMap<>();
        for (TeamRoundTrack trt : teamRoundTrackRepository.findByTrack_Round_Id(roundId)) {
            trackAssignmentByTeam.put(trt.getTeam().getId(), trt);
        }

        Map<Integer, Double> penaltyByTeam = tiebreakEvaluationRepository.findByRound_Id(roundId).stream()
                .collect(Collectors.groupingBy(
                        te -> te.getTeam().getId(),
                        Collectors.summingDouble(TiebreakEvaluation::getPenaltyScore)
                ));

        // Điểm công khai: không trừ micro-penalty tiebreak (chỉ dùng sắp xếp nội bộ).
        // FINISHED: giữ trừ để khớp điểm đã công bố.
        boolean preservePublishedPenaltyInDisplay = round != null
                && round.getHackathon() != null
                && round.getHackathon().getStatus()
                == com.sealhackathon.api.hackathons.value_object.HackathonStatus.FINISHED;

        List<RankRow> rows = new ArrayList<>();
        for (Submission submission : submissions) {
            if (!SubmissionGradablePolicy.isGradable(submission)) {
                continue;
            }

            boolean isFinalSubmission = isFinalRound
                    && submission.getTrack() == null
                    && submission.getRound() != null
                    && Objects.equals(submission.getRound().getId(), roundId);
            if (!isFinalSubmission && submission.getTrack() == null) {
                continue;
            }

            TeamRoundTrack trt = trackAssignmentByTeam.get(submission.getTeam().getId());
            String partStatus = "PARTICIPATING";
            if (trt != null && trt.getParticipationStatus() != null) {
                partStatus = trt.getParticipationStatus().name();
            } else if (isFinalSubmission) {
                partStatus = "ADVANCED";
            }

            List<Criteria> criteria;
            Integer trackId;
            if (isFinalSubmission) {
                trackId = null;
                criteria = criteriaRepository.findByFinalRoundIdOrderByDisplayOrderAsc(roundId).stream()
                        .filter(c -> c.getType() != CriteriaType.PENALTY)
                        .toList();
            } else {
                trackId = submission.getTrack().getId();
                criteria = criteriaRepository.findByTrackIdOrderByDisplayOrderAsc(trackId).stream()
                        .filter(c -> c.getType() != CriteriaType.PENALTY)
                        .toList();
            }
            if (criteria.isEmpty()) {
                continue;
            }

            double total = 0.0;
            for (Criteria criterion : criteria) {
                double avg = averageScore(submission.getId(), criterion.getId(), livePreview);
                total += avg * criterion.getWeight();
            }
            total = ScoreScale.round2(total);

            Criteria priorityCriterion = criteria.stream()
                    .filter(c -> Boolean.TRUE.equals(c.getIsTiebreakerPriority()))
                    .findFirst()
                    .orElse(null);
            double priorityScore = 0.0;
            String priorityCriterionName = null;
            if (priorityCriterion != null) {
                priorityScore = ScoreScale.round2(
                        averageScore(submission.getId(), priorityCriterion.getId(), livePreview));
                priorityCriterionName = priorityCriterion.getName();
            }

            double penalty = ScoreScale.round2(
                    penaltyByTeam.getOrDefault(submission.getTeam().getId(), 0.0));
            double displayTotal = ScoreScale.round2(
                    preservePublishedPenaltyInDisplay ? (total - penalty) : total);

            rows.add(new RankRow(
                    submission.getId(),
                    submission.getTeam().getId(),
                    submission.getTeam().getTeamName(),
                    trackId,
                    trt != null ? trt.getAssignedGroup() : null,
                    displayTotal,
                    partStatus,
                    submission.getSubmittedAt(),
                    submission.getStatus() != null ? submission.getStatus().name() : null,
                    penalty,
                    priorityScore,
                    priorityCriterionName));
        }

        List<RankRow> sortedRows = sortRankRows(rows, isFinalRound);
        return assignRanks(sortedRows, isFinalRound, preservePublishedPenaltyInDisplay);
    }

    static List<RankRow> sortRankRows(List<RankRow> rows, boolean isFinalRound) {
        List<RankRow> sorted = new ArrayList<>(rows);
        sorted.sort(rankComparator(isFinalRound));
        return sorted;
    }

    static List<RoundRankingItemResponse> assignRanks(List<RankRow> sortedRows, boolean isFinalRound) {
        return assignRanks(sortedRows, isFinalRound, false);
    }

    /**
     * @param displayNetsPenalty true khi totalScore đã trừ penalty (hackathon FINISHED) —
     *                           khi đó cờ tiebreakRequired gom theo totalScore; ngược lại gom theo điểm hiệu lực.
     */
    static List<RoundRankingItemResponse> assignRanks(
            List<RankRow> sortedRows, boolean isFinalRound, boolean displayNetsPenalty) {
        List<RoundRankingItemResponse> result = new ArrayList<>();
        if (isFinalRound) {
            Map<String, Long> keyCounts = sortedRows.stream()
                    .collect(Collectors.groupingBy(
                            row -> tieFlagKey(row, displayNetsPenalty), Collectors.counting()));
            int rank = 1;
            for (RankRow row : sortedRows) {
                boolean tie = keyCounts.getOrDefault(tieFlagKey(row, displayNetsPenalty), 0L) > 1;
                result.add(toRankingItem(row, rank++, tie));
            }
            annotateTiebreakReasonLabels(result, sortedRows);
            return result;
        }

        for (int i = 0; i < sortedRows.size(); ) {
            String group = sortedRows.get(i).assignedGroup() != null ? sortedRows.get(i).assignedGroup() : "";
            int j = i;
            while (j < sortedRows.size()) {
                String g = sortedRows.get(j).assignedGroup() != null ? sortedRows.get(j).assignedGroup() : "";
                if (!Objects.equals(g, group)) {
                    break;
                }
                j++;
            }
            List<RankRow> groupSlice = sortedRows.subList(i, j);
            Map<String, Long> keyCounts = groupSlice.stream()
                    .collect(Collectors.groupingBy(
                            row -> tieFlagKey(row, displayNetsPenalty), Collectors.counting()));
            int rankInGroup = 0;
            List<RoundRankingItemResponse> groupResult = new ArrayList<>();
            for (RankRow row : groupSlice) {
                rankInGroup++;
                boolean tie = keyCounts.getOrDefault(tieFlagKey(row, displayNetsPenalty), 0L) > 1;
                groupResult.add(toRankingItem(row, rankInGroup, tie));
            }
            annotateTiebreakReasonLabels(groupResult, groupSlice);
            result.addAll(groupResult);
            i = j;
        }
        return result;
    }

    /**
     * Khi hai đội điểm hiển thị bằng nhau nhưng hạng khác (waterfall/micro-penalty),
     * gắn nhãn ngắn để UI không bị hiểu là lỗi.
     */
    private static void annotateTiebreakReasonLabels(
            List<RoundRankingItemResponse> items, List<RankRow> rows) {
        if (items.size() != rows.size()) {
            return;
        }
        for (int i = 0; i < items.size(); i++) {
            RankRow row = rows.get(i);
            RoundRankingItemResponse item = items.get(i);
            if (row.penaltyScore() > 0) {
                item.setTiebreakReasonLabel("Phân định đồng điểm");
                continue;
            }
            // Winner among same display score as neighbor below
            if (i + 1 < items.size()) {
                RoundRankingItemResponse next = items.get(i + 1);
                RankRow nextRow = rows.get(i + 1);
                if (Objects.equals(item.getTotalScore(), next.getTotalScore())
                        && !Objects.equals(item.getRank(), next.getRank())) {
                    if (row.priorityScore() > nextRow.priorityScore()) {
                        item.setTiebreakReasonLabel("Thắng do tiêu chí phụ");
                    } else if (row.submittedAt() != null && nextRow.submittedAt() != null
                            && row.submittedAt().isBefore(nextRow.submittedAt())) {
                        item.setTiebreakReasonLabel("Thắng do nộp sớm hơn");
                    } else if (nextRow.penaltyScore() > 0) {
                        item.setTiebreakReasonLabel("Thắng phân định đồng điểm");
                    }
                }
            }
        }
    }

    /** Điểm hiệu lực cho cờ tiebreakRequired — khớp detector progression. */
    static double effectiveScoreForTieFlag(RankRow row, boolean displayNetsPenalty) {
        if (displayNetsPenalty) {
            return row.totalScore();
        }
        return row.totalScore() - row.penaltyScore();
    }

    /** Composite key: effectiveTotal + priorityScore + submittedAt — waterfall cannot separate. */
    static String tieFlagKey(RankRow row, boolean displayNetsPenalty) {
        return effectiveScoreForTieFlag(row, displayNetsPenalty)
                + "|" + row.priorityScore()
                + "|" + (row.submittedAt() != null ? row.submittedAt().toString() : "null");
    }

    private static Comparator<RankRow> rankComparator(boolean isFinalRound) {
        Comparator<RankRow> byScore = Comparator
                .comparing(RankRow::totalScore, Comparator.reverseOrder())
                .thenComparing(RankRow::priorityScore, Comparator.reverseOrder())
                .thenComparing(RankRow::submittedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(RankRow::penaltyScore, Comparator.nullsFirst(Double::compareTo))
                .thenComparing(RankRow::teamId);
        Comparator<RankRow> eliminatedLast = Comparator.comparing(RoundRankingQueryService::isEliminated);
        if (isFinalRound) {
            return eliminatedLast.thenComparing(byScore);
        }
        return Comparator
                .comparing(RankRow::assignedGroup, Comparator.nullsLast(String::compareTo))
                .thenComparing(eliminatedLast)
                .thenComparing(byScore);
    }

    private static boolean isEliminated(RankRow row) {
        return ParticipationStatus.ELIMINATED.name().equals(row.participationStatus());
    }

    private static RoundRankingItemResponse toRankingItem(RankRow row, int rank, boolean tiebreakRequired) {
        return RoundRankingItemResponse.builder()
                .rank(rank)
                .teamId(row.teamId())
                .teamName(row.teamName())
                .trackId(row.trackId())
                .assignedGroup(row.assignedGroup())
                .totalScore(ScoreScale.round2(row.totalScore()))
                .tiebreakRequired(tiebreakRequired)
                .participationStatus(row.participationStatus())
                .submittedAt(row.submittedAt())
                .submissionStatus(row.submissionStatus())
                .penaltyScore(ScoreScale.round2(row.penaltyScore()))
                .priorityCriterionScore(ScoreScale.round2(row.priorityScore()))
                .priorityCriterionName(row.priorityCriterionName())
                .submissionId(row.submissionId())
                .build();
    }

    public boolean hasIncompleteScoring(Integer roundId, boolean livePreview) {
        Round round = roundRepository.findById(roundId).orElse(null);
        boolean isFinalRound = round != null && Boolean.TRUE.equals(round.getIsFinal());

        for (Submission submission : mergeRoundSubmissions(roundId)) {
            if (!SubmissionGradablePolicy.isGradable(submission)) {
                continue;
            }
            boolean isFinalSubmission = isFinalRound
                    && submission.getTrack() == null
                    && submission.getRound() != null
                    && Objects.equals(submission.getRound().getId(), roundId);
            if (!isFinalSubmission && submission.getTrack() == null) {
                continue;
            }

            List<Criteria> criteria;
            if (isFinalSubmission) {
                criteria = criteriaRepository.findByFinalRoundIdOrderByDisplayOrderAsc(roundId).stream()
                        .filter(c -> c.getType() != CriteriaType.PENALTY)
                        .toList();
            } else {
                criteria = criteriaRepository.findByTrackIdOrderByDisplayOrderAsc(
                                submission.getTrack().getId()).stream()
                        .filter(c -> c.getType() != CriteriaType.PENALTY)
                        .toList();
            }
            for (Criteria criterion : criteria) {
                long requiredJudges = countAssignedJudges(submission, isFinalSubmission, roundId);
                long count = scoreRepository.countBySubmission_IdAndCriterion_IdAndScoreTypeAndIsFinal(
                        submission.getId(), criterion.getId(), ScoreType.NORMAL, !livePreview);
                if (count < requiredJudges) {
                    return true;
                }
            }
        }
        return false;
    }

    private long countAssignedJudges(Submission submission, boolean isFinalSubmission, Integer roundId) {
        if (isFinalSubmission) {
            return Math.max(1, judgeAssignmentRepository.findByRoundId(roundId).size());
        }
        if (submission.getTrack() != null) {
            return Math.max(1, judgeAssignmentRepository.findByTrackId(submission.getTrack().getId()).size());
        }
        return 1;
    }

    private double averageScore(Integer submissionId, Integer criterionId, boolean livePreview) {
        List<Score> scores = scoreRepository.findBySubmission_IdAndCriterion_IdAndScoreTypeAndIsFinal(
                submissionId, criterionId, ScoreType.NORMAL, !livePreview);
        if (scores.isEmpty()) {
            return 0.0;
        }
        return scores.stream()
                .mapToDouble(Score::getScoreValue)
                .average()
                .orElse(0.0);
    }

    private List<Submission> mergeRoundSubmissions(Integer roundId) {
        Map<Integer, Submission> byId = new HashMap<>();
        for (Submission s : submissionRepository.findByRound_Id(roundId)) {
            byId.put(s.getId(), s);
        }
        for (Submission s : submissionRepository.findByTrack_Round_Id(roundId)) {
            byId.putIfAbsent(s.getId(), s);
        }
        return new ArrayList<>(byId.values());
    }

}

record RankRow(
        Integer submissionId,
        Integer teamId,
        String teamName,
        Integer trackId,
        String assignedGroup,
        double totalScore,
        String participationStatus,
        LocalDateTime submittedAt,
        String submissionStatus,
        double penaltyScore,
        double priorityScore,
        String priorityCriterionName) {}
