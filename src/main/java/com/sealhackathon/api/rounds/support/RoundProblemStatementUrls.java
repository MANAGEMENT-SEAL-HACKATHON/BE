package com.sealhackathon.api.rounds.support;

import com.sealhackathon.api.rounds.entity.Round;
import org.springframework.util.StringUtils;

public final class RoundProblemStatementUrls {

    private RoundProblemStatementUrls() {
    }

    public static String publicPath(Integer roundId) {
        return "/api/v1/rounds/" + roundId + "/problem-statement";
    }

    public static String resolveForResponse(Round round) {
        if (round == null || !RoundProblemStatementStorage.hasStoredFile(round)) {
            return null;
        }
        return publicPath(round.getId());
    }
}
