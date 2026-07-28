package com.sealhackathon.api.hackathons.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Chỉnh tay các mốc cascade — phải thỏa ràng buộc GĐ1
 * (WS/KO trong gap regEnd→eventStart, khác ngày; CK trong cửa sổ 1–2h sau SL; Awards sau hạn CK).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompetitionScheduleOverrides {

    private LocalDateTime workshopStartsAt;
    private LocalDateTime workshopEndsAt;
    private LocalDateTime kickoffStartsAt;
    private LocalDateTime kickoffEndsAt;
    /** null → dùng maxFinalExamAt (seed). */
    private LocalDateTime finalExamAt;
    private LocalDateTime awardsStartsAt;
    private LocalDateTime awardsEndsAt;
}
