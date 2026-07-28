package com.sealhackathon.api.config.seed;

import java.time.LocalDateTime;

/**
 * Lịch round seed — khớp {@link com.sealhackathon.api.rounds.service.impl.RoundServiceImpl#validateSubmissionWindowByCodingDuration}.
 */
public final class RoundScheduleSeedUtil {

    public static final int DEFAULT_PRELIM_CODING_HOURS = 7;

    /**
     * Khoảng cách tối thiểu giữa lúc Sơ loại kết thúc và giờ thi Chung kết.
     * Cửa sổ: {@code [prelimEnd + 1h, prelimEnd + 2h]}.
     */
    public static final int MIN_FINAL_GAP_HOURS_AFTER_PRELIM = 1;

    /** Khoảng cách tối đa giữa lúc Sơ loại kết thúc và giờ thi Chung kết. */
    public static final int MAX_FINAL_GAP_HOURS_AFTER_PRELIM = 2;

    /**
     * @deprecated Dùng {@link #MIN_FINAL_GAP_HOURS_AFTER_PRELIM} / {@link #MAX_FINAL_GAP_HOURS_AFTER_PRELIM}.
     * Giữ alias tạm để tránh break compile nếu còn tham chiếu cũ.
     */
    @Deprecated
    public static final int GRADING_BUFFER_HOURS_AFTER_PRELIM = 0;

    /** @deprecated Xem {@link #MIN_FINAL_GAP_HOURS_AFTER_PRELIM}. */
    @Deprecated
    public static final int FINAL_EXAM_GAP_AFTER_GRADING_HOURS = MIN_FINAL_GAP_HOURS_AFTER_PRELIM;

    /**
     * Số giờ thi CK mặc định khi seed ({@code codingDurationHours}).
     * Open/deadline suy ra theo cùng công thức Sơ loại (2/3 duration / full duration).
     */
    public static final int DEFAULT_FINAL_CODING_HOURS = 2;

    /**
     * Số ngày tối thiểu sau {@code registrationEnd} trước {@code eventStart}
     * để chứa WORKSHOP (regEnd+1) và KICKOFF (regEnd+2) trong gap exclusive.
     */
    public static final int DAYS_REG_END_TO_EVENT_START = 3;

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

    /** Thời điểm Sơ loại kết thúc = examAt + codingDurationHours. */
    public static LocalDateTime prelimEndAt(LocalDateTime prelimExamAt, int prelimCodingHours) {
        return prelimExamAt.plusHours(prelimCodingHours);
    }

    /**
     * Thời điểm sớm nhất cho {@code examAt} round Chung kết — khớp
     * {@link com.sealhackathon.api.rounds.service.impl.RoundServiceImpl} {@code ROUND_FINAL_EXAM_ORDER}.
     * = kết thúc Sơ loại + 1h.
     */
    public static LocalDateTime minFinalExamAt(LocalDateTime prelimExamAt, int prelimCodingHours) {
        return prelimEndAt(prelimExamAt, prelimCodingHours)
                .plusHours(MIN_FINAL_GAP_HOURS_AFTER_PRELIM);
    }

    /**
     * Thời điểm muộn nhất cho {@code examAt} round Chung kết.
     * = kết thúc Sơ loại + 2h (vd SL hết 12:00 → CK tối đa 14:00).
     */
    public static LocalDateTime maxFinalExamAt(LocalDateTime prelimExamAt, int prelimCodingHours) {
        return prelimEndAt(prelimExamAt, prelimCodingHours)
                .plusHours(MAX_FINAL_GAP_HOURS_AFTER_PRELIM);
    }

    /** Hạn nộp CK seed = examAt + {@link #DEFAULT_FINAL_CODING_HOURS}. */
    public static LocalDateTime finalSubmissionDeadline(LocalDateTime finalExamAt) {
        return submissionDeadline(finalExamAt, DEFAULT_FINAL_CODING_HOURS);
    }

    /** Mở nộp CK seed = examAt + 2/3 {@link #DEFAULT_FINAL_CODING_HOURS}. */
    public static LocalDateTime finalSubmissionOpen(LocalDateTime finalExamAt) {
        return submissionOpen(finalExamAt, DEFAULT_FINAL_CODING_HOURS);
    }
}
