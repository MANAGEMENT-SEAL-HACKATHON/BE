package com.se194093.be.criteria.service;

import com.se194093.be.common.response.Warning;
import com.se194093.be.criteria.dto.request.BatchCreateCriteriaRequest;
import com.se194093.be.criteria.dto.request.CloneCriteriaRequest;
import com.se194093.be.criteria.dto.request.CreateCriterionRequest;
import com.se194093.be.criteria.dto.request.UpdateCriterionRequest;
import com.se194093.be.criteria.dto.response.BatchCreateResponse;
import com.se194093.be.criteria.dto.response.CloneResponse;
import com.se194093.be.criteria.dto.response.CriteriaListResponse;
import com.se194093.be.criteria.dto.response.CriterionResponse;

import java.util.List;
import java.util.Optional;

/**
 * FR-04 — CRUD + batch + clone Criteria. Validate weight chỉ ở mức WARN MỀM (không block).
 *
 * <p>Audit: {@code CRITERIA_CREATE}, {@code CRITERIA_UPDATE}, {@code CRITERIA_DELETE},
 * {@code CRITERIA_CLONE}.
 */
public interface CriteriaService {

    /**
     * Container trả về cả entity và Optional&lt;Warning&gt; (cho mutation đơn).
     */
    record CreateResult(CriterionResponse criterion, Optional<Warning> weightWarning) {}

    record UpdateResult(CriterionResponse criterion, Optional<Warning> weightWarning) {}

    CreateResult create(Integer roundId, CreateCriterionRequest req);

    BatchCreateResponse batchCreate(Integer roundId, BatchCreateCriteriaRequest req);

    CriteriaListResponse listByRound(Integer roundId);

    CriterionResponse getById(Integer id);

    UpdateResult update(Integer id, UpdateCriterionRequest req);

    Integer delete(Integer id);

    CloneResponse cloneFromSource(Integer roundId, CloneCriteriaRequest req);

    /**
     * Helper trả về danh sách warning (max 1) — wrap {@link Optional} cho controller dùng tiện.
     */
    List<Warning> wrap(Optional<Warning> single);
}
