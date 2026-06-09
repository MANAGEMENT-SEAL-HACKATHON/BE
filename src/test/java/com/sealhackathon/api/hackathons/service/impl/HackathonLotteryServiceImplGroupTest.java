package com.sealhackathon.api.hackathons.service.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HackathonLotteryServiceImplGroupTest {

    @Test
    void trackSequenceIndex_mapsToRomanTableLetters() {
        assertEquals("Bảng A", HackathonLotteryServiceImpl.assignedGroupForTrackSequenceIndex(0));
        assertEquals("Bảng B", HackathonLotteryServiceImpl.assignedGroupForTrackSequenceIndex(1));
        assertEquals("Bảng C", HackathonLotteryServiceImpl.assignedGroupForTrackSequenceIndex(2));
        assertEquals("Bảng D", HackathonLotteryServiceImpl.assignedGroupForTrackSequenceIndex(3));
    }

    @Test
    void roundRobin_distributesEvenlyAcrossTracks() {
        assertArrayEquals(new int[]{6, 6, 6, 6},
                HackathonLotteryServiceImpl.roundRobinTeamCounts(24, 4));
        assertArrayEquals(new int[]{6, 6, 6, 5},
                HackathonLotteryServiceImpl.roundRobinTeamCounts(23, 4));
        assertArrayEquals(new int[]{2, 2, 1},
                HackathonLotteryServiceImpl.roundRobinTeamCounts(5, 3));
    }

    @Test
    void trackSequenceIndex_rejectsBeyondZ() {
        assertThrows(IllegalArgumentException.class,
                () -> HackathonLotteryServiceImpl.assignedGroupForTrackSequenceIndex(26));
    }
}
