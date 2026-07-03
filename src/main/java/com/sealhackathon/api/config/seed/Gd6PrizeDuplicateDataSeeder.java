package com.sealhackathon.api.config.seed;

import com.sealhackathon.api.chapters.entity.Chapter;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.prizes.repository.PrizeRepository;
import com.sealhackathon.api.prizes.value_object.PrizeRank;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.teams.entity.Team;
import com.sealhackathon.api.tracks.entity.Track;
import com.sealhackathon.api.users.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** GĐ6 — đã có FIRST prize → PRIZE_DUPLICATE. */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class Gd6PrizeDuplicateDataSeeder {

    private final HackathonDevSeedHelper seedHelper;
    private final HackathonRepository hackathonRepository;
    private final RoundRepository roundRepository;
    private final PrizeRepository prizeRepository;
    private final DevSeedCleanup devSeedCleanup;

    @Value("${app.seed.gd6.prize-duplicate.enabled:true}")
    private boolean enabled;

    @Transactional
    public void ensureSeed() {
        if (!enabled) {
            log.info("[Gd6PrizeDuplicateDataSeeder] Tắt");
            return;
        }

        HackathonDevSeedHelper.SeedDates dates = seedHelper.computeGd6PendingConfirmDates();
        HackathonDevSeedHelper.HackathonStructure structure = seedHelper.ensureHackathonStructure(
                Gd6PrizeDuplicateSeedConstants.SLUG_GD6_PRIZE_DUPLICATE,
                "SEAL GĐ6 — Prize duplicate",
                HackathonStatus.PENDING_CONFIRM,
                "Seed GĐ6 — đã có FIRST → POST duplicate PRIZE_DUPLICATE",
                new HackathonDevSeedHelper.PrelimState(false, true, true, true, 1, 2),
                new HackathonDevSeedHelper.FinalState(true, true),
                dates);

        Hackathon hackathon = structure.hackathon();
        Round prelim = structure.prelim();
        Round finalRound = structure.finalRound();
        Track track1 = structure.track1();
        seedHelper.syncHackathonCalendarFromDates(
                Gd6PrizeDuplicateSeedConstants.SLUG_GD6_PRIZE_DUPLICATE, dates);
        seedHelper.repairHackathonForGd6Retest(hackathon, prelim, finalRound);

        User coordinator = seedHelper.requireCoordinator();
        Chapter chapter = seedHelper.requireChapter(Gd1SeedConstants.CHAPTER_FPT_HCM);
        LocalDateTime now = LocalDateTime.now();
        User leader = seedHelper.upsertStudent(
                Gd6PrizeDuplicateSeedConstants.studentEmail(),
                "GD6 PD Leader",
                chapter);
        seedHelper.registerStudent(hackathon, leader);
        Team team = seedHelper.ensureActiveTeam(
                hackathon, Gd6PrizeDuplicateSeedConstants.TEAM_FIRST, leader, chapter, now);
        seedHelper.ensureLottery(hackathon, prelim, track1, "BANG-A", team, coordinator, now);
        seedHelper.markAdvanced(team, prelim, finalRound, hackathon);
        seedHelper.ensureFinalSubmission(hackathon, finalRound, team,
                "https://github.com/seal-warriors/gd6pd-team01");

        hackathon.setStatus(HackathonStatus.PENDING_CONFIRM);
        hackathon.setIndividualRankingEnabled(true);
        hackathonRepository.save(hackathon);
        seedHelper.ensureFirstPrize(hackathon, finalRound, team, coordinator);

        log.info("""
                [Gd6PrizeDuplicateDataSeeder] slug={} prizes=FIRST only
                  POST prize rank FIRST again → PRIZE_DUPLICATE
                """,
                Gd6PrizeDuplicateSeedConstants.SLUG_GD6_PRIZE_DUPLICATE);
    }

    @Transactional
    public void repairForFeTesting() {
        if (!enabled) {
            return;
        }
        hackathonRepository.findBySlug(Gd6PrizeDuplicateSeedConstants.SLUG_GD6_PRIZE_DUPLICATE).ifPresent(h -> {
            seedHelper.syncHackathonCalendarFromDates(
                    Gd6PrizeDuplicateSeedConstants.SLUG_GD6_PRIZE_DUPLICATE,
                    seedHelper.computeGd6PendingConfirmDates());
            if (!prizeRepository.existsByHackathonIdAndPrizeRank(h.getId(), PrizeRank.FIRST)) {
                ensureSeed();
            }
        });
    }

    @Transactional
    public void resetAndSeed() {
        devSeedCleanup.purgeIfPresent(Gd6PrizeDuplicateSeedConstants.SLUG_GD6_PRIZE_DUPLICATE);
        ensureSeed();
    }
}
