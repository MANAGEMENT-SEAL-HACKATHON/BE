package com.sealhackathon.api.config.seed;

import com.sealhackathon.api.chapters.entity.Chapter;
import com.sealhackathon.api.chapters.repository.ChapterRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
     * Đồng bộ submissionOpen / submissionDeadline theo examAt + codingDurationHours (repair DB dev).
     */
    public void repairRoundSubmissionWindow(Round round) {
        repairRoundSubmissionWindowIfNeeded(round);
    }

    @Transactional
    public void repairAllDevHackathonRoundSchedules() {
        int fixed = 0;
        for (String slug : new String[] {
                Gd1SeedConstants.SLUG_INCOMPLETE,
                Gd1SeedConstants.SLUG_READY,
                Gd1SeedConstants.SLUG_ONGOING,
                Gd1SeedConstants.SLUG_FINISHED,
                GdExtendedSeedConstants.SLUG_GD3_PRELIM_OPEN,
                GdExtendedSeedConstants.SLUG_GD4_ADVANCE_READY,
                GdExtendedSeedConstants.SLUG_GD4_TIEBREAK,
                GdExtendedSeedConstants.SLUG_GD5_FINAL_ACTIVE,
                GdExtendedSeedConstants.SLUG_GD6_PENDING_CONFIRM
        }) {
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
                .passwordHash(passwordEncoder.encode(GdExtendedSeedConstants.DEV_STUDENT_PASSWORD))
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
        SeedDates dates = computeRelativeDates();
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

    private Track buildTrack(Round prelim, int sequence) {
        String suffix = sequence == 1 ? "RAG Pipeline" : "AI Agent";
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
        judgeAssignmentRepository.save(JudgeAssignment.builder()
                .judge(judge)
                .track(track)
                .assignmentType(JudgeAssignmentType.NORMAL)
                .assignedBy(coordinator)
                .assignedAt(at)
                .build());
    }

    /**
     * Milestone đủ KO+WS+AWARDS; lịch WS → KO (gap sau regEnd, trước eventStart).
     */
    @Transactional
    public int repairAllDevHackathonMilestoneEvents() {
        List<String> slugs = List.of(
                Gd1SeedConstants.SLUG_INCOMPLETE,
                Gd1SeedConstants.SLUG_READY,
                Gd1SeedConstants.SLUG_ONGOING,
                Gd1SeedConstants.SLUG_FINISHED,
                GdExtendedSeedConstants.SLUG_GD3_PRELIM_OPEN,
                GdExtendedSeedConstants.SLUG_GD4_ADVANCE_READY,
                GdExtendedSeedConstants.SLUG_GD4_TIEBREAK,
                GdExtendedSeedConstants.SLUG_GD5_FINAL_ACTIVE,
                GdExtendedSeedConstants.SLUG_GD6_PENDING_CONFIRM);
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
}
