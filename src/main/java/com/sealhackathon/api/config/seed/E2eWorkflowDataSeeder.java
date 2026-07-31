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
 * Seed E2E GĐ2: 6 đội ACTIVE (3 người/đội), 2 track từ Gd1 — chưa khóa, chưa lottery.
 * Thêm {@link DevSeedCatalog#ORPHAN_COUNT} SV free-agent (chưa ĐK sự kiện, chưa vào đội)
 * để test đăng ký + mời trên sự kiện mới.
 *
 * <p>Hackathon GĐ1 (round/track/criteria/events) do {@link Gd1DataSeeder} tạo.
 * Continuous GĐ2→GĐ6 trên cùng slug {@link DevSeedCatalog#SLUG_E2E_ONGOING}.
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
    private final E2eDevFlowGuard e2eDevFlowGuard;

    @Value("${app.seed.e2e.enabled:true}")
    private boolean enabled;

    @Transactional
    public void ensureSeed() {
        if (!enabled) {
            log.info("[E2eWorkflowDataSeeder] Tắt (app.seed.e2e.enabled=false)");
            return;
        }
        seedTeams();
        seedFreeAgentOrphans();
        if (!e2eDevFlowGuard.isE2eFlowFrozen()) {
            removeSurplusTeamsBeyondProfile();
        }
        logStartupSummary();
    }

    @Transactional
    public void seedTeams() {
        Optional<Hackathon> maybe = hackathonRepository.findBySlug(DevSeedCatalog.SLUG_E2E_ONGOING);
        if (maybe.isEmpty() || maybe.get().getStatus() != HackathonStatus.ONGOING) {
            log.warn("[E2eWorkflowDataSeeder] Bỏ qua — chưa có slug={} ONGOING",
                    DevSeedCatalog.SLUG_E2E_ONGOING);
            return;
        }

        Hackathon hackathon = maybe.get();
        DevSeedCatalog.SnapshotProfile profile = DevSeedCatalog.PROFILE_E2E;
        Chapter hcm = seedHelper.requireChapter(Gd1SeedConstants.CHAPTER_FPT_HCM);
        Chapter hn = seedHelper.requireChapter(Gd1SeedConstants.CHAPTER_FPT_HN);
        LocalDateTime now = LocalDateTime.now();

        List<User> allRegistered = new ArrayList<>();
        boolean createdAny = false;
        for (int i = 1; i <= profile.teamCount(); i++) {
            Chapter chapter = (i % 3 == 0) ? hn : hcm;
            boolean existed = teamRepository.findByHackathon_IdAndTeamNameIgnoreCase(
                    hackathon.getId(), profile.teamName(i)).isPresent();
            Team team = ensureTeamWithMembers(profile, hackathon, i, chapter, now);
            if (!existed) {
                createdAny = true;
            }
            for (TeamMember tm : teamMemberRepository.findByTeam_Id(team.getId())) {
                allRegistered.add(tm.getUser());
            }
        }

        for (User student : allRegistered) {
            registerForHackathon(hackathon, student);
        }

        if (createdAny) {
            log.info("""
                    [E2eWorkflowDataSeeder] slug={} — {}
                      teams: {} … {} (ACTIVE, chưa khóa, chưa lottery)
                      password={}
                    """,
                    hackathon.getSlug(),
                    profile.distributionLabel(),
                    profile.teamName(1),
                    profile.teamName(profile.teamCount()),
                    DevSeedCatalog.DEV_STUDENT_PASSWORD);
        }
    }

    /**
     * 3 SV APPROVED — chưa đăng ký hackathon nào, chưa trong đội.
     * Dùng để test: ĐK sự kiện mới → được leader mời vào đội.
     * (Rule mới: chỉ mời được SV đã ĐK cùng sự kiện.)
     */
    @Transactional
    public void seedFreeAgentOrphans() {
        if (!enabled || DevSeedCatalog.ORPHAN_COUNT <= 0) {
            return;
        }
        Chapter hcm = seedHelper.requireChapter(Gd1SeedConstants.CHAPTER_FPT_HCM);
        int created = 0;
        for (int i = 1; i <= DevSeedCatalog.ORPHAN_COUNT; i++) {
            String email = DevSeedCatalog.orphanEmail(i);
            if (userRepository.findByEmail(email).isPresent()) {
                continue;
            }
            LocalDateTime now = LocalDateTime.now();
            userRepository.save(User.builder()
                    .email(email)
                    .passwordHash(passwordEncoder.encode(DevSeedCatalog.DEV_STUDENT_PASSWORD))
                    .fullName(DevSeedCatalog.orphanDisplayName(i))
                    .role(UserRole.STUDENT)
                    .userType(UserType.INTERNAL)
                    .status(UserStatus.APPROVED)
                    .chapter(hcm)
                    .studentCode("E2EOR%02d".formatted(i))
                    .institution("SEAL E2E free-agent")
                    .isTempAccount(false)
                    .isDeptHead(false)
                    .mustChangePassword(false)
                    .emailVerifiedAt(now)
                    .createdAt(now)
                    .updatedAt(now)
                    .build());
            created++;
        }
        if (created > 0) {
            log.info("""
                    [E2eWorkflowDataSeeder] Free-agent orphans: {} mới (tổng mục tiêu {})
                      emails: {} … {}
                      password={}
                      — chưa ĐK sự kiện / chưa vào đội; ĐK sự kiện rồi mới mời được
                    """,
                    created,
                    DevSeedCatalog.ORPHAN_COUNT,
                    DevSeedCatalog.orphanEmail(1),
                    DevSeedCatalog.orphanEmail(DevSeedCatalog.ORPHAN_COUNT),
                    DevSeedCatalog.DEV_STUDENT_PASSWORD);
        }
    }

    /**
     * Baseline GĐ2 — chỉ khi chưa freeze (hoặc force-gd2-reset).
     */
    @Transactional
    public void repairForGd2Testing() {
        if (!enabled) {
            return;
        }
        if (e2eDevFlowGuard.isE2eFlowFrozen()) {
            e2eDevFlowGuard.logSkip("repairForGd2Testing");
            return;
        }
        hackathonRepository.findBySlug(DevSeedCatalog.SLUG_E2E_ONGOING)
                .filter(h -> h.getStatus() == HackathonStatus.ONGOING
                        || e2eDevFlowGuard.isForceGd2Reset())
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
                    removeSurplusTeamsBeyondProfile();
                    Hackathon refreshed = hackathonRepository.findById(hackathon.getId()).orElse(hackathon);
                    log.info(
                            "[E2eWorkflowDataSeeder] FE repair GĐ2 — regEnd={} prelimActive=false slug={} force={}",
                            refreshed.getRegistrationEnd(),
                            DevSeedCatalog.SLUG_E2E_ONGOING,
                            e2eDevFlowGuard.isForceGd2Reset());
                });
    }

    /**
     * Sau full chain PENDING_CONFIRM / CK locked: reset thẳng GĐ2 nếu không frozen
     * (hoặc khi force-gd2-reset).
     */
    @Transactional
    public void repairForGd5FullChainRetest() {
        if (e2eDevFlowGuard.isE2eFlowFrozen()) {
            e2eDevFlowGuard.logSkip("repairForGd5FullChainRetest");
            return;
        }
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
                    || hackathon.getStatus() == HackathonStatus.FINISHED
                    || Boolean.TRUE.equals(finalRound.getScoringLocked())
                    || e2eDevFlowGuard.isForceGd2Reset()) {
                seedHelper.repairHackathonForGd2Retest(hackathon, prelim, finalRound);
                log.info("[E2eWorkflowDataSeeder] repairForGd5FullChainRetest — reset {} về GĐ2 baseline",
                        DevSeedCatalog.SLUG_E2E_ONGOING);
            }
        });
    }

    /** Xóa E2E-T07+ nếu DB cũ còn (chỉ khi không frozen). */
    private void removeSurplusTeamsBeyondProfile() {
        hackathonRepository.findBySlug(DevSeedCatalog.SLUG_E2E_ONGOING).ifPresent(hackathon -> {
            int max = DevSeedCatalog.PROFILE_E2E.teamCount();
            for (Team team : teamRepository.findByHackathon_Id(hackathon.getId())) {
                String name = team.getTeamName();
                if (name == null || !name.startsWith(DevSeedCatalog.PROFILE_E2E.teamPrefix())) {
                    continue;
                }
                String suffix = name.substring(DevSeedCatalog.PROFILE_E2E.teamPrefix().length());
                if (!suffix.matches("T\\d{2}")) {
                    continue;
                }
                int idx = Integer.parseInt(suffix.substring(1));
                if (idx <= max) {
                    continue;
                }
                teamMemberRepository.findByTeam_Id(team.getId()).forEach(teamMemberRepository::delete);
                teamRepository.delete(team);
                log.info("[E2eWorkflowDataSeeder] Đã xóa đội thừa {}", name);
            }
        });
    }

    private void registerForHackathon(Hackathon hackathon, User user) {
        if (!hackathonRegistrationRepository.existsByHackathon_IdAndUser_Id(hackathon.getId(), user.getId())) {
            hackathonRegistrationRepository.save(HackathonRegistration.builder()
                    .hackathon(hackathon)
                    .user(user)
                    .preferredShirtSize("M")
                    .preferredShirtFit("UNISEX")
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
        Optional<Team> existing = teamRepository.findByHackathon_IdAndTeamNameIgnoreCase(
                hackathon.getId(), teamName);
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

    private void logStartupSummary() {
        log.info("""
                DEV SEED — 1 hackathon
                  ONGOING  slug={} — {} | GĐ2 pre-lottery → continuous GĐ2→GĐ6
                  Free-agent orphans: {} ({} … {}) — chưa ĐK sự kiện
                  SV password: {}
                  force-gd2-reset={} frozen={}
                """,
                DevSeedCatalog.SLUG_E2E_ONGOING,
                DevSeedCatalog.PROFILE_E2E.distributionLabel(),
                DevSeedCatalog.ORPHAN_COUNT,
                DevSeedCatalog.orphanEmail(1),
                DevSeedCatalog.orphanEmail(DevSeedCatalog.ORPHAN_COUNT),
                DevSeedCatalog.DEV_STUDENT_PASSWORD,
                e2eDevFlowGuard.isForceGd2Reset(),
                e2eDevFlowGuard.isE2eFlowFrozen());
    }
}
