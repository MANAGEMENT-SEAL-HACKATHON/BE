package com.sealhackathon.api.appeals.service.impl;

import com.sealhackathon.api.appeals.dto.request.ReviewAppealRequest;
import com.sealhackathon.api.appeals.entity.Appeal;
import com.sealhackathon.api.appeals.mapper.AppealMapper;
import com.sealhackathon.api.appeals.repository.AppealRepository;
import com.sealhackathon.api.appeals.service.AppealReviewService;
import com.sealhackathon.api.appeals.service.AppealWindowService;
import com.sealhackathon.api.appeals.value_object.AppealStatus;
import com.sealhackathon.api.common.audit.AuditAction;
import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.common.exception.ResourceNotFoundException;
import com.sealhackathon.api.common.security.CurrentUserAccessor;
import com.sealhackathon.api.me.student.dto.response.AppealResponse;
import com.sealhackathon.api.notifications.service.NotificationService;
import com.sealhackathon.api.notifications.value_object.NotificationType;
import com.sealhackathon.api.rounds.guard.RoundAccessGuard;
import com.sealhackathon.api.teams.entity.TeamMember;
import com.sealhackathon.api.teams.repository.TeamMemberRepository;
import com.sealhackathon.api.teams.service.TeamReinstatementService;
import com.sealhackathon.api.teams.value_object.TeamMemberStatus;
import com.sealhackathon.api.users.entity.User;
import com.sealhackathon.api.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class AppealReviewServiceImpl implements AppealReviewService {

    private final AppealRepository appealRepository;
    private final AppealMapper appealMapper;
    private final RoundAccessGuard roundAccessGuard;
    private final CurrentUserAccessor currentUserAccessor;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final TeamReinstatementService teamReinstatementService;
    private final NotificationService notificationService;
    private final TeamMemberRepository teamMemberRepository;
    private final AppealWindowService appealWindowService;

    @Override
    @Transactional(readOnly = true)
    public List<AppealResponse> listByRound(Integer roundId, AppealStatus status) {
        roundAccessGuard.requireRound(roundId);
        List<Appeal> appeals = status == null
                ? appealRepository.findByRound_IdOrderByCreatedAtDesc(roundId)
                : appealRepository.findByRound_IdAndStatusOrderByCreatedAtDesc(roundId, status);
        return appeals.stream().map(appealMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AppealResponse getById(Integer appealId) {
        return appealMapper.toResponse(requireAppeal(appealId));
    }

    @Override
    public AppealResponse claim(Integer appealId) {
        Appeal appeal = requireAppeal(appealId);
        appealWindowService.expireOpenAppealsForRound(appeal.getRound().getId());
        appeal = requireAppeal(appealId);

        if (appeal.getStatus() != AppealStatus.PENDING) {
            throw new BusinessRuleException(ErrorCode.APPEAL_NOT_PENDING,
                    "Chỉ nhận đơn ở trạng thái PENDING (hiện %s)".formatted(appeal.getStatus()));
        }

        User reviewer = userRepository.findById(currentUserAccessor.currentUserId()).orElseThrow();
        appeal.setStatus(AppealStatus.UNDER_REVIEW);
        appeal.setReviewedBy(reviewer);
        Appeal saved = appealRepository.save(appeal);

        auditService.log(AuditAction.APPEAL_CLAIM, "appeals", saved.getId(),
                Map.of("roundId", saved.getRound().getId(),
                        "teamId", saved.getTeam().getId(),
                        "reviewerId", reviewer.getId()));

        return appealMapper.toResponse(saved);
    }

    @Override
    public AppealResponse review(Integer appealId, ReviewAppealRequest request) {
        Appeal appeal = requireAppeal(appealId);
        appealWindowService.expireOpenAppealsForRound(appeal.getRound().getId());
        appeal = requireAppeal(appealId);

        if (appeal.getStatus() != AppealStatus.PENDING && appeal.getStatus() != AppealStatus.UNDER_REVIEW) {
            throw new BusinessRuleException(ErrorCode.APPEAL_NOT_PENDING,
                    "Đơn không còn ở trạng thái chờ duyệt (hiện %s)".formatted(appeal.getStatus()));
        }

        String decision = request.getDecision() != null ? request.getDecision().trim().toUpperCase() : "";
        User reviewer = userRepository.findById(currentUserAccessor.currentUserId()).orElseThrow();
        LocalDateTime now = LocalDateTime.now();

        if ("APPROVED".equals(decision)) {
            teamReinstatementService.reinstateFromAppeal(appeal.getTeam(), appeal);
            appeal.setStatus(AppealStatus.APPROVED);
            appeal.setReviewedBy(reviewer);
            appeal.setReviewedAt(now);
            appeal.setDecisionNote(StringUtils.hasText(request.getNote()) ? request.getNote().trim() : null);
            Appeal saved = appealRepository.save(appeal);

            auditService.log(AuditAction.APPEAL_APPROVE, "appeals", saved.getId(),
                    Map.of("teamId", saved.getTeam().getId(),
                            "roundId", saved.getRound().getId()));
            notifyTeam(saved, NotificationType.APPEAL_APPROVED,
                    "Khiếu nại được chấp nhận",
                    "Đơn khiếu nại của đội «%s» đã được chấp nhận. Đội được phục hồi quyền thi."
                            .formatted(saved.getTeam().getTeamName()));
            return appealMapper.toResponse(saved);
        }

        if ("REJECTED".equals(decision)) {
            if (!StringUtils.hasText(request.getNote())) {
                throw new BusinessRuleException(ErrorCode.APPEAL_DECISION_NOTE_REQUIRED,
                        "Từ chối khiếu nại bắt buộc ghi lý do");
            }
            appeal.setStatus(AppealStatus.REJECTED);
            appeal.setReviewedBy(reviewer);
            appeal.setReviewedAt(now);
            appeal.setDecisionNote(request.getNote().trim());
            Appeal saved = appealRepository.save(appeal);

            auditService.log(AuditAction.APPEAL_REJECT, "appeals", saved.getId(),
                    Map.of("teamId", saved.getTeam().getId(),
                            "roundId", saved.getRound().getId(),
                            "note", saved.getDecisionNote()));
            notifyTeam(saved, NotificationType.APPEAL_REJECTED,
                    "Khiếu nại bị từ chối",
                    "Đơn khiếu nại của đội «%s» bị từ chối. Lý do: %s"
                            .formatted(saved.getTeam().getTeamName(), saved.getDecisionNote()));
            return appealMapper.toResponse(saved);
        }

        throw new BusinessRuleException(ErrorCode.VALIDATION_FAILED,
                "decision phải là APPROVED hoặc REJECTED");
    }

    private Appeal requireAppeal(Integer appealId) {
        return appealRepository.findById(appealId)
                .orElseThrow(() -> new ResourceNotFoundException("Appeal", appealId));
    }

    private void notifyTeam(Appeal appeal, String type, String title, String body) {
        Set<User> recipients = new LinkedHashSet<>();
        for (TeamMember tm : teamMemberRepository.findByTeam_Id(appeal.getTeam().getId())) {
            if (tm.getStatus() == TeamMemberStatus.ACCEPTED) {
                recipients.add(tm.getUser());
            }
        }
        if (!recipients.isEmpty()) {
            notificationService.sendBatch(new ArrayList<>(recipients), type, title, body,
                    "appeals", appeal.getId());
        }
    }
}
