package com.sealhackathon.api.config.seed;

import com.sealhackathon.api.chapters.entity.Chapter;
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
 * Seed GĐ4 — published + 6 ADVANCED, CK không có judge.
 *
 * <p>POST assign judge1 → warnings JUDGE_PARTICIPATED_IN_PRELIM + MIN_FINAL_JUDGES_NOT_MET
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class Gd4JudgeAssignWarningsDataSeeder {

    private final HackathonDevSeedHelper seedHelper;
    private final HackathonRepository hackathonRepository;
    private final RoundRepository roundRepository;
    private final TeamRepository teamRepository;
    private final DevSeedCleanup devSeedCleanup;

    @Value("${app.seed.gd4.judge-assign-warnings.enabled:true}")
    private boolean enabled;

    @Transactional
    public void ensureSeed() {
        if (!enabled) {
            log.info("[Gd4JudgeAssignWarningsDataSeeder] Tắt (app.seed.gd4.judge-assign-warnings.enabled=false)");
            return;
        }

        HackathonDevSeedHelper.PrelimState prelimState =
                new HackathonDevSeedHelper.PrelimState(false, true, true, true, 1, 6);
        HackathonDevSeedHelper.HackathonStructure structure = seedHelper.ensureHackathonStructure(
                Gd4JudgeAssignWarningsSeedConstants.SLUG_GD4_JUDGE_ASSIGN_WARNINGS,
                "SEAL GĐ4 — Judge assign warnings",
                HackathonStatus.ONGOING,
                "Seed GĐ4 — 6 ADVANCED, 0 judge CK — POST assign judge1 → warnings",
                prelimState,
                new HackathonDevSeedHelper.FinalState(false, false),
                seedHelper.computeGd4AdvanceReadyDates());

        Hackathon hackathon = structure.hackathon();
        Round prelim = structure.prelim();
        Round finalRound = structure.finalRound();
        Track track1 = structure.track1();
        Track track2 = structure.track2();

        if (needsRepair(hackathon, prelim, finalRound)) {
            seedHelper.repairHackathonForGd4CkActivateRetest(hackathon, prelim, finalRound);
            hackathon = hackathonRepository.findById(hackathon.getId()).orElse(hackathon);
            prelim = loadPrelim(hackathon.getId());
            finalRound = loadFinal(hackathon.getId());
        }

        seedHelper.syncHackathonCalendarFromDates(
                Gd4JudgeAssignWarningsSeedConstants.SLUG_GD4_JUDGE_ASSIGN_WARNINGS,
                seedHelper.computeGd4AdvanceReadyDates());

        User coordinator = seedHelper.requireCoordinator();
        User judge1 = seedHelper.requireJudge1();
        User judge2 = seedHelper.requireJudge2();
        Chapter hcm = seedHelper.requireChapter(Gd1SeedConstants.CHAPTER_FPT_HCM);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime submittedAt = now.minusHours(72);

        List<Team> teams = new ArrayList<>();
        for (int i = 0; i < Gd4CkActivateReadySeedConstants.TEAM_NAMES.length; i++) {
            int idx = i + 1;
            User leader = seedHelper.upsertStudent(
                    Gd4JudgeAssignWarningsSeedConstants.studentEmail(idx),
                    Gd4JudgeAssignWarningsSeedConstants.studentDisplayName(idx),
                    hcm);
            seedHelper.registerStudent(hackathon, leader);
            Team team = seedHelper.ensureActiveTeam(
                    hackathon, Gd4CkActivateReadySeedConstants.TEAM_NAMES[i], leader, hcm, now);
            seedHelper.ensureTeamLocked(team, now);
            Track track = idx <= 4 ? track1 : track2;
            User judge = idx <= 4 ? judge1 : judge2;
            seedHelper.ensureLottery(
                    hackathon, prelim, track, Gd4CkActivateReadySeedConstants.GROUPS[i], team, coordinator, now);
            Submission sub = seedHelper.ensurePrelimSubmission(
                    hackathon, prelim, track, team,
                    com.sealhackathon.api.submissions.value_object.SubmissionStatus.SUBMITTED,
                    false, submittedAt);
            seedHelper.scoreAllTrackCriteria(
                    sub, track, judge, Gd4CkActivateReadySeedConstants.TEAM_SCORES[i], true);
            teams.add(team);
        }

        for (int advancedIndex : Gd4CkActivateReadySeedConstants.ADVANCED_TEAM_INDICES) {
            seedHelper.markAdvanced(teams.get(advancedIndex), prelim, finalRound, hackathon);
        }

        seedHelper.clearFinalRoundJudgeAssignments(finalRound);

        log.info("""
                [Gd4JudgeAssignWarningsDataSeeder] slug={} finalRoundId={}
                  6 ADVANCED, 0 judge CK — POST assign judge1 → JUDGE_PARTICIPATED + MIN_FINAL_JUDGES
                """,
                Gd4JudgeAssignWarningsSeedConstants.SLUG_GD4_JUDGE_ASSIGN_WARNINGS,
                finalRound.getId());
    }

    @Transactional
    public void repairForFeTesting() {
        if (!enabled) {
            return;
        }
        hackathonRepository.findBySlug(Gd4JudgeAssignWarningsSeedConstants.SLUG_GD4_JUDGE_ASSIGN_WARNINGS)
                .ifPresent(h -> {
                    Round prelim = loadPrelim(h.getId());
                    Round finalRound = loadFinal(h.getId());
                    if (needsRepair(h, prelim, finalRound)) {
                        seedHelper.repairHackathonForGd4CkActivateRetest(h, prelim, finalRound);
                        reapplyAdvanced(h, prelim, finalRound);
                    }
                    seedHelper.syncHackathonCalendarFromDates(
                            Gd4JudgeAssignWarningsSeedConstants.SLUG_GD4_JUDGE_ASSIGN_WARNINGS,
                            seedHelper.computeGd4AdvanceReadyDates());
                    seedHelper.clearFinalRoundJudgeAssignments(finalRound);
                });
    }

    private void reapplyAdvanced(Hackathon hackathon, Round prelim, Round finalRound) {
        for (int advancedIndex : Gd4CkActivateReadySeedConstants.ADVANCED_TEAM_INDICES) {
            String teamName = Gd4CkActivateReadySeedConstants.TEAM_NAMES[advancedIndex];
            teamRepository.findByHackathon_IdAndTeamNameIgnoreCase(hackathon.getId(), teamName)
                    .ifPresent(team -> seedHelper.markAdvanced(team, prelim, finalRound, hackathon));
        }
    }

    @Transactional
    public void resetAndSeed() {
        devSeedCleanup.purgeIfPresent(Gd4JudgeAssignWarningsSeedConstants.SLUG_GD4_JUDGE_ASSIGN_WARNINGS);
        ensureSeed();
    }

    private boolean needsRepair(Hackathon hackathon, Round prelim, Round finalRound) {
        if (hackathon.getStatus() != HackathonStatus.ONGOING) {
            return true;
        }
        if (!Boolean.TRUE.equals(prelim.getIsPublished()) || Boolean.TRUE.equals(finalRound.getIsActive())) {
            return true;
        }
        return !seedHelper.isFinalRoundWithoutJudges(finalRound);
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
