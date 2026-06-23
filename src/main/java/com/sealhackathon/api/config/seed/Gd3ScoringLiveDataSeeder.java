package com.sealhackathon.api.config.seed;

import com.sealhackathon.api.chapters.entity.Chapter;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.submissions.entity.Submission;
import com.sealhackathon.api.submissions.value_object.SubmissionStatus;
import com.sealhackathon.api.teams.entity.Team;
import com.sealhackathon.api.teams.repository.TeamRepository;
import com.sealhackathon.api.tracks.entity.Track;
import com.sealhackathon.api.tracks.repository.TrackRepository;
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
 * Seed GĐ3 happy/hybrid — queue PRESENTING, chấm một phần / đủ, timer live.
 *
 * <p>Doc: {@code docs/testing/gd3-full-test-matrix-and-seeds.md} § Profile B
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class Gd3ScoringLiveDataSeeder {

    private final HackathonDevSeedHelper seedHelper;
    private final HackathonRepository hackathonRepository;
    private final RoundRepository roundRepository;
    private final TeamRepository teamRepository;
    private final TrackRepository trackRepository;
    private final DevSeedCleanup devSeedCleanup;

    @Value("${app.seed.gd3.scoring-live.enabled:true}")
    private boolean enabled;

    @Transactional
    public void ensureSeed() {
        if (!enabled) {
            log.info("[Gd3ScoringLiveDataSeeder] Tắt (app.seed.gd3.scoring-live.enabled=false)");
            return;
        }

        HackathonDevSeedHelper.PrelimState prelimState =
                new HackathonDevSeedHelper.PrelimState(true, true, false, false, 2, 4);
        HackathonDevSeedHelper.HackathonStructure structure = seedHelper.ensureHackathonStructure(
                Gd3ScoringLiveSeedConstants.SLUG_GD3_SCORING_LIVE,
                "SEAL GĐ3 — Scoring live",
                HackathonStatus.ONGOING,
                "Seed GĐ3 happy/hybrid — queue PRESENTING, chấm đủ/một phần/chưa chấm",
                prelimState,
                new HackathonDevSeedHelper.FinalState(false, false),
                seedHelper.computeGd3ActivePrelimDates());

        Hackathon hackathon = structure.hackathon();
        Round prelim = structure.prelim();
        Round finalRound = structure.finalRound();
        Track track1 = structure.track1();
        Track track2 = structure.track2();

        if (needsRepair(hackathon, prelim, finalRound)) {
            seedHelper.repairHackathonForGd3Retest(hackathon, prelim, finalRound);
            hackathon = hackathonRepository.findById(hackathon.getId()).orElse(hackathon);
            prelim = loadPrelim(hackathon.getId());
            finalRound = loadFinal(hackathon.getId());
        }

        seedHelper.syncHackathonCalendarFromDates(
                Gd3ScoringLiveSeedConstants.SLUG_GD3_SCORING_LIVE,
                seedHelper.computeGd3ActivePrelimDates());
        prelim = loadPrelim(hackathon.getId());

        User coordinator = seedHelper.requireCoordinator();
        User judge1 = seedHelper.requireJudge1();
        User judge2 = seedHelper.requireJudge2();
        Chapter hcm = seedHelper.requireChapter(Gd1SeedConstants.CHAPTER_FPT_HCM);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime submittedAt = now.minusHours(3);

        List<Team> teams = new ArrayList<>();
        List<Submission> track1Subs = new ArrayList<>();
        List<Submission> track2Subs = new ArrayList<>();

        for (int i = 0; i < Gd3ScoringLiveSeedConstants.TEAM_NAMES.length; i++) {
            int idx = i + 1;
            User leader = seedHelper.upsertStudent(
                    Gd3ScoringLiveSeedConstants.studentEmail(idx),
                    Gd3ScoringLiveSeedConstants.studentDisplayName(idx),
                    hcm);
            seedHelper.registerStudent(hackathon, leader);
            Team team = seedHelper.ensureActiveTeamForLeader(
                    hackathon, Gd3ScoringLiveSeedConstants.TEAM_NAMES[i], leader, hcm, now);
            seedHelper.ensureTeamLocked(team, now);
            Track track = idx <= 3 ? track1 : track2;
            seedHelper.ensureLottery(
                    hackathon, prelim, track, Gd3ScoringLiveSeedConstants.GROUPS[i], team, coordinator, now);
            Submission sub = seedHelper.ensurePrelimSubmission(
                    hackathon, prelim, track, team, SubmissionStatus.SUBMITTED, false, submittedAt);
            seedHelper.clearSubmissionScores(sub.getId());
            teams.add(team);
            if (idx <= 3) {
                track1Subs.add(sub);
            } else {
                track2Subs.add(sub);
            }
        }

        seedScoringState(track1, track2, track1Subs, track2Subs, judge1, judge2, prelim);

        log.info("""
                [Gd3ScoringLiveDataSeeder] slug={} prelimRoundId={}
                  Track1: S01 scored full | S02 partial | S03 PRESENTING (idx=2)
                  Track2: S04 DONE | S05 PRESENTING (idx=1) | S06 no score
                  students: {} … {} password={}
                """,
                Gd3ScoringLiveSeedConstants.SLUG_GD3_SCORING_LIVE,
                prelim.getId(),
                Gd3ScoringLiveSeedConstants.studentEmail(1),
                Gd3ScoringLiveSeedConstants.studentEmail(6),
                DevSeedCatalog.DEV_STUDENT_PASSWORD);
    }

    @Transactional
    public void repairForFeTesting() {
        if (!enabled) {
            return;
        }
        hackathonRepository.findBySlug(Gd3ScoringLiveSeedConstants.SLUG_GD3_SCORING_LIVE).ifPresent(h -> {
            seedHelper.syncHackathonCalendarFromDates(
                    Gd3ScoringLiveSeedConstants.SLUG_GD3_SCORING_LIVE,
                    seedHelper.computeGd3ActivePrelimDates());
            reseedIfPresent(h);
        });
    }

    private void reseedIfPresent(Hackathon hackathon) {
        Round prelim = loadPrelim(hackathon.getId());
        List<Track> tracks = trackRepository.findByRoundIdOrderBySequenceOrderAsc(prelim.getId());
        if (tracks.size() < 2) {
            return;
        }
        Track track1 = tracks.get(0);
        Track track2 = tracks.get(1);
        User judge1 = seedHelper.requireJudge1();
        User judge2 = seedHelper.requireJudge2();

        List<Submission> track1Subs = new ArrayList<>();
        List<Submission> track2Subs = new ArrayList<>();
        for (int i = 0; i < Gd3ScoringLiveSeedConstants.TEAM_NAMES.length; i++) {
            int idx = i + 1;
            var teamOpt = teamRepository.findByHackathon_IdAndTeamNameIgnoreCase(
                    hackathon.getId(), Gd3ScoringLiveSeedConstants.TEAM_NAMES[i]);
            if (teamOpt.isEmpty()) {
                return;
            }
            var subs = seedHelper.ensurePrelimSubmission(
                    hackathon,
                    prelim,
                    idx <= 3 ? track1 : track2,
                    teamOpt.get(),
                    SubmissionStatus.SUBMITTED,
                    false,
                    LocalDateTime.now().minusHours(3));
            if (idx <= 3) {
                track1Subs.add(subs);
            } else {
                track2Subs.add(subs);
            }
        }
        if (track1Subs.size() == 3 && track2Subs.size() == 3) {
            seedScoringState(track1, track2, track1Subs, track2Subs, judge1, judge2, prelim);
        }
    }

    private void seedScoringState(
            Track track1,
            Track track2,
            List<Submission> track1Subs,
            List<Submission> track2Subs,
            User judge1,
            User judge2,
            Round prelim) {
        for (Submission sub : track1Subs) {
            seedHelper.clearSubmissionScores(sub.getId());
        }
        for (Submission sub : track2Subs) {
            seedHelper.clearSubmissionScores(sub.getId());
        }

        // S01: cả 2 judge chấm đủ
        seedHelper.scoreAllTrackCriteria(track1Subs.get(0), track1, judge1, 8.5f, false);
        seedHelper.scoreAllTrackCriteria(track1Subs.get(0), track1, judge2, 8.0f, false);

        // S02: chỉ judge1 chấm đủ
        seedHelper.scoreAllTrackCriteria(track1Subs.get(1), track1, judge1, 7.5f, false);

        // S03, S04, S05, S06: chưa chấm (demo live)

        // Track1: slot index 2 = PRESENTING (S03)
        seedHelper.seedPresentationQueue(prelim, track1, track1Subs, 2);
        // Track2: slot 0 DONE, slot 1 PRESENTING (S05)
        seedHelper.seedPresentationQueue(prelim, track2, track2Subs, 1);
    }

    @Transactional
    public void resetAndSeed() {
        devSeedCleanup.purgeIfPresent(Gd3ScoringLiveSeedConstants.SLUG_GD3_SCORING_LIVE);
        ensureSeed();
    }

    private boolean needsRepair(Hackathon hackathon, Round prelim, Round finalRound) {
        return hackathon.getStatus() != HackathonStatus.ONGOING
                || Boolean.TRUE.equals(prelim.getScoringLocked())
                || Boolean.TRUE.equals(prelim.getIsPublished())
                || Boolean.TRUE.equals(finalRound.getIsActive());
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
