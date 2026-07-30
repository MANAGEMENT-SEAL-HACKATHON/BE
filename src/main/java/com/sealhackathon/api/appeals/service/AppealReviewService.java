package com.sealhackathon.api.appeals.service;

import com.sealhackathon.api.appeals.dto.request.ReviewAppealRequest;
import com.sealhackathon.api.appeals.value_object.AppealStatus;
import com.sealhackathon.api.me.student.dto.response.AppealResponse;

import java.util.List;

public interface AppealReviewService {

    List<AppealResponse> listByRound(Integer roundId, AppealStatus status);

    AppealResponse getById(Integer appealId);

    AppealResponse claim(Integer appealId);

    AppealResponse review(Integer appealId, ReviewAppealRequest request);
}
