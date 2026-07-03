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
import com.sealhackathon.api.tiebreak_evaluations.entity.TiebreakEvaluation;
import com.sealhackathon.api.tiebreak_evaluations.repository.TiebreakEvaluationRepository;
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
import java.util.Optional;

/**
 * Seed GĐ3 hybrid exit — sơ loại đã khóa chấm, đồng điểm Top-N, penalty tiebreak seed.
 *
 * <p>Doc: {@code docs/testing/gd3-full-test-matrix-and-seeds.md} § Profile C
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class Gd3TiebreakHybridDataSeeder {

    private final HackathonDevSeedHelper seedHelper;
    private final HackathonRepository hackathonRepository;
    private final RoundRepository roundRepository;
    private final TeamRepository teamRepository;
    private final TrackRepository trackRepository;
    private final TiebreakEvaluationRepository tiebreakEvaluationRepository;
    private final DevSeedCleanup devSeedCleanup;

    @Value("${app.seed.gd3.tiebreak-hybrid.enabled:true}")
    private boolean enabled;

    @Transactional
    public void ensureSeed() {
        if (!enabled) {
            log.info("[Gd3TiebreakHybridDataSeeder] Tắt (app.seed.gd3.tiebreak-hybrid.enabled=false)");
            return;
        }

        HackathonDevSeedHelper.PrelimState prelimState =
                new HackathonDevSeedHelper.PrelimState(false, true, true, false, 2, 4);
        HackathonDevSeedHelper.HackathonStructure structure = seedHelper.ensureHackathonStructure(
                Gd3TiebreakHybridSeedConstants.SLUG_GD3_TIEBREAK_HYBRID,
                "SEAL GĐ3 — Tiebreak hybrid",
                HackathonStatus.ONGOING,
                "Seed GĐ3 hybrid exit — locked scoring, 2 đội hòa điểm Bảng A, penalty HEAD vote",
                prelimState,
                new HackathonDevSeedHelper.FinalState(false, false),
                seedHelper.computeGd4AdvanceReadyDates());

        Hackathon hackathon = structure.hackathon();
        Round prelim = structure.prelim();
        Round finalRound = structure.finalRound();
        Track track1 = structure.track1();
        Track track2 = structure.track2();

        if (needsRepair(hackathon, prelim, finalRound)) {
            seedHelper.repairHackathonForGd4Retest(hackathon, prelim, finalRound);
            prelim = loadPrelim(hackathon.getId());
            applyTiebreakPrelimState(prelim);
        }

        seedHelper.syncHackathonCalendarFromDates(
                Gd3TiebreakHybridSeedConstants.SLUG_GD3_TIEBREAK_HYBRID,
                seedHelper.computeGd4AdvanceReadyDates());
        prelim = loadPrelim(hackathon.getId());
        applyTiebreakPrelimState(prelim);

        User coordinator = seedHelper.requireCoordinator();
        User judge1 = seedHelper.requireJudge1();
        User judge2 = seedHelper.requireJudge2();
        Chapter hcm = seedHelper.requireChapter(Gd1SeedConstants.CHAPTER_FPT_HCM);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime submittedAt = now.minusHours(72);

        String[] teamNames = {
                Gd3TiebreakHybridSeedConstants.TEAM_TIE_A1,
                Gd3TiebreakHybridSeedConstants.TEAM_TIE_A2,
                Gd3TiebreakHybridSeedConstants.TEAM_TIE_A3,
                Gd3TiebreakHybridSeedConstants.TEAM_CLEAR_B1,
                Gd3TiebreakHybridSeedConstants.TEAM_CLEAR_B2,
                Gd3TiebreakHybridSeedConstants.TEAM_TRACK2
        };
        String[] groups = {"BANG-A", "BANG-A", "BANG-A", "BANG-B", "BANG-B", "BANG-C"};

        List<Team> teams = new ArrayList<>();
        List<Submission> track1Subs = new ArrayList<>();
        List<Submission> track2Subs = new ArrayList<>();

        for (int i = 0; i < teamNames.length; i++) {
            int idx = i + 1;
            User leader = seedHelper.upsertStudent(
                    Gd3TiebreakHybridSeedConstants.studentEmail(idx),
                    Gd3TiebreakHybridSeedConstants.studentDisplayName(idx),
                    hcm);
            seedHelper.registerStudent(hackathon, leader);
            Team team = seedHelper.ensureActiveTeamForLeader(hackathon, teamNames[i], leader, hcm, now);
            seedHelper.ensureTeamLocked(team, now);
            Track track = idx <= 5 ? track1 : track2;
            seedHelper.ensureLottery(hackathon, prelim, track, groups[i], team, coordinator, now);
            Submission sub = seedHelper.ensurePrelimSubmission(
                    hackathon, prelim, track, team, SubmissionStatus.SUBMITTED, false, submittedAt);
            float score = Gd3TiebreakHybridSeedConstants.TEAM_SCORES[i];
            seedHelper.scoreAllTrackCriteria(sub, track, judge1, score, true);
            seedHelper.scoreAllTrackCriteria(sub, track, judge2, score, true);
            teams.add(team);
            if (idx <= 5) {
                track1Subs.add(sub);
            } else {
                track2Subs.add(sub);
            }
        }

        seedHelper.seedPresentationQueue(prelim, track1, track1Subs, -1);
        seedHelper.seedPresentationQueue(prelim, track2, track2Subs, -1);
        seedTiebreakPenalties(prelim, teams.get(0), teams.get(1), judge1);

        log.info("""
                [Gd3TiebreakHybridDataSeeder] slug={} prelimRoundId={} topN=2
                  BANG-A: T01=8.0 T02=8.0 (hòa rank2) T03=6.0 | BANG-B: 9.0 / 5.0
                  HEAD penalty vote: T01 penalty=0, T02 penalty=1
                  students: {} … {} password={}
                """,
                Gd3TiebreakHybridSeedConstants.SLUG_GD3_TIEBREAK_HYBRID,
                prelim.getId(),
                Gd3TiebreakHybridSeedConstants.studentEmail(1),
                Gd3TiebreakHybridSeedConstants.studentEmail(6),
                DevSeedCatalog.DEV_STUDENT_PASSWORD);
    }

    @Transactional
    public void repairForFeTesting() {
        if (!enabled) {
            return;
        }
        hackathonRepository.findBySlug(Gd3TiebreakHybridSeedConstants.SLUG_GD3_TIEBREAK_HYBRID)
                .ifPresent(h -> {
                    seedHelper.syncHackathonCalendarFromDates(
                            Gd3TiebreakHybridSeedConstants.SLUG_GD3_TIEBREAK_HYBRID,
                            seedHelper.computeGd4AdvanceReadyDates());
                    Round prelim = loadPrelim(h.getId());
                    applyTiebreakPrelimState(prelim);
                });
    }

    private void applyTiebreakPrelimState(Round prelim) {
        User coordinator = seedHelper.requireCoordinator();
        prelim.setIsActive(false);
        prelim.setScoringLocked(true);
        prelim.setScoringLockedAt(LocalDateTime.now());
        prelim.setScoringLockedBy(coordinator);
        prelim.setIsPublished(false);
        prelim.setTopNAdvance(2);
        prelim.setMinTeamsFinal(4);
        prelim.setWildcardEnabled(true);
        roundRepository.save(prelim);
    }

    private void seedTiebreakPenalties(Round prelim, Team teamPreferred, Team teamPenalized, User headJudge) {
        tiebreakEvaluationRepository.deleteByRound_IdAndJudge_IdAndIsCastingVoteTrue(
                prelim.getId(), headJudge.getId());
        tiebreakEvaluationRepository.findByRound_IdAndTeam_IdAndJudge_Id(
                        prelim.getId(), teamPreferred.getId(), headJudge.getId())
                .ifPresent(tiebreakEvaluationRepository::delete);
        tiebreakEvaluationRepository.findByRound_IdAndTeam_IdAndJudge_Id(
                        prelim.getId(), teamPenalized.getId(), headJudge.getId())
                .ifPresent(tiebreakEvaluationRepository::delete);
        LocalDateTime now = LocalDateTime.now();
        upsertTiebreakPenalty(prelim, teamPreferred, headJudge, 0f, true, 2, now, null);
        upsertTiebreakPenalty(prelim, teamPenalized, headJudge, 1f, true, 2, now, null);
    }

    private void upsertTiebreakPenalty(
            Round prelim, Team team, User judge, float penalty, boolean castingVote,
            int level, LocalDateTime evaluatedAt, String notes) {
        Optional<TiebreakEvaluation> existing = tiebreakEvaluationRepository.findByRound_IdAndTeam_IdAndJudge_Id(
                prelim.getId(), team.getId(), judge.getId());
        if (existing.isPresent()) {
            TiebreakEvaluation te = existing.get();
            te.setPenaltyScore(penalty);
            te.setIsCastingVote(castingVote);
            te.setTiebreakLevel(level);
            te.setEvaluatedAt(evaluatedAt);
            te.setNotes(notes);
            tiebreakEvaluationRepository.save(te);
            return;
        }
        tiebreakEvaluationRepository.save(TiebreakEvaluation.builder()
                .round(prelim)
                .team(team)
                .judge(judge)
                .penaltyScore(penalty)
                .isCastingVote(castingVote)
                .tiebreakLevel(level)
                .notes(notes)
                .evaluatedAt(evaluatedAt)
                .build());
    }

    @Transactional
    public void resetAndSeed() {
        devSeedCleanup.purgeIfPresent(Gd3TiebreakHybridSeedConstants.SLUG_GD3_TIEBREAK_HYBRID);
        ensureSeed();
    }

    private boolean needsRepair(Hackathon hackathon, Round prelim, Round finalRound) {
        return !Boolean.TRUE.equals(prelim.getScoringLocked())
                || Boolean.TRUE.equals(prelim.getIsActive())
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
