package com.sealhackathon.api.scores.guard;

import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.judge_assignments.repository.JudgeAssignmentRepository;
import com.sealhackathon.api.mentors.entity.MentorAssignment;
import com.sealhackathon.api.mentors.repository.MentorAssignmentRepository;
import com.sealhackathon.api.mentors.repository.MentorTeamAssignmentRepository;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.submissions.entity.Submission;
import com.sealhackathon.api.teams.entity.Team;
import com.sealhackathon.api.tracks.entity.Track;
import com.sealhackathon.api.users.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MentorJudgeConflictGuardTest {

    @Mock private MentorAssignmentRepository mentorAssignmentRepository;
    @Mock private MentorTeamAssignmentRepository mentorTeamAssignmentRepository;
    @Mock private JudgeAssignmentRepository judgeAssignmentRepository;

    @InjectMocks
    private MentorJudgeConflictGuard guard;

    @Test
    void blocksMentorJudgingOwnTeamInFinalRound() {
        Team team = Team.builder().id(5).build();
        Round finalRound = Round.builder().id(99).build();
        Submission submission = Submission.builder()
                .team(team)
                .round(finalRound)
                .track(null)
                .build();

        when(mentorTeamAssignmentRepository.existsByMentor_IdAndTeam_Id(7, 5)).thenReturn(true);
        when(judgeAssignmentRepository.existsByJudgeIdAndRoundScope(7, 99)).thenReturn(true);

        assertThatThrownBy(() -> guard.requireNoConflict(7, submission))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(ex -> ((BusinessRuleException) ex).getCode())
                .isEqualTo(ErrorCode.CONFLICT_MENTOR_JUDGE_SAME_TRACK);
    }

    @Test
    void allowsJudgeWhenNotMentorOfTeam() {
        Team team = Team.builder().id(5).build();
        Round finalRound = Round.builder().id(99).build();
        Submission submission = Submission.builder()
                .team(team)
                .round(finalRound)
                .build();

        when(mentorTeamAssignmentRepository.existsByMentor_IdAndTeam_Id(7, 5)).thenReturn(false);

        assertThatCode(() -> guard.requireNoConflict(7, submission)).doesNotThrowAnyException();
    }

    @Test
    void blocksTrackLevelMentorJudgeConflict() {
        Track track = Track.builder().id(10).build();
        Team team = Team.builder().id(5).build();
        Submission submission = Submission.builder().team(team).track(track).build();

        when(mentorTeamAssignmentRepository.existsByMentor_IdAndTeam_Id(7, 5)).thenReturn(false);
        when(mentorAssignmentRepository.findByTrackId(10)).thenReturn(List.of(
                MentorAssignment.builder().mentor(User.builder().id(7).build()).build()));
        when(judgeAssignmentRepository.existsByJudgeIdAndTrackId(7, 10)).thenReturn(true);

        assertThatThrownBy(() -> guard.requireNoConflict(7, submission))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void resolvesTeamAndTrackFromSubmissionOnly_forCalibrationStylePayload() {
        // Calibration API only sends submissionId; guard must resolve team/track from Submission entity
        Track track = Track.builder().id(10).build();
        Team team = Team.builder().id(5).build();
        Round round = Round.builder().id(3).build();
        Submission submission = Submission.builder()
                .id(42)
                .team(team)
                .track(track)
                .round(round)
                .build();

        when(mentorTeamAssignmentRepository.existsByMentor_IdAndTeam_Id(7, 5)).thenReturn(false);
        when(mentorAssignmentRepository.findByTrackId(10)).thenReturn(List.of(
                MentorAssignment.builder().mentor(User.builder().id(7).build()).build()));
        when(judgeAssignmentRepository.existsByJudgeIdAndTrackId(7, 10)).thenReturn(true);

        assertThatThrownBy(() -> guard.requireNoConflict(7, submission))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(ex -> ((BusinessRuleException) ex).getCode())
                .isEqualTo(ErrorCode.CONFLICT_MENTOR_JUDGE_SAME_TRACK);
    }
}
