# Business Rules Catalog — SEAL Hackathon Backend

**Generated:** 2026-07-21 · **Grain:** B1 (1 rule ≈ 1 ErrorCode / guard / invariant) · **Source of truth:** Java BE enforce

## Mục đích

Catalog thống nhất các business rules đang được enforce trong backend, dạng bảng 11 cột để BA/QA/Dev truy xuất. Tài liệu MF theo giai đoạn (`mf01/01-business-rules.md`, `mf02/01-business-rules-gd2.md`, `mf03/01-business-rules-gd3.md`, `mf03/01-business-rules-gd6.md`) **vẫn giữ nguyên** — file này bổ sung, không thay thế.

## Nguồn sự thật & ưu tiên khi mâu thuẫn

1. `docs/db/schema-v3.0-mysql.md` (constraint / trigger DB)
2. **Code enforce** (`*ServiceImpl`, `*Rules`, `*Policy`, `*Validator`, `ErrorCode`)
3. MF business-rules docs theo giai đoạn
4. `docs/system/workflow.md`

**Related Req ID** map tới `FR-*` trong [`docs/FUNCTIONAL-REQUIREMENTS-BACKLOG.md`](../../docs/FUNCTIONAL-REQUIREMENTS-BACKLOG.md). Rule chỉ có trong code → `N/A`.

## Quy ước

| Trường | Ý nghĩa |
|--------|---------|
| Rule ID | `BR-{MODULE}-{NNN}` — ổn định trong file này |
| Related Req ID | `FR-*` hoặc `N/A` |
| Module | Nhóm nghiệp vụ (Auth, Round, Team, …) |
| Rule Type | `Validation` · `Authorization` · `StateTransition` · `Invariant` · `SideEffect` · `Gate` · `Policy` · `Scheduler` · `Lifecycle` · `Access` · `Audit` · `Calculation` · `Design` · `Gap` |
| Status | `Implemented` · `Partial` · `Spec-only` · `Deprecated` · `Gap` · `Removed/Disabled` — theo **code** |
| Evidence | Path Java tương đối dưới `src/main/java/com/sealhackathon/api/` |

## Thống kê

| Metric | Giá trị |
|--------|--------:|
| Tổng rules | 531 |
| Modules | 34 |
| ErrorCode constants | 235 |
| ErrorCode xuất hiện trong catalog | 202 |
| ErrorCode được reference trong src | 198 |

## Mục lục theo Module

