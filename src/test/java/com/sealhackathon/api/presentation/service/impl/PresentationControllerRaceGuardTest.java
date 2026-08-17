package com.sealhackathon.api.presentation.service.impl;

import com.sealhackathon.api.common.exception.ConflictException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.presentation.dto.request.PresentationControllerGrantRequest;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
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

    @Test
    void expectedZero_whenUnassigned_ok() {
        assertThatCode(() -> assertExpected(null, 0)).doesNotThrowAnyException();
    }

    @Test
    void expectedAutoDefaultMatch_ok() {
        // FE sends resolved AUTO_DEFAULT id; BE must compare against resolved, not override-only
        assertThatCode(() -> assertExpected(5, 5)).doesNotThrowAnyException();
    }

    @Test
    void expectedMismatch_whenCurrentNull_throwsConflictNotNpe() {
        // Regression: Map.of(null value) used to NPE instead of returning 409
        assertThatThrownBy(() -> assertExpected(null, 11))
                .isInstanceOf(ConflictException.class)
                .satisfies(ex -> {
                    ConflictException ce = (ConflictException) ex;
                    assertThat(ce.getCode()).isEqualTo(ErrorCode.CONTROLLER_CONFLICT);
                    assertThat(ce.getDetails()).containsEntry("expectedControllerJudgeId", 11);
                    assertThat(ce.getDetails()).doesNotContainKey("currentControllerJudgeId");
                });
    }

    @Test
    void conflictDetails_skipsNullEntries() {
        Map<String, Object> details = PresentationControllerServiceImpl.conflictDetails(null, 7);
        assertThat(details).containsOnlyKeys("expectedControllerJudgeId");
        assertThat(details.get("expectedControllerJudgeId")).isEqualTo(7);
    }

    private static void assertExpected(Integer currentId, Integer expectedId) {
        if (expectedId == null) {
            return;
        }
        if (expectedId == 0) {
            if (currentId != null) {
                throw new ConflictException(ErrorCode.CONTROLLER_CONFLICT,
                        "Controller đã được chuyển bởi người khác",
                        PresentationControllerServiceImpl.conflictDetails(currentId, expectedId));
            }
            return;
        }
        if (!Objects.equals(currentId, expectedId)) {
            throw new ConflictException(ErrorCode.CONTROLLER_CONFLICT,
                    "Controller đã được chuyển bởi người khác",
                    PresentationControllerServiceImpl.conflictDetails(currentId, expectedId));
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
