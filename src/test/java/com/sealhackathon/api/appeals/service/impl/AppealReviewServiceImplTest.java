package com.sealhackathon.api.appeals.service.impl;

import com.sealhackathon.api.appeals.dto.request.ReviewAppealRequest;
import com.sealhackathon.api.appeals.entity.Appeal;
import com.sealhackathon.api.appeals.mapper.AppealMapper;
import com.sealhackathon.api.appeals.repository.AppealRepository;
import com.sealhackathon.api.appeals.service.AppealWindowService;
import com.sealhackathon.api.appeals.value_object.AppealStatus;
import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.common.security.CurrentUserAccessor;
import com.sealhackathon.api.me.student.dto.response.AppealResponse;
import com.sealhackathon.api.notifications.service.NotificationService;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.guard.RoundAccessGuard;
import com.sealhackathon.api.teams.entity.Team;
import com.sealhackathon.api.teams.repository.TeamMemberRepository;
import com.sealhackathon.api.teams.service.TeamReinstatementService;
import com.sealhackathon.api.users.entity.User;
import com.sealhackathon.api.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AppealReviewServiceImplTest {

    @Mock private AppealRepository appealRepository;
    @Mock private AppealMapper appealMapper;
    @Mock private RoundAccessGuard roundAccessGuard;
    @Mock private CurrentUserAccessor currentUserAccessor;
    @Mock private UserRepository userRepository;
    @Mock private AuditService auditService;
    @Mock private TeamReinstatementService teamReinstatementService;
    @Mock private NotificationService notificationService;
    @Mock private TeamMemberRepository teamMemberRepository;
    @Mock private AppealWindowService appealWindowService;

    @InjectMocks private AppealReviewServiceImpl service;

    private User reviewer;
    private Team team;
    private Round round;
    private Appeal appeal;

    @BeforeEach
    void setUp() {
        reviewer = User.builder().id(7).build();
        team = Team.builder().id(3).teamName("Alpha").build();
        round = Round.builder().id(10).name("Sơ loại").build();
        appeal = Appeal.builder()
                .id(50)
                .team(team)
                .round(round)
                .status(AppealStatus.UNDER_REVIEW)
                .version(0L)
                .build();
        when(currentUserAccessor.currentUserId()).thenReturn(7);
        when(userRepository.findById(7)).thenReturn(Optional.of(reviewer));
        when(appealRepository.findById(50)).thenReturn(Optional.of(appeal));
        when(appealRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(teamMemberRepository.findByTeam_Id(3)).thenReturn(List.of());
        when(appealMapper.toResponse(any())).thenAnswer(inv -> {
            Appeal a = inv.getArgument(0);
            return AppealResponse.builder().id(a.getId()).status(a.getStatus()).build();
        });
        when(appealWindowService.expireOpenAppealsForRound(10)).thenReturn(0);
    }

    @Test
    void approve_reinstatesTeam() {
        ReviewAppealRequest req = ReviewAppealRequest.builder().decision("APPROVED").build();

        AppealResponse resp = service.review(50, req);

        assertThat(resp.getStatus()).isEqualTo(AppealStatus.APPROVED);
        verify(teamReinstatementService).reinstateFromAppeal(team, appeal);
    }

    @Test
    void reject_requiresNote() {
        ReviewAppealRequest req = ReviewAppealRequest.builder().decision("REJECTED").build();

        assertThatThrownBy(() -> service.review(50, req))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.APPEAL_DECISION_NOTE_REQUIRED);
        verify(teamReinstatementService, never()).reinstateFromAppeal(any(), any());
    }

    @Test
    void reject_withNote_succeeds() {
        ReviewAppealRequest req = ReviewAppealRequest.builder()
                .decision("REJECTED")
                .note("Không đủ minh chứng")
                .build();

        AppealResponse resp = service.review(50, req);

        assertThat(resp.getStatus()).isEqualTo(AppealStatus.REJECTED);
        assertThat(appeal.getDecisionNote()).isEqualTo("Không đủ minh chứng");
    }

    @Test
    void concurrentVersion_propagatesOptimisticLock() {
        when(appealRepository.save(any())).thenThrow(
                new ObjectOptimisticLockingFailureException(Appeal.class, 50));

        ReviewAppealRequest req = ReviewAppealRequest.builder().decision("APPROVED").build();

        assertThatThrownBy(() -> service.review(50, req))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }
}
