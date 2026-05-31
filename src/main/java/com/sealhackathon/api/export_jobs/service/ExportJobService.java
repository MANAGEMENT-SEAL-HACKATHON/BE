package com.sealhackathon.api.export_jobs.service;

import com.sealhackathon.api.export_jobs.dto.request.CreateExportJobRequest;
import com.sealhackathon.api.export_jobs.dto.response.ExportJobResponse;

public interface ExportJobService {

    ExportJobResponse create(Integer hackathonId, CreateExportJobRequest req);

    ExportJobResponse getById(Integer jobId);

    /** FR-34/35 — trả file URL hoặc stream; stub trả null. */
    String downloadUrl(Integer jobId);
}
