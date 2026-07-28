package com.sealhackathon.api.export_jobs.support;

import com.sealhackathon.api.export_jobs.value_object.ExportJobType;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.rbl.dto.response.RblVarianceItemResponse;
import com.sealhackathon.api.rbl.service.RblDashboardService;
import com.sealhackathon.api.rbl.support.JudgeResearchType;
import com.sealhackathon.api.rbl.support.JudgeResearchTypeResolver;
import com.sealhackathon.api.rbl.support.RblJudgeAnonymizer;
import com.sealhackathon.api.rounds.dto.response.RoundRankingItemResponse;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.query.RoundRankingQueryService;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.scores.entity.Score;
import com.sealhackathon.api.scores.repository.ScoreRepository;
import com.sealhackathon.api.scores.value_object.ScoreType;
import com.sealhackathon.api.teams.entity.Team;
import com.sealhackathon.api.teams.repository.TeamRepository;
import com.sealhackathon.api.tracks.entity.Track;
import com.sealhackathon.api.tracks.repository.TrackRepository;
import com.sealhackathon.api.users.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ExportCsvBuilder {

    private final TeamRepository teamRepository;
    private final RoundRepository roundRepository;
    private final TrackRepository trackRepository;
    private final ScoreRepository scoreRepository;
    private final RoundRankingQueryService roundRankingQueryService;
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
        String bom = "\uFEFF";
        String header =
                "section,round_id,round_name,is_final,track_id,track_name,rank,team_id,team_name,chapter_name,weighted_avg_score,judge_count,status,note\n";
        StringBuilder sb = new StringBuilder(bom).append(header);

        Map<Integer, Team> teamsById = teamRepository.findByHackathon_Id(hackathon.getId()).stream()
                .collect(Collectors.toMap(Team::getId, team -> team, (left, right) -> left));
        Set<Integer> rankedTeamIds = new HashSet<>();

        List<Round> rounds = roundRepository.findByHackathon_IdOrderByExamAtAsc(hackathon.getId());
        if (rounds.isEmpty()) {
            return buildFallbackTeamsCsv(hackathon, bom);
        }

        for (Round round : rounds) {
            boolean isFinal = Boolean.TRUE.equals(round.getIsFinal());
            List<RoundRankingItemResponse> rankings =
                    roundRankingQueryService.rankingForRound(round.getId(), false);
            if (rankings.isEmpty()) {
                sb.append("ROUND_RANKING,")
                        .append(round.getId()).append(',')
                        .append(csv(round.getName())).append(',')
                        .append(isFinal).append(',')
                        .append(",,,,,,,,")
                        .append(csv("Chưa có kết quả")).append(',')
                        .append('\n');
                continue;
            }
            for (RoundRankingItemResponse item : rankings) {
                if (item.getTeamId() != null) {
                    rankedTeamIds.add(item.getTeamId());
                }
                Team team = item.getTeamId() != null ? teamsById.get(item.getTeamId()) : null;
                String chapter = team != null && team.getChapter() != null ? team.getChapter().getCode() : "";
                String trackName = resolveTrackName(item.getTrackId());
                sb.append("ROUND_RANKING,")
                        .append(round.getId()).append(',')
                        .append(csv(round.getName())).append(',')
                        .append(isFinal).append(',')
                        .append(item.getTrackId() != null ? item.getTrackId() : "").append(',')
                        .append(csv(trackName)).append(',')
                        .append(item.getRank() != null ? item.getRank() : "").append(',')
                        .append(item.getTeamId() != null ? item.getTeamId() : "").append(',')
                        .append(csv(item.getTeamName())).append(',')
                        .append(csv(chapter)).append(',')
                        .append(item.getTotalScore() != null ? item.getTotalScore() : "").append(',')
                        .append(',')
                        .append(csv(item.getParticipationStatus())).append(',')
                        .append(csv(item.getSubmissionStatus())).append('\n');
            }
        }

        for (Team team : teamsById.values()) {
            if (rankedTeamIds.contains(team.getId())) {
                continue;
            }
            String chapter = team.getChapter() != null ? team.getChapter().getCode() : "";
            String status = team.getStatus() != null ? team.getStatus().name() : "";
            String note = status.contains("DISQUAL") ? "DQ" : status;
            sb.append("TEAM_OTHER,,,,,,")
                    .append(team.getId()).append(',')
                    .append(csv(team.getTeamName())).append(',')
                    .append(csv(chapter)).append(',')
                    .append(",,")
                    .append(csv(status)).append(',')
                    .append(csv(note)).append('\n');
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] buildFallbackTeamsCsv(Hackathon hackathon, String bom) {
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

    private String resolveTrackName(Integer trackId) {
        if (trackId == null) {
            return "";
        }
        return trackRepository.findById(trackId).map(Track::getName).orElse("");
    }

    private byte[] buildScoresCsv(Hackathon hackathon, boolean anonymizeJudges) {
        StringBuilder sb = new StringBuilder(
                anonymizeJudges
                        ? "submission_id,anonymized_judge_id,criterion_id,score_value,score_type,scored_at\n"
                        : "submission_id,judge_id,criterion_id,score_value,score_type,scored_at\n");

        for (Score score : collectNormalScores(hackathon).values()) {
            sb.append(score.getSubmission().getId()).append(',')
                    .append(anonymizeJudges
                            ? csv(RblJudgeAnonymizer.anonymize(hackathon.getId(), score.getJudge().getId()))
                            : score.getJudge().getId()).append(',')
                    .append(score.getCriterion().getId()).append(',')
                    .append(score.getScoreValue()).append(',')
                    .append(score.getScoreType()).append(',')
                    .append(score.getScoredAt()).append('\n');
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Long/tidy format cho RQ1–3. PENALTY vẫn xuất (data đầy đủ);
     * script IRR lọc PENALTY khi tính ICC/α.
     */
    private byte[] buildAnonymizedRblCsv(Hackathon hackathon) {
        List<Score> scores = new ArrayList<>(collectResearchScores(hackathon));
        scores.sort(Comparator
                .comparing((Score s) -> s.getSubmission().getRound() != null
                        ? s.getSubmission().getRound().getId() : 0)
                .thenComparing(s -> s.getSubmission().getId())
                .thenComparing(s -> s.getCriterion().getId())
                .thenComparing(s -> s.getJudge().getId()));

        Set<Integer> facultyIds = new LinkedHashSet<>();
        Set<Integer> guestIds = new LinkedHashSet<>();
        Set<Integer> otherIds = new LinkedHashSet<>();
        for (Score score : scores) {
            User judge = score.getJudge();
            JudgeResearchType type = JudgeResearchTypeResolver.resolve(judge);
            switch (type) {
                case FACULTY -> facultyIds.add(judge.getId());
                case GUEST -> guestIds.add(judge.getId());
                case OTHER -> otherIds.add(judge.getId());
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("# irr_filter: exclude criterion_type=PENALTY and score_type=PENALTY\n");
        sb.append("# excluded_from_rq3: ").append(otherIds.size())
                .append(" judges unclassified (OTHER)\n");
        sb.append("# rq3_faculty_n: ").append(facultyIds.size()).append('\n');
        sb.append("# rq3_guest_n: ").append(guestIds.size()).append('\n');
        sb.append("round_id,round_name,submission_id,criterion_id,criterion_name,criterion_type,")
                .append("anonymized_judge_id,judge_type,score_value,score_type,scored_at\n");

        Integer hackathonId = hackathon.getId();
        for (Score score : scores) {
            Round round = score.getSubmission().getRound();
            User judge = score.getJudge();
            JudgeResearchType researchType = JudgeResearchTypeResolver.resolve(judge);
            String criterionType = score.getCriterion().getType() != null
                    ? score.getCriterion().getType().name() : "";
            sb.append(round != null ? round.getId() : "").append(',')
                    .append(csv(round != null ? round.getName() : "")).append(',')
                    .append(score.getSubmission().getId()).append(',')
                    .append(score.getCriterion().getId()).append(',')
                    .append(csv(score.getCriterion().getName())).append(',')
                    .append(csv(criterionType)).append(',')
                    .append(csv(RblJudgeAnonymizer.anonymize(hackathonId, judge.getId()))).append(',')
                    .append(csv(researchType.name())).append(',')
                    .append(score.getScoreValue()).append(',')
                    .append(score.getScoreType() != null ? score.getScoreType().name() : "").append(',')
                    .append(score.getScoredAt() != null ? score.getScoredAt() : "").append('\n');
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    /** Aggregate variance cũ — giữ trong FULL_REPORT. */
    private byte[] buildRblVarianceAggregateCsv(Hackathon hackathon) {
        StringBuilder sb = new StringBuilder(
                "round_id,round_name,criterion_id,criterion_name,criterion_type,"
                        + "anonymized_judge_id,judge_type,mean_score,std_dev\n");
        for (Round round : roundRepository.findByHackathon_IdOrderByExamAtAsc(hackathon.getId())) {
            var variance = rblDashboardService.varianceByRound(round.getId());
            List<RblVarianceItemResponse> items = variance.getPerJudgeSpread() != null
                    ? variance.getPerJudgeSpread()
                    : List.of();
            for (RblVarianceItemResponse item : items) {
                sb.append(round.getId()).append(',')
                        .append(csv(round.getName())).append(',')
                        .append(item.getCriterionId()).append(',')
                        .append(csv(item.getCriterionName())).append(',')
                        .append(csv(item.getCriterionType())).append(',')
                        .append(csv(item.getAnonymizedJudgeId())).append(',')
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

        sb.append("# SECTION: ANONYMIZED_RBL_LONG\n");
        sb.append(new String(buildAnonymizedRblCsv(hackathon), StandardCharsets.UTF_8)).append('\n');

        sb.append("# SECTION: RBL_VARIANCE_AGGREGATE\n");
        sb.append(new String(buildRblVarianceAggregateCsv(hackathon), StandardCharsets.UTF_8)).append('\n');

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private Map<Integer, Score> collectNormalScores(Hackathon hackathon) {
        Map<Integer, Score> scores = collectAllScores(hackathon);
        scores.values().removeIf(score -> score.getScoreType() != ScoreType.NORMAL);
        return scores;
    }

    /** NORMAL + PENALTY (xuất đủ); loại CALIBRATION leftover nếu còn trong bảng scores. */
    private List<Score> collectResearchScores(Hackathon hackathon) {
        return collectAllScores(hackathon).values().stream()
                .filter(s -> s.getScoreType() != ScoreType.CALIBRATION)
                .toList();
    }

    private Map<Integer, Score> collectAllScores(Hackathon hackathon) {
        Map<Integer, Score> scores = new LinkedHashMap<>();
        roundRepository.findByHackathon_IdOrderByExamAtAsc(hackathon.getId()).forEach(round -> {
            scoreRepository.findBySubmission_Round_Id(round.getId())
                    .forEach(score -> scores.putIfAbsent(score.getId(), score));
            trackRepository.findByRoundIdOrderBySequenceOrderAsc(round.getId()).forEach(track ->
                    scoreRepository.findBySubmission_Track_Round_Id(round.getId())
                            .forEach(score -> scores.putIfAbsent(score.getId(), score)));
        });
        return scores;
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
