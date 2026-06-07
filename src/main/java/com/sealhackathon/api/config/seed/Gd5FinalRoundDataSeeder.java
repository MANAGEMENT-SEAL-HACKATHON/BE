package com.sealhackathon.api.config.seed;

import com.sealhackathon.api.chapters.entity.Chapter;
import com.sealhackathon.api.criteria.entity.Criteria;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.scores.entity.Score;
import com.sealhackathon.api.scores.repository.ScoreRepository;
import com.sealhackathon.api.scores.value_object.ScoreType;
import com.sealhackathon.api.submissions.entity.Submission;
import com.sealhackathon.api.submissions.repository.SubmissionRepository;
import com.sealhackathon.api.submissions.value_object.SubmissionStatus;
import com.sealhackathon.api.teams.entity.Team;
import com.sealhackathon.api.teams.repository.TeamRepository;
import com.sealhackathon.api.users.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Seed GĐ5 — {@link GdExtendedSeedConstants#SLUG_GD5_FINAL_ACTIVE}:
 * Sơ loại đã publish+lock, Chung kết <strong>active</strong> và <strong>chưa</strong> lock scoring,
 * hackathon vẫn {@code ONGOING} (chưa {@code PENDING_CONFIRM}).
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class Gd5FinalRoundDataSeeder {

    private final HackathonDevSeedHelper seedHelper;
    private final TeamRepository teamRepository;
    private final RoundRepository roundRepository;
    private final SubmissionRepository submissionRepository;
    private final ScoreRepository scoreRepository;

    @Transactional
    public void ensureSeed() {
        var structure = seedHelper.ensureHackathonStructure(
                GdExtendedSeedConstants.SLUG_GD5_FINAL_ACTIVE,
                "SEAL GĐ5 Final Active",
                HackathonStatus.ONGOING,
                "Seed GĐ5 — Chung kết đang thi: nộp bài CK, chấm, lock CK → sau đó PATCH PENDING_CONFIRM.",
                new HackathonDevSeedHelper.PrelimState(false, true, true, true, 2, 6),
                new HackathonDevSeedHelper.FinalState(true, false));

        applyFinalRoundExamSetup(structure);

        if (teamRepository.existsByHackathon_IdAndTeamNameIgnoreCase(
                structure.hackathon().getId(), GdExtendedSeedConstants.GD5_TEAM_FINAL_SCORED)) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        Chapter hcm = seedHelper.requireChapter(Gd1SeedConstants.CHAPTER_FPT_HCM);
        User coordinator = seedHelper.requireCoordinator();
        User guestJudge = seedHelper.requireGuestJudge();

        Team t1 = seedTeam(structure, GdExtendedSeedConstants.GD5_TEAM_FINAL_SCORED,
                GdExtendedSeedConstants.GD5_STU_01, "GD5 Leader 01", hcm, coordinator, now);
        Team t2 = seedTeam(structure, GdExtendedSeedConstants.GD5_TEAM_FINAL_SUBMITTED,
                GdExtendedSeedConstants.GD5_STU_02, "GD5 Leader 02", hcm, coordinator, now);
        Team t3 = seedTeam(structure, GdExtendedSeedConstants.GD5_TEAM_NO_FINAL_SUB,
                GdExtendedSeedConstants.GD5_STU_03, "GD5 Leader 03", hcm, coordinator, now);
        Team t4 = seedTeam(structure, GdExtendedSeedConstants.GD5_TEAM_ADV_ONLY,
                GdExtendedSeedConstants.GD5_STU_04, "GD5 Leader 04", hcm, coordinator, now);

        Criteria prelimCrit = seedHelper.firstCriterionForTrack(structure.track1());
        Criteria finalCrit = seedHelper.firstCriterionForFinal(structure.finalRound());

        for (Team team : new Team[] {t1, t2, t3, t4}) {
            seedHelper.markAdvanced(team, structure.prelim(), structure.finalRound(), structure.hackathon());
            Submission prelimSub = upsertPrelimSubmission(team, structure);
            upsertScore(prelimSub, prelimCrit, seedHelper.requireJudge1(), 8.0f);
        }

        Submission final1 = upsertFinalSubmission(t1, structure);
        upsertScore(final1, finalCrit, guestJudge, 9.0f);

        upsertFinalSubmission(t2, structure);
        // t3, t4: chưa nộp CK — test POST /submissions (roundId=CK, không trackId)

        log.info("""
                [Gd5FinalRoundDataSeeder] slug={} hackathonId={} status=ONGOING
                  prelim: published+locked | final: active, NOT locked
                  Teams: CK scored | CK submitted chưa chấm | chưa nộp CK (x2)
                  Sau test: PATCH lock CK → PATCH hackathon PENDING_CONFIRM (→ dùng slug gd6 hoặc cùng flow)
                  SV password: {}
                  Doc: docs/testing/seed-coverage-audit.md
                """,
                GdExtendedSeedConstants.SLUG_GD5_FINAL_ACTIVE,
                structure.hackathon().getId(),
                GdExtendedSeedConstants.DEV_STUDENT_PASSWORD);
    }

    private void applyFinalRoundExamSetup(HackathonDevSeedHelper.HackathonStructure structure) {
        var finalRound = structure.finalRound();
        if (finalRound.getProblemReleasedAt() == null) {
            finalRound.setProblemStatementUrl("https://example.com/seed/debai-chung-ket.pdf");
            finalRound.setProblemReleasedAt(LocalDateTime.now());
        }
        roundRepository.save(finalRound);
    }

    private Team seedTeam(
            HackathonDevSeedHelper.HackathonStructure structure,
            String teamName,
            String studentEmail,
            String fullName,
            Chapter hcm,
            User coordinator,
            LocalDateTime now) {
        User leader = seedHelper.upsertStudent(studentEmail, fullName, hcm);
        Team team = seedHelper.ensureActiveTeam(structure.hackathon(), teamName, leader, hcm, now);
        seedHelper.ensureLottery(structure.hackathon(), structure.prelim(), structure.track1(),
                "Bảng A", team, coordinator, now);
        return team;
    }

    private Submission upsertPrelimSubmission(Team team, HackathonDevSeedHelper.HackathonStructure structure) {
        return submissionRepository.findByRound_Id(structure.prelim().getId()).stream()
                .filter(s -> s.getTeam().getId().equals(team.getId()))
                .findFirst()
                .orElseGet(() -> submissionRepository.save(Submission.builder()
                        .team(team)
                        .round(structure.prelim())
                        .hackathon(structure.hackathon())
                        .track(structure.track1())
                        .status(SubmissionStatus.SUBMITTED)
                        .submittedAt(LocalDateTime.now())
                        .repoUrl("https://github.com/seed/gd5-prelim-" + team.getId())
                        .build()));
    }

    private Submission upsertFinalSubmission(Team team, HackathonDevSeedHelper.HackathonStructure structure) {
        return submissionRepository.findByRound_Id(structure.finalRound().getId()).stream()
                .filter(s -> s.getTeam().getId().equals(team.getId()))
                .findFirst()
                .orElseGet(() -> submissionRepository.save(Submission.builder()
                        .team(team)
                        .round(structure.finalRound())
                        .hackathon(structure.hackathon())
                        .track(null)
                        .status(SubmissionStatus.SUBMITTED)
                        .submittedAt(LocalDateTime.now())
                        .repoUrl("https://github.com/seed/gd5-final-" + team.getId())
                        .demoUrl("https://demo.example.com/gd5-final-" + team.getId())
                        .build()));
    }

    private void upsertScore(Submission sub, Criteria crit, User judge, float value) {
        if (scoreRepository.findBySubmission_IdAndCriterion_IdAndScoreTypeAndIsFinal(
                sub.getId(), crit.getId(), ScoreType.NORMAL, true).isEmpty()) {
            scoreRepository.save(Score.builder()
                    .submission(sub)
                    .criterion(crit)
                    .judge(judge)
                    .scoreValue(value)
                    .scoreType(ScoreType.NORMAL)
                    .isFinal(true)
                    .scoredAt(LocalDateTime.now())
                    .build());
        }
    }
}
