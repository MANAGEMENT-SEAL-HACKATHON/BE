package com.sealhackathon.api.presentation.service;

import com.sealhackathon.api.presentation.dto.response.PresentationTimerActionResponse;

public interface PresentationTimerService {

    PresentationTimerActionResponse start(Integer roundId, Integer trackId);

    PresentationTimerActionResponse pause(Integer roundId, Integer trackId);

    PresentationTimerActionResponse resume(Integer roundId, Integer trackId);

    PresentationTimerActionResponse qa(Integer roundId, Integer trackId);

    PresentationTimerActionResponse reset(Integer roundId, Integer trackId);

    /** Kết thúc sớm Q&A (có người) — enforce scoring completeness trừ khi force-ack hợp lệ. */
    PresentationTimerActionResponse end(Integer roundId, Integer trackId, boolean acknowledgeIncompleteScoring);
}
