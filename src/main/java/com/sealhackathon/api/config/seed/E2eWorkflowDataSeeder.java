package com.sealhackathon.api.config.seed;

import com.sealhackathon.api.chapters.entity.Chapter;
import com.sealhackathon.api.hackathons.entity.HackathonRegistration;
import com.sealhackathon.api.hackathons.repository.HackathonRegistrationRepository;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.rounds.value_object.RoundType;
import com.sealhackathon.api.teams.entity.TeamMember;
import com.sealhackathon.api.teams.entity.TeamMemberId;
import com.sealhackathon.api.teams.repository.TeamMemberRepository;
import com.sealhackathon.api.teams.value_object.TeamMemberRole;
import com.sealhackathon.api.teams.value_object.TeamMemberStatus;
import com.sealhackathon.api.teams.entity.Team;
import com.sealhackathon.api.teams.repository.TeamRepository;
import com.sealhackathon.api.teams.value_object.TeamStatus;
import com.sealhackathon.api.users.entity.User;
import com.sealhackathon.api.users.repository.UserRepository;
import com.sealhackathon.api.users.value_object.UserRole;
import com.sealhackathon.api.users.value_object.UserStatus;
import com.sealhackathon.api.users.value_object.UserType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Seed E2E GĐ2: 7 đội ACTIVE (3 người) + 3 SV đăng ký nhưng chưa có đội.
 *
 * <p>Hackathon GĐ1 (round/track/criteria/events) do {@link Gd1DataSeeder} tạo.
 * GĐ3→GĐ6 dùng slug riêng ({@link Gd3SeedConstants}, …).
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class E2eWorkflowDataSeeder {

    private final HackathonDevSeedHelper seedHelper;
    private final HackathonRepository hackathonRepository;
    private final HackathonRegistrationRepository hackathonRegistrationRepository;
    private final RoundRepository roundRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.e2e.enabled:true}")
    private boolean enabled;

    @Transactional
    public void ensureSeed() {
        if (!enabled) {
            log.info("[E2eWorkflowDataSeeder] Tắt (app.seed.e2e.enabled=false)");
            return;
        }
        seedTeamsAndOrphans();
        logStartupSummary();
    }

    @Transactional
    public void seedTeamsAndOrphans() {
        Optional<Hackathon> maybe = hackathonRepository.findBySlug(DevSeedCatalog.SLUG_E2E_ONGOING);
        if (maybe.isEmpty() || maybe.get().getStatus() != HackathonStatus.ONGOING) {
            log.warn("[E2eWorkflowDataSeeder] Bỏ qua — chưa có slug={} ONGOING",
                    DevSeedCatalog.SLUG_E2E_ONGOING);
            return;
        }
        if (teamRepository.existsByHackathon_IdAndTeamNameIgnoreCase(
                maybe.get().getId(), DevSeedCatalog.TEAM_MARKER)) {
            ensureOrphansRegistered(maybe.get());
            return;
        }

        Hackathon hackathon = maybe.get();
        DevSeedCatalog.SnapshotProfile profile = DevSeedCatalog.PROFILE_E2E;
        Chapter hcm = seedHelper.requireChapter(Gd1SeedConstants.CHAPTER_FPT_HCM);
        Chapter hn = seedHelper.requireChapter(Gd1SeedConstants.CHAPTER_FPT_HN);
        LocalDateTime now = LocalDateTime.now();

        List<User> allRegistered = new ArrayList<>();
        for (int i = 1; i <= profile.teamCount(); i++) {
            Chapter chapter = (i % 3 == 0) ? hn : hcm;
            Team team = ensureTeamWithMembers(profile, hackathon, i, chapter, now);
            for (TeamMember tm : teamMemberRepository.findByTeam_Id(team.getId())) {
                allRegistered.add(tm.getUser());
            }
        }

        for (int i = 1; i <= DevSeedCatalog.ORPHAN_COUNT; i++) {
            User orphan = upsertOrphan(i, hcm);
            allRegistered.add(orphan);
        }

        for (User student : allRegistered) {
            registerForHackathon(hackathon, student);
        }

        log.info("""
                [E2eWorkflowDataSeeder] slug={} — {} | {} SV chưa có nhóm
                  teams: {} … {} (ACTIVE, chưa khóa, chưa lottery)
                  orphans: {} / {} / {}
                  password={}
                """,
                hackathon.getSlug(),
                profile.distributionLabel(),
                DevSeedCatalog.ORPHAN_COUNT,
                profile.teamName(1),
                profile.teamName(profile.teamCount()),
                DevSeedCatalog.orphanEmail(1),
                DevSeedCatalog.orphanEmail(2),
                DevSeedCatalog.orphanEmail(3),
                DevSeedCatalog.DEV_STUDENT_PASSWORD);
    }

    /**
     * Đăng ký còn mở, prelim inactive — sẵn sàng test GĐ2 (tạo đội + mời orphan).
     * Gọi sau {@code repairAllDevHackathonRoundSchedules} mỗi lần start BE.
     */
    @Transactional
    public void repairForGd2Testing() {
        if (!enabled) {
            return;
        }
        hackathonRepository.findBySlug(DevSeedCatalog.SLUG_E2E_ONGOING)
                .filter(h -> h.getStatus() == HackathonStatus.ONGOING)
                .filter(h -> teamRepository.existsByHackathon_IdAndTeamNameIgnoreCase(
                        h.getId(), DevSeedCatalog.TEAM_MARKER))
                .ifPresent(hackathon -> {
                    Round prelim = roundRepository.findByHackathon_IdOrderByExamAtAsc(hackathon.getId()).stream()
                            .filter(r -> r.getRoundType() == RoundType.PRELIMINARY)
                            .findFirst()
                            .orElse(null);
                    Round finalRound = roundRepository.findByHackathon_IdOrderByExamAtAsc(hackathon.getId()).stream()
                            .filter(r -> Boolean.TRUE.equals(r.getIsFinal()))
                            .findFirst()
                            .orElse(null);
                    if (prelim == null || finalRound == null) {
                        return;
                    }
                    seedHelper.repairHackathonForGd2Retest(hackathon, prelim, finalRound);
                    Hackathon refreshed = hackathonRepository.findById(hackathon.getId()).orElse(hackathon);
                    log.info(
                            "[E2eWorkflowDataSeeder] FE repair GĐ2 — regEnd={} prelimActive=false slug={}",
                            refreshed.getRegistrationEnd(),
                            DevSeedCatalog.SLUG_E2E_ONGOING);
                });
    }

    @Transactional
    public void repairForGd5FullChainRetest() {
        hackathonRepository.findBySlug(DevSeedCatalog.SLUG_E2E_ONGOING).ifPresent(hackathon -> {
            Round prelim = roundRepository.findByHackathon_IdOrderByExamAtAsc(hackathon.getId()).stream()
                    .filter(r -> r.getRoundType() == RoundType.PRELIMINARY)
                    .findFirst()
                    .orElse(null);
            Round finalRound = roundRepository.findByHackathon_IdOrderByExamAtAsc(hackathon.getId()).stream()
                    .filter(r -> Boolean.TRUE.equals(r.getIsFinal()))
                    .findFirst()
                    .orElse(null);
            if (prelim == null || finalRound == null) {
                return;
            }
            if (hackathon.getStatus() == HackathonStatus.PENDING_CONFIRM
                    || Boolean.TRUE.equals(finalRound.getScoringLocked())) {
                seedHelper.repairHackathonForGd5Retest(hackathon, prelim, finalRound);
                log.info("[E2eWorkflowDataSeeder] repairForGd5FullChainRetest — reset {} về ONGOING + CK mở",
                        DevSeedCatalog.SLUG_E2E_ONGOING);
            }
        });
    }

    private void ensureOrphansRegistered(Hackathon hackathon) {
        Chapter hcm = seedHelper.requireChapter(Gd1SeedConstants.CHAPTER_FPT_HCM);
        for (int i = 1; i <= DevSeedCatalog.ORPHAN_COUNT; i++) {
            User orphan = upsertOrphan(i, hcm);
            registerForHackathon(hackathon, orphan);
        }
    }

    private void registerForHackathon(Hackathon hackathon, User user) {
        if (!hackathonRegistrationRepository.existsByHackathon_IdAndUser_Id(hackathon.getId(), user.getId())) {
            hackathonRegistrationRepository.save(HackathonRegistration.builder()
                    .hackathon(hackathon)
                    .user(user)
                    .build());
        }
    }

    private Team ensureTeamWithMembers(
            DevSeedCatalog.SnapshotProfile profile,
            Hackathon hackathon,
            int teamIndex,
            Chapter chapter,
            LocalDateTime now) {
        String teamName = profile.teamName(teamIndex);
        Optional<Team> existing = teamRepository.findByHackathon_Id(hackathon.getId()).stream()
                .filter(t -> teamName.equals(t.getTeamName()))
                .findFirst();
        if (existing.isPresent()) {
            return existing.get();
        }

        User leader = upsertStudent(profile, teamIndex, 1, chapter);
        Team team = teamRepository.save(Team.builder()
                .hackathon(hackathon)
                .teamName(teamName)
                .leader(leader)
                .chapter(chapter)
                .status(TeamStatus.ACTIVE)
                .isLocked(false)
                .createdAt(now)
                .build());

        for (int m = 1; m <= DevSeedCatalog.MEMBERS_PER_TEAM; m++) {
            User member = m == 1 ? leader : upsertStudent(profile, teamIndex, m, chapter);
            teamMemberRepository.save(TeamMember.builder()
                    .id(new TeamMemberId(team.getId(), member.getId()))
                    .team(team)
                    .user(member)
                    .roleInTeam(m == 1 ? TeamMemberRole.LEADER : TeamMemberRole.MEMBER)
                    .status(TeamMemberStatus.ACCEPTED)
                    .joinedAt(now)
                    .build());
        }
        return team;
    }

    private User upsertStudent(
            DevSeedCatalog.SnapshotProfile profile,
            int teamIndex,
            int memberIndex,
            Chapter chapter) {
        String email = profile.studentEmail(teamIndex, memberIndex);
        return userRepository.findByEmail(email).orElseGet(() -> {
            LocalDateTime now = LocalDateTime.now();
            return userRepository.save(User.builder()
                    .email(email)
                    .passwordHash(passwordEncoder.encode(DevSeedCatalog.DEV_STUDENT_PASSWORD))
                    .fullName(profile.displayName(teamIndex, memberIndex))
                    .role(UserRole.STUDENT)
                    .userType(UserType.INTERNAL)
                    .status(UserStatus.APPROVED)
                    .chapter(chapter)
                    .studentCode("E2E%02d%02d".formatted(teamIndex, memberIndex))
                    .institution("SEAL E2E seed")
                    .isTempAccount(false)
                    .isDeptHead(false)
                    .mustChangePassword(false)
                    .emailVerifiedAt(now)
                    .createdAt(now)
                    .updatedAt(now)
                    .build());
        });
    }

    private User upsertOrphan(int index, Chapter chapter) {
        String email = DevSeedCatalog.orphanEmail(index);
        return userRepository.findByEmail(email).orElseGet(() -> {
            LocalDateTime now = LocalDateTime.now();
            return userRepository.save(User.builder()
                    .email(email)
                    .passwordHash(passwordEncoder.encode(DevSeedCatalog.DEV_STUDENT_PASSWORD))
                    .fullName(DevSeedCatalog.orphanDisplayName(index))
                    .role(UserRole.STUDENT)
                    .userType(UserType.INTERNAL)
                    .status(UserStatus.APPROVED)
                    .chapter(chapter)
                    .studentCode("E2EO%02d".formatted(index))
                    .institution("SEAL E2E seed — orphan")
                    .isTempAccount(false)
                    .isDeptHead(false)
                    .mustChangePassword(false)
                    .emailVerifiedAt(now)
                    .createdAt(now)
                    .updatedAt(now)
                    .build());
        });
    }

    private void logStartupSummary() {
        log.info("""
                DEV SEED — 2 hackathon
                  ONGOING  slug={} — GĐ1 OK | 7 đội + 3 orphan (test GĐ2→GĐ6)
                  FINISHED slug={} — archive
                  SV password: {}
                """,
                DevSeedCatalog.SLUG_E2E_ONGOING,
                DevSeedCatalog.SLUG_ARCHIVE_FINISHED,
                DevSeedCatalog.DEV_STUDENT_PASSWORD);
    }
}
