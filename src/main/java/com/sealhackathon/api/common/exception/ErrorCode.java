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
    public static final String ROUND_PRELIM_DEADLINE_AFTER_FINAL_EXAM = "ROUND_PRELIM_DEADLINE_AFTER_FINAL_EXAM";
    public static final String ROUND_FINAL_DEADLINE_AFTER_AWARDS = "ROUND_FINAL_DEADLINE_AFTER_AWARDS";
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
    public static final String EVENT_PRESENTATION_MISSING = "EVENT_PRESENTATION_MISSING";
    public static final String EVENT_AWARDS_MISSING       = "EVENT_AWARDS_MISSING";
    public static final String EVENT_ORDER_VIOLATION    = "EVENT_ORDER_VIOLATION";
    public static final String EVENT_LOCATION_REQUIRED  = "EVENT_LOCATION_REQUIRED";
    public static final String EVENT_MILESTONE_DUPLICATE = "EVENT_MILESTONE_DUPLICATE";
    public static final String EVENT_CONFLICTS_WITH_MILESTONE = "EVENT_CONFLICTS_WITH_MILESTONE";
    public static final String AWARDS_NEEDS_COMPETITION       = "AWARDS_NEEDS_COMPETITION";
    public static final String AWARDS_BEFORE_COMPETITION_END  = "AWARDS_BEFORE_COMPETITION_END";
    /** AWARDS.startsAt phải sau round Chung kết submissionDeadline */
    public static final String AWARDS_BEFORE_FINAL_DEADLINE   = "AWARDS_BEFORE_FINAL_DEADLINE";
    public static final String PRESENTATION_BEFORE_FINAL_EXAM = "PRESENTATION_BEFORE_FINAL_EXAM";
    public static final String ROUND_TYPE_DUPLICATE           = "ROUND_TYPE_DUPLICATE";

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
