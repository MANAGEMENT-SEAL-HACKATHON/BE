package com.sealhackathon.api.auth.integration;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.sealhackathon.api.chapters.value_object.ChapterStatus;
import com.sealhackathon.api.config.CriteriaCloneSourceUnlinkMigration;
import com.sealhackathon.api.users.entity.User;
import com.sealhackathon.api.users.repository.UserRepository;
import com.sealhackathon.api.users.value_object.UserRole;
import com.sealhackathon.api.users.value_object.UserStatus;
import com.sealhackathon.api.users.value_object.UserType;
import org.junit.jupiter.api.BeforeEach;
import com.sealhackathon.api.chapters.entity.Chapter;
import com.sealhackathon.api.chapters.repository.ChapterRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.profiles.active=test",
        "spring.datasource.url=jdbc:h2:mem:authflow;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "security.jwt.secret=12345678901234567890123456789012",
        "security.jwt.enabled=true",
        "app.storage.student-card-dir=target/test-uploads/student-cards"
})
class AuthOnboardingFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ChapterRepository chapterRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private CriteriaCloneSourceUnlinkMigration criteriaCloneSourceUnlinkMigration;

    @BeforeEach
    void setUp() {
        ensureCoordinator();
    }

    @Test
    void registerToCoordinatorApprove_fullOnboardingFlow() throws Exception {
        String email = "flow." + UUID.randomUUID().toString().substring(0, 8) + "@gmail.com";
        String password = "Student@123";

        // 1) register minimal
        String registerBody = """
                {
                  "email": "%s",
                  "password": "%s",
                  "confirmPassword": "%s"
                }
                """.formatted(email, password, password);
        MvcResult registerResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode registerJson = readJson(registerResult);
        Integer userId = registerJson.path("data").path("userId").asInt();
        assertThat(userId).isPositive();
        assertThat(registerJson.path("data").path("status").asText()).isEqualTo("PENDING");

        // 2) login as pending student
        String loginBody = """
                {
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(email, password);
        MvcResult loginPendingResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andReturn();
        String studentAccessToken = readJson(loginPendingResult)
                .path("data")
                .path("accessToken")
                .asText();
        assertThat(studentAccessToken).isNotBlank();

        // 3) patch /users/me complete profile
        Chapter activeChapter = chapterRepository.findAll().stream()
                .filter(ch -> ch.getStatus().name().equals("ACTIVE"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No active chapter available for test"));
        String patchMeBody = """
                {
                  "fullName": "Flow Test Student",
                  "userType": "INTERNAL",
                  "studentCode": "SE%s",
                  "chapterId": %d,
                  "phone": "0901234567"
                }
                """.formatted(String.valueOf(System.currentTimeMillis()).substring(7), activeChapter.getId());
        MvcResult patchMeResult = mockMvc.perform(patch("/api/v1/users/me")
                        .header("Authorization", "Bearer " + studentAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(patchMeBody))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(readJson(patchMeResult).path("data").path("userType").asText()).isEqualTo("INTERNAL");

        // 4) upload student card
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "student-card.png",
                MediaType.IMAGE_PNG_VALUE,
                "fake-image-content".getBytes(StandardCharsets.UTF_8)
        );
        MvcResult uploadResult = mockMvc.perform(multipart("/api/v1/users/me/student-card")
                        .file(file)
                        .header("Authorization", "Bearer " + studentAccessToken))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(readJson(uploadResult).path("data").path("studentCardImagePath").asText()).isNotBlank();

        // 5) coordinator approve
        String coordinatorLoginBody = """
                {
                  "email": "coord@fpt.edu.vn",
                  "password": "Coordinator@dev1"
                }
                """;
        MvcResult coordinatorLoginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(coordinatorLoginBody))
                .andExpect(status().isOk())
                .andReturn();
        String coordinatorAccessToken = readJson(coordinatorLoginResult)
                .path("data")
                .path("accessToken")
                .asText();
        assertThat(coordinatorAccessToken).isNotBlank();

        String approveBody = """
                {
                  "status": "APPROVED"
                }
                """;
        MvcResult approveResult = mockMvc.perform(patch("/api/v1/users/{userId}/status", userId)
                        .header("Authorization", "Bearer " + coordinatorAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(approveBody))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(readJson(approveResult).path("data").path("status").asText()).isEqualTo("APPROVED");
    }

    private JsonNode readJson(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private void ensureCoordinator() {
        if (userRepository.findByEmail("coord@fpt.edu.vn").isPresent()) {
            return;
        }
        Chapter chapter = chapterRepository.findAll().stream()
                .filter(ch -> ch.getStatus() == ChapterStatus.ACTIVE)
                .findFirst()
                .orElseGet(() -> chapterRepository.save(Chapter.builder()
                        .name("FPT HCM")
                        .code("FPT-HCM")
                        .status(ChapterStatus.ACTIVE)
                        .createdAt(LocalDateTime.now())
                        .build()));
        userRepository.save(User.builder()
                .fullName("Coordinator")
                .email("coord@fpt.edu.vn")
                .passwordHash(passwordEncoder.encode("Coordinator@dev1"))
                .role(UserRole.COORDINATOR)
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
}
