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
import com.sealhackathon.api.wildcard_reviews.repository.WildcardReviewRepository;
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
 * Seed GĐ4 — wildcard đã resolve (2 approve + 2 reject), published, sẵn sàng advance.
 *
 * <p>Doc: {@code docs/testing/gd4-full-test-matrix-and-seeds.md} § Profile E
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class Gd4WildcardResolvedDataSeeder {

    private final HackathonDevSeedHelper seedHelper;
    private final HackathonRepository hackathonRepository;
    private final RoundRepository roundRepository;
    private final TeamRoundTrackRepository teamRoundTrackRepository;
    private final TeamRepository teamRepository;
    private final WildcardReviewRepository wildcardReviewRepository;
    private final DevSeedCleanup devSeedCleanup;

    @Value("${app.seed.gd4.wildcard-resolved.enabled:true}")
    private boolean enabled;

    @Transactional
    public void ensureSeed() {
        if (!enabled) {
            log.info("[Gd4WildcardResolvedDataSeeder] Tắt (app.seed.gd4.wildcard-resolved.enabled=false)");
            return;
        }

        HackathonDevSeedHelper.PrelimState prelimState =
                new HackathonDevSeedHelper.PrelimState(false, true, true, true, 1, 6);
        HackathonDevSeedHelper.HackathonStructure structure = seedHelper.ensureHackathonStructure(
                Gd4WildcardResolvedSeedConstants.SLUG_GD4_WILDCARD_RESOLVED,
                "SEAL GĐ4 — Wildcard resolved",
                HackathonStatus.ONGOING,
                "Seed GĐ4 — wildcard 2 approve + 2 reject, published, POST /advance ngay",
                prelimState,
                new HackathonDevSeedHelper.FinalState(false, false),
                seedHelper.computeGd4AdvanceReadyDates());

        Hackathon hackathon = structure.hackathon();
        Round prelim = structure.prelim();
        Round finalRound = structure.finalRound();
        Track track1 = structure.track1();
        Track track2 = structure.track2();

        if (needsRepair(hackathon, prelim, finalRound)) {
            seedHelper.repairHackathonForGd4WildcardResolvedRetest(hackathon, prelim, finalRound);
            hackathon = hackathonRepository.findById(hackathon.getId()).orElse(hackathon);
            prelim = loadPrelim(hackathon.getId());
            finalRound = loadFinal(hackathon.getId());
        }

        seedHelper.syncHackathonCalendarFromDates(
                Gd4WildcardResolvedSeedConstants.SLUG_GD4_WILDCARD_RESOLVED,
                seedHelper.computeGd4AdvanceReadyDates());

        User coordinator = seedHelper.requireCoordinator();
        User judge1 = seedHelper.requireJudge1();
        User judge2 = seedHelper.requireJudge2();
        Chapter hcm = seedHelper.requireChapter(Gd1SeedConstants.CHAPTER_FPT_HCM);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime submittedAt = now.minusHours(72);

        List<Team> teams = new ArrayList<>();
        for (int i = 0; i < Gd4WildcardResolvedSeedConstants.TEAM_NAMES.length; i++) {
            int idx = i + 1;
            User leader = seedHelper.upsertStudent(
                    Gd4WildcardResolvedSeedConstants.studentEmail(idx),
                    Gd4WildcardResolvedSeedConstants.studentDisplayName(idx),
                    hcm);
            seedHelper.registerStudent(hackathon, leader);
            Team team = seedHelper.ensureActiveTeam(
                    hackathon, Gd4WildcardResolvedSeedConstants.TEAM_NAMES[i], leader, hcm, now);
            seedHelper.ensureTeamLocked(team, now);
            Track track = idx <= 4 ? track1 : track2;
            User judge = idx <= 4 ? judge1 : judge2;
            seedHelper.ensureLottery(
                    hackathon, prelim, track, Gd4WildcardResolvedSeedConstants.GROUPS[i], team, coordinator, now);
            Submission sub = seedHelper.ensurePrelimSubmission(
                    hackathon, prelim, track, team,
                    com.sealhackathon.api.submissions.value_object.SubmissionStatus.SUBMITTED,
                    false, submittedAt);
            seedHelper.scoreAllTrackCriteria(
                    sub, track, judge, Gd4WildcardResolvedSeedConstants.TEAM_SCORES[i], true);
            teams.add(team);
        }

        seedWildcardDecisions(prelim, track1, track2, teams, coordinator);

        log.info("""
                [Gd4WildcardResolvedDataSeeder] slug={} prelimRoundId={}
                  Wildcard: W06+W08 approved, W04 rejected, W02 auto-rejected
                  POST /advance → 6 ADVANCED (4 top1 + W06 + W08)
                  students: {} … {} password={}
                """,
                Gd4WildcardResolvedSeedConstants.SLUG_GD4_WILDCARD_RESOLVED,
                prelim.getId(),
                Gd4WildcardResolvedSeedConstants.studentEmail(1),
                Gd4WildcardResolvedSeedConstants.studentEmail(8),
                DevSeedCatalog.DEV_STUDENT_PASSWORD);
    }

    private void seedWildcardDecisions(Round prelim, Track track1, Track track2, List<Team> teams, User coordinator) {
        seedHelper.ensureWildcardReviewDecision(
                prelim, teams.get(5), track2, 7.0f, true, coordinator, "Seed — duyệt vé vớt W06");
        seedHelper.ensureWildcardReviewDecision(
                prelim, teams.get(7), track2, 7.0f, true, coordinator, "Seed — duyệt vé vớt W08");
        seedHelper.ensureWildcardReviewDecision(
                prelim, teams.get(3), track1, 7.0f, false, coordinator, "Seed — từ chối vé vớt W04");
        seedHelper.ensureWildcardReviewDecision(
                prelim, teams.get(1), track1, 7.0f, false, coordinator, "Tự động từ chối — đủ suất vé vớt");
    }

    @Transactional
    public void repairForFeTesting() {
        if (!enabled) {
            return;
        }
        hackathonRepository.findBySlug(Gd4WildcardResolvedSeedConstants.SLUG_GD4_WILDCARD_RESOLVED).ifPresent(h -> {
            Round prelim = loadPrelim(h.getId());
            Round finalRound = loadFinal(h.getId());
            if (needsRepair(h, prelim, finalRound)) {
                seedHelper.repairHackathonForGd4WildcardResolvedRetest(h, prelim, finalRound);
            }
            seedHelper.syncHackathonCalendarFromDates(
                    Gd4WildcardResolvedSeedConstants.SLUG_GD4_WILDCARD_RESOLVED,
                    seedHelper.computeGd4AdvanceReadyDates());
            if (wildcardReviewRepository.findByRound_Id(prelim.getId()).size() < 4) {
                ensureSeed();
            }
        });
    }

    @Transactional
    public void resetAndSeed() {
        devSeedCleanup.purgeIfPresent(Gd4WildcardResolvedSeedConstants.SLUG_GD4_WILDCARD_RESOLVED);
        ensureSeed();
    }

    private boolean needsRepair(Hackathon hackathon, Round prelim, Round finalRound) {
        if (hackathon.getStatus() != HackathonStatus.ONGOING) {
            return true;
        }
        if (!Boolean.TRUE.equals(prelim.getIsPublished()) || Boolean.TRUE.equals(finalRound.getIsActive())) {
            return true;
        }
        if (wildcardReviewRepository.findByRound_Id(prelim.getId()).size() < 4) {
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
