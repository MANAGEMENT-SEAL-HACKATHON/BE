package com.sealhackathon.api.submissions.service.impl;

import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.value_object.LateSubmissionPolicy;
import com.sealhackathon.api.submissions.entity.Submission;
import com.sealhackathon.api.submissions.value_object.SubmissionStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class SubmissionStatusOnSubmitTest {

    @InjectMocks
    private SubmissionServiceImpl submissionService;

    @Test
    void submittedAfterDeadline_becomesRejected_onHardLock() throws Exception {
        Submission existing = Submission.builder().status(SubmissionStatus.SUBMITTED).build();
        Round round = Round.builder().lateSubmissionPolicy(LateSubmissionPolicy.HARD_LOCK).build();

        assertThat(invokeResolveStatusOnSubmit(existing, round, true))
                .isEqualTo(SubmissionStatus.REJECTED);
    }

    @Test
    void submittedAfterDeadline_becomesLatePending_onAllow() throws Exception {
        Submission existing = Submission.builder().status(SubmissionStatus.SUBMITTED).build();
        Round round = Round.builder().lateSubmissionPolicy(LateSubmissionPolicy.ALLOW_LATE_PENDING).build();

        assertThat(invokeResolveStatusOnSubmit(existing, round, true))
                .isEqualTo(SubmissionStatus.LATE_PENDING);
    }

    @Test
    void submittedOnTime_staysSubmitted() throws Exception {
        Submission existing = Submission.builder().status(SubmissionStatus.SUBMITTED).build();
        Round round = Round.builder().lateSubmissionPolicy(LateSubmissionPolicy.ALLOW_LATE_PENDING).build();

        assertThat(invokeResolveStatusOnSubmit(existing, round, false))
                .isEqualTo(SubmissionStatus.SUBMITTED);
    }

    @Test
    void lateApprovedStaysLateApprovedAfterLateReedit() throws Exception {
        Submission existing = Submission.builder().status(SubmissionStatus.LATE_APPROVED).build();
        Round round = Round.builder().lateSubmissionPolicy(LateSubmissionPolicy.HARD_LOCK).build();

        assertThat(invokeResolveStatusOnSubmit(existing, round, true))
                .isEqualTo(SubmissionStatus.LATE_APPROVED);
    }

    private SubmissionStatus invokeResolveStatusOnSubmit(
            Submission existing, Round round, boolean afterDeadline) throws Exception {
        Method method = SubmissionServiceImpl.class.getDeclaredMethod(
                "resolveStatusOnSubmit", Submission.class, Round.class, boolean.class);
        method.setAccessible(true);
        return (SubmissionStatus) method.invoke(submissionService, existing, round, afterDeadline);
    }
}
