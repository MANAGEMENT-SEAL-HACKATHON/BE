package com.sealhackathon.api.scores.value_object;

/**
 * Loại điểm.
 *
 * <ul>
 *   <li>{@code NORMAL}      — điểm chấm chính thức theo criterion.</li>
 *   <li>{@code CALIBRATION} — điểm trong phiên hiệu chuẩn (không tính tổng).</li>
 *   <li>{@code PENALTY}     — điểm trừ áp dụng cho đội (tiebreak hoặc kỷ luật).</li>
 * </ul>
 */
public enum ScoreType {
    NORMAL,
    CALIBRATION,
    PENALTY
}
