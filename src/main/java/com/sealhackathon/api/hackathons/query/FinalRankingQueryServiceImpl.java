package com.sealhackathon.api.hackathons.query;

import com.sealhackathon.api.hackathons.dto.response.FinalTeamRankingItemResponse;
import com.sealhackathon.api.rounds.dto.response.RoundRankingItemResponse;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.query.RoundRankingQueryService;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.scores.repository.ScoreRepository;
import com.sealhackathon.api.scores.value_object.ScoreType;
import com.sealhackathon.api.submissions.repository.SubmissionRepository;
import com.sealhackathon.api.teams.entity.Team;
import com.sealhackathon.api.teams.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FinalRankingQueryServiceImpl implements FinalRankingQueryService {

    private final RoundRepository roundRepository;
    private final RoundRankingQueryService roundRankingQueryService;
    private final TeamRepository teamRepository;
    private final SubmissionRepository submissionRepository;
    private final ScoreRepository scoreRepository;

    @Override
    public List<FinalTeamRankingItemResponse> teamRankingsForHackathon(Integer hackathonId) {
        Round finalRound = roundRepository.findByHackathon_IdAndIsFinalTrue(hackathonId).orElse(null);
        if (finalRound == null) {
            return List.of();
        }

        List<RoundRankingItemResponse> rankings = roundRankingQueryService.rankingForRound(
                finalRound.getId(), false);
        if (rankings.isEmpty()) {
            return List.of();
        }

        List<RoundRankingItemResponse> sorted = rankings.stream()
                .sorted(Comparator
                        .comparing(RoundRankingItemResponse::getTotalScore, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(RoundRankingItemResponse::getTeamId))
                .toList();

        List<FinalTeamRankingItemResponse> result = new ArrayList<>();
        int rank = 1;
        for (RoundRankingItemResponse item : sorted) {
            Optional<Team> teamOpt = teamRepository.findById(item.getTeamId());
            Integer chapterId = null;
            String chapterName = null;
            if (teamOpt.isPresent() && teamOpt.get().getChapter() != null) {
                chapterId = teamOpt.get().getChapter().getId();
                chapterName = teamOpt.get().getChapter().getName();
            }

            int judgeCount = countJudgesForTeamInRound(item.getTeamId(), finalRound.getId());

            result.add(FinalTeamRankingItemResponse.builder()
                    .rank(rank++)
                    .teamId(item.getTeamId())
                    .teamName(item.getTeamName())
                    .chapterId(chapterId)
                    .chapterName(chapterName)
                    .weightedAvgScore(item.getTotalScore())
                    .judgeCount(judgeCount)
                    .build());
        }
        return result;
    }

    private int countJudgesForTeamInRound(Integer teamId, Integer roundId) {
        return submissionRepository.findByTeam_IdAndRound_Id(teamId, roundId).stream()
                .findFirst()
                .map(sub -> (int) scoreRepository.findBySubmission_Id(sub.getId()).stream()
                        .filter(s -> s.getScoreType() == ScoreType.NORMAL)
                        .map(s -> s.getJudge().getId())
                        .distinct()
                        .count())
                .orElse(0);
    }
}
