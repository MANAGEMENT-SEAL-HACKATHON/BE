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
import com.sealhackathon.api.teams.entity.TeamMember;
import com.sealhackathon.api.teams.entity.TeamMemberId;
import com.sealhackathon.api.teams.repository.TeamMemberRepository;
import com.sealhackathon.api.teams.value_object.TeamMemberRole;
import com.sealhackathon.api.teams.value_object.TeamMemberStatus;
import com.sealhackathon.api.submissions.repository.SubmissionRepository;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration GĐ4 → GĐ6: publish/advance, final activate, lock → PENDING_CONFIRM, prize, confirm, export.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.profiles.active=test",
        "spring.datasource.url=jdbc:h2:mem:gd4gd6;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;NON_KEYWORDS=YEAR",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "security.jwt.secret=12345678901234567890123456789012",
        "security.jwt.enabled=true",
        "app.storage.type=local",
        "app.storage.local-dir=target/test-uploads/gd4-gd6",
        "app.submission.github-public-check-enabled=false"
})
class Gd4ToGd6FlowIntegrationTest {

    /** Magic bytes %PDF- — bắt buộc để SubmissionSlideStorage.validatePdf chấp nhận. */
    private static final byte[] VALID_PDF_BYTES = {0x25, 0x50, 0x44, 0x46, 0x2D, 0x01, 0x02, 0x03};

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
    @Autowired private TeamRepository teamRepository;
    @Autowired private TeamMemberRepository teamMemberRepository;
    @Autowired private SubmissionRepository submissionRepository;

    @MockitoBean
    private CriteriaCloneSourceUnlinkMigration criteriaCloneSourceUnlinkMigration;

    private String suffix;
    private User coordinator;
    private User student;
    private User prelimJudge;
    private User finalJudge;
    private Hackathon hackathon;
    private Round prelimRound;
    private Round finalRound;
    private Track track;
    private Criteria prelimCriterion;
    private Criteria finalCriterion;
    private Team team;

