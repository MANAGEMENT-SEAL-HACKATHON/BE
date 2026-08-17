package com.sealhackathon.api.me.mentor.service.impl;

import com.sealhackathon.api.common.security.CurrentUserAccessor;
import com.sealhackathon.api.events.repository.PresentationSlotRepository;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.me.mentor.dto.response.*;
import com.sealhackathon.api.me.mentor.service.MentorPortalService;
import com.sealhackathon.api.me.support.MentorAccessGuard;
import com.sealhackathon.api.presentation.support.PresentationSlotHelper;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.mentors.entity.MentorAssignment;
import com.sealhackathon.api.mentors.repository.MentorAssignmentRepository;
import com.sealhackathon.api.mentors.entity.MentorTeamAssignment;
import com.sealhackathon.api.mentors.repository.MentorTeamAssignmentRepository;
import com.sealhackathon.api.rounds.query.RoundRankingQueryService;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.submissions.repository.SubmissionRepository;
import com.sealhackathon.api.teams.repository.TeamRoundTrackRepository;
import com.sealhackathon.api.teams.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MentorPortalServiceImpl implements MentorPortalService {

    // 1. INJECT CÁC DEPENDENCY CẦN THIẾT
    private final CurrentUserAccessor currentUserAccessor;
    private final MentorAssignmentRepository mentorAssignmentRepository;
    private final MentorTeamAssignmentRepository mentorTeamAssignmentRepository;
    private final MentorAccessGuard mentorAccessGuard;
    private final SubmissionRepository submissionRepository;
    private final RoundRepository roundRepository;
    private final RoundRankingQueryService roundRankingQueryService;
    private final HackathonRepository hackathonRepository;
    private final TeamRoundTrackRepository teamRoundTrackRepository;
    private final PresentationSlotRepository presentationSlotRepository;
    private final TeamRepository teamRepository;

    @Override
    public List<MentorTrackAssignmentResponse> listTrackAssignments() {
        // BƯỚC 1: Lấy ID của Mentor đang đăng nhập từ Security Context
        Integer mentorId = currentUserAccessor.currentUserId();

        // BƯỚC 2: Truy vấn Database lấy danh sách Phân công Cấp độ Track (GĐ1)
        List<MentorAssignment> assignments = mentorAssignmentRepository.findByMentorId(mentorId);

        // BƯỚC 3: Map Entity sang DTO để trả về cho Frontend (FR-M-05)
        // Dùng Stream API để code gọn gàng, functional và tối ưu performance
        return assignments.stream()
                .map(ma -> MentorTrackAssignmentResponse.builder()
                        .assignmentId(ma.getId())
                        .trackId(ma.getTrack().getId())
                        .trackName(ma.getTrack().getName())
                        .build())
                .toList();
    }

    @Override
    public List<MentorRoundResponse> getMentorRounds() {
        Integer mentorId = currentUserAccessor.currentUserId();
        List<MentorTeamAssignment> assignments = mentorTeamAssignmentRepository.findByMentor_Id(mentorId);

        if (assignments.isEmpty()) {
            // FR-M-05: mentor chỉ gán track — FE bootstrap khi rounds rỗng
            return Collections.emptyList();
        }

        Integer hackathonId = assignments.get(0).getHackathon().getId();

        List<Round> rounds = roundRepository.findByHackathon_IdOrderByExamAtAsc(hackathonId);
        return rounds.stream()
                .map(r -> {
                    List<MentorTeamAssignment> roundAssignments = assignments.stream()
                            .filter(a -> a.getRound().getId().equals(r.getId()))
                            .toList();

                    List<MentorRoundResponse.TeamInfo> teamInfos = roundAssignments.stream()
                            .map(a -> MentorRoundResponse.TeamInfo.builder()
                                    .teamId(a.getTeam().getId())
                                    .teamName(a.getTeam().getTeamName())
                                    .assignmentId(a.getId())
                                    .build())
                            .toList();

                    String status = resolveRoundStatus(r, roundAssignments);
                    String desc = resolveRoundDescription(r, status);

                    return MentorRoundResponse.builder()
                            .roundId(r.getId())
                            .roundName(r.getName())
                            .status(status)
                            .description(desc)
                            .teamCount(teamInfos.size())
                            .teams(teamInfos)
                            .build();
                })
                .toList();
    }

    @Override
    public MentorAssignedTeamsResponse getAssignedTeamsForRound(Integer roundId) {
        Integer mentorId = currentUserAccessor.currentUserId();
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new com.sealhackathon.api.common.exception.ResourceNotFoundException("Round", roundId));

        List<MentorTeamAssignment> assignments =
                mentorTeamAssignmentRepository.findByMentor_IdAndRound_Id(mentorId, roundId);

        List<MentorAssignedTeamsResponse.TeamItem> teams = assignments.stream()
                .map(mta -> {
                    var trtOpt = teamRoundTrackRepository.findByTeam_IdAndTrack_Round_Id(
                            mta.getTeam().getId(), roundId);
                    var slotOpt = presentationSlotRepository.findByRound_IdAndTeam_Id(
                            roundId, mta.getTeam().getId());
                    var trt = trtOpt.orElse(null);
                    var slot = slotOpt.orElse(null);

                    LocalDateTime start = PresentationSlotHelper.resolveStart(trt, slot);
                    LocalDateTime end = PresentationSlotHelper.resolveEnd(trt, slot);

                    return MentorAssignedTeamsResponse.TeamItem.builder()
                            .teamId(mta.getTeam().getId())
                            .teamName(mta.getTeam().getTeamName())
                            .groupNumber(trt != null
                                    ? PresentationSlotHelper.parseGroupNumber(trt.getAssignedGroup())
                                    : 1)
                            .status(mta.getTeam().getStatus().name())
                            .presentationSchedule(PresentationSlotHelper.formatSchedule(start, end))
                            .location(PresentationSlotHelper.resolveLocation(trt, slot))
                            .build();
                })
                .toList();

        return MentorAssignedTeamsResponse.builder()
                .roundName(round.getName())
                .roundStatus(resolveRoundStatus(round, assignments))
                .teams(teams)
                .build();
    }

    @Override
    public List<MentorTeamAssignmentResponse> listTeamAssignments(Integer roundId) {
        // BƯỚC 1: Lấy ID của Mentor đang đăng nhập
        Integer mentorId = currentUserAccessor.currentUserId();

        // BƯỚC 2: Truy vấn Database lấy danh sách Đội mà Mentor này trực tiếp cố vấn (GĐ2)
        List<MentorTeamAssignment> assignments = mentorTeamAssignmentRepository.findByMentor_Id(mentorId);

        // BƯỚC 3: Lọc (Filter) và Map dữ liệu (FR-M-06)
        return assignments.stream()
                // Rào chắn linh hoạt: Nếu client có truyền roundId thì lọc theo round, nếu không thì lấy tất cả
                .filter(mta -> roundId == null || mta.getRound().getId().equals(roundId))
                .map(mta -> MentorTeamAssignmentResponse.builder()
                        .assignmentId(mta.getId())
                        .teamId(mta.getTeam().getId())
                        .teamName(mta.getTeam().getTeamName())
                        .hackathonId(mta.getHackathon().getId())
                        .build())
                .toList();
    }

    @Override
    public MentorPresentationSlotResponse getPresentationSlot(Integer teamId) {
        // 1. RÀO CHẮN BẢO MẬT: Bắt buộc Mentor phải có quyền với Đội này
        mentorAccessGuard.assertAssignedToTeam(teamId);

        // 2. Lấy dữ liệu THẬT từ Database
        var slotOpt = presentationSlotRepository.findTopByTeam_IdOrderByStartsAtDesc(teamId);

        if (slotOpt.isEmpty()) {
            return MentorPresentationSlotResponse.builder()
                    .teamId(teamId)
                    .location("Chưa có lịch thuyết trình cụ thể cho đội này.")
                    .build();
        }

        var slot = slotOpt.get();
        return MentorPresentationSlotResponse.builder()
                .teamId(teamId)
                .slotStartAt(slot.getStartsAt())
                .slotEndAt(slot.getEndsAt())
                .location(slot.getLocation())
                .build();
    }

    @Override
    public List<MentorSubmissionViewResponse> listTeamSubmissions(Integer teamId, Integer roundId) {
        // 1. RÀO CHẮN BẢO MẬT: Bắt buộc Mentor phải có quyền với Đội này (Fix Bug M-BUG-1)
        mentorAccessGuard.assertAssignedToTeam(teamId);

        // 2. Lấy danh sách bài nộp của Đội (Read-Only)
        var submissions = submissionRepository.findByTeam_Id(teamId);

        // 3. Map sang DTO và Lọc theo roundId nếu FE có truyền lên
        return submissions.stream()
                .filter(sub -> roundId == null || (sub.getRound() != null && sub.getRound().getId().equals(roundId)))
                .map(sub -> MentorSubmissionViewResponse.builder()
                        .submissionId(sub.getId())
                        .roundId(sub.getRound() != null ? sub.getRound().getId() : null)
                        .status(sub.getStatus().name())
                        .submittedAt(sub.getSubmittedAt())
                        .build())
                .toList();
    }

    @Override
    public List<MentorTeamScoreResponse> listTeamScores(Integer teamId, Integer roundId) {
        // 1. RÀO CHẮN: Phải là Mentor của đội
        mentorAccessGuard.assertAssignedToTeam(teamId);

        // 2. Xác định các Vòng thi (Rounds) mà đội đang tham gia
        List<com.sealhackathon.api.rounds.entity.Round> roundsToCheck = new ArrayList<>();

        if (roundId != null) {
            // Nếu Client truyền roundId, lấy trực tiếp Round đó
            com.sealhackathon.api.rounds.entity.Round round = roundRepository.findById(roundId)
                    .orElseThrow(() -> new com.sealhackathon.api.common.exception.ResourceNotFoundException("Round", roundId));
            roundsToCheck.add(round);
        } else {
            // Nếu không truyền roundId, lấy tất cả các vòng Sơ loại của đội này thông qua team_round_tracks
            var tracks = teamRoundTrackRepository.findByTeam_Id(teamId);
            tracks.forEach(trt -> roundsToCheck.add(trt.getTrack().getRound()));
        }

        List<MentorTeamScoreResponse> responses = new ArrayList<>();

        // 3. Duyệt qua từng Vòng thi để kiểm tra Rào chắn và Lấy điểm
        for (com.sealhackathon.api.rounds.entity.Round round : roundsToCheck) {

            // 4. RÀO CHẮN (BR-M-9): Chỉ được xem điểm SAU KHI Vòng thi đã chốt (scoring_locked = TRUE)
            if (!Boolean.TRUE.equals(round.getScoringLocked())) {
                throw new com.sealhackathon.api.common.exception.BusinessRuleException(
                        com.sealhackathon.api.common.exception.ErrorCode.ROUND_NOT_SCORING_LOCKED,
                        "Truy cập bị từ chối: Vòng thi '" + round.getName() + "' chưa chốt điểm. Vui lòng quay lại sau.");
            }

            // 5. Tận dụng RoundRankingQueryService để lấy điểm tổng (có trừ Penalty)
            List<com.sealhackathon.api.rounds.dto.response.RoundRankingItemResponse> ranking =
                    roundRankingQueryService.rankingForRound(round.getId(), false);

            Double totalScore = ranking.stream()
                    .filter(r -> r.getTeamId().equals(teamId))
                    .map(com.sealhackathon.api.rounds.dto.response.RoundRankingItemResponse::getTotalScore)
                    .findFirst()
                    .orElse(0.0);

            responses.add(MentorTeamScoreResponse.builder()
                    .roundId(round.getId())
                    .totalScore(java.math.BigDecimal.valueOf(totalScore))
                    .scoringLocked(true)
                    .build());
        }

        return responses;
    }

    @Override
    public MentorRoundScheduleResponse getFinalRoundSchedule(Integer roundId) {
        com.sealhackathon.api.rounds.entity.Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new com.sealhackathon.api.common.exception.ResourceNotFoundException("Round", roundId));

        if (!Boolean.TRUE.equals(round.getIsFinal())) {
            throw new com.sealhackathon.api.common.exception.BusinessRuleException(
                    com.sealhackathon.api.common.exception.ErrorCode.INVALID_FINAL_ROUND, "Vòng thi này không phải là Vòng Chung Kết");
        }

        // Tương tự FR-M-12, schema lịch thuyết trình đang pending, trả về mảng rỗng tạm thời
        return MentorRoundScheduleResponse.builder()
                .roundId(roundId)
                .roundName(round.getName())
                .slots(Collections.emptyList())
                .build();
    }

    @Override
    public MentorRankingResponse getHackathonRankings(Integer hackathonId) {
        com.sealhackathon.api.hackathons.entity.Hackathon hackathon = hackathonRepository.findById(hackathonId)
                .orElseThrow(() -> new com.sealhackathon.api.common.exception.ResourceNotFoundException("Hackathon", hackathonId));

        // RÀO CHẮN: Kết quả chỉ công bố khi Hackathon đã FINISHED hoặc PENDING_CONFIRM
        if (hackathon.getStatus() != com.sealhackathon.api.hackathons.value_object.HackathonStatus.FINISHED
                && hackathon.getStatus() != com.sealhackathon.api.hackathons.value_object.HackathonStatus.PENDING_CONFIRM) {
            throw new com.sealhackathon.api.common.exception.BusinessRuleException(
                    "RESULT_NOT_AVAILABLE",
                    "Bảng xếp hạng chưa sẵn sàng. Đang chờ Ban Tổ Chức công bố kết quả chung cuộc.");
        }

        com.sealhackathon.api.rounds.entity.Round finalRound = roundRepository.findByHackathon_IdAndIsFinalTrue(hackathonId)
                .orElseThrow(() -> new com.sealhackathon.api.common.exception.BusinessRuleException(
                        com.sealhackathon.api.common.exception.ErrorCode.INVALID_STATE, "Hackathon chưa có Vòng Chung Kết"));

        // 1. TÍNH XẾP HẠNG ĐỘI (TEAM RANKING)
        List<com.sealhackathon.api.rounds.dto.response.RoundRankingItemResponse> finalRanking =
                roundRankingQueryService.rankingForRound(finalRound.getId(), false);

        List<MentorRankingResponse.MentorRankingItem> teamRankings = finalRanking.stream()
                .map(r -> MentorRankingResponse.MentorRankingItem.builder()
                        .rank(r.getRank())
                        .teamId(r.getTeamId())
                        .teamName(r.getTeamName())
                        .build())
                .toList();

        // 2. TÍNH XẾP HẠNG CƠ SỞ (CHAPTER RANKING)
        // Lấy Entity Team để biết đội đó thuộc Chapter nào
        List<com.sealhackathon.api.teams.entity.Team> finalTeams = teamRepository.findAllById(
                finalRanking.stream().map(com.sealhackathon.api.rounds.dto.response.RoundRankingItemResponse::getTeamId).toList()
        );
        java.util.Map<Integer, com.sealhackathon.api.teams.entity.Team> teamMap = finalTeams.stream()
                .collect(java.util.stream.Collectors.toMap(com.sealhackathon.api.teams.entity.Team::getId, t -> t));

        // Gom nhóm điểm số theo Chapter
        java.util.Map<com.sealhackathon.api.chapters.entity.Chapter, List<Double>> chapterScoresMap = new java.util.HashMap<>();
        for (var rankItem : finalRanking) {
            com.sealhackathon.api.teams.entity.Team t = teamMap.get(rankItem.getTeamId());
            if (t != null && t.getChapter() != null) {
                chapterScoresMap.computeIfAbsent(t.getChapter(), k -> new ArrayList<>()).add(rankItem.getTotalScore());
            }
        }

        // Tính điểm trung bình và sắp xếp
        List<MentorRankingResponse.MentorRankingItem> chapterRankings = new ArrayList<>();
        int currentRank = 1;

        var sortedChapters = chapterScoresMap.entrySet().stream()
                .map(entry -> {
                    double avgScore = entry.getValue().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                    return new java.util.AbstractMap.SimpleEntry<>(entry.getKey(), avgScore);
                })
                .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue())) // Sắp xếp giảm dần
                .toList();

        for (var entry : sortedChapters) {
            chapterRankings.add(MentorRankingResponse.MentorRankingItem.builder()
                    .rank(currentRank++)
                    .teamId(entry.getKey().getId()) // Tái sử dụng DTO: teamId chứa chapterId
                    .teamName(entry.getKey().getName()) // Tái sử dụng DTO: teamName chứa chapterName
                    .build());
        }

        // 3. TRẢ VỀ KẾT QUẢ TỔNG HỢP
        return MentorRankingResponse.builder()
                .hackathonId(hackathonId)
                .teamRankings(teamRankings)
                .chapterRankings(chapterRankings)
                .build();
    }

    @Override
    public MentorHistoryResponse getHistory(Integer year) {
        Integer mentorId = currentUserAccessor.currentUserId();

        // 1. Lấy tất cả phân công Track của Mentor này
        List<MentorAssignment> trackAssignments = mentorAssignmentRepository.findByMentorId(mentorId);

        // 2. Lọc ra các Hackathon ĐÃ KẾT THÚC (FINISHED) và khớp với năm (year)
        var finishedHackathons = trackAssignments.stream()
                .map(ma -> ma.getTrack().getRound().getHackathon())
                .filter(h -> h.getStatus() == com.sealhackathon.api.hackathons.value_object.HackathonStatus.FINISHED)
                .filter(h -> year == null || h.getYear().equals(year))
                .distinct()
                .toList();

        // 3. Map sang DTO và đếm số lượng đội đã cố vấn trong từng Hackathon
        List<MentorHistoryResponse.MentorHistoryItem> items = finishedHackathons.stream()
                .map(h -> {
                    long teamsMentored = mentorTeamAssignmentRepository.findByMentor_Id(mentorId).stream()
                            .filter(mta -> mta.getHackathon().getId().equals(h.getId()))
                            .map(mta -> mta.getTeam().getId())
                            .distinct()
                            .count();

                    return MentorHistoryResponse.MentorHistoryItem.builder()
                            .hackathonId(h.getId())
                            .hackathonName(h.getName())
                            .teamsMentored((int) teamsMentored)
                            .build();
                })
                .toList();

        return MentorHistoryResponse.builder().items(items).build();
    }

    private static String resolveRoundStatus(Round round, List<MentorTeamAssignment> roundAssignments) {
        if (!roundAssignments.isEmpty()) {
            return "ACTIVE";
        }
        if (Boolean.TRUE.equals(round.getIsActive())) {
            return "ACTIVE";
        }
        if (Boolean.TRUE.equals(round.getScoringLocked())) {
            return "ENDED";
        }
        return "UPCOMING";
    }

    private static String resolveRoundDescription(Round round, String status) {
        if ("ACTIVE".equals(status)) {
            return "Vòng đấu loại trực tiếp của dự án SEAL Hackathon. Hạn nộp bài đang diễn ra.";
        }
        if (Boolean.TRUE.equals(round.getIsFinal())) {
            return "Chung kết xếp hạng và thuyết trình trực tiếp trước hội đồng giám khảo.";
        }
        return "Vòng bán kết đánh giá dự án thực tế. Sắp diễn ra.";
    }
}
