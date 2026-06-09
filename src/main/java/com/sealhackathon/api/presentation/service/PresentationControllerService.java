package com.sealhackathon.api.presentation.service;

import com.sealhackathon.api.presentation.dto.request.PresentationControllerGrantRequest;
import com.sealhackathon.api.presentation.dto.response.PresentationControllerResponse;

public interface PresentationControllerService {

    PresentationControllerResponse getTrackController(Integer trackId);

    PresentationControllerResponse grantTrackController(Integer trackId, PresentationControllerGrantRequest request);

    void revokeTrackController(Integer trackId);

    PresentationControllerResponse getRoundController(Integer roundId);

    PresentationControllerResponse grantRoundController(Integer roundId, PresentationControllerGrantRequest request);

    void revokeRoundController(Integer roundId);
}
