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
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Seed dữ liệu MF-01 Giai đoạn 1 theo {@code docs/workflow/mf01.md} §11.1
 * và mẫu {@code docs/db/schema-v3.0-mysql.md} §6.
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

    public boolean isAlreadySeeded() {
        return hackathonRepository.existsBySlug(Gd1SeedConstants.SLUG_ONGOING);
    }

    @Transactional
    public SeedSummary seedAll() {
        SeedDates dates = computeDates();
        SeedChapters chapters = seedChapters();
        SeedUsers users = seedUsers(chapters);
        verifyCoordinatorId(users.coordinator());

        Hackathon incomplete = seedIncompleteHackathon(users.coordinator(), dates);
        FullHackathonSeed ready = seedFullHackathon(
                Gd1SeedConstants.SLUG_READY,
                "SEAL GĐ1 Ready (DRAFT)",
                HackathonStatus.DRAFT,
                false,
                users,
                dates);
        FullHackathonSeed ongoing = seedFullHackathon(
                Gd1SeedConstants.SLUG_ONGOING,
                "SEAL Spring 2026",
                HackathonStatus.ONGOING,
                true,
                users,
                dates);

        SeedSummary summary = new SeedSummary(users, incomplete, ready, ongoing);
        logSummary(summary);
        return summary;
    }

    private void verifyCoordinatorId(User coordinator) {
        if (coordinator.getId() == null || coordinator.getId() != 1) {
            log.error("[Gd1DataSeeder] Coordinator phải có id=1 (hiện id={}). "
                            + "StubCurrentUserAccessor giả định userId=1 — truncate DB hoặc ddl-auto=create một lần.",
                    coordinator.getId());
        }
    }

    private SeedDates computeDates() {
        LocalDate today = LocalDate.now();
        LocalDate eventStart = today.plusDays(14);
        LocalDate regStart = today;
        LocalDate regEnd = eventStart.minusDays(1);
        LocalDate eventEnd = eventStart.plusDays(45);
        LocalDateTime now = LocalDateTime.now();
        return new SeedDates(
                regStart,
                regEnd,
                eventStart,
                eventEnd,
                now.plusDays(30),
                now.plusDays(45),
                eventStart.minusDays(5).atTime(20, 0),
                eventStart.minusDays(5).atTime(21, 30),
                eventStart.atTime(14, 0),
                eventStart.atTime(17, 0),
                now.plusDays(31).withHour(6).withMinute(0).withSecond(0).withNano(0),
                now.plusDays(31).withHour(19).withMinute(0).withSecond(0).withNano(0),
                eventEnd.minusDays(5).atTime(8, 0),
                eventEnd.minusDays(5).atTime(18, 0));
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

    private User upsertUser(String email, String fullName, UserRole role, UserType userType,
                            UserStatus status, Chapter chapter, boolean temp, boolean deptHead) {
        Optional<User> existing = userRepository.findByEmail(email);
        if (existing.isPresent()) {
            return existing.get();
        }
        LocalDateTime now = LocalDateTime.now();
        return userRepository.save(User.builder()
                .email(email)
                .fullName(fullName)
                .passwordHash(Gd1SeedConstants.PASSWORD_PLACEHOLDER)
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
                                                boolean prelimActive, SeedUsers users, SeedDates dates) {
        if (hackathonRepository.existsBySlug(slug)) {
            Hackathon existing = hackathonRepository.findBySlug(slug).orElseThrow();
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
                .sequenceOrder(1)
                .isFinal(false)
                .roundType(RoundType.PRELIMINARY)
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
                .sequenceOrder(2)
                .isFinal(true)
                .roundType(RoundType.FINAL)
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

        return new FullHackathonSeed(hackathon, prelim, finalRound, track1, track2);
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
        events.add(event(hackathon, createdBy, "Ngày thi Sơ loại & Thuyết trình",
                EventType.PRESENTATION, "FPT HCM — Hội trường B",
                dates.presentationStart(), dates.presentationEnd()));
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

    private void logSummary(SeedSummary summary) {
        User coord = summary.users().coordinator();
        log.info("""
                [Gd1DataSeeder] Seed MF-01 GĐ1 hoàn tất.
                  Coordinator: id={} email={}
                  Hackathons:
                    - {} (id={}) DRAFT — readiness FAIL
                    - {} (id={}) DRAFT — readiness PASS → PATCH ONGOING
                    - {} (id={}) ONGOING — prelim round id={} isActive={}
                  Tracks (ready): id={} / id={}
                  Users: judge1={}, guest={}, mentor={}, pending={}
                """,
                coord.getId(), coord.getEmail(),
                Gd1SeedConstants.SLUG_INCOMPLETE, summary.incomplete().getId(),
                Gd1SeedConstants.SLUG_READY, summary.ready().hackathon().getId(),
                Gd1SeedConstants.SLUG_ONGOING, summary.ongoing().hackathon().getId(),
                summary.ongoing().prelimRound().getId(),
                summary.ongoing().prelimRound().getIsActive(),
                summary.ready().track1().getId(),
                summary.ready().track2().getId(),
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
            LocalDateTime presentationStart,
            LocalDateTime presentationEnd,
            LocalDateTime awardsStart,
            LocalDateTime awardsEnd) {
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
            Track track2) {

        static FullHackathonSeed existing(Hackathon hackathon) {
            return new FullHackathonSeed(hackathon, null, null, null, null);
        }
    }

    public record SeedSummary(
            SeedUsers users,
            Hackathon incomplete,
            FullHackathonSeed ready,
            FullHackathonSeed ongoing) {
    }

    private record CriteriaSeed(String name, CriteriaType type, float weight, int order) {
    }
}
