package com.sealhackathon.api.submissions.service;

import com.sealhackathon.api.submissions.dto.response.SubmissionGithubResponse;

/** FR-17 — async repo metadata + commits (GitHub API). */
public interface SubmissionMetadataService {

    /** Enqueue fetch job for submission repo URL. */
    void enqueueFetch(Integer submissionId);

    /** Process PENDING rows (scheduler / sync-on-read). */
    int processPendingBatch(int limit);

    /**
     * Repo info + commits for UI. Never hard-fails for disabled/token/rate-limit —
     * returns empty payload with status flags.
     *
     * @param anonymous when true (or judge anonymous scoring), redact commit author name/avatar
     */
    SubmissionGithubResponse getGithubInfo(Integer submissionId, boolean anonymous);
}
