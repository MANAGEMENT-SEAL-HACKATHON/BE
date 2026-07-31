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
import com.sealhackathon.api.mentors.entity.MentorAssignment;
import com.sealhackathon.api.mentors.repository.MentorAssignmentRepository;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration GĐ2 → GĐ3: lottery, submit, shuffle, timer, scoring gate (PRESENTING).
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.profiles.active=test",
        "spring.datasource.url=jdbc:h2:mem:gd2gd3;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;NON_KEYWORDS=YEAR",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "security.jwt.secret=12345678901234567890123456789012",
        "security.jwt.enabled=true",
        "app.storage.type=local",
        "app.storage.local-dir=target/test-uploads/gd2-gd3",
        "app.submission.github-public-check-enabled=false"
})
class Gd2Gd3FlowIntegrationTest {

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
    @Autowired private MentorAssignmentRepository mentorAssignmentRepository;
    @Autowired private TeamRepository teamRepository;
    @Autowired private TeamMemberRepository teamMemberRepository;

    @MockitoBean
    private CriteriaCloneSourceUnlinkMigration criteriaCloneSourceUnlinkMigration;

    private String suffix;
    private Chapter chapter;
    private User coordinator;
    private User student;
    private User student2;
    private User judge;
    private User judge2;
    private User mentor;
    private Hackathon hackathon;
    private Round prelimRound;
    private Track track;
    private Criteria criterion;
    private Team team;
    private Team team2;

