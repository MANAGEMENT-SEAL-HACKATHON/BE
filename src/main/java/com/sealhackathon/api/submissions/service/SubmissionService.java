package com.sealhackathon.api.submissions.service;

import com.sealhackathon.api.submissions.dto.request.ResubmitSubmissionRequest;
import com.sealhackathon.api.submissions.dto.request.ReviewSubmissionRequest;
import com.sealhackathon.api.submissions.dto.request.SubmitSubmissionRequest;
import com.sealhackathon.api.submissions.dto.response.SubmissionResponse;

import java.util.List;

public interface SubmissionService {

    SubmissionResponse submit(SubmitSubmissionRequest req);

    List<SubmissionResponse> list(Integer teamId, Integer roundId);

    SubmissionResponse resubmit(Integer submissionId, ResubmitSubmissionRequest req);

    SubmissionResponse review(Integer submissionId, ReviewSubmissionRequest req);
}
