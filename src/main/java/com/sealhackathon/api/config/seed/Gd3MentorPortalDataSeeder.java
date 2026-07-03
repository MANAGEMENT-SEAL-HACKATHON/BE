package com.sealhackathon.api.config.seed;

import com.sealhackathon.api.chapters.entity.Chapter;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.rounds.entity.Round;
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
 * GĐ3 — mentor portal: prelim active, 2 đội có mentor assignment (G3-H03).
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class Gd3MentorPortalDataSeeder {

    private final HackathonDevSeedHelper seedHelper;
    private final HackathonRepository hackathonRepository;
    private final DevSeedCleanup devSeedCleanup;

    @Value("${app.seed.gd3.mentor-portal.enabled:true}")
    private boolean enabled;

    @Transactional
    public void ensureSeed() {
        if (!enabled) {
            log.info("[Gd3MentorPortalDataSeeder] Tắt");
            return;
        }

        HackathonDevSeedHelper.HackathonStructure structure = seedHelper.ensureHackathonStructure(
                Gd3MentorPortalSeedConstants.SLUG_GD3_MENTOR_PORTAL,
                "SEAL GĐ3 — Mentor portal",
                HackathonStatus.ONGOING,
                "Prelim active, 2 đội + mentor assignment (G3-H03)",
                new HackathonDevSeedHelper.PrelimState(true, true, false, false, 2, 4),
                new HackathonDevSeedHelper.FinalState(false, false),
                seedHelper.computeGd3ActivePrelimDates());

        Hackathon hackathon = structure.hackathon();
        Round prelim = structure.prelim();
        Track track1 = structure.track1();
        seedHelper.syncHackathonCalendarFromDates(
                Gd3MentorPortalSeedConstants.SLUG_GD3_MENTOR_PORTAL, seedHelper.computeGd3ActivePrelimDates());

        User coordinator = seedHelper.requireCoordinator();
        User mentor = seedHelper.requireMentor();
        Chapter hcm = seedHelper.requireChapter(Gd1SeedConstants.CHAPTER_FPT_HCM);
        LocalDateTime now = LocalDateTime.now();

        for (int i = 1; i <= 2; i++) {
            User leader = seedHelper.upsertStudent(
                    Gd3MentorPortalSeedConstants.studentEmail(i),
                    "GD3 MP Leader %d".formatted(i),
                    hcm);
            seedHelper.registerStudent(hackathon, leader);
            Team team = seedHelper.ensureActiveTeamForLeader(
                    hackathon, Gd3MentorPortalSeedConstants.teamName(i), leader, hcm, now);
            seedHelper.ensureTeamLocked(team, now);
            seedHelper.ensureLottery(hackathon, prelim, track1, "BANG-%d".formatted(i), team, coordinator, now);
            seedHelper.ensureMentorTeamAssignment(hackathon, prelim, team, mentor, coordinator, now);
        }

        log.info("[Gd3MentorPortalDataSeeder] slug={} hackathonId={} mentor={}",
                Gd3MentorPortalSeedConstants.SLUG_GD3_MENTOR_PORTAL,
                hackathon.getId(),
                Gd1SeedConstants.EMAIL_MENTOR);
    }

    @Transactional
    public void repairForFeTesting() {
        if (!enabled) {
            return;
        }
        hackathonRepository.findBySlug(Gd3MentorPortalSeedConstants.SLUG_GD3_MENTOR_PORTAL)
                .ifPresent(h -> seedHelper.syncHackathonCalendarFromDates(
                        Gd3MentorPortalSeedConstants.SLUG_GD3_MENTOR_PORTAL,
                        seedHelper.computeGd3ActivePrelimDates()));
    }

    @Transactional
    public void resetAndSeed() {
        devSeedCleanup.purgeIfPresent(Gd3MentorPortalSeedConstants.SLUG_GD3_MENTOR_PORTAL);
        ensureSeed();
    }
}