    @BeforeEach
    void setUp() {
        suffix = UUID.randomUUID().toString().substring(0, 8);
        chapter = chapterRepository.save(Chapter.builder()
                .name("IT Chapter " + suffix)
                .code("IT-" + suffix)
                .status(ChapterStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build());

        coordinator = saveUser("coord." + suffix + "@it.test", UserRole.COORDINATOR, "Coordinator@dev1");
        student = saveUser("student." + suffix + "@it.test", UserRole.STUDENT, "Student@dev1");
        student2 = saveUser("student2." + suffix + "@it.test", UserRole.STUDENT, "Student@dev1");
        judge = saveUser("judge." + suffix + "@it.test", UserRole.JUDGE, "Judge@dev1");
        judge2 = saveUser("judge2." + suffix + "@it.test", UserRole.JUDGE, "Judge@dev1");
        mentor = saveUser("mentor." + suffix + "@it.test", UserRole.MENTOR, "Mentor@dev1");

        LocalDateTime now = LocalDateTime.now();
        hackathon = hackathonRepository.save(Hackathon.builder()
                .name("IT GĐ2-GĐ3 " + suffix)
                .slug("it-gd23-" + suffix)
                .season(Season.Spring)
                .year(2026)
                .status(HackathonStatus.ONGOING)
                .registrationStart(LocalDate.now().minusDays(30).atTime(0, 0))
                .registrationEnd(LocalDate.now().minusDays(1).atTime(23, 59))
                .eventStart(LocalDate.now())
                .eventEnd(LocalDate.now().plusDays(7))
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
                .examAt(now.minusHours(1))
                .submissionOpen(now.minusDays(1))
                .submissionDeadline(now.plusDays(1))
                .lateSubmissionPolicy(LateSubmissionPolicy.ALLOW_LATE_PENDING)
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

        criterion = criteriaRepository.save(Criteria.builder()
                .track(track)
                .name("Technical")
                .type(CriteriaType.TECHNICAL)
                .weight(1.0f)
                .maxScore(10)
                .displayOrder(1)
                .build());

        judgeAssignmentRepository.save(JudgeAssignment.builder()
                .judge(judge)
                .track(track)
                .assignmentType(JudgeAssignmentType.HEAD)
                .assignedAt(LocalDateTime.now())
                .assignedBy(coordinator)
                .build());

        mentorAssignmentRepository.save(MentorAssignment.builder()
                .mentor(mentor)
                .track(track)
                .assignedAt(LocalDateTime.now())
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

        team2 = teamRepository.save(Team.builder()
                .hackathon(hackathon)
                .teamName("IT-TEAM2-" + suffix)
                .leader(student2)
                .chapter(chapter)
                .status(TeamStatus.ACTIVE)
                .isLocked(false)
                .build());

        teamMemberRepository.save(TeamMember.builder()
                .id(new TeamMemberId(team2.getId(), student2.getId()))
                .team(team2)
                .user(student2)
                .roleInTeam(TeamMemberRole.LEADER)
                .status(TeamMemberStatus.ACCEPTED)
                .joinedAt(LocalDateTime.now())
                .build());
    }

    @Test
    void lottery_rejectsExplicitAssignmentWhenTeamNotLocked() throws Exception {
        String coordToken = login(coordinator.getEmail(), "Coordinator@dev1");

        String body = """
                {
                  "roundId": %d,
                  "assignments": [{
                    "teamId": %d,
                    "trackId": %d,
                    "assignedGroup": "Bảng A"
                  }]
                }
                """.formatted(prelimRound.getId(), team.getId(), track.getId());

        MvcResult result = mockMvc.perform(patch("/api/v1/hackathons/{id}/lottery", hackathon.getId())
                        .header("Authorization", "Bearer " + coordToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnprocessableEntity())
                .andReturn();

        assertThat(readJson(result).path("error").path("code").asText())
                .isEqualTo("ACTIVE_TEAMS_NOT_LOCKED");
    }

    @Test
    void gd2ToGd3_lotteryActivateMultipartSubmitAndSlide() throws Exception {
        lockAllActiveTeams();

        String coordToken = login(coordinator.getEmail(), "Coordinator@dev1");
        String studentToken = login(student.getEmail(), "Student@dev1");

        // GĐ2 — lottery
        String lotteryBody = """
                {"roundId": %d, "assignments": []}
                """.formatted(prelimRound.getId());
        MvcResult lotteryResult = mockMvc.perform(patch("/api/v1/hackathons/{id}/lottery", hackathon.getId())
                        .header("Authorization", "Bearer " + coordToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(lotteryBody))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(readJson(lotteryResult).path("data").path("assignedCount").asInt()).isEqualTo(2);

        // GĐ3 — activate prelim
        mockMvc.perform(patch("/api/v1/rounds/{id}/activate", prelimRound.getId())
                        .header("Authorization", "Bearer " + coordToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":\"it-test\"}"))
                .andExpect(status().isOk());

        // GĐ3 — multipart submit
        MockMultipartFile slide = new MockMultipartFile(
                "slideFile",
                "team-alpha-pitch-v2.pdf",
                "application/pdf",
                "%PDF-1.4\n".getBytes(StandardCharsets.UTF_8));

        MvcResult submitResult = mockMvc.perform(multipart("/api/v1/submissions")
                        .file(slide)
                        .param("teamId", team.getId().toString())
                        .param("trackId", track.getId().toString())
                        .param("repoUrl", "https://github.com/octocat/Hello-World")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode submitData = readJson(submitResult).path("data");
        int submissionId = submitData.path("id").asInt();
        assertThat(submitData.path("slideFile").asText()).isEqualTo("team-alpha-pitch-v2.pdf");
        assertThat(submitData.path("slideDownloadPath").asText())
                .isEqualTo("/api/v1/submissions/" + submitData.path("id").asInt() + "/slide");

        // GET slide PDF (inline view)
        mockMvc.perform(get("/api/v1/submissions/{id}/slide", submissionId)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk());

        // GET slide PDF (download attachment)
        MvcResult downloadResult = mockMvc.perform(get("/api/v1/submissions/{id}/slide", submissionId)
                        .param("download", "true")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(downloadResult.getResponse().getHeader("Content-Disposition"))
                .contains("attachment")
                .contains("team-alpha-pitch-v2.pdf");
    }

    @Test
    void judgeSubmissions_areAnonymous() throws Exception {
        lockAllActiveTeams();
        runLotteryAndActivateAndSubmit();

        String judgeToken = login(judge.getEmail(), "Judge@dev1");
        MvcResult result = mockMvc.perform(get("/api/v1/me/judge/submissions")
                        .param("roundId", prelimRound.getId().toString())
                        .param("trackId", track.getId().toString())
                        .header("Authorization", "Bearer " + judgeToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode items = readJson(result).path("data");
        assertThat(items.isArray()).isTrue();
        assertThat(items).isNotEmpty();
        assertThat(items.get(0).path("displayCode").asText()).startsWith("#");
        assertThat(items.get(0).has("teamName")).isFalse();
    }

    @Test
    void scoring_rejectsWhenNoPresentingSlot() throws Exception {
        lockAllActiveTeams();
        int submissionId = runLotteryAndActivateAndSubmit();

        String judgeToken = login(judge.getEmail(), "Judge@dev1");
        String scoreBody = """
                {
                  "submissionId": %d,
                  "criterionId": %d,
                  "scoreValue": 8
                }
                """.formatted(submissionId, criterion.getId());

        MvcResult result = mockMvc.perform(post("/api/v1/scores")
                        .header("Authorization", "Bearer " + judgeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(scoreBody))
                .andExpect(status().isUnprocessableEntity())
                .andReturn();

        assertThat(readJson(result).path("error").path("code").asText())
                .isEqualTo("SCORING_NOT_OPEN");
    }

    @Test
    void shuffle_createsSlotsWithFirstPresenting() throws Exception {
        lockTeamAndSubmit();
        String coordToken = login(coordinator.getEmail(), "Coordinator@dev1");

        releaseProblemThenCloseEarly(coordToken, prelimRound.getId());

        MvcResult shuffleResult = mockMvc.perform(post("/api/v1/presentation/queue/shuffle")
                        .header("Authorization", "Bearer " + coordToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(shuffleBody()))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode tracks = readJson(shuffleResult).path("data").path("tracks");
        assertThat(tracks.isArray()).isTrue();
        assertThat(tracks).hasSize(1);
        assertThat(tracks.get(0).path("trackId").asInt()).isEqualTo(track.getId());
        assertThat(tracks.get(0).path("slotCount").asInt()).isEqualTo(1);
        assertThat(tracks.get(0).path("shuffled").asBoolean()).isTrue();

        MvcResult queueResult = mockMvc.perform(get("/api/v1/presentation/queue")
                        .param("roundId", prelimRound.getId().toString())
                        .param("trackId", track.getId().toString())
                        .header("Authorization", "Bearer " + coordToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode items = readJson(queueResult).path("data").path("tracks").get(0).path("items");
        assertThat(items).hasSize(1);
        assertThat(items.get(0).path("status").asText()).isEqualTo("PRESENTING");
    }

    @Test
    void timer_startPauseResume_afterShuffle() throws Exception {
        PresentationCtx ctx = preparePresentationWithShuffle();

        MvcResult startResult = mockMvc.perform(post("/api/v1/presentation/timer/start")
                        .param("roundId", prelimRound.getId().toString())
                        .param("trackId", track.getId().toString())
                        .header("Authorization", "Bearer " + ctx.controllerToken()))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(readJson(startResult).path("data").path("timer").path("phase").asText())
                .isEqualTo("PRESENTING");

        MvcResult pauseResult = mockMvc.perform(post("/api/v1/presentation/timer/pause")
                        .param("roundId", prelimRound.getId().toString())
                        .param("trackId", track.getId().toString())
                        .header("Authorization", "Bearer " + ctx.controllerToken()))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(readJson(pauseResult).path("data").path("timer").path("phase").asText())
                .isEqualTo("PAUSED");

        MvcResult resumeResult = mockMvc.perform(post("/api/v1/presentation/timer/resume")
                        .param("roundId", prelimRound.getId().toString())
                        .param("trackId", track.getId().toString())
                        .header("Authorization", "Bearer " + ctx.controllerToken()))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(readJson(resumeResult).path("data").path("timer").path("phase").asText())
                .isEqualTo("PRESENTING");
    }

    @Test
    void scoring_succeedsWhenSlotPresenting_afterShuffle() throws Exception {
        PresentationCtx ctx = preparePresentationWithShuffle();

        mockMvc.perform(post("/api/v1/presentation/timer/start")
                        .param("roundId", prelimRound.getId().toString())
                        .param("trackId", track.getId().toString())
                        .header("Authorization", "Bearer " + ctx.controllerToken()))
                .andExpect(status().isOk());

        String scoreBody = """
                {
                  "submissionId": %d,
                  "criterionId": %d,
                  "scoreValue": 8.5
                }
                """.formatted(ctx.submissionId(), criterion.getId());

        MvcResult result = mockMvc.perform(post("/api/v1/scores")
                        .header("Authorization", "Bearer " + ctx.judgeToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(scoreBody))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode data = readJson(result).path("data");
        assertThat(data.path("submissionId").asInt()).isEqualTo(ctx.submissionId());
        assertThat(data.path("criterionId").asInt()).isEqualTo(criterion.getId());
        assertThat(data.path("scoreValue").asDouble()).isEqualTo(8.5);
    }

    @Test
    void queue_next_rejectsWhenNoScores() throws Exception {
        TwoTeamCtx ctx = prepareTwoTeamPresentationWithShuffle();

        mockMvc.perform(post("/api/v1/presentation/timer/start")
                        .param("roundId", prelimRound.getId().toString())
                        .param("trackId", track.getId().toString())
                        .header("Authorization", "Bearer " + ctx.controllerToken()))
                .andExpect(status().isOk());

        advancePresentingSlotToEnded(ctx.controllerToken());

        String nextBody = """
                {"currentSubmissionId": %d}
                """.formatted(ctx.firstSubmissionId());

        MvcResult result = mockMvc.perform(patch("/api/v1/presentation/queue/next")
                        .param("roundId", prelimRound.getId().toString())
                        .param("trackId", track.getId().toString())
                        .header("Authorization", "Bearer " + ctx.controllerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(nextBody))
                .andExpect(status().isUnprocessableEntity())
                .andReturn();

        assertThat(readJson(result).path("error").path("code").asText())
                .isEqualTo("SCORING_INCOMPLETE_BEFORE_NEXT");
    }

    @Test
    void queue_next_transitionsToSetup() throws Exception {
        TwoTeamCtx ctx = prepareTwoTeamPresentationWithShuffle();

        mockMvc.perform(post("/api/v1/presentation/timer/start")
                        .param("roundId", prelimRound.getId().toString())
                        .param("trackId", track.getId().toString())
                        .header("Authorization", "Bearer " + ctx.controllerToken()))
                .andExpect(status().isOk());

        String scoreBody = """
                {
                  "submissionId": %d,
                  "criterionId": %d,
                  "scoreValue": 7
                }
                """.formatted(ctx.firstSubmissionId(), criterion.getId());

        mockMvc.perform(post("/api/v1/scores")
                        .header("Authorization", "Bearer " + ctx.judgeToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(scoreBody))
                .andExpect(status().isCreated());

        advancePresentingSlotToEnded(ctx.controllerToken());

        String nextBody = """
                {"currentSubmissionId": %d}
                """.formatted(ctx.firstSubmissionId());

        MvcResult nextResult = mockMvc.perform(patch("/api/v1/presentation/queue/next")
                        .param("roundId", prelimRound.getId().toString())
                        .param("trackId", track.getId().toString())
                        .header("Authorization", "Bearer " + ctx.controllerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(nextBody))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode nextData = readJson(nextResult).path("data");
        assertThat(nextData.path("nextSubmissionId").asInt()).isEqualTo(ctx.secondSubmissionId());
        assertThat(nextData.path("completedSubmissionScoring").path("scoreCount").asLong()).isEqualTo(1);

        MvcResult queueResult = mockMvc.perform(get("/api/v1/presentation/queue")
                        .param("roundId", prelimRound.getId().toString())
                        .param("trackId", track.getId().toString())
                        .header("Authorization", "Bearer " + ctx.controllerToken()))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode items = readJson(queueResult).path("data").path("tracks").get(0).path("items");
        assertThat(items).hasSize(2);
        assertThat(items.get(0).path("status").asText()).isEqualTo("DONE");
        assertThat(items.get(1).path("status").asText()).isEqualTo("PRESENTING");
        assertThat(items.get(1).path("timer").path("phase").asText()).isEqualTo("SETUP");

        String scoreSecond = """
                {
                  "submissionId": %d,
                  "criterionId": %d,
                  "scoreValue": 6
                }
                """.formatted(ctx.secondSubmissionId(), criterion.getId());

        mockMvc.perform(post("/api/v1/scores")
                        .header("Authorization", "Bearer " + ctx.judgeToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(scoreSecond))
                .andExpect(status().isUnprocessableEntity());

        mockMvc.perform(post("/api/v1/presentation/timer/start")
                        .param("roundId", prelimRound.getId().toString())
                        .param("trackId", track.getId().toString())
                        .header("Authorization", "Bearer " + ctx.controllerToken()))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/scores")
                        .header("Authorization", "Bearer " + ctx.judgeToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(scoreSecond))
                .andExpect(status().isCreated());
    }

    @Test
    void timer_start_forbiddenForNonHeadJudge() throws Exception {
        assignSecondJudgeOnTrack();
        preparePresentationWithShuffle();
        String judge2Token = login(judge2.getEmail(), "Judge@dev1");

        mockMvc.perform(post("/api/v1/presentation/timer/start")
                        .param("roundId", prelimRound.getId().toString())
                        .param("trackId", track.getId().toString())
                        .header("Authorization", "Bearer " + judge2Token))
                .andExpect(status().isForbidden());
    }

    @Test
    void queue_next_requiresAcknowledgeWhenSecondJudgeHasNotScored() throws Exception {
        assignSecondJudgeOnTrack();
        PresentationCtx ctx = preparePresentationWithShuffle();

        mockMvc.perform(post("/api/v1/presentation/timer/start")
                        .param("roundId", prelimRound.getId().toString())
                        .param("trackId", track.getId().toString())
                        .header("Authorization", "Bearer " + ctx.controllerToken()))
                .andExpect(status().isOk());

        String scoreBody = """
                {
                  "submissionId": %d,
                  "criterionId": %d,
                  "scoreValue": 8
                }
                """.formatted(ctx.submissionId(), criterion.getId());

        mockMvc.perform(post("/api/v1/scores")
                        .header("Authorization", "Bearer " + ctx.judgeToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(scoreBody))
                .andExpect(status().isCreated());

        advancePresentingSlotToEnded(ctx.controllerToken());

        String nextBody = """
                {"currentSubmissionId": %d}
                """.formatted(ctx.submissionId());

        MvcResult blocked = mockMvc.perform(patch("/api/v1/presentation/queue/next")
                        .param("roundId", prelimRound.getId().toString())
                        .param("trackId", track.getId().toString())
                        .header("Authorization", "Bearer " + ctx.controllerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(nextBody))
                .andExpect(status().isUnprocessableEntity())
                .andReturn();

        assertThat(readJson(blocked).path("error").path("code").asText())
                .isEqualTo("SCORING_INCOMPLETE_BEFORE_NEXT");

        String acknowledgeBody = """
                {
                  "currentSubmissionId": %d,
                  "acknowledgeIncompleteScoring": true,
                  "forceAckReason": "integration test — judge chưa chốt điểm"
                }
                """.formatted(ctx.submissionId());

        MvcResult nextResult = mockMvc.perform(patch("/api/v1/presentation/queue/next")
                        .param("roundId", prelimRound.getId().toString())
                        .param("trackId", track.getId().toString())
                        .header("Authorization", "Bearer " + ctx.controllerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(acknowledgeBody))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode scoring = readJson(nextResult).path("data").path("completedSubmissionScoring");
        assertThat(scoring.path("judgesAssigned").asInt()).isEqualTo(2);
        assertThat(scoring.path("judgesScored").asInt()).isEqualTo(1);
        assertThat(scoring.path("incomplete").asBoolean()).isTrue();
    }

    private void assignSecondJudgeOnTrack() {
        judgeAssignmentRepository.save(JudgeAssignment.builder()
                .judge(judge2)
                .track(track)
                .assignmentType(JudgeAssignmentType.NORMAL)
                .assignedAt(LocalDateTime.now())
                .assignedBy(coordinator)
                .build());
    }

    /** Next chỉ cho phép khi phase ENDED (sau Q&A). */
    private void advancePresentingSlotToEnded(String controllerToken) throws Exception {
        mockMvc.perform(post("/api/v1/presentation/timer/qa")
                        .param("roundId", prelimRound.getId().toString())
                        .param("trackId", track.getId().toString())
                        .header("Authorization", "Bearer " + controllerToken))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/presentation/timer/end")
                        .param("roundId", prelimRound.getId().toString())
                        .param("trackId", track.getId().toString())
                        .header("Authorization", "Bearer " + controllerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"acknowledgeIncompleteScoring\":true}"))
                .andExpect(status().isOk());
    }

    private record PresentationCtx(int submissionId, String controllerToken, String judgeToken) {}

    private record TwoTeamCtx(int firstSubmissionId, int secondSubmissionId, String controllerToken, String judgeToken) {}

    private void lockTeamAndSubmit() throws Exception {
        lockAllActiveTeams();
        runLotteryAndActivateAndSubmit();
    }

    private PresentationCtx preparePresentationWithShuffle() throws Exception {
        int submissionId = lockTeamAndSubmitReturnId();
        String coordToken = login(coordinator.getEmail(), "Coordinator@dev1");
        String judgeToken = login(judge.getEmail(), "Judge@dev1");

        releaseProblemThenCloseEarly(coordToken, prelimRound.getId());

        mockMvc.perform(post("/api/v1/presentation/queue/shuffle")
                        .header("Authorization", "Bearer " + coordToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(shuffleBody()))
                .andExpect(status().isOk());

        return new PresentationCtx(submissionId, judgeToken, judgeToken);
    }

    private TwoTeamCtx prepareTwoTeamPresentationWithShuffle() throws Exception {
        int[] submissionIds = lockBothTeamsSubmitAndReturnIds();
        String coordToken = login(coordinator.getEmail(), "Coordinator@dev1");
        String judgeToken = login(judge.getEmail(), "Judge@dev1");

        releaseProblemThenCloseEarly(coordToken, prelimRound.getId());

        mockMvc.perform(post("/api/v1/presentation/queue/shuffle")
                        .header("Authorization", "Bearer " + coordToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(shuffleBody()))
                .andExpect(status().isOk());

        MvcResult queueResult = mockMvc.perform(get("/api/v1/presentation/queue")
                        .param("roundId", prelimRound.getId().toString())
                        .param("trackId", track.getId().toString())
                        .header("Authorization", "Bearer " + judgeToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode items = readJson(queueResult).path("data").path("tracks").get(0).path("items");
        assertThat(items).hasSize(2);
        int first = items.get(0).path("submissionId").asInt();
        int second = items.get(1).path("submissionId").asInt();
        assertThat(first).isIn(submissionIds[0], submissionIds[1]);
        assertThat(second).isIn(submissionIds[0], submissionIds[1]);

        return new TwoTeamCtx(first, second, judgeToken, judgeToken);
    }

    private int[] lockBothTeamsSubmitAndReturnIds() throws Exception {
        team.setIsLocked(true);
        team2.setIsLocked(true);
        teamRepository.save(team);
        teamRepository.save(team2);

        String coordToken = login(coordinator.getEmail(), "Coordinator@dev1");
        String studentToken = login(student.getEmail(), "Student@dev1");
        String student2Token = login(student2.getEmail(), "Student@dev1");

        String lotteryBody = """
                {
                  "roundId": %d,
                  "assignments": [
                    {"teamId": %d, "trackId": %d, "assignedGroup": "Bảng A"},
                    {"teamId": %d, "trackId": %d, "assignedGroup": "Bảng A"}
                  ]
                }
                """.formatted(
                prelimRound.getId(),
                team.getId(), track.getId(),
                team2.getId(), track.getId());

        mockMvc.perform(patch("/api/v1/hackathons/{id}/lottery", hackathon.getId())
                        .header("Authorization", "Bearer " + coordToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(lotteryBody))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/rounds/{id}/activate", prelimRound.getId())
                        .header("Authorization", "Bearer " + coordToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":\"it-two-teams\"}"))
                .andExpect(status().isOk());

        int id1 = submitSlide(studentToken, team.getId(), "team1.pdf");
        int id2 = submitSlide(student2Token, team2.getId(), "team2.pdf");
        return new int[] {id1, id2};
    }

    private int submitSlide(String studentToken, Integer teamId, String filename) throws Exception {
        MockMultipartFile slide = new MockMultipartFile(
                "slideFile", filename, "application/pdf",
                "%PDF-1.4\n".getBytes(StandardCharsets.UTF_8));

        MvcResult submitResult = mockMvc.perform(multipart("/api/v1/submissions")
                        .file(slide)
                        .param("teamId", teamId.toString())
                        .param("trackId", track.getId().toString())
                        .param("repoUrl", "https://github.com/octocat/Hello-World")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isCreated())
                .andReturn();

        return readJson(submitResult).path("data").path("id").asInt();
    }

    private String shuffleBody() {
        return """
                {
                  "roundId": %d,
                  "trackIds": [%d]
                }
                """.formatted(prelimRound.getId(), track.getId());
    }

    private void releaseProblemThenCloseEarly(String coordToken, Integer roundId) throws Exception {
        mockMvc.perform(patch("/api/v1/rounds/{id}/release-problem", roundId)
                        .header("Authorization", "Bearer " + coordToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/rounds/{id}/close-submission-early", roundId)
                        .header("Authorization", "Bearer " + coordToken))
                .andExpect(status().isOk());
    }

    private int lockTeamAndSubmitReturnId() throws Exception {
        lockAllActiveTeams();
        return runLotteryAndActivateAndSubmit();
    }

    private void lockAllActiveTeams() {
        team.setIsLocked(true);
        team2.setIsLocked(true);
        teamRepository.save(team);
        teamRepository.save(team2);
    }

    private int runLotteryAndActivateAndSubmit() throws Exception {
        String coordToken = login(coordinator.getEmail(), "Coordinator@dev1");
        String studentToken = login(student.getEmail(), "Student@dev1");

        mockMvc.perform(patch("/api/v1/hackathons/{id}/lottery", hackathon.getId())
                        .header("Authorization", "Bearer " + coordToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roundId\": " + prelimRound.getId() + ", \"assignments\": []}"))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/rounds/{id}/activate", prelimRound.getId())
                        .header("Authorization", "Bearer " + coordToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":\"it\"}"))
                .andExpect(status().isOk());

        return submitSlide(studentToken, team.getId(), "slide.pdf");
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
