package com.sealhackathon.api.tracks.support;

import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.value_object.RoundType;
import com.sealhackathon.api.tracks.entity.Track;

import java.util.Map;

/**
 * App-layer guards cho Track thuộc Round Sơ loại/Bán kết (mf01 §6, workflow GĐ1).
 */
public final class TrackRoundRules {

    private TrackRoundRules() {
    }

    /**
     * Mentor/Judge Sơ loại chỉ gắn Track trong round không phải Chung kết.
     */
    public static void requirePreliminaryAssignmentTrack(Track track) {
        if (track == null) {
            throw new BusinessRuleException(ErrorCode.INVALID_STATE,
                    "Track không hợp lệ",
                    Map.of());
        }
        Round round = track.getRound();
        if (round == null) {
            throw new BusinessRuleException(ErrorCode.INVALID_STATE,
                    "Track #%d chưa gắn Round".formatted(track.getId()),
                    Map.of("trackId", track.getId()));
        }
        if (Boolean.TRUE.equals(round.getIsFinal()) || round.getRoundType() == RoundType.FINAL) {
            throw new BusinessRuleException(ErrorCode.DESIGN_VIOLATION,
                    "Track #%d thuộc Round Chung kết — chỉ phân công Mentor/Judge Sơ loại trên Track Sơ loại"
                            .formatted(track.getId()),
                    Map.of("trackId", track.getId(), "roundId", round.getId(),
                            "roundType", round.getRoundType()));
        }
    }
}
