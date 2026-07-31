package com.sealhackathon.api.integration;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.sealhackathon.api.chapters.entity.Chapter;
import com.sealhackathon.api.chapters.repository.ChapterRepository;
import com.sealhackathon.api.chapters.value_object.ChapterStatus;
import com.sealhackathon.api.config.CriteriaCloneSourceUnlinkMigration;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.entity.HackathonRegistration;
import com.sealhackathon.api.hackathons.repository.HackathonRegistrationRepository;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.hackathons.value_object.Season;
import com.sealhackathon.api.teams.entity.Team;
import com.sealhackathon.api.teams.entity.TeamMember;
import com.sealhackathon.api.teams.entity.TeamMemberId;
import com.sealhackathon.api.teams.repository.TeamMemberRepository;
import com.sealhackathon.api.teams.repository.TeamRepository;
import com.sealhackathon.api.teams.value_object.TeamMemberRole;
import com.sealhackathon.api.teams.value_object.TeamMemberStatus;
import com.sealhackathon.api.teams.value_object.TeamStatus;
import com.sealhackathon.api.users.entity.User;
import com.sealhackathon.api.users.repository.UserRepository;
import com.sealhackathon.api.users.value_object.UserRole;
import com.sealhackathon.api.users.value_object.UserStatus;
import com.sealhackathon.api.users.value_object.UserType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.profiles.active=test",
        "spring.datasource.url=jdbc:h2:mem:team-reg-invite;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;NON_KEYWORDS=YEAR",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "security.jwt.secret=12345678901234567890123456789012",
        "security.jwt.enabled=true",
        "app.storage.type=local",
        "app.storage.local-dir=target/test-uploads/team-reg-invite"
})
class TeamRegistrationInviteIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private ChapterRepository chapterRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private HackathonRepository hackathonRepository;
    @Autowired private HackathonRegistrationRepository hackathonRegistrationRepository;
    @Autowired private TeamRepository teamRepository;
    @Autowired private TeamMemberRepository teamMemberRepository;

    @MockitoBean
    private CriteriaCloneSourceUnlinkMigration criteriaCloneSourceUnlinkMigration;

    private String suffix;
    private Chapter chapter;
    private User coordinator;
    private User leader;
    private User inviteeUnregistered;
    private User inviteeRegistered;
    private Hackathon hackathonA;
    private Hackathon hackathonB;

    @BeforeEach
    void setUp() {
        suffix = UUID.randomUUID().toString().substring(0, 8);
        chapter = chapterRepository.save(Chapter.builder()
                .name("TRI " + suffix)
                .code("TRI-" + suffix)
                .status(ChapterStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build());
        coordinator = saveUser(chapter, "coord.tri." + suffix + "@it.test", UserRole.COORDINATOR);
        leader = saveUser(chapter, "leader.tri." + suffix + "@it.test", UserRole.STUDENT);
        inviteeUnregistered = saveUser(chapter, "invitee.out." + suffix + "@it.test", UserRole.STUDENT);
        inviteeRegistered = saveUser(chapter, "invitee.in." + suffix + "@it.test", UserRole.STUDENT);

        hackathonA = saveHackathon("A");
        hackathonB = saveHackathon("B");
    }

    @Test
    @DisplayName("createTeam without registration → LEADER_NOT_REGISTERED")
    void createTeam_withoutRegistration_rejected() throws Exception {
        String token = login(leader.getEmail(), "Student@dev1");
        MvcResult result = mockMvc.perform(post("/api/v1/me/teams")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"hackathonId": %d, "teamName": "TRI-NO-REG-%s"}
                                """.formatted(hackathonA.getId(), suffix)))
                .andExpect(status().isUnprocessableEntity())
                .andReturn();

        assertThat(readJson(result).path("error").path("code").asText())
                .isEqualTo("LEADER_NOT_REGISTERED");
    }

    @Test
    @DisplayName("invite unregistered student → INVITEE_NOT_REGISTERED")
    void invite_unregisteredStudent_rejected() throws Exception {
        register(hackathonA, leader);
        Team team = createTeamWithLeader(hackathonA, leader, "TRI-INV-" + suffix);
        String token = login(leader.getEmail(), "Student@dev1");

        MvcResult result = mockMvc.perform(post("/api/v1/teams/{id}/members/invite", team.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s"}
                                """.formatted(inviteeUnregistered.getEmail())))
                .andExpect(status().isUnprocessableEntity())
                .andReturn();

        assertThat(readJson(result).path("error").path("code").asText())
                .isEqualTo("INVITEE_NOT_REGISTERED");
    }

    @Test
    @DisplayName("lookup?hackathonId=A excludes students only registered on B")
    void lookup_filtersByHackathonRegistration() throws Exception {
        register(hackathonA, leader);
        register(hackathonB, inviteeUnregistered);
        register(hackathonA, inviteeRegistered);

        String token = login(leader.getEmail(), "Student@dev1");
        MvcResult result = mockMvc.perform(get("/api/v1/users/lookup")
                        .param("q", "invitee")
                        .param("hackathonId", String.valueOf(hackathonA.getId()))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode data = readJson(result).path("data");
        assertThat(data.isArray()).isTrue();
        boolean hasRegistered = false;
        boolean hasOther = false;
        for (JsonNode node : data) {
            String email = node.path("email").asText();
            if (inviteeRegistered.getEmail().equalsIgnoreCase(email)) {
                hasRegistered = true;
            }
            if (inviteeUnregistered.getEmail().equalsIgnoreCase(email)) {
                hasOther = true;
            }
        }
        assertThat(hasRegistered).isTrue();
        assertThat(hasOther).isFalse();
    }

    @Test
    @DisplayName("createTeam before registrationStart → REGISTRATION_CLOSED")
    void createTeam_beforeRegistrationStart_rejected() throws Exception {
        Hackathon future = hackathonRepository.save(Hackathon.builder()
                .name("Future " + suffix)
                .slug("it-tri-future-" + suffix)
                .season(Season.Spring)
                .year(2026)
                .status(HackathonStatus.ONGOING)
                .registrationStart(LocalDate.now().plusDays(5).atTime(0, 0))
                .registrationEnd(LocalDate.now().plusDays(20).atTime(23, 59))
                .eventStart(LocalDate.now().plusDays(25))
                .eventEnd(LocalDate.now().plusDays(30))
                .individualRankingEnabled(false)
                .createdBy(coordinator)
                .build());
        // Even if somehow registered (seed edge), window must be closed
        register(future, leader);

        String token = login(leader.getEmail(), "Student@dev1");
        MvcResult result = mockMvc.perform(post("/api/v1/me/teams")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"hackathonId": %d, "teamName": "TRI-FUTURE-%s"}
                                """.formatted(future.getId(), suffix)))
                .andExpect(status().isUnprocessableEntity())
                .andReturn();

        assertThat(readJson(result).path("error").path("code").asText())
                .isEqualTo("REGISTRATION_CLOSED");
    }

    private Hackathon saveHackathon(String label) {
        return hackathonRepository.save(Hackathon.builder()
                .name("TRI " + label + " " + suffix)
                .slug("it-tri-" + label.toLowerCase() + "-" + suffix)
                .season(Season.Spring)
                .year(2026)
                .status(HackathonStatus.ONGOING)
                .registrationStart(LocalDate.now().minusDays(5).atTime(0, 0))
                .registrationEnd(LocalDate.now().plusDays(10).atTime(23, 59))
                .eventStart(LocalDate.now().plusDays(15))
                .eventEnd(LocalDate.now().plusDays(20))
                .individualRankingEnabled(false)
                .createdBy(coordinator)
                .build());
    }

    private void register(Hackathon hackathon, User user) {
        if (!hackathonRegistrationRepository.existsByHackathon_IdAndUser_Id(hackathon.getId(), user.getId())) {
            hackathonRegistrationRepository.save(HackathonRegistration.builder()
                    .hackathon(hackathon)
                    .user(user)
                    .build());
        }
    }

    private Team createTeamWithLeader(Hackathon hackathon, User student, String name) {
        Team team = teamRepository.save(Team.builder()
                .hackathon(hackathon)
                .teamName(name)
                .leader(student)
                .chapter(chapter)
                .status(TeamStatus.PENDING)
                .isLocked(false)
                .build());
        teamMemberRepository.save(TeamMember.builder()
                .id(new TeamMemberId(team.getId(), student.getId()))
                .team(team)
                .user(student)
                .roleInTeam(TeamMemberRole.LEADER)
                .status(TeamMemberStatus.ACCEPTED)
                .joinedAt(LocalDateTime.now())
                .build());
        return team;
    }

    private User saveUser(Chapter ch, String email, UserRole role) {
        return userRepository.save(User.builder()
                .fullName(role.name())
                .email(email)
                .passwordHash(passwordEncoder.encode("Student@dev1"))
                .role(role)
                .userType(UserType.INTERNAL)
                .status(UserStatus.APPROVED)
                .chapter(ch)
                .studentCode(role == UserRole.STUDENT ? "TRI" + suffix : null)
                .isTempAccount(false)
                .isDeptHead(false)
                .mustChangePassword(false)
                .emailVerifiedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());
    }

    private String login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).path("data").path("accessToken").asText();
    }

    private JsonNode readJson(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
