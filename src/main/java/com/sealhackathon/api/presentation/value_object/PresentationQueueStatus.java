package com.sealhackathon.api.presentation.value_object;

public enum PresentationQueueStatus {
    WAITING,
    PRESENTING,
    DONE,
    /** Đội vắng mặt khi tới lượt — bỏ qua, không chờ chấm. */
    SKIPPED,
    ELIMINATED
}
