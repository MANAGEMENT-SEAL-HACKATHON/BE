package com.sealhackathon.api.config.seed;

import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.events.value_object.EventType;
import com.sealhackathon.api.users.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;

/**
 * GĐ1/GĐ6 bad path — ONGOING nhưng thiếu AWARDS → readiness target=AWARDS fail.
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class Gd1NoAwardsDataSeeder {

    private final HackathonDevSeedHelper seedHelper;
    private final HackathonRepository hackathonRepository;
    private final DevSeedCleanup devSeedCleanup;

    @Value("${app.seed.gd1.no-awards.enabled:true}")
    private boolean enabled;

    @Transactional
    public void ensureSeed() {
        if (!enabled) {
            log.info("[Gd1NoAwardsDataSeeder] Tắt (app.seed.gd1.no-awards.enabled=false)");
            return;
        }

        HackathonDevSeedHelper.SeedDates dates = seedHelper.computeGd4AdvanceReadyDates();
        HackathonDevSeedHelper.HackathonStructure structure = seedHelper.ensureHackathonStructure(
                Gd1NoAwardsSeedConstants.SLUG_GD1_NO_AWARDS,
                "[Dev] GĐ1 — No AWARDS",
                HackathonStatus.ONGOING,
                "ONGOING đủ cấu trúc — thiếu AWARDS → readiness AWARDS fail",
                new HackathonDevSeedHelper.PrelimState(false, true, true, true, 2, 4),
                new HackathonDevSeedHelper.FinalState(false, false),
                dates);

        Hackathon hackathon = structure.hackathon();
        User coordinator = seedHelper.requireCoordinator();
        seedHelper.syncHackathonCalendarFromDates(Gd1NoAwardsSeedConstants.SLUG_GD1_NO_AWARDS, dates);
        seedHelper.removeMilestoneEvents(hackathon, EventType.AWARDS);
        seedHelper.ensureMilestoneEventsExcluding(
                hackathon, coordinator, EnumSet.of(EventType.AWARDS));
        hackathon.setStatus(HackathonStatus.ONGOING);
        hackathonRepository.save(hackathon);

        log.info("""
                [Gd1NoAwardsDataSeeder] slug={} hackathonId={}
                  GET readiness?target=AWARDS → ready:false
                """,
                Gd1NoAwardsSeedConstants.SLUG_GD1_NO_AWARDS,
                hackathon.getId());
    }

    @Transactional
    public void repairForFeTesting() {
        if (!enabled) {
            return;
        }
        hackathonRepository.findBySlug(Gd1NoAwardsSeedConstants.SLUG_GD1_NO_AWARDS).ifPresent(h -> {
            HackathonDevSeedHelper.SeedDates dates = seedHelper.computeGd4AdvanceReadyDates();
            seedHelper.syncHackathonCalendarFromDates(Gd1NoAwardsSeedConstants.SLUG_GD1_NO_AWARDS, dates);
            seedHelper.removeMilestoneEvents(h, EventType.AWARDS);
            seedHelper.ensureMilestoneEventsExcluding(
                    h, seedHelper.requireCoordinator(), EnumSet.of(EventType.AWARDS));
            if (h.getStatus() != HackathonStatus.ONGOING) {
                h.setStatus(HackathonStatus.ONGOING);
                hackathonRepository.save(h);
            }
        });
    }

    @Transactional
    public void resetAndSeed() {
        devSeedCleanup.purgeIfPresent(Gd1NoAwardsSeedConstants.SLUG_GD1_NO_AWARDS);
        ensureSeed();
    }
}
