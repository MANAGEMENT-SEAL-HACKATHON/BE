package com.sealhackathon.api.integration;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.sealhackathon.api.chapters.entity.Chapter;
import com.sealhackathon.api.chapters.repository.ChapterRepository;
import com.sealhackathon.api.chapters.value_object.ChapterStatus;
import com.sealhackathon.api.config.CriteriaCloneSourceUnlinkMigration;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.hackathons.value_object.Season;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.rounds.value_object.LateSubmissionPolicy;
import com.sealhackathon.api.rounds.value_object.RoundType;
import com.sealhackathon.api.teams.entity.TeamMember;
import com.sealhackathon.api.teams.entity.TeamMemberId;
import com.sealhackathon.api.teams.repository.TeamMemberRepository;
import com.sealhackathon.api.teams.value_object.TeamMemberRole;
import com.sealhackathon.api.teams.value_object.TeamMemberStatus;
import com.sealhackathon.api.teams.entity.Team;
import com.sealhackathon.api.teams.repository.TeamRepository;
import com.sealhackathon.api.teams.value_object.TeamStatus;
import com.sealhackathon.api.tracks.entity.Track;
import com.sealhackathon.api.tracks.repository.TrackRepository;
import com.sealhackathon.api.tracks.value_object.TrackStatus;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.profiles.active=test",
        "spring.datasource.url=jdbc:h2:mem:gd2-gates;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;NON_KEYWORDS=YEAR",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "security.jwt.secret=12345678901234567890123456789012",
        "security.jwt.enabled=true",
        "app.storage.type=local",
        "app.storage.local-dir=target/test-uploads/gd2-gates"
})
class Gd2LotteryGateIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private ChapterRepository chapterRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private HackathonRepository hackathonRepository;
    @Autowired private RoundRepository roundRepository;
    @Autowired private TrackRepository trackRepository;
    @Autowired private TeamRepository teamRepository;
    @Autowired private TeamMemberRepository teamMemberRepository;

    @MockitoBean
    private CriteriaCloneSourceUnlinkMigration criteriaCloneSourceUnlinkMigration;

    private String suffix;
    private User coordinator;
    private Team team;
    private Round prelim;
    private Track track;
    private Hackathon hackathon;

    @BeforeEach
    void setUp() {
        suffix = UUID.randomUUID().toString().substring(0, 8);
        Chapter chapter = chapterRepository.save(Chapter.builder()
                .name("G2 " + suffix)
                .code("G2-" + suffix)
                .status(ChapterStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build());
        coordinator = saveUser(chapter, "coord.g2." + suffix + "@it.test", UserRole.COORDINATOR);
        User student = saveUser(chapter, "student.g2." + suffix + "@it.test", UserRole.STUDENT);

        hackathon = hackathonRepository.save(Hackathon.builder()
                .name("G2 lottery gate " + suffix)
                .slug("it-gd2-lottery-" + suffix)
                .season(Season.Spring)
                .year(2026)
                .status(HackathonStatus.ONGOING)
                .registrationStart(LocalDate.now().minusDays(30).atTime(0, 0))
                .registrationEnd(LocalDate.now().minusDays(1).atTime(23, 59))
                .eventStart(LocalDate.now())
                .eventEnd(LocalDate.now())
                .individualRankingEnabled(false)
                .createdBy(coordinator)
                .build());

        prelim = roundRepository.save(Round.builder()
                .hackathon(hackathon)
                .name("Prelim")
                .isFinal(false)
                .roundType(RoundType.PRELIMINARY)
                .examAt(LocalDateTime.now().plusDays(1))
                .submissionOpen(LocalDateTime.now())
                .submissionDeadline(LocalDateTime.now().plusDays(1))
                .lateSubmissionPolicy(LateSubmissionPolicy.ALLOW_LATE_PENDING)
                .isActive(false)
                .build());

        track = trackRepository.save(Track.builder()
                .round(prelim)
                .name("Track")
                .status(TrackStatus.OPEN)
                .sequenceOrder(1)
                .maxTeamsPerGroup(8)
                .build());

        team = teamRepository.save(Team.builder()
                .hackathon(hackathon)
                .teamName("G2-T01")
                .leader(student)
                .chapter(chapter)
                .status(TeamStatus.ACTIVE)
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
    }

    @Test
    @DisplayName("slug: seal-gd2-lottery-not-locked — TEAM_NOT_LOCKED (G2-N02)")
    void lotteryRejectsUnlockedTeam() throws Exception {
        String token = login(coordinator.getEmail(), "Coordinator@dev1");
        String body = """
                {"roundId": %d, "assignments": [{"teamId": %d, "trackId": %d, "assignedGroup": "A"}]}
                """.formatted(prelim.getId(), team.getId(), track.getId());

        MvcResult result = mockMvc.perform(patch("/api/v1/hackathons/{id}/lottery", hackathon.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnprocessableEntity())
                .andReturn();

        assertThat(readJson(result).path("error").path("code").asText()).isEqualTo("ACTIVE_TEAMS_NOT_LOCKED");
    }

    @Test
    @DisplayName("slug: seal-gd2-round-active — ROUND_ALREADY_ACTIVE (B-N2)")
    void lotteryRejectsWhenRoundAlreadyActive() throws Exception {
        team.setIsLocked(true);
        teamRepository.save(team);

        prelim.setIsActive(true);
        prelim.setActivatedAt(LocalDateTime.now());
        roundRepository.save(prelim);

        String token = login(coordinator.getEmail(), "Coordinator@dev1");
        MvcResult result = mockMvc.perform(patch("/api/v1/hackathons/{id}/lottery", hackathon.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roundId\": %d}".formatted(prelim.getId())))
                .andExpect(status().isUnprocessableEntity())
                .andReturn();

        assertThat(readJson(result).path("error").path("code").asText()).isEqualTo("ROUND_ALREADY_ACTIVE");
    }

    private User saveUser(Chapter chapter, String email, UserRole role) {
        return userRepository.save(User.builder()
                .fullName(role.name())
                .email(email)
                .passwordHash(passwordEncoder.encode("Coordinator@dev1"))
                .role(role)
                .userType(UserType.INTERNAL)
                .status(UserStatus.APPROVED)
                .chapter(chapter)
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
                        .content("{\"email\": \"%s\", \"password\": \"%s\"}".formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).path("data").path("accessToken").asText();
    }

    private JsonNode readJson(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
