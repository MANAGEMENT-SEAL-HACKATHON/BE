package com.sealhackathon.api.scores.repository;

import org.springframework.stereotype.Component;

/**
 * Placeholder repository cho bảng {@code scores} (chưa có entity ở phase MF-01).
 *
 * <p><b>Mục đích:</b> guard rule trong FR-04 (CRITERIA update/delete/clone replace) tham chiếu
 * tới số score đã ghi cho criterion; cần bean để inject mà không phải tạo entity tạm.
 *
 * <p><b>Hiện tại:</b> luôn trả {@code 0L} → guard "criterion có score" PASS.
 * Khi entity {@code Score} sẵn sàng, thay bằng JpaRepository thật.
 */
@Component
public class ScorePlaceholderRepository {

    /**
     * Đếm score đã chấm cho criterion. TODO: replace khi có entity Score.
     */
    public long countByCriteriaId(Integer criteriaId) {
        return 0L;
    }

    /**
     * Đếm score đã chấm cho mọi criterion thuộc round. TODO: replace khi có entity Score.
     */
    public long countByRoundId(Integer roundId) {
        return 0L;
    }
}
