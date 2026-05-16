package com.se194093.be.rounds.service;

import com.se194093.be.rounds.dto.request.CreateRoundRequest;
import com.se194093.be.rounds.dto.request.UpdateRoundRequest;
import com.se194093.be.rounds.dto.response.RoundResponse;
import com.se194093.be.rounds.dto.response.RoundSummaryResponse;

import java.util.List;

/**
 * FR-03 — CRUD Round trong Track. Activate Round xem {@link RoundActivationService} (FR-06B).
 *
 * <p>Business rules:
 * <ul>
 *   <li><b>KHÔNG validate tổng weight Criteria</b> trong POST/PUT — Criteria chưa tồn tại.</li>
 *   <li>{@code submissionDeadline > submissionOpen} (nếu có) AND {@code submissionDeadline > NOW()} →
 *       422 {@code ROUND_DEADLINE_INVALID}.</li>
 *   <li>{@code forceLocked=true} thiếu {@code forceLockReason} → 422 {@code ROUND_FORCE_LOCK_REASON}.</li>
 *   <li>DELETE chỉ khi không có submission → 409 {@code ROUND_HAS_SUBMISSIONS}; và không is_active.</li>
 *   <li>{@code topNAdvance} sai logic (Round chung kết có top_n_advance hoặc Round không cuối thiếu) → warning.</li>
 * </ul>
 *
 * <p>Audit: {@code ROUND_CREATE}, {@code ROUND_UPDATE}, {@code ROUND_DELETE}, {@code ROUND_LOCK}, {@code ROUND_FORCE_LOCK}.
 */
public interface RoundService {

    RoundResponse create(Integer trackId, CreateRoundRequest req);

    /**
     * @return list kèm criteriaCount/currentWeightTotal cho UI realtime.
     */
    List<RoundSummaryResponse> listByTrack(Integer trackId);

    RoundResponse getById(Integer id);

    RoundResponse update(Integer id, UpdateRoundRequest req);

    Integer delete(Integer id);
}
