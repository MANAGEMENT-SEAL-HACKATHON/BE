package com.sealhackathon.api.export_jobs.support;

import com.sealhackathon.api.chapters.entity.Chapter;
import com.sealhackathon.api.chapters.repository.ChapterRankingRepository;
import com.sealhackathon.api.criteria.repository.CriteriaRepository;
import com.sealhackathon.api.export_jobs.value_object.ExportJobType;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.individual_rankings.repository.IndividualRankingRepository;
import com.sealhackathon.api.judge_assignments.repository.JudgeAssignmentRepository;
import com.sealhackathon.api.prizes.repository.PrizeRepository;
import com.sealhackathon.api.rbl.service.RblDashboardService;
import com.sealhackathon.api.rounds.dto.response.RoundRankingItemResponse;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.query.RoundRankingQueryService;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.rounds.value_object.RoundType;
import com.sealhackathon.api.scores.repository.ScoreRepository;
import com.sealhackathon.api.submissions.repository.SubmissionRepository;
import com.sealhackathon.api.teams.entity.Team;
import com.sealhackathon.api.teams.repository.TeamMemberRepository;
import com.sealhackathon.api.teams.repository.TeamRepository;
import com.sealhackathon.api.tracks.entity.Track;
import com.sealhackathon.api.tracks.repository.TrackRepository;
import com.sealhackathon.api.users.entity.User;
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
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExportCsvBuilderRankingsTest {

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
    @Mock private RoundRankingQueryService roundRankingQueryService;
    @Mock private RblDashboardService rblDashboardService;

    @InjectMocks private ExportCsvBuilder exportCsvBuilder;

    @Test
    void buildRankingsCsv_includesMultipleRoundsAndPlaceholderForEmptyRound() {
        Hackathon hackathon = Hackathon.builder().id(1).name("H1").status(HackathonStatus.ONGOING).build();
        Round prelim = Round.builder().id(10).name("Sơ loại").isFinal(false).roundType(RoundType.PRELIMINARY).build();
        Round finalRound = Round.builder().id(20).name("Chung kết").isFinal(true).roundType(RoundType.FINAL).build();
        User leaderA = User.builder().id(1).fullName("Leader A").email("a@ex.com").build();
        User leaderB = User.builder().id(2).fullName("Leader B").email("b@ex.com").build();
        Chapter chapter = Chapter.builder().id(1).code("FPT-HCM").name("FPT HCM").build();

        when(roundRepository.findByHackathon_IdOrderByExamAtAsc(1)).thenReturn(List.of(prelim, finalRound));
        when(roundRankingQueryService.rankingForRound(10, false)).thenReturn(List.of(
                RoundRankingItemResponse.builder()
                        .rank(1)
                        .teamId(101)
                        .teamName("Team A")
                        .trackId(1)
                        .totalScore(8.5)
                        .participationStatus("ACTIVE")
                        .submissionStatus("SUBMITTED")
                        .build(),
                RoundRankingItemResponse.builder()
                        .rank(2)
                        .teamId(102)
                        .teamName("Team B")
                        .trackId(2)
                        .totalScore(7.2)
                        .participationStatus("ACTIVE")
                        .submissionStatus("SUBMITTED")
                        .build()
        ));
        when(roundRankingQueryService.rankingForRound(20, false)).thenReturn(List.of());
        when(teamRepository.findByHackathon_IdWithLeaderAndChapter(1)).thenReturn(List.of(
                Team.builder().id(101).teamName("Team A").leader(leaderA).chapter(chapter).build(),
                Team.builder().id(102).teamName("Team B").leader(leaderB).build(),
                Team.builder().id(103).teamName("Team C").build()
        ));
        when(teamMemberRepository.findAcceptedByTeam_IdInWithUser(anyCollection())).thenReturn(List.of());
        when(trackRepository.findByHackathonIdOrderById(1)).thenReturn(List.of(
                Track.builder().id(1).name("Bảng A").build(),
                Track.builder().id(2).name("Bảng B").build()
        ));
        when(scoreRepository.countDistinctNormalJudgesByTeamForRound(10)).thenReturn(Map.of(101, 2, 102, 1));
        when(scoreRepository.countDistinctNormalJudgesByTeamForRound(20)).thenReturn(Map.of());
        when(submissionRepository.findByRound_Id(anyInt())).thenReturn(List.of());

        byte[] bytes = exportCsvBuilder.build(hackathon, ExportJobType.CSV_RANKINGS);
        String csv = new String(bytes, StandardCharsets.UTF_8);

        assertThat(csv).contains(ExportCsvBuilder.HEADER_CSV_RANKINGS);
        assertThat(csv).contains("ROUND_RANKING,10,Sơ loại,false,1,Bảng A,1,101,Team A,FPT-HCM,FPT HCM,8.5,2,Leader A,a@ex.com");
        assertThat(csv).contains("ROUND_RANKING,10,Sơ loại,false,2,Bảng B,2,102,Team B");
        assertThat(csv).contains("ROUND_RANKING,20,Chung kết,true,,,,,,,,,");
        assertThat(csv).contains("Chưa có kết quả");
        assertThat(csv).contains("TEAM_OTHER");
        assertThat(csv).contains("Team C");
        assertThat(csv.lines().filter(line -> line.startsWith("ROUND_RANKING")).count()).isGreaterThanOrEqualTo(3);
    }

    @Test
    void buildFullReportCsv_embedsRankingsWithoutCrash() {
        Hackathon hackathon = Hackathon.builder().id(2).name("H2").status(HackathonStatus.FINISHED).build();
        Round prelim = Round.builder().id(11).name("Prelim").isFinal(false).build();

        when(roundRepository.findByHackathon_IdOrderByExamAtAsc(2)).thenReturn(List.of(prelim));
        when(roundRankingQueryService.rankingForRound(11, false)).thenReturn(List.of(
                RoundRankingItemResponse.builder()
                        .rank(1)
                        .teamId(201)
                        .teamName("T1")
                        .trackId(5)
                        .totalScore(9.0)
                        .build()
        ));
        when(teamRepository.findByHackathon_IdWithLeaderAndChapter(2)).thenReturn(List.of(
                Team.builder().id(201).teamName("T1").build()
        ));
        when(teamMemberRepository.findAcceptedByTeam_IdInWithUser(any())).thenReturn(List.of());
        when(trackRepository.findByHackathonIdOrderById(2)).thenReturn(List.of(
                Track.builder().id(5).name("Track X").build()));
        when(trackRepository.findByRoundIdOrderBySequenceOrderAsc(anyInt())).thenReturn(List.of());
        when(scoreRepository.findBySubmission_Round_Id(anyInt())).thenReturn(List.of());
        when(scoreRepository.countDistinctNormalJudgesByTeamForRound(anyInt())).thenReturn(Map.of(201, 3));
        when(submissionRepository.findByRound_Id(anyInt())).thenReturn(List.of());
        when(submissionRepository.findByHackathon_Id(2)).thenReturn(List.of());
        when(chapterRankingRepository.findByHackathon_IdOrderByRankAsc(2)).thenReturn(List.of());
        when(individualRankingRepository.findByHackathon_IdOrderByRankAsc(2)).thenReturn(List.of());
        when(prizeRepository.findByRound_Hackathon_IdOrderByAwardedAtDesc(2)).thenReturn(List.of());
        when(rblDashboardService.varianceByRound(anyInt()))
                .thenReturn(com.sealhackathon.api.rbl.dto.response.RblVarianceResponse.builder()
                        .perJudgeSpread(List.of())
                        .interRaterByCriterion(List.of())
                        .build());

        byte[] bytes = exportCsvBuilder.build(hackathon, ExportJobType.FULL_REPORT);
        String csv = new String(bytes, StandardCharsets.UTF_8);

        assertThat(csv).contains(ExportCsvBuilder.SECTION_RANKINGS);
        assertThat(csv).contains(ExportCsvBuilder.SECTION_TEAMS);
        assertThat(csv).contains("track_name");
        assertThat(csv).contains("round_name");
        assertThat(csv).contains(",3,"); // judge_count
    }
}
