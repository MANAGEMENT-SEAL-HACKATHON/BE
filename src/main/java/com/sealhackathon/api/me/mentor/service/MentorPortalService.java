package com.sealhackathon.api.me.mentor.service;

import com.sealhackathon.api.me.mentor.dto.response.*;

import java.util.List;

public interface MentorPortalService {

    List<MentorTrackAssignmentResponse> listTrackAssignments();

    List<MentorTeamAssignmentResponse> listTeamAssignments(Integer roundId);

    MentorPresentationSlotResponse getPresentationSlot(Integer teamId);

    List<MentorSubmissionViewResponse> listTeamSubmissions(Integer teamId, Integer roundId);

    List<MentorTeamScoreResponse> listTeamScores(Integer teamId, Integer roundId);

    MentorRoundScheduleResponse getFinalRoundSchedule(Integer roundId);

    MentorRankingResponse getHackathonRankings(Integer hackathonId);

    MentorHistoryResponse getHistory(Integer year);
}
