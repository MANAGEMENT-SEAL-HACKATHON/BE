package com.sealhackathon.api.me.student.service.impl;

import com.sealhackathon.api.common.security.CurrentUserAccessor;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.me.student.dto.response.StudentProblemResponse;
import com.sealhackathon.api.me.support.StudentAccessGuard;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.rounds.support.RoundProblemStatementStorage;
import com.sealhackathon.api.storage.StoredObject;
import com.sealhackathon.api.teams.entity.Team;
import com.sealhackathon.api.teams.entity.TeamMember;
import com.sealhackathon.api.teams.entity.TeamRoundParticipation;
import com.sealhackathon.api.teams.entity.TeamRoundTrack;
import com.sealhackathon.api.teams.repository.TeamMemberRepository;
import com.sealhackathon.api.teams.repository.TeamRoundParticipationRepository;
import com.sealhackathon.api.teams.repository.TeamRoundTrackRepository;
import com.sealhackathon.api.teams.value_object.ParticipationStatus;
import com.sealhackathon.api.teams.value_object.TeamMemberStatus;
import com.sealhackathon.api.tracks.entity.Track;
import com.sealhackathon.api.tracks.support.TrackProblemStatementStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentPortalFinalProblemReuseTest {

    @Mock private CurrentUserAccessor currentUserAccessor;
    @Mock private StudentAccessGuard studentAccessGuard;
    @Mock private RoundRepository roundRepository;
    @Mock private TeamMemberRepository teamMemberRepository;
    @Mock private TeamRoundTrackRepository teamRoundTrackRepository;
    @Mock private TeamRoundParticipationRepository teamRoundParticipationRepository;
    @Mock private TrackProblemStatementStorage trackProblemStatementStorage;
    @Mock private RoundProblemStatementStorage roundProblemStatementStorage;

    @InjectMocks
    private StudentPortalServiceImpl service;

    /** CK-01 unit — final problem resolves prelim track PDF, not round PDF. */
    @Test
    void getRoundProblem_final_returnsPrelimTrackFilename() {
        Hackathon hackathon = Hackathon.builder().id(1).build();
        Round finalRound = Round.builder()
                .id(20)
                .isFinal(true)
                .hackathon(hackathon)
                .problemReleasedAt(LocalDateTime.now())
                .build();
        Round prelim = Round.builder().id(10).isFinal(false).hackathon(hackathon).build();
        Track trackA = Track.builder()
                .id(5)
                .name("RAG AI")
                .problemStatementOriginalFilename("rag-ai.pdf")
                .problemStatementStorageKey("tracks/rag.pdf")
                .build();
        Team team = Team.builder().id(100).hackathon(hackathon).teamName("Beta").build();
        TeamMember tm = TeamMember.builder().team(team).status(TeamMemberStatus.ACCEPTED).build();
        TeamRoundTrack trt = TeamRoundTrack.builder()
                .team(team)
                .track(trackA)
                .participationStatus(ParticipationStatus.ADVANCED)
                .build();

        when(roundRepository.findById(20)).thenReturn(Optional.of(finalRound));
        when(roundRepository.findByHackathon_IdOrderByExamAtAsc(1)).thenReturn(List.of(prelim, finalRound));
        when(currentUserAccessor.currentUserId()).thenReturn(9);
        when(teamMemberRepository.findByUser_IdAndStatus(9, TeamMemberStatus.ACCEPTED)).thenReturn(List.of(tm));
        when(teamRoundParticipationRepository.findByTeam_IdAndRound_Id(100, 20))
                .thenReturn(Optional.of(TeamRoundParticipation.builder().team(team).round(finalRound).build()));
        when(teamRoundTrackRepository.findByTeam_IdAndTrack_Round_Id(100, 10)).thenReturn(Optional.of(trt));

        try (MockedStatic<TrackProblemStatementStorage> trackStatic = mockStatic(TrackProblemStatementStorage.class)) {
            trackStatic.when(() -> TrackProblemStatementStorage.displayFilename(trackA)).thenReturn("rag-ai.pdf");
            trackStatic.when(() -> TrackProblemStatementStorage.hasProblemFile(trackA)).thenReturn(true);

            StudentProblemResponse response = service.getRoundProblem(20);

            assertThat(response.getProblemFilename()).isEqualTo("rag-ai.pdf");
            assertThat(response.getTrackId()).isEqualTo(5);
            assertThat(response.getTrackName()).isEqualTo("RAG AI");
            assertThat(response.getReleased()).isTrue();
            assertThat(response.getAvailable()).isTrue();
        }
    }

    @Test
    void getRoundProblem_final_missingTrackPdf_returnsUnavailableNotThrow() {
        Hackathon hackathon = Hackathon.builder().id(1).build();
        Round finalRound = Round.builder()
                .id(20)
                .isFinal(true)
                .hackathon(hackathon)
                .problemReleasedAt(LocalDateTime.now())
                .build();
        Round prelim = Round.builder().id(10).isFinal(false).hackathon(hackathon).build();
        Track trackA = Track.builder().id(5).name("RAG AI").build();
        Team team = Team.builder().id(100).hackathon(hackathon).teamName("Beta").build();
        TeamMember tm = TeamMember.builder().team(team).status(TeamMemberStatus.ACCEPTED).build();
        TeamRoundTrack trt = TeamRoundTrack.builder()
                .team(team)
                .track(trackA)
                .participationStatus(ParticipationStatus.ADVANCED)
                .build();

        when(roundRepository.findById(20)).thenReturn(Optional.of(finalRound));
        when(roundRepository.findByHackathon_IdOrderByExamAtAsc(1)).thenReturn(List.of(prelim, finalRound));
        when(currentUserAccessor.currentUserId()).thenReturn(9);
        when(teamMemberRepository.findByUser_IdAndStatus(9, TeamMemberStatus.ACCEPTED)).thenReturn(List.of(tm));
        when(teamRoundParticipationRepository.findByTeam_IdAndRound_Id(100, 20))
                .thenReturn(Optional.of(TeamRoundParticipation.builder().team(team).round(finalRound).build()));
        when(teamRoundTrackRepository.findByTeam_IdAndTrack_Round_Id(100, 10)).thenReturn(Optional.of(trt));

        try (MockedStatic<TrackProblemStatementStorage> trackStatic = mockStatic(TrackProblemStatementStorage.class)) {
            trackStatic.when(() -> TrackProblemStatementStorage.hasProblemFile(trackA)).thenReturn(false);

            StudentProblemResponse response = service.getRoundProblem(20);

            assertThat(response.getAvailable()).isFalse();
            assertThat(response.getReleased()).isTrue();
            assertThat(response.getTrackId()).isEqualTo(5);
            assertThat(response.getProblemFilename()).isNull();
        }
    }

    @Test
    void downloadRoundProblemStatement_final_usesTrackStorageNotRound() {
        Hackathon hackathon = Hackathon.builder().id(1).build();
        Round finalRound = Round.builder()
                .id(20)
                .isFinal(true)
                .hackathon(hackathon)
                .problemReleasedAt(LocalDateTime.now())
                .build();
        Round prelim = Round.builder().id(10).isFinal(false).hackathon(hackathon).build();
        Track trackA = Track.builder()
                .id(5)
                .name("RAG AI")
                .problemStatementOriginalFilename("rag-ai.pdf")
                .problemStatementStorageKey("tracks/rag.pdf")
                .build();
        Team team = Team.builder().id(100).hackathon(hackathon).teamName("Beta").build();
        TeamMember tm = TeamMember.builder().team(team).status(TeamMemberStatus.ACCEPTED).build();
        TeamRoundTrack trt = TeamRoundTrack.builder()
                .team(team)
                .track(trackA)
                .participationStatus(ParticipationStatus.ADVANCED)
                .build();

        when(roundRepository.findById(20)).thenReturn(Optional.of(finalRound));
        when(roundRepository.findByHackathon_IdOrderByExamAtAsc(1)).thenReturn(List.of(prelim, finalRound));
        when(currentUserAccessor.currentUserId()).thenReturn(9);
        when(teamMemberRepository.findByUser_IdAndStatus(9, TeamMemberStatus.ACCEPTED)).thenReturn(List.of(tm));
        when(teamRoundParticipationRepository.findByTeam_IdAndRound_Id(100, 20))
                .thenReturn(Optional.of(TeamRoundParticipation.builder().team(team).round(finalRound).build()));
        when(teamRoundTrackRepository.findByTeam_IdAndTrack_Round_Id(100, 10)).thenReturn(Optional.of(trt));
        when(trackProblemStatementStorage.load(trackA))
                .thenReturn(new StoredObject(new java.io.ByteArrayInputStream("pdf".getBytes()), "application/pdf", 3));

        try (MockedStatic<TrackProblemStatementStorage> trackStatic = mockStatic(TrackProblemStatementStorage.class)) {
            trackStatic.when(() -> TrackProblemStatementStorage.hasStoredFile(trackA)).thenReturn(true);
            trackStatic.when(() -> TrackProblemStatementStorage.hasProblemFile(trackA)).thenReturn(true);
            trackStatic.when(() -> TrackProblemStatementStorage.displayFilename(trackA)).thenReturn("rag-ai.pdf");

            var resource = service.downloadRoundProblemStatement(20);
            assertThat(resource).isNotNull();
            assertThat(resource.getFilename()).isEqualTo("rag-ai.pdf");
        }
    }
}
