package com.sealhackathon.api.me.mentor.service.impl;

import com.sealhackathon.api.me.mentor.dto.response.*;
import com.sealhackathon.api.me.mentor.service.MentorPortalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MentorPortalServiceImpl implements MentorPortalService {

    @Override
    public List<MentorTrackAssignmentResponse> listTrackAssignments() {
        // TODO: FR-M-05 — mentor track assignments
        return Collections.emptyList();
    }

    @Override
    public List<MentorTeamAssignmentResponse> listTeamAssignments(Integer roundId) {
        // TODO: FR-M-06 — mentor team assignments; filter by roundId when set
        return Collections.emptyList();
    }

    @Override
    public MentorPresentationSlotResponse getPresentationSlot(Integer teamId) {
        // TODO: FR-M-12 — presentation slot for team
        return MentorPresentationSlotResponse.builder().teamId(teamId).build();
    }

    @Override
    public List<MentorSubmissionViewResponse> listTeamSubmissions(Integer teamId, Integer roundId) {
        // TODO: FR-M-10 — mentor-scoped submissions; filter by roundId when set
        return Collections.emptyList();
    }

    @Override
    public List<MentorTeamScoreResponse> listTeamScores(Integer teamId, Integer roundId) {
        // TODO: FR-M-13 — after scoring_locked; filter by roundId when set
        return Collections.emptyList();
    }

    @Override
    public MentorRoundScheduleResponse getFinalRoundSchedule(Integer roundId) {
        // TODO: FR-M-16 — CK schedule (passive); derive from rounds + presentation slots
        return MentorRoundScheduleResponse.builder()
                .roundId(roundId)
                .slots(Collections.emptyList())
                .build();
    }

    @Override
    public MentorRankingResponse getHackathonRankings(Integer hackathonId) {
        // TODO: FR-M-18 — read-only GĐ6 rankings
        return MentorRankingResponse.builder()
                .hackathonId(hackathonId)
                .teamRankings(Collections.emptyList())
                .chapterRankings(Collections.emptyList())
                .build();
    }

    @Override
    public MentorHistoryResponse getHistory(Integer year) {
        // TODO: FR-M-19 — mentor history; filter by year when set
        return MentorHistoryResponse.builder().items(Collections.emptyList()).build();
    }
}
