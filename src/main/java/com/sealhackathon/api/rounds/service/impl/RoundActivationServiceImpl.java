package com.sealhackathon.api.rounds.service.impl;

import com.sealhackathon.api.common.audit.AuditAction;
import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.common.exception.ResourceNotFoundException;
import com.sealhackathon.api.criteria.repository.CriteriaRepository;
import com.sealhackathon.api.criteria.service.WeightSummaryService;
import com.sealhackathon.api.judge_assignments.entity.JudgeAssignment;
import com.sealhackathon.api.judge_assignments.repository.JudgeAssignmentRepository;
import com.sealhackathon.api.judge_assignments.value_object.JudgeAssignmentType;
import com.sealhackathon.api.mentors.entity.MentorAssignment;
import com.sealhackathon.api.mentors.repository.MentorAssignmentRepository;
import com.sealhackathon.api.notifications.service.NotificationService;
import com.sealhackathon.api.rounds.dto.request.ActivateRoundRequest;
import com.sealhackathon.api.rounds.dto.response.RoundResponse;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.mapper.RoundMapper;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.rounds.service.RoundActivationService;
import com.sealhackathon.api.rounds.support.RoundScheduleShiftService;
import com.sealhackathon.api.rounds.value_object.ActivateScheduleMode;
import com.sealhackathon.api.teams.repository.TeamRoundTrackRepository;
import com.sealhackathon.api.tracks.entity.Track;
import com.sealhackathon.api.tracks.repository.TrackRepository;
import com.sealhackathon.api.tracks.value_object.TrackStatus;
import com.sealhackathon.api.users.entity.User;
import com.sealhackathon.api.users.value_object.UserType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class RoundActivationServiceImpl implements RoundActivationService {

    private final RoundRepository roundRepository;
    private final TrackRepository trackRepository;
    private final CriteriaRepository criteriaRepository;
    private final MentorAssignmentRepository mentorAssignmentRepository;
    private final JudgeAssignmentRepository judgeAssignmentRepository;
    private final RoundMapper roundMapper;
    private final AuditService auditService;
    private final WeightSummaryService weightSummaryService;
    private final NotificationService notificationService;
    private final TeamRoundTrackRepository teamRoundTrackRepository;
    private final RoundScheduleShiftService roundScheduleShiftService;
    private final com.sealhackathon.api.hackathons.support.PendingTeamGateService pendingTeamGateService;

    @Override
    public RoundResponse activate(Integer roundId, ActivateRoundRequest request) {
        ActivateRoundRequest body = request != null ? request : ActivateRoundRequest.builder().build();
        Round round = roundRepository.findByIdForUpdate(roundId)
                .orElseThrow(() -> new ResourceNotFoundException("Round", roundId));

        ActivateScheduleMode scheduleMode = body.getScheduleMode() != null
                ? body.getScheduleMode()
                : ActivateScheduleMode.KEEP;

        // RESCHEDULE không còn trên Activate — dùng POST .../competition-schedule/adjust hoặc close-reg-early
        if (scheduleMode == ActivateScheduleMode.RESCHEDULE) {
            throw new BusinessRuleException(ErrorCode.VALIDATION_FAILED,
                    "Dời lịch thi không còn gắn với Kích hoạt vòng. Dùng «Dời lịch thi» (1 lần, trước Kickoff ≥ 4 ngày) hoặc chọn lịch khi «Kết thúc đăng ký sớm».");
        }

        if (Boolean.TRUE.equals(round.getIsActive())) {
            return roundMapper.toResponse(round);
        }

        if (Boolean.TRUE.equals(round.getIsFinal())) {
            validatePreliminaryRoundPublished(round.getHackathon().getId());
            validateFinalRoundCriteria(roundId);
            validateFinalRoundJudges(roundId);
        } else {
            // LOT-04: defense-in-depth — không activate sơ loại khi còn đội PENDING
            pendingTeamGateService.assertNoPendingTeams(round.getHackathon().getId());
            validateTeamsInRound(round);
            validatePreliminaryRoundTracks(round);
        }

        Integer hackathonId = round.getHackathon().getId();
        int deactivated = roundRepository.deactivateOtherActiveRoundsInHackathon(hackathonId, roundId);
        if (deactivated > 0) {
            auditService.log(AuditAction.ROUND_DEACTIVATE, "rounds", roundId,
                    Map.of("hackathonId", hackathonId, "deactivatedCount", deactivated));
        }

        boolean scheduleShifted = roundScheduleShiftService.applyOnActivate(
                round, scheduleMode, body.getNewExamAt());

        LocalDateTime activatedAt = LocalDateTime.now();
        round.setIsActive(true);
        round.setActivatedAt(activatedAt);
        // CK: mỗi đội tiếp tục đề track sơ loại — stamp problemReleasedAt khi activate (không bước Phát đề).
        boolean stampedFinalProblemRelease = false;
        if (Boolean.TRUE.equals(round.getIsFinal()) && round.getProblemReleasedAt() == null) {
            round.setProblemReleasedAt(activatedAt);
            stampedFinalProblemRelease = true;
        }
        Round saved = roundRepository.save(round);

        Map<String, Object> activateAudit = new LinkedHashMap<>();
        activateAudit.put("hackathonId", hackathonId);
        activateAudit.put("note", body.getNote());
        activateAudit.put("isFinal", round.getIsFinal());
        activateAudit.put("siblingDeactivated", deactivated);
        activateAudit.put("scheduleMode", scheduleMode.name());
        activateAudit.put("scheduleShifted", scheduleShifted);
        activateAudit.put("activated", true);
        if (stampedFinalProblemRelease) {
            activateAudit.put("problemReleasedAtStamped", true);
        }
        auditService.log(AuditAction.ROUND_ACTIVATE, "rounds", roundId, activateAudit);

        notifyRoundStarted(saved);
        return roundMapper.toResponse(saved);
    }

    private void validateTeamsInRound(Round round) {
        List<Track> tracks = trackRepository.findByRoundIdOrderBySequenceOrderAsc(round.getId()).stream()
                .filter(t -> t.getStatus() != TrackStatus.CANCELLED)
                .toList();
        if (tracks.isEmpty()) {
            throw new BusinessRuleException(ErrorCode.NO_TEAMS_IN_ROUND,
                    "Không có đội tham gia vòng thi này",
                    Map.of("roundId", round.getId()));
        }
        List<Integer> emptyTrackIds = new ArrayList<>();
        List<String> emptyTrackNames = new ArrayList<>();
        for (Track t : tracks) {
            if (teamRoundTrackRepository.countByTrack_Id(t.getId()) == 0) {
                emptyTrackIds.add(t.getId());
                emptyTrackNames.add(t.getName());
            }
        }
        if (!emptyTrackIds.isEmpty()) {
            throw new BusinessRuleException(ErrorCode.TRACK_EMPTY_TEAMS,
                    "Mọi bảng đấu của vòng thi phải có ít nhất một đội. Còn trống: %s"
                            .formatted(String.join(", ", emptyTrackNames)),
                    Map.of("roundId", round.getId(), "emptyTrackIds", emptyTrackIds,
                            "emptyTrackNames", emptyTrackNames));
        }
    }

    private void validatePreliminaryRoundPublished(Integer hackathonId) {
        boolean unpublished = roundRepository.findByHackathon_IdOrderByExamAtAsc(hackathonId).stream()
                .filter(r -> !Boolean.TRUE.equals(r.getIsFinal()))
                .anyMatch(r -> !Boolean.TRUE.equals(r.getIsPublished()));
        if (unpublished) {
            throw new BusinessRuleException(ErrorCode.RESULT_NOT_PUBLISHED,
                    "Chưa công bố kết quả vòng Sơ loại",
                    Map.of("hackathonId", hackathonId));
        }
    }

    private void validateFinalRoundCriteria(Integer roundId) {
        if (criteriaRepository.countNormalByFinalRoundId(roundId) == 0) {
            throw new BusinessRuleException(ErrorCode.ROUND_NO_CRITERIA,
                    "Vòng thi Chung kết chưa có Criteria",
                    Map.of("roundId", roundId));
        }
        Optional<Double> totalOpt = criteriaRepository.sumWeightExcludingPenaltyByFinalRoundId(roundId);
        double total = totalOpt.orElse(0.0);
        if (Math.abs(total - WeightSummaryService.TARGET) > WeightSummaryService.TOLERANCE) {
            throw new BusinessRuleException(ErrorCode.ROUND_WEIGHT_NOT_ONE,
                    "Vòng thi Chung kết: tổng weight = %.4f".formatted(total),
                    Map.of("roundId", roundId, "currentTotal", total));
        }
    }

    private void validateFinalRoundJudges(Integer roundId) {
        List<JudgeAssignment> assignments = judgeAssignmentRepository.findByRoundId(roundId);
        if (assignments.isEmpty()) {
            throw new BusinessRuleException(ErrorCode.JUDGE_NOT_ASSIGNED,
                    "Vòng thi Chung kết chưa có Judge được phân công",
                    Map.of("roundId", roundId));
        }
        boolean hasFinalExternal = false;
        for (JudgeAssignment ja : assignments) {
            JudgeAssignmentType type = ja.getAssignmentType();
            // HEAD legacy trong DB được chấp nhận như NORMAL — không còn quyền đặc biệt.
            if (type != JudgeAssignmentType.FINAL_EXTERNAL
                    && type != JudgeAssignmentType.NORMAL
                    && type != JudgeAssignmentType.HEAD) {
                throw new BusinessRuleException(ErrorCode.INVALID_ASSIGNMENT_TYPE,
                        "Vòng thi Chung kết chỉ chấp nhận Judge NORMAL hoặc FINAL_EXTERNAL",
                        Map.of("roundId", roundId, "judgeId", ja.getJudge().getId(),
                                "assignmentType", type));
            }
            if (type == JudgeAssignmentType.FINAL_EXTERNAL) {
                hasFinalExternal = true;
                if (ja.getJudge().getUserType() != UserType.EXTERNAL) {
                    throw new BusinessRuleException(ErrorCode.INVALID_ASSIGNMENT_TYPE,
                            "FINAL_EXTERNAL yêu cầu Judge EXTERNAL",
                            Map.of("roundId", roundId, "judgeId", ja.getJudge().getId()));
                }
            }
        }
        if (!hasFinalExternal) {
            throw new BusinessRuleException(ErrorCode.JUDGE_NOT_ASSIGNED,
                    "Vòng thi Chung kết cần ít nhất một Judge FINAL_EXTERNAL",
                    Map.of("roundId", roundId));
        }
    }

    private void validatePreliminaryRoundTracks(Round round) {
        List<Track> tracks = trackRepository.findByRoundIdOrderBySequenceOrderAsc(round.getId()).stream()
                .filter(t -> t.getStatus() != TrackStatus.CANCELLED)
                .toList();
        for (Track t : tracks) {
            if (criteriaRepository.countNormalByTrackId(t.getId()) == 0) {
                throw new BusinessRuleException(ErrorCode.ROUND_NO_CRITERIA,
                        "Bảng đấu '%s' của vòng thi chưa có Criteria".formatted(t.getName()),
                        Map.of("trackId", t.getId(), "roundId", round.getId()));
            }
            if (!weightSummaryService.isValidForTrack(t.getId())) {
                double raw = weightSummaryService.rawTotalForTrack(t.getId()).orElse(0.0);
                throw new BusinessRuleException(ErrorCode.ROUND_WEIGHT_NOT_ONE,
                        "Bảng đấu '%s' của vòng thi: tổng weight = %.4f".formatted(t.getName(), raw),
                        Map.of("trackId", t.getId(), "roundId", round.getId(), "total", raw));
            }
            if (judgeAssignmentRepository.findByTrackId(t.getId()).isEmpty()) {
                throw new BusinessRuleException(ErrorCode.JUDGE_NOT_ASSIGNED,
                        "Bảng đấu '%s' của vòng thi chưa có Judge được phân công".formatted(t.getName()),
                        Map.of("trackId", t.getId(), "roundId", round.getId()));
            }
            validateTrackMentorJudgeConflict(t);
        }
    }

    private void validateTrackMentorJudgeConflict(Track track) {
        for (MentorAssignment ma : mentorAssignmentRepository.findByTrackId(track.getId())) {
            Integer mentorId = ma.getMentor().getId();
            if (judgeAssignmentRepository.existsByJudgeIdAndTrackId(mentorId, track.getId())) {
                throw new BusinessRuleException(ErrorCode.CONFLICT_SAME_TRACK,
                        "Track '%s': user #%d vừa Mentor vừa Judge"
                                .formatted(track.getName(), mentorId),
                        Map.of("trackId", track.getId(), "userId", mentorId));
            }
        }
    }

    private void notifyRoundStarted(Round round) {
        Set<User> recipients = new LinkedHashSet<>();
        if (Boolean.TRUE.equals(round.getIsFinal())) {
            judgeAssignmentRepository.findByRoundId(round.getId()).stream()
                    .map(JudgeAssignment::getJudge)
                    .forEach(recipients::add);
        } else {
            for (Track t : trackRepository.findByRoundIdOrderBySequenceOrderAsc(round.getId())) {
                if (t.getStatus() == TrackStatus.CANCELLED) {
                    continue;
                }
                mentorAssignmentRepository.findByTrackId(t.getId()).stream()
                        .map(MentorAssignment::getMentor)
                        .forEach(recipients::add);
                judgeAssignmentRepository.findByTrackId(t.getId()).stream()
                        .map(JudgeAssignment::getJudge)
                        .forEach(recipients::add);
            }
        }
        if (recipients.isEmpty()) {
            return;
        }
        notificationService.sendBatch(
                new ArrayList<>(recipients),
                "ROUND_STARTED",
                "Vòng '%s' đã được kích hoạt".formatted(round.getName()),
                "Round %s trong hackathon đã active — chuẩn bị bắt đầu."
                        .formatted(round.getName()),
                "rounds",
                round.getId());
    }
}
