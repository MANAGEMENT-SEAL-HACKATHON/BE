package com.sealhackathon.api.config.seed;

import com.sealhackathon.api.chapters.entity.Chapter;
import com.sealhackathon.api.chapters.repository.ChapterRepository;
import com.sealhackathon.api.chapters.value_object.ChapterStatus;
import com.sealhackathon.api.criteria.entity.Criteria;
import com.sealhackathon.api.criteria.repository.CriteriaRepository;
import com.sealhackathon.api.criteria.value_object.CriteriaType;
import com.sealhackathon.api.events.entity.Event;
import com.sealhackathon.api.events.repository.EventRepository;
import com.sealhackathon.api.events.value_object.EventType;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.hackathons.value_object.Season;
import com.sealhackathon.api.judge_assignments.entity.JudgeAssignment;
import com.sealhackathon.api.judge_assignments.repository.JudgeAssignmentRepository;
import com.sealhackathon.api.judge_assignments.value_object.JudgeAssignmentType;
import com.sealhackathon.api.mentor_assignments.entity.MentorAssignment;
import com.sealhackathon.api.mentor_assignments.repository.MentorAssignmentRepository;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.rounds.value_object.LateSubmissionPolicy;
import com.sealhackathon.api.rounds.value_object.RoundType;
import com.sealhackathon.api.rounds.value_object.TiebreakRule;
import com.sealhackathon.api.tracks.entity.Track;
import com.sealhackathon.api.tracks.repository.TrackRepository;
import com.sealhackathon.api.tracks.value_object.TrackStatus;
import com.sealhackathon.api.users.entity.User;
import com.sealhackathon.api.users.repository.UserRepository;
import com.sealhackathon.api.users.value_object.UserRole;
import com.sealhackathon.api.users.value_object.UserStatus;
import com.sealhackathon.api.users.value_object.UserType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Seed dữ liệu MF-01 Giai đoạn 1 theo {@code docs/mf01/04-quy-trinh-van-hanh.md} (Timeline & Events)
 * và {@code docs/mf01/02-functional-requirements.md} §11.1; DDL {@code docs/db/schema-v3.0-mysql.md} §6.
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class Gd1DataSeeder {

    private final ChapterRepository chapterRepository;
    private final UserRepository userRepository;
    private final HackathonRepository hackathonRepository;
    private final RoundRepository roundRepository;
    private final TrackRepository trackRepository;
    private final CriteriaRepository criteriaRepository;
    private final MentorAssignmentRepository mentorAssignmentRepository;
    private final JudgeAssignmentRepository judgeAssignmentRepository;
    private final EventRepository eventRepository;
    private final PasswordEncoder passwordEncoder;

    public boolean isAlreadySeeded() {
        return hackathonRepository.existsBySlug(Gd1SeedConstants.SLUG_ONGOING);
    }

    private static final List<String> SEED_HACKATHON_SLUGS = List.of(
            Gd1SeedConstants.SLUG_INCOMPLETE,
            Gd1SeedConstants.SLUG_READY,
            Gd1SeedConstants.SLUG_ONGOING);

    /**
     * Đồng bộ lịch seed mẫu 24/05–10/06/2026 lên DB dev đã tồn tại (hackathon, events, rounds).
     * Gọi mỗi lần start dev, idempotent.
     */
    @Transactional
    public void repairSeededTimeline() {
        SeedDates dates = computeDates();
        int hackathons = 0;
        int events = 0;
        int rounds = 0;
        if (ensureFinishedHackathonSeed()) {
            hackathons++;
        }
        for (String slug : SEED_HACKATHON_SLUGS) {
            Optional<Hackathon> maybe = hackathonRepository.findBySlug(slug);
            if (maybe.isEmpty()) {
                continue;
            }
            Hackathon h = maybe.get();
            if (repairHackathonCalendar(h, dates)) {
                hackathons++;
            }
            if (!Gd1SeedConstants.SLUG_INCOMPLETE.equals(slug)) {
                events += ensureOrRepairEvents(h, dates);
                rounds += repairRoundsForHackathon(h, dates);
            }
        }
        if (hackathons > 0 || events > 0 || rounds > 0) {
            log.info("""
                    [Gd1DataSeeder] Repair timeline seed data (SEAL 2026 + FINISHED):
                      hackathons={}, events={}, rounds={}""",
                    hackathons, events, rounds);
        }
    }

    /** @deprecated dùng {@link #repairSeededTimeline()} */
    @Deprecated
    @Transactional
    public void repairSeededRoundsExamAt() {
        repairSeededTimeline();
    }

    /**
     * Repair criteria/track sau đổi clone (không FK chéo track) và bổ sung Track 3 demo trên ONGOING.
     */
    @Transactional
    public void repairSeededCriteriaAndTracks() {
        int unlinked = criteriaRepository.unlinkCrossScopeSourceCriteria();
        if (unlinked > 0) {
            log.info("[Gd1DataSeeder] Đã gỡ source_criteria_id chéo track/round cho {} criterion", unlinked);
        }
        int criteriaFilled = 0;
        int track3Added = 0;
        for (String slug : List.of(Gd1SeedConstants.SLUG_READY, Gd1SeedConstants.SLUG_ONGOING)) {
            Optional<Hackathon> hackathon = hackathonRepository.findBySlug(slug);
            if (hackathon.isEmpty()) {
                continue;
            }
            criteriaFilled += ensureTrackCriteriaForHackathon(hackathon.get());
            if (Gd1SeedConstants.SLUG_ONGOING.equals(slug)) {
                track3Added += ensureCloneDemoTrack3(hackathon.get());
            }
        }
        if (criteriaFilled > 0) {
            log.info("[Gd1DataSeeder] Đã bổ sung criteria thiếu cho {} track", criteriaFilled);
        }
        if (track3Added > 0) {
            log.info("[Gd1DataSeeder] Đã thêm Track 3 (clone demo) trên {}", Gd1SeedConstants.SLUG_ONGOING);
        }
    }

    @Transactional
    public SeedSummary seedAll() {
        SeedDates dates = computeDates();
        SeedChapters chapters = seedChapters();
        SeedUsers users = seedUsers(chapters);
        logDevLoginCredentials();
        verifyCoordinatorId(users.coordinator());

        Hackathon incomplete = seedIncompleteHackathon(users.coordinator(), dates);
        FullHackathonSeed ready = seedFullHackathon(
                Gd1SeedConstants.SLUG_READY,
                "SEAL GĐ1 Ready (DRAFT)",
                HackathonStatus.DRAFT,
                false,
                false,
                users,
                dates);
        FullHackathonSeed ongoing = seedFullHackathon(
                Gd1SeedConstants.SLUG_ONGOING,
                "SEAL Spring 2026",
                HackathonStatus.ONGOING,
                true,
                true,
                users,
                dates);
        Hackathon finished = seedFinishedHackathon(users.coordinator());

        SeedSummary summary = new SeedSummary(users, incomplete, ready, ongoing, finished);
        logSummary(summary);
        return summary;
    }

    private boolean ensureFinishedHackathonSeed() {
        if (hackathonRepository.existsBySlug(Gd1SeedConstants.SLUG_FINISHED)) {
            return false;
        }
        return userRepository.findByEmail(Gd1SeedConstants.EMAIL_COORDINATOR)
                .map(this::seedFinishedHackathon)
                .isPresent();
    }

    private Hackathon seedFinishedHackathon(User coordinator) {
        if (hackathonRepository.existsBySlug(Gd1SeedConstants.SLUG_FINISHED)) {
            return hackathonRepository.findBySlug(Gd1SeedConstants.SLUG_FINISHED).orElseThrow();
        }
        SeedDates finishedDates = computeFinishedDates();
        return hackathonRepository.save(Hackathon.builder()
                .name("SEAL Fall 2025 (Completed)")
                .slug(Gd1SeedConstants.SLUG_FINISHED)
                .season(Season.Fall)
                .year(finishedDates.eventStart().getYear())
                .status(HackathonStatus.FINISHED)
                .description("Hackathon đã hoàn thành — seed để test danh sách completed.")
                .registrationStart(finishedDates.regStart())
                .registrationEnd(finishedDates.regEnd())
                .eventStart(finishedDates.eventStart())
                .eventEnd(finishedDates.eventEnd())
                .wildcardEnabled(true)
                .individualRankingEnabled(false)
                .createdBy(coordinator)
                .build());
    }

    private void verifyCoordinatorId(User coordinator) {
        if (coordinator.getId() == null || coordinator.getId() != 1) {
            log.error("[Gd1DataSeeder] Coordinator phải có id=1 (hiện id={}). "
                            + "StubCurrentUserAccessor giả định userId=1 — truncate DB hoặc ddl-auto=create một lần.",
                    coordinator.getId());
        }
    }

    /**
     * Lịch mẫu SEAL 2026 GĐ1: đăng ký 24/05–05/06, WS 06/06, KO 07/06, thi+trao giải 10/06.
     */
    private SeedDates computeDates() {
        LocalDate regStart = LocalDate.of(2026, 5, 24);
        LocalDate regEnd = LocalDate.of(2026, 6, 5);
        LocalDate eventStart = LocalDate.of(2026, 6, 10);
        LocalDate eventEnd = eventStart;
        LocalDateTime workshopStart = LocalDate.of(2026, 6, 6).atTime(20, 0);
        LocalDateTime workshopEnd = LocalDate.of(2026, 6, 6).atTime(21, 30);
        LocalDateTime kickoffStart = LocalDate.of(2026, 6, 7).atTime(14, 0);
        LocalDateTime kickoffEnd = LocalDate.of(2026, 6, 7).atTime(17, 0);
        LocalDateTime awardsStart = eventEnd.atTime(17, 30);
        LocalDateTime awardsEnd = eventEnd.atTime(19, 0);
        LocalDateTime prelimDeadline = eventStart.atTime(11, 30);
        LocalDateTime finalDeadline = eventStart.atTime(16, 30);
        return new SeedDates(
                regStart,
                regEnd,
                eventStart,
                eventEnd,
                prelimDeadline,
                finalDeadline,
                workshopStart,
                workshopEnd,
                kickoffStart,
                kickoffEnd,
                awardsStart,
                awardsEnd);
    }

    /**
     * Lịch completed mẫu (mùa trước): dùng để test filter/status FINISHED.
     */
    private SeedDates computeFinishedDates() {
        LocalDate regStart = LocalDate.of(2025, 10, 1);
        LocalDate regEnd = LocalDate.of(2025, 10, 20);
        LocalDate eventStart = LocalDate.of(2025, 10, 25);
        LocalDate eventEnd = eventStart;
        LocalDateTime workshopStart = LocalDate.of(2025, 10, 22).atTime(19, 30);
        LocalDateTime workshopEnd = LocalDate.of(2025, 10, 22).atTime(21, 0);
        LocalDateTime kickoffStart = LocalDate.of(2025, 10, 25).atTime(8, 0);
        LocalDateTime kickoffEnd = LocalDate.of(2025, 10, 25).atTime(9, 30);
        LocalDateTime awardsStart = eventEnd.atTime(17, 30);
        LocalDateTime awardsEnd = eventEnd.atTime(19, 0);
        LocalDateTime prelimDeadline = eventStart.atTime(12, 0);
        LocalDateTime finalDeadline = eventStart.atTime(16, 30);
        return new SeedDates(
                regStart,
                regEnd,
                eventStart,
                eventEnd,
                prelimDeadline,
                finalDeadline,
                workshopStart,
                workshopEnd,
                kickoffStart,
                kickoffEnd,
                awardsStart,
                awardsEnd);
    }

    private SeedChapters seedChapters() {
        Chapter hcm = upsertChapter(
                Gd1SeedConstants.CHAPTER_FPT_HCM,
                "FPT University Ho Chi Minh City",
                "FPT University",
                "Ho Chi Minh City");
        Chapter hn = upsertChapter(
                Gd1SeedConstants.CHAPTER_FPT_HN,
                "FPT University Hanoi",
                "FPT University",
                "Hanoi");
        Chapter ext = upsertChapter(
                Gd1SeedConstants.CHAPTER_EXT,
                "External Participants",
                null,
                null);
        return new SeedChapters(hcm, hn, ext);
    }

    private Chapter upsertChapter(String code, String name, String university, String city) {
        return chapterRepository.findByCode(code).orElseGet(() -> {
            LocalDateTime now = LocalDateTime.now();
            return chapterRepository.save(Chapter.builder()
                    .code(code)
                    .name(name)
                    .university(university)
                    .city(city)
                    .status(ChapterStatus.ACTIVE)
                    .createdAt(now)
                    .build());
        });
    }

    private SeedUsers seedUsers(SeedChapters chapters) {
        User coordinator = upsertUser(
                Gd1SeedConstants.EMAIL_COORDINATOR,
                "Nguyễn Văn Coordinator",
                UserRole.COORDINATOR,
                UserType.INTERNAL,
                UserStatus.APPROVED,
                chapters.hcm(),
                false,
                false);
        User judge1 = upsertUser(
                Gd1SeedConstants.EMAIL_JUDGE1,
                "Trần Thị Judge Internal",
                UserRole.JUDGE,
                UserType.INTERNAL,
                UserStatus.APPROVED,
                chapters.hcm(),
                false,
                false);
        User guestJudge = upsertUser(
                Gd1SeedConstants.EMAIL_GUEST_JUDGE,
                "Lê Văn Judge External",
                UserRole.JUDGE,
                UserType.EXTERNAL,
                UserStatus.APPROVED,
                chapters.ext(),
                true,
                false);
        User mentor = upsertUser(
                Gd1SeedConstants.EMAIL_MENTOR,
                "Phạm Minh Mentor",
                UserRole.MENTOR,
                UserType.INTERNAL,
                UserStatus.APPROVED,
                chapters.hcm(),
                false,
                false);
        User judge2 = upsertUser(
                Gd1SeedConstants.EMAIL_JUDGE2,
                "Hoàng Judge Two",
                UserRole.JUDGE,
                UserType.INTERNAL,
                UserStatus.APPROVED,
                chapters.hcm(),
                false,
                false);
        User pendingJudge = upsertUser(
                Gd1SeedConstants.EMAIL_PENDING_JUDGE,
                "Pending Judge",
                UserRole.JUDGE,
                UserType.INTERNAL,
                UserStatus.PENDING,
                chapters.hcm(),
                false,
                false);
        return new SeedUsers(coordinator, judge1, judge2, guestJudge, mentor, pendingJudge);
    }

    /**
     * Repair user seed cũ (password placeholder) → bcrypt dev; log bảng login mỗi lần start dev.
     */
    @Transactional
    public void repairDevUserPasswords() {
        for (String email : Gd1SeedConstants.seedEmails()) {
            String plain = Gd1SeedConstants.devPasswordFor(email);
            if (plain == null) {
                continue;
            }
            userRepository.findByEmail(email).ifPresent(user -> {
                if (needsDevPasswordRepair(user.getPasswordHash())) {
                    applyDevPassword(user, plain, "repair");
                } else if (user.getStatus() == UserStatus.APPROVED && user.getEmailVerifiedAt() == null) {
                    user.setEmailVerifiedAt(LocalDateTime.now());
                    user.setUpdatedAt(LocalDateTime.now());
                    userRepository.save(user);
                }
            });
        }
        logDevLoginCredentials();
    }

    private static boolean needsDevPasswordRepair(String passwordHash) {
        return passwordHash == null
                || Gd1SeedConstants.PASSWORD_PLACEHOLDER.equals(passwordHash)
                || !passwordHash.startsWith("$2");
    }

    private User upsertUser(String email, String fullName, UserRole role, UserType userType,
                            UserStatus status, Chapter chapter, boolean temp, boolean deptHead) {
        String plainPassword = Gd1SeedConstants.devPasswordFor(email);
        if (plainPassword == null) {
            throw new IllegalStateException("Chưa cấu hình dev password cho email seed: " + email);
        }
        Optional<User> existing = userRepository.findByEmail(email);
        if (existing.isPresent()) {
            User user = existing.get();
            if (needsDevPasswordRepair(user.getPasswordHash())) {
                applyDevPassword(user, plainPassword, "upsert");
            } else if (user.getStatus() == UserStatus.APPROVED && user.getEmailVerifiedAt() == null) {
                user.setEmailVerifiedAt(LocalDateTime.now());
                user.setUpdatedAt(LocalDateTime.now());
                userRepository.save(user);
            }
            return user;
        }
        LocalDateTime now = LocalDateTime.now();
        String passwordHash = encodeAndLogPassword(email, plainPassword, "insert");
        boolean verified = status == UserStatus.APPROVED;
        return userRepository.save(User.builder()
                .email(email)
                .fullName(fullName)
                .passwordHash(passwordHash)
                .emailVerifiedAt(verified ? now : null)
                .role(role)
                .userType(userType)
                .status(status)
                .chapter(chapter)
                .isTempAccount(temp)
                .isDeptHead(deptHead)
                .createdAt(now)
                .updatedAt(now)
                .build());
    }

    private void applyDevPassword(User user, String plainPassword, String action) {
        String hash = encodeAndLogPassword(user.getEmail(), plainPassword, action);
        user.setPasswordHash(hash);
        user.setMustChangePassword(false);
        if (user.getStatus() == UserStatus.APPROVED && user.getEmailVerifiedAt() == null) {
            user.setEmailVerifiedAt(LocalDateTime.now());
        }
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    private String encodeAndLogPassword(String email, String plainPassword, String action) {
        String hash = passwordEncoder.encode(plainPassword);
        log.info("[Gd1DataSeeder] {} | email={} | Password={} | passwordHash={}",
                action, email, plainPassword, hash);
        return hash;
    }

    private void logDevLoginCredentials() {
        log.info("""
                [Gd1DataSeeder] ========== Dev login (MF-02, profile dev) ==========
                  {}  Password: {}
                  {}  Password: {}
                  {}  Password: {}
                  {}  Password: {}
                  {}  Password: {}
                  {}  Password: {}  (status PENDING — login 401 cho đến khi duyệt)
                ================================================================
                """,
                Gd1SeedConstants.EMAIL_COORDINATOR, Gd1SeedConstants.DEV_COORDINATOR_PASSWORD,
                Gd1SeedConstants.EMAIL_JUDGE1, Gd1SeedConstants.DEV_JUDGE_PASSWORD,
                Gd1SeedConstants.EMAIL_JUDGE2, Gd1SeedConstants.DEV_JUDGE_PASSWORD,
                Gd1SeedConstants.EMAIL_GUEST_JUDGE, Gd1SeedConstants.DEV_GUEST_JUDGE_PASSWORD,
                Gd1SeedConstants.EMAIL_MENTOR, Gd1SeedConstants.DEV_MENTOR_PASSWORD,
                Gd1SeedConstants.EMAIL_PENDING_JUDGE, Gd1SeedConstants.DEV_PENDING_JUDGE_PASSWORD);
    }

    private Hackathon seedIncompleteHackathon(User coordinator, SeedDates dates) {
        if (hackathonRepository.existsBySlug(Gd1SeedConstants.SLUG_INCOMPLETE)) {
            return hackathonRepository.findBySlug(Gd1SeedConstants.SLUG_INCOMPLETE).orElseThrow();
        }
        return hackathonRepository.save(Hackathon.builder()
                .name("SEAL GĐ1 Incomplete")
                .slug(Gd1SeedConstants.SLUG_INCOMPLETE)
                .season(Season.Spring)
                .year(dates.eventStart().getYear())
                .status(HackathonStatus.DRAFT)
                .description("Hackathon DRAFT không có round — test readiness blockers")
                .registrationStart(dates.regStart())
                .registrationEnd(dates.regEnd())
                .eventStart(dates.eventStart())
                .eventEnd(dates.eventEnd())
                .wildcardEnabled(true)
                .individualRankingEnabled(false)
                .createdBy(coordinator)
                .build());
    }

    private FullHackathonSeed seedFullHackathon(String slug, String name, HackathonStatus status,
                                                boolean prelimActive, boolean includeCloneDemoTrack3,
                                                SeedUsers users, SeedDates dates) {
        if (hackathonRepository.existsBySlug(slug)) {
            Hackathon existing = hackathonRepository.findBySlug(slug).orElseThrow();
            repairHackathonCalendar(existing, dates);
            ensureOrRepairEvents(existing, dates);
            repairRoundsForHackathon(existing, dates);
            ensureTrackCriteriaForHackathon(existing);
            if (includeCloneDemoTrack3) {
                ensureCloneDemoTrack3(existing);
            }
            log.info("[Gd1DataSeeder] Hackathon '{}' đã tồn tại (id={})", slug, existing.getId());
            return FullHackathonSeed.existing(existing);
        }

        Hackathon hackathon = hackathonRepository.save(Hackathon.builder()
                .name(name)
                .slug(slug)
                .season(Season.Spring)
                .year(dates.eventStart().getYear())
                .status(status)
                .description("Cuộc thi lập trình SEAL — seed MF-01 GĐ1")
                .registrationStart(dates.regStart())
                .registrationEnd(dates.regEnd())
                .eventStart(dates.eventStart())
                .eventEnd(dates.eventEnd())
                .wildcardEnabled(true)
                .individualRankingEnabled(false)
                .createdBy(users.coordinator())
                .build());

        Round prelim = roundRepository.save(Round.builder()
                .hackathon(hackathon)
                .name("Vòng Sơ loại")
                .examAt(dates.prelimExamAt())
                .isFinal(false)
                .roundType(RoundType.PRELIMINARY)
                .submissionOpen(dates.prelimSubmissionOpen())
                .submissionDeadline(dates.prelimDeadline())
                .codingDurationHours(7)
                .lateSubmissionPolicy(LateSubmissionPolicy.ALLOW_LATE_PENDING)
                .topNAdvance(2)
                .wildcardEnabled(true)
                .minTeamsFinal(6)
                .tiebreakRule(TiebreakRule.PENALTY_SCORE)
                .isActive(prelimActive)
                .build());

        Round finalRound = roundRepository.save(Round.builder()
                .hackathon(hackathon)
                .name("Vòng Chung kết")
                .examAt(dates.finalExamAt())
                .isFinal(true)
                .roundType(RoundType.FINAL)
                .submissionOpen(dates.finalSubmissionOpen())
                .submissionDeadline(dates.finalDeadline())
                .lateSubmissionPolicy(LateSubmissionPolicy.HARD_LOCK)
                .wildcardEnabled(false)
                .tiebreakRule(TiebreakRule.PENALTY_SCORE)
                .isActive(false)
                .build());

        Track track1 = trackRepository.save(Track.builder()
                .round(prelim)
                .name("Track 1 — RAG Pipeline")
                .description("Xây dựng hệ thống RAG")
                .topic("Business Analysis App")
                .maxTeams(8)
                .maxTeamsPerGroup(8)
                .minTeamSize(3)
                .maxTeamSize(5)
                .sequenceOrder(1)
                .status(TrackStatus.OPEN)
                .build());

        Track track2 = trackRepository.save(Track.builder()
                .round(prelim)
                .name("Track 2 — AI Agent")
                .description("Thiết kế AI Agent")
                .topic("Process Automation Agent")
                .maxTeams(8)
                .maxTeamsPerGroup(8)
                .minTeamSize(3)
                .maxTeamSize(5)
                .sequenceOrder(2)
                .status(TrackStatus.OPEN)
                .build());

        seedTrackCriteria(track1);
        seedTrackCriteria(track2);
        Track track3 = null;
        if (includeCloneDemoTrack3) {
            track3 = trackRepository.save(Track.builder()
                    .round(prelim)
                    .name(Gd1SeedConstants.TRACK3_CLONE_DEMO_NAME)
                    .description("Bảng trống criteria — test POST clone từ Track 2")
                    .topic("EV Charging & Integration")
                    .maxTeams(8)
                    .maxTeamsPerGroup(8)
                    .minTeamSize(3)
                    .maxTeamSize(5)
                    .sequenceOrder(3)
                    .status(TrackStatus.OPEN)
                    .build());
        }
        seedFinalCriteria(finalRound);

        User coord = users.coordinator();
        LocalDateTime assignedAt = LocalDateTime.now();
        mentorAssignmentRepository.save(MentorAssignment.builder()
                .mentor(users.mentor())
                .track(track1)
                .assignedBy(coord)
                .assignedAt(assignedAt)
                .build());
        mentorAssignmentRepository.save(MentorAssignment.builder()
                .mentor(users.mentor())
                .track(track2)
                .assignedBy(coord)
                .assignedAt(assignedAt)
                .build());

        saveJudgeAssignment(users.judge1(), track1, coord, assignedAt);
        saveJudgeAssignment(users.guestJudge(), track1, coord, assignedAt);
        saveJudgeAssignment(users.judge1(), track2, coord, assignedAt);
        saveJudgeAssignment(users.judge2(), track2, coord, assignedAt);

        seedEvents(hackathon, coord, dates);

        return new FullHackathonSeed(hackathon, prelim, finalRound, track1, track2, track3);
    }

    private void saveJudgeAssignment(User judge, Track track, User assignedBy, LocalDateTime assignedAt) {
        judgeAssignmentRepository.save(JudgeAssignment.builder()
                .judge(judge)
                .track(track)
                .round(null)
                .assignmentType(JudgeAssignmentType.NORMAL)
                .assignedBy(assignedBy)
                .assignedAt(assignedAt)
                .build());
    }

    private void seedTrackCriteria(Track track) {
        List<CriteriaSeed> rows = List.of(
                new CriteriaSeed("Domain Accuracy", CriteriaType.TECHNICAL, 0.30f, 1),
                new CriteriaSeed("Kiến trúc RAG", CriteriaType.TECHNICAL, 0.30f, 2),
                new CriteriaSeed("Ý tưởng & Thuyết trình", CriteriaType.SOFT_SKILL, 0.15f, 3),
                new CriteriaSeed("Thực thi & Sáng tạo", CriteriaType.TECHNICAL, 0.15f, 4),
                new CriteriaSeed("UX & Giao diện", CriteriaType.SOFT_SKILL, 0.10f, 5));
        for (CriteriaSeed row : rows) {
            criteriaRepository.save(Criteria.builder()
                    .track(track)
                    .round(null)
                    .sourceCriteria(null)
                    .name(row.name())
                    .type(row.type())
                    .weight(row.weight())
                    .maxScore(10)
                    .displayOrder(row.order())
                    .build());
        }
    }

    private void seedFinalCriteria(Round finalRound) {
        List<CriteriaSeed> rows = List.of(
                new CriteriaSeed("Xử lý & Truy xuất", CriteriaType.TECHNICAL, 0.30f, 1),
                new CriteriaSeed("Độ tin cậy", CriteriaType.TECHNICAL, 0.20f, 2),
                new CriteriaSeed("Tư duy Agent", CriteriaType.TECHNICAL, 0.20f, 3),
                new CriteriaSeed("Thực tế & Triển khai", CriteriaType.TECHNICAL, 0.20f, 4),
                new CriteriaSeed("Mở rộng & Scale", CriteriaType.SOFT_SKILL, 0.10f, 5));
        for (CriteriaSeed row : rows) {
            criteriaRepository.save(Criteria.builder()
                    .track(null)
                    .round(finalRound)
                    .sourceCriteria(null)
                    .name(row.name())
                    .type(row.type())
                    .weight(row.weight())
                    .maxScore(10)
                    .displayOrder(row.order())
                    .build());
        }
    }

    private void seedEvents(Hackathon hackathon, User createdBy, SeedDates dates) {
        List<Event> events = new ArrayList<>();
        events.add(event(hackathon, createdBy, "Workshop: RAG & AI Agent Fundamentals",
                EventType.WORKSHOP, "Online (Teams)",
                dates.workshopStart(), dates.workshopEnd()));
        events.add(event(hackathon, createdBy, "Lễ Khai mạc & Bốc thăm chia Track",
                EventType.KICKOFF, "FPT HCM — Hội trường A",
                dates.kickoffStart(), dates.kickoffEnd()));
        events.add(event(hackathon, createdBy, "Vòng Chung kết & Trao giải",
                EventType.AWARDS, "FPT HCM — Hội trường A",
                dates.awardsStart(), dates.awardsEnd()));
        eventRepository.saveAll(events);
    }

    private static Event event(Hackathon hackathon, User createdBy, String title, EventType type,
                               String location, LocalDateTime startsAt, LocalDateTime endsAt) {
        return Event.builder()
                .hackathon(hackathon)
                .title(title)
                .type(type)
                .location(location)
                .startsAt(startsAt)
                .endsAt(endsAt)
                .isPublic(true)
                .createdBy(createdBy)
                .build();
    }

    private int ensureTrackCriteriaForHackathon(Hackathon hackathon) {
        int filled = 0;
        for (Round round : roundRepository.findByHackathon_IdOrderByExamAtAsc(hackathon.getId())) {
            if (Boolean.TRUE.equals(round.getIsFinal())) {
                if (criteriaRepository.countNormalByFinalRoundId(round.getId()) == 0) {
                    seedFinalCriteria(round);
                    filled++;
                }
                continue;
            }
            for (Track track : trackRepository.findByRoundIdOrderBySequenceOrderAsc(round.getId())) {
                if (criteriaRepository.countByTrackId(track.getId()) == 0) {
                    seedTrackCriteria(track);
                    filled++;
                }
            }
        }
        return filled;
    }

    /**
     * Track 3 không criteria — test {@code GET clone-sources} + {@code POST clone} từ Track 2.
     */
    private int ensureCloneDemoTrack3(Hackathon hackathon) {
        Round prelim = roundRepository.findByHackathon_IdOrderByExamAtAsc(hackathon.getId()).stream()
                .filter(r -> !Boolean.TRUE.equals(r.getIsFinal()))
                .findFirst()
                .orElse(null);
        if (prelim == null) {
            return 0;
        }
        boolean hasTrack3 = trackRepository.findByRoundIdOrderBySequenceOrderAsc(prelim.getId()).stream()
                .anyMatch(t -> Gd1SeedConstants.TRACK3_CLONE_DEMO_NAME.equals(t.getName()));
        if (hasTrack3) {
            return 0;
        }
        int nextOrder = trackRepository.findByRoundIdOrderBySequenceOrderAsc(prelim.getId()).stream()
                .mapToInt(Track::getSequenceOrder)
                .max()
                .orElse(0) + 1;
        trackRepository.save(Track.builder()
                .round(prelim)
                .name(Gd1SeedConstants.TRACK3_CLONE_DEMO_NAME)
                .description("Bảng trống criteria — test POST clone từ Track 2")
                .topic("EV Charging & Integration")
                .maxTeams(8)
                .maxTeamsPerGroup(8)
                .minTeamSize(3)
                .maxTeamSize(5)
                .sequenceOrder(nextOrder)
                .status(TrackStatus.OPEN)
                .build());
        return 1;
    }

    private boolean repairHackathonCalendar(Hackathon hackathon, SeedDates dates) {
        boolean changed = false;
        if (!dates.regStart().equals(hackathon.getRegistrationStart())) {
            hackathon.setRegistrationStart(dates.regStart());
            changed = true;
        }
        if (!dates.regEnd().equals(hackathon.getRegistrationEnd())) {
            hackathon.setRegistrationEnd(dates.regEnd());
            changed = true;
        }
        if (!dates.eventStart().equals(hackathon.getEventStart())) {
            hackathon.setEventStart(dates.eventStart());
            changed = true;
        }
        if (!dates.eventEnd().equals(hackathon.getEventEnd())) {
            hackathon.setEventEnd(dates.eventEnd());
            changed = true;
        }
        if (hackathon.getYear() == null || hackathon.getYear() != dates.eventStart().getYear()) {
            hackathon.setYear(dates.eventStart().getYear());
            changed = true;
        }
        if (changed) {
            hackathonRepository.save(hackathon);
        }
        return changed;
    }

    /**
     * Cập nhật milestone đã có; tạo đủ 3 loại nếu hackathon full seed thiếu event.
     */
    private int ensureOrRepairEvents(Hackathon hackathon, SeedDates dates) {
        int updated = repairEventsForHackathon(hackathon, dates);
        boolean hasWorkshop = !eventRepository
                .findByHackathonIdAndType(hackathon.getId(), EventType.WORKSHOP).isEmpty();
        boolean hasKickoff = !eventRepository
                .findByHackathonIdAndType(hackathon.getId(), EventType.KICKOFF).isEmpty();
        boolean hasAwards = !eventRepository
                .findByHackathonIdAndType(hackathon.getId(), EventType.AWARDS).isEmpty();
        if (!hasWorkshop || !hasKickoff || !hasAwards) {
            User createdBy = hackathon.getCreatedBy();
            if (createdBy == null) {
                return updated;
            }
            if (!hasWorkshop) {
                eventRepository.save(event(hackathon, createdBy,
                        "Workshop: RAG & AI Agent Fundamentals",
                        EventType.WORKSHOP, "Online (Teams)",
                        dates.workshopStart(), dates.workshopEnd()));
                updated++;
            }
            if (!hasKickoff) {
                eventRepository.save(event(hackathon, createdBy,
                        "Lễ Khai mạc & Bốc thăm chia Track",
                        EventType.KICKOFF, "FPT HCM — Hội trường A",
                        dates.kickoffStart(), dates.kickoffEnd()));
                updated++;
            }
            if (!hasAwards) {
                eventRepository.save(event(hackathon, createdBy,
                        "Vòng Chung kết & Trao giải",
                        EventType.AWARDS, "FPT HCM — Hội trường A",
                        dates.awardsStart(), dates.awardsEnd()));
                updated++;
            }
        }
        return updated;
    }

    private int repairEventsForHackathon(Hackathon hackathon, SeedDates dates) {
        int count = 0;
        count += repairEventIfPresent(hackathon, EventType.WORKSHOP,
                dates.workshopStart(), dates.workshopEnd());
        count += repairEventIfPresent(hackathon, EventType.KICKOFF,
                dates.kickoffStart(), dates.kickoffEnd());
        count += repairEventIfPresent(hackathon, EventType.AWARDS,
                dates.awardsStart(), dates.awardsEnd());
        return count;
    }

    private int repairEventIfPresent(Hackathon hackathon, EventType type,
                                     LocalDateTime startsAt, LocalDateTime endsAt) {
        int count = 0;
        for (Event event : eventRepository.findByHackathonIdAndType(hackathon.getId(), type)) {
            boolean changed = false;
            if (!startsAt.equals(event.getStartsAt())) {
                event.setStartsAt(startsAt);
                changed = true;
            }
            if (!endsAt.equals(event.getEndsAt())) {
                event.setEndsAt(endsAt);
                changed = true;
            }
            if (changed) {
                eventRepository.save(event);
                count++;
            }
        }
        return count;
    }

    private int repairRoundsForHackathon(Hackathon hackathon, SeedDates dates) {
        List<Round> rounds = roundRepository.findByHackathon_IdOrderByExamAtAsc(hackathon.getId());
        if (rounds.isEmpty()) {
            return 0;
        }
        Round finalRound = rounds.stream()
                .filter(r -> Boolean.TRUE.equals(r.getIsFinal()))
                .findFirst()
                .orElse(null);
        int count = 0;
        for (Round round : rounds) {
            boolean changed = false;
            if (Boolean.TRUE.equals(round.getIsFinal())) {
                if (round.getExamAt() == null || !round.getExamAt().equals(dates.finalExamAt())) {
                    round.setExamAt(dates.finalExamAt());
                    changed = true;
                }
                if (round.getSubmissionOpen() == null
                        || !round.getSubmissionOpen().equals(dates.finalSubmissionOpen())) {
                    round.setSubmissionOpen(dates.finalSubmissionOpen());
                    changed = true;
                }
                if (round.getSubmissionDeadline() == null
                        || !round.getSubmissionDeadline().equals(dates.finalDeadline())) {
                    round.setSubmissionDeadline(dates.finalDeadline());
                    changed = true;
                }
            } else {
                LocalDateTime targetExam = dates.prelimExamAt();
                if (finalRound != null && finalRound.getExamAt() != null
                        && !targetExam.isBefore(finalRound.getExamAt())) {
                    targetExam = finalRound.getExamAt().minusHours(1);
                }
                if (round.getExamAt() == null || !round.getExamAt().equals(targetExam)) {
                    round.setExamAt(targetExam);
                    changed = true;
                }
                if (round.getSubmissionOpen() == null
                        || !round.getSubmissionOpen().equals(dates.prelimSubmissionOpen())) {
                    round.setSubmissionOpen(dates.prelimSubmissionOpen());
                    changed = true;
                }
                if (round.getSubmissionDeadline() == null
                        || !round.getSubmissionDeadline().equals(dates.prelimDeadline())) {
                    round.setSubmissionDeadline(dates.prelimDeadline());
                    changed = true;
                }
            }
            if (changed) {
                roundRepository.save(round);
                count++;
            }
        }
        return count;
    }

    private void logSummary(SeedSummary summary) {
        User coord = summary.users().coordinator();
        log.info("""
                [Gd1DataSeeder] Seed MF-01 GĐ1 hoàn tất.
                  Coordinator: id={} email={}
                  Hackathons:
                    - {} (id={}) DRAFT — readiness FAIL
                    - {} (id={}) DRAFT — readiness PASS → PATCH ONGOING
                    - {} (id={}) ONGOING — prelim id={} active={} examAt={}
                    - final examAt={}
                    - {} (id={}) FINISHED — completed dataset
                  Tracks ready: t1={} t2={}
                  Track 3 clone demo (ongoing): id={}
                  Users: judge1={}, guest={}, mentor={}, pending={}
                """,
                coord.getId(), coord.getEmail(),
                Gd1SeedConstants.SLUG_INCOMPLETE, summary.incomplete().getId(),
                Gd1SeedConstants.SLUG_READY, summary.ready().hackathon().getId(),
                Gd1SeedConstants.SLUG_ONGOING, summary.ongoing().hackathon().getId(),
                summary.ongoing().prelimRound() != null
                        ? summary.ongoing().prelimRound().getId() : "n/a",
                summary.ongoing().prelimRound() != null
                        ? summary.ongoing().prelimRound().getIsActive() : false,
                summary.ongoing().prelimRound() != null
                        ? summary.ongoing().prelimRound().getExamAt() : "n/a",
                summary.ongoing().finalRound() != null
                        ? summary.ongoing().finalRound().getExamAt() : "n/a",
                Gd1SeedConstants.SLUG_FINISHED, summary.finished().getId(),
                summary.ready().track1() != null ? summary.ready().track1().getId() : "n/a",
                summary.ready().track2() != null ? summary.ready().track2().getId() : "n/a",
                summary.ongoing().track3() != null ? summary.ongoing().track3().getId() : "n/a",
                summary.users().judge1().getId(),
                summary.users().guestJudge().getId(),
                summary.users().mentor().getId(),
                summary.users().pendingJudge().getId());
    }

    private record SeedDates(
            LocalDate regStart,
            LocalDate regEnd,
            LocalDate eventStart,
            LocalDate eventEnd,
            LocalDateTime prelimDeadline,
            LocalDateTime finalDeadline,
            LocalDateTime workshopStart,
            LocalDateTime workshopEnd,
            LocalDateTime kickoffStart,
            LocalDateTime kickoffEnd,
            LocalDateTime awardsStart,
            LocalDateTime awardsEnd) {

        /** Ngày giờ thi sơ loại — đầu ngày eventStart. */
        LocalDateTime prelimExamAt() {
            return eventStart.atTime(8, 0);
        }

        /** Ngày giờ thi chung kết — cùng ngày thi, sau sơ loại. */
        LocalDateTime finalExamAt() {
            return eventStart.atTime(13, 0);
        }

        /** Mở nộp bài sơ loại — sau examAt sơ loại. */
        LocalDateTime prelimSubmissionOpen() {
            return eventStart.atTime(9, 0);
        }

        LocalDateTime finalSubmissionOpen() {
            return eventStart.atTime(14, 0);
        }
    }

    private record SeedChapters(Chapter hcm, Chapter hn, Chapter ext) {
    }

    public record SeedUsers(
            User coordinator,
            User judge1,
            User judge2,
            User guestJudge,
            User mentor,
            User pendingJudge) {
    }

    public record FullHackathonSeed(
            Hackathon hackathon,
            Round prelimRound,
            Round finalRound,
            Track track1,
            Track track2,
            Track track3) {

        static FullHackathonSeed existing(Hackathon hackathon) {
            return new FullHackathonSeed(hackathon, null, null, null, null, null);
        }
    }

    public record SeedSummary(
            SeedUsers users,
            Hackathon incomplete,
            FullHackathonSeed ready,
            FullHackathonSeed ongoing,
            Hackathon finished) {
    }

    private record CriteriaSeed(String name, CriteriaType type, float weight, int order) {
    }
}
