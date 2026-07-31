package com.sealhackathon.api.export_jobs.support;

import com.sealhackathon.api.chapters.repository.ChapterRankingRepository;
import com.sealhackathon.api.criteria.entity.Criteria;
import com.sealhackathon.api.criteria.repository.CriteriaRepository;
import com.sealhackathon.api.criteria.value_object.CriteriaType;
import com.sealhackathon.api.export_jobs.value_object.ExportJobType;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.individual_rankings.repository.IndividualRankingRepository;
import com.sealhackathon.api.judge_assignments.repository.JudgeAssignmentRepository;
import com.sealhackathon.api.prizes.repository.PrizeRepository;
import com.sealhackathon.api.rbl.service.RblDashboardService;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.query.RoundRankingQueryService;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.scores.entity.Score;
import com.sealhackathon.api.scores.repository.ScoreRepository;
import com.sealhackathon.api.scores.value_object.ScoreType;
import com.sealhackathon.api.submissions.entity.Submission;
import com.sealhackathon.api.submissions.repository.SubmissionRepository;
import com.sealhackathon.api.teams.repository.TeamMemberRepository;
import com.sealhackathon.api.teams.repository.TeamRepository;
import com.sealhackathon.api.tracks.repository.TrackRepository;
import com.sealhackathon.api.users.entity.User;
import com.sealhackathon.api.users.value_object.UserRole;
import com.sealhackathon.api.users.value_object.UserType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

/** RBL-EXPORT-01 + RBL-JUDGE-TYPE in CSV headers */
@ExtendWith(MockitoExtension.class)
class ExportCsvBuilderRblTest {

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
    void anonymizedRbl_longFormatWithHeadersAndJudgeTypes() {
        Hackathon hackathon = Hackathon.builder().id(7).name("SEAL").status(HackathonStatus.FINISHED).build();
        Round round = Round.builder().id(10).name("Sơ loại").isFinal(false).build();
        Submission sub = Submission.builder().id(100).round(round).build();
        Criteria tech = Criteria.builder().id(1).name("Tech").type(CriteriaType.TECHNICAL).weight(0.5f).build();
        Criteria pen = Criteria.builder().id(9).name("Penalty").type(CriteriaType.PENALTY).weight(0.01f).build();

        User faculty = User.builder().id(21).role(UserRole.JUDGE)
                .userType(UserType.INTERNAL).isTempAccount(false).build();
        User guest = User.builder().id(22).role(UserRole.JUDGE)
                .userType(UserType.EXTERNAL).isTempAccount(true).build();

        Score s1 = Score.builder().id(1).submission(sub).judge(faculty).criterion(tech)
                .scoreValue(8f).scoreType(ScoreType.NORMAL).scoredAt(LocalDateTime.of(2026, 7, 1, 10, 0)).build();
        Score s2 = Score.builder().id(2).submission(sub).judge(guest).criterion(tech)
                .scoreValue(7f).scoreType(ScoreType.NORMAL).scoredAt(LocalDateTime.of(2026, 7, 1, 10, 5)).build();
        Score s3 = Score.builder().id(3).submission(sub).judge(faculty).criterion(pen)
                .scoreValue(-1f).scoreType(ScoreType.PENALTY).scoredAt(LocalDateTime.of(2026, 7, 1, 11, 0)).build();

        when(roundRepository.findByHackathon_IdOrderByExamAtAsc(7)).thenReturn(List.of(round));
        when(scoreRepository.findBySubmission_Round_Id(10)).thenReturn(List.of(s1, s2, s3));
        when(trackRepository.findByRoundIdOrderBySequenceOrderAsc(10)).thenReturn(List.of());

        byte[] bytes = exportCsvBuilder.build(hackathon, ExportJobType.ANONYMIZED_RBL);
        String csv = new String(bytes, StandardCharsets.UTF_8);

        assertThat(csv).contains("# irr_filter: exclude criterion_type=PENALTY and score_type=PENALTY");
        assertThat(csv).contains("# excluded_from_rq3: 0");
        assertThat(csv).contains("# rq3_faculty_n: 1");
        assertThat(csv).contains("# rq3_guest_n: 1");
        assertThat(csv).contains(
                "round_id,round_name,submission_id,criterion_id,criterion_name,criterion_type,"
                        + "anonymized_judge_id,judge_type,score_value,score_type,scored_at");
        assertThat(csv).contains(",FACULTY,");
        assertThat(csv).contains(",GUEST,");
        assertThat(csv).contains(",PENALTY,");
        assertThat(csv).doesNotContain(",21,");
        assertThat(csv).doesNotContain(",22,");
        assertThat(csv).contains("J");
    }
}
