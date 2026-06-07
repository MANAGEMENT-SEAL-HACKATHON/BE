package com.sealhackathon.api.config.seed;

import com.sealhackathon.api.chapters.entity.Chapter;
import com.sealhackathon.api.criteria.entity.Criteria;
import com.sealhackathon.api.criteria.repository.CriteriaRepository;
import com.sealhackathon.api.criteria.value_object.CriteriaType;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
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
 * Seed GĐ4 E2E — {@link GdExtendedSeedConstants#SLUG_GD4_ADVANCE_READY}:
 * Sơ loại đã lock scoring, chưa publish; 8 đội đủ điểm → ranking, wildcard, advance, activate CK.
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class Gd4AdvanceDataSeeder {

    private final HackathonDevSeedHelper seedHelper;
    private final RoundRepository roundRepository;
    private final SubmissionRepository submissionRepository;
    private final ScoreRepository scoreRepository;
    private final CriteriaRepository criteriaRepository;

    @Transactional
    public void ensureSeed() {
        var structure = seedHelper.ensureHackathonStructure(
                GdExtendedSeedConstants.SLUG_GD4_ADVANCE_READY,
                "SEAL GĐ4 Advance Ready",
                HackathonStatus.ONGOING,
                "Seed GĐ4 — prelim locked+scored, chưa publish: test ranking/wildcard/advance/activate CK.",
                new HackathonDevSeedHelper.PrelimState(false, true, true, false, 1, 6),
                new HackathonDevSeedHelper.FinalState(false, false));

        applyPrelimLockedState(structure.prelim());

        LocalDateTime now = LocalDateTime.now();
        Chapter hcm = seedHelper.requireChapter(Gd1SeedConstants.CHAPTER_FPT_HCM);
        User coordinator = seedHelper.requireCoordinator();
        User judge1 = seedHelper.requireJudge1();
        User judge2 = seedHelper.requireJudge2();
        User guestJudge = seedHelper.requireGuestJudge();

        Track track1 = structure.track1();
        Track track2 = structure.track2();

        Team[] teams = seedTeams(structure, hcm, coordinator, now);
        seedLottery(structure, coordinator, now, teams, track1, track2);

        float[] baseScores = {9.5f, 8.0f, 9.0f, 7.5f, 9.2f, 8.1f, 8.8f, 7.0f};
        Track[] tracks = {track1, track1, track1, track1, track2, track2, track2, track2};

        for (int i = 0; i < teams.length; i++) {
            Submission sub = ensureSubmission(teams[i], structure, tracks[i]);
            ensureFullTrackScores(sub, tracks[i], List.of(judge1, judge2), true, baseScores[i]);
        }

        log.info("""
                [Gd4AdvanceDataSeeder] GĐ4 E2E slug={}
                  hackathonId={} prelimRoundId={} finalRoundId={}
                  track1Id={} track2Id={} guestJudgeId={} guestJudgeEmail={}
                  prelim: scoringLocked=true, isPublished=false, topNAdvance=1, minTeamsFinal=6
                  teams (8): t01={} t02={} t03={} t04={} t05={} t06={} t07={} t08={}
                  Gợi ý advance (sau wildcard 2 đội): advanced=[t01,t03,t05,t07,t02,t06] eliminated=[t04,t08]
                  Flow: 4.1 ranking → 4.2 wildcard-candidates → 4.2b PATCH wildcard-reviews → 4.3 publish
                        → 4.4 advance → 4.5 judge FINAL_EXTERNAL (guest) → 4.5b readiness → 4.6 activate CK
                  Accounts: coord={} guest={} students=student.gd4a.leader01..08@fpt.edu.vn pwd={}
                  Doc: docs/testing/gd4-gd5-e2e-seed-data.md
                """,
                GdExtendedSeedConstants.SLUG_GD4_ADVANCE_READY,
                structure.hackathon().getId(),
                structure.prelim().getId(),
                structure.finalRound().getId(),
                track1.getId(),
                track2.getId(),
                guestJudge.getId(),
                Gd1SeedConstants.EMAIL_GUEST_JUDGE,
                teams[0].getId(), teams[1].getId(), teams[2].getId(), teams[3].getId(),
                teams[4].getId(), teams[5].getId(), teams[6].getId(), teams[7].getId(),
                Gd1SeedConstants.EMAIL_COORDINATOR,
                Gd1SeedConstants.EMAIL_GUEST_JUDGE,
                GdExtendedSeedConstants.DEV_STUDENT_PASSWORD);
    }

    private void applyPrelimLockedState(Round prelim) {
        if (!Boolean.TRUE.equals(prelim.getScoringLocked())) {
            prelim.setScoringLocked(true);
            prelim.setScoringLockedAt(LocalDateTime.now());
        }
        prelim.setIsPublished(false);
        prelim.setPublishedAt(null);
        prelim.setIsActive(false);
        prelim.setTopNAdvance(1);
        prelim.setMinTeamsFinal(6);
        prelim.setWildcardEnabled(true);
        if (prelim.getProblemReleasedAt() == null) {
            prelim.setProblemStatementUrl("https://example.com/seed/gd4-prelim-de.pdf");
            prelim.setProblemReleasedAt(LocalDateTime.now());
        }
        roundRepository.save(prelim);
    }

    private Team[] seedTeams(
            HackathonDevSeedHelper.HackathonStructure structure,
            Chapter hcm,
            User coordinator,
            LocalDateTime now) {
        String[] names = {
                GdExtendedSeedConstants.GD4A_TEAM_01,
                GdExtendedSeedConstants.GD4A_TEAM_02,
                GdExtendedSeedConstants.GD4A_TEAM_03,
                GdExtendedSeedConstants.GD4A_TEAM_04,
                GdExtendedSeedConstants.GD4A_TEAM_05,
                GdExtendedSeedConstants.GD4A_TEAM_06,
                GdExtendedSeedConstants.GD4A_TEAM_07,
                GdExtendedSeedConstants.GD4A_TEAM_08
        };
        String[] emails = {
                GdExtendedSeedConstants.GD4A_STU_01,
                GdExtendedSeedConstants.GD4A_STU_02,
                GdExtendedSeedConstants.GD4A_STU_03,
                GdExtendedSeedConstants.GD4A_STU_04,
                GdExtendedSeedConstants.GD4A_STU_05,
                GdExtendedSeedConstants.GD4A_STU_06,
                GdExtendedSeedConstants.GD4A_STU_07,
                GdExtendedSeedConstants.GD4A_STU_08
        };
        Team[] teams = new Team[8];
        for (int i = 0; i < names.length; i++) {
            User leader = seedHelper.upsertStudent(emails[i], "GD4A Leader " + (i + 1), hcm);
            teams[i] = seedHelper.ensureActiveTeam(structure.hackathon(), names[i], leader, hcm, now);
        }
        return teams;
    }

    private void seedLottery(
            HackathonDevSeedHelper.HackathonStructure structure,
            User coordinator,
            LocalDateTime now,
            Team[] teams,
            Track track1,
            Track track2) {
        String[] groups = {"Bảng A", "Bảng A", "Bảng B", "Bảng B", "Bảng C", "Bảng C", "Bảng D", "Bảng D"};
        Track[] tracks = {track1, track1, track1, track1, track2, track2, track2, track2};
        for (int i = 0; i < teams.length; i++) {
            seedHelper.ensureLottery(structure.hackathon(), structure.prelim(), tracks[i],
                    groups[i], teams[i], coordinator, now);
        }
    }

    private Submission ensureSubmission(
            Team team,
            HackathonDevSeedHelper.HackathonStructure structure,
            Track track) {
        return submissionRepository.findByTeam_IdAndRound_Id(team.getId(), structure.prelim().getId())
                .stream()
                .findFirst()
                .or(() -> submissionRepository.findByTeam_IdAndTrack_Round_Id(
                        team.getId(), structure.prelim().getId()).stream().findFirst())
                .map(existing -> {
                    existing.setTrack(track);
                    existing.setStatus(SubmissionStatus.SUBMITTED);
                    existing.setIsLate(false);
                    existing.setRepoUrl("https://github.com/seed/gd4a-" + team.getId());
                    existing.setDemoUrl("https://demo.example.com/gd4a-" + team.getId());
                    return submissionRepository.save(existing);
                })
                .orElseGet(() -> submissionRepository.save(Submission.builder()
                        .team(team)
                        .round(structure.prelim())
                        .hackathon(structure.hackathon())
                        .track(track)
                        .status(SubmissionStatus.SUBMITTED)
                        .isLate(false)
                        .submittedAt(LocalDateTime.now())
                        .repoUrl("https://github.com/seed/gd4a-" + team.getId())
                        .demoUrl("https://demo.example.com/gd4a-" + team.getId())
                        .build()));
    }

    private void ensureFullTrackScores(
            Submission sub,
            Track track,
            List<User> judges,
            boolean isFinal,
            float baseScore) {
        List<Criteria> criteria = criteriaRepository.findByTrackIdOrderByDisplayOrderAsc(track.getId()).stream()
                .filter(c -> c.getType() != CriteriaType.PENALTY)
                .toList();
        for (int i = 0; i < criteria.size(); i++) {
            Criteria criterion = criteria.get(i);
            float value = baseScore + (i * 0.1f);
            for (User judge : judges) {
                upsertScore(sub, criterion, judge, value, isFinal);
            }
        }
    }

    private void upsertScore(Submission sub, Criteria crit, User judge, float value, boolean isFinal) {
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
        score.setComment("Seed GĐ4A");
        score.setIsFinal(isFinal);
        score.setScoredAt(LocalDateTime.now());
        score.setUpdatedAt(LocalDateTime.now());
        scoreRepository.save(score);
    }
}
