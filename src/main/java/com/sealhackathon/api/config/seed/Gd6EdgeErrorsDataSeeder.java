package com.sealhackathon.api.config.seed;

import com.sealhackathon.api.chapters.entity.Chapter;
import com.sealhackathon.api.criteria.entity.Criteria;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
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
 * Seed GĐ6 bad path — confirm khi CK chưa lock.
 *
 * <p>Doc: {@code docs/testing/gd6-full-test-matrix-and-seeds.md} § Profile D
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class Gd6EdgeErrorsDataSeeder {

    private final HackathonDevSeedHelper seedHelper;
    private final HackathonRepository hackathonRepository;
    private final RoundRepository roundRepository;
    private final TeamRepository teamRepository;
    private final PrizeRepository prizeRepository;
    private final DevSeedCleanup devSeedCleanup;

    @Value("${app.seed.gd6.edge-errors.enabled:true}")
    private boolean enabled;

    @Transactional
    public void ensureSeed() {
        if (!enabled) {
            log.info("[Gd6EdgeErrorsDataSeeder] Tắt (app.seed.gd6.edge-errors.enabled=false)");
            return;
        }

        HackathonDevSeedHelper.HackathonStructure structure = seedHelper.ensureHackathonStructure(
                Gd6EdgeErrorsSeedConstants.SLUG_GD6_EDGE_ERRORS,
                "SEAL GĐ6 — Edge errors",
                HackathonStatus.PENDING_CONFIRM,
                "Seed GĐ6 — PENDING_CONFIRM, CK chưa scoring_locked, có FIRST prize",
                new HackathonDevSeedHelper.PrelimState(false, true, true, true, 1, 2),
                new HackathonDevSeedHelper.FinalState(true, false),
                seedHelper.computeGd6PendingConfirmDates());

        Hackathon hackathon = structure.hackathon();
        Round prelim = structure.prelim();
        Round finalRound = structure.finalRound();
        Track track1 = structure.track1();
        Track track2 = structure.track2();

        if (needsRepair(hackathon, prelim, finalRound)) {
            seedHelper.repairHackathonForGd6ConfirmGateRetest(hackathon, prelim, finalRound);
            hackathon = hackathonRepository.findById(hackathon.getId()).orElse(hackathon);
            prelim = loadPrelim(hackathon.getId());
            finalRound = loadFinal(hackathon.getId());
        }

        seedHelper.syncHackathonCalendarFromDates(
                Gd6EdgeErrorsSeedConstants.SLUG_GD6_EDGE_ERRORS,
                seedHelper.computeGd6PendingConfirmDates());

        List<Team> teams = seedTeamsAndScores(hackathon, prelim, finalRound, track1, track2);
        seedHelper.ensureFirstPrize(
                hackathon, finalRound, teams.get(0), seedHelper.requireCoordinator());

        log.info("""
                [Gd6EdgeErrorsDataSeeder] slug={} finalRoundId={} scoring_locked=false
                  PATCH /confirm → ROUND_NOT_SCORING_LOCKED
                """,
                Gd6EdgeErrorsSeedConstants.SLUG_GD6_EDGE_ERRORS,
                finalRound.getId());
    }

    @Transactional
    public void repairForFeTesting() {
        if (!enabled) {
            return;
        }
        hackathonRepository.findBySlug(Gd6EdgeErrorsSeedConstants.SLUG_GD6_EDGE_ERRORS).ifPresent(h -> {
            Round prelim = loadPrelim(h.getId());
            Round finalRound = loadFinal(h.getId());
            if (needsRepair(h, prelim, finalRound)) {
                seedHelper.repairHackathonForGd6ConfirmGateRetest(h, prelim, finalRound);
                reapplyEdgeState(h, finalRound);
            }
            seedHelper.syncHackathonCalendarFromDates(
                    Gd6EdgeErrorsSeedConstants.SLUG_GD6_EDGE_ERRORS,
                    seedHelper.computeGd6PendingConfirmDates());
        });
    }

    private void reapplyEdgeState(Hackathon hackathon, Round finalRound) {
        Round finalRoundRef = finalRound;
        teamRepository.findByHackathon_IdAndTeamNameIgnoreCase(
                        hackathon.getId(), Gd6EdgeErrorsSeedConstants.TEAM_NAMES[0])
                .ifPresent(team -> seedHelper.ensureFirstPrize(
                        hackathon, finalRoundRef, team, seedHelper.requireCoordinator()));
        hackathon.setStatus(HackathonStatus.PENDING_CONFIRM);
        hackathonRepository.save(hackathon);
        Round reloaded = loadFinal(hackathon.getId());
        if (Boolean.TRUE.equals(reloaded.getScoringLocked())) {
            reloaded.setScoringLocked(false);
            roundRepository.save(reloaded);
        }
    }

    @Transactional
    public void resetAndSeed() {
        devSeedCleanup.purgeIfPresent(Gd6EdgeErrorsSeedConstants.SLUG_GD6_EDGE_ERRORS);
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
        LocalDateTime now = LocalDateTime.now();
        List<Team> teams = new ArrayList<>();
        List<Criteria> finalCriteria = seedHelper.listFinalCriteria(finalRound);

        seedHelper.ensureGuestJudgeInvitation(hackathon, guestJudge, coordinator);
        hackathon.setIndividualRankingEnabled(true);
        hackathon.setStatus(HackathonStatus.PENDING_CONFIRM);
        hackathonRepository.save(hackathon);

        for (int i = 0; i < Gd6EdgeErrorsSeedConstants.TEAM_NAMES.length; i++) {
            int idx = i + 1;
            User leader = seedHelper.upsertStudent(
                    Gd6EdgeErrorsSeedConstants.studentEmail(idx),
                    Gd6EdgeErrorsSeedConstants.studentDisplayName(idx),
                    hcm);
            seedHelper.registerStudent(hackathon, leader);
            Team team = seedHelper.ensureActiveTeam(
                    hackathon, Gd6EdgeErrorsSeedConstants.TEAM_NAMES[i], leader, hcm, now);
            Track track = (idx % 2 == 1) ? track1 : track2;
            seedHelper.ensureLottery(hackathon, prelim, track, "BANG-" + ((idx % 2) + 1), team, coordinator, now);
            seedHelper.markAdvanced(team, prelim, finalRound, hackathon);
            Submission sub = seedHelper.ensureFinalSubmission(
                    hackathon, finalRound, team,
                    "https://github.com/seal-warriors/gd6e-team%02d".formatted(idx));
            float score = Gd6EdgeErrorsSeedConstants.TEAM_SCORES[i];
            for (Criteria c : finalCriteria) {
                seedHelper.ensureNormalScore(sub, c, guestJudge, score, true);
            }
            teams.add(team);
        }
        seedHelper.ensureFinalGuestJudgeAssignment(hackathon, finalRound);
        return teams;
    }

    private boolean needsRepair(Hackathon hackathon, Round prelim, Round finalRound) {
        if (hackathon.getStatus() != HackathonStatus.PENDING_CONFIRM) {
            return true;
        }
        if (Boolean.TRUE.equals(finalRound.getScoringLocked())) {
            return true;
        }
        return !prizeRepository.existsByHackathonIdAndPrizeRank(hackathon.getId(), PrizeRank.FIRST);
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
