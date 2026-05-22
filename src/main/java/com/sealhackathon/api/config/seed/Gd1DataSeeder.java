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
 * Seed dữ liệu MF-01 Giai đoạn 1 theo {@code docs/workflow/mf01-gd1-timeline-events.md}
 * và {@code docs/workflow/mf01.md} §11.1; DDL {@code docs/db/schema-v3.0-mysql.md} §6.
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

    /**
     * Cập nhật round seed cũ (thiếu / sai {@code examAt}) sau khi bỏ {@code sequence_order}.
     * Gọi mỗi lần start dev, idempotent.
     */
    @Transactional
    public void repairSeededRoundsExamAt() {
        SeedDates dates = computeDates();
        int repaired = 0;
        for (String slug : List.of(
                Gd1SeedConstants.SLUG_READY,
                Gd1SeedConstants.SLUG_ONGOING)) {
            repaired += hackathonRepository.findBySlug(slug)
                    .map(h -> repairRoundsForHackathon(h, dates))
                    .orElse(0);
        }
        if (repaired > 0) {
            log.info("[Gd1DataSeeder] Đã sửa examAt/submissionOpen cho {} round (seed cũ)", repaired);
        }
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

    /**
     * Lịch kiểu Spring 2026 PDF: workshop trước khai mạc, ngày thi = eventEnd (eventStart + 1d).
     * Fall 2025 tham chiếu: 29/10 WS → 1/11 KO → 2/11 thi+trao giải.
     */
    private SeedDates computeDates() {
        LocalDate today = LocalDate.now();
        LocalDate eventStart = today.plusDays(14);
        LocalDate eventEnd = eventStart.plusDays(1);
        LocalDate regStart = today;
        LocalDate regEnd = eventStart.minusDays(2);
        LocalDateTime workshopStart = eventStart.minusDays(2).atTime(20, 0);
        LocalDateTime workshopEnd = eventStart.minusDays(2).atTime(21, 30);
        LocalDateTime kickoffStart = eventStart.atTime(14, 0);
        LocalDateTime kickoffEnd = eventStart.atTime(17, 0);
        LocalDateTime presentationStart = eventEnd.atTime(6, 0);
        LocalDateTime presentationEnd = eventEnd.atTime(17, 0);
        LocalDateTime awardsStart = eventEnd.atTime(17, 30);
        LocalDateTime awardsEnd = eventEnd.atTime(19, 0);
        return new SeedDates(
                regStart,
                regEnd,
                eventStart,
                eventEnd,
                presentationEnd.plusDays(7),
                awardsEnd.plusDays(7),
                workshopStart,
                workshopEnd,
                kickoffStart,
                kickoffEnd,
                presentationStart,
                presentationEnd,
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
                                                boolean prelimActive, boolean includeCloneDemoTrack3,
                                                SeedUsers users, SeedDates dates) {
        if (hackathonRepository.existsBySlug(slug)) {
            Hackathon existing = hackathonRepository.findBySlug(slug).orElseThrow();
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
                if (round.getSubmissionOpen() == null) {
                    round.setSubmissionOpen(dates.prelimSubmissionOpen());
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
            LocalDateTime presentationStart,
            LocalDateTime presentationEnd,
            LocalDateTime awardsStart,
            LocalDateTime awardsEnd) {

        /** Ngày giờ thi sơ loại — trong khung PRESENTATION (Spring: 12/4 sáng). */
        LocalDateTime prelimExamAt() {
            return presentationStart.withHour(8).withMinute(0).withSecond(0).withNano(0);
        }

        /** Ngày giờ thi chung kết — trong khung AWARDS (Spring: 12/4 chiều). */
        LocalDateTime finalExamAt() {
            return awardsStart.plusMinutes(30);
        }

        /** Mở nộp bài sơ loại — sau KICKOFF. */
        LocalDateTime prelimSubmissionOpen() {
            return kickoffEnd;
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
            FullHackathonSeed ongoing) {
    }

    private record CriteriaSeed(String name, CriteriaType type, float weight, int order) {
    }
}
