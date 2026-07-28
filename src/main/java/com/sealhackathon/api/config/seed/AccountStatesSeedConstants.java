package com.sealhackathon.api.config.seed;

/**
 * Hằng số seed dev — ma trận trạng thái tài khoản (Module 5: xác thực email + duyệt tài khoản).
 *
 * <p>Bổ sung các case "happy → bad" mà seed hackathon chưa phủ:
 * <ul>
 *   <li>{@link #EMAIL_UNVERIFIED_STUDENT} — STUDENT, PENDING, email CHƯA verify → login trả
 *       {@code EMAIL_NOT_VERIFIED} (test gate xác thực + gửi lại email).</li>
 *   <li>{@link #EMAIL_PENDING_MENTOR} — MENTOR, PENDING, email đã verify → nằm trong hàng chờ
 *       "Duyệt tài khoản" của Coordinator (đóng góp badge todo).</li>
 *   <li>{@link #EMAIL_PENDING_JUDGE} — JUDGE, PENDING, email đã verify → hàng chờ duyệt.</li>
 *   <li>{@link #EMAIL_REJECTED_JUDGE} — JUDGE, REJECTED (có lý do) → login trả
 *       {@code ACCOUNT_REJECTED_NOT_ALLOWED_LOGIN} (bad case + hiển thị trên trang duyệt).</li>
 * </ul>
 *
 * <p>Các tài khoản này KHÔNG gắn với hackathon nào (không phải slug catalog); dùng chung mật khẩu
 * {@link #DEV_ACCOUNT_PASSWORD}. Seeder: {@link AccountStatesDataSeeder}.
 */
public final class AccountStatesSeedConstants {

    private AccountStatesSeedConstants() {
    }

    /** Mật khẩu dev dùng chung cho toàn bộ tài khoản trạng thái. */
    public static final String DEV_ACCOUNT_PASSWORD = "Account@dev1";

    public static final String EMAIL_UNVERIFIED_STUDENT = "account.student.unverified@fpt.edu.vn";
    public static final String EMAIL_PENDING_MENTOR = "account.mentor.pending@fpt.edu.vn";
    public static final String EMAIL_PENDING_JUDGE = "account.judge.pending@fpt.edu.vn";
    public static final String EMAIL_REJECTED_JUDGE = "account.judge.rejected@fpt.edu.vn";

    /** MENTOR APPROVED nhưng email chưa verify → login trả {@code EMAIL_NOT_VERIFIED} (cổng độc lập với duyệt). */
    public static final String EMAIL_APPROVED_UNVERIFIED_MENTOR =
            "account.mentor.approved-unverified@fpt.edu.vn";

    public static final String REJECTION_REASON =
            "Ảnh thẻ không rõ nét — vui lòng đăng ký lại với ảnh hợp lệ.";
}
