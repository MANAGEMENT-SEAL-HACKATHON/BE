package com.sealhackathon.api.personnel;

import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.common.security.CurrentUserAccessor;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.support.HackathonArchiveGuard;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.judge_assignments.dto.request.CreateJudgeAssignmentRequest;
import com.sealhackathon.api.judge_assignments.mapper.JudgeAssignmentMapper;
import com.sealhackathon.api.judge_assignments.repository.JudgeAssignmentRepository;
import com.sealhackathon.api.judge_assignments.service.impl.JudgeAssignmentServiceImpl;
import com.sealhackathon.api.mentor_assignments.dto.request.CreateMentorAssignmentRequest;
import com.sealhackathon.api.mentor_assignments.dto.response.MentorAssignmentResponse;
import com.sealhackathon.api.mentor_assignments.mapper.MentorAssignmentMapper;
import com.sealhackathon.api.mentor_assignments.repository.MentorAssignmentRepository;
import com.sealhackathon.api.mentor_assignments.service.impl.MentorAssignmentServiceImpl;
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

/**
 * MF-02 §14 — cùng user: Judge track A + Mentor track B (cùng round); cấm cùng track.
 */
@ExtendWith(MockitoExtension.class)
class PersonnelAssignmentCrossTrackTest {

    @Mock private JudgeAssignmentRepository judgeAssignmentRepository;
    @Mock private MentorAssignmentRepository mentorAssignmentRepository;
    @Mock private UserRepository userRepository;
    @Mock private RoundRepository roundRepository;
    @Mock private TrackRepository trackRepository;
    @Mock private JudgeAssignmentMapper judgeAssignmentMapper;
    @Mock private MentorAssignmentMapper mentorAssignmentMapper;
    @Mock private AuditService auditService;
    @Mock private CurrentUserAccessor currentUserAccessor;
    @Mock private NotificationService notificationService;
    @Spy private HackathonArchiveGuard archiveGuard = new HackathonArchiveGuard();

    @InjectMocks private JudgeAssignmentServiceImpl judgeAssignmentService;
    @InjectMocks private MentorAssignmentServiceImpl mentorAssignmentService;

    @Test
    void judgeAssign_acceptsUserWithRoleMentor_onDifferentTrack() {
        User mentorAccount = approvedUser(10, UserRole.MENTOR);
        Round round = prelimRound();
        Track trackA = track(1, round, "A");
        Track trackB = track(2, round, "B");

        when(userRepository.findById(10)).thenReturn(Optional.of(mentorAccount));
        when(trackRepository.findById(1)).thenReturn(Optional.of(trackA));
        when(mentorAssignmentRepository.existsByMentorIdAndTrackId(10, 1)).thenReturn(false);
        when(judgeAssignmentRepository.existsByJudgeIdAndTrackId(10, 1)).thenReturn(false);
        when(judgeAssignmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(judgeAssignmentMapper.toResponse(any())).thenReturn(null);

        assertDoesNotThrow(() -> judgeAssignmentService.assign(
                CreateJudgeAssignmentRequest.builder().judgeId(10).trackId(1).build()));
    }

    @Test
    void mentorAssign_acceptsUserWithRoleJudge_onDifferentTrack() {
        User judgeAccount = approvedUser(20, UserRole.JUDGE);
        Round round = prelimRound();
        Track trackB = track(2, round, "B");

        when(userRepository.findById(20)).thenReturn(Optional.of(judgeAccount));
        when(trackRepository.findById(2)).thenReturn(Optional.of(trackB));
        when(mentorAssignmentRepository.existsByMentorIdAndTrackId(20, 2)).thenReturn(false);
        when(judgeAssignmentRepository.existsByJudgeIdAndTrackId(20, 2)).thenReturn(false);
        when(judgeAssignmentRepository.existsFinalExternalJudgeInHackathonOfTrack(any(), any(), any()))
                .thenReturn(false);
        when(mentorAssignmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(mentorAssignmentMapper.toResponse(any()))
                .thenReturn(MentorAssignmentResponse.builder().id(1).mentorId(20).trackId(2).build());

        assertDoesNotThrow(() -> mentorAssignmentService.assign(
                CreateMentorAssignmentRequest.builder().mentorId(20).trackId(2).build()));
    }

    @Test
    void judgeAssign_blocksWhenSameUserIsMentorOnSameTrack() {
        User user = approvedUser(10, UserRole.MENTOR);
        Round round = prelimRound();
        Track trackA = track(1, round, "A");

        when(userRepository.findById(10)).thenReturn(Optional.of(user));
        when(trackRepository.findById(1)).thenReturn(Optional.of(trackA));
        when(mentorAssignmentRepository.existsByMentorIdAndTrackId(10, 1)).thenReturn(true);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                judgeAssignmentService.assign(
                        CreateJudgeAssignmentRequest.builder().judgeId(10).trackId(1).build()));
        assertEquals(ErrorCode.CONFLICT_SAME_TRACK, ex.getCode());
    }

    private static User approvedUser(int id, UserRole role) {
        return User.builder()
                .id(id)
                .role(role)
                .status(UserStatus.APPROVED)
                .email("u" + id + "@test.com")
                .fullName("User " + id)
                .build();
    }

    private static Round prelimRound() {
        Hackathon h = Hackathon.builder().id(1).status(HackathonStatus.ONGOING).build();
        return Round.builder()
                .id(100)
                .hackathon(h)
                .isFinal(false)
                .roundType(RoundType.PRELIMINARY)
                .build();
    }

    private static Track track(int id, Round round, String name) {
        return Track.builder()
                .id(id)
                .round(round)
                .name("Track " + name)
                .status(TrackStatus.OPEN)
                .build();
    }
}
