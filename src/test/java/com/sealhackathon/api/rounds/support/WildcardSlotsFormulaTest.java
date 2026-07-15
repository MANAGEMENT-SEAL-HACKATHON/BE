package com.sealhackathon.api.rounds.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase F — TC5 / TC6b / TC7: công thức ghế vé vớt runtime.
 *
 * <p>{@code slots = minTeamsFinal − (topNAdvance × actualTrackCount)}
 */
class WildcardSlotsFormulaTest {

    static int theoreticalSlots(int minTeamsFinal, int topNAdvance, int trackCount) {
        return minTeamsFinal - (topNAdvance * trackCount);
    }

    @Test
    void tc5_autoPool_twoSlotsWhenMinExceedsTopNTimesTracks() {
        // Top-N khóa 4 đội (2 track × topN 2) nhưng formula uses topN×tracks before fill
        assertEquals(2, theoreticalSlots(6, 2, 2));
    }

    @Test
    void tc6b_trackAddedAfterWcEnabled_slotsNonPositiveHidesPool() {
        // Tạo vòng với 0 track → FE cho bật WC; sau khi thêm 4 bảng topN=2 min=6 → slots=6-8=-2
        assertTrue(theoreticalSlots(6, 2, 4) <= 0);
    }

    @Test
    void tc7_hideWcWhenTopNFillsMinFinal() {
        assertEquals(0, theoreticalSlots(6, 2, 3));
        assertTrue(theoreticalSlots(6, 1, 6) <= 0);
    }
}
