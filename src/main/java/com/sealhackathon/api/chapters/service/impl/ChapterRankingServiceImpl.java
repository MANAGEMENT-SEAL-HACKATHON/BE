package com.sealhackathon.api.chapters.service.impl;

import com.sealhackathon.api.chapters.dto.response.ChapterRankingItemResponse;
import com.sealhackathon.api.chapters.entity.ChapterRanking;
import com.sealhackathon.api.chapters.repository.ChapterRankingRepository;
import com.sealhackathon.api.chapters.service.ChapterRankingService;
import com.sealhackathon.api.chapters.entity.Chapter;
import com.sealhackathon.api.chapters.repository.ChapterRepository;
import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.common.exception.ResourceNotFoundException;
import com.sealhackathon.api.hackathons.dto.response.FinalTeamRankingItemResponse;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.query.FinalRankingQueryService;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.prizes.entity.Prize;
import com.sealhackathon.api.prizes.repository.PrizeRepository;
import com.sealhackathon.api.teams.entity.Team;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ChapterRankingServiceImpl implements ChapterRankingService {

    private static final String DEFAULT_FORMULA = "{\"mode\":\"SUM_TEAM_FINAL_SCORES\",\"version\":1}";

    private final ChapterRankingRepository chapterRankingRepository;
    private final HackathonRepository hackathonRepository;
    private final ChapterRepository chapterRepository;
    private final FinalRankingQueryService finalRankingQueryService;
    private final PrizeRepository prizeRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ChapterRankingItemResponse> listByHackathon(Integer hackathonId) {
        Hackathon hackathon = hackathonRepository.findById(hackathonId)
                .orElseThrow(() -> new ResourceNotFoundException("Hackathon", hackathonId));

        if (hackathon.getStatus() != HackathonStatus.FINISHED
                && hackathon.getStatus() != HackathonStatus.PENDING_CONFIRM) {
            throw new BusinessRuleException(ErrorCode.INVALID_STATE,
                    "Bảng xếp hạng Cơ sở (Chapter) chỉ được công bố khi Hackathon PENDING_CONFIRM hoặc FINISHED.");
        }

        return chapterRankingRepository.findByHackathon_IdOrderByRankAsc(hackathonId)
                .stream()
                .map(cr -> ChapterRankingItemResponse.builder()
                        .chapterId(cr.getChapter().getId())
                        .chapterName(cr.getChapter().getName())
                        .bestTeamScore(cr.getBestTeamScore())
                        .totalScore(cr.getTotalScore())
                        .rank(cr.getRank())
                        .teamsParticipated(cr.getTeamsParticipated())
                        .prizesWon(cr.getPrizesWon())
                        .build())
                .toList();
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void calculateAsync(Integer hackathonId) {
        Hackathon hackathon = hackathonRepository.findById(hackathonId)
                .orElseThrow(() -> new ResourceNotFoundException("Hackathon", hackathonId));

        chapterRankingRepository.deleteByHackathon_Id(hackathonId);

        List<FinalTeamRankingItemResponse> teamRankings =
                finalRankingQueryService.teamRankingsForHackathon(hackathonId);
        if (teamRankings.isEmpty()) {
            log.info("[ChapterRanking] hackathonId={} — không có team ranking CK, bỏ qua", hackathonId);
            return;
        }

        Map<Integer, ChapterAgg> byChapter = new HashMap<>();
        for (FinalTeamRankingItemResponse item : teamRankings) {
            if (item.getChapterId() == null) {
                continue;
            }
            float score = item.getWeightedAvgScore() != null ? item.getWeightedAvgScore().floatValue() : 0f;
            ChapterAgg agg = byChapter.computeIfAbsent(item.getChapterId(), id -> new ChapterAgg(
                    item.getChapterId(), item.getChapterName()));
            agg.totalScore += score;
            agg.bestTeamScore = Math.max(agg.bestTeamScore, score);
            agg.teamsParticipated += 1;
        }

        List<Prize> prizes = prizeRepository.findByRound_Hackathon_IdOrderByAwardedAtDesc(hackathonId);
        for (Prize prize : prizes) {
            Team team = prize.getTeam();
            if (team != null && team.getChapter() != null) {
                ChapterAgg agg = byChapter.get(team.getChapter().getId());
                if (agg != null) {
                    agg.prizesWon += 1;
                }
            }
        }

        String formulaSnapshot = hackathon.getChapterScoringFormula() != null
                ? hackathon.getChapterScoringFormula()
                : DEFAULT_FORMULA;
        LocalDateTime now = LocalDateTime.now();

        List<ChapterAgg> sorted = byChapter.values().stream()
                .sorted(Comparator
                        .comparing((ChapterAgg a) -> a.totalScore, Comparator.reverseOrder())
                        .thenComparing((ChapterAgg a) -> a.bestTeamScore, Comparator.reverseOrder())
                        .thenComparing(a -> a.chapterId))
                .toList();

        List<ChapterRanking> rows = new ArrayList<>();
        int rank = 1;
        for (ChapterAgg agg : sorted) {
            Chapter chapter = chapterRepository.findById(agg.chapterId)
                    .orElseThrow(() -> new ResourceNotFoundException("Chapter", agg.chapterId));
            rows.add(ChapterRanking.builder()
                    .hackathon(hackathon)
                    .chapter(chapter)
                    .bestTeamScore(agg.bestTeamScore)
                    .totalScore(agg.totalScore)
                    .rank(rank++)
                    .teamsParticipated(agg.teamsParticipated)
                    .prizesWon(agg.prizesWon)
                    .formulaSnapshot(formulaSnapshot)
                    .calculatedAt(now)
                    .build());
        }
        chapterRankingRepository.saveAll(rows);
        log.info("[ChapterRanking] hackathonId={} — persisted {} chapter rows", hackathonId, rows.size());
    }

    private static final class ChapterAgg {
        private final Integer chapterId;
        private final String chapterName;
        private float bestTeamScore;
        private float totalScore;
        private int teamsParticipated;
        private int prizesWon;

        private ChapterAgg(Integer chapterId, String chapterName) {
            this.chapterId = chapterId;
            this.chapterName = chapterName;
        }
    }
}
