package com.sealhackathon.api.tracks.service.impl;

import com.sealhackathon.api.common.audit.AuditAction;
import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.criteria.repository.CriteriaRepository;
import com.sealhackathon.api.events.repository.EventRepository;
import com.sealhackathon.api.events.value_object.EventType;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.hackathons.support.HackathonArchiveGuard;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.judge_assignments.repository.JudgeAssignmentRepository;
import com.sealhackathon.api.mentors.repository.MentorAssignmentRepository;
import com.sealhackathon.api.notifications.service.NotificationService;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.teams.repository.TeamRepository;
import com.sealhackathon.api.teams.support.HackathonTeamSizeResolver;
import com.sealhackathon.api.tracks.dto.request.UpdateTrackRequest;
import com.sealhackathon.api.tracks.dto.response.TrackResponse;
import com.sealhackathon.api.tracks.entity.Track;
import com.sealhackathon.api.tracks.mapper.TrackMapper;
import com.sealhackathon.api.tracks.repository.TrackRepository;
import com.sealhackathon.api.tracks.service.TrackService;
import com.sealhackathon.api.tracks.value_object.TrackStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrackServiceImplUpdateTest {

    @Mock TrackRepository trackRepository;
    @Mock HackathonRepository hackathonRepository;
    @Mock RoundRepository roundRepository;
    @Spy TrackMapper trackMapper = new TrackMapper();
    @Mock AuditService auditService;
    @Mock TeamRepository teamRepository;
    @Mock MentorAssignmentRepository mentorAssignmentRepository;
    @Mock JudgeAssignmentRepository judgeAssignmentRepository;
    @Mock NotificationService notificationService;
    @Mock EventRepository eventRepository;
    @Mock CriteriaRepository criteriaRepository;
    @Spy HackathonArchiveGuard archiveGuard = new HackathonArchiveGuard();
    @Mock HackathonTeamSizeResolver teamSizeResolver;

    @InjectMocks TrackServiceImpl trackService;

    @Test
    void update_assignTopicFirstTime_withKickoffEvent_succeeds() {
        Hackathon hackathon = Hackathon.builder().id(1).status(HackathonStatus.DRAFT).build();
        Round round = Round.builder().id(10).hackathon(hackathon).build();
        Track track = Track.builder()
                .id(9)
                .name("Track A")
                .status(TrackStatus.OPEN)
                .round(round)
                .minTeamSize(3)
                .maxTeamSize(5)
                .build();

        when(trackRepository.findById(9)).thenReturn(Optional.of(track));
        when(eventRepository.existsByHackathonIdAndType(1, EventType.KICKOFF)).thenReturn(true);
        when(trackRepository.save(track)).thenAnswer(inv -> inv.getArgument(0));

        UpdateTrackRequest req = UpdateTrackRequest.builder()
                .name("ASUS TUF GAMING A15")
                .topic("AI")
                .minTeamSize(3)
                .maxTeamSize(5)
                .maxTeams(8)
                .maxTeamsPerGroup(8)
                .status(TrackStatus.OPEN)
                .build();

        TrackService.UpdateResult result = trackService.update(9, req);

        assertThat(result.track().getTopic()).isEqualTo("AI");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> auditCaptor = ArgumentCaptor.forClass(Map.class);
        verify(auditService).log(
                eq(AuditAction.TRACK_TOPIC_UPDATE),
                eq("tracks"),
                eq(9),
                auditCaptor.capture());
        assertThat(auditCaptor.getValue()).containsEntry("oldTopic", null);
        assertThat(auditCaptor.getValue()).containsEntry("newTopic", "AI");
    }

    @Test
    void update_presentationAndQaMinutes_persistedInResponse() {
        Hackathon hackathon = Hackathon.builder().id(1).status(HackathonStatus.DRAFT).build();
        Round round = Round.builder().id(10).hackathon(hackathon).build();
        Track track = Track.builder()
                .id(9)
                .name("Track A")
                .status(TrackStatus.OPEN)
                .round(round)
                .minTeamSize(3)
                .maxTeamSize(5)
                .presentationMinutes(10)
                .qaMinutes(5)
                .build();

        when(trackRepository.findById(9)).thenReturn(Optional.of(track));
        when(trackRepository.save(track)).thenAnswer(inv -> inv.getArgument(0));

        UpdateTrackRequest req = UpdateTrackRequest.builder()
                .name("Track A")
                .minTeamSize(3)
                .maxTeamSize(5)
                .maxTeams(8)
                .maxTeamsPerGroup(8)
                .status(TrackStatus.OPEN)
                .presentationMinutes(15)
                .qaMinutes(7)
                .build();

        TrackService.UpdateResult result = trackService.update(9, req);

        assertThat(result.track().getPresentationMinutes()).isEqualTo(15);
        assertThat(result.track().getQaMinutes()).isEqualTo(7);
        assertThat(track.getPresentationMinutes()).isEqualTo(15);
        assertThat(track.getQaMinutes()).isEqualTo(7);
    }

    @Test
    void update_assignTopic_withoutKickoffEvent_throwsInvalidState() {
        Hackathon hackathon = Hackathon.builder().id(1).status(HackathonStatus.DRAFT).build();
        Round round = Round.builder().id(10).hackathon(hackathon).build();
        Track track = Track.builder()
                .id(9)
                .name("Track A")
                .status(TrackStatus.OPEN)
                .round(round)
                .minTeamSize(3)
                .maxTeamSize(5)
                .build();

        when(trackRepository.findById(9)).thenReturn(Optional.of(track));
        when(eventRepository.existsByHackathonIdAndType(1, EventType.KICKOFF)).thenReturn(false);

        UpdateTrackRequest req = UpdateTrackRequest.builder()
                .name("Track A")
                .topic("AI")
                .minTeamSize(3)
                .maxTeamSize(5)
                .maxTeams(8)
                .maxTeamsPerGroup(8)
                .status(TrackStatus.OPEN)
                .build();

        assertThatThrownBy(() -> trackService.update(9, req))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("KICKOFF")
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getCode())
                        .isEqualTo(ErrorCode.INVALID_STATE));
    }
}
