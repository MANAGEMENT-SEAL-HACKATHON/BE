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
 * GĐ1 — có KICKOFF, chưa WORKSHOP → POST AWARDS → {@code EVENT_ORDER_VIOLATION} (G1-N02).
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class Gd1EventOrderViolationDataSeeder {

    private final HackathonDevSeedHelper seedHelper;
    private final HackathonRepository hackathonRepository;
    private final DevSeedCleanup devSeedCleanup;

    @Value("${app.seed.gd1.event-order-violation.enabled:true}")
    private boolean enabled;

    @Transactional
    public void ensureSeed() {
        if (!enabled) {
            log.info("[Gd1EventOrderViolationDataSeeder] Tắt");
            return;
        }

        HackathonDevSeedHelper.SeedDates dates = seedHelper.computeGd2RegistrationOpenDates();
        HackathonDevSeedHelper.HackathonStructure structure = seedHelper.ensureHackathonStructure(
                Gd1EventOrderViolationSeedConstants.SLUG_GD1_EVENT_ORDER_VIOLATION,
                "[Dev] GĐ1 — Event order violation",
                HackathonStatus.ONGOING,
                "ONGOING đủ round/track — chỉ KICKOFF (chưa WORKSHOP) → POST AWARDS bị chặn",
                new HackathonDevSeedHelper.PrelimState(false, false, false, false, 2, 4),
                new HackathonDevSeedHelper.FinalState(false, false),
                dates);

        Hackathon hackathon = structure.hackathon();
        User coordinator = seedHelper.requireCoordinator();
        seedHelper.syncHackathonCalendarFromDates(
                Gd1EventOrderViolationSeedConstants.SLUG_GD1_EVENT_ORDER_VIOLATION, dates);
        seedHelper.removeMilestoneEvents(hackathon, EventType.WORKSHOP);
        seedHelper.removeMilestoneEvents(hackathon, EventType.AWARDS);
        seedHelper.ensureMilestoneEventsExcluding(
                hackathon, coordinator, EnumSet.of(EventType.WORKSHOP, EventType.AWARDS));
        hackathon.setStatus(HackathonStatus.ONGOING);
        hackathonRepository.save(hackathon);

        log.info("""
                [Gd1EventOrderViolationDataSeeder] slug={} hackathonId={}
                  POST AWARDS → EVENT_ORDER_VIOLATION (thiếu WORKSHOP)
                """,
                Gd1EventOrderViolationSeedConstants.SLUG_GD1_EVENT_ORDER_VIOLATION,
                hackathon.getId());
    }

    @Transactional
    public void repairForFeTesting() {
        if (!enabled) {
            return;
        }
        hackathonRepository.findBySlug(Gd1EventOrderViolationSeedConstants.SLUG_GD1_EVENT_ORDER_VIOLATION)
                .ifPresent(h -> {
                    HackathonDevSeedHelper.SeedDates dates = seedHelper.computeGd2RegistrationOpenDates();
                    seedHelper.syncHackathonCalendarFromDates(
                            Gd1EventOrderViolationSeedConstants.SLUG_GD1_EVENT_ORDER_VIOLATION, dates);
                    seedHelper.removeMilestoneEvents(h, EventType.WORKSHOP);
                    seedHelper.removeMilestoneEvents(h, EventType.AWARDS);
                    seedHelper.ensureMilestoneEventsExcluding(
                            h, seedHelper.requireCoordinator(), EnumSet.of(EventType.WORKSHOP, EventType.AWARDS));
                    if (h.getStatus() != HackathonStatus.ONGOING) {
                        h.setStatus(HackathonStatus.ONGOING);
                        hackathonRepository.save(h);
                    }
                });
    }

    @Transactional
    public void resetAndSeed() {
        devSeedCleanup.purgeIfPresent(Gd1EventOrderViolationSeedConstants.SLUG_GD1_EVENT_ORDER_VIOLATION);
        ensureSeed();
    }
}
