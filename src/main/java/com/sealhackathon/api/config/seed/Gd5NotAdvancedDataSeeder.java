package com.sealhackathon.api.config.seed;

import com.sealhackathon.api.chapters.entity.Chapter;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
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

/** GĐ5 — team không trong CK participation → TEAM_NOT_IN_ROUND. */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class Gd5NotAdvancedDataSeeder {

    private final HackathonDevSeedHelper seedHelper;
    private final HackathonRepository hackathonRepository;
    private final RoundRepository roundRepository;
    private final DevSeedCleanup devSeedCleanup;

    @Value("${app.seed.gd5.not-advanced.enabled:true}")
    private boolean enabled;

    @Transactional
    public void ensureSeed() {
        if (!enabled) {
            log.info("[Gd5NotAdvancedDataSeeder] Tắt");
            return;
        }

        HackathonDevSeedHelper.SeedDates dates = seedHelper.computeGd5FinalActiveDates();
        HackathonDevSeedHelper.HackathonStructure structure = seedHelper.ensureHackathonStructure(
                Gd5NotAdvancedSeedConstants.SLUG_GD5_NOT_ADVANCED,
                "SEAL GĐ5 — Not advanced",
                HackathonStatus.ONGOING,
                "Seed GĐ5 — 1 team không ADVANCED vào CK → TEAM_NOT_IN_ROUND",
                new HackathonDevSeedHelper.PrelimState(false, true, true, true, 2, 2),
                new HackathonDevSeedHelper.FinalState(true, false),
                dates);

        Hackathon hackathon = structure.hackathon();
        Round prelim = structure.prelim();
        Round finalRound = structure.finalRound();
        Track track1 = structure.track1();
        seedHelper.syncHackathonCalendarFromDates(Gd5NotAdvancedSeedConstants.SLUG_GD5_NOT_ADVANCED, dates);
        seedHelper.repairHackathonForGd5Retest(hackathon, prelim, finalRound);

        User coordinator = seedHelper.requireCoordinator();
        Chapter chapter = seedHelper.requireChapter(Gd1SeedConstants.CHAPTER_FPT_HCM);
        LocalDateTime now = LocalDateTime.now();

        User l1 = seedHelper.upsertStudent(
                Gd5NotAdvancedSeedConstants.studentEmail(1), "GD5 NA Leader 1", chapter);
        User l2 = seedHelper.upsertStudent(
                Gd5NotAdvancedSeedConstants.studentEmail(2), "GD5 NA Leader 2", chapter);
        seedHelper.registerStudent(hackathon, l1);
        seedHelper.registerStudent(hackathon, l2);

        Team advanced = seedHelper.ensureActiveTeam(
                hackathon, Gd5NotAdvancedSeedConstants.TEAM_ADVANCED, l1, chapter, now);
        Team notAdvanced = seedHelper.ensureActiveTeam(
                hackathon, Gd5NotAdvancedSeedConstants.TEAM_NOT_ADVANCED, l2, chapter, now);
        seedHelper.ensureLottery(hackathon, prelim, track1, "BANG-A", advanced, coordinator, now);
        seedHelper.ensureLottery(hackathon, prelim, track1, "BANG-B", notAdvanced, coordinator, now);
        seedHelper.markAdvanced(advanced, prelim, finalRound, hackathon);

        log.info("""
                [Gd5NotAdvancedDataSeeder] slug={}
                  POST submission roundId=final với team not-advanced → TEAM_NOT_IN_ROUND
                """,
                Gd5NotAdvancedSeedConstants.SLUG_GD5_NOT_ADVANCED);
    }

    @Transactional
    public void repairForFeTesting() {
        if (!enabled) {
            return;
        }
        hackathonRepository.findBySlug(Gd5NotAdvancedSeedConstants.SLUG_GD5_NOT_ADVANCED).ifPresent(h -> {
            seedHelper.syncHackathonCalendarFromDates(
                    Gd5NotAdvancedSeedConstants.SLUG_GD5_NOT_ADVANCED,
                    seedHelper.computeGd5FinalActiveDates());
            ensureSeed();
        });
    }

    @Transactional
    public void resetAndSeed() {
        devSeedCleanup.purgeIfPresent(Gd5NotAdvancedSeedConstants.SLUG_GD5_NOT_ADVANCED);
        ensureSeed();
    }
}
