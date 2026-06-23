package com.sealhackathon.api.config.seed;

import com.sealhackathon.api.chapters.entity.Chapter;
import com.sealhackathon.api.criteria.entity.Criteria;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.prizes.repository.PrizeRepository;
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
 * Seed GĐ6 — PENDING_CONFIRM, 0 prize → test trao giải + confirm gate.
 *
 * <p>Doc: {@code docs/testing/gd6-full-test-matrix-and-seeds.md} § Profile A
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class Gd6PrizesEmptyDataSeeder {

    private final HackathonDevSeedHelper seedHelper;
    private final HackathonRepository hackathonRepository;
    private final RoundRepository roundRepository;
    private final PrizeRepository prizeRepository;
    private final DevSeedCleanup devSeedCleanup;

    @Value("${app.seed.gd6.prizes-empty.enabled:true}")
    private boolean enabled;

    @Transactional
    public void ensureSeed() {
        if (!enabled) {
            log.info("[Gd6PrizesEmptyDataSeeder] Tắt (app.seed.gd6.prizes-empty.enabled=false)");
            return;
        }

        HackathonDevSeedHelper.HackathonStructure structure = seedHelper.ensureHackathonStructure(
                Gd6PrizesEmptySeedConstants.SLUG_GD6_PRIZES_EMPTY,
                "SEAL GĐ6 — Prizes empty",
                HackathonStatus.PENDING_CONFIRM,
                "Seed GĐ6 — PENDING_CONFIRM, CK locked, chưa có prize",
                new HackathonDevSeedHelper.PrelimState(false, true, true, true, 1, 2),
                new HackathonDevSeedHelper.FinalState(true, true),
                seedHelper.computeGd6PendingConfirmDates());

        Hackathon hackathon = structure.hackathon();
        Round prelim = structure.prelim();
        Round finalRound = structure.finalRound();
        Track track1 = structure.track1();
        Track track2 = structure.track2();

        if (needsRepair(hackathon, prelim, finalRound)) {
            seedHelper.repairHackathonForGd6PrizesEmptyRetest(hackathon, prelim, finalRound);
            hackathon = hackathonRepository.findById(hackathon.getId()).orElse(hackathon);
            prelim = loadPrelim(hackathon.getId());
            finalRound = loadFinal(hackathon.getId());
        }

        seedHelper.syncHackathonCalendarFromDates(
                Gd6PrizesEmptySeedConstants.SLUG_GD6_PRIZES_EMPTY,
                seedHelper.computeGd6PendingConfirmDates());

        hackathon.setIndividualRankingEnabled(true);
        hackathon.setStatus(HackathonStatus.PENDING_CONFIRM);
        hackathonRepository.save(hackathon);

        seedTeamsAndScores(hackathon, prelim, finalRound, track1, track2);

        log.info("""
                [Gd6PrizesEmptyDataSeeder] slug={} finalRoundId={} prizes=0
                  PATCH /confirm → NO_PRIZES_RECORDED
                  POST /prizes → demo trao FIRST
                """,
                Gd6PrizesEmptySeedConstants.SLUG_GD6_PRIZES_EMPTY,
                finalRound.getId());
    }

    @Transactional
    public void repairForFeTesting() {
        if (!enabled) {
            return;
        }
        hackathonRepository.findBySlug(Gd6PrizesEmptySeedConstants.SLUG_GD6_PRIZES_EMPTY).ifPresent(h -> {
            Round prelim = loadPrelim(h.getId());
            Round finalRound = loadFinal(h.getId());
            if (needsRepair(h, prelim, finalRound)) {
                seedHelper.repairHackathonForGd6PrizesEmptyRetest(h, prelim, finalRound);
            }
            seedHelper.syncHackathonCalendarFromDates(
                    Gd6PrizesEmptySeedConstants.SLUG_GD6_PRIZES_EMPTY,
                    seedHelper.computeGd6PendingConfirmDates());
        });
    }

    @Transactional
    public void resetAndSeed() {
        devSeedCleanup.purgeIfPresent(Gd6PrizesEmptySeedConstants.SLUG_GD6_PRIZES_EMPTY);
        ensureSeed();
    }

    private void seedTeamsAndScores(
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

        seedHelper.ensureGuestJudgeInvitation(hackathon, guestJudge, coordinator);
        List<Criteria> finalCriteria = seedHelper.listFinalCriteria(finalRound);

        for (int i = 0; i < Gd6PrizesEmptySeedConstants.TEAM_NAMES.length; i++) {
            int idx = i + 1;
            User leader = seedHelper.upsertStudent(
                    Gd6PrizesEmptySeedConstants.studentEmail(idx),
                    Gd6PrizesEmptySeedConstants.studentDisplayName(idx),
                    chapters[i]);
            seedHelper.registerStudent(hackathon, leader);
            Team team = seedHelper.ensureActiveTeam(
                    hackathon, Gd6PrizesEmptySeedConstants.TEAM_NAMES[i], leader, chapters[i], now);
            Track track = (idx % 2 == 1) ? track1 : track2;
            seedHelper.ensureLottery(hackathon, prelim, track, "BANG-" + ((idx % 2) + 1), team, coordinator, now);
            seedHelper.markAdvanced(team, prelim, finalRound, hackathon);
            Submission sub = seedHelper.ensureFinalSubmission(
                    hackathon, finalRound, team,
                    "https://github.com/seal-warriors/gd6p-team%02d".formatted(idx));
            float score = Gd6PrizesEmptySeedConstants.TEAM_SCORES[i];
            for (Criteria c : finalCriteria) {
                seedHelper.ensureNormalScore(sub, c, guestJudge, score, true);
            }
        }
        seedHelper.ensureFinalGuestJudgeAssignment(hackathon, finalRound);
    }

    private boolean needsRepair(Hackathon hackathon, Round prelim, Round finalRound) {
        if (hackathon.getStatus() != HackathonStatus.PENDING_CONFIRM) {
            return true;
        }
        if (!Boolean.TRUE.equals(finalRound.getScoringLocked())) {
            return true;
        }
        return !prizeRepository.findByRound_Hackathon_IdOrderByAwardedAtDesc(hackathon.getId()).isEmpty();
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
