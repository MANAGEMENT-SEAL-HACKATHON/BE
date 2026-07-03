package com.sealhackathon.api.config.seed;

import com.sealhackathon.api.chapters.entity.Chapter;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
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
 * Seed GĐ5 bad path — CK inactive → {@code ROUND_NOT_ACTIVE} khi nộp bài.
 *
 * <p>Doc: {@code docs/testing/gd5-full-test-matrix-and-seeds.md} § Profile D
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class Gd5EdgeErrorsDataSeeder {

    private final HackathonDevSeedHelper seedHelper;
    private final HackathonRepository hackathonRepository;
    private final RoundRepository roundRepository;
    private final TeamRoundTrackRepository teamRoundTrackRepository;
    private final TeamRepository teamRepository;
    private final DevSeedCleanup devSeedCleanup;

    @Value("${app.seed.gd5.edge-errors.enabled:true}")
    private boolean enabled;

    @Transactional
    public void ensureSeed() {
        if (!enabled) {
            log.info("[Gd5EdgeErrorsDataSeeder] Tắt (app.seed.gd5.edge-errors.enabled=false)");
            return;
        }

        HackathonDevSeedHelper.HackathonStructure structure = seedHelper.ensureHackathonStructure(
                Gd5EdgeErrorsSeedConstants.SLUG_GD5_EDGE_ERRORS,
                "SEAL GĐ5 — Edge errors",
                HackathonStatus.ONGOING,
                "Seed GĐ5 — 4 ADVANCED, CK inactive → ROUND_NOT_ACTIVE",
                new HackathonDevSeedHelper.PrelimState(false, true, true, true, 2, 2),
                new HackathonDevSeedHelper.FinalState(false, false),
                seedHelper.computeGd5FinalActiveDates());

        Hackathon hackathon = structure.hackathon();
        Round prelim = structure.prelim();
        Round finalRound = structure.finalRound();
        Track track1 = structure.track1();
        Track track2 = structure.track2();

        if (needsRepair(hackathon, prelim, finalRound)) {
            seedHelper.repairHackathonForGd5EdgeRetest(hackathon, prelim, finalRound);
            hackathon = hackathonRepository.findById(hackathon.getId()).orElse(hackathon);
            prelim = loadPrelim(hackathon.getId());
            finalRound = loadFinal(hackathon.getId());
        }

        seedHelper.syncHackathonCalendarFromDates(
                Gd5EdgeErrorsSeedConstants.SLUG_GD5_EDGE_ERRORS, seedHelper.computeGd5FinalActiveDates());

        User coordinator = seedHelper.requireCoordinator();
        Chapter chapter = seedHelper.requireChapter(Gd1SeedConstants.CHAPTER_FPT_HCM);
        LocalDateTime now = LocalDateTime.now();

        List<Team> teams = new ArrayList<>();
        for (int i = 0; i < Gd5EdgeErrorsSeedConstants.TEAM_NAMES.length; i++) {
            int idx = i + 1;
            User leader = seedHelper.upsertStudent(
                    Gd5EdgeErrorsSeedConstants.studentEmail(idx),
                    Gd5EdgeErrorsSeedConstants.studentDisplayName(idx),
                    chapter);
            seedHelper.registerStudent(hackathon, leader);
            Team team = seedHelper.ensureActiveTeam(
                    hackathon, Gd5EdgeErrorsSeedConstants.TEAM_NAMES[i], leader, chapter, now);
            Track track = (idx % 2 == 1) ? track1 : track2;
            String group = "BANG-" + ((idx % 2) + 1);
            seedHelper.ensureLottery(hackathon, prelim, track, group, team, coordinator, now);
            seedHelper.markAdvanced(team, prelim, finalRound, hackathon);
            teams.add(team);
        }

        log.info("""
                [Gd5EdgeErrorsDataSeeder] slug={} finalRoundId={} is_active=false
                  POST /submissions (roundId=final) → ROUND_NOT_ACTIVE
                  student: {} password={}
                """,
                Gd5EdgeErrorsSeedConstants.SLUG_GD5_EDGE_ERRORS,
                finalRound.getId(),
                Gd5EdgeErrorsSeedConstants.studentEmail(1),
                DevSeedCatalog.DEV_STUDENT_PASSWORD);
    }

    @Transactional
    public void repairForFeTesting() {
        if (!enabled) {
            return;
        }
        hackathonRepository.findBySlug(Gd5EdgeErrorsSeedConstants.SLUG_GD5_EDGE_ERRORS).ifPresent(h -> {
            Round prelim = loadPrelim(h.getId());
            Round finalRound = loadFinal(h.getId());
            if (needsRepair(h, prelim, finalRound)) {
                seedHelper.repairHackathonForGd5EdgeRetest(h, prelim, finalRound);
                reapplyAdvanced(h, prelim, finalRound);
            }
            seedHelper.syncHackathonCalendarFromDates(
                    Gd5EdgeErrorsSeedConstants.SLUG_GD5_EDGE_ERRORS,
                    seedHelper.computeGd5FinalActiveDates());
        });
    }

    private void reapplyAdvanced(Hackathon hackathon, Round prelim, Round finalRound) {
        for (String teamName : Gd5EdgeErrorsSeedConstants.TEAM_NAMES) {
            teamRepository.findByHackathon_IdAndTeamNameIgnoreCase(hackathon.getId(), teamName)
                    .ifPresent(team -> seedHelper.markAdvanced(team, prelim, finalRound, hackathon));
        }
    }

    @Transactional
    public void resetAndSeed() {
        devSeedCleanup.purgeIfPresent(Gd5EdgeErrorsSeedConstants.SLUG_GD5_EDGE_ERRORS);
        ensureSeed();
    }

    private boolean needsRepair(Hackathon hackathon, Round prelim, Round finalRound) {
        if (hackathon.getStatus() != HackathonStatus.ONGOING) {
            return true;
        }
        if (Boolean.TRUE.equals(finalRound.getIsActive())) {
            return true;
        }
        long advancedCount = teamRepository.findByHackathon_Id(hackathon.getId()).stream()
                .flatMap(team -> teamRoundTrackRepository.findByTeam_Id(team.getId()).stream())
                .filter(trt -> trt.getParticipationStatus() == ParticipationStatus.ADVANCED)
                .count();
        return advancedCount != Gd5EdgeErrorsSeedConstants.TEAM_NAMES.length;
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
