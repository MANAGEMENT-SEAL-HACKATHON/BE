package com.sealhackathon.api.config.seed;

import com.sealhackathon.api.chapters.entity.Chapter;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.teams.entity.Team;
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
import java.util.List;

/**
 * Seed GĐ3 — prelim inactive, track1 weight ≠ 1, track2 không criteria.
 *
 * <p>PATCH activate → {@code ROUND_WEIGHT_NOT_ONE} hoặc {@code ROUND_NO_CRITERIA}
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class Gd3RoundConfigEdgeDataSeeder {

    private final HackathonDevSeedHelper seedHelper;
    private final HackathonRepository hackathonRepository;
    private final RoundRepository roundRepository;
    private final TrackRepository trackRepository;
    private final DevSeedCleanup devSeedCleanup;

    @Value("${app.seed.gd3.round-config-edge.enabled:true}")
    private boolean enabled;

    @Transactional
    public void ensureSeed() {
        if (!enabled) {
            log.info("[Gd3RoundConfigEdgeDataSeeder] Tắt (app.seed.gd3.round-config-edge.enabled=false)");
            return;
        }

        HackathonDevSeedHelper.PrelimState prelimState =
                new HackathonDevSeedHelper.PrelimState(false, true, false, false, 2, 4);
        HackathonDevSeedHelper.HackathonStructure structure = seedHelper.ensureHackathonStructure(
                Gd3RoundConfigEdgeSeedConstants.SLUG_GD3_ROUND_CONFIG_EDGE,
                "SEAL GĐ3 — Round config edge",
                HackathonStatus.ONGOING,
                "Seed GĐ3 — track1 weight ≠ 1, track2 no criteria → activate fail",
                prelimState,
                new HackathonDevSeedHelper.FinalState(false, false),
                seedHelper.computeGd3ActivePrelimDates());

        Hackathon hackathon = structure.hackathon();
        Round prelim = structure.prelim();
        Round finalRound = structure.finalRound();
        Track track1 = structure.track1();
        Track track2 = structure.track2();

        if (needsRepair(hackathon, prelim, finalRound)) {
            repairConfigState(hackathon, prelim, finalRound);
            prelim = loadPrelim(hackathon.getId());
            List<Track> tracks = trackRepository.findByRoundIdOrderBySequenceOrderAsc(prelim.getId());
            track1 = tracks.get(0);
            track2 = tracks.get(1);
        }

        seedHelper.syncHackathonCalendarFromDates(
                Gd3RoundConfigEdgeSeedConstants.SLUG_GD3_ROUND_CONFIG_EDGE,
                seedHelper.computeGd3ActivePrelimDates());
        prelim = loadPrelim(hackathon.getId());
        track1 = trackRepository.findByRoundIdOrderBySequenceOrderAsc(prelim.getId()).get(0);
        track2 = trackRepository.findByRoundIdOrderBySequenceOrderAsc(prelim.getId()).get(1);

        User coordinator = seedHelper.requireCoordinator();
        Chapter hcm = seedHelper.requireChapter(Gd1SeedConstants.CHAPTER_FPT_HCM);
        LocalDateTime now = LocalDateTime.now();

        for (int i = 0; i < 2; i++) {
            int idx = i + 1;
            User leader = seedHelper.upsertStudent(
                    Gd3RoundConfigEdgeSeedConstants.studentEmail(idx),
                    Gd3RoundConfigEdgeSeedConstants.studentDisplayName(idx),
                    hcm);
            seedHelper.registerStudent(hackathon, leader);
            String teamName = idx == 1
                    ? Gd3RoundConfigEdgeSeedConstants.TEAM_TRACK1
                    : Gd3RoundConfigEdgeSeedConstants.TEAM_TRACK2;
            Team team = seedHelper.ensureActiveTeamForLeader(hackathon, teamName, leader, hcm, now);
            seedHelper.ensureTeamLocked(team, now);
            Track track = idx == 1 ? track1 : track2;
            seedHelper.ensureLottery(hackathon, prelim, track, "BANG-A", team, coordinator, now);
        }

        applyBadRoundConfig(track1, track2);
        seedHelper.repairPrelimState(prelim, new HackathonDevSeedHelper.PrelimState(false, true, false, false, 2, 4));

        log.info("""
                [Gd3RoundConfigEdgeDataSeeder] slug={} prelimRoundId={} isActive=false
                  track1: weight sum ≠ 1 | track2: no criteria
                  PATCH activate → ROUND_WEIGHT_NOT_ONE / ROUND_NO_CRITERIA
                """,
                Gd3RoundConfigEdgeSeedConstants.SLUG_GD3_ROUND_CONFIG_EDGE,
                prelim.getId());
    }

    @Transactional
    public void repairForFeTesting() {
        if (!enabled) {
            return;
        }
        hackathonRepository.findBySlug(Gd3RoundConfigEdgeSeedConstants.SLUG_GD3_ROUND_CONFIG_EDGE).ifPresent(h -> {
            seedHelper.syncHackathonCalendarFromDates(
                    Gd3RoundConfigEdgeSeedConstants.SLUG_GD3_ROUND_CONFIG_EDGE,
                    seedHelper.computeGd3ActivePrelimDates());
            Round prelim = loadPrelim(h.getId());
            Round finalRound = loadFinal(h.getId());
            if (needsRepair(h, prelim, finalRound)) {
                repairConfigState(h, prelim, finalRound);
            }
            List<Track> tracks = trackRepository.findByRoundIdOrderBySequenceOrderAsc(prelim.getId());
            if (tracks.size() >= 2) {
                applyBadRoundConfig(tracks.get(0), tracks.get(1));
            }
            seedHelper.repairPrelimState(prelim, new HackathonDevSeedHelper.PrelimState(false, true, false, false, 2, 4));
        });
    }

    private void applyBadRoundConfig(Track track1, Track track2) {
        seedHelper.setTrackCriteriaWeight(track1, "Domain Accuracy", 0.10f);
        seedHelper.clearTrackCriteria(track2);
    }

    private void repairConfigState(Hackathon hackathon, Round prelim, Round finalRound) {
        seedHelper.clearPrelimRoundArtifacts(hackathon.getId());
        seedHelper.repairPrelimState(
                prelim,
                new HackathonDevSeedHelper.PrelimState(false, true, false, false, 2, 4));
        seedHelper.repairFinalState(finalRound, new HackathonDevSeedHelper.FinalState(false, false));
        if (hackathon.getStatus() != HackathonStatus.ONGOING) {
            hackathon.setStatus(HackathonStatus.ONGOING);
            hackathonRepository.save(hackathon);
        }
        List<Track> tracks = trackRepository.findByRoundIdOrderBySequenceOrderAsc(prelim.getId());
        if (tracks.size() >= 2) {
            seedHelper.clearTrackCriteria(tracks.get(0));
            seedHelper.clearTrackCriteria(tracks.get(1));
            seedHelper.ensureDefaultTrackCriteria(tracks.get(0));
            seedHelper.ensureDefaultTrackCriteria(tracks.get(1));
        }
    }

    @Transactional
    public void resetAndSeed() {
        devSeedCleanup.purgeIfPresent(Gd3RoundConfigEdgeSeedConstants.SLUG_GD3_ROUND_CONFIG_EDGE);
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
