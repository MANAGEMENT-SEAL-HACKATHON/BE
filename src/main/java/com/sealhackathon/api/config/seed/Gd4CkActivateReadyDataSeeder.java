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
 * Seed GĐ4 — published + 6 đội ADVANCED, guest judge, CK chưa active.
 *
 * <p>Doc: {@code docs/testing/gd4-full-test-matrix-and-seeds.md} § Profile C
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class Gd4CkActivateReadyDataSeeder {

    private final HackathonDevSeedHelper seedHelper;
    private final HackathonRepository hackathonRepository;
    private final RoundRepository roundRepository;
    private final TeamRoundTrackRepository teamRoundTrackRepository;
    private final TeamRepository teamRepository;
    private final DevSeedCleanup devSeedCleanup;

    @Value("${app.seed.gd4.ck-activate-ready.enabled:true}")
    private boolean enabled;

    @Transactional
    public void ensureSeed() {
        if (!enabled) {
            log.info("[Gd4CkActivateReadyDataSeeder] Tắt (app.seed.gd4.ck-activate-ready.enabled=false)");
            return;
        }

        HackathonDevSeedHelper.PrelimState prelimState =
                new HackathonDevSeedHelper.PrelimState(false, true, true, true, 1, 6);
        HackathonDevSeedHelper.HackathonStructure structure = seedHelper.ensureHackathonStructure(
                Gd4CkActivateReadySeedConstants.SLUG_GD4_CK_ACTIVATE_READY,
                "SEAL GĐ4 — CK activate ready",
                HackathonStatus.ONGOING,
                "Seed GĐ4 — published + 6 ADVANCED + guest judge, CK inactive",
                prelimState,
                new HackathonDevSeedHelper.FinalState(false, false),
                seedHelper.computeGd4AdvanceReadyDates());

        Hackathon hackathon = structure.hackathon();
        Round prelim = structure.prelim();
        Round finalRound = structure.finalRound();
        Track track1 = structure.track1();
        Track track2 = structure.track2();

        if (needsRepair(hackathon, prelim, finalRound)) {
            seedHelper.repairHackathonForGd4CkActivateRetest(hackathon, prelim, finalRound);
            hackathon = hackathonRepository.findById(hackathon.getId()).orElse(hackathon);
            prelim = loadPrelim(hackathon.getId());
            finalRound = loadFinal(hackathon.getId());
        }

        seedHelper.syncHackathonCalendarFromDates(
                Gd4CkActivateReadySeedConstants.SLUG_GD4_CK_ACTIVATE_READY,
                seedHelper.computeGd4AdvanceReadyDates());

        User coordinator = seedHelper.requireCoordinator();
        User judge1 = seedHelper.requireJudge1();
        User judge2 = seedHelper.requireJudge2();
        Chapter hcm = seedHelper.requireChapter(Gd1SeedConstants.CHAPTER_FPT_HCM);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime submittedAt = now.minusHours(72);

        List<Team> teams = new ArrayList<>();
        for (int i = 0; i < Gd4CkActivateReadySeedConstants.TEAM_NAMES.length; i++) {
            int idx = i + 1;
            User leader = seedHelper.upsertStudent(
                    Gd4CkActivateReadySeedConstants.studentEmail(idx),
                    Gd4CkActivateReadySeedConstants.studentDisplayName(idx),
                    hcm);
            seedHelper.registerStudent(hackathon, leader);
            Team team = seedHelper.ensureActiveTeam(
                    hackathon, Gd4CkActivateReadySeedConstants.TEAM_NAMES[i], leader, hcm, now);
            seedHelper.ensureTeamLocked(team, now);
            Track track = idx <= 4 ? track1 : track2;
            User judge = idx <= 4 ? judge1 : judge2;
            seedHelper.ensureLottery(
                    hackathon, prelim, track, Gd4CkActivateReadySeedConstants.GROUPS[i], team, coordinator, now);
            Submission sub = seedHelper.ensurePrelimSubmission(
                    hackathon, prelim, track, team,
                    com.sealhackathon.api.submissions.value_object.SubmissionStatus.SUBMITTED,
                    false, submittedAt);
            seedHelper.scoreAllTrackCriteria(
                    sub, track, judge, Gd4CkActivateReadySeedConstants.TEAM_SCORES[i], true);
            teams.add(team);
        }

        for (int advancedIndex : Gd4CkActivateReadySeedConstants.ADVANCED_TEAM_INDICES) {
            seedHelper.markAdvanced(teams.get(advancedIndex), prelim, finalRound, hackathon);
        }

        seedHelper.ensureFinalGuestJudgeAssignment(hackathon, finalRound);

        log.info("""
                [Gd4CkActivateReadyDataSeeder] slug={} prelimRoundId={} finalRoundId={}
                  6 ADVANCED — PATCH /rounds/{finalId}/activate sẵn sàng
                  eliminated: {} | {}
                """,
                Gd4CkActivateReadySeedConstants.SLUG_GD4_CK_ACTIVATE_READY,
                prelim.getId(),
                finalRound.getId(),
                teams.get(3).getId(),
                teams.get(7).getId());
    }

    @Transactional
    public void repairForFeTesting() {
        if (!enabled) {
            return;
        }
        hackathonRepository.findBySlug(Gd4CkActivateReadySeedConstants.SLUG_GD4_CK_ACTIVATE_READY).ifPresent(h -> {
            Round prelim = loadPrelim(h.getId());
            Round finalRound = loadFinal(h.getId());
            if (needsRepair(h, prelim, finalRound)) {
                seedHelper.repairHackathonForGd4CkActivateRetest(h, prelim, finalRound);
                reapplyAdvancedTeams(h, prelim, finalRound);
            }
            seedHelper.syncHackathonCalendarFromDates(
                    Gd4CkActivateReadySeedConstants.SLUG_GD4_CK_ACTIVATE_READY,
                    seedHelper.computeGd4AdvanceReadyDates());
        });
    }

    private void reapplyAdvancedTeams(Hackathon hackathon, Round prelim, Round finalRound) {
        for (int advancedIndex : Gd4CkActivateReadySeedConstants.ADVANCED_TEAM_INDICES) {
            String teamName = Gd4CkActivateReadySeedConstants.TEAM_NAMES[advancedIndex];
            teamRepository.findByHackathon_IdAndTeamNameIgnoreCase(hackathon.getId(), teamName)
                    .ifPresent(team -> seedHelper.markAdvanced(team, prelim, finalRound, hackathon));
        }
        seedHelper.ensureFinalGuestJudgeAssignment(hackathon, finalRound);
    }

    @Transactional
    public void resetAndSeed() {
        devSeedCleanup.purgeIfPresent(Gd4CkActivateReadySeedConstants.SLUG_GD4_CK_ACTIVATE_READY);
        ensureSeed();
    }

    private boolean needsRepair(Hackathon hackathon, Round prelim, Round finalRound) {
        if (hackathon.getStatus() != HackathonStatus.ONGOING) {
            return true;
        }
        if (!Boolean.TRUE.equals(prelim.getIsPublished()) || Boolean.TRUE.equals(finalRound.getIsActive())) {
            return true;
        }
        long advancedCount = teamRepository.findByHackathon_Id(hackathon.getId()).stream()
                .flatMap(team -> teamRoundTrackRepository.findByTeam_Id(team.getId()).stream())
                .filter(trt -> trt.getParticipationStatus() == ParticipationStatus.ADVANCED)
                .count();
        return advancedCount != Gd4CkActivateReadySeedConstants.ADVANCED_TEAM_INDICES.length;
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
