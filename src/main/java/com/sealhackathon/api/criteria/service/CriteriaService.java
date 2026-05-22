package com.sealhackathon.api.criteria.service;

import com.sealhackathon.api.common.response.Warning;
import com.sealhackathon.api.criteria.dto.request.BatchCreateCriteriaRequest;
import com.sealhackathon.api.criteria.dto.request.CloneCriteriaRequest;
import com.sealhackathon.api.criteria.dto.request.CreateCriterionRequest;
import com.sealhackathon.api.criteria.dto.request.UpdateCriterionRequest;
import com.sealhackathon.api.criteria.dto.response.BatchCreateResponse;
import com.sealhackathon.api.criteria.dto.response.CloneResponse;
import com.sealhackathon.api.criteria.dto.response.CriteriaCloneSourcesResponse;
import com.sealhackathon.api.criteria.dto.response.CriteriaListResponse;
import com.sealhackathon.api.criteria.dto.response.CriterionResponse;

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

    CriteriaCloneSourcesResponse listCloneSourcesForTrack(Integer targetTrackId);

    CloneResponse cloneFromSourceForTrack(Integer trackId, CloneCriteriaRequest req);

    CloneResponse cloneFromSourceForFinalRound(Integer finalRoundId, CloneCriteriaRequest req);

    @Deprecated
    CloneResponse cloneFromSource(Integer roundId, CloneCriteriaRequest req);

    List<Warning> wrap(Optional<Warning> single);
}
