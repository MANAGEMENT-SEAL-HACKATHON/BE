package com.sealhackathon.api.config;

import com.sealhackathon.api.config.seed.DevSeedCatalog;
import com.sealhackathon.api.config.seed.DevSeedCleanup;
import com.sealhackathon.api.config.seed.E2eDevFlowGuard;
import com.sealhackathon.api.config.seed.E2eWorkflowDataSeeder;
import com.sealhackathon.api.config.seed.Gd1DataSeeder;
import com.sealhackathon.api.config.seed.Gd1SeedConstants;
import com.sealhackathon.api.config.seed.HackathonDevSeedHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Dev profile: 1 happy-path hackathon ({@code seal-e2e-2026}) GĐ2 pre-lottery.
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
    private final E2eDevFlowGuard e2eDevFlowGuard;
    private final HackathonDevSeedHelper hackathonDevSeedHelper;

    @Override
    public void run(String... args) {
        // 1. Purge deprecated / mid-stage / former happy snapshots
        devSeedCleanup.purgeDeprecatedHackathons();

        // 2. Staff users + GĐ1 structure
        gd1DataSeeder.ensureSeedUsers();
        gd1DataSeeder.repairDevUserPasswords();

        boolean frozen = e2eDevFlowGuard.isE2eFlowFrozen();
        // Always — only flips is_tiebreaker_priority; safe when GĐ3+ frozen
        gd1DataSeeder.backfillTiebreakerPriorityFlags();
        if (frozen) {
            e2eDevFlowGuard.logSkip("timeline / criteria-track / GĐ2–GĐ5 repairs");
        } else {
            gd1DataSeeder.repairSeededTimeline();
            gd1DataSeeder.repairSeededFinishedHackathon();
            gd1DataSeeder.repairSeededCriteriaAndTracks();
        }

        if (gd1DataSeeder.isAlreadySeeded()) {
            log.info("[DataInitializer] Seed GĐ1 đã có (slug={}), bỏ qua tạo mới.",
                    Gd1SeedConstants.SLUG_ONGOING);
        } else {
            gd1DataSeeder.seedAll();
        }

        hackathonDevSeedHelper.repairRemoveGuestJudgeFromAllDevPrelimTracks();

        e2eWorkflowDataSeeder.ensureSeed();

        // 3. Repair lịch / baseline — skip destructive when GĐ3+ frozen
        if (!frozen) {
            hackathonDevSeedHelper.repairAllDevHackathonRoundSchedules(false);
            e2eWorkflowDataSeeder.repairForGd5FullChainRetest();
            e2eWorkflowDataSeeder.repairForGd2Testing();
            hackathonDevSeedHelper.repairAllDevHackathonMilestoneEvents(false);
        } else {
            hackathonDevSeedHelper.repairAllDevHackathonRoundSchedules(true);
            hackathonDevSeedHelper.repairAllDevHackathonMilestoneEvents(true);
        }

        hackathonDevSeedHelper.backfillReleasedPrelimTrackProblems();
        hackathonDevSeedHelper.backfillReleasedFinalRoundProblems();
        hackathonDevSeedHelper.backfillSetupProblemPdfs();
        hackathonDevSeedHelper.backfillTiebreakerPriorityFlags();
        hackathonDevSeedHelper.repairAllHackathonBanners();

        log.info("[DataInitializer] Dev seed sẵn sàng — {} happy slug: {} | frozen={} forceGd2Reset={}",
                DevSeedCatalog.ALL_DEV_HACKATHON_SLUGS.length,
                String.join(", ", DevSeedCatalog.ALL_DEV_HACKATHON_SLUGS),
                e2eDevFlowGuard.isE2eFlowFrozen(),
                e2eDevFlowGuard.isForceGd2Reset());
    }
}
