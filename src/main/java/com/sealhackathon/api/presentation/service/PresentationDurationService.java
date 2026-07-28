package com.sealhackathon.api.presentation.service;

import com.sealhackathon.api.presentation.dto.request.PresentationDurationSetupRequest;
import com.sealhackathon.api.presentation.dto.response.PresentationDurationResponse;

public interface PresentationDurationService {

    PresentationDurationResponse getDuration(Integer roundId, Integer trackId);

    PresentationDurationResponse updateDuration(PresentationDurationSetupRequest request);

    /** Gỡ override track — track dùng lại default của round. Chỉ áp dụng GĐ3. */
    PresentationDurationResponse clearTrackOverride(Integer roundId, Integer trackId);
}
