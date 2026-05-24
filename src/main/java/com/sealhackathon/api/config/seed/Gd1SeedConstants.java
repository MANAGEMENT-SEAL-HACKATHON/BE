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
 *   <li>{@link #SLUG_ONGOING} — ONGOING, prelim active; có Track 3 (chưa criteria) để test clone 2→3</li>
 * </ul>
 *
 * <h3>Users (email)</h3>
 * <ul>
 *   <li>{@link #EMAIL_COORDINATOR} — id=1 trên DB trống; khớp {@code StubCurrentUserAccessor}</li>
 *   <li>{@link #EMAIL_JUDGE1}, {@link #EMAIL_JUDGE2} — INTERNAL judges</li>
 *   <li>{@link #EMAIL_GUEST_JUDGE} — EXTERNAL temp judge</li>
 *   <li>{@link #EMAIL_MENTOR} — mentor</li>
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
    public static final String SLUG_ONGOING = "seal-spring-2026";

    public static final String EMAIL_COORDINATOR = "coord@fpt.edu.vn";
    public static final String EMAIL_JUDGE1 = "judge1@fpt.edu.vn";
    public static final String EMAIL_JUDGE2 = "judge2@fpt.edu.vn";
    public static final String EMAIL_GUEST_JUDGE = "guestjudge@gmail.com";
    public static final String EMAIL_MENTOR = "mentor@fpt.edu.vn";
    public static final String EMAIL_PENDING_JUDGE = "pending.judge@fpt.edu.vn";

    private static final String[] SEED_EMAILS = {
            EMAIL_COORDINATOR,
            EMAIL_JUDGE1,
            EMAIL_JUDGE2,
            EMAIL_GUEST_JUDGE,
            EMAIL_MENTOR,
            EMAIL_PENDING_JUDGE
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
        if (EMAIL_JUDGE1.equalsIgnoreCase(normalized) || EMAIL_JUDGE2.equalsIgnoreCase(normalized)) {
            return DEV_JUDGE_PASSWORD;
        }
        if (EMAIL_GUEST_JUDGE.equalsIgnoreCase(normalized)) {
            return DEV_GUEST_JUDGE_PASSWORD;
        }
        if (EMAIL_MENTOR.equalsIgnoreCase(normalized)) {
            return DEV_MENTOR_PASSWORD;
        }
        if (EMAIL_PENDING_JUDGE.equalsIgnoreCase(normalized)) {
            return DEV_PENDING_JUDGE_PASSWORD;
        }
        return null;
    }

    public static String[] seedEmails() {
        return SEED_EMAILS.clone();
    }

    public static final String CHAPTER_FPT_HCM = "FPT-HCM";
    public static final String CHAPTER_FPT_HN = "FPT-HN";
    public static final String CHAPTER_EXT = "EXT";

    /** Track 3 trên {@link #SLUG_ONGOING} — không seed criteria (clone từ Track 2 qua API). */
    public static final String TRACK3_CLONE_DEMO_NAME = "Track 3 — EV & Integration";
}
