package com.sealhackathon.api.config.seed;

import com.sealhackathon.api.chapters.entity.Chapter;
import com.sealhackathon.api.criteria.entity.Criteria;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.submissions.entity.Submission;
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
 * Seed GĐ5 — hackathon {@link Gd5SeedConstants#SLUG_GD5_FINAL_ACTIVE}.
 *
 * <p>Doc: {@code docs/testing/gd5-full-test-matrix-and-seeds.md} § Profile 0
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class Gd5FinalRoundDataSeeder {

    private final HackathonDevSeedHelper seedHelper;
    private final HackathonRepository hackathonRepository;
    private final RoundRepository roundRepository;
    private final DevSeedCleanup devSeedCleanup;

    @Value("${app.seed.gd5.enabled:true}")
    private boolean enabled;

    @Transactional
    public void ensureSeed() {
        if (!enabled) {
            log.info("[Gd5FinalRoundDataSeeder] Tắt (app.seed.gd5.enabled=false)");
            return;
        }

        HackathonDevSeedHelper.HackathonStructure structure = seedHelper.ensureHackathonStructure(
                Gd5SeedConstants.SLUG_GD5_FINAL_ACTIVE,
                "SEAL GĐ5 — Chung kết active",
                HackathonStatus.ONGOING,
                "Seed E2E GĐ5 — CK active, 4 đội ADVANCED, guest judge CK",
                new HackathonDevSeedHelper.PrelimState(false, true, true, true, 1, 2),
                new HackathonDevSeedHelper.FinalState(true, false),
                seedHelper.computeGd5FinalActiveDates());

        Hackathon hackathon = structure.hackathon();
        Round prelim = structure.prelim();
        Round finalRound = structure.finalRound();
        Track track1 = structure.track1();
        Track track2 = structure.track2();

        seedHelper.syncHackathonCalendarFromDates(
                Gd5SeedConstants.SLUG_GD5_FINAL_ACTIVE, seedHelper.computeGd5FinalActiveDates());
        seedHelper.repairHackathonForGd5Retest(hackathon, prelim, finalRound);

        User coordinator = seedHelper.requireCoordinator();
        User guestJudge = seedHelper.requireGuestJudge();
        Chapter chapter = seedHelper.requireChapter(Gd1SeedConstants.CHAPTER_FPT_HCM);
        LocalDateTime now = LocalDateTime.now();

        seedHelper.ensureGuestJudgeInvitation(hackathon, guestJudge, coordinator);

        String[] teamNames = {
                Gd5SeedConstants.TEAM_01,
                Gd5SeedConstants.TEAM_02,
                Gd5SeedConstants.TEAM_03,
                Gd5SeedConstants.TEAM_04
        };
        List<Team> teams = new ArrayList<>();
        for (int i = 0; i < teamNames.length; i++) {
            int idx = i + 1;
            User leader = seedHelper.upsertStudent(
                    Gd5SeedConstants.studentEmail(idx),
                    Gd5SeedConstants.studentDisplayName(idx),
                    chapter);
            seedHelper.registerStudent(hackathon, leader);
            Team team = seedHelper.ensureActiveTeam(hackathon, teamNames[i], leader, chapter, now);
            Track track = (idx % 2 == 1) ? track1 : track2;
            String group = "BANG-" + ((idx % 2) + 1);
            seedHelper.ensureLottery(hackathon, prelim, track, group, team, coordinator, now);
            seedHelper.markAdvanced(team, prelim, finalRound, hackathon);
            teams.add(team);
        }

        List<Criteria> finalCriteria = seedHelper.listFinalCriteria(finalRound);

        Submission sub1 = seedHelper.ensureFinalSubmission(
                hackathon, finalRound, teams.get(0), "https://github.com/seal-warriors/gd5-team01");
        Submission sub2 = seedHelper.ensureFinalSubmission(
                hackathon, finalRound, teams.get(1), "https://github.com/seal-warriors/gd5-team02");

        for (Criteria c : finalCriteria) {
            seedHelper.ensureNormalScore(sub1, c, guestJudge, 8.0f, false);
        }

        log.info("""
                [Gd5FinalRoundDataSeeder] slug={} hackathonId={} prelimRoundId={} finalRoundId={}
                  teams: {} | {} | {} | {}
                  finalSubmissionId(t1)={} finalSubmissionId(t2)={}
                  students: {} … {} password={}
                  guestJudge={} (FINAL_EXTERNAL on CK)
                """,
                Gd5SeedConstants.SLUG_GD5_FINAL_ACTIVE,
                hackathon.getId(),
                prelim.getId(),
                finalRound.getId(),
                teams.get(0).getId(),
                teams.get(1).getId(),
                teams.get(2).getId(),
                teams.get(3).getId(),
                sub1.getId(),
                sub2.getId(),
                Gd5SeedConstants.studentEmail(3),
                Gd5SeedConstants.studentEmail(1),
                DevSeedCatalog.DEV_STUDENT_PASSWORD,
                Gd1SeedConstants.EMAIL_GUEST_JUDGE);
    }

    /** Đồng bộ lịch CK đang mở theo giờ máy — gọi sau repairAll mỗi lần start BE. */
    @Transactional
    public void repairForFeTesting() {
        if (!enabled) {
            return;
        }
        var maybeHackathon = hackathonRepository.findBySlug(Gd5SeedConstants.SLUG_GD5_FINAL_ACTIVE);
        if (maybeHackathon.isEmpty()) {
            return;
        }
        Hackathon hackathon = maybeHackathon.get();
        Round prelim = roundRepository.findByHackathon_IdOrderByExamAtAsc(hackathon.getId()).stream()
                .filter(r -> !Boolean.TRUE.equals(r.getIsFinal()))
                .findFirst()
                .orElse(null);
        Round finalRound = roundRepository.findByHackathon_IdOrderByExamAtAsc(hackathon.getId()).stream()
                .filter(r -> Boolean.TRUE.equals(r.getIsFinal()))
                .findFirst()
                .orElse(null);
        if (prelim == null || finalRound == null) {
            return;
        }
        seedHelper.repairGd5FeTestingScheduleAndState(hackathon, prelim, finalRound);
        finalRound = roundRepository.findById(finalRound.getId()).orElse(finalRound);
        log.info(
                "[Gd5FinalRoundDataSeeder] FE repair — final deadline={} slug={}",
                finalRound.getSubmissionDeadline(),
                Gd5SeedConstants.SLUG_GD5_FINAL_ACTIVE);
    }

    /** Idempotent recreate — purge rồi seed lại khi cần DB sạch. */
    @Transactional
    public void resetAndSeed() {
        devSeedCleanup.purgeIfPresent(Gd5SeedConstants.SLUG_GD5_FINAL_ACTIVE);
        ensureSeed();
    }
}
