package com.sealhackathon.api.hackathons.service;

/**
 * Đồng bộ {@code hackathons.event_start} / {@code event_end} từ lịch các round.
 */
public interface HackathonRoundTimelineSyncService {

    void syncFromRounds(Integer hackathonId);
}
