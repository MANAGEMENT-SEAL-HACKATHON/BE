package com.sealhackathon.api.export_jobs.service.impl;

import com.sealhackathon.api.export_jobs.dto.request.CreateExportJobRequest;
import com.sealhackathon.api.export_jobs.dto.response.ExportJobResponse;
import com.sealhackathon.api.export_jobs.repository.ExportJobRepository;
import com.sealhackathon.api.export_jobs.service.ExportJobService;
import com.sealhackathon.api.export_jobs.value_object.ExportJobStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ExportJobServiceImpl implements ExportJobService {

    private final ExportJobRepository exportJobRepository;

    @Override
    public ExportJobResponse create(Integer hackathonId, CreateExportJobRequest req) {
        // TODO: FR-34 — gate FINISHED, INSERT export_jobs PENDING, enqueue worker
        return ExportJobResponse.builder()
                .hackathonId(hackathonId)
                .type(req.getType())
                .status(ExportJobStatus.PENDING)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ExportJobResponse getById(Integer jobId) {
        // TODO: FR-34 — load job + role check
        return ExportJobResponse.builder()
                .id(jobId)
                .status(ExportJobStatus.PENDING)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public String downloadUrl(Integer jobId) {
        // TODO: FR-34/35 — gate DONE, expires_at, audit download FR-36
        return null;
    }
}
