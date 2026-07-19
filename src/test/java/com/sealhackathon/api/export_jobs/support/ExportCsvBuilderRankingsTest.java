package com.sealhackathon.api.export_jobs.support;

import com.sealhackathon.api.export_jobs.value_object.ExportJobType;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.rounds.dto.response.RoundRankingItemResponse;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.query.RoundRankingQueryService;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.rounds.value_object.RoundType;
import com.sealhackathon.api.teams.entity.Team;
import com.sealhackathon.api.teams.repository.TeamRepository;
import com.sealhackathon.api.tracks.entity.Track;
import com.sealhackathon.api.tracks.repository.TrackRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExportCsvBuilderRankingsTest {

    @Mock private TeamRepository teamRepository;
    @Mock private RoundRepository roundRepository;
    @Mock private TrackRepository trackRepository;
    @Mock private RoundRankingQueryService roundRankingQueryService;
    @Mock private com.sealhackathon.api.scores.repository.ScoreRepository scoreRepository;
    @Mock private com.sealhackathon.api.rbl.service.RblDashboardService rblDashboardService;

    @InjectMocks private ExportCsvBuilder exportCsvBuilder;

    @Test
    void buildRankingsCsv_includesMultipleRoundsAndPlaceholderForEmptyRound() {
        Hackathon hackathon = Hackathon.builder().id(1).name("H1").status(HackathonStatus.ONGOING).build();
        Round prelim = Round.builder().id(10).name("Sơ loại").isFinal(false).roundType(RoundType.PRELIMINARY).build();
        Round finalRound = Round.builder().id(20).name("Chung kết").isFinal(true).roundType(RoundType.FINAL).build();

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
        when(teamRepository.findByHackathon_Id(1)).thenReturn(List.of(
                Team.builder().id(101).teamName("Team A").build(),
                Team.builder().id(102).teamName("Team B").build(),
                Team.builder().id(103).teamName("Team C").build()
        ));
        when(trackRepository.findById(1)).thenReturn(java.util.Optional.of(
                Track.builder().id(1).name("Bảng A").build()));
        when(trackRepository.findById(2)).thenReturn(java.util.Optional.of(
                Track.builder().id(2).name("Bảng B").build()));

        byte[] bytes = exportCsvBuilder.build(hackathon, ExportJobType.CSV_RANKINGS);
        String csv = new String(bytes, StandardCharsets.UTF_8);

        assertThat(csv).contains("ROUND_RANKING,10,Sơ loại,false,1,Bảng A,1,101,Team A");
        assertThat(csv).contains("ROUND_RANKING,10,Sơ loại,false,2,Bảng B,2,102,Team B");
        assertThat(csv).contains("ROUND_RANKING,20,Chung kết,true,,,,,,,,,Chưa có kết quả,");
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
        when(teamRepository.findByHackathon_Id(2)).thenReturn(List.of(
                Team.builder().id(201).teamName("T1").build()
        ));
        when(trackRepository.findById(5)).thenReturn(java.util.Optional.of(
                Track.builder().id(5).name("Track X").build()));
        when(scoreRepository.findBySubmission_Round_Id(org.mockito.ArgumentMatchers.anyInt()))
                .thenReturn(List.of());
        when(trackRepository.findByRoundIdOrderBySequenceOrderAsc(org.mockito.ArgumentMatchers.anyInt()))
                .thenReturn(List.of());
        when(rblDashboardService.varianceByRound(org.mockito.ArgumentMatchers.anyInt()))
                .thenReturn(com.sealhackathon.api.rbl.dto.response.RblVarianceResponse.builder()
                        .perJudgeSpread(List.of())
                        .interRaterByCriterion(List.of())
                        .build());

        byte[] bytes = exportCsvBuilder.build(hackathon, ExportJobType.FULL_REPORT);
        String csv = new String(bytes, StandardCharsets.UTF_8);

        assertThat(csv).contains("# SECTION: RANKINGS");
        assertThat(csv).contains("track_name");
        assertThat(csv).contains("round_name");
    }
}
