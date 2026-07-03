package com.sealhackathon.api.config.seed;

import com.sealhackathon.api.events.value_object.EventType;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.users.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;

/**
 * GĐ1 — ONGOING đủ structure, không gán judge CK (test JUDGE_FINAL_AT_PHASE1 qua UI/API).
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class Gd1JudgeFinalEarlyDataSeeder {

    private final HackathonDevSeedHelper seedHelper;
    private final HackathonRepository hackathonRepository;
    private final RoundRepository roundRepository;
    private final DevSeedCleanup devSeedCleanup;

    @Value("${app.seed.gd1.judge-final-early.enabled:true}")
    private boolean enabled;

    @Transactional
    public void ensureSeed() {
        if (!enabled) {
            log.info("[Gd1JudgeFinalEarlyDataSeeder] Tắt");
            return;
        }

        HackathonDevSeedHelper.SeedDates dates = seedHelper.computeGd2RegistrationOpenDates();
        HackathonDevSeedHelper.HackathonStructure structure = seedHelper.ensureHackathonStructure(
                Gd1JudgeFinalEarlySeedConstants.SLUG_GD1_JUDGE_FINAL_EARLY,
                "[Dev] GĐ1 — Judge final early",
                HackathonStatus.ONGOING,
                "ONGOING đủ round/track/events — chưa gán judge CK (G1-N05)",
                new HackathonDevSeedHelper.PrelimState(false, false, false, false, 2, 4),
                new HackathonDevSeedHelper.FinalState(false, false),
                dates);

        Hackathon hackathon = structure.hackathon();
        Round finalRound = structure.finalRound();
        seedHelper.syncHackathonCalendarFromDates(
                Gd1JudgeFinalEarlySeedConstants.SLUG_GD1_JUDGE_FINAL_EARLY, dates);
        seedHelper.clearFinalJudgeAssignments(finalRound);
        User coordinator = seedHelper.requireCoordinator();
        seedHelper.ensureMilestoneEventsExcluding(hackathon, coordinator, EnumSet.noneOf(EventType.class));
        hackathon.setStatus(HackathonStatus.ONGOING);
        hackathonRepository.save(hackathon);

        log.info("[Gd1JudgeFinalEarlyDataSeeder] slug={} hackathonId={} — no FINAL_EXTERNAL judge",
                Gd1JudgeFinalEarlySeedConstants.SLUG_GD1_JUDGE_FINAL_EARLY, hackathon.getId());
    }

    @Transactional
    public void repairForFeTesting() {
        if (!enabled) {
            return;
        }
        hackathonRepository.findBySlug(Gd1JudgeFinalEarlySeedConstants.SLUG_GD1_JUDGE_FINAL_EARLY)
                .ifPresent(h -> {
                    HackathonDevSeedHelper.SeedDates dates = seedHelper.computeGd2RegistrationOpenDates();
                    seedHelper.syncHackathonCalendarFromDates(
                            Gd1JudgeFinalEarlySeedConstants.SLUG_GD1_JUDGE_FINAL_EARLY, dates);
                    loadFinal(h.getId()).ifPresent(seedHelper::clearFinalJudgeAssignments);
                    if (h.getStatus() != HackathonStatus.ONGOING) {
                        h.setStatus(HackathonStatus.ONGOING);
                        hackathonRepository.save(h);
                    }
                });
    }

    private java.util.Optional<Round> loadFinal(Integer hackathonId) {
        return roundRepository.findByHackathon_IdOrderByExamAtAsc(hackathonId).stream()
                .filter(r -> Boolean.TRUE.equals(r.getIsFinal()))
                .findFirst();
    }

    @Transactional
    public void resetAndSeed() {
        devSeedCleanup.purgeIfPresent(Gd1JudgeFinalEarlySeedConstants.SLUG_GD1_JUDGE_FINAL_EARLY);
        ensureSeed();
    }
}
