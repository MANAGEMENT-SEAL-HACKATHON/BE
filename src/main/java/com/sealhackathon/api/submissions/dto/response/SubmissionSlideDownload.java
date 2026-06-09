package com.sealhackathon.api.submissions.dto.response;

import com.sealhackathon.api.storage.StoredObject;

public record SubmissionSlideDownload(StoredObject content, String filename) {}
