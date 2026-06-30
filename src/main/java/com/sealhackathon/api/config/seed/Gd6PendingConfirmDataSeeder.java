package com.sealhackathon.api.config.seed;

import com.sealhackathon.api.chapters.entity.Chapter;
import com.sealhackathon.api.criteria.entity.Criteria;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.rounds.value_object.RoundType;
import com.sealhackathon.api.submissions.entity.Submission;
import com.sealhackathon.api.prizes.repository.PrizeRepository;
import com.sealhackathon.api.prizes.value_object.PrizeRank;
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
import java.util.ArrayList;
import java.util.List;

/**
 * Seed GĐ6 — hackathon {@link Gd6SeedConstants#SLUG_GD6_PENDING_CONFIRM}.
 *
 * <p>Doc: {@code docs/testing/gd6-full-test-matrix-and-seeds.md} § Profile 0
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class Gd6PendingConfirmDataSeeder {

    private static final float[] TEAM_SCORES = {9.2f, 8.6f, 8.1f};

    private final HackathonDevSeedHelper seedHelper;
    private final HackathonRepository hackathonRepository;
    private final RoundRepository roundRepository;
    private final TeamRepository teamRepository;
    private final PrizeRepository prizeRepository;
    private final DevSeedCleanup devSeedCleanup;

    @Value("${app.seed.gd6.enabled:true}")
    private boolean enabled;

    /** Reset về PENDING_CONFIRM sau khi test confirm → FINISHED (restart BE). */
    @Transactional
    public void repairForFullChainRetest() {
        if (!enabled) {
            return;
        }
        hackathonRepository.findBySlug(Gd6SeedConstants.SLUG_GD6_PENDING_CONFIRM).ifPresent(hackathon -> {
            Round prelim = roundRepository.findByHackathon_IdOrderByExamAtAsc(hackathon.getId()).stream()
                    .filter(r -> r.getRoundType() == RoundType.PRELIMINARY)
                    .findFirst()
                    .orElse(null);
            Round finalRound = roundRepository.findByHackathon_IdOrderByExamAtAsc(hackathon.getId()).stream()
                    .filter(r -> Boolean.TRUE.equals(r.getIsFinal()))
                    .findFirst()
                    .orElse(null);
            if (prelim == null || finalRound == null) {
                return;
            }
            if (hackathon.getStatus() == HackathonStatus.FINISHED) {
                seedHelper.repairHackathonForGd6Retest(hackathon, prelim, finalRound);
                reseedProfile0FirstPrize(hackathon, finalRound);
                log.info("[Gd6PendingConfirmDataSeeder] repairForFullChainRetest — {} → PENDING_CONFIRM",
                        Gd6SeedConstants.SLUG_GD6_PENDING_CONFIRM);
            }
        });
    }

    @Transactional
    public void ensureSeed() {
        if (!enabled) {
            log.info("[Gd6PendingConfirmDataSeeder] Tắt (app.seed.gd6.enabled=false)");
            return;
        }

        HackathonDevSeedHelper.HackathonStructure structure = seedHelper.ensureHackathonStructure(
                Gd6SeedConstants.SLUG_GD6_PENDING_CONFIRM,
                "SEAL GĐ6 — Pending confirm",
                HackathonStatus.PENDING_CONFIRM,
                "Seed E2E GĐ6 — PENDING_CONFIRM, CK locked, 3 đội + FIRST prize",
                new HackathonDevSeedHelper.PrelimState(false, true, true, true, 1, 2),
                new HackathonDevSeedHelper.FinalState(true, true),
                seedHelper.computeGd6PendingConfirmDates());

        Hackathon hackathon = structure.hackathon();
        Round prelim = structure.prelim();
        Round finalRound = structure.finalRound();
        Track track1 = structure.track1();
        Track track2 = structure.track2();

        seedHelper.syncHackathonCalendarFromDates(
                Gd6SeedConstants.SLUG_GD6_PENDING_CONFIRM, seedHelper.computeGd6PendingConfirmDates());

        hackathon = hackathonRepository.findById(hackathon.getId()).orElse(hackathon);
        prelim = roundRepository.findById(prelim.getId()).orElse(prelim);
        finalRound = roundRepository.findById(finalRound.getId()).orElse(finalRound);

        hackathon.setIndividualRankingEnabled(true);
        hackathon.setStatus(HackathonStatus.PENDING_CONFIRM);
        hackathonRepository.save(hackathon);

        User coordinator = seedHelper.requireCoordinator();
        User guestJudge = seedHelper.requireGuestJudge();
        Chapter hcm = seedHelper.requireChapter(Gd1SeedConstants.CHAPTER_FPT_HCM);
        Chapter hn = seedHelper.requireChapter(Gd1SeedConstants.CHAPTER_FPT_HN);
        LocalDateTime now = LocalDateTime.now();

        seedHelper.ensureGuestJudgeInvitation(hackathon, guestJudge, coordinator);

        String[] teamNames = {
                Gd6SeedConstants.TEAM_01,
                Gd6SeedConstants.TEAM_02,
                Gd6SeedConstants.TEAM_03
        };
        Chapter[] chapters = {hcm, hcm, hn};
        List<Team> teams = new ArrayList<>();

        for (int i = 0; i < teamNames.length; i++) {
            int idx = i + 1;
            User leader = seedHelper.upsertStudent(
                    Gd6SeedConstants.studentEmail(idx),
                    Gd6SeedConstants.studentDisplayName(idx),
                    chapters[i]);
            seedHelper.registerStudent(hackathon, leader);
            Team team = seedHelper.ensureActiveTeam(hackathon, teamNames[i], leader, chapters[i], now);
            Track track = (idx % 2 == 1) ? track1 : track2;
            seedHelper.ensureLottery(hackathon, prelim, track, "BANG-" + ((idx % 2) + 1), team, coordinator, now);
            seedHelper.markAdvanced(team, prelim, finalRound, hackathon);
            teams.add(team);
        }

        List<Criteria> finalCriteria = seedHelper.listFinalCriteria(finalRound);
        List<Submission> submissions = new ArrayList<>();
        for (int i = 0; i < teams.size(); i++) {
            Submission sub = seedHelper.ensureFinalSubmission(
                    hackathon, finalRound, teams.get(i),
                    "https://github.com/seal-warriors/gd6-team%02d".formatted(i + 1));
            submissions.add(sub);
            float score = TEAM_SCORES[i];
            for (Criteria c : finalCriteria) {
                seedHelper.ensureNormalScore(sub, c, guestJudge, score, true);
            }
        }

        seedHelper.ensureFirstPrize(hackathon, finalRound, teams.get(0), coordinator);
        seedHelper.ensureFinalGuestJudgeAssignment(hackathon, finalRound);

        log.info("""
                [Gd6PendingConfirmDataSeeder] slug={} hackathonId={} prelimRoundId={} finalRoundId={}
                  teams: {} | {} | {}
                  students: {} … {} password={}
                  guestJudge={} status=PENDING_CONFIRM FIRST prize on team01
                """,
                Gd6SeedConstants.SLUG_GD6_PENDING_CONFIRM,
                hackathon.getId(),
                prelim.getId(),
                finalRound.getId(),
                teams.get(0).getId(),
                teams.get(1).getId(),
                teams.get(2).getId(),
                Gd6SeedConstants.studentEmail(1),
                Gd6SeedConstants.studentEmail(3),
                DevSeedCatalog.DEV_STUDENT_PASSWORD,
                Gd1SeedConstants.EMAIL_GUEST_JUDGE);
    }

    /** Reset Profile 0 về 3 team XH + đúng 1 giải FIRST (R1-api / P5-api Playwright). */
    @Transactional
    public void repairForApiMatrixReadiness() {
        if (!enabled) {
            return;
        }
        repairForFullChainRetest();
        hackathonRepository.findBySlug(Gd6SeedConstants.SLUG_GD6_PENDING_CONFIRM).ifPresent(hackathon -> {
            if (hackathon.getStatus() != HackathonStatus.PENDING_CONFIRM) {
                return;
            }
            Round finalRound = roundRepository.findByHackathon_IdOrderByExamAtAsc(hackathon.getId()).stream()
                    .filter(r -> Boolean.TRUE.equals(r.getIsFinal()))
                    .findFirst()
                    .orElse(null);
            if (finalRound == null) {
                return;
            }
            var prizes = prizeRepository.findByRound_Hackathon_IdOrderByAwardedAtDesc(hackathon.getId());
            boolean baseline = prizes.size() == 1
                    && prizeRepository.existsByHackathonIdAndPrizeRank(hackathon.getId(), PrizeRank.FIRST);
            if (!baseline) {
                reseedProfile0FirstPrize(hackathon, finalRound);
                log.info("[Gd6PendingConfirmDataSeeder] repairForApiMatrixReadiness — Profile 0 prizes → FIRST only");
            }
        });
    }

    private void reseedProfile0FirstPrize(Hackathon hackathon, Round finalRound) {
        Team team01 = teamRepository
                .findByHackathon_IdAndTeamNameIgnoreCase(hackathon.getId(), Gd6SeedConstants.TEAM_01)
                .orElse(null);
        if (team01 == null) {
            return;
        }
        seedHelper.clearGd6Prizes(hackathon.getId());
        seedHelper.ensureFirstPrize(hackathon, finalRound, team01, seedHelper.requireCoordinator());
    }

    /** Đồng bộ lịch đã kết thúc + reset FINISHED → PENDING_CONFIRM khi cần. */
    @Transactional
    public void repairForFeTesting() {
        if (!enabled) {
            return;
        }
        repairForFullChainRetest();
        hackathonRepository.findBySlug(Gd6SeedConstants.SLUG_GD6_PENDING_CONFIRM).ifPresent(hackathon -> {
            boolean synced = seedHelper.syncHackathonCalendarFromDates(
                    Gd6SeedConstants.SLUG_GD6_PENDING_CONFIRM, seedHelper.computeGd6PendingConfirmDates());
            if (synced) {
                Round finalRound = roundRepository.findByHackathon_IdOrderByExamAtAsc(hackathon.getId()).stream()
                        .filter(r -> Boolean.TRUE.equals(r.getIsFinal()))
                        .findFirst()
                        .orElse(null);
                log.info(
                        "[Gd6PendingConfirmDataSeeder] FE repair — final ended slug={} deadline={}",
                        Gd6SeedConstants.SLUG_GD6_PENDING_CONFIRM,
                        finalRound != null ? finalRound.getSubmissionDeadline() : null);
            }
        });
    }

    @Transactional
    public void resetAndSeed() {
        devSeedCleanup.purgeIfPresent(Gd6SeedConstants.SLUG_GD6_PENDING_CONFIRM);
        ensureSeed();
    }
}
