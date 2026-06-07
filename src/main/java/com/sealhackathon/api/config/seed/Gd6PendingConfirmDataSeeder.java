package com.sealhackathon.api.config.seed;

import com.sealhackathon.api.chapters.entity.Chapter;
import com.sealhackathon.api.criteria.entity.Criteria;
import com.sealhackathon.api.criteria.repository.CriteriaRepository;
import com.sealhackathon.api.criteria.value_object.CriteriaType;
import com.sealhackathon.api.events.repository.EventRepository;
import com.sealhackathon.api.events.value_object.EventType;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.prizes.entity.Prize;
import com.sealhackathon.api.prizes.repository.PrizeRepository;
import com.sealhackathon.api.prizes.value_object.PrizeRank;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.scores.entity.Score;
import com.sealhackathon.api.scores.repository.ScoreRepository;
import com.sealhackathon.api.scores.value_object.ScoreType;
import com.sealhackathon.api.submissions.entity.Submission;
import com.sealhackathon.api.submissions.repository.SubmissionRepository;
import com.sealhackathon.api.submissions.value_object.SubmissionStatus;
import com.sealhackathon.api.teams.entity.Team;
import com.sealhackathon.api.tracks.entity.Track;
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
 * CK locked, hackathon {@code PENDING_CONFIRM}, AWARDS event, giải Nhất mẫu — test trao giải / confirm / export.
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class Gd6PendingConfirmDataSeeder {

    private final HackathonDevSeedHelper seedHelper;
    private final HackathonRepository hackathonRepository;
    private final RoundRepository roundRepository;
    private final SubmissionRepository submissionRepository;
    private final ScoreRepository scoreRepository;
    private final CriteriaRepository criteriaRepository;
    private final PrizeRepository prizeRepository;
    private final EventRepository eventRepository;

    @Transactional
    public void ensureSeed() {
        var structure = seedHelper.ensureHackathonStructure(
                GdExtendedSeedConstants.SLUG_GD6_PENDING_CONFIRM,
                "SEAL GĐ6 Pending Confirm",
                HackathonStatus.PENDING_CONFIRM,
                "Seed GĐ6 — trao giải, confirm FINISHED, rankings (sau lock Chung kết).",
                new HackathonDevSeedHelper.PrelimState(false, true, true, true, 2, 3),
                new HackathonDevSeedHelper.FinalState(true, true));

        ensureHackathonPendingConfirm(structure.hackathon());
        ensurePrelimLockedPublished(structure.prelim(), seedHelper.requireCoordinator());
        ensureFinalRoundSetup(structure.finalRound(), seedHelper.requireCoordinator());

        LocalDateTime now = LocalDateTime.now();
        Chapter hcm = seedHelper.requireChapter(Gd1SeedConstants.CHAPTER_FPT_HCM);
        User coordinator = seedHelper.requireCoordinator();
        User judge1 = seedHelper.requireJudge1();
        User guestJudge = seedHelper.requireGuestJudge();

        Team t1 = ensureTeam(structure, GdExtendedSeedConstants.GD6_TEAM_ADV_01,
                GdExtendedSeedConstants.GD6_STU_01, "GD6 Leader 01", hcm, now);
        Team t2 = ensureTeam(structure, GdExtendedSeedConstants.GD6_TEAM_ADV_02,
                GdExtendedSeedConstants.GD6_STU_02, "GD6 Leader 02", hcm, now);
        Team t3 = ensureTeam(structure, GdExtendedSeedConstants.GD6_TEAM_ADV_03,
                GdExtendedSeedConstants.GD6_STU_03, "GD6 Leader 03", hcm, now);

        Track track1 = structure.track1();
        Criteria prelimCrit = firstGradableTrackCriterion(track1);
        List<Criteria> finalCriteria = gradableFinalCriteria(structure.finalRound());

        seedTeamRoundData(t1, structure, track1, "Bảng A", coordinator, now,
                prelimCrit, finalCriteria, judge1, guestJudge, 9.0f, 9.2f);
        seedTeamRoundData(t2, structure, track1, "Bảng A", coordinator, now,
                prelimCrit, finalCriteria, judge1, guestJudge, 8.5f, 8.6f);
        seedTeamRoundData(t3, structure, track1, "Bảng B", coordinator, now,
                prelimCrit, finalCriteria, judge1, guestJudge, 8.0f, 8.1f);

        ensureFirstPrizeIfMissing(structure, t1);

        Submission finalSub1 = findFinalSubmission(t1, structure);
        Submission finalSub2 = findFinalSubmission(t2, structure);
        Submission finalSub3 = findFinalSubmission(t3, structure);

        boolean hasAwards = eventRepository.existsByHackathonIdAndType(
                structure.hackathon().getId(), EventType.AWARDS);

        log.info("""
                [Gd6PendingConfirmDataSeeder] GĐ6 E2E slug={}
                  hackathonId={} status=PENDING_CONFIRM
                  prelimRoundId={} (published+locked) | finalRoundId={} (active+locked)
                  finalCriterionId={} | hasAwardsEvent={}
                  teams: t1={} rank1 CK={} | t2={} test POST SECOND prize | t3={}
                  finalSubmissionId(t1)={} (t2)={} (t3)={}
                  prize FIRST seeded on t1 — test 6.2 POST SECOND for t2
                  coord={} / {} | student01={} student02={} student03={} password={}
                  Flow 6.0b AWARDS ready → 6.1 rankings → 6.2 prize → 6.3 confirm → 6.4 export
                  Doc: docs/testing/gd6-e2e-seed-data.md
                """,
                GdExtendedSeedConstants.SLUG_GD6_PENDING_CONFIRM,
                structure.hackathon().getId(),
                structure.prelim().getId(),
                structure.finalRound().getId(),
                finalCriteria.isEmpty() ? "n/a" : finalCriteria.get(0).getId(),
                hasAwards,
                t1.getId(), finalSub1 != null ? "scored" : "missing",
                t2.getId(), t3.getId(),
                finalSub1 != null ? finalSub1.getId() : "n/a",
                finalSub2 != null ? finalSub2.getId() : "n/a",
                finalSub3 != null ? finalSub3.getId() : "n/a",
                Gd1SeedConstants.EMAIL_COORDINATOR,
                Gd1SeedConstants.DEV_COORDINATOR_PASSWORD,
                GdExtendedSeedConstants.GD6_STU_01,
                GdExtendedSeedConstants.GD6_STU_02,
                GdExtendedSeedConstants.GD6_STU_03,
                GdExtendedSeedConstants.DEV_STUDENT_PASSWORD);
    }

    private void ensureHackathonPendingConfirm(Hackathon hackathon) {
        if (hackathon.getStatus() != HackathonStatus.PENDING_CONFIRM) {
            hackathon.setStatus(HackathonStatus.PENDING_CONFIRM);
            hackathonRepository.save(hackathon);
        }
    }

    private void ensurePrelimLockedPublished(Round prelim, User coordinator) {
        prelim.setIsActive(false);
        prelim.setScoringLocked(true);
        prelim.setIsPublished(true);
        if (prelim.getScoringLockedAt() == null) {
            prelim.setScoringLockedAt(LocalDateTime.now());
        }
        if (prelim.getScoringLockedBy() == null) {
            prelim.setScoringLockedBy(coordinator);
        }
        if (prelim.getPublishedAt() == null) {
            prelim.setPublishedAt(LocalDateTime.now());
        }
        if (prelim.getPublishedBy() == null) {
            prelim.setPublishedBy(coordinator);
        }
        roundRepository.save(prelim);
    }

    private void ensureFinalRoundSetup(Round finalRound, User coordinator) {
        if (finalRound.getProblemReleasedAt() == null) {
            finalRound.setProblemStatementUrl("https://example.com/seed/gd6-debai-chung-ket.pdf");
            finalRound.setProblemReleasedAt(LocalDateTime.now());
        }
        finalRound.setIsActive(true);
        if (finalRound.getActivatedAt() == null) {
            finalRound.setActivatedAt(LocalDateTime.now());
        }
        finalRound.setScoringLocked(true);
        if (finalRound.getScoringLockedAt() == null) {
            finalRound.setScoringLockedAt(LocalDateTime.now());
        }
        if (finalRound.getScoringLockedBy() == null) {
            finalRound.setScoringLockedBy(coordinator);
        }
        roundRepository.save(finalRound);
    }

    private Team ensureTeam(
            HackathonDevSeedHelper.HackathonStructure structure,
            String teamName,
            String studentEmail,
            String fullName,
            Chapter chapter,
            LocalDateTime now) {
        User leader = seedHelper.upsertStudent(studentEmail, fullName, chapter);
        return seedHelper.ensureActiveTeam(structure.hackathon(), teamName, leader, chapter, now);
    }

    private void seedTeamRoundData(
            Team team,
            HackathonDevSeedHelper.HackathonStructure structure,
            Track track,
            String group,
            User coordinator,
            LocalDateTime now,
            Criteria prelimCrit,
            List<Criteria> finalCriteria,
            User prelimJudge,
            User finalJudge,
            float prelimScore,
            float finalBaseScore) {
        seedHelper.ensureLottery(structure.hackathon(), structure.prelim(), track, group, team, coordinator, now);
        seedHelper.markAdvanced(team, structure.prelim(), structure.finalRound(), structure.hackathon());

        Submission prelimSub = upsertPrelimSubmission(team, structure, track);
        upsertFinalizedScore(prelimSub, prelimCrit, prelimJudge, prelimScore);

        Submission finalSub = upsertFinalSubmission(team, structure);
        for (int i = 0; i < finalCriteria.size(); i++) {
            upsertFinalizedScore(finalSub, finalCriteria.get(i), finalJudge, finalBaseScore + (i * 0.05f));
        }
    }

    private Submission findFinalSubmission(Team team, HackathonDevSeedHelper.HackathonStructure structure) {
        return submissionRepository.findByTeam_IdAndRound_Id(team.getId(), structure.finalRound().getId())
                .stream()
                .findFirst()
                .orElse(null);
    }

    private void ensureFirstPrizeIfMissing(HackathonDevSeedHelper.HackathonStructure structure, Team t1) {
        Integer hackathonId = structure.hackathon().getId();
        if (prizeRepository.existsByHackathonIdAndPrizeRank(hackathonId, PrizeRank.FIRST)) {
            return;
        }
        prizeRepository.save(Prize.builder()
                .hackathon(structure.hackathon())
                .round(structure.finalRound())
                .track(null)
                .team(t1)
                .prizeName("Giải Nhất (seed)")
                .prizeRank(PrizeRank.FIRST)
                .prizeValue("7000000")
                .awardedBy(seedHelper.requireCoordinator())
                .build());
    }

    private Submission upsertPrelimSubmission(
            Team team,
            HackathonDevSeedHelper.HackathonStructure structure,
            Track track) {
        return submissionRepository.findByTeam_IdAndRound_Id(team.getId(), structure.prelim().getId())
                .stream()
                .findFirst()
                .orElseGet(() -> submissionRepository.save(Submission.builder()
                        .team(team)
                        .round(structure.prelim())
                        .hackathon(structure.hackathon())
                        .track(track)
                        .status(SubmissionStatus.SUBMITTED)
                        .submittedAt(LocalDateTime.now())
                        .repoUrl("https://github.com/seed/gd6-prelim-" + team.getId())
                        .build()));
    }

    private Submission upsertFinalSubmission(Team team, HackathonDevSeedHelper.HackathonStructure structure) {
        return submissionRepository.findByTeam_IdAndRound_Id(team.getId(), structure.finalRound().getId())
                .stream()
                .findFirst()
                .orElseGet(() -> submissionRepository.save(Submission.builder()
                        .team(team)
                        .round(structure.finalRound())
                        .hackathon(structure.hackathon())
                        .track(null)
                        .status(SubmissionStatus.SUBMITTED)
                        .submittedAt(LocalDateTime.now())
                        .repoUrl("https://github.com/seed/gd6-final-" + team.getId())
                        .demoUrl("https://demo.example.com/gd6-final-" + team.getId())
                        .slideUrl("https://slides.example.com/gd6-final-" + team.getId())
                        .build()));
    }

    private void upsertFinalizedScore(Submission sub, Criteria crit, User judge, float value) {
        Score score = scoreRepository
                .findBySubmission_IdAndJudge_IdAndCriterion_IdAndScoreType(
                        sub.getId(), judge.getId(), crit.getId(), ScoreType.NORMAL)
                .orElseGet(() -> Score.builder()
                        .submission(sub)
                        .criterion(crit)
                        .judge(judge)
                        .scoreType(ScoreType.NORMAL)
                        .build());
        score.setScoreValue(value);
        score.setComment("Seed GĐ6");
        score.setIsFinal(true);
        score.setScoredAt(LocalDateTime.now());
        score.setUpdatedAt(LocalDateTime.now());
        scoreRepository.save(score);
    }

    private Criteria firstGradableTrackCriterion(Track track) {
        return criteriaRepository.findByTrackIdOrderByDisplayOrderAsc(track.getId()).stream()
                .filter(c -> c.getType() != CriteriaType.PENALTY)
                .findFirst()
                .orElseGet(() -> seedHelper.firstCriterionForTrack(track));
    }

    private List<Criteria> gradableFinalCriteria(Round finalRound) {
        return criteriaRepository.findByFinalRoundIdOrderByDisplayOrderAsc(finalRound.getId()).stream()
                .filter(c -> c.getType() != CriteriaType.PENALTY)
                .toList();
    }
}
