package com.sealhackathon.api.rounds.dto.request;

import com.sealhackathon.api.rounds.value_object.ActivateScheduleMode;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * PATCH /api/v1/rounds/{id}/activate — note + optional schedule mode.
 * Chỉ {@link ActivateScheduleMode#KEEP} được chấp nhận trên activate;
 * dời lịch dùng competition-schedule/adjust hoặc đóng ĐK sớm.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivateRoundRequest {

    @Size(max = 1000)
    private String note;

    /**
     * Mặc định {@link ActivateScheduleMode#KEEP} khi null (không surprise thí sinh).
     */
    private ActivateScheduleMode scheduleMode;

    /** Bắt buộc khi {@link ActivateScheduleMode#RESCHEDULE} (bị từ chối trên activate). */
    private LocalDateTime newExamAt;
}
