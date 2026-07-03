package com.sealhackathon.api.config.seed;

import com.sealhackathon.api.chapters.entity.Chapter;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.rounds.value_object.LateSubmissionPolicy;
import com.sealhackathon.api.rounds.value_object.RoundType;
import com.sealhackathon.api.rounds.value_object.TiebreakRule;
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

/**
 * GĐ3 — ≥2 vòng có mentor gán theo đội → FR-13C TeamMentorHistoryPanel.
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class Gd3TeamMentorHistoryDataSeeder {

    private final HackathonDevSeedHelper seedHelper;
    private final HackathonRepository hackathonRepository;
    private final RoundRepository roundRepository;
    private final DevSeedCleanup devSeedCleanup;

    @Value("${app.seed.gd3.team-mentor-history.enabled:true}")
    private boolean enabled;

    @Transactional
    public void ensureSeed() {
        if (!enabled) {
            log.info("[Gd3TeamMentorHistoryDataSeeder] Tắt");
            return;
        }

        HackathonDevSeedHelper.SeedDates dates = seedHelper.computeGd3ActivePrelimDates();
        HackathonDevSeedHelper.HackathonStructure structure = seedHelper.ensureHackathonStructure(
                Gd3TeamMentorHistorySeedConstants.SLUG_GD3_TEAM_MENTOR_HISTORY,
                "SEAL GĐ3 — Team mentor history",
                HackathonStatus.ONGOING,
                "Prelim + semifinal mentor assignments — FR-13C",
                new HackathonDevSeedHelper.PrelimState(true, true, false, false, 2, 4),
                new HackathonDevSeedHelper.FinalState(false, false),
                dates);

        Hackathon hackathon = structure.hackathon();
        Round prelim = structure.prelim();
        Round finalRound = structure.finalRound();
        Track track1 = structure.track1();
        seedHelper.syncHackathonCalendarFromDates(
                Gd3TeamMentorHistorySeedConstants.SLUG_GD3_TEAM_MENTOR_HISTORY, dates);

        Round semifinal = ensureSemifinalRound(hackathon, prelim, finalRound);

        User coordinator = seedHelper.requireCoordinator();
        User mentor = seedHelper.requireMentor();
        Chapter hcm = seedHelper.requireChapter(Gd1SeedConstants.CHAPTER_FPT_HCM);
        LocalDateTime now = LocalDateTime.now();

        User leader = seedHelper.upsertStudent(
                Gd3TeamMentorHistorySeedConstants.studentEmail(),
                "GD3 MH Leader",
                hcm);
        seedHelper.registerStudent(hackathon, leader);
        Team team = seedHelper.ensureActiveTeamForLeader(
                hackathon, Gd3TeamMentorHistorySeedConstants.teamName(), leader, hcm, now);
        seedHelper.ensureTeamLocked(team, now);
        seedHelper.ensureLottery(hackathon, prelim, track1, "MH-A", team, coordinator, now);
        seedHelper.ensureMentorTeamAssignment(hackathon, prelim, team, mentor, coordinator, now);
        seedHelper.ensureMentorTeamAssignment(hackathon, semifinal, team, mentor, coordinator, now);

        log.info("[Gd3TeamMentorHistoryDataSeeder] slug={} hackathonId={} teamId={} rounds={}/{}",
                Gd3TeamMentorHistorySeedConstants.SLUG_GD3_TEAM_MENTOR_HISTORY,
                hackathon.getId(),
                team.getId(),
                prelim.getId(),
                semifinal.getId());
    }

    private Round ensureSemifinalRound(Hackathon hackathon, Round prelim, Round finalRound) {
        return roundRepository.findByHackathon_IdOrderByExamAtAsc(hackathon.getId()).stream()
                .filter(r -> r.getRoundType() == RoundType.SEMIFINAL)
                .findFirst()
                .orElseGet(() -> roundRepository.save(Round.builder()
                        .hackathon(hackathon)
                        .name("Vòng Bán kết")
                        .examAt(prelim.getExamAt().plusDays(1))
                        .isFinal(false)
                        .roundType(RoundType.SEMIFINAL)
                        .submissionOpen(prelim.getSubmissionDeadline().plusHours(1))
                        .submissionDeadline(finalRound.getSubmissionDeadline().minusHours(2))
                        .lateSubmissionPolicy(LateSubmissionPolicy.ALLOW_LATE_PENDING)
                        .wildcardEnabled(false)
                        .tiebreakRule(TiebreakRule.PENALTY_SCORE)
                        .isActive(false)
                        .scoringLocked(false)
                        .isPublished(false)
                        .build()));
    }

    @Transactional
    public void repairForFeTesting() {
        if (!enabled) {
            return;
        }
        hackathonRepository.findBySlug(Gd3TeamMentorHistorySeedConstants.SLUG_GD3_TEAM_MENTOR_HISTORY)
                .ifPresent(h -> seedHelper.syncHackathonCalendarFromDates(
                        Gd3TeamMentorHistorySeedConstants.SLUG_GD3_TEAM_MENTOR_HISTORY,
                        seedHelper.computeGd3ActivePrelimDates()));
    }

    @Transactional
    public void resetAndSeed() {
        devSeedCleanup.purgeIfPresent(Gd3TeamMentorHistorySeedConstants.SLUG_GD3_TEAM_MENTOR_HISTORY);
        ensureSeed();
    }
}
