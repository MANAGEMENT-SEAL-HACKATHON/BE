package com.sealhackathon.api.presentation.guard;

import com.sealhackathon.api.common.exception.AuthException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.common.security.CurrentUserAccessor;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.tracks.entity.Track;
import com.sealhackathon.api.tracks.repository.TrackRepository;
import com.sealhackathon.api.users.value_object.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Force-ack incomplete scoring — chỉ COORDINATOR hoặc giám khảo đang giữ quyền
 * điều khiển presentation (controllerJudge của track/round — không dựa vào
 * assignment_type HEAD).
 */
@Component
@RequiredArgsConstructor
public class PresentationForceAdvanceAckGuard {

    private final CurrentUserAccessor currentUserAccessor;
    private final PresentationControllerGuard controllerGuard;
    private final TrackRepository trackRepository;

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
            Track track = trackRepository.findById(trackId).orElse(null);
            if (track == null) {
                return false;
            }
            return userId.equals(controllerGuard.resolveTrackControllerId(track));
        }
        if (round != null && round.getId() != null) {
            return userId.equals(controllerGuard.resolveRoundControllerId(round));
        }
        return false;
    }

    /**
     * Nếu client gửi ack=true nhưng không đủ quyền → ném 403.
     * Chỉ Coordinator hoặc giám khảo đang điều khiển presentation được truyền cờ này.
     */
    public boolean resolveAcknowledge(boolean requested, Integer trackId, Round round) {
        if (!requested) {
            return false;
        }
        if (canAcknowledgeIncomplete(trackId, round)) {
            return true;
        }
        throw new AuthException(ErrorCode.FORBIDDEN,
                "Chỉ Coordinator hoặc Giám khảo đang điều khiển phần thuyết trình được xác nhận"
                        + " chuyển đội khi chưa đủ giám khảo Chốt điểm",
                HttpStatus.FORBIDDEN);
    }
}
