package com.sealhackathon.api.common.exception;

import org.springframework.http.HttpStatus;

/** 423 Locked — round đã khóa chấm điểm (FR-20A/20B). */
public class ScoringLockedException extends BaseException {

    public ScoringLockedException(String message) {
        super(ErrorCode.SCORING_LOCKED, message, HttpStatus.LOCKED);
    }
}
