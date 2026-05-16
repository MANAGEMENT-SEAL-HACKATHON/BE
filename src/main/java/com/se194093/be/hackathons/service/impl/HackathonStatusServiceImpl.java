package com.se194093.be.hackathons.service.impl;

import com.se194093.be.hackathons.dto.request.ChangeHackathonStatusRequest;
import com.se194093.be.hackathons.dto.response.HackathonResponse;
import com.se194093.be.hackathons.service.HackathonStatusService;
import com.se194093.be.hackathons.value_object.HackathonStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Skeleton — TODO Dev implement theo {@code docs/api/mf-01/fr-06-status.md}.
 *
 * <p>Inject: HackathonRepository, HackathonReadinessService, HackathonMapper, AuditService,
 * NotificationRepository / NotificationFanoutService, CurrentUserAccessor.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class HackathonStatusServiceImpl implements HackathonStatusService {

    /**
     * State machine — 1 chiều. Mỗi state map sang Set những state ĐƯỢC PHÉP chuyển đến.
     */
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

    @Override
    public HackathonResponse changeStatus(Integer hackathonId, ChangeHackathonStatusRequest req) {
        // TODO Dev:
        //  1. h = hackathonRepo.findById(hackathonId) or 404
        //  2. if !isAllowedTransition(h.status, req.targetStatus):
        //         throw ConflictException(STATUS_TRANSITION_INVALID,
        //               "Không thể chuyển %s → %s".formatted(h.status, req.targetStatus))
        //  3. if req.targetStatus == ONGOING:
        //         readiness = readinessService.check(hackathonId, ONGOING)
        //         if !readiness.ready:
        //             throw BusinessRuleException(READINESS_NOT_PASSED,
        //                   "Hackathon chưa sẵn sàng chuyển ONGOING",
        //                   Map.of("blockers", readiness.blockers))
        //  4. oldStatus = h.status; h.status = req.targetStatus; save
        //  5. audit.log(HACKATHON_STATUS_CHANGE, "hackathons", h.id,
        //               Map.of("from", oldStatus, "to", req.targetStatus,
        //                      "note", req.note, "validatedBy", currentUser.id,
        //                      "validatedAt", Instant.now()))
        //  6. if req.targetStatus == ONGOING:
        //         notificationFanoutService.fanoutHackathonOpen(h.id)
        //  7. return mapper.toResponse(h)
        throw new UnsupportedOperationException("FR-06 PATCH /hackathons/{id}/status - to be implemented");
    }
}
