package com.sealhackathon.api.hackathons.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class CloseRegistrationEarlyResponse {

    private final Integer hackathonId;
    private final LocalDateTime closedAt;
    private final int lockedActiveTeams;
    private final int withdrawnOrphans;
    private final int rejectedIncompleteTeams;
    /** Đội đã xác nhận thành lập, chờ Coordinator duyệt. */
    private final List<TeamAwaitingCoordinatorApprovalItem> teamsAwaitingCoordinatorApproval;
    /** Đội đủ thành viên nhưng leader chưa xác nhận — có 24h grace. */
    private final List<TeamInFormationGraceItem> teamsInFormationGracePeriod;

    /** examAt vòng sơ loại sớm nhất (nếu có) — FE banner hint. */
    private final LocalDateTime prelimExamAt;
    /** Số giờ còn lại tới prelimExamAt (null nếu không có / đã qua). */
    private final Long hoursUntilPrelimExam;
    /** true nếu đã nén WS/KO/SL/CK/AWARDS theo registrationEnd mới. */
    private final Boolean timelineCompressed;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class TeamAwaitingCoordinatorApprovalItem {
        private final Integer teamId;
        private final String teamName;
        private final String leaderName;
        private final int acceptedMemberCount;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class TeamInFormationGraceItem {
        private final Integer teamId;
        private final String teamName;
        private final String leaderName;
        private final int acceptedMemberCount;
        private final LocalDateTime formationGraceDeadlineAt;
    }
}
