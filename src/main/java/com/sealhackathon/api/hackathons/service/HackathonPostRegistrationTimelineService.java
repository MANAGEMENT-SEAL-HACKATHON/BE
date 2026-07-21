package com.sealhackathon.api.hackathons.service;

import com.sealhackathon.api.hackathons.entity.Hackathon;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * @deprecated Logic chuyển sang {@link CompetitionScheduleAdjustService}.
 * Giữ bean để tương thích inject cũ; ủy quyền sang service mới.
 */
@Deprecated
@Service
@RequiredArgsConstructor
public class HackathonPostRegistrationTimelineService {

    private final CompetitionScheduleAdjustService competitionScheduleAdjustService;

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> compressAfterRegistrationClosed(Hackathon hackathon) {
        return competitionScheduleAdjustService.compressAfterRegistrationClosed(hackathon);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> applyChosenPrelimExam(Hackathon hackathon, LocalDateTime newPrelimExamAt) {
        return competitionScheduleAdjustService.apply(hackathon, newPrelimExamAt, true);
    }
}
