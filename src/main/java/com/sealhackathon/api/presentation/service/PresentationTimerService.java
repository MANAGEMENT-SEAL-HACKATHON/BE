package com.sealhackathon.api.presentation.service;

import com.sealhackathon.api.presentation.dto.response.PresentationTimerActionResponse;

public interface PresentationTimerService {

    PresentationTimerActionResponse start(Integer roundId, Integer trackId);

    PresentationTimerActionResponse pause(Integer roundId, Integer trackId);

    PresentationTimerActionResponse resume(Integer roundId, Integer trackId);

    PresentationTimerActionResponse qa(Integer roundId, Integer trackId);

    PresentationTimerActionResponse reset(Integer roundId, Integer trackId);

    /** Kết thúc Q&A: sớm → bắt đủ Chốt điểm (trừ force-ack); hết giờ tự nhiên → không scoring guard. */
    PresentationTimerActionResponse end(Integer roundId, Integer trackId, boolean acknowledgeIncompleteScoring);
}
