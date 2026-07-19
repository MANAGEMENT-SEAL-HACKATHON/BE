package com.sealhackathon.api.judge_assignments.service.impl;

import com.sealhackathon.api.common.audit.AuditAction;
import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ConflictException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.common.exception.ResourceNotFoundException;
import com.sealhackathon.api.common.security.CurrentUserAccessor;
import com.sealhackathon.api.config.AppProperties;
import com.sealhackathon.api.config.FrontendUrls;
import com.sealhackathon.api.invitations.service.EmailService;
import com.sealhackathon.api.common.response.Warning;
import com.sealhackathon.api.common.response.WarningCode;
import com.sealhackathon.api.judge_assignments.dto.request.CreateJudgeAssignmentRequest;
import com.sealhackathon.api.judge_assignments.dto.response.JudgeAssignmentResponse;
import com.sealhackathon.api.judge_assignments.entity.JudgeAssignment;
import com.sealhackathon.api.judge_assignments.mapper.JudgeAssignmentMapper;
import com.sealhackathon.api.judge_assignments.repository.JudgeAssignmentRepository;
import com.sealhackathon.api.judge_assignments.service.JudgeAssignmentService;
import com.sealhackathon.api.judge_assignments.value_object.JudgeAssignmentType;
import com.sealhackathon.api.hackathons.support.HackathonArchiveGuard;
import com.sealhackathon.api.mentors.repository.MentorAssignmentRepository;
import com.sealhackathon.api.mentors.repository.MentorTeamAssignmentRepository;
import com.sealhackathon.api.notifications.service.NotificationService;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.teams.repository.TeamRoundTrackRepository;
import com.sealhackathon.api.tracks.entity.Track;
import com.sealhackathon.api.tracks.repository.TrackRepository;
import com.sealhackathon.api.tracks.support.TrackRoundRules;
import com.sealhackathon.api.users.entity.User;
import com.sealhackathon.api.users.repository.UserRepository;
import com.sealhackathon.api.users.support.PersonnelAssignmentRules;
import com.sealhackathon.api.users.value_object.UserStatus;
import com.sealhackathon.api.users.value_object.UserType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * FR-05c — user MENTOR hoặc JUDGE có thể là Judge track; cấm Judge+Mentor cùng track.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class JudgeAssignmentServiceImpl implements JudgeAssignmentService {

    private final JudgeAssignmentRepository judgeAssignmentRepository;
    private final MentorAssignmentRepository mentorAssignmentRepository;
    private final MentorTeamAssignmentRepository mentorTeamAssignmentRepository;
    private final TeamRoundTrackRepository teamRoundTrackRepository;
    private final UserRepository userRepository;
    private final RoundRepository roundRepository;
    private final TrackRepository trackRepository;
    private final JudgeAssignmentMapper judgeAssignmentMapper;
    private final AuditService auditService;
    private final CurrentUserAccessor currentUserAccessor;
    private final NotificationService notificationService;
    private final HackathonArchiveGuard archiveGuard;
    private final EmailService emailService;
    private final AppProperties appProperties;

    @Override
    public CreateResult assign(CreateJudgeAssignmentRequest req) {
        User judge = loadApprovedPersonnel(req.getJudgeId());
        JudgeAssignmentType assignType = req.getAssignmentType() != null
                ? req.getAssignmentType() : JudgeAssignmentType.NORMAL;

        if (req.getTrackId() != null) {
            return assignToTrack(judge, req.getTrackId(), assignType);
        }
        return assignToFinalRound(judge, req.getRoundId(), assignType);
    }

    private CreateResult assignToTrack(User judge, Integer trackId, JudgeAssignmentType assignType) {
        if (judge.getUserType() == UserType.EXTERNAL) {
            throw new BusinessRuleException(ErrorCode.EXTERNAL_JUDGE_NOT_ALLOWED_IN_PRELIM,
                    "Judge EXTERNAL chỉ được phân công Chung kết (FINAL_EXTERNAL), không gán Track sơ loại",
                    Map.of("trackId", trackId, "judgeId", judge.getId()));
        }
        if (assignType == JudgeAssignmentType.FINAL_EXTERNAL) {
            throw new BusinessRuleException(ErrorCode.INVALID_ASSIGNMENT_TYPE,
                    "FINAL_EXTERNAL không dùng cho Track Sơ loại",
                    Map.of("trackId", trackId));
        }
        if (assignType == JudgeAssignmentType.HEAD) {
            throw new BusinessRuleException(ErrorCode.INVALID_ASSIGNMENT_TYPE,
                    "Loại phân công không hợp lệ — Sơ loại chỉ chấp nhận Giám khảo thường (NORMAL)",
                    Map.of("trackId", trackId, "assignmentType", assignType.name()));
        }
        if (assignType != JudgeAssignmentType.NORMAL) {
            throw new BusinessRuleException(ErrorCode.INVALID_ASSIGNMENT_TYPE,
                    "Track Sơ loại chỉ gán Giám khảo NORMAL",
                    Map.of("trackId", trackId, "assignmentType", assignType.name()));
        }
        Track track = trackRepository.findById(trackId)
                .orElseThrow(() -> new ResourceNotFoundException("Track", trackId));
        archiveGuard.assertNotArchivedForTrack(track);
        TrackRoundRules.requirePreliminaryAssignmentTrack(track);

        if (mentorAssignmentRepository.existsByMentorIdAndTrackId(judge.getId(), trackId)) {
            throw new BusinessRuleException(ErrorCode.CONFLICT_SAME_TRACK,
                    "User đang là Mentor Track #%d — không thể phân công Judge cùng Track"
                            .formatted(trackId),
                    Map.of("trackId", trackId, "judgeId", judge.getId()));
        }
        assertNotMentorOfTeamInTrack(judge, trackId);
        if (judgeAssignmentRepository.existsByJudgeIdAndTrackId(judge.getId(), trackId)) {
            throw new ConflictException(ErrorCode.JUDGE_ASSIGN_DUPLICATE,
                    "Judge #%d đã được phân công Track #%d".formatted(judge.getId(), trackId));
        }
        Integer roundId = track.getRound() != null ? track.getRound().getId() : null;
        if (roundId != null
                && judgeAssignmentRepository.existsByJudgeIdAndRoundScope(judge.getId(), roundId)) {
            throw new ConflictException(ErrorCode.JUDGE_ASSIGN_DUPLICATE,
                    ("Giám khảo #%d đã được phân công vào bảng khác trong cùng vòng thi #%d — "
                            + "mỗi giám khảo chỉ được chấm một bảng trong một vòng")
                            .formatted(judge.getId(), roundId),
                    Map.of("judgeId", judge.getId(), "roundId", roundId, "trackId", trackId));
        }

        JudgeAssignment saved = saveAssignment(judge, track, null, assignType);
        JudgeAssignmentResponse response = judgeAssignmentMapper.toResponse(saved);
        auditService.log(AuditAction.JUDGE_ASSIGNED, "judge_assignments", saved.getId(), Map.of(
                "judgeId", judge.getId(), "trackId", trackId, "type", assignType.name()));
        logHeadChangeIfNeeded(saved, null, assignType, trackId, null);
        notificationService.send(judge, "JUDGE_ASSIGNED",
                "Bạn được phân công làm Judge Track '%s'".formatted(track.getName()),
                "Track: %s".formatted(track.getName()),
                "tracks", trackId);
        sendJudgeAssignmentEmail(judge, "Bảng đấu '%s'".formatted(track.getName()),
                track.getHackathon() == null ? null : track.getHackathon().getName());
        return new CreateResult(response, List.of());
    }

    private CreateResult assignToFinalRound(User judge, Integer roundId, JudgeAssignmentType assignType) {
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new ResourceNotFoundException("Round", roundId));
        archiveGuard.assertNotArchivedForRound(round);
        if (!Boolean.TRUE.equals(round.getIsFinal())) {
            throw new BusinessRuleException(ErrorCode.INVALID_FINAL_ROUND,
                    "Judge qua round_id chỉ cho Round Chung kết",
                    Map.of("roundId", roundId));
        }
        if (assignType == JudgeAssignmentType.HEAD) {
            throw new BusinessRuleException(ErrorCode.INVALID_ASSIGNMENT_TYPE,
                    "Loại phân công không hợp lệ — Chung kết chỉ chấp nhận Giám khảo khách (FINAL_EXTERNAL) hoặc Giám khảo thường (NORMAL)",
                    Map.of("roundId", roundId, "assignmentType", assignType.name()));
        }
        throw new BusinessRuleException(ErrorCode.JUDGE_FINAL_AT_PHASE1,
                "Phân công Judge Chung kết chỉ thực hiện ở GĐ4 — không làm ở GĐ1",
                Map.of("roundId", roundId));
    }

    @Override
    public CreateResult assignFinalRoundG4(Integer finalRoundId, Integer judgeId) {
        return assignFinalRoundG4(finalRoundId, judgeId, JudgeAssignmentType.FINAL_EXTERNAL);
    }

    @Override
    public CreateResult assignFinalRoundG4(Integer finalRoundId, Integer judgeId, JudgeAssignmentType assignmentType) {
        User judge = loadApprovedPersonnel(judgeId);
        JudgeAssignmentType assignType = assignmentType != null
                ? assignmentType
                : JudgeAssignmentType.FINAL_EXTERNAL;
        if (assignType == JudgeAssignmentType.HEAD) {
            throw new BusinessRuleException(ErrorCode.INVALID_ASSIGNMENT_TYPE,
                    "Loại phân công không hợp lệ — Chung kết chỉ chấp nhận Giám khảo khách (FINAL_EXTERNAL) hoặc Giám khảo thường (NORMAL)",
                    Map.of("roundId", finalRoundId, "assignmentType", assignType.name()));
        }
        if (assignType != JudgeAssignmentType.NORMAL && assignType != JudgeAssignmentType.FINAL_EXTERNAL) {
            throw new BusinessRuleException(ErrorCode.INVALID_ASSIGNMENT_TYPE,
                    "Round Chung kết chỉ gán NORMAL hoặc FINAL_EXTERNAL",
                    Map.of("roundId", finalRoundId, "assignmentType", assignType.name()));
        }
        if (assignType == JudgeAssignmentType.FINAL_EXTERNAL && judge.getUserType() != UserType.EXTERNAL) {
            throw new BusinessRuleException(ErrorCode.INVALID_ASSIGNMENT_TYPE,
                    "FINAL_EXTERNAL yêu cầu Judge EXTERNAL",
                    Map.of("roundId", finalRoundId, "judgeId", judge.getId(),
                            "userType", judge.getUserType() == null ? "null" : judge.getUserType().name()));
        }
        if (assignType == JudgeAssignmentType.NORMAL && judge.getUserType() != UserType.INTERNAL) {
            throw new BusinessRuleException(ErrorCode.INVALID_ASSIGNMENT_TYPE,
                    "Giám khảo NORMAL của Chung kết yêu cầu Judge INTERNAL",
                    Map.of("roundId", finalRoundId, "judgeId", judge.getId(),
                            "userType", judge.getUserType() == null ? "null" : judge.getUserType().name()));
        }
        Round round = roundRepository.findById(finalRoundId)
                .orElseThrow(() -> new ResourceNotFoundException("Round", finalRoundId));
        archiveGuard.assertNotArchivedForRound(round);
        if (!Boolean.TRUE.equals(round.getIsFinal())) {
            throw new BusinessRuleException(ErrorCode.INVALID_FINAL_ROUND,
                    "Phân công Judge Chung kết chỉ cho Round FINAL",
                    Map.of("roundId", finalRoundId));
        }
        if (judgeAssignmentRepository.existsByJudgeIdAndRoundId(judge.getId(), finalRoundId)) {
            throw new ConflictException(ErrorCode.JUDGE_ASSIGN_DUPLICATE,
                    "Judge #%d đã được phân công Round Chung kết #%d"
                            .formatted(judge.getId(), finalRoundId));
        }

        List<Warning> warnings = new ArrayList<>();
        Integer hackathonId = round.getHackathon().getId();
        if (judgeAssignmentRepository.hasPreliminaryTrackAssignmentInHackathon(judge.getId(), hackathonId)) {
            warnings.add(Warning.builder()
                    .code(WarningCode.JUDGE_PARTICIPATED_IN_PRELIM)
                    .message("Judge #%d đã tham gia chấm Sơ loại — cân nhắc trước khi phân CK"
                            .formatted(judge.getId()))
                    .build());
        }

        JudgeAssignment saved = saveAssignment(judge, null, round, assignType);
        JudgeAssignmentResponse response = judgeAssignmentMapper.toResponse(saved);
        auditService.log(AuditAction.JUDGE_ASSIGNED, "judge_assignments", saved.getId(), Map.of(
                "judgeId", judge.getId(), "roundId", finalRoundId, "type", assignType.name(),
                "phase", "G4"));
        logHeadChangeIfNeeded(saved, null, assignType, null, finalRoundId);
        notificationService.send(judge, "JUDGE_ASSIGNED",
                "Bạn được phân công làm Judge Chung kết '%s'".formatted(round.getName()),
                "Round: %s".formatted(round.getName()),
                "rounds", finalRoundId);
        sendJudgeAssignmentEmail(judge, "Chung kết '%s'".formatted(round.getName()),
                round.getHackathon() == null ? null : round.getHackathon().getName());
        return new CreateResult(response, warnings);
    }

    @Override
    @Transactional(readOnly = true)
    public List<JudgeAssignmentResponse> listByTrack(Integer trackId) {
        return judgeAssignmentRepository.findByTrackId(trackId).stream()
                .map(judgeAssignmentMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<JudgeAssignmentResponse> listByRound(Integer roundId) {
        log.warn("Deprecated: GET /rounds/{}/judges — dùng GET /tracks/{{trackId}}/judges cho Sơ loại", roundId);
        return judgeAssignmentRepository.findByRoundId(roundId).stream()
                .map(judgeAssignmentMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<JudgeAssignmentResponse> listByJudge(Integer judgeId) {
        return judgeAssignmentRepository.findByJudgeId(judgeId).stream()
                .map(judgeAssignmentMapper::toResponse).toList();
    }

    @Override
    public Integer unassign(Integer id) {
        JudgeAssignment ja = judgeAssignmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("JudgeAssignment", id));
        if (ja.getTrack() != null) {
            archiveGuard.assertNotArchivedForTrack(ja.getTrack());
        } else if (ja.getRound() != null) {
            archiveGuard.assertNotArchivedForRound(ja.getRound());
        }
        User judge = ja.getJudge();
        JudgeAssignmentType previousType = ja.getAssignmentType();
        Integer trackId = ja.getTrack() != null ? ja.getTrack().getId() : null;
        Integer roundId = ja.getRound() != null ? ja.getRound().getId() : null;
        String label = ja.getTrack() != null ? ja.getTrack().getName()
                : (ja.getRound() != null ? ja.getRound().getName() : "?");
        judgeAssignmentRepository.delete(ja);
        notificationService.send(judge, "JUDGE_UNASSIGNED",
                "Bạn không còn phân công Judge '%s'".formatted(label),
                "Unassigned by coordinator.",
                "judge_assignments", id);
        auditService.log(AuditAction.JUDGE_UNASSIGNED, "judge_assignments", id,
                Map.of("judgeId", judge.getId()));
        logHeadChangeIfNeeded(ja, previousType, null, trackId, roundId);
        return id;
    }

    private void logHeadChangeIfNeeded(JudgeAssignment ja, JudgeAssignmentType from,
                                       JudgeAssignmentType to, Integer trackId, Integer roundId) {
        boolean fromHead = from == JudgeAssignmentType.HEAD;
        boolean toHead = to == JudgeAssignmentType.HEAD;
        if (!fromHead && !toHead) {
            return;
        }
        Map<String, Object> detail = new HashMap<>();
        detail.put("judgeId", ja.getJudge() != null ? ja.getJudge().getId() : null);
        detail.put("from", from == null ? null : from.name());
        detail.put("to", to == null ? null : to.name());
        if (trackId != null) {
            detail.put("trackId", trackId);
        }
        if (roundId != null) {
            detail.put("roundId", roundId);
        }
        Integer actorId = currentUserAccessor.currentUserId();
        if (actorId != null) {
            detail.put("actorId", actorId);
        }
        auditService.log(AuditAction.JUDGE_HEAD_CHANGED, "judge_assignments",
                ja.getId() != null ? ja.getId() : 0, detail);
    }

    private void sendJudgeAssignmentEmail(User judge, String assignmentLabel, String hackathonName) {
        try {
            emailService.sendJudgeAssignment(judge.getEmail(), judge.getFullName(),
                    assignmentLabel, hackathonName, FrontendUrls.loginUrl(appProperties));
        } catch (RuntimeException ex) {
            log.warn("[JudgeAssign] email failed for {}: {}", judge.getEmail(), ex.getMessage());
        }
    }

    /**
     * Mentor isolation: user đang mentor bất kỳ đội nào thuộc track → cấm phân công Judge track đó.
     */
    private void assertNotMentorOfTeamInTrack(User judge, Integer trackId) {
        boolean mentorsTeamInTrack = mentorTeamAssignmentRepository.findByMentor_Id(judge.getId()).stream()
                .anyMatch(mta -> mta.getTeam() != null
                        && teamRoundTrackRepository
                                .findByTeam_IdAndTrack_Id(mta.getTeam().getId(), trackId)
                                .isPresent());
        if (mentorsTeamInTrack) {
            throw new BusinessRuleException(ErrorCode.CONFLICT_MENTOR_JUDGE_SAME_TRACK,
                    "User đang là Mentor của một đội trong Bảng #%d — không thể phân công làm Giám khảo bảng này"
                            .formatted(trackId),
                    Map.of("trackId", trackId, "judgeId", judge.getId()));
        }
    }

    private User loadApprovedPersonnel(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User (judge)", userId));
        PersonnelAssignmentRules.requireApprovedPersonnel(user, "Judge");
        return user;
    }

    private JudgeAssignment saveAssignment(User judge, Track track, Round round,
                                           JudgeAssignmentType type) {
        Integer uid = currentUserAccessor.currentUserId();
        return judgeAssignmentRepository.save(JudgeAssignment.builder()
                .judge(judge)
                .track(track)
                .round(round)
                .assignmentType(type)
                .assignedAt(LocalDateTime.now())
                .assignedBy(uid == null ? null : User.builder().id(uid).build())
                .build());
    }
}
