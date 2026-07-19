package com.sealhackathon.api.criteria.service;

import com.sealhackathon.api.criteria.dto.request.CriteriaTemplateRequest;
import com.sealhackathon.api.criteria.dto.response.CriteriaTemplateResponse;

import java.util.List;

public interface CriteriaTemplateService {
    record ApplyResult(List<Integer> createdIds, int count) {}

    List<CriteriaTemplateResponse> list();
    CriteriaTemplateResponse get(Integer id);
    CriteriaTemplateResponse create(CriteriaTemplateRequest request);
    CriteriaTemplateResponse update(Integer id, CriteriaTemplateRequest request);
    void delete(Integer id);
    ApplyResult applyToTrack(Integer templateId, Integer trackId, boolean replaceExisting);
    ApplyResult applyToFinalRound(Integer templateId, Integer roundId, boolean replaceExisting);
}
