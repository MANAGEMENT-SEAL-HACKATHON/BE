package com.sealhackathon.api.me.service.impl;

import com.sealhackathon.api.common.audit.AuditAction;
import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.common.exception.AuthException;
import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.common.exception.ResourceNotFoundException;
import com.sealhackathon.api.common.security.CurrentUserAccessor;
import com.sealhackathon.api.common.value_object.AssignmentResponseStatus;
import com.sealhackathon.api.judge_assignments.entity.JudgeAssignment;
import com.sealhackathon.api.judge_assignments.repository.JudgeAssignmentRepository;
import com.sealhackathon.api.me.dto.request.AssignmentDeclineRequest;
import com.sealhackathon.api.me.dto.response.AssignmentResponseStatusResponse;
import com.sealhackathon.api.me.service.AssignmentResponseService;
import com.sealhackathon.api.mentors.entity.MentorAssignment;
import com.sealhackathon.api.mentors.entity.MentorTeamAssignment;
import com.sealhackathon.api.mentors.repository.MentorAssignmentRepository;
import com.sealhackathon.api.mentors.repository.MentorTeamAssignmentRepository;
import com.sealhackathon.api.notifications.service.StakeholderBroadcastService;
import com.sealhackathon.api.notifications.value_object.NotificationType;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.tracks.entity.Track;
import com.sealhackathon.api.users.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AssignmentResponseServiceImpl implements AssignmentResponseService {

    private final CurrentUserAccessor currentUserAccessor;
    private final JudgeAssignmentRepository judgeAssignmentRepository;
    private final MentorAssignmentRepository mentorAssignmentRepository;
    private final MentorTeamAssignmentRepository mentorTeamAssignmentRepository;
    private final StakeholderBroadcastService stakeholderBroadcastService;
    private final AuditService auditService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AssignmentResponseStatusResponse declineJudgeAssignment(Integer assignmentId,
                                                                   AssignmentDeclineRequest request) {
        JudgeAssignment ja = loadOwnedJudgeAssignment(assignmentId);
        assertCanDeclineJudge(ja);
        String reason = requireReason(request);

        ja.setResponseStatus(AssignmentResponseStatus.DECLINED);
        ja.setRespondedAt(LocalDateTime.now());
        ja.setDeclineReason(reason);
        judgeAssignmentRepository.save(ja);

        Integer hackathonId = resolveJudgeHackathonId(ja);
        String scope = describeJudgeScope(ja);
        User judge = ja.getJudge();
        String judgeName = judge != null ? judge.getFullName() : ("#" + currentUserAccessor.currentUserId());

        auditService.log(AuditAction.JUDGE_DECLINED, "judge_assignments", ja.getId(), Map.of(
                "hackathonId", hackathonId == null ? "" : hackathonId,
                "scope", scope,
                "reason", reason));

        if (hackathonId != null) {
            stakeholderBroadcastService.broadcast(
                    hackathonId,
                    NotificationType.JUDGE_DECLINED,
                    "Giám khảo từ chối phân công",
                    List.of(
                            judgeName + " đã từ chối phân công: " + scope,
                            "Lý do: " + reason,
                            "Vui lòng phân công lại giám khảo trước khi kích hoạt / phát đề."),
                    "judge_assignments",
                    ja.getId(),
                    true);
        }

        return toJudgeResponse(ja);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AssignmentResponseStatusResponse acceptJudgeAssignment(Integer assignmentId) {
        JudgeAssignment ja = loadOwnedJudgeAssignment(assignmentId);
        ja.setResponseStatus(AssignmentResponseStatus.ACCEPTED);
        ja.setRespondedAt(LocalDateTime.now());
        ja.setDeclineReason(null);
        judgeAssignmentRepository.save(ja);
        return toJudgeResponse(ja);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AssignmentResponseStatusResponse declineMentorAssignment(Integer assignmentId,
                                                                    AssignmentDeclineRequest request) {
        MentorAssignment ma = loadOwnedMentorAssignment(assignmentId);
        assertCanDeclineMentorTrack(ma);
        String reason = requireReason(request);

        ma.setResponseStatus(AssignmentResponseStatus.DECLINED);
        ma.setRespondedAt(LocalDateTime.now());
        ma.setDeclineReason(reason);
        mentorAssignmentRepository.save(ma);

        Integer hackathonId = ma.getTrack() != null && ma.getTrack().getRound() != null
                && ma.getTrack().getRound().getHackathon() != null
                ? ma.getTrack().getRound().getHackathon().getId()
                : null;
        String trackName = ma.getTrack() != null ? ma.getTrack().getName() : ("#" + assignmentId);
        User mentor = ma.getMentor();
        String mentorName = mentor != null ? mentor.getFullName() : ("#" + currentUserAccessor.currentUserId());

        auditService.log(AuditAction.MENTOR_DECLINED, "mentor_assignments", ma.getId(), Map.of(
                "hackathonId", hackathonId == null ? "" : hackathonId,
                "trackId", ma.getTrack() == null ? "" : ma.getTrack().getId(),
                "reason", reason));

        if (hackathonId != null) {
            stakeholderBroadcastService.broadcast(
                    hackathonId,
                    NotificationType.MENTOR_DECLINED,
                    "Mentor từ chối phân công",
                    List.of(
                            mentorName + " đã từ chối hỗ trợ bảng «" + trackName + "»",
                            "Lý do: " + reason,
                            "Vui lòng phân công lại mentor."),
                    "mentor_assignments",
                    ma.getId(),
                    true);
        }

        return toMentorResponse(ma);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AssignmentResponseStatusResponse acceptMentorAssignment(Integer assignmentId) {
        MentorAssignment ma = loadOwnedMentorAssignment(assignmentId);
        ma.setResponseStatus(AssignmentResponseStatus.ACCEPTED);
        ma.setRespondedAt(LocalDateTime.now());
        ma.setDeclineReason(null);
        mentorAssignmentRepository.save(ma);
        return toMentorResponse(ma);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AssignmentResponseStatusResponse declineMentorTeamAssignment(Integer assignmentId,
                                                                        AssignmentDeclineRequest request) {
        MentorTeamAssignment mta = loadOwnedMentorTeamAssignment(assignmentId);
        assertCanDeclineMentorTeam(mta);
        String reason = requireReason(request);

        mta.setResponseStatus(AssignmentResponseStatus.DECLINED);
        mta.setRespondedAt(LocalDateTime.now());
        mta.setDeclineReason(reason);
        mentorTeamAssignmentRepository.save(mta);

        Integer hackathonId = mta.getHackathon() != null ? mta.getHackathon().getId() : null;
        String teamName = mta.getTeam() != null ? mta.getTeam().getTeamName() : ("#" + assignmentId);
        String roundName = mta.getRound() != null ? mta.getRound().getName() : "";
        User mentor = mta.getMentor();
        String mentorName = mentor != null ? mentor.getFullName() : ("#" + currentUserAccessor.currentUserId());

        auditService.log(AuditAction.MENTOR_DECLINED, "mentor_team_assignments", mta.getId(), Map.of(
                "hackathonId", hackathonId == null ? "" : hackathonId,
                "teamId", mta.getTeam() == null ? "" : mta.getTeam().getId(),
                "roundId", mta.getRound() == null ? "" : mta.getRound().getId(),
                "reason", reason));

        if (hackathonId != null) {
            stakeholderBroadcastService.broadcast(
                    hackathonId,
                    NotificationType.MENTOR_DECLINED,
                    "Mentor từ chối phân công đội",
                    List.of(
                            mentorName + " đã từ chối hỗ trợ đội «" + teamName + "»"
                                    + (roundName.isBlank() ? "" : " (vòng " + roundName + ")"),
                            "Lý do: " + reason,
                            "Vui lòng phân công lại mentor cho đội bị ảnh hưởng."),
                    "mentor_team_assignments",
                    mta.getId(),
                    true);
        }

        return toMentorTeamResponse(mta);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AssignmentResponseStatusResponse acceptMentorTeamAssignment(Integer assignmentId) {
        MentorTeamAssignment mta = loadOwnedMentorTeamAssignment(assignmentId);
        mta.setResponseStatus(AssignmentResponseStatus.ACCEPTED);
        mta.setRespondedAt(LocalDateTime.now());
        mta.setDeclineReason(null);
        mentorTeamAssignmentRepository.save(mta);
        return toMentorTeamResponse(mta);
    }

    private JudgeAssignment loadOwnedJudgeAssignment(Integer assignmentId) {
        JudgeAssignment ja = judgeAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("JudgeAssignment", assignmentId));
        Integer userId = currentUserAccessor.currentUserId();
        if (ja.getJudge() == null || !userId.equals(ja.getJudge().getId())) {
            throw new AuthException(ErrorCode.FORBIDDEN, "Phân công không thuộc tài khoản hiện tại",
                    HttpStatus.FORBIDDEN);
        }
        return ja;
    }

    private MentorAssignment loadOwnedMentorAssignment(Integer assignmentId) {
        MentorAssignment ma = mentorAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("MentorAssignment", assignmentId));
        Integer userId = currentUserAccessor.currentUserId();
        if (ma.getMentor() == null || !userId.equals(ma.getMentor().getId())) {
            throw new AuthException(ErrorCode.FORBIDDEN, "Phân công không thuộc tài khoản hiện tại",
                    HttpStatus.FORBIDDEN);
        }
        return ma;
    }

    private MentorTeamAssignment loadOwnedMentorTeamAssignment(Integer assignmentId) {
        MentorTeamAssignment mta = mentorTeamAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("MentorTeamAssignment", assignmentId));
        Integer userId = currentUserAccessor.currentUserId();
        if (mta.getMentor() == null || !userId.equals(mta.getMentor().getId())) {
            throw new AuthException(ErrorCode.FORBIDDEN, "Phân công không thuộc tài khoản hiện tại",
                    HttpStatus.FORBIDDEN);
        }
        return mta;
    }

    private void assertCanDeclineJudge(JudgeAssignment ja) {
        Round round = resolveJudgeRound(ja);
        Track track = ja.getTrack();
        if (isRoundTooLate(round) || isTrackProblemReleased(track)) {
            throw tooLate();
        }
    }

    private void assertCanDeclineMentorTrack(MentorAssignment ma) {
        Track track = ma.getTrack();
        Round round = track != null ? track.getRound() : null;
        if (isRoundTooLate(round) || isTrackProblemReleased(track)) {
            throw tooLate();
        }
    }

    private void assertCanDeclineMentorTeam(MentorTeamAssignment mta) {
        if (isRoundTooLate(mta.getRound())) {
            throw tooLate();
        }
    }

    private static boolean isRoundTooLate(Round round) {
        if (round == null) {
            return false;
        }
        return Boolean.TRUE.equals(round.getIsActive()) || round.getProblemReleasedAt() != null;
    }

    private static boolean isTrackProblemReleased(Track track) {
        return track != null && track.getProblemReleasedAt() != null;
    }

    private static BusinessRuleException tooLate() {
        return new BusinessRuleException(ErrorCode.ASSIGNMENT_DECLINE_TOO_LATE,
                "Không thể từ chối phân công sau khi vòng đã kích hoạt hoặc đã phát đề");
    }

    private static String requireReason(AssignmentDeclineRequest request) {
        if (request == null || !StringUtils.hasText(request.getReason())) {
            throw new BusinessRuleException(ErrorCode.VALIDATION_FAILED, "Lý do từ chối bắt buộc");
        }
        return request.getReason().trim();
    }

    private static Round resolveJudgeRound(JudgeAssignment ja) {
        if (ja.getRound() != null) {
            return ja.getRound();
        }
        if (ja.getTrack() != null) {
            return ja.getTrack().getRound();
        }
        return null;
    }

    private static Integer resolveJudgeHackathonId(JudgeAssignment ja) {
        Round round = resolveJudgeRound(ja);
        if (round != null && round.getHackathon() != null) {
            return round.getHackathon().getId();
        }
        return null;
    }

    private static String describeJudgeScope(JudgeAssignment ja) {
        List<String> parts = new ArrayList<>();
        if (ja.getTrack() != null) {
            parts.add("bảng «" + ja.getTrack().getName() + "»");
        }
        Round round = resolveJudgeRound(ja);
        if (round != null) {
            parts.add("vòng «" + round.getName() + "»");
        }
        return parts.isEmpty() ? ("assignment #" + ja.getId()) : String.join(" / ", parts);
    }

    private static AssignmentResponseStatusResponse toJudgeResponse(JudgeAssignment ja) {
        return AssignmentResponseStatusResponse.builder()
                .assignmentId(ja.getId())
                .assignmentKind("JUDGE")
                .responseStatus(ja.getResponseStatus())
                .respondedAt(ja.getRespondedAt())
                .declineReason(ja.getDeclineReason())
                .build();
    }

    private static AssignmentResponseStatusResponse toMentorResponse(MentorAssignment ma) {
        return AssignmentResponseStatusResponse.builder()
                .assignmentId(ma.getId())
                .assignmentKind("MENTOR_TRACK")
                .responseStatus(ma.getResponseStatus())
                .respondedAt(ma.getRespondedAt())
                .declineReason(ma.getDeclineReason())
                .build();
    }

    private static AssignmentResponseStatusResponse toMentorTeamResponse(MentorTeamAssignment mta) {
        return AssignmentResponseStatusResponse.builder()
                .assignmentId(mta.getId())
                .assignmentKind("MENTOR_TEAM")
                .responseStatus(mta.getResponseStatus())
                .respondedAt(mta.getRespondedAt())
                .declineReason(mta.getDeclineReason())
                .build();
    }
}
