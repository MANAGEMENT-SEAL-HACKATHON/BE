package com.sealhackathon.api.export_jobs.support;



import com.sealhackathon.api.export_jobs.value_object.ExportJobType;

import com.sealhackathon.api.hackathons.dto.response.FinalTeamRankingItemResponse;

import com.sealhackathon.api.hackathons.entity.Hackathon;

import com.sealhackathon.api.hackathons.query.FinalRankingQueryService;

import com.sealhackathon.api.rbl.dto.response.RblVarianceItemResponse;

import com.sealhackathon.api.rbl.service.RblDashboardService;

import com.sealhackathon.api.rounds.entity.Round;

import com.sealhackathon.api.rounds.repository.RoundRepository;

import com.sealhackathon.api.scores.entity.Score;

import com.sealhackathon.api.scores.repository.ScoreRepository;

import com.sealhackathon.api.scores.value_object.ScoreType;

import com.sealhackathon.api.teams.entity.Team;

import com.sealhackathon.api.teams.repository.TeamRepository;

import com.sealhackathon.api.tracks.repository.TrackRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;



import java.nio.charset.StandardCharsets;

import java.time.LocalDateTime;

import java.util.LinkedHashMap;

import java.util.List;

import java.util.Map;

import java.util.Objects;



@Component

@RequiredArgsConstructor

public class ExportCsvBuilder {



    private final TeamRepository teamRepository;

    private final RoundRepository roundRepository;

    private final TrackRepository trackRepository;

    private final ScoreRepository scoreRepository;

    private final FinalRankingQueryService finalRankingQueryService;

    private final RblDashboardService rblDashboardService;



    public byte[] build(Hackathon hackathon, ExportJobType type) {

        return switch (type) {

            case CSV_RANKINGS -> buildRankingsCsv(hackathon);

            case CSV_SCORES -> buildScoresCsv(hackathon, false);

            case ANONYMIZED_RBL -> buildAnonymizedRblCsv(hackathon);

            case FULL_REPORT -> buildFullReportCsv(hackathon);

        };

    }



    private byte[] buildRankingsCsv(Hackathon hackathon) {

        List<FinalTeamRankingItemResponse> rankings =

                finalRankingQueryService.teamRankingsForHackathon(hackathon.getId());

        String bom = "\uFEFF";

        if (!rankings.isEmpty()) {

            StringBuilder ranked = new StringBuilder(bom);

            ranked.append("section,rank,team_id,team_name,chapter_name,weighted_avg_score,judge_count,status,note\n");

            for (FinalTeamRankingItemResponse item : rankings) {
                ranked.append("TEAM_RANKING,").append(item.getRank()).append(',')
                        .append(item.getTeamId()).append(',')
                        .append(csv(item.getTeamName())).append(',')
                        .append(csv(item.getChapterName())).append(',')
                        .append(item.getWeightedAvgScore() != null ? item.getWeightedAvgScore() : "").append(',')
                        .append(item.getJudgeCount() != null ? item.getJudgeCount() : "").append(',')
                        .append(',')
                        .append('\n');
            }

            // DQ / other teams not in ranking still listed
            for (Team team : teamRepository.findByHackathon_Id(hackathon.getId())) {
                boolean inRanking = rankings.stream().anyMatch(r -> Objects.equals(r.getTeamId(), team.getId()));
                if (inRanking) {
                    continue;
                }
                String chapter = team.getChapter() != null ? team.getChapter().getCode() : "";
                String status = team.getStatus() != null ? team.getStatus().name() : "";
                String note = status.contains("DISQUAL") ? "DQ" : status;
                ranked.append("TEAM_OTHER,").append(',')
                        .append(team.getId()).append(',')
                        .append(csv(team.getTeamName())).append(',')
                        .append(csv(chapter)).append(',')
                        .append(',').append(',')
                        .append(csv(status)).append(',')
                        .append(csv(note)).append('\n');
            }

            return ranked.toString().getBytes(StandardCharsets.UTF_8);

        }



        StringBuilder sb = new StringBuilder(bom + "section,team_id,team_name,status,chapter,note\n");

        for (Team team : teamRepository.findByHackathon_Id(hackathon.getId())) {

            String chapter = team.getChapter() != null ? team.getChapter().getCode() : "";
            String status = team.getStatus() != null ? team.getStatus().name() : "";
            String note = status.contains("DISQUAL") ? "DQ" : "";

            sb.append("TEAM,").append(team.getId()).append(',')

                    .append(csv(team.getTeamName())).append(',')

                    .append(status).append(',')

                    .append(csv(chapter)).append(',')
                    .append(csv(note)).append('\n');

        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);

    }



