package com.sealhackathon.api.presentation.service;

import com.sealhackathon.api.presentation.dto.response.PresentationQueueNextResponse;
import com.sealhackathon.api.presentation.dto.response.PresentationQueueResponse;

public interface PresentationQueueService {

    PresentationQueueResponse getQueue(Integer roundId);

    PresentationQueueNextResponse advanceNext(Integer roundId, Integer currentTeamId);
}
