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

/**
 * Seed GĐ3 bad path — round inactive, track thiếu judge, submission INCOMPLETE.
 *
 * <p>Doc: {@code docs/testing/gd3-full-test-matrix-and-seeds.md} § Profile D
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class Gd3EdgeErrorsDataSeeder {

    private final HackathonDevSeedHelper seedHelper;
    private final HackathonRepository hackathonRepository;
    private final RoundRepository roundRepository;
    private final TeamRepository teamRepository;
    private final TrackRepository trackRepository;
    private final DevSeedCleanup devSeedCleanup;

    @Value("${app.seed.gd3.edge-errors.enabled:true}")
    private boolean enabled;

    @Transactional
    public void ensureSeed() {
        if (!enabled) {
            log.info("[Gd3EdgeErrorsDataSeeder] Tắt (app.seed.gd3.edge-errors.enabled=false)");
            return;
        }

        HackathonDevSeedHelper.PrelimState prelimState =
                new HackathonDevSeedHelper.PrelimState(false, true, false, false, 2, 4);
        HackathonDevSeedHelper.HackathonStructure structure = seedHelper.ensureHackathonStructure(
                Gd3EdgeErrorsSeedConstants.SLUG_GD3_EDGE_ERRORS,
                "SEAL GĐ3 — Edge errors",
                HackathonStatus.ONGOING,
                "Seed GĐ3 bad path — round inactive, track1 thiếu judge, INCOMPLETE slide",
                prelimState,
                new HackathonDevSeedHelper.FinalState(false, false),
                seedHelper.computeGd3ActivePrelimDates());

        Hackathon hackathon = structure.hackathon();
        Round prelim = structure.prelim();
        Round finalRound = structure.finalRound();
        Track track1 = structure.track1();
        Track track2 = structure.track2();

        if (needsRepair(hackathon, prelim, finalRound)) {
            repairEdgeState(hackathon, prelim, finalRound, track1);
            prelim = loadPrelim(hackathon.getId());
            track1 = trackRepository.findByRoundIdOrderBySequenceOrderAsc(prelim.getId()).get(0);
            track2 = trackRepository.findByRoundIdOrderBySequenceOrderAsc(prelim.getId()).get(1);
        }

        seedHelper.syncHackathonCalendarFromDates(
                Gd3EdgeErrorsSeedConstants.SLUG_GD3_EDGE_ERRORS,
                seedHelper.computeGd3ActivePrelimDates());
        prelim = loadPrelim(hackathon.getId());

        User coordinator = seedHelper.requireCoordinator();
        Chapter hcm = seedHelper.requireChapter(Gd1SeedConstants.CHAPTER_FPT_HCM);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime submittedAt = prelim.getSubmissionDeadline().minusHours(2);

        String[] teamNames = {
                Gd3EdgeErrorsSeedConstants.TEAM_COMPLETE,
                Gd3EdgeErrorsSeedConstants.TEAM_INCOMPLETE,
                Gd3EdgeErrorsSeedConstants.TEAM_NO_SUBMIT,
                Gd3EdgeErrorsSeedConstants.TEAM_TRACK2
        };

        List<Team> teams = new ArrayList<>();
        for (int i = 0; i < teamNames.length; i++) {
            int idx = i + 1;
            User leader = seedHelper.upsertStudent(
                    Gd3EdgeErrorsSeedConstants.studentEmail(idx),
                    Gd3EdgeErrorsSeedConstants.studentDisplayName(idx),
                    hcm);
            seedHelper.registerStudent(hackathon, leader);
            Team team = seedHelper.ensureActiveTeamForLeader(hackathon, teamNames[i], leader, hcm, now);
            seedHelper.ensureTeamLocked(team, now);
            Track track = idx <= 3 ? track1 : track2;
            String group = idx <= 3 ? "BANG-A" : "BANG-B";
            seedHelper.ensureLottery(hackathon, prelim, track, group, team, coordinator, now);
            teams.add(team);
        }

        seedEdgeSubmissions(hackathon, prelim, track1, track2, teams, submittedAt);
        seedHelper.clearTrackJudgeAssignments(track1);
        seedHelper.clearPresentationQueues(prelim, track1, track2);

        log.info("""
                [Gd3EdgeErrorsDataSeeder] slug={} prelimRoundId={} isActive=false
                  E01 SUBMITTED+slide | E02 INCOMPLETE (repo only) | E03 none | E04 SUBMITTED+slide
                  track1: no judges (activate → JUDGE_NOT_ASSIGNED) | track2: có judge
                  students: {} … {} password={}
                """,
                Gd3EdgeErrorsSeedConstants.SLUG_GD3_EDGE_ERRORS,
                prelim.getId(),
                Gd3EdgeErrorsSeedConstants.studentEmail(1),
                Gd3EdgeErrorsSeedConstants.studentEmail(4),
                DevSeedCatalog.DEV_STUDENT_PASSWORD);
    }

    @Transactional
    public void repairForFeTesting() {
        if (!enabled) {
            return;
        }
        hackathonRepository.findBySlug(Gd3EdgeErrorsSeedConstants.SLUG_GD3_EDGE_ERRORS).ifPresent(h -> {
            seedHelper.syncHackathonCalendarFromDates(
                    Gd3EdgeErrorsSeedConstants.SLUG_GD3_EDGE_ERRORS,
                    seedHelper.computeGd3ActivePrelimDates());
            Round prelim = loadPrelim(h.getId());
            List<Track> tracks = trackRepository.findByRoundIdOrderBySequenceOrderAsc(prelim.getId());
            if (tracks.size() < 2) {
                return;
            }
            repairEdgeState(h, prelim, loadFinal(h.getId()), tracks.get(0));
            reseedIfPresent(h);
        });
    }

    private void reseedIfPresent(Hackathon hackathon) {
        Round prelim = loadPrelim(hackathon.getId());
        List<Track> tracks = trackRepository.findByRoundIdOrderBySequenceOrderAsc(prelim.getId());
        if (tracks.size() < 2) {
            return;
        }
        Track track1 = tracks.get(0);
        Track track2 = tracks.get(1);
        String[] teamNames = {
                Gd3EdgeErrorsSeedConstants.TEAM_COMPLETE,
                Gd3EdgeErrorsSeedConstants.TEAM_INCOMPLETE,
                Gd3EdgeErrorsSeedConstants.TEAM_NO_SUBMIT,
                Gd3EdgeErrorsSeedConstants.TEAM_TRACK2
        };
        List<Team> teams = new ArrayList<>();
        for (String name : teamNames) {
            teamRepository.findByHackathon_IdAndTeamNameIgnoreCase(hackathon.getId(), name)
                    .ifPresent(teams::add);
        }
        if (teams.size() != 4) {
            return;
        }
        LocalDateTime submittedAt = prelim.getSubmissionDeadline().minusHours(2);
        seedEdgeSubmissions(hackathon, prelim, track1, track2, teams, submittedAt);
        seedHelper.clearTrackJudgeAssignments(track1);
        seedHelper.clearPresentationQueues(prelim, track1, track2);
    }

    private void seedEdgeSubmissions(
            Hackathon hackathon,
            Round prelim,
            Track track1,
            Track track2,
            List<Team> teams,
            LocalDateTime submittedAt) {
        for (int i = 0; i < teams.size(); i++) {
            Team team = teams.get(i);
            if (i == 2) {
                seedHelper.clearPrelimSubmission(team, prelim);
                continue;
            }
            Track track = i < 3 ? track1 : track2;
            Submission sub = seedHelper.ensurePrelimSubmission(
                    hackathon, prelim, track, team, SubmissionStatus.SUBMITTED, false, submittedAt);
            seedHelper.clearSubmissionScores(sub.getId());
            if (i == 0 || i == 3) {
                seedHelper.markSubmissionSlideSeeded(sub);
            }
        }
    }

    private void repairEdgeState(Hackathon hackathon, Round prelim, Round finalRound, Track track1) {
        seedHelper.clearPrelimRoundArtifacts(hackathon.getId());
        seedHelper.syncHackathonCalendarFromDates(
                Gd3EdgeErrorsSeedConstants.SLUG_GD3_EDGE_ERRORS,
                seedHelper.computeGd3ActivePrelimDates());
        seedHelper.repairPrelimState(
                prelim,
                new HackathonDevSeedHelper.PrelimState(false, true, false, false, 2, 4));
        seedHelper.repairFinalState(finalRound, new HackathonDevSeedHelper.FinalState(false, false));
        if (hackathon.getStatus() != HackathonStatus.ONGOING) {
            hackathon.setStatus(HackathonStatus.ONGOING);
            hackathonRepository.save(hackathon);
        }
        seedHelper.clearTrackJudgeAssignments(track1);
    }

    @Transactional
    public void resetAndSeed() {
        devSeedCleanup.purgeIfPresent(Gd3EdgeErrorsSeedConstants.SLUG_GD3_EDGE_ERRORS);
        ensureSeed();
    }

    private boolean needsRepair(Hackathon hackathon, Round prelim, Round finalRound) {
        return hackathon.getStatus() != HackathonStatus.ONGOING
                || Boolean.TRUE.equals(prelim.getIsActive())
                || Boolean.TRUE.equals(prelim.getScoringLocked())
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
