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

public interface CriteriaService {

    record CreateResult(CriterionResponse criterion, Optional<Warning> weightWarning) {}

    record UpdateResult(CriterionResponse criterion, Optional<Warning> weightWarning) {}

    CreateResult createForTrack(Integer trackId, CreateCriterionRequest req);

    CreateResult createForFinalRound(Integer finalRoundId, CreateCriterionRequest req);

    /** @deprecated — chỉ Round FINAL */
    @Deprecated
    CreateResult create(Integer roundId, CreateCriterionRequest req);

    BatchCreateResponse batchCreateForTrack(Integer trackId, BatchCreateCriteriaRequest req);

    BatchCreateResponse batchCreateForFinalRound(Integer finalRoundId, BatchCreateCriteriaRequest req);

    @Deprecated
    BatchCreateResponse batchCreate(Integer roundId, BatchCreateCriteriaRequest req);

    CriteriaListResponse listByTrack(Integer trackId);

    CriteriaListResponse listByFinalRound(Integer finalRoundId);

    @Deprecated
    CriteriaListResponse listByRound(Integer roundId);

    CriterionResponse getById(Integer id);

    UpdateResult update(Integer id, UpdateCriterionRequest req);

    Integer delete(Integer id);

    CloneResponse cloneFromSourceForTrack(Integer trackId, CloneCriteriaRequest req);

    CloneResponse cloneFromSourceForFinalRound(Integer finalRoundId, CloneCriteriaRequest req);

    @Deprecated
    CloneResponse cloneFromSource(Integer roundId, CloneCriteriaRequest req);

    List<Warning> wrap(Optional<Warning> single);
}
