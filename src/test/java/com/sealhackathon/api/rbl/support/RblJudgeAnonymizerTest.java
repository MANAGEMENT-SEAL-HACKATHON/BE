package com.sealhackathon.api.rbl.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** RBL-HASH-01 */
class RblJudgeAnonymizerTest {

    @Test
    void anonymize_stableForSameInput() {
        String a = RblJudgeAnonymizer.anonymize(1, 42);
        String b = RblJudgeAnonymizer.anonymize(1, 42);
        assertThat(a).isEqualTo(b);
        assertThat(a).startsWith("J");
    }

    @Test
    void anonymize_differsAcrossHackathons() {
        String a = RblJudgeAnonymizer.anonymize(1, 42);
        String b = RblJudgeAnonymizer.anonymize(2, 42);
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void anonymize_nullJudgeReturnsEmpty() {
        assertThat(RblJudgeAnonymizer.anonymize(1, null)).isEmpty();
    }
}
