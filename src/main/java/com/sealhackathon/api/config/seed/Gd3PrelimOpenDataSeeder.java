package com.sealhackathon.api.config.seed;

import com.sealhackathon.api.chapters.entity.Chapter;
import com.sealhackathon.api.criteria.entity.Criteria;
import com.sealhackathon.api.criteria.repository.CriteriaRepository;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.submissions.entity.Submission;
import com.sealhackathon.api.submissions.repository.SubmissionRepository;
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
 * Seed GĐ3 — hackathon {@link Gd3SeedConstants#SLUG_GD3_PRELIM_OPEN}.
 *
 * <p>Doc: {@code docs/testing/fe-gd3-api-mapping.md} §14
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class Gd3PrelimOpenDataSeeder {

    private final HackathonDevSeedHelper seedHelper;
    private final HackathonRepository hackathonRepository;
    private final RoundRepository roundRepository;
    private final SubmissionRepository submissionRepository;
    private final CriteriaRepository criteriaRepository;
    private final DevSeedCleanup devSeedCleanup;

    @Value("${app.seed.gd3.enabled:true}")
    private boolean enabled;

    @Transactional
    public void ensureSeed() {
        if (!enabled) {
            log.info("[Gd3PrelimOpenDataSeeder] Tắt (app.seed.gd3.enabled=false)");
            return;
        }

        HackathonDevSeedHelper.PrelimState prelimState =
                new HackathonDevSeedHelper.PrelimState(true, true, false, false, 2, 4);
        HackathonDevSeedHelper.HackathonStructure structure = seedHelper.ensureHackathonStructure(
                Gd3SeedConstants.SLUG_GD3_PRELIM_OPEN,
                "SEAL GĐ3 — Prelim open",
                HackathonStatus.ONGOING,
                "Seed FE GĐ3 — sơ loại active, 6 đội, late/queue/scoring",
                prelimState,
                new HackathonDevSeedHelper.FinalState(false, false),
                seedHelper.computeGd3ActivePrelimDates());

        Hackathon hackathon = structure.hackathon();
        Round prelim = structure.prelim();
        Round finalRound = structure.finalRound();
        Track track1 = structure.track1();
        Track track2 = structure.track2();

        if (needsGd3Repair(hackathon, prelim, finalRound)) {
            seedHelper.repairHackathonForGd3Retest(hackathon, prelim, finalRound);
            hackathon = hackathonRepository.findById(hackathon.getId()).orElse(hackathon);
            prelim = loadPrelim(hackathon.getId());
            finalRound = loadFinal(hackathon.getId());
        }

        seedHelper.syncHackathonCalendarFromDates(
                Gd3SeedConstants.SLUG_GD3_PRELIM_OPEN, seedHelper.computeGd3ActivePrelimDates());
        prelim = loadPrelim(hackathon.getId());

        User coordinator = seedHelper.requireCoordinator();
        User judge1 = seedHelper.requireJudge1();
        User judge2 = seedHelper.requireJudge2();
        Chapter hcm = seedHelper.requireChapter(Gd1SeedConstants.CHAPTER_FPT_HCM);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime onTime = now.minusHours(3);
        LocalDateTime lateAt = now.minusHours(1);

        String[] teamNames = {
                Gd3SeedConstants.TEAM_01,
                Gd3SeedConstants.TEAM_02,
                Gd3SeedConstants.TEAM_03,
                Gd3SeedConstants.TEAM_04,
                Gd3SeedConstants.TEAM_05,
                Gd3SeedConstants.TEAM_06
        };
        List<Team> teams = new ArrayList<>();
        for (int i = 0; i < teamNames.length; i++) {
            int idx = i + 1;
            User leader = seedHelper.upsertStudent(
                    Gd3SeedConstants.studentEmail(idx),
                    Gd3SeedConstants.studentDisplayName(idx),
                    hcm);
            seedHelper.registerStudent(hackathon, leader);
            Team team = seedHelper.ensureActiveTeam(hackathon, teamNames[i], leader, hcm, now);
            seedHelper.ensureTeamLocked(team, now);
            Track track = idx <= 3 ? track1 : track2;
            String group = "BANG-" + ((idx - 1) % 2 + 1);
            seedHelper.ensureLottery(hackathon, prelim, track, group, team, coordinator, now);
            teams.add(team);
        }

        Submission sub01 = seedHelper.ensurePrelimSubmission(
                hackathon, prelim, track1, teams.get(0), SubmissionStatus.SUBMITTED, false, onTime);
        Submission sub02 = seedHelper.ensurePrelimSubmission(
                hackathon, prelim, track1, teams.get(1), SubmissionStatus.LATE_PENDING, true, lateAt);
        Submission sub03 = seedHelper.ensurePrelimSubmission(
                hackathon, prelim, track1, teams.get(2), SubmissionStatus.LATE_APPROVED, true, lateAt);
        sub03.setReviewedBy(coordinator);
        sub03.setReviewedAt(now);
        submissionRepository.save(sub03);
        Submission sub05 = seedHelper.ensurePrelimSubmission(
                hackathon, prelim, track2, teams.get(4), SubmissionStatus.SUBMITTED, false, onTime);
        Submission sub06 = seedHelper.ensurePrelimSubmission(
                hackathon, prelim, track2, teams.get(5), SubmissionStatus.SUBMITTED, false, onTime);

        seedHelper.scoreAllTrackCriteria(sub01, track1, judge1, 9.0f, true);
        seedHelper.scoreAllTrackCriteria(sub03, track1, judge1, 8.5f, true);
        seedHelper.scoreAllTrackCriteria(sub05, track2, judge2, 8.8f, true);
        List<Criteria> track2Criteria = criteriaRepository.findByTrackIdOrderByDisplayOrderAsc(track2.getId());
        if (!track2Criteria.isEmpty()) {
            seedHelper.ensureNormalScore(sub06, track2Criteria.get(0), judge2, 7.5f, false);
        }

        seedHelper.seedPresentationQueue(prelim, track1, List.of(sub01, sub03));
        seedHelper.seedPresentationQueue(prelim, track2, List.of(sub05, sub06));

        log.info("""
                [Gd3PrelimOpenDataSeeder] slug={} hackathonId={} prelimRoundId={} track1={} track2={}
                  teams: {} | {} | {} | {} | {} | {}
                  students: {} … {} password={}
                  lateSubmissionId(team02)={} | team04 chưa nộp | slot PRESENTING team01
                """,
                Gd3SeedConstants.SLUG_GD3_PRELIM_OPEN,
                hackathon.getId(),
                prelim.getId(),
                track1.getId(),
                track2.getId(),
                teams.get(0).getId(),
                teams.get(1).getId(),
                teams.get(2).getId(),
                teams.get(3).getId(),
                teams.get(4).getId(),
                teams.get(5).getId(),
                Gd3SeedConstants.studentEmail(1),
                Gd3SeedConstants.studentEmail(6),
                DevSeedCatalog.DEV_STUDENT_PASSWORD,
                sub02.getId());
    }

    @Transactional
    public void repairForFeTesting() {
        if (!enabled) {
            return;
        }
        var maybeHackathon = hackathonRepository.findBySlug(Gd3SeedConstants.SLUG_GD3_PRELIM_OPEN);
        if (maybeHackathon.isEmpty()) {
            return;
        }
        Hackathon hackathon = maybeHackathon.get();
        Round prelim = loadPrelim(hackathon.getId());
        Round finalRound = loadFinal(hackathon.getId());
        if (needsGd3Repair(hackathon, prelim, finalRound)) {
            seedHelper.repairHackathonForGd3Retest(hackathon, prelim, finalRound);
            prelim = loadPrelim(hackathon.getId());
        }
        boolean synced = seedHelper.syncHackathonCalendarFromDates(
                Gd3SeedConstants.SLUG_GD3_PRELIM_OPEN, seedHelper.computeGd3ActivePrelimDates());
        if (synced) {
            log.info(
                    "[Gd3PrelimOpenDataSeeder] FE repair — sync lịch prelim theo giờ máy slug={} deadline={}",
                    Gd3SeedConstants.SLUG_GD3_PRELIM_OPEN,
                    loadPrelim(hackathon.getId()).getSubmissionDeadline());
        }
    }

    @Transactional
    public void resetAndSeed() {
        devSeedCleanup.purgeIfPresent(Gd3SeedConstants.SLUG_GD3_PRELIM_OPEN);
        ensureSeed();
    }

    private boolean needsGd3Repair(Hackathon hackathon, Round prelim, Round finalRound) {
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
