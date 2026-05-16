package com.se194093.be.hackathons.service;

import com.se194093.be.hackathons.dto.response.HackathonReadinessResponse;
import com.se194093.be.hackathons.value_object.HackathonStatus;

/**
 * FR-06 — Tổng hợp kiểm tra readiness của Hackathon trước khi chuyển status.
 *
 * <p>Dùng cho cả 2 mục đích:
 * <ol>
 *   <li><b>GET {@code /hackathons/{id}/readiness}</b> — dry-run, KHÔNG block; trả full response.</li>
 *   <li><b>PATCH {@code /hackathons/{id}/status}</b> — Gate cứng; nếu {@code !ready} thì service
 *       throws {@code BusinessRuleException(READINESS_NOT_PASSED)} kèm blockers trong details.</li>
 * </ol>
 *
 * <p>Mở rộng tương lai: hỗ trợ target {@code PENDING_CONFIRM}, {@code FINISHED} cho GĐ5/GĐ6.
 * MF-01 chỉ implement {@code DRAFT → ONGOING}.
 */
public interface HackathonReadinessService {

    /**
     * Chạy mọi rule readiness cho 1 transition đích.
     *
     * <p>Mặc định target = {@link HackathonStatus#ONGOING} nếu null.
     *
     * @param hackathonId id Hackathon
     * @param target      target status muốn check; null → mặc định ONGOING
     * @return response đầy đủ {@code ready, blockers, warnings, summary}
     */
    HackathonReadinessResponse check(Integer hackathonId, HackathonStatus target);
}
