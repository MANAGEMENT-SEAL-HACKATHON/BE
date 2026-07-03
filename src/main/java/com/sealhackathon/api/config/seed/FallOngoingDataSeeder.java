package com.sealhackathon.api.config.seed;

import com.sealhackathon.api.chapters.entity.Chapter;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.hackathons.value_object.Season;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.teams.entity.Team;
import com.sealhackathon.api.teams.repository.TeamRepository;
import com.sealhackathon.api.teams.value_object.TeamMemberRole;
import com.sealhackathon.api.teams.value_object.TeamMemberStatus;
import com.sealhackathon.api.teams.value_object.TeamStatus;
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
 * Fall ONGOING — đăng ký còn mở, prelim chưa active, đội chưa lottery → FR-U-15-F.
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class FallOngoingDataSeeder {

    private final HackathonDevSeedHelper seedHelper;
    private final HackathonRepository hackathonRepository;
    private final TeamRepository teamRepository;
    private final DevSeedCleanup devSeedCleanup;

    @Value("${app.seed.fall-ongoing.enabled:true}")
    private boolean enabled;

    @Transactional
    public void ensureSeed() {
        if (!enabled) {
            log.info("[FallOngoingDataSeeder] Tắt");
            return;
        }

        HackathonDevSeedHelper.SeedDates dates = seedHelper.computeGd2RegistrationOpenDates();
        HackathonDevSeedHelper.HackathonStructure structure = seedHelper.ensureHackathonStructure(
                FallOngoingSeedConstants.SLUG_FALL_ONGOING,
                "SEAL Fall 2026 — Track select",
                HackathonStatus.ONGOING,
                "Fall ONGOING, registration open, prelim inactive — leader chọn track (FR-U-15-F)",
                new HackathonDevSeedHelper.PrelimState(false, false, false, false, 2, 4),
                new HackathonDevSeedHelper.FinalState(false, false),
                dates);

        Hackathon hackathon = structure.hackathon();
        hackathon.setSeason(Season.Fall);
        hackathon.setYear(2026);
        hackathon.setIndividualRankingEnabled(true);
        hackathonRepository.save(hackathon);
        seedHelper.syncHackathonCalendarFromDates(FallOngoingSeedConstants.SLUG_FALL_ONGOING, dates);

        Round prelim = structure.prelim();
        Track track1 = structure.track1();
        Chapter hcm = seedHelper.requireChapter(Gd1SeedConstants.CHAPTER_FPT_HCM);
        LocalDateTime now = LocalDateTime.now();

        for (int i = 1; i <= 3; i++) {
            User leader = seedHelper.upsertStudent(
                    FallOngoingSeedConstants.studentEmail(i),
                    "Fall Leader %d".formatted(i),
                    hcm);
            seedHelper.registerStudent(hackathon, leader);
            Team team = seedHelper.ensureActiveTeam(
                    hackathon, FallOngoingSeedConstants.teamName(i), leader, hcm, now);
            if (team.getStatus() != TeamStatus.ACTIVE) {
                team.setStatus(TeamStatus.ACTIVE);
                team.setIsLocked(false);
                team = teamRepository.save(team);
            }
            seedHelper.ensureTeamMember(team, leader, TeamMemberRole.LEADER, TeamMemberStatus.ACCEPTED, now);
            seedHelper.clearTeamRoundTracks(team.getId());
        }

        log.info("[FallOngoingDataSeeder] slug={} hackathonId={} trackId={} prelimId={}",
                FallOngoingSeedConstants.SLUG_FALL_ONGOING,
                hackathon.getId(),
                track1.getId(),
                prelim.getId());
    }

    @Transactional
    public void repairForFeTesting() {
        if (!enabled) {
            return;
        }
        hackathonRepository.findBySlug(FallOngoingSeedConstants.SLUG_FALL_ONGOING).ifPresent(h -> {
            HackathonDevSeedHelper.SeedDates dates = seedHelper.computeGd2RegistrationOpenDates();
            seedHelper.syncHackathonCalendarFromDates(FallOngoingSeedConstants.SLUG_FALL_ONGOING, dates);
            h.setSeason(Season.Fall);
            h.setYear(2026);
            hackathonRepository.save(h);
            teamRepository.findByHackathon_Id(h.getId()).stream()
                    .filter(t -> t.getStatus() == TeamStatus.PENDING || t.getStatus() == TeamStatus.ACTIVE)
                    .forEach(team -> {
                        team.setStatus(TeamStatus.ACTIVE);
                        team.setIsLocked(false);
                        team.setLockedAt(null);
                        team.setFormationSubmittedAt(null);
                        teamRepository.save(team);
                        seedHelper.clearTeamRoundTracks(team.getId());
                    });
        });
    }

    @Transactional
    public void resetAndSeed() {
        devSeedCleanup.purgeIfPresent(FallOngoingSeedConstants.SLUG_FALL_ONGOING);
        ensureSeed();
    }
}
