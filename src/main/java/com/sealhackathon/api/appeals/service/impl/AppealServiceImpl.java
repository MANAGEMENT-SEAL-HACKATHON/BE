package com.sealhackathon.api.appeals.service.impl;

import com.sealhackathon.api.appeals.dto.request.AppealEvidenceItemRequest;
import com.sealhackathon.api.appeals.dto.response.AppealEvidenceUploadResponse;
import com.sealhackathon.api.appeals.entity.Appeal;
import com.sealhackathon.api.appeals.entity.AppealEvidence;
import com.sealhackathon.api.appeals.mapper.AppealMapper;
import com.sealhackathon.api.appeals.repository.AppealRepository;
import com.sealhackathon.api.appeals.service.AppealService;
import com.sealhackathon.api.appeals.support.AppealEvidenceStorageService;
import com.sealhackathon.api.appeals.value_object.AppealEvidenceType;
import com.sealhackathon.api.appeals.value_object.AppealStatus;
import com.sealhackathon.api.common.audit.AuditAction;
import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ConflictException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.common.exception.ResourceNotFoundException;
import com.sealhackathon.api.common.security.CurrentUserAccessor;
import com.sealhackathon.api.me.student.dto.request.CreateAppealRequest;
import com.sealhackathon.api.me.student.dto.response.AppealResponse;
import com.sealhackathon.api.me.support.StudentAccessGuard;
import com.sealhackathon.api.notifications.service.NotificationService;
import com.sealhackathon.api.notifications.value_object.NotificationType;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.teams.entity.Team;
import com.sealhackathon.api.teams.entity.TeamMember;
import com.sealhackathon.api.teams.repository.TeamMemberRepository;
import com.sealhackathon.api.teams.repository.TeamRepository;
import com.sealhackathon.api.teams.value_object.TeamMemberStatus;
import com.sealhackathon.api.teams.value_object.TeamStatus;
import com.sealhackathon.api.users.entity.User;
import com.sealhackathon.api.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class AppealServiceImpl implements AppealService {

    private final AppealRepository appealRepository;
    private final TeamRepository teamRepository;
    private final RoundRepository roundRepository;
    private final StudentAccessGuard studentAccessGuard;
    private final CurrentUserAccessor currentUserAccessor;
    private final UserRepository userRepository;
    private final AppealMapper appealMapper;
    private final AppealEvidenceStorageService evidenceStorageService;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final TeamMemberRepository teamMemberRepository;

    @Override
    public AppealResponse create(CreateAppealRequest request) {
        studentAccessGuard.assertTeamLeader(request.getTeamId());

        Team team = teamRepository.findById(request.getTeamId())
                .orElseThrow(() -> new ResourceNotFoundException("Team", request.getTeamId()));

        if (team.getStatus() != TeamStatus.ELIMINATED) {
            throw new BusinessRuleException(ErrorCode.INVALID_STATE,
                    "Đội của bạn không nằm trong diện bị loại thủ công để có thể khiếu nại.");
        }
        if (!StringUtils.hasText(team.getEliminationReason())) {
            throw new BusinessRuleException(ErrorCode.INVALID_STATE,
                    "Chỉ đội bị loại kỷ luật (có lý do DQ) mới được khiếu nại.");
        }

        Round round = roundRepository.findById(request.getRoundId())
                .orElseThrow(() -> new ResourceNotFoundException("Round", request.getRoundId()));

        if (team.getHackathon() == null || round.getHackathon() == null
                || !team.getHackathon().getId().equals(round.getHackathon().getId())) {
            throw new BusinessRuleException(ErrorCode.CROSS_HACKATHON_VIOLATION,
                    "Đội và vòng thi không cùng hackathon");
        }

        LocalDateTime now = LocalDateTime.now();
        if (round.getAppealWindowEndsAt() == null) {
            throw new BusinessRuleException(ErrorCode.APPEAL_WINDOW_NOT_OPEN,
                    "Cửa sổ khiếu nại chưa được mở cho vòng này");
        }
        if (!now.isBefore(round.getAppealWindowEndsAt())) {
            throw new BusinessRuleException(ErrorCode.APPEAL_DEADLINE_EXPIRED,
                    "Đã quá hạn nộp đơn khiếu nại");
        }

        if (appealRepository.existsByTeam_IdAndRound_Id(team.getId(), round.getId())) {
            throw new ConflictException(ErrorCode.APPEAL_ALREADY_SUBMITTED,
                    "Đội của bạn đã gửi đơn khiếu nại cho vòng thi này rồi.");
        }

        List<AppealEvidenceItemRequest> evidenceItems = normalizeEvidences(request);
        if (evidenceItems.isEmpty()) {
            throw new BusinessRuleException(ErrorCode.APPEAL_EVIDENCE_REQUIRED,
                    "Phải đính kèm ít nhất một minh chứng");
        }

        User submitter = userRepository.findById(currentUserAccessor.currentUserId()).orElseThrow();

        Appeal appeal = Appeal.builder()
                .team(team)
                .round(round)
                .submittedBy(submitter)
                .reason(request.getReason())
                .evidenceUrl(evidenceItems.get(0).getUrl())
                .status(AppealStatus.PENDING)
                .build();

        int order = 0;
        for (AppealEvidenceItemRequest item : evidenceItems) {
            AppealEvidence evidence = AppealEvidence.builder()
                    .appeal(appeal)
                    .url(item.getUrl())
                    .type(item.getType())
                    .caption(item.getCaption())
                    .displayOrder(item.getDisplayOrder() != null ? item.getDisplayOrder() : order)
                    .build();
            appeal.getEvidences().add(evidence);
            order++;
        }

        Appeal saved = appealRepository.save(appeal);

        auditService.log(AuditAction.APPEAL_SUBMIT, "appeals", saved.getId(),
                Map.of("teamId", team.getId(), "roundId", round.getId(),
                        "evidenceCount", evidenceItems.size()));

        notifyTeam(saved, NotificationType.APPEAL_SUBMITTED,
                "Đã gửi đơn khiếu nại",
                "Đội «%s» đã gửi đơn khiếu nại cho vòng «%s»."
                        .formatted(team.getTeamName(), round.getName()));

        return appealMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppealResponse> listMine() {
        Integer userId = currentUserAccessor.currentUserId();
        List<TeamMember> memberships = teamMemberRepository.findByUser_IdAndStatus(userId, TeamMemberStatus.ACCEPTED);
        if (memberships.isEmpty()) {
            return List.of();
        }
        Set<Integer> teamIds = memberships.stream()
                .map(tm -> tm.getTeam().getId())
                .collect(Collectors.toSet());
        return appealRepository.findByTeam_IdInOrderByCreatedAtDesc(teamIds).stream()
                .map(appealMapper::toResponse)
                .toList();
    }

    @Override
    public AppealEvidenceUploadResponse uploadEvidence(MultipartFile file) {
        Integer userId = currentUserAccessor.currentUserId();
        String key = evidenceStorageService.store(userId, file);
        return AppealEvidenceUploadResponse.builder()
                .storageKey(key)
                .contentType(file.getContentType())
                .sizeBytes(file.getSize())
                .build();
    }

    private static List<AppealEvidenceItemRequest> normalizeEvidences(CreateAppealRequest request) {
        List<AppealEvidenceItemRequest> items = new ArrayList<>();
        if (request.getEvidences() != null) {
            for (AppealEvidenceItemRequest item : request.getEvidences()) {
                if (item != null && StringUtils.hasText(item.getUrl()) && item.getType() != null) {
                    items.add(item);
                }
            }
        }
        if (items.isEmpty() && StringUtils.hasText(request.getEvidenceUrl())) {
            items.add(AppealEvidenceItemRequest.builder()
                    .url(request.getEvidenceUrl().trim())
                    .type(inferType(request.getEvidenceUrl()))
                    .displayOrder(0)
                    .build());
        }
        return items;
    }

    private static AppealEvidenceType inferType(String url) {
        String lower = url.toLowerCase();
        if (lower.endsWith(".mp4") || lower.endsWith(".webm") || lower.endsWith(".mov")
                || lower.contains("video")) {
            return AppealEvidenceType.VIDEO;
        }
        if (lower.startsWith("http://") || lower.startsWith("https://")) {
            if (lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png")
                    || lower.endsWith(".webp") || lower.endsWith(".gif")
                    || lower.contains("appeal-evidences/")) {
                return AppealEvidenceType.IMAGE;
            }
            return AppealEvidenceType.LINK;
        }
        return AppealEvidenceType.IMAGE;
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