- [Auth](#auth) (43)
- [UserAdmin](#useradmin) (12)
- [Hackathon](#hackathon) (15)
- [Readiness](#readiness) (14)
- [Registration](#registration) (29)
- [Round](#round) (33)
- [Track](#track) (15)
- [Criteria](#criteria) (12)
- [JudgeAssign](#judgeassign) (13)
- [MentorAssign](#mentorassign) (11)
- [Event](#event) (18)
- [TempJudge](#tempjudge) (4)
- [Invitation](#invitation) (18)
- [Team](#team) (59)
- [Lottery](#lottery) (8)
- [Submission](#submission) (33)
- [Score](#score) (13)
- [RoundProgression](#roundprogression) (46)
- [Presentation](#presentation) (27)
- [Appeal](#appeal) (8)
- [Calibration](#calibration) (4)
- [JudgePortal](#judgeportal) (5)
- [StudentPortal](#studentportal) (1)
- [MentorPortal](#mentorportal) (8)
- [Closure](#closure) (12)
- [Prize](#prize) (19)
- [Ranking](#ranking) (17)
- [Export](#export) (11)
- [Rbl](#rbl) (6)
- [Announcement](#announcement) (4)
- [LiveScoring](#livescoring) (6)
- [Archive](#archive) (1)
- [Common](#common) (3)
- [RoundAccess](#roundaccess) (3)
- [Appendix A — ErrorCode orphan / unused](#appendix-a--errorcode-orphan--unused)
- [Appendix B — FR collisions & doc drift](#appendix-b--fr-collisions--doc-drift)
- [Appendix C — Module → Java package](#appendix-c--module--java-package)

## Auth

| Rule ID | Related Req ID | Module | Rule Type | Business Rule Statement | Condition / Trigger | System Action / Expected Result | Exception / Error Message | Test Case / Example Data | Status | Evidence Link / Note |
|---------|-----------------|--------|-----------|-------------------------|---------------------|---------------------------------|---------------------------|--------------------------|--------|----------------------|
| BR-AUTH-001 | FR-07 | Auth | Validation | Đăng nhập yêu cầu email/mật khẩu khớp | User không tồn tại hoặc passwordHash null/sai | Từ chối đăng nhập | INVALID_CREDENTIALS | login wrong password / unknown email → 401 | Implemented | auth/service/AuthService.java |
| BR-AUTH-002 | FR-07 | Auth | Gate | STUDENT PENDING được đăng nhập (không chờ duyệt) | role=STUDENT và status=PENDING | Cho phép login tiếp tục assert khác | N/A | login student PENDING sau verify email | Partial | auth/service/AuthService.java#assertApproved |
| BR-AUTH-003 | FR-07 | Auth | Gate | Tài khoản PENDING (không phải student/guest-judge onboard) không login | status=PENDING và không thuộc exception | 401 | ACCOUNT_PENDING | login judge/mentor PENDING | Implemented | auth/service/AuthService.java |
| BR-AUTH-004 | FR-07 | Auth | Gate | Tài khoản REJECTED không login | status=REJECTED | 401 | REJECTED_NOT_ALLOWED_LOGIN | login rejected user | Implemented | auth/service/AuthService.java |
| BR-AUTH-005 | FR-07 | Auth | Gate | Chỉ status APPROVED (ngoài exception) được login | status khác PENDING/REJECTED/APPROVED | 401 | UNAUTHORIZED | login invalid status | Implemented | auth/service/AuthService.java |
| BR-AUTH-006 | FR-05a | Auth | Gate | Guest judge PENDING chỉ login khi mustChangePassword=true | isTempAccount + EXTERNAL + JUDGE + PENDING + mustChangePassword | Cho phép login lần đầu | N/A | guest judge first login | Implemented | auth/service/AuthService.java |
| BR-AUTH-007 | FR-07 | Auth | Gate | Email phải verified trước login (trừ temp / đã verify / OAuth no-password) | emailVerifiedAt=null và có passwordHash và không temp | 401 | EMAIL_NOT_VERIFIED | login unverified student | Implemented | auth/service/AuthService.java |
| BR-AUTH-008 | FR-05a | Auth | Gate | Invitation judge hết hạn chặn login | temp account có invitation chưa accept và expiresAt < now | 401 | INVITATION_EXPIRED | login after invitation expiry | Implemented | auth/service/AuthService.java |
| BR-AUTH-009 | FR-05a | Auth | Gate | Temp judge không login khi mọi hackathon gắn đã kết thúc | isTempAccount và tất cả hackathon FINISHED hoặc sau eventEnd | 401 | TEMP_JUDGE_HACKATHON_ENDED | login after event_end | Implemented | invitations/.../GuestJudgeLifecycleServiceImpl.java |
| BR-AUTH-010 | FR-07 | Auth | SideEffect | Login thành công cập nhật lastLoginAt, cấp access+refresh, audit | Pass mọi gate | Tạo session + ACCOUNT_LOGIN | N/A | assert tokens + lastLoginAt | Implemented | auth/service/AuthService.java |
| BR-AUTH-011 | FR-07 | Auth | Validation | Đổi MK cần mật khẩu hiện tại đúng | currentPassword không khớp passwordHash | 400 | PASSWORD_MISMATCH | change-password wrong current | Implemented | auth/service/AuthService.java |
| BR-AUTH-012 | FR-07 | Auth | Validation | Mật khẩu mới phải khác mật khẩu hiện tại | newPassword matches current hash | 422 | NEW_PASSWORD_SAME_AS_CURRENT | same password | Implemented | auth/service/AuthService.java |
| BR-AUTH-013 | FR-05a | Auth | StateTransition | Guest judge đổi MK lần đầu → APPROVED + clear mustChangePassword | isTemp EXTERNAL JUDGE PENDING + change password ok | status→APPROVED; mustChangePassword=false | N/A | guest change-password activates | Implemented | auth/service/AuthService.java |
| BR-AUTH-014 | FR-05a | Auth | SideEffect | Đổi MK gắn accept invitation JUDGE chưa accept | Có invitation JUDGE acceptedAt=null mới nhất | set acceptedAt=now | N/A | invitation acceptedAt after change-pw | Implemented | auth/service/AuthService.java |
| BR-AUTH-015 | FR-07 | Auth | SideEffect | Đổi MK thành công revoke mọi session | changePassword ok | revokeAllForUser | N/A | old refresh invalid after change-pw | Implemented | auth/service/AuthService.java |
| BR-AUTH-016 | FR-07 | Auth | StateTransition | Refresh rotation: revoke session cũ, tạo refresh mới | POST refresh với raw token hợp lệ | rotate + access mới | N/A | refresh returns new refreshToken | Implemented | auth/service/AuthService.java + UserSessionService |
| BR-AUTH-017 | FR-07 | Auth | Gate | Refresh vẫn kiểm tra APPROVED/email verify/temp-judge ended | rotate ok rồi assertApproved/assertEmailVerified/assertHackathonNotEnded | Có thể 401 sau rotate | ACCOUNT_PENDING / EMAIL_NOT_VERIFIED / TEMP_JUDGE_HACKATHON_ENDED | refresh after reject account | Implemented | auth/service/AuthService.java |
| BR-AUTH-018 | FR-07 | Auth | SideEffect | Logout revoke một refresh token | rawRefreshToken blank → no-op; có token → revoke | revokedAt=now | N/A | logout then refresh fails | Implemented | auth/service/AuthService.java |
| BR-AUTH-019 | FR-07 | Auth | SideEffect | Logout-all revoke mọi session user hiện tại | Bearer authenticated | revokeAllActiveByUserId | N/A | logout-all invalidates all devices | Implemented | auth/service/AuthService.java |
| BR-AUTH-020 | FR-07 | Auth | Validation | Email đăng ký không được trùng | existsByEmail | 409 | ACCOUNT_DUPLICATE_EMAIL | register duplicate email | Implemented | auth/service/RegistrationService.java |
| BR-AUTH-021 | FR-07 | Auth | Validation | confirmPassword phải khớp password | password != confirmPassword | 422 | VALIDATION_FAILED | mismatch confirm | Implemented | auth/service/RegistrationService.java |
| BR-AUTH-022 | FR-07 | Auth | StateTransition | Đăng ký STUDENT mở → PENDING, userType UNSPECIFIED, chưa verify | POST register hợp lệ | INSERT user PENDING + gửi verify email | N/A | register creates PENDING | Implemented | auth/service/RegistrationService.java |
| BR-AUTH-023 | FR-07 | Auth | SideEffect | Sau register gửi email verification (fail email không fail register) | register ok | sendVerificationEmail; catch log warn | N/A | SMTP fail vẫn 200 register | Implemented | auth/service/RegistrationService.java + EmailVerificationService |
| BR-AUTH-024 | FR-07 | Auth | Invariant | Access token typ phải là access | parseAccessToken typ khác access | 401 | UNAUTHORIZED | use password_reset as Bearer | Implemented | auth/service/JwtTokenService.java |
| BR-AUTH-025 | FR-07 | Auth | Validation | JWT access/reset/verify phải đúng issuer + chữ ký + chưa hết hạn | token hỏng/hết hạn | 401/400 theo loại | UNAUTHORIZED / PASSWORD_RESET_TOKEN_INVALID / EMAIL_VERIFICATION_TOKEN_INVALID | expired tokens | Implemented | auth/service/JwtTokenService.java |
| BR-AUTH-026 | FR-07 | Auth | Policy | TTL access/reset/email-verify lấy từ JwtProperties | tạo token | exp = now + configured TTL | N/A | config TTL hours/minutes | Implemented | auth/service/JwtTokenService.java |
| BR-AUTH-027 | FR-07 | Auth | Invariant | Refresh token lưu SHA-256 hash, raw chỉ trả client | createSession | store tokenHash | N/A | DB không chứa raw token | Implemented | auth/service/UserSessionService.java |
| BR-AUTH-028 | FR-07 | Auth | Gate | Refresh hết hạn → invalid | expiresAt < now | 401 | REFRESH_TOKEN_INVALID | refresh after TTL days | Implemented | auth/service/UserSessionService.java |
| BR-AUTH-029 | FR-07 | Auth | Gate | Reuse refresh đã revoke → revoke ALL sessions rồi invalid | tokenHash tìm thấy nhưng revokedAt != null | revokeAllForUser + 401 | REFRESH_TOKEN_INVALID | reuse old refresh after rotation | Implemented | auth/service/UserSessionService.java |
| BR-AUTH-030 | FR-07 | Auth | StateTransition | Verify email set emailVerifiedAt (idempotent nếu đã verify) | token email_verification hợp lệ | set verifiedAt | EMAIL_VERIFICATION_TOKEN_INVALID nếu user/token sai | verify twice = no error | Implemented | auth/service/EmailVerificationService.java |
| BR-AUTH-031 | FR-07 | Auth | Policy | Resend chỉ gửi khi STUDENT chưa verify, có password, không temp; response luôn generic | POST resend | có thể gửi mail; message chung | N/A | resend unknown email same message | Implemented | auth/service/EmailVerificationService.java |
| BR-AUTH-032 | FR-07 | Auth | Policy | Forgot-password luôn trả message chung; chỉ gửi khi APPROVED + có passwordHash | requestReset | email nếu đủ điều kiện | N/A | forgot non-existing email 200 | Implemented | auth/service/PasswordResetService.java |
| BR-AUTH-033 | FR-07 | Auth | Validation | Reset password: new != current nếu đã có hash | matches current | 422 | NEW_PASSWORD_SAME_AS_CURRENT | reset to same password | Implemented | auth/service/PasswordResetService.java |
| BR-AUTH-034 | FR-07 | Auth | SideEffect | Reset thành công revoke all sessions + clear mustChangePassword | reset ok | revokeAll + update hash | N/A | sessions dead after reset | Implemented | auth/service/PasswordResetService.java |
| BR-AUTH-035 | FR-07 | Auth | Gate | OAuth login nếu chưa link provider và không auto-link → lỗi | không có OAuthAccount và (no user hoặc autoLink off) | 401 | OAUTH_ACCOUNT_NOT_LINKED | google login unlinked | Implemented | auth/service/SocialAuthService.java |
| BR-AUTH-036 | FR-07 | Auth | Policy | Auto-create user từ social khi autoCreateUserOnLogin | email chưa có user + flag on | tạo STUDENT PENDING, emailVerifiedAt=now, passwordHash=null | N/A | first google login creates user | Implemented | auth/service/SocialAuthService.java |
| BR-AUTH-037 | FR-07 | Auth | Gate | Auto-link by email có thể yêu cầu xác nhận password | requirePasswordForAutoLink và password sai/thiếu | 401 | OAUTH_PASSWORD_CONFIRM_REQUIRED | auto-link without password | Implemented | auth/service/SocialAuthService.java |
| BR-AUTH-038 | FR-07 | Auth | Invariant | Provider UID không được gắn user khác | link/auto-link khi đã gắn user khác | 409 | OAUTH_ACCOUNT_ALREADY_LINKED | link same google to two users | Implemented | auth/service/SocialAuthService.java |
| BR-AUTH-039 | FR-07 | Auth | Validation | Link OAuth: email identity phải trùng account trừ khi allowLinkDifferentEmail | email mismatch + flag false | 400 | OAUTH_EMAIL_MISMATCH | link different email | Implemented | auth/service/SocialAuthService.java |
| BR-AUTH-040 | FR-07 | Auth | Invariant | Không gỡ social cuối khi không có password | linkedProviders<=1 và passwordHash null/blank | 409 | OAUTH_UNLINK_FORBIDDEN | unlink last oauth no password | Implemented | auth/service/SocialAuthService.java |
| BR-AUTH-041 | FR-07 | Auth | SideEffect | OAuth login set emailVerifiedAt nếu null + assertApproved (student PENDING OK) | loginWithIdentity | verify email + session | ACCOUNT_* nếu blocked | oauth login pending student | Implemented | auth/service/SocialAuthService.java |
| BR-AUTH-042 | FR-07 | Auth | Validation | OAuth identity email chưa verified bởi provider | Social login email_verified=false | 401 | OAUTH_EMAIL_NOT_VERIFIED | Google account unverified email | Implemented | auth/service/SocialAuthService.java |
| BR-AUTH-043 | FR-07 | Auth | Validation | OAuth access/id token không hợp lệ | Verify Google/GitHub token fail | 401 | OAUTH_TOKEN_INVALID | Bad google id_token | Implemented | auth/service/social/*IdentityVerifier.java |

## UserAdmin

| Rule ID | Related Req ID | Module | Rule Type | Business Rule Statement | Condition / Trigger | System Action / Expected Result | Exception / Error Message | Test Case / Example Data | Status | Evidence Link / Note |
|---------|-----------------|--------|-----------|-------------------------|---------------------|---------------------------------|---------------------------|--------------------------|--------|----------------------|
| BR-UADMIN-001 | FR-08/FR-09 | UserAdmin | StateTransition | Không đổi status từ APPROVED | from=APPROVED | 422 | INVALID_STATUS_TRANSITION | approve already approved | Implemented | users/.../UserAdminServiceImpl.java |
| BR-UADMIN-002 | FR-09 | UserAdmin | StateTransition | Chỉ PENDING → APPROVED | to=APPROVED và from!=PENDING | 422 | INVALID_STATUS_TRANSITION | approve from REJECTED | Implemented | users/.../UserAdminServiceImpl.java |
| BR-UADMIN-003 | FR-08 | UserAdmin | Validation | Duyệt STUDENT cần userType INTERNAL/EXTERNAL | STUDENT + UNSPECIFIED/null | 422 | VALIDATION_FAILED | approve without userType | Implemented | users/.../UserAdminServiceImpl.java |
| BR-UADMIN-004 | FR-08 | UserAdmin | Validation | Duyệt STUDENT cần studentCode | blank studentCode | 422 | STUDENT_CODE_REQUIRED | approve missing code | Implemented | users/.../UserAdminServiceImpl.java |
| BR-UADMIN-005 | FR-08 | UserAdmin | Validation | Duyệt STUDENT cần ảnh thẻ SV | studentCardImagePath blank | 422 | VALIDATION_FAILED | approve without card | Implemented | users/.../UserAdminServiceImpl.java |
| BR-UADMIN-006 | FR-08 | UserAdmin | Validation | INTERNAL cần chapter; EXTERNAL cần institution | thiếu chapter/institution theo type | 422 | INVALID_CHAPTER / INSTITUTION_REQUIRED | approve INTERNAL no chapter | Implemented | users/.../UserAdminServiceImpl.java |
| BR-UADMIN-007 | FR-09 | UserAdmin | Validation | REJECTED bắt buộc rejectionReason | to=REJECTED và reason blank | 422 | REJECTION_REASON_REQUIRED | reject without reason | Implemented | users/.../UserAdminServiceImpl.java |
| BR-UADMIN-008 | FR-09 | UserAdmin | StateTransition | REJECTED→PENDING cần overrideReason | from REJECTED to PENDING blank override | 422 | INVALID_STATUS_TRANSITION | reopen without override | Implemented | users/.../UserAdminServiceImpl.java |
| BR-UADMIN-009 | FR-08 | UserAdmin | Validation | patchMe: studentCode không trùng user khác | existsByStudentCodeAndIdNot | 422 | STUDENT_CODE_DUPLICATE | duplicate studentCode | Implemented | users/.../UserAdminServiceImpl.java |
| BR-UADMIN-010 | FR-08 | UserAdmin | Validation | chapterId phải tồn tại và ACTIVE | chapter missing/inactive | 422 | INVALID_CHAPTER | patch chapter CANCELLED | Implemented | users/.../UserAdminServiceImpl.java |
| BR-UADMIN-011 | FR-08 | UserAdmin | Validation | userType cập nhật không được UNSPECIFIED | req.userType=UNSPECIFIED | 422 | VALIDATION_FAILED | set UNSPECIFIED | Implemented | users/.../UserAdminServiceImpl.java |
| BR-UADMIN-012 | N/A | UserAdmin | Policy | PATCH isDeptHead đã ngừng — luôn lỗi | req.isDeptHead != null | 422 | INVALID_ASSIGNMENT_TYPE | patch isDeptHead | Implemented | users/.../UserAdminServiceImpl.java |

## Hackathon

| Rule ID | Related Req ID | Module | Rule Type | Business Rule Statement | Condition / Trigger | System Action / Expected Result | Exception / Error Message | Test Case / Example Data | Status | Evidence Link / Note |
|---------|-----------------|--------|-----------|-------------------------|---------------------|---------------------------------|---------------------------|--------------------------|--------|----------------------|
| BR-HACK-001 | FR-01 | Hackathon | Invariant | (name; season; year) phải unique | CREATE/CLONE/UPDATE khi bộ ba đã tồn tại | Reject ConflictException | HACKATHON_DUPLICATE | TC create duplicate name+season+year | Implemented | hackathons/service/impl/HackathonServiceImpl.java |
| BR-HACK-002 | FR-01 | Hackathon | Invariant | slug phải unique | CREATE/CLONE/UPDATE slug đã dùng | Reject ConflictException | HACKATHON_DUPLICATE | TC duplicate slug | Implemented | hackathons/service/impl/HackathonServiceImpl.java |
| BR-HACK-003 | FR-01 | Hackathon | Validation | eventStart phải >= registrationEnd | regEnd và eventStart đều khác null và eventStart < regEnd | Reject BusinessRuleException | HACKATHON_DATE_RANGE | TC eventStart before regEnd | Implemented | hackathons/service/impl/HackathonServiceImpl.java |
| BR-HACK-004 | FR-01 | Hackathon | SideEffect | Hackathon mới luôn DRAFT; gắn createdBy nếu có user | CREATE hoặc CLONE thành công | Set status=DRAFT; audit HACKATHON_CREATE/CLONE | N/A | TC create returns DRAFT | Implemented | hackathons/service/impl/HackathonServiceImpl.java |
| BR-HACK-005 | FR-01 | Hackathon | SideEffect | Clone copy cấu trúc từ source + metadata clonedFrom/clonedAt | CLONE từ sourceId tồn tại | copyStructureFrom; audit HACKATHON_CLONE | N/A | TC clone structure | Implemented | hackathons/service/impl/HackathonServiceImpl.java; HackathonCloneSupport.java |
| BR-HACK-006 | FR-01 | Hackathon | StateTransition | Chỉ sửa khi DRAFT | UPDATE khi status != DRAFT | Reject | HACKATHON_NOT_DRAFT | TC update ONGOING | Implemented | hackathons/service/impl/HackathonServiceImpl.java |
| BR-HACK-007 | FR-01 | Hackathon | StateTransition | Chỉ xóa khi DRAFT | DELETE khi status != DRAFT | Reject | HACKATHON_NOT_DRAFT | TC delete non-DRAFT | Implemented | hackathons/service/impl/HackathonServiceImpl.java |
| BR-HACK-008 | FR-01 | Hackathon | Invariant | Không xóa khi còn Round/Track/Event con | DELETE và exists track\|round\|event | Reject | HACKATHON_HAS_CHILDREN | TC delete with children | Implemented | hackathons/service/impl/HackathonServiceImpl.java |
| BR-HACK-009 | FR-01 | Hackathon | Gate | FINISHED = archived — chặn mutation | archiveGuard trên update/delete/banner | Reject ConflictException | HACKATHON_ARCHIVED | TC mutate FINISHED | Implemented | hackathons/support/HackathonArchiveGuard.java |
| BR-HACK-010 | FR-01 | Hackathon | Validation | Banner chỉ đổi khi DRAFT hoặc ONGOING | uploadBanner status khác DRAFT/ONGOING | Reject | HACKATHON_NOT_DRAFT | TC banner PENDING_CONFIRM | Implemented | hackathons/service/impl/HackathonServiceImpl.java |
| BR-HACK-011 | FR-01 | Hackathon | Validation | Banner file: bắt buộc; ≤5MB; image MIME | uploadBanner file invalid | Reject | VALIDATION_FAILED | TC banner >5MB | Implemented | hackathons/support/HackathonBannerStorageService.java |
| BR-HACK-012 | FR-06 | Hackathon | StateTransition | Chỉ DRAFT→ONGOING; ONGOING→PENDING_CONFIRM; PENDING_CONFIRM→FINISHED | PATCH status transition không trong ALLOWED_TRANSITIONS | Reject | STATUS_TRANSITION_INVALID | G1-E03; invalid transition | Implemented | hackathons/service/impl/HackathonStatusServiceImpl.java |
| BR-HACK-013 | FR-06 | Hackathon | Gate | DRAFT→ONGOING bắt buộc readiness ONGOING pass | target=ONGOING và readiness.ready=false | Reject kèm blockers | READINESS_NOT_PASSED | G1-N04; incomplete seed | Implemented | hackathons/service/impl/HackathonStatusServiceImpl.java |
| BR-HACK-014 | FR-06 | Hackathon | SideEffect | Chuyển ONGOING fan-out HACKATHON_OPEN tới user APPROVED | status → ONGOING thành công | sendBatch notification | N/A | TC notification after ONGOING | Implemented | hackathons/service/impl/HackathonStatusServiceImpl.java |
| BR-HACK-015 | FR-06 | Hackathon | SideEffect | Mọi đổi status ghi audit (from/to/note/validatedBy/At) | changeStatus thành công | audit HACKATHON_STATUS_CHANGE | N/A | TC audit status change | Implemented | hackathons/service/impl/HackathonStatusServiceImpl.java |

## Readiness

| Rule ID | Related Req ID | Module | Rule Type | Business Rule Statement | Condition / Trigger | System Action / Expected Result | Exception / Error Message | Test Case / Example Data | Status | Evidence Link / Note |
|---------|-----------------|--------|-----------|-------------------------|---------------------|---------------------------------|---------------------------|--------------------------|--------|----------------------|
| BR-READY-001 | FR-06 | Readiness | Gate | G1: ≥1 round PRELIMINARY/SEMIFINAL và mỗi round đó ≥1 Track (không CANCELLED) | check ONGOING; prelim empty hoặc track empty | Blocker ready=false | MISSING_PRELIMINARY_ROUND | Gate G1; seal-gd1-incomplete | Implemented | hackathons/service/impl/HackathonReadinessServiceImpl.java |
| BR-READY-002 | FR-06 | Readiness | Gate | G2: đúng 1 Round Chung kết (is_final=TRUE) | finalCount=0 hoặc >1 khi ONGOING | Blocker | MISSING_FINAL_ROUND | G1-N08; Gate G2 | Implemented | hackathons/service/impl/HackathonReadinessServiceImpl.java |
| BR-READY-003 | FR-06 | Readiness | Gate | G3: mọi Track SL có ≥1 criteria NORMAL | normal criteria count = 0 trên track | Blocker | ROUND_NO_CRITERIA | Gate G3 missing criteria | Implemented | hackathons/service/impl/HackathonReadinessServiceImpl.java |
| BR-READY-004 | FR-06 | Readiness | Gate | G3: tổng weight NORMAL của mỗi Track = 1.0 (±0.001) | isValidForTrack=false | Blocker | TRACK_CRITERIA_WEIGHT | Gate G3 weight != 1 | Implemented | hackathons/service/impl/HackathonReadinessServiceImpl.java; WeightSummaryServiceImpl.java |
| BR-READY-005 | FR-06 | Readiness | Gate | G4: Round CK có ≥1 criteria NORMAL | normalFinal=0 | Blocker | ROUND_NO_CRITERIA | Gate G4 no final criteria | Implemented | hackathons/service/impl/HackathonReadinessServiceImpl.java |
| BR-READY-006 | FR-06 | Readiness | Gate | G4: tổng weight NORMAL của CK = 1.0 (±0.001) | isValidForFinalRound=false | Blocker | FINAL_CRITERIA_WEIGHT | Gate G4 weight != 1 | Implemented | hackathons/service/impl/HackathonReadinessServiceImpl.java |
| BR-READY-007 | FR-06 | Readiness | Gate | G5: phải có event KICKOFF | !exists KICKOFF khi ONGOING readiness | Blocker | EVENT_KICKOFF_MISSING | Gate G5; G1-N04 | Implemented | hackathons/service/impl/HackathonReadinessServiceImpl.java |
| BR-READY-008 | FR-06 | Readiness | Gate | Milestone WORKSHOP/KICKOFF/AWARDS (nếu có) phải pass schedule validateBlocking | validateMilestoneEventsGate catch BRE | Blocker với code từ validator | EVENT_OUT_OF_HACKATHON; EVENT_ORDER_VIOLATION; EVENT_OVERLAP; EVENT_END_REQUIRED; EVENT_END_BEFORE_START; EVENT_MILESTONE_DUPLICATE; EVENT_CONFLICTS_WITH_MILESTONE; EVENT_LOCATION_REQUIRED; AWARDS_BEFORE_FINAL_DEADLINE | G1-N01..N03 via readiness | Implemented | hackathons/service/impl/HackathonReadinessServiceImpl.java; EventScheduleValidatorImpl.java |
| BR-READY-009 | FR-06 | Readiness | Gate | Mọi round.examAt phải hợp lệ vs KICKOFF end và [eventStart; eventEnd] | collectRoundExamAtViolations (bỏ EVENT_PRESENTATION_MISSING / EVENT_AWARDS_MISSING) | Blocker | ROUND_EXAM_BEFORE_KICKOFF; EVENT_OUT_OF_HACKATHON | TC examAt before kickoff end | Implemented | hackathons/service/impl/HackathonReadinessServiceImpl.java; HackathonTimelineServiceImpl.java |
| BR-READY-010 | FR-06 | Readiness | Policy | Thiếu Mentor/Judge trên Track SL chỉ WARNING; không block ONGOING | mentors==0 hoặc judges==0 trên track | Warning READINESS_WARNING; ready vẫn true nếu không blocker khác | N/A (READINESS_WARNING) | Playbook soft warning mentor/judge | Implemented | hackathons/service/impl/HackathonReadinessServiceImpl.java |
| BR-READY-011 | FR-06 | Readiness | Gate | target FINAL_ROUND: cần CK + criteria weight OK + ≥1 FINAL_EXTERNAL + ≥1 team trong CK | checkFinalRoundReadiness | Blockers tương ứng | MISSING_FINAL_ROUND; ROUND_NO_CRITERIA; FINAL_CRITERIA_WEIGHT; JUDGE_FINAL_AT_PHASE1; NO_TEAMS_IN_ROUND | G1-R01; G4-R01 | Implemented | hackathons/service/impl/HackathonReadinessServiceImpl.java |
| BR-READY-012 | FR-06 | Readiness | Policy | FINAL_ROUND: thiếu AWARDS chỉ warning | !exists AWARDS khi FINAL_ROUND check | Warning khuyến nghị trước GĐ6 | N/A (READINESS_WARNING) | G1-R early awards warning | Implemented | hackathons/service/impl/HackathonReadinessServiceImpl.java |
| BR-READY-013 | FR-06 | Readiness | Gate | target AWARDS/PENDING_CONFIRM: bắt buộc có AWARDS hợp lệ schedule | !exists AWARDS hoặc validateBlocking fail | Blocker | EVENT_AWARDS_MISSING (+ codes schedule) | G1-R02; G6-R01 | Implemented | hackathons/service/impl/HackathonReadinessServiceImpl.java |
| BR-READY-014 | FR-06 | Readiness | SideEffect | Mọi check readiness ghi audit | check() luôn chạy | audit HACKATHON_READINESS_CHECK | N/A | TC audit readiness | Implemented | hackathons/service/impl/HackathonReadinessServiceImpl.java |

## Registration

| Rule ID | Related Req ID | Module | Rule Type | Business Rule Statement | Condition / Trigger | System Action / Expected Result | Exception / Error Message | Test Case / Example Data | Status | Evidence Link / Note |
|---------|-----------------|--------|-----------|-------------------------|---------------------|---------------------------------|---------------------------|--------------------------|--------|----------------------|
| BR-REG-001 | FR-07B | Registration | Validation | Chỉ đăng ký khi hackathon ONGOING | register status != ONGOING | Reject | HACKATHON_NOT_ONGOING | TC register DRAFT | Implemented | hackathons/service/impl/HackathonRegistrationServiceImpl.java |
| BR-REG-002 | FR-07B | Registration | Invariant | Đã rút đăng ký thì không đăng ký lại | exists withdrawal cho (hackathon; user) | Reject | REGISTRATION_WITHDRAWN | TC re-register after withdraw | Implemented | hackathons/service/impl/HackathonRegistrationServiceImpl.java |
| BR-REG-003 | FR-07B | Registration | Invariant | Một user chỉ active registration trên một hackathon ONGOING | đã register hackathon ONGOING khác | Reject | REGISTRATION_ALREADY_ACTIVE_ELSEWHERE | TC dual ONGOING register | Implemented | hackathons/service/impl/HackathonRegistrationServiceImpl.java |
| BR-REG-004 | FR-07B | Registration | Validation | Không vượt maxParticipants | count >= maxParticipants | Reject | INVALID_STATE | TC full capacity | Implemented | hackathons/service/impl/HackathonRegistrationServiceImpl.java |
| BR-REG-005 | FR-07B | Registration | Gate | Cổng ĐK đóng khi closedEarly hoặc today > registrationEnd | isRegistrationClosed | Reject | REGISTRATION_CLOSED | TC after regEnd | Implemented | hackathons/service/impl/HackathonRegistrationServiceImpl.java; HackathonRegistrationSupport.java |
| BR-REG-006 | FR-07B | Registration | Gate | Chưa tới registrationStart thì chưa mở ĐK | today < registrationStart | Reject | REGISTRATION_CLOSED | TC before regStart | Implemented | hackathons/service/impl/HackathonRegistrationServiceImpl.java |
| BR-REG-007 | FR-07B | Registration | Invariant | Không đăng ký trùng cùng hackathon | exists registration | Reject ConflictException | INVALID_STATE | TC double register | Implemented | hackathons/service/impl/HackathonRegistrationServiceImpl.java |
| BR-REG-008 | FR-07B | Registration | Validation | Hủy ĐK: phải rời đội ACTIVE trước | unregister khi còn trong active team | Reject | INVALID_STATE | TC unregister while in team | Implemented | hackathons/service/impl/HackathonRegistrationServiceImpl.java |
| BR-REG-009 | FR-07B | Registration | SideEffect | Unregister ghi withdrawal rồi xóa registration | unregister hợp lệ | save withdrawal; delete registration | REGISTRATION_WITHDRAWN (nếu đã withdraw) | TC withdraw once | Implemented | hackathons/service/impl/HackathonRegistrationServiceImpl.java |
| BR-REG-010 | FR-07B | Registration | Validation | Close early chỉ khi ONGOING | status != ONGOING | Reject | HACKATHON_NOT_ONGOING | TC close early DRAFT | Implemented | hackathons/service/impl/HackathonRegistrationCloseServiceImpl.java |
| BR-REG-011 | FR-07B | Registration | Gate | Không close early nếu đã đóng | isRegistrationClosed true | Reject | REGISTRATION_ALREADY_CLOSED | TC double close early | Implemented | hackathons/service/impl/HackathonRegistrationCloseServiceImpl.java |
| BR-REG-012 | FR-07B | Registration | SideEffect | Close early: stamp closedEarlyAt; kéo registrationEnd về today; **bắt buộc newPrelimExamAt**; lock ACTIVE; reject/grace PENDING; withdraw orphan; **dời lịch 1 lần** (WS→KO→SL→CK→Awards) + notify mentor/GK/SV; set scheduleAdjustedAt | closeRegistrationEarly + body | Mutate teams/regs/rounds/events; audit | VALIDATION_FAILED nếu thiếu newPrelimExamAt; SCHEDULE_ALREADY_ADJUSTED | TC close-reg + preview | Implemented | HackathonRegistrationCloseServiceImpl; CompetitionScheduleAdjustService |
| BR-REG-013 | FR-10 | Registration | Gate | Đăng ký hackathon chỉ khi ONGOING | status != ONGOING | 422 | HACKATHON_NOT_ONGOING | register DRAFT | Implemented | HackathonRegistrationServiceImpl.java |
| BR-REG-014 | FR-10 | Registration | Gate | Không đăng ký lại sau khi đã withdraw | có HackathonRegistrationWithdrawal | 422 | REGISTRATION_WITHDRAWN | re-register after withdraw | Implemented | HackathonRegistrationServiceImpl.java |
| BR-REG-015 | FR-10 | Registration | Invariant | Một user chỉ đăng ký một hackathon ONGOING tại một thời điểm | đã reg hackathon ONGOING khác | 422 | REGISTRATION_ALREADY_ACTIVE_ELSEWHERE | dual ONGOING regs | Implemented | HackathonRegistrationServiceImpl.java |
| BR-REG-016 | FR-10 | Registration | Validation | Không vượt maxParticipants | count >= maxParticipants | 422 | INVALID_STATE | hit capacity | Implemented | HackathonRegistrationServiceImpl.java |
| BR-REG-017 | FR-10 | Registration | Gate | Đăng ký đóng khi closedEarly hoặc today > registrationEnd | isRegistrationClosed | 422 | REGISTRATION_CLOSED | register after end | Implemented | HackathonRegistrationServiceImpl.java + HackathonRegistrationSupport |
| BR-REG-018 | FR-10 | Registration | Gate | Chưa tới registrationStart thì không đăng ký | today < registrationStart | 422 | REGISTRATION_CLOSED | register before start | Implemented | HackathonRegistrationServiceImpl.java |
| BR-REG-019 | FR-10 | Registration | Validation | Không đăng ký trùng cùng hackathon | exists registration | 409 | INVALID_STATE | double register | Implemented | HackathonRegistrationServiceImpl.java |
| BR-REG-020 | FR-10 | Registration | Gate | Không hủy đăng ký khi đang trong đội ACTIVE/PENDING (accepted) | isUserInAnyActiveTeamForHackathon | 422 | INVALID_STATE | unregister while in team | Implemented | HackathonRegistrationServiceImpl.java |
| BR-REG-021 | FR-10 | Registration | SideEffect | Unregister tạo withdrawal rồi xóa registration | unregister ok | INSERT withdrawal + DELETE reg | REGISTRATION_WITHDRAWN nếu đã withdraw | cannot rejoin after unregister | Implemented | HackathonRegistrationServiceImpl.java |
| BR-REG-022 | FR-13A | Registration | Gate | Chỉ close-early khi ONGOING và chưa closed | not ONGOING / already closed | 422 | HACKATHON_NOT_ONGOING / REGISTRATION_ALREADY_CLOSED | close twice | Implemented | HackathonRegistrationCloseServiceImpl.java |
| BR-REG-023 | FR-13A | Registration | SideEffect | Close-early set registrationClosedEarlyAt; kéo registrationEnd về today nếu còn sau | close ok | persist closedEarly + end | N/A | registration closed immediately | Implemented | HackathonRegistrationCloseServiceImpl.java |
| BR-REG-024 | FR-13A | Registration | SideEffect | Khóa mọi đội ACTIVE chưa lock | close-early | isLocked=true + TEAM_LOCKED audit | N/A | ACTIVE teams locked | Implemented | HackathonRegistrationCloseServiceImpl.java |
| BR-REG-025 | FR-13 | Registration | StateTransition | PENDING ngoài khoảng size → REJECTED + withdraw members | acceptedCount out of min-max | REJECTED + LEFT + withdraw reg | N/A | incomplete teams rejected on close | Implemented | HackathonRegistrationCloseServiceImpl.java |
| BR-REG-026 | FR-13 | Registration | Gate | PENDING đủ size + đã formationSubmitted + no pending invite + all APPROVED → awaiting coordinator (không auto-approve) | đủ điều kiện duyệt | liệt kê awaitingApproval + notify COORD | N/A | teams stay PENDING awaiting | Implemented | HackathonRegistrationCloseServiceImpl.java |
| BR-REG-027 | FR-11 | Registration | Scheduler | PENDING đủ size chưa confirm → set formationGraceDeadlineAt = now+24h + notify | đủ size chưa submit | grace period 24h | N/A | grace notification TEAM_FORMATION_GRACE | Implemented | HackathonRegistrationCloseServiceImpl.java |
| BR-REG-028 | FR-10 | Registration | SideEffect | Orphan registration (không trong đội active) bị withdraw khi close-early | reg user not in active team | withdraw + count orphans | N/A | orphans withdrawn | Implemented | HackathonRegistrationCloseServiceImpl.java |
| BR-REG-029 | FR-10 | Registration | Invariant | isRegistrationClosed = closedEarlyAt != null OR today > registrationEnd | đọc support | true/false | N/A | unit test date boundary (day after end) | Implemented | hackathons/support/HackathonRegistrationSupport.java |

## Round

| Rule ID | Related Req ID | Module | Rule Type | Business Rule Statement | Condition / Trigger | System Action / Expected Result | Exception / Error Message | Test Case / Example Data | Status | Evidence Link / Note |
|---------|-----------------|--------|-----------|-------------------------|---------------------|---------------------------------|---------------------------|--------------------------|--------|----------------------|
| BR-ROUND-001 | FR-02 | Round | Gate | Chỉ tạo/sửa/xóa Round khi parent DRAFT\|ONGOING và chưa archived | guardHackathonMutable fail | Reject | TRACK_HACKATHON_LOCKED; HACKATHON_ARCHIVED | TC round mutate FINISHED | Implemented | rounds/service/impl/RoundServiceImpl.java |
| BR-ROUND-002 | FR-02 | Round | Validation | submissionDeadline > submissionOpen và > now | validateDeadline fail | Reject | ROUND_DEADLINE_INVALID | TC deadline past/equal open | Implemented | rounds/service/impl/RoundServiceImpl.java |
| BR-ROUND-003 | FR-02 | Round | Validation | examAt phải trước submissionOpen | examAt >= submissionOpen | Reject | ROUND_EXAM_BEFORE_SUBMISSION_OPEN | TC exam after open | Implemented | rounds/service/impl/RoundServiceImpl.java |
| BR-ROUND-004 | FR-02 | Round | Validation | Nếu có codingDurationHours>0: open=examAt+2/3; deadline=examAt+duration (auto-apply rồi verify) | window không khớp expected | Reject | ROUND_DEADLINE_INVALID | G1-ROUND-03; coding window | Implemented | rounds/service/impl/RoundServiceImpl.java; RoundScheduleSeedUtil |
| BR-ROUND-005 | FR-02 | Round | Validation | Prelim examAt >= registrationEnd + 5 ngày | non-final exam date trước min | Reject | ROUND_PRELIM_EXAM_ORDER | TC prelim too soon after regEnd | Implemented | rounds/service/impl/RoundServiceImpl.java; RoundScheduleValidator.java |
| BR-ROUND-006 | FR-02 | Round | Invariant | Mỗi RoundType chỉ 1 lần / hackathon | create duplicate roundType | Reject | ROUND_TYPE_DUPLICATE | TC duplicate PRELIMINARY | Implemented | rounds/service/impl/RoundServiceImpl.java |
| BR-ROUND-007 | FR-02 | Round | Invariant | Chỉ 1 Round final / hackathon | create second isFinal | Reject | ROUND_DUPLICATE_FINAL | TC second final | Implemented | rounds/service/impl/RoundServiceImpl.java |
| BR-ROUND-008 | FR-02 | Round | Validation | Tạo final yêu cầu đã có prelim-like | create final khi chưa có prelim | Reject | ROUND_FINAL_REQUIRES_PRELIM | TC final first | Implemented | rounds/service/impl/RoundServiceImpl.java |
| BR-ROUND-009 | FR-02 | Round | Validation | Final exam cùng ngày prelim (nếu codingHours>0) và trong [minFinal; maxFinal]; else examAt > latest prelim | final exam order sai | Reject | ROUND_FINAL_EXAM_ORDER | TC final exam order | Implemented | rounds/service/impl/RoundServiceImpl.java |
| BR-ROUND-010 | FR-02 | Round | Validation | Prelim examAt phải trước final.examAt | prelim exam >= final exam | Reject | ROUND_PRELIM_EXAM_ORDER | TC prelim after final | Implemented | rounds/service/impl/RoundServiceImpl.java |
| BR-ROUND-011 | FR-02 | Round | Validation | examAt sau KICKOFF.endsAt (nếu có) và trong [eventStart; eventEnd] | timeline validateRoundExamAt | Reject | ROUND_EXAM_BEFORE_KICKOFF; EVENT_OUT_OF_HACKATHON | TC exam before KO end | Implemented | events/service/impl/HackathonTimelineServiceImpl.java |
| BR-ROUND-013 | FR-02 | Round | Validation | Non-final không được roundType=FINAL khi is_final=false | mismatch flags | Reject | INVALID_STATE | TC inconsistent flags | Implemented | rounds/service/impl/RoundServiceImpl.java |
| BR-ROUND-014 | FR-02 | Round | Validation | Prelim submissionDeadline < final.examAt (nếu có final) | ordering fail | Reject | ROUND_PRELIM_DEADLINE_AFTER_FINAL_EXAM | TC prelim deadline after final exam | Implemented | rounds/service/impl/RoundServiceImpl.java |
| BR-ROUND-015 | FR-02 | Round | Validation | Final submissionDeadline < AWARDS.startsAt (nếu có AWARDS) | deadline >= awardsStart | Reject | ROUND_FINAL_DEADLINE_AFTER_AWARDS | TC final deadline after awards | Implemented | rounds/service/impl/RoundServiceImpl.java |
| BR-ROUND-016 | FR-02 | Round | StateTransition | Round active không sửa examAt/open/deadline — phải deactivate trước | isActive && scheduleChanged | Reject | INVALID_STATE | TC edit schedule while active | Implemented | rounds/service/impl/RoundServiceImpl.java |
| BR-ROUND-017 | FR-02 | Round | Validation | forceLocked=true bắt buộc forceLockReason | force lock không reason | Reject | ROUND_FORCE_LOCK_REASON | TC force lock blank reason | Implemented | rounds/service/impl/RoundServiceImpl.java |
| BR-ROUND-018 | FR-02 | Round | StateTransition | Không xóa round đang active | delete isActive | Reject | ROUND_ANOTHER_ACTIVE | TC delete active round | Implemented | rounds/service/impl/RoundServiceImpl.java |
| BR-ROUND-019 | FR-02 | Round | Invariant | Không xóa round đã có submission | count submissions > 0 | Reject | ROUND_HAS_SUBMISSIONS | TC delete with submissions | Implemented | rounds/service/impl/RoundServiceImpl.java |
| BR-ROUND-020 | FR-02 | Round | Invariant | Không xóa round đã có điểm chấm (scores) | count scores > 0 | Reject | ROUND_HAS_CRITERIA | TC delete with scores | Implemented | rounds/service/impl/RoundServiceImpl.java |
| BR-ROUND-021 | FR-02 | Round | SideEffect | Xóa round: cascade criteria/judges; notify JUDGE_UNASSIGNED; sync timeline | delete thành công | delete + notify + audit | N/A | TC delete round notifies judges | Implemented | rounds/service/impl/RoundServiceImpl.java |
| BR-ROUND-022 | FR-02 | Round | Design | Không upload đề trên Round (SL=track; CK=reuse track) | uploadProblemStatement bất kỳ | Reject | DESIGN_VIOLATION | TC upload round PDF | Implemented | rounds/service/impl/RoundServiceImpl.java |
| BR-ROUND-023 | FR-02 | Round | Validation | Dismiss migration banner chỉ cho final có clearedAt | dismiss sai điều kiện | Reject | VALIDATION_FAILED | TC dismiss non-final | Implemented | rounds/service/impl/RoundServiceImpl.java |
| BR-ROUND-024 | FR-02 | Round | SideEffect | Create/update round sync hackathon timeline từ rounds | create/update/delete round | hackathonRoundTimelineSyncService.syncFromRounds | N/A | TC timeline sync | Implemented | rounds/service/impl/RoundServiceImpl.java |
| BR-ROUND-025 | FR-02 | Round | Gate | Activate prelim: không còn team PENDING | assertNoPendingTeams | Reject | TEAMS_PENDING_APPROVAL | LOT-04; activate with pending | Implemented | rounds/service/impl/RoundActivationServiceImpl.java; PendingTeamGateService.java |
| BR-ROUND-026 | FR-02 | Round | Gate | Activate prelim: mọi track non-CANCELLED phải có ≥1 đội | emptyTrackIds non-empty (hoặc không track → NO_TEAMS) | Reject | TRACK_EMPTY_TEAMS; NO_TEAMS_IN_ROUND | G3-N01 | Implemented | rounds/service/impl/RoundActivationServiceImpl.java |
| BR-ROUND-027 | FR-02 | Round | Gate | Activate prelim: mỗi track có criteria; weight=1; ≥1 judge; không mentor+judge cùng user | validatePreliminaryRoundTracks | Reject | ROUND_NO_CRITERIA; ROUND_WEIGHT_NOT_ONE; JUDGE_NOT_ASSIGNED; CONFLICT_SAME_TRACK | TC activate missing judge | Implemented | rounds/service/impl/RoundActivationServiceImpl.java |
| BR-ROUND-028 | FR-02 | Round | Gate | Activate final: prelim-like phải isPublished | unpublished prelim exists | Reject | RESULT_NOT_PUBLISHED | G4-N01 | Implemented | rounds/service/impl/RoundActivationServiceImpl.java |
| BR-ROUND-029 | FR-02 | Round | Gate | Activate final: có criteria + weight≈1 | validateFinalRoundCriteria | Reject | ROUND_NO_CRITERIA; ROUND_WEIGHT_NOT_ONE | G4-N02 criteria | Implemented | rounds/service/impl/RoundActivationServiceImpl.java |
| BR-ROUND-030 | FR-02 | Round | Gate | Activate final: ≥1 assignment; chỉ NORMAL/HEAD/FINAL_EXTERNAL; ≥1 FINAL_EXTERNAL; FINAL_EXTERNAL phải EXTERNAL user | validateFinalRoundJudges | Reject | JUDGE_NOT_ASSIGNED; INVALID_ASSIGNMENT_TYPE | G4-N02 judges | Implemented | rounds/service/impl/RoundActivationServiceImpl.java |
| BR-ROUND-031 | FR-02 | Round | SideEffect | Activate: deactivate sibling active rounds; set isActive/activatedAt; CK stamp problemReleasedAt nếu null; notify ROUND_STARTED | activate KEEP thành công | deactivateOthers; audit; notify | N/A | G3-H01; G4-H04 | Implemented | rounds/service/impl/RoundActivationServiceImpl.java |
| BR-ROUND-032 | FR-02 | Round | StateTransition | ~~START_NOW early-start~~ **Removed (phase 2)** — activate trên round đã active chỉ idempotent; dời lịch dùng «Dời lịch thi» / close-reg-early | round.isActive | Return current round (no schedule shift) | N/A (START_NOW enum removed) | historical: TC start-early after release | Removed | rounds/service/impl/RoundActivationServiceImpl.java |
| BR-ROUND-033 | FR-02 | Round | Validation | RESCHEDULE cần codingDurationHours; newExamAt > now; window consistency; allowEarly nới gap ĐK nhưng vẫn ≤eventEnd (cascade có thể bump); giữ thứ tự sibling hoặc skip khi cascade; **RESCHEDULE Sơ loại:** ngày thi ≥ registrationEnd + 3 (WS+KO). ~~START_NOW lead 1–30m removed (phase 2).~~ | RoundScheduleShiftService + Validator | Reject | VALIDATION_FAILED; ROUND_EXAM_BEFORE_SUBMISSION_OPEN; EVENT_OUT_OF_HACKATHON; ROUND_FINAL_EXAM_ORDER; ROUND_PRELIM_EXAM_ORDER | TC reschedule past; RESCHEDULE gap WS/KO | Implemented | rounds/support/RoundScheduleShiftService.java; RoundScheduleValidator.java |
| BR-ROUND-034 | FR-02 | Round | SideEffect | RESCHEDULE: shift lịch; giữ inactive; không notify ROUND_STARTED; audit ROUND_SCHEDULE_SHIFTED; **cascade:** CK (1–2h), sync eventStart/End, WS/KO/AWARDS, slots. ~~START_NOW cascade removed (phase 2).~~ | scheduleMode=RESCHEDULE (via adjust/close-reg; rejected on activate) | apply shift + cascade | N/A | TC cascade final; milestones | Implemented | rounds/support/RoundScheduleShiftService.java; events/service/MilestoneEventRescheduleService.java |

## Track

| Rule ID | Related Req ID | Module | Rule Type | Business Rule Statement | Condition / Trigger | System Action / Expected Result | Exception / Error Message | Test Case / Example Data | Status | Evidence Link / Note |
|---------|-----------------|--------|-----------|-------------------------|---------------------|---------------------------------|---------------------------|--------------------------|--------|----------------------|
| BR-TRACK-001 | FR-03 | Track | Design | Không tạo Track trong Round final | createByRound isFinal | Reject | DESIGN_VIOLATION | TC track on final | Implemented | tracks/service/impl/TrackServiceImpl.java |
| BR-TRACK-002 | FR-03 | Track | Gate | Track mutate chỉ khi parent DRAFT\|ONGOING; không archived | guardParentStatus | Reject | TRACK_HACKATHON_LOCKED; HACKATHON_ARCHIVED | TC track on FINISHED | Implemented | tracks/service/impl/TrackServiceImpl.java |
| BR-TRACK-003 | FR-03 | Track | Validation | maxTeamSize >= minTeamSize; maxTeamsPerGroup <= maxTeams | validateSizes | Reject | TRACK_INVALID_TEAM_SIZE; TRACK_INVALID_GROUP_CAP | TC invalid sizes | Implemented | tracks/service/impl/TrackServiceImpl.java |
| BR-TRACK-004 | FR-03 | Track | Invariant | min/max team size các Track cùng hackathon phải giao nhau | assertCompatibleWithExistingTracks | Reject | TRACK_INVALID_TEAM_SIZE | TC non-overlapping team sizes | Implemented | teams/support/HackathonTeamSizeResolver.java |
| BR-TRACK-005 | FR-03 | Track | StateTransition | Không CANCELLED track khi còn team PENDING\|ACTIVE được phân công | cancel with activeTeams>0 | Reject | TRACK_CANCEL_HAS_TEAMS | TC cancel with teams | Implemented | tracks/service/impl/TrackServiceImpl.java |
| BR-TRACK-006 | FR-03 | Track | Invariant | Không xóa track còn team PENDING\|ACTIVE | delete with teams | Reject | TRACK_HAS_TEAMS | TC delete with teams | Implemented | tracks/service/impl/TrackServiceImpl.java |
| BR-TRACK-007 | FR-03 | Track | StateTransition | Không xóa track khi round cha active | round.isActive | Reject | TRACK_HAS_ACTIVE_ROUND | TC delete under active round | Implemented | tracks/service/impl/TrackServiceImpl.java |
| BR-TRACK-008 | FR-03 | Track | Validation | Đổi topic chỉ sau khi đã có KICKOFF | topic changed và chưa có KICKOFF | Reject | INVALID_STATE | TC topic before kickoff | Implemented | tracks/service/impl/TrackServiceImpl.java |
| BR-TRACK-009 | FR-03 | Track | SideEffect | sequenceOrder: null hoặc trùng → gán max+1 | create track | auto assign sequence | N/A | TC default sequence | Implemented | tracks/service/impl/TrackServiceImpl.java |
| BR-TRACK-010 | FR-03 | Track | SideEffect | Xóa track: notify MENTOR/JUDGE_UNASSIGNED; xóa judge assignments | delete thành công | notify + delete judges | N/A | TC delete track notifies | Implemented | tracks/service/impl/TrackServiceImpl.java |
| BR-TRACK-011 | FR-03 | Track | Design | Upload/phát đề chỉ Track vòng sơ loại (không final) | upload/release trên final track | Reject | DESIGN_VIOLATION | TC release on final track | Implemented | tracks/service/impl/TrackServiceImpl.java |
| BR-TRACK-012 | FR-03 | Track | StateTransition | Không thay PDF sau khi track hoặc round đã phát đề | problemReleasedAt set | Reject | INVALID_STATE | TC replace PDF after release | Implemented | tracks/service/impl/TrackServiceImpl.java |
| BR-TRACK-013 | FR-03 | Track | Gate | Phát đề: round phải active; track không CANCELLED; chưa phát; now >= examAt; đã có PDF | releaseProblem guards | Reject | ROUND_NOT_ACTIVE; INVALID_STATE; INVALID_ROUND_STATE_BEFORE_EXAM; VALIDATION_FAILED | EARLY-WAIT release before exam | Implemented | tracks/service/impl/TrackServiceImpl.java; RoundAccessGuard.java |
| BR-TRACK-014 | FR-03 | Track | SideEffect | Phát đề stamp problemReleasedAt + notify PROBLEM_RELEASED tới member ACCEPTED của đội trong track | release thành công | audit TRACK_RELEASE_PROBLEM; notify | N/A | TC release notifies students | Implemented | tracks/service/impl/TrackServiceImpl.java |
| BR-TRACK-015 | FR-05 | Track | Design | Mentor/Judge sơ loại chỉ gắn Track non-final | requirePreliminaryAssignmentTrack | Reject | DESIGN_VIOLATION; INVALID_STATE | TC assign on final track | Implemented | tracks/support/TrackRoundRules.java |

## Criteria

| Rule ID | Related Req ID | Module | Rule Type | Business Rule Statement | Condition / Trigger | System Action / Expected Result | Exception / Error Message | Test Case / Example Data | Status | Evidence Link / Note |
|---------|-----------------|--------|-----------|-------------------------|---------------------|---------------------------------|---------------------------|--------------------------|--------|----------------------|
| BR-CRIT-001 | FR-04 | Criteria | Gate | CRUD criteria chặn khi hackathon FINISHED | archiveGuard on track/round/criteria | Reject | HACKATHON_ARCHIVED | TC criteria on archived | Implemented | criteria/service/impl/CriteriaServiceImpl.java |
| BR-CRIT-002 | FR-04 | Criteria | Invariant | Không sửa/xóa criterion đã có scores | update/delete với scores | Reject | CRITERIA_HAS_SCORES | TC edit scored criterion | Implemented | criteria/service/impl/CriteriaServiceImpl.java |
| BR-CRIT-003 | FR-04 | Criteria | Validation | API round criteria chỉ cho is_final=TRUE | loadFinalRound non-final | Reject | ROUND_NOT_FINAL_FOR_CRITERIA | TC criteria on prelim round API | Implemented | criteria/service/impl/CriteriaServiceImpl.java |
| BR-CRIT-004 | FR-04 | Criteria | Validation | Clone track: bắt buộc sourceTrackId khác đích; source có criteria; không dùng sourceRoundId | cloneFromSourceForTrack | Reject | CRITERIA_CLONE_SOURCE_EMPTY; CRITERIA_CLONE_CROSS_SCOPE | TC clone empty source | Implemented | criteria/service/impl/CriteriaServiceImpl.java |
| BR-CRIT-005 | FR-04 | Criteria | Validation | Clone final: bắt buộc sourceRoundId final khác đích; không dùng sourceTrackId; source phải final | cloneFromSourceForFinalRound | Reject | CRITERIA_CLONE_SOURCE_EMPTY; CRITERIA_CLONE_CROSS_SCOPE | TC clone track→final | Implemented | criteria/service/impl/CriteriaServiceImpl.java |
| BR-CRIT-006 | FR-04 | Criteria | Invariant | replaceExisting=true bị chặn nếu đích đã có scores | replace path | Reject | CRITERIA_HAS_SCORES | TC replace with scores | Implemented | criteria/service/impl/CriteriaServiceImpl.java |
| BR-CRIT-007 | FR-04 | Criteria | Policy | Tạo/update criteria: nếu tổng weight != 1.0 trả Warning WEIGHT_NOT_ONE (không throw) | warningIfNotOne* | Response warning; vẫn 2xx | N/A (WEIGHT_NOT_ONE) | TC create weight 0.5 warning | Implemented | criteria/service/impl/WeightSummaryServiceImpl.java |
| BR-CRIT-008 | FR-04 | Criteria | Invariant | Weight hợp lệ = sum NORMAL (exclude PENALTY) ≈ 1.0 và có ≥1 NORMAL | isValidForTrack/FinalRound | Dùng cho readiness/activate | N/A | Unit WeightSummary | Implemented | criteria/service/impl/WeightSummaryServiceImpl.java |
| BR-CRIT-009 | FR-04 | Criteria | Validation | Apply template: đích đã có criteria thì cần replaceExisting | prepareTarget without replace | Reject | CRITERIA_TARGET_HAS_EXISTING | TC apply without replace | Implemented | criteria/service/impl/CriteriaTemplateServiceImpl.java |
| BR-CRIT-010 | FR-04 | Criteria | Validation | Apply template chỉ vào round final | applyToFinalRound non-final | Reject | ROUND_NOT_FINAL_FOR_CRITERIA | TC template to prelim round | Implemented | criteria/service/impl/CriteriaTemplateServiceImpl.java |
| BR-CRIT-011 | FR-04 | Criteria | Invariant | replaceExisting bị chặn nếu criterion có scores | prepareTarget replace | Reject | CRITERIA_HAS_SCORES | TC template replace scored | Implemented | criteria/service/impl/CriteriaTemplateServiceImpl.java |
| BR-CRIT-012 | FR-04 | Criteria | Validation | Round không hợp lệ cho criteria (XOR / non-final) | criteria gắn round sai | Reject | INVALID_ROUND_FOR_CRITERIA | criteria on wrong round | Implemented | criteria/service/impl/CriteriaServiceImpl.java |

## JudgeAssign

| Rule ID | Related Req ID | Module | Rule Type | Business Rule Statement | Condition / Trigger | System Action / Expected Result | Exception / Error Message | Test Case / Example Data | Status | Evidence Link / Note |
|---------|-----------------|--------|-----------|-------------------------|---------------------|---------------------------------|---------------------------|--------------------------|--------|----------------------|
| BR-JUDGE-001 | FR-05 | JudgeAssign | Authorization | Judge phải role MENTOR\|JUDGE và status APPROVED | loadApprovedPersonnel | Reject | USER_INVALID_ROLE; USER_NOT_APPROVED | Personnel Guard | Implemented | users/support/PersonnelAssignmentRules.java; JudgeAssignmentServiceImpl.java |
| BR-JUDGE-002 | FR-05 | JudgeAssign | Validation | EXTERNAL không gán Track sơ loại | assignToTrack EXTERNAL | Reject | EXTERNAL_JUDGE_NOT_ALLOWED_IN_PRELIM | TC external on prelim track | Implemented | judge_assignments/service/impl/JudgeAssignmentServiceImpl.java |
| BR-JUDGE-003 | FR-05 | JudgeAssign | Validation | Track sơ loại chỉ assignmentType NORMAL (cấm FINAL_EXTERNAL/HEAD/khác) | assignToTrack type sai | Reject | INVALID_ASSIGNMENT_TYPE | G1-JUDGE type | Implemented | judge_assignments/service/impl/JudgeAssignmentServiceImpl.java |
| BR-JUDGE-004 | FR-05 | JudgeAssign | Invariant | Không Judge nếu đang Mentor cùng track | exists mentor assignment | Reject | CONFLICT_SAME_TRACK | TC mentor then judge same track | Implemented | judge_assignments/service/impl/JudgeAssignmentServiceImpl.java |
| BR-JUDGE-005 | FR-05 | JudgeAssign | Invariant | Không Judge nếu đang Mentor team thuộc track đó | assertNotMentorOfTeamInTrack | Reject | CONFLICT_MENTOR_JUDGE_SAME_TRACK | TC mentor-team then judge track | Implemented | judge_assignments/service/impl/JudgeAssignmentServiceImpl.java |
| BR-JUDGE-006 | FR-05 | JudgeAssign | Invariant | Mỗi judge chỉ 1 track / round (existsByJudgeIdAndRoundScope) | đã gán track khác cùng round | Reject | JUDGE_ASSIGN_DUPLICATE | G1-JUDGE-04 multi-track | Implemented | judge_assignments/service/impl/JudgeAssignmentServiceImpl.java |
| BR-JUDGE-007 | FR-05 | JudgeAssign | Invariant | Không gán trùng cùng track | existsByJudgeIdAndTrackId | Reject | JUDGE_ASSIGN_DUPLICATE | TC duplicate assign | Implemented | judge_assignments/service/impl/JudgeAssignmentServiceImpl.java |
| BR-JUDGE-008 | FR-05A | JudgeAssign | Gate | Gán Judge CK qua round_id ở GĐ1 bị chặn cứng | assignToFinalRound (API phase1) | Reject | JUDGE_FINAL_AT_PHASE1 | G1-N05 | Implemented | judge_assignments/service/impl/JudgeAssignmentServiceImpl.java |
| BR-JUDGE-009 | FR-05 | JudgeAssign | Validation | round_id phải là final (khi gọi path final) | !isFinal | Reject | INVALID_FINAL_ROUND | TC non-final roundId | Implemented | judge_assignments/service/impl/JudgeAssignmentServiceImpl.java |
| BR-JUDGE-010 | FR-05 | JudgeAssign | Validation | G4 assign: chỉ NORMAL\|FINAL_EXTERNAL; FINAL_EXTERNAL→EXTERNAL; NORMAL→INTERNAL; không HEAD | assignFinalRoundG4 | Reject | INVALID_ASSIGNMENT_TYPE | G4-H03 types | Implemented | judge_assignments/service/impl/JudgeAssignmentServiceImpl.java |
| BR-JUDGE-011 | FR-05 | JudgeAssign | Policy | G4: judge đã chấm prelim trong hackathon → Warning JUDGE_PARTICIPATED_IN_PRELIM (không block) | hasPreliminaryTrackAssignmentInHackathon | Warning + vẫn tạo assignment | N/A | TC prelim judge to final warning | Implemented | judge_assignments/service/impl/JudgeAssignmentServiceImpl.java |
| BR-JUDGE-012 | FR-05 | JudgeAssign | SideEffect | Assign/unassign: audit; notify JUDGE_*; email best-effort | assign/unassign thành công | notify + optional email | N/A | TC assign notifies | Implemented | judge_assignments/service/impl/JudgeAssignmentServiceImpl.java |
| BR-JUDGE-013 | FR-05 | JudgeAssign | Validation | Judge INTERNAL không được gán Chung kết (trừ ngoại lệ dept-head nếu còn) | assign final INTERNAL không hợp lệ | Reject | INTERNAL_JUDGE_NOT_ALLOWED_IN_FINAL | INTERNAL on FINAL_EXTERNAL path | Implemented | judge_assignments / personnel guards |

## MentorAssign

| Rule ID | Related Req ID | Module | Rule Type | Business Rule Statement | Condition / Trigger | System Action / Expected Result | Exception / Error Message | Test Case / Example Data | Status | Evidence Link / Note |
|---------|-----------------|--------|-----------|-------------------------|---------------------|---------------------------------|---------------------------|--------------------------|--------|----------------------|
| BR-MENT-001 | FR-05 | MentorAssign | Authorization | Mentor phải MENTOR\|JUDGE APPROVED | requireApprovedPersonnel | Reject | USER_INVALID_ROLE; USER_NOT_APPROVED | Personnel Guard mentor | Implemented | users/support/PersonnelAssignmentRules.java; MentorAssignmentServiceImpl.java |
| BR-MENT-002 | FR-05 | MentorAssign | Design | Mentor chỉ trên Track sơ loại | TrackRoundRules | Reject | DESIGN_VIOLATION | TC mentor final track | Implemented | mentors/service/impl/MentorAssignmentServiceImpl.java |
| BR-MENT-003 | FR-05 | MentorAssign | StateTransition | Track phải OPEN (không CANCELLED / không status khác) | status != OPEN | Reject | INVALID_STATE | TC mentor on CLOSED track | Implemented | mentors/service/impl/MentorAssignmentServiceImpl.java |
| BR-MENT-004 | FR-05 | MentorAssign | Gate | Parent hackathon DRAFT\|ONGOING; không archived | status locked / FINISHED | Reject | TRACK_HACKATHON_LOCKED; HACKATHON_ARCHIVED | TC mentor FINISHED | Implemented | mentors/service/impl/MentorAssignmentServiceImpl.java |
| BR-MENT-005 | FR-05 | MentorAssign | Invariant | Không duplicate mentor cùng track | exists mentor+track | Reject | MENTOR_ASSIGN_DUPLICATE | TC duplicate mentor | Implemented | mentors/service/impl/MentorAssignmentServiceImpl.java |
| BR-MENT-006 | FR-05 | MentorAssign | Invariant | Không Mentor nếu đang Judge cùng track | exists judge on track | Reject | CONFLICT_SAME_TRACK | TC judge then mentor same | Implemented | mentors/service/impl/MentorAssignmentServiceImpl.java |
| BR-MENT-007 | FR-05 | MentorAssign | Invariant | Judge FINAL_EXTERNAL của hackathon không làm Mentor sơ loại | existsFinalExternalJudgeInHackathonOfTrack | Reject | FINAL_JUDGE_CANNOT_BE_MENTOR | TC final guest as mentor | Implemented | mentors/service/impl/MentorAssignmentServiceImpl.java |
| BR-MENT-008 | FR-05 | MentorAssign | Invariant | Mỗi user chỉ Mentor 1 track / round | existsByMentorIdAndRoundIdExcludingTrack | Reject | PERSONNEL_ONE_TRACK_PER_ROUND | TC mentor two tracks same round | Implemented | mentors/service/impl/MentorAssignmentServiceImpl.java |
| BR-MENT-009 | FR-05 | MentorAssign | Policy | Cross-track Mentor A + Judge B (khác track) được phép | không throw khi khác track | Allow | N/A | PersonnelAssignmentRules javadoc | Implemented | users/support/PersonnelAssignmentRules.java |
| BR-MENT-010 | FR-05 | MentorAssign | SideEffect | Assign/unassign: audit; notify MENTOR_*; email best-effort | assign/unassign | notify + email | N/A | TC mentor notify | Implemented | mentors/service/impl/MentorAssignmentServiceImpl.java |
| BR-MENT-011 | FR-05 | MentorAssign | Validation | Mentor INTERNAL không được gán vòng Chung kết | assign mentor trên final | Reject | INTERNAL_MENTOR_NOT_ALLOWED_IN_FINAL | Mentor on final round | Implemented | mentors / TrackRoundRules |

## Event

| Rule ID | Related Req ID | Module | Rule Type | Business Rule Statement | Condition / Trigger | System Action / Expected Result | Exception / Error Message | Test Case / Example Data | Status | Evidence Link / Note |
|---------|-----------------|--------|-----------|-------------------------|---------------------|---------------------------------|---------------------------|--------------------------|--------|----------------------|
| BR-EVENT-001 | FR-06A | Event | Gate | Event CRUD chặn khi hackathon FINISHED | archiveGuard | Reject | HACKATHON_ARCHIVED | TC event on archived | Implemented | events/service/impl/EventServiceImpl.java |
| BR-EVENT-002 | FR-06A | Event | Gate | POST order: WORKSHOP yêu cầu đã có KICKOFF | create WORKSHOP không có KO | Reject | EVENT_KICKOFF_MISSING | G1-N01 EventServiceImpl path | Implemented | events/service/impl/EventServiceImpl.java |
| BR-EVENT-003 | FR-06A | Event | Gate | POST order: AWARDS yêu cầu đã có WORKSHOP | create AWARDS không có WS | Reject | EVENT_ORDER_VIOLATION | G1-N02 | Implemented | events/service/impl/EventServiceImpl.java |
| BR-EVENT-004 | FR-06A | Event | Gate | Không xóa KICKOFF khi còn WORKSHOP/AWARDS; không xóa WORKSHOP khi còn AWARDS | assertDeleteDependencies | Reject | EVENT_ORDER_VIOLATION | G1-N06; G1-N07 | Implemented | events/service/impl/EventServiceImpl.java |
| BR-EVENT-005 | FR-06A | Event | Validation | Phải có location hoặc meetUrl | cả hai blank | Reject | EVENT_LOCATION_REQUIRED | TC event no location | Implemented | events/service/impl/EventScheduleValidatorImpl.java |
| BR-EVENT-006 | FR-06A | Event | Validation | Milestone bắt buộc endsAt; endsAt >= startsAt | endsAt null hoặc trước startsAt | Reject | EVENT_END_REQUIRED; EVENT_END_BEFORE_START | TC milestone no end | Implemented | events/service/impl/EventScheduleValidatorImpl.java |
| BR-EVENT-007 | FR-06A | Event | Validation | Mỗi type milestone chỉ 1 event / hackathon | second KICKOFF/WORKSHOP/AWARDS | Reject | EVENT_MILESTONE_DUPLICATE | TC duplicate KICKOFF | Implemented | events/service/impl/EventScheduleValidatorImpl.java |
| BR-EVENT-008 | FR-06A | Event | Validation | Không overlap cùng type milestone; không overlap milestone↔OTHER | findOverlapping / findOtherOverlapping | Reject | EVENT_OVERLAP; EVENT_CONFLICTS_WITH_MILESTONE | TC overlapping events | Implemented | events/service/impl/EventScheduleValidatorImpl.java |
| BR-EVENT-009 | FR-06A | Event | Validation | Lịch thực tế phaseOrder: WORKSHOP → KICKOFF → AWARDS (earlier.end < later.start) | validateLayer3Ordering | Reject | EVENT_ORDER_VIOLATION | G1-E01 schedule order | Implemented | events/service/impl/EventScheduleValidatorImpl.java; EventTimeline.java |
| BR-EVENT-010 | FR-06A | Event | Validation | WORKSHOP và KICKOFF phải khác calendar day | cùng ngày | Reject | EVENT_ORDER_VIOLATION | G1-N03 | Implemented | events/service/impl/EventScheduleValidatorImpl.java |
| BR-EVENT-011 | FR-06A | Event | Validation | KICKOFF trong (registrationEnd; eventStart) exclusive | KickoffWindowRule | Reject | EVENT_OUT_OF_HACKATHON | TC KO on regEnd day | Implemented | events/service/impl/window/KickoffWindowRule.java |
| BR-EVENT-012 | FR-06A | Event | Validation | WORKSHOP trong (registrationEnd; eventStart); và đã có KICKOFF (window rule) | WorkshopWindowRule | Reject | EVENT_OUT_OF_HACKATHON | G1-N01 via validator path | Implemented | events/service/impl/window/WorkshopWindowRule.java |
| BR-EVENT-013 | FR-06A | Event | Validation | AWARDS starts đúng ngày eventEnd; không kết thúc sau eventEnd | AwardsWindowRule | Reject | EVENT_OUT_OF_HACKATHON | TC AWARDS wrong day | Implemented | events/service/impl/window/AwardsWindowRule.java |
| BR-EVENT-014 | FR-06A | Event | Validation | AWARDS.startsAt phải sau publishedAt/scoringLockedAt/hoặc examAt+coding+buffer của CK | validateAwardsAfterFinalSubmissionDeadline | Reject | AWARDS_BEFORE_FINAL_DEADLINE | TC awards before final done | Implemented | events/service/impl/EventScheduleValidatorImpl.java |
| BR-EVENT-015 | FR-06A | Event | Validation | OTHER nằm trong [eventStart; eventEnd]; PRESENTATION skip window rules | validateWithinHackathon / PRESENTATION return | Reject EVENT_OUT_OF_HACKATHON (OTHER) | EVENT_OUT_OF_HACKATHON | TC OTHER outside window | Implemented | events/service/impl/EventScheduleValidatorImpl.java |
| BR-EVENT-016 | FR-06A | Event | SideEffect | isPublic=true → fan-out EVENT_REMINDER tới APPROVED; đổi startsAt reset reminderSentAt | create/update public | sendBatch | N/A | TC public event notify | Implemented | events/service/impl/EventServiceImpl.java |
| BR-EVENT-017 | FR-06A | Event | Invariant | Sau create/update/delete KICKOFF: assertAllRoundsExamAtValid (ném violation đầu tiên) | KICKOFF mutation | Reject timeline code | ROUND_EXAM_BEFORE_KICKOFF; EVENT_OUT_OF_HACKATHON | TC KO change breaks examAt | Implemented | events/service/impl/EventServiceImpl.java; HackathonTimelineServiceImpl.java |
| BR-EVENT-018 | FR-06A | Event | Gate | Thiếu event PRESENTATION khi readiness yêu cầu (nếu còn path) | Readiness/timeline check presentation | Blocker hoặc reject | EVENT_PRESENTATION_MISSING | Readiness target needing presentation | Partial | events / HackathonReadinessServiceImpl (may be soft-skipped) |

## TempJudge

| Rule ID | Related Req ID | Module | Rule Type | Business Rule Statement | Condition / Trigger | System Action / Expected Result | Exception / Error Message | Test Case / Example Data | Status | Evidence Link / Note |
|---------|-----------------|--------|-----------|-------------------------|---------------------|---------------------------------|---------------------------|--------------------------|--------|----------------------|
| BR-TJUDGE-001 | FR-05A | TempJudge | Invariant | Email temp judge chưa tồn tại | existsByEmail | Reject | USER_EMAIL_TAKEN | TC duplicate email invite | Implemented | users/service/impl/TempJudgeServiceImpl.java |
| BR-TJUDGE-002 | FR-05A | TempJudge | SideEffect | Tạo EXTERNAL JUDGE isTempAccount PENDING + invitation 72h + email temp password; mustChangePassword=true | createTempJudge | save user+invitation; audit TEMP_ACCOUNT_CREATE | N/A | TC create temp judge PENDING | Implemented | users/service/impl/TempJudgeServiceImpl.java; InvitationConstants.java |
| BR-TJUDGE-003 | FR-05A | TempJudge | Gate | Không mời/resend khi hackathon FINISHED hoặc today > eventEnd | assertHackathonNotEnded | Reject | TEMP_JUDGE_HACKATHON_ENDED | TC invite after eventEnd | Implemented | invitations/service/impl/GuestJudgeLifecycleServiceImpl.java |
| BR-TJUDGE-004 | FR-05A | TempJudge | Authorization | Login temp judge bị chặn khi mọi hackathon gắn đã ended | assertHackathonNotEndedForTempJudge | AuthException 401 | TEMP_JUDGE_HACKATHON_ENDED | TC login after finished | Implemented | invitations/service/impl/GuestJudgeLifecycleServiceImpl.java |

## Invitation

| Rule ID | Related Req ID | Module | Rule Type | Business Rule Statement | Condition / Trigger | System Action / Expected Result | Exception / Error Message | Test Case / Example Data | Status | Evidence Link / Note |
|---------|-----------------|--------|-----------|-------------------------|---------------------|---------------------------------|---------------------------|--------------------------|--------|----------------------|
| BR-INV-001 | FR-05A | Invitation | Validation | Resend/revoke chỉ cho role JUDGE invitation | role != JUDGE | Reject | VALIDATION_FAILED | TC resend mentor invite | Implemented | invitations/service/impl/InvitationServiceImpl.java |
| BR-INV-002 | FR-05A | Invitation | StateTransition | Không resend/revoke khi đã accepted (acceptedAt hoặc mustChangePassword=false) | accepted | Reject | INVITATION_ALREADY_ACCEPTED | TC revoke after activate | Implemented | invitations/service/impl/InvitationServiceImpl.java |
| BR-INV-003 | FR-05A | Invitation | StateTransition | Không resend/revoke khi đã revoked | revokedAt set | Reject | INVITATION_ALREADY_REVOKED | TC double revoke | Implemented | invitations/service/impl/InvitationServiceImpl.java |
| BR-INV-004 | FR-05A | Invitation | Gate | Resend chỉ khi hết hạn hoặc lastTokenSent=false | stillValid && !lastSendFailed | Reject | INVITATION_STILL_VALID | TC resend while valid | Implemented | invitations/service/impl/InvitationServiceImpl.java |
| BR-INV-005 | FR-05A | Invitation | Gate | Resend cần có KICKOFF có startsAt; now < kickoffStart − 48h | assertResendAllowed | Reject | EVENT_KICKOFF_NOT_FOUND; INVITATION_RESEND_AFTER_KICKOFF_CUTOFF | TC resend within 48h KO | Implemented | invitations/service/impl/GuestJudgeLifecycleServiceImpl.java |
| BR-INV-006 | FR-05A | Invitation | Invariant | Invitation judge khách phải gắn hackathon | hackathon null trên invitation | Reject | INVITATION_HACKATHON_REQUIRED | TC invitation without hackathon | Implemented | invitations/service/impl/GuestJudgeLifecycleServiceImpl.java |
| BR-INV-007 | FR-05A | Invitation | SideEffect | Resend: reset temp password + token + expiresAt +72h; email; audit INVITATION_RESEND | resend allowed | update user/invitation | N/A | TC resend rotates token | Implemented | invitations/service/impl/InvitationServiceImpl.java |
| BR-INV-008 | FR-05a | Invitation | Validation | Resend chỉ invitation role JUDGE | role != JUDGE | 422 | VALIDATION_FAILED | resend non-judge inv | Implemented | invitations/.../InvitationServiceImpl.java |
| BR-INV-009 | FR-05a | Invitation | Gate | Không resend khi đã accept | acceptedAt != null | 409 | INVITATION_ALREADY_ACCEPTED | resend accepted | Implemented | invitations/.../InvitationServiceImpl.java |
| BR-INV-010 | FR-05a | Invitation | Gate | Không resend khi đã revoke | revokedAt != null | 409 | INVITATION_ALREADY_REVOKED | resend revoked | Implemented | invitations/.../InvitationServiceImpl.java |
| BR-INV-011 | FR-05a | Invitation | Gate | Token còn hạn và lần gửi trước OK → không resend | stillValid && !lastSendFailed | 422 | INVITATION_STILL_VALID | resend while valid | Implemented | invitations/.../InvitationServiceImpl.java |
| BR-INV-012 | FR-05a | Invitation | Gate | Resend cần KICKOFF tồn tại và startsAt | không KICKOFF / startsAt null | 422 | EVENT_KICKOFF_NOT_FOUND | resend no kickoff | Implemented | GuestJudgeLifecycleServiceImpl.java |
| BR-INV-013 | FR-05a | Invitation | Gate | Chỉ resend trước KICKOFF ≥48h | now >= startsAt-48h hoặc đã qua startsAt | 422 | INVITATION_RESEND_AFTER_KICKOFF_CUTOFF | resend within 48h of kickoff | Implemented | GuestJudgeLifecycleServiceImpl.java |
| BR-INV-014 | FR-05a | Invitation | Gate | Hackathon đã ended → không mời/resend | FINISHED hoặc after eventEnd | 422 | TEMP_JUDGE_HACKATHON_ENDED | resend after finish | Implemented | GuestJudgeLifecycleServiceImpl.java |
| BR-INV-015 | FR-05a | Invitation | SideEffect | Resend: temp password mới + mustChangePassword + token mới + expiry 72h | resend allowed | email + rotate password | N/A | password changes on resend | Implemented | InvitationServiceImpl.java |
| BR-INV-016 | FR-05a | Invitation | Gate | Không revoke khi đã accept hoặc user đã đổi MK (mustChangePassword=false) | acceptedAt hoặc !mustChangePassword | 409 | INVITATION_ALREADY_ACCEPTED | revoke after activate | Implemented | InvitationServiceImpl.java |
| BR-INV-017 | FR-05a | Invitation | Gate | Không revoke hai lần | revokedAt != null | 409 | INVITATION_ALREADY_REVOKED | double revoke | Implemented | InvitationServiceImpl.java |
| BR-INV-018 | FR-05a | Invitation | Invariant | Invitation judge khách phải gắn hackathon | hackathon == null trên invitation | 422 | INVITATION_HACKATHON_REQUIRED | invitation without hackathon | Implemented | GuestJudgeLifecycleServiceImpl.java |

## Team

| Rule ID | Related Req ID | Module | Rule Type | Business Rule Statement | Condition / Trigger | System Action / Expected Result | Exception / Error Message | Test Case / Example Data | Status | Evidence Link / Note |
|---------|-----------------|--------|-----------|-------------------------|---------------------|---------------------------------|---------------------------|--------------------------|--------|----------------------|
| BR-TEAM-001 | FR-11 | Team | Authorization | Chỉ STUDENT tạo đội | role != STUDENT | 422 | TEAM_LEADER_INVALID_ROLE | mentor create team | Implemented | teams/.../TeamServiceImpl.java#createTeam |
| BR-TEAM-002 | FR-11 | Team | Gate | Hackathon phải ONGOING | status != ONGOING | 422 | HACKATHON_NOT_ONGOING | create on DRAFT | Implemented | TeamServiceImpl.java |
| BR-TEAM-003 | FR-11 | Team | Gate | Không tạo đội khi registration closed | isRegistrationClosed | 422 | REGISTRATION_CLOSED | create after close | Implemented | TeamServiceImpl.java |
| BR-TEAM-004 | FR-11 | Team | Validation | Tên đội unique case-insensitive trong hackathon | duplicate name | 409 | TEAM_NAME_DUPLICATE | duplicate team name | Implemented | TeamServiceImpl.java |
| BR-TEAM-005 | FR-11 | Team | Invariant | User không được ACCEPTED trong đội ACTIVE/PENDING khác cùng hackathon | existsAcceptedInActiveOrPendingTeam | 409 | USER_IN_ANOTHER_TEAM | create while already member | Implemented | TeamServiceImpl.java |
| BR-TEAM-006 | FR-11 | Team | SideEffect | Tạo PENDING + leader ACCEPTED; chapter từ leader; isLocked=false | create ok | TEAM_CREATE audit | N/A | leader is ACCEPTED LEADER | Implemented | TeamServiceImpl.java |
| BR-TEAM-007 | FR-11 | Team | Gap | createTeam không kiểm tra user đã register hackathon / APPROVED / prelim active | student create | cho phép tạo nếu pass gates hiện có | N/A | create without registration; create after prelim active | Gap | TeamServiceImpl.java (assertPrelimRoundNotActive chỉ admin) |
| BR-TEAM-008 | FR-11/FR-12 | Team | Authorization | Chỉ COORDINATOR / mentor được gán / member PENDING\|ACCEPTED xem chi tiết đội | caller không thuộc nhóm | 403 | FORBIDDEN | student unrelated GET team | Implemented | teams/support/TeamAccessGuard.java |
| BR-TEAM-009 | FR-11 | Team | Authorization | Coordinator thấy mọi đội; student chỉ đội mình (PENDING/ACCEPTED membership) | listTeams | filter theo role | N/A | student list hidden other teams | Implemented | TeamServiceImpl.java#listTeams |
| BR-TEAM-010 | FR-13 | Team | StateTransition | Chỉ duyệt đội PENDING → ACTIVE | status != PENDING khi duyệt | 422 | INVALID_STATUS_TRANSITION | approve REJECTED | Implemented | TeamServiceImpl.java#assertCoordinatorCanApproveTeam |
| BR-TEAM-011 | FR-13 | Team | Gate | Duyệt cần formationSubmittedAt != null | chưa confirm formation | 422 | TEAM_FORMATION_NOT_SUBMITTED | approve before confirm | Implemented | TeamServiceImpl.java |
| BR-TEAM-012 | FR-13 | Team | Validation | Số ACCEPTED trong [min,max] theo track config | out of range | 422 | TEAM_INVALID_MEMBER_COUNT | approve size 2 when min 3 | Implemented | TeamServiceImpl.java |
| BR-TEAM-013 | FR-13 | Team | Gate | Không còn lời mời PENDING | pendingCount > 0 | 422 | TEAM_HAS_PENDING_MEMBERS | approve with pending invite | Implemented | TeamServiceImpl.java |
| BR-TEAM-014 | FR-13 | Team | Gate | Mọi member ACCEPTED phải user APPROVED | có member chưa duyệt TK | 422 | TEAM_HAS_UNAPPROVED_MEMBERS | approve with pending user | Implemented | TeamServiceImpl.java |
| BR-TEAM-015 | FR-13 | Team | Gate | Duyệt chỉ khi Hackathon ONGOING | hackathon not ONGOING | 422 | HACKATHON_NOT_ONGOING | approve after finish | Implemented | TeamServiceImpl.java |
| BR-TEAM-016 | FR-13A | Team | SideEffect | Nếu registration đã đóng khi duyệt → lock ngay | approve ACTIVE + registration closed | isLocked + lockedAt | N/A | approve after close locks immediately | Implemented | TeamServiceImpl.java#lockIfRegistrationClosed |
| BR-TEAM-017 | FR-13 | Team | Validation | Đã ACTIVE thì không approve lại (idempotent error) | status==ACTIVE request ACTIVE | 422 | TEAM_ALREADY_ACTIVE | double approve | Implemented | TeamServiceImpl.java |
| BR-TEAM-018 | FR-13 | Team | Validation | Reject bắt buộc rejectionReason | blank reason | 422 | REJECTION_REASON_REQUIRED | reject no reason | Implemented | TeamServiceImpl.java |
| BR-TEAM-019 | FR-13 | Team | SideEffect | Reject → releaseMembers (PENDING→REJECTED, ACCEPTED→LEFT, notify TEAM_RELEASED, không withdraw reg mặc định) | reject team | releaseMembers(..., false) | N/A | members can join other teams; still registered | Implemented | TeamServiceImpl + TeamMembershipReleaseServiceImpl |
| BR-TEAM-020 | FR-11 | Team | Authorization | Chỉ leader confirm formation | caller != leader | 422 | FORBIDDEN | member confirm | Implemented | TeamServiceImpl.java#confirmFormation |
| BR-TEAM-021 | FR-11 | Team | Gate | Confirm khi PENDING, chưa lock, chưa submitted, đủ size, không pending invite | vi phạm từng điều kiện |  | TEAM_LOCKED / INVALID_STATUS_TRANSITION / TEAM_FORMATION_ALREADY_SUBMITTED / TEAM_INVALID_MEMBER_COUNT / TEAM_FORMATION_PENDING_INVITES | confirm with pending invite | Implemented | TeamServiceImpl.java |
| BR-TEAM-022 | FR-13 | Team | SideEffect | Confirm clear grace deadline + notify coordinators TEAM_AWAITING_APPROVAL | confirm ok | formationSubmittedAt=now | N/A | coord notified | Implemented | TeamServiceImpl.java |
| BR-TEAM-023 | FR-13 | Team | Policy | Bulk approve: skip đã ACTIVE; lỗi per-team gom errors; cùng assertCoordinatorCanApproveTeam | bulk request | partial success response | HACKATHON_NOT_ONGOING nếu hackathon sai | bulk mixed statuses | Implemented | TeamServiceImpl.java#bulkApproveTeams |
| BR-TEAM-024 | FR-11C | Team | Authorization | Chỉ leader hiện tại transfer | caller != leader | 422 | FORBIDDEN | member transfer | Implemented | TeamServiceImpl.java |
| BR-TEAM-025 | FR-11C | Team | Gate | Transfer chỉ khi chưa lock, chưa formation submit, status PENDING | assertLeaderCanChangeMembership fail |  | TEAM_LOCKED / TEAM_FORMATION_ALREADY_SUBMITTED / TEAM_ALREADY_ACTIVE | transfer after confirm | Implemented | TeamServiceImpl.java |
| BR-TEAM-026 | FR-11C | Team | Validation | newLeader phải là member ACCEPTED của đội | không trong đội / chưa ACCEPTED |  | NEW_LEADER_NOT_MEMBER / NEW_LEADER_NOT_APPROVED | transfer to pending invitee | Implemented | TeamServiceImpl.java |
| BR-TEAM-027 | FR-11D | Team | Authorization | Chỉ leader hoặc Coordinator giải tán | khác cả hai | 422 | FORBIDDEN | member disband | Implemented | TeamServiceImpl.java |
| BR-TEAM-028 | FR-11D | Team | Gate | Có mentor assignment → không disband | existsByTeam_Id mentor | 409 | TEAM_HAS_MENTOR_CANNOT_DISBAND | disband after mentor assign | Implemented | TeamServiceImpl.java |
| BR-TEAM-029 | FR-11D | Team | Gate | Leader không disband khi locked / đã confirm formation / đã ACTIVE | leader + các trạng thái đó |  | TEAM_LOCKED / TEAM_FORMATION_ALREADY_SUBMITTED / TEAM_ALREADY_ACTIVE | leader disband ACTIVE | Implemented | TeamServiceImpl.java |
| BR-TEAM-030 | FR-11D | Team | SideEffect | Disband → REJECTED + releaseMembers(false) + xóa TRT/TRP | disband ok | cleanup tracks/participations | N/A | journey cleared | Implemented | TeamServiceImpl.java |
| BR-TEAM-031 | FR-12 | Team | Authorization | Chỉ leader mời | caller != leader | 422 | FORBIDDEN | member invite | Implemented | TeamServiceImpl.java |
| BR-TEAM-032 | FR-12 | Team | Gate | Không mời khi registration closed / locked / formation submitted / ACTIVE | các gate |  | REGISTRATION_CLOSED / TEAM_LOCKED / TEAM_FORMATION_ALREADY_SUBMITTED / TEAM_ALREADY_ACTIVE | invite after close | Implemented | TeamServiceImpl.java |
| BR-TEAM-033 | FR-12 | Team | Validation | Invitee phải STUDENT APPROVED tồn tại theo email | sai role/status/not found |  | INVITEE_INVALID_ROLE / INVITEE_NOT_APPROVED / RESOURCE_NOT_FOUND | invite pending user | Implemented | TeamServiceImpl.java |
| BR-TEAM-034 | FR-12 | Team | Validation | ACCEPTED+PENDING count không vượt maxTeamSize | full roster+invites | 422 | TEAM_MEMBER_FULL | invite at cap | Implemented | TeamServiceImpl.java |
| BR-TEAM-035 | FR-12 | Team | Invariant | Invitee không ACCEPTED đội khác; không duplicate PENDING/ACCEPTED trên đội | đã ở đội/đã mời |  | USER_IN_ANOTHER_TEAM / DUPLICATE_PENDING_INVITATION | double invite | Implemented | TeamServiceImpl.java |
| BR-TEAM-036 | FR-12 | Team | Authorization | ACCEPT/REJECT chỉ chính invitee; LEFT self hoặc leader | sai actor | 422 | FORBIDDEN | leader accept for other | Implemented | TeamServiceImpl.java |
| BR-TEAM-037 | FR-12 | Team | Gate | ACCEPT/REJECT bị chặn nếu team locked | isLocked | 422 | TEAM_LOCKED | accept when locked | Implemented | TeamServiceImpl.java |
| BR-TEAM-038 | FR-12 | Team | StateTransition | ACCEPT chỉ từ PENDING; đội phải còn PENDING/ACTIVE; chưa ở đội khác; chưa full ACCEPTED | vi phạm |  | INVALID_STATUS_TRANSITION / TEAM_NOT_ACCEPTING_INVITES / USER_IN_ANOTHER_TEAM / TEAM_MEMBER_FULL | accept after disband | Implemented | TeamServiceImpl.java |
| BR-TEAM-039 | FR-12 | Team | StateTransition | REJECT chỉ PENDING → REJECTED | không PENDING |  | INVALID_STATUS_TRANSITION | reject accepted | Implemented | TeamServiceImpl.java |
| BR-TEAM-040 | FR-12 | Team | Invariant | Leader không LEFT; leader remove chỉ ACCEPTED (không dùng LEFT cho PENDING invite) | leader leave / remove pending via LEFT |  | LEADER_CANNOT_LEAVE_TEAM / INVALID_STATUS_TRANSITION | leader leave without transfer | Implemented | TeamServiceImpl.java |
| BR-TEAM-041 | FR-12 | Team | Authorization/Validation | Leader hủy chỉ PENDING invite khi còn đổi được membership | không PENDING / không leader / membership locked |  | FORBIDDEN / CANNOT_DELETE_ACCEPTED_MEMBER / TEAM_* | delete accepted member | Implemented | TeamServiceImpl.java#removePendingMember |
| BR-TEAM-042 | FR-13B-R | Team | Gate | Đổi track khi round chưa active và chưa scoringLocked | isActive hoặc scoringLocked | 422 | ROUND_ALREADY_ACTIVE | relottery after activate | Implemented | TeamServiceImpl.java#reassignTrack |
| BR-TEAM-043 | FR-13B-R | Team | Validation | Track mới thuộc same round và OPEN | sai round / CLOSED |  | INVALID_STATE / TRACK_CLOSED | move to closed track | Implemented | TeamServiceImpl.java |
| BR-TEAM-044 | FR-13C | Team | Gate | Cần team_round_participation; không gán FINAL; không trùng mentor cùng round | thiếu TRP / isFinal / already has mentor |  | TEAM_NOT_IN_ROUND / MENTOR_ASSIGNMENT_NOT_FOR_FINAL_ROUND / TEAM_ALREADY_HAS_MENTOR_IN_ROUND | assign mentor final | Implemented | TeamServiceImpl.java |
| BR-TEAM-045 | FR-13C | Team | Validation | User gán phải role MENTOR | role != MENTOR | 422 | USER_INVALID_ROLE | assign student as mentor | Implemented | TeamServiceImpl.java |
| BR-TEAM-046 | FR-13C | Team | Gap | removeMentor xóa assignment không check điểm (docs yêu cầu ROUND_HAS_SCORES) | DELETE mentor | xóa luôn | ROUND_HAS_SCORES (chưa dùng) | remove mentor after scores exist | Partial | TeamServiceImpl.java#removeMentor vs docs mf02 |
| BR-TEAM-047 | FR-13/GD3 | Team | Validation | DQ bắt buộc reason; không DQ lại | blank / already ELIMINATED |  | VALIDATION_FAILED / INVALID_STATE | DQ without reason | Implemented | TeamServiceImpl.java#eliminateTeam |
| BR-TEAM-049 | FR-11 | Team | Gate | Admin create chặn khi prelim round đã active | any non-final round isActive | 422 | ROUND_ALREADY_ACTIVE | admin create after prelim open | Implemented | TeamServiceImpl.java#adminCreateTeam |
| BR-TEAM-050 | FR-11 | Team | Validation | Admin create bắt buộc size trong min-max (1 leader + members) | out of range |  | TEAM_INVALID_MEMBER_COUNT | admin create 2 people | Implemented | TeamServiceImpl.java |
| BR-TEAM-051 | FR-11 | Team | StateTransition | Admin create → ACTIVE ngay; lock nếu registration closed | create ok | ACTIVE + optional lock | USER_IN_ANOTHER_TEAM nếu member đã có đội | admin create after close locks | Implemented | TeamServiceImpl.java |
| BR-TEAM-052 | FR-12 | Team | Gate | Admin add member khi chưa lock, chưa full, user chưa có đội | locked/full/in team |  | TEAM_LOCKED / TEAM_MEMBER_FULL / USER_IN_ANOTHER_TEAM | admin add when locked | Implemented | TeamServiceImpl.java#adminAddMember |
| BR-TEAM-053 | FR-11 | Team | Gate | Merge cùng hackathon, không self, không locked, prelim chưa active, total ≤ max | vi phạm |  | INVALID_STATE / TEAM_LOCKED / ROUND_ALREADY_ACTIVE / TEAM_MEMBER_FULL | merge over max | Implemented | TeamServiceImpl.java#adminMergeTeams |
| BR-TEAM-054 | FR-11 | Team | StateTransition | Sau merge: nếu ≥ min → target ACTIVE (+lock nếu closed); source REJECTED + releaseMembers | merge ok | gộp members làm MEMBER | N/A | source disbanded message | Implemented | TeamServiceImpl.java |
| BR-TEAM-055 | FR-11 | Team | Policy | Incomplete/matchmaking = PENDING với acceptedCount ngoài [min,max] | query incomplete | trả danh sách | N/A | under-min and over-max both listed | Implemented | TeamServiceImpl.java#getIncompleteTeams |
| BR-TEAM-056 | FR-13A | Team | Scheduler | Cron khóa ACTIVE khi registration period ended; reject PENDING out-of-range + withdraw orphans | ONGOING + registration ended | lock ACTIVE; reject incomplete; withdraw orphans | N/A | run lockTeamsAfterRegistrationEnd | Implemented | teams/.../TeamLockServiceImpl.java |
| BR-TEAM-057 | FR-11 | Team | Scheduler | PENDING chưa confirm và graceDeadline < now → REJECTED + withdraw ACCEPTED + notify | expireOverdueGraceTeams | LEFT + withdraw + TEAM_FORMATION_GRACE_EXPIRED | N/A | wait >24h after close-early grace | Implemented | FormationGraceExpiryServiceImpl.java |
| BR-TEAM-059 | FR-13/GD3 | Team | Invariant | Backfill candidate không ELIMINATED; same assignedGroup (case-insensitive) cho TOP_N path | filter ranking | skip ineligible | DQ_NO_BACKFILL_* audits khi empty | empty bench logs audit only | Implemented | TeamDqBackfillServiceImpl.java |
| BR-TEAM-060 | FR-11 | Team | Authorization | Journey yêu cầu TeamAccessGuard | unauthorized viewer | 403 | FORBIDDEN | IDOR journey | Implemented | TeamJourneyServiceImpl.java |
| BR-TEAM-061 | FR-13 | Team | SideEffect | releaseMembers: PENDING→REJECTED; ACCEPTED→LEFT (+optional withdraw); notify TEAM_RELEASED | reject/disband/merge source | audit TEAM_MEMBERS_RELEASED | N/A | banner TEAM_RELEASED | Implemented | TeamMembershipReleaseServiceImpl.java |

## Lottery

| Rule ID | Related Req ID | Module | Rule Type | Business Rule Statement | Condition / Trigger | System Action / Expected Result | Exception / Error Message | Test Case / Example Data | Status | Evidence Link / Note |
|---------|-----------------|--------|-----------|-------------------------|---------------------|---------------------------------|---------------------------|--------------------------|--------|----------------------|
| BR-LOT-001 | FR-13B | Lottery | Gate | Lottery chỉ ONGOING | not ONGOING |  | HACKATHON_NOT_ONGOING | lottery DRAFT | Implemented | HackathonLotteryServiceImpl.java |
| BR-LOT-002 | FR-13B | Lottery | Gate | Registration phải ended; mọi ACTIVE phải locked | !canRunLottery |  | REGISTRATION_CLOSED (chưa end) / ACTIVE_TEAMS_NOT_LOCKED | lottery before close / unlocked ACTIVE | Implemented | HackathonLotteryServiceImpl + HackathonRegistrationSupport |
| BR-LOT-003 | FR-13B | Lottery | Gate | Không còn đội PENDING (awaiting/grace/blocked) | PendingTeamGateSnapshot.hasPending | 422 | TEAMS_PENDING_APPROVAL | lottery with pending teams | Implemented | PendingTeamGateService.java |
| BR-LOT-004 | FR-13B | Lottery | Gate | Không lottery cho FINAL; round chưa active | isFinal / isActive |  | INVALID_FINAL_ROUND / ROUND_ALREADY_ACTIVE | lottery final round | Implemented | HackathonLotteryServiceImpl.java |
| BR-LOT-005 | FR-13B | Lottery | Policy | Auto-lottery: ACTIVE+locked chưa có track; shuffle; round-robin OPEN tracks; group = Bảng A/B/… theo sequence | assignments empty | tạo assignments auto | INVALID_STATE nếu không có OPEN track mà còn team | auto lottery 24 teams 4 tracks balanced | Implemented | HackathonLotteryServiceImpl.java |
| BR-LOT-006 | FR-13B | Lottery | Validation | Manual assignment: team cùng hackathon, ACTIVE, locked, track OPEN cùng round, chưa có track round | vi phạm từng check |  | CROSS_HACKATHON_VIOLATION / TEAM_NOT_ACTIVE / TEAM_NOT_LOCKED / TRACK_CLOSED / TEAM_ALREADY_IN_TRACK_THIS_ROUND / INVALID_STATE | assign unlocked team | Implemented | HackathonLotteryServiceImpl.java |
| BR-LOT-007 | FR-13B | Lottery | SideEffect | Tạo TRP nếu thiếu + TRT registrationType=ASSIGNED + audit TEAM_TRACK_ASSIGNED | assignment ok | persist participation+track | N/A | team appears in track | Implemented | HackathonLotteryServiceImpl.java |
| BR-LOT-008 | FR-13B | Lottery | Invariant | Lottery/createTeam/close dùng findByIdForUpdate serialize critical section | concurrent ops | pessimistic lock hackathon row | N/A | race create vs lottery | Implemented | HackathonLotteryServiceImpl / TeamServiceImpl / CloseService |

## Submission

| Rule ID | Related Req ID | Module | Rule Type | Business Rule Statement | Condition / Trigger | System Action / Expected Result | Exception / Error Message | Test Case / Example Data | Status | Evidence Link / Note |
|---------|-----------------|--------|-----------|-------------------------|---------------------|---------------------------------|---------------------------|--------------------------|--------|----------------------|
| BR-SUB-001 | FR-16 | Submission | Authorization | Chỉ sinh viên APPROVED được nộp bài | Actor role≠STUDENT hoặc status≠APPROVED | Từ chối submit | FORBIDDEN | POST submit với JUDGE/DRAFT student | Implemented | submissions/service/impl/SubmissionServiceImpl.java |
| BR-SUB-002 | FR-16 | Submission | Authorization | Người nộp phải là thành viên ACCEPTED của đội | Không có TeamMember ACCEPTED | Từ chối submit | NOT_TEAM_MEMBER | Submit teamId không thuộc user | Implemented | submissions/service/impl/SubmissionServiceImpl.java |
| BR-SUB-003 | FR-16 | Submission | Validation | Đội phải ACTIVE mới nộp | team.status≠ACTIVE | Từ chối | TEAM_NOT_ACTIVE | Submit đội PENDING/ELIMINATED | Implemented | submissions/service/impl/SubmissionServiceImpl.java |
| BR-SUB-004 | FR-16 | Submission | Gate | Hackathon FINISHED không nhận bài | hackathon.status=FINISHED | Từ chối | EVENT_FINISHED | Submit sau FINISHED | Implemented | submissions/service/impl/SubmissionServiceImpl.java |
| BR-SUB-005 | FR-16/FR-30A | Submission | Gate | Hackathon PENDING_CONFIRM (đã đóng sổ) không nhận bài | hackathon.status=PENDING_CONFIRM | Từ chối | SUBMISSION_CLOSED | Submit sau lock CK | Implemented | submissions/service/impl/SubmissionServiceImpl.java |
| BR-SUB-006 | FR-16 | Submission | Gate | Hackathon DRAFT chưa mở nộp | hackathon.status=DRAFT | Từ chối | SUBMISSION_NOT_STARTED | Submit khi DRAFT | Implemented | submissions/service/impl/SubmissionServiceImpl.java |
| BR-SUB-007 | FR-16 | Submission | Gate | Chỉ ONGOING mới nộp | status≠ONGOING (các case còn lại) | Từ chối | HACKATHON_NOT_ONGOING | Submit CANCELLED | Implemented | submissions/service/impl/SubmissionServiceImpl.java |
| BR-SUB-008 | FR-16 | Submission | Invariant | Round/Track phải cùng hackathon với đội | team.hackathonId≠round.hackathonId | Từ chối | CROSS_HACKATHON_VIOLATION | Cross-hackathon submit | Implemented | submissions/service/impl/SubmissionServiceImpl.java |
| BR-SUB-009 | FR-15/FR-16 | Submission | Gate | Round phải is_active | Round chưa activate hoặc đã deactivate | Từ chối | ROUND_NOT_ACTIVE | Submit round inactive | Implemented | rounds/guard/RoundAccessGuard.java;SubmissionServiceImpl.java |
| BR-SUB-010 | FR-16 | Submission | Validation | Sơ loại bắt buộc trackId; CK bắt buộc roundId không trackId | isFinal+trackId hoặc !isFinal+chỉ roundId hoặc thiếu cả hai | Từ chối | INVALID_STATE | Submit CK với trackId / SL thiếu trackId | Implemented | submissions/service/impl/SubmissionServiceImpl.java#validateSubmissionRouting |
| BR-SUB-011 | FR-16 | Submission | Authorization | Sơ loại: đội phải có TeamRoundTrack | Không có TRT team+track | Từ chối | TEAM_NOT_IN_TRACK | Submit track chưa gán | Implemented | submissions/service/impl/SubmissionServiceImpl.java |
| BR-SUB-012 | FR-16/FR-30 | Submission | Authorization | Sơ loại chỉ PARTICIPATING được nộp/sửa; ADVANCED\|ELIMINATED chặn | TRT.participationStatus≠PARTICIPATING | 403 | PRELIM_NOT_MUTABLE | Submit sau advance/eliminate | Implemented | teams/support/PrelimMutationGuard.java |
| BR-SUB-013 | FR-26 | Submission | Authorization | CK: đội phải có TeamRoundParticipation | Không có TRP team+finalRound | Từ chối | TEAM_NOT_IN_ROUND | Submit CK chưa advance | Implemented | submissions/service/impl/SubmissionServiceImpl.java |
| BR-SUB-014 | FR-16 | Submission | Validation | Multipart: repoUrl phải GitHub public đúng pattern; cấm Drive | repoUrl blank/Drive/sai pattern | Từ chối | VALIDATION_FAILED\|INVALID_REPO_PLATFORM | Submit Drive / sai URL | Implemented | submissions/support/GitHubRepoValidator.java |
| BR-SUB-015 | FR-16 | Submission | Validation | HEAD GitHub 404/≥400 → không public (nếu check bật) | HTTP 404 hoặc ≥400 hoặc network error | Từ chối | REPO_NOT_PUBLIC | Private repo / toggle app.submission.github-public-check-enabled | Implemented | submissions/support/GitHubRepoValidator.java |
| BR-SUB-016 | FR-16 | Submission | Validation | JSON mode: cấm Drive trong repoUrl | repoUrl chứa drive.google.com | Từ chối | INVALID_REPO_PLATFORM | Legacy JSON submit Drive | Implemented | SubmissionServiceImpl#validateRepoUrl |
| BR-SUB-017 | FR-16 | Submission | Validation | JSON mode: slideUrl không được đuôi .pdf | slideUrl endsWith .pdf | Từ chối | INVALID_SLIDE_FORMAT | slideUrl=...pdf | Implemented | SubmissionServiceImpl#validateSlideUrl |
| BR-SUB-018 | FR-16 | Submission | Validation | Sau deadline/close-early (policy≠HARD_LOCK) bắt buộc lateReason | afterDeadline && !HARD_LOCK && blank lateReason | Từ chối | LATE_REASON_REQUIRED | Late submit không reason | Implemented | submissions/service/impl/SubmissionServiceImpl.java |
| BR-SUB-019 | FR-16 | Submission | Policy | afterDeadline = closedEarly≠null OR now≥submissionDeadline | Close sớm hoặc hết hạn | Đánh isLate / resolve status | N/A | Close-early rồi submit ngay | Implemented | SubmissionServiceImpl#submitInternal |
| BR-SUB-020 | FR-16 | Submission | StateTransition | Trong hạn → status SUBMITTED | !afterDeadline | Set SUBMITTED | N/A | Submit trước deadline | Implemented | SubmissionServiceImpl#resolveSubmitStatus |
| BR-SUB-021 | FR-16/FR-26 | Submission | StateTransition | HARD_LOCK + afterDeadline → REJECTED | Late + policy HARD_LOCK (CK) | Set REJECTED isLate | N/A | Final late submit | Implemented | SubmissionServiceImpl#resolveSubmitStatus;LateSubmissionPolicy.java |
| BR-SUB-022 | FR-16/FR-16A | Submission | StateTransition | ALLOW_LATE_PENDING + afterDeadline → LATE_PENDING | Late + ALLOW_LATE_PENDING | Set LATE_PENDING chờ duyệt | N/A | Prelim late submit | Implemented | SubmissionServiceImpl;LateSubmissionPolicy.java |
| BR-SUB-023 | FR-16 | Submission | Invariant | Đã LATE_APPROVED\|ACCEPTED thì giữ status khi sửa lại | existing LATE_APPROVED/ACCEPTED | Không hạ status | N/A | Sửa bài đã duyệt trễ | Implemented | SubmissionServiceImpl#resolveStatusOnSubmit |
| BR-SUB-024 | FR-16 | Submission | StateTransition | Đã SUBMITTED rồi sửa sau deadline → LATE_PENDING/REJECTED | existing SUBMITTED && afterDeadline | Chuyển computed late status | N/A | Sửa bài đúng hạn sau close-early | Implemented | SubmissionServiceImpl#resolveStatusOnSubmit |
| BR-SUB-025 | FR-16 | Submission | StateTransition | Bài REJECTED không nộp lại | existing.status=REJECTED | Từ chối | INVALID_STATE | Resubmit sau reject | Implemented | submissions/service/impl/SubmissionServiceImpl.java |
| BR-SUB-026 | FR-16 | Submission | Validation | Multipart: lần tạo hoặc chưa có slide → PDF bắt buộc; validate size/MIME/magic | slide thiếu/sai PDF/quá MB | Từ chối | SLIDE_FILE_REQUIRED\|INVALID_SLIDE_FILE | Submit không PDF / PNG | Implemented | submissions/support/SubmissionSlideStorage.java |
| BR-SUB-027 | FR-16 | Submission | Validation | Sau store vẫn thiếu slideStorageKey → lỗi | store thất bại silent | Từ chối | INVALID_SLIDE_FILE | Mock storage fail | Implemented | SubmissionServiceImpl#submitInternal |
| BR-SUB-028 | FR-16 | Submission | Invariant | Upsert top-by-team+track (SL) hoặc team+round (CK) | Submit lại cùng scope | Update cùng bản ghi + audit CREATE/UPDATE | N/A | Nộp 2 lần cùng track | Implemented | SubmissionServiceImpl#submitInternal |
| BR-SUB-029 | FR-17 | Submission | SideEffect | Sau nộp: notify members + enqueue metadata + roster invalidate | Submit thành công | Notify SUBMISSION_RECEIVED; enqueueFetch; WS invalidate | N/A | Kiểm tra notification + metadata row | Implemented | SubmissionServiceImpl;SubmissionMetadataServiceImpl.java |
| BR-SUB-030 | FR-17 | Submission | SideEffect | Enqueue metadata chỉ khi có repoUrl và chưa có row; status PENDING | enqueueFetch(submissionId) | Tạo SubmissionMetadata PENDING; không worker | N/A | Double enqueue idempotent | Partial | submissions/service/impl/SubmissionMetadataServiceImpl.java |
| BR-SUB-031 | FR-18 | Submission | Policy | Chỉ SUBMITTED\|LATE_APPROVED\|ACCEPTED được chấm | status khác (LATE_PENDING/REJECTED…) | isGradable=false | SUBMISSION_NOT_GRADABLE (khi score) | Chấm LATE_PENDING | Implemented | submissions/policy/SubmissionGradablePolicy.java |
| BR-SUB-032 | FR-16 | Submission | Authorization | Slide: COORDINATOR ok; JUDGE cần assignment round; STUDENT cần member; khác forbidden | GET slide | 403 FORBIDDEN | FORBIDDEN | Judge không assign tải slide | Implemented | SubmissionServiceImpl#assertSlideAccess |
| BR-SUB-033 | FR-18 | Submission | Authorization | List: JUDGE ẩn teamName/lateReason; bắt buộc roundId+assignment; STUDENT bắt buộc teamId+member | GET list theo role | Filter/ẩn danh | FORBIDDEN | Judge list không roundId | Implemented | SubmissionServiceImpl#list* |

## Score

| Rule ID | Related Req ID | Module | Rule Type | Business Rule Statement | Condition / Trigger | System Action / Expected Result | Exception / Error Message | Test Case / Example Data | Status | Evidence Link / Note |
|---------|-----------------|--------|-----------|-------------------------|---------------------|---------------------------------|---------------------------|--------------------------|--------|----------------------|
| BR-SCORE-001 | FR-18 | Score | Validation | Chấm chỉ khi gradable | !SubmissionGradablePolicy | Từ chối | SUBMISSION_NOT_GRADABLE | Score LATE_PENDING | Implemented | scores/service/impl/ScoreServiceImpl.java |
| BR-SCORE-002 | FR-18/FR-21 | Score | StateTransition | Đội ELIMINATED khỏi track không chấm | TRT=ELIMINATED | Từ chối | INVALID_STATE | Score đội đã loại | Implemented | ScoreServiceImpl.java |
| BR-SCORE-003 | FR-18 | Score | Validation | scoreValue ≤ criterion.maxScore | score > max | Từ chối | SCORE_EXCEEDS_MAX | Score 101/100 | Implemented | ScoreServiceImpl.java |
| BR-SCORE-004 | FR-18 | Score | Authorization | Judge phải assign track (SL) hoặc round (CK) | Không assignment | 403 | JUDGE_NOT_ASSIGNED_TO_TRACK\|JUDGE_NOT_ASSIGNED | Judge track khác | Implemented | scores/guard/JudgeAssignmentGuard.java |
| BR-SCORE-005 | FR-18 | Score | Authorization | Cấm mentor đội/track đồng thời judge chấm | Mentor team hoặc mentor+judge cùng track | Từ chối | CONFLICT_MENTOR_JUDGE_SAME_TRACK | Mentor chấm đội mình | Implemented | scores/guard/MentorJudgeConflictGuard.java |
| BR-SCORE-006 | FR-20A | Score | Gate | Round scoring_locked → không chấm | scoringLocked=true | 423 ScoringLockedException | SCORING_LOCKED | Score sau lock | Implemented | ScoreServiceImpl.java |
| BR-SCORE-007 | FR-18/FR-23 | Score | Gate | Chỉ chấm khi RoundPhase=JUDGING | phase≠JUDGING (còn CODING) | Từ chối | SCORING_NOT_OPEN | Score trước hết hạn nộp | Implemented | ScoreServiceImpl#requireScoringOpen;RoundPhaseResolver.java |
| BR-SCORE-008 | FR-18/FR-23 | Score | Gate | Chỉ chấm khi slot PRESENTING | queueStatus≠PRESENTING hoặc không slot | Từ chối | SCORING_NOT_OPEN | Score đội WAITING | Implemented | ScoreServiceImpl#requireScoringOpen |
| BR-SCORE-009 | FR-18/FR-23 | Score | Gate | Timer phase phải PRESENTING\|QA\|PAUSED\|ENDED (không IDLE/SETUP) | timer IDLE/SETUP | Từ chối | SCORING_NOT_OPEN | Score trước start timer | Implemented | presentation/support/PresentationScoringGate.java |
| BR-SCORE-010 | FR-18 | Score | Validation | Criterion phải thuộc track (SL) hoặc round (CK) của submission | Criterion sai scope | Từ chối | CRITERION_WRONG_ROUND | Criterion track khác | Implemented | ScoreServiceImpl#validateCriterionForSubmission |
| BR-SCORE-011 | FR-18/FR-23 | Score | SideEffect | Upsert score: isFinal=false; xóa confirm judge; publish LiveScoreSavedEvent | POST score (kể cả sửa) | Invalidate Chốt điểm | N/A | Sửa điểm sau confirm → phải confirm lại | Implemented | ScoreServiceImpl.java |
| BR-SCORE-012 | FR-20 | Score | Gate | GET ranking (không preview) cần scoring_locked | !scoringLocked | Từ chối | ROUND_NOT_SCORING_LOCKED | Ranking trước lock dùng preview | Implemented | RoundProgressionServiceImpl#ranking |
| BR-SCORE-013 | FR-20/FR-24 | Score | Gate | Public scoreboard cần isPublished | !isPublished | Từ chối | RESULT_NOT_PUBLISHED | Scoreboard trước publish | Implemented | RoundProgressionServiceImpl#scoreboard |

## RoundProgression

| Rule ID | Related Req ID | Module | Rule Type | Business Rule Statement | Condition / Trigger | System Action / Expected Result | Exception / Error Message | Test Case / Example Data | Status | Evidence Link / Note |
|---------|-----------------|--------|-----------|-------------------------|---------------------|---------------------------------|---------------------------|--------------------------|--------|----------------------|
| BR-RPROG-001 | FR-16A | RoundProgression | StateTransition | Chỉ LATE_PENDING mới review | status≠LATE_PENDING | Từ chối | SUBMISSION_NOT_LATE_PENDING | Review SUBMITTED | Implemented | SubmissionServiceImpl#reviewLate |
| BR-RPROG-002 | FR-16A/FR-26 | RoundProgression | Gate | CK HARD_LOCK không duyệt bài trễ | isFinal && HARD_LOCK | Từ chối | LATE_PENDING_NOT_ALLOWED | Review late trên CK | Implemented | SubmissionServiceImpl#reviewLate |
| BR-RPROG-003 | FR-16A | RoundProgression | Validation | REJECT bắt buộc note | decision=REJECT && blank note | Từ chối | REVIEW_NOTE_REQUIRED | Reject không note | Implemented | SubmissionServiceImpl#reviewLate |
| BR-RPROG-004 | FR-16A | RoundProgression | StateTransition | APPROVE→LATE_APPROVED; REJECT→REJECTED + reviewedBy/At/note | PATCH review-late | Persist + audit SUBMISSION_LATE_REVIEW | N/A | Approve rồi chấm được | Implemented | SubmissionServiceImpl#reviewLate |
| BR-RPROG-005 | FR-16A/FR-23 | RoundProgression | SideEffect | APPROVE: append queue nếu đã shuffle; fail → flag queueAppendFailed + notify Coord | Approve sau shuffle | appendLateApprovedIfShuffled hoặc soft-fail | N/A (soft) | Approve khi queue đã shuffle | Implemented | SubmissionServiceImpl#reviewLate;PresentationQueueServiceImpl#appendLateApprovedIfShuffled |
| BR-RPROG-006 | FR-15A | RoundProgression | StateTransition | Phát đề one-way: đã problemReleasedAt không sửa | problemReleasedAt≠null | Từ chối | INVALID_STATE | Release lần 2 | Implemented | RoundProgressionServiceImpl#releaseProblem |
| BR-RPROG-007 | FR-15A | RoundProgression | Gate | Chỉ phát đề khi đã tới examAt | examAt null hoặc now&lt;examAt | Từ chối | INVALID_ROUND_STATE_BEFORE_EXAM | Release trước giờ thi | Implemented | RoundProgressionServiceImpl#releaseProblem |
| BR-RPROG-008 | FR-15A | RoundProgression | Validation | SL: có track ACTIVE; mỗi track có PDF đề | Thiếu track/PDF | Từ chối | VALIDATION_FAILED | Release thiếu PDF track | Implemented | RoundProgressionServiceImpl#releaseProblem |
| BR-RPROG-009 | FR-15A | RoundProgression | Validation | CK: không upload PDF round; reuse PDF track sơ loại của đội advance | file≠empty hoặc thiếu prelim PDF | Từ chối | VALIDATION_FAILED | Upload PDF trên CK | Implemented | RoundProgressionServiceImpl#releaseProblem |
| BR-RPROG-010 | FR-15A | RoundProgression | SideEffect | Set problemReleasedAt (+track SL); audit; notify PROBLEM_RELEASED | Release OK | Persist + notify | N/A | Kiểm tra notification | Implemented | RoundProgressionServiceImpl#releaseProblem |
| BR-RPROG-011 | FR-16/FR-20A | RoundProgression | Gate | Đã lock scoring không close-early | scoringLocked | Từ chối | INVALID_STATE | Close sau lock | Implemented | RoundProgressionServiceImpl#closeSubmissionEarly |
| BR-RPROG-012 | FR-16 | RoundProgression | StateTransition | Chỉ close-early một lần | submissionClosedEarlyAt≠null | Từ chối | SUBMISSION_ALREADY_CLOSED | Close 2 lần | Implemented | RoundProgressionServiceImpl#closeSubmissionEarly |
| BR-RPROG-013 | FR-15A/FR-16 | RoundProgression | Gate | Phải đã phát đề | problemReleasedAt=null | Từ chối | INVALID_ROUND_STATE_UNRELEASED | Close trước release | Implemented | RoundProgressionServiceImpl#closeSubmissionEarly |
| BR-RPROG-014 | FR-16 | RoundProgression | Gate | Phải đã tới examAt | now&lt;examAt | Từ chối | INVALID_ROUND_STATE_BEFORE_EXAM | Close trước exam | Implemented | RoundProgressionServiceImpl#closeSubmissionEarly |
| BR-RPROG-015 | FR-16 | RoundProgression | SideEffect | Set closedEarlyAt; clamp deadline=now-5s; normalize submissionOpen≤deadline | Close OK | Clamp deadline → mọi submit sau = late | N/A | Submit ngay sau close | Implemented | RoundProgressionServiceImpl#closeSubmissionEarly |
| BR-RPROG-016 | FR-20A | RoundProgression | StateTransition | Không lock lại khi đã locked | scoringLocked | Từ chối | INVALID_STATE | Lock 2 lần | Implemented | RoundProgressionServiceImpl#lockScoring |
| BR-RPROG-017 | FR-20A | RoundProgression | Gate | Phải closedEarly hoặc pastDeadline | Chưa hết giờ và chưa close sớm | Từ chối | INVALID_ROUND_STATE_NOT_CLOSED | Lock khi còn CODING | Implemented | RoundProgressionServiceImpl#lockScoring |
| BR-RPROG-018 | FR-20A/FR-23 | RoundProgression | Gate | Phải đã shuffle queue (CK: round flag; SL: mọi track) | Chưa shuffle | Từ chối | INVALID_ROUND_STATE_QUEUE_NOT_SHUFFLED | Lock chưa shuffle | Implemented | rounds/support/RoundPresentationReadiness.java |
| BR-RPROG-019 | FR-20A/FR-23 | RoundProgression | Gate | Mọi slot terminal DONE\|ELIMINATED (SKIPPED không terminal) | Còn WAITING/PRESENTING/SKIPPED | Từ chối | INVALID_ROUND_STATE_PRESENTATIONS_INCOMPLETE | Lock còn WAITING hoặc SKIPPED | Implemented | RoundPresentationReadiness.java |
| BR-RPROG-020 | FR-20A | RoundProgression | Gate | Còn pending submissions: cần force=true + reason; không force → block | pending&gt;0 && !force | Từ chối hoặc warning | INVALID_ROUND_STATE_SCORING_INCOMPLETE\|FORCE_LOCK_REASON_REQUIRED | Lock thiếu điểm không force / force không reason | Implemented | RoundProgressionServiceImpl#lockScoring |
| BR-RPROG-021 | FR-20A | RoundProgression | Validation | force=true luôn bắt buộc reason (kể cả đủ điểm) | force && blank reason | Từ chối | FORCE_LOCK_REASON_REQUIRED | Force lock không reason | Implemented | RoundProgressionServiceImpl#lockScoring |
| BR-RPROG-022 | FR-20A/FR-30A | RoundProgression | SideEffect | Set locked+finalize is_final; event; nếu FINAL → hackathon PENDING_CONFIRM; auto tiebreak resolvable | Lock OK | scores.is_final=1; FR-30A status; autoApplyResolvableTiebreaks | N/A | Lock CK → PENDING_CONFIRM | Implemented | RoundProgressionServiceImpl#lockScoring;#transitionHackathonToPendingConfirm;#finalizeScoresForRound |
| BR-RPROG-023 | FR-20A | RoundProgression | Validation | Unlock bắt buộc reason; round phải đang locked | blank reason / chưa lock | Từ chối | UNLOCK_REASON_REQUIRED\|INVALID_STATE | Unlock không reason | Implemented | RoundProgressionServiceImpl#unlockScoring |
| BR-RPROG-024 | FR-20A | RoundProgression | SideEffect | Clear lock flags; audit; publish scoringUnlocked | Unlock OK | Cho chấm lại | N/A | Unlock rồi POST score | Implemented | RoundProgressionServiceImpl#unlockScoring |
| BR-RPROG-025 | FR-24 | RoundProgression | Validation | Publish chỉ round Sơ loại | isFinal=true | Từ chối | INVALID_STATE | Publish round CK | Implemented | RoundProgressionServiceImpl#publish |
| BR-RPROG-026 | FR-24 | RoundProgression | Gate | Publish cần scoring_locked | !scoringLocked | Từ chối | ROUND_NOT_SCORING_LOCKED | Publish trước lock | Implemented | RoundProgressionServiceImpl#publish |
| BR-RPROG-027 | FR-24 | RoundProgression | StateTransition | Đã published không publish lại | isPublished | Từ chối | INVALID_STATE | Publish 2 lần | Implemented | RoundProgressionServiceImpl#publish |
| BR-RPROG-028 | FR-24 | RoundProgression | SideEffect | Set isPublished+publishedAt/By; announcement best-effort; notify SCORE_RELEASED | Publish OK | Công bố kết quả | N/A | Student xem scoreboard | Implemented | RoundProgressionServiceImpl#publish |
| BR-RPROG-029 | FR-24/FR-30 | RoundProgression | Gate | Advance roster cần published | !isPublished | Từ chối | RESULT_NOT_PUBLISHED | Roster trước publish | Implemented | RoundProgressionServiceImpl#advanceRoster |
| BR-RPROG-030 | FR-22B | RoundProgression | Invariant | SL: đồng điểm tại cutoff TopN theo partition track_group → cần tiebreak | borderlineTeams &gt; remainingSlots | Trả TiebreakItem | N/A | Tạo 2 đội cùng điểm rank N | Implemented | RoundProgressionServiceImpl#detectRawTiebreakItems |
| BR-RPROG-031 | FR-22B | RoundProgression | Invariant | CK: mọi nhóm đồng điểm liên tiếp trên bảng xếp hạng | ≥2 đội same effective score | Trả items partition FINAL | N/A | 2 đội CK cùng điểm | Implemented | RoundProgressionServiceImpl#detectTiebreakForFinalRound |
| BR-RPROG-032 | FR-22B | RoundProgression | Policy | Effective score = total−penalty trừ khi hackathon FINISHED (đã net) | Hackathon status | Tránh trừ penalty 2 lần | N/A | FINISHED vs ONGOING | Implemented | RoundProgressionServiceImpl#effectiveScoreForTieDetection |
| BR-RPROG-033 | FR-22B | RoundProgression | Policy | SUBMISSION_TIME / PENALTY_SCORE auto-order nếu tách được; COORDINATOR_DECISION hoặc DEEP_TIE → manual | tiebreakRule + candidates | suggestedOrderedTeamIds hoặc requiresManual | N/A | Cùng submittedAt → DEEP_TIE | Implemented | rounds/support/TiebreakRuleOrdering.java |
| BR-RPROG-034 | FR-22B | RoundProgression | SideEffect | Sau lock: autoApplyResolvableTiebreaks cho item không manual | Lock scoring | Ghi penalty 0.01 increments casting vote | N/A | Lock với rule SUBMISSION_TIME tách được | Implemented | RoundProgressionServiceImpl#autoApplyResolvableTiebreaks;#applyTiebreakOrder |
| BR-RPROG-035 | FR-22B | RoundProgression | Gate | Resolve tiebreak cần scoring_locked | !locked | Từ chối | ROUND_NOT_SCORING_LOCKED | Resolve trước lock | Implemented | RoundProgressionServiceImpl#resolveTiebreak |
| BR-RPROG-036 | FR-22B | RoundProgression | Validation | orderedTeamIds không trùng | duplicate ids | Từ chối | INVALID_STATE | orderedTeamIds=[1,1] | Implemented | RoundProgressionServiceImpl#resolveTiebreak |
| BR-RPROG-037 | FR-22B | RoundProgression | Validation | orderedTeamIds phải khớp đúng nhóm đang tie | Set≠candidate set | INVALID_STATE hoặc TIEBREAK_ALREADY_RESOLVED | INVALID_STATE\|TIEBREAK_ALREADY_RESOLVED | Thứ tự đội không thuộc nhóm hòa | Implemented | RoundProgressionServiceImpl#resolveTiebreak |
| BR-RPROG-038 | FR-22B | RoundProgression | StateTransition | Đã có casting-vote → 409 | isCastingVote tồn tại | Conflict | TIEBREAK_ALREADY_RESOLVED | 2 Coord resolve song song | Implemented | RoundProgressionServiceImpl#resolveTiebreak |
| BR-RPROG-049 | FR-30 | RoundProgression | Validation | Advance chỉ round Sơ loại | isFinal | Từ chối | INVALID_STATE | Advance round CK | Implemented | RoundProgressionServiceImpl#requirePreliminaryRoundForProgression |
| BR-RPROG-050 | FR-24/FR-30 | RoundProgression | Gate | Advance cần scoring_locked + isPublished | Thiếu lock hoặc publish | Từ chối | ROUND_NOT_SCORING_LOCKED\|RESULT_NOT_PUBLISHED | Advance trước publish | Implemented | RoundProgressionServiceImpl#requireScoringLockedAndPublished |
| BR-RPROG-051 | FR-22B/FR-30 | RoundProgression | Gate | Còn unresolved tiebreak → không advance (auto-resolve trước) | tiebreak() còn items | Từ chối kèm unresolvedItems | TIEBREAK_REQUIRED | Advance khi còn hòa cutoff | Implemented | RoundProgressionServiceImpl#advanceTeams |
| BR-RPROG-052 | FR-30 | RoundProgression | Gate | Hackathon phải có round FINAL | Không có isFinal round | Từ chối | INVALID_FINAL_ROUND | Advance thiếu CK | Implemented | RoundProgressionServiceImpl#advanceTeams |
| BR-RPROG-053 | FR-30 | RoundProgression | Validation | Team không vừa advanced vừa eliminated | overlap sets | Từ chối | INVALID_STATE | Cùng teamId 2 list | Implemented | RoundProgressionServiceImpl#advanceTeams |
| BR-RPROG-054 | FR-30 | RoundProgression | SideEffect | Set TRT ADVANCED/ELIMINATED; upsert TeamRoundParticipation CK; audit | Advance OK | Idempotent upsert TRP | N/A | Advance rồi submit CK | Implemented | RoundProgressionServiceImpl#advanceTeams;#upsertFinalRoundParticipation |
| BR-RPROG-056 | FR-27 | RoundProgression | Validation | Chỉ assign judge trên round FINAL; warning nếu &lt;1 hoặc &lt;3 judges | !isFinal / ít judge | Assign FINAL_EXTERNAL + warnings MIN_FINAL_JUDGES_NOT_MET | INVALID_FINAL_ROUND | Assign trên prelim | Implemented | RoundProgressionServiceImpl#assignFinalJudges |
| BR-RPROG-057 | FR-16/FR-26 | RoundProgression | Policy | ALLOW_LATE_PENDING (SL) vs HARD_LOCK (CK) — BC-01 | Round.lateSubmissionPolicy | Nhánh status late | N/A | Đổi policy round | Implemented | rounds/value_object/LateSubmissionPolicy.java |

## Presentation

| Rule ID | Related Req ID | Module | Rule Type | Business Rule Statement | Condition / Trigger | System Action / Expected Result | Exception / Error Message | Test Case / Example Data | Status | Evidence Link / Note |
|---------|-----------------|--------|-----------|-------------------------|---------------------|---------------------------------|---------------------------|--------------------------|--------|----------------------|
| BR-PRES-001 | FR-23 | Presentation | Gate | Shuffle/next/skip chỉ khi RoundPhase=JUDGING | Còn CODING (trước deadline) | Từ chối | SUBMISSION_NOT_CLOSED_FOR_SHUFFLE | Shuffle trước hết hạn | Implemented | PresentationQueueServiceImpl#requireJudgingPhase;RoundPhaseResolver |
| BR-PRES-002 | FR-23/FR-20A | Presentation | Gate | Đã scoring_locked không shuffle | scoringLocked | Từ chối | INVALID_STATE | Shuffle sau lock | Implemented | PresentationQueueServiceImpl#shuffle |
| BR-PRES-003 | FR-23 | Presentation | StateTransition | Đã shuffled + còn slots → conflict | presentationShuffled && slots≠empty | 409 | PRESENTATION_ALREADY_SHUFFLED | Shuffle 2 lần | Implemented | PresentationQueueServiceImpl#shuffle |
| BR-PRES-004 | FR-23 | Presentation | Gate | Có PRESENTING\|DONE\|SKIPPED → không shuffle lại | Slot đã start | Từ chối | PRESENTATION_ALREADY_STARTED | Shuffle sau next | Implemented | PresentationQueueServiceImpl#assertNoPresentationStarted |
| BR-PRES-005 | FR-23 | Presentation | Policy | Shuffle chỉ lấy submission gradable (SL theo track; CK theo TRP) | Fisher-Yates + tạo slots | Slot1=PRESENTING còn lại WAITING; clear confirms | N/A | LATE_PENDING không vào queue | Implemented | PresentationQueueServiceImpl#shuffleTrack;#shuffleFinalRound |
| BR-PRES-006 | FR-23 | Presentation | Authorization | Shuffle/next/skip: Coord hoặc controller judge track/round | Không phải controller | 403 | NOT_TRACK_CONTROLLER | Judge thường gọi next | Implemented | presentation/guard/PresentationControllerGuard.java |
| BR-PRES-007 | FR-23 | Presentation | Authorization | Student chỉ xem queue hackathon đã đăng ký; Judge ẩn tên đội | Student cross-hackathon | 403 | FORBIDDEN | Student xem queue hackathon khác | Implemented | PresentationQueueServiceImpl#assertCanViewQueue |
| BR-PRES-008 | FR-23 | Presentation | StateTransition | Chuyển đội tiếp chỉ khi timer ENDED | phase≠ENDED | Từ chối | INVALID_STATE | Next khi còn PRESENTING | Implemented | PresentationQueueServiceImpl#advanceForScope |
| BR-PRES-009 | FR-23/FR-18 | Presentation | Gate | Cần có điểm; incomplete cần ack khi kết thúc sớm Q&A (hoặc slot cũ null qaEndedEarly) | NO_SCORES hoặc thiếu Chốt && requireComplete && !ack | Từ chối | SCORING_INCOMPLETE_BEFORE_NEXT | Next chưa confirm đủ judge sau early-end | Implemented | presentation/support/PresentationNextScoringGuard.java |
| BR-PRES-010 | FR-23 | Presentation | Authorization | acknowledgeIncompleteScoring chỉ Coord hoặc presentation controller | ack=true không đủ quyền | 403 | FORBIDDEN | Judge thường ack incomplete | Implemented | presentation/guard/PresentationForceAdvanceAckGuard.java |
| BR-PRES-011 | FR-23 | Presentation | Invariant | SL 1 judge: đủ criteria; SL ≥2 hoặc CK: mọi judge phải confirm Chốt điểm | isScoringIncomplete | Block next/early-end trừ force-ack hoặc hết giờ tự nhiên | SCORING_INCOMPLETE_BEFORE_NEXT | CK 3 judge thiếu 1 confirm | Implemented | presentation/support/PresentationScoringCompletionHelper.java |
| BR-PRES-012 | FR-23 | Presentation | SideEffect | Current→DONE; next→PRESENTING+SETUP; xóa confirm của next; publish WS | Next OK | Reset timer next | N/A | Kiểm tra SETUP trước start | Implemented | PresentationQueueServiceImpl#advanceForScope |
| BR-PRES-013 | FR-23 | Presentation | StateTransition | Skip no-show: không skip DONE; set SKIPPED+ENDED | submission DONE | Từ chối hoặc skip | INVALID_STATE | Skip đội DONE | Implemented | PresentationQueueServiceImpl#skipNoShow |
| BR-PRES-014 | FR-16A/FR-23 | Presentation | SideEffect | LATE_APPROVED sau shuffle → append WAITING cuối queue | Approve late + shuffled + chưa có slot | Tạo slot + publish | N/A | Approve late sau shuffle | Implemented | PresentationQueueServiceImpl#appendLateApprovedIfShuffled |
| BR-PRES-015 | FR-26 | Presentation | Invariant | CK/HARD_LOCK mà thấy LATE_PENDING\|LATE_APPROVED → log+audit INVARIANT_VIOLATION | GET queue final | Warn only không block | N/A | Data corrupt late trên CK | Implemented | PresentationQueueServiceImpl#warnHardLockLateInvariant |
| BR-PRES-016 | FR-23 | Presentation | Gate | Điều khiển timer cần JUDGING phase | phase≠JUDGING | Từ chối | SCORING_NOT_OPEN | Timer khi còn CODING | Implemented | PresentationTimerServiceImpl#resolveContext |
| BR-PRES-017 | FR-23 | Presentation | Validation | Phải có slot PRESENTING (pessimistic lock) | Không PRESENTING | Từ chối | INVALID_STATE | Start không có đội | Implemented | PresentationTimerServiceImpl#requirePresentingSlot |
| BR-PRES-018 | FR-23 | Presentation | StateTransition | Start chỉ từ IDLE\|SETUP\|ENDED | Timer đã chạy | Từ chối | INVALID_STATE | Start 2 lần | Implemented | PresentationTimerServiceImpl#start |
| BR-PRES-019 | FR-23 | Presentation | StateTransition | Pause: PRESENTING\|QA; Resume: PAUSED; QA chỉ từ PRESENTING; End sớm chỉ từ QA (hoặc pause sau QA) | Sai phase | Từ chối | INVALID_STATE | QA từ IDLE; End từ PRESENTING | Implemented | PresentationTimerServiceImpl |
| BR-PRES-020 | FR-23 | Presentation | StateTransition | QA remaining≤0 → ENDED + qaEndedEarly=false; không scoring guard | Timeout QA | Set ENDED | N/A | Hết giờ QA tự nhiên | Implemented | PresentationTimerServiceImpl#end;PresentationQaTimeoutMaterializer |
| BR-PRES-021 | FR-23 | Presentation | Validation | CK không set/clear duration theo trackId | isFinal+trackId | Từ chối | DESIGN_VIOLATION | PATCH duration CK+trackId | Implemented | PresentationDurationServiceImpl.java |
| BR-PRES-022 | FR-23 | Presentation | Gate | Đổi duration cấm khi scoringLocked hoặc đã DONE/timer started | Locked hoặc timer started | Từ chối | INVALID_STATE | Đổi phút sau start | Implemented | presentation/support/PresentationDurationMutationGuard.java |
| BR-PRES-023 | FR-23 | Presentation | SideEffect | Update duration → reschedule slots từ examAt nếu chưa có DONE và chưa lock | Update/clear duration | Cascade startsAt/endsAt + WS | N/A | Đổi phút trước shuffle start | Implemented | PresentationSlotCascadeServiceImpl.java |
| BR-PRES-024 | FR-23 | Presentation | Authorization | Grant controller: judge phải assigned track/round; expectedControllerJudgeId race → CONTROLLER_CONFLICT | Grant sai judge / race | 409 hoặc validation | JUDGE_NOT_ASSIGNED_TO_TRACK\|JUDGE_NOT_ASSIGNED\|CONTROLLER_CONFLICT | Grant judge chưa assign | Implemented | PresentationControllerServiceImpl.java |
| BR-PRES-025 | FR-23 | Presentation | Validation | Round controller chỉ cho isFinal | !isFinal get/grant round | Từ chối | INVALID_STATE | Grant controller round SL | Implemented | PresentationControllerServiceImpl.java |
| BR-PRES-026 | FR-23 | Presentation | Policy | Default controller = judge assignedAt sớm nhất (không dùng HEAD) | Không override controllerJudge | resolveTrack/RoundControllerId | N/A | 2 judge → earliest assignedAt | Implemented | PresentationControllerGuard#findDefaultControllerJudgeId |
| BR-PRES-027 | FR-18/FR-23 | Presentation | Policy | PUBLISHED&gt;SCORING_LOCKED&gt;SETUP(!active)&gt;CODING(now&lt;deadline)&gt;JUDGING | Thời gian/flags round | Điều khiển scoring/timer/shuffle | N/A | Close-early → JUDGING ngay | Implemented | presentation/support/RoundPhaseResolver.java |
| BR-PRES-028 | FR-23/FR-18 | Presentation | Gate | Kết thúc sớm Q&A (remaining&gt;0): bắt đủ GK Chốt điểm (trừ Coord/controller force-ack); set qaEndedEarly=true; FE chỉ hiện nút khi allJudgesSubmitted | Thiếu chốt && !ack | Từ chối | SCORING_INCOMPLETE_BEFORE_NEXT | Early-end khi GK chưa HOÀN TẤT & CHỐT | Implemented | PresentationTimerServiceImpl#end;timerControlGates.js |
| BR-PRES-029 | FR-23/FR-18 | Presentation | Policy | Next sau hết giờ tự nhiên (qaEndedEarly=false): ghi nhận điểm tới đâu, thiếu cũng được (vẫn chặn NO_SCORES) | incomplete sau natural | Cho phép Next | N/A | Natural end thiếu 1 judge | Implemented | PresentationNextScoringGuard;PresentationQueueServiceImpl |

## Appeal

> **Phase 10 — DQ appeal window** (cửa sổ nghỉ sau công bố sơ loại). Cũ: deadline 24h từ `eliminatedAt` — **đã thay** bằng `rounds.appeal_window_ends_at` mở one-shot lúc first publish.

| Rule ID | Related Req ID | Module | Rule Type | Business Rule Statement | Condition / Trigger | System Action / Expected Result | Exception / Error Message | Test Case / Example Data | Status | Evidence Link / Note |
|---------|-----------------|--------|-----------|-------------------------|---------------------|---------------------------------|---------------------------|--------------------------|--------|----------------------|
| BR-APPEAL-001 | FR-U-30 | Appeal | Policy | `appealWindowMinutes` cấu hình được: default **30**, min **10** (hoặc 0 — xem BR-APPEAL-008); sửa qua `PATCH /hackathons/{id}/appeal-window-minutes` khi DRAFT/ONGOING và **trước** prelim publish | Status≠DRAFT\|ONGOING, hoặc prelim đã `isPublished`, hoặc 1..9 phút | Từ chối | APPEAL_WINDOW_LOCKED_AFTER_PUBLISH\|APPEAL_WINDOW_BELOW_MINIMUM\|INVALID_STATE | Sửa sau publish SL | Implemented | hackathons/service/impl/HackathonServiceImpl#updateAppealWindowMinutes;HackathonController |
| BR-APPEAL-002 | FR-U-30 | Appeal | Lifecycle | Cửa sổ mở **chỉ lần first prelim publish**; `appeal_window_ends_at` one-shot — **không** reset khi republish / publish lại | `appealWindowEndsAt` đã set | `openOnFirstPublish` no-op; republish chỉ tăng `publish_revision` + `results_revised_at` | N/A | Republish sau approve | Implemented | appeals/service/impl/AppealWindowServiceImpl#openOnFirstPublish;#republish;RoundProgressionServiceImpl#publish |
| BR-APPEAL-003 | FR-U-30 | Appeal | Gate | Late publish khi remaining &lt; configured: bắt chọn mode **DELAY_FINAL** / **SHRINK** / **SKIP**; **SHRINK** bị chặn nếu remaining &lt; 10 phút; SKIP bắt buộc `skipReason` | `!fits` && mode null / SHRINK&lt;10 / SKIP thiếu lý do | Preflight liệt kê mode; publish áp dụng mode | APPEAL_WINDOW_DOES_NOT_FIT\|APPEAL_WINDOW_BELOW_MINIMUM\|APPEAL_WINDOW_SKIP_REASON_REQUIRED | Publish sát giờ CK | Implemented | AppealWindowServiceImpl#preflight;#openOnFirstPublish;PublishWithAppealWindowRequest |
| BR-APPEAL-004 | FR-U-30 | Appeal | Validation | Submit: **leader only**; chỉ đội **manual DQ** (`ELIMINATED` + có `eliminationReason`); **≥1 evidence**; trong cửa sổ; unique `(team_id, round_id)` | Member / ACTIVE / hết cửa sổ / trùng / thiếu evidence | Tạo PENDING + evidences | FORBIDDEN\|INVALID_STATE\|APPEAL_WINDOW_NOT_OPEN\|APPEAL_DEADLINE_EXPIRED\|APPEAL_EVIDENCE_REQUIRED\|APPEAL_ALREADY_SUBMITTED | Member appeal; hết hạn | Implemented | appeals/service/impl/AppealServiceImpl#create;me/support/StudentAccessGuard |
| BR-APPEAL-005 | FR-U-30 | Appeal | Gate | Advance bị chặn khi còn appeal **PENDING** hoặc **UNDER_REVIEW**; sau cửa sổ đóng, open → **EXPIRED** rồi advance được | `exists PENDING\|UNDER_REVIEW` sau expire | 422 block advance; EXPIRED không chặn | APPEAL_PENDING_BLOCKS_ADVANCE | Advance còn PENDING | Implemented | RoundProgressionServiceImpl#advanceTeams;AppealWindowServiceImpl#expireOpenAppealsForRound |
| BR-APPEAL-006 | FR-U-30 | Appeal | StateTransition | **Approve** phục hồi đội (ACTIVE, clear DQ) **chỉ pre-advance**; **Reject** bắt buộc `decisionNote` | Approve sau ADVANCED / Reject thiếu note | Reinstate hoặc REJECTED | APPEAL_APPROVE_AFTER_ADVANCE\|APPEAL_DECISION_NOTE_REQUIRED\|APPEAL_NOT_PENDING | Approve sau chốt CK | Implemented | AppealReviewServiceImpl#review;teams/service/impl/TeamReinstatementServiceImpl |
| BR-APPEAL-007 | FR-U-30 | Appeal | Policy | T-5 / DELAY_FINAL dời CK dùng ngân sách chung **`appeal_delay_minutes_applied` cap 30**; `allowEarlyExamAt` — **không** đổi MIN/MAX gap hours timeline | `applied + minutes &gt; 30` | Từ chối hoặc dời `examAt` + cộng budget | APPEAL_DELAY_LIMIT_EXCEEDED\|APPEAL_DELAY_NOT_APPLICABLE | Delay lần 2 vượt 30 | Implemented | rounds/support/RoundScheduleShiftService#delayFinalForAppeals;AppealWindowServiceImpl#applyDelay |
| BR-APPEAL-008 | FR-U-30 | Appeal | Policy | `appealWindowMinutes=0` = **emergency off**: không mở cửa sổ khi publish (GĐ4 như không có appeal) | configured ≤ 0 | `openOnFirstPublish` return sớm; preflight `fits=true` | N/A | Set 0 trước publish | Implemented | AppealWindowServiceImpl#openOnFirstPublish;#preflight;HackathonServiceImpl#validateAppealWindowMinutes |

## Calibration

| Rule ID | Related Req ID | Module | Rule Type | Business Rule Statement | Condition / Trigger | System Action / Expected Result | Exception / Error Message | Test Case / Example Data | Status | Evidence Link / Note |
|---------|-----------------|--------|-----------|-------------------------|---------------------|---------------------------------|---------------------------|--------------------------|--------|----------------------|
| BR-CALIB-001 | FR-29 | Calibration | Validation | Prompt: round thuộc hackathon; sample submission thuộc round | Sai hackathon/sample | Từ chối | INVALID_STATE | Sample submission round khác | Implemented | rbl/calibration/service/CalibrationService.java |
| BR-CALIB-002 | FR-29 | Calibration | StateTransition | Chấm thử chỉ khi prompt OPEN; close set CLOSED+closedAt | status≠OPEN | Từ chối | INVALID_STATE | Submit sau close | Implemented | CalibrationService#submit;#close |
| BR-CALIB-003 | FR-29 | Calibration | Authorization | Judge list open / submit / distribution cần assignment round | Không assign | Từ chối | INVALID_STATE | Judge chưa assign chấm thử | Implemented | CalibrationService#requireAssigned |
| BR-CALIB-004 | FR-29 | Calibration | Validation | Điểm thử ≤ maxScore criterion thuộc round | Criterion sai / vượt max | Từ chối | INVALID_STATE | Score thử vượt max | Implemented | CalibrationService#submit |

## JudgePortal

| Rule ID | Related Req ID | Module | Rule Type | Business Rule Statement | Condition / Trigger | System Action / Expected Result | Exception / Error Message | Test Case / Example Data | Status | Evidence Link / Note |
|---------|-----------------|--------|-----------|-------------------------|---------------------|---------------------------------|---------------------------|--------------------------|--------|----------------------|
| BR-JPORT-001 | FR-18 | JudgePortal | Authorization | Judge list submissions: roundId bắt buộc + roundScope assignment; chỉ gradable + track được assign | Không assign | 403/400 | FORBIDDEN\|VALIDATION_FAILED | Judge list round khác | Implemented | me/judge/.../JudgePortalServiceImpl.java#listSubmissions |
| BR-JPORT-002 | FR-18/FR-23 | JudgePortal | Gate | Chốt điểm: assigned; !scoringLocked; đủ criteria; slot PRESENTING; idempotent nếu đã confirm | Thiếu điểm / không PRESENTING / locked | Từ chối | VALIDATION_FAILED\|INVALID_STATE\|SCORING_LOCKED | Confirm khi WAITING | Implemented | JudgePortalServiceImpl#confirmSubmissionScoring |
| BR-JPORT-003 | FR-18/FR-20A | JudgePortal | Gate | Sửa comment điểm: chỉ owner; không khi scoringLocked | Locked hoặc không owner | FORBIDDEN / ScoringLockedException | FORBIDDEN\|SCORING_LOCKED | Sửa comment sau lock | Implemented | JudgePortalServiceImpl#updateScoreComment |
| BR-JPORT-004 | FR-22B | JudgePortal | Authorization | Judge casting vote chỉ khi chưa scoringLocked + có assignment scope | Locked / không assign | FORBIDDEN / ScoringLockedException | FORBIDDEN\|SCORING_LOCKED | Vote sau lock (Coord resolve) | Implemented | JudgePortalServiceImpl#submitTiebreakVote |
| BR-JPORT-005 | FR-18 | JudgePortal | Authorization | Cập nhật completion_status chỉ assignment của mình; không khi locked | Assignment người khác / locked | FORBIDDEN / ScoringLockedException | FORBIDDEN\|SCORING_LOCKED\|VALIDATION_FAILED | Cập nhật assignment người khác | Implemented | JudgePortalServiceImpl#updateScoringCompletion |

## StudentPortal

| Rule ID | Related Req ID | Module | Rule Type | Business Rule Statement | Condition / Trigger | System Action / Expected Result | Exception / Error Message | Test Case / Example Data | Status | Evidence Link / Note |
|---------|-----------------|--------|-----------|-------------------------|---------------------|---------------------------------|---------------------------|--------------------------|--------|----------------------|
| BR-SPORT-001 | FR-24 | StudentPortal | Gate | Student xem điểm chi tiết cần isPublished | !published | Từ chối | RESULT_NOT_PUBLISHED | Xem điểm trước publish | Implemented | me/student/.../StudentPortalServiceImpl.java#getTeamScoreBreakdown |

## MentorPortal

| Rule ID | Related Req ID | Module | Rule Type | Business Rule Statement | Condition / Trigger | System Action / Expected Result | Exception / Error Message | Test Case / Example Data | Status | Evidence Link / Note |
|---------|-----------------|--------|-----------|-------------------------|---------------------|---------------------------------|---------------------------|--------------------------|--------|----------------------|
| BR-MPORT-001 | N/A | MentorPortal | Access | Mentor chỉ xem team nếu được gán team (mọi round) hoặc gán track mà team đang thuộc | assertAssignedToTeam fail | 403 FORBIDDEN | FORBIDDEN | Mentor ngoài phạm vi | Implemented | me/support/MentorAccessGuard.java |
| BR-MPORT-002 | N/A | MentorPortal | Guard | Mentor xem điểm đội chỉ khi round.scoringLocked=true | scoringLocked≠true | Từ chối | ROUND_NOT_SCORING_LOCKED | Round chưa lock | Implemented | me/mentor/service/impl/MentorPortalServiceImpl.java |
| BR-MPORT-003 | N/A | MentorPortal | Guard | Lịch CK chỉ cho round isFinal=true | isFinal≠true | Từ chối | INVALID_FINAL_ROUND | Round sơ loại | Implemented | me/mentor/service/impl/MentorPortalServiceImpl.java |
| BR-MPORT-004 | FR-31 | MentorPortal | Guard | Mentor xem XH hackathon khi PENDING_CONFIRM hoặc FINISHED | status khác | Từ chối | RESULT_NOT_AVAILABLE | ONGOING mentor rankings | Implemented | me/mentor/service/impl/MentorPortalServiceImpl.java |
| BR-MPORT-005 | FR-31 | MentorPortal | Guard | Mentor rankings cần có vòng CK | Thiếu isFinal | Từ chối | INVALID_STATE | Hackathon không CK | Implemented | me/mentor/service/impl/MentorPortalServiceImpl.java |
| BR-MPORT-006 | FR-33 | MentorPortal | Calculation | Mentor chapter ranking dùng AVG điểm team (khác official SUM) | getHackathonRankings | Sort avg giảm dần | N/A | So với GET chapter-rankings official | Implemented | me/mentor/service/impl/MentorPortalServiceImpl.java |
| BR-MPORT-007 | N/A | MentorPortal | Policy | Lịch sử mentor chỉ hackathon FINISHED (lọc năm tùy chọn) | getHistory | Filter FINISHED + year | N/A | ONGOING không vào history | Implemented | me/mentor/service/impl/MentorPortalServiceImpl.java |
| BR-MPORT-008 | N/A | MentorPortal | Access | Portal /api/v1/me mentor* yêu cầu @MentorOnly | Gọi API mentor | 403 nếu không mentor duyệt | FORBIDDEN | STUDENT gọi mentor API | Implemented | me/controller/MentorMeController.java |

## Closure

| Rule ID | Related Req ID | Module | Rule Type | Business Rule Statement | Condition / Trigger | System Action / Expected Result | Exception / Error Message | Test Case / Example Data | Status | Evidence Link / Note |
|---------|-----------------|--------|-----------|-------------------------|---------------------|---------------------------------|---------------------------|--------------------------|--------|----------------------|
| BR-CLOSE-001 | FR-33 | Closure | Guard | confirm phải là true mới được chốt kết quả | PATCH confirm với confirm≠true | Từ chối | INVALID_STATE | confirm:false → 422 | Implemented | hackathons/service/impl/HackathonClosureServiceImpl.java |
| BR-CLOSE-002 | FR-33 | Closure | Guard | Chỉ confirm khi hackathon PENDING_CONFIRM | status ≠ PENDING_CONFIRM | Từ chối | HACKATHON_NOT_PENDING_CONFIRM | ONGOING/FINISHED → 422 | Implemented | hackathons/service/impl/HackathonClosureServiceImpl.java |
| BR-CLOSE-003 | FR-33 | Closure | Guard | Phải có Round Chung kết (isFinal) | Không tìm thấy round isFinal=true | Từ chối | MISSING_FINAL_ROUND | Hackathon thiếu CK | Implemented | hackathons/service/impl/HackathonClosureServiceImpl.java |
| BR-CLOSE-004 | FR-33 | Closure | Guard | Phải khóa chấm Chung kết trước confirm | finalRound.scoringLocked ≠ true | Từ chối | ROUND_NOT_SCORING_LOCKED | CK chưa lock | Implemented | hackathons/service/impl/HackathonClosureServiceImpl.java |
| BR-CLOSE-005 | FR-33 | Closure | Guard | Chưa chấm đủ điểm CK thì không confirm | hasIncompleteScoring(finalRound)=true | Từ chối | SCORING_INCOMPLETE_BEFORE_CONFIRM | Thiếu điểm CK | Implemented | hackathons/service/impl/HackathonClosureServiceImpl.java |
| BR-CLOSE-006 | FR-33 | Closure | Guard | CK còn DEEP_TIE chưa resolve thì không confirm | tiebreak(finalRound) không rỗng | Từ chối | TIEBREAK_UNRESOLVED | Còn đồng điểm CK | Implemented | hackathons/service/impl/HackathonClosureServiceImpl.java |
| BR-CLOSE-007 | FR-33 | Closure | Guard | Cần ≥1 prize trước khi confirm | Danh sách prize theo hackathon rỗng | Từ chối | NO_PRIZES_RECORDED | Profile prizes-empty | Implemented | hackathons/service/impl/HackathonClosureServiceImpl.java |
| BR-CLOSE-008 | FR-33 | Closure | Lifecycle | Confirm chuyển PENDING_CONFIRM → FINISHED (một chiều) | Tất cả gate pass | Set status FINISHED + save | N/A | Happy path Profile B | Implemented | hackathons/service/impl/HackathonClosureServiceImpl.java |
| BR-CLOSE-009 | FR-36 | Closure | Audit | Ghi audit đổi status khi confirm | Sau save FINISHED | log HACKATHON_STATUS_CHANGE (from/to/note/validatedBy/via=confirm) | N/A | Kiểm tra audit log | Implemented | hackathons/service/impl/HackathonClosureServiceImpl.java |
| BR-CLOSE-010 | FR-33 | Closure | SideEffect | Công bố kết quả qua announcement khi confirm | Sau FINISHED | publishResults RESULTS_PUBLISHED + STOMP | N/A | Feed announcement sau confirm | Implemented | hackathons/service/impl/HackathonClosureServiceImpl.java; announcements/service/AnnouncementService.java |
| BR-CLOSE-011 | FR-33 | Closure | Lifecycle | Sau confirm phát HackathonFinishedEvent | Transaction commit | Listener tính chapter + individual rankings | N/A | AFTER_COMMIT calculateAsync | Implemented | hackathons/listener/HackathonFinishedEventListener.java |
| BR-CLOSE-012 | FR-33 | Closure | Gap | Batch notification RESULT_PUBLISHED chưa làm | Sau finished event | TODO trong listener | N/A | Code comment TODO | Gap | hackathons/listener/HackathonFinishedEventListener.java |

## Prize

| Rule ID | Related Req ID | Module | Rule Type | Business Rule Statement | Condition / Trigger | System Action / Expected Result | Exception / Error Message | Test Case / Example Data | Status | Evidence Link / Note |
|---------|-----------------|--------|-----------|-------------------------|---------------------|---------------------------------|---------------------------|--------------------------|--------|----------------------|
| BR-PRIZE-001 | FR-32 | Prize | Guard | FINISHED không trao giải mới | status=FINISHED khi award | Conflict | HACKATHON_ARCHIVED | FINISHED + POST prize | Implemented | prizes/service/impl/PrizeServiceImpl.java |
| BR-PRIZE-002 | FR-32 | Prize | Guard | Chỉ trao giải khi PENDING_CONFIRM | status ≠ PENDING_CONFIRM (và không FINISHED) | Từ chối | HACKATHON_NOT_PENDING_CONFIRM | ONGOING + POST | Implemented | prizes/service/impl/PrizeServiceImpl.java |
| BR-PRIZE-003 | FR-32 | Prize | Guard | Round phải thuộc hackathon đang trao giải | round.hackathonId ≠ path hackathonId | Từ chối | CROSS_HACKATHON_VIOLATION | Round hackathon khác | Implemented | prizes/service/impl/PrizeServiceImpl.java |
| BR-PRIZE-004 | FR-32 | Prize | Guard | Team phải thuộc hackathon đang trao giải | team.hackathonId ≠ path hackathonId | Từ chối | CROSS_HACKATHON_VIOLATION | Team hackathon khác | Implemented | prizes/service/impl/PrizeServiceImpl.java |
| BR-PRIZE-005 | FR-32 | Prize | Guard | Nếu có trackId thì track phải thuộc round đã chọn | track.roundId ≠ req.roundId | Từ chối | CROSS_HACKATHON_VIOLATION | Track sai round | Implemented | prizes/service/impl/PrizeServiceImpl.java |
| BR-PRIZE-006 | FR-32 | Prize | Guard | Round còn tiebreak chưa resolve thì không trao giải | tiebreak(round) không rỗng | Từ chối | TIEBREAK_UNRESOLVED | DEEP_TIE còn mở | Implemented | prizes/service/impl/PrizeServiceImpl.java |
| BR-PRIZE-007 | FR-32 | Prize | Invariant | Một đội ≤ 1 giải chính trong hackathon (và theo round) | exists round+team hoặc hackathon+team | Conflict | PRIZE_DUPLICATE | Cùng team trao 2 lần | Implemented | prizes/service/impl/PrizeServiceImpl.java#assertNoDuplicate |
| BR-PRIZE-008 | FR-32 | Prize | Invariant | Một prizeRank ≤ 1 lần trong hackathon (và theo round) | prizeRank≠null và đã tồn tại | Conflict | PRIZE_DUPLICATE | Trùng FIRST/SECOND | Implemented | prizes/service/impl/PrizeServiceImpl.java#assertNoDuplicate |
| BR-PRIZE-009 | FR-36 | Prize | Audit | Trao giải ghi audit PRIZE_AWARDED | Sau save prize | log meta hackathon/round/team/rank | N/A | Audit sau POST | Implemented | prizes/service/impl/PrizeServiceImpl.java |
| BR-PRIZE-010 | FR-32 | Prize | Guard | FINISHED không sửa giải đã trao | status=FINISHED khi update | Conflict | HACKATHON_ARCHIVED | FINISHED + PATCH prize | Implemented | prizes/service/impl/PrizeServiceImpl.java |
| BR-PRIZE-011 | FR-32 | Prize | Guard | Đổi đội: team mới phải cùng hackathon | newTeam.hackathon ≠ prize.hackathon | Từ chối | CROSS_HACKATHON_VIOLATION | Đổi team sai HK | Implemented | prizes/service/impl/PrizeServiceImpl.java |
| BR-PRIZE-012 | FR-32 | Prize | Invariant | Đổi đội: đội mới chưa có giải chính trong hackathon | existsByHackathonIdAndTeamId(newTeam) | Conflict | PRIZE_DUPLICATE | Reassign vào đội đã có giải | Implemented | prizes/service/impl/PrizeServiceImpl.java |
| BR-PRIZE-013 | FR-36 | Prize | Audit | Sửa giải ghi PRIZE_AWARD_UPDATED (reason, old/new team) | Sau update | audit log | N/A | PATCH prize | Implemented | prizes/service/impl/PrizeServiceImpl.java |
| BR-PRIZE-014 | FR-32 | Prize | Guard | Chỉ xem danh sách giải khi PENDING_CONFIRM hoặc FINISHED | status khác hai trạng thái trên | Từ chối | INVALID_STATE | ONGOING GET prizes | Implemented | prizes/service/impl/PrizeServiceImpl.java |
| BR-PRIZE-015 | FR-32 | Prize | Guard | Thu hồi bắt buộc category + note không rỗng | Thiếu category hoặc note | Từ chối | PRIZE_REVOKE_REASON_REQUIRED | DELETE body thiếu | Implemented | prizes/service/impl/PrizeServiceImpl.java |
| BR-PRIZE-016 | FR-32 | Prize | Guard | Category thu hồi chỉ AWARDED_IN_ERROR\|TEAM_DQ\|DUPLICATE_AWARD\|OTHER | Category ngoài allowlist | Từ chối | PRIZE_REVOKE_CATEGORY_INVALID | Category lạ | Implemented | prizes/service/impl/PrizeServiceImpl.java |
| BR-PRIZE-017 | FR-32 | Prize | Guard | FINISHED không thu hồi giải | status=FINISHED khi revoke | Conflict | HACKATHON_ARCHIVED | Profile C DELETE | Implemented | prizes/service/impl/PrizeServiceImpl.java |
| BR-PRIZE-018 | FR-36 | Prize | Audit | Thu hồi ghi PRIZE_REVOKED kèm revokeCategory + revokeNote | Sau delete prize | audit append-only | N/A | Revoke có note | Implemented | prizes/service/impl/PrizeServiceImpl.java |
| BR-PRIZE-019 | FR-32 | Prize | Access | API prizes/closure/export chỉ COORDINATOR | Gọi endpoint @CoordinatorOnly | 403 nếu không phải Coord | FORBIDDEN | Role STUDENT gọi | Implemented | prizes/controller/*; hackathons/controller/HackathonClosureController.java; export_jobs/controller/ExportJobController.java |

## Ranking

| Rule ID | Related Req ID | Module | Rule Type | Business Rule Statement | Condition / Trigger | System Action / Expected Result | Exception / Error Message | Test Case / Example Data | Status | Evidence Link / Note |
|---------|-----------------|--------|-----------|-------------------------|---------------------|---------------------------------|---------------------------|--------------------------|--------|----------------------|
| BR-RANK-001 | FR-31 | Ranking | Policy | XH team CK không gate theo status hackathon | GET team-rankings mọi status | Trả ranking CK hoặc [] | N/A | ONGOING vẫn 200 nếu đã chấm | Implemented | hackathons/query/FinalRankingQueryServiceImpl.java; HackathonClosureServiceImpl.java |
| BR-RANK-002 | FR-31 | Ranking | Invariant | Không có round FINAL → ranking rỗng | findByIsFinalTrue empty | return [] | N/A | Hackathon thiếu CK | Implemented | hackathons/query/FinalRankingQueryServiceImpl.java |
| BR-RANK-003 | FR-31 | Ranking | Calculation | Xếp hạng theo totalScore giảm dần, tie-break teamId tăng | Có ranking CK | Gán rank tuần tự 1..n | N/A | 3 team t1>t2>t3 | Implemented | hackathons/query/FinalRankingQueryServiceImpl.java |
| BR-RANK-004 | FR-31 | Ranking | Calculation | judgeCount = số judge distinct score NORMAL trên submission CK | Có submission CK | Đếm judge NORMAL | N/A | Kiểm tra judgeCount | Implemented | hackathons/query/FinalRankingQueryServiceImpl.java |
| BR-RANK-005 | FR-31 | Ranking | Invariant | Gắn chapterId/chapterName từ team.chapter nếu có | Team có chapter | Điền meta chapter vào item | N/A | Team có/không chapter | Implemented | hackathons/query/FinalRankingQueryServiceImpl.java |
| BR-RANK-006 | FR-33 | Ranking | Guard | XH Chapter chỉ công bố khi PENDING_CONFIRM hoặc FINISHED | status khác hai trạng thái | Từ chối | INVALID_STATE | ONGOING GET chapter-rankings | Implemented | chapters/service/impl/ChapterRankingServiceImpl.java |
| BR-RANK-007 | FR-33 | Ranking | Lifecycle | Tính & persist chapter ranking sau confirm (async AFTER_COMMIT) | HackathonFinishedEvent | delete cũ + tính lại + save | N/A | Sau confirm có rows | Implemented | HackathonFinishedEventListener.java; ChapterRankingServiceImpl.java |
| BR-RANK-008 | FR-33 | Ranking | Invariant | Không có team ranking CK → bỏ qua persist | teamRankings empty | return sớm, không rows | N/A | Confirm khi CK trống điểm | Implemented | chapters/service/impl/ChapterRankingServiceImpl.java |
| BR-RANK-009 | FR-33 | Ranking | Invariant | Team không có chapterId không góp vào XH chapter | item.chapterId=null | Bỏ qua team đó | N/A | Team không gắn chapter | Implemented | chapters/service/impl/ChapterRankingServiceImpl.java |
| BR-RANK-010 | FR-33 | Ranking | Calculation | Công thức mặc định SUM điểm CK các team theo chapter; bestTeamScore=max; prizesWon đếm prize của team thuộc chapter | calculateAsync | Sort totalScore↓, bestTeamScore↓, chapterId↑ rồi gán rank | N/A | So khớp totalScore | Implemented | chapters/service/impl/ChapterRankingServiceImpl.java |
| BR-RANK-011 | FR-33 | Ranking | Invariant | Lưu formulaSnapshot từ hackathon.chapterScoringFormula hoặc DEFAULT SUM_TEAM_FINAL_SCORES v1 | Khi persist | Ghi snapshot vào row | N/A | Kiểm tra formulaSnapshot | Implemented | chapters/service/impl/ChapterRankingServiceImpl.java |
| BR-RANK-012 | FR-33 | Ranking | Policy | GET chapter trước calculateAsync có thể trả [] dù PENDING_CONFIRM | Chưa chạy async | 200 [] | N/A | Profile B trước event | Implemented | chapters/service/impl/ChapterRankingServiceImpl.java |
| BR-RANK-013 | FR-33 | Ranking | Guard | XH cá nhân chỉ khi individual_ranking_enabled=true | cờ ≠ true | Từ chối | INVALID_STATE | cờ false | Implemented | individual_rankings/service/impl/IndividualRankingServiceImpl.java |
| BR-RANK-014 | FR-33 | Ranking | Guard | XH cá nhân chỉ công bố khi FINISHED | status ≠ FINISHED | Từ chối | INVALID_STATE | PENDING_CONFIRM GET individual | Implemented | individual_rankings/service/impl/IndividualRankingServiceImpl.java |
| BR-RANK-015 | FR-33 | Ranking | Lifecycle | calculateAsync bỏ qua nếu cờ tắt | individualRankingEnabled≠true | log + return | N/A | Confirm với cờ false | Implemented | individual_rankings/service/impl/IndividualRankingServiceImpl.java |
| BR-RANK-016 | FR-33 | Ranking | Calculation | Điểm SV = tổng (điểm team CK / số member ACCEPTED) trên các team tham gia CK | calculateAsync | Chỉ member ACCEPTED; chia đều share | N/A | Team 2 SV điểm chia đôi | Implemented | individual_rankings/service/impl/IndividualRankingServiceImpl.java |
| BR-RANK-017 | FR-33 | Ranking | Calculation | Rank theo score giảm dần, tie-break userId tăng; cumulativeScore hiện = scoreThisHackathon | Sau aggregate | Persist rank 1..n | N/A | Hai user cùng điểm | Implemented | individual_rankings/service/impl/IndividualRankingServiceImpl.java |

## Export

| Rule ID | Related Req ID | Module | Rule Type | Business Rule Statement | Condition / Trigger | System Action / Expected Result | Exception / Error Message | Test Case / Example Data | Status | Evidence Link / Note |
|---------|-----------------|--------|-----------|-------------------------|---------------------|---------------------------------|---------------------------|--------------------------|--------|----------------------|
| BR-EXPORT-001 | FR-34 | Export | Guard | Chỉ tạo export khi hackathon FINISHED | status ≠ FINISHED | Từ chối | INVALID_STATE | PENDING_CONFIRM POST export | Implemented | export_jobs/service/impl/ExportJobServiceImpl.java |
| BR-EXPORT-002 | FR-34 | Export | Lifecycle | Tạo job PENDING → build CSV → storage → DONE ngay (đồng bộ) | create() thành công | status DONE + fileUrl + finishedAt | N/A | POST → DONE không PENDING lâu | Implemented | export_jobs/service/impl/ExportJobServiceImpl.java |
| BR-EXPORT-003 | FR-36 | Export | Audit | Tạo export ghi EXPORT_JOB_CREATED | Sau DONE | audit type + storageKey | N/A | Audit sau create | Implemented | export_jobs/service/impl/ExportJobServiceImpl.java |
| BR-EXPORT-004 | FR-34 | Export | Guard | Download chỉ khi job DONE | status ≠ DONE | Từ chối | INVALID_STATE | (không dùng EXPORT_JOB_NOT_READY) | Implemented | export_jobs/service/impl/ExportJobServiceImpl.java |
| BR-EXPORT-005 | FR-34 | Export | Guard | fileUrl null/blank → không download | fileUrl thiếu | Từ chối | INVALID_STATE | Job DONE nhưng thiếu URL | Implemented | export_jobs/service/impl/ExportJobServiceImpl.java |
| BR-EXPORT-006 | FR-34 | Export | Guard | Object storage phải còn file | !objectStorage.exists | Từ chối | INVALID_STATE | Xóa file storage tay | Implemented | export_jobs/service/impl/ExportJobServiceImpl.java |
| BR-EXPORT-007 | FR-36 | Export | Audit | Download ghi EXPORT_FILE_DOWNLOADED | Sau kiểm tra file OK | audit download | N/A | GET download | Implemented | export_jobs/service/impl/ExportJobServiceImpl.java |
| BR-EXPORT-008 | FR-34 | Export | Calculation | CSV_RANKINGS: ranking mọi round + TEAM_OTHER cho team chưa có rank; BOM UTF-8 | type=CSV_RANKINGS | Xuất CSV rankings | N/A | So header section=ROUND_RANKING | Implemented | export_jobs/support/ExportCsvBuilder.java |
| BR-EXPORT-009 | FR-35 | Export | Policy | ANONYMIZED_RBL: điểm NORMAL+PENALTY (loại CALIBRATION); ẩn danh judge; gắn judge_type FACULTY/GUEST/OTHER | type=ANONYMIZED_RBL | CSV long-format RBL | N/A | Không lộ judge_id thô | Implemented | export_jobs/support/ExportCsvBuilder.java; rbl/support/RblJudgeAnonymizer.java |
| BR-EXPORT-010 | FR-34 | Export | Calculation | FULL_REPORT gộp rankings + scores anonymized + RBL long + variance aggregate | type=FULL_REPORT | CSV multi-section | N/A | Có SECTION markers | Implemented | export_jobs/support/ExportCsvBuilder.java |
| BR-EXPORT-011 | FR-34 | Export | Policy | List export jobs theo hackathon không kiểm tra FINISHED | GET list | Trả jobs hoặc 404 hackathon | N/A | List khi ONGOING | Implemented | export_jobs/service/impl/ExportJobServiceImpl.java |

## Rbl

| Rule ID | Related Req ID | Module | Rule Type | Business Rule Statement | Condition / Trigger | System Action / Expected Result | Exception / Error Message | Test Case / Example Data | Status | Evidence Link / Note |
|---------|-----------------|--------|-----------|-------------------------|---------------------|---------------------------------|---------------------------|--------------------------|--------|----------------------|
| BR-RBL-001 | FR-35 | Rbl | Access | API variance/progress RBL chỉ COORDINATOR | Gọi /rbl rounds endpoints | @CoordinatorOnly | FORBIDDEN | STUDENT gọi variance | Implemented | rbl/controller/RblDashboardController.java |
| BR-RBL-002 | FR-35 | Rbl | Policy | Variance không lộ judgeId thô — chỉ pseudonym J{hash36} ổn định theo (hackathonId,judgeId) | varianceByRound | anonymizedJudgeId | N/A | Cùng judge cùng hash trong 1 HK | Implemented | rbl/service/impl/RblDashboardServiceImpl.java; rbl/support/RblJudgeAnonymizer.java |
| BR-RBL-003 | FR-35 | Rbl | Calculation | Spread/IRR loại criterion type PENALTY; chỉ score NORMAL | Query variance | Loại PENALTY | N/A | PENALTY không vào spread | Implemented | rbl/service/impl/RblDashboardServiceImpl.java |
| BR-RBL-004 | FR-35 | Rbl | Calculation | Inter-rater STDDEV chỉ submission có ≥2 judge distinct | HAVING COUNT DISTINCT judge ≥2 | Aggregate mean_inter_rater_std | N/A | 1 judge → không vào IRR | Implemented | rbl/service/impl/RblDashboardServiceImpl.java |
| BR-RBL-005 | FR-35 | Rbl | Invariant | GUEST=JUDGE&(temp\|EXTERNAL); FACULTY=JUDGE&INTERNAL&!temp; còn lại OTHER | Export/RBL classify | Gắn judge_type | N/A | Temp account = GUEST | Implemented | rbl/support/JudgeResearchTypeResolver.java |
| BR-RBL-006 | N/A | Rbl | Calculation | completionPct = scoredSubmissions(NORMAL)/totalSubmissions *100 (round 2) | scoringProgress | Trả % | N/A | 0 submission → 0% | Implemented | rbl/service/impl/RblDashboardServiceImpl.java |

## Announcement

| Rule ID | Related Req ID | Module | Rule Type | Business Rule Statement | Condition / Trigger | System Action / Expected Result | Exception / Error Message | Test Case / Example Data | Status | Evidence Link / Note |
|---------|-----------------|--------|-----------|-------------------------|---------------------|---------------------------------|---------------------------|--------------------------|--------|----------------------|
| BR-ANN-001 | FR-33 | Announcement | SideEffect | publishResults lưu softHidden=false + publish STOMP + audit ANNOUNCEMENT_PUBLISHED | Gọi từ confirm | Persist + live event | N/A | Subscribe announcements | Implemented | announcements/service/AnnouncementService.java |
| BR-ANN-002 | N/A | Announcement | Policy | Feed chỉ hiện announcement softHidden=false | listVisible/feed | Lọc soft hidden | N/A | softHide rồi list | Implemented | announcements/service/AnnouncementService.java |
| BR-ANN-003 | N/A | Announcement | Invariant | unreadCount = số announcement tạo sau lastViewedAt (hoặc tất cả nếu chưa xem) | feedForCurrentUser | Tính unread + lastViewedAt | N/A | markViewed rồi feed | Implemented | announcements/service/AnnouncementService.java |
| BR-ANN-004 | N/A | Announcement | Policy | Coord có thể soft-hide; ghi audit ANNOUNCEMENT_SOFT_HIDE | softHide(id, hidden) | Cập nhật softHidden | N/A | Hide rồi listVisible rỗng item đó | Implemented | announcements/service/AnnouncementService.java |

## LiveScoring

| Rule ID | Related Req ID | Module | Rule Type | Business Rule Statement | Condition / Trigger | System Action / Expected Result | Exception / Error Message | Test Case / Example Data | Status | Evidence Link / Note |
|---------|-----------------|--------|-----------|-------------------------|---------------------|---------------------------------|---------------------------|--------------------------|--------|----------------------|
| BR-LIVE-001 | N/A | LiveScoring | Access | STOMP CONNECT bắt buộc Bearer JWT (trừ jwt.enabled=false stub Coord) | CONNECT thiếu token | IllegalArgumentException | N/A | WS không token | Implemented | live_scoring/security/StompJwtChannelInterceptor.java |
| BR-LIVE-002 | N/A | LiveScoring | Access | SUBSCRIBE phải có SealAuthentication | User chưa auth | AccessDeniedException | N/A | Subscribe anonymous | Implemented | live_scoring/security/StompSubscribeAuthorizationInterceptor.java |
| BR-LIVE-003 | N/A | LiveScoring | Access | Topic round leaderboard/scoring/presentation-queue: Coord hoặc judge được gán round | canAccessRound false | AccessDenied | N/A | Judge ngoài round | Implemented | live_scoring/security/StompSubscribeAuthorizationInterceptor.java |
| BR-LIVE-004 | N/A | LiveScoring | Access | Topic tracks/{id}/score-saved: Coord hoặc judge được gán track | canAccessTrack false | AccessDenied | N/A | Judge ngoài track | Implemented | live_scoring/security/StompSubscribeAuthorizationInterceptor.java |
| BR-LIVE-005 | N/A | LiveScoring | Access | Presentation queue: Coord \| judge track/round \| presentation controller | Không đủ quyền | AccessDenied | N/A | User thường subscribe queue | Implemented | live_scoring/security/StompSubscribeAuthorizationInterceptor.java |
| BR-LIVE-006 | FR-33 | LiveScoring | Access | Topic hackathons/{id}/announcements: mọi user đã auth (approved) được subscribe | SUBSCRIBE announcements | Cho phép nếu đã auth | N/A | Student subscribe sau confirm | Implemented | live_scoring/security/StompSubscribeAuthorizationInterceptor.java |

## Archive

| Rule ID | Related Req ID | Module | Rule Type | Business Rule Statement | Condition / Trigger | System Action / Expected Result | Exception / Error Message | Test Case / Example Data | Status | Evidence Link / Note |
|---------|-----------------|--------|-----------|-------------------------|---------------------|---------------------------------|---------------------------|--------------------------|--------|----------------------|
| BR-ARCH-001 | N/A | Archive | Guard | FINISHED chặn mọi mutation qua HackathonArchiveGuard (hackathon/round/track/criteria) | assertNotArchived* khi FINISHED | Conflict | HACKATHON_ARCHIVED | Sửa round khi FINISHED | Implemented | hackathons/support/HackathonArchiveGuard.java |

## Common

| Rule ID | Related Req ID | Module | Rule Type | Business Rule Statement | Condition / Trigger | System Action / Expected Result | Exception / Error Message | Test Case / Example Data | Status | Evidence Link / Note |
|---------|-----------------|--------|-----------|-------------------------|---------------------|---------------------------------|---------------------------|--------------------------|--------|----------------------|
| BR-COMMON-001 | N/A | Common | Validation | Optimistic lock / concurrent update conflict | JPA version conflict hoặc handler map | 409/422 Conflict | CONCURRENT_MODIFICATION | Two Coord lock-scoring race | Implemented | common/exception (GlobalExceptionHandler / OptimisticLock) |
| BR-COMMON-002 | N/A | Common | Validation | Lỗi nội bộ không map business | Unhandled exception | 500 | INTERNAL_ERROR | Force NPE in handler test | Implemented | common/exception/GlobalExceptionHandler.java |
| BR-COMMON-003 | N/A | Common | Gap | Endpoint/logic chưa implement trả NOT_IMPLEMENTED | Gọi API stub | 501/422 | NOT_IMPLEMENTED | Legacy stub endpoints | Partial | Reserved ErrorCode; sparse usage |

## RoundAccess

| Rule ID | Related Req ID | Module | Rule Type | Business Rule Statement | Condition / Trigger | System Action / Expected Result | Exception / Error Message | Test Case / Example Data | Status | Evidence Link / Note |
|---------|-----------------|--------|-----------|-------------------------|---------------------|---------------------------------|---------------------------|--------------------------|--------|----------------------|
| BR-ROUNDA-001 | N/A | RoundAccess | Guard | requireActiveRound: isActive≠true → ROUND_NOT_ACTIVE (chưa kích hoạt / đã kết thúc) | Round inactive | Từ chối | ROUND_NOT_ACTIVE | Submit khi inactive | Implemented | rounds/guard/RoundAccessGuard.java |
| BR-ROUNDA-002 | N/A | RoundAccess | Guard | requireUnlockedRound: scoringLocked → ScoringLockedException | Round đã lock | Từ chối (scoring locked) | SCORING_LOCKED (exception type) | Chấm khi đã lock | Implemented | rounds/guard/RoundAccessGuard.java |
| BR-ROUNDA-003 | N/A | RoundAccess | Invariant | requireActiveRoundForUpdate dùng PESSIMISTIC_WRITE chống Lost Update | close-early/lock-scoring song song | Khóa row round | N/A | 2 Coord lock song song | Implemented | rounds/guard/RoundAccessGuard.java |

## Appendix A — ErrorCode orphan / unused

### A.1 Declared in `ErrorCode.java` nhưng không thấy reference `ErrorCode.*` trong `src/main/java` (35)

| ErrorCode | Ghi chú |
|-----------|---------|
| `AWARDS_BEFORE_COMPETITION_END` | Có thể chỉ dùng string literal / chưa wire / reserved |
| `AWARDS_NEEDS_COMPETITION` | Có thể chỉ dùng string literal / chưa wire / reserved |
| `CRITERIA_HAS_CLONE_DEPENDENTS` | Có thể chỉ dùng string literal / chưa wire / reserved |
| `CRITERIA_WEIGHT_RANGE` | Có thể chỉ dùng string literal / chưa wire / reserved |
| `DEPT_HEAD_NOT_CONFIRMED` | PATCH isDeptHead đã ngừng (INVALID_ASSIGNMENT_TYPE) |
| `ELIMINATION_REASON_REQUIRED` | Có thể chỉ dùng string literal / chưa wire / reserved |
| `EXPORT_JOB_NOT_READY` | Download dùng INVALID_STATE thay thế |
| `INVALID_MENTOR_FOR_TEAM` | Có thể chỉ dùng string literal / chưa wire / reserved |
| `INVITATION_NOT_FOUND` | Có thể chỉ dùng string literal / chưa wire / reserved |
| `INVITATION_PENDING_EXISTS` | Có thể chỉ dùng string literal / chưa wire / reserved |
| `INVITATION_REQUIRED` | Có thể chỉ dùng string literal / chưa wire / reserved |
| `JUDGE_OFFLINE` | Có thể chỉ dùng string literal / chưa wire / reserved |
| `MENTOR_TEAM_CROSS_HACKATHON` | Có thể chỉ dùng string literal / chưa wire / reserved |
| `PRESENTATION_BEFORE_FINAL_EXAM` | Có thể chỉ dùng string literal / chưa wire / reserved |
| `PRIZE_CATALOG_LOCKED` | Reserved — chưa thấy throw |
| `ROUND_EXAM_OUTSIDE_AWARDS` | Có thể chỉ dùng string literal / chưa wire / reserved |
| `ROUND_EXAM_OUTSIDE_PRESENTATION` | Có thể chỉ dùng string literal / chưa wire / reserved |
| `ROUND_HAS_ACTIVE_STATE` | Có thể chỉ dùng string literal / chưa wire / reserved |
| `ROUND_HAS_SCORES` | Docs FR-13C yêu cầu khi removeMentor — code chưa enforce (Gap) |
| `TEAM_ALREADY_PARTICIPATES_IN_ROUND` | Có thể chỉ dùng string literal / chưa wire / reserved |
| `TEAM_LEADER_NOT_APPROVED` | createTeam không check APPROVED (Gap vs docs) |
| `TEAM_NOT_ADVANCING` | Có thể chỉ dùng string literal / chưa wire / reserved |
| `TEAM_NOT_READY` | Có thể chỉ dùng string literal / chưa wire / reserved |
| `TEAM_ROUND_PARTICIPATION_MISSING` | Có thể chỉ dùng string literal / chưa wire / reserved |
| `TOKEN_EXPIRED` | Có thể chỉ dùng string literal / chưa wire / reserved |
| `TOKEN_INVALID` | Có thể chỉ dùng string literal / chưa wire / reserved |
| `TOKEN_USED` | Có thể chỉ dùng string literal / chưa wire / reserved |
| `TRACK_DELETE_HAS_SUBMISSIONS` | Có thể chỉ dùng string literal / chưa wire / reserved |
| `TRACK_GROUP_FULL` | Có thể chỉ dùng string literal / chưa wire / reserved |
| `TRACK_HAS_CRITERIA` | Có thể chỉ dùng string literal / chưa wire / reserved |
| `TRACK_NOT_ALLOWED_FOR_FINAL` | Có thể chỉ dùng string literal / chưa wire / reserved |
| `TRACK_NOT_CANCELLED` | Có thể chỉ dùng string literal / chưa wire / reserved |
| `TRACK_PARENT_ROUND_ACTIVE` | Có thể chỉ dùng string literal / chưa wire / reserved |
| `TRACK_SEQUENCE_DUPLICATE` | Có thể chỉ dùng string literal / chưa wire / reserved |
| `USER_TYPE_LOCKED` | Có thể chỉ dùng string literal / chưa wire / reserved |

### A.2 Có reference trong src nhưng chưa gắn rule riêng trong catalog (0)

_Không có (hoặc đã cover qua rule khác)._

### A.3 Mã trong catalog không phải constant `ErrorCode` (1)

`RESULT_NOT_AVAILABLE`

_Thường là string literal runtime / warning codes._

## Appendix B — FR collisions & doc drift

| Issue | Chi tiết |
|-------|----------|
| FR-07 collision | Backlog dùng `FR-07` cho **Hackathon status** (GĐ1) và **Auth register/login** (GĐ2). Catalog gắn theo ngữ cảnh module. |
| FR-05A vs FR-05a | Guest judge / temp account — dùng `FR-05A`. |
| mf02 BR doc stale | `mf02/01-business-rules-gd2.md` vẫn ghi TODO/501 — **code đã implement** (Status=Implemented). |
| Student PENDING login | Code cho phép STUDENT PENDING login sau verify email; docs auth cũ có thể nói chỉ APPROVED. |
| createTeam gaps | Student create không bắt buộc đã register / APPROVED / prelim inactive (admin path có `ROUND_ALREADY_ACTIVE`). |
| removeMentor | Docs yêu cầu `ROUND_HAS_SCORES` — code chưa check → Status Gap trên rule liên quan. |
| Team rankings status gate | Docs: chỉ PENDING_CONFIRM+; **code** `FinalRankingQueryServiceImpl` không gate status. |
| EXPORT_JOB_NOT_READY | Constant tồn tại; download dùng `INVALID_STATE`. |
| Mentor chapter AVG vs official SUM | Mentor portal AVG; `ChapterRankingServiceImpl` SUM. |
| RESULT_PUBLISHED batch notify | TODO trong `HackathonFinishedEventListener` (announcement STOMP đã có). |

## Appendix C — Module → Java package

| Module | Package / entry |
|--------|-----------------|
| Auth | `auth.service.*` |
| UserAdmin | `users.service.impl.UserAdminServiceImpl` |
| Hackathon / Status / Readiness / Registration | `hackathons.service.impl.*` |
| Round / Activate / Progression | `rounds.service.impl.*` |
| Track | `tracks.service.impl` + `tracks.support.TrackRoundRules` |
| Criteria | `criteria.service.impl.*` |
| JudgeAssign / MentorAssign | `judge_assignments` / `mentors` + `PersonnelAssignmentRules` |
| Event | `events.service.impl.*` |
| Team / Lottery / Lock | `teams.service.impl.*` / `HackathonLotteryServiceImpl` |
| Submission / Score | `submissions.*` / `scores.*` |
| Presentation | `presentation.service.impl.*` |
| Appeal | `appeals.service.impl.*` + `rounds.support.RoundScheduleShiftService#delayFinalForAppeals` + `teams.service.impl.TeamReinstatementServiceImpl` |
| Closure / Prize / Ranking / Export | `HackathonClosureServiceImpl` / `prizes` / rankings / `export_jobs` |
| RBL / Calibration | `rbl.service` / `rbl.calibration` |
| Portals | `me.student` / `me.judge` / `me.mentor` |

---

_Regenerate extracts: cập nhật TSV trong `docs/_br_extract/` rồi chạy `node docs/_br_extract/build-catalog.mjs`._
