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
import com.sealhackathon.api.mentors.repository.MentorAssignmentRepository;
import com.sealhackathon.api.notifications.service.NotificationService;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.rounds.value_object.RoundType;
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
    void assignFinalRoundG4_acceptsInternalAsHead() {
        User internal = approved(13, UserRole.JUDGE, UserType.INTERNAL);
        Round finalRound = finalRound(99);

        when(userRepository.findById(13)).thenReturn(Optional.of(internal));
        when(roundRepository.findById(99)).thenReturn(Optional.of(finalRound));
        when(judgeAssignmentRepository.existsByJudgeIdAndRoundId(13, 99)).thenReturn(false);
        when(judgeAssignmentRepository.hasPreliminaryTrackAssignmentInHackathon(13, 1)).thenReturn(false);
        when(judgeAssignmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(judgeAssignmentMapper.toResponse(any())).thenReturn(null);

        assertDoesNotThrow(() ->
                judgeAssignmentService.assignFinalRoundG4(99, 13, JudgeAssignmentType.HEAD));
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
