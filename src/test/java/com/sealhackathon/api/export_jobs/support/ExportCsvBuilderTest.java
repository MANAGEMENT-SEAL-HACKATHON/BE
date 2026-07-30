package com.sealhackathon.api.export_jobs.support;

import com.sealhackathon.api.appeals.repository.AppealRepository;
import com.sealhackathon.api.chapters.repository.ChapterRankingRepository;
import com.sealhackathon.api.criteria.repository.CriteriaRepository;
import com.sealhackathon.api.export_jobs.value_object.ExportJobType;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.individual_rankings.repository.IndividualRankingRepository;
import com.sealhackathon.api.judge_assignments.repository.JudgeAssignmentRepository;
import com.sealhackathon.api.prizes.repository.PrizeRepository;
import com.sealhackathon.api.rbl.dto.response.RblVarianceResponse;
import com.sealhackathon.api.rbl.service.RblDashboardService;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.query.RoundRankingQueryService;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.scores.repository.ScoreRepository;
import com.sealhackathon.api.submissions.repository.SubmissionRepository;
import com.sealhackathon.api.teams.repository.TeamMemberRepository;
import com.sealhackathon.api.teams.repository.TeamRepository;
import com.sealhackathon.api.tracks.repository.TrackRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExportCsvBuilderTest {

    @Mock private TeamRepository teamRepository;
    @Mock private TeamMemberRepository teamMemberRepository;
    @Mock private RoundRepository roundRepository;
    @Mock private TrackRepository trackRepository;
    @Mock private ScoreRepository scoreRepository;
    @Mock private SubmissionRepository submissionRepository;
    @Mock private CriteriaRepository criteriaRepository;
    @Mock private JudgeAssignmentRepository judgeAssignmentRepository;
    @Mock private ChapterRankingRepository chapterRankingRepository;
    @Mock private IndividualRankingRepository individualRankingRepository;
    @Mock private PrizeRepository prizeRepository;
    @Mock private AppealRepository appealRepository;
    @Mock private RoundRankingQueryService roundRankingQueryService;
    @Mock private RblDashboardService rblDashboardService;

    @InjectMocks private ExportCsvBuilder exportCsvBuilder;

    @Test
    void headerConstants_matchFinalizedStrings() {
        assertThat(ExportCsvBuilder.HEADER_CSV_SCORES).isEqualTo(
                "hackathon_id,hackathon_name,round_id,round_name,is_final,track_id,track_name,"
                        + "team_id,team_name,chapter_code,chapter_name,submission_id,submission_status,is_late,"
                        + "judge_id,judge_name,judge_email,judge_type,"
                        + "criterion_id,criterion_name,criterion_type,criterion_weight,criterion_max_score,"
                        + "score_value,weighted_value,score_type,comment,scored_at");
        assertThat(ExportCsvBuilder.HEADER_CSV_SCORES_ANONYMIZED).contains("anonymized_judge_id");
        assertThat(ExportCsvBuilder.HEADER_CSV_SCORES_ANONYMIZED).doesNotContain("judge_name");
        assertThat(ExportCsvBuilder.HEADER_CSV_RANKINGS).isEqualTo(
                "section,round_id,round_name,is_final,track_id,track_name,rank,team_id,team_name,"
                        + "chapter_code,chapter_name,weighted_avg_score,judge_count,"
                        + "leader_name,leader_email,member_count,members,"
                        + "is_disqualified,elimination_reason,submitted_at,is_late,status,note");
    }

    @Test
    void csvScores_emitsExactHeaderWithBom_forEmptyData() {
        Hackathon hackathon = Hackathon.builder().id(1).name("H").status(HackathonStatus.FINISHED).build();
        when(roundRepository.findByHackathon_IdOrderByExamAtAsc(1)).thenReturn(List.of());
        when(trackRepository.findByHackathonIdOrderById(1)).thenReturn(List.of());

        String csv = new String(exportCsvBuilder.build(hackathon, ExportJobType.CSV_SCORES), StandardCharsets.UTF_8);

        assertThat(csv).startsWith(ExportCsvBuilder.UTF8_BOM + ExportCsvBuilder.HEADER_CSV_SCORES + "\n");
    }

    @Test
    void csvRankings_emitsExactHeaderWithBom_forMinimalRound() {
        Hackathon hackathon = Hackathon.builder().id(2).name("H2").status(HackathonStatus.FINISHED).build();
        Round round = Round.builder().id(10).name("Prelim").isFinal(false).build();
        when(roundRepository.findByHackathon_IdOrderByExamAtAsc(2)).thenReturn(List.of(round));
        when(teamRepository.findByHackathon_IdWithLeaderAndChapter(2)).thenReturn(List.of());
        when(trackRepository.findByHackathonIdOrderById(2)).thenReturn(List.of());
        when(scoreRepository.countDistinctNormalJudgesByTeamForRound(10)).thenReturn(Map.of());
        when(submissionRepository.findByRound_Id(10)).thenReturn(List.of());
        when(roundRankingQueryService.rankingForRound(10, false)).thenReturn(List.of());

        String csv = new String(exportCsvBuilder.build(hackathon, ExportJobType.CSV_RANKINGS), StandardCharsets.UTF_8);

        assertThat(csv).startsWith(ExportCsvBuilder.UTF8_BOM + ExportCsvBuilder.HEADER_CSV_RANKINGS + "\n");
        assertThat(csv).contains("ROUND_RANKING,10,Prelim,false");
    }

    @Test
    void fullReport_containsAllSectionMarkers_forMinimalData() {
        Hackathon hackathon = Hackathon.builder().id(3).name("H3").status(HackathonStatus.FINISHED).build();
        Round round = Round.builder().id(30).name("R").isFinal(false).build();

        when(roundRepository.findByHackathon_IdOrderByExamAtAsc(3)).thenReturn(List.of(round));
        when(teamRepository.findByHackathon_IdWithLeaderAndChapter(3)).thenReturn(List.of());
        when(trackRepository.findByHackathonIdOrderById(3)).thenReturn(List.of());
        when(trackRepository.findByRoundIdOrderBySequenceOrderAsc(anyInt())).thenReturn(List.of());
        when(scoreRepository.findBySubmission_Round_Id(anyInt())).thenReturn(List.of());
        when(scoreRepository.countDistinctNormalJudgesByTeamForRound(anyInt())).thenReturn(Map.of());
        when(submissionRepository.findByRound_Id(anyInt())).thenReturn(List.of());
        when(submissionRepository.findByHackathon_Id(3)).thenReturn(List.of());
        when(roundRankingQueryService.rankingForRound(anyInt(), any(Boolean.class))).thenReturn(List.of());
        when(chapterRankingRepository.findByHackathon_IdOrderByRankAsc(3)).thenReturn(List.of());
        when(individualRankingRepository.findByHackathon_IdOrderByRankAsc(3)).thenReturn(List.of());
        when(prizeRepository.findByRound_Hackathon_IdOrderByAwardedAtDesc(3)).thenReturn(List.of());
        when(appealRepository.findByRound_IdOrderByCreatedAtDesc(anyInt())).thenReturn(List.of());
        when(rblDashboardService.varianceByRound(anyInt())).thenReturn(RblVarianceResponse.builder()
                .perJudgeSpread(List.of())
                .interRaterByCriterion(List.of())
                .build());

        String csv = new String(exportCsvBuilder.build(hackathon, ExportJobType.FULL_REPORT), StandardCharsets.UTF_8);

        assertThat(csv).contains(ExportCsvBuilder.SECTION_TEAMS);
        assertThat(csv).contains(ExportCsvBuilder.SECTION_TEAM_MEMBERS);
        assertThat(csv).contains(ExportCsvBuilder.SECTION_CRITERIA);
        assertThat(csv).contains(ExportCsvBuilder.SECTION_JUDGE_ASSIGNMENTS);
        assertThat(csv).contains(ExportCsvBuilder.SECTION_SUBMISSIONS);
        assertThat(csv).contains(ExportCsvBuilder.SECTION_RANKINGS);
        assertThat(csv).contains(ExportCsvBuilder.SECTION_CHAPTER_RANKINGS);
        assertThat(csv).contains(ExportCsvBuilder.SECTION_INDIVIDUAL_RANKINGS);
        assertThat(csv).contains(ExportCsvBuilder.SECTION_PRIZES);
        assertThat(csv).contains(ExportCsvBuilder.SECTION_APPEALS);
        assertThat(csv).contains(ExportCsvBuilder.SECTION_SCORES_ANONYMIZED);
        assertThat(csv).contains(ExportCsvBuilder.SECTION_ANONYMIZED_RBL_LONG);
        assertThat(csv).contains(ExportCsvBuilder.SECTION_RBL_VARIANCE_AGGREGATE);
        assertThat(csv).contains(ExportCsvBuilder.HEADER_CSV_RANKINGS);
        assertThat(csv).contains(ExportCsvBuilder.HEADER_CSV_SCORES_ANONYMIZED);
    }
}
