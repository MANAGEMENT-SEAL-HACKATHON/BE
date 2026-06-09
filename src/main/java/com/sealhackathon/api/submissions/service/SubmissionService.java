package com.sealhackathon.api.submissions.service;

import com.sealhackathon.api.submissions.dto.request.ResubmitSubmissionRequest;
import com.sealhackathon.api.submissions.dto.request.ReviewLateSubmissionRequest;
import com.sealhackathon.api.submissions.dto.request.ReviewSubmissionRequest;
import com.sealhackathon.api.submissions.dto.request.SubmitSubmissionRequest;
import com.sealhackathon.api.submissions.dto.response.SubmissionResponse;
import com.sealhackathon.api.submissions.dto.response.SubmissionSlideDownload;
import com.sealhackathon.api.submissions.value_object.SubmissionStatus;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface SubmissionService {

    SubmissionResponse submit(SubmitSubmissionRequest req);

    SubmissionResponse submitMultipart(
            Integer teamId,
            Integer trackId,
            Integer roundId,
            String repoUrl,
            String lateReason,
            MultipartFile slideFile);

    SubmissionSlideDownload getSlideDownload(Integer submissionId);

    List<SubmissionResponse> list(Integer teamId, Integer roundId, SubmissionStatus status);

    SubmissionResponse resubmit(Integer submissionId, ResubmitSubmissionRequest req);

    SubmissionResponse reviewLate(Integer submissionId, ReviewLateSubmissionRequest req);

    /** @deprecated v4.1 — dùng {@link #reviewLate} */
    @Deprecated
    SubmissionResponse review(Integer submissionId, ReviewSubmissionRequest req);
}
