package com.sealhackathon.api.submissions.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * @deprecated Backward-compatible adapter. Use {@link SubmissionRepository}.
 */
@Deprecated
@Component
@RequiredArgsConstructor
public class SubmissionPlaceholderRepository {

    private final SubmissionRepository submissionRepository;

    public long countByRoundId(Integer roundId) {
        return submissionRepository.countByRoundId(roundId);
    }
}
