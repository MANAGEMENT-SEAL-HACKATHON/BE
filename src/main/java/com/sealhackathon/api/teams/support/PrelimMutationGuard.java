package com.sealhackathon.api.teams.support;

import com.sealhackathon.api.common.exception.AuthException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.teams.entity.TeamRoundTrack;
import com.sealhackathon.api.teams.value_object.ParticipationStatus;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Chặn mutation Sơ loại khi TeamRoundTrack đã ADVANCED hoặc ELIMINATED (GĐ5 Bug5 / G5-A).
 */
@Component
public class PrelimMutationGuard {

    /**
     * Chỉ cho phép thao tác khi {@code participationStatus == PARTICIPATING}.
     * ADVANCED / ELIMINATED → 403 {@link ErrorCode#PRELIM_NOT_MUTABLE}.
     */
    public void assertPrelimMutable(TeamRoundTrack trt) {
        if (trt == null) {
            throw new AuthException(ErrorCode.TEAM_NOT_IN_TRACK,
                    "Đội chưa được phân bảng Sơ loại.",
                    HttpStatus.FORBIDDEN);
        }
        ParticipationStatus status = trt.getParticipationStatus();
        if (status == ParticipationStatus.PARTICIPATING) {
            return;
        }
        String label = status != null ? status.name() : "UNKNOWN";
        throw new AuthException(
                ErrorCode.PRELIM_NOT_MUTABLE,
                "Đội đã " + label + " — không thể nộp bài / đổi bảng vòng Sơ loại.",
                HttpStatus.FORBIDDEN,
                Map.of("participationStatus", label, "teamId", trt.getTeam().getId()));
    }
}
