package com.sealhackathon.api.me.student.service.impl;

import com.sealhackathon.api.appeals.service.AppealService;
import com.sealhackathon.api.common.exception.ResourceNotFoundException;
import com.sealhackathon.api.common.security.CurrentUserAccessor;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.me.student.dto.request.CreateAppealRequest;
import com.sealhackathon.api.me.student.dto.request.RelotteryTrackRequest;
import com.sealhackathon.api.me.student.dto.response.*;
import com.sealhackathon.api.me.student.service.StudentPortalService;
import com.sealhackathon.api.me.support.StudentAccessGuard;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.submissions.entity.Submission;
import com.sealhackathon.api.submissions.repository.SubmissionRepository;
import com.sealhackathon.api.submissions.value_object.SubmissionStatus;
import com.sealhackathon.api.team_members.entity.TeamMember;
import com.sealhackathon.api.team_members.repository.TeamMemberRepository;
import com.sealhackathon.api.team_members.value_object.TeamMemberStatus;
import com.sealhackathon.api.teams.entity.Team;
import com.sealhackathon.api.teams.value_object.TeamStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentPortalServiceImpl implements StudentPortalService {

    private final AppealService appealService;
    private final StudentAccessGuard studentAccessGuard;
    private final CurrentUserAccessor currentUserAccessor;
    private final SubmissionRepository submissionRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final RoundRepository roundRepository;

    @Override
    public List<StudentHackathonBrowseItemResponse> browseHackathons(String status) {
        return Collections.emptyList();
    }

    @Override
    public List<MeTeamSummaryResponse> listMyTeams() {
        return Collections.emptyList();
    }

    @Override
    @Transactional
    public void relotteryTrack(Integer teamId, Integer roundId, RelotteryTrackRequest request) {
    }

    @Override
    @Transactional
    public void selectFallTrack(Integer trackId) {
    }

    @Override
    public StudentProblemResponse getRoundProblem(Integer roundId) {
        return StudentProblemResponse.builder().roundId(roundId).released(false).build();
    }

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
    public StudentRoundDeadlineResponse getCurrentDeadline() {
        Integer userId = currentUserAccessor.currentUserId();
        Round activeRound = findActivePrelimRoundForUser(userId);
        return StudentRoundDeadlineResponse.builder()
                .roundId(activeRound.getId())
                .deadline(activeRound.getSubmissionDeadline())
                .build();
    }

    @Override
    public List<StudentLeaderboardItemResponse> getRoundLeaderboard(Integer roundId) {
        return Collections.emptyList();
    }

    @Override
    public StudentRankingResponse getHackathonRankings(Integer hackathonId) {
        return StudentRankingResponse.builder()
                .hackathonId(hackathonId)
                .items(Collections.emptyList())
                .build();
    }

    @Override
    public List<StudentPrizeResponse> listMyPrizes() {
        return Collections.emptyList();
    }

    @Override
    public List<CertificateResponse> listMyCertificates() {
        return Collections.emptyList();
    }

    @Override
    public String certificateDownloadUrl(Integer certificateId) {
        return null;
    }

    @Override
    @Transactional
    public AppealResponse createAppeal(CreateAppealRequest request) {
        return appealService.create(request);
    }

    @Override
    public StudentHistoryResponse getHistory() {
        return StudentHistoryResponse.builder().hackathons(Collections.emptyList()).build();
    }

    @Override
    public List<AnnualAwardResponse> getAnnualAwards(Integer year) {
        return Collections.emptyList();
    }

    private List<Submission> findSubmissions(Integer teamId, Integer roundId) {
        if (roundId != null) {
            return merge(
                    submissionRepository.findByTeam_IdAndRound_Id(teamId, roundId),
                    submissionRepository.findByTeam_IdAndTrack_Round_Id(teamId, roundId));
        }
        return submissionRepository.findByTeam_Id(teamId);
    }

    private Round findActivePrelimRoundForUser(Integer userId) {
        return teamMemberRepository.findByUser_IdAndStatus(userId, TeamMemberStatus.ACCEPTED).stream()
                .map(TeamMember::getTeam)
                .filter(t -> t.getStatus() == TeamStatus.ACTIVE)
                .filter(t -> t.getHackathon().getStatus() == HackathonStatus.ONGOING)
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
        return StudentSubmissionStatusResponse.builder()
                .submissionId(s.getId())
                .roundId(s.getRound() != null ? s.getRound().getId()
                        : (s.getTrack() != null && s.getTrack().getRound() != null
                        ? s.getTrack().getRound().getId() : null))
                .repoUrl(s.getRepoUrl())
                .demoUrl(s.getDemoUrl())
                .slideUrl(s.getSlideUrl())
                .status(mapStatusForFe(s.getStatus()))
                .submittedAt(s.getSubmittedAt())
                .build();
    }

    static String mapStatusForFe(SubmissionStatus status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case SUBMITTED, LATE, LATE_APPROVED, ACCEPTED -> "ON_TIME";
            case LATE_PENDING -> "LATE_PENDING";
            case REJECTED -> "REJECTED";
        };
    }
}
