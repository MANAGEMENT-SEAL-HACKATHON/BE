package com.sealhackathon.api.config.seed;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoundScheduleSeedUtilTest {

    @Test
    void minAndMaxFinalExamAt_areOneToTwoHoursAfterPrelimEnd() {
        LocalDateTime prelimExam = LocalDateTime.of(2026, 7, 29, 8, 0);
        LocalDateTime prelimDeadline = RoundScheduleSeedUtil.submissionDeadline(
                prelimExam, RoundScheduleSeedUtil.DEFAULT_PRELIM_CODING_HOURS);
        LocalDateTime minFinal = RoundScheduleSeedUtil.minFinalExamAt(
                prelimExam, RoundScheduleSeedUtil.DEFAULT_PRELIM_CODING_HOURS);
        LocalDateTime maxFinal = RoundScheduleSeedUtil.maxFinalExamAt(
                prelimExam, RoundScheduleSeedUtil.DEFAULT_PRELIM_CODING_HOURS);

        // 08:00 + 7h coding = 15:00 end → CK window 16:00–17:00
        assertEquals(LocalDateTime.of(2026, 7, 29, 15, 0), prelimDeadline);
        assertEquals(LocalDateTime.of(2026, 7, 29, 16, 0), minFinal);
        assertEquals(LocalDateTime.of(2026, 7, 29, 17, 0), maxFinal);
    }

    @Test
    void userExample_prelimEndsAtNoon_allowsFinalAtTwoPm() {
        LocalDateTime prelimExam = LocalDateTime.of(2026, 8, 8, 8, 0);
        int codingHours = 4; // ends 12:00
        assertEquals(LocalDateTime.of(2026, 8, 8, 13, 0),
                RoundScheduleSeedUtil.minFinalExamAt(prelimExam, codingHours));
        assertEquals(LocalDateTime.of(2026, 8, 8, 14, 0),
                RoundScheduleSeedUtil.maxFinalExamAt(prelimExam, codingHours));
    }

    @Test
    void finalSubmissionWindow_matchesCodingDurationHours() {
        LocalDateTime finalExam = LocalDateTime.of(2026, 7, 31, 18, 0);
        LocalDateTime open = RoundScheduleSeedUtil.finalSubmissionOpen(finalExam);
        LocalDateTime deadline = RoundScheduleSeedUtil.finalSubmissionDeadline(finalExam);

        assertEquals(LocalDateTime.of(2026, 7, 31, 19, 20), open);
        assertEquals(LocalDateTime.of(2026, 7, 31, 20, 0), deadline);
        assertTrue(deadline.isAfter(open));
    }

    @Test
    void gd2OpenCalendar_leavesRoomForWorkshopAndKickoff() {
        LocalDate today = LocalDate.now();
        LocalDate regEnd = today.plusDays(14);
        LocalDate eventStart = regEnd.plusDays(RoundScheduleSeedUtil.DAYS_REG_END_TO_EVENT_START);
        LocalDate wsDay = regEnd.plusDays(1);
        LocalDate koDay = regEnd.plusDays(2);

        assertTrue(wsDay.isAfter(regEnd));
        assertTrue(koDay.isAfter(regEnd));
        assertTrue(koDay.isBefore(eventStart));
        assertTrue(wsDay.isBefore(eventStart));
    }
}
