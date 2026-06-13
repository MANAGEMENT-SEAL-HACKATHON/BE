package com.sealhackathon.api.export_jobs.dto.response;

import com.sealhackathon.api.storage.StoredObject;

public record ExportFileDownload(StoredObject content, String filename) {}
