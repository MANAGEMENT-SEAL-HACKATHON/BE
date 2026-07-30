package com.sealhackathon.api.common.audit;

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
    public static final String HACKATHON_CLONE          = "HACKATHON_CLONE";
    public static final String HACKATHON_UPDATE         = "HACKATHON_UPDATE";
    public static final String HACKATHON_DELETE         = "HACKATHON_DELETE";
    public static final String HACKATHON_STATUS_CHANGE  = "HACKATHON_STATUS_CHANGE";
    public static final String HACKATHON_READINESS_CHECK = "HACKATHON_READINESS_CHECK";
    public static final String HACKATHON_REGISTRATION_CLOSED_EARLY = "HACKATHON_REGISTRATION_CLOSED_EARLY";
    public static final String HACKATHON_REGISTRATION_EXTENDED = "HACKATHON_REGISTRATION_EXTENDED";

    // ---------- FR-02 TRACK ----------
    public static final String TRACK_CREATE             = "TRACK_CREATE";
    public static final String TRACK_UPDATE             = "TRACK_UPDATE";
    public static final String TRACK_DELETE             = "TRACK_DELETE";
    public static final String TRACK_TOPIC_UPDATE       = "TRACK_TOPIC_UPDATE";

    // ---------- FR-03 / FR-06B ROUND ----------
    public static final String ROUND_CREATE             = "ROUND_CREATE";
    public static final String ROUND_UPDATE             = "ROUND_UPDATE";
    public static final String ROUND_DELETE             = "ROUND_DELETE";
    public static final String ROUND_ACTIVATE           = "ROUND_ACTIVATE";
    public static final String ROUND_DEACTIVATE         = "ROUND_DEACTIVATE";
    public static final String ROUND_LOCK               = "ROUND_LOCK";
    public static final String ROUND_FORCE_LOCK         = "ROUND_FORCE_LOCK";
    public static final String ROUND_CLOSE_SUBMISSION_EARLY = "ROUND_CLOSE_SUBMISSION_EARLY";
    public static final String ROUND_SCHEDULE_SHIFTED = "ROUND_SCHEDULE_SHIFTED";


    // ---------- FR-04 CRITERIA ----------
    public static final String CRITERIA_CREATE          = "CRITERIA_CREATE";
    public static final String CRITERIA_CLONE           = "CRITERIA_CLONE";
    public static final String CRITERIA_UPDATE          = "CRITERIA_UPDATE";
    public static final String CRITERIA_DELETE          = "CRITERIA_DELETE";

    // ---------- MF-02 AUTH / ACCOUNT ----------
    public static final String ACCOUNT_REGISTER           = "ACCOUNT_REGISTER";
    public static final String ACCOUNT_APPROVE            = "ACCOUNT_APPROVE";
    public static final String ACCOUNT_REJECT             = "ACCOUNT_REJECT";
    public static final String ACCOUNT_STATUS_OVERRIDE    = "ACCOUNT_STATUS_OVERRIDE";
    public static final String ACCOUNT_LOGIN              = "ACCOUNT_LOGIN";
    public static final String ACCOUNT_LOGOUT             = "ACCOUNT_LOGOUT";
    public static final String ACCOUNT_LOGOUT_ALL         = "ACCOUNT_LOGOUT_ALL";
    public static final String ACCOUNT_PASSWORD_CHANGED   = "ACCOUNT_PASSWORD_CHANGED";
    public static final String ACCOUNT_PASSWORD_RESET_REQUESTED = "ACCOUNT_PASSWORD_RESET_REQUESTED";
    public static final String ACCOUNT_PASSWORD_RESET     = "ACCOUNT_PASSWORD_RESET";
    public static final String ACCOUNT_EMAIL_VERIFIED   = "ACCOUNT_EMAIL_VERIFIED";
    public static final String ACCOUNT_EMAIL_VERIFICATION_RESENT = "ACCOUNT_EMAIL_VERIFICATION_RESENT";
    public static final String ACCOUNT_OAUTH_LINKED       = "ACCOUNT_OAUTH_LINKED";
    public static final String ACCOUNT_OAUTH_UNLINKED     = "ACCOUNT_OAUTH_UNLINKED";

    // ---------- FR-05 PERSONNEL ----------
    public static final String TEMP_ACCOUNT_CREATE      = "TEMP_ACCOUNT_CREATE";
    public static final String INVITATION_RESEND        = "INVITATION_RESEND";
    public static final String INVITATION_CREATE        = "INVITATION_CREATE";
    public static final String INVITATION_REVOKE        = "INVITATION_REVOKE";
    public static final String USER_DEPT_HEAD_SET       = "USER_DEPT_HEAD_SET";
    public static final String MENTOR_ASSIGNED          = "MENTOR_ASSIGNED";
    public static final String MENTOR_UNASSIGNED        = "MENTOR_UNASSIGNED";
    public static final String JUDGE_ASSIGNED           = "JUDGE_ASSIGNED";
    public static final String JUDGE_UNASSIGNED         = "JUDGE_UNASSIGNED";
    public static final String JUDGE_DECLINED           = "JUDGE_DECLINED";
    public static final String MENTOR_DECLINED          = "MENTOR_DECLINED";
    /** Tạo/đổi/gỡ assignment_type HEAD — tra cứu thẩm quyền tiebreak/force-ack. */
    public static final String JUDGE_HEAD_CHANGED       = "JUDGE_HEAD_CHANGED";

    // ---------- FR-06A EVENTS ----------
    public static final String EVENT_CREATE             = "EVENT_CREATE";
    public static final String EVENT_UPDATE             = "EVENT_UPDATE";
    public static final String EVENT_DELETE             = "EVENT_DELETE";

    // ---------- MF-02 GĐ2 TEAMS ----------
    public static final String TEAM_CREATE              = "TEAM_CREATE";
    public static final String TEAM_UPDATE              = "TEAM_UPDATE";
    public static final String TEAM_FORMATION_CONFIRMED = "TEAM_FORMATION_CONFIRMED";
    public static final String TEAM_APPROVE             = "TEAM_APPROVE";
    public static final String TEAM_REJECT              = "TEAM_REJECT";
    public static final String TEAM_DISBAND             = "TEAM_DISBAND";
    public static final String TEAM_MEMBERS_RELEASED    = "TEAM_MEMBERS_RELEASED";
    public static final String TEAM_LOCKED              = "TEAM_LOCKED";
    public static final String LEADER_TRANSFERRED       = "LEADER_TRANSFERRED";
    public static final String MEMBER_INVITED           = "MEMBER_INVITED";
    public static final String MEMBER_INVITE_CANCELLED  = "MEMBER_INVITE_CANCELLED";
    public static final String MEMBER_ACCEPTED          = "MEMBER_ACCEPTED";
    public static final String MEMBER_REJECTED          = "MEMBER_REJECTED";
    public static final String MEMBER_LEFT              = "MEMBER_LEFT";
    public static final String TEAM_TRACK_ASSIGNED      = "TEAM_TRACK_ASSIGNED";
    public static final String TEAM_TRACK_CHANGED       = "TEAM_TRACK_CHANGED";
    public static final String MENTOR_TEAM_ASSIGNED     = "MENTOR_TEAM_ASSIGNED";
    public static final String MENTOR_TEAM_UNASSIGNED   = "MENTOR_TEAM_UNASSIGNED";

    // ---------- MF-03 GĐ3 SUBMISSIONS / SCORES ----------
    public static final String SUBMISSION_CREATE            = "SUBMISSION_CREATE";
    public static final String SUBMISSION_UPDATE            = "SUBMISSION_UPDATE";
    public static final String SUBMISSION_LATE_REVIEW       = "SUBMISSION_LATE_REVIEW";
    public static final String SUBMISSION_LATE_QUEUE_APPEND_FAILED = "SUBMISSION_LATE_QUEUE_APPEND_FAILED";
    public static final String PRESENTATION_QUEUE_SHUFFLE = "PRESENTATION_QUEUE_SHUFFLE";
    public static final String PRESENTATION_CONTROLLER_GRANTED = "PRESENTATION_CONTROLLER_GRANTED";
    public static final String PRESENTATION_CONTROLLER_REVOKED = "PRESENTATION_CONTROLLER_REVOKED";
    public static final String PRESENTATION_NO_SHOW_SKIPPED = "PRESENTATION_NO_SHOW_SKIPPED";
    public static final String PRESENTATION_FORCE_ADVANCE_ACK = "PRESENTATION_FORCE_ADVANCE_ACK";
    public static final String PRESENTATION_DURATION_UPDATED = "PRESENTATION_DURATION_UPDATED";
    public static final String ROUND_SCORING_UNLOCKED = "ROUND_SCORING_UNLOCKED";
    public static final String ANNOUNCEMENT_PUBLISHED = "ANNOUNCEMENT_PUBLISHED";
    public static final String ANNOUNCEMENT_SOFT_HIDE = "ANNOUNCEMENT_SOFT_HIDE";
    public static final String INVARIANT_VIOLATION_HARD_LOCK_LATE = "INVARIANT_VIOLATION_HARD_LOCK_LATE";
    public static final String PRIZE_AWARD_UPDATED = "PRIZE_AWARD_UPDATED";
    public static final String SCORE_UPSERT                 = "SCORE_UPSERT";
    public static final String TRACK_RELEASE_PROBLEM       = "TRACK_RELEASE_PROBLEM";
    public static final String ROUND_RELEASE_PROBLEM        = "ROUND_RELEASE_PROBLEM";
    /** CK migration: backup + clear legacy round-level problem PDF. */
    public static final String ROUND_FINAL_PROBLEM_PDF_CLEARED = "ROUND_FINAL_PROBLEM_PDF_CLEARED";
    public static final String ROUND_FINAL_PROBLEM_MIGRATION_BANNER_DISMISSED =
            "ROUND_FINAL_PROBLEM_MIGRATION_BANNER_DISMISSED";
    public static final String ROUND_PUBLISH                = "ROUND_PUBLISH";
    public static final String ROUND_ADVANCE_TEAMS          = "ROUND_ADVANCE_TEAMS";
    public static final String ROUND_TIEBREAK_RESOLVED      = "ROUND_TIEBREAK_RESOLVED";
    public static final String TEAM_ELIMINATE_MANUAL        = "TEAM_ELIMINATE_MANUAL";
    public static final String TEAM_ELIMINATE_DQ            = "TEAM_ELIMINATE_DQ";
    public static final String TOP_N_BACKFILL               = "TOP_N_BACKFILL";
    public static final String DQ_NO_BACKFILL_BENCH_EMPTY   = "DQ_NO_BACKFILL_BENCH_EMPTY";
    public static final String DQ_REJECTED_CK_ACTIVE        = "DQ_REJECTED_CK_ACTIVE";
    public static final String DQ_AFTER_FINISHED_LOG_ONLY   = "DQ_AFTER_FINISHED_LOG_ONLY";

    // ---------- MF-03 GĐ6 PRIZES ----------
    public static final String PRIZE_AWARDED              = "PRIZE_AWARDED";
    public static final String PRIZE_REVOKED              = "PRIZE_REVOKED";

    // ---------- MF-03 GĐ6 EXPORT ----------
    public static final String EXPORT_JOB_CREATED         = "EXPORT_JOB_CREATED";
    public static final String EXPORT_FILE_DOWNLOADED     = "EXPORT_FILE_DOWNLOADED";

    // ---------- KITS ----------
    public static final String KIT_ISSUED                   = "KIT_ISSUED";
    public static final String KIT_REVOKED                  = "KIT_REVOKED";

    // ---------- APPEALS (GĐ4 DQ) ----------
    public static final String APPEAL_SUBMIT                = "APPEAL_SUBMIT";
    public static final String APPEAL_CLAIM                 = "APPEAL_CLAIM";
    public static final String APPEAL_APPROVE               = "APPEAL_APPROVE";
    public static final String APPEAL_REJECT                = "APPEAL_REJECT";
    public static final String APPEAL_EXPIRE                = "APPEAL_EXPIRE";
    public static final String APPEAL_WINDOW_OPEN           = "APPEAL_WINDOW_OPEN";
    public static final String APPEAL_WINDOW_SHRUNK         = "APPEAL_WINDOW_SHRUNK";
    public static final String APPEAL_WINDOW_SKIPPED        = "APPEAL_WINDOW_SKIPPED";
    public static final String APPEAL_WINDOW_CLOSE_EARLY    = "APPEAL_WINDOW_CLOSE_EARLY";
    public static final String APPEAL_FINAL_DELAY           = "APPEAL_FINAL_DELAY";
    public static final String HACKATHON_APPEAL_WINDOW_UPDATE = "HACKATHON_APPEAL_WINDOW_UPDATE";
    public static final String TEAM_REINSTATE_APPEAL        = "TEAM_REINSTATE_APPEAL";
    public static final String ROUND_REPUBLISH              = "ROUND_REPUBLISH";

    // ---------- WARNINGS ----------
    public static final String WARNING_CONFLICT_CHECK_SKIPPED = "WARNING_CONFLICT_CHECK_SKIPPED";
    public static final String WARNING_EVENT_ORDER            = "WARNING_EVENT_ORDER";
    public static final String WARNING_WEIGHT_NOT_ONE         = "WARNING_WEIGHT_NOT_ONE";
    public static final String WARNING_JUDGE_FINAL_AT_PHASE1  = "WARNING_JUDGE_FINAL_AT_PHASE1";
}
