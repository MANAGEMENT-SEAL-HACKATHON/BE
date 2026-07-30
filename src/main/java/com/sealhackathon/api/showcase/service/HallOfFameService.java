package com.sealhackathon.api.showcase.service;

import com.sealhackathon.api.showcase.dto.response.HallOfFameEntryResponse;

import java.util.List;

public interface HallOfFameService {

    /** Snapshot FIRST prize champion for a finished hackathon. Idempotent per hackathon. */
    void snapshotFromFinishedHackathon(Integer hackathonId);

    /** One-shot backfill for all FINISHED hackathons missing a hall-of-fame row. */
    int backfillFinishedHackathons();

    List<HallOfFameEntryResponse> listPublic(Integer year);

    List<HallOfFameEntryResponse> listByHackathon(Integer hackathonId);
}
