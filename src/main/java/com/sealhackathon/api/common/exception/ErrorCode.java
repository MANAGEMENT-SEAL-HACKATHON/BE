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
    public static final String EMAIL_NOT_VERIFIED = "EMAIL_NOT_VERIFIED";
    public static final String EMAIL_VERIFICATION_TOKEN_INVALID = "EMAIL_VERIFICATION_TOKEN_INVALID";
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
    /** Close-early khi chưa phát đề. */
    public static final String INVALID_ROUND_STATE_UNRELEASED = "INVALID_ROUND_STATE_UNRELEASED";
    /** Close-early khi chưa tới examAt (server clock). */
    public static final String INVALID_ROUND_STATE_BEFORE_EXAM = "INVALID_ROUND_STATE_BEFORE_EXAM";
    /** Lock scoring khi vòng còn ONGOING (chưa close-early / chưa hết deadline). */
    public static final String INVALID_ROUND_STATE_NOT_CLOSED = "INVALID_ROUND_STATE_NOT_CLOSED";
    /** lockScoring — chưa xáo hàng đợi thuyết trình. */
    public static final String INVALID_ROUND_STATE_QUEUE_NOT_SHUFFLED = "INVALID_ROUND_STATE_QUEUE_NOT_SHUFFLED";
    /** lockScoring — còn slot WAITING/PRESENTING. */
    public static final String INVALID_ROUND_STATE_PRESENTATIONS_INCOMPLETE =
            "INVALID_ROUND_STATE_PRESENTATIONS_INCOMPLETE";
    /** lockScoring — còn bài chưa chấm (force có thể bypass). */
    public static final String INVALID_ROUND_STATE_SCORING_INCOMPLETE =
            "INVALID_ROUND_STATE_SCORING_INCOMPLETE";

    // ---------- FR-04 CRITERIA ----------
    public static final String CRITERIA_HAS_SCORES      = "CRITERIA_HAS_SCORES";
    public static final String CRITERIA_HAS_CLONE_DEPENDENTS = "CRITERIA_HAS_CLONE_DEPENDENTS";
    public static final String CRITERIA_WEIGHT_RANGE    = "CRITERIA_WEIGHT_RANGE";
    public static final String CRITERIA_CLONE_SOURCE_EMPTY = "CRITERIA_CLONE_SOURCE_EMPTY";
    public static final String CRITERIA_CLONE_CROSS_SCOPE = "CRITERIA_CLONE_CROSS_SCOPE";
    public static final String CRITERIA_TARGET_HAS_EXISTING = "CRITERIA_TARGET_HAS_EXISTING";
    /** Đã có criterion khác trên cùng track/final với is_tiebreaker_priority=true. */
    public static final String TIEBREAKER_PRIORITY_ALREADY_EXISTS = "TIEBREAKER_PRIORITY_ALREADY_EXISTS";
    /** PENALTY không được đánh dấu tiebreaker priority. */
    public static final String TIEBREAKER_PRIORITY_PENALTY_NOT_ALLOWED = "TIEBREAKER_PRIORITY_PENALTY_NOT_ALLOWED";

    // ---------- FR-06B ROUND ACTIVATE ----------
    public static final String ROUND_NO_CRITERIA        = "ROUND_NO_CRITERIA";
    public static final String ROUND_WEIGHT_NOT_ONE     = "ROUND_WEIGHT_NOT_ONE";

    // ---------- FR-05 PERSONNEL ----------
    public static final String USER_INVALID_ROLE        = "USER_INVALID_ROLE";
    public static final String USER_NOT_APPROVED        = "USER_NOT_APPROVED";
    public static final String USER_EMAIL_TAKEN         = "USER_EMAIL_TAKEN";
    public static final String INVITATION_NOT_FOUND     = "INVITATION_NOT_FOUND";
    public static final String INVITATION_ALREADY_ACCEPTED = "INVITATION_ALREADY_ACCEPTED";
    public static final String INVITATION_ALREADY_REVOKED  = "INVITATION_ALREADY_REVOKED";
    public static final String INVITATION_STILL_VALID     = "INVITATION_STILL_VALID";
    public static final String INVITATION_EXPIRED         = "INVITATION_EXPIRED";
    public static final String INVITATION_HACKATHON_REQUIRED = "INVITATION_HACKATHON_REQUIRED";
    public static final String INVITATION_RESEND_AFTER_KICKOFF_CUTOFF = "INVITATION_RESEND_AFTER_KICKOFF_CUTOFF";
    public static final String TEMP_JUDGE_HACKATHON_ENDED = "TEMP_JUDGE_HACKATHON_ENDED";
    public static final String PASSWORD_MISMATCH          = "PASSWORD_MISMATCH";
    public static final String NEW_PASSWORD_SAME_AS_CURRENT = "NEW_PASSWORD_SAME_AS_CURRENT";
    public static final String MENTOR_ASSIGN_DUPLICATE  = "MENTOR_ASSIGN_DUPLICATE";
    public static final String JUDGE_ASSIGN_DUPLICATE   = "JUDGE_ASSIGN_DUPLICATE";
    /** Một người (Mentor hoặc Judge) chỉ được gán 1 bảng trong cùng vòng Sơ loại. */
    public static final String PERSONNEL_ONE_TRACK_PER_ROUND = "PERSONNEL_ONE_TRACK_PER_ROUND";

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
    /** Buffet endsAt must be >= startsAt. */
    public static final String EVENT_BUFFET_OUT_OF_WINDOW = "EVENT_BUFFET_OUT_OF_WINDOW";
    /** Buffet must lie within [prelimEnd, final.examAt]. */
    public static final String EVENT_BUFFET_OUT_OF_BREAK = "EVENT_BUFFET_OUT_OF_BREAK";
    /** At most one BUFFET event per hackathon. */
    public static final String EVENT_BUFFET_DUPLICATE = "EVENT_BUFFET_DUPLICATE";
    /** Prelim/final rounds (with examAt) required to place BUFFET. */
    public static final String EVENT_BUFFET_ROUNDS_MISSING = "EVENT_BUFFET_ROUNDS_MISSING";
    /** Buffet event/menu locked after prelim results are published. */
    public static final String BUFFET_LOCKED_AFTER_PUBLISH = "BUFFET_LOCKED_AFTER_PUBLISH";
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
    public static final String ROUND_EXAM_OUTSIDE_AWARDS       = "ROUND_EXAM_OUTSIDE_AWARDS";

    // ---------- FR-07 READINESS (G1–G5) ----------
    public static final String MISSING_PRELIMINARY_ROUND = "MISSING_PRELIMINARY_ROUND";
    public static final String MISSING_FINAL_ROUND       = "MISSING_FINAL_ROUND";
    public static final String TRACK_CRITERIA_WEIGHT     = "TRACK_CRITERIA_WEIGHT";
    public static final String FINAL_CRITERIA_WEIGHT     = "FINAL_CRITERIA_WEIGHT";

    // ---------- DB TRIGGER / XOR (422) ----------
    public static final String CONFLICT_SAME_TRACK                  = "CONFLICT_SAME_TRACK";
    public static final String INTERNAL_JUDGE_NOT_ALLOWED_IN_FINAL  = "INTERNAL_JUDGE_NOT_ALLOWED_IN_FINAL";
    public static final String EXTERNAL_JUDGE_NOT_ALLOWED_IN_PRELIM = "EXTERNAL_JUDGE_NOT_ALLOWED_IN_PRELIM";
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
    public static final String REGISTRATION_ALREADY_CLOSED              = "REGISTRATION_ALREADY_CLOSED";
    public static final String REGISTRATION_WITHDRAWN                   = "REGISTRATION_WITHDRAWN";
    public static final String REGISTRATION_ALREADY_ACTIVE_ELSEWHERE    = "REGISTRATION_ALREADY_ACTIVE_ELSEWHERE";
    /** Đã dùng hết số lần dời hạn đăng ký. */
    public static final String REGISTRATION_EXTENSION_LIMIT_REACHED     = "REGISTRATION_EXTENSION_LIMIT_REACHED";
    /** Ngày hạn đăng ký mới không hợp lệ (không sau hạn hiện tại / không sau hôm nay). */
    public static final String REGISTRATION_EXTENSION_INVALID_DATE      = "REGISTRATION_EXTENSION_INVALID_DATE";
    /** Dời hạn ĐK làm xung đột mốc WS/KO/SL/eventStart — cần điều chỉnh lịch. */
    public static final String REGISTRATION_EXTENSION_TIMELINE_CONFLICT = "REGISTRATION_EXTENSION_TIMELINE_CONFLICT";
    /** Đã dời lịch thi 1 lần — không cho dời lại. */
    public static final String SCHEDULE_ALREADY_ADJUSTED                = "SCHEDULE_ALREADY_ADJUSTED";
    /** Còn dưới 4 ngày trước Kickoff — quá muộn để dời lịch. */
    public static final String SCHEDULE_ADJUST_TOO_LATE                 = "SCHEDULE_ADJUST_TOO_LATE";
    /** Ngày thi SL quá sớm — không đủ chỗ Workshop + Khai mạc. */
    public static final String SCHEDULE_ADJUST_PRELIM_TOO_SOON          = "SCHEDULE_ADJUST_PRELIM_TOO_SOON";
    public static final String TEAM_LEADER_NOT_APPROVED                 = "TEAM_LEADER_NOT_APPROVED";
    public static final String TEAM_LEADER_INVALID_ROLE                 = "TEAM_LEADER_INVALID_ROLE";
    public static final String USER_IN_ANOTHER_TEAM                     = "USER_IN_ANOTHER_TEAM";
    public static final String TEAM_LOCKED                              = "TEAM_LOCKED";
    public static final String TEAM_FORMATION_ALREADY_SUBMITTED         = "TEAM_FORMATION_ALREADY_SUBMITTED";
    public static final String TEAM_FORMATION_NOT_SUBMITTED             = "TEAM_FORMATION_NOT_SUBMITTED";
    public static final String TEAM_FORMATION_PENDING_INVITES           = "TEAM_FORMATION_PENDING_INVITES";
    public static final String TEAM_HAS_PENDING_MEMBERS                 = "TEAM_HAS_PENDING_MEMBERS";
    public static final String TEAM_HAS_UNAPPROVED_MEMBERS              = "TEAM_HAS_UNAPPROVED_MEMBERS";
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
    /** Leader chưa đăng ký hackathon — không tạo đội / mời. */
    public static final String LEADER_NOT_REGISTERED                    = "LEADER_NOT_REGISTERED";
    /** Người được mời chưa đăng ký cùng hackathon. */
    public static final String INVITEE_NOT_REGISTERED                   = "INVITEE_NOT_REGISTERED";
    public static final String NOT_IMPLEMENTED                          = "NOT_IMPLEMENTED";

    // ---------- MF-03 GĐ3–GĐ5 (GD03 v4.1 §7) ----------
    public static final String NO_TEAMS_IN_ROUND                        = "NO_TEAMS_IN_ROUND";
    /** Activate prelim: ít nhất một bảng đấu chưa có đội (every-track gate). */
    public static final String TRACK_EMPTY_TEAMS                        = "TRACK_EMPTY_TEAMS";
    public static final String JUDGE_NOT_ASSIGNED                       = "JUDGE_NOT_ASSIGNED";
    public static final String ROUND_NOT_ACTIVE                         = "ROUND_NOT_ACTIVE";
    public static final String TEAM_NOT_READY                           = "TEAM_NOT_READY";
    public static final String NOT_TEAM_MEMBER                          = "NOT_TEAM_MEMBER";
    public static final String TEAM_NOT_IN_TRACK                          = "TEAM_NOT_IN_TRACK";
    /** Đội ADVANCED/ELIMINATED — không được mutate Sơ loại (submit, relottery, …). */
    public static final String PRELIM_NOT_MUTABLE                         = "PRELIM_NOT_MUTABLE";
    public static final String TRACK_NOT_ALLOWED_FOR_FINAL                = "TRACK_NOT_ALLOWED_FOR_FINAL";
    public static final String SUBMISSION_NOT_GRADABLE                    = "SUBMISSION_NOT_GRADABLE";
    public static final String JUDGE_NOT_ASSIGNED_TO_TRACK                = "JUDGE_NOT_ASSIGNED_TO_TRACK";
    public static final String SCORE_EXCEEDS_MAX                          = "SCORE_EXCEEDS_MAX";
    public static final String SCORING_LOCKED                             = "SCORING_LOCKED";
    public static final String CONFLICT_MENTOR_JUDGE_SAME_TRACK           = "CONFLICT_MENTOR_JUDGE_SAME_TRACK";
    public static final String CRITERION_WRONG_ROUND                      = "CRITERION_WRONG_ROUND";
    public static final String INVALID_REPO_PLATFORM                      = "INVALID_REPO_PLATFORM";
    public static final String REPO_NOT_PUBLIC                            = "REPO_NOT_PUBLIC";
    public static final String INVALID_SLIDE_FORMAT                       = "INVALID_SLIDE_FORMAT";
    public static final String SLIDE_FILE_REQUIRED                        = "SLIDE_FILE_REQUIRED";
    public static final String INVALID_SLIDE_FILE                         = "INVALID_SLIDE_FILE";
    public static final String SCORING_NOT_OPEN                           = "SCORING_NOT_OPEN";
    public static final String SCORING_INCOMPLETE_BEFORE_NEXT             = "SCORING_INCOMPLETE_BEFORE_NEXT";
    public static final String SCORING_INCOMPLETE_BEFORE_CONFIRM          = "SCORING_INCOMPLETE_BEFORE_CONFIRM";
    public static final String NOT_TRACK_CONTROLLER                       = "NOT_TRACK_CONTROLLER";
    public static final String REVIEW_NOTE_REQUIRED                       = "REVIEW_NOTE_REQUIRED";
    public static final String LATE_REASON_REQUIRED                       = "LATE_REASON_REQUIRED";
    public static final String SUBMISSION_NOT_LATE_PENDING                = "SUBMISSION_NOT_LATE_PENDING";
    public static final String TIEBREAK_ALREADY_RESOLVED                  = "TIEBREAK_ALREADY_RESOLVED";
    public static final String TIEBREAK_UNRESOLVED                         = "TIEBREAK_UNRESOLVED";
    public static final String TEAM_NOT_ACCEPTING_INVITES                 = "TEAM_NOT_ACCEPTING_INVITES";
    public static final String LATE_PENDING_NOT_ALLOWED                   = "LATE_PENDING_NOT_ALLOWED";
    /** Đã quay số thuyết trình — không cho nộp / duyệt muộn. */
    public static final String SUBMISSION_LOCKED_AFTER_SHUFFLE            = "SUBMISSION_LOCKED_AFTER_SHUFFLE";
    public static final String SUBMISSION_ALREADY_CLOSED                  = "SUBMISSION_ALREADY_CLOSED";
    /** Close-early: còn đội eligible chưa nộp bài. */
    public static final String TEAMS_NOT_ALL_SUBMITTED                   = "TEAMS_NOT_ALL_SUBMITTED";
    /** Còn trong cửa sổ nộp — không cho shuffle. */
    public static final String SUBMISSION_NOT_CLOSED_FOR_SHUFFLE          = "SUBMISSION_NOT_CLOSED_FOR_SHUFFLE";
    public static final String SUBMISSION_NOT_STARTED                     = "SUBMISSION_NOT_STARTED";
    public static final String SUBMISSION_CLOSED                          = "SUBMISSION_CLOSED";
    public static final String EVENT_FINISHED                             = "EVENT_FINISHED";
    /** Đã bắt đầu thuyết trình — không shuffle lại. */
    public static final String PRESENTATION_ALREADY_STARTED               = "PRESENTATION_ALREADY_STARTED";
    /** Queue đã shuffle — retry idempotent (409). */
    public static final String PRESENTATION_ALREADY_SHUFFLED              = "PRESENTATION_ALREADY_SHUFFLED";
    /** Takeover/transfer race — controller đã đổi. */
    public static final String CONTROLLER_CONFLICT                        = "CONTROLLER_CONFLICT";
    /** Transfer sang judge offline / không có session. */
    public static final String JUDGE_OFFLINE                              = "JUDGE_OFFLINE";
    public static final String UNLOCK_REASON_REQUIRED                     = "UNLOCK_REASON_REQUIRED";
    public static final String PRIZE_CATALOG_LOCKED                       = "PRIZE_CATALOG_LOCKED";
    public static final String FORCE_LOCK_REASON_REQUIRED                 = "FORCE_LOCK_REASON_REQUIRED";
    public static final String ELIMINATION_REASON_REQUIRED                = "ELIMINATION_REASON_REQUIRED";
    public static final String DEPT_HEAD_NOT_CONFIRMED                    = "DEPT_HEAD_NOT_CONFIRMED";
    public static final String ROUND_NOT_SCORING_LOCKED                     = "ROUND_NOT_SCORING_LOCKED";
    public static final String TIEBREAK_REQUIRED                            = "TIEBREAK_REQUIRED";
    public static final String RESULT_NOT_PUBLISHED                         = "RESULT_NOT_PUBLISHED";
    public static final String TEAM_NOT_ADVANCING                           = "TEAM_NOT_ADVANCING";
    public static final String ROUND_HAS_ACTIVE_STATE                       = "ROUND_HAS_ACTIVE_STATE";
    public static final String TRACK_PARENT_ROUND_ACTIVE                    = "TRACK_PARENT_ROUND_ACTIVE";
    public static final String PRIZE_DUPLICATE                              = "PRIZE_DUPLICATE";
    public static final String HACKATHON_NOT_PENDING_CONFIRM                = "HACKATHON_NOT_PENDING_CONFIRM";
    public static final String NO_PRIZES_RECORDED                           = "NO_PRIZES_RECORDED";
    public static final String EXPORT_JOB_NOT_READY                         = "EXPORT_JOB_NOT_READY";
    public static final String TRACK_DELETE_HAS_SUBMISSIONS                 = "TRACK_DELETE_HAS_SUBMISSIONS";
    public static final String CONCURRENT_MODIFICATION                      = "CONCURRENT_MODIFICATION";
    public static final String ACTIVE_TEAMS_NOT_LOCKED                      = "ACTIVE_TEAMS_NOT_LOCKED";
    /** Còn đội PENDING (chờ duyệt / 24h grace / blocked) — chặn lottery + activate prelim. */
    public static final String TEAMS_PENDING_APPROVAL                       = "TEAMS_PENDING_APPROVAL";
    public static final String PRIZE_REVOKE_REASON_REQUIRED                 = "PRIZE_REVOKE_REASON_REQUIRED";
    public static final String PRIZE_REVOKE_CATEGORY_INVALID                = "PRIZE_REVOKE_CATEGORY_INVALID";
    /** Trao giải chỉ cho đội vào Chung kết (có TRP trên vòng final). */
    public static final String PRIZE_TEAM_NOT_FINALIST                      = "PRIZE_TEAM_NOT_FINALIST";

    // ---------- KITS ----------
    public static final String KIT_OUT_OF_STOCK                             = "KIT_OUT_OF_STOCK";
    public static final String KIT_ALREADY_ISSUED                           = "KIT_ALREADY_ISSUED";
    public static final String KIT_BUNDLE_EMPTY                             = "KIT_BUNDLE_EMPTY";
    public static final String KIT_ITEM_IN_BUNDLE                           = "KIT_ITEM_IN_BUNDLE";
    public static final String KIT_ITEM_NAME_REQUIRED                       = "KIT_ITEM_NAME_REQUIRED";
}
