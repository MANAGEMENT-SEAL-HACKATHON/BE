package com.sealhackathon.api.appeals.service.impl;

import com.sealhackathon.api.appeals.repository.AppealRepository;
import com.sealhackathon.api.appeals.service.AppealService;
import com.sealhackathon.api.appeals.value_object.AppealStatus;
import com.sealhackathon.api.me.student.dto.request.CreateAppealRequest;
import com.sealhackathon.api.me.student.dto.response.AppealResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AppealServiceImpl implements AppealService {

    private final AppealRepository appealRepository;

    @Override
    public AppealResponse create(CreateAppealRequest request) {
        // TODO: FR-U-30 — 24h window, team leader, INSERT appeals
        return AppealResponse.builder()
                .teamId(request.getTeamId())
                .roundId(request.getRoundId())
                .reason(request.getReason())
                .evidenceUrl(request.getEvidenceUrl())
                .status(AppealStatus.PENDING)
                .build();
    }
}
