package com.se194093.be.config;

import com.se194093.be.audit_logs.entity.AuditLog;
import com.se194093.be.audit_logs.repository.AuditLogRepository;
import com.se194093.be.chapters.entity.Chapter;
import com.se194093.be.chapters.repository.ChapterRepository;
import com.se194093.be.chapters.value_object.ChapterStatus;
import com.se194093.be.criteria.entity.Criteria;
import com.se194093.be.criteria.repository.CriteriaRepository;
import com.se194093.be.criteria.value_object.CriteriaType;
import com.se194093.be.events.entity.Event;
import com.se194093.be.events.repository.EventRepository;
import com.se194093.be.events.value_object.EventType;
import com.se194093.be.hackathons.entity.Hackathon;
import com.se194093.be.hackathons.repository.HackathonRepository;
import com.se194093.be.hackathons.value_object.HackathonStatus;
import com.se194093.be.hackathons.value_object.Season;
import com.se194093.be.invitations.entity.Invitation;
import com.se194093.be.invitations.repository.InvitationRepository;
import com.se194093.be.judge_assignments.entity.JudgeAssignment;
import com.se194093.be.judge_assignments.repository.JudgeAssignmentRepository;
import com.se194093.be.judge_assignments.value_object.JudgeAssignmentType;
import com.se194093.be.mentor_assignments.entity.MentorAssignment;
import com.se194093.be.mentor_assignments.repository.MentorAssignmentRepository;
import com.se194093.be.notifications.entity.Notification;
import com.se194093.be.notifications.repository.NotificationRepository;
import com.se194093.be.rounds.entity.Round;
import com.se194093.be.rounds.repository.RoundRepository;
import com.se194093.be.rounds.value_object.TiebreakRule;
import com.se194093.be.tracks.entity.Track;
import com.se194093.be.tracks.repository.TrackRepository;
import com.se194093.be.tracks.value_object.TrackStatus;
import com.se194093.be.users.entity.User;
import com.se194093.be.users.repository.UserRepository;
import com.se194093.be.users.value_object.UserRole;
import com.se194093.be.users.value_object.UserStatus;
import com.se194093.be.users.value_object.UserType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Seed dataset RICH cho dev test MF-01.
 *
 * <p><b>Chiến lược:</b>
 * <ul>
 *   <li>{@code @Profile("dev")} — chỉ chạy khi active profile = dev.</li>
 *   <li><b>Idempotent</b> — skip toàn bộ nếu {@code hackathonRepository.count() > 0}. KHÔNG auto-wipe.</li>
 *   <li>Single transaction — rollback hết nếu lỗi giữa chừng.</li>
 * </ul>
 *
 * <p><b>Dataset overview:</b>
 * <ul>
 *   <li>4 Chapters · ~28 Users (đa role/status/type) · 6 Hackathons (DRAFT trống / DRAFT ready /
 *       DRAFT weight lệch / ONGOING / PENDING_CONFIRM / FINISHED)</li>
 *   <li>~11 Tracks · ~20 Rounds · ~60 Criteria (1 round weight 0.85 để test WARN)</li>
 *   <li>~12 Events · 7 Invitations (mix accepted/pending/expired)</li>
 *   <li>~8 MentorAssignments · ~10 JudgeAssignments (có 1 cặp CONFLICT 2 chiều có chủ đích)</li>
 *   <li>~8 Notifications · ~10 AuditLogs (snapshot lịch sử)</li>
 * </ul>
 *
 * <p>Tham chiếu plan: {@code .cursor/plans/datainitializer_dev_seed_c014b642.plan.md}.
 */
