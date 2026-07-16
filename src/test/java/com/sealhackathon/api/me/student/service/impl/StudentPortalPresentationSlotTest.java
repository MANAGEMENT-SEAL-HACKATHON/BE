package com.sealhackathon.api.me.student.service.impl;

import com.sealhackathon.api.common.security.CurrentUserAccessor;
import com.sealhackathon.api.events.entity.PresentationSlot;
import com.sealhackathon.api.events.repository.PresentationSlotRepository;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.me.student.dto.response.StudentPresentationSlotResponse;
import com.sealhackathon.api.me.support.StudentAccessGuard;
import com.sealhackathon.api.presentation.value_object.PresentationQueueStatus;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.submissions.entity.Submission;
import com.sealhackathon.api.teams.entity.Team;
import com.sealhackathon.api.teams.entity.TeamMember;
import com.sealhackathon.api.teams.entity.TeamRoundTrack;
import com.sealhackathon.api.teams.repository.TeamMemberRepository;
import com.sealhackathon.api.teams.repository.TeamRoundTrackRepository;
import com.sealhackathon.api.teams.value_object.TeamMemberStatus;
import com.sealhackathon.api.tracks.entity.Track;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentPortalPresentationSlotTest {

    @Mock private CurrentUserAccessor currentUserAccessor;
    @Mock private StudentAccessGuard studentAccessGuard;
    @Mock private RoundRepository roundRepository;
    @Mock private TeamMemberRepository teamMemberRepository;
    @Mock private TeamRoundTrackRepository teamRoundTrackRepository;
    @Mock private PresentationSlotRepository presentationSlotRepository;

    @InjectMocks
    private StudentPortalServiceImpl service;

    @Test
    void getPresentationSlot_beforeShuffle_returnsUnavailable() {
        Round round = prelimRound(10, 1);
        Team team = team(5, 1);
        when(roundRepository.findById(10)).thenReturn(Optional.of(round));
        when(currentUserAccessor.currentUserId()).thenReturn(100);
        when(teamMemberRepository.findByUser_IdAndStatus(100, TeamMemberStatus.ACCEPTED))
                .thenReturn(List.of(member(team)));
        when(teamRoundTrackRepository.findByTeam_IdAndTrack_Round_Id(5, 10))
                .thenReturn(Optional.of(TeamRoundTrack.builder().team(team).build()));
        when(presentationSlotRepository.findByRound_IdAndTeam_Id(10, 5))
                .thenReturn(Optional.empty());

        StudentPresentationSlotResponse response = service.getPresentationSlot(10);

        assertThat(response.getAvailable()).isFalse();
        assertThat(response.getMessage()).isEqualTo("Chưa quay số");
        assertThat(response.getRoundIsFinal()).isFalse();
        assertThat(response.getOrder()).isNull();
        verify(studentAccessGuard).assertTeamMember(5);
    }

    @Test
    void getPresentationSlot_skipsSkippedWhenCountingTeamsAhead() {
        Round round = prelimRound(10, 1);
        Track track = Track.builder().id(3).build();
        Team myTeam = team(5, 1);
        when(roundRepository.findById(10)).thenReturn(Optional.of(round));
        when(currentUserAccessor.currentUserId()).thenReturn(100);
        when(teamMemberRepository.findByUser_IdAndStatus(100, TeamMemberStatus.ACCEPTED))
                .thenReturn(List.of(member(myTeam)));
        when(teamRoundTrackRepository.findByTeam_IdAndTrack_Round_Id(5, 10))
                .thenReturn(Optional.of(TeamRoundTrack.builder().team(myTeam).build()));

        PresentationSlot skipped = slot(10, PresentationQueueStatus.SKIPPED, track, submission(10));
        PresentationSlot presenting = slot(12, PresentationQueueStatus.PRESENTING, track, submission(12));
        PresentationSlot waitingAhead = slot(14, PresentationQueueStatus.WAITING, track, submission(14));
        PresentationSlot mine = slot(15, PresentationQueueStatus.WAITING, track, submission(123));
        mine.setTeam(myTeam);

        when(presentationSlotRepository.findByRound_IdAndTeam_Id(10, 5))
                .thenReturn(Optional.of(mine));
        when(presentationSlotRepository.findByRound_IdAndTrack_IdOrderBySequenceOrderAsc(10, 3))
                .thenReturn(List.of(skipped, presenting, waitingAhead, mine));

        StudentPresentationSlotResponse response = service.getPresentationSlot(10);

        assertThat(response.getAvailable()).isTrue();
        assertThat(response.getOrder()).isEqualTo(15);
        assertThat(response.getDisplayCode()).isEqualTo("#123");
        assertThat(response.getStatus()).isEqualTo("WAITING");
        assertThat(response.getTrackId()).isEqualTo(3);
        assertThat(response.getCurrentPresentingOrder()).isEqualTo(12);
        assertThat(response.getCurrentPresentingDisplayCode()).isEqualTo("#12");
        // STT-06: SKIPPED excluded; PRESENTING excluded; only WAITING order 14 ahead
        assertThat(response.getTeamsAhead()).isEqualTo(1);
    }

    private static Round prelimRound(Integer id, Integer hackathonId) {
        return Round.builder()
                .id(id)
                .isFinal(false)
                .hackathon(Hackathon.builder().id(hackathonId).build())
                .build();
    }

    private static Team team(Integer id, Integer hackathonId) {
        return Team.builder()
                .id(id)
                .hackathon(Hackathon.builder().id(hackathonId).build())
                .build();
    }

    private static TeamMember member(Team team) {
        return TeamMember.builder().team(team).build();
    }

    private static Submission submission(Integer id) {
        return Submission.builder().id(id).build();
    }

    private static PresentationSlot slot(
            Integer sequenceOrder, PresentationQueueStatus status,
            Track track, Submission submission) {
        return PresentationSlot.builder()
                .sequenceOrder(sequenceOrder)
                .queueStatus(status)
                .track(track)
                .submission(submission)
                .build();
    }
}
