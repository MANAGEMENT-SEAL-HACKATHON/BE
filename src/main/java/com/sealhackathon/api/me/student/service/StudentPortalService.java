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

    List<StudentSubmissionStatusResponse> listTeamSubmissions(Integer teamId);

    List<StudentLeaderboardItemResponse> getRoundLeaderboard(Integer roundId);

    StudentRankingResponse getHackathonRankings(Integer hackathonId);

    List<StudentPrizeResponse> listMyPrizes();

    List<CertificateResponse> listMyCertificates();

    String certificateDownloadUrl(Integer certificateId);

    AppealResponse createAppeal(CreateAppealRequest request);

    StudentHistoryResponse getHistory();

    List<AnnualAwardResponse> getAnnualAwards(Integer year);
}
