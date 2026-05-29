package com.sealhackathon.api.rounds.query;

import com.sealhackathon.api.criteria.entity.Criteria;
import com.sealhackathon.api.criteria.repository.CriteriaRepository;
import com.sealhackathon.api.criteria.value_object.CriteriaType;
import com.sealhackathon.api.rounds.dto.response.RoundRankingItemResponse;
import com.sealhackathon.api.scores.entity.Score;
import com.sealhackathon.api.scores.repository.ScoreRepository;
import com.sealhackathon.api.scores.value_object.ScoreType;
import com.sealhackathon.api.submissions.entity.Submission;
import com.sealhackathon.api.submissions.policy.SubmissionGradablePolicy;
import com.sealhackathon.api.submissions.repository.SubmissionRepository;
import com.sealhackathon.api.team_round_tracks.entity.TeamRoundTrack;
import com.sealhackathon.api.team_round_tracks.repository.TeamRoundTrackRepository;
import com.sealhackathon.api.team_round_participation.value_object.ParticipationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** FR-20 — xếp hạng có trọng số, BUG-4 COALESCE cho criterion chưa chấm. */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoundRankingQueryService {

    private final SubmissionRepository submissionRepository;
    private final ScoreRepository scoreRepository;
    private final CriteriaRepository criteriaRepository;
    private final TeamRoundTrackRepository teamRoundTrackRepository;

    public List<RoundRankingItemResponse> rankingForRound(Integer roundId, boolean livePreview) {
        List<Submission> submissions = mergeRoundSubmissions(roundId);
        if (submissions.isEmpty()) {
            return List.of();
        }

        Map<Integer, TeamRoundTrack> trackAssignmentByTeam = new HashMap<>();
        for (TeamRoundTrack trt : teamRoundTrackRepository.findByTrack_Round_Id(roundId)) {
            trackAssignmentByTeam.put(trt.getTeam().getId(), trt);
        }

        List<RankRow> rows = new ArrayList<>();
        for (Submission submission : submissions) {
            if (!SubmissionGradablePolicy.isGradable(submission)) {
                continue;
            }
            if (submission.getTrack() == null) {
                continue;
            }
            TeamRoundTrack trt = trackAssignmentByTeam.get(submission.getTeam().getId());
            if (trt != null && trt.getParticipationStatus() == ParticipationStatus.ELIMINATED) {
                continue;
            }

            Integer trackId = submission.getTrack().getId();
            List<Criteria> criteria = criteriaRepository.findByTrackIdOrderByDisplayOrderAsc(trackId).stream()
                    .filter(c -> c.getType() != CriteriaType.PENALTY)
                    .toList();
            if (criteria.isEmpty()) {
                continue;
            }

            double total = 0.0;
            for (Criteria criterion : criteria) {
                double avg = averageScore(submission.getId(), criterion.getId(), livePreview);
                total += avg * criterion.getWeight();
            }

            rows.add(new RankRow(
                    submission.getTeam().getId(),
                    submission.getTeam().getTeamName(),
                    trackId,
                    trt != null ? trt.getAssignedGroup() : null,
                    total
            ));
        }

        rows.sort(Comparator
                .comparing(RankRow::assignedGroup, Comparator.nullsLast(String::compareTo))
                .thenComparing(RankRow::totalScore, Comparator.reverseOrder())
                .thenComparing(RankRow::teamId));

        List<RoundRankingItemResponse> result = new ArrayList<>();
        String currentGroup = null;
        int rankInGroup = 0;
        for (RankRow row : rows) {
            String group = row.assignedGroup() != null ? row.assignedGroup() : "";
            if (!Objects.equals(group, currentGroup)) {
                currentGroup = group;
                rankInGroup = 0;
            }
            rankInGroup++;
            result.add(RoundRankingItemResponse.builder()
                    .rank(rankInGroup)
                    .teamId(row.teamId())
                    .teamName(row.teamName())
                    .trackId(row.trackId())
                    .assignedGroup(row.assignedGroup())
                    .totalScore(row.totalScore())
                    .tiebreakRequired(false)
                    .build());
        }
        return result;
    }

    public boolean hasIncompleteScoring(Integer roundId, boolean livePreview) {
        for (Submission submission : mergeRoundSubmissions(roundId)) {
            if (!SubmissionGradablePolicy.isGradable(submission) || submission.getTrack() == null) {
                continue;
            }
            List<Criteria> criteria = criteriaRepository.findByTrackIdOrderByDisplayOrderAsc(
                    submission.getTrack().getId()).stream()
                    .filter(c -> c.getType() != CriteriaType.PENALTY)
                    .toList();
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

    private record RankRow(Integer teamId, String teamName, Integer trackId, String assignedGroup, double totalScore) {}
}
