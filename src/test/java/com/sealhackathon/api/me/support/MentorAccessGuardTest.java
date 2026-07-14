package com.sealhackathon.api.me.support;

import com.sealhackathon.api.common.exception.AuthException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.common.security.CurrentUserAccessor;
import com.sealhackathon.api.mentors.entity.MentorTeamAssignment;
import com.sealhackathon.api.mentors.repository.MentorAssignmentRepository;
import com.sealhackathon.api.mentors.repository.MentorTeamAssignmentRepository;
import com.sealhackathon.api.teams.entity.TeamRoundTrack;
import com.sealhackathon.api.teams.repository.TeamRoundTrackRepository;
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
class MentorAccessGuardTest {

    @Mock private CurrentUserAccessor currentUserAccessor;
    @Mock private MentorTeamAssignmentRepository mentorTeamAssignmentRepository;
    @Mock private MentorAssignmentRepository mentorAssignmentRepository;
    @Mock private TeamRoundTrackRepository teamRoundTrackRepository;

    @InjectMocks
    private MentorAccessGuard guard;

    @Test
    void allowsDirectTeamAssignment() {
        when(currentUserAccessor.currentUserId()).thenReturn(7);
        when(mentorTeamAssignmentRepository.findByTeam_IdOrderByRound_IdAsc(5)).thenReturn(List.of(
                MentorTeamAssignment.builder().mentor(User.builder().id(7).build()).build()));

        assertThatCode(() -> guard.assertAssignedToTeam(5)).doesNotThrowAnyException();
    }

    @Test
    void allowsTrackOnlyMentorForTeamOnTrack() {
        when(currentUserAccessor.currentUserId()).thenReturn(7);
        when(mentorTeamAssignmentRepository.findByTeam_IdOrderByRound_IdAsc(5)).thenReturn(List.of());
        Track track = Track.builder().id(10).build();
        when(teamRoundTrackRepository.findByTeam_Id(5)).thenReturn(List.of(
                TeamRoundTrack.builder().track(track).build()));
        when(mentorAssignmentRepository.existsByMentorIdAndTrackId(7, 10)).thenReturn(true);

        assertThatCode(() -> guard.assertAssignedToTeam(5)).doesNotThrowAnyException();
    }

    @Test
    void forbidsMentorForUnassignedTeam() {
        when(currentUserAccessor.currentUserId()).thenReturn(7);
        when(mentorTeamAssignmentRepository.findByTeam_IdOrderByRound_IdAsc(99)).thenReturn(List.of());
        when(teamRoundTrackRepository.findByTeam_Id(99)).thenReturn(List.of());

        assertThatThrownBy(() -> guard.assertAssignedToTeam(99))
                .isInstanceOf(AuthException.class)
                .extracting(ex -> ((AuthException) ex).getCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    void forbidsWhenTrackMentorDoesNotMatchTeamTracks() {
        when(currentUserAccessor.currentUserId()).thenReturn(7);
        when(mentorTeamAssignmentRepository.findByTeam_IdOrderByRound_IdAsc(5)).thenReturn(List.of());
        Track track = Track.builder().id(10).build();
        when(teamRoundTrackRepository.findByTeam_Id(5)).thenReturn(List.of(
                TeamRoundTrack.builder().track(track).build()));
        when(mentorAssignmentRepository.existsByMentorIdAndTrackId(7, 10)).thenReturn(false);

        assertThatThrownBy(() -> guard.assertAssignedToTeam(5))
                .isInstanceOf(AuthException.class)
                .extracting(ex -> ((AuthException) ex).getCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
    }
}
