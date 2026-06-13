package com.sealhackathon.api.export_jobs.support;

import com.sealhackathon.api.export_jobs.value_object.ExportJobType;
import com.sealhackathon.api.hackathons.entity.Hackathon;
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

@Component
@RequiredArgsConstructor
public class ExportCsvBuilder {

    private final TeamRepository teamRepository;
    private final RoundRepository roundRepository;
    private final TrackRepository trackRepository;
    private final ScoreRepository scoreRepository;

    public byte[] build(Hackathon hackathon, ExportJobType type) {
        return switch (type) {
            case CSV_RANKINGS -> buildRankingsCsv(hackathon);
            case CSV_SCORES -> buildScoresCsv(hackathon);
            case ANONYMIZED_RBL, FULL_REPORT -> buildSummaryCsv(hackathon, type);
        };
    }

    private byte[] buildRankingsCsv(Hackathon hackathon) {
        StringBuilder sb = new StringBuilder("team_id,team_name,status,chapter\n");
        for (Team team : teamRepository.findByHackathon_Id(hackathon.getId())) {
            String chapter = team.getChapter() != null ? team.getChapter().getCode() : "";
            sb.append(team.getId()).append(',')
                    .append(csv(team.getTeamName())).append(',')
                    .append(team.getStatus()).append(',')
                    .append(csv(chapter)).append('\n');
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] buildScoresCsv(Hackathon hackathon) {
        StringBuilder sb = new StringBuilder(
                "submission_id,judge_id,criterion_id,score_value,score_type,scored_at\n");
        Map<Integer, Score> scores = new LinkedHashMap<>();
        roundRepository.findByHackathon_IdOrderByExamAtAsc(hackathon.getId()).forEach(round -> {
            scoreRepository.findBySubmission_Round_Id(round.getId())
                    .forEach(score -> scores.putIfAbsent(score.getId(), score));
            trackRepository.findByRoundIdOrderBySequenceOrderAsc(round.getId()).forEach(track ->
                    scoreRepository.findBySubmission_Track_Round_Id(round.getId())
                            .forEach(score -> scores.putIfAbsent(score.getId(), score)));
        });
        for (Score score : scores.values()) {
            if (score.getScoreType() != ScoreType.NORMAL) {
                continue;
            }
            sb.append(score.getSubmission().getId()).append(',')
                    .append(score.getJudge().getId()).append(',')
                    .append(score.getCriterion().getId()).append(',')
                    .append(score.getScoreValue()).append(',')
                    .append(score.getScoreType()).append(',')
                    .append(score.getScoredAt()).append('\n');
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] buildSummaryCsv(Hackathon hackathon, ExportJobType type) {
        StringBuilder sb = new StringBuilder("hackathon_id,hackathon_name,export_type,generated_at\n");
        sb.append(hackathon.getId()).append(',')
                .append(csv(hackathon.getName())).append(',')
                .append(type).append(',')
                .append(LocalDateTime.now()).append('\n');
        return sb.toString().getBytes(StandardCharsets.UTF_8);
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
