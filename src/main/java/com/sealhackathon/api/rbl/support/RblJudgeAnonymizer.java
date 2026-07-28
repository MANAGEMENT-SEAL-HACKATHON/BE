package com.sealhackathon.api.rbl.support;

import java.util.Objects;

/**
 * Pseudonym ổn định theo (hackathonId, judgeId) — thuật toán không đổi so với ExportCsvBuilder cũ.
 */
public final class RblJudgeAnonymizer {

    private RblJudgeAnonymizer() {}

    public static String anonymize(Integer hackathonId, Integer judgeId) {
        if (judgeId == null) {
            return "";
        }
        int hash = Objects.hash(hackathonId, judgeId);
        return "J" + Integer.toUnsignedString(hash, 36).toUpperCase();
    }
}
