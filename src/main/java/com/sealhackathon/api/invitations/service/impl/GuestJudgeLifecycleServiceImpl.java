package com.sealhackathon.api.invitations.service.impl;

import com.sealhackathon.api.common.exception.AuthException;
import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.events.entity.Event;
import com.sealhackathon.api.events.repository.EventRepository;
import com.sealhackathon.api.events.value_object.EventType;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.invitations.entity.Invitation;
import com.sealhackathon.api.invitations.repository.InvitationRepository;
import com.sealhackathon.api.invitations.service.GuestJudgeLifecycleService;
import com.sealhackathon.api.judge_assignments.entity.JudgeAssignment;
import com.sealhackathon.api.judge_assignments.repository.JudgeAssignmentRepository;
import com.sealhackathon.api.users.entity.User;
import com.sealhackathon.api.users.value_object.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GuestJudgeLifecycleServiceImpl implements GuestJudgeLifecycleService {

    private final InvitationRepository invitationRepository;
    private final EventRepository eventRepository;
    private final JudgeAssignmentRepository judgeAssignmentRepository;

    @Override
    public void assertHackathonNotEndedForTempJudge(User user) {
        if (!Boolean.TRUE.equals(user.getIsTempAccount())) {
            return;
        }
        List<Hackathon> linked = resolveAllHackathons(user);
        if (linked.isEmpty()) {
            return;
        }
        boolean anyActive = linked.stream().anyMatch(h -> !isHackathonEnded(h));
        if (anyActive) {
            return;
        }
        Hackathon ended = linked.get(0);
        throw new AuthException(ErrorCode.TEMP_JUDGE_HACKATHON_ENDED,
                "Tài khoản judge khách đã hết hiệu lực sau khi cuộc thi kết thúc",
                HttpStatus.UNAUTHORIZED,
                Map.of("hackathonId", ended.getId(), "eventEnd", String.valueOf(ended.getEventEnd())));
    }

    @Override
    public void assertHackathonNotEnded(Hackathon hackathon) {
        if (isHackathonEnded(hackathon)) {
            throw new BusinessRuleException(ErrorCode.TEMP_JUDGE_HACKATHON_ENDED,
                    "Hackathon đã kết thúc — không thể mời hoặc gửi lại judge khách",
                    Map.of("hackathonId", hackathon.getId(), "eventEnd", String.valueOf(hackathon.getEventEnd())));
        }
    }

    @Override
    public void assertResendAllowed(Invitation invitation) {
        Hackathon hackathon = requireHackathonOnInvitationEntity(invitation);
        assertHackathonNotEnded(hackathon);

        List<Event> kickoffs = eventRepository.findLatestByType(hackathon.getId(), EventType.KICKOFF);
        if (kickoffs.isEmpty()) {
            throw new BusinessRuleException(ErrorCode.EVENT_KICKOFF_NOT_FOUND,
                    "Chưa có sự kiện KICKOFF — không thể gửi lại lời mời",
                    Map.of("hackathonId", hackathon.getId()));
        }
        Event kickoff = kickoffs.get(0);
        LocalDateTime startsAt = kickoff.getStartsAt();
        if (startsAt == null) {
            throw new BusinessRuleException(ErrorCode.EVENT_KICKOFF_NOT_FOUND,
                    "Sự kiện KICKOFF chưa có thời gian bắt đầu",
                    Map.of("hackathonId", hackathon.getId(), "eventId", kickoff.getId()));
        }

        LocalDateTime now = LocalDateTime.now();
        if (!now.isBefore(startsAt)) {
            throw new BusinessRuleException(ErrorCode.INVITATION_RESEND_AFTER_KICKOFF_CUTOFF,
                    "Không gửi lại lời mời sau khi KICKOFF đã bắt đầu",
                    Map.of("kickoffStartsAt", startsAt.toString()));
        }
        LocalDateTime cutoff = startsAt.minusHours(RESEND_CUTOFF_HOURS_BEFORE_KICKOFF);
        if (now.isAfter(cutoff)) {
            throw new BusinessRuleException(ErrorCode.INVITATION_RESEND_AFTER_KICKOFF_CUTOFF,
                    "Chỉ gửi lại lời mời trước KICKOFF ít nhất " + RESEND_CUTOFF_HOURS_BEFORE_KICKOFF + " giờ",
                    Map.of("kickoffStartsAt", startsAt.toString(), "resendDeadline", cutoff.toString()));
        }
    }

    @Override
    public void requireHackathonOnInvitation(Invitation invitation) {
        requireHackathonOnInvitationEntity(invitation);
    }

    @Override
    public Hackathon requireHackathonOnInvitationEntity(Invitation invitation) {
        if (invitation.getHackathon() == null) {
            throw new BusinessRuleException(ErrorCode.INVITATION_HACKATHON_REQUIRED,
                    "Invitation judge khách phải gắn hackathon",
                    Map.of("invitationId", invitation.getId()));
        }
        return invitation.getHackathon();
    }

    private Optional<Hackathon> resolveHackathon(User user) {
        return resolveAllHackathons(user).stream()
                .filter(h -> !isHackathonEnded(h))
                .findFirst()
                .or(() -> resolveAllHackathons(user).stream().findFirst());
    }

    /** Tất cả hackathon gắn assignment hoặc invitation JUDGE (giữ thứ tự: assignment trước). */
    private List<Hackathon> resolveAllHackathons(User user) {
        java.util.LinkedHashSet<Integer> seen = new java.util.LinkedHashSet<>();
        java.util.ArrayList<Hackathon> out = new java.util.ArrayList<>();
        for (JudgeAssignment ja : judgeAssignmentRepository.findByJudgeId(user.getId())) {
            Hackathon h = hackathonFromAssignment(ja);
            if (h != null && h.getId() != null && seen.add(h.getId())) {
                out.add(h);
            }
        }
        invitationRepository.findByEmail(user.getEmail()).stream()
                .filter(inv -> inv.getRole() == UserRole.JUDGE)
                .map(Invitation::getHackathon)
                .filter(h -> h != null && h.getId() != null && seen.add(h.getId()))
                .forEach(out::add);
        return out;
    }

    private Hackathon hackathonFromAssignment(JudgeAssignment ja) {
        if (ja.getTrack() != null && ja.getTrack().getRound() != null) {
            return ja.getTrack().getRound().getHackathon();
        }
        if (ja.getRound() != null) {
            return ja.getRound().getHackathon();
        }
        return null;
    }

    private static boolean isHackathonEnded(Hackathon hackathon) {
        if (hackathon.getStatus() == HackathonStatus.FINISHED) {
            return true;
        }
        LocalDate eventEnd = hackathon.getEventEnd();
        return eventEnd != null && LocalDate.now().isAfter(eventEnd);
    }
}
