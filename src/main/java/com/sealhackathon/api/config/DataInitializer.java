package com.sealhackathon.api.config;

import com.sealhackathon.api.config.seed.DevSeedCleanup;
import com.sealhackathon.api.config.seed.E2eWorkflowDataSeeder;
import com.sealhackathon.api.config.seed.Gd1DataSeeder;
import com.sealhackathon.api.config.seed.Gd3CalibrationTimerDataSeeder;
import com.sealhackathon.api.config.seed.Gd3EdgeErrorsDataSeeder;
import com.sealhackathon.api.config.seed.Gd3LateReviewDataSeeder;
import com.sealhackathon.api.config.seed.Gd3PrelimOpenDataSeeder;
import com.sealhackathon.api.config.seed.Gd3ScoringLiveDataSeeder;
import com.sealhackathon.api.config.seed.Gd3TiebreakHybridDataSeeder;
import com.sealhackathon.api.config.seed.Gd4AdvanceReadyDataSeeder;
import com.sealhackathon.api.config.seed.Gd4CkActivateReadyDataSeeder;
import com.sealhackathon.api.config.seed.Gd4EdgeErrorsDataSeeder;
import com.sealhackathon.api.config.seed.Gd4PublishedDataSeeder;
import com.sealhackathon.api.config.seed.Gd4TiebreakGateDataSeeder;
import com.sealhackathon.api.config.seed.Gd4TiebreakResolvedDataSeeder;
import com.sealhackathon.api.config.seed.Gd4WildcardResolvedDataSeeder;
import com.sealhackathon.api.config.seed.Gd5CalibrationTimerDataSeeder;
import com.sealhackathon.api.config.seed.Gd5EdgeErrorsDataSeeder;
import com.sealhackathon.api.config.seed.Gd5FinalRoundDataSeeder;
import com.sealhackathon.api.config.seed.Gd5LateHardlockDataSeeder;
import com.sealhackathon.api.config.seed.Gd5ScoringLiveDataSeeder;
import com.sealhackathon.api.config.seed.Gd5SubmitOpenDataSeeder;
import com.sealhackathon.api.config.seed.Gd6ConfirmReadyDataSeeder;
import com.sealhackathon.api.config.seed.Gd6EdgeErrorsDataSeeder;
import com.sealhackathon.api.config.seed.Gd6FinishedExportDataSeeder;
import com.sealhackathon.api.config.seed.Gd6PendingConfirmDataSeeder;
import com.sealhackathon.api.config.seed.Gd6PrizesEmptyDataSeeder;
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
    private final Gd3LateReviewDataSeeder gd3LateReviewDataSeeder;
    private final Gd3ScoringLiveDataSeeder gd3ScoringLiveDataSeeder;
    private final Gd3TiebreakHybridDataSeeder gd3TiebreakHybridDataSeeder;
    private final Gd3EdgeErrorsDataSeeder gd3EdgeErrorsDataSeeder;
    private final Gd3CalibrationTimerDataSeeder gd3CalibrationTimerDataSeeder;
    private final Gd4AdvanceReadyDataSeeder gd4AdvanceReadyDataSeeder;
    private final Gd4PublishedDataSeeder gd4PublishedDataSeeder;
    private final Gd4TiebreakGateDataSeeder gd4TiebreakGateDataSeeder;
    private final Gd4CkActivateReadyDataSeeder gd4CkActivateReadyDataSeeder;
    private final Gd4EdgeErrorsDataSeeder gd4EdgeErrorsDataSeeder;
    private final Gd4WildcardResolvedDataSeeder gd4WildcardResolvedDataSeeder;
    private final Gd4TiebreakResolvedDataSeeder gd4TiebreakResolvedDataSeeder;
    private final Gd5FinalRoundDataSeeder gd5FinalRoundDataSeeder;
    private final Gd5SubmitOpenDataSeeder gd5SubmitOpenDataSeeder;
    private final Gd5ScoringLiveDataSeeder gd5ScoringLiveDataSeeder;
    private final Gd5CalibrationTimerDataSeeder gd5CalibrationTimerDataSeeder;
    private final Gd5EdgeErrorsDataSeeder gd5EdgeErrorsDataSeeder;
    private final Gd5LateHardlockDataSeeder gd5LateHardlockDataSeeder;
    private final Gd6PendingConfirmDataSeeder gd6PendingConfirmDataSeeder;
    private final Gd6PrizesEmptyDataSeeder gd6PrizesEmptyDataSeeder;
    private final Gd6ConfirmReadyDataSeeder gd6ConfirmReadyDataSeeder;
    private final Gd6FinishedExportDataSeeder gd6FinishedExportDataSeeder;
    private final Gd6EdgeErrorsDataSeeder gd6EdgeErrorsDataSeeder;
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
        gd3LateReviewDataSeeder.ensureSeed();
        gd3ScoringLiveDataSeeder.ensureSeed();
        gd3TiebreakHybridDataSeeder.ensureSeed();
        gd3EdgeErrorsDataSeeder.ensureSeed();
        gd3CalibrationTimerDataSeeder.ensureSeed();
        gd4AdvanceReadyDataSeeder.ensureSeed();
        gd4PublishedDataSeeder.ensureSeed();
        gd4TiebreakGateDataSeeder.ensureSeed();
        gd4CkActivateReadyDataSeeder.ensureSeed();
        gd4EdgeErrorsDataSeeder.ensureSeed();
        gd4WildcardResolvedDataSeeder.ensureSeed();
        gd4TiebreakResolvedDataSeeder.ensureSeed();
        gd5FinalRoundDataSeeder.ensureSeed();
        gd5SubmitOpenDataSeeder.ensureSeed();
        gd5ScoringLiveDataSeeder.ensureSeed();
        gd5CalibrationTimerDataSeeder.ensureSeed();
        gd5EdgeErrorsDataSeeder.ensureSeed();
        gd5LateHardlockDataSeeder.ensureSeed();
        gd6PendingConfirmDataSeeder.repairForFullChainRetest();
        gd6PendingConfirmDataSeeder.ensureSeed();
        gd6PrizesEmptyDataSeeder.ensureSeed();
        gd6ConfirmReadyDataSeeder.ensureSeed();
        gd6FinishedExportDataSeeder.ensureSeed();
        gd6EdgeErrorsDataSeeder.ensureSeed();
        hackathonDevSeedHelper.repairAllDevHackathonRoundSchedules();
        e2eWorkflowDataSeeder.repairForGd2Testing();
        gd3PrelimOpenDataSeeder.repairForFeTesting();
        gd3LateReviewDataSeeder.repairForFeTesting();
        gd3ScoringLiveDataSeeder.repairForFeTesting();
        gd3TiebreakHybridDataSeeder.repairForFeTesting();
        gd3EdgeErrorsDataSeeder.repairForFeTesting();
        gd3CalibrationTimerDataSeeder.repairForFeTesting();
        gd4AdvanceReadyDataSeeder.repairForFeTesting();
        gd4PublishedDataSeeder.repairForFeTesting();
        gd4TiebreakGateDataSeeder.repairForFeTesting();
        gd4CkActivateReadyDataSeeder.repairForFeTesting();
        gd4EdgeErrorsDataSeeder.repairForFeTesting();
        gd4WildcardResolvedDataSeeder.repairForFeTesting();
        gd4TiebreakResolvedDataSeeder.repairForFeTesting();
        gd5FinalRoundDataSeeder.repairForFeTesting();
        gd5SubmitOpenDataSeeder.repairForFeTesting();
        gd5ScoringLiveDataSeeder.repairForFeTesting();
        gd5CalibrationTimerDataSeeder.repairForFeTesting();
        gd5EdgeErrorsDataSeeder.repairForFeTesting();
        gd5LateHardlockDataSeeder.repairForFeTesting();
        gd6PendingConfirmDataSeeder.repairForFeTesting();
        gd6PrizesEmptyDataSeeder.repairForFeTesting();
        gd6ConfirmReadyDataSeeder.repairForFeTesting();
        gd6FinishedExportDataSeeder.repairForFeTesting();
        gd6EdgeErrorsDataSeeder.repairForFeTesting();
        hackathonDevSeedHelper.backfillReleasedPrelimTrackProblems();
        hackathonDevSeedHelper.backfillReleasedFinalRoundProblems();
        hackathonDevSeedHelper.repairAllDevHackathonMilestoneEvents();
        hackathonDevSeedHelper.repairAllHackathonBanners();
    }
}

