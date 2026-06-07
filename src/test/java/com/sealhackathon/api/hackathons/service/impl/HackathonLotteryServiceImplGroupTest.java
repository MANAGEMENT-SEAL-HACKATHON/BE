package com.sealhackathon.api.hackathons.service.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HackathonLotteryServiceImplGroupTest {

    @Test
    void fourTeamsMaxEight_usesTwoGroups() {
        assertEquals(2, HackathonLotteryServiceImpl.resolveGroupCount(4, 8));
        assertEquals(2, HackathonLotteryServiceImpl.resolveGroupCount(2, 8));
    }

    @Test
    void singleTeam_oneGroup() {
        assertEquals(1, HackathonLotteryServiceImpl.resolveGroupCount(1, 8));
    }

    @Test
    void overflowNeedsThirdGroup() {
        assertEquals(3, HackathonLotteryServiceImpl.resolveGroupCount(17, 8));
    }
}
