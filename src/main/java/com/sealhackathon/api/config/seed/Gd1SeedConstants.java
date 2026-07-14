package com.sealhackathon.api.config.seed;

/**
 * Hằng số seed MF-01 Giai đoạn 1 — dùng trong Postman / dev smoke.
 *
 * <p>Sau khi start app (profile {@code dev}), xem log {@code [Gd1DataSeeder]} để lấy ID thực tế.
 *
 * <h3>Hackathon slugs</h3>
 * <ul>
 *   <li>{@link #SLUG_INCOMPLETE} — DRAFT, không round (readiness fail)</li>
 *   <li>{@link #SLUG_READY} — DRAFT, đủ G1–G5 (PATCH ONGOING)</li>
 *   <li>{@link #SLUG_ONGOING} — ONGOING, prelim active; có Track 3 (criteria + mentor/giám khảo)</li>
 *   <li>{@link #SLUG_FINISHED} — FINISHED, full seed (events/rounds/tracks/criteria), read-only archive</li>
 * </ul>
 *
 * <h3>Lịch seed (relative {@code LocalDate.now()})</h3>
 * <ul>
 *   <li>Đăng ký còn mở ~14 ngày; {@code eventStart = regEnd + 3} (đủ gap WORKSHOP + KICKOFF)</li>
 *   <li>WORKSHOP {@code regEnd+1} · KICKOFF {@code regEnd+2} · Thi ngày {@code eventStart} · AWARDS {@code eventEnd}</li>
 *   <li>CK: {@code codingDurationHours=2}; {@code examAt} ≈ 18:00; open/deadline theo 2/3 và full duration</li>
 *   <li>PDF đề seed: classpath {@code seed/HistAR_CP3Part2_EXE101.pdf} trên tracks Sơ loại + vòng CK</li>
 * </ul>
 *
 * <h3>Users (email)</h3>
 * <ul>
 *   <li>{@link #EMAIL_COORDINATOR} — id=1 trên DB trống; khớp {@code StubCurrentUserAccessor}</li>
 *   <li>{@link #EMAIL_JUDGE1}…{@link #EMAIL_JUDGE4} — INTERNAL judges (pool sơ loại HEAD/NORMAL)</li>
 *   <li>{@link #EMAIL_GUEST_JUDGE}…{@link #EMAIL_GUEST_JUDGE3} — EXTERNAL temp judges (CK FINAL_EXTERNAL)</li>
 *   <li>{@link #EMAIL_MENTOR}…{@link #EMAIL_MENTOR3} — mentors</li>
 *   <li>{@link #EMAIL_PENDING_JUDGE} — PENDING (FR-05 negative)</li>
 * </ul>
 *
 * <h3>Dev passwords (MF-02 login)</h3>
 * Xem log {@code [Gd1DataSeeder] Dev login credentials} khi start profile {@code dev}.
 */
public final class Gd1SeedConstants {

    private Gd1SeedConstants() {
    }

    /** Chỉ dev — đăng nhập JWT (profile dev). */
    public static final String DEV_COORDINATOR_PASSWORD = "Coordinator@dev1";
    public static final String DEV_JUDGE_PASSWORD = "Judge@dev1";
    public static final String DEV_GUEST_JUDGE_PASSWORD = "GuestJudge@dev1";
    public static final String DEV_MENTOR_PASSWORD = "Mentor@dev1";
    public static final String DEV_PENDING_JUDGE_PASSWORD = "PendingJudge@dev1";

    /** Legacy — DB cũ; {@link com.sealhackathon.api.config.seed.Gd1DataSeeder} sẽ repair sang bcrypt thật. */
    public static final String PASSWORD_PLACEHOLDER = "bcrypt-placeholder";

    public static final String SLUG_INCOMPLETE = "seal-gd1-incomplete";
    public static final String SLUG_READY = "seal-gd1-ready";
    /** Hackathon E2E GĐ1→GĐ6 — 7 đội + 3 orphan. */
    public static final String SLUG_ONGOING = "seal-e2e-2026";
    public static final String SLUG_FINISHED = "seal-fall-2025-finished";

