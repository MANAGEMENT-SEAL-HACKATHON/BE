package com.sealhackathon.api.presentation.support;

import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.tracks.entity.Track;
import org.springframework.stereotype.Component;

@Component
public class PresentationDurationResolver {

    private static final int DEFAULT_PRESENTATION_MINUTES = 10;
    private static final int DEFAULT_QA_MINUTES = 5;

    public int presentationMinutes(Track track, Round round) {
        if (track != null && track.getPresentationMinutes() != null) {
            return track.getPresentationMinutes();
        }
        if (round.getDefaultPresentationMinutes() != null) {
            return round.getDefaultPresentationMinutes();
        }
        return DEFAULT_PRESENTATION_MINUTES;
    }

    public int qaMinutes(Track track, Round round) {
        if (track != null && track.getQaMinutes() != null) {
            return track.getQaMinutes();
        }
        if (round.getDefaultQaMinutes() != null) {
            return round.getDefaultQaMinutes();
        }
        return DEFAULT_QA_MINUTES;
    }

    public int slotMinutes(Track track, Round round) {
        return presentationMinutes(track, round) + qaMinutes(track, round);
    }
}
