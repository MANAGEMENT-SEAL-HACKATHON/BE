package com.sealhackathon.api.judge_assignments.service.impl;

import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.common.exception.ConflictException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.common.security.CurrentUserAccessor;
import com.sealhackathon.api.config.AppProperties;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.support.HackathonArchiveGuard;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.invitations.service.EmailService;
import com.sealhackathon.api.judge_assignments.dto.request.CreateJudgeAssignmentRequest;
import com.sealhackathon.api.judge_assignments.entity.JudgeAssignment;
import com.sealhackathon.api.judge_assignments.mapper.JudgeAssignmentMapper;
import com.sealhackathon.api.judge_assignments.repository.JudgeAssignmentRepository;
import com.sealhackathon.api.judge_assignments.value_object.JudgeAssignmentType;
import com.sealhackathon.api.mentors.repository.MentorAssignmentRepository;
import com.sealhackathon.api.mentors.repository.MentorTeamAssignmentRepository;
import com.sealhackathon.api.notifications.service.NotificationService;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.rounds.value_object.RoundType;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JudgeAssignmentServiceImplMultiTrackTest {

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
    void assignToTrack_rejectsSecondTrackInSamePrelimRound() {
        User judge = approved(20, UserRole.JUDGE, UserType.INTERNAL);
        Round round = prelimRound(5);
        Track track2 = track(2, round);

        when(userRepository.findById(20)).thenReturn(Optional.of(judge));
        when(trackRepository.findById(2)).thenReturn(Optional.of(track2));
        when(mentorAssignmentRepository.existsByMentorIdAndTrackId(20, 2)).thenReturn(false);
        when(judgeAssignmentRepository.existsByJudgeIdAndTrackId(20, 2)).thenReturn(false);
        when(judgeAssignmentRepository.existsByJudgeIdAndRoundScope(20, 5)).thenReturn(true);

        ConflictException ex = assertThrows(ConflictException.class, () ->
                judgeAssignmentService.assign(
                        CreateJudgeAssignmentRequest.builder().judgeId(20).trackId(2).build()));
        assertEquals(ErrorCode.JUDGE_ASSIGN_DUPLICATE, ex.getCode());
        verify(judgeAssignmentRepository, never()).save(any());
    }

    @Test
    void assignToTrack_allowsAfterRemovedFromOtherTrackInSameRound() {
        User judge = approved(21, UserRole.JUDGE, UserType.INTERNAL);
        Round round = prelimRound(6);
        Track track2 = track(12, round);

        when(userRepository.findById(21)).thenReturn(Optional.of(judge));
        when(trackRepository.findById(12)).thenReturn(Optional.of(track2));
        when(mentorAssignmentRepository.existsByMentorIdAndTrackId(21, 12)).thenReturn(false);
        when(judgeAssignmentRepository.existsByJudgeIdAndTrackId(21, 12)).thenReturn(false);
        when(judgeAssignmentRepository.existsByJudgeIdAndRoundScope(21, 6)).thenReturn(false);
        when(judgeAssignmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(judgeAssignmentMapper.toResponse(any())).thenReturn(null);

        judgeAssignmentService.assign(
                CreateJudgeAssignmentRequest.builder().judgeId(21).trackId(12).build());

        verify(judgeAssignmentRepository).save(any(JudgeAssignment.class));
        verify(judgeAssignmentRepository).existsByJudgeIdAndRoundScope(eq(21), eq(6));
    }

    private static User approved(int id, UserRole role, UserType type) {
        return User.builder().id(id).role(role).userType(type).status(UserStatus.APPROVED)
                .email("u" + id + "@test.vn").fullName("U" + id).build();
    }

    private static Round prelimRound(int id) {
        Hackathon h = Hackathon.builder().id(1).status(HackathonStatus.ONGOING).build();
        return Round.builder().id(id).isFinal(false).roundType(RoundType.PRELIMINARY).hackathon(h).build();
    }

    private static Track track(int id, Round round) {
        return Track.builder().id(id).name("T" + id).status(TrackStatus.OPEN).round(round).build();
    }
}
