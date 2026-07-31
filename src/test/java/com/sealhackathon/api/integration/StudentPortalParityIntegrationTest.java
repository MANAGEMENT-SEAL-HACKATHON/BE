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
import com.sealhackathon.api.individual_rankings.entity.IndividualRanking;
import com.sealhackathon.api.individual_rankings.repository.IndividualRankingRepository;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.rounds.value_object.LateSubmissionPolicy;
import com.sealhackathon.api.rounds.value_object.RoundType;
import com.sealhackathon.api.teams.entity.TeamMember;
import com.sealhackathon.api.teams.entity.TeamMemberId;
import com.sealhackathon.api.teams.repository.TeamMemberRepository;
import com.sealhackathon.api.teams.value_object.TeamMemberRole;
import com.sealhackathon.api.teams.value_object.TeamMemberStatus;
import com.sealhackathon.api.teams.repository.TeamRoundTrackRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.profiles.active=test",
        "spring.datasource.url=jdbc:h2:mem:student-parity;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;NON_KEYWORDS=YEAR",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "security.jwt.secret=12345678901234567890123456789012",
        "security.jwt.enabled=true",
        "app.storage.type=local",
        "app.storage.local-dir=target/test-uploads/student-parity"
})
class StudentPortalParityIntegrationTest {

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
    @Autowired private TeamRoundTrackRepository teamRoundTrackRepository;
    @Autowired private IndividualRankingRepository individualRankingRepository;

    @MockitoBean
    private CriteriaCloneSourceUnlinkMigration criteriaCloneSourceUnlinkMigration;

    private String suffix;
    private Chapter chapter;
    private User coordinator;
    private User student;
    private Hackathon fallHackathon;
    private Hackathon springHackathon;
    private Hackathon finishedFall;
    private Round fallPrelim;
    private Track fallTrack;
    private Team fallTeam;
    private Team springTeam;

