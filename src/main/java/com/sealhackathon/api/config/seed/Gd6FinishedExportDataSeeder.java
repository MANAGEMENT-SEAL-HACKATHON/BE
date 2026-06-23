package com.sealhackathon.api.config.seed;

import com.sealhackathon.api.chapters.entity.Chapter;
import com.sealhackathon.api.chapter_rankings.service.ChapterRankingService;
import com.sealhackathon.api.criteria.entity.Criteria;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.individual_rankings.service.IndividualRankingService;
import com.sealhackathon.api.prizes.repository.PrizeRepository;
import com.sealhackathon.api.prizes.value_object.PrizeRank;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.submissions.entity.Submission;
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
 * Seed GĐ6 — {@code FINISHED}, chapter/individual rankings + export.
 *
 * <p>Doc: {@code docs/testing/gd6-full-test-matrix-and-seeds.md} § Profile C
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class Gd6FinishedExportDataSeeder {

    private final HackathonDevSeedHelper seedHelper;
    private final HackathonRepository hackathonRepository;
    private final RoundRepository roundRepository;
    private final TeamRepository teamRepository;
    private final PrizeRepository prizeRepository;
    private final ChapterRankingService chapterRankingService;
    private final IndividualRankingService individualRankingService;
    private final DevSeedCleanup devSeedCleanup;

    @Value("${app.seed.gd6.finished-export.enabled:true}")
    private boolean enabled;

    @Transactional
    public void ensureSeed() {
        if (!enabled) {
            log.info("[Gd6FinishedExportDataSeeder] Tắt (app.seed.gd6.finished-export.enabled=false)");
            return;
        }

        HackathonDevSeedHelper.HackathonStructure structure = seedHelper.ensureHackathonStructure(
                Gd6FinishedExportSeedConstants.SLUG_GD6_FINISHED_EXPORT,
                "SEAL GĐ6 — Finished export",
                HackathonStatus.FINISHED,
                "Seed GĐ6 — FINISHED, rankings persisted, test export CSV",
                new HackathonDevSeedHelper.PrelimState(false, true, true, true, 1, 2),
                new HackathonDevSeedHelper.FinalState(true, true),
                seedHelper.computeGd6PendingConfirmDates());

        Hackathon hackathon = structure.hackathon();
        Round prelim = structure.prelim();
        Round finalRound = structure.finalRound();
        Track track1 = structure.track1();
        Track track2 = structure.track2();

        if (needsRepair(hackathon, prelim, finalRound)) {
            seedHelper.repairHackathonForGd6FinishedExportRetest(hackathon, prelim, finalRound);
            hackathon = hackathonRepository.findById(hackathon.getId()).orElse(hackathon);
            prelim = loadPrelim(hackathon.getId());
            finalRound = loadFinal(hackathon.getId());
        }

        seedHelper.syncHackathonCalendarFromDates(
                Gd6FinishedExportSeedConstants.SLUG_GD6_FINISHED_EXPORT,
                seedHelper.computeGd6PendingConfirmDates());

        hackathon.setIndividualRankingEnabled(true);
        hackathon.setStatus(HackathonStatus.FINISHED);
        hackathonRepository.save(hackathon);

        List<Team> teams = seedTeamsAndScores(hackathon, prelim, finalRound, track1, track2);
        User coordinator = seedHelper.requireCoordinator();
        seedHelper.ensureFirstPrize(hackathon, finalRound, teams.get(0), coordinator);
        seedHelper.ensureSecondPrize(hackathon, finalRound, teams.get(1), coordinator);
        seedHelper.ensureThirdPrize(hackathon, finalRound, teams.get(2), coordinator);

        chapterRankingService.calculateAsync(hackathon.getId());
        individualRankingService.calculateAsync(hackathon.getId());

        log.info("""
                [Gd6FinishedExportDataSeeder] slug={} hackathonId={} status=FINISHED
                  POST /export-jobs type=CSV_RANKINGS → 201 DONE
                """,
                Gd6FinishedExportSeedConstants.SLUG_GD6_FINISHED_EXPORT,
                hackathon.getId());
    }

    @Transactional
    public void repairForFeTesting() {
        if (!enabled) {
            return;
        }
        hackathonRepository.findBySlug(Gd6FinishedExportSeedConstants.SLUG_GD6_FINISHED_EXPORT).ifPresent(h -> {
            Round prelim = loadPrelim(h.getId());
            Round finalRound = loadFinal(h.getId());
            if (needsRepair(h, prelim, finalRound)) {
                seedHelper.repairHackathonForGd6FinishedExportRetest(h, prelim, finalRound);
                reapplyFinishedState(h, prelim, finalRound);
            }
            seedHelper.syncHackathonCalendarFromDates(
                    Gd6FinishedExportSeedConstants.SLUG_GD6_FINISHED_EXPORT,
                    seedHelper.computeGd6PendingConfirmDates());
        });
    }

    private void reapplyFinishedState(Hackathon hackathon, Round prelim, Round finalRound) {
        User coordinator = seedHelper.requireCoordinator();
        for (int i = 0; i < Gd6FinishedExportSeedConstants.TEAM_NAMES.length; i++) {
            int rank = i;
            teamRepository.findByHackathon_IdAndTeamNameIgnoreCase(
                            hackathon.getId(), Gd6FinishedExportSeedConstants.TEAM_NAMES[i])
                    .ifPresent(team -> {
                        if (rank == 0) {
                            seedHelper.ensureFirstPrize(hackathon, finalRound, team, coordinator);
                        } else if (rank == 1) {
                            seedHelper.ensureSecondPrize(hackathon, finalRound, team, coordinator);
                        } else {
                            seedHelper.ensureThirdPrize(hackathon, finalRound, team, coordinator);
                        }
                    });
        }
        hackathon.setStatus(HackathonStatus.FINISHED);
        hackathon.setIndividualRankingEnabled(true);
        hackathonRepository.save(hackathon);
        chapterRankingService.calculateAsync(hackathon.getId());
        individualRankingService.calculateAsync(hackathon.getId());
    }

    @Transactional
    public void resetAndSeed() {
        devSeedCleanup.purgeIfPresent(Gd6FinishedExportSeedConstants.SLUG_GD6_FINISHED_EXPORT);
        ensureSeed();
    }

    private List<Team> seedTeamsAndScores(
            Hackathon hackathon,
            Round prelim,
            Round finalRound,
            Track track1,
            Track track2) {
        User coordinator = seedHelper.requireCoordinator();
        User guestJudge = seedHelper.requireGuestJudge();
        Chapter hcm = seedHelper.requireChapter(Gd1SeedConstants.CHAPTER_FPT_HCM);
        Chapter hn = seedHelper.requireChapter(Gd1SeedConstants.CHAPTER_FPT_HN);
        Chapter[] chapters = {hcm, hcm, hn};
        LocalDateTime now = LocalDateTime.now();
        List<Team> teams = new ArrayList<>();
        List<Criteria> finalCriteria = seedHelper.listFinalCriteria(finalRound);

        seedHelper.ensureGuestJudgeInvitation(hackathon, guestJudge, coordinator);

        for (int i = 0; i < Gd6FinishedExportSeedConstants.TEAM_NAMES.length; i++) {
            int idx = i + 1;
            User leader = seedHelper.upsertStudent(
                    Gd6FinishedExportSeedConstants.studentEmail(idx),
                    Gd6FinishedExportSeedConstants.studentDisplayName(idx),
                    chapters[i]);
            seedHelper.registerStudent(hackathon, leader);
            Team team = seedHelper.ensureActiveTeam(
                    hackathon, Gd6FinishedExportSeedConstants.TEAM_NAMES[i], leader, chapters[i], now);
            Track track = (idx % 2 == 1) ? track1 : track2;
            seedHelper.ensureLottery(hackathon, prelim, track, "BANG-" + ((idx % 2) + 1), team, coordinator, now);
            seedHelper.markAdvanced(team, prelim, finalRound, hackathon);
            Submission sub = seedHelper.ensureFinalSubmission(
                    hackathon, finalRound, team,
                    "https://github.com/seal-warriors/gd6f-team%02d".formatted(idx));
            float score = Gd6FinishedExportSeedConstants.TEAM_SCORES[i];
            for (Criteria c : finalCriteria) {
                seedHelper.ensureNormalScore(sub, c, guestJudge, score, true);
            }
            teams.add(team);
        }
        seedHelper.ensureFinalGuestJudgeAssignment(hackathon, finalRound);
        return teams;
    }

    private boolean needsRepair(Hackathon hackathon, Round prelim, Round finalRound) {
        if (hackathon.getStatus() != HackathonStatus.FINISHED) {
            return true;
        }
        if (!Boolean.TRUE.equals(finalRound.getScoringLocked())) {
            return true;
        }
        for (PrizeRank rank : List.of(PrizeRank.FIRST, PrizeRank.SECOND, PrizeRank.THIRD)) {
            if (!prizeRepository.existsByHackathonIdAndPrizeRank(hackathon.getId(), rank)) {
                return true;
            }
        }
        return false;
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
