package com.se194093.be.hackathons.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.se194093.be.common.response.Warning;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * FR-06 GET /api/v1/hackathons/{id}/readiness — dry-run gate check trước khi chuyển ONGOING.
 *
 * <p>Format:
 * <pre>
 * {
 *   "ready": false,
 *   "targetStatus": "ONGOING",
 *   "blockers": [
 *     { "code": "ROUND_WEIGHT_NOT_ONE", "message": "Track A - Round Sơ loại: tổng weight 0.75" },
 *     { "code": "EVENT_KICKOFF_MISSING", "message": "Thiếu sự kiện KICKOFF" }
 *   ],
 *   "warnings": [
 *     { "code": "READINESS_WARNING", "message": "Chưa có Mentor cho Track A" }
 *   ],
 *   "summary": {
 *     "tracksCount":    2,
 *     "roundsCount":    4,
 *     "criteriaCount":  16,
 *     "tempJudgesCount": 6,
 *     "mentorAssignmentsCount": 3,
 *     "judgeAssignmentsCount":  6,
 *     "eventsCount":    3
 *   }
 * }
 * </pre>
 */
@Getter
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HackathonReadinessResponse {

    private final boolean ready;
    private final String targetStatus;
    private final List<Blocker> blockers;
    private final List<Warning> warnings;
    private final Map<String, Object> summary;

    @Getter
    @Builder
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Blocker {
        private final String code;
        private final String message;
        private final Map<String, Object> details;
    }
}
