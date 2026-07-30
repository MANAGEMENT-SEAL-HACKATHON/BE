package com.sealhackathon.api.me.service.impl;

import com.sealhackathon.api.common.audit.AuditAction;
import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.common.security.CurrentUserAccessor;
import com.sealhackathon.api.common.value_object.AssignmentResponseStatus;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.judge_assignments.entity.JudgeAssignment;
import com.sealhackathon.api.judge_assignments.repository.JudgeAssignmentRepository;
import com.sealhackathon.api.judge_assignments.value_object.CompletionStatus;
import com.sealhackathon.api.judge_assignments.value_object.JudgeAssignmentType;
import com.sealhackathon.api.me.dto.request.AssignmentDeclineRequest;
import com.sealhackathon.api.mentors.repository.MentorAssignmentRepository;
import com.sealhackathon.api.mentors.repository.MentorTeamAssignmentRepository;
import com.sealhackathon.api.notifications.service.StakeholderBroadcastService;
import com.sealhackathon.api.notifications.value_object.NotificationType;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.tracks.entity.Track;
import com.sealhackathon.api.users.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssignmentResponseServiceImplTest {

    @Mock private CurrentUserAccessor currentUserAccessor;
    @Mock private JudgeAssignmentRepository judgeAssignmentRepository;
    @Mock private MentorAssignmentRepository mentorAssignmentRepository;
    @Mock private MentorTeamAssignmentRepository mentorTeamAssignmentRepository;
    @Mock private StakeholderBroadcastService stakeholderBroadcastService;
    @Mock private AuditService auditService;

    @InjectMocks
    private AssignmentResponseServiceImpl service;

    private User judge;
    private Hackathon hackathon;
    private Round round;
    private Track track;

    @BeforeEach
    void setUp() {
        judge = User.builder().id(7).fullName("Judge Seven").email("j7@fpt.edu.vn").build();
        hackathon = Hackathon.builder().id(1).name("SEAL").build();
        round = Round.builder().id(10).name("Sơ loại").isActive(false).hackathon(hackathon).build();
        track = Track.builder().id(20).name("Track A").round(round).build();
    }

    @Test
    void newJudgeAssignment_defaultsToAccepted() {
        JudgeAssignment ja = JudgeAssignment.builder()
                .judge(judge)
                .track(track)
                .assignmentType(JudgeAssignmentType.NORMAL)
                .completionStatus(CompletionStatus.NOT_STARTED)
                .build();

        assertThat(ja.getResponseStatus()).isEqualTo(AssignmentResponseStatus.ACCEPTED);
    }

    @Test
    void declineJudge_whenRoundActive_throwsTooLate() {
        round.setIsActive(true);
        JudgeAssignment ja = JudgeAssignment.builder()
                .id(99)
                .judge(judge)
                .track(track)
                .responseStatus(AssignmentResponseStatus.ACCEPTED)
                .build();
        when(currentUserAccessor.currentUserId()).thenReturn(7);
        when(judgeAssignmentRepository.findById(99)).thenReturn(Optional.of(ja));

        assertThatThrownBy(() -> service.declineJudgeAssignment(99,
                AssignmentDeclineRequest.builder().reason("busy").build()))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(ex -> ((BusinessRuleException) ex).getCode())
                .isEqualTo(ErrorCode.ASSIGNMENT_DECLINE_TOO_LATE);

        verify(stakeholderBroadcastService, never()).broadcast(
                anyInt(), anyString(), anyString(), anyList(), anyString(), anyInt(), anyBoolean());
    }

    @Test
    void declineJudge_whenProblemReleased_throwsTooLate() {
        round.setProblemReleasedAt(LocalDateTime.now());
        JudgeAssignment ja = JudgeAssignment.builder()
                .id(99)
                .judge(judge)
                .track(track)
                .responseStatus(AssignmentResponseStatus.ACCEPTED)
                .build();
        when(currentUserAccessor.currentUserId()).thenReturn(7);
        when(judgeAssignmentRepository.findById(99)).thenReturn(Optional.of(ja));

        assertThatThrownBy(() -> service.declineJudgeAssignment(99,
                AssignmentDeclineRequest.builder().reason("conflict").build()))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(ex -> ((BusinessRuleException) ex).getCode())
                .isEqualTo(ErrorCode.ASSIGNMENT_DECLINE_TOO_LATE);
    }

    @Test
    void declineJudge_notifiesStakeholdersAndAudits() {
        JudgeAssignment ja = JudgeAssignment.builder()
                .id(99)
                .judge(judge)
                .track(track)
                .responseStatus(AssignmentResponseStatus.ACCEPTED)
                .build();
        when(currentUserAccessor.currentUserId()).thenReturn(7);
        when(judgeAssignmentRepository.findById(99)).thenReturn(Optional.of(ja));
        when(judgeAssignmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.declineJudgeAssignment(99,
                AssignmentDeclineRequest.builder().reason("schedule conflict").build());

        assertThat(result.getResponseStatus()).isEqualTo(AssignmentResponseStatus.DECLINED);
        assertThat(result.getDeclineReason()).isEqualTo("schedule conflict");
        assertThat(ja.getResponseStatus()).isEqualTo(AssignmentResponseStatus.DECLINED);

        verify(auditService).log(eq(AuditAction.JUDGE_DECLINED), eq("judge_assignments"), eq(99),
                org.mockito.ArgumentMatchers.<java.util.Map<String, Object>>any());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<java.util.List<String>> linesCaptor = ArgumentCaptor.forClass(java.util.List.class);
        verify(stakeholderBroadcastService).broadcast(
                eq(1),
                eq(NotificationType.JUDGE_DECLINED),
                eq("Giám khảo từ chối phân công"),
                linesCaptor.capture(),
                eq("judge_assignments"),
                eq(99),
                eq(true));
        assertThat(linesCaptor.getValue().get(0)).contains("Judge Seven");
        assertThat(linesCaptor.getValue().get(1)).contains("schedule conflict");
    }

    @Test
    void acceptJudge_clearsDecline() {
        JudgeAssignment ja = JudgeAssignment.builder()
                .id(99)
                .judge(judge)
                .track(track)
                .responseStatus(AssignmentResponseStatus.DECLINED)
                .declineReason("was busy")
                .respondedAt(LocalDateTime.now().minusDays(1))
                .build();
        when(currentUserAccessor.currentUserId()).thenReturn(7);
        when(judgeAssignmentRepository.findById(99)).thenReturn(Optional.of(ja));
        when(judgeAssignmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.acceptJudgeAssignment(99);

        assertThat(result.getResponseStatus()).isEqualTo(AssignmentResponseStatus.ACCEPTED);
        assertThat(result.getDeclineReason()).isNull();
        assertThat(ja.getDeclineReason()).isNull();
    }
}
