package com.sealhackathon.api.appeals.service;

import com.sealhackathon.api.me.student.dto.request.CreateAppealRequest;
import com.sealhackathon.api.me.student.dto.response.AppealResponse;

public interface AppealService {

    AppealResponse create(CreateAppealRequest request);
}
