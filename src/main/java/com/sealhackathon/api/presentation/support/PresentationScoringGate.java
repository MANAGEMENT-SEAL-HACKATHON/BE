package com.sealhackathon.api.presentation.support;

import com.sealhackathon.api.presentation.value_object.PresentationTimerPhase;

/**
 * Cửa chấm điểm gắn với phiên thuyết trình — tách khỏi {@code queue_status}.
 */
public final class PresentationScoringGate {

    private PresentationScoringGate() {
    }

    public static boolean isTimerOpenForScoring(PresentationTimerPhase phase) {
        if (phase == null) {
            return false;
        }
        return switch (phase) {
            case PRESENTING, QA, PAUSED, ENDED -> true;
            case IDLE, SETUP -> false;
        };
    }

    public static String scoringClosedMessage(PresentationTimerPhase phase) {
        if (phase == PresentationTimerPhase.SETUP) {
            return "Đội đang chuyển tiếp — chờ presentation controller bắt đầu timer";
        }
        return "Chưa bắt đầu phiên thuyết trình — presentation controller cần start timer";
    }
}
