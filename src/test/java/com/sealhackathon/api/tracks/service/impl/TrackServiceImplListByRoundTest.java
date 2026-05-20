package com.sealhackathon.api.tracks.service.impl;

import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.common.exception.ResourceNotFoundException;
import com.sealhackathon.api.criteria.repository.CriteriaRepository;
import com.sealhackathon.api.events.repository.EventRepository;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.mentor_assignments.repository.MentorAssignmentRepository;
import com.sealhackathon.api.notifications.service.NotificationService;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.teams.repository.TeamPlaceholderRepository;
import com.sealhackathon.api.tracks.dto.response.TrackSummaryResponse;
import com.sealhackathon.api.tracks.entity.Track;
import com.sealhackathon.api.tracks.mapper.TrackMapper;
import com.sealhackathon.api.tracks.repository.TrackRepository;
import com.sealhackathon.api.tracks.value_object.TrackStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrackServiceImplListByRoundTest {

    @Mock private TrackRepository trackRepository;
    @Mock private HackathonRepository hackathonRepository;
    @Mock private RoundRepository roundRepository;
    @Mock private TrackMapper trackMapper;
    @Mock private AuditService auditService;
    @Mock private TeamPlaceholderRepository teamRepository;
    @Mock private MentorAssignmentRepository mentorAssignmentRepository;
    @Mock private NotificationService notificationService;
    @Mock private EventRepository eventRepository;
    @Mock private CriteriaRepository criteriaRepository;

    @InjectMocks
    private TrackServiceImpl trackService;

    @Test
    void listByRound_returnsTracksOrderedBySequence() {
        Round round = Round.builder().id(10).build();
        Track t1 = Track.builder().id(1).round(round).sequenceOrder(1).status(TrackStatus.OPEN).build();
        Track t2 = Track.builder().id(2).round(round).sequenceOrder(2).status(TrackStatus.OPEN).build();

        when(roundRepository.findById(10)).thenReturn(Optional.of(round));
        when(trackRepository.findByRoundIdOrderBySequenceOrderAsc(10)).thenReturn(List.of(t1, t2));
        when(trackMapper.toSummary(t1)).thenReturn(TrackSummaryResponse.builder()
                .id(1).roundId(10).name("A").sequenceOrder(1).status(TrackStatus.OPEN).build());
        when(trackMapper.toSummary(t2)).thenReturn(TrackSummaryResponse.builder()
                .id(2).roundId(10).name("B").sequenceOrder(2).status(TrackStatus.OPEN).build());

        List<TrackSummaryResponse> result = trackService.listByRound(10, null);

        assertEquals(2, result.size());
        assertEquals(10, result.get(0).getRoundId());
        verify(trackRepository).findByRoundIdOrderBySequenceOrderAsc(10);
    }

    @Test
    void listByRound_filtersByStatus() {
        Round round = Round.builder().id(10).build();
        Track open = Track.builder().id(1).round(round).status(TrackStatus.OPEN).build();
        Track cancelled = Track.builder().id(2).round(round).status(TrackStatus.CANCELLED).build();

        when(roundRepository.findById(10)).thenReturn(Optional.of(round));
        when(trackRepository.findByRoundIdOrderBySequenceOrderAsc(10)).thenReturn(List.of(open, cancelled));
        when(trackMapper.toSummary(open)).thenReturn(TrackSummaryResponse.builder()
                .id(1).roundId(10).status(TrackStatus.OPEN).build());

        List<TrackSummaryResponse> result = trackService.listByRound(10, TrackStatus.OPEN);

        assertEquals(1, result.size());
        assertEquals(TrackStatus.OPEN, result.get(0).getStatus());
    }

    @Test
    void listByRound_throwsWhenRoundMissing() {
        when(roundRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> trackService.listByRound(99, null));
    }
}
