package com.sealhackathon.api.config.seed;

import com.sealhackathon.api.chapters.entity.Chapter;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.teams.entity.Team;
import com.sealhackathon.api.teams.repository.TeamRepository;
import com.sealhackathon.api.teams.value_object.TeamStatus;
import com.sealhackathon.api.teams.value_object.TeamMemberRole;
import com.sealhackathon.api.teams.value_object.TeamMemberStatus;
import com.sealhackathon.api.tracks.entity.Track;
import com.sealhackathon.api.users.entity.User;
import com.sealhackathon.api.users.value_object.UserType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * GĐ2 negative matrix — 9 đội đa trạng thái trên slug riêng (không đụng seal-e2e-2026).
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class Gd2TeamsEdgeDataSeeder {

    private final HackathonDevSeedHelper seedHelper;
    private final HackathonRepository hackathonRepository;
    private final RoundRepository roundRepository;
    private final TeamRepository teamRepository;
    private final DevSeedCleanup devSeedCleanup;

    @Value("${app.seed.gd2.teams-edge.enabled:true}")
    private boolean enabled;

    @Transactional
    public void ensureSeed() {
        if (!enabled) {
            log.info("[Gd2TeamsEdgeDataSeeder] Tắt (app.seed.gd2.teams-edge.enabled=false)");
            return;
        }

        HackathonDevSeedHelper.SeedDates dates = seedHelper.computeGd2RegistrationOpenDates();
        HackathonDevSeedHelper.HackathonStructure structure = seedHelper.ensureHackathonStructure(
                Gd2TeamsEdgeSeedConstants.SLUG_GD2_TEAMS_EDGE,
                "SEAL GĐ2 — Teams edge",
                HackathonStatus.ONGOING,
                "Seed GĐ2 — PENDING/REJECTED/ELIMINATED/locked/mentor matrix",
                new HackathonDevSeedHelper.PrelimState(false, false, false, false, 2, 4),
                new HackathonDevSeedHelper.FinalState(false, false),
                dates);

        Hackathon hackathon = structure.hackathon();
        Round prelim = structure.prelim();
        Track track1 = structure.track1();
        Track track2 = structure.track2();
        seedHelper.syncHackathonCalendarFromDates(Gd2TeamsEdgeSeedConstants.SLUG_GD2_TEAMS_EDGE, dates);

        if (needsRepair(hackathon)) {
            seedTeams(hackathon, prelim, track1, track2);
        } else {
            seedTeams(hackathon, prelim, track1, track2);
        }

        log.info("""
                [Gd2TeamsEdgeDataSeeder] slug={} hackathonId={}
                  9 teams — PENDING/ACTIVE/REJECTED/ELIMINATED + locked + mentor
                  coord: /teams → tab Chờ duyệt / Đã duyệt
                """,
                Gd2TeamsEdgeSeedConstants.SLUG_GD2_TEAMS_EDGE,
                hackathon.getId());
    }

    private void seedTeams(Hackathon hackathon, Round prelim, Track track1, Track track2) {
        User coordinator = seedHelper.requireCoordinator();
        User mentor = seedHelper.requireMentor();
        Chapter hcm = seedHelper.requireChapter(Gd1SeedConstants.CHAPTER_FPT_HCM);
        Chapter hn = seedHelper.requireChapter(Gd1SeedConstants.CHAPTER_FPT_HN);
        Chapter ext = seedHelper.requireChapter(Gd1SeedConstants.CHAPTER_EXT);
        LocalDateTime now = LocalDateTime.now();

        // T01 — PENDING 1 member
        User l01 = upsert(Gd2TeamsEdgeSeedConstants.studentEmail("hcm.leader01"), "GD2 Leader 01", hcm);
        seedHelper.registerStudent(hackathon, l01);
        Team t01 = seedHelper.ensureTeam(hackathon, Gd2TeamsEdgeSeedConstants.TEAM_01, l01, hcm,
                TeamStatus.PENDING, false, now);
        seedHelper.ensureTeamMember(t01, l01, TeamMemberRole.LEADER, TeamMemberStatus.ACCEPTED, now);

        // T02 — PENDING 2 ACCEPTED + 1 PENDING invite
        User l02 = upsert(Gd2TeamsEdgeSeedConstants.studentEmail("hn.leader02"), "GD2 Leader 02", hn);
        User m02a = upsert(Gd2TeamsEdgeSeedConstants.studentEmail("hcm.member03"), "GD2 M02a", hcm);
        User m02b = upsert(Gd2TeamsEdgeSeedConstants.studentEmail("hcm.member04"), "GD2 M02b", hcm);
        User m02p = upsertExt(Gd2TeamsEdgeSeedConstants.extEmail("pending"), "GD2 Pending Invitee", ext);
        for (User u : new User[] {l02, m02a, m02b, m02p}) {
            seedHelper.registerStudent(hackathon, u);
        }
        Team t02 = seedHelper.ensureTeam(hackathon, Gd2TeamsEdgeSeedConstants.TEAM_02, l02, hn,
                TeamStatus.PENDING, false, now);
        seedHelper.ensureTeamMember(t02, l02, TeamMemberRole.LEADER, TeamMemberStatus.ACCEPTED, now);
        seedHelper.ensureTeamMember(t02, m02a, TeamMemberRole.MEMBER, TeamMemberStatus.ACCEPTED, now);
        seedHelper.ensureTeamMember(t02, m02b, TeamMemberRole.MEMBER, TeamMemberStatus.ACCEPTED, now);
        seedHelper.ensureTeamMember(t02, m02p, TeamMemberRole.MEMBER, TeamMemberStatus.PENDING, now);

        // T03 — PENDING 4 ACCEPTED, formation submitted
        User l03 = upsert(Gd2TeamsEdgeSeedConstants.studentEmail("hcm.leader03"), "GD2 Leader 03", hcm);
        User[] m03 = {
                upsert(Gd2TeamsEdgeSeedConstants.studentEmail("member06"), "GD2 M06", hcm),
                upsert(Gd2TeamsEdgeSeedConstants.studentEmail("hn.member07"), "GD2 M07", hn),
                upsertExt(Gd2TeamsEdgeSeedConstants.extEmail("member08"), "GD2 M08", ext),
        };
        seedHelper.registerStudent(hackathon, l03);
        for (User u : m03) {
            seedHelper.registerStudent(hackathon, u);
        }
        Team t03 = seedHelper.ensureTeam(hackathon, Gd2TeamsEdgeSeedConstants.TEAM_03, l03, hcm,
                TeamStatus.PENDING, false, now);
        seedHelper.ensureTeamMember(t03, l03, TeamMemberRole.LEADER, TeamMemberStatus.ACCEPTED, now);
        for (User u : m03) {
            seedHelper.ensureTeamMember(t03, u, TeamMemberRole.MEMBER, TeamMemberStatus.ACCEPTED, now);
        }
        seedHelper.markFormationSubmitted(t03, now);

        // T04 — ACTIVE + lottery + mentor
        User l04 = upsertExt(Gd2TeamsEdgeSeedConstants.extEmail("leader04"), "GD2 Leader 04", ext);
        User m04a = upsert(Gd2TeamsEdgeSeedConstants.studentEmail("hcm.member10"), "GD2 M10", hcm);
        User m04b = upsert(Gd2TeamsEdgeSeedConstants.studentEmail("hn.member11"), "GD2 M11", hn);
        User busy = upsertExt(Gd2TeamsEdgeSeedConstants.extEmail("pool.busy"), "GD2 Pool Busy", ext);
        for (User u : new User[] {l04, m04a, m04b, busy}) {
            seedHelper.registerStudent(hackathon, u);
        }
        Team t04 = seedHelper.ensureTeam(hackathon, Gd2TeamsEdgeSeedConstants.TEAM_04, l04, ext,
                TeamStatus.ACTIVE, false, now);
        seedHelper.ensureTeamMember(t04, l04, TeamMemberRole.LEADER, TeamMemberStatus.ACCEPTED, now);
        seedHelper.ensureTeamMember(t04, m04a, TeamMemberRole.MEMBER, TeamMemberStatus.ACCEPTED, now);
        seedHelper.ensureTeamMember(t04, m04b, TeamMemberRole.MEMBER, TeamMemberStatus.ACCEPTED, now);
        seedHelper.ensureTeamMember(t04, busy, TeamMemberRole.MEMBER, TeamMemberStatus.ACCEPTED, now);
        seedHelper.ensureLottery(hackathon, prelim, track1, "BANG-A", t04, coordinator, now);
        seedHelper.ensureMentorTeamAssignment(hackathon, prelim, t04, mentor, coordinator, now);

        // T05 — ACTIVE locked + lottery
        User l05 = upsert(Gd2TeamsEdgeSeedConstants.studentEmail("hcm.leader05"), "GD2 Leader 05", hcm);
        User[] m05 = {
                upsert(Gd2TeamsEdgeSeedConstants.studentEmail("member12"), "GD2 M12", hcm),
                upsertExt(Gd2TeamsEdgeSeedConstants.extEmail("member13"), "GD2 M13", ext),
                upsert(Gd2TeamsEdgeSeedConstants.studentEmail("hn.member14"), "GD2 M14", hn),
        };
        seedHelper.registerStudent(hackathon, l05);
        for (User u : m05) {
            seedHelper.registerStudent(hackathon, u);
        }
        Team t05 = seedHelper.ensureTeam(hackathon, Gd2TeamsEdgeSeedConstants.TEAM_05, l05, hcm,
                TeamStatus.ACTIVE, true, now);
        seedHelper.ensureTeamMember(t05, l05, TeamMemberRole.LEADER, TeamMemberStatus.ACCEPTED, now);
        for (User u : m05) {
            seedHelper.ensureTeamMember(t05, u, TeamMemberRole.MEMBER, TeamMemberStatus.ACCEPTED, now);
        }
        seedHelper.ensureLottery(hackathon, prelim, track1, "BANG-A", t05, coordinator, now);

        // T06 — REJECTED
        User l06 = upsert(Gd2TeamsEdgeSeedConstants.studentEmail("hcm.leader06"), "GD2 Leader 06", hcm);
        seedHelper.registerStudent(hackathon, l06);
        Team t06 = seedHelper.ensureTeam(hackathon, Gd2TeamsEdgeSeedConstants.TEAM_06, l06, hcm,
                TeamStatus.REJECTED, false, now);
        seedHelper.ensureTeamMember(t06, l06, TeamMemberRole.LEADER, TeamMemberStatus.ACCEPTED, now);

        // T07 — ACTIVE no mentor
        User l07 = upsert(Gd2TeamsEdgeSeedConstants.studentEmail("hcm.leader07"), "GD2 Leader 07", hcm);
        User[] m07 = {
                upsert(Gd2TeamsEdgeSeedConstants.studentEmail("hn.member17"), "GD2 M17", hn),
                upsertExt(Gd2TeamsEdgeSeedConstants.extEmail("member18"), "GD2 M18", ext),
        };
        seedHelper.registerStudent(hackathon, l07);
        for (User u : m07) {
            seedHelper.registerStudent(hackathon, u);
        }
        Team t07 = seedHelper.ensureTeam(hackathon, Gd2TeamsEdgeSeedConstants.TEAM_07, l07, hcm,
                TeamStatus.ACTIVE, false, now);
        seedHelper.ensureTeamMember(t07, l07, TeamMemberRole.LEADER, TeamMemberStatus.ACCEPTED, now);
        for (User u : m07) {
            seedHelper.ensureTeamMember(t07, u, TeamMemberRole.MEMBER, TeamMemberStatus.ACCEPTED, now);
        }

        // T08 — ELIMINATED
        User l08 = upsert(Gd2TeamsEdgeSeedConstants.studentEmail("hcm.leader08"), "GD2 Leader 08", hcm);
        seedHelper.registerStudent(hackathon, l08);
        Team t08 = seedHelper.ensureTeam(hackathon, Gd2TeamsEdgeSeedConstants.TEAM_08, l08, hcm,
                TeamStatus.ELIMINATED, false, now);
        seedHelper.ensureTeamMember(t08, l08, TeamMemberRole.LEADER, TeamMemberStatus.ACCEPTED, now);

        // T09 — ACTIVE track2
        User l09 = upsertExt(Gd2TeamsEdgeSeedConstants.extEmail("leader09"), "GD2 Leader 09", ext);
        User[] m09 = {
                upsert(Gd2TeamsEdgeSeedConstants.studentEmail("hcm.member21"), "GD2 M21", hcm),
                upsertExt(Gd2TeamsEdgeSeedConstants.extEmail("member22"), "GD2 M22", ext),
        };
        seedHelper.registerStudent(hackathon, l09);
        for (User u : m09) {
            seedHelper.registerStudent(hackathon, u);
        }
        Team t09 = seedHelper.ensureTeam(hackathon, Gd2TeamsEdgeSeedConstants.TEAM_09, l09, ext,
                TeamStatus.ACTIVE, false, now);
        seedHelper.ensureTeamMember(t09, l09, TeamMemberRole.LEADER, TeamMemberStatus.ACCEPTED, now);
        for (User u : m09) {
            seedHelper.ensureTeamMember(t09, u, TeamMemberRole.MEMBER, TeamMemberStatus.ACCEPTED, now);
        }
        seedHelper.ensureLottery(hackathon, prelim, track2, "BANG-B", t09, coordinator, now);
    }

    private User upsert(String email, String name, Chapter chapter) {
        return seedHelper.upsertStudent(email, name, chapter);
    }

    private User upsertExt(String email, String name, Chapter chapter) {
        return seedHelper.upsertStudent(email, name, chapter, UserType.EXTERNAL);
    }

    @Transactional
    public void repairForFeTesting() {
        if (!enabled) {
            return;
        }
        hackathonRepository.findBySlug(Gd2TeamsEdgeSeedConstants.SLUG_GD2_TEAMS_EDGE).ifPresent(h -> {
            HackathonDevSeedHelper.SeedDates dates = seedHelper.computeGd2RegistrationOpenDates();
            seedHelper.syncHackathonCalendarFromDates(Gd2TeamsEdgeSeedConstants.SLUG_GD2_TEAMS_EDGE, dates);
            if (h.getStatus() != HackathonStatus.ONGOING) {
                h.setStatus(HackathonStatus.ONGOING);
                hackathonRepository.save(h);
            }
            Round prelim = loadPrelim(h.getId());
            if (teamRepository.findByHackathon_Id(h.getId()).size() < 9) {
                var structure = seedHelper.ensureHackathonStructure(
                        Gd2TeamsEdgeSeedConstants.SLUG_GD2_TEAMS_EDGE,
                        h.getName(),
                        HackathonStatus.ONGOING,
                        h.getDescription(),
                        new HackathonDevSeedHelper.PrelimState(false, false, false, false, 2, 4),
                        new HackathonDevSeedHelper.FinalState(false, false),
                        dates);
                seedTeams(h, prelim, structure.track1(), structure.track2());
            }
        });
    }

    @Transactional
    public void resetAndSeed() {
        devSeedCleanup.purgeIfPresent(Gd2TeamsEdgeSeedConstants.SLUG_GD2_TEAMS_EDGE);
        ensureSeed();
    }

    private boolean needsRepair(Hackathon hackathon) {
        return hackathon.getStatus() != HackathonStatus.ONGOING
                || teamRepository.findByHackathon_Id(hackathon.getId()).size() < 9;
    }

    private Round loadPrelim(Integer hackathonId) {
        return roundRepository.findByHackathon_IdOrderByExamAtAsc(hackathonId).stream()
                .filter(r -> !Boolean.TRUE.equals(r.getIsFinal()))
                .findFirst()
                .orElseThrow();
    }
}
