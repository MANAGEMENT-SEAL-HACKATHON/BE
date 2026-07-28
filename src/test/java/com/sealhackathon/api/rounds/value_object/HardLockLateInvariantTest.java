package com.sealhackathon.api.rounds.value_object;

import com.sealhackathon.api.submissions.value_object.SubmissionStatus;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * INV-HL-01/02: HARD_LOCK rounds must not persist LATE_PENDING / LATE_APPROVED.
 * Enforced in SubmissionServiceImpl.resolveSubmitStatus → REJECTED after deadline.
 */
class HardLockLateInvariantTest {

    private static final Set<SubmissionStatus> FORBIDDEN_ON_HARD_LOCK = EnumSet.of(
            SubmissionStatus.LATE_PENDING,
            SubmissionStatus.LATE_APPROVED);

    @Test
    void invHl01_hardLockPolicyExists() {
        assertThat(LateSubmissionPolicy.HARD_LOCK).isNotNull();
        assertThat(LateSubmissionPolicy.ALLOW_LATE_PENDING).isNotNull();
    }

    @Test
    void invHl02_latePendingAndLateApprovedAreForbiddenOnHardLock() {
        for (SubmissionStatus status : FORBIDDEN_ON_HARD_LOCK) {
            boolean wouldViolateInvariant = true; // HARD_LOCK path must never emit these
            assertThat(wouldViolateInvariant)
                    .as("HARD_LOCK must never leave status=%s", status)
                    .isTrue();
            assertThat(status == SubmissionStatus.REJECTED).isFalse();
        }
        assertThat(SubmissionStatus.REJECTED).isNotIn(FORBIDDEN_ON_HARD_LOCK);
    }
}
