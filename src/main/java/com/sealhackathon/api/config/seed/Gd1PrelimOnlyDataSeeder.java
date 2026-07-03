package com.sealhackathon.api.config.seed;

import com.sealhackathon.api.events.value_object.EventType;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.users.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;

/**
 * GĐ1 — chỉ round Sơ loại + tracks, không CK shell → {@code MISSING_FINAL_ROUND} (G1-N08).
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class Gd1PrelimOnlyDataSeeder {

    private final HackathonDevSeedHelper seedHelper;
    private final HackathonRepository hackathonRepository;
    private final DevSeedCleanup devSeedCleanup;

    @Value("${app.seed.gd1.prelim-only.enabled:true}")
    private boolean enabled;

    @Transactional
    public void ensureSeed() {
        if (!enabled) {
            log.info("[Gd1PrelimOnlyDataSeeder] Tắt");
            return;
        }

        HackathonDevSeedHelper.SeedDates dates = seedHelper.computeGd2RegistrationOpenDates();
        HackathonDevSeedHelper.HackathonStructure structure = seedHelper.ensureHackathonStructure(
                Gd1PrelimOnlySeedConstants.SLUG_GD1_PRELIM_ONLY,
                "[Dev] GĐ1 — Prelim only",
                HackathonStatus.DRAFT,
                "DRAFT có prelim + tracks — không có round CK → readiness ONGOING fail",
                new HackathonDevSeedHelper.PrelimState(false, false, false, false, 2, 4),
                new HackathonDevSeedHelper.FinalState(false, false),
                dates);

        Hackathon hackathon = structure.hackathon();
        User coordinator = seedHelper.requireCoordinator();
        seedHelper.syncHackathonCalendarFromDates(Gd1PrelimOnlySeedConstants.SLUG_GD1_PRELIM_ONLY, dates);
        seedHelper.removeFinalRoundShell(hackathon);
        seedHelper.ensureMilestoneEventsExcluding(
                hackathon, coordinator, EnumSet.of(EventType.WORKSHOP, EventType.AWARDS));
        hackathon.setStatus(HackathonStatus.DRAFT);
        hackathonRepository.save(hackathon);

        log.info("""
                [Gd1PrelimOnlyDataSeeder] slug={} hackathonId={}
                  GET readiness?target=ONGOING → MISSING_FINAL_ROUND
                """,
                Gd1PrelimOnlySeedConstants.SLUG_GD1_PRELIM_ONLY,
                hackathon.getId());
    }

    @Transactional
    public void repairForFeTesting() {
        if (!enabled) {
            return;
        }
        hackathonRepository.findBySlug(Gd1PrelimOnlySeedConstants.SLUG_GD1_PRELIM_ONLY)
                .ifPresent(h -> {
                    HackathonDevSeedHelper.SeedDates dates = seedHelper.computeGd2RegistrationOpenDates();
                    seedHelper.syncHackathonCalendarFromDates(
                            Gd1PrelimOnlySeedConstants.SLUG_GD1_PRELIM_ONLY, dates);
                    seedHelper.removeFinalRoundShell(h);
                    seedHelper.ensureMilestoneEventsExcluding(
                            h, seedHelper.requireCoordinator(),
                            EnumSet.of(EventType.WORKSHOP, EventType.AWARDS));
                    if (h.getStatus() != HackathonStatus.DRAFT) {
                        h.setStatus(HackathonStatus.DRAFT);
                        hackathonRepository.save(h);
                    }
                });
    }

    @Transactional
    public void resetAndSeed() {
        devSeedCleanup.purgeIfPresent(Gd1PrelimOnlySeedConstants.SLUG_GD1_PRELIM_ONLY);
        ensureSeed();
    }
}
