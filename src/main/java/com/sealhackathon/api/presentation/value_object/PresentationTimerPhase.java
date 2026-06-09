package com.sealhackathon.api.presentation.value_object;

public enum PresentationTimerPhase {
    IDLE,
    /** Chuyển tiếp sau next — đội đã lên lượt (queue PRESENTING) nhưng chưa start timer / chưa cho chấm. */
    SETUP,
    PRESENTING,
    QA,
    PAUSED,
    ENDED
}
