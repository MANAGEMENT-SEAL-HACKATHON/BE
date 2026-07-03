package com.sealhackathon.api.teams.support;

import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.teams.repository.TeamRoundTrackRepository;
import com.sealhackathon.api.tracks.entity.Track;
import com.sealhackathon.api.tracks.repository.TrackRepository;
import com.sealhackathon.api.tracks.value_object.TrackStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HackathonTeamSizeResolverTest {

    @Mock private TrackRepository trackRepository;
    @Mock private TeamRoundTrackRepository teamRoundTrackRepository;

    @InjectMocks
    private HackathonTeamSizeResolver resolver;

    @Test
    void forHackathonReturnsIntersection() {
        when(trackRepository.findByHackathonIdOrderById(1)).thenReturn(List.of(
                Track.builder().minTeamSize(3).maxTeamSize(5).status(TrackStatus.OPEN).build(),
                Track.builder().minTeamSize(4).maxTeamSize(6).status(TrackStatus.OPEN).build()));

        HackathonTeamSizeResolver.TeamSizeLimits limits = resolver.forHackathon(1);

        assertThat(limits.minTeamSize()).isEqualTo(4);
        assertThat(limits.maxTeamSize()).isEqualTo(5);
    }

    @Test
    void forHackathonThrowsWhenRangesDoNotIntersect() {
        when(trackRepository.findByHackathonIdOrderById(1)).thenReturn(List.of(
                Track.builder().minTeamSize(3).maxTeamSize(4).status(TrackStatus.OPEN).build(),
                Track.builder().minTeamSize(5).maxTeamSize(6).status(TrackStatus.OPEN).build()));

        assertThatThrownBy(() -> resolver.forHackathon(1))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(ex -> ((BusinessRuleException) ex).getCode())
                .isEqualTo(ErrorCode.TRACK_INVALID_TEAM_SIZE);
    }

    @Test
    void assertCompatibleRejectsIncompatibleTrack() {
        when(trackRepository.findByHackathonIdOrderById(1)).thenReturn(List.of(
                Track.builder().id(2).minTeamSize(3).maxTeamSize(5).status(TrackStatus.OPEN).build()));

        assertThatThrownBy(() -> resolver.assertCompatibleWithExistingTracks(1, 6, 8, null))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(ex -> ((BusinessRuleException) ex).getCode())
                .isEqualTo(ErrorCode.TRACK_INVALID_TEAM_SIZE);
    }
}
