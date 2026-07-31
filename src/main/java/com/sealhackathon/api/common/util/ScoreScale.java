package com.sealhackathon.api.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class ScoreScale {

    private ScoreScale() {
    }

    public static double round2(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    public static Double round2Nullable(Double value) {
        return value == null ? null : round2(value);
    }
}