@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private static final String DEV_PASSWORD_HASH = "$2a$10$DEV_STUB_HASH_DO_NOT_USE_IN_PROD";

    private final ChapterRepository chapterRepo;
    private final UserRepository userRepo;
    private final HackathonRepository hackathonRepo;
    private final TrackRepository trackRepo;
    private final RoundRepository roundRepo;
    private final CriteriaRepository criteriaRepo;
    private final EventRepository eventRepo;
    private final InvitationRepository invitationRepo;
    private final MentorAssignmentRepository mentorRepo;
    private final JudgeAssignmentRepository judgeRepo;
    private final NotificationRepository notificationRepo;
    private final AuditLogRepository auditLogRepo;
    private final ObjectMapper objectMapper;

    private final LocalDateTime now = LocalDateTime.now();
    private final LocalDate today  = LocalDate.now();

    @Override
    @Transactional
    public void run(String... args) {
        if (hackathonRepo.count() > 0) {
            log.info("[DataInitializer] DB has data ({} hackathons), skip seed.", hackathonRepo.count());
            return;
        }

        log.info("[DataInitializer] Seeding RICH dataset for MF-01 dev test...");

        Map<String, Chapter>          chapters = seedChapters();
        Map<String, User>             users    = seedUsers(chapters);
        Map<String, Hackathon>        hacks    = seedHackathons(users);
        Map<String, Track>            tracks   = seedTracks(hacks);
        Map<String, Round>            rounds   = seedRounds(tracks);
        seedCriteria(rounds);
        seedEvents(hacks);
        seedInvitations(users);
        seedMentorAssignments(users, tracks);
        seedJudgeAssignments(users, rounds, tracks);
        seedNotifications(users, hacks);
        seedAuditLogs(users, hacks, rounds);

        log.info("[DataInitializer] DONE.");
        log.info("[DataInitializer] Counts: chapters={}, users={}, hackathons={}, tracks={}, "
                + "rounds={}, criteria={}, events={}, invitations={}, mentorAssignments={}, "
                + "judgeAssignments={}, notifications={}, auditLogs={}",
                chapterRepo.count(), userRepo.count(), hackathonRepo.count(), trackRepo.count(),
                roundRepo.count(), criteriaRepo.count(), eventRepo.count(), invitationRepo.count(),
                mentorRepo.count(), judgeRepo.count(), notificationRepo.count(), auditLogRepo.count());
    }

    // ============================================================
    // 1. CHAPTERS
    // ============================================================
    private Map<String, Chapter> seedChapters() {
        Map<String, Chapter> m = new LinkedHashMap<>();
        m.put("fptHcm", chapterRepo.save(Chapter.builder()
                .code("FPT-HCM").name("FPT University HCMC")
                .university("FPT University").city("Ho Chi Minh")
                .status(ChapterStatus.ACTIVE).createdAt(now).build()));
        m.put("fptHn", chapterRepo.save(Chapter.builder()
                .code("FPT-HN").name("FPT University Hanoi")
                .university("FPT University").city("Hanoi")
                .status(ChapterStatus.ACTIVE).createdAt(now).build()));
        m.put("fptDn", chapterRepo.save(Chapter.builder()
                .code("FPT-DN").name("FPT University Da Nang")
                .university("FPT University").city("Da Nang")
                .status(ChapterStatus.ACTIVE).createdAt(now).build()));
        m.put("hust", chapterRepo.save(Chapter.builder()
                .code("HUST").name("Hanoi University of Science and Technology")
                .university("Hanoi University of Science and Technology").city("Hanoi")
                .status(ChapterStatus.ACTIVE).createdAt(now).build()));
        log.info("[DataInitializer] Seeded {} chapters", m.size());
        return m;
    }

    // ============================================================
    // 2. USERS (~28 records, đa role/status/type)
    // ============================================================
    private Map<String, User> seedUsers(Map<String, Chapter> chapters) {
        Map<String, User> m = new LinkedHashMap<>();

        // 2 COORDINATOR — id ~1, ~2
        m.put("coord1", userRepo.save(buildInternalUser("Nguyen Van Coord", "coord1@seal.fpt.edu.vn",
                UserRole.COORDINATOR, UserStatus.APPROVED, chapters.get("fptHcm"))));
        m.put("coord2", userRepo.save(buildInternalUser("Tran Thi Coord", "coord2@seal.fpt.edu.vn",
                UserRole.COORDINATOR, UserStatus.APPROVED, chapters.get("fptHn"))));

        // 5 MENTOR INTERNAL APPROVED
        m.put("mentor1", userRepo.save(buildInternalUser("Le Minh Mentor", "mentor1@fpt.edu.vn",
                UserRole.MENTOR, UserStatus.APPROVED, chapters.get("fptHcm"))));
        m.put("mentor2", userRepo.save(buildInternalUser("Pham Quoc Mentor", "mentor2@fpt.edu.vn",
                UserRole.MENTOR, UserStatus.APPROVED, chapters.get("fptHcm"))));
        m.put("mentor3", userRepo.save(buildInternalUser("Vu Huyen Mentor", "mentor3@fpt.edu.vn",
                UserRole.MENTOR, UserStatus.APPROVED, chapters.get("fptHn"))));
        m.put("mentor4", userRepo.save(buildInternalUser("Hoang Anh Mentor", "mentor4@fpt.edu.vn",
                UserRole.MENTOR, UserStatus.APPROVED, chapters.get("fptDn"))));
        m.put("mentor5", userRepo.save(buildInternalUser("Dao Thi Mentor", "mentor5@fpt.edu.vn",
                UserRole.MENTOR, UserStatus.APPROVED, chapters.get("hust"))));

        // 1 MENTOR PENDING — test guard 422 USER_NOT_APPROVED
        m.put("mentorPending", userRepo.save(buildInternalUser("Pending Mentor", "mentor.pending@fpt.edu.vn",
                UserRole.MENTOR, UserStatus.PENDING, chapters.get("fptHcm"))));

        // 5 JUDGE INTERNAL APPROVED (FPT faculty)
        m.put("judgeI1", userRepo.save(buildInternalUser("Dr. Hoang Internal Judge", "judge1@fpt.edu.vn",
                UserRole.JUDGE, UserStatus.APPROVED, chapters.get("fptHcm"))));
        m.put("judgeI2", userRepo.save(buildInternalUser("Dr. Linh Internal Judge", "judge2@fpt.edu.vn",
                UserRole.JUDGE, UserStatus.APPROVED, chapters.get("fptHcm"))));
        m.put("judgeI3", userRepo.save(buildInternalUser("Dr. Tuan Internal Judge", "judge3@fpt.edu.vn",
                UserRole.JUDGE, UserStatus.APPROVED, chapters.get("fptHn"))));
        m.put("judgeI4", userRepo.save(buildInternalUser("Dr. Mai Internal Judge", "judge4@fpt.edu.vn",
                UserRole.JUDGE, UserStatus.APPROVED, chapters.get("fptDn"))));
        m.put("judgeI5", userRepo.save(buildInternalUser("Dr. Long Internal Judge", "judge5@fpt.edu.vn",
                UserRole.JUDGE, UserStatus.APPROVED, chapters.get("hust"))));

        // 5 JUDGE EXTERNAL APPROVED (Temp accounts — guests từ industry)
        m.put("judgeE1", userRepo.save(buildExternalTempJudge("Mr. Pham Duc Nhi", "nhi.pham@google.com",
                "Google Vietnam", UserStatus.APPROVED)));
        m.put("judgeE2", userRepo.save(buildExternalTempJudge("Ms. Tran Linh", "linh.tran@microsoft.com",
                "Microsoft Vietnam", UserStatus.APPROVED)));
        m.put("judgeE3", userRepo.save(buildExternalTempJudge("Dr. Le Van Hieu", "hieu.le@vinai.io",
                "VinAI Research", UserStatus.APPROVED)));
        m.put("judgeE4", userRepo.save(buildExternalTempJudge("Ms. Bui Hoa", "hoa.bui@fpt-software.com",
                "FPT Software", UserStatus.APPROVED)));
        m.put("judgeE5", userRepo.save(buildExternalTempJudge("Mr. Nguyen Minh", "minh.nguyen@momo.vn",
                "MoMo", UserStatus.APPROVED)));

        // 1 JUDGE EXTERNAL PENDING — chưa accept invitation
        m.put("judgeEPending", userRepo.save(buildExternalTempJudge("Mr. Pending External", "pending@guest.com",
                "Pending Co", UserStatus.PENDING)));

        // 8 STUDENT: 5 APPROVED, 2 PENDING, 1 REJECTED
        m.put("student1", userRepo.save(buildStudent("Nguyen Sinh Vien 1", "sv1@fpt.edu.vn",
                "HE160001", chapters.get("fptHcm"), UserStatus.APPROVED, null)));
        m.put("student2", userRepo.save(buildStudent("Tran Sinh Vien 2", "sv2@fpt.edu.vn",
                "HE160002", chapters.get("fptHcm"), UserStatus.APPROVED, null)));
        m.put("student3", userRepo.save(buildStudent("Le Sinh Vien 3", "sv3@fpt.edu.vn",
                "HE160003", chapters.get("fptHn"), UserStatus.APPROVED, null)));
        m.put("student4", userRepo.save(buildStudent("Pham Sinh Vien 4", "sv4@fpt.edu.vn",
                "HE160004", chapters.get("fptHn"), UserStatus.APPROVED, null)));
        m.put("student5", userRepo.save(buildStudent("Hoang Sinh Vien 5", "sv5@fpt.edu.vn",
                "HE160005", chapters.get("fptDn"), UserStatus.APPROVED, null)));
        m.put("student6", userRepo.save(buildStudent("Vu Sinh Vien 6", "sv6@fpt.edu.vn",
                "HE160006", chapters.get("fptHcm"), UserStatus.PENDING, null)));
        m.put("student7", userRepo.save(buildStudent("Dao Sinh Vien 7", "sv7@fpt.edu.vn",
                "HE160007", chapters.get("hust"), UserStatus.PENDING, null)));
        m.put("student8", userRepo.save(buildStudent("Bui Sinh Vien 8", "sv8@fpt.edu.vn",
                "HE160008", chapters.get("fptHcm"), UserStatus.REJECTED,
                "Hồ sơ thiếu mã sinh viên hợp lệ")));

        log.info("[DataInitializer] Seeded {} users (2 coord, 6 mentor, 11 judge, 8 student, 1 pending external)",
                m.size());
        return m;
    }

    private User buildInternalUser(String fullName, String email, UserRole role,
                                   UserStatus status, Chapter chapter) {
        return User.builder()
                .fullName(fullName).email(email).passwordHash(DEV_PASSWORD_HASH)
                .role(role).userType(UserType.INTERNAL).isTempAccount(false)
                .status(status).chapter(chapter)
                .institution("FPT University").phone("+84-900-000-000")
                .createdAt(now).updatedAt(now)
                .build();
    }

    private User buildExternalTempJudge(String fullName, String email, String institution,
                                        UserStatus status) {
        return User.builder()
                .fullName(fullName).email(email).passwordHash(null)
                .role(UserRole.JUDGE).userType(UserType.EXTERNAL).isTempAccount(true)
                .status(status).institution(institution).phone("+84-901-000-000")
                .createdAt(now).updatedAt(now)
                .build();
    }

    private User buildStudent(String fullName, String email, String studentCode, Chapter chapter,
                              UserStatus status, String rejectionReason) {
        return User.builder()
                .fullName(fullName).email(email).passwordHash(DEV_PASSWORD_HASH)
                .role(UserRole.STUDENT).userType(UserType.INTERNAL).isTempAccount(false)
                .studentCode(studentCode).chapter(chapter).status(status)
                .rejectionReason(rejectionReason).institution("FPT University")
                .createdAt(now).updatedAt(now)
                .build();
    }

    // ============================================================
    // 3. HACKATHONS (6 kịch bản test)
    // ============================================================
    private Map<String, Hackathon> seedHackathons(Map<String, User> users) {
        Map<String, Hackathon> m = new LinkedHashMap<>();
        User coord1 = users.get("coord1");

        // H1 — FINISHED (Fall 2024)
        m.put("h1Finished", hackathonRepo.save(Hackathon.builder()
                .name("SEAL Fall 2024").slug("seal-fall-2024")
                .season(Season.Fall).year(2024).status(HackathonStatus.FINISHED)
                .description("Kỳ thi đã kết thúc — dùng để test list filter status=FINISHED")
                .registrationStart(today.minusDays(360)).registrationEnd(today.minusDays(330))
                .eventStart(today.minusDays(320)).eventEnd(today.minusDays(280))
                .wildcardEnabled(false).individualRankingEnabled(false)
                .createdBy(coord1).createdAt(now.minusDays(360)).updatedAt(now.minusDays(280))
                .build()));

        // H2 — PENDING_CONFIRM (Spring 2025)
        m.put("h2Pending", hackathonRepo.save(Hackathon.builder()
                .name("SEAL Spring 2025").slug("seal-spring-2025")
                .season(Season.Spring).year(2025).status(HackathonStatus.PENDING_CONFIRM)
                .description("Đợi BTC chốt giải — test transition PENDING_CONFIRM → FINISHED")
                .registrationStart(today.minusDays(240)).registrationEnd(today.minusDays(210))
                .eventStart(today.minusDays(200)).eventEnd(today.minusDays(160))
                .wildcardEnabled(true).individualRankingEnabled(false)
                .createdBy(coord1).createdAt(now.minusDays(240)).updatedAt(now.minusDays(160))
                .build()));

        // H3 — ONGOING (Fall 2025)
        m.put("h3Ongoing", hackathonRepo.save(Hackathon.builder()
                .name("SEAL Fall 2025").slug("seal-fall-2025")
                .season(Season.Fall).year(2025).status(HackathonStatus.ONGOING)
                .description("Đang chạy — Round Sơ loại isActive=true")
                .registrationStart(today.minusDays(120)).registrationEnd(today.minusDays(90))
                .eventStart(today.minusDays(80)).eventEnd(today.plusDays(20))
                .wildcardEnabled(true).individualRankingEnabled(true)
                .createdBy(coord1).createdAt(now.minusDays(120)).updatedAt(now.minusDays(80))
                .build()));

        // H4 — DRAFT READY (Spring 2026)
        m.put("h4DraftReady", hackathonRepo.save(Hackathon.builder()
                .name("SEAL Spring 2026").slug("seal-spring-2026")
                .season(Season.Spring).year(2026).status(HackathonStatus.DRAFT)
                .description("DRAFT đầy đủ Track/Round/Criteria/Event — readiness PASS, sẵn sàng PATCH /status to ONGOING")
                .registrationStart(today.plusDays(60)).registrationEnd(today.plusDays(90))
                .eventStart(today.plusDays(100)).eventEnd(today.plusDays(140))
                .wildcardEnabled(false).individualRankingEnabled(false)
                .createdBy(coord1).createdAt(now.minusDays(10)).updatedAt(now)
                .build()));

        // H5 — DRAFT WEIGHT LỆCH (Summer 2026)
        m.put("h5WeightWarn", hackathonRepo.save(Hackathon.builder()
                .name("SEAL Summer 2026").slug("seal-summer-2026")
                .season(Season.Summer).year(2026).status(HackathonStatus.DRAFT)
                .description("DRAFT có Round Chung kết weight tổng 0.85 — test FR-04 WARN + FR-06B activate FAIL "
                           + "+ FR-06 readiness FAIL (cũng thiếu KICKOFF)")
                .registrationStart(today.plusDays(180)).registrationEnd(today.plusDays(210))
                .eventStart(today.plusDays(220)).eventEnd(today.plusDays(260))
                .wildcardEnabled(false).individualRankingEnabled(false)
                .createdBy(coord1).createdAt(now.minusDays(5)).updatedAt(now)
                .build()));

        // H6 — DRAFT TRỐNG (Winter 2026)
        m.put("h6Empty", hackathonRepo.save(Hackathon.builder()
                .name("SEAL Winter 2026").slug("seal-winter-2026")
                .season(Season.Winter).year(2026).status(HackathonStatus.DRAFT)
                .description("DRAFT vừa tạo, chưa có Track — test DELETE OK + readiness FAIL MISSING_TRACK")
                .registrationStart(today.plusDays(270)).registrationEnd(today.plusDays(300))
                .eventStart(today.plusDays(310)).eventEnd(today.plusDays(340))
                .wildcardEnabled(false).individualRankingEnabled(false)
                .createdBy(coord1).createdAt(now).updatedAt(now)
                .build()));

        log.info("[DataInitializer] Seeded {} hackathons (1 FINISHED, 1 PENDING_CONFIRM, 1 ONGOING, 3 DRAFT)",
                m.size());
        return m;
    }

    // ============================================================
    // 4. TRACKS (~11 records)
    // ============================================================
    private Map<String, Track> seedTracks(Map<String, Hackathon> hacks) {
        Map<String, Track> m = new LinkedHashMap<>();

        // H1 — 3 tracks (1 CANCELLED để test filter)
        m.put("h1_web",       trackRepo.save(buildTrack(hacks.get("h1Finished"),
                "Web & Mobile",  24, 6, 3, 5, TrackStatus.CLOSED)));
        m.put("h1_ai",        trackRepo.save(buildTrack(hacks.get("h1Finished"),
                "AI / Data",     20, 5, 3, 5, TrackStatus.CLOSED)));
        m.put("h1_cancelled", trackRepo.save(buildTrack(hacks.get("h1Finished"),
                "IoT (cancelled)", 16, 4, 3, 5, TrackStatus.CANCELLED)));

        // H2 — 2 tracks
        m.put("h2_web", trackRepo.save(buildTrack(hacks.get("h2Pending"),
                "Web Innovation", 24, 6, 3, 5, TrackStatus.CLOSED)));
        m.put("h2_ai",  trackRepo.save(buildTrack(hacks.get("h2Pending"),
                "AI Frontier",    20, 5, 3, 5, TrackStatus.CLOSED)));

        // H3 — 2 tracks
        m.put("h3_web", trackRepo.save(buildTrack(hacks.get("h3Ongoing"),
                "Fullstack Web",  30, 6, 3, 5, TrackStatus.OPEN)));
        m.put("h3_ai",  trackRepo.save(buildTrack(hacks.get("h3Ongoing"),
                "Applied AI",     24, 6, 3, 5, TrackStatus.OPEN)));

        // H4 — 2 tracks (DRAFT ready)
        m.put("h4_web", trackRepo.save(buildTrack(hacks.get("h4DraftReady"),
                "Cloud Native",   32, 8, 3, 5, TrackStatus.OPEN)));
        m.put("h4_ai",  trackRepo.save(buildTrack(hacks.get("h4DraftReady"),
                "Generative AI",  28, 7, 3, 5, TrackStatus.OPEN)));

        // H5 — 1 track (weight lệch scenario)
        m.put("h5_track", trackRepo.save(buildTrack(hacks.get("h5WeightWarn"),
                "Edge Computing", 20, 5, 3, 5, TrackStatus.OPEN)));

        // H6 — 0 track

        log.info("[DataInitializer] Seeded {} tracks", m.size());
        return m;
    }

    private Track buildTrack(Hackathon h, String name, int maxTeams, int maxPerGroup,
                             int minTeamSize, int maxTeamSize, TrackStatus status) {
        return Track.builder()
                .hackathon(h).name(name)
                .description(name + " — auto-seeded by DataInitializer")
                .maxTeams(maxTeams).maxTeamsPerGroup(maxPerGroup)
                .minTeamSize(minTeamSize).maxTeamSize(maxTeamSize)
                .status(status)
                .build();
    }

    // ============================================================
    // 5. ROUNDS (~20 records)
    // ============================================================
    private Map<String, Round> seedRounds(Map<String, Track> tracks) {
        Map<String, Round> m = new LinkedHashMap<>();

        // H1 Finished — 2 round/track, scoringLocked=true
        m.put("h1_web_r1", roundRepo.save(buildRound(tracks.get("h1_web"), "Sơ loại", 1,
                now.minusDays(320), now.minusDays(310), true, false, null, false)));
        m.put("h1_web_r2", roundRepo.save(buildRound(tracks.get("h1_web"), "Chung kết", 2,
                now.minusDays(300), now.minusDays(290), true, false, null, false)));
        m.put("h1_ai_r1",  roundRepo.save(buildRound(tracks.get("h1_ai"), "Sơ loại", 1,
                now.minusDays(320), now.minusDays(310), true, false, null, false)));
        // Round có forceLocked để demo
        m.put("h1_ai_r2",  roundRepo.save(buildRound(tracks.get("h1_ai"), "Chung kết", 2,
                now.minusDays(300), now.minusDays(290), true, true,
                "Khoá khẩn cấp: phát hiện 1 đội vi phạm điều lệ", false)));

        // H2 PENDING_CONFIRM
        m.put("h2_web_r1", roundRepo.save(buildRound(tracks.get("h2_web"), "Sơ loại", 1,
                now.minusDays(200), now.minusDays(190), true, false, null, false)));
        m.put("h2_web_r2", roundRepo.save(buildRound(tracks.get("h2_web"), "Chung kết", 2,
                now.minusDays(180), now.minusDays(170), true, false, null, false)));
        m.put("h2_ai_r1",  roundRepo.save(buildRound(tracks.get("h2_ai"), "Sơ loại", 1,
                now.minusDays(200), now.minusDays(190), true, false, null, false)));
        m.put("h2_ai_r2",  roundRepo.save(buildRound(tracks.get("h2_ai"), "Chung kết", 2,
                now.minusDays(180), now.minusDays(170), true, false, null, false)));

        // H3 ONGOING — h3_web Sơ loại isActive=true
        m.put("h3_web_r1", roundRepo.save(buildRound(tracks.get("h3_web"), "Sơ loại", 1,
                now.minusDays(80), now.plusDays(5), false, false, null, true)));
        m.put("h3_web_r2", roundRepo.save(buildRound(tracks.get("h3_web"), "Chung kết", 2,
                now.plusDays(10), now.plusDays(15), false, false, null, false)));
        m.put("h3_ai_r1",  roundRepo.save(buildRound(tracks.get("h3_ai"), "Sơ loại", 1,
                now.minusDays(80), now.plusDays(5), false, false, null, false)));
        m.put("h3_ai_r2",  roundRepo.save(buildRound(tracks.get("h3_ai"), "Chung kết", 2,
                now.plusDays(10), now.plusDays(15), false, false, null, false)));

        // H4 DRAFT READY — chưa active
        m.put("h4_web_r1", roundRepo.save(buildRound(tracks.get("h4_web"), "Sơ loại", 1,
                now.plusDays(100), now.plusDays(110), false, false, null, false)));
        m.put("h4_web_r2", roundRepo.save(buildRound(tracks.get("h4_web"), "Chung kết", 2,
                now.plusDays(115), now.plusDays(125), false, false, null, false)));
        m.put("h4_ai_r1",  roundRepo.save(buildRound(tracks.get("h4_ai"), "Sơ loại", 1,
                now.plusDays(100), now.plusDays(110), false, false, null, false)));
        m.put("h4_ai_r2",  roundRepo.save(buildRound(tracks.get("h4_ai"), "Chung kết", 2,
                now.plusDays(115), now.plusDays(125), false, false, null, false)));

        // H5 — 2 round; round 2 sẽ có Criteria tổng 0.85
        m.put("h5_r1", roundRepo.save(buildRound(tracks.get("h5_track"), "Sơ loại", 1,
                now.plusDays(220), now.plusDays(230), false, false, null, false)));
        m.put("h5_r2", roundRepo.save(buildRound(tracks.get("h5_track"), "Chung kết", 2,
                now.plusDays(235), now.plusDays(245), false, false, null, false)));

        log.info("[DataInitializer] Seeded {} rounds", m.size());
        return m;
    }

    private Round buildRound(Track t, String name, int seq, LocalDateTime submissionOpen,
                             LocalDateTime submissionDeadline, boolean scoringLocked,
                             boolean forceLocked, String forceLockReason, boolean isActive) {
        return Round.builder()
                .track(t).name(name).sequenceOrder(seq)
                .submissionOpen(submissionOpen).submissionDeadline(submissionDeadline)
                .codingDurationHours(120).problemStatementUrl("https://github.com/seal/round-problem-" + seq + ".md")
                .topNAdvance(seq == 1 ? 16 : null).wildcardEnabled(false).minTeamsFinal(seq == 2 ? 4 : null)
                .tiebreakRule(TiebreakRule.PENALTY_SCORE)
                .isActive(isActive).scoringLocked(scoringLocked)
                .scoringLockedAt(scoringLocked ? now : null)
                .forceLocked(forceLocked).forceLockReason(forceLockReason)
                .createdAt(now)
                .build();
    }

    // ============================================================
    // 6. CRITERIA (~60 records)
    // ============================================================
    private void seedCriteria(Map<String, Round> rounds) {
        int totalCount = 0;

        // Helper: default 4 criteria tổng = 1.0
        for (Map.Entry<String, Round> e : rounds.entrySet()) {
            String key = e.getKey();
            Round r = e.getValue();

            // H5 round 2 — chỉ 3 criteria tổng 0.85 (test WARN)
            if (key.equals("h5_r2")) {
                criteriaRepo.save(buildCriterion(r, null, "Code Quality",  CriteriaType.TECHNICAL,  0.30f, 1));
                criteriaRepo.save(buildCriterion(r, null, "Demo Polish",   CriteriaType.TECHNICAL,  0.30f, 2));
                criteriaRepo.save(buildCriterion(r, null, "Pitch Quality", CriteriaType.SOFT_SKILL, 0.25f, 3));
                totalCount += 3;
                continue;
            }

            // Default: 4 criteria 0.30/0.20/0.30/0.20 = 1.00
            criteriaRepo.save(buildCriterion(r, null, "Code Quality",  CriteriaType.TECHNICAL,  0.30f, 1));
            criteriaRepo.save(buildCriterion(r, null, "Demo Polish",   CriteriaType.TECHNICAL,  0.20f, 2));
            criteriaRepo.save(buildCriterion(r, null, "Pitch Quality", CriteriaType.SOFT_SKILL, 0.30f, 3));
            criteriaRepo.save(buildCriterion(r, null, "Innovation",    CriteriaType.SOFT_SKILL, 0.20f, 4));
            totalCount += 4;

            // H1 ai r2 — thêm 1 PENALTY criterion (weight 0.05, KHÔNG tính vào tổng 1.0)
            if (key.equals("h1_ai_r2")) {
                criteriaRepo.save(buildCriterion(r, null, "Late submission penalty",
                        CriteriaType.PENALTY, 0.05f, 5));
                totalCount++;
            }
        }

        // CLONE DEMO — clone criteria của h4_web_r1 sang h4_ai_r1 (4 criterion) để demo source_criteria_id
        Round sourceRound = rounds.get("h4_web_r1");
        Round targetRound = rounds.get("h4_ai_r1");
        List<Criteria> sourceList = criteriaRepo.findByRoundIdOrderByDisplayOrderAsc(sourceRound.getId());
        // Trước tiên xóa criteria default đã seed của h4_ai_r1 để clone sạch
        List<Criteria> existingTarget = criteriaRepo.findByRoundIdOrderByDisplayOrderAsc(targetRound.getId());
        criteriaRepo.deleteAll(existingTarget);
        totalCount -= existingTarget.size();
        for (Criteria src : sourceList) {
            criteriaRepo.save(Criteria.builder()
                    .round(targetRound).sourceCriteria(src)
                    .name(src.getName()).type(src.getType())
                    .weight(src.getWeight()).maxScore(src.getMaxScore())
                    .description("CLONED from h4_web_r1.criteria#" + src.getId())
                    .rubricUrl(src.getRubricUrl()).displayOrder(src.getDisplayOrder())
                    .build());
            totalCount++;
        }

        log.info("[DataInitializer] Seeded {} criteria (incl. 1 PENALTY in H1, 1 weight-lệch round in H5, "
                + "1 clone-from-source pair in H4)", totalCount);
    }

    private Criteria buildCriterion(Round r, Criteria source, String name, CriteriaType type,
                                    Float weight, int displayOrder) {
        return Criteria.builder()
                .round(r).sourceCriteria(source).name(name).type(type)
                .weight(weight).maxScore(10).description(name + " — auto-seeded")
                .rubricUrl("https://docs.seal/rubric/" + name.toLowerCase().replace(" ", "-") + ".md")
                .displayOrder(displayOrder)
                .build();
    }

    // ============================================================
    // 7. EVENTS (~12 records)
    // ============================================================
    private void seedEvents(Map<String, Hackathon> hacks) {
        int n = 0;
        // H1 FINISHED — KICKOFF + WORKSHOP + AWARDS (đã qua)
        eventRepo.save(buildEvent(hacks.get("h1Finished"), "Khai mạc SEAL Fall 2024", EventType.KICKOFF,
                now.minusDays(320), now.minusDays(320).plusHours(2), "FPT HCMC Hall", null, true)); n++;
        eventRepo.save(buildEvent(hacks.get("h1Finished"), "Workshop Code Review", EventType.WORKSHOP,
                now.minusDays(310), now.minusDays(310).plusHours(3), null, "https://meet.google.com/seal-ws-1", true)); n++;
        eventRepo.save(buildEvent(hacks.get("h1Finished"), "Trao giải Fall 2024", EventType.AWARDS,
                now.minusDays(280), now.minusDays(280).plusHours(2), "FPT HCMC Hall", null, true)); n++;

        // H2 PENDING_CONFIRM — KICKOFF + AWARDS đã qua
        eventRepo.save(buildEvent(hacks.get("h2Pending"), "Khai mạc SEAL Spring 2025", EventType.KICKOFF,
                now.minusDays(200), now.minusDays(200).plusHours(2), "FPT HCMC Hall", null, true)); n++;
        eventRepo.save(buildEvent(hacks.get("h2Pending"), "Trao giải Spring 2025", EventType.AWARDS,
                now.minusDays(160), now.minusDays(160).plusHours(2), "FPT HCMC Hall", null, true)); n++;

        // H3 ONGOING — KICKOFF đã qua, PRESENTATION + AWARDS sắp tới
        eventRepo.save(buildEvent(hacks.get("h3Ongoing"), "Khai mạc SEAL Fall 2025", EventType.KICKOFF,
                now.minusDays(80), now.minusDays(80).plusHours(2), "FPT HCMC Hall", null, true)); n++;
        eventRepo.save(buildEvent(hacks.get("h3Ongoing"), "Demo Day Track Web", EventType.PRESENTATION,
                now.plusDays(10), now.plusDays(10).plusHours(4), "FPT HCMC Hall A", null, true)); n++;
        eventRepo.save(buildEvent(hacks.get("h3Ongoing"), "Trao giải Fall 2025", EventType.AWARDS,
                now.plusDays(15), now.plusDays(15).plusHours(2), "FPT HCMC Hall", null, true)); n++;

        // H4 DRAFT READY — KICKOFF + AWARDS tương lai
        eventRepo.save(buildEvent(hacks.get("h4DraftReady"), "Khai mạc SEAL Spring 2026", EventType.KICKOFF,
                now.plusDays(100), now.plusDays(100).plusHours(2), "FPT HCMC Hall", null, true)); n++;
        eventRepo.save(buildEvent(hacks.get("h4DraftReady"), "Workshop Generative AI", EventType.WORKSHOP,
                now.plusDays(110), now.plusDays(110).plusHours(3), null, "https://meet.google.com/seal-ws-4", true)); n++;
        eventRepo.save(buildEvent(hacks.get("h4DraftReady"), "Trao giải Spring 2026", EventType.AWARDS,
                now.plusDays(140), now.plusDays(140).plusHours(2), "FPT HCMC Hall", null, true)); n++;

        // H5 WEIGHT WARN — KHÔNG tạo KICKOFF (test readiness blocker EVENT_KICKOFF_MISSING)
        eventRepo.save(buildEvent(hacks.get("h5WeightWarn"), "Workshop Edge Computing", EventType.WORKSHOP,
                now.plusDays(220), now.plusDays(220).plusHours(3), null, "https://meet.google.com/seal-ws-5", true)); n++;

        // H6 — không event nào (DRAFT trống)

        log.info("[DataInitializer] Seeded {} events (H5 intentionally MISSING KICKOFF)", n);
    }

    private Event buildEvent(Hackathon h, String title, EventType type, LocalDateTime starts,
                             LocalDateTime ends, String location, String meetUrl, boolean isPublic) {
        return Event.builder()
                .hackathon(h).title(title).type(type)
                .description(title + " — auto-seeded")
                .location(location).meetUrl(meetUrl)
                .startsAt(starts).endsAt(ends).isPublic(isPublic)
                .build();
    }

    // ============================================================
    // 8. INVITATIONS (7 records)
    // ============================================================
    private void seedInvitations(Map<String, User> users) {
        User inviter = users.get("coord1");

        // I1-I4 — 4 Judge external đã ACCEPTED
        invitationRepo.save(buildInvitation(users.get("judgeE1").getEmail(), inviter,
                now.minusDays(50), now.minusDays(48))); // accepted 2 ngày sau khi mời
        invitationRepo.save(buildInvitation(users.get("judgeE2").getEmail(), inviter,
                now.minusDays(45), now.minusDays(44)));
        invitationRepo.save(buildInvitation(users.get("judgeE3").getEmail(), inviter,
                now.minusDays(40), now.minusDays(39)));
        invitationRepo.save(buildInvitation(users.get("judgeE4").getEmail(), inviter,
                now.minusDays(35), now.minusDays(34)));

        // I5 — PENDING, expires NOW+24h (test resend OK)
        Invitation i5 = Invitation.builder()
                .email(users.get("judgeE5").getEmail()).role(UserRole.JUDGE).invitedBy(inviter)
                .token(generateToken()).expiresAt(now.plusHours(24)).acceptedAt(null)
                .createdAt(now.minusHours(24)).build();
        invitationRepo.save(i5);

        // I6 — EXPIRED, expires NOW-1h (test resend regenerate)
        Invitation i6 = Invitation.builder()
                .email(users.get("judgeEPending").getEmail()).role(UserRole.JUDGE).invitedBy(inviter)
                .token(generateToken()).expiresAt(now.minusHours(1)).acceptedAt(null)
                .createdAt(now.minusDays(2).minusHours(1)).build();
        invitationRepo.save(i6);

        // I7 — ACCEPTED đã consumed (test 409 INVITATION_ALREADY_ACCEPTED)
        Invitation i7 = Invitation.builder()
                .email("already.accepted@guest.com").role(UserRole.JUDGE).invitedBy(inviter)
                .token(generateToken()).expiresAt(now.plusDays(2)).acceptedAt(now.minusDays(1))
                .createdAt(now.minusDays(3)).build();
        invitationRepo.save(i7);

        log.info("[DataInitializer] Seeded 7 invitations (4 accepted, 1 pending, 1 expired, 1 already-accepted)");
    }

    private Invitation buildInvitation(String email, User invitedBy, LocalDateTime createdAt,
                                       LocalDateTime acceptedAt) {
        return Invitation.builder()
                .email(email).role(UserRole.JUDGE).invitedBy(invitedBy)
                .token(generateToken())
                .expiresAt(createdAt.plusHours(48)).acceptedAt(acceptedAt)
                .createdAt(createdAt)
                .build();
    }

    private String generateToken() {
        return UUID.randomUUID().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", "");
    }

    // ============================================================
    // 9. MENTOR ASSIGNMENTS (~8 records)
    // ============================================================
    private void seedMentorAssignments(Map<String, User> users, Map<String, Track> tracks) {
        User coord1 = users.get("coord1");
        int n = 0;

        // 5 mentor APPROVED phân vào 5 track khác nhau
        mentorRepo.save(buildMentor(users.get("mentor1"), tracks.get("h3_web"), coord1)); n++;
        mentorRepo.save(buildMentor(users.get("mentor2"), tracks.get("h3_ai"),  coord1)); n++;
        mentorRepo.save(buildMentor(users.get("mentor3"), tracks.get("h4_web"), coord1)); n++;
        mentorRepo.save(buildMentor(users.get("mentor4"), tracks.get("h4_ai"),  coord1)); n++;
        mentorRepo.save(buildMentor(users.get("mentor5"), tracks.get("h5_track"), coord1)); n++;

        // Multi-hackathon: mentor1 cũng phụ trách 1 track ở H4
        mentorRepo.save(buildMentor(users.get("mentor1"), tracks.get("h4_web"), coord1)); n++;

        // CONFLICT pair: mentor2 đã phụ trách h3_ai (track) — JudgeAssignments §10 sẽ phân mentor2 làm Judge
        // ở h3_ai_r1 (cùng Track) → tạo MENTOR_JUDGE_CONFLICT có chủ đích

        // Bonus: mentor3 cũng phụ trách h3_web (cùng track với mentor1) — test list track có nhiều mentor
        mentorRepo.save(buildMentor(users.get("mentor3"), tracks.get("h3_web"), coord1)); n++;

        // Bonus: mentor4 phụ trách thêm h3_web — track có 3 mentor
        mentorRepo.save(buildMentor(users.get("mentor4"), tracks.get("h3_web"), coord1)); n++;

        log.info("[DataInitializer] Seeded {} mentor assignments (1 multi-hackathon, 1 track có 3 mentors, "
                + "1 user CONFLICT chuẩn bị §10)", n);
    }

    private MentorAssignment buildMentor(User mentor, Track track, User assignedBy) {
        return MentorAssignment.builder()
                .mentor(mentor).track(track).assignedBy(assignedBy).assignedAt(now)
                .build();
    }

    // ============================================================
    // 10. JUDGE ASSIGNMENTS (~10 records)
    // ============================================================
    private void seedJudgeAssignments(Map<String, User> users, Map<String, Round> rounds,
                                      Map<String, Track> tracks) {
        User coord1 = users.get("coord1");
        int n = 0;

        // 5 judge INTERNAL phân Round Sơ loại H3/H4
        judgeRepo.save(buildJudge(users.get("judgeI1"), rounds.get("h3_web_r1"),
                JudgeAssignmentType.HEAD, coord1)); n++;
        judgeRepo.save(buildJudge(users.get("judgeI2"), rounds.get("h3_web_r1"),
                JudgeAssignmentType.NORMAL, coord1)); n++;
        judgeRepo.save(buildJudge(users.get("judgeI3"), rounds.get("h3_ai_r1"),
                JudgeAssignmentType.HEAD, coord1)); n++;
        judgeRepo.save(buildJudge(users.get("judgeI4"), rounds.get("h4_web_r1"),
                JudgeAssignmentType.HEAD, coord1)); n++;
        judgeRepo.save(buildJudge(users.get("judgeI5"), rounds.get("h4_ai_r1"),
                JudgeAssignmentType.HEAD, coord1)); n++;

        // 4 judge EXTERNAL phân Round Sơ loại với assignmentType đa dạng
        judgeRepo.save(buildJudge(users.get("judgeE1"), rounds.get("h3_web_r1"),
                JudgeAssignmentType.CALIBRATION, coord1)); n++;
        judgeRepo.save(buildJudge(users.get("judgeE2"), rounds.get("h3_ai_r1"),
                JudgeAssignmentType.NORMAL, coord1)); n++;
        judgeRepo.save(buildJudge(users.get("judgeE3"), rounds.get("h4_web_r1"),
                JudgeAssignmentType.CALIBRATION, coord1)); n++;
        judgeRepo.save(buildJudge(users.get("judgeE4"), rounds.get("h4_ai_r1"),
                JudgeAssignmentType.NORMAL, coord1)); n++;

        // 1 judge phân Round Chung kết H4 — minh họa kịch bản warning JUDGE_FINAL_ROUND_AT_PHASE1
        judgeRepo.save(buildJudge(users.get("judgeI1"), rounds.get("h4_web_r2"),
                JudgeAssignmentType.HEAD, coord1)); n++;

        // CONFLICT pair: mentor2 đã là Mentor track h3_ai — gán cùng làm Judge round h3_ai_r1 trong cùng Track
        // → khi gọi POST /judge-assignments cùng pair sẽ trả MENTOR_JUDGE_CONFLICT warning,
        // ở đây seed sẵn để Dev test query inverse-lookup ra cặp conflict
        judgeRepo.save(buildJudge(users.get("mentor2"), rounds.get("h3_ai_r2"),
                JudgeAssignmentType.NORMAL, coord1)); n++;

        log.info("[DataInitializer] Seeded {} judge assignments (1 Final-round case + 1 Mentor-Judge CONFLICT)", n);
    }

    private JudgeAssignment buildJudge(User judge, Round round, JudgeAssignmentType type, User assignedBy) {
        return JudgeAssignment.builder()
                .judge(judge).round(round).assignmentType(type)
                .assignedBy(assignedBy).assignedAt(now)
                .build();
    }

    // ============================================================
    // 11. NOTIFICATIONS (~8 records)
    // ============================================================
    private void seedNotifications(Map<String, User> users, Map<String, Hackathon> hacks) {
        int n = 0;

        // Coordinator — HACKATHON_OPEN khi H3 chuyển ONGOING
        notificationRepo.save(buildNotification(users.get("coord1"), "HACKATHON_OPEN",
                "SEAL Fall 2025 đã mở cổng đăng ký",
                "Hackathon SEAL Fall 2025 đã chuyển ONGOING. Mời các bên liên quan tham gia.",
                "hackathons", hacks.get("h3Ongoing").getId(), true, now.minusDays(80))); n++;

        // Coordinator — EVENT_REMINDER cho KICKOFF H3
        notificationRepo.save(buildNotification(users.get("coord1"), "EVENT_REMINDER",
                "Sự kiện sắp diễn ra: Demo Day Track Web",
                "Bạn có sự kiện Demo Day Track Web trong 10 ngày tới.",
                "events", null, false, now.minusHours(2))); n++;

        // Coordinator 2 — REMINDER cho AWARDS
        notificationRepo.save(buildNotification(users.get("coord2"), "EVENT_REMINDER",
                "Trao giải Fall 2025 sắp diễn ra",
                "Trao giải Fall 2025 trong 15 ngày tới.",
                "events", null, false, now.minusHours(1))); n++;

        // Mentor — MENTOR_ASSIGNED
        notificationRepo.save(buildNotification(users.get("mentor1"), "MENTOR_ASSIGNED",
                "Bạn được phân công Mentor Track Fullstack Web (H3)",
                "Vui lòng vào hệ thống xác nhận và liên hệ với các đội thi.",
                "tracks", null, true, now.minusDays(85))); n++;

        notificationRepo.save(buildNotification(users.get("mentor2"), "MENTOR_ASSIGNED",
                "Bạn được phân công Mentor Track Applied AI (H3)",
                "Vui lòng vào hệ thống xác nhận và liên hệ với các đội thi.",
                "tracks", null, false, now.minusDays(85))); n++;

        notificationRepo.save(buildNotification(users.get("mentor3"), "MENTOR_ASSIGNED",
                "Bạn được phân công Mentor Track Cloud Native (H4)",
                "Vui lòng vào hệ thống xác nhận và liên hệ với các đội thi.",
                "tracks", null, false, now.minusDays(8))); n++;

        // Judge — JUDGE_ASSIGNED
        notificationRepo.save(buildNotification(users.get("judgeI1"), "JUDGE_ASSIGNED",
                "Bạn được phân công Judge Round Sơ loại Fullstack Web (H3)",
                "Type: HEAD. Hãy vào hệ thống xem chi tiết Round và Criteria.",
                "rounds", null, true, now.minusDays(85))); n++;

        notificationRepo.save(buildNotification(users.get("judgeE1"), "JUDGE_ASSIGNED",
                "Bạn được phân công Judge khách mời (Calibration) Round Sơ loại Fullstack Web (H3)",
                "Type: CALIBRATION. Tham khảo rubric trước khi chấm.",
                "rounds", null, false, now.minusDays(85))); n++;

        log.info("[DataInitializer] Seeded {} notifications", n);
    }

    private Notification buildNotification(User user, String type, String title, String body,
                                           String referenceType, Integer referenceId,
                                           boolean isRead, LocalDateTime sentAt) {
        return Notification.builder()
                .user(user).type(type).title(title).body(body)
                .referenceType(referenceType).referenceId(referenceId)
                .isRead(isRead).sentAt(sentAt).readAt(isRead ? sentAt.plusMinutes(15) : null)
                .build();
    }

    // ============================================================
    // 12. AUDIT LOGS (~10 records)
    // ============================================================
    private void seedAuditLogs(Map<String, User> users, Map<String, Hackathon> hacks,
                               Map<String, Round> rounds) {
        User coord1 = users.get("coord1");
        int n = 0;

        // HACKATHON_CREATE cho 6 hackathon
        for (Map.Entry<String, Hackathon> e : hacks.entrySet()) {
            Hackathon h = e.getValue();
            auditLogRepo.save(buildAudit(coord1, "HACKATHON_CREATE", "hackathons", h.getId(),
                    Map.of("name", h.getName(), "season", h.getSeason().name(), "year", h.getYear()),
                    h.getCreatedAt()));
            n++;
        }

        // HACKATHON_STATUS_CHANGE
        auditLogRepo.save(buildAudit(coord1, "HACKATHON_STATUS_CHANGE", "hackathons",
                hacks.get("h3Ongoing").getId(),
                Map.of("from", "DRAFT", "to", "ONGOING", "note", "Mở cổng đăng ký"),
                now.minusDays(120))); n++;
        auditLogRepo.save(buildAudit(coord1, "HACKATHON_STATUS_CHANGE", "hackathons",
                hacks.get("h2Pending").getId(),
                Map.of("from", "ONGOING", "to", "PENDING_CONFIRM", "note", "Kết thúc Chung kết"),
                now.minusDays(160))); n++;
        auditLogRepo.save(buildAudit(coord1, "HACKATHON_STATUS_CHANGE", "hackathons",
                hacks.get("h1Finished").getId(),
                Map.of("from", "PENDING_CONFIRM", "to", "FINISHED", "note", "Chốt giải"),
                now.minusDays(280))); n++;

        // ROUND_ACTIVATE
        auditLogRepo.save(buildAudit(coord1, "ROUND_ACTIVATE", "rounds",
                rounds.get("h3_web_r1").getId(),
                Map.of("trackId", rounds.get("h3_web_r1").getTrack().getId(), "weightTotal", 1.0),
                now.minusDays(80))); n++;

        // TEMP_ACCOUNT_CREATE cho judge externals
        for (String k : List.of("judgeE1", "judgeE2", "judgeE3", "judgeE4", "judgeE5")) {
            User u = users.get(k);
            auditLogRepo.save(buildAudit(coord1, "TEMP_ACCOUNT_CREATE", "users", u.getId(),
                    Map.of("email", u.getEmail(), "institution", u.getInstitution()),
                    now.minusDays(50)));
            n++;
        }

        // MENTOR_ASSIGNED + JUDGE_ASSIGNED điển hình
        auditLogRepo.save(buildAudit(coord1, "MENTOR_ASSIGNED", "mentor_assignments", null,
                Map.of("mentorId", users.get("mentor1").getId(), "trackName", "Fullstack Web (H3)"),
                now.minusDays(85))); n++;
        auditLogRepo.save(buildAudit(coord1, "JUDGE_ASSIGNED", "judge_assignments", null,
                Map.of("judgeId", users.get("judgeI1").getId(), "roundName", "Sơ loại Fullstack Web (H3)",
                       "type", "HEAD"),
                now.minusDays(85))); n++;

        log.info("[DataInitializer] Seeded {} audit logs", n);
    }

    private AuditLog buildAudit(User user, String action, String targetTable, Integer targetId,
                                Map<String, Object> detail, LocalDateTime createdAt) {
        JsonNode detailNode = detail == null ? null : objectMapper.valueToTree(detail);
        return AuditLog.builder()
                .user(user).action(action).targetTable(targetTable).targetId(targetId)
                .detail(detailNode).ipAddress("127.0.0.1").createdAt(createdAt)
                .build();
    }
}
