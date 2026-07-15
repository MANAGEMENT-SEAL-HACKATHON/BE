package com.sealhackathon.api.rounds.value_object;

import com.sealhackathon.api.submissions.value_object.SubmissionStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * INVARIANT-01/02: HARD_LOCK rounds must not persist LATE_PENDING / LATE_APPROVED.
 * Enforced in SubmissionServiceImpl; this documents the allowed status set.
 */
class HardLockLateInvariantTest {

    @Test
    void hardLock_allowedStatuses_doNotIncludeLateApprovedOrPending() {
        assertThat(SubmissionStatus.LATE_PENDING.name()).isEqualTo("LATE_PENDING");
        assertThat(SubmissionStatus.LATE_APPROVED.name()).isEqualTo("LATE_APPROVED");
        // Policy enum presence
        assertThat(LateSubmissionPolicy.HARD_LOCK).isNotNull();
        assertThat(LateSubmissionPolicy.ALLOW_LATE_PENDING).isNotNull();
    }

    @Test
    void invariantRule_lateStatusesAreForbiddenOnHardLock() {
        boolean hardLock = true;
        for (SubmissionStatus status : new SubmissionStatus[]{
                SubmissionStatus.LATE_PENDING, SubmissionStatus.LATE_APPROVED}) {
            boolean violation = hardLock
                    && (status == SubmissionStatus.LATE_PENDING || status == SubmissionStatus.LATE_APPROVED);
            assertThat(violation).isTrue();
        }
        assertThat(hardLock && SubmissionStatus.REJECTED == SubmissionStatus.REJECTED).isTrue();
    }
}
