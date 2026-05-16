package com.se194093.be.common.exception;

/**
 * Tập hợp mã lỗi business (4xx) dùng xuyên suốt MF-01.
 *
 * <p>Mỗi lỗi có 1 mã ngắn (snake_case in caps) để client/UI mapping. Tách thành nhóm:
 * <ul>
 *   <li><b>Common</b>: chung cho mọi feature</li>
 *   <li><b>Hackathon (FR-01, FR-06)</b></li>
 *   <li><b>Track (FR-02)</b></li>
 *   <li><b>Round (FR-03, FR-06B)</b></li>
 *   <li><b>Criteria (FR-04)</b></li>
 *   <li><b>Personnel (FR-05)</b></li>
 *   <li><b>Events (FR-06A)</b></li>
 * </ul>
 */
public final class ErrorCode {

    private ErrorCode() {}

    // ---------- COMMON ----------
    public static final String VALIDATION_FAILED        = "VALIDATION_FAILED";
    public static final String RESOURCE_NOT_FOUND       = "RESOURCE_NOT_FOUND";
    public static final String FORBIDDEN                = "FORBIDDEN";
    public static final String INTERNAL_ERROR           = "INTERNAL_ERROR";
    public static final String INVALID_STATE            = "INVALID_STATE";

    // ---------- FR-01 HACKATHON ----------
    public static final String HACKATHON_DUPLICATE      = "HACKATHON_DUPLICATE";
    public static final String HACKATHON_DATE_RANGE     = "HACKATHON_DATE_RANGE";
    public static final String HACKATHON_NOT_DRAFT      = "HACKATHON_NOT_DRAFT";
    public static final String HACKATHON_HAS_CHILDREN   = "HACKATHON_HAS_CHILDREN";

    // ---------- FR-02 TRACK ----------
    public static final String TRACK_HAS_TEAMS          = "TRACK_HAS_TEAMS";
    public static final String TRACK_HAS_ACTIVE_ROUND   = "TRACK_HAS_ACTIVE_ROUND";
    public static final String TRACK_INVALID_TEAM_SIZE  = "TRACK_INVALID_TEAM_SIZE";
    public static final String TRACK_INVALID_GROUP_CAP  = "TRACK_INVALID_GROUP_CAP";
    public static final String TRACK_HACKATHON_LOCKED   = "TRACK_HACKATHON_LOCKED";

    // ---------- FR-03 ROUND ----------
    public static final String ROUND_DEADLINE_INVALID   = "ROUND_DEADLINE_INVALID";
    public static final String ROUND_FORCE_LOCK_REASON  = "ROUND_FORCE_LOCK_REASON";
    public static final String ROUND_HAS_SUBMISSIONS    = "ROUND_HAS_SUBMISSIONS";
    public static final String ROUND_ANOTHER_ACTIVE     = "ROUND_ANOTHER_ACTIVE";

    // ---------- FR-04 CRITERIA ----------
    public static final String CRITERIA_HAS_SCORES      = "CRITERIA_HAS_SCORES";
    public static final String CRITERIA_WEIGHT_RANGE    = "CRITERIA_WEIGHT_RANGE";
    public static final String CRITERIA_CLONE_SOURCE_EMPTY = "CRITERIA_CLONE_SOURCE_EMPTY";

    // ---------- FR-06B ROUND ACTIVATE ----------
    public static final String ROUND_NO_CRITERIA        = "ROUND_NO_CRITERIA";
    public static final String ROUND_WEIGHT_NOT_ONE     = "ROUND_WEIGHT_NOT_ONE";

    // ---------- FR-05 PERSONNEL ----------
    public static final String USER_INVALID_ROLE        = "USER_INVALID_ROLE";
    public static final String USER_NOT_APPROVED        = "USER_NOT_APPROVED";
    public static final String USER_EMAIL_TAKEN         = "USER_EMAIL_TAKEN";
    public static final String INVITATION_NOT_FOUND     = "INVITATION_NOT_FOUND";
    public static final String INVITATION_ALREADY_ACCEPTED = "INVITATION_ALREADY_ACCEPTED";
    public static final String MENTOR_ASSIGN_DUPLICATE  = "MENTOR_ASSIGN_DUPLICATE";
    public static final String JUDGE_ASSIGN_DUPLICATE   = "JUDGE_ASSIGN_DUPLICATE";

    // ---------- FR-06 STATUS ----------
    public static final String STATUS_TRANSITION_INVALID = "STATUS_TRANSITION_INVALID";
    public static final String READINESS_NOT_PASSED      = "READINESS_NOT_PASSED";

    // ---------- FR-06A EVENTS ----------
    public static final String EVENT_OUT_OF_HACKATHON   = "EVENT_OUT_OF_HACKATHON";
    public static final String EVENT_OVERLAP            = "EVENT_OVERLAP";
    public static final String EVENT_END_BEFORE_START   = "EVENT_END_BEFORE_START";
    public static final String EVENT_KICKOFF_MISSING    = "EVENT_KICKOFF_MISSING";
}
