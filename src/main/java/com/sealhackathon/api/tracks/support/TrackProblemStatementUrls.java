package com.sealhackathon.api.tracks.support;

import com.sealhackathon.api.tracks.entity.Track;

public final class TrackProblemStatementUrls {

    private TrackProblemStatementUrls() {
    }

    public static String coordinatorPath(Integer trackId) {
        return "/api/v1/tracks/" + trackId + "/problem-statement";
    }

    public static String resolveForResponse(Track track) {
        if (track == null || !TrackProblemStatementStorage.hasStoredFile(track)) {
            return null;
        }
        return coordinatorPath(track.getId());
    }
}
