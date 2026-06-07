package com.sealhackathon.api.config.seed;

import com.sealhackathon.api.chapters.entity.Chapter;
import com.sealhackathon.api.criteria.entity.Criteria;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.prizes.entity.Prize;
import com.sealhackathon.api.prizes.repository.PrizeRepository;
import com.sealhackathon.api.prizes.value_object.PrizeRank;
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
import java.util.List;

/**
 * Seed GĐ6 — {@link GdExtendedSeedConstants#SLUG_GD6_PENDING_CONFIRM}:
 * Sau GĐ5: CK đã lock, hackathon {@code PENDING_CONFIRM}, giải Nhất mẫu (test trao giải / confirm).
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class Gd6PendingConfirmDataSeeder {

    private final HackathonDevSeedHelper seedHelper;
    private final TeamRepository teamRepository;
    private final SubmissionRepository submissionRepository;
    private final ScoreRepository scoreRepository;
    private final PrizeRepository prizeRepository;

    @Transactional
    public void ensureSeed() {
        var structure = seedHelper.ensureHackathonStructure(
                GdExtendedSeedConstants.SLUG_GD6_PENDING_CONFIRM,
                "SEAL GĐ6 Pending Confirm",
                HackathonStatus.PENDING_CONFIRM,
                "Seed GĐ6 — trao giải, confirm FINISHED, rankings (sau lock Chung kết).",
                new HackathonDevSeedHelper.PrelimState(false, true, true, true, 2, 3),
                new HackathonDevSeedHelper.FinalState(true, true));

        if (teamRepository.existsByHackathon_IdAndTeamNameIgnoreCase(
                structure.hackathon().getId(), GdExtendedSeedConstants.GD6_TEAM_ADV_01)) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        Chapter hcm = seedHelper.requireChapter(Gd1SeedConstants.CHAPTER_FPT_HCM);
        User coordinator = seedHelper.requireCoordinator();
        User judge1 = seedHelper.requireJudge1();

        Team t1 = seedHelper.ensureActiveTeam(structure.hackathon(), GdExtendedSeedConstants.GD6_TEAM_ADV_01, 
                seedHelper.upsertStudent(GdExtendedSeedConstants.GD6_STU_01, "GD6 Leader 01", hcm), hcm, now);
        Team t2 = seedHelper.ensureActiveTeam(structure.hackathon(), GdExtendedSeedConstants.GD6_TEAM_ADV_02,
                seedHelper.upsertStudent(GdExtendedSeedConstants.GD6_STU_02, "GD6 Leader 02", hcm), hcm, now);
        Team t3 = seedHelper.ensureActiveTeam(structure.hackathon(), GdExtendedSeedConstants.GD6_TEAM_ADV_03,
                seedHelper.upsertStudent(GdExtendedSeedConstants.GD6_STU_03, "GD6 Leader 03", hcm), hcm, now);

        seedHelper.ensureLottery(structure.hackathon(), structure.prelim(), structure.track1(), "Bảng A", t1, coordinator, now);
        seedHelper.ensureLottery(structure.hackathon(), structure.prelim(), structure.track1(), "Bảng A", t2, coordinator, now);
        seedHelper.ensureLottery(structure.hackathon(), structure.prelim(), structure.track1(), "Bảng B", t3, coordinator, now);

        Criteria prelimCrit = seedHelper.firstCriterionForTrack(structure.track1());
        Criteria finalCrit = seedHelper.firstCriterionForFinal(structure.finalRound());

        for (Team team : List.of(t1, t2, t3)) {
            seedHelper.markAdvanced(team, structure.prelim(), structure.finalRound(), structure.hackathon());
            Submission prelimSub = upsertPrelimSubmission(team, structure);
            upsertScore(prelimSub, prelimCrit, judge1, 9.0f);
            Submission finalSub = upsertFinalSubmission(team, structure);
            upsertScore(finalSub, finalCrit, judge1, 8.5f);
        }

        if (!prizeRepository.existsByHackathonIdAndTeamId(structure.hackathon().getId(), t1.getId())) {
            prizeRepository.save(Prize.builder()
                    .hackathon(structure.hackathon())
                    .round(structure.finalRound())
                    .track(null)
                    .team(t1)
                    .prizeName("Giải Nhất (seed)")
                    .prizeRank(PrizeRank.FIRST)
                    .prizeValue("7000000")
                    .awardedBy(coordinator)
                    .build());
        }

        log.info("""
                [Gd6PendingConfirmDataSeeder] slug={} hackathonId={} status=PENDING_CONFIRM
                  3 teams ADVANCED + prelim/final submissions + FIRST prize on team 01
                  Test thêm: POST prize SECOND team 02 | PATCH confirm FINISHED
                  SV password: {}
                """,
                GdExtendedSeedConstants.SLUG_GD6_PENDING_CONFIRM,
                structure.hackathon().getId(),
                GdExtendedSeedConstants.DEV_STUDENT_PASSWORD);
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
                        .repoUrl("https://github.com/seed/gd6-prelim-" + team.getId())
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
                        .repoUrl("https://github.com/seed/gd6-final-" + team.getId())
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
