package com.sealhackathon.api.config.seed;

import com.sealhackathon.api.events.value_object.EventType;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * GĐ1 — ONGOING đủ structure nhưng 0 milestone event (POST WORKSHOP → EVENT_KICKOFF_MISSING).
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class Gd1EventOrderBadDataSeeder {

    private final HackathonDevSeedHelper seedHelper;
    private final HackathonRepository hackathonRepository;
    private final DevSeedCleanup devSeedCleanup;

    @Value("${app.seed.gd1.event-order-bad.enabled:true}")
    private boolean enabled;

    @Transactional
    public void ensureSeed() {
        if (!enabled) {
            log.info("[Gd1EventOrderBadDataSeeder] Tắt");
            return;
        }

        HackathonDevSeedHelper.SeedDates dates = seedHelper.computeGd2RegistrationOpenDates();
        HackathonDevSeedHelper.HackathonStructure structure = seedHelper.ensureHackathonStructure(
                Gd1EventOrderBadSeedConstants.SLUG_GD1_EVENT_ORDER_BAD,
                "[Dev] GĐ1 — Event order bad",
                HackathonStatus.ONGOING,
                "ONGOING đủ round/track — 0 event (G1-N01)",
                new HackathonDevSeedHelper.PrelimState(false, false, false, false, 2, 4),
                new HackathonDevSeedHelper.FinalState(false, false),
                dates);

        Hackathon hackathon = structure.hackathon();
        seedHelper.syncHackathonCalendarFromDates(Gd1EventOrderBadSeedConstants.SLUG_GD1_EVENT_ORDER_BAD, dates);
        for (EventType type : EventType.values()) {
            seedHelper.removeMilestoneEvents(hackathon, type);
        }
        hackathon.setStatus(HackathonStatus.ONGOING);
        hackathonRepository.save(hackathon);

        log.info("[Gd1EventOrderBadDataSeeder] slug={} hackathonId={} — no milestone events",
                Gd1EventOrderBadSeedConstants.SLUG_GD1_EVENT_ORDER_BAD, hackathon.getId());
    }

    @Transactional
    public void repairForFeTesting() {
        if (!enabled) {
            return;
        }
        hackathonRepository.findBySlug(Gd1EventOrderBadSeedConstants.SLUG_GD1_EVENT_ORDER_BAD)
                .ifPresent(h -> {
                    HackathonDevSeedHelper.SeedDates dates = seedHelper.computeGd2RegistrationOpenDates();
                    seedHelper.syncHackathonCalendarFromDates(
                            Gd1EventOrderBadSeedConstants.SLUG_GD1_EVENT_ORDER_BAD, dates);
                    for (EventType type : EventType.values()) {
                        seedHelper.removeMilestoneEvents(h, type);
                    }
                    if (h.getStatus() != HackathonStatus.ONGOING) {
                        h.setStatus(HackathonStatus.ONGOING);
                        hackathonRepository.save(h);
                    }
                });
    }

    @Transactional
    public void resetAndSeed() {
        devSeedCleanup.purgeIfPresent(Gd1EventOrderBadSeedConstants.SLUG_GD1_EVENT_ORDER_BAD);
        ensureSeed();
    }
}
