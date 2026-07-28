package com.sealhackathon.api.presentation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PresentationControllerResponse {

    private Integer judgeId;
    private String judgeName;
    /** Tên đầy đủ — alias cho FE */
    private String judgeFullName;
    /** OVERRIDE = coordinator chỉ định; AUTO_DEFAULT = tự chọn theo rule guard */
    private String source;
    private Boolean isDeptHead;
    /** ISO-8601 last heartbeat */
    private String lastSeenAt;
    /** lastSeen within 90s */
    private Boolean online;
}
