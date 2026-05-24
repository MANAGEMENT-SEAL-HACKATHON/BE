package com.sealhackathon.api.common.exception;

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
    public static final String UNAUTHORIZED               = "UNAUTHORIZED";
    public static final String INTERNAL_ERROR           = "INTERNAL_ERROR";
    public static final String INVALID_STATE            = "INVALID_STATE";

    // ---------- MF-02 AUTH / ACCOUNT ----------
    public static final String INVALID_CREDENTIALS      = "INVALID_CREDENTIALS";
    public static final String ACCOUNT_DUPLICATE_EMAIL    = "ACCOUNT_DUPLICATE_EMAIL";
    public static final String INVITATION_REQUIRED        = "INVITATION_REQUIRED";
    public static final String INVITATION_TOKEN_INVALID = "TOKEN_INVALID";
    public static final String INVITATION_TOKEN_EXPIRED = "TOKEN_EXPIRED";
    public static final String INVITATION_TOKEN_USED     = "TOKEN_USED";
    public static final String ACCOUNT_PENDING_NOT_ALLOWED_LOGIN = "ACCOUNT_PENDING";
    public static final String ACCOUNT_REJECTED_NOT_ALLOWED_LOGIN = "REJECTED_NOT_ALLOWED_LOGIN";
    public static final String INSTITUTION_REQUIRED     = "INSTITUTION_REQUIRED";
    public static final String INVALID_CHAPTER          = "INVALID_CHAPTER";
    public static final String STUDENT_CODE_REQUIRED    = "STUDENT_CODE_REQUIRED";
    public static final String STUDENT_CODE_DUPLICATE   = "STUDENT_CODE_DUPLICATE";
    public static final String INVITATION_PENDING_EXISTS = "INVITATION_PENDING_EXISTS";
    public static final String USER_TYPE_LOCKED         = "USER_TYPE_LOCKED";
    public static final String REJECTION_REASON_REQUIRED = "REJECTION_REASON_REQUIRED";
    public static final String INVALID_STATUS_TRANSITION = "INVALID_STATUS_TRANSITION";
    public static final String REFRESH_TOKEN_INVALID    = "REFRESH_TOKEN_INVALID";
    public static final String EMAIL_VERIFY_TOKEN_INVALID = "EMAIL_VERIFY_TOKEN_INVALID";

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
    public static final String TRACK_NOT_CANCELLED      = "TRACK_NOT_CANCELLED";
    public static final String TRACK_HAS_CRITERIA       = "TRACK_HAS_CRITERIA";
    public static final String TRACK_CANCEL_HAS_TEAMS     = "TRACK_CANCEL_HAS_TEAMS";
    public static final String TRACK_SEQUENCE_DUPLICATE   = "TRACK_SEQUENCE_DUPLICATE";

    // ---------- FR-03 ROUND ----------
    public static final String ROUND_DEADLINE_INVALID   = "ROUND_DEADLINE_INVALID";
    /** @deprecated dùng {@link #ROUND_FINAL_EXAM_ORDER} */
    @Deprecated
    public static final String ROUND_FINAL_SEQUENCE_ORDER = "ROUND_FINAL_EXAM_ORDER";
    public static final String ROUND_FINAL_EXAM_ORDER     = "ROUND_FINAL_EXAM_ORDER";
    public static final String ROUND_PRELIM_EXAM_ORDER    = "ROUND_PRELIM_EXAM_ORDER";
    public static final String ROUND_DUPLICATE_FINAL      = "ROUND_DUPLICATE_FINAL";
    public static final String ROUND_FINAL_REQUIRES_PRELIM = "ROUND_FINAL_REQUIRES_PRELIM";
    public static final String ROUND_EXAM_BEFORE_SUBMISSION_OPEN = "ROUND_EXAM_BEFORE_SUBMISSION_OPEN";
    public static final String ROUND_HAS_CRITERIA       = "ROUND_HAS_CRITERIA";
    public static final String ROUND_FORCE_LOCK_REASON  = "ROUND_FORCE_LOCK_REASON";
    public static final String ROUND_HAS_SUBMISSIONS    = "ROUND_HAS_SUBMISSIONS";
    public static final String ROUND_ANOTHER_ACTIVE     = "ROUND_ANOTHER_ACTIVE";

    // ---------- FR-04 CRITERIA ----------
    public static final String CRITERIA_HAS_SCORES      = "CRITERIA_HAS_SCORES";
    public static final String CRITERIA_HAS_CLONE_DEPENDENTS = "CRITERIA_HAS_CLONE_DEPENDENTS";
    public static final String CRITERIA_WEIGHT_RANGE    = "CRITERIA_WEIGHT_RANGE";
    public static final String CRITERIA_CLONE_SOURCE_EMPTY = "CRITERIA_CLONE_SOURCE_EMPTY";
    public static final String CRITERIA_CLONE_CROSS_SCOPE = "CRITERIA_CLONE_CROSS_SCOPE";
    public static final String CRITERIA_TARGET_HAS_EXISTING = "CRITERIA_TARGET_HAS_EXISTING";

    // ---------- FR-06B ROUND ACTIVATE ----------
    public static final String ROUND_NO_CRITERIA        = "ROUND_NO_CRITERIA";
    public static final String ROUND_WEIGHT_NOT_ONE     = "ROUND_WEIGHT_NOT_ONE";

    // ---------- FR-05 PERSONNEL ----------
    public static final String USER_INVALID_ROLE        = "USER_INVALID_ROLE";
    public static final String USER_NOT_APPROVED        = "USER_NOT_APPROVED";
    public static final String USER_EMAIL_TAKEN         = "USER_EMAIL_TAKEN";
    public static final String INVITATION_NOT_FOUND     = "INVITATION_NOT_FOUND";
    public static final String INVITATION_ALREADY_ACCEPTED = "INVITATION_ALREADY_ACCEPTED";
    public static final String INVITATION_STILL_VALID     = "INVITATION_STILL_VALID";
    public static final String INVITATION_EXPIRED         = "INVITATION_EXPIRED";
    public static final String INVITATION_HACKATHON_REQUIRED = "INVITATION_HACKATHON_REQUIRED";
    public static final String INVITATION_RESEND_AFTER_KICKOFF_CUTOFF = "INVITATION_RESEND_AFTER_KICKOFF_CUTOFF";
    public static final String TEMP_JUDGE_HACKATHON_ENDED = "TEMP_JUDGE_HACKATHON_ENDED";
    public static final String PASSWORD_MISMATCH          = "PASSWORD_MISMATCH";
    public static final String NEW_PASSWORD_SAME_AS_CURRENT = "NEW_PASSWORD_SAME_AS_CURRENT";
    public static final String MENTOR_ASSIGN_DUPLICATE  = "MENTOR_ASSIGN_DUPLICATE";
    public static final String JUDGE_ASSIGN_DUPLICATE   = "JUDGE_ASSIGN_DUPLICATE";

    // ---------- FR-06 STATUS ----------
    public static final String STATUS_TRANSITION_INVALID = "STATUS_TRANSITION_INVALID";
    public static final String READINESS_NOT_PASSED      = "READINESS_NOT_PASSED";

    // ---------- FR-06A EVENTS ----------
    public static final String EVENT_OUT_OF_HACKATHON   = "EVENT_OUT_OF_HACKATHON";
    public static final String EVENT_OVERLAP            = "EVENT_OVERLAP";
    public static final String EVENT_END_BEFORE_START   = "EVENT_END_BEFORE_START";
    public static final String EVENT_END_REQUIRED       = "EVENT_END_REQUIRED";
    public static final String EVENT_KICKOFF_MISSING    = "EVENT_KICKOFF_MISSING";
    public static final String EVENT_KICKOFF_NOT_FOUND  = "EVENT_KICKOFF_NOT_FOUND";
    public static final String EVENT_PRESENTATION_MISSING = "EVENT_PRESENTATION_MISSING";
    public static final String EVENT_AWARDS_MISSING       = "EVENT_AWARDS_MISSING";
    public static final String EVENT_ORDER_VIOLATION    = "EVENT_ORDER_VIOLATION";
    public static final String EVENT_LOCATION_REQUIRED  = "EVENT_LOCATION_REQUIRED";
    public static final String EVENT_MILESTONE_DUPLICATE = "EVENT_MILESTONE_DUPLICATE";
    public static final String EVENT_CONFLICTS_WITH_MILESTONE = "EVENT_CONFLICTS_WITH_MILESTONE";

    // ---------- FR-03 ROUND examAt vs events ----------
    public static final String ROUND_EXAM_BEFORE_KICKOFF       = "ROUND_EXAM_BEFORE_KICKOFF";
    public static final String ROUND_EXAM_OUTSIDE_PRESENTATION = "ROUND_EXAM_OUTSIDE_PRESENTATION";
    /** @deprecated dùng {@link #ROUND_EXAM_OUTSIDE_AWARDS} */
    @Deprecated
    public static final String ROUND_EXAM_ON_OR_AFTER_AWARDS   = "ROUND_EXAM_OUTSIDE_AWARDS";
    public static final String ROUND_EXAM_OUTSIDE_AWARDS       = "ROUND_EXAM_OUTSIDE_AWARDS";

    // ---------- FR-07 READINESS (G1–G5) ----------
    public static final String MISSING_PRELIMINARY_ROUND = "MISSING_PRELIMINARY_ROUND";
    public static final String MISSING_FINAL_ROUND       = "MISSING_FINAL_ROUND";
    public static final String TRACK_CRITERIA_WEIGHT     = "TRACK_CRITERIA_WEIGHT";
    public static final String FINAL_CRITERIA_WEIGHT     = "FINAL_CRITERIA_WEIGHT";

    // ---------- DB TRIGGER / XOR (422) ----------
    public static final String CONFLICT_SAME_TRACK                  = "CONFLICT_SAME_TRACK";
    public static final String INTERNAL_JUDGE_NOT_ALLOWED_IN_FINAL  = "INTERNAL_JUDGE_NOT_ALLOWED_IN_FINAL";
    public static final String INTERNAL_MENTOR_NOT_ALLOWED_IN_FINAL = "INTERNAL_MENTOR_NOT_ALLOWED_IN_FINAL";
    public static final String INVALID_ASSIGNMENT_TYPE              = "INVALID_ASSIGNMENT_TYPE";
    public static final String INVALID_FINAL_ROUND                  = "INVALID_FINAL_ROUND";
    public static final String DESIGN_VIOLATION                     = "DESIGN_VIOLATION";
    public static final String INVALID_ROUND_FOR_CRITERIA         = "INVALID_ROUND_FOR_CRITERIA";
    public static final String FINAL_JUDGE_CANNOT_BE_MENTOR         = "FINAL_JUDGE_CANNOT_BE_MENTOR";
    public static final String ROUND_NOT_FINAL_FOR_CRITERIA         = "ROUND_NOT_FINAL_FOR_CRITERIA";
    public static final String JUDGE_FINAL_AT_PHASE1                  = "JUDGE_FINAL_AT_PHASE1";
}
