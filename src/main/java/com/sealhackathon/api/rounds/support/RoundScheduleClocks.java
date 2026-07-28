package com.sealhackathon.api.rounds.support;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Clock helpers for round schedule shifts (JVM local zone — khớp {@code LocalDateTime.now()} toàn BE).
 */
public final class RoundScheduleClocks {

    private RoundScheduleClocks() {
    }

    /**
     * Làm tròn lên phút kế tiếp khi còn giây/nanos (14:32:15 → 14:33:00);
     * nếu đã đúng :00.000 thì giữ phút hiện tại đã truncate.
     */
    public static LocalDateTime ceilToNextMinute(LocalDateTime value) {
        if (value == null) {
            return null;
        }
        LocalDateTime truncated = value.truncatedTo(ChronoUnit.MINUTES);
        if (value.getSecond() > 0 || value.getNano() > 0) {
            return truncated.plusMinutes(1);
        }
        return truncated;
    }

    public static LocalDateTime nowCeilToNextMinute() {
        return ceilToNextMinute(LocalDateTime.now());
    }
}
