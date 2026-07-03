package com.sealhackathon.api.config.seed;

import com.sealhackathon.api.chapters.entity.Chapter;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.teams.entity.Team;
import com.sealhackathon.api.teams.repository.TeamRepository;
import com.sealhackathon.api.teams.value_object.TeamStatus;
import com.sealhackathon.api.users.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * GĐ2 — đội ACTIVE chưa khóa → lottery gate TEAM_NOT_LOCKED.
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class Gd2LotteryNotLockedDataSeeder {

    private final HackathonDevSeedHelper seedHelper;
    private final HackathonRepository hackathonRepository;
    private final TeamRepository teamRepository;
    private final DevSeedCleanup devSeedCleanup;

    @Value("${app.seed.gd2.lottery-not-locked.enabled:true}")
    private boolean enabled;

    @Transactional
    public void ensureSeed() {
        if (!enabled) {
            log.info("[Gd2LotteryNotLockedDataSeeder] Tắt");
            return;
        }

        HackathonDevSeedHelper.SeedDates dates = seedHelper.computeGd2RegistrationOpenDates();
        HackathonDevSeedHelper.HackathonStructure structure = seedHelper.ensureHackathonStructure(
                Gd2LotteryNotLockedSeedConstants.SLUG_GD2_LOTTERY_NOT_LOCKED,
                "SEAL GĐ2 — Lottery not locked",
                HackathonStatus.ONGOING,
                "3 đội ACTIVE chưa lock — lottery bị chặn (G2-N02)",
                new HackathonDevSeedHelper.PrelimState(false, false, false, false, 2, 4),
                new HackathonDevSeedHelper.FinalState(false, false),
                dates);

        Hackathon hackathon = structure.hackathon();
        seedHelper.syncHackathonCalendarFromDates(
                Gd2LotteryNotLockedSeedConstants.SLUG_GD2_LOTTERY_NOT_LOCKED, dates);

        Chapter hcm = seedHelper.requireChapter(Gd1SeedConstants.CHAPTER_FPT_HCM);
        LocalDateTime now = LocalDateTime.now();
        for (int i = 1; i <= 3; i++) {
            User leader = seedHelper.upsertStudent(
                    Gd2LotteryNotLockedSeedConstants.studentEmail(i),
                    "GD2 NL Leader %d".formatted(i),
                    hcm);
            seedHelper.registerStudent(hackathon, leader);
            Team team = seedHelper.ensureActiveTeam(
                    hackathon, Gd2LotteryNotLockedSeedConstants.teamName(i), leader, hcm, now);
            if (Boolean.TRUE.equals(team.getIsLocked())) {
                team.setIsLocked(false);
                team.setLockedAt(null);
                teamRepository.save(team);
            }
        }

        log.info("[Gd2LotteryNotLockedDataSeeder] slug={} hackathonId={}",
                Gd2LotteryNotLockedSeedConstants.SLUG_GD2_LOTTERY_NOT_LOCKED, hackathon.getId());
    }

    @Transactional
    public void repairForFeTesting() {
        if (!enabled) {
            return;
        }
        hackathonRepository.findBySlug(Gd2LotteryNotLockedSeedConstants.SLUG_GD2_LOTTERY_NOT_LOCKED)
                .ifPresent(h -> {
                    HackathonDevSeedHelper.SeedDates dates = seedHelper.computeGd2RegistrationOpenDates();
                    seedHelper.syncHackathonCalendarFromDates(
                            Gd2LotteryNotLockedSeedConstants.SLUG_GD2_LOTTERY_NOT_LOCKED, dates);
                    teamRepository.findByHackathon_Id(h.getId()).stream()
                            .filter(t -> t.getStatus() == TeamStatus.ACTIVE)
                            .forEach(team -> {
                                team.setIsLocked(false);
                                team.setLockedAt(null);
                                teamRepository.save(team);
                            });
                });
    }

    @Transactional
    public void resetAndSeed() {
        devSeedCleanup.purgeIfPresent(Gd2LotteryNotLockedSeedConstants.SLUG_GD2_LOTTERY_NOT_LOCKED);
        ensureSeed();
    }
}
