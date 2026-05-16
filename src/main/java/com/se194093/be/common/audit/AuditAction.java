package com.se194093.be.common.audit;

/**
 * Hằng số action code dùng cho cột {@code audit_logs.action}.
 *
 * <p>Convention: SNAKE_CASE in caps. Mỗi mutation MF-01 gọi {@code AuditService.log(...)}
 * với một trong các action sau.
 */
public final class AuditAction {

    private AuditAction() {}

    // ---------- FR-01 / FR-06 HACKATHON ----------
    public static final String HACKATHON_CREATE         = "HACKATHON_CREATE";
    public static final String HACKATHON_UPDATE         = "HACKATHON_UPDATE";
    public static final String HACKATHON_DELETE         = "HACKATHON_DELETE";
    public static final String HACKATHON_STATUS_CHANGE  = "HACKATHON_STATUS_CHANGE";
    public static final String HACKATHON_READINESS_CHECK = "HACKATHON_READINESS_CHECK";

    // ---------- FR-02 TRACK ----------
    public static final String TRACK_CREATE             = "TRACK_CREATE";
    public static final String TRACK_UPDATE             = "TRACK_UPDATE";
    public static final String TRACK_DELETE             = "TRACK_DELETE";

    // ---------- FR-03 / FR-06B ROUND ----------
    public static final String ROUND_CREATE             = "ROUND_CREATE";
    public static final String ROUND_UPDATE             = "ROUND_UPDATE";
    public static final String ROUND_DELETE             = "ROUND_DELETE";
    public static final String ROUND_ACTIVATE           = "ROUND_ACTIVATE";
    public static final String ROUND_DEACTIVATE         = "ROUND_DEACTIVATE";
    public static final String ROUND_LOCK               = "ROUND_LOCK";
    public static final String ROUND_FORCE_LOCK         = "ROUND_FORCE_LOCK";

    // ---------- FR-04 CRITERIA ----------
    public static final String CRITERIA_CREATE          = "CRITERIA_CREATE";
    public static final String CRITERIA_CLONE           = "CRITERIA_CLONE";
    public static final String CRITERIA_UPDATE          = "CRITERIA_UPDATE";
    public static final String CRITERIA_DELETE          = "CRITERIA_DELETE";

    // ---------- FR-05 PERSONNEL ----------
    public static final String TEMP_ACCOUNT_CREATE      = "TEMP_ACCOUNT_CREATE";
    public static final String INVITATION_RESEND        = "INVITATION_RESEND";
    public static final String MENTOR_ASSIGNED          = "MENTOR_ASSIGNED";
    public static final String MENTOR_UNASSIGNED        = "MENTOR_UNASSIGNED";
    public static final String JUDGE_ASSIGNED           = "JUDGE_ASSIGNED";
    public static final String JUDGE_UNASSIGNED         = "JUDGE_UNASSIGNED";

    // ---------- FR-06A EVENTS ----------
    public static final String EVENT_CREATE             = "EVENT_CREATE";
    public static final String EVENT_UPDATE             = "EVENT_UPDATE";
    public static final String EVENT_DELETE             = "EVENT_DELETE";

    // ---------- WARNINGS ----------
    public static final String WARNING_CONFLICT_CHECK_SKIPPED = "WARNING_CONFLICT_CHECK_SKIPPED";
    public static final String WARNING_EVENT_ORDER            = "WARNING_EVENT_ORDER";
    public static final String WARNING_WEIGHT_NOT_ONE         = "WARNING_WEIGHT_NOT_ONE";
    public static final String WARNING_JUDGE_FINAL_AT_PHASE1  = "WARNING_JUDGE_FINAL_AT_PHASE1";
}
