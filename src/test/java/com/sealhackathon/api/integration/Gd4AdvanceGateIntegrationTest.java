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
import com.sealhackathon.api.judge_assignments.entity.JudgeAssignment;
import com.sealhackathon.api.judge_assignments.repository.JudgeAssignmentRepository;
import com.sealhackathon.api.judge_assignments.value_object.JudgeAssignmentType;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.profiles.active=test",
        "spring.datasource.url=jdbc:h2:mem:gd4-gates;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;NON_KEYWORDS=YEAR",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "security.jwt.secret=12345678901234567890123456789012",
        "security.jwt.enabled=true",
        "app.storage.type=local",
        "app.storage.local-dir=target/test-uploads/gd4-gates"
})
class Gd4AdvanceGateIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private ChapterRepository chapterRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private HackathonRepository hackathonRepository;
    @Autowired private RoundRepository roundRepository;
    @Autowired private TrackRepository trackRepository;
    @Autowired private CriteriaRepository criteriaRepository;
    @Autowired private JudgeAssignmentRepository judgeAssignmentRepository;

    @MockitoBean
    private CriteriaCloneSourceUnlinkMigration criteriaCloneSourceUnlinkMigration;

    private User coordinator;
    private Round prelim;
    private Round finalRound;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Chapter chapter = chapterRepository.save(Chapter.builder()
                .name("G4 " + suffix)
                .code("G4-" + suffix)
                .status(ChapterStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build());
        coordinator = saveUser(chapter, "coord.g4." + suffix + "@it.test", UserRole.COORDINATOR);
        User guestJudge = saveUser(chapter, "guest.g4." + suffix + "@ext.test", UserRole.JUDGE);

        Hackathon hackathon = hackathonRepository.save(Hackathon.builder()
                .name("G4 CK unpublished " + suffix)
                .slug("it-gd4-ck-unpub-" + suffix)
                .season(Season.Spring)
                .year(2026)
                .status(HackathonStatus.ONGOING)
                .registrationStart(LocalDate.now().minusDays(60))
                .registrationEnd(LocalDate.now().minusDays(30))
                .eventStart(LocalDate.now())
                .eventEnd(LocalDate.now())
                .wildcardEnabled(false)
                .individualRankingEnabled(false)
                .createdBy(coordinator)
                .build());

        prelim = roundRepository.save(Round.builder()
                .hackathon(hackathon)
                .name("Prelim")
                .isFinal(false)
                .roundType(RoundType.PRELIMINARY)
                .examAt(LocalDateTime.now().minusDays(2))
                .submissionOpen(LocalDateTime.now().minusDays(3))
                .submissionDeadline(LocalDateTime.now().minusDays(1))
                .lateSubmissionPolicy(LateSubmissionPolicy.ALLOW_LATE_PENDING)
                .isActive(false)
                .scoringLocked(true)
                .scoringLockedAt(LocalDateTime.now().minusHours(2))
                .isPublished(false)
                .build());

        finalRound = roundRepository.save(Round.builder()
                .hackathon(hackathon)
                .name("Final")
                .isFinal(true)
                .roundType(RoundType.FINAL)
                .examAt(LocalDateTime.now().plusDays(1))
                .submissionOpen(LocalDateTime.now())
                .submissionDeadline(LocalDateTime.now().plusDays(1))
                .lateSubmissionPolicy(LateSubmissionPolicy.HARD_LOCK)
                .isActive(false)
                .build());

        Track track = trackRepository.save(Track.builder()
                .round(prelim)
                .name("Track")
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

        criteriaRepository.save(Criteria.builder()
                .round(finalRound)
                .name("Final Tech")
                .type(CriteriaType.TECHNICAL)
                .weight(1.0f)
                .maxScore(10)
                .displayOrder(1)
                .build());

        judgeAssignmentRepository.save(JudgeAssignment.builder()
                .judge(guestJudge)
                .round(finalRound)
                .assignmentType(JudgeAssignmentType.FINAL_EXTERNAL)
                .assignedAt(LocalDateTime.now())
                .assignedBy(coordinator)
                .build());
    }

    @Test
    @DisplayName("slug: seal-gd4-ck-unpublished — RESULT_NOT_PUBLISHED (G4-N01)")
    void activateFinalWithoutPrelimPublishFails() throws Exception {
        String token = login(coordinator.getEmail(), "Coordinator@dev1");
        MvcResult result = mockMvc.perform(patch("/api/v1/rounds/{id}/activate", finalRound.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":\"gate-test\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andReturn();

        assertThat(readJson(result).path("error").path("code").asText()).isEqualTo("RESULT_NOT_PUBLISHED");
    }

    @Test
    @DisplayName("slug: seal-gd4-tiebreak-gate — prelim locked unpublished blocks advance readiness")
    void prelimLockedUnpublishedHasNoPublishTimestamp() {
        assertThat(prelim.getScoringLocked()).isTrue();
        assertThat(prelim.getIsPublished()).isFalse();
    }

    private User saveUser(Chapter chapter, String email, UserRole role) {
        return userRepository.save(User.builder()
                .fullName(role.name())
                .email(email)
                .passwordHash(passwordEncoder.encode("Coordinator@dev1"))
                .role(role)
                .userType(role == UserRole.JUDGE ? UserType.EXTERNAL : UserType.INTERNAL)
                .status(UserStatus.APPROVED)
                .chapter(chapter)
                .isTempAccount(role == UserRole.JUDGE)
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
