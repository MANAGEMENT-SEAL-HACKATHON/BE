package com.sealhackathon.api.presentation.service;

import com.sealhackathon.api.presentation.dto.request.PresentationShuffleRequest;
import com.sealhackathon.api.presentation.dto.response.PresentationQueueNextResponse;
import com.sealhackathon.api.presentation.dto.response.PresentationQueueResponse;
import com.sealhackathon.api.presentation.dto.response.PresentationShuffleResponse;

public interface PresentationQueueService {

    PresentationQueueResponse getQueue(Integer roundId, Integer trackId);

    PresentationQueueNextResponse advanceNext(
            Integer roundId,
            Integer trackId,
            Integer currentSubmissionId,
            Integer currentTeamId,
            boolean acknowledgeIncompleteScoring);

    PresentationShuffleResponse shuffle(PresentationShuffleRequest request);
}
