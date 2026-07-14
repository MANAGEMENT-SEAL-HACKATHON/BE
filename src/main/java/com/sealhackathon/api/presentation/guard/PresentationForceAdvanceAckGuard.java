package com.sealhackathon.api.presentation.guard;

import com.sealhackathon.api.common.exception.AuthException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.common.security.CurrentUserAccessor;
import com.sealhackathon.api.judge_assignments.repository.JudgeAssignmentRepository;
import com.sealhackathon.api.judge_assignments.value_object.JudgeAssignmentType;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.users.value_object.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Force-ack incomplete scoring — chỉ COORDINATOR hoặc judge assignment_type=HEAD.
 */
@Component
@RequiredArgsConstructor
public class PresentationForceAdvanceAckGuard {

    private final CurrentUserAccessor currentUserAccessor;
    private final JudgeAssignmentRepository judgeAssignmentRepository;

    /**
     * @return true nếu được phép dùng {@code acknowledgeIncompleteScoring}
     */
    public boolean canAcknowledgeIncomplete(Integer trackId, Round round) {
        var user = currentUserAccessor.currentUser();
        if (user != null && user.getRole() == UserRole.COORDINATOR) {
            return true;
        }
        Integer userId = currentUserAccessor.currentUserId();
        if (userId == null) {
            return false;
        }
        if (trackId != null) {
            return judgeAssignmentRepository.findByJudgeIdAndTrackId(userId, trackId).stream()
                    .anyMatch(ja -> ja.getAssignmentType() == JudgeAssignmentType.HEAD);
        }
        if (round != null && round.getId() != null) {
            return judgeAssignmentRepository.findByRoundId(round.getId()).stream()
                    .anyMatch(ja -> ja.getJudge() != null
                            && userId.equals(ja.getJudge().getId())
                            && ja.getAssignmentType() == JudgeAssignmentType.HEAD);
        }
        return false;
    }

    /**
     * Nếu client gửi ack=true nhưng không đủ role → bỏ qua flag (incomplete vẫn block),
     * hoặc ném 403 khi muốn cứng. Plan: NORMAL không được truyền cờ — treat as false + Forbidden nếu cố ý.
     */
    public boolean resolveAcknowledge(boolean requested, Integer trackId, Round round) {
        if (!requested) {
            return false;
        }
        if (canAcknowledgeIncomplete(trackId, round)) {
            return true;
        }
        throw new AuthException(ErrorCode.FORBIDDEN,
                "Chỉ Coordinator hoặc Head Judge được xác nhận chuyển đội khi chưa đủ giám khảo Chốt điểm",
                HttpStatus.FORBIDDEN);
    }
}
