package com.sealhackathon.api.tracks.service.impl;

import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.common.exception.ConflictException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.criteria.repository.CriteriaRepository;
import com.sealhackathon.api.events.repository.EventRepository;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.hackathons.support.HackathonArchiveGuard;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.judge_assignments.repository.JudgeAssignmentRepository;
import com.sealhackathon.api.mentor_assignments.repository.MentorAssignmentRepository;
import com.sealhackathon.api.notifications.service.NotificationService;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.teams.repository.TeamRepository;
import com.sealhackathon.api.tracks.dto.response.TrackResponse;
import com.sealhackathon.api.tracks.entity.Track;
import com.sealhackathon.api.tracks.mapper.TrackMapper;
import com.sealhackathon.api.tracks.repository.TrackRepository;
import com.sealhackathon.api.tracks.value_object.TrackStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrackServiceImplDeleteTest {

    @Mock TrackRepository trackRepository;
    @Mock HackathonRepository hackathonRepository;
    @Mock RoundRepository roundRepository;
    @Mock TrackMapper trackMapper;
    @Mock AuditService auditService;
    @Mock TeamRepository teamRepository;
    @Mock MentorAssignmentRepository mentorAssignmentRepository;
    @Mock JudgeAssignmentRepository judgeAssignmentRepository;
    @Mock NotificationService notificationService;
    @Mock EventRepository eventRepository;
    @Mock CriteriaRepository criteriaRepository;
    @Spy HackathonArchiveGuard archiveGuard = new HackathonArchiveGuard();

    @InjectMocks TrackServiceImpl trackService;

    @Test
    void delete_openTrackWithoutTeams_succeeds() {
        Hackathon hackathon = Hackathon.builder().id(1).status(HackathonStatus.DRAFT).build();
        Round round = Round.builder().id(10).hackathon(hackathon).isActive(false).build();
        Track track = Track.builder().id(9).name("Deep Learning").status(TrackStatus.OPEN).round(round).build();

        when(trackRepository.findById(9)).thenReturn(Optional.of(track));
        when(teamRepository.countActiveByTrackId(eq(9), any())).thenReturn(0L);
        when(mentorAssignmentRepository.findByTrackId(9)).thenReturn(List.of());
        when(judgeAssignmentRepository.findByTrackId(9)).thenReturn(List.of());
        when(trackMapper.toResponse(track)).thenReturn(TrackResponse.builder().id(9).build());

        Integer deleted = trackService.delete(9);

        assertThat(deleted).isEqualTo(9);
        verify(trackRepository).delete(track);
    }

    @Test
    void delete_finishedHackathon_throwsArchived() {
        Hackathon hackathon = Hackathon.builder().id(1).status(HackathonStatus.FINISHED).build();
        Round round = Round.builder().id(10).hackathon(hackathon).isActive(false).build();
        Track track = Track.builder().id(9).status(TrackStatus.OPEN).round(round).build();

        when(trackRepository.findById(9)).thenReturn(Optional.of(track));

        assertThatThrownBy(() -> trackService.delete(9))
                .isInstanceOf(ConflictException.class)
                .matches(ex -> ErrorCode.HACKATHON_ARCHIVED.equals(((ConflictException) ex).getCode()));
    }

    @Test
    void delete_openTrackWithTeams_throwsTrackHasTeams() {
        Hackathon hackathon = Hackathon.builder().id(1).status(HackathonStatus.DRAFT).build();
        Round round = Round.builder().id(10).hackathon(hackathon).isActive(false).build();
        Track track = Track.builder().id(9).status(TrackStatus.OPEN).round(round).build();

        when(trackRepository.findById(9)).thenReturn(Optional.of(track));
        when(teamRepository.countActiveByTrackId(eq(9), any())).thenReturn(2L);

        assertThatThrownBy(() -> trackService.delete(9))
                .isInstanceOf(ConflictException.class)
                .matches(ex -> ErrorCode.TRACK_HAS_TEAMS.equals(((ConflictException) ex).getCode()));
    }
}
