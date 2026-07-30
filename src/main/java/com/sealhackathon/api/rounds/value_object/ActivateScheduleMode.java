package com.sealhackathon.api.rounds.value_object;

/**
 * Lịch thi khi Activate / dời lịch — Coord chọn rõ; mặc định {@link #KEEP}.
 * START_NOW (bắt đầu sớm) đã bị gỡ (phase 2) — client gửi giá trị đó sẽ lỗi parse enum.
 */
public enum ActivateScheduleMode {
    /** Chỉ kích hoạt môi trường; giữ examAt / deadline (API ẩn / không hiện modal UI). */
    KEEP,
    /** Chỉ dời examAt sang {@code newExamAt}; giữ isActive = false (không kích hoạt). */
    RESCHEDULE
}
