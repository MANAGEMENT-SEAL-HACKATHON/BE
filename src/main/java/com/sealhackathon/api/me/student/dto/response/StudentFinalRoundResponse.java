package com.sealhackathon.api.me.student.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** GĐ5 — thông tin vòng Chung kết cho student (không cần quyền coordinator rounds list). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentFinalRoundResponse {

    private Integer roundId;
    private String name;
    private Boolean isActive;
    private Boolean scoringLocked;
    private LocalDateTime submissionDeadline;
    private Boolean problemReleased;
}
