package com.sealhackathon.api.rounds.query;

import com.sealhackathon.api.criteria.entity.Criteria;
import com.sealhackathon.api.criteria.repository.CriteriaRepository;
import com.sealhackathon.api.criteria.value_object.CriteriaType;
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

            double penalty = penaltyByTeam.getOrDefault(submission.getTeam().getId(), 0.0);
            total -= penalty;

            rows.add(new RankRow(
                    submission.getTeam().getId(),
                    submission.getTeam().getTeamName(),
                    trackId,
                    trt != null ? trt.getAssignedGroup() : null,
                    total,
                    partStatus));
        }

        List<RankRow> sortedRows = sortRankRows(rows, isFinalRound);
        return assignRanks(sortedRows, isFinalRound);
    }

    static List<RankRow> sortRankRows(List<RankRow> rows, boolean isFinalRound) {
        List<RankRow> sorted = new ArrayList<>(rows);
        sorted.sort(rankComparator(isFinalRound));
        return sorted;
    }

    static List<RoundRankingItemResponse> assignRanks(List<RankRow> sortedRows, boolean isFinalRound) {
        List<RoundRankingItemResponse> result = new ArrayList<>();
        if (isFinalRound) {
            Map<Double, Long> scoreCounts = sortedRows.stream()
                    .collect(Collectors.groupingBy(RankRow::totalScore, Collectors.counting()));
            int rank = 1;
            for (RankRow row : sortedRows) {
                boolean tie = scoreCounts.getOrDefault(row.totalScore(), 0L) > 1;
                result.add(toRankingItem(row, rank++, tie));
            }
            return result;
        }

        String currentGroup = null;
        int rankInGroup = 0;
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
            Map<Double, Long> scoreCounts = groupSlice.stream()
                    .collect(Collectors.groupingBy(RankRow::totalScore, Collectors.counting()));
            rankInGroup = 0;
            for (RankRow row : groupSlice) {
                rankInGroup++;
                boolean tie = scoreCounts.getOrDefault(row.totalScore(), 0L) > 1;
                result.add(toRankingItem(row, rankInGroup, tie));
            }
            i = j;
        }
        return result;
    }

    private static Comparator<RankRow> rankComparator(boolean isFinalRound) {
        Comparator<RankRow> byScore = Comparator
                .comparing(RankRow::totalScore, Comparator.reverseOrder())
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
                .totalScore(row.totalScore())
                .tiebreakRequired(tiebreakRequired)
                .participationStatus(row.participationStatus())
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
                long count = scoreRepository.countBySubmission_IdAndCriterion_IdAndScoreTypeAndIsFinal(
                        submission.getId(), criterion.getId(), ScoreType.NORMAL, !livePreview);
                if (count == 0) {
                    return true;
                }
            }
        }
        return false;
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

    // Cập nhật Record để chứa thêm thông tin participationStatus
}

record RankRow(Integer teamId, String teamName, Integer trackId, String assignedGroup, double totalScore, String participationStatus) {}