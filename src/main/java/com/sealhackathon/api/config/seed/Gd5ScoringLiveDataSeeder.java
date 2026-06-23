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
 * Seed GĐ5 — queue CK PRESENTING, chấm đủ / một phần / chưa chấm.
 *
 * <p>Doc: {@code docs/testing/gd5-full-test-matrix-and-seeds.md} § Profile B
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class Gd5ScoringLiveDataSeeder {

    private final HackathonDevSeedHelper seedHelper;
    private final HackathonRepository hackathonRepository;
    private final RoundRepository roundRepository;
    private final TeamRepository teamRepository;
    private final DevSeedCleanup devSeedCleanup;

    @Value("${app.seed.gd5.scoring-live.enabled:true}")
    private boolean enabled;

    @Transactional
    public void ensureSeed() {
        if (!enabled) {
            log.info("[Gd5ScoringLiveDataSeeder] Tắt (app.seed.gd5.scoring-live.enabled=false)");
            return;
        }

        HackathonDevSeedHelper.HackathonStructure structure = seedHelper.ensureHackathonStructure(
                Gd5ScoringLiveSeedConstants.SLUG_GD5_SCORING_LIVE,
                "SEAL GĐ5 — Scoring live",
                HackathonStatus.ONGOING,
                "Seed GĐ5 — queue CK PRESENTING, chấm đủ/một phần/chưa chấm",
                new HackathonDevSeedHelper.PrelimState(false, true, true, true, 2, 2),
                new HackathonDevSeedHelper.FinalState(true, false),
                seedHelper.computeGd5FinalActiveDates());

        Hackathon hackathon = structure.hackathon();
        Round prelim = structure.prelim();
        Round finalRound = structure.finalRound();
        Track track1 = structure.track1();
        Track track2 = structure.track2();

        seedHelper.syncHackathonCalendarFromDates(
                Gd5ScoringLiveSeedConstants.SLUG_GD5_SCORING_LIVE, seedHelper.computeGd5FinalActiveDates());
        seedHelper.repairHackathonForGd5Retest(hackathon, prelim, finalRound);

        User coordinator = seedHelper.requireCoordinator();
        User guestJudge = seedHelper.requireGuestJudge();
        Chapter chapter = seedHelper.requireChapter(Gd1SeedConstants.CHAPTER_FPT_HCM);
        LocalDateTime now = LocalDateTime.now();

        seedHelper.ensureGuestJudgeInvitation(hackathon, guestJudge, coordinator);

        List<Team> teams = new ArrayList<>();
        List<Submission> finalSubs = new ArrayList<>();
        for (int i = 0; i < Gd5ScoringLiveSeedConstants.TEAM_NAMES.length; i++) {
            int idx = i + 1;
            User leader = seedHelper.upsertStudent(
                    Gd5ScoringLiveSeedConstants.studentEmail(idx),
                    Gd5ScoringLiveSeedConstants.studentDisplayName(idx),
                    chapter);
            seedHelper.registerStudent(hackathon, leader);
            Team team = seedHelper.ensureActiveTeam(
                    hackathon, Gd5ScoringLiveSeedConstants.TEAM_NAMES[i], leader, chapter, now);
            Track track = (idx % 2 == 1) ? track1 : track2;
            String group = "BANG-" + ((idx % 2) + 1);
            seedHelper.ensureLottery(hackathon, prelim, track, group, team, coordinator, now);
            seedHelper.markAdvanced(team, prelim, finalRound, hackathon);
            Submission sub = seedHelper.ensureFinalSubmission(
                    hackathon, finalRound, team, "https://github.com/seal-warriors/gd5l-team%02d".formatted(idx));
            finalSubs.add(sub);
            teams.add(team);
        }

        List<Criteria> finalCriteria = seedHelper.listFinalCriteria(finalRound);
        for (Criteria c : finalCriteria) {
            seedHelper.ensureNormalScore(finalSubs.get(0), c, guestJudge, 8.5f, false);
        }
        if (!finalCriteria.isEmpty()) {
            seedHelper.ensureNormalScore(finalSubs.get(1), finalCriteria.get(0), guestJudge, 7.0f, false);
        }

        seedHelper.seedFinalPresentationQueue(finalRound, finalSubs, 2);

        log.info("""
                [Gd5ScoringLiveDataSeeder] slug={} finalRoundId={}
                  T1 scored full | T2 partial | T3 PRESENTING | T4 WAITING
                  guestJudge={}
                """,
                Gd5ScoringLiveSeedConstants.SLUG_GD5_SCORING_LIVE,
                finalRound.getId(),
                Gd1SeedConstants.EMAIL_GUEST_JUDGE);
    }

    @Transactional
    public void repairForFeTesting() {
        if (!enabled) {
            return;
        }
        hackathonRepository.findBySlug(Gd5ScoringLiveSeedConstants.SLUG_GD5_SCORING_LIVE).ifPresent(h -> {
            Round prelim = loadPrelim(h.getId());
            Round finalRound = loadFinal(h.getId());
            seedHelper.repairGd5ScoringLiveFeTesting(h, prelim, finalRound);
            reseedScoringState(h, finalRound);
        });
    }

    private void reseedScoringState(Hackathon hackathon, Round finalRound) {
        User guestJudge = seedHelper.requireGuestJudge();
        List<Submission> finalSubs = new ArrayList<>();
        for (String teamName : Gd5ScoringLiveSeedConstants.TEAM_NAMES) {
            var teamOpt = teamRepository.findByHackathon_IdAndTeamNameIgnoreCase(hackathon.getId(), teamName);
            if (teamOpt.isEmpty()) {
                return;
            }
            finalSubs.add(seedHelper.ensureFinalSubmission(
                    hackathon, finalRound, teamOpt.get(),
                    "https://github.com/seal-warriors/gd5l-" + teamOpt.get().getId()));
        }
        List<Criteria> finalCriteria = seedHelper.listFinalCriteria(finalRound);
        for (Criteria c : finalCriteria) {
            seedHelper.ensureNormalScore(finalSubs.get(0), c, guestJudge, 8.5f, false);
        }
        if (!finalCriteria.isEmpty()) {
            seedHelper.ensureNormalScore(finalSubs.get(1), finalCriteria.get(0), guestJudge, 7.0f, false);
        }
        seedHelper.seedFinalPresentationQueue(finalRound, finalSubs, 2);
    }

    @Transactional
    public void resetAndSeed() {
        devSeedCleanup.purgeIfPresent(Gd5ScoringLiveSeedConstants.SLUG_GD5_SCORING_LIVE);
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
