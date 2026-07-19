package com.sealhackathon.api.judge_assignments.service.impl;

import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.common.security.CurrentUserAccessor;
import com.sealhackathon.api.config.AppProperties;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.support.HackathonArchiveGuard;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.invitations.service.EmailService;
import com.sealhackathon.api.judge_assignments.dto.request.CreateJudgeAssignmentRequest;
import com.sealhackathon.api.judge_assignments.mapper.JudgeAssignmentMapper;
import com.sealhackathon.api.judge_assignments.repository.JudgeAssignmentRepository;
import com.sealhackathon.api.judge_assignments.value_object.JudgeAssignmentType;
import com.sealhackathon.api.mentors.entity.MentorTeamAssignment;
import com.sealhackathon.api.mentors.repository.MentorAssignmentRepository;
import com.sealhackathon.api.mentors.repository.MentorTeamAssignmentRepository;
import com.sealhackathon.api.notifications.service.NotificationService;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.rounds.value_object.RoundType;
import com.sealhackathon.api.teams.entity.Team;
import com.sealhackathon.api.teams.entity.TeamRoundTrack;
import com.sealhackathon.api.teams.repository.TeamRoundTrackRepository;
import com.sealhackathon.api.tracks.entity.Track;
import com.sealhackathon.api.tracks.repository.TrackRepository;
import com.sealhackathon.api.tracks.value_object.TrackStatus;
import com.sealhackathon.api.users.entity.User;
import com.sealhackathon.api.users.repository.UserRepository;
import com.sealhackathon.api.users.value_object.UserRole;
import com.sealhackathon.api.users.value_object.UserStatus;
import com.sealhackathon.api.users.value_object.UserType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JudgeAssignmentRoleGuardTest {

    @Mock private JudgeAssignmentRepository judgeAssignmentRepository;
    @Mock private MentorAssignmentRepository mentorAssignmentRepository;
    @Mock private MentorTeamAssignmentRepository mentorTeamAssignmentRepository;
    @Mock private TeamRoundTrackRepository teamRoundTrackRepository;
    @Mock private UserRepository userRepository;
    @Mock private RoundRepository roundRepository;
    @Mock private TrackRepository trackRepository;
    @Mock private JudgeAssignmentMapper judgeAssignmentMapper;
    @Mock private AuditService auditService;
    @Mock private CurrentUserAccessor currentUserAccessor;
    @Mock private NotificationService notificationService;
    @Mock private EmailService emailService;
    @Mock private AppProperties appProperties;
    @Spy private HackathonArchiveGuard archiveGuard = new HackathonArchiveGuard();

    @InjectMocks private JudgeAssignmentServiceImpl judgeAssignmentService;

    @Test
    void assignToTrack_rejectsExternalJudge() {
        User external = approved(10, UserRole.JUDGE, UserType.EXTERNAL);
        Round round = prelimRound();
        Track track = track(1, round);

        when(userRepository.findById(10)).thenReturn(Optional.of(external));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                judgeAssignmentService.assign(
                        CreateJudgeAssignmentRequest.builder().judgeId(10).trackId(1).build()));
        assertEquals(ErrorCode.EXTERNAL_JUDGE_NOT_ALLOWED_IN_PRELIM, ex.getCode());
    }

    @Test
    void assignFinalRoundG4_rejectsInternalAsFinalExternal() {
        User internal = approved(11, UserRole.JUDGE, UserType.INTERNAL);

        when(userRepository.findById(11)).thenReturn(Optional.of(internal));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                judgeAssignmentService.assignFinalRoundG4(99, 11, JudgeAssignmentType.FINAL_EXTERNAL));
        assertEquals(ErrorCode.INVALID_ASSIGNMENT_TYPE, ex.getCode());
    }

    @Test
    void assignFinalRoundG4_acceptsExternalAsFinalExternal() {
        User external = approved(12, UserRole.JUDGE, UserType.EXTERNAL);
        Round finalRound = finalRound(99);

        when(userRepository.findById(12)).thenReturn(Optional.of(external));
        when(roundRepository.findById(99)).thenReturn(Optional.of(finalRound));
        when(judgeAssignmentRepository.existsByJudgeIdAndRoundId(12, 99)).thenReturn(false);
        when(judgeAssignmentRepository.hasPreliminaryTrackAssignmentInHackathon(12, 1)).thenReturn(false);
        when(judgeAssignmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(judgeAssignmentMapper.toResponse(any())).thenReturn(null);

        assertDoesNotThrow(() ->
                judgeAssignmentService.assignFinalRoundG4(99, 12, JudgeAssignmentType.FINAL_EXTERNAL));
    }

    @Test
    void assignFinalRoundG4_rejectsHead() {
        User internal = approved(13, UserRole.JUDGE, UserType.INTERNAL);

        when(userRepository.findById(13)).thenReturn(Optional.of(internal));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                judgeAssignmentService.assignFinalRoundG4(99, 13, JudgeAssignmentType.HEAD));
        assertEquals(ErrorCode.INVALID_ASSIGNMENT_TYPE, ex.getCode());
    }

    @Test
    void assignFinalRoundG4_acceptsInternalAsNormal() {
        User internal = approved(13, UserRole.JUDGE, UserType.INTERNAL);
        Round finalRound = finalRound(99);

        when(userRepository.findById(13)).thenReturn(Optional.of(internal));
        when(roundRepository.findById(99)).thenReturn(Optional.of(finalRound));
        when(judgeAssignmentRepository.existsByJudgeIdAndRoundId(13, 99)).thenReturn(false);
        when(judgeAssignmentRepository.hasPreliminaryTrackAssignmentInHackathon(13, 1)).thenReturn(false);
        when(judgeAssignmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(judgeAssignmentMapper.toResponse(any())).thenReturn(null);

        assertDoesNotThrow(() ->
                judgeAssignmentService.assignFinalRoundG4(99, 13, JudgeAssignmentType.NORMAL));
    }

    @Test
    void assignToTrack_rejectsHead() {
        User internal = approved(14, UserRole.JUDGE, UserType.INTERNAL);

        when(userRepository.findById(14)).thenReturn(Optional.of(internal));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                judgeAssignmentService.assign(
                        CreateJudgeAssignmentRequest.builder()
                                .judgeId(14)
                                .trackId(1)
                                .assignmentType(JudgeAssignmentType.HEAD)
                                .build()));
        assertEquals(ErrorCode.INVALID_ASSIGNMENT_TYPE, ex.getCode());
    }

    @Test
    void assignToTrack_acceptsInternalAsNormal() {
        User internal = approved(14, UserRole.JUDGE, UserType.INTERNAL);
        Round round = prelimRound();
        Track track = track(1, round);

        when(userRepository.findById(14)).thenReturn(Optional.of(internal));
        when(trackRepository.findById(1)).thenReturn(Optional.of(track));
        when(mentorAssignmentRepository.existsByMentorIdAndTrackId(14, 1)).thenReturn(false);
        when(judgeAssignmentRepository.existsByJudgeIdAndTrackId(14, 1)).thenReturn(false);
        when(judgeAssignmentRepository.existsByJudgeIdAndRoundScope(14, 5)).thenReturn(false);
        when(judgeAssignmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(judgeAssignmentMapper.toResponse(any())).thenReturn(null);
        when(currentUserAccessor.currentUserId()).thenReturn(1);

        assertDoesNotThrow(() ->
                judgeAssignmentService.assign(
                        CreateJudgeAssignmentRequest.builder()
                                .judgeId(14)
                                .trackId(1)
                                .assignmentType(JudgeAssignmentType.NORMAL)
                                .build()));
    }

    @Test
    void assignToTrack_rejectsMentorOfTeamInTrack() {
        User internal = approved(16, UserRole.JUDGE, UserType.INTERNAL);
        Round round = prelimRound();
        Track track = track(1, round);
        Team team = Team.builder().id(50).build();

        when(userRepository.findById(16)).thenReturn(Optional.of(internal));
        when(trackRepository.findById(1)).thenReturn(Optional.of(track));
        when(mentorAssignmentRepository.existsByMentorIdAndTrackId(16, 1)).thenReturn(false);
        when(mentorTeamAssignmentRepository.findByMentor_Id(16)).thenReturn(java.util.List.of(
                MentorTeamAssignment.builder().mentor(internal).team(team).round(round).build()));
        when(teamRoundTrackRepository.findByTeam_IdAndTrack_Id(50, 1)).thenReturn(
                Optional.of(new TeamRoundTrack()));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                judgeAssignmentService.assign(
                        CreateJudgeAssignmentRequest.builder().judgeId(16).trackId(1).build()));
        assertEquals(ErrorCode.CONFLICT_MENTOR_JUDGE_SAME_TRACK, ex.getCode());
    }

    @Test
    void assignToTrack_rejectsFinalExternal() {
        User internal = approved(15, UserRole.JUDGE, UserType.INTERNAL);

        when(userRepository.findById(15)).thenReturn(Optional.of(internal));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                judgeAssignmentService.assign(
                        CreateJudgeAssignmentRequest.builder()
                                .judgeId(15)
                                .trackId(1)
                                .assignmentType(JudgeAssignmentType.FINAL_EXTERNAL)
                                .build()));
        assertEquals(ErrorCode.INVALID_ASSIGNMENT_TYPE, ex.getCode());
    }

    private static User approved(int id, UserRole role, UserType type) {
        return User.builder().id(id).role(role).userType(type).status(UserStatus.APPROVED)
                .email("u" + id + "@test.vn").fullName("U" + id).build();
    }

    private static Round prelimRound() {
        Hackathon h = Hackathon.builder().id(1).status(HackathonStatus.ONGOING).build();
        return Round.builder().id(5).isFinal(false).roundType(RoundType.PRELIMINARY).hackathon(h).build();
    }

    private static Round finalRound(int id) {
        Hackathon h = Hackathon.builder().id(1).status(HackathonStatus.ONGOING).name("H").build();
        return Round.builder().id(id).isFinal(true).name("CK").roundType(RoundType.FINAL).hackathon(h).build();
    }

    private static Track track(int id, Round round) {
        return Track.builder().id(id).name("T" + id).status(TrackStatus.OPEN).round(round).build();
    }
}
