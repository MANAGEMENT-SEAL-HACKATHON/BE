package com.sealhackathon.api.submissions.repository;

import org.springframework.stereotype.Component;

/**
 * Placeholder repository cho bảng {@code submissions} (chưa có entity ở phase MF-01).
 *
 * <p><b>Mục đích:</b> guard rule trong FR-03 (ROUND delete) tham chiếu tới số bài submit
 * của round; cần bean để inject mà không phải tạo entity tạm.
 *
 * <p><b>Hiện tại:</b> luôn trả {@code 0L} → guard "round có submission" PASS.
 * Khi entity {@code Submission} sẵn sàng, thay bằng JpaRepository thật.
 */
@Component
public class SubmissionPlaceholderRepository {

    /**
     * Đếm submission của round. TODO: replace bằng JpaRepository thật khi có entity Submission.
     */
    public long countByRoundId(Integer roundId) {
        return 0L;
    }
}
