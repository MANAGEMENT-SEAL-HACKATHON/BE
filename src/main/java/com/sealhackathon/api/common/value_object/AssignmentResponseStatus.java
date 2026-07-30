package com.sealhackathon.api.common.value_object;

/**
 * Response of a mentor/judge to a personnel assignment.
 *
 * <p><b>Default must be {@link #ACCEPTED}</b> — existing activate/scoring gates treat
 * assignments as active; {@link #PENDING} would break all pre-existing data and tests.
 */
public enum AssignmentResponseStatus {
    PENDING,
    ACCEPTED,
    DECLINED
}
