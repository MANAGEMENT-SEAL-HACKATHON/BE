package com.sealhackathon.api.presentation.service.impl;

import com.sealhackathon.api.common.exception.ConflictException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.presentation.dto.request.PresentationControllerGrantRequest;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Lightweight FAIL-02 / race-guard unit coverage for expectedControllerJudgeId compare.
 */
class PresentationControllerRaceGuardTest {

    @Test
    void expectedMismatch_throwsControllerConflict() {
        Integer current = 10;
        Integer expected = 11;
        assertThatThrownBy(() -> assertExpected(current, expected))
                .isInstanceOf(ConflictException.class)
                .extracting(ex -> ((ConflictException) ex).getCode())
                .isEqualTo(ErrorCode.CONTROLLER_CONFLICT);
    }

    @Test
    void expectedMatch_ok() {
        assertExpected(10, 10);
    }

    @Test
    void expectedNull_skipsRaceCheck() {
        assertExpected(10, null);
    }

    private static void assertExpected(Integer currentId, Integer expectedId) {
        if (expectedId == null) {
            return;
        }
        if (expectedId == 0) {
            if (currentId != null) {
                throw new ConflictException(ErrorCode.CONTROLLER_CONFLICT,
                        "Controller đã được chuyển bởi người khác",
                        java.util.Map.of("currentControllerJudgeId", currentId));
            }
            return;
        }
        if (!Objects.equals(currentId, expectedId)) {
            throw new ConflictException(ErrorCode.CONTROLLER_CONFLICT,
                    "Controller đã được chuyển bởi người khác",
                    java.util.Map.of("currentControllerJudgeId", currentId,
                            "expectedControllerJudgeId", expectedId));
        }
    }

    @Test
    void grantRequest_holdsModeAndExpected() {
        PresentationControllerGrantRequest req = PresentationControllerGrantRequest.builder()
                .judgeId(5)
                .expectedControllerJudgeId(3)
                .mode("TAKEOVER")
                .build();
        assertThat(req.getMode()).isEqualTo("TAKEOVER");
        assertThat(req.getExpectedControllerJudgeId()).isEqualTo(3);
    }
}
