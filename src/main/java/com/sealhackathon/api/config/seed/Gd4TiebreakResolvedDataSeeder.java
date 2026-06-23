package com.sealhackathon.api.config.seed;

import com.sealhackathon.api.chapters.entity.Chapter;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.submissions.entity.Submission;
import com.sealhackathon.api.team_round_participation.value_object.ParticipationStatus;
import com.sealhackathon.api.team_round_tracks.repository.TeamRoundTrackRepository;
import com.sealhackathon.api.teams.entity.Team;
import com.sealhackathon.api.teams.repository.TeamRepository;
import com.sealhackathon.api.tiebreak_evaluations.repository.TiebreakEvaluationRepository;
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
 * Seed GĐ4 — coordinator đã resolve tiebreak, published, POST /advance OK.
 *
 * <p>Doc: {@code docs/testing/gd4-full-test-matrix-and-seeds.md} § Profile F
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class Gd4TiebreakResolvedDataSeeder {

    private final HackathonDevSeedHelper seedHelper;
    private final HackathonRepository hackathonRepository;
    private final RoundRepository roundRepository;
    private final TeamRoundTrackRepository teamRoundTrackRepository;
    private final TeamRepository teamRepository;
    private final TiebreakEvaluationRepository tiebreakEvaluationRepository;
    private final DevSeedCleanup devSeedCleanup;

    @Value("${app.seed.gd4.tiebreak-resolved.enabled:true}")
    private boolean enabled;

    @Transactional
    public void ensureSeed() {
        if (!enabled) {
            log.info("[Gd4TiebreakResolvedDataSeeder] Tắt (app.seed.gd4.tiebreak-resolved.enabled=false)");
            return;
        }

        HackathonDevSeedHelper.PrelimState prelimState =
                new HackathonDevSeedHelper.PrelimState(false, true, true, true, 1, 6);
        HackathonDevSeedHelper.HackathonStructure structure = seedHelper.ensureHackathonStructure(
                Gd4TiebreakResolvedSeedConstants.SLUG_GD4_TIEBREAK_RESOLVED,
                "SEAL GĐ4 — Tiebreak resolved",
                HackathonStatus.ONGOING,
                "Seed GĐ4 — tiebreak đã resolve, POST /advance không còn TIEBREAK_REQUIRED",
                prelimState,
                new HackathonDevSeedHelper.FinalState(false, false),
                seedHelper.computeGd4AdvanceReadyDates());

        Hackathon hackathon = structure.hackathon();
        Round prelim = structure.prelim();
        Round finalRound = structure.finalRound();
        Track track1 = structure.track1();

        if (needsRepair(hackathon, prelim, finalRound)) {
            seedHelper.repairHackathonForGd4TiebreakResolvedRetest(hackathon, prelim, finalRound);
            hackathon = hackathonRepository.findById(hackathon.getId()).orElse(hackathon);
            prelim = loadPrelim(hackathon.getId());
            finalRound = loadFinal(hackathon.getId());
        }

        seedHelper.syncHackathonCalendarFromDates(
                Gd4TiebreakResolvedSeedConstants.SLUG_GD4_TIEBREAK_RESOLVED,
                seedHelper.computeGd4AdvanceReadyDates());
        prelim = loadPrelim(hackathon.getId());
        prelim.setTopNAdvance(1);
        roundRepository.save(prelim);

        User coordinator = seedHelper.requireCoordinator();
        User judge1 = seedHelper.requireJudge1();
        Chapter hcm = seedHelper.requireChapter(Gd1SeedConstants.CHAPTER_FPT_HCM);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime submittedAt = now.minusHours(72);

        List<Team> teams = new ArrayList<>();
        for (int i = 0; i < Gd4TiebreakResolvedSeedConstants.TEAM_NAMES.length; i++) {
            int idx = i + 1;
            User leader = seedHelper.upsertStudent(
                    Gd4TiebreakResolvedSeedConstants.studentEmail(idx),
                    Gd4TiebreakResolvedSeedConstants.studentDisplayName(idx),
                    hcm);
            seedHelper.registerStudent(hackathon, leader);
            Team team = seedHelper.ensureActiveTeam(
                    hackathon, Gd4TiebreakResolvedSeedConstants.TEAM_NAMES[i], leader, hcm, now);
            seedHelper.ensureTeamLocked(team, now);
            seedHelper.ensureLottery(hackathon, prelim, track1, "A", team, coordinator, now);
            Submission sub = seedHelper.ensurePrelimSubmission(
                    hackathon, prelim, track1, team,
                    com.sealhackathon.api.submissions.value_object.SubmissionStatus.SUBMITTED,
                    false, submittedAt);
            seedHelper.scoreAllTrackCriteria(
                    sub, track1, judge1, Gd4TiebreakResolvedSeedConstants.TEAM_SCORES[i], true);
            teams.add(team);
        }

        seedHelper.seedCoordinatorTiebreakResolve(
                prelim,
                teams.get(0),
                teams.get(1),
                coordinator,
                "Seed coordinator tiebreak resolve — TR02 penalty 0.01");

        log.info("""
                [Gd4TiebreakResolvedDataSeeder] slug={} prelimRoundId={} topN=1
                  TR01 vs TR02 hòa 9.0 — coordinator resolve xong
                  POST /advance → 200 (không TIEBREAK_REQUIRED)
                  students: {} … {} password={}
                """,
                Gd4TiebreakResolvedSeedConstants.SLUG_GD4_TIEBREAK_RESOLVED,
                prelim.getId(),
                Gd4TiebreakResolvedSeedConstants.studentEmail(1),
                Gd4TiebreakResolvedSeedConstants.studentEmail(4),
                DevSeedCatalog.DEV_STUDENT_PASSWORD);
    }

    @Transactional
    public void repairForFeTesting() {
        if (!enabled) {
            return;
        }
        hackathonRepository.findBySlug(Gd4TiebreakResolvedSeedConstants.SLUG_GD4_TIEBREAK_RESOLVED).ifPresent(h -> {
            Round prelim = loadPrelim(h.getId());
            Round finalRound = loadFinal(h.getId());
            if (needsRepair(h, prelim, finalRound)) {
                seedHelper.repairHackathonForGd4TiebreakResolvedRetest(h, prelim, finalRound);
                ensureSeed();
                return;
            }
            seedHelper.syncHackathonCalendarFromDates(
                    Gd4TiebreakResolvedSeedConstants.SLUG_GD4_TIEBREAK_RESOLVED,
                    seedHelper.computeGd4AdvanceReadyDates());
        });
    }

    @Transactional
    public void resetAndSeed() {
        devSeedCleanup.purgeIfPresent(Gd4TiebreakResolvedSeedConstants.SLUG_GD4_TIEBREAK_RESOLVED);
        ensureSeed();
    }

    private boolean needsRepair(Hackathon hackathon, Round prelim, Round finalRound) {
        if (hackathon.getStatus() != HackathonStatus.ONGOING) {
            return true;
        }
        if (!Boolean.TRUE.equals(prelim.getIsPublished()) || Boolean.TRUE.equals(finalRound.getIsActive())) {
            return true;
        }
        if (tiebreakEvaluationRepository.findByRound_Id(prelim.getId()).isEmpty()) {
            return true;
        }
        return teamRepository.findByHackathon_Id(hackathon.getId()).stream()
                .flatMap(team -> teamRoundTrackRepository.findByTeam_Id(team.getId()).stream())
                .anyMatch(trt -> trt.getParticipationStatus() == ParticipationStatus.ADVANCED);
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
