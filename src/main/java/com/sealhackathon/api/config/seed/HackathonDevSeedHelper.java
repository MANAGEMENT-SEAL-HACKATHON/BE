package com.sealhackathon.api.config.seed;

import com.sealhackathon.api.chapters.entity.Chapter;
import com.sealhackathon.api.chapters.repository.ChapterRepository;
import com.sealhackathon.api.criteria.entity.Criteria;
import com.sealhackathon.api.criteria.repository.CriteriaRepository;
import com.sealhackathon.api.criteria.value_object.CriteriaType;
import com.sealhackathon.api.events.entity.Event;
import com.sealhackathon.api.events.entity.PresentationSlot;
import com.sealhackathon.api.events.repository.EventRepository;
import com.sealhackathon.api.events.repository.PresentationSlotRepository;
import com.sealhackathon.api.events.value_object.EventType;
import com.sealhackathon.api.presentation.value_object.PresentationQueueStatus;
import com.sealhackathon.api.presentation.value_object.PresentationTimerPhase;
import com.sealhackathon.api.hackathon_registrations.entity.HackathonRegistration;
import com.sealhackathon.api.hackathon_registrations.repository.HackathonRegistrationRepository;
import com.sealhackathon.api.invitations.entity.Invitation;
import com.sealhackathon.api.invitations.repository.InvitationRepository;
import com.sealhackathon.api.scores.entity.Score;
import com.sealhackathon.api.scores.repository.ScoreRepository;
import com.sealhackathon.api.scores.value_object.ScoreType;
import com.sealhackathon.api.submissions.entity.Submission;
import com.sealhackathon.api.submissions.repository.SubmissionRepository;
import com.sealhackathon.api.submissions.value_object.SubmissionStatus;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.hackathons.value_object.Season;
import com.sealhackathon.api.judge_assignments.entity.JudgeAssignment;
import com.sealhackathon.api.judge_assignments.repository.JudgeAssignmentRepository;
import com.sealhackathon.api.judge_assignments.value_object.JudgeAssignmentType;
import com.sealhackathon.api.mentor_assignments.entity.MentorAssignment;
import com.sealhackathon.api.prizes.entity.Prize;
import com.sealhackathon.api.prizes.repository.PrizeRepository;
import com.sealhackathon.api.prizes.value_object.PrizeRank;
import com.sealhackathon.api.mentor_assignments.repository.MentorAssignmentRepository;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.rounds.value_object.LateSubmissionPolicy;
import com.sealhackathon.api.rounds.value_object.RoundType;
import com.sealhackathon.api.rounds.value_object.TiebreakRule;
import com.sealhackathon.api.team_members.entity.TeamMember;
import com.sealhackathon.api.team_members.entity.TeamMemberId;
import com.sealhackathon.api.team_members.repository.TeamMemberRepository;
import com.sealhackathon.api.team_members.value_object.TeamMemberRole;
import com.sealhackathon.api.team_members.value_object.TeamMemberStatus;
import com.sealhackathon.api.team_round_participation.entity.TeamRoundParticipation;
import com.sealhackathon.api.team_round_participation.repository.TeamRoundParticipationRepository;
import com.sealhackathon.api.team_round_tracks.entity.TeamRoundTrack;
import com.sealhackathon.api.team_round_tracks.repository.TeamRoundTrackRepository;
import com.sealhackathon.api.team_round_participation.value_object.ParticipationStatus;
import com.sealhackathon.api.team_round_tracks.value_object.RegistrationType;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Helper tạo hackathon + rounds + tracks + criteria + judge (dev seed GĐ3/GĐ4/GĐ6).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HackathonDevSeedHelper {

    private final ChapterRepository chapterRepository;
    private final UserRepository userRepository;
    private final HackathonRepository hackathonRepository;
    private final RoundRepository roundRepository;
    private final TrackRepository trackRepository;
    private final CriteriaRepository criteriaRepository;
    private final MentorAssignmentRepository mentorAssignmentRepository;
    private final JudgeAssignmentRepository judgeAssignmentRepository;
    private final EventRepository eventRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamRoundParticipationRepository teamRoundParticipationRepository;
    private final TeamRoundTrackRepository teamRoundTrackRepository;
    private final SubmissionRepository submissionRepository;
    private final ScoreRepository scoreRepository;
    private final InvitationRepository invitationRepository;
    private final HackathonRegistrationRepository hackathonRegistrationRepository;
    private final PrizeRepository prizeRepository;
    private final PresentationSlotRepository presentationSlotRepository;
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    public record SeedDates(
            LocalDate regStart,
            LocalDate regEnd,
            LocalDate eventStart,
            LocalDate eventEnd,
            LocalDateTime prelimDeadline,
            LocalDateTime finalDeadline,
            LocalDateTime prelimExamAt,
            LocalDateTime finalExamAt,
            LocalDateTime prelimSubmissionOpen,
            LocalDateTime finalSubmissionOpen) {
    }

    public record PrelimState(
            boolean active,
            boolean problemReleased,
            boolean scoringLocked,
            boolean published,
            Integer topNAdvance,
            Integer minTeamsFinal) {
    }

    public record FinalState(boolean active, boolean scoringLocked) {
    }

    public record HackathonStructure(
            Hackathon hackathon,
            Round prelim,
            Round finalRound,
            Track track1,
            Track track2) {
    }

    public SeedDates computeRelativeDates() {
        LocalDate today = LocalDate.now();
        LocalDate regStart = today.minusDays(14);
        LocalDate regEnd = today.plusDays(14);
        LocalDate eventStart = today.plusDays(15);
        LocalDate eventEnd = eventStart.plusDays(45);
        int prelimHours = RoundScheduleSeedUtil.DEFAULT_PRELIM_CODING_HOURS;
        LocalDateTime prelimExamAt = eventStart.atTime(8, 0);
        LocalDateTime prelimOpen = RoundScheduleSeedUtil.submissionOpen(prelimExamAt, prelimHours);
        LocalDateTime prelimDeadline = RoundScheduleSeedUtil.submissionDeadline(prelimExamAt, prelimHours);
        LocalDateTime finalExamAt = RoundScheduleSeedUtil.minFinalExamAt(prelimExamAt, prelimHours);
        LocalDateTime finalOpen = finalExamAt;
        LocalDateTime finalDeadline = eventStart.atTime(16, 30);
        return new SeedDates(
                regStart,
                regEnd,
                eventStart,
                eventEnd,
                prelimDeadline,
                finalDeadline,
                prelimExamAt,
                finalExamAt,
                prelimOpen,
                finalOpen);
    }

    /**
     * Lịch GĐ2 — đăng ký còn mở, prelim/CK ở tương lai (chưa thi).
     */
    public SeedDates computeGd2RegistrationOpenDates() {
        LocalDate today = LocalDate.now();
        LocalDate regStart = today.minusDays(14);
        LocalDate regEnd = today.plusDays(14);
        LocalDate eventStart = today.plusDays(15);
        LocalDate eventEnd = eventStart.plusDays(30);
        int prelimHours = RoundScheduleSeedUtil.DEFAULT_PRELIM_CODING_HOURS;
        LocalDateTime prelimExamAt = eventStart.atTime(8, 0);
        LocalDateTime prelimOpen = RoundScheduleSeedUtil.submissionOpen(prelimExamAt, prelimHours);
        LocalDateTime prelimDeadline = RoundScheduleSeedUtil.submissionDeadline(prelimExamAt, prelimHours);
        LocalDateTime finalExamAt = RoundScheduleSeedUtil.minFinalExamAt(prelimExamAt, prelimHours);
        LocalDateTime finalOpen = finalExamAt;
        LocalDateTime finalDeadline = eventStart.atTime(16, 30);
        return new SeedDates(
                regStart,
                regEnd,
                eventStart,
                eventEnd,
                prelimDeadline,
                finalDeadline,
                prelimExamAt,
                finalExamAt,
                prelimOpen,
                finalOpen);
    }

    /**
     * Lịch GĐ3: đăng ký đã đóng, sơ loại <b>đang diễn ra</b> theo giờ máy lúc start BE
     * (mở nộp ~2h trước, deadline còn ~8h — luôn test được dù restart buổi tối).
     */
    public SeedDates computeGd3ActivePrelimDates() {
        LocalDate today = LocalDate.now();
        LocalDate regStart = today.minusDays(30);
        LocalDate regEnd = today.minusDays(1);
        LocalDate eventStart = today.plusDays(14);
        LocalDate eventEnd = eventStart.plusDays(30);
        int prelimHours = RoundScheduleSeedUtil.DEFAULT_PRELIM_CODING_HOURS;
        LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);
        LocalDateTime prelimOpen = now.minusHours(2);
        LocalDateTime prelimDeadline = now.plusHours(8);
        long openOffsetMinutes = (prelimHours * 60L * 2L) / 3L;
        LocalDateTime prelimExamAt = prelimOpen.minusMinutes(openOffsetMinutes);
        LocalDateTime finalExamAt = RoundScheduleSeedUtil.minFinalExamAt(prelimExamAt, prelimHours);
        if (!finalExamAt.isAfter(prelimDeadline)) {
            finalExamAt = eventStart.atTime(8, 0);
        }
        LocalDateTime finalOpen = finalExamAt;
        LocalDateTime finalDeadline = eventStart.atTime(16, 30);
        return new SeedDates(
                regStart,
                regEnd,
                eventStart,
                eventEnd,
                prelimDeadline,
                finalDeadline,
                prelimExamAt,
                finalExamAt,
                prelimOpen,
                finalOpen);
    }

    /**
     * Lịch GĐ4: đăng ký đã đóng, sơ loại <b>đã kết thúc</b> theo giờ máy, CK chưa active.
     */
    public SeedDates computeGd4AdvanceReadyDates() {
        LocalDate today = LocalDate.now();
        LocalDate regStart = today.minusDays(45);
        LocalDate regEnd = today.minusDays(20);
        LocalDate eventStart = today.plusDays(7);
        LocalDate eventEnd = eventStart.plusDays(30);
        int prelimHours = RoundScheduleSeedUtil.DEFAULT_PRELIM_CODING_HOURS;
        LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);
        LocalDateTime prelimDeadline = now.minusHours(48);
        long openOffsetMinutes = (prelimHours * 60L * 2L) / 3L;
        LocalDateTime prelimOpen = prelimDeadline.minusHours(prelimHours).plusMinutes(openOffsetMinutes);
        LocalDateTime prelimExamAt = prelimOpen.minusMinutes(openOffsetMinutes);
        LocalDateTime finalExamAt = eventStart.atTime(8, 0);
        LocalDateTime finalOpen = finalExamAt;
        LocalDateTime finalDeadline = eventStart.atTime(16, 30);
        return new SeedDates(
                regStart,
                regEnd,
                eventStart,
                eventEnd,
                prelimDeadline,
                finalDeadline,
                prelimExamAt,
                finalExamAt,
                prelimOpen,
                finalOpen);
    }

    /**
     * Lịch GĐ5: sơ loại đã xong, CK <b>đang diễn ra</b> (mở nộp ~2h trước, deadline còn ~8h).
     */
    public SeedDates computeGd5FinalActiveDates() {
        LocalDate today = LocalDate.now();
        LocalDate regStart = today.minusDays(60);
        LocalDate regEnd = today.minusDays(30);
        LocalDate eventStart = today.plusDays(7);
        LocalDate eventEnd = eventStart.plusDays(30);
        int prelimHours = RoundScheduleSeedUtil.DEFAULT_PRELIM_CODING_HOURS;
        LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);
        LocalDateTime prelimDeadline = now.minusDays(3);
        long openOffsetMinutes = (prelimHours * 60L * 2L) / 3L;
        LocalDateTime prelimOpen = prelimDeadline.minusHours(prelimHours).plusMinutes(openOffsetMinutes);
        LocalDateTime prelimExamAt = prelimOpen.minusMinutes(openOffsetMinutes);
        LocalDateTime finalOpen = now.minusHours(2);
        LocalDateTime finalDeadline = now.plusHours(8);
        LocalDateTime finalExamAt = finalOpen;
        return new SeedDates(
                regStart,
                regEnd,
                eventStart,
                eventEnd,
                prelimDeadline,
                finalDeadline,
                prelimExamAt,
                finalExamAt,
                prelimOpen,
                finalOpen);
    }

    /**
     * Lịch GĐ6: sơ loại + CK <b>đã kết thúc</b>, hackathon chờ xác nhận đóng.
     */
    public SeedDates computeGd6PendingConfirmDates() {
        LocalDate today = LocalDate.now();
        LocalDate regStart = today.minusDays(90);
        LocalDate regEnd = today.minusDays(60);
        LocalDate eventStart = today.minusDays(7);
        LocalDate eventEnd = today.plusDays(7);
        int prelimHours = RoundScheduleSeedUtil.DEFAULT_PRELIM_CODING_HOURS;
        LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);
        LocalDateTime prelimDeadline = now.minusDays(10);
        long openOffsetMinutes = (prelimHours * 60L * 2L) / 3L;
        LocalDateTime prelimOpen = prelimDeadline.minusHours(prelimHours).plusMinutes(openOffsetMinutes);
        LocalDateTime prelimExamAt = prelimOpen.minusMinutes(openOffsetMinutes);
        LocalDateTime finalDeadline = now.minusHours(36);
        LocalDateTime finalOpen = finalDeadline.minusHours(4);
        LocalDateTime finalExamAt = finalOpen;
        return new SeedDates(
                regStart,
                regEnd,
                eventStart,
                eventEnd,
                prelimDeadline,
                finalDeadline,
                prelimExamAt,
                finalExamAt,
                prelimOpen,
                finalOpen);
    }

    /**
     * Đồng bộ hackathon + rounds theo {@link SeedDates} — gọi mỗi lần start dev cho slug extended.
     */
    @Transactional
    public boolean syncHackathonCalendarFromDates(String slug, SeedDates dates) {
        Optional<Hackathon> maybe = hackathonRepository.findBySlug(slug);
        if (maybe.isEmpty()) {
            return false;
        }
        Hackathon hackathon = maybe.get();
        boolean changed = syncHackathonFields(hackathon, dates);
        changed |= syncRoundsFromDates(hackathon.getId(), dates);
        if (changed) {
            log.info("[HackathonDevSeedHelper] Đã sync lịch slug={} reg={}→{} eventStart={} prelimExamAt={}",
                    slug, dates.regStart(), dates.regEnd(), dates.eventStart(), dates.prelimExamAt());
        }
        return changed;
    }

    private boolean syncHackathonFields(Hackathon hackathon, SeedDates dates) {
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

    private boolean syncRoundsFromDates(Integer hackathonId, SeedDates dates) {
        List<Round> rounds = roundRepository.findByHackathon_IdOrderByExamAtAsc(hackathonId);
        if (rounds.isEmpty()) {
            return false;
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
                if (round.getSubmissionOpen() == null || !round.getSubmissionOpen().equals(dates.finalSubmissionOpen())) {
                    round.setSubmissionOpen(dates.finalSubmissionOpen());
                    changed = true;
                }
                if (round.getSubmissionDeadline() == null || !round.getSubmissionDeadline().equals(dates.finalDeadline())) {
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
                int hours = round.getCodingDurationHours() != null && round.getCodingDurationHours() > 0
                        ? round.getCodingDurationHours()
                        : RoundScheduleSeedUtil.DEFAULT_PRELIM_CODING_HOURS;
                LocalDateTime exam = round.getExamAt();
                LocalDateTime expectedOpen = dates.prelimSubmissionOpen() != null
                        ? dates.prelimSubmissionOpen()
                        : RoundScheduleSeedUtil.submissionOpen(exam, hours);
                LocalDateTime expectedDeadline = dates.prelimDeadline() != null
                        ? dates.prelimDeadline()
                        : RoundScheduleSeedUtil.submissionDeadline(exam, hours);
                if (round.getCodingDurationHours() == null) {
                    round.setCodingDurationHours(hours);
                    changed = true;
                }
                if (round.getSubmissionOpen() == null || !round.getSubmissionOpen().equals(expectedOpen)) {
                    round.setSubmissionOpen(expectedOpen);
                    changed = true;
                }
                if (round.getSubmissionDeadline() == null || !round.getSubmissionDeadline().equals(expectedDeadline)) {
                    round.setSubmissionDeadline(expectedDeadline);
                    changed = true;
                }
            }
            if (changed) {
                roundRepository.save(round);
                count++;
            }
        }
        return count > 0;
    }

    /**
     * Đồng bộ submissionOpen / submissionDeadline theo examAt + codingDurationHours (repair DB dev).
     */
    public void repairRoundSubmissionWindow(Round round) {
        repairRoundSubmissionWindowIfNeeded(round);
    }

    @Transactional
    public void repairAllDevHackathonRoundSchedules() {
        int fixed = 0;
        for (String slug : DevSeedCatalog.ALL_DEV_HACKATHON_SLUGS) {
            var hackathon = hackathonRepository.findBySlug(slug);
            if (hackathon.isPresent()) {
                fixed += repairHackathonRounds(hackathon.get().getId());
            }
        }
        if (fixed > 0) {
            log.info("[HackathonDevSeedHelper] Đã repair submission window cho {} round (examAt + 2/3 duration)", fixed);
        }
    }

    private int repairHackathonRounds(Integer hackathonId) {
        int count = 0;
        LocalDate eventStart = hackathonRepository.findById(hackathonId)
                .map(Hackathon::getEventStart)
                .orElse(null);
        List<Round> rounds = roundRepository.findByHackathon_IdOrderByExamAtAsc(hackathonId);
        Round prelim = rounds.stream()
                .filter(r -> !Boolean.TRUE.equals(r.getIsFinal()))
                .findFirst()
                .orElse(null);
        for (Round round : rounds) {
            if (Boolean.TRUE.equals(round.getIsFinal())) {
                if (repairFinalRoundScheduleIfNeeded(round, prelim, eventStart)) {
                    count++;
                }
            } else if (repairRoundSubmissionWindowIfNeeded(round)) {
                count++;
            }
        }
        return count;
    }

    private boolean repairFinalRoundScheduleIfNeeded(Round finalRound, Round prelim, LocalDate eventStart) {
        if (prelim == null || prelim.getExamAt() == null) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        if (finalRound.getSubmissionOpen() != null
                && finalRound.getSubmissionDeadline() != null
                && finalRound.getSubmissionOpen().isBefore(now)
                && finalRound.getSubmissionDeadline().isAfter(now.plusHours(1))) {
            return false;
        }
        if (Boolean.TRUE.equals(finalRound.getScoringLocked())
                && finalRound.getSubmissionDeadline() != null
                && finalRound.getSubmissionDeadline().isBefore(now)) {
            return false;
        }
        int prelimHours = prelim.getCodingDurationHours() != null && prelim.getCodingDurationHours() > 0
                ? prelim.getCodingDurationHours()
                : RoundScheduleSeedUtil.DEFAULT_PRELIM_CODING_HOURS;
        LocalDateTime minExam = RoundScheduleSeedUtil.minFinalExamAt(prelim.getExamAt(), prelimHours);
        LocalDateTime deadline = eventStart != null
                ? eventStart.atTime(16, 30)
                : minExam.plusHours(1).plusMinutes(30);
        boolean changed = false;
        if (finalRound.getExamAt() == null || finalRound.getExamAt().isBefore(minExam)) {
            finalRound.setExamAt(minExam);
            changed = true;
        }
        LocalDateTime exam = finalRound.getExamAt();
        if (finalRound.getSubmissionOpen() == null || !finalRound.getSubmissionOpen().equals(exam)) {
            finalRound.setSubmissionOpen(exam);
            changed = true;
        }
        if (finalRound.getSubmissionDeadline() == null || !finalRound.getSubmissionDeadline().equals(deadline)) {
            finalRound.setSubmissionDeadline(deadline);
            changed = true;
        }
        if (changed) {
            roundRepository.save(finalRound);
        }
        return changed;
    }

    private boolean repairRoundSubmissionWindowIfNeeded(Round round) {
        if (round.getExamAt() == null || Boolean.TRUE.equals(round.getIsFinal())) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        if (round.getSubmissionOpen() != null
                && round.getSubmissionDeadline() != null
                && round.getSubmissionOpen().isBefore(now)
                && round.getSubmissionDeadline().isAfter(now.plusHours(1))) {
            return false;
        }
        int hours = round.getCodingDurationHours() != null && round.getCodingDurationHours() > 0
                ? round.getCodingDurationHours()
                : RoundScheduleSeedUtil.DEFAULT_PRELIM_CODING_HOURS;
        LocalDateTime open = RoundScheduleSeedUtil.submissionOpen(round.getExamAt(), hours);
        LocalDateTime deadline = RoundScheduleSeedUtil.submissionDeadline(round.getExamAt(), hours);
        boolean changed = false;
        if (round.getCodingDurationHours() == null) {
            round.setCodingDurationHours(hours);
            changed = true;
        }
        if (!open.equals(round.getSubmissionOpen())) {
            round.setSubmissionOpen(open);
            changed = true;
        }
        if (!deadline.equals(round.getSubmissionDeadline())) {
            round.setSubmissionDeadline(deadline);
            changed = true;
        }
        if (changed) {
            roundRepository.save(round);
        }
        return changed;
    }

    public User requireCoordinator() {
        return userRepository.findByEmail(Gd1SeedConstants.EMAIL_COORDINATOR)
                .orElseThrow(() -> new IllegalStateException("Thiếu coordinator seed GĐ1"));
    }

    public User requireJudge1() {
        return userRepository.findByEmail(Gd1SeedConstants.EMAIL_JUDGE1)
                .orElseThrow(() -> new IllegalStateException("Thiếu judge1 seed GĐ1"));
    }

    public User requireJudge2() {
        return userRepository.findByEmail(Gd1SeedConstants.EMAIL_JUDGE2)
                .orElseThrow(() -> new IllegalStateException("Thiếu judge2 seed GĐ1"));
    }

    public User requireGuestJudge() {
        return userRepository.findByEmail(Gd1SeedConstants.EMAIL_GUEST_JUDGE)
                .orElseThrow(() -> new IllegalStateException("Thiếu guest judge seed GĐ1"));
    }

    public User requireMentor() {
        return userRepository.findByEmail(Gd1SeedConstants.EMAIL_MENTOR)
                .orElseThrow(() -> new IllegalStateException("Thiếu mentor seed GĐ1"));
    }

    public Chapter requireChapter(String code) {
        return chapterRepository.findByCode(code)
                .orElseThrow(() -> new IllegalStateException("Thiếu chapter " + code));
    }

    public User upsertStudent(String email, String fullName, Chapter chapter) {
        Optional<User> existing = userRepository.findByEmail(email);
        if (existing.isPresent()) {
            return existing.get();
        }
        LocalDateTime now = LocalDateTime.now();
        return userRepository.save(User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(DevSeedCatalog.DEV_STUDENT_PASSWORD))
                .fullName(fullName)
                .role(UserRole.STUDENT)
                .userType(UserType.INTERNAL)
                .status(UserStatus.APPROVED)
                .chapter(chapter)
                .institution("Seed extended — " + email)
                .isTempAccount(false)
                .isDeptHead(false)
                .mustChangePassword(false)
                .emailVerifiedAt(now)
                .createdAt(now)
                .updatedAt(now)
                .build());
    }

    public HackathonStructure ensureHackathonStructure(
            String slug,
            String name,
            HackathonStatus status,
            String description,
            PrelimState prelimState,
            FinalState finalState) {
        return ensureHackathonStructure(
                slug, name, status, description, prelimState, finalState, computeRelativeDates());
    }

    public HackathonStructure ensureHackathonStructure(
            String slug,
            String name,
            HackathonStatus status,
            String description,
            PrelimState prelimState,
            FinalState finalState,
            SeedDates dates) {
        User coordinator = requireCoordinator();
        User judge1 = requireJudge1();
        User judge2 = userRepository.findByEmail(Gd1SeedConstants.EMAIL_JUDGE2).orElse(judge1);
        User guestJudge = requireGuestJudge();
        User mentor = requireMentor();

        Hackathon hackathon = hackathonRepository.findBySlug(slug).orElseGet(() ->
                hackathonRepository.save(Hackathon.builder()
                        .name(name)
                        .slug(slug)
                        .season(Season.Spring)
                        .year(dates.eventStart().getYear())
                        .status(status)
                        .description(description)
                        .registrationStart(dates.regStart())
                        .registrationEnd(dates.regEnd())
                        .eventStart(dates.eventStart())
                        .eventEnd(dates.eventEnd())
                        .wildcardEnabled(true)
                        .individualRankingEnabled(false)
                        .createdBy(coordinator)
                        .build()));

        if (hackathon.getStatus() != status) {
            hackathon.setStatus(status);
            hackathonRepository.save(hackathon);
        }
        syncHackathonFields(hackathon, dates);
        syncRoundsFromDates(hackathon.getId(), dates);

        List<Round> rounds = roundRepository.findByHackathon_IdOrderByExamAtAsc(hackathon.getId());
        Round prelim;
        Round finalRound;
        if (rounds.isEmpty()) {
            prelim = roundRepository.save(buildPrelimRound(hackathon, dates, prelimState));
            finalRound = roundRepository.save(buildFinalRound(hackathon, dates, finalState));
            ensureMinimalEvents(hackathon, coordinator, dates);
        } else {
            prelim = rounds.stream().filter(r -> !Boolean.TRUE.equals(r.getIsFinal())).findFirst().orElseThrow();
            finalRound = rounds.stream().filter(r -> Boolean.TRUE.equals(r.getIsFinal())).findFirst().orElseThrow();
            applyPrelimState(prelim, prelimState, coordinator);
            applyFinalState(finalRound, finalState, coordinator);
        }

        Track track1 = trackRepository.findByRoundIdOrderBySequenceOrderAsc(prelim.getId()).stream()
                .findFirst()
                .orElseGet(() -> trackRepository.save(buildTrack(prelim, 1)));
        Track track2 = trackRepository.findByRoundIdOrderBySequenceOrderAsc(prelim.getId()).stream()
                .skip(1)
                .findFirst()
                .orElseGet(() -> trackRepository.save(buildTrack(prelim, 2)));

        ensureTrackCriteria(track1);
        ensureTrackCriteria(track2);
        ensureFinalCriteria(finalRound);
        ensureJudgeMentorAssignments(track1, track2, finalRound, judge1, judge2, guestJudge, mentor, coordinator);

        repairRoundSubmissionWindow(prelim);

        return new HackathonStructure(hackathon, prelim, finalRound, track1, track2);
    }

    public Team ensureActiveTeam(
            Hackathon hackathon,
            String teamName,
            User leader,
            Chapter chapter,
            LocalDateTime now) {
        return teamRepository.findByHackathon_Id(hackathon.getId()).stream()
                .filter(t -> teamName.equals(t.getTeamName()))
                .findFirst()
                .orElseGet(() -> {
                    Team team = teamRepository.save(Team.builder()
                            .hackathon(hackathon)
                            .teamName(teamName)
                            .leader(leader)
                            .chapter(chapter)
                            .status(TeamStatus.ACTIVE)
                            .isLocked(false)
                            .createdAt(now)
                            .build());
                    teamMemberRepository.save(TeamMember.builder()
                            .id(new TeamMemberId(team.getId(), leader.getId()))
                            .team(team)
                            .user(leader)
                            .roleInTeam(TeamMemberRole.LEADER)
                            .status(TeamMemberStatus.ACCEPTED)
                            .joinedAt(now)
                            .build());
                    return team;
                });
    }

    /** Tìm đội theo leader (rename nếu đổi tên seed) — tránh tạo trùng khi đổi teamName. */
    public Team ensureActiveTeamForLeader(
            Hackathon hackathon,
            String teamName,
            User leader,
            Chapter chapter,
            LocalDateTime now) {
        return teamRepository.findByLeader_Id(leader.getId()).stream()
                .filter(t -> hackathon.getId().equals(t.getHackathon().getId()))
                .findFirst()
                .map(team -> {
                    if (!teamName.equals(team.getTeamName())) {
                        team.setTeamName(teamName);
                        return teamRepository.save(team);
                    }
                    return team;
                })
                .orElseGet(() -> ensureActiveTeam(hackathon, teamName, leader, chapter, now));
    }

    public void ensureLottery(
            Hackathon hackathon,
            Round prelim,
            Track track,
            String group,
            Team team,
            User coordinator,
            LocalDateTime now) {
        if (teamRoundParticipationRepository.findByTeam_IdAndRound_Id(team.getId(), prelim.getId()).isEmpty()) {
            teamRoundParticipationRepository.save(TeamRoundParticipation.builder()
                    .team(team)
                    .round(prelim)
                    .hackathon(hackathon)
                    .createdAt(now)
                    .build());
        }
        boolean hasTrack = teamRoundTrackRepository.findByTeam_Id(team.getId()).stream()
                .anyMatch(trt -> trt.getTrack().getId().equals(track.getId()));
        if (!hasTrack) {
            teamRoundTrackRepository.save(TeamRoundTrack.builder()
                    .team(team)
                    .track(track)
                    .assignedGroup(group)
                    .participationStatus(ParticipationStatus.PARTICIPATING)
                    .registrationType(RegistrationType.ASSIGNED)
                    .assignedAt(now)
                    .assignedBy(coordinator)
                    .build());
        }
    }

    public void markAdvanced(Team team, Round prelim, Round finalRound, Hackathon hackathon) {
        TeamRoundTrack trt = teamRoundTrackRepository.findByTeam_IdAndTrack_Round_Id(team.getId(), prelim.getId())
                .orElseThrow();
        trt.setParticipationStatus(ParticipationStatus.ADVANCED);
        teamRoundTrackRepository.save(trt);
        teamRoundParticipationRepository.findByTeam_IdAndRound_Id(team.getId(), finalRound.getId())
                .orElseGet(() -> teamRoundParticipationRepository.save(TeamRoundParticipation.builder()
                        .team(team)
                        .round(finalRound)
                        .hackathon(hackathon)
                        .build()));
    }

    public Criteria firstCriterionForTrack(Track track) {
        List<Criteria> list = criteriaRepository.findByTrackIdOrderByDisplayOrderAsc(track.getId());
        if (list.isEmpty()) {
            throw new IllegalStateException("Track " + track.getId() + " chưa có criteria");
        }
        return list.get(0);
    }

    public Criteria firstCriterionForFinal(Round finalRound) {
        return criteriaRepository.findByFinalRoundIdOrderByDisplayOrderAsc(finalRound.getId()).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Final round chưa có criteria"));
    }

    public List<Criteria> listFinalCriteria(Round finalRound) {
        return criteriaRepository.findByFinalRoundIdOrderByDisplayOrderAsc(finalRound.getId());
    }

    private Round buildPrelimRound(Hackathon hackathon, SeedDates dates, PrelimState state) {
        Round round = Round.builder()
                .hackathon(hackathon)
                .name("Vòng Sơ loại")
                .examAt(dates.prelimExamAt())
                .isFinal(false)
                .roundType(RoundType.PRELIMINARY)
                .submissionOpen(dates.prelimSubmissionOpen())
                .submissionDeadline(dates.prelimDeadline())
                .codingDurationHours(7)
                .lateSubmissionPolicy(LateSubmissionPolicy.ALLOW_LATE_PENDING)
                .topNAdvance(state.topNAdvance() != null ? state.topNAdvance() : 2)
                .wildcardEnabled(true)
                .minTeamsFinal(state.minTeamsFinal() != null ? state.minTeamsFinal() : 6)
                .tiebreakRule(TiebreakRule.PENALTY_SCORE)
                .isActive(state.active())
                .scoringLocked(state.scoringLocked())
                .isPublished(state.published())
                .build();
        if (state.problemReleased()) {
            round.setProblemStatementUrl("https://example.com/seed/debai-so-loai.pdf");
            round.setProblemReleasedAt(LocalDateTime.now());
        }
        if (state.active()) {
            round.setActivatedAt(LocalDateTime.now());
        }
        if (state.scoringLocked()) {
            round.setScoringLockedAt(LocalDateTime.now());
        }
        if (state.published()) {
            round.setPublishedAt(LocalDateTime.now());
        }
        return round;
    }

    private Round buildFinalRound(Hackathon hackathon, SeedDates dates, FinalState state) {
        Round round = Round.builder()
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
                .isActive(state.active())
                .scoringLocked(state.scoringLocked())
                .build();
        if (state.active()) {
            round.setActivatedAt(LocalDateTime.now());
        }
        if (state.scoringLocked()) {
            round.setScoringLockedAt(LocalDateTime.now());
        }
        return round;
    }

    private void applyPrelimState(Round prelim, PrelimState state, User coordinator) {
        prelim.setIsActive(state.active());
        if (state.active() && prelim.getActivatedAt() == null) {
            prelim.setActivatedAt(LocalDateTime.now());
        }
        prelim.setScoringLocked(state.scoringLocked());
        if (state.scoringLocked()) {
            prelim.setScoringLockedAt(LocalDateTime.now());
            prelim.setScoringLockedBy(coordinator);
        }
        prelim.setIsPublished(state.published());
        if (state.published()) {
            prelim.setPublishedAt(LocalDateTime.now());
            prelim.setPublishedBy(coordinator);
        }
        if (state.problemReleased()) {
            prelim.setProblemStatementUrl("https://example.com/seed/debai-so-loai.pdf");
            prelim.setProblemReleasedAt(LocalDateTime.now());
        }
        if (state.topNAdvance() != null) {
            prelim.setTopNAdvance(state.topNAdvance());
        }
        if (state.minTeamsFinal() != null) {
            prelim.setMinTeamsFinal(state.minTeamsFinal());
        }
        roundRepository.save(prelim);
    }

    private void applyFinalState(Round finalRound, FinalState state, User coordinator) {
        finalRound.setIsActive(state.active());
        if (state.active() && finalRound.getActivatedAt() == null) {
            finalRound.setActivatedAt(LocalDateTime.now());
        }
        finalRound.setScoringLocked(state.scoringLocked());
        if (state.scoringLocked()) {
            finalRound.setScoringLockedAt(LocalDateTime.now());
            finalRound.setScoringLockedBy(coordinator);
        }
        roundRepository.save(finalRound);
    }

    /** Bảo đảm đủ {@code trackCount} track trên vòng sơ loại (criteria + judge). */
    public List<Track> ensureTracks(Round prelim, int trackCount, User judge1, User judge2,
                                    User coordinator) {
        List<Track> existing = trackRepository.findByRoundIdOrderBySequenceOrderAsc(prelim.getId());
        List<Track> tracks = new ArrayList<>(existing);
        LocalDateTime at = LocalDateTime.now();
        for (int seq = tracks.size() + 1; seq <= trackCount; seq++) {
            Track track = trackRepository.save(buildTrack(prelim, seq));
            tracks.add(track);
        }
        while (tracks.size() > trackCount) {
            // không xóa track thừa trên DB cũ — chỉ dùng N track đầu
            break;
        }
        List<Track> result = tracks.subList(0, Math.min(trackCount, tracks.size()));
        for (Track track : result) {
            ensureTrackCriteria(track);
            if (judgeAssignmentRepository.findByTrackId(track.getId()).isEmpty()) {
                User judge = track.getSequenceOrder() % 2 == 1 ? judge1 : judge2;
                saveJudgeTrack(judge, track, coordinator, at);
            }
            ensureHeadJudgeOnTrack(track);
        }
        return result;
    }

    /** @deprecated dùng {@link #ensureTracks(Round, int, User, User, User)} */
    @Deprecated
    public Track ensureThirdTrack(Round prelim, User judge2, User coordinator) {
        return ensureTracks(prelim, 3, requireJudge1(), judge2, coordinator).get(2);
    }

    private Track buildTrack(Round prelim, int sequence) {
        String suffix = switch (sequence) {
            case 1 -> "RAG Pipeline";
            case 2 -> "AI Agent";
            default -> "EV & Integration";
        };
        return Track.builder()
                .round(prelim)
                .name("Track " + sequence + " — " + suffix)
                .description("Seed track " + sequence)
                .topic("Seed topic " + sequence)
                .maxTeams(8)
                .maxTeamsPerGroup(8)
                .minTeamSize(3)
                .maxTeamSize(5)
                .sequenceOrder(sequence)
                .status(TrackStatus.OPEN)
                .build();
    }

    private void ensureTrackCriteria(Track track) {
        if (!criteriaRepository.findByTrackIdOrderByDisplayOrderAsc(track.getId()).isEmpty()) {
            return;
        }
        List<String[]> rows = List.of(
                new String[] {"Domain Accuracy", "TECHNICAL", "0.30", "1"},
                new String[] {"Kiến trúc", "TECHNICAL", "0.30", "2"},
                new String[] {"Thuyết trình", "SOFT_SKILL", "0.20", "3"},
                new String[] {"Thực thi", "TECHNICAL", "0.20", "4"});
        for (String[] row : rows) {
            criteriaRepository.save(Criteria.builder()
                    .track(track)
                    .name(row[0])
                    .type(CriteriaType.valueOf(row[1]))
                    .weight(Float.parseFloat(row[2]))
                    .maxScore(10)
                    .displayOrder(Integer.parseInt(row[3]))
                    .build());
        }
    }

    private void ensureFinalCriteria(Round finalRound) {
        if (!criteriaRepository.findByFinalRoundIdOrderByDisplayOrderAsc(finalRound.getId()).isEmpty()) {
            return;
        }
        criteriaRepository.save(Criteria.builder()
                .round(finalRound)
                .name("Tổng thể Chung kết")
                .type(CriteriaType.TECHNICAL)
                .weight(1.0f)
                .maxScore(10)
                .displayOrder(1)
                .build());
    }

    private void ensureJudgeMentorAssignments(
            Track track1,
            Track track2,
            Round finalRound,
            User judge1,
            User judge2,
            User guestJudge,
            User mentor,
            User coordinator) {
        LocalDateTime at = LocalDateTime.now();
        if (judgeAssignmentRepository.findByTrackId(track1.getId()).isEmpty()) {
            saveJudgeTrack(judge1, track1, coordinator, at);
            saveJudgeTrack(guestJudge, track1, coordinator, at);
        }
        if (judgeAssignmentRepository.findByTrackId(track2.getId()).isEmpty()) {
            saveJudgeTrack(judge1, track2, coordinator, at);
            saveJudgeTrack(judge2, track2, coordinator, at);
        }
        if (mentorAssignmentRepository.findByTrackId(track1.getId()).isEmpty()) {
            mentorAssignmentRepository.save(MentorAssignment.builder()
                    .mentor(mentor)
                    .track(track1)
                    .assignedBy(coordinator)
                    .assignedAt(at)
                    .build());
        }
        if (judgeAssignmentRepository.findByRoundId(finalRound.getId()).isEmpty()) {
            judgeAssignmentRepository.save(JudgeAssignment.builder()
                    .judge(guestJudge)
                    .round(finalRound)
                    .assignmentType(JudgeAssignmentType.FINAL_EXTERNAL)
                    .assignedBy(coordinator)
                    .assignedAt(at)
                    .build());
        }
    }

    private void saveJudgeTrack(User judge, Track track, User coordinator, LocalDateTime at) {
        boolean trackHasHead = judgeAssignmentRepository.findByTrackId(track.getId()).stream()
                .anyMatch(ja -> ja.getAssignmentType() == JudgeAssignmentType.HEAD);
        JudgeAssignmentType type = trackHasHead ? JudgeAssignmentType.NORMAL : JudgeAssignmentType.HEAD;
        judgeAssignmentRepository.save(JudgeAssignment.builder()
                .judge(judge)
                .track(track)
                .assignmentType(type)
                .assignedBy(coordinator)
                .assignedAt(at)
                .build());
    }

    /** Idempotent — DB cũ có thể chỉ có NORMAL; promotion judge đầu tiên lên HEAD. */
    public void ensureHeadJudgeOnTrack(Track track) {
        List<JudgeAssignment> assignments = judgeAssignmentRepository.findByTrackId(track.getId());
        if (assignments.isEmpty()) {
            return;
        }
        boolean hasHead = assignments.stream()
                .anyMatch(ja -> ja.getAssignmentType() == JudgeAssignmentType.HEAD);
        if (!hasHead) {
            JudgeAssignment first = assignments.get(0);
            first.setAssignmentType(JudgeAssignmentType.HEAD);
            judgeAssignmentRepository.save(first);
        }
    }

    /**
     * Milestone đủ KO+WS+AWARDS; lịch WS → KO (gap sau regEnd, trước eventStart).
     */
    @Transactional
    public int repairAllDevHackathonMilestoneEvents() {
        List<String> slugs = List.of(DevSeedCatalog.ALL_DEV_HACKATHON_SLUGS);
        int fixed = 0;
        for (String slug : slugs) {
            Optional<Hackathon> maybe = hackathonRepository.findBySlug(slug);
            if (maybe.isEmpty() || Gd1SeedConstants.SLUG_INCOMPLETE.equals(slug)) {
                continue;
            }
            fixed += ensureMilestoneEvents(maybe.get(), requireCoordinator());
        }
        if (fixed > 0) {
            log.info("[HackathonDevSeedHelper] Repair milestone events: {} thay đổi", fixed);
        }
        return fixed;
    }

    private int ensureMilestoneEvents(Hackathon hackathon, User coordinator) {
        LocalDate regEnd = hackathon.getRegistrationEnd();
        LocalDate eventStart = hackathon.getEventStart();
        LocalDate eventEnd = hackathon.getEventEnd() != null ? hackathon.getEventEnd() : eventStart;
        if (regEnd == null || eventStart == null) {
            return 0;
        }
        LocalDate wsDay = regEnd.plusDays(1);
        LocalDate koDay = regEnd.plusDays(2);
        if (!koDay.isBefore(eventStart)) {
            koDay = eventStart.minusDays(1);
            wsDay = koDay.minusDays(1);
        }
        LocalDateTime wsStart = wsDay.atTime(20, 0);
        LocalDateTime wsEnd = wsDay.atTime(21, 30);
        LocalDateTime koStart = koDay.atTime(14, 0);
        LocalDateTime koEnd = koDay.atTime(17, 0);
        LocalDateTime awardsStart = eventEnd.atTime(17, 30);
        LocalDateTime awardsEnd = eventEnd.atTime(19, 0);

        int count = 0;
        count += upsertMilestoneEvent(hackathon, coordinator, EventType.KICKOFF,
                "Lễ Khai mạc (seed)", "FPT HCM — Hội trường A", koStart, koEnd);
        count += upsertMilestoneEvent(hackathon, coordinator, EventType.WORKSHOP,
                "Workshop (seed)", "Online", wsStart, wsEnd);
        count += upsertMilestoneEvent(hackathon, coordinator, EventType.AWARDS,
                "Lễ trao giải (seed)", "FPT HCM — Hội trường A", awardsStart, awardsEnd);
        return count;
    }

    private int upsertMilestoneEvent(Hackathon hackathon, User coordinator, EventType type,
                                     String title, String location,
                                     LocalDateTime startsAt, LocalDateTime endsAt) {
        List<Event> existing = eventRepository.findByHackathonIdAndType(hackathon.getId(), type);
        if (existing.isEmpty()) {
            eventRepository.save(Event.builder()
                    .hackathon(hackathon)
                    .title(title)
                    .type(type)
                    .location(location)
                    .startsAt(startsAt)
                    .endsAt(endsAt)
                    .isPublic(true)
                    .createdBy(coordinator)
                    .build());
            return 1;
        }
        int count = 0;
        for (Event event : existing) {
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

    private void ensureMinimalEvents(Hackathon hackathon, User coordinator, SeedDates dates) {
        ensureMilestoneEvents(hackathon, coordinator);
    }

    public void registerStudent(Hackathon hackathon, User student) {
        if (!hackathonRegistrationRepository.existsByHackathon_IdAndUser_Id(hackathon.getId(), student.getId())) {
            hackathonRegistrationRepository.save(HackathonRegistration.builder()
                    .hackathon(hackathon)
                    .user(student)
                    .build());
        }
    }

    public void releaseFinalProblem(Round finalRound) {
        if (finalRound.getProblemReleasedAt() == null) {
            finalRound.setProblemStatementUrl("https://example.com/seed/debai-chung-ket-gd5.pdf");
            finalRound.setProblemReleasedAt(LocalDateTime.now());
            roundRepository.save(finalRound);
        }
    }

    public void ensureGuestJudgeInvitation(Hackathon hackathon, User guestJudge, User coordinator) {
        Optional<Invitation> existing = invitationRepository.findByEmail(guestJudge.getEmail()).stream()
                .filter(inv -> inv.getRole() == UserRole.JUDGE)
                .filter(inv -> inv.getHackathon() != null
                        && inv.getHackathon().getId().equals(hackathon.getId()))
                .findFirst();
        if (existing.isPresent()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        invitationRepository.save(Invitation.builder()
                .email(guestJudge.getEmail())
                .role(UserRole.JUDGE)
                .hackathon(hackathon)
                .invitedBy(coordinator)
                .token("seed-guest-judge-" + hackathon.getSlug() + "-" + now.toEpochSecond(java.time.ZoneOffset.UTC))
                .expiresAt(now.plusYears(1))
                .acceptedAt(now)
                .createdAt(now)
                .build());
    }

    public Submission ensureFinalSubmission(
            Hackathon hackathon,
            Round finalRound,
            Team team,
            String repoUrl) {
        List<Submission> existing = submissionRepository.findByTeam_IdAndRound_Id(team.getId(), finalRound.getId());
        if (!existing.isEmpty()) {
            return existing.get(0);
        }
        return submissionRepository.save(Submission.builder()
                        .team(team)
                        .hackathon(hackathon)
                        .round(finalRound)
                        .track(null)
                        .repoUrl(repoUrl)
                        .demoUrl("https://demo.example.com/" + team.getTeamName().replace(' ', '-'))
                        .slideUrl("https://docs.google.com/presentation/d/seed-" + team.getId() + "/edit")
                        .status(SubmissionStatus.SUBMITTED)
                        .submittedAt(LocalDateTime.now())
                        .isLate(false)
                        .build());
    }

    public void ensureNormalScore(
            Submission submission,
            Criteria criterion,
            User judge,
            float scoreValue,
            boolean isFinal) {
        scoreRepository
                .findBySubmission_IdAndJudge_IdAndCriterion_IdAndScoreType(
                        submission.getId(), judge.getId(), criterion.getId(), ScoreType.NORMAL)
                .orElseGet(() -> scoreRepository.save(Score.builder()
                        .submission(submission)
                        .judge(judge)
                        .criterion(criterion)
                        .scoreValue(scoreValue)
                        .comment("Seed GĐ5 score")
                        .scoreType(ScoreType.NORMAL)
                        .isFinal(isFinal)
                        .scoredAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build()));
    }

    /** Xóa submission/score CK để chạy lại full chain GĐ5 trên cùng hackathon dev. */
    @Transactional
    public void clearFinalRoundArtifacts(Integer hackathonId) {
        jdbcTemplate.update("""
                DELETE s FROM scores s
                INNER JOIN submissions sub ON sub.id = s.submission_id
                INNER JOIN rounds r ON r.id = sub.round_id
                WHERE r.hackathon_id = ? AND r.is_final = 1
                """, hackathonId);
        jdbcTemplate.update("""
                DELETE cs FROM calibration_sessions cs
                INNER JOIN rounds r ON r.id = cs.round_id
                WHERE r.hackathon_id = ? AND r.is_final = 1
                """, hackathonId);
        jdbcTemplate.update("""
                DELETE sm FROM submission_metadata sm
                INNER JOIN submissions sub ON sub.id = sm.submission_id
                INNER JOIN rounds r ON r.id = sub.round_id
                WHERE r.hackathon_id = ? AND r.is_final = 1
                """, hackathonId);
        jdbcTemplate.update("""
                DELETE sub FROM submissions sub
                INNER JOIN rounds r ON r.id = sub.round_id
                WHERE r.hackathon_id = ? AND r.is_final = 1
                """, hackathonId);
    }

    /** Reset hackathon GĐ5 về trạng thái sẵn sàng chạy lại GĐ5 (ONGOING, CK mở, chưa lock). */
    @Transactional
    public void repairHackathonForGd5Retest(Hackathon hackathon, Round prelim, Round finalRound) {
        User coordinator = requireCoordinator();
        syncHackathonCalendarFromDates(hackathon.getSlug(), computeGd5FinalActiveDates());
        if (hackathon.getStatus() != HackathonStatus.ONGOING) {
            hackathon.setStatus(HackathonStatus.ONGOING);
            hackathonRepository.save(hackathon);
        }
        clearFinalRoundArtifacts(hackathon.getId());
        applyPrelimState(prelim, new PrelimState(false, true, true, true, 2, 2), coordinator);
        applyFinalState(finalRound, new FinalState(true, false), coordinator);
        releaseFinalProblem(finalRound);
        finalRound.setForceLocked(false);
        finalRound.setForceLockReason(null);
        finalRound.setScoringLockedBy(null);
        roundRepository.save(finalRound);
        ensureFinalGuestJudgeAssignment(hackathon, finalRound);
    }

    /**
     * Đồng bộ lịch + trạng thái CK mở cho GĐ5 mà <b>không</b> xóa submission/score đã seed.
     */
    @Transactional
    public void repairGd5FeTestingScheduleAndState(Hackathon hackathon, Round prelim, Round finalRound) {
        User coordinator = requireCoordinator();
        syncHackathonCalendarFromDates(hackathon.getSlug(), computeGd5FinalActiveDates());
        if (hackathon.getStatus() != HackathonStatus.ONGOING) {
            hackathon.setStatus(HackathonStatus.ONGOING);
            hackathonRepository.save(hackathon);
        }
        applyPrelimState(prelim, new PrelimState(false, true, true, true, 2, 2), coordinator);
        applyFinalState(finalRound, new FinalState(true, false), coordinator);
        releaseFinalProblem(finalRound);
        finalRound.setForceLocked(false);
        finalRound.setForceLockReason(null);
        finalRound.setScoringLockedBy(null);
        roundRepository.save(finalRound);
        ensureFinalGuestJudgeAssignment(hackathon, finalRound);
    }

    /** Xóa kết quả GĐ6 (rankings + prizes) để chạy lại confirm flow. */
    @Transactional
    public void clearGd6ClosureArtifacts(Integer hackathonId) {
        jdbcTemplate.update("DELETE FROM individual_rankings WHERE hackathon_id = ?", hackathonId);
        jdbcTemplate.update("DELETE FROM chapter_rankings WHERE hackathon_id = ?", hackathonId);
        jdbcTemplate.update("DELETE FROM prizes WHERE hackathon_id = ?", hackathonId);
    }

    /** Reset hackathon GĐ6 về PENDING_CONFIRM (sau FINISHED hoặc retest). */
    @Transactional
    public void repairHackathonForGd6Retest(Hackathon hackathon, Round prelim, Round finalRound) {
        User coordinator = requireCoordinator();
        syncHackathonCalendarFromDates(hackathon.getSlug(), computeGd6PendingConfirmDates());
        clearGd6ClosureArtifacts(hackathon.getId());
        applyPrelimState(prelim, new PrelimState(false, true, true, true, 2, 2), coordinator);
        applyFinalState(finalRound, new FinalState(true, true), coordinator);
        releaseFinalProblem(finalRound);
        hackathon.setStatus(HackathonStatus.PENDING_CONFIRM);
        hackathon.setIndividualRankingEnabled(true);
        hackathonRepository.save(hackathon);
        ensureFinalGuestJudgeAssignment(hackathon, finalRound);
    }

    public void ensureFirstPrize(Hackathon hackathon, Round finalRound, Team team, User coordinator) {
        if (!prizeRepository.existsByHackathonIdAndPrizeRank(hackathon.getId(), PrizeRank.FIRST)) {
            prizeRepository.save(Prize.builder()
                    .hackathon(hackathon)
                    .round(finalRound)
                    .team(team)
                    .prizeRank(PrizeRank.FIRST)
                    .prizeName("Giải Nhất")
                    .prizeValue("7000000")
                    .description("Seed GĐ6 — giải nhất")
                    .awardedBy(coordinator)
                    .build());
        }
    }

    /** Đảm bảo guest judge có FINAL_EXTERNAL trên CK + invitation còn hiệu lực. */
    public void ensureFinalGuestJudgeAssignment(Hackathon hackathon, Round finalRound) {
        User coordinator = requireCoordinator();
        User guestJudge = requireGuestJudge();
        ensureGuestJudgeInvitation(hackathon, guestJudge, coordinator);
        boolean hasFinal = judgeAssignmentRepository.findByRoundId(finalRound.getId()).stream()
                .anyMatch(ja -> ja.getJudge().getId().equals(guestJudge.getId())
                        && ja.getAssignmentType() == JudgeAssignmentType.FINAL_EXTERNAL);
        if (!hasFinal) {
            judgeAssignmentRepository.save(JudgeAssignment.builder()
                    .judge(guestJudge)
                    .round(finalRound)
                    .assignmentType(JudgeAssignmentType.FINAL_EXTERNAL)
                    .assignedBy(coordinator)
                    .assignedAt(LocalDateTime.now())
                    .build());
        }
    }

    public void ensureTeamLocked(Team team, LocalDateTime now) {
        if (!Boolean.TRUE.equals(team.getIsLocked())) {
            team.setIsLocked(true);
            team.setLockedAt(now);
            teamRepository.save(team);
        }
    }

    public Submission ensurePrelimSubmission(
            Hackathon hackathon,
            Round prelim,
            Track track,
            Team team,
            SubmissionStatus status,
            boolean isLate,
            LocalDateTime submittedAt) {
        Optional<Submission> existing = submissionRepository.findByTeam_IdAndRound_Id(team.getId(), prelim.getId())
                .stream()
                .findFirst();
        if (existing.isPresent()) {
            Submission sub = existing.get();
            sub.setStatus(status);
            sub.setIsLate(isLate);
            sub.setSubmittedAt(submittedAt);
            return submissionRepository.save(sub);
        }
        return submissionRepository.save(Submission.builder()
                .team(team)
                .hackathon(hackathon)
                .round(prelim)
                .track(track)
                .repoUrl("https://github.com/seal-warriors/%s".formatted(team.getTeamName().replace(' ', '-').toLowerCase()))
                .demoUrl("https://demo.example.com/" + team.getId())
                .slideUrl("https://docs.google.com/presentation/d/seed-" + team.getId() + "/edit")
                .status(status)
                .isLate(isLate)
                .submittedAt(submittedAt)
                .build());
    }

    public void scoreAllTrackCriteria(
            Submission submission,
            Track track,
            User judge,
            float scoreValue,
            boolean isFinal) {
        for (Criteria c : criteriaRepository.findByTrackIdOrderByDisplayOrderAsc(track.getId())) {
            ensureNormalScore(submission, c, judge, scoreValue, isFinal);
        }
    }

    public void seedPresentationQueue(Round prelim, Track track, List<Submission> gradableInOrder) {
        presentationSlotRepository.deleteByRound_IdAndTrack_Id(prelim.getId(), track.getId());
        if (gradableInOrder.isEmpty()) {
            return;
        }
        LocalDateTime examAt = prelim.getExamAt() != null
                ? prelim.getExamAt()
                : LocalDateTime.now().withSecond(0).withNano(0);
        int slotMinutes = track.getPresentationMinutes() != null && track.getPresentationMinutes() > 0
                ? track.getPresentationMinutes()
                : 10;
        int order = 1;
        for (Submission submission : gradableInOrder) {
            LocalDateTime start = examAt.plusMinutes((long) (order - 1) * slotMinutes);
            PresentationQueueStatus status = order == 1
                    ? PresentationQueueStatus.PRESENTING
                    : PresentationQueueStatus.WAITING;
            presentationSlotRepository.save(PresentationSlot.builder()
                    .round(prelim)
                    .track(track)
                    .submission(submission)
                    .team(submission.getTeam())
                    .startsAt(start)
                    .endsAt(start.plusMinutes(slotMinutes))
                    .location("Phòng " + track.getSequenceOrder() + "-" + order)
                    .sequenceOrder(order)
                    .queueStatus(status)
                    .timerPhase(PresentationTimerPhase.IDLE)
                    .pausedAccumulatedSeconds(0)
                    .build());
            order++;
        }
    }

    @Transactional
    public void clearPrelimRoundArtifacts(Integer hackathonId) {
        jdbcTemplate.update("""
                DELETE s FROM scores s
                INNER JOIN submissions sub ON sub.id = s.submission_id
                INNER JOIN rounds r ON r.id = sub.round_id
                WHERE r.hackathon_id = ? AND r.is_final = 0
                """, hackathonId);
        jdbcTemplate.update("""
                DELETE cs FROM calibration_sessions cs
                INNER JOIN rounds r ON r.id = cs.round_id
                WHERE r.hackathon_id = ? AND r.is_final = 0
                """, hackathonId);
        jdbcTemplate.update("""
                DELETE ps FROM presentation_slots ps
                INNER JOIN teams t ON t.id = ps.team_id
                WHERE t.hackathon_id = ?
                """, hackathonId);
        jdbcTemplate.update("""
                DELETE sm FROM submission_metadata sm
                INNER JOIN submissions sub ON sub.id = sm.submission_id
                INNER JOIN rounds r ON r.id = sub.round_id
                WHERE r.hackathon_id = ? AND r.is_final = 0
                """, hackathonId);
        jdbcTemplate.update("""
                DELETE jsc FROM judge_submission_scoring_confirmations jsc
                INNER JOIN submissions sub ON sub.id = jsc.submission_id
                INNER JOIN rounds r ON r.id = sub.round_id
                WHERE r.hackathon_id = ? AND r.is_final = 0
                """, hackathonId);
        jdbcTemplate.update("""
                DELETE sub FROM submissions sub
                INNER JOIN rounds r ON r.id = sub.round_id
                WHERE r.hackathon_id = ? AND r.is_final = 0
                """, hackathonId);
        jdbcTemplate.update("""
                DELETE wr FROM wildcard_reviews wr
                INNER JOIN rounds r ON r.id = wr.round_id
                WHERE r.hackathon_id = ? AND r.is_final = 0
                """, hackathonId);
    }

    @Transactional
    public void resetTeamsToPreAdvance(Hackathon hackathon, Round prelim, Round finalRound) {
        for (Team team : teamRepository.findByHackathon_Id(hackathon.getId())) {
            teamRoundParticipationRepository.findByTeam_IdAndRound_Id(team.getId(), finalRound.getId())
                    .ifPresent(teamRoundParticipationRepository::delete);
            teamRoundTrackRepository.findByTeam_IdAndTrack_Round_Id(team.getId(), prelim.getId())
                    .ifPresent(trt -> {
                        trt.setParticipationStatus(ParticipationStatus.PARTICIPATING);
                        teamRoundTrackRepository.save(trt);
                    });
        }
    }

    @Transactional
    public void clearPrelimLotteryAssignments(Integer hackathonId, Integer prelimRoundId) {
        jdbcTemplate.update("""
                DELETE trt FROM team_round_tracks trt
                INNER JOIN tracks t ON t.id = trt.track_id
                WHERE t.round_id = ?
                """, prelimRoundId);
        jdbcTemplate.update("""
                DELETE trp FROM team_round_participation trp
                INNER JOIN rounds r ON r.id = trp.round_id
                WHERE r.hackathon_id = ? AND r.is_final = 0
                """, hackathonId);
    }

    /** Reset hackathon E2E GĐ2 — đăng ký mở, prelim inactive, đội chưa khóa / chưa lottery. */
    @Transactional
    public void repairHackathonForGd2Retest(Hackathon hackathon, Round prelim, Round finalRound) {
        User coordinator = requireCoordinator();
        syncHackathonCalendarFromDates(hackathon.getSlug(), computeGd2RegistrationOpenDates());
        applyPrelimState(prelim, new PrelimState(false, false, false, false, 2, 4), coordinator);
        prelim.setActivatedAt(null);
        prelim.setProblemStatementUrl(null);
        prelim.setProblemReleasedAt(null);
        roundRepository.save(prelim);
        applyFinalState(finalRound, new FinalState(false, false), coordinator);
        if (hackathon.getStatus() != HackathonStatus.ONGOING) {
            hackathon.setStatus(HackathonStatus.ONGOING);
            hackathonRepository.save(hackathon);
        }
        clearPrelimLotteryAssignments(hackathon.getId(), prelim.getId());
        for (Team team : teamRepository.findByHackathon_Id(hackathon.getId())) {
            if (Boolean.TRUE.equals(team.getIsLocked())) {
                team.setIsLocked(false);
                team.setLockedAt(null);
                teamRepository.save(team);
            }
        }
    }

    /** Reset hackathon GĐ3 về prelim active (sau khi đã lock/publish hoặc test xong). */
    @Transactional
    public void repairHackathonForGd3Retest(Hackathon hackathon, Round prelim, Round finalRound) {
        User coordinator = requireCoordinator();
        clearPrelimRoundArtifacts(hackathon.getId());
        clearFinalRoundArtifacts(hackathon.getId());
        resetTeamsToPreAdvance(hackathon, prelim, finalRound);
        SeedDates gd3Dates = computeGd3ActivePrelimDates();
        syncHackathonCalendarFromDates(hackathon.getSlug(), gd3Dates);
        applyPrelimState(prelim, new PrelimState(true, true, false, false, 2, 4), coordinator);
        applyFinalState(finalRound, new FinalState(false, false), coordinator);
        if (hackathon.getStatus() != HackathonStatus.ONGOING) {
            hackathon.setStatus(HackathonStatus.ONGOING);
            hackathonRepository.save(hackathon);
        }
    }

    /** Reset hackathon GĐ4 về trạng thái sẵn sàng publish/advance. */
    @Transactional
    public void repairHackathonForGd4Retest(Hackathon hackathon, Round prelim, Round finalRound) {
        User coordinator = requireCoordinator();
        clearFinalRoundArtifacts(hackathon.getId());
        resetTeamsToPreAdvance(hackathon, prelim, finalRound);
        SeedDates gd4Dates = computeGd4AdvanceReadyDates();
        syncHackathonCalendarFromDates(hackathon.getSlug(), gd4Dates);
        applyPrelimState(prelim, new PrelimState(false, true, true, false, 1, 6), coordinator);
        prelim.setWildcardEnabled(true);
        roundRepository.save(prelim);
        if (!Boolean.TRUE.equals(hackathon.getWildcardEnabled())) {
            hackathon.setWildcardEnabled(true);
            hackathonRepository.save(hackathon);
        }
        applyFinalState(finalRound, new FinalState(false, false), coordinator);
        if (hackathon.getStatus() != HackathonStatus.ONGOING) {
            hackathon.setStatus(HackathonStatus.ONGOING);
            hackathonRepository.save(hackathon);
        }
    }
}
