package com.sealhackathon.api.me.student.service;

import com.sealhackathon.api.me.student.dto.request.CreateAppealRequest;
import com.sealhackathon.api.me.student.dto.request.RelotteryTrackRequest;
import com.sealhackathon.api.me.student.dto.response.*;
import com.sealhackathon.api.teams.dto.request.CreateTeamRequest;
import com.sealhackathon.api.teams.dto.response.TeamResponse;
import com.sealhackathon.api.tracks.dto.response.TrackSummaryResponse;

import java.util.List;

public interface StudentPortalService {

    List<StudentHackathonBrowseItemResponse> browseHackathons(String status);

    List<MeTeamSummaryResponse> listMyTeams(boolean includeEliminated);

    TeamResponse createTeam(CreateTeamRequest request);

    void relotteryTrack(Integer teamId, Integer roundId, RelotteryTrackRequest request);

    void selectFallTrack(Integer trackId);

    List<TrackSummaryResponse> listSelectableFallTracks(Integer hackathonId);

    StudentProblemResponse getRoundProblem(Integer roundId);

    org.springframework.core.io.Resource downloadRoundProblemStatement(Integer roundId);

    List<StudentSubmissionStatusResponse> listTeamSubmissions(Integer teamId, Integer roundId);

    StudentSubmissionStatusResponse getLatestSubmission(Integer teamId, Integer roundId);

    /** A2-1 — điểm đội ẩn danh sau publish. */
    StudentTeamScoreBreakdownResponse getTeamScoreBreakdown(Integer teamId, Integer roundId);

    StudentRoundDeadlineResponse getCurrentDeadline(Integer hackathonId);

    StudentFinalRoundResponse getFinalRoundForHackathon(Integer hackathonId);

    List<StudentLeaderboardItemResponse> getRoundLeaderboard(Integer roundId);

    /**
     * FR student STT — live presentation slot for the student's team in this round.
     * Before shuffle: {@code available=false}, message "Chưa quay số" (HTTP 200).
     */
    StudentPresentationSlotResponse getPresentationSlot(Integer roundId);

    StudentRankingResponse getHackathonRankings(Integer hackathonId);

    List<StudentPrizeResponse> listMyPrizes();

    AppealResponse createAppeal(CreateAppealRequest request);

    java.util.List<AppealResponse> listMyAppeals();

    com.sealhackathon.api.appeals.dto.response.AppealEvidenceUploadResponse uploadAppealEvidence(
            org.springframework.web.multipart.MultipartFile file);

    StudentHistoryResponse getHistory();

    List<AnnualAwardResponse> getAnnualAwards(Integer year);
}
