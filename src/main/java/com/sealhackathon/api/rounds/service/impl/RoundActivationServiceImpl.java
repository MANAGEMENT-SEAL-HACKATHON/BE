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
import com.sealhackathon.api.mentor_assignments.entity.MentorAssignment;
import com.sealhackathon.api.mentor_assignments.repository.MentorAssignmentRepository;
import com.sealhackathon.api.notifications.service.NotificationService;
import com.sealhackathon.api.rounds.dto.response.RoundResponse;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.mapper.RoundMapper;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.rounds.service.RoundActivationService;
import com.sealhackathon.api.team_round_participation.repository.TeamRoundParticipationRepository;
import com.sealhackathon.api.tracks.entity.Track;
import com.sealhackathon.api.tracks.repository.TrackRepository;
import com.sealhackathon.api.tracks.value_object.TrackStatus;
import com.sealhackathon.api.users.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
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
    private final TeamRoundParticipationRepository teamRoundParticipationRepository;

    @Override
    public RoundResponse activate(Integer roundId, String note) {
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new ResourceNotFoundException("Round", roundId));

        if (Boolean.TRUE.equals(round.getIsFinal())) {
            validatePreliminaryRoundPublished(round.getHackathon().getId());
            validateFinalRoundCriteria(roundId);
            validateFinalRoundJudges(roundId);
        } else {
            validateTeamsInRound(round);
            validatePreliminaryRoundTracks(round);
        }

        Integer hackathonId = round.getHackathon().getId();
        int deactivated = roundRepository.deactivateOtherActiveRoundsInHackathon(hackathonId, roundId);
        if (deactivated > 0) {
            auditService.log(AuditAction.ROUND_DEACTIVATE, "rounds", roundId,
                    Map.of("hackathonId", hackathonId, "deactivatedCount", deactivated));
        }

        round.setIsActive(true);
        round.setActivatedAt(LocalDateTime.now());
        Round saved = roundRepository.save(round);

        auditService.log(AuditAction.ROUND_ACTIVATE, "rounds", roundId, Map.of(
                "hackathonId", hackathonId,
                "note", note,
                "isFinal", round.getIsFinal(),
                "siblingDeactivated", deactivated
        ));

        notifyRoundStarted(saved);
        return roundMapper.toResponse(saved);
    }

    private void validateTeamsInRound(Round round) {
        if (teamRoundParticipationRepository.countByRound_Id(round.getId()) == 0) {
            throw new BusinessRuleException(ErrorCode.NO_TEAMS_IN_ROUND,
                    "Không có đội tham gia round này",
                    Map.of("roundId", round.getId()));
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
                    "Round Chung kết chưa có Criteria",
                    Map.of("roundId", roundId));
        }
        Optional<Double> totalOpt = criteriaRepository.sumWeightExcludingPenaltyByFinalRoundId(roundId);
        double total = totalOpt.orElse(0.0);
        if (Math.abs(total - WeightSummaryService.TARGET) > WeightSummaryService.TOLERANCE) {
            throw new BusinessRuleException(ErrorCode.ROUND_WEIGHT_NOT_ONE,
                    "Round Chung kết: tổng weight = %.4f".formatted(total),
                    Map.of("roundId", roundId, "currentTotal", total));
        }
    }

    private void validateFinalRoundJudges(Integer roundId) {
        List<JudgeAssignment> assignments = judgeAssignmentRepository.findByRoundId(roundId);
        if (assignments.isEmpty()) {
            throw new BusinessRuleException(ErrorCode.JUDGE_NOT_ASSIGNED,
                    "Round Chung kết chưa có Judge được phân công",
                    Map.of("roundId", roundId));
        }
        for (JudgeAssignment ja : assignments) {
            if (ja.getAssignmentType() != JudgeAssignmentType.FINAL_EXTERNAL) {
                throw new BusinessRuleException(ErrorCode.INVALID_ASSIGNMENT_TYPE,
                        "Round Chung kết chỉ chấp nhận Judge FINAL_EXTERNAL",
                        Map.of("roundId", roundId, "judgeId", ja.getJudge().getId(),
                                "assignmentType", ja.getAssignmentType()));
            }
        }
    }

    private void validatePreliminaryRoundTracks(Round round) {
        List<Track> tracks = trackRepository.findByRoundIdOrderBySequenceOrderAsc(round.getId()).stream()
                .filter(t -> t.getStatus() != TrackStatus.CANCELLED)
                .toList();
        for (Track t : tracks) {
            if (criteriaRepository.countNormalByTrackId(t.getId()) == 0) {
                throw new BusinessRuleException(ErrorCode.ROUND_NO_CRITERIA,
                        "Track '%s' chưa có Criteria".formatted(t.getName()),
                        Map.of("trackId", t.getId(), "roundId", round.getId()));
            }
            if (!weightSummaryService.isValidForTrack(t.getId())) {
                double raw = weightSummaryService.rawTotalForTrack(t.getId()).orElse(0.0);
                throw new BusinessRuleException(ErrorCode.ROUND_WEIGHT_NOT_ONE,
                        "Track '%s': tổng weight = %.4f".formatted(t.getName(), raw),
                        Map.of("trackId", t.getId(), "roundId", round.getId(), "total", raw));
            }
            if (judgeAssignmentRepository.findByTrackId(t.getId()).isEmpty()) {
                throw new BusinessRuleException(ErrorCode.JUDGE_NOT_ASSIGNED,
                        "Track '%s' chưa có Judge được phân công".formatted(t.getName()),
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
