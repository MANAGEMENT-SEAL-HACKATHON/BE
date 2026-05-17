package com.se194093.be.hackathons.service.impl;

import com.se194093.be.common.audit.AuditAction;
import com.se194093.be.common.audit.AuditService;
import com.se194093.be.common.exception.BusinessRuleException;
import com.se194093.be.common.exception.ConflictException;
import com.se194093.be.common.exception.ErrorCode;
import com.se194093.be.common.exception.ResourceNotFoundException;
import com.se194093.be.common.security.CurrentUserAccessor;
import com.se194093.be.hackathons.dto.request.ChangeHackathonStatusRequest;
import com.se194093.be.hackathons.dto.response.HackathonReadinessResponse;
import com.se194093.be.hackathons.dto.response.HackathonResponse;
import com.se194093.be.hackathons.entity.Hackathon;
import com.se194093.be.hackathons.mapper.HackathonMapper;
import com.se194093.be.hackathons.repository.HackathonRepository;
import com.se194093.be.hackathons.service.HackathonReadinessService;
import com.se194093.be.hackathons.service.HackathonStatusService;
import com.se194093.be.hackathons.value_object.HackathonStatus;
import com.se194093.be.notifications.service.NotificationService;
import com.se194093.be.users.entity.User;
import com.se194093.be.users.repository.UserRepository;
import com.se194093.be.users.value_object.UserStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * FR-06 PATCH /hackathons/{id}/status — state machine + gate check + fan-out HACKATHON_OPEN.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class HackathonStatusServiceImpl implements HackathonStatusService {

    private static final Map<HackathonStatus, Set<HackathonStatus>> ALLOWED_TRANSITIONS = Map.of(
            HackathonStatus.DRAFT,            EnumSet.of(HackathonStatus.ONGOING),
            HackathonStatus.ONGOING,          EnumSet.of(HackathonStatus.PENDING_CONFIRM),
            HackathonStatus.PENDING_CONFIRM,  EnumSet.of(HackathonStatus.FINISHED),
            HackathonStatus.FINISHED,         EnumSet.noneOf(HackathonStatus.class)
    );

    public static boolean isAllowedTransition(HackathonStatus from, HackathonStatus to) {
        if (from == null || to == null || from == to) {
            return false;
        }
        return ALLOWED_TRANSITIONS.getOrDefault(from, EnumSet.noneOf(HackathonStatus.class)).contains(to);
    }

    private final HackathonRepository hackathonRepository;
    private final HackathonReadinessService readinessService;
    private final HackathonMapper hackathonMapper;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final CurrentUserAccessor currentUserAccessor;

    @Override
    public HackathonResponse changeStatus(Integer hackathonId, ChangeHackathonStatusRequest req) {
        Hackathon h = hackathonRepository.findById(hackathonId)
                .orElseThrow(() -> new ResourceNotFoundException("Hackathon", hackathonId));

        HackathonStatus from = h.getStatus();
        HackathonStatus to = req.getTargetStatus();
        if (!isAllowedTransition(from, to)) {
            throw new ConflictException(ErrorCode.STATUS_TRANSITION_INVALID,
                    "Không thể chuyển %s → %s".formatted(from, to),
                    Map.of("from", from.name(), "to", to.name()));
        }

        if (to == HackathonStatus.ONGOING) {
            HackathonReadinessResponse readiness = readinessService.check(hackathonId, HackathonStatus.ONGOING);
            if (!readiness.isReady()) {
                throw new BusinessRuleException(ErrorCode.READINESS_NOT_PASSED,
                        "Hackathon chưa sẵn sàng chuyển ONGOING (%d blocker)"
                                .formatted(readiness.getBlockers().size()),
                        Map.of("blockers", readiness.getBlockers()));
            }
        }

        h.setStatus(to);
        Hackathon saved = hackathonRepository.save(h);

        Integer validatedBy = currentUserAccessor.currentUserId();
        Map<String, Object> auditDetail = new HashMap<>();
        auditDetail.put("from", from.name());
        auditDetail.put("to", to.name());
        auditDetail.put("note", req.getNote());
        auditDetail.put("validatedBy", validatedBy);
        auditDetail.put("validatedAt", LocalDateTime.now().toString());
        auditService.log(AuditAction.HACKATHON_STATUS_CHANGE, "hackathons", saved.getId(), auditDetail);

        if (to == HackathonStatus.ONGOING) {
            List<User> approvedUsers = userRepository.findAllByStatus(UserStatus.APPROVED);
            notificationService.sendBatch(
                    approvedUsers,
                    "HACKATHON_OPEN",
                    "Hackathon '%s' đã mở".formatted(saved.getName()),
                    "Hackathon %s %d đã chuyển ONGOING — mời các bên liên quan tham gia."
                            .formatted(saved.getSeason(), saved.getYear()),
                    "hackathons", saved.getId());
        }

        return hackathonMapper.toResponse(saved);
    }
}
