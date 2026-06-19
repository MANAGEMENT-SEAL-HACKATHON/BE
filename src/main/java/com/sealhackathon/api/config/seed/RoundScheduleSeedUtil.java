package com.sealhackathon.api.config.seed;

import java.time.LocalDateTime;

/**
 * Lịch round seed — khớp {@link com.sealhackathon.api.rounds.service.impl.RoundServiceImpl#validateSubmissionWindowByCodingDuration}.
 */
public final class RoundScheduleSeedUtil {

    public static final int DEFAULT_PRELIM_CODING_HOURS = 7;

    /** Dự trù thời gian chấm Sơ loại sau khi hết giờ làm bài */
    public static final int GRADING_BUFFER_HOURS_AFTER_PRELIM = 2;

    /** Khoảng cách tối thiểu giữa lúc chấm xong Sơ loại và giờ thi Chung kết */
    public static final int FINAL_EXAM_GAP_AFTER_GRADING_HOURS = 1;

    private RoundScheduleSeedUtil() {
    }

    /** {@code examAt + (codingDurationHours * 2 / 3)} — phút làm tròn xuống theo phút. */
    public static LocalDateTime submissionOpen(LocalDateTime examAt, int codingDurationHours) {
        long openOffsetMinutes = (codingDurationHours * 60L * 2L) / 3L;
        return examAt.plusMinutes(openOffsetMinutes);
    }

    /** {@code examAt + codingDurationHours}. */
    public static LocalDateTime submissionDeadline(LocalDateTime examAt, int codingDurationHours) {
        return examAt.plusHours(codingDurationHours);
    }

    /**
     * Thời điểm sớm nhất cho {@code examAt} round Chung kết — khớp
     * {@link com.sealhackathon.api.rounds.service.impl.RoundServiceImpl} {@code ROUND_FINAL_EXAM_ORDER}.
     * = kết thúc Sơ loại + buffer chấm + 1h nghỉ.
     */
    public static LocalDateTime minFinalExamAt(LocalDateTime prelimExamAt, int prelimCodingHours) {
        return prelimExamAt.plusHours(prelimCodingHours)
                .plusHours(GRADING_BUFFER_HOURS_AFTER_PRELIM)
                .plusHours(FINAL_EXAM_GAP_AFTER_GRADING_HOURS);
    }
}
