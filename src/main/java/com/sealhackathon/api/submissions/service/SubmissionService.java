package com.sealhackathon.api.submissions.service;

import com.sealhackathon.api.submissions.dto.request.ReviewLateSubmissionRequest;
import com.sealhackathon.api.submissions.dto.request.SubmitSubmissionRequest;
import com.sealhackathon.api.submissions.dto.response.SubmissionResponse;
import com.sealhackathon.api.submissions.dto.response.SubmissionSlideDownload;
import com.sealhackathon.api.submissions.value_object.SubmissionStatus;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface SubmissionService {

    SubmissionResponse submit(SubmitSubmissionRequest req);

    SubmissionResponse submitMultipart(
            Integer teamId, Integer trackId, Integer roundId,
            String repoUrl, String demoUrl, String lateReason,
            MultipartFile slideFile);

    SubmissionSlideDownload getSlideDownload(Integer submissionId);

    List<SubmissionResponse> list(Integer teamId, Integer roundId, SubmissionStatus status);

    SubmissionResponse reviewLate(Integer submissionId, ReviewLateSubmissionRequest req);
}
