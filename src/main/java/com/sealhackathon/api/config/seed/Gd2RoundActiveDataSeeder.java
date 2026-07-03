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

/**
 * GĐ2 — prelim {@code is_active=true}, lottery đã chạy → PATCH lottery → {@code ROUND_ALREADY_ACTIVE}.
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class Gd2RoundActiveDataSeeder {

    private static final int TEAM_COUNT = 4;

    private final HackathonDevSeedHelper seedHelper;
    private final HackathonRepository hackathonRepository;
    private final RoundRepository roundRepository;
    private final DevSeedCleanup devSeedCleanup;

    @Value("${app.seed.gd2.round-active.enabled:true}")
    private boolean enabled;

    @Transactional
    public void ensureSeed() {
        if (!enabled) {
            log.info("[Gd2RoundActiveDataSeeder] Tắt");
            return;
        }

        HackathonDevSeedHelper.SeedDates dates = seedHelper.computeGd3ActivePrelimDates();
        HackathonDevSeedHelper.HackathonStructure structure = seedHelper.ensureHackathonStructure(
                Gd2RoundActiveSeedConstants.SLUG_GD2_ROUND_ACTIVE,
                "SEAL GĐ2 — Round active",
                HackathonStatus.ONGOING,
                "Prelim active + lottery xong — không bốc thăm lại (B-N2)",
                new HackathonDevSeedHelper.PrelimState(true, true, false, false, 2, 4),
                new HackathonDevSeedHelper.FinalState(false, false),
                dates);

        Hackathon hackathon = structure.hackathon();
        Round prelim = structure.prelim();
        Track track1 = structure.track1();
        Track track2 = structure.track2();
        seedHelper.syncHackathonCalendarFromDates(Gd2RoundActiveSeedConstants.SLUG_GD2_ROUND_ACTIVE, dates);

        User coordinator = seedHelper.requireCoordinator();
        Chapter hcm = seedHelper.requireChapter(Gd1SeedConstants.CHAPTER_FPT_HCM);
        LocalDateTime now = LocalDateTime.now();

        for (int i = 1; i <= TEAM_COUNT; i++) {
            User leader = seedHelper.upsertStudent(
                    Gd2RoundActiveSeedConstants.studentEmail(i),
                    "GD2 RA Leader %d".formatted(i),
                    hcm);
            seedHelper.registerStudent(hackathon, leader);
            Team team = seedHelper.ensureActiveTeamForLeader(
                    hackathon, Gd2RoundActiveSeedConstants.teamName(i), leader, hcm, now);
            seedHelper.ensureTeamLocked(team, now);
            Track track = i <= 2 ? track1 : track2;
            String group = "BANG-" + ((i - 1) % 2 + 1);
            seedHelper.ensureLottery(hackathon, prelim, track, group, team, coordinator, now);
        }

        prelim.setIsActive(true);
        roundRepository.save(prelim);
        hackathon.setStatus(HackathonStatus.ONGOING);
        hackathonRepository.save(hackathon);

        log.info("""
                [Gd2RoundActiveDataSeeder] slug={} hackathonId={} prelimRoundId={}
                  PATCH lottery → ROUND_ALREADY_ACTIVE
                """,
                Gd2RoundActiveSeedConstants.SLUG_GD2_ROUND_ACTIVE,
                hackathon.getId(),
                prelim.getId());
    }

    @Transactional
    public void repairForFeTesting() {
        if (!enabled) {
            return;
        }
        hackathonRepository.findBySlug(Gd2RoundActiveSeedConstants.SLUG_GD2_ROUND_ACTIVE)
                .ifPresent(h -> {
                    HackathonDevSeedHelper.SeedDates dates = seedHelper.computeGd3ActivePrelimDates();
                    seedHelper.syncHackathonCalendarFromDates(
                            Gd2RoundActiveSeedConstants.SLUG_GD2_ROUND_ACTIVE, dates);
                    roundRepository.findByHackathon_IdOrderByExamAtAsc(h.getId()).stream()
                            .filter(r -> !Boolean.TRUE.equals(r.getIsFinal()))
                            .findFirst()
                            .ifPresent(prelim -> {
                                if (!Boolean.TRUE.equals(prelim.getIsActive())) {
                                    prelim.setIsActive(true);
                                    roundRepository.save(prelim);
                                }
                            });
                    if (h.getStatus() != HackathonStatus.ONGOING) {
                        h.setStatus(HackathonStatus.ONGOING);
                        hackathonRepository.save(h);
                    }
                });
    }

    @Transactional
    public void resetAndSeed() {
        devSeedCleanup.purgeIfPresent(Gd2RoundActiveSeedConstants.SLUG_GD2_ROUND_ACTIVE);
        ensureSeed();
    }
}
