package com.sealhackathon.api.presentation.service;

import com.sealhackathon.api.presentation.dto.response.PresentationTimerActionResponse;

public interface PresentationTimerService {

    PresentationTimerActionResponse start(Integer roundId, Integer trackId);

    PresentationTimerActionResponse pause(Integer roundId, Integer trackId);

    PresentationTimerActionResponse resume(Integer roundId, Integer trackId);

    PresentationTimerActionResponse qa(Integer roundId, Integer trackId);

    PresentationTimerActionResponse reset(Integer roundId, Integer trackId);
}
