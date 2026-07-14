package com.sealhackathon.api.rounds.value_object;

/**
 * Lịch thi khi Activate round — Coord chọn rõ; mặc định {@link #KEEP}.
 */
public enum ActivateScheduleMode {
    /** Chỉ kích hoạt môi trường; giữ examAt / deadline. */
    KEEP,
    /** Bắt đầu thi ngay — nén examAt = ceil(now). */
    START_NOW,
    /** Dời examAt sang {@code newExamAt} do Coord chọn. */
    RESCHEDULE
}
