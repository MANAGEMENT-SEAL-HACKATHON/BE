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
    public static final String PASSWORD_RESET_TOKEN_INVALID = "PASSWORD_RESET_TOKEN_INVALID";
    public static final String OAUTH_TOKEN_INVALID      = "OAUTH_TOKEN_INVALID";
    public static final String OAUTH_ACCOUNT_NOT_LINKED = "OAUTH_ACCOUNT_NOT_LINKED";
    public static final String OAUTH_ACCOUNT_ALREADY_LINKED = "OAUTH_ACCOUNT_ALREADY_LINKED";
    public static final String OAUTH_EMAIL_MISMATCH = "OAUTH_EMAIL_MISMATCH";
    public static final String OAUTH_EMAIL_NOT_VERIFIED = "OAUTH_EMAIL_NOT_VERIFIED";
    public static final String OAUTH_PASSWORD_CONFIRM_REQUIRED = "OAUTH_PASSWORD_CONFIRM_REQUIRED";
    public static final String OAUTH_UNLINK_FORBIDDEN = "OAUTH_UNLINK_FORBIDDEN";

    // ---------- FR-01 HACKATHON ----------
    public static final String HACKATHON_DUPLICATE      = "HACKATHON_DUPLICATE";
    public static final String HACKATHON_DATE_RANGE     = "HACKATHON_DATE_RANGE";
    public static final String HACKATHON_NOT_DRAFT      = "HACKATHON_NOT_DRAFT";
    public static final String HACKATHON_HAS_CHILDREN   = "HACKATHON_HAS_CHILDREN";
    /** Hackathon FINISHED — read-only archive; mọi mutation con bị chặn. */
    public static final String HACKATHON_ARCHIVED       = "HACKATHON_ARCHIVED";

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

    // ---------- MF-02 GĐ2 TEAMS (FR-11 … FR-13C) — dùng khi implement logic ----------
    public static final String CROSS_HACKATHON_VIOLATION                = "CROSS_HACKATHON_VIOLATION";
    public static final String TEAM_NAME_DUPLICATE                    = "TEAM_NAME_DUPLICATE";
    public static final String HACKATHON_NOT_ONGOING                    = "HACKATHON_NOT_ONGOING";
    public static final String REGISTRATION_CLOSED                      = "REGISTRATION_CLOSED";
    public static final String TEAM_LEADER_NOT_APPROVED                 = "TEAM_LEADER_NOT_APPROVED";
    public static final String TEAM_LEADER_INVALID_ROLE                 = "TEAM_LEADER_INVALID_ROLE";
    public static final String USER_IN_ANOTHER_TEAM                     = "USER_IN_ANOTHER_TEAM";
    public static final String TEAM_LOCKED                              = "TEAM_LOCKED";
    public static final String NEW_LEADER_NOT_MEMBER                    = "NEW_LEADER_NOT_MEMBER";
    public static final String NEW_LEADER_NOT_APPROVED                  = "NEW_LEADER_NOT_APPROVED";
    public static final String TEAM_ALREADY_ACTIVE                      = "TEAM_ALREADY_ACTIVE";
    public static final String TEAM_HAS_MENTOR_CANNOT_DISBAND           = "TEAM_HAS_MENTOR_CANNOT_DISBAND";
    public static final String TEAM_INVALID_MEMBER_COUNT                = "TEAM_INVALID_MEMBER_COUNT";
    public static final String TEAM_NOT_ACTIVE                          = "TEAM_NOT_ACTIVE";
    public static final String TEAM_NOT_LOCKED                          = "TEAM_NOT_LOCKED";
    public static final String TEAM_ALREADY_IN_TRACK_THIS_ROUND         = "TEAM_ALREADY_IN_TRACK_THIS_ROUND";
    public static final String TEAM_ROUND_PARTICIPATION_MISSING         = "TEAM_ROUND_PARTICIPATION_MISSING";
    public static final String TRACK_CLOSED                             = "TRACK_CLOSED";
    public static final String TRACK_GROUP_FULL                         = "TRACK_GROUP_FULL";
    public static final String ROUND_ALREADY_ACTIVE                     = "ROUND_ALREADY_ACTIVE";
    public static final String TEAM_ALREADY_HAS_MENTOR_IN_ROUND         = "TEAM_ALREADY_HAS_MENTOR_IN_ROUND";
    public static final String INVALID_MENTOR_FOR_TEAM                  = "INVALID_MENTOR_FOR_TEAM";
    public static final String MENTOR_ASSIGNMENT_NOT_FOR_FINAL_ROUND    = "MENTOR_ASSIGNMENT_NOT_FOR_FINAL_ROUND";
    public static final String TEAM_NOT_IN_ROUND                        = "TEAM_NOT_IN_ROUND";
    public static final String MENTOR_TEAM_CROSS_HACKATHON              = "MENTOR_TEAM_CROSS_HACKATHON";
    public static final String LEADER_CANNOT_LEAVE_TEAM                 = "LEADER_CANNOT_LEAVE_TEAM";
    public static final String DUPLICATE_PENDING_INVITATION             = "DUPLICATE_PENDING_INVITATION";
    public static final String ROUND_HAS_SCORES                         = "ROUND_HAS_SCORES";
    public static final String CANNOT_DELETE_ACCEPTED_MEMBER            = "CANNOT_DELETE_ACCEPTED_MEMBER";
    public static final String TEAM_ALREADY_PARTICIPATES_IN_ROUND       = "TEAM_ALREADY_PARTICIPATES_IN_ROUND";
    public static final String TEAM_MEMBER_FULL                         = "TEAM_MEMBER_FULL";
    public static final String INVITEE_NOT_APPROVED                     = "INVITEE_NOT_APPROVED";
    public static final String INVITEE_INVALID_ROLE                     = "INVITEE_INVALID_ROLE";
    public static final String NOT_IMPLEMENTED                          = "NOT_IMPLEMENTED";

    // ---------- MF-03 GĐ3–GĐ5 (GD03 §7.1) ----------
    public static final String NO_TEAMS_IN_ROUND                        = "NO_TEAMS_IN_ROUND";
    public static final String JUDGE_NOT_ASSIGNED                       = "JUDGE_NOT_ASSIGNED";
    public static final String ROUND_NOT_ACTIVE                         = "ROUND_NOT_ACTIVE";
    public static final String TEAM_NOT_IN_TRACK                          = "TEAM_NOT_IN_TRACK";
    public static final String TRACK_NOT_ALLOWED_FOR_FINAL                = "TRACK_NOT_ALLOWED_FOR_FINAL";
    public static final String SUBMISSION_NOT_GRADABLE                    = "SUBMISSION_NOT_GRADABLE";
    public static final String JUDGE_NOT_ASSIGNED_TO_TRACK                = "JUDGE_NOT_ASSIGNED_TO_TRACK";
    public static final String SCORE_EXCEEDS_MAX                          = "SCORE_EXCEEDS_MAX";
    public static final String SCORING_LOCKED                             = "SCORING_LOCKED";
    public static final String CONFLICT_MENTOR_JUDGE_SAME_TRACK           = "CONFLICT_MENTOR_JUDGE_SAME_TRACK";
    public static final String CRITERION_WRONG_TRACK                      = "CRITERION_WRONG_TRACK";
    public static final String LATE_PENDING_NOT_ALLOWED                   = "LATE_PENDING_NOT_ALLOWED";
    /** GD03 §7.1 — alias nghiệp vụ; {@link #ROUND_FORCE_LOCK_REASON} giữ cho FR-03 round update. */
    public static final String FORCE_LOCK_REASON_REQUIRED                 = "FORCE_LOCK_REASON_REQUIRED";
    public static final String DEPT_HEAD_NOT_CONFIRMED                    = "DEPT_HEAD_NOT_CONFIRMED";
    public static final String ROUND_NOT_SCORING_LOCKED                     = "ROUND_NOT_SCORING_LOCKED";
    public static final String TIEBREAK_REQUIRED                            = "TIEBREAK_REQUIRED";
    public static final String TEAM_NOT_ADVANCING                           = "TEAM_NOT_ADVANCING";
    public static final String ROUND_HAS_ACTIVE_STATE                       = "ROUND_HAS_ACTIVE_STATE";
    public static final String TRACK_PARENT_ROUND_ACTIVE                    = "TRACK_PARENT_ROUND_ACTIVE";
    public static final String PRIZE_DUPLICATE                              = "PRIZE_DUPLICATE";
    public static final String HACKATHON_NOT_PENDING_CONFIRM                = "HACKATHON_NOT_PENDING_CONFIRM";
    public static final String TRACK_DELETE_HAS_SUBMISSIONS                 = "TRACK_DELETE_HAS_SUBMISSIONS";
}
