package com.sealhackathon.api.presentation.service.impl;

import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.common.security.CurrentUserAccessor;
import com.sealhackathon.api.events.entity.PresentationSlot;
import com.sealhackathon.api.events.repository.PresentationSlotRepository;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.live_scoring.PresentationQueuePublisher;
import com.sealhackathon.api.presentation.dto.response.PresentationQueueResponse;
import com.sealhackathon.api.presentation.guard.PresentationControllerGuard;
import com.sealhackathon.api.presentation.support.PresentationDurationResolver;
import com.sealhackathon.api.presentation.value_object.PresentationQueueStatus;
import com.sealhackathon.api.presentation.value_object.PresentationTimerPhase;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.submissions.entity.Submission;
import com.sealhackathon.api.submissions.repository.SubmissionRepository;
import com.sealhackathon.api.teams.repository.TeamRoundParticipationRepository;
import com.sealhackathon.api.teams.entity.Team;
import com.sealhackathon.api.tracks.entity.Track;
import com.sealhackathon.api.tracks.repository.TrackRepository;
import com.sealhackathon.api.common.security.CurrentUserStub;
import com.sealhackathon.api.users.value_object.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PresentationQueueAnonymityTest {

    @Mock private RoundRepository roundRepository;
    @Mock private HackathonRepository hackathonRepository;
    @Mock private TrackRepository trackRepository;
    @Mock private SubmissionRepository submissionRepository;
    @Mock private TeamRoundParticipationRepository teamRoundParticipationRepository;
    @Mock private PresentationSlotRepository presentationSlotRepository;
    @Mock private PresentationDurationResolver durationResolver;
    @Mock private PresentationControllerGuard controllerGuard;
    @Mock private AuditService auditService;
    @Mock private PresentationQueuePublisher queuePublisher;
    @Mock private CurrentUserAccessor currentUserAccessor;

    @InjectMocks
    private PresentationQueueServiceImpl queueService;

    @Test
    void judgeQueue_hidesTeamIdentity() {
        Round round = Round.builder().id(5).isFinal(false)
                .hackathon(Hackathon.builder().id(1).build()).build();
        Track track = Track.builder().id(10).name("AI").round(round).presentationShuffled(true).build();
        Team team = Team.builder().id(77).teamName("Hidden").build();
        Submission submission = Submission.builder().id(31).team(team).build();
        PresentationSlot slot = PresentationSlot.builder()
                .submission(submission)
                .team(team)
                .sequenceOrder(1)
                .queueStatus(PresentationQueueStatus.PRESENTING)
                .timerPhase(PresentationTimerPhase.IDLE)
                .startsAt(LocalDateTime.now())
                .endsAt(LocalDateTime.now().plusMinutes(10))
                .pausedAccumulatedSeconds(0)
                .build();

        when(currentUserAccessor.currentUser()).thenReturn(
                CurrentUserStub.builder().userId(9).role(UserRole.JUDGE).build());
        when(roundRepository.findById(5)).thenReturn(Optional.of(round));
        when(hackathonRepository.findById(1)).thenReturn(Optional.of(round.getHackathon()));
        when(trackRepository.findByRoundIdOrderBySequenceOrderAsc(5)).thenReturn(List.of(track));
        when(presentationSlotRepository.findByRound_IdAndTrack_IdOrderBySequenceOrderAsc(5, 10))
                .thenReturn(List.of(slot));
        when(durationResolver.presentationMinutes(track, round)).thenReturn(10);
        when(durationResolver.qaMinutes(track, round)).thenReturn(5);

        PresentationQueueResponse queue = queueService.getQueue(5, 10);
        var item = queue.getTracks().get(0).getItems().get(0);

        assertThat(item.getDisplayCode()).isEqualTo("#31");
        assertThat(item.getTeamId()).isNull();
        assertThat(item.getTeamName()).isNull();
    }
}