    @BeforeEach
    void setUp() {
        suffix = UUID.randomUUID().toString().substring(0, 8);
        Chapter chapter = chapterRepository.save(Chapter.builder()
                .name("IT " + suffix)
                .code("IT-" + suffix)
                .status(ChapterStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build());

        coordinator = saveUser("coord." + suffix + "@it.test", UserRole.COORDINATOR, UserType.INTERNAL, "Coordinator@dev1", chapter);
        student = saveUser("student." + suffix + "@it.test", UserRole.STUDENT, UserType.INTERNAL, "Student@dev1", chapter);
        prelimJudge = saveUser("judge." + suffix + "@it.test", UserRole.JUDGE, UserType.INTERNAL, "Judge@dev1", chapter);
        finalJudge = saveUser("guest." + suffix + "@ext.test", UserRole.JUDGE, UserType.EXTERNAL, "GuestJudge@dev1", chapter);

        LocalDateTime now = LocalDateTime.now();
        hackathon = hackathonRepository.save(Hackathon.builder()
                .name("IT GĐ4-GĐ6 " + suffix)
                .slug("it-gd46-" + suffix)
                .season(Season.Spring)
                .year(2026)
                .status(HackathonStatus.ONGOING)
                .registrationStart(LocalDate.now().minusDays(30))
                .registrationEnd(LocalDate.now().minusDays(1))
                .eventStart(LocalDate.now())
                .eventEnd(LocalDate.now().plusDays(7))
                .wildcardEnabled(false)
                .individualRankingEnabled(false)
                .createdBy(coordinator)
                .createdAt(now)
                .updatedAt(now)
                .build());

        prelimRound = roundRepository.save(Round.builder()
                .hackathon(hackathon)
                .name("Vòng Sơ loại")
                .isFinal(false)
                .roundType(RoundType.PRELIMINARY)
                .examAt(now.minusHours(3))
                .submissionOpen(now.minusDays(1))
                .submissionDeadline(now.plusDays(1))
                .lateSubmissionPolicy(LateSubmissionPolicy.ALLOW_LATE_PENDING)
                .topNAdvance(1)
                .isActive(false)
                .scoringLocked(false)
                .isPublished(false)
                .build());

        finalRound = roundRepository.save(Round.builder()
                .hackathon(hackathon)
                .name("Chung kết")
                .isFinal(true)
                .roundType(RoundType.FINAL)
                .examAt(now.minusHours(1))
                .submissionOpen(now.minusHours(2))
                .submissionDeadline(now.plusDays(2))
                .lateSubmissionPolicy(LateSubmissionPolicy.HARD_LOCK)
                .isActive(false)
                .scoringLocked(false)
                .isPublished(false)
                .build());

        track = trackRepository.save(Track.builder()
                .round(prelimRound)
                .name("Track AI")
                .status(TrackStatus.OPEN)
                .sequenceOrder(1)
                .maxTeamsPerGroup(8)
                .problemStatementStorageKey("tracks/test-problem-" + suffix + ".pdf")
                .problemStatementOriginalFilename("test-problem.pdf")
                .build());

        prelimCriterion = criteriaRepository.save(Criteria.builder()
                .track(track)
                .name("Technical")
                .type(CriteriaType.TECHNICAL)
                .weight(1.0f)
                .maxScore(10)
                .displayOrder(1)
                .build());

        finalCriterion = criteriaRepository.save(Criteria.builder()
                .round(finalRound)
                .name("Final Technical")
                .type(CriteriaType.TECHNICAL)
                .weight(1.0f)
                .maxScore(10)
                .displayOrder(1)
                .build());

        judgeAssignmentRepository.save(JudgeAssignment.builder()
                .judge(prelimJudge)
                .track(track)
                .assignmentType(JudgeAssignmentType.HEAD)
                .assignedAt(LocalDateTime.now())
                .assignedBy(coordinator)
                .build());

        team = teamRepository.save(Team.builder()
                .hackathon(hackathon)
                .teamName("IT-TEAM-" + suffix)
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
    void activatePrelim_secondCallIsIdempotent() throws Exception {
        preparePrelimWithSubmissionAndScore();
        String coordToken = login(coordinator.getEmail(), "Coordinator@dev1");

        mockMvc.perform(patch("/api/v1/rounds/{id}/activate", prelimRound.getId())
                        .header("Authorization", "Bearer " + coordToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":\"first\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/rounds/{id}/activate", prelimRound.getId())
                        .header("Authorization", "Bearer " + coordToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":\"idempotent\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void gd4ThroughGd6_publishAdvanceConfirmAndExport() throws Exception {
        int prelimSubmissionId = preparePrelimWithSubmissionAndScore();
        String coordToken = login(coordinator.getEmail(), "Coordinator@dev1");
        String studentToken = login(student.getEmail(), "Student@dev1");
        String prelimJudgeToken = login(prelimJudge.getEmail(), "Judge@dev1");
        String finalJudgeToken = login(finalJudge.getEmail(), "GuestJudge@dev1");

        mockMvc.perform(patch("/api/v1/rounds/{id}/activate", prelimRound.getId())
                        .header("Authorization", "Bearer " + coordToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":\"it-gd46\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/rounds/{id}/lock-scoring", prelimRound.getId())
                        .header("Authorization", "Bearer " + coordToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/rounds/{id}/publish", prelimRound.getId())
                        .header("Authorization", "Bearer " + coordToken))
                .andExpect(status().isOk());

        String advanceBody = """
                {"advancedTeamIds": [%d], "eliminatedTeamIds": [], "note": "advance to final"}
                """.formatted(team.getId());
        mockMvc.perform(post("/api/v1/rounds/{id}/advance", prelimRound.getId())
                        .header("Authorization", "Bearer " + coordToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(advanceBody))
                .andExpect(status().isOk());

        String assignJudges = """
                {"judgeIds": [%d], "assignmentType": "FINAL_EXTERNAL"}
                """.formatted(finalJudge.getId());
        mockMvc.perform(post("/api/v1/rounds/{id}/judge-assignments", finalRound.getId())
                        .header("Authorization", "Bearer " + coordToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assignJudges))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/rounds/{id}/activate", finalRound.getId())
                        .header("Authorization", "Bearer " + coordToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":\"final-active\"}"))
                .andExpect(status().isOk());

        // GĐ5: chưa nộp → 200 + data omitted/null (ApiResponse NON_NULL → field absent)
        mockMvc.perform(get("/api/v1/me/submission")
                        .param("teamId", team.getId().toString())
                        .param("roundId", finalRound.getId().toString())
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").doesNotExist());

        MockMultipartFile finalSlide = new MockMultipartFile(
                "slideFile", "slide-chung-ket.pdf", "application/pdf", VALID_PDF_BYTES);
        MvcResult finalSubmit = mockMvc.perform(multipart("/api/v1/submissions")
                        .file(finalSlide)
                        .param("teamId", team.getId().toString())
                        .param("roundId", finalRound.getId().toString()) // CK: roundId only, no trackId
                        .param("repoUrl", "https://github.com/octocat/final")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.slideFile").exists())
                .andExpect(jsonPath("$.data.slideDownloadPath").isNotEmpty())
                .andReturn();
        int finalSubmissionId = readJson(finalSubmit).path("data").path("id").asInt();
        assertThat(finalSubmissionId).isNotEqualTo(prelimSubmissionId);
        assertThat(submissionRepository.findById(prelimSubmissionId)).isPresent();
        assertThat(submissionRepository.findById(finalSubmissionId)).isPresent();
        assertThat(submissionRepository.findAll()).hasSizeGreaterThanOrEqualTo(2);

        mockMvc.perform(get("/api/v1/me/submission")
                        .param("teamId", team.getId().toString())
                        .param("roundId", finalRound.getId().toString())
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.submissionId").exists())
                .andExpect(jsonPath("$.data.hasSlide").value(true));

        // CK activate đã stamp problemReleasedAt — chỉ cần close-early
        mockMvc.perform(post("/api/v1/rounds/{id}/close-submission-early", finalRound.getId())
                        .header("Authorization", "Bearer " + coordToken))
                .andExpect(status().isOk());

        String shuffleFinal = "{\"roundId\": %d}".formatted(finalRound.getId());
        mockMvc.perform(post("/api/v1/presentation/queue/shuffle")
                        .header("Authorization", "Bearer " + coordToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(shuffleFinal))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/presentation/timer/start")
                        .param("roundId", finalRound.getId().toString())
                        .header("Authorization", "Bearer " + coordToken))
                .andExpect(status().isOk());

        String finalScoreBody = """
                {"submissionId": %d, "criterionId": %d, "scoreValue": 9}
                """.formatted(finalSubmissionId, finalCriterion.getId());
        mockMvc.perform(post("/api/v1/scores")
                        .header("Authorization", "Bearer " + finalJudgeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(finalScoreBody))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/presentation/timer/qa")
                        .param("roundId", finalRound.getId().toString())
                        .header("Authorization", "Bearer " + coordToken))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/presentation/timer/end")
                        .param("roundId", finalRound.getId().toString())
                        .header("Authorization", "Bearer " + coordToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"acknowledgeIncompleteScoring\":true}"))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/v1/presentation/queue/next")
                        .param("roundId", finalRound.getId().toString())
                        .header("Authorization", "Bearer " + coordToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentSubmissionId\": %d, \"acknowledgeIncompleteScoring\": true}"
                                .formatted(finalSubmissionId)))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/rounds/{id}/lock-scoring", finalRound.getId())
                        .header("Authorization", "Bearer " + coordToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        hackathon = hackathonRepository.findById(hackathon.getId()).orElseThrow();
        assertThat(hackathon.getStatus()).isEqualTo(HackathonStatus.PENDING_CONFIRM);

        String prizeBody = """
                {
                  "roundId": %d,
                  "teamId": %d,
                  "prizeName": "Giải Nhất",
                  "prizeRank": "FIRST"
                }
                """.formatted(finalRound.getId(), team.getId());
        mockMvc.perform(post("/api/v1/hackathons/{id}/prizes", hackathon.getId())
                        .header("Authorization", "Bearer " + coordToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(prizeBody))
                .andExpect(status().isCreated());

        mockMvc.perform(patch("/api/v1/hackathons/{id}/confirm", hackathon.getId())
                        .header("Authorization", "Bearer " + coordToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"confirm\": true, \"note\": \"IT confirm\"}"))
                .andExpect(status().isOk());

        hackathon = hackathonRepository.findById(hackathon.getId()).orElseThrow();
        assertThat(hackathon.getStatus()).isEqualTo(HackathonStatus.FINISHED);

        MvcResult exportResult = mockMvc.perform(post("/api/v1/hackathons/{id}/export-jobs", hackathon.getId())
                        .header("Authorization", "Bearer " + coordToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\": \"CSV_RANKINGS\"}"))
                .andExpect(status().isAccepted())
                .andReturn();
        assertThat(readJson(exportResult).path("data").path("id").isNumber()).isTrue();

        int rblJobId = readJson(mockMvc.perform(post("/api/v1/hackathons/{id}/export-jobs", hackathon.getId())
                        .header("Authorization", "Bearer " + coordToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\": \"ANONYMIZED_RBL\"}"))
                .andExpect(status().isAccepted())
                .andReturn()).path("data").path("id").asInt();

        MvcResult rblDownload = mockMvc.perform(get("/api/v1/export-jobs/{id}/download", rblJobId)
                        .header("Authorization", "Bearer " + coordToken))
                .andExpect(status().isOk())
                .andReturn();
        String rblCsv = rblDownload.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(rblCsv).contains("anonymized_judge_id");
        assertThat(rblCsv.lines().count()).isGreaterThan(1);

        int fullJobId = readJson(mockMvc.perform(post("/api/v1/hackathons/{id}/export-jobs", hackathon.getId())
                        .header("Authorization", "Bearer " + coordToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\": \"FULL_REPORT\"}"))
                .andExpect(status().isAccepted())
                .andReturn()).path("data").path("id").asInt();

        MvcResult fullDownload = mockMvc.perform(get("/api/v1/export-jobs/{id}/download", fullJobId)
                        .header("Authorization", "Bearer " + coordToken))
                .andExpect(status().isOk())
                .andReturn();
        String fullCsv = fullDownload.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(fullCsv).contains("# SECTION: RANKINGS");
        assertThat(fullCsv).contains("# SECTION: RBL_VARIANCE_ANONYMIZED");
        assertThat(fullCsv.lines().count()).isGreaterThan(10);

        MvcResult journeyResult = mockMvc.perform(get("/api/v1/teams/{id}/journey", team.getId())
                        .header("Authorization", "Bearer " + coordToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode steps = readJson(journeyResult).path("data").path("steps");
        assertThat(steps.isArray()).isTrue();
        assertThat(steps.size()).isGreaterThanOrEqualTo(1);

        // prelim submission id used only to satisfy compiler — flow exercised above
        assertThat(prelimSubmissionId).isPositive();
        assertThat(prelimJudgeToken).isNotBlank();
    }

    @Test
    void submit_rejectsWhenNeitherTrackIdNorRoundId() throws Exception {
        String studentToken = login(student.getEmail(), "Student@dev1");
        MockMultipartFile slide = new MockMultipartFile(
                "slideFile", "orphan.pdf", "application/pdf", VALID_PDF_BYTES);
        mockMvc.perform(multipart("/api/v1/submissions")
                        .file(slide)
                        .param("teamId", team.getId().toString())
                        .param("repoUrl", "https://github.com/octocat/orphan")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isUnprocessableEntity());
    }

    private int preparePrelimWithSubmissionAndScore() throws Exception {
        team.setIsLocked(true);
        teamRepository.save(team);

        String coordToken = login(coordinator.getEmail(), "Coordinator@dev1");
        String studentToken = login(student.getEmail(), "Student@dev1");
        String judgeToken = login(prelimJudge.getEmail(), "Judge@dev1");

        mockMvc.perform(patch("/api/v1/hackathons/{id}/lottery", hackathon.getId())
                        .header("Authorization", "Bearer " + coordToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roundId\": %d, \"assignments\": []}".formatted(prelimRound.getId())))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/rounds/{id}/activate", prelimRound.getId())
                        .header("Authorization", "Bearer " + coordToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":\"pre-score\"}"))
                .andExpect(status().isOk());

        MockMultipartFile slide = new MockMultipartFile(
                "slideFile", "prelim.pdf", "application/pdf", VALID_PDF_BYTES);
        MvcResult submit = mockMvc.perform(multipart("/api/v1/submissions")
                        .file(slide)
                        .param("teamId", team.getId().toString())
                        .param("trackId", track.getId().toString())
                        .param("repoUrl", "https://github.com/octocat/prelim")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.slideFile").exists())
                .andReturn();
        int submissionId = readJson(submit).path("data").path("id").asInt();

        mockMvc.perform(patch("/api/v1/rounds/{id}/release-problem", prelimRound.getId())
                        .header("Authorization", "Bearer " + coordToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/rounds/{id}/close-submission-early", prelimRound.getId())
                        .header("Authorization", "Bearer " + coordToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/presentation/queue/shuffle")
                        .header("Authorization", "Bearer " + coordToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roundId\": %d, \"trackIds\": [%d]}".formatted(prelimRound.getId(), track.getId())))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/presentation/timer/start")
                        .param("roundId", prelimRound.getId().toString())
                        .param("trackId", track.getId().toString())
                        .header("Authorization", "Bearer " + coordToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/scores")
                        .header("Authorization", "Bearer " + judgeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"submissionId": %d, "criterionId": %d, "scoreValue": 8}
                                """.formatted(submissionId, prelimCriterion.getId())))
                .andExpect(status().isCreated());

        // Gate 3 lock-scoring: slot must leave PRESENTING/WAITING → DONE
        mockMvc.perform(post("/api/v1/presentation/timer/qa")
                        .param("roundId", prelimRound.getId().toString())
                        .param("trackId", track.getId().toString())
                        .header("Authorization", "Bearer " + coordToken))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/presentation/timer/end")
                        .param("roundId", prelimRound.getId().toString())
                        .param("trackId", track.getId().toString())
                        .header("Authorization", "Bearer " + coordToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"acknowledgeIncompleteScoring\":true}"))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/v1/presentation/queue/next")
                        .param("roundId", prelimRound.getId().toString())
                        .param("trackId", track.getId().toString())
                        .header("Authorization", "Bearer " + coordToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentSubmissionId\": %d, \"acknowledgeIncompleteScoring\": true}"
                                .formatted(submissionId)))
                .andExpect(status().isOk());

        return submissionId;
    }

    private User saveUser(String email, UserRole role, UserType type, String rawPassword, Chapter chapter) {
        return userRepository.save(User.builder()
                .fullName(role.name() + " " + suffix)
                .email(email)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .role(role)
                .userType(type)
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
