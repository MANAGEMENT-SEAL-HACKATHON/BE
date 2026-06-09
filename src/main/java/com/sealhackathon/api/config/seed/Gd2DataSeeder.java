package com.sealhackathon.api.config.seed;

import com.sealhackathon.api.chapters.entity.Chapter;
import com.sealhackathon.api.chapters.repository.ChapterRepository;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.mentor_team_assignments.entity.MentorTeamAssignment;
import com.sealhackathon.api.mentor_team_assignments.repository.MentorTeamAssignmentRepository;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.rounds.value_object.RoundType;
import com.sealhackathon.api.team_members.entity.TeamMember;
import com.sealhackathon.api.team_members.entity.TeamMemberId;
import com.sealhackathon.api.team_members.repository.TeamMemberRepository;
import com.sealhackathon.api.team_members.value_object.TeamMemberRole;
import com.sealhackathon.api.team_members.value_object.TeamMemberStatus;
import com.sealhackathon.api.team_round_participation.entity.TeamRoundParticipation;
import com.sealhackathon.api.team_round_participation.repository.TeamRoundParticipationRepository;
import com.sealhackathon.api.team_round_tracks.entity.TeamRoundTrack;
import com.sealhackathon.api.team_round_tracks.repository.TeamRoundTrackRepository;
import com.sealhackathon.api.team_round_tracks.value_object.RegistrationType;
import com.sealhackathon.api.teams.entity.Team;
import com.sealhackathon.api.teams.repository.TeamRepository;
import com.sealhackathon.api.teams.value_object.TeamStatus;
import com.sealhackathon.api.tracks.entity.Track;
import com.sealhackathon.api.tracks.repository.TrackRepository;
import com.sealhackathon.api.users.entity.User;
import com.sealhackathon.api.users.repository.UserRepository;
import com.sealhackathon.api.users.value_object.UserRole;
import com.sealhackathon.api.users.value_object.UserStatus;
import com.sealhackathon.api.users.value_object.UserType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Seed MF-02 GĐ2 — ≥5 bản ghi đa dạng / bảng teams, team_members, lottery, mentor.
 * Tham chiếu: {@code docs/mf02/05-test-data-gd2-teams.md}.
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class Gd2DataSeeder {

    private final ChapterRepository chapterRepository;
    private final UserRepository userRepository;
    private final HackathonRepository hackathonRepository;
    private final RoundRepository roundRepository;
    private final TrackRepository trackRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamRoundParticipationRepository teamRoundParticipationRepository;
    private final TeamRoundTrackRepository teamRoundTrackRepository;
    private final MentorTeamAssignmentRepository mentorTeamAssignmentRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void ensureSeed() {
        Optional<Hackathon> hackathon = hackathonRepository.findBySlug(Gd1SeedConstants.SLUG_ONGOING);
        if (hackathon.isEmpty() || hackathon.get().getStatus() != HackathonStatus.ONGOING) {
            log.warn("[Gd2DataSeeder] Bỏ qua — chưa có hackathon ONGOING slug={}", Gd1SeedConstants.SLUG_ONGOING);
            return;
        }
        if (teamRepository.existsByHackathon_IdAndTeamNameIgnoreCase(hackathon.get().getId(), Gd2SeedConstants.TEAM_01)) {
            return;
        }
        seedAll(hackathon.get());
    }

    /**
     * Mỗi lần start dev: đảm bảo GĐ2 test được — đăng ký còn mở, prelim chưa active, unlock teams (trừ demo locked).
     */
    @Transactional
    public void repairForFeTesting() {
        Optional<Hackathon> hackathon = hackathonRepository.findBySlug(Gd1SeedConstants.SLUG_ONGOING);
        if (hackathon.isEmpty() || hackathon.get().getStatus() != HackathonStatus.ONGOING) {
            return;
        }
        Hackathon h = hackathon.get();
        if (!teamRepository.existsByHackathon_IdAndTeamNameIgnoreCase(h.getId(), Gd2SeedConstants.TEAM_01)) {
            return;
        }

        roundRepository.findByHackathon_IdOrderByExamAtAsc(h.getId()).stream()
                .filter(r -> r.getRoundType() == RoundType.PRELIMINARY)
                .findFirst()
                .ifPresent(prelim -> {
                    if (Boolean.TRUE.equals(prelim.getIsActive())) {
                        prelim.setIsActive(false);
                        prelim.setActivatedAt(null);
                        roundRepository.save(prelim);
                    }
                });

        LocalDateTime now = LocalDateTime.now();
        int unlocked = 0;
        int lockedDemo = 0;
        for (Team team : teamRepository.findByHackathon_Id(h.getId())) {
            if (!Gd2SeedConstants.TEAM_05.equals(team.getTeamName())) {
                if (Boolean.TRUE.equals(team.getIsLocked())) {
                    team.setIsLocked(false);
                    team.setLockedAt(null);
                    teamRepository.save(team);
                    unlocked++;
                }
                continue;
            }
            if (team.getStatus() == TeamStatus.ACTIVE && !Boolean.TRUE.equals(team.getIsLocked())) {
                team.setIsLocked(true);
                team.setLockedAt(now);
                teamRepository.save(team);
                lockedDemo++;
            }
        }

        log.info("""
                [Gd2DataSeeder] FE repair slug={} hackathonId={}:
                  registration: {} → {} (today={}) — đăng ký {}
                  prelim: inactive (GĐ2 — chưa gate activate)
                  teams unlocked={} | demo locked ({}): 1
                  Password SV: {} | Chi tiết: docs/testing/fe-gd1-gd2-gd3-workflow-mapping.md
                """,
                h.getSlug(),
                h.getId(),
                h.getRegistrationStart(),
                h.getRegistrationEnd(),
                LocalDate.now(),
                isRegistrationOpen(h) ? "ĐANG MỞ" : "ĐÃ ĐÓNG",
                unlocked,
                Gd2SeedConstants.TEAM_05,
                Gd2SeedConstants.DEV_STUDENT_PASSWORD);
    }

    private static boolean isRegistrationOpen(Hackathon h) {
        LocalDate today = LocalDate.now();
        return h.getRegistrationStart() != null
                && h.getRegistrationEnd() != null
                && !today.isBefore(h.getRegistrationStart())
                && !today.isAfter(h.getRegistrationEnd());
    }

    private void seedAll(Hackathon hackathon) {
        Map<String, Chapter> chapters = loadChapters();
        User mentor = userRepository.findByEmail(Gd1SeedConstants.EMAIL_MENTOR)
                .orElseThrow(() -> new IllegalStateException("Thiếu mentor seed GĐ1"));
        User coordinator = userRepository.findByEmail(Gd1SeedConstants.EMAIL_COORDINATOR)
                .orElseThrow(() -> new IllegalStateException("Thiếu coordinator seed GĐ1"));

        Map<String, User> students = seedStudents(chapters);

        Round prelim = roundRepository.findByHackathon_IdOrderByExamAtAsc(hackathon.getId()).stream()
                .filter(r -> r.getRoundType() == RoundType.PRELIMINARY)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Thiếu round PRELIMINARY"));
        List<Track> tracks = trackRepository.findByRoundIdOrderBySequenceOrderAsc(prelim.getId());
        if (tracks.size() < 2) {
            throw new IllegalStateException("Cần ít nhất 2 track ở vòng sơ loại");
        }
        Track track1 = tracks.get(0);
        Track track2 = tracks.get(1);

        LocalDateTime now = LocalDateTime.now();

        Team t01 = saveTeam(hackathon, Gd2SeedConstants.TEAM_01, students.get(Gd2SeedConstants.STU_HCM_LEADER_01),
                chapters.get(Gd1SeedConstants.CHAPTER_FPT_HCM), TeamStatus.PENDING, false, null, null, now);
        addMember(t01, students.get(Gd2SeedConstants.STU_HCM_LEADER_01), TeamMemberRole.LEADER, TeamMemberStatus.ACCEPTED, now);

        Team t02 = saveTeam(hackathon, Gd2SeedConstants.TEAM_02, students.get(Gd2SeedConstants.STU_HN_LEADER_02),
                chapters.get(Gd1SeedConstants.CHAPTER_FPT_HN), TeamStatus.PENDING, false, null, null, now);
        addMember(t02, students.get(Gd2SeedConstants.STU_HN_LEADER_02), TeamMemberRole.LEADER, TeamMemberStatus.ACCEPTED, now);
        addMember(t02, students.get(Gd2SeedConstants.STU_HCM_03), TeamMemberRole.MEMBER, TeamMemberStatus.ACCEPTED, now);
        addMember(t02, students.get(Gd2SeedConstants.STU_HCM_04), TeamMemberRole.MEMBER, TeamMemberStatus.PENDING, null);

        Team t03 = saveTeam(hackathon, Gd2SeedConstants.TEAM_03, students.get(Gd2SeedConstants.STU_HCM_LEADER_03),
                chapters.get(Gd1SeedConstants.CHAPTER_FPT_HCM), TeamStatus.PENDING, false, null, null, now);
        addMember(t03, students.get(Gd2SeedConstants.STU_HCM_LEADER_03), TeamMemberRole.LEADER, TeamMemberStatus.ACCEPTED, now);
        addMember(t03, students.get(Gd2SeedConstants.STU_HCM_06), TeamMemberRole.MEMBER, TeamMemberStatus.ACCEPTED, now);
        addMember(t03, students.get(Gd2SeedConstants.STU_HN_07), TeamMemberRole.MEMBER, TeamMemberStatus.ACCEPTED, now);
        addMember(t03, students.get(Gd2SeedConstants.STU_EXT_08), TeamMemberRole.MEMBER, TeamMemberStatus.ACCEPTED, now);

        Team t04 = saveTeam(hackathon, Gd2SeedConstants.TEAM_04, students.get(Gd2SeedConstants.STU_EXT_LEADER_04),
                chapters.get(Gd1SeedConstants.CHAPTER_EXT), TeamStatus.ACTIVE, false, null, null, now);
        addMember(t04, students.get(Gd2SeedConstants.STU_EXT_LEADER_04), TeamMemberRole.LEADER, TeamMemberStatus.ACCEPTED, now);
        addMember(t04, students.get(Gd2SeedConstants.STU_HCM_10), TeamMemberRole.MEMBER, TeamMemberStatus.ACCEPTED, now);
        addMember(t04, students.get(Gd2SeedConstants.STU_HN_11), TeamMemberRole.MEMBER, TeamMemberStatus.ACCEPTED, now);
        addMember(t04, students.get(Gd2SeedConstants.STU_POOL_BUSY), TeamMemberRole.MEMBER, TeamMemberStatus.ACCEPTED, now);
        seedLottery(hackathon, prelim, track1, "Bảng A", t04, coordinator, now);
        seedMentor(hackathon, prelim, mentor, t04, coordinator, now);

        Team t05 = saveTeam(hackathon, Gd2SeedConstants.TEAM_05, students.get(Gd2SeedConstants.STU_HCM_LEADER_05),
                chapters.get(Gd1SeedConstants.CHAPTER_FPT_HCM), TeamStatus.ACTIVE, true, now, null, now);
        addMember(t05, students.get(Gd2SeedConstants.STU_HCM_LEADER_05), TeamMemberRole.LEADER, TeamMemberStatus.ACCEPTED, now);
        addMember(t05, students.get(Gd2SeedConstants.STU_HCM_12), TeamMemberRole.MEMBER, TeamMemberStatus.ACCEPTED, now);
        addMember(t05, students.get(Gd2SeedConstants.STU_EXT_13), TeamMemberRole.MEMBER, TeamMemberStatus.ACCEPTED, now);
        addMember(t05, students.get(Gd2SeedConstants.STU_HN_14), TeamMemberRole.MEMBER, TeamMemberStatus.ACCEPTED, now);
        seedLottery(hackathon, prelim, track1, "Bảng A", t05, coordinator, now);
        seedMentor(hackathon, prelim, mentor, t05, coordinator, now);

        Team t06 = saveTeam(hackathon, Gd2SeedConstants.TEAM_06, students.get(Gd2SeedConstants.STU_HCM_LEADER_06),
                chapters.get(Gd1SeedConstants.CHAPTER_FPT_HCM), TeamStatus.REJECTED, false, null,
                "Hồ sơ không khớp quy chế chapter FPT-HCM", now);
        addMember(t06, students.get(Gd2SeedConstants.STU_HCM_LEADER_06), TeamMemberRole.LEADER, TeamMemberStatus.ACCEPTED, now);
        addMember(t06, students.get(Gd2SeedConstants.STU_HCM_15), TeamMemberRole.MEMBER, TeamMemberStatus.ACCEPTED, now);
        addMember(t06, students.get(Gd2SeedConstants.STU_EXT_16), TeamMemberRole.MEMBER, TeamMemberStatus.ACCEPTED, now);

        Team t07 = saveTeam(hackathon, Gd2SeedConstants.TEAM_07, students.get(Gd2SeedConstants.STU_HCM_LEADER_07),
                chapters.get(Gd1SeedConstants.CHAPTER_FPT_HCM), TeamStatus.ACTIVE, false, null, null, now);
        addMember(t07, students.get(Gd2SeedConstants.STU_HCM_LEADER_07), TeamMemberRole.LEADER, TeamMemberStatus.ACCEPTED, now);
        addMember(t07, students.get(Gd2SeedConstants.STU_HN_17), TeamMemberRole.MEMBER, TeamMemberStatus.ACCEPTED, now);
        addMember(t07, students.get(Gd2SeedConstants.STU_EXT_18), TeamMemberRole.MEMBER, TeamMemberStatus.ACCEPTED, now);
        seedLottery(hackathon, prelim, track2, "Bảng B", t07, coordinator, now);
        seedMentor(hackathon, prelim, mentor, t07, coordinator, now);

        Team t08 = saveTeam(hackathon, Gd2SeedConstants.TEAM_08, students.get(Gd2SeedConstants.STU_HCM_LEADER_08),
                chapters.get(Gd1SeedConstants.CHAPTER_FPT_HCM), TeamStatus.ELIMINATED, false, null, null, now);
        t08.setEliminatedAt(now.minusDays(1));
        t08.setEliminationReason("Không nộp bài sơ loại");
        teamRepository.save(t08);
        addMember(t08, students.get(Gd2SeedConstants.STU_HCM_LEADER_08), TeamMemberRole.LEADER, TeamMemberStatus.ACCEPTED, now);
        addMember(t08, students.get(Gd2SeedConstants.STU_HCM_19), TeamMemberRole.MEMBER, TeamMemberStatus.ACCEPTED, now);
        addMember(t08, students.get(Gd2SeedConstants.STU_HN_20), TeamMemberRole.MEMBER, TeamMemberStatus.ACCEPTED, now);

        Team t09 = saveTeam(hackathon, Gd2SeedConstants.TEAM_09, students.get(Gd2SeedConstants.STU_EXT_LEADER_09),
                chapters.get(Gd1SeedConstants.CHAPTER_EXT), TeamStatus.ACTIVE, false, null, null, now);
        addMember(t09, students.get(Gd2SeedConstants.STU_EXT_LEADER_09), TeamMemberRole.LEADER, TeamMemberStatus.ACCEPTED, now);
        addMember(t09, students.get(Gd2SeedConstants.STU_HCM_21), TeamMemberRole.MEMBER, TeamMemberStatus.ACCEPTED, now);
        addMember(t09, students.get(Gd2SeedConstants.STU_EXT_22), TeamMemberRole.MEMBER, TeamMemberStatus.ACCEPTED, now);
        seedLottery(hackathon, prelim, track2, "Bảng B", t09, coordinator, now);
        seedMentor(hackathon, prelim, mentor, t09, coordinator, now);

        seedLottery(hackathon, prelim, track1, "Bảng A", t08, coordinator, now);
        seedMentor(hackathon, prelim, mentor, t03, coordinator, now);

        addMember(t02, students.get(Gd2SeedConstants.STU_EXT_23), TeamMemberRole.MEMBER, TeamMemberStatus.LEFT, now);

        log.info("""
                [Gd2DataSeeder] Đã seed MF-02 GĐ2 trên hackathon id={} slug={}:
                  users (students): {} | teams: 9 | lottery: 5 | mentor: 5
                  Password sinh viên: {}
                  Chi tiết: docs/mf02/05-test-data-gd2-teams.md
                """,
                hackathon.getId(),
                hackathon.getSlug(),
                Gd2SeedConstants.ALL_STUDENT_EMAILS.length,
                Gd2SeedConstants.DEV_STUDENT_PASSWORD);
    }

    private Map<String, Chapter> loadChapters() {
        Map<String, Chapter> map = new HashMap<>();
        for (String code : List.of(
                Gd1SeedConstants.CHAPTER_FPT_HCM,
                Gd1SeedConstants.CHAPTER_FPT_HN,
                Gd1SeedConstants.CHAPTER_EXT)) {
            Chapter ch = chapterRepository.findByCode(code)
                    .orElseThrow(() -> new IllegalStateException("Thiếu chapter " + code));
            map.put(code, ch);
        }
        return map;
    }

    private Map<String, User> seedStudents(Map<String, Chapter> chapters) {
        Map<String, User> users = new HashMap<>();
        int idx = 1;
        for (String email : Gd2SeedConstants.ALL_STUDENT_EMAILS) {
            boolean external = email.contains("@gmail.com");
            UserType type = external ? UserType.EXTERNAL : UserType.INTERNAL;
            Chapter chapter = external
                    ? chapters.get(Gd1SeedConstants.CHAPTER_EXT)
                    : (email.contains(".hn.") ? chapters.get(Gd1SeedConstants.CHAPTER_FPT_HN)
                    : chapters.get(Gd1SeedConstants.CHAPTER_FPT_HCM));
            String code = (external ? "EXTGD2" : "SEGD2") + String.format("%03d", idx++);
            String institution = external ? "Trường seed GĐ2 — " + localPart(email) : null;
            String name = displayName(email);
            users.put(email, upsertStudent(email, name, type, chapter, code, institution));
        }
        return users;
    }

    private static String localPart(String email) {
        return email.substring(0, email.indexOf('@'));
    }

    private static String displayName(String email) {
        return localPart(email).replace('.', ' ').replace("student gd2", "SV GD2");
    }

    private User upsertStudent(String email, String fullName, UserType userType, Chapter chapter,
                               String studentCode, String institution) {
        Optional<User> existing = userRepository.findByEmail(email);
        if (existing.isPresent()) {
            User u = existing.get();
            if (u.getRole() != UserRole.STUDENT) {
                throw new IllegalStateException("Email seed GĐ2 trùng role khác STUDENT: " + email);
            }
            return u;
        }
        LocalDateTime now = LocalDateTime.now();
        return userRepository.save(User.builder()
                .email(email)
                .fullName(fullName)
                .passwordHash(passwordEncoder.encode(Gd2SeedConstants.DEV_STUDENT_PASSWORD))
                .role(UserRole.STUDENT)
                .userType(userType)
                .studentCode(studentCode)
                .institution(institution)
                .status(UserStatus.APPROVED)
                .chapter(chapter)
                .isTempAccount(false)
                .isDeptHead(false)
                .mustChangePassword(false)
                .emailVerifiedAt(now)
                .createdAt(now)
                .updatedAt(now)
                .build());
    }

    private Team saveTeam(Hackathon hackathon, String teamName, User leader, Chapter chapter,
                          TeamStatus status, boolean locked, LocalDateTime lockedAt,
                          String rejectionReason, LocalDateTime now) {
        return teamRepository.save(Team.builder()
                .hackathon(hackathon)
                .teamName(teamName)
                .leader(leader)
                .chapter(chapter)
                .status(status)
                .isLocked(locked)
                .lockedAt(lockedAt)
                .rejectionReason(rejectionReason)
                .createdAt(now)
                .build());
    }

    private void addMember(Team team, User user, TeamMemberRole role, TeamMemberStatus status,
                             LocalDateTime joinedAt) {
        teamMemberRepository.save(TeamMember.builder()
                .id(new TeamMemberId(team.getId(), user.getId()))
                .team(team)
                .user(user)
                .roleInTeam(role)
                .status(status)
                .joinedAt(status == TeamMemberStatus.ACCEPTED ? joinedAt : null)
                .build());
    }

    private void seedLottery(Hackathon hackathon, Round prelim, Track track, String group,
                             Team team, User assignedBy, LocalDateTime now) {
        if (teamRoundParticipationRepository.findByTeam_IdAndRound_Id(team.getId(), prelim.getId()).isEmpty()) {
            teamRoundParticipationRepository.save(TeamRoundParticipation.builder()
                    .team(team)
                    .round(prelim)
                    .hackathon(hackathon)
                    .createdAt(now)
                    .build());
        }
        boolean hasTrack = teamRoundTrackRepository.findByTeam_Id(team.getId()).stream()
                .anyMatch(trt -> trt.getTrack().getId().equals(track.getId()));
        if (!hasTrack) {
            teamRoundTrackRepository.save(TeamRoundTrack.builder()
                    .team(team)
                    .track(track)
                    .assignedGroup(group)
                    .registrationType(RegistrationType.ASSIGNED)
                    .assignedAt(now)
                    .assignedBy(assignedBy)
                    .build());
        }
    }

    private void seedMentor(Hackathon hackathon, Round prelim, User mentor, Team team,
                            User assignedBy, LocalDateTime now) {
        if (mentorTeamAssignmentRepository.findByTeam_IdAndRound_Id(team.getId(), prelim.getId()).isEmpty()) {
            mentorTeamAssignmentRepository.save(MentorTeamAssignment.builder()
                    .mentor(mentor)
                    .team(team)
                    .round(prelim)
                    .hackathon(hackathon)
                    .assignedAt(now)
                    .assignedBy(assignedBy)
                    .build());
        }
    }
}
