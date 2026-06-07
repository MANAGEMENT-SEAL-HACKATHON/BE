package com.sealhackathon.api.presentation.service;

/**
 * Cập nhật {@code presentation_slots} khi {@code round.examAt} thay đổi.
 */
public interface PresentationSlotCascadeService {

    void rescheduleForRound(Integer roundId);
}
