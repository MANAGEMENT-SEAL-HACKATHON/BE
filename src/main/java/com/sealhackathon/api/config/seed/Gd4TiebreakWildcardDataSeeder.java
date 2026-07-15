package com.sealhackathon.api.config.seed;

import com.sealhackathon.api.chapters.entity.Chapter;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.rounds.value_object.TiebreakRule;
import com.sealhackathon.api.submissions.entity.Submission;
import com.sealhackathon.api.submissions.value_object.SubmissionStatus;
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

/**
 * Phase E seeds: SUBMISSION_TIME / COORDINATOR_DECISION / Wildcard gap.
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class Gd4TiebreakWildcardDataSeeder {

    private final HackathonDevSeedHelper seedHelper;
    private final HackathonRepository hackathonRepository;
    private final RoundRepository roundRepository;
    private final DevSeedCleanup devSeedCleanup;

    @Value("${app.seed.gd4.enabled:true}")
    private boolean enabled;

    @Transactional
    public void ensureSeed() {
        if (!enabled) {
            return;
        }
        seedSubmissionTime();
        seedManualTiebreak();
        seedWildcardGap();
    }

    @Transactional
    public void repairForFeTesting() {
        if (!enabled) {
            return;
        }
        ensureSeed();
    }

    private void seedSubmissionTime() {
        String slug = Gd4TiebreakWildcardSeedConstants.SLUG_TIEBREAK_SUBMISSION_TIME;
        HackathonDevSeedHelper.PrelimState prelimState =
                new HackathonDevSeedHelper.PrelimState(false, true, true, false, 2, 2);
        HackathonDevSeedHelper.HackathonStructure structure = seedHelper.ensureHackathonStructure(
                slug,
                "SEAL GĐ4 — Tiebreak Submission Time",
                HackathonStatus.ONGOING,
                "Seed: Team2 & Team3 đồng điểm biên Top-2, SUBMISSION_TIME, Team2 nộp sớm hơn 5 phút",
                prelimState,
                new HackathonDevSeedHelper.FinalState(false, false),
                seedHelper.computeGd4AdvanceReadyDates());

        Round prelim = applyPrelimConfig(structure.prelim(), TiebreakRule.SUBMISSION_TIME, false, 2, 2);
        Track track1 = structure.track1();
        User coordinator = seedHelper.requireCoordinator();
        User judge1 = seedHelper.requireJudge1();
        Chapter hcm = seedHelper.requireChapter(Gd1SeedConstants.CHAPTER_FPT_HCM);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime baseSubmit = now.minusHours(72);

        float[] scores = {9.0f, 8.0f, 8.0f};
        LocalDateTime[] submits = {baseSubmit, baseSubmit.minusMinutes(5), baseSubmit};
        for (int i = 0; i < 3; i++) {
            int idx = i + 1;
            User leader = seedHelper.upsertStudent(
                    Gd4TiebreakWildcardSeedConstants.studentEmail("gd4st", idx),
                    Gd4TiebreakWildcardSeedConstants.studentDisplayName("gd4st", idx),
                    hcm);
            seedHelper.registerStudent(structure.hackathon(), leader);
            Team team = seedHelper.ensureActiveTeam(
                    structure.hackathon(), "GD4-ST-T%02d".formatted(idx), leader, hcm, now);
            seedHelper.ensureTeamLocked(team, now);
            seedHelper.ensureLottery(structure.hackathon(), prelim, track1, "A", team, coordinator, now);
            Submission sub = seedHelper.ensurePrelimSubmission(
                    structure.hackathon(), prelim, track1, team,
                    SubmissionStatus.SUBMITTED, false, submits[i]);
            seedHelper.scoreAllTrackCriteria(sub, track1, judge1, scores[i], true);
        }

        seedHelper.ensureFinalGuestJudgeAssignment(structure.hackathon(), structure.finalRound());
        seedHelper.seedFinalRoundProblem(structure.finalRound());
        log.info("[Gd4TiebreakWildcardDataSeeder] slug={} prelimId={} rule=SUBMISSION_TIME",
                slug, prelim.getId());
    }

    private void seedManualTiebreak() {
        String slug = Gd4TiebreakWildcardSeedConstants.SLUG_TIEBREAK_MANUAL;
        HackathonDevSeedHelper.PrelimState prelimState =
                new HackathonDevSeedHelper.PrelimState(false, true, true, false, 1, 2);
        HackathonDevSeedHelper.HackathonStructure structure = seedHelper.ensureHackathonStructure(
                slug,
                "SEAL GĐ4 — Tiebreak Manual",
                HackathonStatus.ONGOING,
                "Seed: 2 đội đồng điểm biên Top-1, COORDINATOR_DECISION — cần reorder tay",
                prelimState,
                new HackathonDevSeedHelper.FinalState(false, false),
                seedHelper.computeGd4AdvanceReadyDates());

        Round prelim = applyPrelimConfig(
                structure.prelim(), TiebreakRule.COORDINATOR_DECISION, false, 1, 2);
        Track track1 = structure.track1();
        User coordinator = seedHelper.requireCoordinator();
        User judge1 = seedHelper.requireJudge1();
        Chapter hcm = seedHelper.requireChapter(Gd1SeedConstants.CHAPTER_FPT_HCM);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime submittedAt = now.minusHours(72);

        for (int i = 0; i < 2; i++) {
            int idx = i + 1;
            User leader = seedHelper.upsertStudent(
                    Gd4TiebreakWildcardSeedConstants.studentEmail("gd4man", idx),
                    Gd4TiebreakWildcardSeedConstants.studentDisplayName("gd4man", idx),
                    hcm);
            seedHelper.registerStudent(structure.hackathon(), leader);
            Team team = seedHelper.ensureActiveTeam(
                    structure.hackathon(), "GD4-MAN-T%02d".formatted(idx), leader, hcm, now);
            seedHelper.ensureTeamLocked(team, now);
            seedHelper.ensureLottery(structure.hackathon(), prelim, track1, "A", team, coordinator, now);
            Submission sub = seedHelper.ensurePrelimSubmission(
                    structure.hackathon(), prelim, track1, team,
                    SubmissionStatus.SUBMITTED, false, submittedAt);
            seedHelper.scoreAllTrackCriteria(sub, track1, judge1, 8.5f, true);
        }

        seedHelper.ensureFinalGuestJudgeAssignment(structure.hackathon(), structure.finalRound());
        seedHelper.seedFinalRoundProblem(structure.finalRound());
        log.info("[Gd4TiebreakWildcardDataSeeder] slug={} prelimId={} rule=COORDINATOR_DECISION",
                slug, prelim.getId());
    }

    private void seedWildcardGap() {
        String slug = Gd4TiebreakWildcardSeedConstants.SLUG_WILDCARD_GAP;
        // 2 tracks × topN=1 = 2 auto; minFinal=4 → slots=2
        HackathonDevSeedHelper.PrelimState prelimState =
                new HackathonDevSeedHelper.PrelimState(false, true, true, true, 1, 4);
        HackathonDevSeedHelper.HackathonStructure structure = seedHelper.ensureHackathonStructure(
                slug,
                "SEAL GĐ4 — Wildcard Gap",
                HackathonStatus.ONGOING,
                "Seed: 2 bảng, topN=1, minFinal=4 → 2 ghế vé vớt",
                prelimState,
                new HackathonDevSeedHelper.FinalState(false, false),
                seedHelper.computeGd4AdvanceReadyDates());

        Round prelim = applyPrelimConfig(structure.prelim(), TiebreakRule.PENALTY_SCORE, true, 1, 4);
        Track track1 = structure.track1();
        Track track2 = structure.track2();
        User coordinator = seedHelper.requireCoordinator();
        User judge1 = seedHelper.requireJudge1();
        User judge2 = seedHelper.requireJudge2();
        Chapter hcm = seedHelper.requireChapter(Gd1SeedConstants.CHAPTER_FPT_HCM);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime submittedAt = now.minusHours(72);

        // 4 teams / track: ranks 1..4
        float[] scores = {9.0f, 7.5f, 6.5f, 5.0f};
        String[] groups = {"A", "A", "A", "A"};
        for (int i = 0; i < 4; i++) {
            int idx = i + 1;
            seedGapTeam(structure.hackathon(), prelim, track1, judge1, coordinator, hcm, now, submittedAt,
                    idx, "GD4-WC-A%02d".formatted(idx), groups[i], scores[i], "gd4wca");
        }
        for (int i = 0; i < 4; i++) {
            int idx = i + 1;
            seedGapTeam(structure.hackathon(), prelim, track2, judge2, coordinator, hcm, now, submittedAt,
                    idx + 4, "GD4-WC-B%02d".formatted(idx), "B", scores[i], "gd4wcb");
        }

        seedHelper.ensureFinalGuestJudgeAssignment(structure.hackathon(), structure.finalRound());
        seedHelper.seedFinalRoundProblem(structure.finalRound());
        log.info("[Gd4TiebreakWildcardDataSeeder] slug={} prelimId={} slots≈2", slug, prelim.getId());
    }

    private void seedGapTeam(
            Hackathon hackathon,
            Round prelim,
            Track track,
            User judge,
            User coordinator,
            Chapter chapter,
            LocalDateTime now,
            LocalDateTime submittedAt,
            int emailIdx,
            String teamName,
            String group,
            float score,
            String emailPrefix) {
        User leader = seedHelper.upsertStudent(
                Gd4TiebreakWildcardSeedConstants.studentEmail(emailPrefix, emailIdx),
                Gd4TiebreakWildcardSeedConstants.studentDisplayName(emailPrefix, emailIdx),
                chapter);
        seedHelper.registerStudent(hackathon, leader);
        Team team = seedHelper.ensureActiveTeam(hackathon, teamName, leader, chapter, now);
        seedHelper.ensureTeamLocked(team, now);
        seedHelper.ensureLottery(hackathon, prelim, track, group, team, coordinator, now);
        Submission sub = seedHelper.ensurePrelimSubmission(
                hackathon, prelim, track, team, SubmissionStatus.SUBMITTED, false, submittedAt);
        seedHelper.scoreAllTrackCriteria(sub, track, judge, score, true);
    }

    private Round applyPrelimConfig(
            Round prelim,
            TiebreakRule rule,
            boolean wildcardEnabled,
            int topN,
            int minFinal) {
        prelim.setTiebreakRule(rule);
        prelim.setWildcardEnabled(wildcardEnabled);
        prelim.setTopNAdvance(topN);
        prelim.setMinTeamsFinal(minFinal);
        return roundRepository.save(prelim);
    }
}
