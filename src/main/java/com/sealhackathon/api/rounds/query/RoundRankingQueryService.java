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
import com.sealhackathon.api.team_round_tracks.entity.TeamRoundTrack;
import com.sealhackathon.api.team_round_tracks.repository.TeamRoundTrackRepository;
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

        if (isFinalRound) {
            rows.sort(Comparator
                    .comparing(RankRow::totalScore, Comparator.reverseOrder())
                    .thenComparing(RankRow::teamId));
        } else {
            rows.sort(Comparator
                    .comparing(RankRow::assignedGroup, Comparator.nullsLast(String::compareTo))
                    .thenComparing(RankRow::totalScore, Comparator.reverseOrder())
                    .thenComparing(RankRow::teamId));
        }

        List<RoundRankingItemResponse> result = new ArrayList<>();
        if (isFinalRound) {
            int rank = 1;
            for (RankRow row : rows) {
                result.add(toRankingItem(row, rank++));
            }
            return result;
        }

        String currentGroup = null;
        int rankInGroup = 0;
        for (RankRow row : rows) {
            String group = row.assignedGroup() != null ? row.assignedGroup() : "";
            if (!Objects.equals(group, currentGroup)) {
                currentGroup = group;
                rankInGroup = 0;
            }
            rankInGroup++;
            result.add(toRankingItem(row, rankInGroup));
        }
        return result;
    }

    private static RoundRankingItemResponse toRankingItem(RankRow row, int rank) {
        return RoundRankingItemResponse.builder()
                .rank(rank)
                .teamId(row.teamId())
                .teamName(row.teamName())
                .trackId(row.trackId())
                .assignedGroup(row.assignedGroup())
                .totalScore(row.totalScore())
                .tiebreakRequired(false)
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
    private record RankRow(Integer teamId, String teamName, Integer trackId, String assignedGroup, double totalScore, String participationStatus) {}
}