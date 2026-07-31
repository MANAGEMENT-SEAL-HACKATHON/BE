package com.sealhackathon.api.hackathons.value_object;

/**
 * Giai đoạn đăng ký dẫn xuất (không thay {@link HackathonStatus}).
 */
public enum RegistrationPhase {
    /** Chưa tới mốc mở đăng ký. */
    NOT_YET_OPEN,
    /** Cửa sổ đăng ký đang mở. */
    OPEN,
    /** Đã kết thúc đăng ký sớm. */
    CLOSED_EARLY,
    /** Đã hết hạn đăng ký (tự nhiên). */
    CLOSED
}
