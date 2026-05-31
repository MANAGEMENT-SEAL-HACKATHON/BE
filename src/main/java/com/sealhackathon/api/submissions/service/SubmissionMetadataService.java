package com.sealhackathon.api.submissions.service;

/** FR-17 — async repo metadata (optional). */
public interface SubmissionMetadataService {

    /** Enqueue fetch job for submission repo URL. */
    void enqueueFetch(Integer submissionId);
}
