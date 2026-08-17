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
import com.sealhackathon.api.hackathons.entity.HackathonRegistration;
import com.sealhackathon.api.hackathons.repository.HackathonRegistrationRepository;
import com.sealhackathon.api.individual_rankings.entity.IndividualRanking;
import com.sealhackathon.api.individual_rankings.repository.IndividualRankingRepository;
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
import com.sealhackathon.api.hackathons.support.HackathonBannerStorageService;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.hackathons.value_object.Season;
import com.sealhackathon.api.judge_assignments.entity.JudgeAssignment;
import com.sealhackathon.api.judge_assignments.repository.JudgeAssignmentRepository;
import com.sealhackathon.api.judge_assignments.value_object.JudgeAssignmentType;
import com.sealhackathon.api.mentors.entity.MentorAssignment;
import com.sealhackathon.api.prizes.entity.Prize;
import com.sealhackathon.api.prizes.repository.PrizeRepository;
import com.sealhackathon.api.prizes.value_object.PrizeRank;
import com.sealhackathon.api.mentors.repository.MentorAssignmentRepository;
import com.sealhackathon.api.mentors.entity.MentorTeamAssignment;
import com.sealhackathon.api.mentors.repository.MentorTeamAssignmentRepository;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.rounds.value_object.LateSubmissionPolicy;
import com.sealhackathon.api.rounds.value_object.RoundType;
import com.sealhackathon.api.rounds.value_object.TiebreakRule;
import com.sealhackathon.api.teams.entity.TeamMember;
import com.sealhackathon.api.teams.entity.TeamMemberId;
import com.sealhackathon.api.teams.repository.TeamMemberRepository;
import com.sealhackathon.api.teams.value_object.TeamMemberRole;
import com.sealhackathon.api.teams.value_object.TeamMemberStatus;
import com.sealhackathon.api.teams.entity.TeamRoundParticipation;
import com.sealhackathon.api.teams.repository.TeamRoundParticipationRepository;
import com.sealhackathon.api.teams.entity.TeamRoundTrack;
import com.sealhackathon.api.teams.repository.TeamRoundTrackRepository;
import com.sealhackathon.api.teams.value_object.ParticipationStatus;
import com.sealhackathon.api.teams.value_object.RegistrationType;
import com.sealhackathon.api.teams.entity.Team;
import com.sealhackathon.api.teams.repository.TeamRepository;
import com.sealhackathon.api.teams.value_object.TeamStatus;
import com.sealhackathon.api.tiebreak_evaluations.entity.TiebreakEvaluation;
import com.sealhackathon.api.tiebreak_evaluations.repository.TiebreakEvaluationRepository;
import com.sealhackathon.api.tracks.entity.Track;
import com.sealhackathon.api.tracks.repository.TrackRepository;
import com.sealhackathon.api.tracks.support.TrackProblemStatementStorage;
import com.sealhackathon.api.rounds.support.RoundProblemStatementStorage;
import com.sealhackathon.api.rounds.support.SeedProblemPdf;
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
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

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
    private final MentorTeamAssignmentRepository mentorTeamAssignmentRepository;
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
    private final TiebreakEvaluationRepository tiebreakEvaluationRepository;
    private final IndividualRankingRepository individualRankingRepository;
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final HackathonBannerStorageService bannerStorageService;
    private final TrackProblemStatementStorage trackProblemStatementStorage;
    private final RoundProblemStatementStorage roundProblemStatementStorage;

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

    /** Timer state cho slot đang {@code PRESENTING} khi seed queue dev. */
    public record PresentationTimerSeed(
            PresentationTimerPhase phase,
            LocalDateTime presentationStartedAt,
            LocalDateTime qaStartedAt,
            LocalDateTime pausedAt,
            PresentationTimerPhase phaseBeforePause,
            int pausedAccumulatedSeconds) {

        public static PresentationTimerSeed pausedFromPresenting() {
            LocalDateTime now = LocalDateTime.now();
            return new PresentationTimerSeed(
                    PresentationTimerPhase.PAUSED,
                    now.minusMinutes(3),
                    null,
                    now.minusSeconds(45),
                    PresentationTimerPhase.PRESENTING,
                    0);
        }

        public static PresentationTimerSeed qa() {
            LocalDateTime now = LocalDateTime.now();
            return new PresentationTimerSeed(
                    PresentationTimerPhase.QA,
                    now.minusMinutes(8),
                    now.minusMinutes(2),
                    null,
                    null,
                    0);
        }

        public static PresentationTimerSeed presenting() {
            LocalDateTime now = LocalDateTime.now();
            return new PresentationTimerSeed(
                    PresentationTimerPhase.PRESENTING,
                    now.minusMinutes(2),
                    null,
                    null,
                    null,
                    0);
        }
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
        LocalDate eventStart = regEnd.plusDays(RoundScheduleSeedUtil.DAYS_REG_END_TO_EVENT_START);
        LocalDate eventEnd = eventStart.plusDays(45);
        int prelimHours = RoundScheduleSeedUtil.DEFAULT_PRELIM_CODING_HOURS;
        LocalDateTime prelimExamAt = eventStart.atTime(8, 0);
        LocalDateTime prelimOpen = RoundScheduleSeedUtil.submissionOpen(prelimExamAt, prelimHours);
        LocalDateTime prelimDeadline = RoundScheduleSeedUtil.submissionDeadline(prelimExamAt, prelimHours);
        LocalDateTime finalExamAt = RoundScheduleSeedUtil.maxFinalExamAt(prelimExamAt, prelimHours);
        LocalDateTime finalOpen = RoundScheduleSeedUtil.finalSubmissionOpen(finalExamAt);
        LocalDateTime finalDeadline = RoundScheduleSeedUtil.finalSubmissionDeadline(finalExamAt);
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
     * Lịch GĐ2 — đăng ký <b>đã đóng</b> (registration_end trong quá khứ).
     */
    public SeedDates computeGd2RegistrationClosedDates() {
        LocalDate today = LocalDate.now();
        LocalDate regStart = today.minusDays(30);
        LocalDate regEnd = today.minusDays(1);
        LocalDate eventStart = today.plusDays(14);
        LocalDate eventEnd = eventStart.plusDays(30);
        int prelimHours = RoundScheduleSeedUtil.DEFAULT_PRELIM_CODING_HOURS;
        LocalDateTime prelimExamAt = eventStart.atTime(8, 0);
        LocalDateTime prelimOpen = RoundScheduleSeedUtil.submissionOpen(prelimExamAt, prelimHours);
        LocalDateTime prelimDeadline = RoundScheduleSeedUtil.submissionDeadline(prelimExamAt, prelimHours);
        LocalDateTime finalExamAt = RoundScheduleSeedUtil.maxFinalExamAt(prelimExamAt, prelimHours);
        LocalDateTime finalOpen = RoundScheduleSeedUtil.finalSubmissionOpen(finalExamAt);
        LocalDateTime finalDeadline = RoundScheduleSeedUtil.finalSubmissionDeadline(finalExamAt);
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
     * Lịch GĐ5 — CK active, deadline đã qua, policy ALLOW_LATE_PENDING (LATE_PENDING row).
     */
    public SeedDates computeGd5LatePendingDates() {
        LocalDate today = LocalDate.now();
        LocalDate regStart = today.minusDays(45);
        LocalDate regEnd = today.minusDays(20);
        LocalDate eventStart = today.plusDays(7);
        LocalDate eventEnd = eventStart.plusDays(30);
        LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);
        LocalDateTime finalDeadline = now.minusHours(2);
        LocalDateTime finalOpen = finalDeadline.minusDays(3);
        LocalDateTime finalExamAt = finalOpen.minusHours(1);
        int prelimHours = RoundScheduleSeedUtil.DEFAULT_PRELIM_CODING_HOURS;
        LocalDateTime prelimExamAt = today.minusDays(25).atTime(8, 0);
        LocalDateTime prelimOpen = RoundScheduleSeedUtil.submissionOpen(prelimExamAt, prelimHours);
        LocalDateTime prelimDeadline = RoundScheduleSeedUtil.submissionDeadline(prelimExamAt, prelimHours);
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
        // Cần ≥3 ngày sau regEnd cho WORKSHOP + KICKOFF trước eventStart (gap exclusive).
        LocalDate eventStart = regEnd.plusDays(RoundScheduleSeedUtil.DAYS_REG_END_TO_EVENT_START);
        LocalDate eventEnd = eventStart.plusDays(30);
        int prelimHours = RoundScheduleSeedUtil.DEFAULT_PRELIM_CODING_HOURS;
        LocalDateTime prelimExamAt = eventStart.atTime(8, 0);
        LocalDateTime prelimOpen = RoundScheduleSeedUtil.submissionOpen(prelimExamAt, prelimHours);
        LocalDateTime prelimDeadline = RoundScheduleSeedUtil.submissionDeadline(prelimExamAt, prelimHours);
        LocalDateTime finalExamAt = RoundScheduleSeedUtil.maxFinalExamAt(prelimExamAt, prelimHours);
        LocalDateTime finalOpen = RoundScheduleSeedUtil.finalSubmissionOpen(finalExamAt);
        LocalDateTime finalDeadline = RoundScheduleSeedUtil.finalSubmissionDeadline(finalExamAt);
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
        LocalDateTime finalExamAt = RoundScheduleSeedUtil.maxFinalExamAt(prelimExamAt, prelimHours);
        if (!finalExamAt.isAfter(prelimDeadline)) {
            finalExamAt = eventStart.atTime(8, 0);
        }
        LocalDateTime finalOpen = RoundScheduleSeedUtil.finalSubmissionOpen(finalExamAt);
        LocalDateTime finalDeadline = RoundScheduleSeedUtil.finalSubmissionDeadline(finalExamAt);
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
        LocalDateTime finalOpen = RoundScheduleSeedUtil.finalSubmissionOpen(finalExamAt);
        LocalDateTime finalDeadline = RoundScheduleSeedUtil.finalSubmissionDeadline(finalExamAt);
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
     * Lịch GĐ5 — CK <b>đang active</b> nhưng deadline nộp <b>đã qua</b> (HARD_LOCK → {@code REJECTED}).
     */
    public SeedDates computeGd5LateHardLockDates() {
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
        LocalDateTime finalOpen = now.minusHours(12);
        LocalDateTime finalDeadline = now.minusHours(2);
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

    /** Đảm bảo mọi hackathon seed có banner file nội bộ (không dùng URL ngoài). */
    @Transactional
    public int repairAllHackathonBanners() {
        int updated = 0;
        for (Hackathon hackathon : hackathonRepository.findAll()) {
            if (ensureBannerImage(hackathon)) {
                updated++;
            }
        }
        if (updated > 0) {
            log.info("[HackathonDevSeedHelper] Đã tạo/cập nhật banner file cho {} hackathon", updated);
        }
        return updated;
    }

    public boolean ensureBannerImage(Hackathon hackathon) {
        if (hackathon == null || hackathon.getId() == null) {
            return false;
        }
        String current = hackathon.getBannerUrl();
        if (HackathonBannerStorageService.isStorageKey(current)) {
            return false;
        }
        String key = bannerStorageService.storeDefaultBanner(hackathon.getId(), hackathon.getName());
        hackathon.setBannerUrl(key);
        hackathonRepository.save(hackathon);
        return true;
    }

    private boolean syncHackathonFields(Hackathon hackathon, SeedDates dates) {
        boolean changed = false;
        if (!dates.regStart().atTime(0, 0).equals(hackathon.getRegistrationStart())) {
            hackathon.setRegistrationStart(dates.regStart().atTime(0, 0));
            changed = true;
        }
        if (!dates.regEnd().atTime(23, 59).equals(hackathon.getRegistrationEnd())) {
            hackathon.setRegistrationEnd(dates.regEnd().atTime(23, 59));
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
        // Seed sạch: đồng bộ cờ đóng đăng ký theo lịch hiện tại của seed.
        LocalDate today = LocalDate.now();
        if (dates.regEnd() != null && !dates.regEnd().isAfter(today)) {
            LocalDateTime expectedClosedAt = dates.regEnd().atTime(23, 59);
            if (!expectedClosedAt.equals(hackathon.getRegistrationClosedEarlyAt())) {
                hackathon.setRegistrationClosedEarlyAt(expectedClosedAt);
                changed = true;
            }
        } else if (hackathon.getRegistrationClosedEarlyAt() != null) {
            hackathon.setRegistrationClosedEarlyAt(null);
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
                if (round.getCodingDurationHours() == null
                        || round.getCodingDurationHours() != RoundScheduleSeedUtil.DEFAULT_FINAL_CODING_HOURS) {
                    // Chỉ ép duration mặc định khi seed lịch chuẩn (không đụng snapshot live GĐ5/6
                    // đã set codingDuration khác hoặc null có chủ đích qua dates.final).
                    if (dates.finalDeadline().equals(
                            RoundScheduleSeedUtil.finalSubmissionDeadline(dates.finalExamAt()))
                            && dates.finalSubmissionOpen().equals(
                            RoundScheduleSeedUtil.finalSubmissionOpen(dates.finalExamAt()))) {
                        round.setCodingDurationHours(RoundScheduleSeedUtil.DEFAULT_FINAL_CODING_HOURS);
                        changed = true;
                    }
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
        repairAllDevHackathonRoundSchedules(false);
    }

    /**
     * @param skipE2eWhenFrozen khi true và E2E đang GĐ3+, bỏ qua slug e2e (giữ lịch tay / sau tua giờ máy).
     */
    public void repairAllDevHackathonRoundSchedules(boolean skipE2eWhenFrozen) {
        int fixed = 0;
        for (String slug : DevSeedCatalog.ALL_DEV_HACKATHON_SLUGS) {
            if (skipE2eWhenFrozen && DevSeedCatalog.SLUG_E2E_ONGOING.equals(slug)) {
                continue;
            }
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
        List<Round> rounds = roundRepository.findByHackathon_IdOrderByExamAtAsc(hackathonId);
        Round prelim = rounds.stream()
                .filter(r -> !Boolean.TRUE.equals(r.getIsFinal()))
                .findFirst()
                .orElse(null);
        for (Round round : rounds) {
            if (Boolean.TRUE.equals(round.getIsFinal())) {
                if (repairFinalRoundScheduleIfNeeded(round, prelim)) {
                    count++;
                }
            } else if (repairRoundSubmissionWindowIfNeeded(round)) {
                count++;
            }
        }
        return count;
    }

    private boolean repairFinalRoundScheduleIfNeeded(Round finalRound, Round prelim) {
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
        LocalDateTime maxExam = RoundScheduleSeedUtil.maxFinalExamAt(prelim.getExamAt(), prelimHours);
        boolean changed = false;
        if (finalRound.getExamAt() == null
                || finalRound.getExamAt().isBefore(minExam)
                || finalRound.getExamAt().isAfter(maxExam)) {
            finalRound.setExamAt(maxExam);
            changed = true;
        }
        LocalDateTime exam = finalRound.getExamAt();
        LocalDateTime open = RoundScheduleSeedUtil.finalSubmissionOpen(exam);
        LocalDateTime deadline = RoundScheduleSeedUtil.finalSubmissionDeadline(exam);
        if (finalRound.getCodingDurationHours() == null
                || finalRound.getCodingDurationHours() != RoundScheduleSeedUtil.DEFAULT_FINAL_CODING_HOURS) {
            finalRound.setCodingDurationHours(RoundScheduleSeedUtil.DEFAULT_FINAL_CODING_HOURS);
            changed = true;
        }
        if (finalRound.getSubmissionOpen() == null || !finalRound.getSubmissionOpen().equals(open)) {
            finalRound.setSubmissionOpen(open);
            changed = true;
        }
        if (finalRound.getSubmissionDeadline() == null
                || !finalRound.getSubmissionDeadline().equals(deadline)
                || !finalRound.getSubmissionDeadline().isAfter(open)) {
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

    public User requireJudge3() {
        return userRepository.findByEmail(Gd1SeedConstants.EMAIL_JUDGE3)
                .orElseThrow(() -> new IllegalStateException("Thiếu judge3 seed GĐ1"));
    }

    public User requireJudge4() {
        return userRepository.findByEmail(Gd1SeedConstants.EMAIL_JUDGE4)
                .orElseThrow(() -> new IllegalStateException("Thiếu judge4 seed GĐ1"));
    }

    public User requireGuestJudge() {
        return userRepository.findByEmail(Gd1SeedConstants.EMAIL_GUEST_JUDGE)
                .orElseThrow(() -> new IllegalStateException("Thiếu guest judge seed GĐ1"));
    }

    public User requireGuestJudge2() {
        return userRepository.findByEmail(Gd1SeedConstants.EMAIL_GUEST_JUDGE2)
                .orElseThrow(() -> new IllegalStateException("Thiếu guestjudge2 seed GĐ1"));
    }

    public User requireGuestJudge3() {
        return userRepository.findByEmail(Gd1SeedConstants.EMAIL_GUEST_JUDGE3)
                .orElseThrow(() -> new IllegalStateException("Thiếu guestjudge3 seed GĐ1"));
    }

    public User requireMentor() {
        return userRepository.findByEmail(Gd1SeedConstants.EMAIL_MENTOR)
                .orElseThrow(() -> new IllegalStateException("Thiếu mentor seed GĐ1"));
    }

    public User requireMentor2() {
        return userRepository.findByEmail(Gd1SeedConstants.EMAIL_MENTOR2)
                .orElseThrow(() -> new IllegalStateException("Thiếu mentor2 seed GĐ1"));
    }

    public User requireMentor3() {
        return userRepository.findByEmail(Gd1SeedConstants.EMAIL_MENTOR3)
                .orElseThrow(() -> new IllegalStateException("Thiếu mentor3 seed GĐ1"));
    }

    public Chapter requireChapter(String code) {
        return chapterRepository.findByCode(code)
                .orElseThrow(() -> new IllegalStateException("Thiếu chapter " + code));
    }

    public User upsertStudent(String email, String fullName, Chapter chapter) {
        return upsertStudent(email, fullName, chapter, UserType.INTERNAL);
    }

    public User upsertStudent(String email, String fullName, Chapter chapter, UserType userType) {
        Optional<User> existing = userRepository.findByEmail(email);
        if (existing.isPresent()) {
            User user = existing.get();
            if (user.getUserType() != userType) {
                user.setUserType(userType);
                userRepository.save(user);
            }
            return user;
        }
        LocalDateTime now = LocalDateTime.now();
        return userRepository.save(User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(DevSeedCatalog.DEV_STUDENT_PASSWORD))
                .fullName(fullName)
                .role(UserRole.STUDENT)
                .userType(userType)
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

    /** Idempotent mentor dev — password {@link Gd1SeedConstants#DEV_MENTOR_PASSWORD}. */
    public User upsertMentor(String email, String fullName, Chapter chapter) {
        Optional<User> existing = userRepository.findByEmail(email);
        if (existing.isPresent()) {
            User user = existing.get();
            if (user.getRole() != UserRole.MENTOR) {
                user.setRole(UserRole.MENTOR);
                userRepository.save(user);
            }
            return user;
        }
        LocalDateTime now = LocalDateTime.now();
        return userRepository.save(User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(Gd1SeedConstants.DEV_MENTOR_PASSWORD))
                .fullName(fullName)
                .role(UserRole.MENTOR)
                .userType(UserType.INTERNAL)
                .status(UserStatus.APPROVED)
                .chapter(chapter)
                .institution("Seed mentor — " + email)
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
        User judge3 = userRepository.findByEmail(Gd1SeedConstants.EMAIL_JUDGE3).orElse(judge1);
        User judge4 = userRepository.findByEmail(Gd1SeedConstants.EMAIL_JUDGE4).orElse(judge2);
        User guestJudge = requireGuestJudge();
        User guestJudge2 = userRepository.findByEmail(Gd1SeedConstants.EMAIL_GUEST_JUDGE2).orElse(guestJudge);
        User mentor = requireMentor();
        User mentor2 = userRepository.findByEmail(Gd1SeedConstants.EMAIL_MENTOR2).orElse(mentor);
        User mentor3 = userRepository.findByEmail(Gd1SeedConstants.EMAIL_MENTOR3).orElse(mentor);

        Hackathon hackathon = hackathonRepository.findBySlug(slug).orElseGet(() ->
                hackathonRepository.save(Hackathon.builder()
                        .name(name)
                        .slug(slug)
                        .season(Season.Spring)
                        .year(dates.eventStart().getYear())
                        .status(status)
                        .description(description)
                        .registrationStart(dates.regStart().atTime(0, 0))
                        .registrationEnd(dates.regEnd().atTime(23, 59))
                        .registrationClosedEarlyAt(
                                dates.regEnd() != null && !dates.regEnd().isAfter(LocalDate.now())
                                        ? dates.regEnd().atTime(23, 59)
                                        : null)
                        .eventStart(dates.eventStart())
                        .eventEnd(dates.eventEnd())
                        .individualRankingEnabled(false)
                        .createdBy(coordinator)
                        .build()));

        if (hackathon.getStatus() != status) {
            hackathon.setStatus(status);
            hackathonRepository.save(hackathon);
        }
        syncHackathonFields(hackathon, dates);
        ensureBannerImage(hackathon);
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
            finalRound = rounds.stream().filter(r -> Boolean.TRUE.equals(r.getIsFinal())).findFirst()
                    .orElseGet(() -> roundRepository.save(buildFinalRound(hackathon, dates, finalState)));
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
        ensureJudgeMentorAssignments(
                track1, track2, finalRound,
                List.of(judge1, judge2, judge3, judge4),
                List.of(guestJudge, guestJudge2),
                List.of(mentor, mentor2, mentor3),
                coordinator);

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

    /** Đội tham dự Sơ loại nhưng không vào CK — TRT=ELIMINATED, gỡ TRP CK nếu có. */
    public void markEliminatedFromPrelim(Team team, Round prelim, Round finalRound) {
        TeamRoundTrack trt = teamRoundTrackRepository.findByTeam_IdAndTrack_Round_Id(team.getId(), prelim.getId())
                .orElseThrow();
        trt.setParticipationStatus(ParticipationStatus.ELIMINATED);
        teamRoundTrackRepository.save(trt);
        teamRoundParticipationRepository.findByTeam_IdAndRound_Id(team.getId(), finalRound.getId())
                .ifPresent(teamRoundParticipationRepository::delete);
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
                .minTeamsFinal(state.minTeamsFinal() != null ? state.minTeamsFinal() : 6)
                .tiebreakRule(TiebreakRule.PENALTY_SCORE)
                .isActive(state.active())
                .scoringLocked(state.scoringLocked())
                .isPublished(state.published())
                .build();
        if (state.problemReleased()) {
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
                .codingDurationHours(RoundScheduleSeedUtil.DEFAULT_FINAL_CODING_HOURS)
                .lateSubmissionPolicy(LateSubmissionPolicy.HARD_LOCK)
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
            prelim.setProblemReleasedAt(LocalDateTime.now());
            seedPrelimTrackProblems(prelim);
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
                .topic(resolveSeedTrackTopic(sequence))
                .maxTeams(8)
                .maxTeamsPerGroup(8)
                .minTeamSize(3)
                .maxTeamSize(5)
                .sequenceOrder(sequence)
                .status(TrackStatus.OPEN)
                .build();
    }

    /** Chủ đề mặc định theo thứ tự bảng đấu seed. */
    public static String resolveSeedTrackTopic(int sequence) {
        return switch (sequence) {
            case 1 -> "RAG Pipeline cho tài liệu nội bộ";
            case 2 -> "AI Agent tự động hóa quy trình";
            default -> "EV Charging & Tích hợp hệ thống thông minh";
        };
    }

    /** Dev: gán topic cho track prelim còn null/blank (DB cũ hoặc sau migrate). */
    @Transactional
    public void backfillTrackTopics() {
        int updated = 0;
        for (Track track : trackRepository.findAll()) {
            if (Boolean.TRUE.equals(track.getRound().getIsFinal())) {
                continue;
            }
            String topic = track.getTopic();
            if (topic != null && !topic.isBlank()) {
                continue;
            }
            int seq = track.getSequenceOrder() != null ? track.getSequenceOrder() : 1;
            track.setTopic(resolveSeedTrackTopic(seq));
            trackRepository.save(track);
            updated++;
        }
        if (updated > 0) {
            log.info("[HackathonDevSeedHelper] Backfill track topics — tracks={}", updated);
        }
    }

    private void ensureTrackCriteria(Track track) {
        ensureDefaultTrackCriteria(track);
    }

    /** Tạo lại bộ criteria mặc định cho track (idempotent nếu đã có). */
    public void ensureDefaultTrackCriteria(Track track) {
        if (!criteriaRepository.findByTrackIdOrderByDisplayOrderAsc(track.getId()).isEmpty()) {
            return;
        }
        // name, type, weight, order, description — không đổi weight/order so với seed cũ
        List<String[]> rows = List.of(
                new String[] {
                    "Domain Accuracy",
                    "TECHNICAL",
                    "0.30",
                    "1",
                    "Độ chính xác nghiệp vụ / domain so với đề bài và dữ liệu thực tế.\n"
                            + "Gợi ý (thang 0–10): 9–10 rất chính xác, ít lỗi domain; 7–8 tốt, còn thiếu cạnh; "
                            + "5–6 đạt cơ bản; ≤4 lệch domain hoặc nhiều sai sót."
                },
                new String[] {
                    "Kiến trúc",
                    "TECHNICAL",
                    "0.30",
                    "2",
                    "Thiết kế hệ thống, module hóa, lựa chọn công nghệ và khả năng mở rộng.\n"
                            + "Gợi ý (thang 0–10): 9–10 rõ ràng, vững; 7–8 ổn định; 5–6 chạy được nhưng mỏng; ≤4 rối / khó bảo trì."
                },
                new String[] {
                    "Thuyết trình",
                    "SOFT_SKILL",
                    "0.20",
                    "3",
                    "Khả năng truyền đạt ý tưởng, cấu trúc bài nói và trả lời câu hỏi.\n"
                            + "Gợi ý (thang 0–10): 9–10 mạch lạc, thuyết phục; 7–8 rõ ràng; 5–6 hiểu được ý chính; ≤4 khó theo dõi."
                },
                new String[] {
                    "Thực thi",
                    "TECHNICAL",
                    "0.20",
                    "4",
                    "Mức độ hoàn thiện demo, tính năng chạy được và chất lượng triển khai thực tế.\n"
                            + "Gợi ý (thang 0–10): 9–10 demo đầy đủ, ổn định; 7–8 phần lớn chạy tốt; 5–6 demo cơ bản; ≤4 gần như không chạy."
                });
        boolean priorityAssigned = false;
        for (String[] row : rows) {
            CriteriaType type = CriteriaType.valueOf(row[1]);
            boolean isPriority = !priorityAssigned && type != CriteriaType.PENALTY;
            if (isPriority) {
                priorityAssigned = true;
            }
            criteriaRepository.save(Criteria.builder()
                    .track(track)
                    .name(row[0])
                    .type(type)
                    .weight(Float.parseFloat(row[2]))
                    .maxScore(10)
                    .displayOrder(Integer.parseInt(row[3]))
                    .description(row[4])
                    .isTiebreakerPriority(isPriority)
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
                .isTiebreakerPriority(true)
                .description(
                        "Đánh giá tổng thể phần trình bày và sản phẩm ở vòng Chung kết "
                                + "(nội dung, demo, trả lời giám khảo).\n"
                                + "Gợi ý (thang 0–10): 9–10 xuất sắc toàn diện; 7–8 tốt, còn điểm cần cải thiện; "
                                + "5–6 đạt mức trung bình; ≤4 chưa sẵn sàng cho chung kết.")
                .build());
    }

    /**
     * Idempotent — gán {@code is_tiebreaker_priority} cho track/final chưa có flag.
     * Chỉ UPDATE boolean; không đụng scores/lottery. An toàn khi E2E flow frozen.
     */
    @Transactional
    public void backfillTiebreakerPriorityFlags() {
        int updated = 0;
        for (Track track : trackRepository.findAll()) {
            List<Criteria> list = criteriaRepository.findByTrackIdOrderByDisplayOrderAsc(track.getId());
            if (list.isEmpty()) {
                continue;
            }
            if (list.stream().anyMatch(c -> Boolean.TRUE.equals(c.getIsTiebreakerPriority()))) {
                continue;
            }
            Optional<Criteria> pick = list.stream()
                    .filter(c -> c.getType() != CriteriaType.PENALTY)
                    .findFirst();
            if (pick.isPresent()) {
                Criteria c = pick.get();
                c.setIsTiebreakerPriority(true);
                criteriaRepository.save(c);
                updated++;
            }
        }
        for (Round round : roundRepository.findAll()) {
            if (!Boolean.TRUE.equals(round.getIsFinal())) {
                continue;
            }
            List<Criteria> list =
                    criteriaRepository.findByFinalRoundIdOrderByDisplayOrderAsc(round.getId());
            if (list.isEmpty()) {
                continue;
            }
            if (list.stream().anyMatch(c -> Boolean.TRUE.equals(c.getIsTiebreakerPriority()))) {
                continue;
            }
            Optional<Criteria> pick = list.stream()
                    .filter(c -> c.getType() != CriteriaType.PENALTY)
                    .findFirst();
            if (pick.isPresent()) {
                Criteria c = pick.get();
                c.setIsTiebreakerPriority(true);
                criteriaRepository.save(c);
                updated++;
            }
        }
        if (updated > 0) {
            log.info("[HackathonDevSeedHelper] backfillTiebreakerPriority: {} track/final scopes",
                    updated);
        }
    }

    /**
     * Prelim: chỉ INTERNAL (HEAD + ≥1 NORMAL). CK: EXTERNAL FINAL_EXTERNAL + optional INTERNAL HEAD.
     */
    private void ensureJudgeMentorAssignments(
            Track track1,
            Track track2,
            Round finalRound,
            List<User> internalJudges,
            List<User> guestJudges,
            List<User> mentors,
            User coordinator) {
        LocalDateTime at = LocalDateTime.now();
        User j1 = internalJudges.get(0);
        User j2 = internalJudges.size() > 1 ? internalJudges.get(1) : j1;
        User j3 = internalJudges.size() > 2 ? internalJudges.get(2) : j1;
        User j4 = internalJudges.size() > 3 ? internalJudges.get(3) : j2;
        User m1 = mentors.get(0);
        User m2 = mentors.size() > 1 ? mentors.get(1) : m1;
        User m3 = mentors.size() > 2 ? mentors.get(2) : m1;

        if (judgeAssignmentRepository.findByTrackId(track1.getId()).isEmpty()) {
            saveJudgeTrack(j1, track1, coordinator, at);
            saveJudgeTrack(j2, track1, coordinator, at);
        }
        if (judgeAssignmentRepository.findByTrackId(track2.getId()).isEmpty()) {
            saveJudgeTrack(j3, track2, coordinator, at);
            saveJudgeTrack(j4, track2, coordinator, at);
        }
        if (mentorAssignmentRepository.findByTrackId(track1.getId()).isEmpty()) {
            mentorAssignmentRepository.save(MentorAssignment.builder()
                    .mentor(m1)
                    .track(track1)
                    .assignedBy(coordinator)
                    .assignedAt(at)
                    .build());
        }
        if (mentorAssignmentRepository.findByTrackId(track2.getId()).isEmpty()) {
            mentorAssignmentRepository.save(MentorAssignment.builder()
                    .mentor(m2)
                    .track(track2)
                    .assignedBy(coordinator)
                    .assignedAt(at)
                    .build());
        }
        // m3 reserved for track3 when present (Gd1 path); unused on 2-track helper

        if (judgeAssignmentRepository.findByRoundId(finalRound.getId()).isEmpty()) {
            judgeAssignmentRepository.save(JudgeAssignment.builder()
                    .judge(j1)
                    .round(finalRound)
                    .assignmentType(JudgeAssignmentType.HEAD)
                    .assignedBy(coordinator)
                    .assignedAt(at)
                    .build());
            for (User guest : guestJudges) {
                judgeAssignmentRepository.save(JudgeAssignment.builder()
                        .judge(guest)
                        .round(finalRound)
                        .assignmentType(JudgeAssignmentType.FINAL_EXTERNAL)
                        .assignedBy(coordinator)
                        .assignedAt(at)
                        .build());
            }
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

    /** Gỡ mọi guest EXTERNAL khỏi track sơ loại — guest chỉ thuộc round CK (FINAL_EXTERNAL). */
    @Transactional
    public void repairRemoveGuestJudgeFromPrelimTracks(Hackathon hackathon) {
        for (String email : Gd1SeedConstants.guestJudgeEmails()) {
            userRepository.findByEmail(email).ifPresent(guest -> {
                roundRepository.findByHackathon_IdOrderByExamAtAsc(hackathon.getId()).stream()
                        .filter(r -> !Boolean.TRUE.equals(r.getIsFinal()))
                        .forEach(prelim -> trackRepository.findByRoundIdOrderBySequenceOrderAsc(prelim.getId())
                                .forEach(track -> {
                                    judgeAssignmentRepository.findByTrackId(track.getId()).stream()
                                            .filter(ja -> guest.getId().equals(ja.getJudge().getId()))
                                            .forEach(judgeAssignmentRepository::delete);
                                    ensureHeadJudgeOnTrack(track);
                                }));
            });
        }
        // Also purge any EXTERNAL UserType left on prelim tracks (legacy mis-seed)
        roundRepository.findByHackathon_IdOrderByExamAtAsc(hackathon.getId()).stream()
                .filter(r -> !Boolean.TRUE.equals(r.getIsFinal()))
                .forEach(prelim -> trackRepository.findByRoundIdOrderBySequenceOrderAsc(prelim.getId())
                        .forEach(track -> {
                            judgeAssignmentRepository.findByTrackId(track.getId()).stream()
                                    .filter(ja -> ja.getJudge() != null
                                            && ja.getJudge().getUserType() == UserType.EXTERNAL)
                                    .forEach(judgeAssignmentRepository::delete);
                            ensureHeadJudgeOnTrack(track);
                        }));
    }

    @Transactional
    public void repairRemoveGuestJudgeFromAllDevPrelimTracks() {
        for (String slug : DevSeedCatalog.ALL_DEV_HACKATHON_SLUGS) {
            hackathonRepository.findBySlug(slug).ifPresent(h -> {
                repairRemoveGuestJudgeFromPrelimTracks(h);
                backfillPrelimInternalJudgesAndFinalAssignments(h);
            });
        }
    }

    /** Sau purge guest: mỗi track sơ loại ≥2 INTERNAL; CK có HEAD + ≥1 FINAL_EXTERNAL. */
    @Transactional
    public void backfillPrelimInternalJudgesAndFinalAssignments(Hackathon hackathon) {
        User coordinator = requireCoordinator();
        User j1 = requireJudge1();
        User j2 = userRepository.findByEmail(Gd1SeedConstants.EMAIL_JUDGE2).orElse(j1);
        User j3 = userRepository.findByEmail(Gd1SeedConstants.EMAIL_JUDGE3).orElse(j1);
        User j4 = userRepository.findByEmail(Gd1SeedConstants.EMAIL_JUDGE4).orElse(j2);
        List<User> pool = List.of(j1, j2, j3, j4);
        LocalDateTime at = LocalDateTime.now();

        roundRepository.findByHackathon_IdOrderByExamAtAsc(hackathon.getId()).stream()
                .filter(r -> !Boolean.TRUE.equals(r.getIsFinal()))
                .forEach(prelim -> {
                    List<Track> tracks = trackRepository.findByRoundIdOrderBySequenceOrderAsc(prelim.getId());
                    for (int i = 0; i < tracks.size(); i++) {
                        Track track = tracks.get(i);
                        List<JudgeAssignment> current = judgeAssignmentRepository.findByTrackId(track.getId());
                        if (current.size() >= 2) {
                            ensureHeadJudgeOnTrack(track);
                            continue;
                        }
                        User head = pool.get(i % pool.size());
                        User normal = pool.get((i + 1) % pool.size());
                        if (current.isEmpty()) {
                            saveJudgeTrack(head, track, coordinator, at);
                            saveJudgeTrack(normal, track, coordinator, at);
                        } else {
                            Integer existingId = current.get(0).getJudge().getId();
                            User extra = pool.stream()
                                    .filter(u -> !u.getId().equals(existingId))
                                    .findFirst()
                                    .orElse(normal);
                            if (!judgeAssignmentRepository.existsByJudgeIdAndTrackId(extra.getId(), track.getId())) {
                                saveJudgeTrack(extra, track, coordinator, at);
                            }
                            ensureHeadJudgeOnTrack(track);
                        }
                    }
                });

        roundRepository.findByHackathon_IdOrderByExamAtAsc(hackathon.getId()).stream()
                .filter(r -> Boolean.TRUE.equals(r.getIsFinal()))
                .findFirst()
                .ifPresent(finalRound -> ensureFinalGuestJudgeAssignment(hackathon, finalRound));
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
    public void repairAllDevHackathonMilestoneEvents() {
        repairAllDevHackathonMilestoneEvents(false);
    }

    public int repairAllDevHackathonMilestoneEvents(boolean skipE2eWhenFrozen) {
        List<String> slugs = List.of(DevSeedCatalog.ALL_DEV_HACKATHON_SLUGS);
        int fixed = 0;
        for (String slug : slugs) {
            if (skipE2eWhenFrozen && DevSeedCatalog.SLUG_E2E_ONGOING.equals(slug)) {
                continue;
            }
            Optional<Hackathon> maybe = hackathonRepository.findBySlug(slug);
            if (maybe.isEmpty()) {
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
        return ensureMilestoneEventsExcluding(hackathon, coordinator, EnumSet.noneOf(EventType.class));
    }

    /**
     * Milestone KO+WS+AWARDS — WS (regEnd+1) → KO (regEnd+2) trong gap exclusive trước eventStart;
     * AWARDS đúng ngày eventEnd.
     */
    @Transactional
    public int ensureMilestoneEventsExcluding(Hackathon hackathon, User coordinator, Set<EventType> exclude) {
        LocalDate regEnd = hackathon.getRegistrationEnd() != null
                ? hackathon.getRegistrationEnd().toLocalDate()
                : null;
        LocalDate eventStart = hackathon.getEventStart();
        LocalDate eventEnd = hackathon.getEventEnd() != null ? hackathon.getEventEnd() : eventStart;
        if (regEnd == null || eventStart == null) {
            return 0;
        }
        LocalDate wsDay = regEnd.plusDays(1);
        LocalDate koDay = regEnd.plusDays(2);
        if (!koDay.isBefore(eventStart)) {
            log.warn(
                    "[HackathonDevSeedHelper] slug={} gap regEnd={}→eventStart={} quá hẹp cho WS+KO — "
                            + "cần ≥{} ngày; bỏ qua repair milestone",
                    hackathon.getSlug(),
                    regEnd,
                    eventStart,
                    RoundScheduleSeedUtil.DAYS_REG_END_TO_EVENT_START);
            return 0;
        }
        LocalDateTime wsStart = wsDay.atTime(20, 0);
        LocalDateTime wsEnd = wsDay.atTime(21, 30);
        LocalDateTime koStart = koDay.atTime(14, 0);
        LocalDateTime koEnd = koDay.atTime(17, 0);
        LocalDateTime awardsStart = eventEnd.atTime(17, 30);
        LocalDateTime awardsEnd = eventEnd.atTime(19, 0);

        int count = 0;
        if (!exclude.contains(EventType.KICKOFF)) {
            count += upsertMilestoneEvent(hackathon, coordinator, EventType.KICKOFF,
                    "Lễ Khai mạc (seed)", "FPT HCM — Hội trường A", koStart, koEnd);
        }
        if (!exclude.contains(EventType.WORKSHOP)) {
            count += upsertMilestoneEvent(hackathon, coordinator, EventType.WORKSHOP,
                    "Workshop (seed)", "Online", wsStart, wsEnd);
        }
        if (!exclude.contains(EventType.AWARDS)) {
            count += upsertMilestoneEvent(hackathon, coordinator, EventType.AWARDS,
                    "Lễ trao giải (seed)", "FPT HCM — Hội trường A", awardsStart, awardsEnd);
        }
        return count;
    }

    @Transactional
    public void removeMilestoneEvents(Hackathon hackathon, EventType type) {
        eventRepository.findByHackathonIdAndType(hackathon.getId(), type)
                .forEach(eventRepository::delete);
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
                    .preferredShirtSize("M")
                    .preferredShirtFit("UNISEX")
                    .build());
        }
    }

    public void seedPrelimTrackProblems(Round prelim) {
        trackRepository.findByRoundIdOrderBySequenceOrderAsc(prelim.getId()).stream()
                .filter(t -> t.getStatus() != TrackStatus.CANCELLED)
                .forEach(t -> {
                    if (!TrackProblemStatementStorage.hasStoredFile(t)) {
                        trackProblemStatementStorage.storeSeedPdf(
                                t, SeedProblemPdf.displayFilename());
                        trackRepository.save(t);
                    }
                });
    }

    /**
     * CK không có PDF riêng — chỉ stamp {@code problemReleasedAt} để student resolve đề theo track sơ loại.
     * Legacy: nếu còn file PDF trên round final thì xóa (migration design).
     */
    public void seedFinalRoundProblem(Round finalRound) {
        if (finalRound == null) {
            return;
        }
        if (RoundProblemStatementStorage.hasStoredFile(finalRound)) {
            roundProblemStatementStorage.clearStoredFile(finalRound);
        }
        if (finalRound.getProblemReleasedAt() == null) {
            finalRound.setProblemReleasedAt(LocalDateTime.now());
        }
        roundRepository.save(finalRound);
    }

    /**
     * Dev setup: PDF đề trên mọi track Sơ loại (không gắn PDF lên vòng CK).
     */
    @Transactional
    public void backfillSetupProblemPdfs() {
        int tracks = 0;
        for (Round round : roundRepository.findAll()) {
            if (Boolean.TRUE.equals(round.getIsFinal())) {
                // CK: chỉ stamp released, không store PDF
                if (finalRoundNeedsReleaseOnly(round)) {
                    seedFinalRoundProblem(round);
                }
            } else {
                int before = trackRepository.findByRoundIdOrderBySequenceOrderAsc(round.getId()).stream()
                        .filter(t -> !TrackProblemStatementStorage.hasStoredFile(t))
                        .mapToInt(t -> 1)
                        .sum();
                if (before > 0) {
                    seedPrelimTrackProblems(round);
                    tracks += before;
                }
            }
        }
        if (tracks > 0) {
            log.info("[HackathonDevSeedHelper] Backfill PDF setup — tracks≈{}", tracks);
        }
    }

    private boolean finalRoundNeedsReleaseOnly(Round round) {
        return round.getProblemReleasedAt() == null || RoundProblemStatementStorage.hasStoredFile(round);
    }

    /** Dev: bổ sung PDF seed cho mọi vòng Sơ loại đã phát đề nhưng bảng đấu chưa có file storage. */
    @Transactional
    public void backfillReleasedPrelimTrackProblems() {
        roundRepository.findAll().stream()
                .filter(r -> !Boolean.TRUE.equals(r.getIsFinal()))
                .filter(r -> r.getProblemReleasedAt() != null)
                .forEach(this::seedPrelimTrackProblems);
    }

    /** CK: stamp problemReleasedAt only — không upload PDF round-level. */
    public void releaseFinalProblem(Round finalRound) {
        if (finalRound.getProblemReleasedAt() == null) {
            finalRound.setProblemReleasedAt(LocalDateTime.now());
            roundRepository.save(finalRound);
        }
        if (RoundProblemStatementStorage.hasStoredFile(finalRound)) {
            roundProblemStatementStorage.clearStoredFile(finalRound);
            roundRepository.save(finalRound);
        }
    }

    /** Dev: bổ sung PDF seed cho vòng Chung kết đã phát đề nhưng chưa có file storage. */
    @Transactional
    public void backfillReleasedFinalRoundProblems() {
        roundRepository.findAll().stream()
                .filter(r -> Boolean.TRUE.equals(r.getIsFinal()))
                .filter(r -> r.getProblemReleasedAt() != null)
                .forEach(r -> {
                    if (!RoundProblemStatementStorage.hasStoredFile(r)) {
                        roundProblemStatementStorage.storeSeedPdf(r, SeedProblemPdf.displayFilename());
                        roundRepository.save(r);
                    }
                });
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
        // Token phải unique per email — nhiều guest cùng slug + cùng giây sẽ trùng UK nếu chỉ dùng epoch.
        String emailKey = guestJudge.getEmail() == null
                ? "unknown"
                : guestJudge.getEmail().trim().toLowerCase().replace('@', '-').replace('.', '-');
        invitationRepository.save(Invitation.builder()
                .email(guestJudge.getEmail())
                .role(UserRole.JUDGE)
                .hackathon(hackathon)
                .invitedBy(coordinator)
                .token("seed-guest-judge-" + hackathon.getSlug() + "-" + emailKey)
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
                .map(existing -> {
                    if (existing.getScoreValue() == null
                            || Float.compare(existing.getScoreValue(), scoreValue) != 0) {
                        existing.setScoreValue(scoreValue);
                        existing.setUpdatedAt(LocalDateTime.now());
                        return scoreRepository.save(existing);
                    }
                    return existing;
                })
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

    /**
     * Chấm đủ mọi giám khảo được gán trên round CK (HEAD + FINAL_EXTERNAL).
     * Confirm GĐ6 gọi {@code hasIncompleteScoring} — thiếu 1 judge → {@code SCORING_INCOMPLETE_BEFORE_CONFIRM}.
     */
    public void ensureFinalScoresFromAllAssignedJudges(
            Round finalRound, Submission submission, List<Criteria> criteria, float scoreValue) {
        if (finalRound == null || submission == null || criteria == null || criteria.isEmpty()) {
            return;
        }
        List<User> judges = judgeAssignmentRepository.findByRoundId(finalRound.getId()).stream()
                .map(ja -> ja.getJudge())
                .filter(j -> j != null && j.getId() != null)
                .collect(java.util.stream.Collectors.toMap(
                        User::getId, j -> j, (a, b) -> a, java.util.LinkedHashMap::new))
                .values()
                .stream()
                .toList();
        if (judges.isEmpty()) {
            judges = List.of(requireGuestJudge());
        }
        for (Criteria c : criteria) {
            if (c.getType() == CriteriaType.PENALTY) {
                continue;
            }
            for (int ji = 0; ji < judges.size(); ji++) {
                // Lệch nhẹ giữa GK → inter-rater StdDev > 0 trên Analytics (RBL).
                float adjusted = Math.max(0f, Math.min(10f, scoreValue + (ji * 0.4f) - 0.4f));
                ensureNormalScore(submission, c, judges.get(ji), adjusted, true);
            }
        }
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
                DELETE sm FROM submission_metadata sm
                INNER JOIN submissions sub ON sub.id = sm.submission_id
                INNER JOIN rounds r ON r.id = sub.round_id
                WHERE r.hackathon_id = ? AND r.is_final = 1
                """, hackathonId);
        jdbcTemplate.update("""
                DELETE ps FROM presentation_slots ps
                INNER JOIN submissions sub ON sub.id = ps.submission_id
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

    /** Xóa chỉ prizes (giữ rankings) — reset Profile 0 sau API matrix. */
    @Transactional
    public void clearGd6Prizes(Integer hackathonId) {
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
        ensurePrize(hackathon, finalRound, team, PrizeRank.FIRST, "Giải Nhất", "7000000",
                "Seed GĐ6 — giải nhất", coordinator);
    }

    public void ensureSecondPrize(Hackathon hackathon, Round finalRound, Team team, User coordinator) {
        ensurePrize(hackathon, finalRound, team, PrizeRank.SECOND, "Giải Nhì", "5000000",
                "Seed GĐ6 — giải nhì", coordinator);
    }

    public void ensureThirdPrize(Hackathon hackathon, Round finalRound, Team team, User coordinator) {
        ensurePrize(hackathon, finalRound, team, PrizeRank.THIRD, "Giải Ba", "3000000",
                "Seed GĐ6 — giải ba", coordinator);
    }

    public void ensurePrize(
            Hackathon hackathon,
            Round finalRound,
            Team team,
            PrizeRank rank,
            String prizeName,
            String prizeValue,
            String description,
            User coordinator) {
        if (!prizeRepository.existsByHackathonIdAndPrizeRank(hackathon.getId(), rank)) {
            prizeRepository.save(Prize.builder()
                    .hackathon(hackathon)
                    .round(finalRound)
                    .team(team)
                    .prizeRank(rank)
                    .prizeName(prizeName)
                    .prizeValue(prizeValue)
                    .description(description)
                    .awardedBy(coordinator)
                    .build());
        }
    }

    /** Reset GĐ6 prizes-empty — PENDING_CONFIRM, CK locked, không có prize. */
    @Transactional
    public void repairHackathonForGd6PrizesEmptyRetest(Hackathon hackathon, Round prelim, Round finalRound) {
        repairHackathonForGd6Retest(hackathon, prelim, finalRound);
    }

    /**
     * Reset GĐ6 confirm-gate — PENDING_CONFIRM nhưng CK <b>chưa</b> scoring_locked.
     */
    @Transactional
    public void repairHackathonForGd6ConfirmGateRetest(Hackathon hackathon, Round prelim, Round finalRound) {
        User coordinator = requireCoordinator();
        syncHackathonCalendarFromDates(hackathon.getSlug(), computeGd6PendingConfirmDates());
        clearGd6ClosureArtifacts(hackathon.getId());
        applyPrelimState(prelim, new PrelimState(false, true, true, true, 1, 2), coordinator);
        applyFinalState(finalRound, new FinalState(true, false), coordinator);
        releaseFinalProblem(finalRound);
        ensureFinalGuestJudgeAssignment(hackathon, finalRound);
        hackathon.setIndividualRankingEnabled(true);
        hackathon.setStatus(HackathonStatus.PENDING_CONFIRM);
        hackathonRepository.save(hackathon);
    }

    /** Reset GĐ6 finished-export — FINISHED + giữ/xóa rankings để tính lại trong seeder. */
    @Transactional
    public void repairHackathonForGd6FinishedExportRetest(Hackathon hackathon, Round prelim, Round finalRound) {
        User coordinator = requireCoordinator();
        syncHackathonCalendarFromDates(hackathon.getSlug(), computeGd6PendingConfirmDates());
        clearGd6ClosureArtifacts(hackathon.getId());
        applyPrelimState(prelim, new PrelimState(false, true, true, true, 1, 2), coordinator);
        applyFinalState(finalRound, new FinalState(true, true), coordinator);
        releaseFinalProblem(finalRound);
        ensureFinalGuestJudgeAssignment(hackathon, finalRound);
        hackathon.setIndividualRankingEnabled(true);
        hackathon.setStatus(HackathonStatus.FINISHED);
        hackathonRepository.save(hackathon);
    }

    /** Đảm bảo đúng 2 guest FINAL_EXTERNAL (+ INTERNAL HEAD) trên CK + invitation. */
    public void ensureFinalGuestJudgeAssignment(Hackathon hackathon, Round finalRound) {
        User coordinator = requireCoordinator();
        User headJudge = requireJudge1();
        List<User> guests = new java.util.ArrayList<>();
        guests.add(requireGuestJudge());
        userRepository.findByEmail(Gd1SeedConstants.EMAIL_GUEST_JUDGE2).ifPresent(guests::add);
        for (User guest : guests) {
            ensureGuestJudgeInvitation(hackathon, guest, coordinator);
        }
        boolean hasHead = judgeAssignmentRepository.findByRoundId(finalRound.getId()).stream()
                .anyMatch(ja -> ja.getAssignmentType() == JudgeAssignmentType.HEAD);
        if (!hasHead) {
            judgeAssignmentRepository.save(JudgeAssignment.builder()
                    .judge(headJudge)
                    .round(finalRound)
                    .assignmentType(JudgeAssignmentType.HEAD)
                    .assignedBy(coordinator)
                    .assignedAt(LocalDateTime.now())
                    .build());
        }
        for (User guestJudge : guests) {
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
    }

    public void ensureTeamLocked(Team team, LocalDateTime now) {
        if (!Boolean.TRUE.equals(team.getIsLocked())) {
            team.setIsLocked(true);
            team.setLockedAt(now);
            teamRepository.save(team);
        }
    }

    /** Khóa mọi đội ACTIVE chưa khóa — GĐ4+ seed phải giữ nền GĐ2 (xem lại lịch sử). */
    public int ensureAllActiveTeamsLocked(Integer hackathonId, LocalDateTime now) {
        int locked = 0;
        for (Team team : teamRepository.findByHackathon_Id(hackathonId)) {
            if (team.getStatus() == TeamStatus.ACTIVE && !Boolean.TRUE.equals(team.getIsLocked())) {
                ensureTeamLocked(team, now);
                locked++;
            }
        }
        return locked;
    }

    public Team ensureTeam(
            Hackathon hackathon,
            String teamName,
            User leader,
            Chapter chapter,
            TeamStatus status,
            boolean locked,
            LocalDateTime now) {
        Team team = teamRepository.findByHackathon_Id(hackathon.getId()).stream()
                .filter(t -> teamName.equals(t.getTeamName()))
                .findFirst()
                .orElseGet(() -> teamRepository.save(Team.builder()
                        .hackathon(hackathon)
                        .teamName(teamName)
                        .leader(leader)
                        .chapter(chapter)
                        .status(status)
                        .isLocked(locked)
                        .lockedAt(locked ? now : null)
                        .createdAt(now)
                        .build()));
        team.setStatus(status);
        team.setIsLocked(locked);
        team.setLockedAt(locked ? (team.getLockedAt() != null ? team.getLockedAt() : now) : null);
        if (status == TeamStatus.REJECTED && team.getRejectionReason() == null) {
            team.setRejectionReason("Seed GĐ2 — rejected demo");
        }
        if (status == TeamStatus.ELIMINATED && team.getEliminatedAt() == null) {
            team.setEliminatedAt(now);
            team.setEliminationReason("Seed GĐ2 — eliminated demo");
        }
        return teamRepository.save(team);
    }

    public void ensureTeamMember(
            Team team,
            User user,
            TeamMemberRole role,
            TeamMemberStatus status,
            LocalDateTime now) {
        TeamMemberId id = new TeamMemberId(team.getId(), user.getId());
        if (teamMemberRepository.existsById(id)) {
            TeamMember member = teamMemberRepository.findById(id).orElseThrow();
            member.setRoleInTeam(role);
            member.setStatus(status);
            teamMemberRepository.save(member);
            return;
        }
        teamMemberRepository.save(TeamMember.builder()
                .id(id)
                .team(team)
                .user(user)
                .roleInTeam(role)
                .status(status)
                .joinedAt(now)
                .build());
    }

    public void markFormationSubmitted(Team team, LocalDateTime now) {
        if (team.getFormationSubmittedAt() == null) {
            team.setFormationSubmittedAt(now);
            teamRepository.save(team);
        }
    }

    public void repairFinishedArchiveAwardsSeed() {
        hackathonRepository.findBySlug(Gd1SeedConstants.SLUG_FINISHED).ifPresent(h -> {
            User student = upsertStudent(
                    Gd1SeedConstants.EMAIL_ARCHIVE_STUDENT,
                    "Archive Fall 2025 Student",
                    requireChapter(Gd1SeedConstants.CHAPTER_FPT_HCM));
            ensureFinishedArchiveIndividualRankings(h, student, 1);
        });
    }

    /**
     * Deepen shallow FINISHED archive ({@link Gd1SeedConstants#SLUG_FINISHED}):
     * locked teams + lottery + published prelim + advanced + prelim/final submissions & scores
     * with Internal + Guest judges on CK (RQ3). Idempotent.
     */
    @Transactional
    public void deepenFinishedArchiveIfShallow() {
        hackathonRepository.findBySlug(Gd1SeedConstants.SLUG_FINISHED).ifPresent(hackathon -> {
            List<Round> rounds = roundRepository.findByHackathon_IdOrderByExamAtAsc(hackathon.getId());
            Round prelim = rounds.stream()
                    .filter(r -> !Boolean.TRUE.equals(r.getIsFinal()))
                    .findFirst()
                    .orElse(null);
            Round finalRound = rounds.stream()
                    .filter(r -> Boolean.TRUE.equals(r.getIsFinal()))
                    .findFirst()
                    .orElse(null);
            if (prelim == null || finalRound == null) {
                return;
            }

            LocalDateTime lockedAt = LocalDateTime.now();
            prelim.setIsActive(false);
            prelim.setScoringLocked(true);
            prelim.setScoringLockedAt(lockedAt);
            prelim.setIsPublished(true);
            finalRound.setIsActive(false);
            finalRound.setScoringLocked(true);
            finalRound.setScoringLockedAt(lockedAt);
            roundRepository.save(prelim);
            roundRepository.save(finalRound);

            ensureFinalGuestJudgeAssignment(hackathon, finalRound);

            List<Track> tracks = trackRepository.findByHackathonIdOrderById(hackathon.getId());
            if (tracks.size() < 2) {
                log.warn("[HackathonDevSeedHelper] deepenFinishedArchive — need ≥2 tracks on {}",
                        Gd1SeedConstants.SLUG_FINISHED);
                return;
            }
            Track track1 = tracks.get(0);
            Track track2 = tracks.get(1);

            long finalSubs = submissionRepository.countByRoundId(finalRound.getId());
            List<Team> activeTeams = teamRepository.findByHackathon_Id(hackathon.getId()).stream()
                    .filter(t -> t.getStatus() == TeamStatus.ACTIVE)
                    .toList();
            List<Criteria> finalCriteria = listFinalCriteria(finalRound);
            float[] finalScores = {9.1f, 8.6f, 8.2f};
            if (activeTeams.size() >= 3 && finalSubs >= 3) {
                // Đã sâu — vẫn refresh điểm CK lệch giữa GK (inter-rater RBL).
                List<Submission> existingFinals = submissionRepository.findByRound_Id(finalRound.getId());
                for (int i = 0; i < existingFinals.size(); i++) {
                    float base = finalScores[Math.min(i, finalScores.length - 1)];
                    if (!finalCriteria.isEmpty()) {
                        ensureFinalScoresFromAllAssignedJudges(
                                finalRound, existingFinals.get(i), finalCriteria, base);
                    }
                }
                log.debug("[HackathonDevSeedHelper] deepenFinishedArchive — already deep, refreshed RBL scores (teams={}, finalSubs={})",
                        activeTeams.size(), finalSubs);
                return;
            }

            User coordinator = requireCoordinator();
            Chapter hcm = requireChapter(Gd1SeedConstants.CHAPTER_FPT_HCM);
            Chapter hn = requireChapter(Gd1SeedConstants.CHAPTER_FPT_HN);
            LocalDateTime now = LocalDateTime.now();
            float[] prelimScores = {9.0f, 8.5f, 8.0f};
            String[] teamNames = {
                    "Archive Alpha",
                    "Archive Beta",
                    "Archive Gamma"
            };
            Chapter[] chapters = {hcm, hcm, hn};
            User judge1 = requireJudge1();
            User judge2 = requireJudge2();
            List<Team> teams = new ArrayList<>();
            List<Submission> prelimTrack1 = new ArrayList<>();
            List<Submission> prelimTrack2 = new ArrayList<>();
            List<Submission> finalSubList = new ArrayList<>();
            LocalDateTime prelimSubmittedAt = now.minusDays(40);

            for (int i = 0; i < teamNames.length; i++) {
                int idx = i + 1;
                User leader = upsertStudent(
                        "student.archive.t%d@fpt.edu.vn".formatted(idx),
                        "Archive Student %d".formatted(idx),
                        chapters[i]);
                registerStudent(hackathon, leader);
                Team team = ensureActiveTeam(hackathon, teamNames[i], leader, chapters[i], now);
                ensureTeamLocked(team, now);
                Track track = (idx % 2 == 1) ? track1 : track2;
                ensureLottery(hackathon, prelim, track, "BANG-" + ((idx % 2) + 1), team, coordinator, now);
                markAdvanced(team, prelim, finalRound, hackathon);
                teams.add(team);

                User judge = (i % 2 == 0) ? judge1 : judge2;
                Submission prelimSub = ensurePrelimSubmission(
                        hackathon, prelim, track, team,
                        SubmissionStatus.SUBMITTED,
                        false,
                        prelimSubmittedAt.minusMinutes(i));
                scoreAllTrackCriteria(prelimSub, track, judge, prelimScores[i], true);
                if (i % 2 == 0) {
                    prelimTrack1.add(prelimSub);
                } else {
                    prelimTrack2.add(prelimSub);
                }

                Submission finalSub = ensureFinalSubmission(
                        hackathon, finalRound, team,
                        "https://github.com/seal-warriors/archive-team%02d".formatted(idx));
                if (!finalCriteria.isEmpty()) {
                    ensureFinalScoresFromAllAssignedJudges(
                            finalRound, finalSub, finalCriteria, finalScores[i]);
                }
                finalSubList.add(finalSub);
            }

            seedPresentationQueue(prelim, track1, prelimTrack1, -1);
            seedPresentationQueue(prelim, track2, prelimTrack2, -1);
            seedFinalPresentationQueue(finalRound, finalSubList, -1);
            seedPrelimTrackProblems(prelim);
            seedFinalRoundProblem(finalRound);

            if (!teams.isEmpty()) {
                ensureFirstPrize(hackathon, finalRound, teams.get(0), coordinator);
                if (teams.size() > 1) {
                    ensureSecondPrize(hackathon, finalRound, teams.get(1), coordinator);
                }
                if (teams.size() > 2) {
                    ensureThirdPrize(hackathon, finalRound, teams.get(2), coordinator);
                }
            }

            log.info("""
                    [HackathonDevSeedHelper] deepenFinishedArchive slug={} teams={} finalSubs={}
                      Internal+Guest scored CK — RBL analytics ready
                    """,
                    Gd1SeedConstants.SLUG_FINISHED,
                    teams.size(),
                    finalSubList.size());
        });
    }

    /** Gỡ mọi team_round_tracks của đội — dùng seed Fall track select. */
    @Transactional
    public void clearTeamRoundTracks(Integer teamId) {
        teamRoundTrackRepository.findByTeam_Id(teamId).forEach(teamRoundTrackRepository::delete);
    }

    /** Gỡ mentor-team assignments của hackathon — giữ mentor track assignments. */
    @Transactional
    public void clearMentorTeamAssignmentsForHackathon(Integer hackathonId) {
        mentorTeamAssignmentRepository.findByHackathon_Id(hackathonId)
                .forEach(mentorTeamAssignmentRepository::delete);
    }

    /** Gỡ mọi mentor-team assignments của mentor — dùng cho tài khoản mentor track-only riêng. */
    @Transactional
    public void clearMentorTeamAssignmentsForMentor(Integer mentorId) {
        mentorTeamAssignmentRepository.findByMentor_Id(mentorId)
                .forEach(mentorTeamAssignmentRepository::delete);
    }

    /**
     * Bổ sung individual_rankings cho archive Fall FINISHED — FR-U-32 e2e.
     */
    @Transactional
    public void ensureFinishedArchiveIndividualRankings(Hackathon hackathon, User student, int rank) {
        if (hackathon.getStatus() != HackathonStatus.FINISHED || hackathon.getSeason() != Season.Fall) {
            return;
        }
        hackathon.setIndividualRankingEnabled(true);
        hackathonRepository.save(hackathon);
        boolean exists = individualRankingRepository.findByHackathon_IdOrderByRankAsc(hackathon.getId()).stream()
                .anyMatch(ir -> ir.getUser().getId().equals(student.getId()));
        if (!exists) {
            individualRankingRepository.save(IndividualRanking.builder()
                    .hackathon(hackathon)
                    .user(student)
                    .scoreThisHackathon(90f + rank)
                    .cumulativeScore(90f + rank)
                    .rank(rank)
                    .isEnabled(true)
                    .calculatedAt(LocalDateTime.now())
                    .build());
        }
    }

    public void ensureMentorTeamAssignment(
            Hackathon hackathon,
            Round prelim,
            Team team,
            User mentor,
            User coordinator,
            LocalDateTime now) {
        if (mentorTeamAssignmentRepository.findByTeam_IdAndRound_Id(team.getId(), prelim.getId()).isEmpty()) {
            mentorTeamAssignmentRepository.save(MentorTeamAssignment.builder()
                    .hackathon(hackathon)
                    .round(prelim)
                    .team(team)
                    .mentor(mentor)
                    .assignedBy(coordinator)
                    .assignedAt(now)
                    .build());
        }
    }

    public void clearFinalJudgeAssignments(Round finalRound) {
        judgeAssignmentRepository.findByRoundId(finalRound.getId())
                .forEach(judgeAssignmentRepository::delete);
    }

    public Submission ensureFinalSubmission(
            Hackathon hackathon,
            Round finalRound,
            Team team,
            SubmissionStatus status,
            boolean isLate,
            LocalDateTime submittedAt) {
        List<Submission> existing = submissionRepository.findByTeam_IdAndRound_Id(team.getId(), finalRound.getId());
        if (!existing.isEmpty()) {
            Submission sub = existing.get(0);
            sub.setStatus(status);
            sub.setIsLate(isLate);
            sub.setSubmittedAt(submittedAt);
            return submissionRepository.save(sub);
        }
        return submissionRepository.save(Submission.builder()
                .team(team)
                .hackathon(hackathon)
                .round(finalRound)
                .track(null)
                .repoUrl("https://github.com/seal-warriors/%s".formatted(team.getTeamName().replace(' ', '-').toLowerCase()))
                .demoUrl("https://demo.example.com/" + team.getId())
                .slideUrl("https://docs.google.com/presentation/d/seed-" + team.getId() + "/edit")
                .status(status)
                .isLate(isLate)
                .submittedAt(submittedAt)
                .build());
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

    /** Đánh dấu slide đã lưu (dev) — FE hiển thị {@code ON_TIME}, không cần file MinIO thật. */
    public void markSubmissionSlideSeeded(Submission submission) {
        submission.setSlideStorageKey("dev-seed/slides/submission-" + submission.getId() + ".pdf");
        submission.setSlideOriginalFilename("seed-slide.pdf");
        submission.setSlideContentType("application/pdf");
        submission.setSlideSizeBytes(1024L);
        submission.setSlideUploadedAt(LocalDateTime.now());
        submissionRepository.save(submission);
    }

    public void clearTrackJudgeAssignments(Track track) {
        judgeAssignmentRepository.deleteByTrackId(track.getId());
    }

    /** Xóa toàn bộ criteria của track — dùng seed ROUND_NO_CRITERIA. */
    @Transactional
    public void clearTrackCriteria(Track track) {
        criteriaRepository.findByTrackIdOrderByDisplayOrderAsc(track.getId())
                .forEach(criteriaRepository::delete);
    }

    /** Xóa criteria vòng Chung kết — dùng seed activate CK → ROUND_NO_CRITERIA. */
    @Transactional
    public void clearFinalRoundCriteria(Round finalRound) {
        criteriaRepository.findByFinalRoundIdOrderByDisplayOrderAsc(finalRound.getId())
                .forEach(criteriaRepository::delete);
    }

    /**
     * Xóa hẳn round CK (shell) — dùng seed prelim-only → MISSING_FINAL_ROUND.
     */
    @Transactional
    public void removeFinalRoundShell(Hackathon hackathon) {
        Round finalRound = roundRepository.findByHackathon_IdOrderByExamAtAsc(hackathon.getId()).stream()
                .filter(r -> Boolean.TRUE.equals(r.getIsFinal()))
                .findFirst()
                .orElse(null);
        if (finalRound == null) {
            return;
        }
        clearFinalRoundArtifacts(hackathon.getId());
        clearFinalRoundCriteria(finalRound);
        clearFinalRoundJudgeAssignments(finalRound);
        jdbcTemplate.update(
                "DELETE FROM team_round_participation WHERE round_id = ?", finalRound.getId());
        roundRepository.delete(finalRound);
    }

    /** Sửa weight một tiêu chí track — dùng seed ROUND_WEIGHT_NOT_ONE. */
    @Transactional
    public void setTrackCriteriaWeight(Track track, String criteriaName, float weight) {
        criteriaRepository.findByTrackIdOrderByDisplayOrderAsc(track.getId()).stream()
                .filter(c -> criteriaName.equals(c.getName()))
                .findFirst()
                .ifPresent(c -> {
                    c.setWeight(weight);
                    criteriaRepository.save(c);
                });
    }

    /** Cấu hình vòng sơ loại GĐ4 variant — không gọi lock API (tránh auto-apply tiebreak). */
    @Transactional
    public void applyPrelimGd4VariantConfig(
            Round prelim,
            TiebreakRule tiebreakRule,
            Integer topNAdvance,
            Integer minTeamsFinal,
            boolean scoringLocked,
            boolean published) {
        User coordinator = requireCoordinator();
        applyPrelimState(
                prelim,
                new PrelimState(false, true, scoringLocked, published, topNAdvance, minTeamsFinal),
                coordinator);
        prelim.setTiebreakRule(tiebreakRule);
        roundRepository.save(prelim);
    }

    @Transactional
    public void clearPrelimTiebreakEvaluations(Round prelim) {
        tiebreakEvaluationRepository.findByRound_Id(prelim.getId())
                .forEach(tiebreakEvaluationRepository::delete);
    }

    /** Gán mentor theo track (FR-18) — idempotent. */
    @Transactional
    public void ensureMentorTrackAssignment(Track track, User mentor, User coordinator) {
        boolean exists = mentorAssignmentRepository.findByTrackId(track.getId()).stream()
                .anyMatch(ma -> ma.getMentor().getId().equals(mentor.getId()));
        if (!exists) {
            mentorAssignmentRepository.save(MentorAssignment.builder()
                    .mentor(mentor)
                    .track(track)
                    .assignedBy(coordinator)
                    .assignedAt(LocalDateTime.now())
                    .build());
        }
    }

    /** Gán judge lên track nếu chưa có — idempotent. */
    @Transactional
    public void ensureJudgeOnTrack(User judge, Track track, User coordinator) {
        if (!judgeAssignmentRepository.existsByJudgeIdAndTrackId(judge.getId(), track.getId())) {
            saveJudgeTrack(judge, track, coordinator, LocalDateTime.now());
        }
    }

    /** Gỡ mentor khỏi track. */
    @Transactional
    public void clearMentorAssignments(Track track) {
        mentorAssignmentRepository.findByTrackId(track.getId())
                .forEach(mentorAssignmentRepository::delete);
    }

    public boolean isTrackWithoutJudges(Track track) {
        return judgeAssignmentRepository.findByTrackId(track.getId()).isEmpty();
    }

    public void repairPrelimState(Round prelim, PrelimState state) {
        applyPrelimState(prelim, state, requireCoordinator());
    }

    public void repairFinalState(Round finalRound, FinalState state) {
        applyFinalState(finalRound, state, requireCoordinator());
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

    public void clearPresentationQueues(Round prelim, Track track1, Track track2) {
        presentationSlotRepository.deleteByRound_IdAndTrack_Id(prelim.getId(), track1.getId());
        presentationSlotRepository.deleteByRound_IdAndTrack_Id(prelim.getId(), track2.getId());
    }

    public void clearSubmissionScores(Integer submissionId) {
        scoreRepository.deleteAll(scoreRepository.findBySubmission_Id(submissionId));
    }

    /** Xóa bài nộp sơ loại của đội (scores + metadata + submission) — dùng reset demo. */
    public void clearPrelimSubmission(Team team, Round prelim) {
        List<Submission> subs = submissionRepository.findByTeam_IdAndRound_Id(team.getId(), prelim.getId());
        for (Submission sub : subs) {
            clearSubmissionScores(sub.getId());
            jdbcTemplate.update(
                    "DELETE FROM submission_metadata WHERE submission_id = ?",
                    sub.getId());
            jdbcTemplate.update(
                    "DELETE FROM judge_submission_scoring_confirmations WHERE submission_id = ?",
                    sub.getId());
            if (sub.getTrack() != null) {
                presentationSlotRepository
                        .findByRound_IdAndTrack_IdOrderBySequenceOrderAsc(
                                prelim.getId(), sub.getTrack().getId())
                        .stream()
                        .filter(slot -> slot.getSubmission() != null
                                && slot.getSubmission().getId().equals(sub.getId()))
                        .forEach(presentationSlotRepository::delete);
            }
            submissionRepository.delete(sub);
        }
    }

    public void seedPresentationQueue(Round prelim, Track track, List<Submission> gradableInOrder) {
        seedPresentationQueue(prelim, track, gradableInOrder, 0, null);
    }

    /**
     * @param presentingIndex slot đang PRESENTING (0-based); {@code -1} = tất cả DONE
     */
    public void seedPresentationQueue(
            Round prelim,
            Track track,
            List<Submission> gradableInOrder,
            int presentingIndex) {
        seedPresentationQueue(prelim, track, gradableInOrder, presentingIndex, null);
    }

    /**
     * @param presentingTimer timer cho slot PRESENTING; {@code null} → {@code SETUP}
     */
    public void seedPresentationQueue(
            Round prelim,
            Track track,
            List<Submission> gradableInOrder,
            int presentingIndex,
            PresentationTimerSeed presentingTimer) {
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
        for (int i = 0; i < gradableInOrder.size(); i++) {
            Submission submission = gradableInOrder.get(i);
            LocalDateTime start = examAt.plusMinutes((long) (order - 1) * slotMinutes);
            PresentationQueueStatus status;
            if (presentingIndex < 0) {
                status = PresentationQueueStatus.DONE;
            } else if (i < presentingIndex) {
                status = PresentationQueueStatus.DONE;
            } else if (i == presentingIndex) {
                status = PresentationQueueStatus.PRESENTING;
            } else {
                status = PresentationQueueStatus.WAITING;
            }
            PresentationTimerPhase timerPhase = status == PresentationQueueStatus.PRESENTING
                    ? PresentationTimerPhase.SETUP
                    : PresentationTimerPhase.IDLE;
            Integer pausedAccumulated = 0;
            LocalDateTime presentationStartedAt = null;
            LocalDateTime qaStartedAt = null;
            LocalDateTime pausedAt = null;
            PresentationTimerPhase phaseBeforePause = null;
            if (status == PresentationQueueStatus.PRESENTING && presentingTimer != null) {
                timerPhase = presentingTimer.phase();
                presentationStartedAt = presentingTimer.presentationStartedAt();
                qaStartedAt = presentingTimer.qaStartedAt();
                pausedAt = presentingTimer.pausedAt();
                phaseBeforePause = presentingTimer.phaseBeforePause();
                pausedAccumulated = presentingTimer.pausedAccumulatedSeconds();
            }
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
                    .timerPhase(timerPhase)
                    .presentationStartedAt(presentationStartedAt)
                    .qaStartedAt(qaStartedAt)
                    .pausedAt(pausedAt)
                    .timerPhaseBeforePause(phaseBeforePause)
                    .pausedAccumulatedSeconds(pausedAccumulated)
                    .build());
            order++;
        }
        track.setPresentationShuffled(true);
        trackRepository.save(track);
    }

    public void clearFinalRoundJudgeAssignments(Round finalRound) {
        judgeAssignmentRepository.deleteByRoundId(finalRound.getId());
    }

    public boolean isFinalRoundWithoutJudges(Round finalRound) {
        return judgeAssignmentRepository.findByRoundId(finalRound.getId()).isEmpty();
    }

    public void seedFinalPresentationQueue(Round finalRound, List<Submission> gradableInOrder) {
        seedFinalPresentationQueue(finalRound, gradableInOrder, 0, null);
    }

    public void seedFinalPresentationQueue(
            Round finalRound,
            List<Submission> gradableInOrder,
            int presentingIndex) {
        seedFinalPresentationQueue(finalRound, gradableInOrder, presentingIndex, null);
    }

    /**
     * Hàng đợi thuyết trình CK — {@code trackId=null}, một pool chung.
     *
     * @param presentingIndex slot đang PRESENTING (0-based); {@code -1} = tất cả DONE
     */
    public void seedFinalPresentationQueue(
            Round finalRound,
            List<Submission> gradableInOrder,
            int presentingIndex,
            PresentationTimerSeed presentingTimer) {
        presentationSlotRepository.deleteByRound_IdAndTrackIsNull(finalRound.getId());
        if (gradableInOrder.isEmpty()) {
            return;
        }
        LocalDateTime examAt = finalRound.getExamAt() != null
                ? finalRound.getExamAt()
                : LocalDateTime.now().withSecond(0).withNano(0);
        int slotMinutes = finalRound.getDefaultPresentationMinutes() != null
                && finalRound.getDefaultPresentationMinutes() > 0
                ? finalRound.getDefaultPresentationMinutes()
                : 10;
        int order = 1;
        for (int i = 0; i < gradableInOrder.size(); i++) {
            Submission submission = gradableInOrder.get(i);
            LocalDateTime start = examAt.plusMinutes((long) (order - 1) * slotMinutes);
            PresentationQueueStatus status;
            if (presentingIndex < 0) {
                status = PresentationQueueStatus.DONE;
            } else if (i < presentingIndex) {
                status = PresentationQueueStatus.DONE;
            } else if (i == presentingIndex) {
                status = PresentationQueueStatus.PRESENTING;
            } else {
                status = PresentationQueueStatus.WAITING;
            }
            PresentationTimerPhase timerPhase = status == PresentationQueueStatus.PRESENTING
                    ? PresentationTimerPhase.SETUP
                    : PresentationTimerPhase.IDLE;
            Integer pausedAccumulated = 0;
            LocalDateTime presentationStartedAt = null;
            LocalDateTime qaStartedAt = null;
            LocalDateTime pausedAt = null;
            PresentationTimerPhase phaseBeforePause = null;
            if (status == PresentationQueueStatus.PRESENTING && presentingTimer != null) {
                timerPhase = presentingTimer.phase();
                presentationStartedAt = presentingTimer.presentationStartedAt();
                qaStartedAt = presentingTimer.qaStartedAt();
                pausedAt = presentingTimer.pausedAt();
                phaseBeforePause = presentingTimer.phaseBeforePause();
                pausedAccumulated = presentingTimer.pausedAccumulatedSeconds();
            }
            presentationSlotRepository.save(PresentationSlot.builder()
                    .round(finalRound)
                    .track(null)
                    .submission(submission)
                    .team(submission.getTeam())
                    .startsAt(start)
                    .endsAt(start.plusMinutes(slotMinutes))
                    .location("CK Phòng " + order)
                    .sequenceOrder(order)
                    .queueStatus(status)
                    .timerPhase(timerPhase)
                    .presentationStartedAt(presentationStartedAt)
                    .qaStartedAt(qaStartedAt)
                    .pausedAt(pausedAt)
                    .timerPhaseBeforePause(phaseBeforePause)
                    .pausedAccumulatedSeconds(pausedAccumulated)
                    .build());
            order++;
        }
        finalRound.setPresentationShuffled(true);
        roundRepository.save(finalRound);
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
        // Xoá bài nộp/điểm + lottery toàn bộ — tránh BXH còn điểm trong khi track đếm 0 đội.
        clearPrelimRoundArtifacts(hackathon.getId());
        clearFinalRoundArtifacts(hackathon.getId());
        clearWorkflowArtifactsForGd2Reset(hackathon.getId());
        clearAllLotteryAssignments(hackathon.getId());
        resetTeamsToPreAdvance(hackathon, prelim, finalRound);

        syncHackathonCalendarFromDates(hackathon.getSlug(), computeGd2RegistrationOpenDates());
        applyPrelimState(prelim, new PrelimState(false, false, false, false, 2, 4), coordinator);
        prelim.setActivatedAt(null);
        prelim.setSubmissionClosedEarlyAt(null);
        prelim.setProblemStatementUrl(null);
        prelim.setProblemReleasedAt(null);
        prelim.setScoringLockedAt(null);
        prelim.setScoringLockedBy(null);
        prelim.setPublishedAt(null);
        prelim.setPublishedBy(null);
        trackRepository.findByRoundIdOrderBySequenceOrderAsc(prelim.getId())
                .forEach(t -> {
                    t.setProblemStatementUrl(null);
                    t.setProblemStatementStorageKey(null);
                    t.setProblemStatementOriginalFilename(null);
                    trackRepository.save(t);
                });
        roundRepository.save(prelim);
        applyFinalState(finalRound, new FinalState(false, false), coordinator);
        finalRound.setActivatedAt(null);
        finalRound.setSubmissionClosedEarlyAt(null);
        finalRound.setProblemReleasedAt(null);
        finalRound.setForceLocked(false);
        finalRound.setForceLockReason(null);
        finalRound.setScoringLockedBy(null);
        roundRepository.save(finalRound);

        hackathon.setScheduleAdjustedAt(null);
        if (hackathon.getStatus() != HackathonStatus.ONGOING) {
            hackathon.setStatus(HackathonStatus.ONGOING);
        }
        hackathonRepository.save(hackathon);

        for (Team team : teamRepository.findByHackathon_Id(hackathon.getId())) {
            if (Boolean.TRUE.equals(team.getIsLocked())) {
                team.setIsLocked(false);
                team.setLockedAt(null);
                teamRepository.save(team);
            }
        }
        // Gắn lại PDF đề track + CK sau khi reset storage key (Coord upload trước phát đề).
        seedPrelimTrackProblems(prelim);
        seedFinalRoundProblem(finalRound);
    }

    /** Xóa prizes / export_jobs / tiebreak khi tua về GĐ2. */
    @Transactional
    public void clearWorkflowArtifactsForGd2Reset(Integer hackathonId) {
        jdbcTemplate.update("""
                DELETE te FROM tiebreak_evaluations te
                INNER JOIN rounds r ON r.id = te.round_id
                WHERE r.hackathon_id = ?
                """, hackathonId);
        jdbcTemplate.update("DELETE FROM prizes WHERE hackathon_id = ?", hackathonId);
        jdbcTemplate.update("DELETE FROM export_jobs WHERE hackathon_id = ?", hackathonId);
        jdbcTemplate.update("""
                DELETE ir FROM individual_rankings ir
                WHERE ir.hackathon_id = ?
                """, hackathonId);
        jdbcTemplate.update("""
                DELETE cr FROM chapter_rankings cr
                WHERE cr.hackathon_id = ?
                """, hackathonId);
    }

    /** Xóa toàn bộ phân bảng Sơ loại + Chung kết. */
    @Transactional
    public void clearAllLotteryAssignments(Integer hackathonId) {
        jdbcTemplate.update("""
                DELETE trt FROM team_round_tracks trt
                INNER JOIN tracks t ON t.id = trt.track_id
                INNER JOIN rounds r ON r.id = t.round_id
                WHERE r.hackathon_id = ?
                """, hackathonId);
        jdbcTemplate.update("""
                DELETE trp FROM team_round_participation trp
                INNER JOIN rounds r ON r.id = trp.round_id
                WHERE r.hackathon_id = ?
                """, hackathonId);
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

    /** Reset hackathon GĐ4 published — chưa advance, CK chưa active. */
    @Transactional
    public void repairHackathonForGd4PublishedRetest(Hackathon hackathon, Round prelim, Round finalRound) {
        User coordinator = requireCoordinator();
        clearFinalRoundArtifacts(hackathon.getId());
        resetTeamsToPreAdvance(hackathon, prelim, finalRound);
        syncHackathonCalendarFromDates(hackathon.getSlug(), computeGd4AdvanceReadyDates());
        applyPrelimState(prelim, new PrelimState(false, true, true, true, 1, 6), coordinator);
        roundRepository.save(prelim);
        applyFinalState(finalRound, new FinalState(false, false), coordinator);
        if (hackathon.getStatus() != HackathonStatus.ONGOING) {
            hackathon.setStatus(HackathonStatus.ONGOING);
            hackathonRepository.save(hackathon);
        }
    }

    /** Reset hackathon GĐ4 tiebreak gate — locked, unpublished, chưa advance. */
    @Transactional
    public void repairHackathonForGd4TiebreakRetest(Hackathon hackathon, Round prelim, Round finalRound) {
        repairHackathonForGd4Retest(hackathon, prelim, finalRound);
    }

    /** Reset hackathon GĐ4 tiebreak resolved — locked + published, chưa advance, xóa penalty cũ. */
    @Transactional
    public void repairHackathonForGd4TiebreakResolvedRetest(Hackathon hackathon, Round prelim, Round finalRound) {
        User coordinator = requireCoordinator();
        clearFinalRoundArtifacts(hackathon.getId());
        clearPrelimRoundArtifacts(hackathon.getId());
        resetTeamsToPreAdvance(hackathon, prelim, finalRound);
        syncHackathonCalendarFromDates(hackathon.getSlug(), computeGd4AdvanceReadyDates());
        applyPrelimState(prelim, new PrelimState(false, true, true, true, 1, 6), coordinator);
        prelim.setTopNAdvance(1);
        roundRepository.save(prelim);
        applyFinalState(finalRound, new FinalState(false, false), coordinator);
        if (hackathon.getStatus() != HackathonStatus.ONGOING) {
            hackathon.setStatus(HackathonStatus.ONGOING);
            hackathonRepository.save(hackathon);
        }
    }

    /** Reset hackathon GĐ5 late-hardlock — CK active, deadline đã qua, chưa có submission CK. */
    @Transactional
    public void repairHackathonForGd5LateHardLockRetest(Hackathon hackathon, Round prelim, Round finalRound) {
        User coordinator = requireCoordinator();
        syncHackathonCalendarFromDates(hackathon.getSlug(), computeGd5LateHardLockDates());
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
     * Seed penalty coordinator sau {@code POST /tiebreak/resolve} — đội thứ hai trong thứ tự bị -0.01.
     */
    @Transactional
    public void seedCoordinatorTiebreakResolve(
            Round prelim, Team preferredTeam, Team penalizedTeam, User coordinator, String note) {
        tiebreakEvaluationRepository.findByRound_IdAndTeam_IdAndJudge_Id(
                        prelim.getId(), preferredTeam.getId(), coordinator.getId())
                .ifPresent(tiebreakEvaluationRepository::delete);
        LocalDateTime now = LocalDateTime.now();
        Optional<TiebreakEvaluation> existing = tiebreakEvaluationRepository.findByRound_IdAndTeam_IdAndJudge_Id(
                prelim.getId(), penalizedTeam.getId(), coordinator.getId());
        if (existing.isPresent()) {
            TiebreakEvaluation te = existing.get();
            te.setPenaltyScore(0.01f);
            te.setIsCastingVote(true);
            te.setTiebreakLevel(2);
            te.setNotes(note);
            te.setEvaluatedAt(now);
            tiebreakEvaluationRepository.save(te);
            return;
        }
        tiebreakEvaluationRepository.save(TiebreakEvaluation.builder()
                .round(prelim)
                .team(penalizedTeam)
                .judge(coordinator)
                .penaltyScore(0.01f)
                .isCastingVote(true)
                .tiebreakLevel(2)
                .notes(note)
                .evaluatedAt(now)
                .build());
    }

    /**
     * Reset hackathon GĐ4 CK activate ready — published + 6 đội ADVANCED, guest judge, CK inactive.
     * Không reset participation; gọi {@link #markAdvanced} lại trong seeder nếu cần.
     */
    @Transactional
    public void repairHackathonForGd4CkActivateRetest(Hackathon hackathon, Round prelim, Round finalRound) {
        User coordinator = requireCoordinator();
        clearFinalRoundArtifacts(hackathon.getId());
        syncHackathonCalendarFromDates(hackathon.getSlug(), computeGd4AdvanceReadyDates());
        applyPrelimState(prelim, new PrelimState(false, true, true, true, 1, 6), coordinator);
        roundRepository.save(prelim);
        applyFinalState(finalRound, new FinalState(false, false), coordinator);
        releaseFinalProblem(finalRound);
        ensureFinalGuestJudgeAssignment(hackathon, finalRound);
        if (hackathon.getStatus() != HackathonStatus.ONGOING) {
            hackathon.setStatus(HackathonStatus.ONGOING);
            hackathonRepository.save(hackathon);
        }
    }

    /** Reset hackathon GĐ4 edge — published + advanced, CK không có judge. */
    @Transactional
    public void repairHackathonForGd4EdgeRetest(Hackathon hackathon, Round prelim, Round finalRound) {
        User coordinator = requireCoordinator();
        clearFinalRoundArtifacts(hackathon.getId());
        syncHackathonCalendarFromDates(hackathon.getSlug(), computeGd4AdvanceReadyDates());
        applyPrelimState(prelim, new PrelimState(false, true, true, true, 2, 4), coordinator);
        applyFinalState(finalRound, new FinalState(false, false), coordinator);
        clearFinalRoundJudgeAssignments(finalRound);
        if (hackathon.getStatus() != HackathonStatus.ONGOING) {
            hackathon.setStatus(HackathonStatus.ONGOING);
            hackathonRepository.save(hackathon);
        }
    }

    /** Reset hackathon GĐ5 submit-open — CK active, chưa có submission CK. */
    @Transactional
    public void repairHackathonForGd5SubmitOpenRetest(Hackathon hackathon, Round prelim, Round finalRound) {
        repairHackathonForGd5Retest(hackathon, prelim, finalRound);
    }

    /** Đồng bộ lịch GĐ5 scoring-live mà không xóa submission/score/queue đã seed. */
    @Transactional
    public void repairGd5ScoringLiveFeTesting(Hackathon hackathon, Round prelim, Round finalRound) {
        repairGd5FeTestingScheduleAndState(hackathon, prelim, finalRound);
    }

    /** Reset hackathon GĐ5 edge — CK inactive, đội đã ADVANCED. */
    @Transactional
    public void repairHackathonForGd5EdgeRetest(Hackathon hackathon, Round prelim, Round finalRound) {
        User coordinator = requireCoordinator();
        syncHackathonCalendarFromDates(hackathon.getSlug(), computeGd5FinalActiveDates());
        if (hackathon.getStatus() != HackathonStatus.ONGOING) {
            hackathon.setStatus(HackathonStatus.ONGOING);
            hackathonRepository.save(hackathon);
        }
        applyPrelimState(prelim, new PrelimState(false, true, true, true, 2, 2), coordinator);
        applyFinalState(finalRound, new FinalState(false, false), coordinator);
        releaseFinalProblem(finalRound);
    }

    /** Reset GĐ4 SUBMISSION_TIME tiebreak — locked, unpublished, rule SUBMISSION_TIME. */
    @Transactional
    public void repairHackathonForGd4TiebreakSubmissionTimeRetest(
            Hackathon hackathon, Round prelim, Round finalRound) {
        clearFinalRoundArtifacts(hackathon.getId());
        clearPrelimTiebreakEvaluations(prelim);
        resetTeamsToPreAdvance(hackathon, prelim, finalRound);
        syncHackathonCalendarFromDates(hackathon.getSlug(), computeGd4AdvanceReadyDates());
        applyPrelimGd4VariantConfig(
                prelim,
                TiebreakRule.SUBMISSION_TIME,
                2,
                3,
                true,
                false);
        applyFinalState(finalRound, new FinalState(false, false), requireCoordinator());
        if (hackathon.getStatus() != HackathonStatus.ONGOING) {
            hackathon.setStatus(HackathonStatus.ONGOING);
            hackathonRepository.save(hackathon);
        }
    }

    /** Reset GĐ4 COORDINATOR_DECISION tiebreak — locked, unpublished, chưa resolve. */
    @Transactional
    public void repairHackathonForGd4TiebreakManualRetest(
            Hackathon hackathon, Round prelim, Round finalRound) {
        clearFinalRoundArtifacts(hackathon.getId());
        clearPrelimTiebreakEvaluations(prelim);
        resetTeamsToPreAdvance(hackathon, prelim, finalRound);
        syncHackathonCalendarFromDates(hackathon.getSlug(), computeGd4AdvanceReadyDates());
        applyPrelimGd4VariantConfig(
                prelim,
                TiebreakRule.COORDINATOR_DECISION,
                1,
                4,
                true,
                false);
        applyFinalState(finalRound, new FinalState(false, false), requireCoordinator());
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
        applyPrelimState(prelim, new PrelimState(false, true, true, true, 1, 6), coordinator);
        roundRepository.save(prelim);
        applyFinalState(finalRound, new FinalState(false, false), coordinator);
        if (hackathon.getStatus() != HackathonStatus.ONGOING) {
            hackathon.setStatus(HackathonStatus.ONGOING);
            hackathonRepository.save(hackathon);
        }
    }
}
