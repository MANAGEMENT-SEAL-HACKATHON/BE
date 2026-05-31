package com.sealhackathon.api.submissions.service.impl;

import com.sealhackathon.api.submissions.entity.Submission;
import com.sealhackathon.api.submissions.entity.SubmissionMetadata;
import com.sealhackathon.api.submissions.repository.SubmissionMetadataRepository;
import com.sealhackathon.api.submissions.repository.SubmissionRepository;
import com.sealhackathon.api.submissions.service.SubmissionMetadataService;
import com.sealhackathon.api.submissions.value_object.MetadataFetchStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** FR-17 — enqueue metadata fetch (async job TODO). */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SubmissionMetadataServiceImpl implements SubmissionMetadataService {

    private final SubmissionRepository submissionRepository;
    private final SubmissionMetadataRepository submissionMetadataRepository;

    @Override
    public void enqueueFetch(Integer submissionId) {
        Submission submission = submissionRepository.findById(submissionId).orElse(null);
        if (submission == null || !org.springframework.util.StringUtils.hasText(submission.getRepoUrl())) {
            return;
        }
        if (submissionMetadataRepository.existsById(submissionId)) {
            return;
        }
        SubmissionMetadata meta = SubmissionMetadata.builder()
                .submission(submission)
                .metadataFetchStatus(MetadataFetchStatus.PENDING)
                .build();
        submissionMetadataRepository.save(meta);
        log.debug("[FR-17] Enqueued metadata fetch for submission #{}", submissionId);
    }
}
