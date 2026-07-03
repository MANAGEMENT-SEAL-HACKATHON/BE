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
 * GĐ1 bad path — đủ round/track nhưng thiếu KICKOFF → readiness ONGOING fail.
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class Gd1NoKickoffDataSeeder {

    private final HackathonDevSeedHelper seedHelper;
    private final HackathonRepository hackathonRepository;
    private final DevSeedCleanup devSeedCleanup;

    @Value("${app.seed.gd1.no-kickoff.enabled:true}")
    private boolean enabled;

    @Transactional
    public void ensureSeed() {
        if (!enabled) {
            log.info("[Gd1NoKickoffDataSeeder] Tắt (app.seed.gd1.no-kickoff.enabled=false)");
            return;
        }

        HackathonDevSeedHelper.SeedDates dates = seedHelper.computeGd2RegistrationOpenDates();
        HackathonDevSeedHelper.HackathonStructure structure = seedHelper.ensureHackathonStructure(
                Gd1NoKickoffSeedConstants.SLUG_GD1_NO_KICKOFF,
                "[Dev] GĐ1 — No KICKOFF",
                HackathonStatus.DRAFT,
                "DRAFT đủ round/track — thiếu KICKOFF → readiness ONGOING fail",
                new HackathonDevSeedHelper.PrelimState(false, false, false, false, 2, 4),
                new HackathonDevSeedHelper.FinalState(false, false),
                dates);

        Hackathon hackathon = structure.hackathon();
        User coordinator = seedHelper.requireCoordinator();
        seedHelper.syncHackathonCalendarFromDates(Gd1NoKickoffSeedConstants.SLUG_GD1_NO_KICKOFF, dates);
        seedHelper.removeMilestoneEvents(hackathon, EventType.KICKOFF);
        seedHelper.ensureMilestoneEventsExcluding(
                hackathon, coordinator, EnumSet.of(EventType.KICKOFF));
        hackathon.setStatus(HackathonStatus.DRAFT);
        hackathonRepository.save(hackathon);

        log.info("""
                [Gd1NoKickoffDataSeeder] slug={} hackathonId={}
                  GET readiness?target=ONGOING → ready:false (EVENT_KICKOFF_MISSING)
                """,
                Gd1NoKickoffSeedConstants.SLUG_GD1_NO_KICKOFF,
                hackathon.getId());
    }

    @Transactional
    public void repairForFeTesting() {
        if (!enabled) {
            return;
        }
        hackathonRepository.findBySlug(Gd1NoKickoffSeedConstants.SLUG_GD1_NO_KICKOFF).ifPresent(h -> {
            HackathonDevSeedHelper.SeedDates dates = seedHelper.computeGd2RegistrationOpenDates();
            seedHelper.syncHackathonCalendarFromDates(Gd1NoKickoffSeedConstants.SLUG_GD1_NO_KICKOFF, dates);
            seedHelper.removeMilestoneEvents(h, EventType.KICKOFF);
            seedHelper.ensureMilestoneEventsExcluding(
                    h, seedHelper.requireCoordinator(), EnumSet.of(EventType.KICKOFF));
            if (h.getStatus() != HackathonStatus.DRAFT) {
                h.setStatus(HackathonStatus.DRAFT);
                hackathonRepository.save(h);
            }
        });
    }

    @Transactional
    public void resetAndSeed() {
        devSeedCleanup.purgeIfPresent(Gd1NoKickoffSeedConstants.SLUG_GD1_NO_KICKOFF);
        ensureSeed();
    }
}
