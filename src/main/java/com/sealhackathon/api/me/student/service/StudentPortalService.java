package com.sealhackathon.api.me.student.service;

import com.sealhackathon.api.me.student.dto.request.CreateAppealRequest;
import com.sealhackathon.api.me.student.dto.request.RelotteryTrackRequest;
import com.sealhackathon.api.me.student.dto.response.*;

import java.util.List;

public interface StudentPortalService {

    List<StudentHackathonBrowseItemResponse> browseHackathons(String status);

    List<MeTeamSummaryResponse> listMyTeams();

    void relotteryTrack(Integer teamId, Integer roundId, RelotteryTrackRequest request);

    void selectFallTrack(Integer trackId);

    StudentProblemResponse getRoundProblem(Integer roundId);

    org.springframework.core.io.Resource downloadRoundProblemStatement(Integer roundId);

    List<StudentSubmissionStatusResponse> listTeamSubmissions(Integer teamId, Integer roundId);

    StudentSubmissionStatusResponse getLatestSubmission(Integer teamId, Integer roundId);

    StudentRoundDeadlineResponse getCurrentDeadline(Integer hackathonId);

    StudentFinalRoundResponse getFinalRoundForHackathon(Integer hackathonId);

    List<StudentLeaderboardItemResponse> getRoundLeaderboard(Integer roundId);

    StudentRankingResponse getHackathonRankings(Integer hackathonId);

    List<StudentPrizeResponse> listMyPrizes();

    List<CertificateResponse> listMyCertificates();

    CertificateDownload getCertificateDownload(Integer certificateId);

    AppealResponse createAppeal(CreateAppealRequest request);

    StudentHistoryResponse getHistory();

    List<AnnualAwardResponse> getAnnualAwards(Integer year);
}
