package com.sealhackathon.api.presentation.support;

import com.sealhackathon.api.events.entity.PresentationSlot;
import com.sealhackathon.api.events.repository.PresentationSlotRepository;
import com.sealhackathon.api.presentation.value_object.PresentationTimerPhase;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.tracks.entity.Track;

/**
 * Lazy materialize: QA hết giờ tự nhiên → ENDED, không qua scoring guard.
 */
public final class PresentationQaTimeoutMaterializer {

    private PresentationQaTimeoutMaterializer() {
    }

    /**
     * @return true nếu đã chuyển phase sang ENDED
     */
    public static boolean materializeIfExpired(
            PresentationSlot slot,
            Track track,
            Round round,
            PresentationDurationResolver durationResolver,
            PresentationSlotRepository slotRepository) {
        if (slot == null || slot.getTimerPhase() != PresentationTimerPhase.QA) {
            return false;
        }
        int remaining = PresentationTimerCalculator.remainingSeconds(slot, track, round, durationResolver);
        if (remaining > 0) {
            return false;
        }
        slot.setTimerPhase(PresentationTimerPhase.ENDED);
        slotRepository.save(slot);
        return true;
    }
}
