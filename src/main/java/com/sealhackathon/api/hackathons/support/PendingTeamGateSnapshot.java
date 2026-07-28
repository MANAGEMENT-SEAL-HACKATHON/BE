package com.sealhackathon.api.hackathons.support;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Phân loại đội PENDING trước bốc thăm / kích hoạt sơ loại.
 * Không gộp thành 1 con số: awaiting (đã confirm) / grace (còn 24h) / blocked khác.
 */
public record PendingTeamGateSnapshot(
        int awaitingApprovalCount,
        int graceCount,
        int blockedOtherCount,
        LocalDateTime earliestGraceDeadlineAt
) {
    public int total() {
        return awaitingApprovalCount + graceCount + blockedOtherCount;
    }

    public boolean hasPending() {
        return total() > 0;
    }

    public String message() {
        StringBuilder sb = new StringBuilder("Còn ")
                .append(total())
                .append(" đội đang chờ xử lý trước khi bốc thăm / kích hoạt sơ loại. ");
        if (awaitingApprovalCount > 0) {
            sb.append(awaitingApprovalCount)
                    .append(" đội đã xác nhận — đang chờ bạn duyệt. ");
        }
        if (graceCount > 0) {
            sb.append(graceCount)
                    .append(" đội chưa xác nhận — còn trong 24h suy nghĩ");
            if (earliestGraceDeadlineAt != null) {
                sb.append(" (hạn gần nhất ")
                        .append(earliestGraceDeadlineAt.toLocalDate())
                        .append(' ')
                        .append(String.format("%02d:%02d",
                                earliestGraceDeadlineAt.getHour(),
                                earliestGraceDeadlineAt.getMinute()))
                        .append(')');
            }
            sb.append(". ");
        }
        if (blockedOtherCount > 0) {
            sb.append(blockedOtherCount)
                    .append(" đội cần xem lại / từ chối. ");
        }
        sb.append("Duyệt hoặc từ chối hết trước khi tiếp tục.");
        return sb.toString().trim();
    }

    public Map<String, Object> details() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("pendingTotal", total());
        map.put("awaitingApprovalCount", awaitingApprovalCount);
        map.put("graceCount", graceCount);
        map.put("blockedOtherCount", blockedOtherCount);
        if (earliestGraceDeadlineAt != null) {
            map.put("earliestGraceDeadlineAt", earliestGraceDeadlineAt.toString());
        }
        return map;
    }
}
