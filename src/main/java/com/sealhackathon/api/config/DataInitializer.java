package com.sealhackathon.api.config;

import com.sealhackathon.api.config.seed.DevSeedCleanup;
import com.sealhackathon.api.config.seed.E2eWorkflowDataSeeder;
import com.sealhackathon.api.config.seed.Gd1DataSeeder;
import com.sealhackathon.api.config.seed.Gd3PrelimOpenDataSeeder;
import com.sealhackathon.api.config.seed.Gd4AdvanceReadyDataSeeder;
import com.sealhackathon.api.config.seed.Gd5FinalRoundDataSeeder;
import com.sealhackathon.api.config.seed.Gd6PendingConfirmDataSeeder;
import com.sealhackathon.api.config.seed.Gd1SeedConstants;
import com.sealhackathon.api.config.seed.HackathonDevSeedHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Dev profile: seed FINISHED archive + hackathon E2E GĐ1→GĐ6.
 *
 * <p>Doc: {@code docs/testing/dev-seed-guide.md}
 */
@Slf4j
@Component
@Profile("dev")
@Order(2)
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final DevSeedCleanup devSeedCleanup;
    private final Gd1DataSeeder gd1DataSeeder;
    private final E2eWorkflowDataSeeder e2eWorkflowDataSeeder;
    private final Gd3PrelimOpenDataSeeder gd3PrelimOpenDataSeeder;
    private final Gd4AdvanceReadyDataSeeder gd4AdvanceReadyDataSeeder;
    private final Gd5FinalRoundDataSeeder gd5FinalRoundDataSeeder;
    private final Gd6PendingConfirmDataSeeder gd6PendingConfirmDataSeeder;
    private final HackathonDevSeedHelper hackathonDevSeedHelper;

    @Override
    public void run(String... args) {
        devSeedCleanup.purgeDeprecatedHackathons();

        gd1DataSeeder.repairSeededTimeline();
        gd1DataSeeder.repairSeededFinishedHackathon();
        gd1DataSeeder.repairSeededCriteriaAndTracks();
        gd1DataSeeder.repairDevUserPasswords();

        if (gd1DataSeeder.isAlreadySeeded()) {
            log.info("[DataInitializer] Seed GĐ1 đã có (slug={}), bỏ qua tạo mới.",
                    Gd1SeedConstants.SLUG_ONGOING);
        } else {
            gd1DataSeeder.seedAll();
        }

        e2eWorkflowDataSeeder.ensureSeed();
        e2eWorkflowDataSeeder.repairForGd5FullChainRetest();
        gd3PrelimOpenDataSeeder.ensureSeed();
        gd4AdvanceReadyDataSeeder.ensureSeed();
        gd5FinalRoundDataSeeder.ensureSeed();
        gd6PendingConfirmDataSeeder.repairForFullChainRetest();
        gd6PendingConfirmDataSeeder.ensureSeed();
        hackathonDevSeedHelper.repairAllDevHackathonRoundSchedules();
        e2eWorkflowDataSeeder.repairForGd2Testing();
        gd3PrelimOpenDataSeeder.repairForFeTesting();
        gd4AdvanceReadyDataSeeder.repairForFeTesting();
        gd5FinalRoundDataSeeder.repairForFeTesting();
        gd6PendingConfirmDataSeeder.repairForFeTesting();
        hackathonDevSeedHelper.repairAllDevHackathonMilestoneEvents();
    }
}

