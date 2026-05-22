package com.sealhackathon.api.tracks.service.impl;

import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.criteria.repository.CriteriaRepository;
import com.sealhackathon.api.events.repository.EventRepository;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.judge_assignments.repository.JudgeAssignmentRepository;
import com.sealhackathon.api.mentor_assignments.repository.MentorAssignmentRepository;
import com.sealhackathon.api.notifications.service.NotificationService;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.teams.repository.TeamPlaceholderRepository;
import com.sealhackathon.api.tracks.dto.request.CreateTrackRequest;
import com.sealhackathon.api.tracks.dto.response.TrackResponse;
import com.sealhackathon.api.tracks.entity.Track;
import com.sealhackathon.api.tracks.mapper.TrackMapper;
import com.sealhackathon.api.tracks.repository.TrackRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrackServiceImplCreateSequenceTest {

    @Mock private TrackRepository trackRepository;
    @Mock private HackathonRepository hackathonRepository;
    @Mock private RoundRepository roundRepository;
    @Mock private TrackMapper trackMapper;
    @Mock private AuditService auditService;
    @Mock private TeamPlaceholderRepository teamRepository;
    @Mock private MentorAssignmentRepository mentorAssignmentRepository;
    @Mock private JudgeAssignmentRepository judgeAssignmentRepository;
    @Mock private NotificationService notificationService;
    @Mock private EventRepository eventRepository;
    @Mock private CriteriaRepository criteriaRepository;

    @InjectMocks
    private TrackServiceImpl trackService;

    @Test
    void createByRound_withoutSequenceOrder_assignsMaxPlusOne() {
        Hackathon hackathon = Hackathon.builder().id(1).status(HackathonStatus.DRAFT).build();
        Round round = Round.builder().id(10).hackathon(hackathon).isFinal(false).build();
        CreateTrackRequest req = CreateTrackRequest.builder()
                .name("EVSWAP2")
                .minTeamSize(1)
                .maxTeamSize(5)
                .build();
        Track entity = Track.builder().round(round).name("EVSWAP2").sequenceOrder(2).build();
        Track saved = Track.builder().id(99).round(round).name("EVSWAP2").sequenceOrder(2).build();

        when(roundRepository.findById(10)).thenReturn(Optional.of(round));
        when(trackRepository.maxSequenceOrderByRoundId(10)).thenReturn(1);
        when(trackMapper.toEntity(req, round, 2)).thenReturn(entity);
        when(trackRepository.save(entity)).thenReturn(saved);
        when(trackMapper.toResponse(saved)).thenReturn(TrackResponse.builder().id(99).sequenceOrder(2).build());

        TrackResponse result = trackService.createByRound(10, req);

        assertThat(result.getSequenceOrder()).isEqualTo(2);
        verify(trackMapper).toEntity(req, round, 2);
    }

    @Test
    void createByRound_withTakenSequenceOrder_fallsBackToMaxPlusOne() {
        Hackathon hackathon = Hackathon.builder().id(1).status(HackathonStatus.DRAFT).build();
        Round round = Round.builder().id(10).hackathon(hackathon).isFinal(false).build();
        CreateTrackRequest req = CreateTrackRequest.builder()
                .name("EVSWAP2")
                .sequenceOrder(1)
                .minTeamSize(1)
                .maxTeamSize(5)
                .build();
        Track entity = Track.builder().round(round).sequenceOrder(2).build();
        Track saved = Track.builder().id(99).round(round).sequenceOrder(2).build();

        when(roundRepository.findById(10)).thenReturn(Optional.of(round));
        when(trackRepository.existsByRoundIdAndSequenceOrder(10, 1)).thenReturn(true);
        when(trackRepository.maxSequenceOrderByRoundId(10)).thenReturn(1);
        when(trackMapper.toEntity(req, round, 2)).thenReturn(entity);
        when(trackRepository.save(entity)).thenReturn(saved);
        when(trackMapper.toResponse(saved)).thenReturn(TrackResponse.builder().id(99).sequenceOrder(2).build());

        assertThat(trackService.createByRound(10, req).getSequenceOrder()).isEqualTo(2);
        verify(trackMapper).toEntity(req, round, 2);
    }

    @Test
    void createByRound_firstTrackWithoutSequenceOrder_assignsOne() {
        Hackathon hackathon = Hackathon.builder().id(1).status(HackathonStatus.DRAFT).build();
        Round round = Round.builder().id(10).hackathon(hackathon).isFinal(false).build();
        CreateTrackRequest req = CreateTrackRequest.builder()
                .name("EVSWAP1")
                .minTeamSize(1)
                .maxTeamSize(5)
                .build();
        Track entity = Track.builder().round(round).sequenceOrder(1).build();
        Track saved = Track.builder().id(1).round(round).sequenceOrder(1).build();

        when(roundRepository.findById(10)).thenReturn(Optional.of(round));
        when(trackRepository.maxSequenceOrderByRoundId(10)).thenReturn(0);
        when(trackMapper.toEntity(req, round, 1)).thenReturn(entity);
        when(trackRepository.save(entity)).thenReturn(saved);
        when(trackMapper.toResponse(saved)).thenReturn(TrackResponse.builder().id(1).sequenceOrder(1).build());

        assertThat(trackService.createByRound(10, req).getSequenceOrder()).isEqualTo(1);
    }
}
