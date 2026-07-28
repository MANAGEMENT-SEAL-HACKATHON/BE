package com.sealhackathon.api.presentation.guard;

import com.sealhackathon.api.common.exception.AuthException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.common.security.CurrentUserAccessor;
import com.sealhackathon.api.judge_assignments.entity.JudgeAssignment;
import com.sealhackathon.api.judge_assignments.repository.JudgeAssignmentRepository;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.tracks.entity.Track;
import com.sealhackathon.api.users.value_object.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PresentationControllerGuard {

    private final CurrentUserAccessor currentUserAccessor;
    private final JudgeAssignmentRepository judgeAssignmentRepository;

    public void requireControllerForTrack(Integer trackId, Track track, Round round) {
        if (isCoordinator()) {
            return;
        }
        Integer userId = requireUserId();
        Integer controllerId = resolveTrackControllerId(track);
        if (controllerId != null && controllerId.equals(userId)) {
            return;
        }
        throw forbidden(trackId, "track");
    }

    public void requireControllerForRound(Integer roundId, Round round) {
        if (isCoordinator()) {
            return;
        }
        Integer userId = requireUserId();
        Integer controllerId = resolveRoundControllerId(round);
        if (controllerId != null && controllerId.equals(userId)) {
            return;
        }
        throw forbidden(roundId, "round");
    }

    public boolean canControlTrack(Integer userId, Track track, Round round, boolean coordinator) {
        if (coordinator) {
            return true;
        }
        Integer controllerId = resolveTrackControllerId(track);
        return controllerId != null && controllerId.equals(userId);
    }

    public boolean canControlRound(Integer userId, Round round, boolean coordinator) {
        if (coordinator) {
            return true;
        }
        Integer controllerId = resolveRoundControllerId(round);
        return controllerId != null && controllerId.equals(userId);
    }

    public Integer resolveTrackControllerId(Track track) {
        if (track.getControllerJudge() != null) {
            return track.getControllerJudge().getId();
        }
        return findDefaultControllerJudgeId(judgeAssignmentRepository.findByTrackId(track.getId()))
                .orElse(null);
    }

    public Integer resolveRoundControllerId(Round round) {
        if (round.getControllerJudge() != null) {
            return round.getControllerJudge().getId();
        }
        return findDefaultControllerJudgeId(judgeAssignmentRepository.findByRoundId(round.getId()))
                .orElse(null);
    }

    /**
     * Mặc định: judge được gán sớm nhất (assignedAt).
     * Không dùng assignment_type HEAD hay is_dept_head — timer override qua coordinator grant.
     */
    static Optional<Integer> findDefaultControllerJudgeId(List<JudgeAssignment> assignments) {
        if (assignments == null || assignments.isEmpty()) {
            return Optional.empty();
        }
        Comparator<JudgeAssignment> byAssignedAt = Comparator.comparing(
                JudgeAssignment::getAssignedAt,
                Comparator.nullsLast(Comparator.naturalOrder()));

        return assignments.stream()
                .sorted(byAssignedAt)
                .map(ja -> ja.getJudge() != null ? ja.getJudge().getId() : null)
                .filter(Objects::nonNull)
                .findFirst();
    }

    private boolean isCoordinator() {
        var user = currentUserAccessor.currentUser();
        return user != null && user.getRole() == UserRole.COORDINATOR;
    }

    private Integer requireUserId() {
        Integer userId = currentUserAccessor.currentUserId();
        if (userId == null) {
            throw new AuthException(ErrorCode.UNAUTHORIZED, "Chưa xác thực", HttpStatus.UNAUTHORIZED);
        }
        return userId;
    }

    private static AuthException forbidden(Integer id, String scope) {
        return new AuthException(ErrorCode.NOT_TRACK_CONTROLLER,
                "Không có quyền điều khiển presentation " + scope + " " + id,
                HttpStatus.FORBIDDEN);
    }
}
