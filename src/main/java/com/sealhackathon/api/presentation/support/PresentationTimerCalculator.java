package com.sealhackathon.api.presentation.support;

import com.sealhackathon.api.events.entity.PresentationSlot;
import com.sealhackathon.api.presentation.value_object.PresentationTimerPhase;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.tracks.entity.Track;

import java.time.Duration;
import java.time.LocalDateTime;

public final class PresentationTimerCalculator {

    private PresentationTimerCalculator() {}

    public static int remainingSeconds(
            PresentationSlot slot,
            Track track,
            Round round,
            PresentationDurationResolver durationResolver) {
        if (slot == null || slot.getTimerPhase() == null
                || slot.getTimerPhase() == PresentationTimerPhase.IDLE
                || slot.getTimerPhase() == PresentationTimerPhase.SETUP) {
            return durationResolver.presentationMinutes(track, round) * 60;
        }
        if (slot.getTimerPhase() == PresentationTimerPhase.ENDED) {
            return 0;
        }

        // int presentationSeconds = durationResolver.presentationMinutes(track, round) * 60;
        // int qaSeconds = durationResolver.qaMinutes(track, round) * 60;
        // Thay vì nhân phút với 60, ép cứng số giây trực tiếp để test:
        int presentationSeconds = 20; // 20 giây thuyết trình
        int qaSeconds = 10;           // 10 giây Q&A
        int paused = slot.getPausedAccumulatedSeconds() != null ? slot.getPausedAccumulatedSeconds() : 0;
        LocalDateTime now = LocalDateTime.now();

        if (slot.getTimerPhase() == PresentationTimerPhase.PAUSED) {
            return remainingForPaused(slot, presentationSeconds, qaSeconds, paused, now);
        }
        if (slot.getTimerPhase() == PresentationTimerPhase.PRESENTING) {
            if (slot.getPresentationStartedAt() == null) {
                return presentationSeconds;
            }
            long elapsed = Duration.between(slot.getPresentationStartedAt(), now).getSeconds() - paused;
            return Math.max(0, presentationSeconds - (int) elapsed);
        }
        if (slot.getTimerPhase() == PresentationTimerPhase.QA) {
            if (slot.getQaStartedAt() == null) {
                return qaSeconds;
            }
            long elapsed = Duration.between(slot.getQaStartedAt(), now).getSeconds() - paused;
            return Math.max(0, qaSeconds - (int) elapsed);
        }
        return 0;
    }

    private static int remainingForPaused(
            PresentationSlot slot,
            int presentationSeconds,
            int qaSeconds,
            int paused,
            LocalDateTime now) {
        PresentationTimerPhase before = slot.getTimerPhaseBeforePause();

        if (before == PresentationTimerPhase.QA && slot.getQaStartedAt() != null) {
            // FIX LỖI BÓNG MA: Bỏ hoàn toàn việc tính thêm biến "extra"
            // Khi đã Pause, thời gian trôi qua chốt cứng tại thời điểm pausedAt.
            long elapsed = Duration.between(slot.getQaStartedAt(), slot.getPausedAt() != null ? slot.getPausedAt() : now).getSeconds() - paused;
            return Math.max(0, qaSeconds - (int) elapsed);
        }
        if (slot.getPresentationStartedAt() != null) {
            // Đóng băng thời gian đối với pha PRESENTING
            long elapsed = Duration.between(slot.getPresentationStartedAt(), slot.getPausedAt() != null ? slot.getPausedAt() : now).getSeconds() - paused;
            return Math.max(0, presentationSeconds - (int) elapsed);
        }

        return before == PresentationTimerPhase.QA ? qaSeconds : presentationSeconds;
    }
}