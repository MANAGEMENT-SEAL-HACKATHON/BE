package com.sealhackathon.api.rounds.support;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class RoundScheduleClocksTest {

    @Test
    void ceilToNextMinute_whenHasSeconds_roundsUp() {
        LocalDateTime in = LocalDateTime.of(2026, 7, 14, 14, 32, 15);
        assertThat(RoundScheduleClocks.ceilToNextMinute(in))
                .isEqualTo(LocalDateTime.of(2026, 7, 14, 14, 33, 0));
    }

    @Test
    void ceilToNextMinute_whenAlreadyOnMinute_keeps() {
        LocalDateTime in = LocalDateTime.of(2026, 7, 14, 14, 32, 0);
        assertThat(RoundScheduleClocks.ceilToNextMinute(in))
                .isEqualTo(LocalDateTime.of(2026, 7, 14, 14, 32, 0));
    }
}
