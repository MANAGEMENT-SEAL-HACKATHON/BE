package com.sealhackathon.api.notifications.value_object;

/**
 * String constants for notification {@code type} values used by new notification code.
 * Existing call sites may still use raw strings until migrated.
 */
public final class NotificationType {

    public static final String COMPETITION_SCHEDULE_UPDATED = "COMPETITION_SCHEDULE_UPDATED";
    public static final String ROUND_SCHEDULE_UPDATED = "ROUND_SCHEDULE_UPDATED";
    /** Coordinator kết thúc thời gian thi / đóng cổng nộp sớm. */
    public static final String SUBMISSION_CLOSED_EARLY = "SUBMISSION_CLOSED_EARLY";
    public static final String EVENT_REMINDER = "EVENT_REMINDER";
    public static final String EVENT_UPCOMING = "EVENT_UPCOMING";
    public static final String REGISTRATION_EXTENDED = "REGISTRATION_EXTENDED";
    public static final String JUDGE_DECLINED = "JUDGE_DECLINED";
    public static final String MENTOR_DECLINED = "MENTOR_DECLINED";
    /** Future-ready — kit issuance fan-out. */
    public static final String KIT_ISSUED = "KIT_ISSUED";
    public static final String HACKATHON_BROADCAST = "HACKATHON_BROADCAST";

    public static final String APPEAL_WINDOW_OPENED = "APPEAL_WINDOW_OPENED";
    public static final String APPEAL_WINDOW_SKIPPED = "APPEAL_WINDOW_SKIPPED";
    public static final String APPEAL_SUBMITTED = "APPEAL_SUBMITTED";
    public static final String APPEAL_APPROVED = "APPEAL_APPROVED";
    public static final String APPEAL_REJECTED = "APPEAL_REJECTED";
    public static final String APPEAL_EXPIRED = "APPEAL_EXPIRED";
    public static final String RESULTS_REVISED = "RESULTS_REVISED";

    private NotificationType() {
    }
}
