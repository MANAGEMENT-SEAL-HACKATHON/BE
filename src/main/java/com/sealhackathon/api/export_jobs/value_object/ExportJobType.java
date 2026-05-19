package com.sealhackathon.api.export_jobs.value_object;

/**
 * Loại job xuất dữ liệu Hackathon.
 *
 * <ul>
 *   <li>{@code CSV_SCORES}     — bảng điểm chi tiết</li>
 *   <li>{@code CSV_RANKINGS}   — bảng xếp hạng đội/chapter</li>
 *   <li>{@code ANONYMIZED_RBL} — dataset Rubric/Bias/Leniency ẩn danh</li>
 *   <li>{@code FULL_REPORT}    — báo cáo toàn diện</li>
 * </ul>
 */
public enum ExportJobType {
    CSV_SCORES,
    CSV_RANKINGS,
    ANONYMIZED_RBL,
    FULL_REPORT
}
