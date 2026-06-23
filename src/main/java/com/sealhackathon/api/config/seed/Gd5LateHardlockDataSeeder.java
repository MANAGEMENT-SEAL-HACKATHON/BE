package com.sealhackathon.api.config.seed;

import com.sealhackathon.api.chapters.entity.Chapter;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.submissions.repository.SubmissionRepository;
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
 * Seed GĐ5 — CK active, deadline đã qua, POST nộp → {@code REJECTED} (HARD_LOCK).
 *
 * <p>Doc: {@code docs/testing/gd5-full-test-matrix-and-seeds.md} § Profile E
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class Gd5LateHardlockDataSeeder {

    private final HackathonDevSeedHelper seedHelper;
    private final HackathonRepository hackathonRepository;
    private final RoundRepository roundRepository;
    private final SubmissionRepository submissionRepository;
    private final DevSeedCleanup devSeedCleanup;

    @Value("${app.seed.gd5.late-hardlock.enabled:true}")
    private boolean enabled;

    @Transactional
    public void ensureSeed() {
        if (!enabled) {
            log.info("[Gd5LateHardlockDataSeeder] Tắt (app.seed.gd5.late-hardlock.enabled=false)");
            return;
        }

        HackathonDevSeedHelper.HackathonStructure structure = seedHelper.ensureHackathonStructure(
                Gd5LateHardlockSeedConstants.SLUG_GD5_LATE_HARDLOCK,
                "SEAL GĐ5 — CK late HARD_LOCK",
                HackathonStatus.ONGOING,
                "Seed GĐ5 — CK active, deadline đã qua, POST /submissions → REJECTED",
                new HackathonDevSeedHelper.PrelimState(false, true, true, true, 2, 2),
                new HackathonDevSeedHelper.FinalState(true, false),
                seedHelper.computeGd5LateHardLockDates());

        Hackathon hackathon = structure.hackathon();
        Round prelim = structure.prelim();
        Round finalRound = structure.finalRound();
        Track track1 = structure.track1();
        Track track2 = structure.track2();

        seedHelper.syncHackathonCalendarFromDates(
                Gd5LateHardlockSeedConstants.SLUG_GD5_LATE_HARDLOCK,
                seedHelper.computeGd5LateHardLockDates());
        seedHelper.repairHackathonForGd5LateHardLockRetest(hackathon, prelim, finalRound);
        finalRound = loadFinal(hackathon.getId());

        User coordinator = seedHelper.requireCoordinator();
        User guestJudge = seedHelper.requireGuestJudge();
        Chapter chapter = seedHelper.requireChapter(Gd1SeedConstants.CHAPTER_FPT_HCM);
        LocalDateTime now = LocalDateTime.now();

        seedHelper.ensureGuestJudgeInvitation(hackathon, guestJudge, coordinator);

        List<Team> teams = new ArrayList<>();
        for (int i = 0; i < Gd5LateHardlockSeedConstants.TEAM_NAMES.length; i++) {
            int idx = i + 1;
            User leader = seedHelper.upsertStudent(
                    Gd5LateHardlockSeedConstants.studentEmail(idx),
                    Gd5LateHardlockSeedConstants.studentDisplayName(idx),
                    chapter);
            seedHelper.registerStudent(hackathon, leader);
            Team team = seedHelper.ensureActiveTeam(
                    hackathon, Gd5LateHardlockSeedConstants.TEAM_NAMES[i], leader, chapter, now);
            Track track = (idx % 2 == 1) ? track1 : track2;
            String group = "BANG-" + ((idx % 2) + 1);
            seedHelper.ensureLottery(hackathon, prelim, track, group, team, coordinator, now);
            seedHelper.markAdvanced(team, prelim, finalRound, hackathon);
            teams.add(team);
        }

        log.info("""
                [Gd5LateHardlockDataSeeder] slug={} finalRoundId={} deadline={}
                  CK active, 0 submission — POST /submissions sau deadline → status REJECTED
                  students: {} … {} password={}
                """,
                Gd5LateHardlockSeedConstants.SLUG_GD5_LATE_HARDLOCK,
                finalRound.getId(),
                finalRound.getSubmissionDeadline(),
                Gd5LateHardlockSeedConstants.studentEmail(1),
                Gd5LateHardlockSeedConstants.studentEmail(4),
                DevSeedCatalog.DEV_STUDENT_PASSWORD);
    }

    @Transactional
    public void repairForFeTesting() {
        if (!enabled) {
            return;
        }
        hackathonRepository.findBySlug(Gd5LateHardlockSeedConstants.SLUG_GD5_LATE_HARDLOCK).ifPresent(h -> {
            Round prelim = loadPrelim(h.getId());
            Round finalRound = loadFinal(h.getId());
            seedHelper.syncHackathonCalendarFromDates(
                    Gd5LateHardlockSeedConstants.SLUG_GD5_LATE_HARDLOCK,
                    seedHelper.computeGd5LateHardLockDates());
            seedHelper.repairHackathonForGd5LateHardLockRetest(h, prelim, finalRound);
            finalRound = loadFinal(h.getId());
            if (submissionRepository.countByRoundId(finalRound.getId()) > 0) {
                seedHelper.clearFinalRoundArtifacts(h.getId());
            }
        });
    }

    @Transactional
    public void resetAndSeed() {
        devSeedCleanup.purgeIfPresent(Gd5LateHardlockSeedConstants.SLUG_GD5_LATE_HARDLOCK);
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
