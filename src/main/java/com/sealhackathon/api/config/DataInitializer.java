package com.sealhackathon.api.config;

import com.sealhackathon.api.config.seed.AccountStatesDataSeeder;
import com.sealhackathon.api.config.seed.DevSeedCleanup;
import com.sealhackathon.api.config.seed.E2eWorkflowDataSeeder;
import com.sealhackathon.api.config.seed.Gd1DataSeeder;
import com.sealhackathon.api.config.seed.Gd1EventOrderBadDataSeeder;
import com.sealhackathon.api.config.seed.Gd1EventOrderViolationDataSeeder;
import com.sealhackathon.api.config.seed.Gd1JudgeFinalEarlyDataSeeder;
import com.sealhackathon.api.config.seed.Gd1NoAwardsDataSeeder;
import com.sealhackathon.api.config.seed.Gd1NoKickoffDataSeeder;
import com.sealhackathon.api.config.seed.Gd1PrelimOnlyDataSeeder;
import com.sealhackathon.api.config.seed.Gd2LotteryNotLockedDataSeeder;
import com.sealhackathon.api.config.seed.Gd2RegistrationClosedDataSeeder;
import com.sealhackathon.api.config.seed.Gd2RoundActiveDataSeeder;
import com.sealhackathon.api.config.seed.Gd2TeamsEdgeDataSeeder;
import com.sealhackathon.api.config.seed.Gd3CalibrationTimerDataSeeder;
import com.sealhackathon.api.config.seed.Gd3EdgeErrorsDataSeeder;
import com.sealhackathon.api.config.seed.Gd3JudgeMentorConflictDataSeeder;
import com.sealhackathon.api.config.seed.Gd3LateReviewDataSeeder;
import com.sealhackathon.api.config.seed.FallOngoingDataSeeder;
import com.sealhackathon.api.config.seed.Gd3MentorPortalDataSeeder;
import com.sealhackathon.api.config.seed.Gd3MentorTrackOnlyDataSeeder;
import com.sealhackathon.api.config.seed.Gd3TeamMentorHistoryDataSeeder;
import com.sealhackathon.api.config.seed.Gd3NoLotteryDataSeeder;
import com.sealhackathon.api.config.seed.Gd3PrelimOpenDataSeeder;
import com.sealhackathon.api.config.seed.Gd3RoundConfigEdgeDataSeeder;
import com.sealhackathon.api.config.seed.Gd3ScoringLiveDataSeeder;
import com.sealhackathon.api.config.seed.Gd3ScoringGateDataSeeder;
import com.sealhackathon.api.config.seed.Gd3TiebreakHybridDataSeeder;
import com.sealhackathon.api.config.seed.Gd4AdvanceReadyDataSeeder;
import com.sealhackathon.api.config.seed.Gd4CkActivateReadyDataSeeder;
import com.sealhackathon.api.config.seed.Gd4CkNoCriteriaDataSeeder;
import com.sealhackathon.api.config.seed.Gd4CkUnpublishedDataSeeder;
import com.sealhackathon.api.config.seed.Gd4EdgeErrorsDataSeeder;
import com.sealhackathon.api.config.seed.Gd4JudgeAssignWarningsDataSeeder;
import com.sealhackathon.api.config.seed.Gd4PublishedDataSeeder;
import com.sealhackathon.api.config.seed.Gd4TiebreakGateDataSeeder;
import com.sealhackathon.api.config.seed.Gd4TiebreakResolvedDataSeeder;
import com.sealhackathon.api.config.seed.Gd4WildcardDisabledDataSeeder;
import com.sealhackathon.api.config.seed.Gd4WildcardResolvedDataSeeder;
import com.sealhackathon.api.config.seed.Gd5CalibrationTimerDataSeeder;
import com.sealhackathon.api.config.seed.Gd5EdgeErrorsDataSeeder;
import com.sealhackathon.api.config.seed.Gd5JudgeEdgeDataSeeder;
import com.sealhackathon.api.config.seed.Gd5LatePendingDataSeeder;
import com.sealhackathon.api.config.seed.Gd5NotAdvancedDataSeeder;
import com.sealhackathon.api.config.seed.Gd6PrizeDuplicateDataSeeder;
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
    private final Gd1NoKickoffDataSeeder gd1NoKickoffDataSeeder;
    private final Gd1NoAwardsDataSeeder gd1NoAwardsDataSeeder;
    private final Gd1JudgeFinalEarlyDataSeeder gd1JudgeFinalEarlyDataSeeder;
    private final Gd1EventOrderBadDataSeeder gd1EventOrderBadDataSeeder;
    private final Gd1EventOrderViolationDataSeeder gd1EventOrderViolationDataSeeder;
    private final Gd1PrelimOnlyDataSeeder gd1PrelimOnlyDataSeeder;
    private final Gd2TeamsEdgeDataSeeder gd2TeamsEdgeDataSeeder;
    private final Gd2RegistrationClosedDataSeeder gd2RegistrationClosedDataSeeder;
    private final Gd2LotteryNotLockedDataSeeder gd2LotteryNotLockedDataSeeder;
    private final Gd2RoundActiveDataSeeder gd2RoundActiveDataSeeder;
    private final FallOngoingDataSeeder fallOngoingDataSeeder;
    private final E2eWorkflowDataSeeder e2eWorkflowDataSeeder;
    private final Gd3PrelimOpenDataSeeder gd3PrelimOpenDataSeeder;
    private final Gd3LateReviewDataSeeder gd3LateReviewDataSeeder;
    private final Gd3ScoringLiveDataSeeder gd3ScoringLiveDataSeeder;
    private final Gd3ScoringGateDataSeeder gd3ScoringGateDataSeeder;
    private final Gd3TiebreakHybridDataSeeder gd3TiebreakHybridDataSeeder;
    private final Gd3EdgeErrorsDataSeeder gd3EdgeErrorsDataSeeder;
    private final Gd3CalibrationTimerDataSeeder gd3CalibrationTimerDataSeeder;
    private final Gd3JudgeMentorConflictDataSeeder gd3JudgeMentorConflictDataSeeder;
    private final Gd3RoundConfigEdgeDataSeeder gd3RoundConfigEdgeDataSeeder;
    private final Gd3NoLotteryDataSeeder gd3NoLotteryDataSeeder;
    private final Gd3MentorPortalDataSeeder gd3MentorPortalDataSeeder;
    private final Gd3MentorTrackOnlyDataSeeder gd3MentorTrackOnlyDataSeeder;
    private final Gd3TeamMentorHistoryDataSeeder gd3TeamMentorHistoryDataSeeder;
    private final Gd4AdvanceReadyDataSeeder gd4AdvanceReadyDataSeeder;
    private final Gd4CkUnpublishedDataSeeder gd4CkUnpublishedDataSeeder;
    private final Gd4PublishedDataSeeder gd4PublishedDataSeeder;
    private final Gd4TiebreakGateDataSeeder gd4TiebreakGateDataSeeder;
    private final Gd4CkActivateReadyDataSeeder gd4CkActivateReadyDataSeeder;
    private final Gd4EdgeErrorsDataSeeder gd4EdgeErrorsDataSeeder;
    private final Gd4WildcardResolvedDataSeeder gd4WildcardResolvedDataSeeder;
    private final Gd4TiebreakResolvedDataSeeder gd4TiebreakResolvedDataSeeder;
    private final Gd4WildcardDisabledDataSeeder gd4WildcardDisabledDataSeeder;
    private final Gd4JudgeAssignWarningsDataSeeder gd4JudgeAssignWarningsDataSeeder;
    private final Gd4CkNoCriteriaDataSeeder gd4CkNoCriteriaDataSeeder;
    private final Gd5FinalRoundDataSeeder gd5FinalRoundDataSeeder;
    private final Gd5SubmitOpenDataSeeder gd5SubmitOpenDataSeeder;
    private final Gd5ScoringLiveDataSeeder gd5ScoringLiveDataSeeder;
    private final Gd5CalibrationTimerDataSeeder gd5CalibrationTimerDataSeeder;
    private final Gd5EdgeErrorsDataSeeder gd5EdgeErrorsDataSeeder;
    private final Gd5LateHardlockDataSeeder gd5LateHardlockDataSeeder;
    private final Gd5JudgeEdgeDataSeeder gd5JudgeEdgeDataSeeder;
    private final Gd5LatePendingDataSeeder gd5LatePendingDataSeeder;
    private final Gd5NotAdvancedDataSeeder gd5NotAdvancedDataSeeder;
    private final Gd6PendingConfirmDataSeeder gd6PendingConfirmDataSeeder;
    private final Gd6PrizesEmptyDataSeeder gd6PrizesEmptyDataSeeder;
    private final Gd6ConfirmReadyDataSeeder gd6ConfirmReadyDataSeeder;
    private final Gd6FinishedExportDataSeeder gd6FinishedExportDataSeeder;
    private final Gd6EdgeErrorsDataSeeder gd6EdgeErrorsDataSeeder;
    private final Gd6PrizeDuplicateDataSeeder gd6PrizeDuplicateDataSeeder;
    private final AccountStatesDataSeeder accountStatesDataSeeder;
    private final HackathonDevSeedHelper hackathonDevSeedHelper;

    @Override
    public void run(String... args) {
        devSeedCleanup.purgeDeprecatedHackathons();

        gd1DataSeeder.repairSeededTimeline();
        gd1DataSeeder.repairSeededFinishedHackathon();
        hackathonDevSeedHelper.repairFinishedArchiveAwardsSeed();
        gd1DataSeeder.repairSeededCriteriaAndTracks();
        gd1DataSeeder.repairDevUserPasswords();

        if (gd1DataSeeder.isAlreadySeeded()) {
            log.info("[DataInitializer] Seed GĐ1 đã có (slug={}), bỏ qua tạo mới.",
                    Gd1SeedConstants.SLUG_ONGOING);
        } else {
            gd1DataSeeder.seedAll();
        }

        e2eWorkflowDataSeeder.ensureSeed();
        gd1DataSeeder.ensureIncompleteSeed();
        gd1NoKickoffDataSeeder.ensureSeed();
        gd1NoAwardsDataSeeder.ensureSeed();
        gd1JudgeFinalEarlyDataSeeder.ensureSeed();
        gd1EventOrderBadDataSeeder.ensureSeed();
        gd1EventOrderViolationDataSeeder.ensureSeed();
        gd1PrelimOnlyDataSeeder.ensureSeed();
        gd2TeamsEdgeDataSeeder.ensureSeed();
        gd2RegistrationClosedDataSeeder.ensureSeed();
        gd2LotteryNotLockedDataSeeder.ensureSeed();
        gd2RoundActiveDataSeeder.ensureSeed();
        fallOngoingDataSeeder.ensureSeed();
        e2eWorkflowDataSeeder.repairForGd5FullChainRetest();
        gd3PrelimOpenDataSeeder.ensureSeed();
        gd3LateReviewDataSeeder.ensureSeed();
        gd3ScoringLiveDataSeeder.ensureSeed();
        gd3ScoringGateDataSeeder.ensureSeed();
        gd3TiebreakHybridDataSeeder.ensureSeed();
        gd3EdgeErrorsDataSeeder.ensureSeed();
        gd3CalibrationTimerDataSeeder.ensureSeed();
        gd3JudgeMentorConflictDataSeeder.ensureSeed();
        gd3RoundConfigEdgeDataSeeder.ensureSeed();
        gd3NoLotteryDataSeeder.ensureSeed();
        gd3MentorPortalDataSeeder.ensureSeed();
        gd3MentorTrackOnlyDataSeeder.ensureSeed();
        gd3TeamMentorHistoryDataSeeder.ensureSeed();
        gd4AdvanceReadyDataSeeder.ensureSeed();
        gd4CkUnpublishedDataSeeder.ensureSeed();
        gd4PublishedDataSeeder.ensureSeed();
        gd4TiebreakGateDataSeeder.ensureSeed();
        gd4CkActivateReadyDataSeeder.ensureSeed();
        gd4EdgeErrorsDataSeeder.ensureSeed();
        gd4WildcardResolvedDataSeeder.ensureSeed();
        gd4TiebreakResolvedDataSeeder.ensureSeed();
        gd4WildcardDisabledDataSeeder.ensureSeed();
        gd4JudgeAssignWarningsDataSeeder.ensureSeed();
        gd4CkNoCriteriaDataSeeder.ensureSeed();
        gd6PendingConfirmDataSeeder.repairForFullChainRetest();
        gd6PendingConfirmDataSeeder.repairForApiMatrixReadiness();
        gd6PendingConfirmDataSeeder.ensureSeed();
        gd6PrizesEmptyDataSeeder.ensureSeed();
        gd6ConfirmReadyDataSeeder.ensureSeed();
        gd6FinishedExportDataSeeder.ensureSeed();
        gd6EdgeErrorsDataSeeder.ensureSeed();
        gd6PrizeDuplicateDataSeeder.ensureSeed();
        log.info("[DataInitializer] GĐ6 dev seeds ready (slugs seal-gd6-*).");
        gd5FinalRoundDataSeeder.ensureSeed();
        gd5SubmitOpenDataSeeder.ensureSeed();
        gd5ScoringLiveDataSeeder.ensureSeed();
        gd5CalibrationTimerDataSeeder.ensureSeed();
        gd5EdgeErrorsDataSeeder.ensureSeed();
        gd5LateHardlockDataSeeder.ensureSeed();
        gd5JudgeEdgeDataSeeder.ensureSeed();
        gd5LatePendingDataSeeder.ensureSeed();
        gd5NotAdvancedDataSeeder.ensureSeed();
        accountStatesDataSeeder.ensureSeed();
        hackathonDevSeedHelper.repairAllDevHackathonRoundSchedules();
        e2eWorkflowDataSeeder.repairForGd2Testing();
        gd3PrelimOpenDataSeeder.repairForFeTesting();
        gd3LateReviewDataSeeder.repairForFeTesting();
        gd3ScoringLiveDataSeeder.repairForFeTesting();
        gd3ScoringGateDataSeeder.repairForFeTesting();
        gd3TiebreakHybridDataSeeder.repairForFeTesting();
        gd3EdgeErrorsDataSeeder.repairForFeTesting();
        gd3CalibrationTimerDataSeeder.repairForFeTesting();
        gd3JudgeMentorConflictDataSeeder.repairForFeTesting();
        gd3RoundConfigEdgeDataSeeder.repairForFeTesting();
        gd3NoLotteryDataSeeder.repairForFeTesting();
        gd3MentorPortalDataSeeder.repairForFeTesting();
        gd3MentorTrackOnlyDataSeeder.repairForFeTesting();
        gd3TeamMentorHistoryDataSeeder.repairForFeTesting();
        gd4AdvanceReadyDataSeeder.repairForFeTesting();
        gd4CkUnpublishedDataSeeder.repairForFeTesting();
        gd4PublishedDataSeeder.repairForFeTesting();
        gd4TiebreakGateDataSeeder.repairForFeTesting();
        gd4CkActivateReadyDataSeeder.repairForFeTesting();
        gd4EdgeErrorsDataSeeder.repairForFeTesting();
        gd4WildcardResolvedDataSeeder.repairForFeTesting();
        gd4TiebreakResolvedDataSeeder.repairForFeTesting();
        gd4WildcardDisabledDataSeeder.repairForFeTesting();
        gd4JudgeAssignWarningsDataSeeder.repairForFeTesting();
        gd4CkNoCriteriaDataSeeder.repairForFeTesting();
        gd5FinalRoundDataSeeder.repairForFeTesting();
        gd5SubmitOpenDataSeeder.repairForFeTesting();
        gd5ScoringLiveDataSeeder.repairForFeTesting();
        gd5CalibrationTimerDataSeeder.repairForFeTesting();
        gd5EdgeErrorsDataSeeder.repairForFeTesting();
        gd5LateHardlockDataSeeder.repairForFeTesting();
        gd5JudgeEdgeDataSeeder.repairForFeTesting();
        gd5LatePendingDataSeeder.repairForFeTesting();
        gd5NotAdvancedDataSeeder.repairForFeTesting();
        gd1NoKickoffDataSeeder.repairForFeTesting();
        gd1NoAwardsDataSeeder.repairForFeTesting();
        gd1JudgeFinalEarlyDataSeeder.repairForFeTesting();
        gd1EventOrderBadDataSeeder.repairForFeTesting();
        gd1EventOrderViolationDataSeeder.repairForFeTesting();
        gd1PrelimOnlyDataSeeder.repairForFeTesting();
        gd2TeamsEdgeDataSeeder.repairForFeTesting();
        gd2RegistrationClosedDataSeeder.repairForFeTesting();
        gd2RoundActiveDataSeeder.repairForFeTesting();
        fallOngoingDataSeeder.repairForFeTesting();
        gd6PrizeDuplicateDataSeeder.repairForFeTesting();
        gd6PendingConfirmDataSeeder.repairForFeTesting();
        gd6PendingConfirmDataSeeder.repairForApiMatrixReadiness();
        gd6PrizesEmptyDataSeeder.repairForFeTesting();
        gd6ConfirmReadyDataSeeder.repairForFeTesting();
        gd6FinishedExportDataSeeder.repairForFeTesting();
        gd6EdgeErrorsDataSeeder.repairForFeTesting();
        hackathonDevSeedHelper.backfillReleasedPrelimTrackProblems();
        hackathonDevSeedHelper.backfillReleasedFinalRoundProblems();
        hackathonDevSeedHelper.repairAllDevHackathonMilestoneEvents();
        hackathonDevSeedHelper.repairAllHackathonBanners();
        hackathonDevSeedHelper.repairFinishedArchiveAwardsSeed();
        gd2LotteryNotLockedDataSeeder.repairForFeTesting();
        accountStatesDataSeeder.repairForFeTesting();
        log.info("[DataInitializer] Dev seed GĐ3–GĐ6 hoàn tất — API matrix sẵn sàng.");
    }
}

