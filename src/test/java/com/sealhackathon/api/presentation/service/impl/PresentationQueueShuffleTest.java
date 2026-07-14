package com.sealhackathon.api.presentation.service.impl;

import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.common.security.CurrentUserAccessor;
import com.sealhackathon.api.hackathons.repository.HackathonRegistrationRepository;
import com.sealhackathon.api.events.repository.JudgeSubmissionScoringConfirmationRepository;
import com.sealhackathon.api.events.entity.PresentationSlot;
import com.sealhackathon.api.events.repository.PresentationSlotRepository;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.live_scoring.PresentationQueuePublisher;
import com.sealhackathon.api.presentation.dto.request.PresentationShuffleRequest;
import com.sealhackathon.api.presentation.dto.response.PresentationShuffleResponse;
import com.sealhackathon.api.presentation.guard.PresentationControllerGuard;
import com.sealhackathon.api.presentation.guard.PresentationForceAdvanceAckGuard;
import com.sealhackathon.api.presentation.support.PresentationDurationResolver;
import com.sealhackathon.api.presentation.support.PresentationNextScoringGuard;
import com.sealhackathon.api.presentation.support.RoundPhaseResolver;
import com.sealhackathon.api.presentation.value_object.PresentationQueueStatus;
import com.sealhackathon.api.presentation.value_object.RoundPhase;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.submissions.entity.Submission;
import com.sealhackathon.api.submissions.repository.SubmissionRepository;
import com.sealhackathon.api.submissions.value_object.SubmissionStatus;
import com.sealhackathon.api.teams.repository.TeamRoundParticipationRepository;
import com.sealhackathon.api.teams.entity.Team;
import com.sealhackathon.api.tracks.entity.Track;
import com.sealhackathon.api.tracks.repository.TrackRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PresentationQueueShuffleTest {

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
    @Mock private PresentationNextScoringGuard nextScoringGuard;
    @Mock private JudgeSubmissionScoringConfirmationRepository scoringConfirmationRepository;
    @Mock private RoundPhaseResolver roundPhaseResolver;
    @Mock private PresentationForceAdvanceAckGuard forceAdvanceAckGuard;
    @Mock private HackathonRegistrationRepository hackathonRegistrationRepository;

    @InjectMocks
    private PresentationQueueServiceImpl queueService;

    @Test
    void shuffle_createsSlotsFromGradableSubmissions() {
        Round round = Round.builder()
                .id(5)
                .isFinal(false)
                .isActive(true)
                .examAt(LocalDateTime.now().minusHours(2))
                .submissionDeadline(LocalDateTime.now().minusMinutes(5))
                .hackathon(Hackathon.builder().id(1).build())
                .build();
        Track track = Track.builder().id(10).name("AI").round(round).build();
        Submission s1 = gradableSubmission(1, track);
        Submission s2 = gradableSubmission(2, track);

        when(roundRepository.findById(5)).thenReturn(Optional.of(round));
        when(hackathonRepository.findById(1)).thenReturn(Optional.of(round.getHackathon()));
        when(trackRepository.findByRoundIdOrderBySequenceOrderAsc(5)).thenReturn(List.of(track));
        when(submissionRepository.findByTrack_Round_Id(5)).thenReturn(List.of(s1, s2));
        when(durationResolver.slotMinutes(track, round)).thenReturn(15);
        when(durationResolver.presentationMinutes(track, round)).thenReturn(10);
        when(durationResolver.qaMinutes(track, round)).thenReturn(5);
        when(presentationSlotRepository.findByRound_IdAndTrack_IdOrderBySequenceOrderAsc(5, 10))
                .thenReturn(List.of());
        when(roundPhaseResolver.resolve(round)).thenReturn(RoundPhase.JUDGING);
        doNothing().when(controllerGuard).requireControllerForTrack(any(), any(), any());

        PresentationShuffleResponse response = queueService.shuffle(
                PresentationShuffleRequest.builder().roundId(5).trackIds(List.of(10)).build());

        assertThat(response.getTracks()).hasSize(1);
        assertThat(response.getTracks().get(0).getSlotCount()).isEqualTo(2);

        ArgumentCaptor<PresentationSlot> captor = ArgumentCaptor.forClass(PresentationSlot.class);
        verify(presentationSlotRepository, org.mockito.Mockito.atLeast(2)).save(captor.capture());
        verify(scoringConfirmationRepository).deleteByTrackScope(5, 10);
        assertThat(captor.getAllValues())
                .extracting(PresentationSlot::getSubmission)
                .extracting(Submission::getId)
                .containsExactlyInAnyOrder(1, 2);
        assertThat(captor.getAllValues().stream()
                .filter(s -> s.getQueueStatus() == PresentationQueueStatus.PRESENTING)
                .count()).isEqualTo(1);
    }

    private static Submission gradableSubmission(int id, Track track) {
        return Submission.builder()
                .id(id)
                .status(SubmissionStatus.SUBMITTED)
                .team(Team.builder().id(id + 100).teamName("T" + id).build())
                .track(track)
                .round(track.getRound())
                .build();
    }
}
