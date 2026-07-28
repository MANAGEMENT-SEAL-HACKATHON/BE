package com.sealhackathon.api.config.seed;

import com.sealhackathon.api.chapters.entity.Chapter;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.submissions.entity.Submission;
import com.sealhackathon.api.teams.value_object.ParticipationStatus;
import com.sealhackathon.api.teams.repository.TeamRoundTrackRepository;
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
 * Seed GĐ4 — hackathon {@link Gd4SeedConstants#SLUG_GD4_ADVANCE_READY}.
 *
 * <p>Doc: {@code docs/testing/gd4-full-test-matrix-and-seeds.md} § Profile 0
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class Gd4AdvanceReadyDataSeeder {

    private final HackathonDevSeedHelper seedHelper;
    private final HackathonRepository hackathonRepository;
    private final RoundRepository roundRepository;
    private final TeamRoundTrackRepository teamRoundTrackRepository;
    private final TeamRepository teamRepository;
    private final DevSeedCleanup devSeedCleanup;

    @Value("${app.seed.gd4.enabled:true}")
    private boolean enabled;

    @Transactional
    public void ensureSeed() {
        if (!enabled) {
            log.info("[Gd4AdvanceReadyDataSeeder] Tắt (app.seed.gd4.enabled=false)");
            return;
        }

        HackathonDevSeedHelper.PrelimState prelimState =
                new HackathonDevSeedHelper.PrelimState(false, true, true, true, 1, 6);
        HackathonDevSeedHelper.HackathonStructure structure = seedHelper.ensureHackathonStructure(
                Gd4SeedConstants.SLUG_GD4_ADVANCE_READY,
                "SEAL GĐ4 — Advance ready",
                HackathonStatus.ONGOING,
                "Seed FE GĐ4 — prelim locked+published, 8 đội scored + STT queue",
                prelimState,
                new HackathonDevSeedHelper.FinalState(false, false),
                seedHelper.computeGd4AdvanceReadyDates());

        Hackathon hackathon = structure.hackathon();
        Round prelim = structure.prelim();
        Round finalRound = structure.finalRound();
        Track track1 = structure.track1();
        Track track2 = structure.track2();

        if (needsGd4Repair(hackathon, prelim, finalRound)) {
            seedHelper.repairHackathonForGd4Retest(hackathon, prelim, finalRound);
            hackathon = hackathonRepository.findById(hackathon.getId()).orElse(hackathon);
            prelim = loadPrelim(hackathon.getId());
            finalRound = loadFinal(hackathon.getId());
        }

        seedHelper.syncHackathonCalendarFromDates(
                Gd4SeedConstants.SLUG_GD4_ADVANCE_READY, seedHelper.computeGd4AdvanceReadyDates());
        prelim = loadPrelim(hackathon.getId());

        User coordinator = seedHelper.requireCoordinator();
        User judge1 = seedHelper.requireJudge1();
        User judge2 = seedHelper.requireJudge2();
        Chapter hcm = seedHelper.requireChapter(Gd1SeedConstants.CHAPTER_FPT_HCM);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime submittedAt = now.minusHours(72);

        List<Team> teams = new ArrayList<>();
        List<Submission> track1Subs = new ArrayList<>();
        List<Submission> track2Subs = new ArrayList<>();
        for (int i = 0; i < Gd4SeedConstants.TEAM_NAMES.length; i++) {
            int idx = i + 1;
            User leader = seedHelper.upsertStudent(
                    Gd4SeedConstants.studentEmail(idx),
                    Gd4SeedConstants.studentDisplayName(idx),
                    hcm);
            seedHelper.registerStudent(hackathon, leader);
            Team team = seedHelper.ensureActiveTeam(
                    hackathon, Gd4SeedConstants.TEAM_NAMES[i], leader, hcm, now);
            seedHelper.ensureTeamLocked(team, now);
            Track track = idx <= 4 ? track1 : track2;
            User judge = idx <= 4 ? judge1 : judge2;
            seedHelper.ensureLottery(
                    hackathon, prelim, track, Gd4SeedConstants.GROUPS[i], team, coordinator, now);
            Submission sub = seedHelper.ensurePrelimSubmission(
                    hackathon, prelim, track, team,
                    com.sealhackathon.api.submissions.value_object.SubmissionStatus.SUBMITTED,
                    false, submittedAt.minusMinutes(i));
            seedHelper.scoreAllTrackCriteria(sub, track, judge, Gd4SeedConstants.TEAM_SCORES[i], true);
            teams.add(team);
            if (idx <= 4) {
                track1Subs.add(sub);
            } else {
                track2Subs.add(sub);
            }
        }

        seedHelper.seedPresentationQueue(prelim, track1, track1Subs, -1);
        seedHelper.seedPresentationQueue(prelim, track2, track2Subs, -1);

        // CK: stamp released only (reuse track PDF) — không PDF round-level
        seedHelper.ensureFinalGuestJudgeAssignment(hackathon, finalRound);
        seedHelper.seedFinalRoundProblem(finalRound);
        int finalCriteriaCount = seedHelper.listFinalCriteria(finalRound).size();

        log.info("""
                [Gd4AdvanceReadyDataSeeder] slug={} hackathonId={} prelimRoundId={} finalRoundId={}
                  teams: {} | {} | {} | {} | {} | {} | {} | {}
                  students: {} … {} password={}
                  prelim locked+published + STT queues — sẵn sàng ranking/wildcard/advance
                  final criteria={} guestJudge={} (không tie topN mỗi bảng)
                """,
                Gd4SeedConstants.SLUG_GD4_ADVANCE_READY,
                hackathon.getId(),
                prelim.getId(),
                finalRound.getId(),
                teams.get(0).getId(),
                teams.get(1).getId(),
                teams.get(2).getId(),
                teams.get(3).getId(),
                teams.get(4).getId(),
                teams.get(5).getId(),
                teams.get(6).getId(),
                teams.get(7).getId(),
                Gd4SeedConstants.studentEmail(1),
                Gd4SeedConstants.studentEmail(8),
                DevSeedCatalog.DEV_STUDENT_PASSWORD,
                finalCriteriaCount,
                Gd1SeedConstants.EMAIL_GUEST_JUDGE);
    }

    /** Đồng bộ lịch + trạng thái GĐ4 theo giờ máy — gọi sau repairAll mỗi lần start BE. */
    @Transactional
    public void repairForFeTesting() {
        if (!enabled) {
            return;
        }
        var maybeHackathon = hackathonRepository.findBySlug(Gd4SeedConstants.SLUG_GD4_ADVANCE_READY);
        if (maybeHackathon.isEmpty()) {
            return;
        }
        Hackathon hackathon = maybeHackathon.get();
        Round prelim = loadPrelim(hackathon.getId());
        Round finalRound = loadFinal(hackathon.getId());
        if (needsGd4Repair(hackathon, prelim, finalRound)) {
            seedHelper.repairHackathonForGd4Retest(hackathon, prelim, finalRound);
            prelim = loadPrelim(hackathon.getId());
        }
        boolean synced = seedHelper.syncHackathonCalendarFromDates(
                Gd4SeedConstants.SLUG_GD4_ADVANCE_READY, seedHelper.computeGd4AdvanceReadyDates());
        if (synced) {
            log.info(
                    "[Gd4AdvanceReadyDataSeeder] FE repair — prelim ended slug={} deadline={}",
                    Gd4SeedConstants.SLUG_GD4_ADVANCE_READY,
                    loadPrelim(hackathon.getId()).getSubmissionDeadline());
        }
    }

    @Transactional
    public void resetAndSeed() {
        devSeedCleanup.purgeIfPresent(Gd4SeedConstants.SLUG_GD4_ADVANCE_READY);
        ensureSeed();
    }

    private boolean needsGd4Repair(Hackathon hackathon, Round prelim, Round finalRound) {
        if (hackathon.getStatus() != HackathonStatus.ONGOING) {
            return true;
        }
        if (Boolean.TRUE.equals(finalRound.getIsActive())) {
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
