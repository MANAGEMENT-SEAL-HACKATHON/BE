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

/**
 * Seed GĐ4 — prelim locked + scored, wildcard disabled → empty candidates.
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class Gd4WildcardDisabledDataSeeder {

    private final HackathonDevSeedHelper seedHelper;
    private final HackathonRepository hackathonRepository;
    private final RoundRepository roundRepository;
    private final TeamRoundTrackRepository teamRoundTrackRepository;
    private final TeamRepository teamRepository;
    private final DevSeedCleanup devSeedCleanup;

    @Value("${app.seed.gd4.wildcard-disabled.enabled:true}")
    private boolean enabled;

    @Transactional
    public void ensureSeed() {
        if (!enabled) {
            log.info("[Gd4WildcardDisabledDataSeeder] Tắt (app.seed.gd4.wildcard-disabled.enabled=false)");
            return;
        }

        HackathonDevSeedHelper.PrelimState prelimState =
                new HackathonDevSeedHelper.PrelimState(false, true, true, false, 1, 6);
        HackathonDevSeedHelper.HackathonStructure structure = seedHelper.ensureHackathonStructure(
                Gd4WildcardDisabledSeedConstants.SLUG_GD4_WILDCARD_DISABLED,
                "SEAL GĐ4 — Wildcard disabled",
                HackathonStatus.ONGOING,
                "Seed GĐ4 — wildcardEnabled=false, GET wildcard-candidates → []",
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
                Gd4WildcardDisabledSeedConstants.SLUG_GD4_WILDCARD_DISABLED,
                seedHelper.computeGd4AdvanceReadyDates());
        prelim = loadPrelim(hackathon.getId());

        User coordinator = seedHelper.requireCoordinator();
        User judge1 = seedHelper.requireJudge1();
        User judge2 = seedHelper.requireJudge2();
        Chapter hcm = seedHelper.requireChapter(Gd1SeedConstants.CHAPTER_FPT_HCM);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime submittedAt = now.minusHours(72);

        for (int i = 0; i < Gd4SeedConstants.TEAM_NAMES.length; i++) {
            int idx = i + 1;
            User leader = seedHelper.upsertStudent(
                    Gd4WildcardDisabledSeedConstants.studentEmail(idx),
                    Gd4WildcardDisabledSeedConstants.studentDisplayName(idx),
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
                    false, submittedAt);
            seedHelper.scoreAllTrackCriteria(sub, track, judge, Gd4SeedConstants.TEAM_SCORES[i], true);
        }

        seedHelper.setWildcardEnabled(hackathon, prelim, false);

        log.info("""
                [Gd4WildcardDisabledDataSeeder] slug={} prelimRoundId={}
                  wildcardEnabled=false — GET /wildcard-candidates → candidates=[]
                """,
                Gd4WildcardDisabledSeedConstants.SLUG_GD4_WILDCARD_DISABLED,
                prelim.getId());
    }

    @Transactional
    public void repairForFeTesting() {
        if (!enabled) {
            return;
        }
        hackathonRepository.findBySlug(Gd4WildcardDisabledSeedConstants.SLUG_GD4_WILDCARD_DISABLED).ifPresent(h -> {
            Round prelim = loadPrelim(h.getId());
            Round finalRound = loadFinal(h.getId());
            if (needsGd4Repair(h, prelim, finalRound)) {
                seedHelper.repairHackathonForGd4Retest(h, prelim, finalRound);
                prelim = loadPrelim(h.getId());
            }
            seedHelper.syncHackathonCalendarFromDates(
                    Gd4WildcardDisabledSeedConstants.SLUG_GD4_WILDCARD_DISABLED,
                    seedHelper.computeGd4AdvanceReadyDates());
            seedHelper.setWildcardEnabled(h, prelim, false);
        });
    }

    @Transactional
    public void resetAndSeed() {
        devSeedCleanup.purgeIfPresent(Gd4WildcardDisabledSeedConstants.SLUG_GD4_WILDCARD_DISABLED);
        ensureSeed();
    }

    private boolean needsGd4Repair(Hackathon hackathon, Round prelim, Round finalRound) {
        if (hackathon.getStatus() != HackathonStatus.ONGOING) {
            return true;
        }
        if (Boolean.TRUE.equals(prelim.getIsPublished()) || Boolean.TRUE.equals(finalRound.getIsActive())) {
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
