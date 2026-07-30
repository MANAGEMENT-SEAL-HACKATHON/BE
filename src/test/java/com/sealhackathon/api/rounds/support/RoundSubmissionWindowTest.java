package com.sealhackathon.api.rounds.support;

import com.sealhackathon.api.rounds.entity.Round;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoundSubmissionWindowTest {

    @Test
    void closed_whenSubmissionClosedEarlyAtSet() {
        Round round = Round.builder()
                .submissionClosedEarlyAt(LocalDateTime.now().minusMinutes(1))
                .submissionDeadline(LocalDateTime.now().plusHours(2))
                .build();
        assertTrue(RoundSubmissionWindow.isClosed(round, LocalDateTime.now()));
    }

    @Test
    void closed_whenDeadlinePassed() {
        Round round = Round.builder()
                .submissionDeadline(LocalDateTime.now().minusMinutes(1))
                .build();
        assertTrue(RoundSubmissionWindow.isClosed(round, LocalDateTime.now()));
    }

    @Test
    void open_whenDeadlineInFuture_andNotClosedEarly() {
        Round round = Round.builder()
                .submissionDeadline(LocalDateTime.now().plusHours(1))
                .build();
        assertFalse(RoundSubmissionWindow.isClosed(round, LocalDateTime.now()));
    }

    @Test
    void open_whenNoDeadline_andNotClosedEarly() {
        Round round = Round.builder().build();
        assertFalse(RoundSubmissionWindow.isClosed(round, LocalDateTime.now()));
    }
}
