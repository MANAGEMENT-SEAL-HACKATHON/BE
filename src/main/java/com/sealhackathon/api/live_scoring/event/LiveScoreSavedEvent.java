package com.sealhackathon.api.live_scoring.event;

import com.sealhackathon.api.scores.dto.response.ScoreResponse;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/** FR-18A — phát sau khi judge lưu điểm nháp. */
@Getter
public class LiveScoreSavedEvent extends ApplicationEvent {

    private final Integer roundId;
    private final Integer trackId;
    private final ScoreResponse score;

    public LiveScoreSavedEvent(Object source, Integer roundId, Integer trackId, ScoreResponse score) {
        super(source);
        this.roundId = roundId;
        this.trackId = trackId;
        this.score = score;
    }
}
