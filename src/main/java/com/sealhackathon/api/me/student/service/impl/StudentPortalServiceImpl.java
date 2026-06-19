package com.sealhackathon.api.me.student.service.impl;

import com.sealhackathon.api.appeals.service.AppealService;
import com.sealhackathon.api.certificates.repository.CertificateRepository;
import com.sealhackathon.api.certificates.support.CertificateFileResolver;
import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.common.exception.ResourceNotFoundException;
import com.sealhackathon.api.common.security.CurrentUserAccessor;
import com.sealhackathon.api.hackathon_registrations.repository.HackathonRegistrationRepository;
import com.sealhackathon.api.hackathon_registrations.repository.HackathonRegistrationWithdrawalRepository;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.me.student.dto.request.CreateAppealRequest;
import com.sealhackathon.api.me.student.dto.request.RelotteryTrackRequest;
import com.sealhackathon.api.me.student.dto.response.*;
import com.sealhackathon.api.me.student.service.StudentPortalService;
import com.sealhackathon.api.me.support.StudentAccessGuard;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.query.RoundRankingQueryService;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.rounds.support.RoundProblemStatementStorage;
import com.sealhackathon.api.storage.StoredObjectResource;
import com.sealhackathon.api.submissions.entity.Submission;
import com.sealhackathon.api.submissions.repository.SubmissionRepository;
import com.sealhackathon.api.submissions.support.SubmissionSlideStorage;
import com.sealhackathon.api.submissions.value_object.SubmissionStatus;
import com.sealhackathon.api.team_members.entity.TeamMember;
import com.sealhackathon.api.team_members.repository.TeamMemberRepository;
import com.sealhackathon.api.team_members.value_object.TeamMemberStatus;
import com.sealhackathon.api.teams.entity.Team;
import com.sealhackathon.api.teams.value_object.TeamStatus;
import com.sealhackathon.api.team_round_tracks.entity.TeamRoundTrack;
import com.sealhackathon.api.team_round_tracks.repository.TeamRoundTrackRepository;
import com.sealhackathon.api.tracks.entity.Track;
import com.sealhackathon.api.tracks.support.TrackProblemStatementStorage;
import com.sealhackathon.api.prizes.repository.PrizeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.MalformedURLException;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
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
    private final TeamRoundTrackRepository teamRoundTrackRepository;
    private final RoundRepository roundRepository;
    private final SubmissionRepository submissionRepository;
    private final RoundRankingQueryService roundRankingQueryService;
    private final PrizeRepository prizeRepository;
    private final CertificateRepository certificateRepository;
    private final CertificateFileResolver certificateFileResolver;
    private final AppealService appealService;
    private final RoundProblemStatementStorage roundProblemStatementStorage;
    private final TrackProblemStatementStorage trackProblemStatementStorage;

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
                    return StudentHackathonBrowseItemResponse.builder()
                            .id(h.getId())
                            .name(h.getName())
                            .status(h.getStatus().name())
                            .registered(isRegistered)
                            .registrationWithdrawn(registrationWithdrawn)
                            .registeredElsewhere(registeredElsewhere)
                            .build();
                })
                .toList();
    }

    @Override
    public List<MeTeamSummaryResponse> listMyTeams() {
        Integer userId = currentUserAccessor.currentUserId();

        // Lấy các đội mà sinh viên ĐÃ CHẤP NHẬN tham gia (ACCEPTED)
        List<TeamMember> myMemberships = teamMemberRepository.findByUser_IdAndStatus(userId, TeamMemberStatus.ACCEPTED);

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

        if (round.getProblemReleasedAt() == null) {
            throw new BusinessRuleException(ErrorCode.RESOURCE_NOT_FOUND,
                    "Đề bài vòng thi này chưa được Ban Tổ Chức công bố.");
        }

        String studentDownloadPath = studentProblemDownloadPath(roundId);
        if (Boolean.TRUE.equals(round.getIsFinal())) {
            String filename = RoundProblemStatementStorage.displayFilename(round);
            return StudentProblemResponse.builder()
                    .roundId(roundId)
                    .problemStatement(filename)
                    .problemUrl(studentDownloadPath)
                    .problemDownloadPath(studentDownloadPath)
                    .problemFilename(filename)
                    .released(true)
                    .build();
        }

        Track track = resolveStudentTrackForRound(roundId);
        String filename = TrackProblemStatementStorage.displayFilename(track);
        return StudentProblemResponse.builder()
                .roundId(roundId)
                .problemStatement(filename)
                .problemUrl(studentDownloadPath)
                .problemDownloadPath(studentDownloadPath)
                .problemFilename(filename)
                .released(true)
                .build();
    }

    @Override
    public Resource downloadRoundProblemStatement(Integer roundId) {
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new ResourceNotFoundException("Round", roundId));
        if (round.getProblemReleasedAt() == null) {
            throw new BusinessRuleException(ErrorCode.RESOURCE_NOT_FOUND,
                    "Đề bài vòng thi này chưa được Ban Tổ Chức công bố.");
        }
        if (Boolean.TRUE.equals(round.getIsFinal())) {
            return resolveRoundProblemResource(round);
        }
        Track track = resolveStudentTrackForRound(roundId);
        return resolveTrackProblemResource(track);
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
            throw new ResourceNotFoundException("Submission", "teamId=" + teamId);
        }
        Submission latest = submissions.stream()
                .max(Comparator.comparing(Submission::getSubmittedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElseThrow();
        return toSubmissionStatus(latest);
    }

    @Override
    public StudentRoundDeadlineResponse getCurrentDeadline(Integer hackathonId) {
        Integer userId = currentUserAccessor.currentUserId();
        Round activeRound = findActivePrelimRoundForUser(userId, hackathonId);
        return StudentRoundDeadlineResponse.builder()
                .roundId(activeRound.getId())
                .deadline(activeRound.getSubmissionDeadline())
                .problemReleased(activeRound.getProblemReleasedAt() != null)
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

        var trtOpt = teamRoundTrackRepository.findByTeam_Id(team.getId()).stream()
                .filter(trt -> !Boolean.TRUE.equals(trt.getTrack().getRound().getIsFinal()))
                .findFirst();
        String participation = trtOpt.map(trt -> trt.getParticipationStatus().name()).orElse("PENDING");
        if (!"ADVANCED".equalsIgnoreCase(participation)) {
            throw new BusinessRuleException(ErrorCode.FORBIDDEN,
                    "Đội chưa đủ điều kiện tham gia Vòng Chung kết");
        }

        Round finalRound = roundRepository.findByHackathon_IdAndIsFinalTrue(hackathonId)
                .orElseThrow(() -> new BusinessRuleException(ErrorCode.INVALID_STATE, "Chưa có vòng Chung kết"));

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

        // RÀO CHẮN: Xếp hạng chỉ xem được khi đã PUBLISHED (FR-U-21)
        if (!Boolean.TRUE.equals(round.getIsPublished())) {
            throw new BusinessRuleException(ErrorCode.RESULT_NOT_PUBLISHED,
                    "Kết quả vòng thi này chưa được công bố. Vui lòng quay lại sau.");
        }

        var rankings = roundRankingQueryService.rankingForRound(roundId, false);
        return rankings.stream().map(r -> StudentLeaderboardItemResponse.builder()
                .rank(r.getRank())
                .teamId(r.getTeamId())
                .teamName(r.getTeamName())
                .totalScore(BigDecimal.valueOf(r.getTotalScore()))
                .build()).toList();
    }

    @Override
    public StudentRankingResponse getHackathonRankings(Integer hackathonId) {
        Hackathon hackathon = hackathonRepository.findById(hackathonId)
                .orElseThrow(() -> new ResourceNotFoundException("Hackathon", hackathonId));

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
                        .totalScore(BigDecimal.valueOf(r.getTotalScore()))
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
    public List<CertificateResponse> listMyCertificates() {
        Integer userId = currentUserAccessor.currentUserId();
        return certificateRepository.findByUser_Id(userId).stream()
                .map(cert -> CertificateResponse.builder()
                        .id(cert.getId())
                        .hackathonId(cert.getHackathon().getId())
                        .hackathonName(cert.getHackathon().getName())
                        .issuedAt(cert.getIssuedAt())
                        .downloadUrl("/api/v1/me/certificates/" + cert.getId() + "/download")
                        .build())
                .toList();
    }

    @Override
    public CertificateDownload getCertificateDownload(Integer certificateId) {
        Integer userId = currentUserAccessor.currentUserId();
        var cert = certificateRepository.findById(certificateId)
                .orElseThrow(() -> new ResourceNotFoundException("Certificate", certificateId));

        if (!cert.getUser().getId().equals(userId)) {
            throw new com.sealhackathon.api.common.exception.AuthException(ErrorCode.FORBIDDEN,
                    "Bạn không có quyền tải giấy chứng nhận này.", org.springframework.http.HttpStatus.FORBIDDEN);
        }

        return certificateFileResolver.resolve(cert);
    }

    @Override
    public StudentHistoryResponse getHistory() {
        Integer userId = currentUserAccessor.currentUserId();
        List<TeamMember> myMemberships = teamMemberRepository.findByUser_IdAndStatus(userId, TeamMemberStatus.ACCEPTED);

        List<StudentHistoryResponse.StudentHistoryHackathonItem> items = myMemberships.stream()
                .filter(tm -> tm.getTeam().getHackathon().getStatus() == HackathonStatus.FINISHED)
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
        return Collections.emptyList();
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

        com.sealhackathon.api.rounds.entity.Round round = trt.getTrack().getRound();

        if (Boolean.TRUE.equals(round.getIsActive())) {
            throw new BusinessRuleException("ROUND_ALREADY_ACTIVE", "Vòng thi đã bắt đầu. Không thể đổi Track nữa.");
        }

        var newTrack = com.sealhackathon.api.tracks.entity.Track.builder().id(request.getTrackId()).build();
        trt.setTrack(newTrack);
        teamRoundTrackRepository.save(trt);
    }

    @Override
    @Transactional
    public void selectFallTrack(Integer trackId) {
        throw new BusinessRuleException(ErrorCode.NOT_IMPLEMENTED, "Tính năng tự chọn Track không áp dụng cho mùa giải hiện tại.");
    }

    @Override
    @Transactional
    public AppealResponse createAppeal(CreateAppealRequest request) {
        return appealService.create(request);
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
                .orElseThrow(() -> new ResourceNotFoundException("Round", "active prelim"));
    }

    private static List<Submission> merge(List<Submission> a, List<Submission> b) {
        return java.util.stream.Stream.concat(a.stream(), b.stream())
                .distinct()
                .toList();
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