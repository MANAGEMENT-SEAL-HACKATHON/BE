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
 * Seed GĐ3 — judge1 vừa mentor vừa judge track1, queue PRESENTING.
 *
 * <p>POST score → 409 {@code CONFLICT_MENTOR_JUDGE_SAME_TRACK}
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class Gd3JudgeMentorConflictDataSeeder {

    private final HackathonDevSeedHelper seedHelper;
    private final HackathonRepository hackathonRepository;
    private final RoundRepository roundRepository;
    private final DevSeedCleanup devSeedCleanup;

    @Value("${app.seed.gd3.judge-mentor-conflict.enabled:true}")
    private boolean enabled;

    @Transactional
    public void ensureSeed() {
        if (!enabled) {
            log.info("[Gd3JudgeMentorConflictDataSeeder] Tắt (app.seed.gd3.judge-mentor-conflict.enabled=false)");
            return;
        }

        HackathonDevSeedHelper.PrelimState prelimState =
                new HackathonDevSeedHelper.PrelimState(true, true, false, false, 2, 4);
        HackathonDevSeedHelper.HackathonStructure structure = seedHelper.ensureHackathonStructure(
                Gd3JudgeMentorConflictSeedConstants.SLUG_GD3_JUDGE_MENTOR_CONFLICT,
                "SEAL GĐ3 — Judge mentor conflict",
                HackathonStatus.ONGOING,
                "Seed GĐ3 — judge1 vừa mentor vừa judge track1 → CONFLICT_MENTOR_JUDGE_SAME_TRACK",
                prelimState,
                new HackathonDevSeedHelper.FinalState(false, false),
                seedHelper.computeGd3ActivePrelimDates());

        Hackathon hackathon = structure.hackathon();
        Round prelim = structure.prelim();
        Round finalRound = structure.finalRound();
        Track track1 = structure.track1();

        if (needsRepair(hackathon, prelim, finalRound)) {
            seedHelper.repairHackathonForGd3Retest(hackathon, prelim, finalRound);
            hackathon = hackathonRepository.findById(hackathon.getId()).orElse(hackathon);
            prelim = loadPrelim(hackathon.getId());
            finalRound = loadFinal(hackathon.getId());
        }

        seedHelper.syncHackathonCalendarFromDates(
                Gd3JudgeMentorConflictSeedConstants.SLUG_GD3_JUDGE_MENTOR_CONFLICT,
                seedHelper.computeGd3ActivePrelimDates());
        prelim = loadPrelim(hackathon.getId());

        User coordinator = seedHelper.requireCoordinator();
        User judge1 = seedHelper.requireJudge1();
        Chapter hcm = seedHelper.requireChapter(Gd1SeedConstants.CHAPTER_FPT_HCM);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime submittedAt = prelim.getSubmissionDeadline().minusHours(2);

        String[] teamNames = {
                Gd3JudgeMentorConflictSeedConstants.TEAM_PRESENTING,
                Gd3JudgeMentorConflictSeedConstants.TEAM_WAITING
        };
        List<Submission> subs = new ArrayList<>();
        for (int i = 0; i < teamNames.length; i++) {
            int idx = i + 1;
            User leader = seedHelper.upsertStudent(
                    Gd3JudgeMentorConflictSeedConstants.studentEmail(idx),
                    Gd3JudgeMentorConflictSeedConstants.studentDisplayName(idx),
                    hcm);
            seedHelper.registerStudent(hackathon, leader);
            Team team = seedHelper.ensureActiveTeamForLeader(hackathon, teamNames[i], leader, hcm, now);
            seedHelper.ensureTeamLocked(team, now);
            seedHelper.ensureLottery(hackathon, prelim, track1, "BANG-A", team, coordinator, now);
            Submission sub = seedHelper.ensurePrelimSubmission(
                    hackathon, prelim, track1, team, SubmissionStatus.SUBMITTED, false, submittedAt);
            seedHelper.markSubmissionSlideSeeded(sub);
            seedHelper.clearSubmissionScores(sub.getId());
            subs.add(sub);
        }

        seedHelper.clearMentorAssignments(track1);
        seedHelper.ensureMentorTrackAssignment(track1, judge1, coordinator);
        seedHelper.ensureJudgeOnTrack(judge1, track1, coordinator);
        seedHelper.seedPresentationQueue(
                prelim,
                track1,
                subs,
                0,
                HackathonDevSeedHelper.PresentationTimerSeed.presenting());

        log.info("""
                [Gd3JudgeMentorConflictDataSeeder] slug={} prelimRoundId={}
                  judge1 vừa mentor vừa judge track1 — POST score → CONFLICT_MENTOR_JUDGE_SAME_TRACK
                """,
                Gd3JudgeMentorConflictSeedConstants.SLUG_GD3_JUDGE_MENTOR_CONFLICT,
                prelim.getId());
    }

    @Transactional
    public void repairForFeTesting() {
        if (!enabled) {
            return;
        }
        hackathonRepository.findBySlug(Gd3JudgeMentorConflictSeedConstants.SLUG_GD3_JUDGE_MENTOR_CONFLICT)
                .ifPresent(h -> {
                    seedHelper.syncHackathonCalendarFromDates(
                            Gd3JudgeMentorConflictSeedConstants.SLUG_GD3_JUDGE_MENTOR_CONFLICT,
                            seedHelper.computeGd3ActivePrelimDates());
                    Round prelim = loadPrelim(h.getId());
                    Round finalRound = loadFinal(h.getId());
                    if (needsRepair(h, prelim, finalRound)) {
                        seedHelper.repairHackathonForGd3Retest(h, prelim, finalRound);
                    }
                    ensureSeed();
                });
    }

    @Transactional
    public void resetAndSeed() {
        devSeedCleanup.purgeIfPresent(Gd3JudgeMentorConflictSeedConstants.SLUG_GD3_JUDGE_MENTOR_CONFLICT);
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