    public static final String EMAIL_COORDINATOR = "coord@fpt.edu.vn";
    public static final String EMAIL_JUDGE1 = "judge1@fpt.edu.vn";
    public static final String EMAIL_JUDGE2 = "judge2@fpt.edu.vn";
    public static final String EMAIL_JUDGE3 = "judge3@fpt.edu.vn";
    public static final String EMAIL_JUDGE4 = "judge4@fpt.edu.vn";
    public static final String EMAIL_GUEST_JUDGE = "guestjudge@gmail.com";
    public static final String EMAIL_GUEST_JUDGE2 = "guestjudge2@gmail.com";
    public static final String EMAIL_GUEST_JUDGE3 = "guestjudge3@gmail.com";
    public static final String EMAIL_MENTOR = "mentor@fpt.edu.vn";
    public static final String EMAIL_MENTOR2 = "mentor2@fpt.edu.vn";
    public static final String EMAIL_MENTOR3 = "mentor3@fpt.edu.vn";
    public static final String EMAIL_PENDING_JUDGE = "pending.judge@fpt.edu.vn";

    /** Student archive Fall 2025 — FR-U-32 annual awards e2e. */
    public static final String EMAIL_ARCHIVE_STUDENT = "student.archive.fall2025@fpt.edu.vn";

    private static final String[] SEED_EMAILS = {
            EMAIL_COORDINATOR,
            EMAIL_JUDGE1,
            EMAIL_JUDGE2,
            EMAIL_JUDGE3,
            EMAIL_JUDGE4,
            EMAIL_GUEST_JUDGE,
            EMAIL_GUEST_JUDGE2,
            EMAIL_GUEST_JUDGE3,
            EMAIL_MENTOR,
            EMAIL_MENTOR2,
            EMAIL_MENTOR3,
            EMAIL_PENDING_JUDGE
    };

    private static final String[] INTERNAL_JUDGE_EMAILS = {
            EMAIL_JUDGE1, EMAIL_JUDGE2, EMAIL_JUDGE3, EMAIL_JUDGE4
    };

    private static final String[] GUEST_JUDGE_EMAILS = {
            EMAIL_GUEST_JUDGE, EMAIL_GUEST_JUDGE2, EMAIL_GUEST_JUDGE3
    };

    private static final String[] MENTOR_EMAILS = {
            EMAIL_MENTOR, EMAIL_MENTOR2, EMAIL_MENTOR3
    };

    /** Mật khẩu plaintext dev theo email seed; {@code null} nếu không phải user seed. */
    public static String devPasswordFor(String email) {
        if (email == null) {
            return null;
        }
        String normalized = email.trim().toLowerCase();
        if (EMAIL_COORDINATOR.equalsIgnoreCase(normalized)) {
            return DEV_COORDINATOR_PASSWORD;
        }
        for (String judgeEmail : INTERNAL_JUDGE_EMAILS) {
            if (judgeEmail.equalsIgnoreCase(normalized)) {
                return DEV_JUDGE_PASSWORD;
            }
        }
        for (String guestEmail : GUEST_JUDGE_EMAILS) {
            if (guestEmail.equalsIgnoreCase(normalized)) {
                return DEV_GUEST_JUDGE_PASSWORD;
            }
        }
        for (String mentorEmail : MENTOR_EMAILS) {
            if (mentorEmail.equalsIgnoreCase(normalized)) {
                return DEV_MENTOR_PASSWORD;
            }
        }
        if (EMAIL_PENDING_JUDGE.equalsIgnoreCase(normalized)) {
            return DEV_PENDING_JUDGE_PASSWORD;
        }
        return null;
    }

    public static String[] internalJudgeEmails() {
        return INTERNAL_JUDGE_EMAILS.clone();
    }

    public static String[] guestJudgeEmails() {
        return GUEST_JUDGE_EMAILS.clone();
    }

    public static String[] mentorEmails() {
        return MENTOR_EMAILS.clone();
    }

    public static String[] seedEmails() {
        return SEED_EMAILS.clone();
    }

    public static final String CHAPTER_FPT_HCM = "FPT-HCM";
    public static final String CHAPTER_FPT_HN = "FPT-HN";
    public static final String CHAPTER_EXT = "EXT";

    /** Track 3 trên {@link #SLUG_ONGOING} — EV & Integration, đủ criteria + mentor/giám khảo. */
    public static final String TRACK3_CLONE_DEMO_NAME = "Track 3 — EV & Integration";
}
