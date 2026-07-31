package com.sealhackathon.api.integration;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.sealhackathon.api.chapters.entity.Chapter;
import com.sealhackathon.api.chapters.repository.ChapterRepository;
import com.sealhackathon.api.chapters.value_object.ChapterStatus;
import com.sealhackathon.api.config.CriteriaCloneSourceUnlinkMigration;
import com.sealhackathon.api.criteria.entity.Criteria;
import com.sealhackathon.api.criteria.repository.CriteriaRepository;
import com.sealhackathon.api.criteria.value_object.CriteriaType;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.hackathons.value_object.Season;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.rounds.value_object.LateSubmissionPolicy;
import com.sealhackathon.api.rounds.value_object.RoundType;
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
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.profiles.active=test",
        "spring.datasource.url=jdbc:h2:mem:gd1-gates;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;NON_KEYWORDS=YEAR",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "security.jwt.secret=12345678901234567890123456789012",
        "security.jwt.enabled=true",
        "app.storage.type=local",
        "app.storage.local-dir=target/test-uploads/gd1-gates"
})
class Gd1ReadinessGateIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private ChapterRepository chapterRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private HackathonRepository hackathonRepository;
    @Autowired private RoundRepository roundRepository;
    @Autowired private TrackRepository trackRepository;
    @Autowired private CriteriaRepository criteriaRepository;

    @MockitoBean
    private CriteriaCloneSourceUnlinkMigration criteriaCloneSourceUnlinkMigration;

    private String suffix;
    private Chapter chapter;
    private User coordinator;

    @BeforeEach
    void setUp() {
        suffix = UUID.randomUUID().toString().substring(0, 8);
        chapter = chapterRepository.save(Chapter.builder()
                .name("G1 Gate " + suffix)
                .code("G1-" + suffix)
                .status(ChapterStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build());
        coordinator = saveUser("coord.g1." + suffix + "@it.test", UserRole.COORDINATOR, "Coordinator@dev1");
    }

    @Test
    @DisplayName("slug: seal-gd1-incomplete — readiness NOT_READY (no rounds)")
    void incompleteDraftHasNoRoundsBlocker() throws Exception {
        Hackathon hackathon = hackathonRepository.save(Hackathon.builder()
                .name("Incomplete " + suffix)
                .slug("it-gd1-incomplete-" + suffix)
                .season(Season.Spring)
                .year(2026)
                .status(HackathonStatus.DRAFT)
                .registrationStart(LocalDate.now().minusDays(7).atTime(0, 0))
                .registrationEnd(LocalDate.now().plusDays(7).atTime(23, 59))
                .eventStart(LocalDate.now().plusDays(14))
                .eventEnd(LocalDate.now().plusDays(14))
                .individualRankingEnabled(false)
                .createdBy(coordinator)
                .build());

        String token = login(coordinator.getEmail(), "Coordinator@dev1");
        MvcResult result = mockMvc.perform(get("/api/v1/hackathons/{id}/readiness", hackathon.getId())
                        .param("target", "ONGOING")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode blockers = readJson(result).path("data").path("blockers");
        assertThat(blockers.isArray()).isTrue();
        assertThat(blockers).isNotEmpty();
    }

    @Test
    @DisplayName("slug: seal-gd1-prelim-only — MISSING_FINAL_ROUND (G1-N08)")
    void prelimOnlyMissingFinalRound() throws Exception {
        Hackathon hackathon = hackathonRepository.save(Hackathon.builder()
                .name("Prelim only " + suffix)
                .slug("it-gd1-prelim-only-" + suffix)
                .season(Season.Spring)
                .year(2026)
                .status(HackathonStatus.DRAFT)
                .registrationStart(LocalDate.now().minusDays(7).atTime(0, 0))
                .registrationEnd(LocalDate.now().plusDays(7).atTime(23, 59))
                .eventStart(LocalDate.now().plusDays(14))
                .eventEnd(LocalDate.now().plusDays(14))
                .individualRankingEnabled(false)
                .createdBy(coordinator)
                .build());

        Round prelim = roundRepository.save(Round.builder()
                .hackathon(hackathon)
                .name("Vòng Sơ loại")
                .isFinal(false)
                .roundType(RoundType.PRELIMINARY)
                .examAt(LocalDateTime.now().plusDays(10))
                .submissionOpen(LocalDateTime.now())
                .submissionDeadline(LocalDateTime.now().plusDays(9))
                .lateSubmissionPolicy(LateSubmissionPolicy.ALLOW_LATE_PENDING)
                .build());

        Track track = trackRepository.save(Track.builder()
                .round(prelim)
                .name("Track 1")
                .status(TrackStatus.OPEN)
                .sequenceOrder(1)
                .maxTeamsPerGroup(8)
                .build());

        criteriaRepository.save(Criteria.builder()
                .track(track)
                .name("Tech")
                .type(CriteriaType.TECHNICAL)
                .weight(1.0f)
                .maxScore(10)
                .displayOrder(1)
                .build());

        String token = login(coordinator.getEmail(), "Coordinator@dev1");
        MvcResult result = mockMvc.perform(get("/api/v1/hackathons/{id}/readiness", hackathon.getId())
                        .param("target", "ONGOING")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        boolean hasMissingFinal = StreamSupport.stream(
                        readJson(result).path("data").path("blockers").spliterator(), false)
                .anyMatch(b -> "MISSING_FINAL_ROUND".equals(b.path("code").asText()));
        assertThat(hasMissingFinal).isTrue();
    }

    @Test
    @DisplayName("slug: seal-gd1-event-order-bad — POST WORKSHOP without KICKOFF (G1-N01)")
    void eventOrderBadRejectsWorkshopWithoutKickoff() throws Exception {
        Hackathon hackathon = hackathonRepository.save(Hackathon.builder()
                .name("Event order bad " + suffix)
                .slug("it-gd1-event-bad-" + suffix)
                .season(Season.Spring)
                .year(2026)
                .status(HackathonStatus.ONGOING)
                .registrationStart(LocalDate.now().minusDays(30).atTime(0, 0))
                .registrationEnd(LocalDate.now().plusDays(7).atTime(23, 59))
                .eventStart(LocalDate.now().plusDays(14))
                .eventEnd(LocalDate.now().plusDays(14))
                .individualRankingEnabled(false)
                .createdBy(coordinator)
                .build());

        String token = login(coordinator.getEmail(), "Coordinator@dev1");
        MvcResult eventResult = mockMvc.perform(post("/api/v1/hackathons/{id}/events", hackathon.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "WORKSHOP",
                                  "title": "WS probe",
                                  "startsAt": "2026-08-01T10:00:00",
                                  "endsAt": "2026-08-01T12:00:00"
                                }
                                """))
                .andReturn();

        assertThat(eventResult.getResponse().getStatus()).isBetween(400, 422);
        assertThat(readJson(eventResult).path("error").path("code").asText())
                .isEqualTo("EVENT_KICKOFF_MISSING");
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
