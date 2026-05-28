package com.sealhackathon.api.common.response;

/**
 * Mã cảnh báo mềm MF-03 (GĐ3–GĐ5) — trả trong {@code warnings[]} của {@link ApiResponse}, không chặn 2xx.
 */
public final class WarningCode {

    private WarningCode() {}

    // ---------- MF-03 GĐ3–GĐ5 (GD03 §7.2) ----------
    /** Judge đã tham gia Sơ loại — đề xuất không phân CK (FR-31). */
    public static final String JUDGE_PARTICIPATED_IN_PRELIM = "JUDGE_PARTICIPATED_IN_PRELIM";
    /** Tổng đội advance &lt; min_teams_final — cần Wild Card (FR-29). */
    public static final String MIN_TEAMS_NOT_REACHED = "MIN_TEAMS_NOT_REACHED";
    /** Có Track chưa chấm đủ trước lock (FR-26). */
    public static final String PARTIAL_SCORING_BEFORE_LOCK = "PARTIAL_SCORING_BEFORE_LOCK";
    /** Xếp hạng khi chưa chấm đủ tiêu chí (FR-27). */
    public static final String INCOMPLETE_SCORING_IN_RANKING = "INCOMPLETE_SCORING_IN_RANKING";
}
