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
import java.util.ArrayList;
import java.util.List;

/**
 * Seed GĐ5 — CK active, 4 đội ADVANCED, chưa nộp CK.
 *
 * <p>Doc: {@code docs/testing/gd5-full-test-matrix-and-seeds.md} § Profile A
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class Gd5SubmitOpenDataSeeder {

    private final HackathonDevSeedHelper seedHelper;
    private final HackathonRepository hackathonRepository;
    private final RoundRepository roundRepository;
    private final DevSeedCleanup devSeedCleanup;

    @Value("${app.seed.gd5.submit-open.enabled:true}")
    private boolean enabled;

    @Transactional
    public void ensureSeed() {
        if (!enabled) {
            log.info("[Gd5SubmitOpenDataSeeder] Tắt (app.seed.gd5.submit-open.enabled=false)");
            return;
        }

        HackathonDevSeedHelper.HackathonStructure structure = seedHelper.ensureHackathonStructure(
                Gd5SubmitOpenSeedConstants.SLUG_GD5_SUBMIT_OPEN,
                "SEAL GĐ5 — Submit open",
                HackathonStatus.ONGOING,
                "Seed GĐ5 — CK active, 4 ADVANCED, chưa có submission CK",
                new HackathonDevSeedHelper.PrelimState(false, true, true, true, 2, 2),
                new HackathonDevSeedHelper.FinalState(true, false),
                seedHelper.computeGd5FinalActiveDates());

        Hackathon hackathon = structure.hackathon();
        Round prelim = structure.prelim();
        Round finalRound = structure.finalRound();
        Track track1 = structure.track1();
        Track track2 = structure.track2();

        seedHelper.syncHackathonCalendarFromDates(
                Gd5SubmitOpenSeedConstants.SLUG_GD5_SUBMIT_OPEN, seedHelper.computeGd5FinalActiveDates());
        seedHelper.repairHackathonForGd5SubmitOpenRetest(hackathon, prelim, finalRound);

        User coordinator = seedHelper.requireCoordinator();
        User guestJudge = seedHelper.requireGuestJudge();
        Chapter chapter = seedHelper.requireChapter(Gd1SeedConstants.CHAPTER_FPT_HCM);
        LocalDateTime now = LocalDateTime.now();

        seedHelper.ensureGuestJudgeInvitation(hackathon, guestJudge, coordinator);

        List<Team> teams = new ArrayList<>();
        for (int i = 0; i < Gd5SubmitOpenSeedConstants.TEAM_NAMES.length; i++) {
            int idx = i + 1;
            User leader = seedHelper.upsertStudent(
                    Gd5SubmitOpenSeedConstants.studentEmail(idx),
                    Gd5SubmitOpenSeedConstants.studentDisplayName(idx),
                    chapter);
            seedHelper.registerStudent(hackathon, leader);
            Team team = seedHelper.ensureActiveTeam(
                    hackathon, Gd5SubmitOpenSeedConstants.TEAM_NAMES[i], leader, chapter, now);
            Track track = (idx % 2 == 1) ? track1 : track2;
            String group = "BANG-" + ((idx % 2) + 1);
            seedHelper.ensureLottery(hackathon, prelim, track, group, team, coordinator, now);
            seedHelper.markAdvanced(team, prelim, finalRound, hackathon);
            teams.add(team);
        }

        log.info("""
                [Gd5SubmitOpenDataSeeder] slug={} finalRoundId={}
                  4 ADVANCED, 0 submission CK — demo POST /submissions
                  students: {} … {} password={}
                """,
                Gd5SubmitOpenSeedConstants.SLUG_GD5_SUBMIT_OPEN,
                finalRound.getId(),
                Gd5SubmitOpenSeedConstants.studentEmail(1),
                Gd5SubmitOpenSeedConstants.studentEmail(4),
                DevSeedCatalog.DEV_STUDENT_PASSWORD);
    }

    @Transactional
    public void repairForFeTesting() {
        if (!enabled) {
            return;
        }
        hackathonRepository.findBySlug(Gd5SubmitOpenSeedConstants.SLUG_GD5_SUBMIT_OPEN).ifPresent(h -> {
            Round prelim = loadPrelim(h.getId());
            Round finalRound = loadFinal(h.getId());
            seedHelper.repairHackathonForGd5SubmitOpenRetest(h, prelim, finalRound);
        });
    }

    @Transactional
    public void resetAndSeed() {
        devSeedCleanup.purgeIfPresent(Gd5SubmitOpenSeedConstants.SLUG_GD5_SUBMIT_OPEN);
        ensureSeed();
    }

    private Round loadPrelim(Integer hackathonId) {
        return roundRepository.findByHackathon_IdOrderByExamAtAsc(hackathonId).stream()
                .filter(r -> !Boolean.TRUE.equals(r.getIsFinal()))
                .findFirst()
                .orElseThrow();
    }

    private Round loadFinal(Integer hackathonId) {
        return roundRepository.findByHackathon_IdOrderByExamAtAsc(hackathonId).stream()
                .filter(r -> Boolean.TRUE.equals(r.getIsFinal()))
                .findFirst()
                .orElseThrow();
    }
}
