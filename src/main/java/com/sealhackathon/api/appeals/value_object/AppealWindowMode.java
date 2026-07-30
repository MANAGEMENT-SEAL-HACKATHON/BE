package com.sealhackathon.api.appeals.value_object;

/**
 * Modes when publishing late (remaining minutes &lt; configured appeal window).
 */
public enum AppealWindowMode {
    DELAY_FINAL,
    SHRINK,
    SKIP
}
