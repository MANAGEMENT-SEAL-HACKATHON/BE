package com.sealhackathon.api.appeals.service;

import com.sealhackathon.api.appeals.dto.request.AppealDelayRequest;
import com.sealhackathon.api.appeals.dto.request.PublishWithAppealWindowRequest;
import com.sealhackathon.api.appeals.dto.response.AppealDelayPreviewResponse;
import com.sealhackathon.api.appeals.dto.response.AppealWindowStatusResponse;
import com.sealhackathon.api.appeals.dto.response.PublishPreflightResponse;
import com.sealhackathon.api.rounds.entity.Round;

import java.time.LocalDateTime;

public interface AppealWindowService {

    int MIN_APPEAL_WINDOW_MINUTES = 10;
    int MAX_APPEAL_DELAY_MINUTES = 30;
    int DEFAULT_APPEAL_WINDOW_MINUTES = 30;

    PublishPreflightResponse preflight(Integer roundId);

    /**
     * Open appeal window on first publish (one-shot). Applies late-publish mode when needed.
     * Must be called after round.isPublished / publishedAt are set.
     */
    void openOnFirstPublish(Round prelimRound, PublishWithAppealWindowRequest modeRequest, LocalDateTime publishedAt);

    /** Close early — only when no PENDING/UNDER_REVIEW. */
    AppealWindowStatusResponse closeEarly(Integer roundId);

    /** Expire open appeals whose window has closed; returns count expired. */
    int expireOpenAppealsForRound(Integer roundId);

    /** Scheduler entry — expire across all rounds with closed windows. */
    int expireAllDueAppeals();

    AppealWindowStatusResponse getWindowStatus(Integer roundId);

    AppealDelayPreviewResponse previewDelay(Integer prelimRoundId, AppealDelayRequest request);

    AppealDelayPreviewResponse applyDelay(Integer prelimRoundId, AppealDelayRequest request);

    /** Republish results after appeal approve — does NOT reset appealWindowEndsAt. */
    AppealWindowStatusResponse republish(Integer roundId);

    void validateAppealWindowMinutes(int minutes);
}
