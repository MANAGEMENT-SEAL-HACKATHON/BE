package com.sealhackathon.api.export_jobs.service;

import com.sealhackathon.api.export_jobs.dto.request.CreateExportJobRequest;
import com.sealhackathon.api.export_jobs.dto.response.ExportFileDownload;
import com.sealhackathon.api.export_jobs.dto.response.ExportJobResponse;

public interface ExportJobService {

    ExportJobResponse create(Integer hackathonId, CreateExportJobRequest req);

    ExportJobResponse getById(Integer jobId);

    ExportFileDownload downloadFile(Integer jobId);
}
