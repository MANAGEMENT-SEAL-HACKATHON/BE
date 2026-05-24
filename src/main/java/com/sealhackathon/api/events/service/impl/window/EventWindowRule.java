package com.sealhackathon.api.events.service.impl.window;

import com.sealhackathon.api.hackathons.entity.Hackathon;

import java.time.LocalDateTime;

/**
 * FR-06A Lớp 1 — kiểm tra khung ngày của một loại sự kiện so với mốc Hackathon
 * (registration / event window) và các sự kiện/round liên quan.
 *
 * <p>Mỗi loại milestone có rule riêng, đặt trong subpackage này để tránh gom logic
 * vào một method khổng lồ — vốn dễ gây sai timeline khi sửa.
 *
 * <ul>
 *   <li>{@code WorkshopWindowRule}: {@code regEnd < date < eventStart}</li>
 *   <li>{@code KickoffWindowRule}: {@code date == eventStart}</li>
 *   <li>{@code PresentationWindowRule}: {@code date ∈ [eventStart, eventEnd]} và sau Final.examAt</li>
 *   <li>{@code AwardsWindowRule}: {@code date == eventEnd} và sau mốc thi cuối</li>
 * </ul>
 */
public interface EventWindowRule {

    /**
     * Ném {@link com.sealhackathon.api.common.exception.BusinessRuleException} (422) nếu vi phạm.
     *
     * @param hackathon       Hackathon đích
     * @param startsAt        thời điểm bắt đầu
     * @param effectiveEnd    {@code endsAt} hoặc {@code startsAt} nếu null
     * @param excludeEventId  id event đang sửa (POST: 0)
     */
    void check(Hackathon hackathon, LocalDateTime startsAt, LocalDateTime effectiveEnd,
               Integer excludeEventId);
}