    @BeforeEach
    void setUp() {
        suffix = UUID.randomUUID().toString().substring(0, 8);
        chapter = chapterRepository.save(Chapter.builder()
                .name("Parity Chapter " + suffix)
                .code("PC-" + suffix)
                .status(ChapterStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build());

        coordinator = saveUser("coord.parity." + suffix + "@it.test", UserRole.COORDINATOR, "Coordinator@dev1");
        student = saveUser("student.parity." + suffix + "@it.test", UserRole.STUDENT, "Student@dev1");

        LocalDateTime now = LocalDateTime.now();

        fallHackathon = hackathonRepository.save(Hackathon.builder()
                .name("Fall Parity " + suffix)
                .slug("fall-parity-" + suffix)
                .season(Season.Fall)
                .year(2026)
                .status(HackathonStatus.ONGOING)
                .registrationStart(LocalDate.now().minusDays(10))
                .registrationEnd(LocalDate.now().plusDays(10))
                .eventStart(LocalDate.now())
                .eventEnd(LocalDate.now().plusDays(30))
                .individualRankingEnabled(true)
                .createdBy(coordinator)
                .createdAt(now)
                .updatedAt(now)
                .build());

        springHackathon = hackathonRepository.save(Hackathon.builder()
                .name("Spring Parity " + suffix)
                .slug("spring-parity-" + suffix)
                .season(Season.Spring)
                .year(2026)
                .status(HackathonStatus.ONGOING)
                .registrationStart(LocalDate.now().minusDays(10))
                .registrationEnd(LocalDate.now().plusDays(10))
                .eventStart(LocalDate.now())
                .eventEnd(LocalDate.now().plusDays(30))
                .individualRankingEnabled(false)
                .createdBy(coordinator)
                .createdAt(now)
                .updatedAt(now)
                .build());

        finishedFall = hackathonRepository.save(Hackathon.builder()
                .name("Fall Finished " + suffix)
                .slug("fall-finished-" + suffix)
                .season(Season.Fall)
                .year(2025)
                .status(HackathonStatus.FINISHED)
                .registrationStart(LocalDate.of(2025, 8, 1))
                .registrationEnd(LocalDate.of(2025, 9, 1))
                .eventStart(LocalDate.of(2025, 9, 15))
                .eventEnd(LocalDate.of(2025, 10, 15))
                .individualRankingEnabled(true)
                .createdBy(coordinator)
                .createdAt(now)
                .updatedAt(now)
                .build());

        fallPrelim = roundRepository.save(Round.builder()
                .hackathon(fallHackathon)
                .name("Vòng Sơ loại Fall")
                .isFinal(false)
                .roundType(RoundType.PRELIMINARY)
                .examAt(now.plusDays(5))
                .submissionOpen(now)
                .submissionDeadline(now.plusDays(4))
                .lateSubmissionPolicy(LateSubmissionPolicy.ALLOW_LATE_PENDING)
                .isActive(false)
                .scoringLocked(false)
                .isPublished(false)
                .build());

        fallTrack = trackRepository.save(Track.builder()
                .round(fallPrelim)
                .name("Track Fall AI")
                .status(TrackStatus.OPEN)
                .sequenceOrder(1)
                .maxTeamsPerGroup(8)
                .build());

        fallTeam = saveActiveTeam(fallHackathon, "FALL-TEAM-" + suffix, student);
        springTeam = saveActiveTeam(springHackathon, "SPRING-TEAM-" + suffix, student);

        individualRankingRepository.save(IndividualRanking.builder()
                .hackathon(finishedFall)
                .user(student)
                .scoreThisHackathon(95f)
                .cumulativeScore(95f)
                .rank(1)
                .isEnabled(true)
                .calculatedAt(now)
                .build());
    }

    @Test
    @DisplayName("slug: seal-fall-ongoing-2026 — FR-U-15-F selectFallTrack")
    void selectFallTrack_createsTeamRoundTrackForFallLeader() throws Exception {
        String token = login(student.getEmail(), "Student@dev1");

        mockMvc.perform(post("/api/v1/me/tracks/{trackId}/select", fallTrack.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        assertThat(teamRoundTrackRepository.findByTeam_IdAndTrack_Round_Id(fallTeam.getId(), fallPrelim.getId()))
                .isPresent();
    }

    @Test
    void selectFallTrack_rejectsSpringHackathon() throws Exception {
        User springStudent = saveUser("spring.only." + suffix + "@it.test", UserRole.STUDENT, "Student@dev1");
        saveActiveTeam(springHackathon, "SPRING-ONLY-" + suffix, springStudent);

        Round springPrelim = roundRepository.save(Round.builder()
                .hackathon(springHackathon)
                .name("Spring Prelim")
                .isFinal(false)
                .roundType(RoundType.PRELIMINARY)
                .examAt(LocalDateTime.now().plusDays(5))
                .submissionOpen(LocalDateTime.now())
                .submissionDeadline(LocalDateTime.now().plusDays(4))
                .lateSubmissionPolicy(LateSubmissionPolicy.ALLOW_LATE_PENDING)
                .isActive(false)
                .scoringLocked(false)
                .isPublished(false)
                .build());

        Track springTrack = trackRepository.save(Track.builder()
                .round(springPrelim)
                .name("Spring Track")
                .status(TrackStatus.OPEN)
                .sequenceOrder(1)
                .maxTeamsPerGroup(8)
                .build());

        String token = login(springStudent.getEmail(), "Student@dev1");

        mockMvc.perform(post("/api/v1/me/tracks/{trackId}/select", springTrack.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("NOT_APPLICABLE"));
    }

    @Test
    @DisplayName("FR-U-32 getAnnualAwards — finished Fall ranking (inline fixture)")
    void getAnnualAwards_returnsFallIndividualRanking() throws Exception {
        String token = login(student.getEmail(), "Student@dev1");

        MvcResult result = mockMvc.perform(get("/api/v1/me/annual-awards")
                        .param("year", "2025")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode items = readJson(result).path("data");
        assertThat(items.isArray()).isTrue();
        assertThat(items).hasSize(1);
        assertThat(items.get(0).path("awardName").asText()).isEqualTo("Best Innovator");
        assertThat(items.get(0).path("hackathonName").asText()).contains("Fall Finished");
        assertThat(items.get(0).path("rank").asInt()).isEqualTo(1);
    }

    private Team saveActiveTeam(Hackathon hackathon, String teamName, User leader) {
        Team team = teamRepository.save(Team.builder()
                .hackathon(hackathon)
                .teamName(teamName)
                .leader(leader)
                .chapter(chapter)
                .status(TeamStatus.ACTIVE)
                .isLocked(false)
                .build());

        teamMemberRepository.save(TeamMember.builder()
                .id(new TeamMemberId(team.getId(), leader.getId()))
                .team(team)
                .user(leader)
                .roleInTeam(TeamMemberRole.LEADER)
                .status(TeamMemberStatus.ACCEPTED)
                .joinedAt(LocalDateTime.now())
                .build());

        return team;
    }

    private User saveUser(String email, UserRole role, String rawPassword) {
        return userRepository.save(User.builder()
                .fullName(role.name() + " " + suffix)
                .email(email)
                .passwordHash(passwordEncoder.encode(rawPassword))
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
        String body = """
                {"email": "%s", "password": "%s"}
                """.formatted(email, password);
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).path("data").path("accessToken").asText();
    }

    private JsonNode readJson(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
