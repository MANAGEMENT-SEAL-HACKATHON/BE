package com.sealhackathon.api.config.seed;

import com.sealhackathon.api.chapters.entity.Chapter;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.rounds.value_object.LateSubmissionPolicy;
import com.sealhackathon.api.teams.entity.Team;
import com.sealhackathon.api.teams.repository.TeamRepository;
import com.sealhackathon.api.tracks.entity.Track;
import com.sealhackathon.api.users.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** GĐ2 — registration_end đã qua, đội chưa khóa → REGISTRATION_CLOSED. */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class Gd2RegistrationClosedDataSeeder {

    private final HackathonDevSeedHelper seedHelper;
    private final HackathonRepository hackathonRepository;
    private final RoundRepository roundRepository;
    private final TeamRepository teamRepository;
    private final DevSeedCleanup devSeedCleanup;

    @Value("${app.seed.gd2.registration-closed.enabled:true}")
    private boolean enabled;

    @Transactional
    public void ensureSeed() {
        if (!enabled) {
            log.info("[Gd2RegistrationClosedDataSeeder] Tắt");
            return;
        }

        HackathonDevSeedHelper.SeedDates dates = seedHelper.computeGd2RegistrationClosedDates();
        HackathonDevSeedHelper.HackathonStructure structure = seedHelper.ensureHackathonStructure(
                Gd2RegistrationClosedSeedConstants.SLUG_GD2_REGISTRATION_CLOSED,
                "SEAL GĐ2 — Registration closed",
                HackathonStatus.ONGOING,
                "Seed GĐ2 — registration_end đã qua → REGISTRATION_CLOSED",
                new HackathonDevSeedHelper.PrelimState(false, false, false, false, 2, 4),
                new HackathonDevSeedHelper.FinalState(false, false),
                dates);

        Hackathon hackathon = structure.hackathon();
        seedHelper.syncHackathonCalendarFromDates(
                Gd2RegistrationClosedSeedConstants.SLUG_GD2_REGISTRATION_CLOSED, dates);

        User coordinator = seedHelper.requireCoordinator();
        Chapter hcm = seedHelper.requireChapter(Gd1SeedConstants.CHAPTER_FPT_HCM);
        LocalDateTime now = LocalDateTime.now();
        User leader = seedHelper.upsertStudent(
                Gd2RegistrationClosedSeedConstants.studentEmail(),
                "GD2 RC Leader",
                hcm);
        seedHelper.registerStudent(hackathon, leader);
        seedHelper.ensureTeam(hackathon, Gd2RegistrationClosedSeedConstants.TEAM_UNLOCKED, leader, hcm,
                com.sealhackathon.api.teams.value_object.TeamStatus.ACTIVE, false, now);

        log.info("""
                [Gd2RegistrationClosedDataSeeder] slug={} registrationEnd={}
                  POST /teams → REGISTRATION_CLOSED
                """,
                Gd2RegistrationClosedSeedConstants.SLUG_GD2_REGISTRATION_CLOSED,
                hackathon.getRegistrationEnd());
    }

    @Transactional
    public void repairForFeTesting() {
        if (!enabled) {
            return;
        }
        hackathonRepository.findBySlug(Gd2RegistrationClosedSeedConstants.SLUG_GD2_REGISTRATION_CLOSED)
                .ifPresent(h -> {
                    HackathonDevSeedHelper.SeedDates dates = seedHelper.computeGd2RegistrationClosedDates();
                    seedHelper.syncHackathonCalendarFromDates(
                            Gd2RegistrationClosedSeedConstants.SLUG_GD2_REGISTRATION_CLOSED, dates);
                    if (h.getStatus() != HackathonStatus.ONGOING) {
                        h.setStatus(HackathonStatus.ONGOING);
                        hackathonRepository.save(h);
                    }
                    if (teamRepository.findByHackathon_Id(h.getId()).isEmpty()) {
                        ensureSeed();
                    }
                });
    }

    @Transactional
    public void resetAndSeed() {
        devSeedCleanup.purgeIfPresent(Gd2RegistrationClosedSeedConstants.SLUG_GD2_REGISTRATION_CLOSED);
        ensureSeed();
    }
}