    private byte[] buildScoresCsv(Hackathon hackathon, boolean anonymizeJudges) {

        StringBuilder sb = new StringBuilder(

                anonymizeJudges

                        ? "submission_id,anonymized_judge_id,criterion_id,score_value,score_type,scored_at\n"

                        : "submission_id,judge_id,criterion_id,score_value,score_type,scored_at\n");

        for (Score score : collectNormalScores(hackathon).values()) {

            sb.append(score.getSubmission().getId()).append(',')

                    .append(anonymizeJudges

                            ? csv(anonymizeJudge(hackathon.getId(), score.getJudge().getId()))

                            : score.getJudge().getId()).append(',')

                    .append(score.getCriterion().getId()).append(',')

                    .append(score.getScoreValue()).append(',')

                    .append(score.getScoreType()).append(',')

                    .append(score.getScoredAt()).append('\n');

        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);

    }



    private byte[] buildAnonymizedRblCsv(Hackathon hackathon) {

        StringBuilder sb = new StringBuilder(

                "round_id,round_name,criterion_id,criterion_name,criterion_type,anonymized_judge_id,judge_type,mean_score,std_dev\n");

        for (Round round : roundRepository.findByHackathon_IdOrderByExamAtAsc(hackathon.getId())) {

            List<RblVarianceItemResponse> items = rblDashboardService.varianceByRound(round.getId());

            for (RblVarianceItemResponse item : items) {

                sb.append(round.getId()).append(',')

                        .append(csv(round.getName())).append(',')

                        .append(item.getCriterionId()).append(',')

                        .append(csv(item.getCriterionName())).append(',')

                        .append(csv(item.getCriterionType())).append(',')

                        .append(csv(anonymizeJudge(hackathon.getId(), item.getJudgeId()))).append(',')

                        .append(csv(item.getJudgeType())).append(',')

                        .append(item.getMeanScore() != null ? item.getMeanScore() : "").append(',')

                        .append(item.getStdDev() != null ? item.getStdDev() : "").append('\n');

            }

        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);

    }



    private byte[] buildFullReportCsv(Hackathon hackathon) {

        StringBuilder sb = new StringBuilder();

        sb.append("# hackathon_id,").append(hackathon.getId()).append('\n');

        sb.append("# hackathon_name,").append(csv(hackathon.getName())).append('\n');

        sb.append("# export_type,FULL_REPORT\n");

        sb.append("# generated_at,").append(LocalDateTime.now()).append('\n');

        sb.append('\n');



        sb.append("# SECTION: RANKINGS\n");

        sb.append(new String(buildRankingsCsv(hackathon), StandardCharsets.UTF_8)).append('\n');



        sb.append("# SECTION: SCORES_ANONYMIZED\n");

        sb.append(new String(buildScoresCsv(hackathon, true), StandardCharsets.UTF_8)).append('\n');



        sb.append("# SECTION: RBL_VARIANCE_ANONYMIZED\n");

        sb.append(new String(buildAnonymizedRblCsv(hackathon), StandardCharsets.UTF_8)).append('\n');



        return sb.toString().getBytes(StandardCharsets.UTF_8);

    }



    private Map<Integer, Score> collectNormalScores(Hackathon hackathon) {

        Map<Integer, Score> scores = new LinkedHashMap<>();

        roundRepository.findByHackathon_IdOrderByExamAtAsc(hackathon.getId()).forEach(round -> {

            scoreRepository.findBySubmission_Round_Id(round.getId())

                    .forEach(score -> scores.putIfAbsent(score.getId(), score));

            trackRepository.findByRoundIdOrderBySequenceOrderAsc(round.getId()).forEach(track ->

                    scoreRepository.findBySubmission_Track_Round_Id(round.getId())

                            .forEach(score -> scores.putIfAbsent(score.getId(), score)));

        });

        scores.values().removeIf(score -> score.getScoreType() != ScoreType.NORMAL);

        return scores;

    }



    private static String anonymizeJudge(Integer hackathonId, Integer judgeId) {

        if (judgeId == null) {

            return "";

        }

        int hash = Objects.hash(hackathonId, judgeId);

        return "J" + Integer.toUnsignedString(hash, 36).toUpperCase();

    }



    private static String csv(String value) {

        if (value == null) {

            return "";

        }

        String escaped = value.replace("\"", "\"\"");

        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n")) {

            return "\"" + escaped + "\"";

        }

        return escaped;

    }

}

