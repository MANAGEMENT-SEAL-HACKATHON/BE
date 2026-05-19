package com.sealhackathon.api.hackathons.service;

import com.sealhackathon.api.hackathons.dto.request.ChangeHackathonStatusRequest;
import com.sealhackathon.api.hackathons.dto.response.HackathonResponse;

/**
 * FR-06 PATCH /api/v1/hackathons/{id}/status — state machine + Gate cứng.
 *
 * <p>Business rules:
 * <ul>
 *   <li>Transition tuyến tính 1 chiều: DRAFT → ONGOING → PENDING_CONFIRM → FINISHED.
 *       Sai chiều → 409 {@code STATUS_TRANSITION_INVALID}.</li>
 *   <li>Khi target = ONGOING: gọi {@link HackathonReadinessService#check} — nếu không ready,
 *       throw 422 {@code READINESS_NOT_PASSED} kèm blockers vào {@code details}.</li>
 *   <li>Sau khi đổi sang ONGOING: enqueue notification fan-out {@code HACKATHON_OPEN} cho
 *       mọi user APPROVED.</li>
 * </ul>
 *
 * <p>Audit: {@code HACKATHON_STATUS_CHANGE} snapshot {from, to, validatedAt, validatedBy, note}.
 */
public interface HackathonStatusService {

    HackathonResponse changeStatus(Integer hackathonId, ChangeHackathonStatusRequest req);
}
