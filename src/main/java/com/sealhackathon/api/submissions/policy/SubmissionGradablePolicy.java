package com.sealhackathon.api.submissions.policy;

import com.sealhackathon.api.submissions.entity.Submission;
import com.sealhackathon.api.submissions.value_object.SubmissionStatus;

import java.util.EnumSet;
import java.util.Set;

/** FR-18 — submission status cho phép chấm điểm. */
public final class SubmissionGradablePolicy {

    private static final Set<SubmissionStatus> GRADABLE = EnumSet.of(
            SubmissionStatus.SUBMITTED,
            SubmissionStatus.LATE_APPROVED,
            SubmissionStatus.ACCEPTED
    );

    private SubmissionGradablePolicy() {}

    public static boolean isGradable(Submission submission) {
        return submission != null && GRADABLE.contains(submission.getStatus());
    }
}
