package com.sealhackathon.api.rounds.value_object;

/**
 * Lịch thi khi Activate / dời lịch — Coord chọn rõ; mặc định {@link #KEEP}.
 */
public enum ActivateScheduleMode {
    /** Chỉ kích hoạt môi trường; giữ examAt / deadline (API ẩn / không hiện modal UI). */
    KEEP,
    /** Bắt đầu thi sớm — examAt = now + setupLeadMinutes (không ceil), mặc định lead 5. */
    START_NOW,
    /** Chỉ dời examAt sang {@code newExamAt}; giữ isActive = false (không kích hoạt). */
    RESCHEDULE
}
