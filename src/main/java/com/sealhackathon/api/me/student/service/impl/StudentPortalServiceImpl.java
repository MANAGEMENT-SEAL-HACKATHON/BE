package com.sealhackathon.api.me.student.service.impl;

import com.sealhackathon.api.appeals.service.AppealService;
import com.sealhackathon.api.me.student.dto.request.CreateAppealRequest;
import com.sealhackathon.api.me.student.dto.request.RelotteryTrackRequest;
import com.sealhackathon.api.me.student.dto.response.*;
import com.sealhackathon.api.me.student.service.StudentPortalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentPortalServiceImpl implements StudentPortalService {

    private final AppealService appealService;

    @Override
    public List<StudentHackathonBrowseItemResponse> browseHackathons(String status) {
        // TODO: FR-U-05 — filter ONGOING hackathons + registration flag
        return Collections.emptyList();
    }

    @Override
    public List<MeTeamSummaryResponse> listMyTeams() {
        // TODO: FR-U-15 — teams for current user + track/lottery
        return Collections.emptyList();
    }

    @Override
    @Transactional
    public void relotteryTrack(Integer teamId, Integer roundId, RelotteryTrackRequest request) {
        // TODO: FR-U-16 — re-lottery within window
    }

    @Override
    @Transactional
    public void selectFallTrack(Integer trackId) {
        // TODO: FR-U-15-F — fall track selection
    }

    @Override
    public StudentProblemResponse getRoundProblem(Integer roundId) {
        // TODO: FR-U-17 — student view after release
        return StudentProblemResponse.builder().roundId(roundId).released(false).build();
    }

    @Override
    public List<StudentSubmissionStatusResponse> listTeamSubmissions(Integer teamId) {
        // TODO: FR-U-20 — submissions for team
        return Collections.emptyList();
    }

    @Override
    public List<StudentLeaderboardItemResponse> getRoundLeaderboard(Integer roundId) {
        // TODO: FR-U-21 — published leaderboard, no judge_id
        return Collections.emptyList();
    }

    @Override
    public StudentRankingResponse getHackathonRankings(Integer hackathonId) {
        // TODO: FR-U-27 — delegate GĐ6 rankings
        return StudentRankingResponse.builder()
                .hackathonId(hackathonId)
                .items(Collections.emptyList())
                .build();
    }

    @Override
    public List<StudentPrizeResponse> listMyPrizes() {
        // TODO: FR-U-28 — prizes for current user teams
        return Collections.emptyList();
    }

    @Override
    public List<CertificateResponse> listMyCertificates() {
        // TODO: FR-U-29 — certificates module
        return Collections.emptyList();
    }

    @Override
    public String certificateDownloadUrl(Integer certificateId) {
        // TODO: FR-U-29 — S3 signed URL
        return null;
    }

    @Override
    @Transactional
    public AppealResponse createAppeal(CreateAppealRequest request) {
        return appealService.create(request);
    }

    @Override
    public StudentHistoryResponse getHistory() {
        // TODO: FR-U-31 — aggregate past hackathons
        return StudentHistoryResponse.builder().hackathons(Collections.emptyList()).build();
    }

    @Override
    public List<AnnualAwardResponse> getAnnualAwards(Integer year) {
        // TODO: FR-U-32 — fall only annual awards; filter by year when set
        return Collections.emptyList();
    }
}
