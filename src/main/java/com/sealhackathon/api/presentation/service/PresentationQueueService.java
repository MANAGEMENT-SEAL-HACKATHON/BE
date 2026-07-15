package com.sealhackathon.api.presentation.service;

import com.sealhackathon.api.presentation.dto.request.PresentationShuffleRequest;
import com.sealhackathon.api.presentation.dto.response.PresentationQueueNextResponse;
import com.sealhackathon.api.presentation.dto.response.PresentationQueueResponse;
import com.sealhackathon.api.presentation.dto.response.PresentationShuffleResponse;
import com.sealhackathon.api.submissions.entity.Submission;

public interface PresentationQueueService {

    PresentationQueueResponse getQueue(Integer roundId, Integer trackId);

    PresentationQueueNextResponse advanceNext(
            Integer roundId,
            Integer trackId,
            Integer currentSubmissionId,
            Integer currentTeamId,
            boolean acknowledgeIncompleteScoring);

    PresentationShuffleResponse shuffle(PresentationShuffleRequest request);

    /**
     * Đánh dấu no-show: slot → SKIPPED (không tính như DONE đã thuyết trình).
     */
    PresentationQueueResponse skipNoShow(Integer roundId, Integer trackId, Integer submissionId);

    /**
     * Sau duyệt LATE_APPROVED: chèn WAITING cuối hàng đợi nếu đã shuffle — không reshuffle.
     * @return true nếu đã append slot mới
     */
    boolean appendLateApprovedIfShuffled(Submission submission);
}
