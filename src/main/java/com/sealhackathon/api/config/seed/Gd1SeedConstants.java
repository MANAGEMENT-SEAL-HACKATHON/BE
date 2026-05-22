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
 */
public final class Gd1SeedConstants {

    private Gd1SeedConstants() {
    }

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

    public static final String CHAPTER_FPT_HCM = "FPT-HCM";
    public static final String CHAPTER_FPT_HN = "FPT-HN";
    public static final String CHAPTER_EXT = "EXT";

    /** Track 3 trên {@link #SLUG_ONGOING} — không seed criteria (clone từ Track 2 qua API). */
    public static final String TRACK3_CLONE_DEMO_NAME = "Track 3 — EV & Integration";
}
