package com.sealhackathon.api.me.student.service.impl;

import com.sealhackathon.api.common.audit.AuditAction;
import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.common.exception.ResourceNotFoundException;
import com.sealhackathon.api.common.security.CurrentUserAccessor;
import com.sealhackathon.api.common.util.ScoreScale;
import com.sealhackathon.api.hackathons.repository.HackathonRegistrationRepository;
import com.sealhackathon.api.hackathons.repository.HackathonRegistrationWithdrawalRepository;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.hackathons.support.HackathonRegistrationSupport;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.hackathons.value_object.Season;
import com.sealhackathon.api.individual_rankings.entity.IndividualRanking;
import com.sealhackathon.api.individual_rankings.repository.IndividualRankingRepository;
import com.sealhackathon.api.events.entity.PresentationSlot;
import com.sealhackathon.api.events.repository.PresentationSlotRepository;
import com.sealhackathon.api.me.student.dto.request.RelotteryTrackRequest;
import com.sealhackathon.api.me.student.dto.response.*;
import com.sealhackathon.api.me.student.service.StudentPortalService;
import com.sealhackathon.api.me.support.StudentAccessGuard;
import com.sealhackathon.api.presentation.support.PresentationDurationResolver;
import com.sealhackathon.api.presentation.support.PresentationTimerCalculator;
import com.sealhackathon.api.presentation.value_object.PresentationQueueStatus;
import com.sealhackathon.api.presentation.value_object.PresentationTimerPhase;
import com.sealhackathon.api.rounds.dto.response.ScoreBreakdownResponse;
import com.sealhackathon.api.rounds.dto.response.RoundRankingItemResponse;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.query.RoundRankingQueryService;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.rounds.service.RoundProgressionService;
import com.sealhackathon.api.rounds.support.RoundPresentationReadiness;
import com.sealhackathon.api.rounds.support.RoundProblemStatementStorage;
import com.sealhackathon.api.rounds.support.RoundResultVisibility;
import com.sealhackathon.api.storage.StoredObjectResource;
import com.sealhackathon.api.submissions.entity.Submission;
import com.sealhackathon.api.submissions.repository.SubmissionRepository;
import com.sealhackathon.api.submissions.support.SubmissionSlideStorage;
import com.sealhackathon.api.submissions.value_object.SubmissionStatus;
import com.sealhackathon.api.teams.dto.request.CreateTeamRequest;
import com.sealhackathon.api.teams.dto.response.TeamResponse;
import com.sealhackathon.api.teams.service.TeamService;
import com.sealhackathon.api.teams.entity.TeamMember;
import com.sealhackathon.api.teams.repository.TeamMemberRepository;
import com.sealhackathon.api.teams.value_object.TeamMemberStatus;
import com.sealhackathon.api.teams.entity.TeamRoundParticipation;
import com.sealhackathon.api.teams.repository.TeamRoundParticipationRepository;
import com.sealhackathon.api.teams.entity.TeamRoundTrack;
import com.sealhackathon.api.teams.repository.TeamRoundTrackRepository;
import com.sealhackathon.api.teams.support.PrelimMutationGuard;
import com.sealhackathon.api.teams.value_object.ParticipationStatus;
import com.sealhackathon.api.teams.value_object.RegistrationType;
import com.sealhackathon.api.teams.entity.Team;
import com.sealhackathon.api.teams.repository.TeamRepository;
import com.sealhackathon.api.teams.value_object.TeamStatus;
import com.sealhackathon.api.tracks.dto.response.TrackSummaryResponse;
import com.sealhackathon.api.tracks.entity.Track;
import com.sealhackathon.api.tracks.mapper.TrackMapper;
import com.sealhackathon.api.tracks.repository.TrackRepository;
import com.sealhackathon.api.tracks.support.TrackProblemStatementStorage;
import com.sealhackathon.api.users.entity.User;
import com.sealhackathon.api.prizes.repository.PrizeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.MalformedURLException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentPortalServiceImpl implements StudentPortalService {

    private final CurrentUserAccessor currentUserAccessor;
    private final StudentAccessGuard studentAccessGuard;
    private final HackathonRepository hackathonRepository;
    private final HackathonRegistrationRepository hackathonRegistrationRepository;
    private final HackathonRegistrationWithdrawalRepository hackathonRegistrationWithdrawalRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamRepository teamRepository;
    private final TeamRoundTrackRepository teamRoundTrackRepository;
    private final TeamRoundParticipationRepository teamRoundParticipationRepository;
    private final TrackRepository trackRepository;
    private final TrackMapper trackMapper;
    private final IndividualRankingRepository individualRankingRepository;
    private final RoundRepository roundRepository;
    private final SubmissionRepository submissionRepository;
    private final PresentationSlotRepository presentationSlotRepository;
    private final PresentationDurationResolver presentationDurationResolver;
    private final RoundRankingQueryService roundRankingQueryService;
    private final RoundProgressionService roundProgressionService;
    private final RoundPresentationReadiness presentationReadiness;
    private final PrizeRepository prizeRepository;
    private final AuditService auditService;
    private final RoundProblemStatementStorage roundProblemStatementStorage;
    private final TrackProblemStatementStorage trackProblemStatementStorage;
    private final PrelimMutationGuard prelimMutationGuard;
    private final TeamService teamService;

    private static String studentProblemDownloadPath(Integer roundId) {
        return "/api/v1/me/rounds/" + roundId + "/problem-statement";
    }

    // =================================================================================
    // CÁC API NHÓM READ-ONLY (XEM DỮ LIỆU)
    // =================================================================================

    @Override
    public List<StudentHackathonBrowseItemResponse> browseHackathons(String status) {
        Integer userId = currentUserAccessor.currentUserId();
        List<Hackathon> hackathons = hackathonRepository.findAll();
        boolean hasOngoingRegistration = hackathonRegistrationRepository
                .existsByUser_IdAndHackathon_Status(userId, HackathonStatus.ONGOING);

        return hackathons.stream()
                .filter(h -> status == null || h.getStatus().name().equalsIgnoreCase(status))
                .map(h -> {
                    boolean isRegistered = hackathonRegistrationRepository
                            .existsByHackathon_IdAndUser_Id(h.getId(), userId);
                    boolean registrationWithdrawn = hackathonRegistrationWithdrawalRepository
                            .existsByHackathon_IdAndUser_Id(h.getId(), userId);
                    boolean registeredElsewhere = !isRegistered && hasOngoingRegistration;
                    boolean notYetOpen = HackathonRegistrationSupport.isRegistrationNotYetOpen(h);
                    boolean windowOpen = HackathonRegistrationSupport.isRegistrationWindowOpen(h);
                    return StudentHackathonBrowseItemResponse.builder()
                            .id(h.getId())
                            .name(h.getName())
                            .status(h.getStatus().name())
                            .registered(isRegistered)
                            .registrationWithdrawn(registrationWithdrawn)
                            .registeredElsewhere(registeredElsewhere)
                            .description(h.getDescription())
                            .bannerUrl(h.getBannerUrl())
                            .season(h.getSeason() == null ? null : h.getSeason().name())
                            .year(h.getYear())
                            .registrationStart(h.getRegistrationStart())
                            .registrationEnd(h.getRegistrationEnd())
                            .eventStart(h.getEventStart())
                            .eventEnd(h.getEventEnd())
                            .maxParticipants(h.getMaxParticipants())
                            .registrationNotYetOpen(notYetOpen)
                            .registrationWindowOpen(windowOpen)
                            .build();
                })
                .sorted((a, b) -> {
                    // Đã ĐK trước → đang mở ĐK → registrationStart asc → tên
                    int regCmp = Boolean.compare(Boolean.TRUE.equals(b.getRegistered()),
                            Boolean.TRUE.equals(a.getRegistered()));
                    if (regCmp != 0) return regCmp;
                    int openCmp = Boolean.compare(Boolean.TRUE.equals(b.getRegistrationWindowOpen()),
                            Boolean.TRUE.equals(a.getRegistrationWindowOpen()));
                    if (openCmp != 0) return openCmp;
                    java.time.LocalDateTime as = a.getRegistrationStart();
                    java.time.LocalDateTime bs = b.getRegistrationStart();
                    if (as != null && bs != null) {
                        int startCmp = as.compareTo(bs);
                        if (startCmp != 0) return startCmp;
                    } else if (as != null) {
                        return -1;
                    } else if (bs != null) {
                        return 1;
                    }
                    String an = a.getName() == null ? "" : a.getName();
                    String bn = b.getName() == null ? "" : b.getName();
                    return an.compareToIgnoreCase(bn);
                })
                .toList();
    }

    @Override
    @Transactional
    public TeamResponse createTeam(CreateTeamRequest request) {
        return teamService.createTeam(request);
    }

    @Override
    public List<MeTeamSummaryResponse> listMyTeams(boolean includeEliminated) {
        Integer userId = currentUserAccessor.currentUserId();

        // Mặc định PENDING/ACTIVE; includeEliminated thêm ELIMINATED (trang kết quả)
        List<TeamMember> myMemberships = includeEliminated
                ? teamMemberRepository.findMembershipsIncludingEliminatedByUserId(userId)
                : teamMemberRepository.findActiveMembershipsByUserId(userId);

        return myMemberships.stream().map(tm -> {
            com.sealhackathon.api.teams.entity.Team team = tm.getTeam();

            // Tìm Track Sơ loại (nếu đội đã bốc thăm/chọn bảng)
            var trtOpt = teamRoundTrackRepository.findByTeam_Id(team.getId()).stream()
                    .filter(trt -> !trt.getTrack().getRound().getIsFinal()) // Chỉ lấy Vòng Sơ Loại/Bán Kết
                    .findFirst();

            return MeTeamSummaryResponse.builder()
                    .teamId(team.getId())
                    .teamName(team.getTeamName())
                    .hackathonId(team.getHackathon().getId())
                    .leaderId(team.getLeader().getId())
                    .status(team.getStatus().name())
                    .trackId(trtOpt.map(trt -> trt.getTrack().getId()).orElse(null))
                    .trackName(trtOpt.map(trt -> trt.getTrack().getName()).orElse(null))
                    .lotteryStatus(trtOpt.map(trt -> trt.getParticipationStatus().name()).orElse("PENDING"))
                    .build();
        }).toList();
    }

    @Override
    public StudentProblemResponse getRoundProblem(Integer roundId) {
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new ResourceNotFoundException("Round", roundId));

        if (Boolean.TRUE.equals(round.getIsFinal())) {
            if (round.getProblemReleasedAt() == null) {
                throw new BusinessRuleException(ErrorCode.RESOURCE_NOT_FOUND,
                        "Đề bài vòng thi này chưa được Ban Tổ Chức công bố.");
            }
            FinalPrelimTrackResolve resolved = resolveStudentPrelimTrackForFinal(round);
            if (!resolved.eligible()) {
                throw new BusinessRuleException(ErrorCode.FORBIDDEN,
                        "Đội của bạn không đủ điều kiện xem đề Chung kết.");
            }
            Track track = resolved.track();
            if (track == null || !TrackProblemStatementStorage.hasProblemFile(track)) {
                return StudentProblemResponse.builder()
                        .roundId(roundId)
                        .released(true)
                        .available(false)
                        .trackId(track != null ? track.getId() : null)
                        .trackName(track != null ? track.getName() : null)
                        .build();
            }
            String studentDownloadPath = studentProblemDownloadPath(roundId);
            String filename = TrackProblemStatementStorage.displayFilename(track);
            return StudentProblemResponse.builder()
                    .roundId(roundId)
                    .problemStatement(filename)
                    .problemUrl(studentDownloadPath)
                    .problemDownloadPath(studentDownloadPath)
                    .problemFilename(filename)
                    .released(true)
                    .available(true)
                    .trackId(track.getId())
                    .trackName(track.getName())
                    .build();
        }

        Track track = resolveStudentTrackForRound(roundId);
        assertPrelimProblemReleased(round, track);

        String studentDownloadPath = studentProblemDownloadPath(roundId);
        String filename = TrackProblemStatementStorage.displayFilename(track);
        return StudentProblemResponse.builder()
                .roundId(roundId)
                .problemStatement(filename)
                .problemUrl(studentDownloadPath)
                .problemDownloadPath(studentDownloadPath)
                .problemFilename(filename)
                .released(true)
                .available(true)
                .build();
    }

    @Override
    public Resource downloadRoundProblemStatement(Integer roundId) {
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new ResourceNotFoundException("Round", roundId));
        if (Boolean.TRUE.equals(round.getIsFinal())) {
            if (round.getProblemReleasedAt() == null) {
                throw new BusinessRuleException(ErrorCode.RESOURCE_NOT_FOUND,
                        "Đề bài vòng thi này chưa được Ban Tổ Chức công bố.");
            }
            FinalPrelimTrackResolve resolved = resolveStudentPrelimTrackForFinal(round);
            if (!resolved.eligible()) {
                throw new BusinessRuleException(ErrorCode.FORBIDDEN,
                        "Đội của bạn không đủ điều kiện xem đề Chung kết.");
            }
            Track track = resolved.track();
            if (track == null || !TrackProblemStatementStorage.hasProblemFile(track)) {
                throw new BusinessRuleException(ErrorCode.RESOURCE_NOT_FOUND,
                        "Không tìm thấy đề — liên hệ Coordinator.");
            }
            return resolveTrackProblemResource(track);
        }
        Track track = resolveStudentTrackForRound(roundId);
        assertPrelimProblemReleased(round, track);
        return resolveTrackProblemResource(track);
    }

    private void assertPrelimProblemReleased(Round round, Track track) {
        if (track.getProblemReleasedAt() != null || round.getProblemReleasedAt() != null) {
            return;
        }
        throw new BusinessRuleException(ErrorCode.RESOURCE_NOT_FOUND,
                "Đề bài cho bảng đấu của bạn chưa được Ban Tổ Chức công bố.");
    }

    private boolean isPrelimProblemReleased(Round round, Track track) {
        if (track != null && track.getProblemReleasedAt() != null) {
            return true;
        }
        return round.getProblemReleasedAt() != null;
    }

    private Resource resolveRoundProblemResource(Round round) {
        if (RoundProblemStatementStorage.hasStoredFile(round)) {
            String filename = RoundProblemStatementStorage.displayFilename(round);
            return StoredObjectResource.toResource(roundProblemStatementStorage.load(round), filename);
        }
        if (StringUtils.hasText(round.getProblemStatementUrl())) {
            return loadExternalProblemResource(round.getProblemStatementUrl(), "Vòng thi chưa có file đề bài");
        }
        throw new BusinessRuleException(ErrorCode.RESOURCE_NOT_FOUND,
                "Vòng Chung kết chưa có file đề bài — Coordinator cần upload PDF trước khi phát đề.");
    }

    private Resource resolveTrackProblemResource(Track track) {
        if (TrackProblemStatementStorage.hasStoredFile(track)) {
            String filename = TrackProblemStatementStorage.displayFilename(track);
            return StoredObjectResource.toResource(trackProblemStatementStorage.load(track), filename);
        }
        if (StringUtils.hasText(track.getProblemStatementUrl())) {
            return loadExternalProblemResource(track.getProblemStatementUrl(), "Bảng đấu chưa có file đề bài");
        }
        throw new BusinessRuleException(ErrorCode.RESOURCE_NOT_FOUND,
                "Bảng đấu \"" + track.getName() + "\" chưa có file đề bài — Coordinator cần upload PDF cho bảng này trước khi phát đề.");
    }

    private Resource loadExternalProblemResource(String url, String missingMessage) {
        if (!StringUtils.hasText(url)) {
            throw new BusinessRuleException(ErrorCode.RESOURCE_NOT_FOUND, missingMessage);
        }
        try {
            return new UrlResource(url);
        } catch (MalformedURLException ex) {
            throw new BusinessRuleException(ErrorCode.RESOURCE_NOT_FOUND, "URL đề bài không hợp lệ");
        }
    }

    private Track resolveStudentTrackForRound(Integer roundId) {
        Integer userId = currentUserAccessor.currentUserId();
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new ResourceNotFoundException("Round", roundId));
        List<TeamMember> memberships = teamMemberRepository.findByUser_IdAndStatus(userId, TeamMemberStatus.ACCEPTED);
        for (TeamMember tm : memberships) {
            Team team = tm.getTeam();
            if (!team.getHackathon().getId().equals(round.getHackathon().getId())) {
                continue;
            }
            Optional<TeamRoundTrack> trt =
                    teamRoundTrackRepository.findByTeam_IdAndTrack_Round_Id(team.getId(), roundId);
            if (trt.isPresent()) {
                return trt.get().getTrack();
            }
        }
        throw new BusinessRuleException(ErrorCode.RESOURCE_NOT_FOUND,
                "Đội của bạn chưa được phân bảng đấu cho vòng này.");
    }

    /**
     * CK: đội phải có TeamRoundParticipation; PDF = đề track sơ loại (TeamRoundTrack ADVANCED ưu tiên).
     */
    private FinalPrelimTrackResolve resolveStudentPrelimTrackForFinal(Round finalRound) {
        Integer userId = currentUserAccessor.currentUserId();
        Integer hackathonId = finalRound.getHackathon().getId();
        Optional<Round> prelimOpt = roundRepository.findByHackathon_IdOrderByExamAtAsc(hackathonId).stream()
                .filter(r -> !Boolean.TRUE.equals(r.getIsFinal()))
                .findFirst();

        List<TeamMember> memberships = teamMemberRepository.findByUser_IdAndStatus(userId, TeamMemberStatus.ACCEPTED);
        for (TeamMember tm : memberships) {
            Team team = tm.getTeam();
            if (!team.getHackathon().getId().equals(hackathonId)) {
                continue;
            }
            boolean inFinal = teamRoundParticipationRepository
                    .findByTeam_IdAndRound_Id(team.getId(), finalRound.getId())
                    .isPresent();
            if (!inFinal) {
                continue;
            }
            if (prelimOpt.isEmpty()) {
                return FinalPrelimTrackResolve.eligibleMissingTrack();
            }
            Round prelim = prelimOpt.get();
            Optional<TeamRoundTrack> advanced = teamRoundTrackRepository
                    .findByTeam_IdAndTrack_Round_Id(team.getId(), prelim.getId())
                    .filter(trt -> trt.getParticipationStatus() == ParticipationStatus.ADVANCED
                            || trt.getParticipationStatus() == ParticipationStatus.PARTICIPATING);
            if (advanced.isPresent()) {
                return FinalPrelimTrackResolve.ok(advanced.get().getTrack());
            }
            Optional<TeamRoundTrack> any = teamRoundTrackRepository
                    .findByTeam_IdAndTrack_Round_Id(team.getId(), prelim.getId());
            if (any.isPresent()) {
                return FinalPrelimTrackResolve.ok(any.get().getTrack());
            }
            return FinalPrelimTrackResolve.eligibleMissingTrack();
        }
        return FinalPrelimTrackResolve.notEligible();
    }

    private record FinalPrelimTrackResolve(boolean eligible, Track track) {
        static FinalPrelimTrackResolve notEligible() {
            return new FinalPrelimTrackResolve(false, null);
        }

        static FinalPrelimTrackResolve eligibleMissingTrack() {
            return new FinalPrelimTrackResolve(true, null);
        }

        static FinalPrelimTrackResolve ok(Track track) {
            return new FinalPrelimTrackResolve(true, track);
        }
    }

    // =================================================================================
    // API SUBMISSION MỚI (Merge từ Phát + Guard của Huy)
    // =================================================================================

    @Override
    public List<StudentSubmissionStatusResponse> listTeamSubmissions(Integer teamId, Integer roundId) {
        studentAccessGuard.assertTeamMember(teamId);
        return findSubmissions(teamId, roundId).stream()
                .map(this::toSubmissionStatus)
                .toList();
    }

    @Override
    public StudentSubmissionStatusResponse getLatestSubmission(Integer teamId, Integer roundId) {
        studentAccessGuard.assertTeamMember(teamId);
        List<Submission> submissions = findSubmissions(teamId, roundId);
        if (submissions.isEmpty()) {
            // GĐ3/GĐ5: chưa nộp → 200 + null (không 404 spam portal)
            return null;
        }
        Submission latest = submissions.stream()
                .max(Comparator.comparing(Submission::getSubmittedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElseThrow();
        return toSubmissionStatus(latest);
    }

    @Override
    public StudentTeamScoreBreakdownResponse getTeamScoreBreakdown(Integer teamId, Integer roundId) {
        studentAccessGuard.assertTeamMember(teamId);
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new ResourceNotFoundException("Round", roundId));
        if (!RoundResultVisibility.visibleToParticipants(round, round.getHackathon())) {
            throw new BusinessRuleException(ErrorCode.RESULT_NOT_PUBLISHED,
                    "Kết quả vòng chưa được công bố — đội chưa thể xem điểm chi tiết",
                    Map.of("roundId", roundId));
        }
        List<Submission> submissions = findSubmissions(teamId, roundId);
        if (submissions.isEmpty()) {
            throw new ResourceNotFoundException("Submission", "teamId=" + teamId + ",roundId=" + roundId);
        }
        Submission submission = submissions.stream()
                .max(Comparator.comparing(Submission::getSubmittedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElseThrow();

        ScoreBreakdownResponse raw = roundProgressionService.scoreBreakdown(roundId, submission.getId());

        // Sort judges by judgeId for stable «Giám khảo 1/2/3»; never expose ids/names.
        List<ScoreBreakdownResponse.JudgeRow> sortedJudges = (raw.getJudges() == null ? List.<ScoreBreakdownResponse.JudgeRow>of() : raw.getJudges())
                .stream()
                .sorted(Comparator.comparing(ScoreBreakdownResponse.JudgeRow::getJudgeId,
                        Comparator.nullsLast(Integer::compareTo)))
                .toList();

        Map<Integer, Integer> judgeIdToOrdinal = new HashMap<>();
        List<StudentTeamScoreBreakdownResponse.AnonymousJudge> anonJudges = new ArrayList<>();
        for (int i = 0; i < sortedJudges.size(); i++) {
            int ordinal = i + 1;
            judgeIdToOrdinal.put(sortedJudges.get(i).getJudgeId(), ordinal);
            anonJudges.add(StudentTeamScoreBreakdownResponse.AnonymousJudge.builder()
                    .ordinal(ordinal)
                    .label("Giám khảo " + ordinal)
                    .build());
        }

        List<StudentTeamScoreBreakdownResponse.CriterionColumn> criteria = (raw.getCriteria() == null
                ? List.<ScoreBreakdownResponse.CriterionColumn>of()
                : raw.getCriteria()).stream()
                .map(c -> StudentTeamScoreBreakdownResponse.CriterionColumn.builder()
                        .criterionId(c.getCriterionId())
                        .name(c.getName())
                        .maxScore(c.getMaxScore())
                        .build())
                .toList();

        List<StudentTeamScoreBreakdownResponse.Cell> cells = (raw.getCells() == null
                ? List.<ScoreBreakdownResponse.Cell>of()
                : raw.getCells()).stream()
                .map(c -> StudentTeamScoreBreakdownResponse.Cell.builder()
                        .judgeOrdinal(judgeIdToOrdinal.getOrDefault(c.getJudgeId(), 0))
                        .criterionId(c.getCriterionId())
                        .scoreValue(c.getScoreValue())
                        .comment(c.getComment())
                        .build())
                .filter(c -> c.getJudgeOrdinal() > 0)
                .toList();

        List<StudentTeamScoreBreakdownResponse.CriterionAvg> avgs = (raw.getCriterionStats() == null
                ? List.<ScoreBreakdownResponse.CriterionStats>of()
                : raw.getCriterionStats()).stream()
                .map(s -> StudentTeamScoreBreakdownResponse.CriterionAvg.builder()
                        .criterionId(s.getCriterionId())
                        .average(s.getMean())
                        .build())
                .toList();

        Team team = teamRepository.findById(teamId).orElse(null);
        return StudentTeamScoreBreakdownResponse.builder()
                .roundId(roundId)
                .roundName(round.getName())
                .teamId(teamId)
                .teamName(team != null ? team.getTeamName() : (raw.getTeamName()))
                .submissionId(submission.getId())
                .criteria(criteria)
                .judges(anonJudges)
                .cells(cells)
                .criterionAverages(avgs)
                .teamAverage(raw.getOverallMean())
                .build();
    }

    @Override
    public StudentRoundDeadlineResponse getCurrentDeadline(Integer hackathonId) {
        Integer userId = currentUserAccessor.currentUserId();
        Round activeRound = findActivePrelimRoundForUserOrNull(userId, hackathonId);
        if (activeRound == null) {
            // GĐ2 / prelim chưa activate — không 404 để student portal không spam lỗi
            return StudentRoundDeadlineResponse.builder()
                    .problemReleased(false)
                    .build();
        }
        boolean problemReleased = activeRound.getProblemReleasedAt() != null;
        if (!problemReleased) {
            try {
                Track track = resolveStudentTrackForRound(activeRound.getId());
                problemReleased = isPrelimProblemReleased(activeRound, track);
            } catch (BusinessRuleException | ResourceNotFoundException ignored) {
                // Chưa phân bảng — coi như chưa phát đề
            }
        }
        return StudentRoundDeadlineResponse.builder()
                .roundId(activeRound.getId())
                .deadline(activeRound.getSubmissionDeadline())
                .problemReleased(problemReleased)
                .closedEarlyAt(activeRound.getSubmissionClosedEarlyAt())
                .presentationShuffled(presentationReadiness.isShuffled(activeRound))
                .build();
    }

    @Override
    public StudentFinalRoundResponse getFinalRoundForHackathon(Integer hackathonId) {
        Integer userId = currentUserAccessor.currentUserId();
        hackathonRepository.findById(hackathonId)
                .orElseThrow(() -> new ResourceNotFoundException("Hackathon", hackathonId));

        Team team = teamMemberRepository.findByUser_IdAndStatus(userId, TeamMemberStatus.ACCEPTED).stream()
                .map(TeamMember::getTeam)
                .filter(t -> Objects.equals(t.getHackathon().getId(), hackathonId))
                .filter(t -> t.getStatus() != TeamStatus.ELIMINATED)
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException(ErrorCode.FORBIDDEN,
                        "Bạn chưa tham gia đội trong hackathon này"));

        Round finalRound = roundRepository.findByHackathon_IdAndIsFinalTrue(hackathonId)
                .orElseThrow(() -> new BusinessRuleException(ErrorCode.INVALID_STATE, "Chưa có vòng Chung kết"));

        if (teamRoundParticipationRepository.findByTeam_IdAndRound_Id(team.getId(), finalRound.getId()).isEmpty()) {
            throw new BusinessRuleException(ErrorCode.FORBIDDEN,
                    "Đội chưa đủ điều kiện tham gia Vòng Chung kết");
        }

        return StudentFinalRoundResponse.builder()
                .roundId(finalRound.getId())
                .name(finalRound.getName())
                .isActive(finalRound.getIsActive())
                .scoringLocked(finalRound.getScoringLocked())
                .submissionDeadline(finalRound.getSubmissionDeadline())
                .problemReleased(finalRound.getProblemReleasedAt() != null)
                .build();
    }

    // =================================================================================
    // CÁC API VIEW
    // =================================================================================

    @Override
    public List<StudentLeaderboardItemResponse> getRoundLeaderboard(Integer roundId) {
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new ResourceNotFoundException("Round", roundId));

        Integer hackathonId = round.getHackathon() != null ? round.getHackathon().getId() : null;
        if (hackathonId != null) {
            studentAccessGuard.assertParticipatedInHackathon(hackathonId);
        }

        // Prelim: isPublished. Final: scoringLocked + PENDING_CONFIRM/FINISHED
        if (!RoundResultVisibility.visibleToParticipants(round, round.getHackathon())) {
            throw new BusinessRuleException(ErrorCode.RESULT_NOT_PUBLISHED,
                    "Kết quả vòng thi này chưa được công bố. Vui lòng quay lại sau.");
        }

        List<RoundRankingItemResponse> rankings =
                roundRankingQueryService.rankingForRound(roundId, false);

        // Prelim: filter to student's track / assigned group
        if (!Boolean.TRUE.equals(round.getIsFinal())) {
            Team team = resolveStudentTeamForRound(round);
            Optional<TeamRoundTrack> trtOpt =
                    teamRoundTrackRepository.findByTeam_IdAndTrack_Round_Id(team.getId(), roundId);
            if (trtOpt.isPresent()) {
                TeamRoundTrack trt = trtOpt.get();
                Integer trackId = trt.getTrack() != null ? trt.getTrack().getId() : null;
                String assignedGroup = trt.getAssignedGroup();
                String trackName = trt.getTrack() != null ? trt.getTrack().getName() : null;

                List<RoundRankingItemResponse> filtered = rankings.stream()
                        .filter(r -> Objects.equals(r.getTrackId(), trackId))
                        .filter(r -> assignedGroup == null
                                || Objects.equals(r.getAssignedGroup(), assignedGroup))
                        .toList();

                int totalInGroup = filtered.size();
                return filtered.stream().map(r -> StudentLeaderboardItemResponse.builder()
                        .rank(r.getRank())
                        .teamId(r.getTeamId())
                        .teamName(r.getTeamName())
                        .totalScore(toScaledScore(r.getTotalScore()))
                        .assignedGroup(r.getAssignedGroup() != null ? r.getAssignedGroup() : assignedGroup)
                        .trackId(trackId)
                        .trackName(trackName)
                        .rankInGroup(r.getRank())
                        .totalInGroup(totalInGroup)
                        .build()).toList();
            }
        }

        int total = rankings.size();
        return rankings.stream().map(r -> StudentLeaderboardItemResponse.builder()
                .rank(r.getRank())
                .teamId(r.getTeamId())
                .teamName(r.getTeamName())
                .totalScore(toScaledScore(r.getTotalScore()))
                .assignedGroup(r.getAssignedGroup())
                .trackId(r.getTrackId())
                .rankInGroup(r.getRank())
                .totalInGroup(total)
                .build()).toList();
    }

    @Override
    public StudentPresentationSlotResponse getPresentationSlot(Integer roundId) {
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new ResourceNotFoundException("Round", roundId));

        Team team = resolveStudentTeamForRound(round);
        studentAccessGuard.assertTeamMember(team.getId());

        Optional<PresentationSlot> mySlotOpt =
                presentationSlotRepository.findByRound_IdAndTeam_Id(roundId, team.getId());
        if (mySlotOpt.isEmpty()) {
            // Trước quay số — 200 + available=false (tránh FE spam 404)
            return StudentPresentationSlotResponse.builder()
                    .available(false)
                    .message("Chưa quay số")
                    .roundIsFinal(Boolean.TRUE.equals(round.getIsFinal()))
                    .build();
        }

        PresentationSlot mySlot = mySlotOpt.get();
        Integer myOrder = mySlot.getSequenceOrder();
        Integer trackId = mySlot.getTrack() != null ? mySlot.getTrack().getId() : null;

        List<PresentationSlot> peerSlots = trackId != null
                ? presentationSlotRepository.findByRound_IdAndTrack_IdOrderBySequenceOrderAsc(roundId, trackId)
                : presentationSlotRepository.findByRound_IdAndTrackIsNullOrderBySequenceOrderAsc(roundId);

        Optional<PresentationSlot> presenting = peerSlots.stream()
                .filter(s -> s.getQueueStatus() == PresentationQueueStatus.PRESENTING)
                .findFirst();

        // STT-06: chỉ đếm WAITING phía trước — SKIPPED/DONE/ELIMINATED/PRESENTING không tính
        int teamsAhead = 0;
        if (myOrder != null) {
            teamsAhead = (int) peerSlots.stream()
                    .filter(s -> s.getQueueStatus() == PresentationQueueStatus.WAITING)
                    .filter(s -> s.getSequenceOrder() != null && s.getSequenceOrder() < myOrder)
                    .count();
        }

        String timerPhase = mySlot.getTimerPhase() != null ? mySlot.getTimerPhase().name() : null;
        Integer remainingSeconds = null;
        if (mySlot.getQueueStatus() == PresentationQueueStatus.PRESENTING
                || (mySlot.getTimerPhase() != null
                    && mySlot.getTimerPhase() != PresentationTimerPhase.IDLE
                    && mySlot.getTimerPhase() != PresentationTimerPhase.SETUP
                    && mySlot.getTimerPhase() != PresentationTimerPhase.ENDED)) {
            remainingSeconds = PresentationTimerCalculator.remainingSeconds(
                    mySlot, mySlot.getTrack(), round, presentationDurationResolver);
        }

        return StudentPresentationSlotResponse.builder()
                .available(true)
                .order(myOrder)
                .displayCode(toDisplayCode(mySlot))
                .status(mySlot.getQueueStatus() != null
                        ? mySlot.getQueueStatus().name()
                        : PresentationQueueStatus.WAITING.name())
                .trackId(trackId)
                .roundIsFinal(Boolean.TRUE.equals(round.getIsFinal()))
                .currentPresentingOrder(presenting.map(PresentationSlot::getSequenceOrder).orElse(null))
                .currentPresentingDisplayCode(presenting.map(StudentPortalServiceImpl::toDisplayCode).orElse(null))
                .teamsAhead(teamsAhead)
                .timerPhase(timerPhase)
                .remainingSeconds(remainingSeconds)
                .build();
    }

    /**
     * Resolve the student's team that participates in this round (prelim track or final participation).
     */
    private Team resolveStudentTeamForRound(Round round) {
        Integer userId = currentUserAccessor.currentUserId();
        Integer hackathonId = round.getHackathon().getId();
        boolean isFinal = Boolean.TRUE.equals(round.getIsFinal());

        List<TeamMember> memberships =
                teamMemberRepository.findByUser_IdAndStatus(userId, TeamMemberStatus.ACCEPTED);
        for (TeamMember tm : memberships) {
            Team team = tm.getTeam();
            if (!Objects.equals(team.getHackathon().getId(), hackathonId)) {
                continue;
            }
            if (isFinal) {
                if (teamRoundParticipationRepository
                        .findByTeam_IdAndRound_Id(team.getId(), round.getId())
                        .isPresent()) {
                    return team;
                }
            } else if (teamRoundTrackRepository
                    .findByTeam_IdAndTrack_Round_Id(team.getId(), round.getId())
                    .isPresent()) {
                return team;
            }
        }
        throw new BusinessRuleException(ErrorCode.FORBIDDEN,
                "Đội của bạn không tham gia vòng thi này.");
    }

    private static BigDecimal toScaledScore(Double score) {
        if (score == null) {
            return null;
        }
        return BigDecimal.valueOf(ScoreScale.round2(score)).setScale(2, RoundingMode.HALF_UP);
    }

    private static String toDisplayCode(PresentationSlot slot) {
        if (slot.getSubmission() != null && slot.getSubmission().getId() != null) {
            return "#" + slot.getSubmission().getId();
        }
        if (slot.getSequenceOrder() != null) {
            return "#" + slot.getSequenceOrder();
        }
        return null;
    }

    @Override
    public StudentRankingResponse getHackathonRankings(Integer hackathonId) {
        Hackathon hackathon = hackathonRepository.findById(hackathonId)
                .orElseThrow(() -> new ResourceNotFoundException("Hackathon", hackathonId));

        studentAccessGuard.assertParticipatedInHackathon(hackathonId);

        if (hackathon.getStatus() != HackathonStatus.FINISHED && hackathon.getStatus() != HackathonStatus.PENDING_CONFIRM) {
            throw new BusinessRuleException("RESULT_NOT_AVAILABLE",
                    "Bảng xếp hạng chung cuộc chưa sẵn sàng.");
        }

        Round finalRound = roundRepository.findByHackathon_IdAndIsFinalTrue(hackathonId)
                .orElseThrow(() -> new BusinessRuleException(ErrorCode.INVALID_STATE, "Chưa có vòng chung kết"));

        var rankings = roundRankingQueryService.rankingForRound(finalRound.getId(), false);
        List<StudentLeaderboardItemResponse> items = rankings.stream()
                .map(r -> StudentLeaderboardItemResponse.builder()
                        .rank(r.getRank())
                        .teamId(r.getTeamId())
                        .teamName(r.getTeamName())
                        .totalScore(toScaledScore(r.getTotalScore()))
                        .build()).toList();

        return StudentRankingResponse.builder()
                .hackathonId(hackathonId)
                .items(items)
                .build();
    }

    @Override
    public List<StudentPrizeResponse> listMyPrizes() {
        Integer userId = currentUserAccessor.currentUserId();

        List<Integer> myTeamIds = teamMemberRepository.findByUser_IdAndStatus(userId, TeamMemberStatus.ACCEPTED)
                .stream().map(tm -> tm.getTeam().getId()).toList();

        if (myTeamIds.isEmpty()) return Collections.emptyList();

        return prizeRepository.findByTeam_IdIn(myTeamIds).stream()
                .map(p -> StudentPrizeResponse.builder()
                        .prizeId(p.getId())
                        .hackathonId(p.getHackathon().getId())
                        .prizeName(p.getPrizeName())
                        .rank(p.getPrizeRank() != null ? p.getPrizeRank().ordinal() + 1 : null)
                        .build())
                .toList();
    }

    @Override
    public StudentHistoryResponse getHistory() {
        Integer userId = currentUserAccessor.currentUserId();
        List<TeamMember> myMemberships = teamMemberRepository.findMembershipsIncludingEliminatedByUserId(userId);

        List<StudentHistoryResponse.StudentHistoryHackathonItem> items = myMemberships.stream()
                .filter(tm -> {
                    HackathonStatus status = tm.getTeam().getHackathon().getStatus();
                    return status == HackathonStatus.FINISHED
                            || status == HackathonStatus.ONGOING
                            || status == HackathonStatus.PENDING_CONFIRM;
                })
                .map(tm -> StudentHistoryResponse.StudentHistoryHackathonItem.builder()
                        .hackathonId(tm.getTeam().getHackathon().getId())
                        .name(tm.getTeam().getHackathon().getName())
                        .role(tm.getRoleInTeam().name())
                        .outcome(tm.getTeam().getStatus().name())
                        .build())
                .toList();

        return StudentHistoryResponse.builder().hackathons(items).build();
    }

    @Override
    public List<AnnualAwardResponse> getAnnualAwards(Integer year) {
        Integer userId = currentUserAccessor.currentUserId();
        int targetYear = year != null ? year : Year.now().getValue();

        return individualRankingRepository.findFallAwardsForUser(userId, targetYear).stream()
                .map(this::toAnnualAward)
                .toList();
    }

    // =================================================================================
    // CÁC API NHÓM MUTATION (GHI DỮ LIỆU / THAO TÁC)
    // =================================================================================

    @Override
    @Transactional
    public void relotteryTrack(Integer teamId, Integer roundId, RelotteryTrackRequest request) {
        studentAccessGuard.assertTeamLeader(teamId);

        var trt = teamRoundTrackRepository.findByTeam_IdAndTrack_Round_Id(teamId, roundId)
                .orElseThrow(() -> new BusinessRuleException(ErrorCode.TEAM_NOT_IN_ROUND, "Đội chưa được phân bảng Sơ loại."));
        prelimMutationGuard.assertPrelimMutable(trt);

        com.sealhackathon.api.rounds.entity.Round round = trt.getTrack().getRound();

        if (Boolean.TRUE.equals(round.getIsActive())) {
            throw new BusinessRuleException("ROUND_ALREADY_ACTIVE", "Vòng thi đã bắt đầu. Không thể đổi Track nữa.");
        }

        var newTrack = trackRepository.findById(request.getTrackId())
                .orElseThrow(() -> new ResourceNotFoundException("Track", request.getTrackId()));

        if (!Objects.equals(newTrack.getRound().getId(), roundId)) {
            throw new BusinessRuleException(ErrorCode.INVALID_STATE,
                    "Track không thuộc vòng Sơ loại đang chọn.",
                    Map.of("trackId", newTrack.getId(), "roundId", roundId));
        }
        if (!Objects.equals(newTrack.getRound().getHackathon().getId(), trt.getTeam().getHackathon().getId())) {
            throw new BusinessRuleException(ErrorCode.INVALID_STATE,
                    "Track không thuộc cùng Hackathon với đội.",
                    Map.of("trackId", newTrack.getId(), "hackathonId", trt.getTeam().getHackathon().getId()));
        }
        if (newTrack.getStatus() == com.sealhackathon.api.tracks.value_object.TrackStatus.CANCELLED) {
            throw new BusinessRuleException(ErrorCode.INVALID_STATE,
                    "Không thể chọn Track đã hủy.",
                    Map.of("trackId", newTrack.getId()));
        }

        Integer oldTrackId = trt.getTrack().getId();
        trt.setTrack(newTrack);
        teamRoundTrackRepository.save(trt);

        auditService.log(AuditAction.TEAM_TRACK_CHANGED, "team_round_tracks", trt.getId(),
                Map.of("teamId", teamId, "roundId", roundId,
                        "fromTrackId", oldTrackId, "toTrackId", newTrack.getId()));
    }

    @Override
    @Transactional
    public void selectFallTrack(Integer trackId) {
        Integer userId = currentUserAccessor.currentUserId();
        FallTrackSelectContext ctx = resolveFallTrackSelectForLeader(userId);

        Track track = trackRepository.findById(trackId)
                .orElseThrow(() -> new ResourceNotFoundException("Track", trackId));

        if (track.getRound() == null || !Objects.equals(track.getRound().getId(), ctx.prelim().getId())) {
            throw new BusinessRuleException(ErrorCode.INVALID_STATE,
                    "Track không thuộc vòng Sơ loại của kỳ Hackathon này.");
        }

        persistFallTrackSelection(ctx.team(), ctx.prelim(), ctx.hackathon(), track, userId);
    }

    @Override
    public List<TrackSummaryResponse> listSelectableFallTracks(Integer hackathonId) {
        Integer userId = currentUserAccessor.currentUserId();
        FallTrackSelectContext ctx = resolveFallTrackSelectForHackathon(userId, hackathonId);
        return trackRepository.findByRoundIdOrderBySequenceOrderAsc(ctx.prelim().getId()).stream()
                .map(trackMapper::toSummary)
                .toList();
    }

    private record FallTrackSelectContext(Team team, Hackathon hackathon, Round prelim) {}

    private FallTrackSelectContext resolveFallTrackSelectForLeader(Integer userId) {
        Team team = teamRepository.findByLeader_Id(userId).stream()
                .filter(t -> t.getStatus() == TeamStatus.ACTIVE)
                .filter(t -> t.getHackathon().getStatus() == HackathonStatus.ONGOING)
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException(ErrorCode.FORBIDDEN,
                        "Không tìm thấy đội ACTIVE đang tham gia kỳ Hackathon."));
        return validateFallTrackSelectContext(team);
    }

    private FallTrackSelectContext resolveFallTrackSelectForHackathon(Integer userId, Integer hackathonId) {
        Team team = teamRepository.findByLeader_Id(userId).stream()
                .filter(t -> Objects.equals(t.getHackathon().getId(), hackathonId))
                .filter(t -> t.getStatus() == TeamStatus.ACTIVE)
                .filter(t -> t.getHackathon().getStatus() == HackathonStatus.ONGOING)
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException(ErrorCode.FORBIDDEN,
                        "Không tìm thấy đội ACTIVE đang tham gia kỳ Hackathon."));
        return validateFallTrackSelectContext(team);
    }

    private FallTrackSelectContext validateFallTrackSelectContext(Team team) {
        studentAccessGuard.assertTeamLeader(team.getId());

        Hackathon hackathon = team.getHackathon();
        if (hackathon.getSeason() != Season.Fall) {
            throw new BusinessRuleException("NOT_APPLICABLE",
                    "Tính năng tự chọn Track chỉ áp dụng cho mùa Fall.");
        }

        if (HackathonRegistrationSupport.isRegistrationClosed(hackathon)) {
            throw new BusinessRuleException(ErrorCode.INVALID_STATE,
                    "Thời gian đăng ký đã kết thúc. Không thể chọn Track.");
        }

        Round prelim = roundRepository.findByHackathon_IdOrderByExamAtAsc(hackathon.getId()).stream()
                .filter(r -> !Boolean.TRUE.equals(r.getIsFinal()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Round", "prelim"));

        if (Boolean.TRUE.equals(prelim.getIsActive())) {
            throw new BusinessRuleException("ROUND_ALREADY_ACTIVE",
                    "Vòng thi đã bắt đầu. Không thể chọn Track nữa.");
        }

        return new FallTrackSelectContext(team, hackathon, prelim);
    }

    private void persistFallTrackSelection(Team team, Round prelim, Hackathon hackathon, Track track, Integer userId) {
        User assigner = User.builder().id(userId).build();
        LocalDateTime now = LocalDateTime.now();

        Optional<TeamRoundTrack> existing = teamRoundTrackRepository
                .findByTeam_IdAndTrack_Round_Id(team.getId(), prelim.getId());

        TeamRoundTrack trt;
        if (existing.isPresent()) {
            trt = existing.get();
            prelimMutationGuard.assertPrelimMutable(trt);
            trt.setTrack(track);
            trt.setRegistrationType(RegistrationType.PREFERRED);
            trt.setAssignedAt(now);
            trt.setAssignedBy(assigner);
        } else {
            if (teamRoundParticipationRepository.findByTeam_IdAndRound_Id(team.getId(), prelim.getId()).isEmpty()) {
                teamRoundParticipationRepository.save(TeamRoundParticipation.builder()
                        .team(team)
                        .round(prelim)
                        .hackathon(hackathon)
                        .createdAt(now)
                        .build());
            }
            trt = TeamRoundTrack.builder()
                    .team(team)
                    .track(track)
                    .registrationType(RegistrationType.PREFERRED)
                    .assignedAt(now)
                    .assignedBy(assigner)
                    .build();
        }

        teamRoundTrackRepository.save(trt);

        auditService.log(AuditAction.TEAM_TRACK_ASSIGNED, "team_round_tracks", trt.getId(),
                Map.of("teamId", team.getId(), "trackId", track.getId(), "roundId", prelim.getId(),
                        "registrationType", RegistrationType.PREFERRED.name()));
    }

    // =================================================================================
    // HELPER METHODS
    // =================================================================================

    private List<Submission> findSubmissions(Integer teamId, Integer roundId) {
        if (roundId != null) {
            return merge(
                    submissionRepository.findByTeam_IdAndRound_Id(teamId, roundId),
                    submissionRepository.findByTeam_IdAndTrack_Round_Id(teamId, roundId));
        }
        return submissionRepository.findByTeam_Id(teamId);
    }

    private Round findActivePrelimRoundForUser(Integer userId, Integer hackathonId) {
        Round round = findActivePrelimRoundForUserOrNull(userId, hackathonId);
        if (round == null) {
            throw new ResourceNotFoundException("Round", "active prelim");
        }
        return round;
    }

    private Round findActivePrelimRoundForUserOrNull(Integer userId, Integer hackathonId) {
        return teamMemberRepository.findByUser_IdAndStatus(userId, TeamMemberStatus.ACCEPTED).stream()
                .map(TeamMember::getTeam)
                .filter(t -> t.getStatus() == TeamStatus.ACTIVE)
                .filter(t -> t.getHackathon().getStatus() == HackathonStatus.ONGOING)
                .filter(t -> hackathonId == null || Objects.equals(t.getHackathon().getId(), hackathonId))
                .map(Team::getHackathon)
                .filter(Objects::nonNull)
                .distinct()
                .flatMap(h -> roundRepository.findByHackathon_IdOrderByExamAtAsc(h.getId()).stream())
                .filter(r -> Boolean.TRUE.equals(r.getIsActive()))
                .filter(r -> !Boolean.TRUE.equals(r.getIsFinal()))
                .findFirst()
                .orElse(null);
    }

    private static List<Submission> merge(List<Submission> a, List<Submission> b) {
        return java.util.stream.Stream.concat(a.stream(), b.stream())
                .distinct()
                .toList();
    }

    private AnnualAwardResponse toAnnualAward(IndividualRanking ranking) {
        Hackathon hackathon = ranking.getHackathon();
        Integer rank = ranking.getRank();
        return AnnualAwardResponse.builder()
                .hackathonId(hackathon.getId())
                .hackathonName(hackathon.getName())
                .year(hackathon.getYear())
                .rank(rank)
                .awardName(deriveAwardName(rank))
                .category("INDIVIDUAL")
                .build();
    }

    private static String deriveAwardName(Integer rank) {
        if (rank == null) {
            return "Fall Individual Award";
        }
        return switch (rank) {
            case 1 -> "Best Innovator";
            case 2 -> "Outstanding Performer";
            case 3 -> "Rising Star";
            default -> "Top " + rank + " Fall";
        };
    }

    private StudentSubmissionStatusResponse toSubmissionStatus(Submission s) {
        boolean hasSlide = StringUtils.hasText(s.getSlideStorageKey());
        String slideFile = SubmissionSlideStorage.displayFilename(s);
        String slideDownloadPath = hasSlide && s.getId() != null
                ? "/api/v1/submissions/" + s.getId() + "/slide"
                : null;
        return StudentSubmissionStatusResponse.builder()
                .submissionId(s.getId())
                .roundId(s.getRound() != null ? s.getRound().getId()
                        : (s.getTrack() != null && s.getTrack().getRound() != null
                        ? s.getTrack().getRound().getId() : null))
                .repoUrl(s.getRepoUrl())
                .demoUrl(s.getDemoUrl())
                .slideUrl(s.getSlideUrl())
                .slideFile(slideFile)
                .slideDownloadPath(slideDownloadPath)
                .hasSlide(hasSlide)
                .status(mapStatusForFe(s.getStatus(), hasSlide))
                .submittedAt(s.getSubmittedAt())
                .build();
    }

    static String mapStatusForFe(SubmissionStatus status, boolean hasSlide) {
        if (status == null) {
            return null;
        }
        if (!hasSlide && status != SubmissionStatus.REJECTED) {
            return "INCOMPLETE";
        }
        return switch (status) {
            case SUBMITTED, LATE, LATE_APPROVED, ACCEPTED -> "ON_TIME";
            case LATE_PENDING -> "LATE_PENDING";
            case REJECTED -> "REJECTED";
        };
    }
}