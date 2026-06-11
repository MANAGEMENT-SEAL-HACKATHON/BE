package com.sealhackathon.api.config.seed;

import com.sealhackathon.api.hackathon_registrations.entity.HackathonRegistration;
import com.sealhackathon.api.hackathon_registrations.repository.HackathonRegistrationRepository;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.team_members.entity.TeamMember;
import com.sealhackathon.api.team_members.entity.TeamMemberId;
import com.sealhackathon.api.team_members.repository.TeamMemberRepository;
import com.sealhackathon.api.team_members.value_object.TeamMemberRole;
import com.sealhackathon.api.team_members.value_object.TeamMemberStatus;
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
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@Profile("dev")
@Order(100)
@RequiredArgsConstructor
@Slf4j
public class MatchmakingDataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final HackathonRepository hackathonRepository;
    private final HackathonRegistrationRepository hackathonRegistrationRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("[DataSeeder] Đang tạo bộ Data chuẩn để Test Matchmaking & God Mode...");
        Hackathon hackathon = hackathonRepository.findById(7).orElse(null);
        if (hackathon == null) return;

        // Tạo 11 User Test
        List<User> users = new ArrayList<>();
        for (int i = 1; i <= 11; i++) {
            String email = "test.user" + i + "@fpt.edu.vn";
            if (!userRepository.existsByEmail(email)) {
                User u = userRepository.save(User.builder().fullName("SV Test " + i).email(email)
                        .passwordHash(passwordEncoder.encode("Student@dev1")).role(UserRole.STUDENT)
                        .userType(UserType.INTERNAL).status(UserStatus.APPROVED).studentCode("SE" + 9900 + i).build());
                hackathonRegistrationRepository.save(HackathonRegistration.builder().user(u).hackathon(hackathon).build());
                users.add(u);
            }
        }
        if(users.isEmpty()) return;

        // User 1, 2, 3: Bỏ bơ vơ (Orphans)

        // Đội Test A (2 pax - PENDING): User 4, 5
        createTeamWithMembers(hackathon, "Đội Test A", TeamStatus.PENDING, users.get(3), List.of(users.get(4)));

        // Đội Test B (2 pax - PENDING): User 6, 7
        createTeamWithMembers(hackathon, "Đội Test B", TeamStatus.PENDING, users.get(5), List.of(users.get(6)));

        // Đội Test C (4 pax - ACTIVE): User 8, 9, 10, 11
        createTeamWithMembers(hackathon, "Đội Test C", TeamStatus.ACTIVE, users.get(7), List.of(users.get(8), users.get(9), users.get(10)));

        log.info("[DataSeeder] Hoàn tất bộ Data! Có 3 Orphans, Đội A(2), Đội B(2), Đội C(4).");
    }

    private void createTeamWithMembers(Hackathon h, String name, TeamStatus status, User leader, List<User> members) {
        Team t = teamRepository.save(Team.builder().hackathon(h).teamName(name).leader(leader).status(status).isLocked(false).build());
        teamMemberRepository.save(TeamMember.builder().id(new TeamMemberId(t.getId(), leader.getId())).team(t).user(leader)
                .roleInTeam(TeamMemberRole.LEADER).status(TeamMemberStatus.ACCEPTED).joinedAt(LocalDateTime.now()).build());
        for (User m : members) {
            teamMemberRepository.save(TeamMember.builder().id(new TeamMemberId(t.getId(), m.getId())).team(t).user(m)
                    .roleInTeam(TeamMemberRole.MEMBER).status(TeamMemberStatus.ACCEPTED).joinedAt(LocalDateTime.now()).build());
        }
    }
}