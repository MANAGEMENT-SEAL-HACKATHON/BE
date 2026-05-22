package com.sealhackathon.api.events.service;

import com.sealhackathon.api.common.exception.BusinessRuleException;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Ràng buộc timeline hackathon: event milestone + {@code round.examAt} (Fall/Spring PDF).
 */
public interface HackathonTimelineService {

    /**
     * Validate {@code examAt} vs KICKOFF / PRESENTATION / AWARDS khi event đã tồn tại.
     */
    void validateRoundExamAt(Integer hackathonId, boolean isFinal, LocalDateTime examAt);

    /**
     * Kiểm tra mọi round của hackathon — dùng readiness & sau PUT event milestone.
     *
     * @return danh sách lỗi (rỗng nếu hợp lệ)
     */
    List<BusinessRuleException> collectRoundExamAtViolations(Integer hackathonId);

    /**
     * Ném lỗi đầu tiên nếu có vi phạm (PUT event).
     */
    void assertAllRoundsExamAtValid(Integer hackathonId);
}
