package com.sealhackathon.api.me.judge.service.impl;

import com.sealhackathon.api.common.security.CurrentUserAccessor;
import com.sealhackathon.api.judge_assignments.repository.JudgeAssignmentRepository;
import com.sealhackathon.api.me.judge.dto.response.JudgeSubmissionListItemResponse;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.scores.repository.ScoreRepository;
import com.sealhackathon.api.submissions.entity.Submission;
import com.sealhackathon.api.submissions.repository.SubmissionRepository;
import com.sealhackathon.api.submissions.value_object.SubmissionStatus;
import com.sealhackathon.api.teams.entity.Team;
import com.sealhackathon.api.tracks.entity.Track;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.teams.repository.TeamRepository;
import com.sealhackathon.api.tiebreak_evaluations.repository.TiebreakEvaluationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JudgePortalServiceTest {

    @Mock private CurrentUserAccessor currentUserAccessor;
    @Mock private JudgeAssignmentRepository judgeAssignmentRepository;
    @Mock private ScoreRepository scoreRepository;
    @Mock private TiebreakEvaluationRepository tiebreakEvaluationRepository;
    @Mock private TeamRepository teamRepository;
    @Mock private RoundRepository roundRepository;
    @Mock private SubmissionRepository submissionRepository;

    @InjectMocks
    private JudgePortalServiceImpl judgePortalService;

    @Test
    void listSubmissions_doesNotExposeTeamName() {
        when(currentUserAccessor.currentUserId()).thenReturn(99);
        when(judgeAssignmentRepository.existsByJudgeIdAndRoundScope(99, 5)).thenReturn(true);
        when(judgeAssignmentRepository.findByJudgeId(99)).thenReturn(List.of());

        Track track = Track.builder().id(3).name("AI").round(Round.builder().id(5).build()).build();
        Submission submission = Submission.builder()
                .id(42)
                .status(SubmissionStatus.SUBMITTED)
                .team(Team.builder().id(7).teamName("Secret Team").build())
                .track(track)
                .repoUrl("https://github.com/o/r")
                .slideStorageKey("k")
                .slideOriginalFilename("pitch-v3-final.pdf")
                .build();
        when(submissionRepository.findByTrack_Round_Id(5)).thenReturn(List.of(submission));

        List<JudgeSubmissionListItemResponse> items = judgePortalService.listSubmissions(5, 3);

        assertThat(items).hasSize(1);
        assertThat(items.get(0).getDisplayCode()).isEqualTo("#42");
        assertThat(items.get(0).getSubmissionId()).isEqualTo(42);
        assertThat(items.get(0).getSlideFile()).isEqualTo("pitch-v3-final.pdf");
        assertThat(JudgeSubmissionListItemResponse.class.getDeclaredFields())
                .noneMatch(f -> f.getName().equals("teamName"));
    }
}
